package com.sildfs.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sildfs.message.SildReq;
import com.sildfs.message.SildResp;
import com.sildfs.transaction.SildAbstractEntry;
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

		/**
		 * Execute according to the method; The error handling should be done in
		 * each of these methods below
		 */
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

			// Here response is only the header
			SildResp resp = new SildResp("ACK", -1, -1, 0, byte_file.length);

			// Reply to the client; header + content
			out.print(resp.getMessage() + new String(byte_file));

		} catch (Exception e) {
			SildResp resp = new SildResp("ERROR", -1, -1, 206);
			out.print(resp.getMessage());
		}
	}

	public void start_txn(SildReq req) {
		int txn_id = SildLog.getAvail_txn_id();
		try {
			// Generate a new transaction id

			// Generate a SildEntry
			SildNewtxn new_txn_entry = new SildNewtxn(txn_id, this.getDir(),
					req.getData());

			// Create a hidden folder for entry-logs
			(new File(this.getDir() + "/." + txn_id)).mkdirs();
			
			// Serialize and store this entry, flush it to disk
			recordEntry(txn_id, new_txn_entry);

			// Respond to the client
			SildResp resp = new SildResp("ACK", txn_id, 0);
			out.print(resp.getMessage());
		} catch (Exception e) {
			SildResp resp = new SildResp("ERROR", txn_id, req.getSeq_num(), 205);
			out.print(resp.getMessage());
			e.printStackTrace();
		}
	}

	public void write(SildReq req) {

	}

	public void commit(SildReq req) {

	}

	public void abort(SildReq req) {

	}

	public void recordEntry(int txn_id, SildAbstractEntry e) throws Exception {
		// Create the object storing file
		FileOutputStream fos = new FileOutputStream(this.getDir() + "/."
				+ txn_id + "/" + e.getSeq_num());
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(e);

		// Flush to disk
		oos.flush();
		fos.flush();
		fos.getFD().sync();

		FileInputStream fis = new FileInputStream(this.getDir() + "/." + txn_id
				+ "/" + e.getSeq_num());
		ObjectInputStream ois = new ObjectInputStream(fis);

		SildNewtxn s = (SildNewtxn) ois.readObject();

		System.out.println("here");
		s.execute();
		
		oos.close();
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
