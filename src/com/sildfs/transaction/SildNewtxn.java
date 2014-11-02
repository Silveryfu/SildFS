package com.sildfs.transaction;

import java.io.File;
import java.io.PrintStream;
import java.io.Serializable;

import com.sildfs.message.SildResp;

public class SildNewtxn extends SildAbstractEntry implements Serializable {

	private static final long serialVersionUID = 2241696188128158240L;
	private boolean isnewfile;

	public SildNewtxn() {

	}

	public SildNewtxn(int txn_id, String dir, String file) {
		this.setTxn_id(txn_id);
		this.setSeq_num(0);
		this.setDir(dir);
		this.setFile(file);
	}

	public void execute() throws Exception {
		System.out.println(this.getFile());
		try {
			this.setF(new File(this.getDir() + "/" + this.getFile()));

			// If this file does not exist, create a new one
			if (!checkFile(this.getF())) {
				this.getF().createNewFile();
				this.setIsnewfile(true);
			}

			// Obtain the lock on this file; TOCHECK
			obtainLocks(this.getF());

		} catch (Exception e) {
			if (this.isIsnewfile())
				this.getF().delete();
			throw new Exception();
		}
	}

	public void obtainLocks(File f) throws Exception {
	}

	public boolean checkFile(File f) {
		return (f.exists() && !f.isDirectory());
	}

	// For unit test
//	public static void main(String[] args) {
//		SildNewtxn s1 = new SildNewtxn();
//		s1.setDir("/home/dif/docs");
//		s1.setFile("to");
//
//		// Try to run to instance of this code and see the magic
//		System.out.println("success!");
//		while (true)
//			;
//	}

	public boolean isIsnewfile() {
		return isnewfile;
	}

	public void setIsnewfile(boolean isnewfile) {
		this.isnewfile = isnewfile;
	}

}
