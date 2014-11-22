package com.sildfs.replica;

import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;

import com.sildfs.server.SildRecoveryAgent;
import com.sildfs.transaction.SildLog;

/**
 * This file defines the backup replica; Wrapping up client handlers,
 * server agent (deal with the primary server), and recovery agent
 *
 * @author dif
 */

public class SildReplica {
	
	/* Replica server parameters */
	private ServerSocket listenSocket;
	private String ip;
	private int portNumber;
	private String dir;
	
	private SildRecoveryAgent recov_agent;
	private SildServerAgent server_agent;
	
	/* The default IP and port number */
	private static final String DEFAULT_IP = "127.0.0.1";
	private static final int DEFAULT_PORT = 7890;
	
	/**
	 * Use executor services, 'pool' for service thread, whose underlying
	 * implementation will be CachedThreadPool for better thread resource
	 * utilization; 'trunk' for listening thread, whose underlying imple-
	 * mentation will be SingleThreadPool;
	 */
	private ExecutorService pool, trunk;
	
	
	
	
	
	
	
	
	
	
	
	
	
	public String getDir() {
		return dir;
	}

	public void setDir(String dir) {
		this.dir = dir;
	}

	public ServerSocket getListenSocket() {
		return listenSocket;
	}

	public void setListenSocket(ServerSocket listenSocket) {
		this.listenSocket = listenSocket;
	}

	public int getPortNumber() {
		return portNumber;
	}

	public void setPortNumber(int portNumber) {
		this.portNumber = portNumber;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}
	
	public void printParam() {
		System.out.println(this.getIp());
		System.out.println(this.getPortNumber());
		System.out.println(this.getDir());
	}

	public SildRecoveryAgent getAgent() {
		return recov_agent;
	}

	public void setAgent(SildRecoveryAgent agent) {
		this.recov_agent = agent;
	}
}
