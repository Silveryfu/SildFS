package com.sildfs.message;

/**
 * A request here contains one transaction from the client, this class convert
 * the request into certain fields, providing methods for each field; The reason
 * why not integrate all the methods and data in simply one method and thus
 * utilizing the stack and improving efficiency is subtle; The major concern is
 * over a high workload on the server when the stack memory can be filled up
 * possibly. Some tradeoff regarding this issue maybe play here in the future.
 * 
 * The SildReq class extends SildEntry, so that it may be put in the log without
 * casting
 * 
 * @author dif
 * @Date: Oct.14 2014
 */

import com.sildfs.transaction.SildEntry;

public class SildReq extends SildEntry {

	private String header;
	private String method;

	public SildReq() {
	};

	public SildReq(String raw_data) {
		this.parseHeader(raw_data);
	}

	public void parseHeader(String raw_data) {
		try {
			String[] fields = raw_data.split(" ");
			this.setMethod(fields[0]);
			this.setTxn_id(Integer.valueOf(fields[1]));
			this.setSeq_num(Integer.valueOf(fields[2]));
			this.setData_length(Integer.valueOf(fields[3]));
		} catch (Exception e) {
			System.out.println("Wrong header format.");
		}
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

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}
}
