/* begin license *
 * 
 * The Meresco Owlim package consists out of a HTTP server written in Java that
 * provides access to an Owlim Triple store, as well as python bindings to
 * communicate as a client with the server. 
 * 
 * Copyright (C) 2011-2012 Seecr (Seek You Too B.V.) http://seecr.nl
 * Copyright (C) 2011 Seek You Too B.V. (CQ2) http://www.cq2.nl
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

import java.util.List;
import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.RuntimeException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import static org.junit.Assert.*;

import static org.meresco.owlimhttpserver.Utils.createTempDirectory;
import static org.meresco.owlimhttpserver.Utils.deleteDirectory;

public class TransactionLogTest {
    TransactionLog transactionLog;
    File tempdir;
    TSMock tsMock;
    PrintStream orig_stdout;
    OutputStream stdout;

    @Before
    public void setUp() throws Exception {
    	tempdir = createTempDirectory();
    	stdout = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(stdout);
        orig_stdout = System.out;
        System.setOut(ps);
        setTransactionLog();
    }

    @After
    public void tearDown() throws Exception {
    	System.setOut(orig_stdout);
        deleteDirectory(tempdir);
    }

    public void setTransactionLog(double maxSizeInMb) throws Exception {
        tsMock = new TSMock();
        transactionLog = new TransactionLog(tsMock, tempdir, maxSizeInMb);
        transactionLog.init();
    }
        
    public void setTransactionLog() throws Exception {
        tsMock = new TSMock();
        transactionLog = new TransactionLog(tsMock, tempdir);
        transactionLog.init();
    }

    @Test
    public void testAddToTransactionLog() throws Exception {
        String filename = String.valueOf(System.currentTimeMillis());
        transactionLog.prepare("addRDF", "testRecord", filename, "<x>ignored</x>");
        assertTrue(new File(transactionLog.getTempLogDir().toString(), filename).isFile());
    }

    @Test
    public void testPrepareWithSameFilename() throws Exception {
        String filename = String.valueOf(System.currentTimeMillis());
        transactionLog.prepare("addRDF", "testRecord", filename, "<x>ignored</x>");
        transactionLog.prepare("addRDF", "testRecord", filename, "<x>ignored</x>");
        assertEquals(2, this.transactionLog.getTempLogDir().list().length);
    }

    @Test
    public void testCommitToTransactionLog() throws FileNotFoundException, Exception {
        String filename = String.valueOf(transactionLog.getTime());
        String xml = "<x>ignoréd</x>";
        transactionLog.prepare("addRDF", "testRecord", filename, xml);
        transactionLog.commit(filename);
        ArrayList<String> files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.size());
        assertEquals("current", files.get(0));
        String expectedXml = "<transaction_item>" +
                "<action>addRDF</action>" +
                "<identifier>testRecord</identifier>" + 
                "<filedata>&lt;x&gt;ignoréd&lt;/x&gt;</filedata>" +
            "</transaction_item>\n";
        assertEquals(expectedXml, Utils.read(new File(transactionLog.getTransactionLogDir(), files.get(0))));
    }

    @Test
    public void testCheckIsAddedToLogWhenExists() throws Exception {
        String filenameTransactionLog;
        String filename = String.valueOf(System.currentTimeMillis());
        filenameTransactionLog = transactionLog.prepare("addRDF", "testRecord", filename, "<x>ignored1</x>");
        assertEquals(filename, filenameTransactionLog);
        filenameTransactionLog = transactionLog.prepare("addRDF", "testRecord", filename, "<x>ignored2</x>");
        assertEquals(filename + "_1", filenameTransactionLog);
        filenameTransactionLog = transactionLog.prepare("addRDF", "testRecord", filename, "<x>ignored3</x>");
        assertEquals(filename + "_1_1", filenameTransactionLog);
        transactionLog.commit(filename);
        ArrayList<String> files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.size());
        assertEquals("current", files.get(0));
        assertEquals("<transaction_item>" +
        		"<action>addRDF</action>" +
        		"<identifier>testRecord</identifier>" +
        		"<filedata>&lt;x&gt;ignored1&lt;/x&gt;</filedata>" +
        	"</transaction_item>\n", Utils.read(new File(transactionLog.getTransactionLogDir(), files.get(0))));
    }

    @Test 
    public void testCommitWithExistingName() throws Exception {
        String filename = String.valueOf(System.currentTimeMillis());
        transactionLog.prepare("addRDF", "testRecord", filename, "<x>ignored</x>");
        transactionLog.commit(filename);
        transactionLog.prepare("addRDF", "testRecord", filename, "<x>ignored</x>");
        transactionLog.commit(filename);
        transactionLog.prepare("addRDF", "testRecord", filename, "<x>ignored</x>");
        transactionLog.commit(filename);
        ArrayList<String> files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.size());
        assertEquals("current", files.get(0));
        assertEquals(3, countTransactionItems(files.get(0)));
    }

    @Test
    public void testEscapeIdentifier() throws FileNotFoundException, Exception {
        String filename = String.valueOf(System.currentTimeMillis());
        transactionLog.prepare("addRDF", "<testRecord>", filename, "<x>ignored</x>");
        String expectedXml = "<transaction_item>" +
                "<action>addRDF</action>" +
                "<identifier>&lt;testRecord&gt;</identifier>" + 
                "<filedata>&lt;x&gt;ignored&lt;/x&gt;</filedata>" +
            "</transaction_item>\n";
        FileInputStream fileInputStream = new FileInputStream(new File(transactionLog.getTempLogDir(), filename));
        assertEquals(expectedXml, Utils.read(fileInputStream));
    }

    @Test
    public void testAddSuccesfullRecord() throws FileNotFoundException, TransactionLogException, IOException {
    	assertTrue(transactionLog.committedFilePath.isFile());
    	assertFalse(transactionLog.committingFilePath.isFile());
    	transactionLog.add("record", "<x>ignored</x>");
    	ArrayList<String> files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.size());
        String expectedXml = "<transaction_item>" +
                "<action>addRDF</action>" +
                "<identifier>record</identifier>" + 
                "<filedata>&lt;x&gt;ignored&lt;/x&gt;</filedata>" +
            "</transaction_item>\n";
        FileInputStream fileInputStream = new FileInputStream(new File(transactionLog.getTransactionLogDir(), files.get(0)));
        assertEquals(expectedXml, Utils.read(fileInputStream));
        assertEquals(expectedXml.length(), new File(transactionLog.getTransactionLogDir(), files.get(0)).length());
    	assertTrue(transactionLog.committedFilePath.isFile());
    	assertFalse(transactionLog.committingFilePath.isFile());
    }

    @Test
    public void testAddDeleteRecord() throws FileNotFoundException, TransactionLogException, IOException {
        transactionLog.delete("record");
        ArrayList<String> files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.size());
        String expectedXml = "<transaction_item>" +
                "<action>delete</action>" +
                "<identifier>record</identifier>" + 
                "<filedata></filedata>" +
            "</transaction_item>\n";
        FileInputStream fileInputStream = new FileInputStream(new File(transactionLog.getTransactionLogDir(), files.get(0)));
        assertEquals(expectedXml, Utils.read(fileInputStream));
        assertEquals(expectedXml.length(), new File(transactionLog.getTransactionLogDir(), files.get(0)).length());
    }

    @Test
    public void testAddNotWhenFailed() throws IOException {
        class MyTripleStore extends OwlimTripleStore {
            public void addRDF(String identifier, String body) {
                throw new RuntimeException();
            }
        }
        TransactionLog transactionLog = new TransactionLog(new MyTripleStore(), tempdir);
        try {
            transactionLog.add("record", "<x>ignored</x>");
            fail("Should raise an TransactionLogException");
        } catch (TransactionLogException e) {}
        ArrayList<String> files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.size());
        assertEquals(0, countTransactionItems(files.get(0)));
    }

    @Test
    public void testAddWhenAlreadyExistsInTemp() throws Exception {
        final long time = System.currentTimeMillis();
        class MyTransactionLog extends TransactionLog {
            public MyTransactionLog(TripleStore tripleStore, File baseDir) throws IOException {
                super(tripleStore, baseDir);
            }
            long getTime() {
                return time;
            }
        }
        transactionLog = new MyTransactionLog(tsMock, tempdir);
        transactionLog.init();
        transactionLog.prepare("addRDF", "testRecord", String.valueOf(time), "<x>ignored</x>");

        transactionLog.add("testRecord", "data");
        ArrayList<String> files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.size());
        FileInputStream fileInputStream = new FileInputStream(new File(transactionLog.getTransactionLogDir(), files.get(0)));
        assertTrue(Utils.read(fileInputStream).contains("data"));
    }

    @Test
    public void testRollback() throws Exception {
        String filename = String.valueOf(System.currentTimeMillis());
        transactionLog.prepare("addRDF", "record1", filename, "data");
        transactionLog.prepare("addRDF", "record1", filename + "_1", "data");
        transactionLog.rollback(filename);
        String[] tmpFiles = transactionLog.getTempLogDir().list();
        String[] expected = {filename + "_1"};
        assertArrayEquals(expected, tmpFiles); 
    }

    @Test
    public void testRollbackNothingToDo() throws Exception {
        String filename = String.valueOf(System.currentTimeMillis());
        transactionLog.prepare("addRDF", "record1", filename + "_1", "data");
        transactionLog.rollback(filename);
        String[] tmpFiles = transactionLog.getTempLogDir().list();
        String[] expected = {filename + "_1"};
        assertArrayEquals(expected, tmpFiles); 
    }

    @Test
    public void testRollbackWhenPrepareFailes() throws IOException {
        final List<Boolean> rollback = new ArrayList<Boolean>();
        class MyTransactionLog extends TransactionLog {
            public MyTransactionLog(TripleStore tripleStore, File baseDir) throws IOException {
                super(tripleStore, baseDir);
            }
            String prepare(String action, String identifier, String filename, String filedata) {
                throw new RuntimeException();
            }
            void rollback(String filename) {
                rollback.add(true);
            }
        }
        transactionLog = new MyTransactionLog(tsMock, tempdir);
        try {
            transactionLog.doProcess("addRDF", "record", "data");
            fail("Should raise an Exception");
        } catch (TransactionLogException e) {}
        assertTrue(rollback.get(0));
    }

    @Test
    public void testRollbackWhenAddRDFFailes() throws IOException {
        final List<Boolean> rollback = new ArrayList<Boolean>();
        class MyTripleStore extends OwlimTripleStore {
            public void addRDF(String identifier, String body) {
                throw new RuntimeException();
            }
        }
        class MyTransactionLog extends TransactionLog {
            public MyTransactionLog(TripleStore tripleStore, File baseDir) throws IOException {
                super(tripleStore, baseDir);
            }
            void rollback(String filename) {
                rollback.add(true);
            }
        }
        TransactionLog transactionLog = new MyTransactionLog(new MyTripleStore(), tempdir);
        try {
            transactionLog.add("record", "data");
            fail("Should raise an Exception");
        } catch (TransactionLogException e) {}
        assertTrue(rollback.get(0));
    }

    @Test
    public void testRollbackWhenDeleteFailes() throws IOException {
        final List<Boolean> rollback = new ArrayList<Boolean>();
        class MyTripleStore extends OwlimTripleStore {
            public void delete(String identifier) {
                throw new RuntimeException();
            }
        }
        class MyTransactionLog extends TransactionLog {
            public MyTransactionLog(TripleStore tripleStore, File baseDir) throws IOException {
                super(tripleStore, baseDir);
            }
            void rollback(String filename) {
                rollback.add(true);
            }
        }
        TransactionLog transactionLog = new MyTransactionLog(new MyTripleStore(), tempdir);
        try {
            transactionLog.delete("record");
            fail("Should raise an Exception");
        } catch (TransactionLogException e) {}
        assertTrue(rollback.get(0));
    }

    @Test
    public void testRollbackAll() throws Exception {
        final List<String> rollback = new ArrayList<String>();
        class MyTransactionLog extends TransactionLog {
            public MyTransactionLog(TripleStore tripleStore, File baseDir) throws IOException {
                super(tripleStore, baseDir);
            }
            void rollback(String filename) {
                rollback.add(filename);
            }
        }
        
        transactionLog = new MyTransactionLog(tsMock, tempdir);
        transactionLog.init();
        transactionLog.committedFilePath.renameTo(transactionLog.committingFilePath);
        transactionLog.rollbackAll("record", 123);
        assertEquals("record", rollback.get(0));
        assertEquals("undoCommit", tsMock.actions.get(0));
    }

    @Test
    public void testRollbackWhenCommitFailes() throws Exception {
        final List<Boolean> rollbackAll = new ArrayList<Boolean>();
        final List<Boolean> committed = new ArrayList<Boolean>();
        class MyTransactionLog extends TransactionLog {
            public MyTransactionLog(TripleStore tripleStore, File baseDir) throws IOException {
                super(tripleStore, baseDir);
            }
            void commit_do(String filename) throws IOException {
            	committed.add(true);
            	super.commit_do(filename);
        		if (committed.size() < 3) {
            		return;
            	}
        		ArrayList<String> files = super.getTransactionItemFiles();
                String filedata = Utils.read(new File(super.transactionLogDir, files.get(0)));
                assertTrue(filedata.contains("record.rdf"));
                throw new RuntimeException();
            }
            void rollbackAll(String filename, long originalPosition) throws IOException {
                rollbackAll.add(true);
                super.rollbackAll(filename, originalPosition);
            }
        }
        transactionLog = new MyTransactionLog(tsMock, tempdir);
        transactionLog.init();
        addFilesToTransactionLog();
        try {
            transactionLog.doProcess("addRDF", "record.rdf", "data");
            fail("Should raise an Exception");
        } catch (TransactionLogException e) {}
        assertTrue(rollbackAll.get(0));
        ArrayList<String> files = transactionLog.getTransactionItemFiles();
        String filedata = Utils.read(new File(transactionLog.transactionLogDir, files.get(0)));
        assertTrue(filedata.contains("testRecord.rdf"));
        assertFalse(filedata.contains("record.rdf"));
        assertEquals(2, countTransactionItems(files.get(0)));
    }

    @Test
    public void testClearTransactionLog() throws TransactionLogException, IOException {
        transactionLog.add("record", "data");
        assertEquals(1, transactionLog.getTransactionItemFiles().size());
        transactionLog.clear();
        assertEquals(0, transactionLog.getTransactionItemFiles().size());
    }

    @Test
    public void testClearWhenShutdownSuccesFull() throws Exception {
        addFilesToTransactionLog();
        assertEquals(1, transactionLog.getTransactionItemFiles().size());
        transactionLog.persistTripleStore(transactionLog.transactionLogDir.listFiles()[0]);
        assertEquals(0, transactionLog.getTransactionItemFiles().size());
    }

    @Test
    public void testClearNotWhenShutdownFails() throws TransactionLogException, IOException {
        addFilesToTransactionLog();
        assertEquals(1, transactionLog.getTransactionItemFiles().size());

        class MyTripleStore extends OwlimTripleStore {
            public void shutdown() {
                throw new RuntimeException();
            }
        }
        transactionLog = new TransactionLog(new MyTripleStore(), tempdir);
        try {
            transactionLog.persistTripleStore(transactionLog.transactionLogDir.listFiles()[0]);
            fail("Should raise an error");
        } catch (Exception e) {
        	ArrayList<String> files = transactionLog.getTransactionItemFiles();
            assertEquals(1, transactionLog.getTransactionItemFiles().size());
            assertEquals(2, countTransactionItems(files.get(0)));
        }
    }
    
    @Test
    public void testClearOnlyOneFile() throws Exception {
    	transactionLog = new TransactionLog(this.tsMock, this.tempdir, 1.0/1024/1024*5);
    	transactionLog.init();
    	transactionLog.add("testRecord", "<x>ignored</x>");
    	transactionLog.add("testRecord2", "<x>ignored</x>");
    	transactionLog.add("testRecord3", "<x>ignored</x>");
    	ArrayList<String> files = transactionLog.getTransactionItemFiles();
    	assertEquals(2, files.size());
    	assertEquals(2, countTransactionItems(files.get(0)));
    	assertEquals(1, countTransactionItems(files.get(1)));
    	transactionLog.clear(new File(transactionLog.transactionLogDir, files.get(0)));
    	files = transactionLog.getTransactionItemFiles();
    	assertEquals(1, files.size());
    	assertEquals(1, countTransactionItems(files.get(0)));
    }

    @Test
    public void testStartupAfterShutdown() throws Exception {
        addFilesToTransactionLog();
        assertEquals(1, transactionLog.getTransactionItemFiles().size());

        this.tsMock = new TSMock();
        transactionLog = new TransactionLog(this.tsMock, this.tempdir);
        transactionLog.persistTripleStore(transactionLog.transactionLogDir.listFiles()[0]);
        assertEquals(2, this.tsMock.actions.size());
        assertEquals("shutdown", this.tsMock.actions.get(0));
        assertEquals("startup", this.tsMock.actions.get(1));
    }

    @Test
    public void testRecoverTransactionLog() throws TransactionLogException, Exception {
        addFilesToTransactionLog();
        tsMock = new TSMock();
        transactionLog = new TransactionLog(tsMock, tempdir);
        transactionLog.recoverTripleStore();
        assertEquals(4, tsMock.actions.size());
        assertEquals("add:testRecord.rdf|<x>ignored</x>", tsMock.actions.get(0));
        assertEquals("delete:testRecord.rdf", tsMock.actions.get(1));
        assertEquals("shutdown", tsMock.actions.get(2));
        assertEquals("startup", tsMock.actions.get(3));
    }

    @Test
    public void testCorruptedCurrentFileInTransactionLog() throws Exception {
    	String currentData = "<transaction_item>\n" +
    		"    <action>addRDF</action>\n" +
    		"    <identifier>test1.rdf</identifier>\n" +
    		"    <filedata>ignored</filedata>\n" +
        	"<transaction_item>\n" +
    		"    <action>addRDF</action>\n" +
    		"    <identifier>test2.rdf</identifier>\n" +
    		"    <filedata>ignored</filedata>\n" +
    		"</transaction_item>\n";
    		
    	Utils.write(transactionLog.transactionLogFilePath, currentData); 
    	
    	OutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        PrintStream err = System.err;
        System.setErr(ps);
    	try {
    		transactionLog.recoverTripleStore();
    		fail("Should fail");
    	} catch (TransactionLogException e)  {
    		assertEquals("Corrupted transaction_item in " + transactionLog.transactionLogFilePath.getAbsolutePath() + ". This should never occur.", e.getMessage());
    		assertTrue(os.toString(), os.toString().contains("XML document structures must start and end within the same entity."));
    	} finally {
    		System.setErr(err);
    	}
    	assertEquals(0, tsMock.actions.size());
    }
    
    @Test
    public void testCorruptedLastItemInNonCurrentFileInTransactionLog() throws Exception {
    	String nonCurrentData = "<transaction_item>\n" +
    		"    <action>addRDF</action>\n" +
    		"    <identifier>test1.rdf</identifier>\n" +
    		"    <filedata>ignored</filedata>\n" +
        	"</transaction_item>\n" +
        	"<transaction_item>\n" +
    		"    <action>addRDF</action>\n";
 
    	String currentData = "<transaction_item>\n" +
		"    <action>addRDF</action>\n" +
		"    <identifier>test2.rdf</identifier>\n" +
		"    <filedata>ignored</filedata>\n" +
    	"</transaction_item>\n";
    	
    	File nonCurrentFile = new File(transactionLog.transactionLogDir, String.valueOf(transactionLog.getTime()));
    	Utils.write(nonCurrentFile, nonCurrentData);
    	Utils.write(transactionLog.transactionLogFilePath, currentData); 
    	
    	try {
    		transactionLog.recoverTripleStore();
    		fail("Should fail");
    	} catch (TransactionLogException e)  {
    		assertEquals("Last TransactionLog item in " + nonCurrentFile.getAbsolutePath() + " is corrupted. This should never occur.", e.getMessage());
    	}
    	assertEquals(1, tsMock.actions.size());
    	assertEquals("add:test1.rdf|ignored", tsMock.actions.get(0));
    }
    
    @Test
    public void testRemoveFilesWhenTransactionLogIsRecovered() throws Exception {
    	addFilesToTransactionLog();
        transactionLog.init();
        String[] files = transactionLog.getTransactionLogDir().list();
        assertEquals(0, countTransactionItems(files[0]));
        assertEquals(1, files.length);
        assertEquals("current", files[0]);
    }

    @Test
    public void testNotPersistingOnInitIfTransactionLogIsEmpty() throws Exception {
        final List<Boolean> recoverCalled = new ArrayList<Boolean>();
        final List<Boolean> persistCalled = new ArrayList<Boolean>();
        class MyTransactionLog extends TransactionLog {
            public MyTransactionLog(TripleStore triplestore, File baseDir) throws IOException {
                super(triplestore, baseDir);
            }
            void persistTripleStore(File transactionLogFile) {
                persistCalled.add(true);
            }
            void recoverTripleStore() throws Exception {
                recoverCalled.add(true);
                super.recoverTripleStore();
            }
        }
        transactionLog = new MyTransactionLog(tsMock, tempdir);
        transactionLog.init();
        assertTrue(recoverCalled.get(0));
        assertEquals(0, persistCalled.size());
    } 

    @Test
    public void testPersistOnInitIfTransactionLogIsNotEmpty() throws Exception {
        final List<Boolean> recoverCalled = new ArrayList<Boolean>();
        final List<Boolean> persistCalled = new ArrayList<Boolean>();
        class MyTransactionLog extends TransactionLog {
            public MyTransactionLog(TripleStore triplestore, File baseDir) throws IOException {
                super(triplestore, baseDir);
            }
            void persistTripleStore(File transactionLogFile) {
                persistCalled.add(true);
            }
            void recoverTripleStore() throws Exception {
                recoverCalled.add(true);
                super.recoverTripleStore();
            }
        }
        addFilesToTransactionLog();
        transactionLog = new MyTransactionLog(tsMock, tempdir);
        transactionLog.init();
        assertTrue(recoverCalled.get(0));
        assertTrue(persistCalled.get(0));
    } 

    @Test
    public void testClearTempLogDir() throws Exception {
        String filename = String.valueOf(transactionLog.getTime());
        transactionLog.prepare("addRDF", "record", filename, "ignored");
        transactionLog.prepare("addRDF", "record", filename + "_1", "ignored");
        assertEquals(2, transactionLog.getTempLogDir().list().length);
        transactionLog = new TransactionLog(tsMock, tempdir);
        assertEquals(0, transactionLog.getTempLogDir().list().length);
    }
    
    @Test
    public void testCommitCreatesCommittingFile() throws Exception {
    	String filename = String.valueOf(transactionLog.getTime());
        filename = transactionLog.prepare("addRDF", "record", filename, "ignored");
        
        transactionLog.transactionLog.close();
        
        try {
        	transactionLog.commit(filename);
        	fail("Should fail");
        } catch (Exception e) {
        	assertTrue(transactionLog.committingFilePath.isFile());
        	assertFalse(transactionLog.committedFilePath.isFile());
        }
    }
    
    @Test
    public void testRollbackAfterCommitFailsClearsTransactionLogFile() throws Exception {
    	final List<String> rollbackCalled = new ArrayList<String>();
 
        class MyTransactionLog extends TransactionLog {
            public MyTransactionLog(TripleStore triplestore, File baseDir) throws IOException {
                super(triplestore, baseDir);
            }
            void rollback(String filename) {
                rollbackCalled.add(filename);
            }
        }
        transactionLog = new MyTransactionLog(tsMock, tempdir);
        transactionLog.init();
        transactionLog.add("test1.rdf", "ignored");
        
        long originalPosition = transactionLog.transactionLog.getFilePointer();
        String tempFile = null;
        try {
        	tempFile = transactionLog.prepare("addRDF", "test2.rdf", String.valueOf(transactionLog.getTime()), "ignored");
        	Process process = Runtime.getRuntime().exec("chmod 0555 " + transactionLog.tempLogDir.getAbsolutePath());
            process.waitFor();
            transactionLog.commit(tempFile);
            fail("Should fail on remove.");
        } catch (Exception e) {
        	assertTrue(transactionLog.committingFilePath.isFile());
        	assertFalse(transactionLog.committedFilePath.isFile());
        	String transactionLogData = Utils.read(transactionLog.transactionLogFilePath);
        	assertTrue(transactionLogData, transactionLogData.contains("<identifier>test1.rdf</identifier>"));
        	assertTrue(transactionLogData, transactionLogData.contains("<identifier>test2.rdf</identifier>"));
        	tsMock.actions.clear();
        	transactionLog.rollbackAll(tempFile, originalPosition);
        	assertFalse(transactionLog.committingFilePath.isFile());
        	assertTrue(transactionLog.committedFilePath.isFile());
        	transactionLogData = Utils.read(transactionLog.transactionLogFilePath);
        	assertEquals("<transaction_item>" +
        			"<action>addRDF</action>" +
        			"<identifier>test1.rdf</identifier>" +
        			"<filedata>ignored</filedata>" +
        		"</transaction_item>\n", transactionLogData);
        	assertEquals(1, rollbackCalled.size());
        	assertEquals(tempFile, rollbackCalled.get(0));
        	assertEquals(1, tsMock.actions.size());
        	assertEquals("undoCommit", tsMock.actions.get(0));
        }
    }
    
    @Test
    public void testSplitInMultipleTransactionFiles() throws Exception {
    	setTransactionLog(1.0/1024/1024*5);
    	transactionLog.add("test1.rdf", "ignored");
    	transactionLog.add("test2.rdf", "ignored");
    	transactionLog.add("test3.rdf", "ignored");
    	ArrayList<String> tsFiles = transactionLog.getTransactionItemFiles();
    	assertEquals(2, tsFiles.size());
    	assertEquals("<transaction_item>" +
    			"<action>addRDF</action>" +
    			"<identifier>test1.rdf</identifier>" +
    			"<filedata>ignored</filedata>" +
    		"</transaction_item>\n" +
    		"<transaction_item>" +
				"<action>addRDF</action>" +
				"<identifier>test2.rdf</identifier>" +
				"<filedata>ignored</filedata>" +
			"</transaction_item>\n", Utils.read(new File(transactionLog.transactionLogDir, tsFiles.get(0))));
    	assertEquals("<transaction_item>" +
				"<action>addRDF</action>" +
				"<identifier>test3.rdf</identifier>" +
				"<filedata>ignored</filedata>" +
			"</transaction_item>\n", Utils.read(new File(transactionLog.transactionLogDir, tsFiles.get(1))));
    }
    
    @Test
    public void testRecoverAfterCrashWhileInCommittingState() throws Exception {
    	String currentData = "<transaction_item>\n" +
		"    <action>addRDF</action>\n" +
		"    <identifier>test1.rdf</identifier>\n" +
		"    <filedata>ignored</filedata>\n" +
    	"</transaction_item>\n" +
    	"<transaction_item>\n" +
		"    <action>addRDF</action>";
    	Utils.write(transactionLog.transactionLogFilePath, currentData);
    	transactionLog.committedFilePath.renameTo(transactionLog.committingFilePath);
    	setTransactionLog(1.0/1024/1024*5);
    	String[] expected = {"add:test1.rdf|ignored", "shutdown", "startup"};
    	assertArrayEquals(expected, tsMock.actions.toArray());
    }
    
    @Test
    public void testRecoverAfterCrashWhileInCommittedState() throws Exception {
    	String currentData = "<transaction_item>\n" +
		"    <action>addRDF</action>\n" +
		"    <identifier>test1.rdf</identifier>\n" +
		"    <filedata>ignored</filedata>\n" +
    	"</transaction_item>\n" +
    	"<transaction_item>\n" +
		"    <action>addRDF</action>";
    	Utils.write(transactionLog.transactionLogFilePath, currentData);
    	transactionLog = new TransactionLog(tsMock, tempdir);
    	try {
    		transactionLog.init();
    		fail("commited state with incomplete transactionLog should fail");
    	} catch (Exception e) {
    		assertEquals("Last TransactionLog item is incomplete while not in the committing state. This should never occur.", e.getMessage());
    	}
    	String[] expected = {"add:test1.rdf|ignored"};
    	assertArrayEquals(expected, tsMock.actions.toArray());
    	assertTrue(transactionLog.committedFilePath.isFile());
    }

    @Test
    public void testRecoverMultipleFiles() throws Exception {
    	setTransactionLog(1.0/1024/1024*5);
    	transactionLog.add("test1.rdf", "ignored");
    	transactionLog.add("test2.rdf", "ignored");
    	transactionLog.add("test3.rdf", "ignored");
    	
    	setTransactionLog(1.0/1024/1024*5);
    	String[] expected = {"add:test1.rdf|ignored", "add:test2.rdf|ignored", "shutdown", "startup", "add:test3.rdf|ignored", "shutdown", "startup"};
    	assertArrayEquals(expected, tsMock.actions.toArray());
    }
    
    @Test
    public void testBigCurrentFileInTransactionLog() throws Exception {
    	String filedata = StringUtils.repeat(StringUtils.repeat("ignored", 1024), 100);
    	String tsItem = "<transaction_item>\n" +
		"    <action>addRDF</action>\n" +
		"    <identifier>test1.rdf</identifier>\n" +
		"    <filedata>" + filedata + "</filedata>\n" +
    	"</transaction_item>\n";
    	FileWriter fw = new FileWriter(transactionLog.transactionLogFilePath);
    	for (int i = 0; i < 100; i++) {
    		fw.write(tsItem);
    	}
		fw.close();
		class MyTSMock extends TSMock {
			public void addRDF(String identifier, String data) {
		        actions.add("add:" + identifier);
		    }
		}
		tsMock = new MyTSMock();
		transactionLog = new TransactionLog(tsMock, tempdir);
        transactionLog.init();
        assertEquals(StringUtils.repeat(".", 50) + " 50Mb", stdout.toString(), stdout.toString());
    }
    
    private void addFilesToTransactionLog() throws TransactionLogException, IOException {
        transactionLog.add("testRecord.rdf", "<x>ignored</x>");
        transactionLog.delete("testRecord.rdf");
    }
    
    private int countTransactionItems(String filename) throws IOException {
    	return Utils.read(new File(transactionLog.getTransactionLogDir(), filename)).split("<transaction_item>").length - 1;
    }
}
