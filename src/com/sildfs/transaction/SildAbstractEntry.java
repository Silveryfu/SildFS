package com.sildfs.transaction;

import java.io.File;
import java.io.PrintStream;
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Defines an abstract entry for one SildTxn; a committed log will become an
 * entry in SildLog; Subclasses are SildNewtxn, SildWrite, SildCommit, and
 * SildAbort; Noticeably, here we need to use reentrant lock for file locking
 * , since the FileLock is shared by the currently running JVM
 * 
 * @author: dif
 */

public abstract class SildAbstractEntry implements Serializable {

	private static final long serialVersionUID = -2965180848659888333L;
	private int txn_id;
	private int seq_num;
	private String dir;
	private String file;
	private FileChannel channel;
	private ReentrantLock lock;
	private File f;

	public abstract void execute() throws Exception; 

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public String getDir() {
		return dir;
	}

	public void setDir(String dir) {
		this.dir = dir;
	}

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

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public FileChannel getChannel() {
		return channel;
	}

	public void setChannel(FileChannel channel) {
		this.channel = channel;
	}

	public ReentrantLock getLock() {
		return lock;
	}

	public void setLock(ReentrantLock lock) {
		this.lock = lock;
	}
	
	public File getF() {
		return f;
	}
	
	public void setF(File f) {
		this.f = f;
	}
}
