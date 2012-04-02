package org.meresco.owlimhttpserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public class BufferedLineReader {

	Reader reader;
	String dataBuffer = "";
	
	final static int BUFFER_SIZE = 4096;
	
	public BufferedLineReader(Reader reader) {
		this.reader = reader;
	}
	
	public String readLine() throws IOException {
		String newLine;
		
		while (true) {
			if ((newLine = this.readLineFromBuffer()) != null) {
				return newLine;
			}

			char[] buffer = new char[BUFFER_SIZE];
			int length = 0;
			if ((length = this.reader.read(buffer)) == -1) {
				String result = this.dataBuffer;
				this.dataBuffer = null;
				return result;
			}
			this.dataBuffer += String.valueOf(buffer, 0, length);
		}		
	}
	
	private String readLineFromBuffer() {
		int position;
		if (this.dataBuffer != null && (position = this.dataBuffer.indexOf("\n")) != -1) {
			String result = this.dataBuffer.substring(0, position + 1);
			this.dataBuffer = this.dataBuffer.substring(position + 1, this.dataBuffer.length());
			return result;
		}
		return null;
	}

	public void close() throws IOException {
		this.reader.close();
	}
}
