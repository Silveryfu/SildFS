package com.sildfs.transaction;

import java.util.HashMap;

import com.sildfs.message.SildReq;

/**
 * Store the transaction in memory
 * 
 * @author dif
 */

public class SildTxn {
	private SildNewtxn new_txn;
	private SildReq old_commit;
	private boolean committed;

	private HashMap<Integer, SildData> data_list;
	private int txn_id;

	public SildTxn() {
	}

	public SildTxn(int txn_id, SildNewtxn new_txn) {
		this.setNew_txn(new_txn);
		this.setTxn_id(txn_id);
		this.setData_list(new HashMap<Integer, SildData>());
	}
	
	public void addData(int seq_num, SildData data) {
		data_list.put(seq_num, data);
	}

	public int getTxn_id() {
		return txn_id;
	}

	public SildNewtxn getNew_txn() {
		return new_txn;
	}

	public void setNew_txn(SildNewtxn new_txn) {
		this.new_txn = new_txn;
	}

	public HashMap<Integer, SildData> getData_list() {
		return data_list;
	}

	public void setTxn_id(int txn_id) {
		this.txn_id = txn_id;
	}

	public void setData_list(HashMap<Integer, SildData> data_list) {
		this.data_list = data_list;
	}
	
	public boolean isCommitted() {
		return committed;
	}

	public void setCommitted(boolean committed) {
		this.committed = committed;
	}
	
	public SildReq getOld_commit() {
		return old_commit;
	}

	public void setOld_commit(SildReq old_commit) {
		this.old_commit = old_commit;
	}
}
