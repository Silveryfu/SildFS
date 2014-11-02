package com.sildfs.tool;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Defines a data reader, reading bytes with unlimited buffer size; A converter
 * can convert bytebuffer content to a output string
 * 
 * @author dif
 */

public class SildDataReader {

	private static final int DEFAULT_BUFFER_SIZE = 1024;
	private List<ByteBuffer> buffers;
	private int buffer_size;
	private Socket socket;

	protected SildDataReader() {
	}

	public SildDataReader(Socket socket) {
		this.setSocket(socket);
		this.setBuffer_size(DEFAULT_BUFFER_SIZE);
	}

	public SildDataReader(Socket socket, int buffer_size) {
		this.setSocket(socket);
		this.setBuffer_size(buffer_size);
	}

	public void readData() {
		try{
			buffers.clear();
			InputStream in = socket.getInputStream();
			int k = 0;
			byte r = 0;
			int bytesRead = 0;
			while((bytesRead = in.read()) > 0) {
				
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
/*
			while (true) {
				ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
				for (k = 0; k < BUFFER_SIZE; k++) {
					r = (byte) in.read();
					if (r != (byte) 0xFF)
						buffer.put(r); // 125 for now it ends with '}'//
										// 255=0xFF
					else {
						buffer.put(r); // now we have a complete package, ended
										// with EOT
						buffer.flip();
						buffers.add(buffer);
						return k; // if k=0 here, then it is most possibly the
									// end of input stream
					}
				}
			}
		}

		public String getAsString() throws UnsupportedEncodingException {
			StringBuffer str = new StringBuffer();
			for (ByteBuffer buffer : buffers) {
				str.append(new String(buffer.array(), 0, buffer.limit(),
						"ISO-8859-1"));
			}
			return str.toString();
		}
	}
*/
	public static void main(String[] args) {

	}

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public List<ByteBuffer> getBuffers() {
		return buffers;
	}

	public void setBuffers(List<ByteBuffer> buffers) {
		this.buffers = buffers;
	}

	public int getBuffer_size() {
		return buffer_size;
	}

	public void setBuffer_size(int buffer_size) {
		this.buffer_size = buffer_size;
	}
}
