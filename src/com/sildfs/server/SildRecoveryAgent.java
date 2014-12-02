package com.sildfs.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.sildfs.transaction.SildData;
import com.sildfs.transaction.SildLog;
import com.sildfs.transaction.SildNewtxn;
import com.sildfs.transaction.SildTxn;

public class SildRecoveryAgent {
	private String dir;

	public SildRecoveryAgent(String dir) {
		this.setDir(dir);
	}

	protected SildRecoveryAgent() {

	}

	public void recover() {
		clearUncommitted();
		generateFileLocks();
		recoverTxn();
		recoverHistory();
	};

	public void clearUncommitted() {
		try {
			File file = new File(this.getDir() + "/.TXN/");
			File[] txn_list = file.listFiles();
			if (txn_list == null) {
				return;
			}
			for (File txn_dir : txn_list) {
				File f = new File(txn_dir.getAbsolutePath() + "/C");
				if (!f.exists())
					deleteDirectory(txn_dir);
			}
		} catch (Exception e) {
			System.out
					.println("--P-- Unable to clean up the uncommitted files.");
			e.printStackTrace();
		}
	}

	public void generateFileLocks() {
		try {
			File f = new File(this.getDir());
			File[] listOfFiles = f.listFiles();

			ConcurrentHashMap<String, ReadWriteLock> file_locks = new ConcurrentHashMap<String, ReadWriteLock>();
			for (int i = 0; i < listOfFiles.length; i++) {
				String file_name = listOfFiles[i].getName();

				// Ignore the hidden files
				if (file_name.startsWith("."))
					continue;

				// Create file_locks and add them to the map in handler class
				file_locks.put(file_name, new ReentrantReadWriteLock());
			}
			SildHandler.setFile_locks(file_locks);
		} catch (Exception e) {
			System.out.println("Unable to recover file locks.");
			e.printStackTrace();
		}
	};

	public void recoverTxn() {
		try {
			File f = new File(this.getDir() + "/.TXN");
			File e;

			ConcurrentHashMap<Integer, SildTxn> txn_list = new ConcurrentHashMap<Integer, SildTxn>();
			// If no previous transaction took place, ignore
			if (!f.exists())
				return;
			File[] listOfFiles = f.listFiles();

			for (int i = 0; i < listOfFiles.length; i++) {
				String txn_name = listOfFiles[i].getName();
				int tid = Integer.valueOf(txn_name.split(".txn")[1]);
				SildLog.getUsed_txn().put(tid, true);
				e = new File(this.getDir() + "/.TXN/" + txn_name + "/");
				File[] elist = e.listFiles();
				Arrays.sort(elist);
				for (int j = 0; j < elist.length; j++) {
					String file_name = elist[j].getName();
					if (file_name.contains("O"))
						continue;
					if (file_name.contains("C")) {
						SildHandler.getCommitted_txn().put(tid, true);
						continue;
					}

					int seq_num = Integer.valueOf(file_name);
					FileInputStream fis = new FileInputStream(elist[j]);
					ObjectInputStream ois = new ObjectInputStream(fis);
					if (seq_num == 0) {
						SildNewtxn s = (SildNewtxn) ois.readObject();
						SildTxn txn = new SildTxn(tid, s);
						txn_list.put(tid, txn);
					} else {
						txn_list.get(tid).getData_list()
								.put(seq_num, (SildData) ois.readObject());
					}
					ois.close();
				}
			}

			SildHandler.setTxn_list(txn_list);
		} catch (Exception e) {
			System.out
					.println("Not able to recover transaction. Abort previous ones.");
			e.printStackTrace();
		}
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

	public void recoverFiles() {
	}

	public void recoverHistory() {
	}

	public String getDir() {
		return dir;
	}

	public void setDir(String dir) {
		this.dir = dir;
	}

	public static void main(String[] args) {
		SildRecoveryAgent sra = new SildRecoveryAgent("/home/dif/docs");
		sra.generateFileLocks();
	}
}
