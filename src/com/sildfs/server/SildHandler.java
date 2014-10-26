package com.sildfs.server;

import java.net.Socket;
import com.sildfs.tools.*;

/**
 * The client handler
 * 
 * @author dif
 * @Date: Oct.14 2014
 */

public class SildHandler implements Runnable {

	private Socket socket;

	public SildHandler(Socket socket) {
		this.setSocket(socket);
	}

	protected SildHandler() {
	};

	public void run() {
		System.out.println("Start handling client: "
				+ this.getSocket().getInetAddress()+this.getSocket().getPort());
		

	
		
		
	}

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}
}