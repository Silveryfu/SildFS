package com.sildfs.replica;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.sildfs.server.SildRecoveryAgent;
import com.sildfs.transaction.SildData;
import com.sildfs.transaction.SildNewtxn;

/**
 * A handler for the replica replication workload
 * 
 * @author dif
 */

public class SildReplicaHandler implements Runnable {

	private Socket primary_so;
	private String dir;
	private String log_dir;

	public void run() {
		try {
			File txn_dir = null;
			int seq_counter = 0;
			ObjectInputStream ois = new ObjectInputStream(
					primary_so.getInputStream());
			Object o;
			while (!((o = ois.readObject()) instanceof String)) {
				// If the incoming object is a new transaction entry
				if (o instanceof SildNewtxn) {
					SildNewtxn new_txn = (SildNewtxn) o;
					int tid = new_txn.getTxn_id();
					seq_counter = 0;

					// Put a committed mark for the previous transaction log
					if (txn_dir != null) {
						File committed_mark = new File(
								txn_dir.getAbsolutePath() + "/C");
						committed_mark.createNewFile();
					}

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
				} else {
					// If incoming object is a SildData entry
					SildData sild_data = (SildData) o;
					File sild_data_file = new File(txn_dir.getAbsolutePath()
							+ "/" + seq_counter++);

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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void replay() {
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
