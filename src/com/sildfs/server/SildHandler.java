package com.sildfs.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.sildfs.message.SildReq;
import com.sildfs.message.SildResp;
import com.sildfs.transaction.SildAbstractEntry;
import com.sildfs.transaction.SildData;
import com.sildfs.transaction.SildLog;
import com.sildfs.transaction.SildNewtxn;
import com.sildfs.transaction.SildTxn;

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

	// Every time a new file is created, a corresponding lock is generated
	private static ConcurrentHashMap<String, ReentrantLock> file_locks;
	
	private static ConcurrentHashMap<Integer, SildTxn> txn_list;

	// Initialize the static fiedls
	static {
		// Initialize the transaction id to file name map
		txn_list = new ConcurrentHashMap<Integer, SildTxn>();
		
		// Initialize the lock map
		file_locks = new ConcurrentHashMap<String, ReentrantLock>();
	}
	
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

			// Read the data field;
			while (!reader.ready())
				;
			char[] data = new char[req.getData_length()];
			if (reader.read(data) != req.getData_length())
				throw new Exception();

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
			this.write(req);
		} else if (method.equals("COMMIT")) {
			this.commit(req);
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
			SildResp resp = new SildResp("ACK", -1, -1 , 0, byte_file.length);

			// Reply to the client; header + content
			out.print(resp.getMessage() + new String(byte_file));

		} catch (Exception e) {
			SildResp resp = new SildResp("ERROR", -1, -1, 206);
			out.print(resp.getMessage());
		}
	}

	public void start_txn(SildReq req) {
		// Generate a new transaction id
		int txn_id = SildLog.getAvail_txn_id();
		try {
			// Create a hidden folder for entry-logs
			(new File(this.getDir() + "/.TXN/.txn" + txn_id)).mkdirs();

			
			// Generate a SildEntry
			SildNewtxn new_txn_entry = new SildNewtxn(txn_id, this.getDir(),
					req.getData());

			// Create a transaction in memory
			SildTxn txn = new SildTxn(txn_id, new_txn_entry);
			
			// Store it in the txn_list
			txn_list.put(txn_id, txn);

			// Serialize and store this entry, flush it to disk
			recordEntry(txn_id, new_txn_entry);

			// Respond to the client
			SildResp resp = new SildResp("ACK", txn_id, 0);
			out.print(resp.getMessage());
		} catch (Exception e) {
			SildResp resp = new SildResp("ERROR", txn_id, 0, 205);
			out.print(resp.getMessage());
			e.printStackTrace();
		}
	}

	public void write(SildReq req) {
		// Cache the txn_id, seq_num, data
		int txn_id = req.getTxn_id();
		int seq_num = req.getSeq_num();
		String data = req.getData();

		try {
			// Check if the transaction id is legal
			File f = new File(this.getDir() + "/.TXN/.txn" + txn_id);
			if (!f.exists() || !f.isDirectory()) {
				SildResp resp = new SildResp("ERROR", txn_id, seq_num, 201);
				out.print(resp.getMessage());
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			// If it is a legal id, generate a SildEntry
			// SildWrite write = new SildWrite(txn_id, seq_num, this.getDir(),
			// txn_file.get(txn_id), req.getData());
			//
			// // Serialize and store this entry, flush it to disk
			// recordEntry(txn_id, write);
			SildData sild_data = new SildData(data);
			
			// Store it in memory
			txn_list.get(txn_id).addData(seq_num, sild_data);
			
			recordData(txn_id, seq_num, sild_data);
		} catch (Exception e) {
			SildResp resp = new SildResp("ERROR", txn_id, seq_num, 205);
			out.print(resp.getMessage());
			e.printStackTrace();
		}
	}

	public void commit(SildReq req) {
		// Cache the txn_id, seq_num, txn_log folder
		int txn_id = req.getTxn_id();
		int seq_num = req.getSeq_num();
		String txn_log = this.getDir() + "/.TXN/.txn" + txn_id;
		boolean isMissing = false;
		int counter = 0;
		int given_seq = 0;

		try {
			// Check if the transaction id is legal
			File f = new File(txn_log);
			if (!f.exists() || !f.isDirectory()) {
				SildResp resp = new SildResp("ERROR", txn_id, seq_num, 201);
				out.print(resp.getMessage());
				return;
			}

			// Check if there are missing packets; If there are, ask for client
			// re-send
			File[] listOfFiles = f.listFiles();
			Arrays.sort(listOfFiles);
			for (int i = 0; i < listOfFiles.length; i++) {
				given_seq = Integer.valueOf(listOfFiles[i].getName());
				if (given_seq == counter) {
					// If this packet exists
					counter++;
				} else {
					isMissing = true;
					// Ask re-send the missing packets
					while (counter < given_seq) {
						SildResp resp = new SildResp("ASK_RESEND", txn_id,
								counter);
						out.print(resp.getMessage());
						counter++;
					}
					// At the end of while loop, counter is equal to given_seq
					counter++;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// If there is some packets missing, should abort committing
		if (isMissing)
			return;
		try {
			// Commit this transaction
			SildTxn txn = txn_list.get(txn_id);
			
			// Execute the new transaction call
			SildNewtxn new_txn = txn.getNew_txn();
			new_txn.execute();
			
			HashMap<Integer, SildData> data_list = txn.getData_list();
			
			// Get the update text data
			StringBuilder text = new StringBuilder();
			for (int i = 1; i <= given_seq; i++) {
				text.append(data_list.get(i).getData());
			}

			// Update the file; flush to disk
			File f = new_txn.getF();
			FileOutputStream fos = new FileOutputStream(f, true);
			fos.write(text.toString().getBytes());
			fos.flush();
			fos.getFD().sync();
			fos.close();
			
			// Send ACK to the client
			SildResp resp = new SildResp("ACK", txn_id, seq_num);
			out.print(resp.getMessage());
		} catch (Exception e) {
			SildResp resp = new SildResp("ERROR", txn_id, seq_num, 205);
			out.print(resp.getMessage());
			e.printStackTrace();
		}
	}

	public void abort(SildReq req) {

	}

	/**
	 * A more light-weight write log
	 * 
	 * @param txn_id
	 * @param seq_num
	 * @param data
	 * @throws Exception
	 */
	public void recordData(int txn_id, int seq_num, SildData data)
			throws Exception {
		// Create the object storing file
		File f = new File(this.getDir() + "/.TXN/.txn" + txn_id + "/" + seq_num);
		f.createNewFile();
		FileOutputStream fos = new FileOutputStream(f);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(data);

		// Flush to disk
		oos.flush();
		fos.flush();
		fos.getFD().sync();
		oos.close();
	}

	/**
	 * Record write command including operations; heavey weight.
	 * 
	 * @param txn_id
	 * @param e
	 * @throws Exception
	 */
	public void recordEntry(int txn_id, SildAbstractEntry e) throws Exception {
		// Create the object storing file
		FileOutputStream fos = new FileOutputStream(this.getDir()
				+ "/.TXN/.txn" + txn_id + "/" + e.getSeq_num());
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(e);

		// Flush to disk
		oos.flush();
		fos.flush();
		fos.getFD().sync();

		// FileInputStream fis = new FileInputStream(this.getDir() + "/." +
		// txn_id
		// + "/" + e.getSeq_num());
		// ObjectInputStream ois = new ObjectInputStream(fis);
		//
		// SildNewtxn s = (SildNewtxn) ois.readObject();
		//
		// System.out.println("here");
		// s.execute();

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
