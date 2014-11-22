package com.sildfs.tool;

/**
 * This class defines a reader which reads primary server configure info
 * from the configuration file primary.txt
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

public class SildConfReader {

	private String ip;
	private int port;

	public void read(String file) {
		try {
			File f = new File(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(f)));
			this.setIp(checkIp(br.readLine()));
			this.setPort(checkPort(Integer.valueOf(br.readLine())));
		} catch (FileNotFoundException fe) {
			System.out.println("primary.txt not found --> Abort.");
			System.exit(0);
		} catch (NumberFormatException ne) {
			System.out.println("Invalid port number --> Abort.");
			System.exit(0);
		} catch (Exception e) {
			System.out.println("Unknown error reading primary.txt --> Abort.");
			e.printStackTrace();
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

	public static void main(String[] args) {
		SildConfReader cr = new SildConfReader();
		cr.read("./primary.txt");
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
}