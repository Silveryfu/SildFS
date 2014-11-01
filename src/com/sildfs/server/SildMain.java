package com.sildfs.server;

/**
 * This file contains the trunk of sildfs. 
 * 
 * @author: dif
 */

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sildfs.tool.SildArgParser;
import com.sildfs.transaction.SildLog;

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

	/**
	 * The transactional log
	 */
	private SildLog sildlog;

	public void startService() {
		// Initialize the executor service
		trunk = Executors.newSingleThreadExecutor();
		pool = Executors.newCachedThreadPool();

		// Initialize the SildLog
		sildlog = new SildLog();
		
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
					+ "fails to initialize. Please check if the port "
					+ this.getPortNumber() + " is already in use.");
			e.printStackTrace();
			return;
		}

		// TODO: handler the client side sudden disconnection here
		while (true) {
			try {
				// Start the listening socket
				Socket clientSocket = listenSocket.accept();
				SildHandler handler = new SildHandler(clientSocket);
				
				// Set the file directory
				handler.setDir(this.getDir());

				// Start handler for this client
				pool.execute(handler);
			} catch (Exception e) {
				System.out.println("Failed to accept new client.");
			}
		}
	}

	protected SildMain() {
	}

	SildMain(String dir) {
		this.setIp(DEFAULT_IP);
		this.setPortNumber(DEFAULT_PORT);
		this.setDir(dir);
	}

	SildMain(int portNumber, String dir) {
		this.setIp(DEFAULT_IP);
		this.setPortNumber(portNumber);
		this.setDir(dir);
	}

	SildMain(String ip, String dir) {
		this.setIp(ip);
		this.setPortNumber(DEFAULT_PORT);
		this.setDir(dir);
	}

	SildMain(String ip, int portNumber, String dir) {
		this.setIp(ip);
		this.setPortNumber(portNumber);
		this.setDir(dir);
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
	
	public SildLog getSildlog() {
		return sildlog;
	}

	public void setSildlog(SildLog sildlog) {
		this.sildlog = sildlog;
	}

	protected void printParam() {
		System.out.println(this.getIp());
		System.out.println(this.getPortNumber());
		System.out.println(this.getDir());
	}

	public static void main(String[] args) {

		SildArgParser parser = new SildArgParser();

		// Parse input arguments
		parser.parse(args);
		String dir = parser.getDir();
		String ip = parser.getIp();
		int port = parser.getPort();

		// Enumerate the possibilities, setting up server parameters
		SildMain sild;
		if (ip == null && port == 0) {
			sild = new SildMain(dir);
		} else if (ip == null && port != 0) {
			sild = new SildMain(port, dir);
		} else if (port == 0 && ip != null) {
			sild = new SildMain(ip, dir);
		} else {
			sild = new SildMain(ip, port, dir);
		}

		// Start Sild service
		sild.printParam();
		sild.startService();
	}
}
