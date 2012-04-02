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
