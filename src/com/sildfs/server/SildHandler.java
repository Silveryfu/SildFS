package com.sildfs.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import com.sildfs.message.SildReq;
import com.sildfs.message.*;


/**
 * The client handler
 * 
 * @author dif
 * @Date: Oct.14 2014
 */

public class SildHandler implements Runnable {

	private Socket socket;
	private BufferedReader reader;

	public SildHandler(Socket socket) {
		this.setSocket(socket);
	}

	protected SildHandler() {
	};

	public void run() {
		System.out.println("Start handling client: "
				+ this.getSocket().getInetAddress() + ":"
				+ this.getSocket().getPort());
		try {
			// Initialize the buffered reader
			reader = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));

			// Call receive; call respond
			if (this.receive())
				this.respond();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean receive() throws IOException {
		// Read the header field
		SildReq req = new SildReq();
		req.parseHeader(reader.readLine());
		req.printAll();
		
		String method = req.getMethod();
		if (method.equals("READ")) {
			

		} else if (method.equals("NEW_TXN")) {

		} else if (method.equals("WRITE")) {

		} else if (method.equals("COMMIT")) {

		} else if (method.equals("ABORT")) {

		} else {
			System.out.println("Un-understandable method.");
		}

		return true;
	}

	public void respond() throws IOException {
		System.out.println("Responding to " + this.getSocket().getInetAddress()
				+ ":" + this.getSocket().getPort());
	}

	public void read() throws IOException {

	}

	public void start_txn() throws IOException {

	}

	public void write() throws IOException {

	}

	public void commit() throws IOException {

	}

	public void abort() throws IOException {

	}

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}
}
