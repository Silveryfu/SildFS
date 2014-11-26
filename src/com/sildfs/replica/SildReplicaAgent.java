package com.sildfs.replica;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.sildfs.server.SildPrimaryAgent;

/**
 * This file defines a replica agent handling replication with a replica peer;
 * The replica peer will send her IP and port number to the server's listening
 * port, and the SildHandler is responsible to create a replica agent; The
 * replica agent will in turn reply to the peer its socket port number, and
 * start to replicate its server content. Every client request from now on will
 * be replicated before replying an acknowledgment.
 * 
 * SildServerAgent is its counterpart.
 * 
 * @author dif
 */

public class SildReplicaAgent implements Runnable {

	private String ip;
	private int listen_port;
	private ServerSocket listenSocket;
	private String primary_ip;
	private int primary_port;

	/**
	 * SildServerAgent is only valid when current server is a primary server;
	 */
	private SildPrimaryAgent server_agent;
	private static final String INIT_REQUEST = "NEW_REPLICA 0 0 ";

	
	public void startListen() {
		try {
			/**
			 * Create the server listening socket, binds it to the given or
			 * default Internet address and port number
			 */
			listenSocket = new ServerSocket();
			listenSocket.bind(new InetSocketAddress(InetAddress.getByName(this
					.getIp()), 0));
			listen_port = listenSocket.getLocalPort();

			System.out.println("SildFS replica listening to port: "
					+ this.getListen_port());

		} catch (Exception e) {
			System.out.println("Listening socket "
					+ "fails to initialize. Please check if the port "
					+ this.getListen_port() + " is already in use.");
			e.printStackTrace();
			return;
		}

		this.initRequest();
		while (true) {
			try {
				// Start the listening socket
				Socket clientSocket = listenSocket.accept();
				System.out.println("primary server:" + clientSocket.getPort());

			} catch (Exception e) {
				System.out
						.println("Failed to accept new primary server request.");
			}
		}
	}

	public void initRequest() {

		try {
			Socket so_primary = new Socket(this.getPrimary_ip(),
					this.getPrimary_port());
			DataOutputStream out = new DataOutputStream(
					so_primary.getOutputStream());

			String data = this.getPrimary_ip() + " " + this.getListen_port();
			String length = (new Integer(data.length())).toString();

			try {
				out.writeBytes(INIT_REQUEST + length + "\r\n\r\n" + data + "\n");
			} catch (IOException ioe) {
				System.out
						.println("Could not send message to the primary server: "
								+ ioe.getMessage());
				return;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() {
		this.startListen();
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getListen_port() {
		return listen_port;
	}

	public void setListen_port(int listen_port) {
		this.listen_port = listen_port;
	}

	public String getPrimary_ip() {
		return primary_ip;
	}

	public void setPrimary_ip(String primary_ip) {
		this.primary_ip = primary_ip;
	}

	public int getPrimary_port() {
		return primary_port;
	}

	public void setPrimary_port(int primary_port) {
		this.primary_port = primary_port;
	}
}
