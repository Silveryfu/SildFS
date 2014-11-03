package com.sildfs.transaction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

public class SildWrite extends SildAbstractEntry {

	private static final long serialVersionUID = -1269244041863421775L;

	public SildWrite(int txn_id, int seq_num, String dir, String file,
			String data) {
		this.setTxn_id(txn_id);
		this.setSeq_num(seq_num);
		this.setDir(dir);
		this.setFile(file);
		this.setData(data);
	}

	public void execute() throws Exception {
		this.setF(new File(this.getDir() + "/" + this.getFile()));
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(
				this.getF(), true)));

		// Append the data to the file;
		out.println(this.getData());
		out.flush();
		out.close();
	}
	
	// For unit testing
//	public static void main(String[] args) {
//	}
}
