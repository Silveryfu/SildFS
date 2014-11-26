package com.sildfs.server;

/**
 * This file defines a server agent handling replication with a primary server
 * peer; The replica peer will send her listening IP and peer agent's port
 * number to the server's listening port, and the SildHandler is responsible to
 * create a replica agent; The replica agent will start to replicate its server
 * content. Every client request from now on will be replicated before replying
 * an acknowledgment.
 * 
 * @author dif
 */

public class SildPrimaryAgent implements Runnable {

	private String replica_ip;
	private int replica_port;

	public void run() {

	}

	public String getReplica_ip() {
		return replica_ip;
	}

	public void setReplica_ip(String replica_ip) {
		this.replica_ip = replica_ip;
	}

	public int getReplica_port() {
		return replica_port;
	}

	public void setReplica_port(int replica_port) {
		this.replica_port = replica_port;
	}
	
	public void foo() {
		System.out.println(this.getReplica_ip() + " " + this.getReplica_port());
	}
}
