package com.sildfs.transaction;

import java.io.File;
import java.io.PrintStream;
import java.io.RandomAccessFile;

import com.sildfs.message.SildResp;

public class SildNewtxn extends SildAbstractEntry implements Runnable {

	private boolean isnewfile;

	public SildNewtxn() {

	}

	public SildNewtxn(int txn_id, String dir, String file, PrintStream out) {
		this.setTxn_id(txn_id);
		this.setSeq_num(0);
		this.setDir(dir);
		this.setFile(file);
		this.setOut(out);

	}

	public void execute() {
		try {
			this.setF(new File(this.getDir() + "/" + this.getFile()));

			// If this file does not exist, create a new one
			if (!checkFile(this.getF())) {
				this.getF().createNewFile();
				this.setIsnewfile(true);
			}

			// Obtain the lock on this file; TOCHECK
			obtainLocks(this.getF());

			// Respond to the client
			SildResp resp = new SildResp("ACK", this.getTxn_id(), 0);
			this.getOut().print(resp.getMessage());
		} catch (Exception e) {
			SildResp resp = new SildResp("ERROR", this.getTxn_id(),
					this.getSeq_num(), 205);
			this.getOut().print(resp.getMessage());
			e.printStackTrace();
			try {
				this.getChannel().close();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
			// if it is a newly created file, remote it
			if (this.isIsnewfile())
				this.getF().delete();
		}
	}

	public void obtainLocks(File f) throws Exception {
	}

	public boolean checkFile(File f) {
		return (f.exists() && !f.isDirectory());
	}

	// For unit test
	public static void main(String[] args) {
		SildNewtxn s1 = new SildNewtxn();
		s1.setDir("/home/dif/docs");
		s1.setFile("to");
		
		// Try to run to instance of this code and see the magic
		System.out.println("success!");
		while (true)
			;
	}
	
	public void run() {
		execute();
	}

	public boolean isIsnewfile() {
		return isnewfile;
	}

	public void setIsnewfile(boolean isnewfile) {
		this.isnewfile = isnewfile;
	}

}
