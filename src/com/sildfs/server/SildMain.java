package com.sildfs.server;

/**
 * This file contains the trunk of sildfs. 
 * 
 * @Author: dif
 * @Date: Oct.14 2014
 */

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SildMain implements Runnable {

	/* Server parameters */
	private ServerSocket listenSocket;
	private String ip;
	private int portNumber;
	private String dir;

	/* The default ip and port number, as specified by the sample client */
	private static final String DEFAULT_IP = "127.0.0.1";
	private static final int DEFAULT_PORT = 7896;

	/**
	 * Use executor services, 'pool' for service thread, whose underlying
	 * implementation will be CachedThreadPool for better thread resource
	 * utilization; 'trunk' for listening thread, whose underlying imple-
	 * mentation will be SingleThreadPool
	 */
	private ExecutorService pool, trunk;

	public void startService() {
		trunk = Executors.newSingleThreadExecutor();
		pool = Executors.newCachedThreadPool();

		// Start the listening service
		trunk.execute(this);
	}

	public void run() {
		try {
			/**
			 * Create the server listening socket, binds it to the given or
			 * default Internet address and port number
			 */
			listenSocket = new ServerSocket();
			listenSocket.bind(new InetSocketAddress(InetAddress.getByName(this
					.getIp()), this.getPortNumber()));

			System.out.println("SildFS starts listening to port: "
					+ this.getPortNumber());

		} catch (Exception e) {
			System.out.println("Listening socket "
					+ "fails to initialize. Please check if the port"
					+ this.getPortNumber() + "is already in use.");
			e.printStackTrace();
			return;
		}

		// TODO: handler the client side sudden disconnection here
		while (true) {
			try {
				// Start the lisnening socket
				Socket clientSocket = listenSocket.accept();
				SildHandler handler = new SildHandler(clientSocket);

				// Start handler for this client
				pool.execute(handler);
			} catch (Exception e) {
				System.out.println("Failed to accept new client.");
			}
		}
	}

	SildMain() {
		this.setIp(DEFAULT_IP);
		this.setPortNumber(DEFAULT_PORT);
	}

	SildMain(int portNumber) {
		this.setIp(DEFAULT_IP);
		this.setPortNumber(portNumber);
	}

	SildMain(String ip, int portNumber) {
		this.setIp(ip);
		this.setPortNumber(portNumber);
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

	public static void main(String[] args) {
		
		SildMain sild = new SildMain();
		sild.startService();
	}
}
