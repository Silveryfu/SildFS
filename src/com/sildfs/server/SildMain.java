package com.sildfs.server;

/**
 * @Author: Silvery Fu
 * @Date: Oct.14 2014
 */

import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SildMain {
	private ServerSocket listenSocket;
	private int portNumber;

	// The default port number, as specified by the sample client 

	private static final int DEFAULT_PORT = 7896;

	/**
	 * Use executor serivces, 'pool' for service thread, whose underlying
	 * implementation will be CachedThreadPool for better thread resource
	 * utilization; 'thunk' for listening thread, whose underlying imple-
	 * mentation will be SingleThreadPool
	 */
	private ExecutorService pool, trunk;

	/**
	 * Initialize all the thread executors
	 */
	public void startService() {
		trunk = Executors.newSingleThreadExecutor();
		pool = Executors.newCachedThreadPool();
	}

	public void run() {
		try {
			listenSocket = new ServerSocket(this.getPortNumber());
		} catch (Exception e) {
			System.out.println("Listening socket "
					+ "fails to initialize. Please check if the port"
					+ this.getPortNumber() + "is already in use.");
			e.printStackTrace();
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
		System.out.println(args[0]);
		System.out.println(args[1]);
		SildMain sm = new SildMain(Integer.valueOf(args[1]));
	}
}
