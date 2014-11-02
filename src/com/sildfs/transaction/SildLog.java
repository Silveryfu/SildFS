package com.sildfs.transaction;

/**
 * Defines the log which writeS on the disk for successful committed 
 * transaction
 * 
 * @author admin
 */

public class SildLog {
	
	/**
	 * The avain_txn_id that every get() will increase it by 1; No setter is given
	 * for this integer to avoid misusage.
	 */
	private static int avail_txn_id;

	/* The location where the log is stores */
	private String log_dir;
	
	/* The log file name */
	private String log_file;
	
	private static final String LOG_DIR = "./.sild";
	private static final String LOG_FILE = ".sildlog";
	
	public SildLog() {
		this.avail_txn_id = 0;
		this.setLog_dir(LOG_DIR);
		this.setLog_file(LOG_FILE);
	}
	
	public void createDir() {
		
	}
	
	public void createLog() {
		
	}
	
	public static synchronized int getAvail_txn_id() {
		return ++avail_txn_id;
	}
	
	public void setLog_dir(String log_dir) {
		this.log_dir = log_dir;
	}
	
	public String getLog_file() {
		return log_file;
	}
	
	public String getLog_dir() {
		return log_dir;
	}

	public void setLog_file(String log_file) {
		this.log_file = log_file;
	}
	
	public static void main(String[] args) {
	}
}

	
