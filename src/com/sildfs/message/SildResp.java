package com.sildfs.message;

import java.util.HashMap;

/**
 * Defines the responding message to the client
 * 
 * @author dif
 */

public class SildResp {

	private String error_code;
	private String message;
	private static HashMap<Integer, String> error_map;

	static {
		error_map = new HashMap<Integer, String>();
		error_map.put(201, "Invalid TXN ID");
		error_map.put(202, "Invalid operation");
		error_map.put(204, "Wrong message format");
		error_map.put(205, "File I/O error");
		error_map.put(206, "File not found");
		error_map.put(207, "Bad Sequence number. Invalid operation.");

		// For there is no error
		error_map.put(-1, "\r\n");
	}

	public SildResp() {

	}

	// For ACK and ASK_RESEND
	public SildResp(String method, int txn_id, int seq_num) {
		StringBuilder mbuilder = new StringBuilder();

		mbuilder.append(method + " ");
		mbuilder.append(txn_id + " ");
		mbuilder.append(seq_num + " ");
		mbuilder.append(-1 + " ");
		mbuilder.append("0\r\n\r\n\r\n");

		this.setMessage(mbuilder.toString() + "\n");
	}

	// For ERROR method only
	public SildResp(String method, int txn_id, int seq_num, int error_code) {
		StringBuilder mbuilder = new StringBuilder();

		mbuilder.append(method + " ");
		mbuilder.append(txn_id + " ");
		mbuilder.append(seq_num + " ");
		mbuilder.append(error_code + " ");
		mbuilder.append(error_map.get(error_code).length() + "\r\n\r\n");
		mbuilder.append(error_map.get(error_code));

		this.setMessage(mbuilder.toString() + "\n");
	}

	// For READ reply header
	public SildResp(String method, int txn_id, int seq_num, int error_code,
			long content_length) {
		StringBuilder mbuilder = new StringBuilder();
		mbuilder.append(method + " ");
		mbuilder.append(txn_id + " ");
		mbuilder.append(seq_num + " ");
		mbuilder.append(-1 + " ");
		mbuilder.append(content_length + "\r\n\r\n");

		this.setMessage(mbuilder.toString());
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public static void main(String[] args) {
		System.out.println("i here");
	}
}
