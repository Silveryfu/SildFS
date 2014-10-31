package com.sildfs.message;
/**
 * An abstract class for all messages.
 * 
 * @Author: dif
 * @Date: Oct.14 2014
 */
public abstract class SildMsg {

	private int txn_id;
	private int seq_num;
	private int data_length;
	private String data;
	
	public int getTxn_id() {
		return txn_id;
	}

	public void setTxn_id(int txn_id) {
		this.txn_id = txn_id;
	}

	public int getSeq_num() {
		return seq_num;
	}

	public void setSeq_num(int seq_num) {
		this.seq_num = seq_num;
	}

	public int getData_length() {
		return data_length;
	}

	public void setData_length(int data_length) {
		this.data_length = data_length;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public static void main(String[] args) {

	}

}
