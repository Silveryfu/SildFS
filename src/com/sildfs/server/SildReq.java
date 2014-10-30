package com.sildfs.server;

/**
 * A request here contains one transaction from the client, this class convert
 * the request into certain fields, providing methods for each field; The reason
 * why not integrate all the methods and data in simply one method and thus
 * utilizing the stack and improving efficiency is subtle; The major concern is
 * over a high workload on the server when the stack memory can be filled up
 * possibly. Some tradeoff regarding this issue maybe play here in the future.
 * 
 * @author dif
 * @Date: Oct.14 2014
 */

public class SildReq {

	private String header;
	private String data;

	private String method;
	private int txn_id;
	private int seq_num;
	private long data_length;

	private String status;

	SildReq(String raw_data) {
		this.parse(raw_data);
	}

	protected void parse(String raw_data) {
//		System.out.println(raw_data);
		String[] fields = raw_data.split(" ");
		this.setMethod(fields[0]);
		this.setTxn_id(Integer.valueOf(fields[1]));
		this.setSeq_num(Integer.valueOf(fields[2]));
		this.setData_length(Long.valueOf(fields[3]));
	}

	public void parseHeader() {

	}

	public void printAll() {
		System.out.println(this.getMethod() + "\n" + this.getTxn_id() + "\n"
				+ this.getSeq_num() + "\n" + this.getData_length());
	}

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

	public long getData_length() {
		return data_length;
	}

	public void setData_length(long data_length) {
		this.data_length = data_length;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
