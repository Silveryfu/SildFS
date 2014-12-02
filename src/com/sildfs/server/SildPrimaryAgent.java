package com.sildfs.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;

import com.sildfs.transaction.SildData;
import com.sildfs.transaction.SildNewtxn;
import com.sildfs.transaction.SildTxn;

/**
 * This file defines a server agent handling replication with a primary server
 * peer; The replica peer will send her listening IP and peer agent's port
 * number to the server's listening port, and the SildHandler is responsible to
 * create a replica agent; The replica agent will start to replicate its server
 * content. Every client request from now on will be replicated before replying
 * an acknowledgment.
 * 
 * Unlike the SildReplicaAgent, this agent runs completely synchronously.
 * 
 * @author dif
 */

public class SildPrimaryAgent implements Runnable {

	private String replica_ip;
	private int replica_port;
	private String log_base;
	private Socket replica_so;
	private String dir;

	private TreeMap<Integer, Integer> commit_order;
	private HashMap<Integer, SildTxn> txn_list;
	private static final int TIME_OUT = 12000;

	public void run() {

	}

	// Receive files from the replica
	public void synchReplica() {
		try {
			long startTime = System.currentTimeMillis();
			System.out.println("--P-- Synchronizing with the replica.");

			int tid = -1;

			File txn_dir = null;
			int seq_counter = 0;
			Integer order = new Integer(-2);
			boolean isExist = false;
			commit_order = new TreeMap<Integer, Integer>();
			txn_list = new HashMap<Integer, SildTxn>();

			// Obtain input stream
			ObjectInputStream ois = new ObjectInputStream(
					replica_so.getInputStream());
			Object o;

			replica_so.setSoTimeout(TIME_OUT);
			while (!((o = ois.readObject()) instanceof String)) {
				// If the incoming object is a new transaction entry
				if (o instanceof SildNewtxn) {

					SildNewtxn new_txn = (SildNewtxn) o;
					tid = new_txn.getTxn_id();

					// Check if replica already has this transaction
					if ((isExist = checkExist(tid)))
						continue;

					// Create a transaction in memory
					SildTxn txn = new SildTxn(tid, new_txn);

					// Store it in the txn_list
					txn_list.put(tid, txn);

					seq_counter = 0;

					// Make a new directory
					txn_dir = new File(this.getLog_base() + "/.txn" + tid);
					txn_dir.mkdir();

					// Record this entry to the disk
					File new_txn_file = new File(txn_dir.getAbsolutePath()
							+ "/" + seq_counter++);
					FileOutputStream fos = new FileOutputStream(new_txn_file);
					ObjectOutputStream oos = new ObjectOutputStream(fos);
					oos.writeObject(new_txn);

					// Flush to disk
					oos.flush();
					fos.flush();
					fos.getFD().sync();
					fos.getFD().sync();
					oos.close();
					fos.close();

					// Put a committed mark and ordering for the previous
					// transaction log
					File committed_mark = new File(txn_dir.getAbsolutePath()
							+ "/C");
					committed_mark.createNewFile();
				} else if (o instanceof Integer) {
					if (isExist)
						continue;

					// Store the commit order in memory
					order = (Integer) o;
					File committed_ordering = new File(
							txn_dir.getAbsolutePath() + "/O" + order.toString());
					committed_ordering.createNewFile();
					commit_order.put(order, (Integer) tid);
				} else {
					if (isExist)
						continue;

					// If incoming object is a SildData entry
					SildData sild_data = (SildData) o;
					File sild_data_file = new File(txn_dir.getAbsolutePath()
							+ "/" + seq_counter);

					// Store it in memory
					txn_list.get(tid).addData(seq_counter++, sild_data);

					// Record this entry to the disk
					sild_data_file.createNewFile();

					FileOutputStream fos = new FileOutputStream(sild_data_file);
					ObjectOutputStream oos = new ObjectOutputStream(fos);
					oos.writeObject(sild_data);

					// Flush to disk
					oos.flush();
					fos.flush();
					fos.getFD().sync();
					fos.getFD().sync();
					oos.close();
					fos.close();
				}
			}
			long endTime = System.currentTimeMillis();
			System.out.println("--P-- Log replication completed -- using: "
					+ (-startTime + endTime) + " ms.");

			// Apply the log to rebuild the files
			replay();

			replyToReplica("ACK");

			System.out.println("--P-- Synchronization completes in "
					+ (endTime - startTime) + " ms.");
		} catch (Exception e) {
			System.out.println("--P-- Failed to synch with the replica.");
		}
	}

	/* Replicate the committed transaction */
	public void replicate_committed() {
		try {
			// When doing replications for the committed transactions,
			// the other ongoing commits should wait until this operation
			// completes and vice-versa
			SildHandler.rep_lock.writeLock().lock();
			
			// Obtain the output stream to replica
			replica_so = new Socket(this.getReplica_ip(),
					this.getReplica_port());
			ObjectOutputStream oos = new ObjectOutputStream(
					replica_so.getOutputStream());

			File f = new File(this.getLog_base());
			File[] listOfFiles = f.listFiles();
			// If there is no transaction
			if (listOfFiles == null || listOfFiles.length == 0) {
				oos.writeObject(new String("0"));
				ObjectInputStream ois = new ObjectInputStream(
						replica_so.getInputStream());
				Object a;
				if ((a = ois.readObject()) instanceof String) {
					String msg = (String) a;
					if (msg.equals("ACK")) {
						System.out
								.println("--P-- Replication completed on replica.");

						// Set the replication finish flag to true and release
						// the
						// lock
						SildHandler.rep_lock.writeLock().unlock();
					} else if (msg.equals("ACKSYN")) {
						System.out
								.println("--P-- Replication completed on replica.");
						synchReplica();
						SildHandler.rep_lock.writeLock().unlock();
					} else {
						SildHandler.rep_lock.writeLock().unlock();

						// Wait for 1000ms before next sent
						Thread.sleep(1000);
						replicate_committed();
					}
				}
				;
				return;
			}

			for (int i = 0; i < listOfFiles.length; i++) {
				File commit_mark = new File(listOfFiles[i].getAbsolutePath()
						+ "/" + "C");

				// If this transaction is already committed
				if (!commit_mark.exists()) {
					continue;
				}

				Integer commit_order = null;
				File[] txn_files = listOfFiles[i].listFiles();
				Arrays.sort(txn_files);

				for (int j = 0; j < txn_files.length; j++) {
					if (txn_files[j].getName().contains("C"))
						continue;
					if (txn_files[j].getName().contains("O")) {
						// Send the commit order to replica
						commit_order = new Integer(txn_files[j].getName()
								.split("O")[1]);
						continue;
					}
					FileInputStream fis = new FileInputStream(txn_files[j]);
					ObjectInputStream ois = new ObjectInputStream(fis);
					oos.writeObject(ois.readObject());
				}

				// Send the commit order at the end
				oos.writeObject(new Integer(commit_order));
			}
			// Write an ending separator
			oos.writeObject(new String("0"));

			ObjectInputStream ois = new ObjectInputStream(
					replica_so.getInputStream());
			Object a;
			if ((a = ois.readObject()) instanceof String) {
				String msg = (String) a;
				if (msg.equals("ACK")) {
					System.out
							.println("--P-- Replication completed on replica.");

					// Set the replication finish flag to true and release
					// the
					// lock
					SildHandler.rep_lock.writeLock().unlock();
				} else if (msg.equals("ACKSYN")) {
					System.out
							.println("--P-- Replication completed on replica.");
					synchReplica();
					SildHandler.rep_lock.writeLock().unlock();
				} else {
					SildHandler.rep_lock.writeLock().unlock();

					// Wait for 1000ms before next sent
					Thread.sleep(1000);
					replicate_committed();
				}
			} else {
				SildHandler.rep_lock.writeLock().unlock();
			}
			;
		} catch (Exception e) {
			System.out.println("--P-- Error occurs during replication.");
			SildHandler.rep_lock.writeLock().unlock();
			e.printStackTrace();
		}
	}

	public void replicate_live(int tid) {
		try {
			Socket so = new Socket(this.getReplica_ip(), this.getReplica_port());
			ObjectOutputStream oos = new ObjectOutputStream(
					so.getOutputStream());

			File f = new File(this.getLog_base() + "/.txn" + tid);

			Integer commit_order = null;
			File[] txn_files = f.listFiles();
			Arrays.sort(txn_files);

			oos.writeObject(new Character('Y'));
			for (int j = 0; j < txn_files.length; j++) {
				if (txn_files[j].getName().contains("C"))
					continue;
				if (txn_files[j].getName().contains("O")) {
					// Send the commit order to replica
					commit_order = new Integer(txn_files[j].getName()
							.split("O")[1]);
					continue;
				}
				FileInputStream fis = new FileInputStream(txn_files[j]);
				ObjectInputStream ois = new ObjectInputStream(fis);
				oos.writeObject(ois.readObject());
			}

			// Send the commit order at the end
			oos.writeObject(new Integer(commit_order));

			// Write an ending separator
			oos.writeObject(new String("0"));

			ObjectInputStream ois = new ObjectInputStream(so.getInputStream());
			Object a;
			if ((a = ois.readObject()) instanceof String) {
				String msg = (String) a;
				if (msg.equals("ACK")) {
					System.out
							.println("--P-- Live-replication completed on replica.");
				} else {
					replicate_committed();
				}
			}
			;

		} catch (Exception e) {
			System.out.println("--P-- Live-replication meets some errors.");
			e.printStackTrace();
		}
	}

	public void replay() {
		long startTime = System.currentTimeMillis();
		try {
			for (int order : commit_order.keySet()) {
				int tid = commit_order.get(order);
				SildTxn txn = txn_list.get(tid);

				// Execute the new transaction call
				SildNewtxn new_txn = txn.getNew_txn();

				// Modify the directory to primary's
				new_txn.setDir(this.getDir());
				new_txn.execute();

				// Obtain the update data list
				HashMap<Integer, SildData> data_list = txn.getData_list();

				// Get the update text data
				StringBuilder text = new StringBuilder();
				for (int j = 1; j <= data_list.size(); j++) {
					text.append(data_list.get(j).getData());
				}

				// Update the file; flush to disk
				File f = new_txn.getF();
				FileOutputStream fos = new FileOutputStream(f, true);
				fos.write(text.toString().getBytes());
				fos.flush();
				fos.getFD().sync();
				fos.getFD().sync();
				fos.close();
			}
			long endTime = System.currentTimeMillis();
			System.out.println("--P-- Files updated -- using: "
					+ (-startTime + endTime) + " ms.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean checkExist(int tid) {
		try {
			File txn_dir = new File(this.getLog_base() + "/.txn" + tid);
			if (txn_dir.exists()) {
				File f = new File(txn_dir.getAbsolutePath() + "/C");
				if (f.exists()) {
					return true;
				} else
					deleteDirectory(txn_dir);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public void deleteDirectory(File path) throws Exception {
		File[] sub = path.listFiles();
		for (File file : sub) {
			if (file.isDirectory()) {
				deleteDirectory(file);
				file.delete();
			} else {
				file.delete();
			}
		}
		path.delete();
	}

	public void replyToReplica(String message) {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(
					this.replica_so.getOutputStream());
			oos.writeObject(message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* Replicate the current base files */
	public void replicate_files() {

	}

	public String getReplica_ip() {
		return replica_ip;
	}

	public void setReplica_ip(String replica_ip) {
		this.replica_ip = replica_ip;
	}

	public int getReplica_port() {
		return replica_port;
	}

	public void setReplica_port(int replica_port) {
		this.replica_port = replica_port;
	}

	public void foo() {
		System.out.println("--P-- Replica is ready on: " + this.getReplica_ip()
				+ " " + this.getReplica_port());
	}

	public String getLog_base() {
		return log_base;
	}

	public void setLog_base(String log_base) {
		this.log_base = log_base;
	}

	public String getDir() {
		return dir;
	}

	public void setDir(String dir) {
		this.dir = dir;
	}
}
