package com.sildfs.replica;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sildfs.server.SildHeartBeater;
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
	private String log_dir;
	private String dir;

	/**
	 * SildServerAgent is only valid when current server is a primary server;
	 */
	private SildPrimaryAgent server_agent;
	private static final String INIT_REQUEST = "NEW_REPLICA 0 0 ";

	/**
	 * Use ExecutorService for handling thread
	 */
	private ExecutorService ex;

	public void startListen() {
		try {
			/* Initialize the executor service */
			ex = Executors.newCachedThreadPool();

			/**
			 * Create the server listening socket, binds it to the given or
			 * default Internet address and port number
			 */
			listenSocket = new ServerSocket();
			listenSocket.bind(new InetSocketAddress(InetAddress.getByName(this
					.getIp()), 0));
			listen_port = listenSocket.getLocalPort();

			System.out.println("--R-- SildFS replica listening to port: "
					+ this.getListen_port());

		} catch (Exception e) {
			System.out.println("--R-- Listening socket "
					+ "fails to initialize. Please check if the port "
					+ this.getListen_port() + " is already in use.");
			return;
		}

		// Send the initial request to the primary
		this.initRequest();
		while (true) {
			try {
				// Start the listening socket
				Socket clientSocket = listenSocket.accept();
				System.out.println("--R-- Replicate from primary server on: "
						+ clientSocket.getInetAddress().getHostAddress() + " "
						+ clientSocket.getPort());

				// Use SildReplicaHandler to handle the workload
				SildReplicaHandler sh = new SildReplicaHandler();
				sh.setPrimary_so(clientSocket);
				sh.setDir(this.getDir());
				sh.setLog_dir(this.getLog_dir());

				// Execute the handler
				ex.execute(sh);
			} catch (Exception e) {
				System.out
						.println("--R-- Failed to accept new primary server request.");
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
						.println("--R-- Could not send message to the primary server: "
								+ ioe.getMessage());
			}
			
			// Start listen to the heart-beat of the primary,
			// Reuse this socket as the heart-beat socket
			TimerTask t = new SildHeartBeater(so_primary, true);
			Timer timer = new Timer(true);
			
			// The heart-beat rate is 300ms
			timer.scheduleAtFixedRate(t, 1000, 300);
			
		} catch (Exception e) {
			System.out
			.println("--R-- Could not send initial request to the primary. Check if it exists.");
			// Start listen to the heart-beat of the primary,
			// Reuse this socket as the heart-beat socket
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

	public String getLog_dir() {
		return log_dir;
	}

	public void setLog_dir(String log_dir) {
		this.log_dir = log_dir;
	}

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
}
