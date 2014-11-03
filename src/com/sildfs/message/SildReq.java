package com.sildfs.message;

import com.sildfs.exception.SildWrongFormatException;

/**
 * A request here contains one transaction from the client, this class convert
 * the request into certain fields, providing methods for each field; The reason
 * why not integrate all the methods and data in simply one method and thus
 * utilizing the stack and improving efficiency is subtle; The major concern is
 * over a high workload on the server when the stack memory can be filled up
 * possibly. Some tradeoff regarding this issue maybe play here in the future.
 * 
 * The SildReq class extends SildEntry, so that up-casting can be done
 * intuitively
 * 
 * @author dif
 */

public class SildReq extends SildAbstractMsg {

	public SildReq() {
	};

	public SildReq(String raw_header) throws Exception {
		this.parseHeader(raw_header);
	}

	public void parseHeader(String raw_header) throws Exception {
		String[] fields = raw_header.split(" ");

		this.setMethod(fields[0].toUpperCase());
		this.setTxn_id(Integer.valueOf(fields[1]));
		this.setSeq_num(Integer.valueOf(fields[2]));
		this.setData_length(Integer.valueOf(fields[3]));
	}

	public void parseData(String raw_data) {
		this.setData(raw_data);
	}

	public void printAll() {
		System.out.println(this.getMethod() + "\n" + this.getTxn_id() + "\n"
				+ this.getSeq_num() + "\n" + this.getData_length() + "\n"
				+ this.getData());
	}

	public static void main(String[] args) {

	}
}
