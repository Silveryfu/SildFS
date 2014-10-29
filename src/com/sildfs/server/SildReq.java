package com.sildfs.server;

/**
 * A request is one packet from the client, this class convert the request into
 * certain fields, providing methods for each field; The reason why not
 * integrate all the methods and data in simply one method and thus utilizing
 * the stack and improving efficiency is subtle; The major concern is over a
 * high workload on the server when the stack memory can be filled up
 * forseenably. Some tradeoff regarding this issue maybe play here in the
 * future.
 * 
 * @author dif
 * @Date: Oct.14 2014
 */

public class SildReq {

	private String header;
	private String data;
	
	private String method;
	private String txn_id;
	private String seq_num;
	private String data_length;
	
	
	
	public static void main(String[] args) {

	}
	
	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getTxn_id() {
		return txn_id;
	}

	public void setTxn_id(String txn_id) {
		this.txn_id = txn_id;
	}

	public String getSeq_num() {
		return seq_num;
	}

	public void setSeq_num(String seq_num) {
		this.seq_num = seq_num;
	}

	public String getData_length() {
		return data_length;
	}

	public void setData_length(String data_length) {
		this.data_length = data_length;
	}
}
