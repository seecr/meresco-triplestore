/* begin license *
 * 
 * The Meresco Owlim package consists out of a HTTP server written in Java that
 * provides access to an Owlim Triple store, as well as python bindings to
 * communicate as a client with the server. 
 * 
 * Copyright (C) 2012 Seecr (Seek You Too B.V.) http://seecr.nl
 * 
 * This file is part of "Meresco Owlim"
 * 
 * "Meresco Owlim" is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * "Meresco Owlim" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with "Meresco Owlim"; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * end license */

package org.meresco.owlimhttpserver;

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
