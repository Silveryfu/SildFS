package com.sildfs.server;

/**
 * This file contains the trunk of SildFS; It can be both a primary server 
 * or a backup server wrapping up the client handler, replica agent, recovery
 * agent.
 *  
 * @author: dif
 */

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sildfs.replica.SildReplicaAgent;
import com.sildfs.tool.SildArgParser;
import com.sildfs.tool.SildConfModifier;
import com.sildfs.tool.SildConfReader;
import com.sildfs.transaction.SildLog;

public class SildMain implements Runnable {

	private static SildMain sild;

	/* Server identification */
	private boolean isReplica;

	/* Server parameters */
	private ServerSocket listenSocket;
	private String ip;
	private int portNumber;
	private String dir;

	/**
	 * SildRecoveryAgent is responsible for recovery routine; SildReplicaAgent
	 * is only valid when current server is a replica server;
	 */
	private SildRecoveryAgent recov_agent;
	private SildReplicaAgent replica_agent;

	/* The default IP and port number, as specified by the sample client */
	private static final String DEFAULT_IP = "127.0.0.1";
	private static final int DEFAULT_PORT = 7896;

	/**
	 * Use executor services, 'pool' for service thread, whose underlying
	 * implementation will be CachedThreadPool for better thread resource
	 * utilization; 'trunk' for listening thread, whose underlying imple-
	 * mentation will be SingleThreadPool;
	 */
	private ExecutorService pool, trunk, replica;

	/**
	 * The transaction log
	 */
	private SildLog sildlog;

	/**
	 * These two attributes are only meaningful when the current server is a
	 * replica server; backup_port is for when replica becomes primary.
	 */
	private String primary_ip;
	private int primary_port;

	/**
	 * The path to the primary server configuration file
	 */
	private static String primary_file;

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

	public void startReplicaService() {
		// Initialize the executor service for replica agent thread
		replica = Executors.newSingleThreadExecutor();

		// Initialize the file folder and log folder
		File file_dir = new File(this.getDir());
		File log_dir = new File(this.getDir() + "/.TXN/");
		if (!file_dir.exists())
			file_dir.mkdirs();
		if (!log_dir.exists())
			log_dir.mkdir();

		// Initialize the replica agent
		replica_agent = new SildReplicaAgent();

		replica_agent.setIp(this.getIp());
		replica_agent.setPrimary_ip(this.getPrimary_ip());
		replica_agent.setPrimary_port(this.getPrimary_port());
		replica_agent.setDir(this.getDir());
		replica_agent.setLog_dir(log_dir.getAbsolutePath());

		replica.execute(replica_agent);
	}

	public void stopReplicaAgent() {
		// Shutdown the replica agent
		this.replica.shutdown();
		System.out
				.println("--R-- Replica agent stopping. Modifying primary configuration file.");

		// Modify the primary server configuration file
		SildConfModifier sm = new SildConfModifier();
		sm.modify(primary_file, this.getIp(), this.getPortNumber());
		System.out.println("--R-- Configuration file modification completed.");

		this.setPortNumber(this.getPortNumber());
		this.setReplica(false);
		System.out
				.println("--R-- Primary server configuration completed. Launching primary server.");
	}
	
	public void reboot() {
		// Run the recovery agent
		this.setAgent(new SildRecoveryAgent(this.getDir()));
		this.getAgent().clearUncommitted();
		this.getAgent().recover();
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

			System.out
					.println("--P-- SildFS primary starts listening to port: "
							+ this.getPortNumber());

		} catch (Exception e) {
			System.out.println("--P-- Listening socket "
					+ "fails to initialize. Please check if the port "
					+ this.getPortNumber() + " is already in use.");
			e.printStackTrace();
			return;
		}

		// TimerTask t = new SildCollector(this.getDir());

		// Running the collector task every interval
		// Timer timer = new Timer(true);
		// timer.scheduleAtFixedRate(t, 5000, 60 * 60 * 1000);

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
				System.out.println("--P-- Failed to accept new client.");
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

	public SildMain(String ip, int portNumber, String dir, String primaryIp,
			int primaryPort) {
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

	public static String getPrimary_file() {
		return primary_file;
	}

	public static void setPrimary_file(String primary_file) {
		SildMain.primary_file = primary_file;
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

	public static SildMain getSild() {
		return sild;
	}

	public static void setSild(SildMain sild) {
		SildMain.sild = sild;
	}

	public static void main(String[] args) {
		SildArgParser arg_parser = new SildArgParser();
		SildConfReader conf_reader = new SildConfReader();
		SildMain sild;

		// Parse input arguments
		arg_parser.parse(args);

		if (arg_parser.isPrimary()) {
			/*
			 * Start a primary server. Do sanity check for primary.txt
			 */
			arg_parser.checkFromFile();

			// Read primary configuration file
			conf_reader.read(arg_parser.getPrimaryFile());

			// Create a SildFS primary server
			sild = new SildMain(conf_reader.getIp(), conf_reader.getPort(),
					arg_parser.getDir());
			sild.setReplica(false);

			// Start service
			sild.startService();

		} else if (arg_parser.isReplica()) {
			/*
			 * Start a replica server. Do sanity check for primary.txt
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
			/*
			 * Start a rebooted server. Do sanity check for primary.txt
			 */
			arg_parser.checkFromFile();

		} else {
			/*
			 * Start a server without replication. Do sanity check for
			 * primary.txt
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
