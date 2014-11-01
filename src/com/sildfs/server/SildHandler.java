package com.sildfs.server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import com.sildfs.message.SildReq;

/**
 * The client handler
 * 
 * @author dif
 */

public class SildHandler implements Runnable {

	private Socket socket;
	private BufferedReader reader;
	private String dir;

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

			// Call receive
			while (this.receive())
				;

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean receive() throws IOException {
		SildReq req = new SildReq();

		// Read the header field
		req.parseHeader(reader.readLine());

		// Skip the blank line
		reader.readLine();

		// Read the data field
		req.parseData(reader.readLine());

		req.printAll();
		// Execute according to the method
		String method = req.getMethod();
		if (method.equals("READ")) {
			this.read(req.getData());
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

	public void read(String file_name) {
		try {
			RandomAccessFile read_file = new RandomAccessFile(this.getDir()
					+ "/" + file_name, "r");
			FileChannel inChannel = read_file.getChannel();
			PrintStream out = new PrintStream(this.getSocket()
					.getOutputStream(), true);

			// Another candidate is to use MappedByteBuffer
			int BUFFER_SIZE = 1024;
			ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);
			byte[] byte_array;

			while (inChannel.read(buf) > 0) {
				buf.flip();
				byte_array = new byte[buf.limit()];
				buf.get(byte_array);
				out.write(byte_array);
				buf.clear();
			}

			out.print("\r\n\r\n");

			inChannel.close();
			read_file.close();

		} catch (FileNotFoundException fe) {
			System.out.println("File not found.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void start_txn() {

	}

	public void write() {

	}

	public void commit() {

	}

	public void abort() {

	}

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public String getDir() {
		return dir;
	}

	public void setDir(String dir) {
		this.dir = dir;
	}
}
