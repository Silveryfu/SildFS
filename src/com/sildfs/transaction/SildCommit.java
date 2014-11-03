package com.sildfs.transaction;

public class SildCommit extends SildAbstractEntry {

	private static final long serialVersionUID = 1L;

	public SildCommit(int txn_id, int seq_num, String dir, String file,
			String data) {
		this.setTxn_id(txn_id);
		this.setSeq_num(seq_num);
		this.setDir(dir);
		this.setFile(file);
		this.setData(data);
	}

	public void execute() throws Exception {
		
		
	}
}
