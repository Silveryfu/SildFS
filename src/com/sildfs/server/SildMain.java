package com.sildfs.server;

/**
 * This file contains the trunk of SildFS; It can be both a primary server 
 * or a backup server wrapping up the client handler, replica agent, recovery
 * agent.
 *  
 * @author: dif
 */

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sildfs.tool.SildArgParser;
import com.sildfs.tool.SildConfReader;
import com.sildfs.transaction.SildLog;

public class SildMain implements Runnable {

	/* Server identification */
	private boolean isReplica;

	/* Server parameters */
	private ServerSocket listenSocket;
	private String ip;
	private int portNumber;
	private String dir;

	private SildRecoveryAgent recov_agent;
	private SildPeerAgent peer_agent;

	/* The default IP and port number, as specified by the sample client */
	private static final String DEFAULT_IP = "127.0.0.1";
	private static final int DEFAULT_PORT = 7896;

	/**
	 * Use executor services, 'pool' for service thread, whose underlying
	 * implementation will be CachedThreadPool for better thread resource
	 * utilization; 'trunk' for listening thread, whose underlying imple-
	 * mentation will be SingleThreadPool;
	 */
	private ExecutorService pool, trunk;

	/**
	 * The transaction log
	 */
	private SildLog sildlog;

	public void startService() {
		// Run the recovery agent
		this.setAgent(new SildRecoveryAgent(this.getDir()));
		this.getAgent().recover();

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

		TimerTask t = new SildCollector(this.getDir());

		// Running the collector task every interval
		Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(t, 5000, 15 * 60 * 1000);

		// TODO: handle the client side sudden disconnection here
		while (true) {
			try {
				// Start the listening socket
				Socket clientSocket = listenSocket.accept();
				SildHandler handler = new SildHandler(clientSocket);

				// Set the file directory
				handler.setDir(this.getDir());

				handler.setAgent(this.getAgent());

				// Start handler for this client
				pool.execute(handler);
			} catch (Exception e) {
				System.out.println("Failed to accept new client.");
			}
		}
	}

	protected SildMain() {
	}

	public SildMain(String dir) {
		this.setIp(DEFAULT_IP);
		this.setPortNumber(DEFAULT_PORT);
		this.setDir(dir);
	}

	public SildMain(int portNumber, String dir) {
		this.setIp(DEFAULT_IP);
		this.setPortNumber(portNumber);
		this.setDir(dir);
	}

	public SildMain(String ip, String dir) {
		this.setIp(ip);
		this.setPortNumber(DEFAULT_PORT);
		this.setDir(dir);
	}

	public SildMain(String ip, int portNumber, String dir) {
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
	
	public boolean isReplica() {
		return isReplica;
	}

	public void setReplica(boolean isReplica) {
		this.isReplica = isReplica;
	}

	public static void main(String[] args) {
		SildArgParser arg_parser = new SildArgParser();
		SildConfReader conf_reader = new SildConfReader();
		SildMain sild;

		// Parse input arguments
		arg_parser.parse(args);

		if (arg_parser.isPrimary()) {
			/* Start a primary server.
			 * Do sanity check for primary.txt
			 */
			arg_parser.checkFromFile();

			// Read primary configuration file
			conf_reader.read(arg_parser.getPrimaryFile());

			// Create a SildFS primary server
			sild = new SildMain(conf_reader.getIp(),
					conf_reader.getPort(), arg_parser.getDir());
			sild.setReplica(false);

			// Start service
			sild.startService();

		} else if (arg_parser.isReplica()) {
			/* Start a replica server.
			 * Do sanity check for primary.txt
			 */
			arg_parser.checkFromFile();

			// Start a server from command line input
			String dir = arg_parser.getDir();
			String ip = arg_parser.getIp();
			int port = arg_parser.getPort();

			if (ip == null && port == 0) {
				sild = new SildMain(dir);
			} else if (ip == null && port != 0) {
				sild = new SildMain(port, dir);
			} else if (port == 0 && ip != null) {
				sild = new SildMain(ip, dir);
			} else {
				sild = new SildMain(ip, port, dir);
			}
			sild.startService();
		} else if (arg_parser.isReboot()) {
			/* Start a rebooted server.
			 * Do sanity check for primary.txt
			 */
			arg_parser.checkFromFile();

		} else {
			/* Start a server without replication.
			 * Do sanity check for primary.txt
			 */
			arg_parser.checkPlain();
			// Start a server from command line input
			String dir = arg_parser.getDir();
			String ip = arg_parser.getIp();
			int port = arg_parser.getPort();

			// Enumerate the possibilities, setting up server parameters
			if (ip == null && port == 0) {
				sild = new SildMain(dir);
			} else if (ip == null && port != 0) {
				sild = new SildMain(port, dir);
			} else if (port == 0 && ip != null) {
				sild = new SildMain(ip, dir);
			} else {
				sild = new SildMain(ip, port, dir);
			}
			sild.startService();
		}
	}
}
