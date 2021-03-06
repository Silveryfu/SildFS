package com.sildfs.replica;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import com.sildfs.transaction.SildData;
import com.sildfs.transaction.SildNewtxn;
import com.sildfs.transaction.SildTxn;

/**
 * A handler for the replica replication workload
 * 
 * @author dif
 */

public class SildReplicaHandler implements Runnable {

	private Socket primary_so;
	private String dir;
	private String log_dir;
	private TreeMap<Integer, Integer> commit_order;
	private HashMap<Integer, SildTxn> txn_list;
	private static final int TIME_OUT = 12000;
	private HashSet<Integer> sent;

	public void run() {
		boolean isLiveReplication = false;
		int tid = -1;
		try {
			File txn_dir = null;
			int seq_counter = 0;
			Integer order = new Integer(-2);
			boolean isExist = false;
			sent = new HashSet<Integer>();
			commit_order = new TreeMap<Integer, Integer>();
			txn_list = new HashMap<Integer, SildTxn>();
			long startTime = System.currentTimeMillis();

			// Obtain input stream
			ObjectInputStream ois = new ObjectInputStream(
					primary_so.getInputStream());
			Object o;

			primary_so.setSoTimeout(TIME_OUT);
			while (!((o = ois.readObject()) instanceof String)) {
				// If the incoming object is a new transaction entry
				if (o instanceof SildNewtxn) {

					SildNewtxn new_txn = (SildNewtxn) o;
					tid = new_txn.getTxn_id();

					// Check if replica already has this transaction
					if ((isExist = checkExist(tid)))
						continue;
					
					sent.add(new Integer(tid));
					
					// Create a transaction in memory
					SildTxn txn = new SildTxn(tid, new_txn);

					// Store it in the txn_list
					txn_list.put(tid, txn);

					seq_counter = 0;

					// Make a new directory
					txn_dir = new File(this.getLog_dir() + "/.txn" + tid);
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
				} else if (o instanceof Character) {

					// If it is live replication messages
					isLiveReplication = true;
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
			System.out.println("--R-- Log replication completed -- using: "
					+ (-startTime + endTime) + " ms.");
			// Apply the live replication
			if (isLiveReplication)
				replayLive(tid);

			// Apply the log to rebuild the files
			else {
				replay();
				replyToPrimary("ACKSYN");

				// Send to the primary those logs the primary does not contain
				synchPrimary();
				return;
			}
			// Reply an ACK to the primary
			replyToPrimary("ACK");
		} catch (Exception e) {
			System.out
					.println("--R-- Malicious attacks detected. Request ignored.");
		}
	}

	public void synchPrimary() {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(
					primary_so.getOutputStream());

			File f = new File(this.getLog_dir());
			File[] listOfFiles = f.listFiles();
			
			// If there is no transaction
			if (listOfFiles == null || listOfFiles.length == 0) {
				oos.writeObject(new String("0"));
				ObjectInputStream ois = new ObjectInputStream(
						primary_so.getInputStream());
				Object a;
				if ((a = ois.readObject()) instanceof String) {
					String msg = (String) a;
					if (msg.equals("ACK")) {
						System.out
								.println("--R-- Synchonization completed on primary.");
					} else {
						// Wait for 1000ms before next sent
						Thread.sleep(1000);
						synchPrimary();
					}
				}
				;
				return;
			}

			for (int i = 0; i < listOfFiles.length; i++) {
				if (sent.contains(new Integer(Integer.valueOf(listOfFiles[i]
						.getName().split("n")[1])))) {
					continue;
				}

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
					primary_so.getInputStream());
			Object a;
			if ((a = ois.readObject()) instanceof String) {
				String msg = (String) a;
				if (msg.equals("ACK")) {
					System.out
							.println("--R-- Synchronization completed on primary.");

					// Set the replication finish flag to true and release
					// the
					// lock
				} else {

					// Wait for 000ms before next sent
					Thread.sleep(1000);
				}
			} else {
			}
			;
		} catch (Exception e) {
			System.out.println("--R-- Error occurs during synchronization.");
			e.printStackTrace();
		}
	}

	public void replyToPrimary(String message) {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(this
					.getPrimary_so().getOutputStream());
			oos.writeObject(message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void replayLive(int tid) {
		long startTime = System.currentTimeMillis();
		try {

			SildTxn txn = txn_list.get(tid);
			// Execute the new transaction call
			SildNewtxn new_txn = txn.getNew_txn();

			// Modify the directory to replica's
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

			long endTime = System.currentTimeMillis();
			System.out
					.println("--R-- Files updated for live replication -- using: "
							+ (-startTime + endTime) + " ms.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* Apply the log entries */
	public void replay() {
		long startTime = System.currentTimeMillis();
		try {
			for (int order : commit_order.keySet()) {
				int tid = commit_order.get(order);

				SildTxn txn = txn_list.get(tid);

				// Execute the new transaction call
				SildNewtxn new_txn = txn.getNew_txn();

				// Modify the directory to replica's
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
			System.out.println("--R-- Files updated -- using: "
					+ (-startTime + endTime) + " ms.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Check if the transaction log already exists; If it exists but not yet
	 * committed, remove the directory.
	 * 
	 * @param tid
	 * @return
	 */
	public boolean checkExist(int tid) {
		try {
			File txn_dir = new File(this.getLog_dir() + "/.txn" + tid);
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

	public Socket getPrimary_so() {
		return primary_so;
	}

	public void setPrimary_so(Socket primary_so) {
		this.primary_so = primary_so;
	}

	public String getLog_dir() {
		return log_dir;
	}

	public void setLog_dir(String log_dir) {
		this.log_dir = log_dir;
	}

	public String getDir() {
		return dir;
	}

	public void setDir(String dir) {
		this.dir = dir;
	}
}
