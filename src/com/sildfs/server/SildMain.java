package com.sildfs.server;

/**
 * This file contains the trunk of sildfs. 
 * 
 * 
 * @Author: dif
 * @Date: Oct.14 2014
 */

import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SildMain implements Runnable {

	/* Server parameters */
	private ServerSocket listenSocket;
	private int portNumber;
	private int proctocolType;

	/* Store the connected client handler list */
	private List<SildHandler> handlerList;

	/* The default port number, as specified by the sample client */
	private static final int DEFAULT_PORT = 7896;
	private static final int TYPE_TCP = 1;
	private static final int TYPE_UDP = 2;

	/**
	 * Use executor services, 'pool' for service thread, whose underlying
	 * implementation will be CachedThreadPool for better thread resource
	 * utilization; 'trunk' for listening thread, whose underlying imple-
	 * mentation will be SingleThreadPool
	 */
	private ExecutorService pool, trunk;

	/**
	 * Initialize all the thread executors
	 */
	public void startService() {
		trunk = Executors.newSingleThreadExecutor();
		pool = Executors.newCachedThreadPool();

		// Initialiize the client list
		handlerList = new LinkedList<SildHandler>();
		
		// Start the listening service
		trunk.execute(this);
	}

	public void run() {
		try {
			listenSocket = new ServerSocket(this.getPortNumber());
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
				
				// Add handler to the list and start handling
				handlerList.add(handler);
				pool.execute(handler);
			} catch (Exception e) {
				System.out.println("Failed to accept new client.");
			}
		}
	}

	SildMain() {
		this.setPortNumber(DEFAULT_PORT);
	}

	SildMain(int portNumber) {
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

	public static void main(String[] args) {
		// System.out.println(args[0]);
		// System.out.println(args[1]);

		// Use the default port number
		SildMain sm = new SildMain();
		sm.startService();
	}
}
