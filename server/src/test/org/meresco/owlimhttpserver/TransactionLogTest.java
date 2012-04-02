/* begin license *
 * 
 * The Meresco Owlim package consists out of a HTTP server written in Java that
 * provides access to an Owlim Triple store, as well as python bindings to
 * communicate as a client with the server. 
 * 
 * Copyright (C) 2011 Seecr (Seek You Too B.V.) http://seecr.nl
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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.RuntimeException;

import org.junit.Ignore;
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

    @Before
    public void setUp() throws Exception {
        setTransactionLog();
    }

    @After
    public void tearDown() throws Exception {
        deleteDirectory(tempdir);
    }

    public void setTransactionLog(double maxSizeInMb) throws Exception {
        tempdir = createTempDirectory();
        tsMock = new TSMock();
        transactionLog = new TransactionLog(tsMock, tempdir, maxSizeInMb);
        transactionLog.init();
    }
        
    public void setTransactionLog() throws Exception {
        tempdir = createTempDirectory();
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
        String[] files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.length);
        assertEquals("current", files[0]);
        String expectedXml = "<transaction_item>" +
                "<action>addRDF</action>" +
                "<identifier>testRecord</identifier>" + 
                "<filedata>&lt;x&gt;ignoréd&lt;/x&gt;</filedata>" +
            "</transaction_item>";
        assertEquals(expectedXml, Utils.read(new File(transactionLog.getTransactionLogDir(), files[0])));
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
        String[] files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.length);
        assertEquals("current", files[0]);
        assertEquals("<transaction_item>" +
        		"<action>addRDF</action>" +
        		"<identifier>testRecord</identifier>" +
        		"<filedata>&lt;x&gt;ignored1&lt;/x&gt;</filedata>" +
        	"</transaction_item>", Utils.read(new File(transactionLog.getTransactionLogDir(), files[0])));
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
        String[] files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.length);
        assertEquals("current", files[0]);
        assertEquals(3, countTransactionItems(files[0]));
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
        String[] files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.length);
        String expectedXml = "<transaction_item>" +
                "<action>addRDF</action>" +
                "<identifier>record</identifier>" + 
                "<filedata>&lt;x&gt;ignored&lt;/x&gt;</filedata>" +
            "</transaction_item>\n";
        FileInputStream fileInputStream = new FileInputStream(new File(transactionLog.getTransactionLogDir(), files[0]));
        assertEquals(expectedXml, Utils.read(fileInputStream));
        assertEquals(expectedXml.length(), new File(transactionLog.getTransactionLogDir(), files[0]).length());
    	assertTrue(transactionLog.committedFilePath.isFile());
    	assertFalse(transactionLog.committingFilePath.isFile());
    }

    @Test
    public void testAddDeleteRecord() throws FileNotFoundException, TransactionLogException, IOException {
        transactionLog.delete("record");
        String[] files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.length);
        String expectedXml = "<transaction_item>" +
                "<action>delete</action>" +
                "<identifier>record</identifier>" + 
                "<filedata></filedata>" +
            "</transaction_item>\n";
        FileInputStream fileInputStream = new FileInputStream(new File(transactionLog.getTransactionLogDir(), files[0]));
        assertEquals(expectedXml, Utils.read(fileInputStream));
        assertEquals(expectedXml.length(), new File(transactionLog.getTransactionLogDir(), files[0]).length());
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
        String[] files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.length);
        assertEquals(0, countTransactionItems(files[0]));
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
        String[] files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.length);
        FileInputStream fileInputStream = new FileInputStream(new File(transactionLog.getTransactionLogDir(), files[0]));
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
        transactionLog.rollbackAll("record", 123);
        assertEquals("record", rollback.get(0));
        assertEquals("undoCommit", tsMock.actions.get(0));
    }

    @Test
    public void testRollbackWhenCommitFailes() throws Exception {
        final List<Boolean> rollbackAll = new ArrayList<Boolean>();
        class MyTransactionLog extends TransactionLog {
            public MyTransactionLog(TripleStore tripleStore, File baseDir) throws IOException {
                super(tripleStore, baseDir);
            }
            void commit(String filename) {
                throw new RuntimeException();
            }
            void rollbackAll(String filename, long originalPosition) {
                rollbackAll.add(true);
            }
        }
        transactionLog = new MyTransactionLog(tsMock, tempdir);
        transactionLog.init();
        try {
            transactionLog.doProcess("addRDF", "record", "data");
            fail("Should raise an Exception");
        } catch (TransactionLogException e) {}
        assertTrue(rollbackAll.get(0));
    }

    @Test
    public void testClearTransactionLog() throws TransactionLogException, IOException {
        transactionLog.add("record", "data");
        assertEquals(1, transactionLog.getTransactionItemFiles().length);
        transactionLog.clear();
        assertEquals(0, transactionLog.getTransactionItemFiles().length);
    }

    @Test
    public void testClearWhenShutdownSuccesFull() throws Exception {
        addFilesToTransactionLog();
        assertEquals(1, transactionLog.getTransactionItemFiles().length);
        transactionLog.persistTripleStore(transactionLog.transactionLogDir.listFiles()[0]);
        assertEquals(0, transactionLog.getTransactionItemFiles().length);
    }

    @Test
    public void testClearNotWhenShutdownFails() throws TransactionLogException, IOException {
        addFilesToTransactionLog();
        assertEquals(1, transactionLog.getTransactionItemFiles().length);

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
        	String[] files = transactionLog.getTransactionItemFiles();
            assertEquals(1, transactionLog.getTransactionItemFiles().length);
            assertEquals(2, countTransactionItems(files[0]));
        }
    }

    @Test
    public void testStartupAfterShutdown() throws Exception {
        addFilesToTransactionLog();
        assertEquals(1, transactionLog.getTransactionItemFiles().length);

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
        String filename = String.valueOf(System.currentTimeMillis());
        transactionLog.prepare("addRDF", "record", filename, "ignored");
        transactionLog.prepare("addRDF", "record", filename + "_1", "ignored");
        assertEquals(2, transactionLog.getTempLogDir().list().length);
        transactionLog = new TransactionLog(tsMock, tempdir);
        assertEquals(0, transactionLog.getTempLogDir().list().length);
    }

    private void addFilesToTransactionLog() throws TransactionLogException, IOException {
        transactionLog.add("testRecord.rdf", "<x>ignored</x>");
        transactionLog.delete("testRecord.rdf");
    }
    
    private int countTransactionItems(String filename) throws IOException {
    	return Utils.read(new File(transactionLog.getTransactionLogDir(), filename)).split("<transaction_item>").length - 1;
    }
}
