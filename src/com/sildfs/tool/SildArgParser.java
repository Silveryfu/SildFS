package com.sildfs.tool;

/**
 * Define a parser for parsing input arguments.
 * 
 * @author dif
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sildfs.exception.SildDepracatedException;
import com.sildfs.exception.SildInvalidReplicaException;
import com.sildfs.exception.SildNoDirectorySpecifiedException;
import com.sildfs.exception.SildNoPrimaryConfFoundException;
import com.sildfs.exception.SildUndefinedArgException;

public class SildArgParser {
	private String ip;
	private String dir;
	private int port;
	private String primary;

	// Flags for different server types
	private boolean isReplicated;
	private boolean isReboot;
	private boolean isFresh;

	// The IP, port number, and directory for the replicas
	private String Rip;
	private int Rport;
	private String Rdir;

	public SildArgParser() {

	}

	public void parse(String[] args) {
		try {
			for (int i = 0; i < args.length; i = i + 2) {
				if (args[i].equals("-ip")) {
					this.setIp(checkIp(args[i + 1]));
				} else if (args[i].equals("-port")) {
					this.setPort(checkPort(Integer.valueOf(args[i + 1])));
				} else if (args[i].equals("-dir")) {
					this.setDir(args[i + 1]);
				} else if (args[i].equals("-p")) {
					this.setPrimary(args[i + 1]);
				} else if (args[i].equals("-f")) {
					this.setFresh(true);
					--i;
				} else if (args[i].equals("-b")) {
					this.setReboot(true);
					--i;
				} else if (args[i].equals("-r")) {
					this.setReplicated(true);
					--i;
				} else if (args[i].equals("-Rip")) {
					this.setRip(checkIp(args[i + 1]));
				} else if (args[i].equals("-Rport")) {
					this.setRport(checkPort(Integer.valueOf(args[i + 1])));
				} else if (args[i].equals("-Rdir")) {
					this.setRdir(args[i + 1]);
				} else {
					throw new SildUndefinedArgException(args[i]);
				}
			}

		} catch (SildUndefinedArgException eu) {
			System.out.println("Undefined argument: \"" + eu.getMessage()
					+ "\" --> Abort.");
			System.exit(0);
		} catch (Exception e) {
			System.out.println("Bad input --> Abort.");
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void checkReboot() {

	}

	// Check if the replica setup is descend
	public void checkReplica() {
		try {
			if (this.getRdir() == null && this.isReplicated())
				throw new SildInvalidReplicaException();

			if (this.getRip() == null && isReplicated())
				throw new SildInvalidReplicaException();

			if (this.getRport() == 0 && isReplicated())
				throw new SildInvalidReplicaException();

			Path path = Paths.get(this.getRdir());

			// If directory does not exist, create one
			if (!Files.exists(path)) {
				File new_dir = new File(this.getDir());
				new_dir.mkdir();
			}
		} catch (SildInvalidReplicaException e) {
			System.out
					.println("Bad replica parameters, please check its IP, port, and directory --> Abort.");
			System.exit(0);
		} catch (InvalidPathException ep) {
			System.out.println("Invalid directory path --> Abort.");
			System.exit(0);
		}
	}

	public void checkFresh() {
		try {
			if (this.getPrimary() == null)
				throw new SildNoPrimaryConfFoundException();

			if (this.getDir() == null)
				throw new SildNoDirectorySpecifiedException();

			Path path = Paths.get(this.getDir());

			// If directory does not exist, create one
			if (!Files.exists(path)) {
				File new_dir = new File(this.getDir());
				new_dir.mkdir();
			}
		} catch (SildNoDirectorySpecifiedException en) {
			System.out
					.println("Please specify the file directory (-dir [directory]) --> Abort.");
			System.exit(0);
		} catch (SildNoPrimaryConfFoundException pe) {
			System.out.println("No primary.txt found --> Abort.");
			System.exit(0);
		} catch (InvalidPathException ep) {
			System.out.println("Invalid directory path --> Abort.");
			System.exit(0);
		}
	}

	public void checkPlain() {
		try {
			if (this.getDir() == null)
				throw new SildNoDirectorySpecifiedException();

			Path path = Paths.get(this.getDir());

			// If directory does not exist, create one
			if (!Files.exists(path)) {
				File new_dir = new File(this.getDir());
				new_dir.mkdir();
			}
		} catch (SildNoDirectorySpecifiedException en) {
			System.out
					.println("Please specify the file directory (-dir [directory]) --> Abort.");
			System.exit(0);
		} catch (InvalidPathException ep) {
			System.out.println("Invalid directory path --> Abort.");
			System.exit(0);
		}
	}

	public String checkIp(String ip) {
		String IPADDRESS_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
				+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
				+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
				+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

		if (ip.matches(IPADDRESS_PATTERN))
			return ip;
		else {
			System.out.println("Invalid Ip: " + ip + " --> Abort.");
			System.exit(0);
			return null;
		}
	}

	public int checkPort(int port) {
		if (port >= 1 && port <= 65535)
			return port;
		else {
			System.out.println("Invalid port: " + port + " --> Abort.");
			System.exit(0);
			return 0;
		}
	}

	public void printAll() {
		System.out.println("IP: " + this.getIp());
		System.out.println("Port: " + this.getPort());
		System.out.println("Directory: " + this.getDir());
	}

	// For unit testing
	public static void main(String[] args) {
		SildArgParser p = new SildArgParser();
		p.parse(args);
		p.printAll();
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getDir() {
		return dir;
	}

	public void setDir(String dir) {
		this.dir = dir;
	}

	public String getPrimary() {
		return primary;
	}

	public void setPrimary(String primary) {
		this.primary = primary;
	}

	public String getRip() {
		return Rip;
	}

	public void setRip(String rip) {
		Rip = rip;
	}

	public int getRport() {
		return Rport;
	}

	public void setRport(int rport) {
		Rport = rport;
	}

	public String getRdir() {
		return Rdir;
	}

	public void setRdir(String rdir) {
		Rdir = rdir;
	}

	public boolean isReplicated() {
		return isReplicated;
	}

	public void setReplicated(boolean isReplicated) {
		this.isReplicated = isReplicated;
	}

	public boolean isReboot() {
		return isReboot;
	}

	public void setReboot(boolean isReboot) {
		this.isReboot = isReboot;
	}

	public boolean isFresh() {
		return isFresh;
	}

	public void setFresh(boolean isFresh) {
		this.isFresh = isFresh;
	}
}
