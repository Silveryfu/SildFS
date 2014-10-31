package com.sildfs.tools;

public class SildArgParser {
	private String ip;
	private int port;
	private String dir;

	SildArgParser() {

	}

	public void parse(String[] args) {
		for (int i = 0; i < args.length; i = i + 2) {
			if (args[i].equals("-ip")) {

			} else if (args[i].equals("-port")) {

			} else if (args[i].equals("-dir")) {

			} else {
				System.out.println("Un-understandable argument: " + args[i]);
				System.out.println("Sorry, I'll exit.");
				System.exit(0);
			}
		}
	}
	
	public static void main(String[] args) {
		SildArgParser p = new SildArgParser();
		System.out.println(p.getDir());
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
}
