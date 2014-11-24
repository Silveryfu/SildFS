package com.sildfs.server;

/**
 * This file defines a replica agent handling replication with a replica peer;
 * The replica peer will send her IP and port number to the server's listening
 * port, and the SildHandler is responsible to create a replica agent; The
 * replica agent will in turn reply to the peer its socket port number, and
 * start to replicate its server content. Every client request from now on
 * will be replicated before replying an acknowledgment.
 * 
 * SildServerAgent is its counterpart.
 * 
 * @author dif
 */

public class SildReplicaAgent {

}
