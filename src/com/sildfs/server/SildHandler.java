package com.sildfs.server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sildfs.message.SildReq;
import com.sildfs.message.SildResp;
import com.sildfs.transaction.SildLog;
import com.sildfs.transaction.SildNewtxn;

/**
 * The client handler
 * 
 * @author dif
 */

public class SildHandler implements Runnable {

	private Socket socket;
	private BufferedReader reader;
	private PrintStream out;
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
			/**
			 * Initialize the buffered reader; Force it to read in ISO-8859-1
			 * 8-bit char ISO-8859-1 is known for preserver the binary stream
			 */
			reader = new BufferedReader(new InputStreamReader(
					socket.getInputStream(), "ISO-8859-1"));

			/**
			 * Initialize the output stream
			 */
			out = new PrintStream(this.getSocket().getOutputStream(), false);

			// Call receive
			while (this.receive())
				;

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean receive() throws IOException {
		SildReq req = new SildReq();
		try {
			// Read the header field
			while (!reader.ready())
				;
			req.parseHeader(reader.readLine());

			// Skip the blank line
			while (!reader.ready())
				;
			reader.readLine();

			// Read the data field; TODO check if content length is true
			while (!reader.ready())
				;
			char[] data = new char[req.getData_length()];
			int dataRead = reader.read(data);
			req.parseData(new String(data));
			// Skip the last \n
			while (!reader.ready())
				;
			reader.readLine();

			// If it is commit or abort message, skip one more
			if (req.getMethod().equals("COMMIT")
					|| req.getMethod().equals("ABORT")) {
				while (!reader.ready())
					;
				reader.readLine();
			}

		} catch (Exception e) {
			SildResp resp = new SildResp("ERROR", req.getTxn_id(),
					req.getSeq_num(), 204);
			out.print(resp.getMessage());
			e.printStackTrace();
		}

		String method = req.getMethod();
		// Execute according to the method
		if (method.equals("READ")) {
			this.read(req);
		} else if (method.equals("NEW_TXN")) {
			this.start_txn(req);
		} else if (method.equals("WRITE")) {

		} else if (method.equals("COMMIT")) {

		} else if (method.equals("ABORT")) {

		} else {
			System.out.println("Un-understandable method.");
		}

		return true;
	}

	public void read(SildReq req) {
		try {
			/**
			 * Read the given file and output to the client; throws file not
			 * found error Read the entire file; Should change to File.seperator
			 * here to provide compatibility
			 */
			Path path = Paths.get(this.getDir() + "/" + req.getData());
			byte[] byte_file = Files.readAllBytes(path);
			System.out.println(byte_file.length);
			// Here response is only the header
			SildResp resp = new SildResp("ACK", req.getTxn_id(),
					req.getSeq_num(), 0, byte_file.length);

			// Reply to the client
			out.print(resp.getMessage() + new String(byte_file));

		} catch (Exception e) {
			SildResp resp = new SildResp("ERROR", req.getTxn_id(),
					req.getSeq_num(), 206);
			out.print(resp.getMessage());
		}
	}

	public void start_txn(SildReq req) {
		// Generate a new transaction id
		int txn_id = SildLog.getAvail_txn_id();

		// Generate a SildEntry
		SildNewtxn new_txn_entry = new SildNewtxn(SildLog.getAvail_txn_id(),
				this.getDir(), req.getData(), out);
		new_txn_entry.execute();

	}

	public void write(SildReq req) {

	}

	public void commit(SildReq req) {

	}

	public void abort(SildReq req) {

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
