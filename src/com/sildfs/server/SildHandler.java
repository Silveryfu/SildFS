package com.sildfs.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.sildfs.message.SildReq;
import com.sildfs.message.SildResp;
import com.sildfs.transaction.SildAbstractEntry;
import com.sildfs.transaction.SildData;
import com.sildfs.transaction.SildLog;
import com.sildfs.transaction.SildNewtxn;
import com.sildfs.transaction.SildTxn;

/**
 * The client handler; Handles client's new_txn, commit, abort, write, read as
 * well as replica's initial request message
 * 
 * @author dif
 */

public class SildHandler implements Runnable {

	private Socket socket;
	private BufferedReader reader;
	private PrintStream out;
	private String dir;
	private SildRecoveryAgent recov_agent;
	private static SildPrimaryAgent primary_agent;

	private static final int SOCKET_TIMEOUT = 120000;

	// Every time a new file is created, a corresponding lock is generated
	private static ConcurrentHashMap<String, ReadWriteLock> file_locks;

	private static ConcurrentHashMap<Integer, SildTxn> txn_list;

	private static ConcurrentHashMap<Integer, Boolean> committed_txn;

	public static Object rep_lock;

	public static AtomicBoolean isRepFinish, isReplicated;

	// Initialize the static fields
	static {
		// Initialize the transaction id to file name map
		txn_list = new ConcurrentHashMap<Integer, SildTxn>();

		// Initialize the lock map
		file_locks = new ConcurrentHashMap<String, ReadWriteLock>();

		// Initialize committed history
		committed_txn = new ConcurrentHashMap<Integer, Boolean>();

		// A replication lock
		rep_lock = new Object();

		// Boolean to flag if the replication finished
		isRepFinish = new AtomicBoolean(true);

		// Boolean to flag if there is a replica
		isReplicated = new AtomicBoolean(false);
	}

	public SildHandler(Socket socket) {
		this.setSocket(socket);
	}

	protected SildHandler() {
	};

	public void run() {
		System.out.println("--P-- Start handling client: "
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

			// Set a socket timeout
			this.getSocket().setSoTimeout(SOCKET_TIMEOUT);

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
			req.parseHeader(reader.readLine());

			// Skip the blank line
			reader.readLine();

			// Read the data field;
			char[] data = new char[req.getData_length()];
			if (reader.read(data) != req.getData_length())
				throw new Exception();

			req.parseData(new String(data));

			// Skip the last
			reader.readLine();

			// If it is commit or abort message, skip one more
			if (req.getMethod().equals("COMMIT")
					|| req.getMethod().equals("ABORT")) {
				reader.readLine();
			}

		} catch (SocketTimeoutException se) {
			this.getSocket().close();
			System.out.println("--P-- Time out on: "
					+ this.getSocket().getPort());
			return false;
		} catch (Exception e) {
			SildResp resp = new SildResp("ERROR", req.getTxn_id(),
					req.getSeq_num(), 204);
			out.print(resp.getMessage());
			return false;
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
			this.abort(req);
		} else if (method.equals("NEW_REPLICA")) {
			this.new_replica(req);
		} else {
			System.out.println("Un-understandable method.");
		}

		return false;
	}

	public void read(SildReq req) {
		try {
			/**
			 * Read the given file and output to the client; throws file not
			 * found error Read the entire file; Should change to File.seperator
			 * here to provide compatibility
			 */
			String file_name = req.getData();
			ReadWriteLock rwlock = file_locks.get(file_name);
			if (rwlock == null) {
				File f = new File(this.getDir() + "/" + file_name);
				if (f.exists() && !f.isDirectory())
					file_locks.put(file_name, new ReentrantReadWriteLock());
				else
					throw new Exception();
			}

			rwlock = file_locks.get(file_name);

			Lock rlock = rwlock.readLock();

			rlock.lock();
			Path path = Paths.get(this.getDir() + "/" + req.getData());
			byte[] byte_file = Files.readAllBytes(path);
			rlock.unlock();

			// Here response is only the header
			SildResp resp = new SildResp("ACK", -1, -1, 0, byte_file.length);

			// Reply to the client; header + content
			out.print(resp.getMessage() + new String(byte_file));
		} catch (Exception e) {
			try {
				SildResp resp = new SildResp("ERROR", -1, -1, 206);
				out.print(resp.getMessage());
			} catch (Exception e2) {
				e2.printStackTrace();
			}
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

		if (committed_txn.get(txn_id) != null) {
			// Send ACK to the client
			SildResp resp = new SildResp("ERROR", txn_id, -1, 202);
			out.print(resp.getMessage());
			return;
		}

		try {
			// Check if the transaction id is legal
			File f = new File(this.getDir() + "/.TXN/.txn" + txn_id);
			if (!f.exists() || !f.isDirectory()) {
				SildResp resp = new SildResp("ERROR", txn_id, seq_num, 201);
				out.print(resp.getMessage());
				return;
			}

			// Check if the seq_num is legal
			f = new File(this.getDir() + "/.TXN/.txn" + txn_id + "/" + seq_num);
			if (seq_num <= 0 || f.exists()) {
				SildResp resp = new SildResp("ERROR", txn_id, seq_num, 207);
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

			// Check if there is an old committed, try commit again
			if (txn_list.get(txn_id).isCommitted()) {
				executeOldCommit(txn_id);
			}
			;

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
			// Check if it is already successfully committed
			if (committed_txn.get(txn_id) != null) {
				// Send ACK to the client
				SildResp resp = new SildResp("ACK", txn_id, -1);
				out.print(resp.getMessage());
				return;
			}

			// Check if the transaction id is legal
			File f = new File(txn_log);
			if (!f.exists() || !f.isDirectory()) {
				SildResp resp = new SildResp("ERROR", txn_id, seq_num, 201);
				out.print(resp.getMessage());
				return;
			}

			// Check if there is an old commit
			if (txn_list.get(txn_id).isCommitted()) {
				SildReq sildreq = txn_list.get(txn_id).getOld_commit();
				if (seq_num != sildreq.getSeq_num()) {
					SildResp resp = new SildResp("ERROR", txn_id, seq_num, 207);
					out.print(resp.getMessage());
					return;
				}
			}

			// Check if there are missing packets which have lower sequence
			// numbers than the largest existing one; If there are, ask for
			// client re-send
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

			// Check if the sequence number is legal
			if (seq_num < given_seq) {
				SildResp resp = new SildResp("ERROR", txn_id, seq_num, 207);
				out.print(resp.getMessage());
				return;
			}

			// Check if there are missing packets which have greater sequence
			// numbers
			for (; counter <= seq_num; counter++) {
				SildResp resp = new SildResp("ASK_RESEND", txn_id, counter);
				out.print(resp.getMessage());
				isMissing = true;
			}

			SildTxn txn = txn_list.get(txn_id);
			txn.setCommitted(true);
			txn.setOld_commit(req);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// If there is some packets missing, should abort committing
		if (isMissing)
			return;

		/* Commit this transaction */
		try {
			SildTxn txn = txn_list.get(txn_id);

			// Execute the new transaction call
			SildNewtxn new_txn = txn.getNew_txn();

			// If the replication for committed transaction has not yet
			// finished, wait until it does
			synchronized (rep_lock) {
				while (!isRepFinish.get()) {
					try {
						System.out
								.println("--P-- Awaiting for replication completion.");
						rep_lock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			new_txn.execute();

			String file_name = new_txn.getFile();

			// If this is a new file, create a lock on it
			if (new_txn.isIsnewfile())
				file_locks.put(file_name, new ReentrantReadWriteLock());

			HashMap<Integer, SildData> data_list = txn.getData_list();

			// Get the update text data
			StringBuilder text = new StringBuilder();
			for (int i = 1; i <= given_seq; i++) {
				text.append(data_list.get(i).getData());
			}

			// Lock the target file
			Lock flock = file_locks.get(file_name).writeLock();
			flock.lock();

			if (committed_txn.get(txn_id) != null) {
				flock.unlock();
				// Send ACK to the client
				SildResp resp = new SildResp("ACK", txn_id, -1);
				out.print(resp.getMessage());
				return;
			}

			// Update the file; flush to disk
			File f = new_txn.getF();
			FileOutputStream fos = new FileOutputStream(f, true);
			fos.write(text.toString().getBytes());
			fos.flush();
			fos.getFD().sync();
			fos.getFD().sync();
			fos.close();

			// Release the lock;
			flock.unlock();

			// Set the committed list
			committed_txn.put(txn_id, true);

			// Mark this log as committed
			File commit_mark = new File(txn_log + "/C");
			commit_mark.createNewFile();

			// Mark the commit order
			File commit_order = new File(txn_log + "/O"
					+ committed_txn.keySet().size());
			commit_order.createNewFile();

			if (isReplicated.get()) {
				primary_agent.replicate_live(txn_id);
			}

			// Send ACK to the client
			SildResp resp = new SildResp("ACK", txn_id, -1);
			out.print(resp.getMessage());

		} catch (Exception e) {
			SildResp resp = new SildResp("ERROR", txn_id, seq_num, 205);
			out.print(resp.getMessage());
			e.printStackTrace();
		}

		// try {
		// clear_log(txn_id);
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
	}

	public void abort(SildReq req) {
		int txn_id = req.getTxn_id();
		try {
			clear_log(txn_id);
			SildResp resp = new SildResp("ACK", txn_id, -1);
			out.print(resp.getMessage());
		} catch (Exception e) {
			SildResp resp = new SildResp("ERROR", txn_id, -1, 205);
			out.print(resp.getMessage());
		}
	}

	public void new_replica(SildReq req) {
		String[] ip_port = req.getData().split(" ");

		primary_agent = new SildPrimaryAgent();
		primary_agent.setReplica_ip(ip_port[0]);
		primary_agent.setReplica_port(Integer.valueOf(ip_port[1]));
		primary_agent.setLog_base(this.getDir() + "/.TXN/");
		isReplicated.set(true);
		primary_agent.foo();
		try {
			// Give a time window for the replica start the listen port
			Thread.sleep(50);
			primary_agent.replicate_committed();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void clear_log(int txn_id) throws Exception {
		// Clear in memory
		txn_list.remove(txn_id);

		// Clear on disk
		File f = new File(this.getDir() + "/.TXN/.txn" + txn_id);
		deleteDirectory(f);
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
		fos.getFD().sync();
		oos.close();
	}

	/**
	 * Record write command including operations; heavy weight.
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

	public void deleteDirectory(File path) throws Exception {
		File[] sub = path.listFiles();
		for (File file : sub) {
			if (file.isDirectory()) {
				deleteDirectory(file);
				file.delete();
			} else {
				file.delete();
			}
		}
		path.delete();
	}

	public void executeOldCommit(int txn_id) {
		commit(txn_list.get(txn_id).getOld_commit());
	}

	public static ConcurrentHashMap<String, ReadWriteLock> getFile_locks() {
		return file_locks;
	}

	public static void setFile_locks(
			ConcurrentHashMap<String, ReadWriteLock> file_locks) {
		SildHandler.file_locks = file_locks;
	}

	public static ConcurrentHashMap<Integer, SildTxn> getTxn_list() {
		return txn_list;
	}

	public static void setTxn_list(ConcurrentHashMap<Integer, SildTxn> txn_list) {
		SildHandler.txn_list = txn_list;
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

	public SildRecoveryAgent getAgent() {
		return recov_agent;
	}

	public void setAgent(SildRecoveryAgent agent) {
		this.recov_agent = agent;
	}

	public static ConcurrentHashMap<Integer, Boolean> getCommitted_txn() {
		return committed_txn;
	}

	public static void setCommitted_txn(
			ConcurrentHashMap<Integer, Boolean> committed_txn) {
		SildHandler.committed_txn = committed_txn;
	}
}
