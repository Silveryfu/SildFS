package com.sildfs.replica;

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

public class SildServerAgent implements Runnable {

	public void initRequest(){
		
	}
	
	
	public void run() {

	}
}
