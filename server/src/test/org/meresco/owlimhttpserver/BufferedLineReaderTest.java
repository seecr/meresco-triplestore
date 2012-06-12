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

import static org.junit.Assert.*;
import static org.meresco.owlimhttpserver.Utils.createTempDirectory;
import static org.meresco.owlimhttpserver.Utils.deleteDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class BufferedLineReaderTest {
	File tempdir;
	
    @Before
    public void setUp() throws Exception {
        tempdir = createTempDirectory();
    }

    @After
    public void tearDown() throws Exception {
        deleteDirectory(tempdir);
    }
    
	@Test
	public void testReadLines() throws IOException {
		BufferedReader br = new BufferedReader(new StringReader("line1\nline2\nline3"));
		BufferedLineReader blr = new BufferedLineReader(br);
		assertEquals("line1\n", blr.readLine());
		assertEquals("line2\n", blr.readLine());
		assertEquals("line3", blr.readLine());
		assertEquals(null, blr.readLine());
	}
	
	@Test
	public void testReadOneLine() throws IOException {
		BufferedReader br = new BufferedReader(new StringReader("line1"));
		BufferedLineReader blr = new BufferedLineReader(br);
		assertEquals("line1", blr.readLine());
		assertEquals(null, blr.readLine());
	}
	
	@Test
	public void testReadNoLines() throws IOException {
		BufferedReader br = new BufferedReader(new StringReader(""));
		BufferedLineReader blr = new BufferedLineReader(br);
		assertEquals("", blr.readLine());
		assertEquals(null, blr.readLine());
	}	
	
	@Test
	public void testReadLinesCarriageReturnLineFeed() throws IOException {
		BufferedReader br = new BufferedReader(new StringReader("line1\r\nline2"));
		BufferedLineReader blr = new BufferedLineReader(br);
		assertEquals("line1\r\n", blr.readLine());
		assertEquals("line2", blr.readLine());
		assertEquals(null, blr.readLine());
	}
}
