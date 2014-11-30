package com.sildfs.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

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

	public void run() {

	}

	/* Replicate the committed transaction */
	public void replicate_committed() {
		try {
			// When doing replications for the committed transactions,
			// the other ongoing commits should wait until this operation
			// completes and vice-versa
			SildHandler.rep_lock.writeLock().lock();

			// Obtain the output stream to replica
			Socket so = new Socket(this.getReplica_ip(), this.getReplica_port());
			ObjectOutputStream oos = new ObjectOutputStream(
					so.getOutputStream());

			File f = new File(this.getLog_base());
			File[] listOfFiles = f.listFiles();

			// If there is no transaction
			if (listOfFiles == null) {
				oos.writeObject(new String("0"));
				ObjectInputStream ois = new ObjectInputStream(
						so.getInputStream());
				Object a;
				if ((a = ois.readObject()) instanceof String) {
					String msg = (String) a;
					if (msg.equals("ACK")) {
						System.out
								.println("--P-- Replication completed on replica.");

						// Set the replication finish flag to true and
						// release
						// the
						// lock
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

			ObjectInputStream ois = new ObjectInputStream(so.getInputStream());
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
				} else {
					SildHandler.rep_lock.writeLock().unlock();

					// Wait for 000ms before next sent
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
}
