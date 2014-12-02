package com.sildfs.transaction;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defines the log which writes on the disk for successful committed 
 * transaction
 * 
 * @author dif
 */

public class SildLog {
	
	/**
	 * The avain_txn_id that every get() will increase it by 1; No setter is given
	 * for this integer to avoid misusage.
	 */
	private static int avail_txn_id;
	private static ConcurrentHashMap<Integer, Boolean> used_txn;
	
	static {
		used_txn = new ConcurrentHashMap<Integer, Boolean>();
	}
	
	public SildLog() {
		avail_txn_id = 0;
	}
	
	public static ConcurrentHashMap<Integer, Boolean> getUsed_txn() {
		return used_txn;
	}

	public static void setUsed_txn(ConcurrentHashMap<Integer, Boolean> used_txn) {
		SildLog.used_txn = used_txn;
	}

	public static synchronized int getAvail_txn_id() {
		Random rn = new Random();
		avail_txn_id = rn.nextInt(100000) + 100000;
		while(used_txn.get(avail_txn_id) != null) {
			avail_txn_id = rn.nextInt(100000) + 100000;
		}
		used_txn.put(avail_txn_id, true);
		return avail_txn_id;
	}
}

	
