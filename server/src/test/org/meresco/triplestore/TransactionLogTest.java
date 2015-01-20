/* begin license *
 *
 * The Meresco Owlim package consists out of a HTTP server written in Java that
 * provides access to an Owlim Triple store, as well as python bindings to
 * communicate as a client with the server.
 *
 * Copyright (C) 2011-2014 Seecr (Seek You Too B.V.) http://seecr.nl
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

package org.meresco.triplestore;

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
import org.apache.commons.codec.binary.Base64;
import org.openrdf.rio.RDFFormat;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import static org.junit.Assert.*;

import static org.meresco.triplestore.Utils.createTempDirectory;
import static org.meresco.triplestore.Utils.deleteDirectory;


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
    public void testCommitToTransactionLog() throws Exception {
        String xml = "<x>ignoréd</x>";
        String filedata = Base64.encodeBase64String(xml.getBytes());
        transactionLog.add("testRecord", xml, RDFFormat.RDFXML);
        ArrayList<String> files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.size());
        assertEquals("current", files.get(0));
        String expectedXml = "<transaction_item>" +
                "<action>add</action>" +
                "<identifier>testRecord</identifier>" +
                "<filedata>" + filedata + "</filedata>" +
            "</transaction_item>\n";
        assertEquals(expectedXml, Utils.read(new File(transactionLog.getTransactionLogDir(), files.get(0))));
    }

    @Test
    public void testAddMultipleSameIdentifiers() throws Exception {
        transactionLog.add("testRecord", "<x>ignored</x>", RDFFormat.RDFXML);
        transactionLog.add("testRecord", "<x>ignored</x>", RDFFormat.RDFXML);
        transactionLog.add("testRecord", "<x>ignored</x>", RDFFormat.RDFXML);
        ArrayList<String> files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.size());
        assertEquals("current", files.get(0));
        assertEquals(3, countTransactionItems(files.get(0)));
    }

    @Test
    public void testAddUnsupporedFormat() throws Exception {
        try {
            transactionLog.add("testRecord", "ignored", RDFFormat.NTRIPLES);
            fail();
        } catch (UnsupportedOperationException e) {
            assertEquals("Only RDFXML supported with transactionLog", e.getMessage());
        }
        ArrayList<String> files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.size());
        assertEquals("", Utils.read(new File(transactionLog.getTransactionLogDir(), files.get(0))));
    }

    @Test
    public void testEscapeIdentifier() throws FileNotFoundException, Exception {
        String filedata = Base64.encodeBase64String("<x>ignored</x>".getBytes());
        transactionLog.add("<testRecord>", "<x>ignored</x>", RDFFormat.RDFXML);
        String expectedXml = "<transaction_item>" +
                "<action>add</action>" +
                "<identifier>&lt;testRecord&gt;</identifier>" +
                "<filedata>" + filedata + "</filedata>" +
            "</transaction_item>\n";
        assertEquals(expectedXml, Utils.read(transactionLog.transactionLogFilePath));
    }

    @Test
    public void testAddSuccesfullRecord() throws Exception {
    	assertTrue(transactionLog.committedFilePath.isFile());
    	assertFalse(transactionLog.committingFilePath.isFile());
    	transactionLog.add("record", "<x>ignored</x>", RDFFormat.RDFXML);
    	ArrayList<String> files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.size());
        String filedata = Base64.encodeBase64String("<x>ignored</x>".getBytes());
        String expectedXml = "<transaction_item>" +
                "<action>add</action>" +
                "<identifier>record</identifier>" +
                "<filedata>" + filedata + "</filedata>" +
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
    public void testAddTriple() throws Exception {
        final ArrayList<String> calls = new ArrayList<String>();
        class MyTripleStore extends SesameTriplestore {
            public void addTriple(String body) {
                calls.add("addTriple");
            }
        }
        TransactionLog transactionLog = new TransactionLog(new MyTripleStore(), tempdir);
        transactionLog.init();

        transactionLog.addTriple("uri:subj|uri:pred|uri:subj");
        ArrayList<String> files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.size());
        String filedata = Base64.encodeBase64String("uri:subj|uri:pred|uri:subj".getBytes());
        String expectedXml = "<transaction_item>" +
                "<action>addTriple</action>" +
                "<identifier></identifier>" +
                "<filedata>" + filedata + "</filedata>" +
            "</transaction_item>\n";
        assertEquals(1, calls.size());
        FileInputStream fileInputStream = new FileInputStream(new File(transactionLog.getTransactionLogDir(), files.get(0)));
        assertEquals(expectedXml, Utils.read(fileInputStream));
        assertEquals(expectedXml.length(), new File(transactionLog.getTransactionLogDir(), files.get(0)).length());
    }

    @Test
    public void testRemoveTriple() throws Exception {
        final ArrayList<String> calls = new ArrayList<String>();
        class MyTripleStore extends SesameTriplestore {
            public void removeTriple(String body) {
                calls.add("removeTriple");
            }
        }
        TransactionLog transactionLog = new TransactionLog(new MyTripleStore(), tempdir);
        transactionLog.init();

        transactionLog.removeTriple("uri:subj|uri:pred|uri:subj");
        ArrayList<String> files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.size());
        String filedata = Base64.encodeBase64String("uri:subj|uri:pred|uri:subj".getBytes());
        String expectedXml = "<transaction_item>" +
                "<action>removeTriple</action>" +
                "<identifier></identifier>" +
                "<filedata>" + filedata + "</filedata>" +
            "</transaction_item>\n";
        assertEquals(1, calls.size());
        FileInputStream fileInputStream = new FileInputStream(new File(transactionLog.getTransactionLogDir(), files.get(0)));
        assertEquals(expectedXml, Utils.read(fileInputStream));
        assertEquals(expectedXml.length(), new File(transactionLog.getTransactionLogDir(), files.get(0)).length());
    }

    @Test
    public void testAddNotWhenFailed() throws Exception {
        class MyTripleStore extends SesameTriplestore {
            public void add(String identifier, String body) {
                throw new RuntimeException();
            }
        }
        TransactionLog transactionLog = new TransactionLog(new MyTripleStore(), tempdir);
        try {
            transactionLog.add("record", "<x>ignored</x>", RDFFormat.RDFXML);
            fail("Should raise an TransactionLogException");
        } catch (RuntimeException e) {}
        ArrayList<String> files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.size());
        assertEquals(0, countTransactionItems(files.get(0)));
    }

    @Test
    public void testNotAddedToTransactionLogWhenAddFails() throws Exception {
        class MyTripleStore extends SesameTriplestore {
            public void add(String identifier, String body) {
                throw new RuntimeException();
            }
        }
        class MyTransactionLog extends TransactionLog {
            public MyTransactionLog(Triplestore tripleStore, File baseDir) throws Exception {
                super(tripleStore, baseDir);
            }
        }
        TransactionLog transactionLog = new MyTransactionLog(new MyTripleStore(), tempdir);
        try {
            transactionLog.add("record", "data", RDFFormat.RDFXML);
            fail("Should raise an Exception");
        } catch (RuntimeException e) {}
        assertEquals("", Utils.read(transactionLog.transactionLogFilePath));
    }

    @Test
    public void testRollbackWhenDeleteFails() throws Exception {
        class MyTripleStore extends SesameTriplestore {
            public void delete(String identifier) {
                throw new RuntimeException();
            }
        }
        class MyTransactionLog extends TransactionLog {
            public MyTransactionLog(Triplestore tripleStore, File baseDir) throws Exception {
                super(tripleStore, baseDir);
            }
        }
        TransactionLog transactionLog = new MyTransactionLog(new MyTripleStore(), tempdir);
        try {
            transactionLog.delete("record");
            fail("Should raise an Exception");
        } catch (RuntimeException e) {}
        assertEquals("", Utils.read(transactionLog.transactionLogFilePath));
    }

    @Test
    public void testFailingCommit() throws Exception {
        final List<Boolean> committed = new ArrayList<Boolean>();
        class MyTransactionLog extends TransactionLog {
            public MyTransactionLog(Triplestore tripleStore, File baseDir) throws Exception {
                super(tripleStore, baseDir);
            }
            void commit_do(TransactionItem tsItem) throws IOException {
            	committed.add(true);
            	super.commit_do(tsItem);
        		if (committed.size() < 3) {
            		return;
            	}
        		ArrayList<String> files = super.getTransactionItemFiles();
                String filedata = Utils.read(new File(super.transactionLogDir, files.get(0)));
                assertTrue(filedata.contains("record.rdf"));
                throw new RuntimeException("An error message");
            }
        }
        transactionLog = new MyTransactionLog(tsMock, tempdir);
        transactionLog.init();
        addFilesToTransactionLog();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(stderr);
        PrintStream orig_stderr = System.err;
        System.setErr(ps);
        try {
            transactionLog.writeTransactionItem("add", "record.rdf", "data");
            fail("Should raise an Exception");
        } catch (Error e) {}
    	finally {
    		System.setErr(orig_stderr);
    	}
        ArrayList<String> files = transactionLog.getTransactionItemFiles();
        String filedata = Utils.read(new File(transactionLog.transactionLogDir, files.get(0)));
        assertTrue(filedata.contains("testRecord.rdf"));
        assertTrue(filedata.contains("record.rdf"));//Transaction is saved but the server will crash
        assertEquals(3, countTransactionItems(files.get(0)));
        assertTrue(stderr.toString(), stderr.toString().contains("An error message"));
    }

    @Test
    public void testClearTransactionLog() throws Exception {
        transactionLog.add("record", "data", RDFFormat.RDFXML);
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
    public void testClearNotWhenShutdownFails() throws Exception {
        class MyTripleStore extends TSMock {
            public void shutdown() {
                throw new RuntimeException();
            }
        }
        transactionLog = new TransactionLog(new MyTripleStore(), tempdir);
        addFilesToTransactionLog();
        assertEquals(1, transactionLog.getTransactionItemFiles().size());

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
    	transactionLog.add("testRecord", "<x>ignored</x>", RDFFormat.RDFXML);
        Thread.sleep(1);
    	transactionLog.add("testRecord2", "<x>ignored</x>", RDFFormat.RDFXML);
        Thread.sleep(1);
    	transactionLog.add("testRecord3", "<x>ignored</x>", RDFFormat.RDFXML);
    	ArrayList<String> files = transactionLog.getTransactionItemFiles();
    	assertEquals(4, files.size());
    	assertEquals(1, countTransactionItems(files.get(0)));
    	assertEquals(1, countTransactionItems(files.get(1)));
    	assertEquals(1, countTransactionItems(files.get(2)));
    	assertEquals(0, countTransactionItems(files.get(3)));
    	transactionLog.clear(new File(transactionLog.transactionLogDir, files.get(0)));
    	files = transactionLog.getTransactionItemFiles();
    	assertEquals(3, files.size());
    	assertEquals(1, countTransactionItems(files.get(0)));
    	assertEquals(1, countTransactionItems(files.get(1)));
    	assertEquals(0, countTransactionItems(files.get(2)));
    }

    @Test
    public void testStrangeCharacter() throws Exception {
        transactionLog = new TransactionLog(this.tsMock, this.tempdir, 1024);
        transactionLog.init();
        transactionLog.add("testRecord", "redrum:md5:43494d3c3ab83ba652004d940127738e|http://data.linkedmdb.org/resource/movie/plots|Symphony in Blood Red is an Italian &apos;giallo&apos;, a horror film inspired by the wor, RDFFormat.RDFXMLk of Dario Argento. It is the first feature movie directed by Luigi Pastore, and in collaboration with Antonio Tentori (Cat In The Brain), who co-wrote the screenplay.", RDFFormat.RDFXML);
        ArrayList<String> files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.size());
        tsMock = new TSMock();
        transactionLog = new TransactionLog(tsMock, tempdir);
        transactionLog.recoverTripleStore();
    }

    @Test
    public void testStartupAfterShutdown() throws Exception {
        addFilesToTransactionLog();
        assertEquals(1, transactionLog.getTransactionItemFiles().size());

        this.tsMock = new TSMock();
        transactionLog = new TransactionLog(this.tsMock, this.tempdir);
        transactionLog.persistTripleStore(transactionLog.transactionLogDir.listFiles()[0]);
        assertEquals(6, this.tsMock.actions.size());
        assertEquals("add:testRecord.rdf|<x>ignored</x>|RDF/XML", this.tsMock.actions.get(0));
        assertEquals("delete:testRecord.rdf", tsMock.actions.get(1));
        assertEquals("shutdown", this.tsMock.actions.get(2));
        assertEquals("startup", this.tsMock.actions.get(3));
        assertEquals("shutdown", this.tsMock.actions.get(4));
        assertEquals("startup", this.tsMock.actions.get(5));
    }

    @Test
    public void testRecoverTransactionLog() throws TransactionLogException, Exception {
        addFilesToTransactionLog();
        tsMock = new TSMock();
        transactionLog = new TransactionLog(tsMock, tempdir);
        transactionLog.recoverTripleStore();
        assertEquals(4, tsMock.actions.size());
        assertEquals("add:testRecord.rdf|<x>ignored</x>|RDF/XML", tsMock.actions.get(0));
        assertEquals("delete:testRecord.rdf", tsMock.actions.get(1));
        assertEquals("shutdown", tsMock.actions.get(2));
        assertEquals("startup", tsMock.actions.get(3));
    }

    @Test
    public void testCorruptedCurrentFileInTransactionLog() throws Exception {
    	String currentData = "<transaction_item>\n" +
    		"    <action>add</action>\n" +
    		"    <identifier>test1.rdf</identifier>\n" +
    		"    <filedata>ignored</filedata>\n" +
        	"<transaction_item>\n" +
    		"    <action>add</action>\n" +
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
    		assertEquals("Corrupted transaction_item in " + transactionLog.transactionLogFilePath.getAbsolutePath() + " at line 9. This should never occur.", e.getMessage());
    		assertTrue(os.toString(), os.toString().contains("XML document structures must start and end within the same entity."));
    	} finally {
    		System.setErr(err);
    	}
    	assertEquals(0, tsMock.actions.size());
    }

    @Test
    public void testCorruptedLastItemInNonCurrentFileInTransactionLog() throws Exception {
        String filedata = Base64.encodeBase64String("ignored".getBytes());
    	String nonCurrentData = "<transaction_item>\n" +
    		"    <action>add</action>\n" +
    		"    <identifier>test1.rdf</identifier>\n" +
    		"    <filedata>" + filedata + "</filedata>\n" +
        	"</transaction_item>\n" +
        	"<transaction_item>\n" +
    		"    <action>add</action>\n";

    	String currentData = "<transaction_item>\n" +
		"    <action>add</action>\n" +
		"    <identifier>test2.rdf</identifier>\n" +
		"    <filedata>" + filedata + "</filedata>\n" +
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
    	assertEquals("add:test1.rdf|ignored|RDF/XML", tsMock.actions.get(0));
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
            public MyTransactionLog(Triplestore triplestore, File baseDir) throws Exception {
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
            public MyTransactionLog(Triplestore triplestore, File baseDir) throws Exception {
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
    public void testCommitCreatesCommittingFile() throws Exception {
    	TransactionItem tsItem = new TransactionItem("add", "record", "ignored");
        transactionLog.transactionLog.close();

        try {
        	transactionLog.commit(tsItem);
        	fail("Should fail");
        } catch (Exception e) {
        	assertTrue(transactionLog.committingFilePath.isFile());
        	assertFalse(transactionLog.committedFilePath.isFile());
        }
    }

    @Test
    public void testSplitInMultipleTransactionFiles() throws Exception {
        String filedata = Base64.encodeBase64String("ignored".getBytes());
    	setTransactionLog(1.0/1024/1024*5);
    	transactionLog.add("test1.rdf", "ignored", RDFFormat.RDFXML);
        Thread.sleep(1);
    	transactionLog.add("test2.rdf", "ignored", RDFFormat.RDFXML);
        Thread.sleep(1);
    	transactionLog.add("test3.rdf", "ignored", RDFFormat.RDFXML);
    	ArrayList<String> tsFiles = transactionLog.getTransactionItemFiles();
    	assertEquals(4, tsFiles.size());
    	assertEquals("<transaction_item>" +
    			"<action>add</action>" +
    			"<identifier>test1.rdf</identifier>" +
    			"<filedata>" + filedata + "</filedata>" +
    		"</transaction_item>\n", Utils.read(new File(transactionLog.transactionLogDir, tsFiles.get(0))));
    	assertEquals("<transaction_item>" +
				"<action>add</action>" +
				"<identifier>test2.rdf</identifier>" +
				"<filedata>" + filedata + "</filedata>" +
			"</transaction_item>\n", Utils.read(new File(transactionLog.transactionLogDir, tsFiles.get(1))));
    	assertEquals("<transaction_item>" +
				"<action>add</action>" +
				"<identifier>test3.rdf</identifier>" +
				"<filedata>" + filedata + "</filedata>" +
			"</transaction_item>\n", Utils.read(new File(transactionLog.transactionLogDir, tsFiles.get(2))));
    	assertEquals("", Utils.read(new File(transactionLog.transactionLogDir, tsFiles.get(3))));
    }

    @Test
    public void testRecoverAfterCrashWhileInCommittingState() throws Exception {
        String filedata = Base64.encodeBase64String("ignored".getBytes());
    	String currentData = "<transaction_item>\n" +
		"    <action>add</action>\n" +
		"    <identifier>test1.rdf</identifier>\n" +
		"    <filedata>" + filedata + "</filedata>\n" +
    	"</transaction_item>\n" +
    	"<transaction_item>\n" +
		"    <action>add</action>";
    	Utils.write(transactionLog.transactionLogFilePath, currentData);
    	transactionLog.committedFilePath.renameTo(transactionLog.committingFilePath);
    	setTransactionLog(1.0/1024/1024*5);
    	String[] expected = {"add:test1.rdf|ignored|RDF/XML", "shutdown", "startup"};
    	assertArrayEquals(expected, tsMock.actions.toArray());
    }

    @Test
    public void testRecoverAfterCrashWhileInCommittedState() throws Exception {
        String filedata = Base64.encodeBase64String("ignored".getBytes());
    	String currentData = "<transaction_item>\n" +
		"    <action>add</action>\n" +
		"    <identifier>test1.rdf</identifier>\n" +
		"    <filedata>" + filedata + "</filedata>\n" +
    	"</transaction_item>\n" +
    	"<transaction_item>\n" +
		"    <action>add</action>";
    	Utils.write(transactionLog.transactionLogFilePath, currentData);
    	try {
    		transactionLog = new TransactionLog(tsMock, tempdir);
        	fail("commited state with incomplete transactionLog should fail");
    	} catch (Exception e) {
    		assertEquals("Last TransactionLog item is incomplete while not in the committing state. This should never occur.", e.getMessage());
    	}
    	String[] expected = {"add:test1.rdf|ignored|RDF/XML"};
    	assertArrayEquals(expected, tsMock.actions.toArray());
    	assertTrue(transactionLog.committedFilePath.isFile());
    }

    @Test
    public void testRecoverMultipleFiles() throws Exception {
    	setTransactionLog(1.0/1024/1024*5);
    	transactionLog.add("test1.rdf", "ignored", RDFFormat.RDFXML);
        Thread.sleep(1);
    	transactionLog.add("test2.rdf", "ignored", RDFFormat.RDFXML);
        Thread.sleep(1);
    	transactionLog.add("test3.rdf", "ignored", RDFFormat.RDFXML);

        setTransactionLog(1.0/1024/1024*5);
    	String[] expected = {"add:test1.rdf|ignored|RDF/XML", "shutdown", "startup", "add:test2.rdf|ignored|RDF/XML", "shutdown", "startup", "add:test3.rdf|ignored|RDF/XML", "shutdown", "startup"};
        assertArrayEquals(expected, tsMock.actions.toArray());
    }

    @Test
    public void testBigCurrentFileInTransactionLog() throws Exception {
    	String filedata = StringUtils.repeat(StringUtils.repeat("ignored", 1024), 100);
    	String tsItem = "<transaction_item>\n" +
		"    <action>add</action>\n" +
		"    <identifier>test1.rdf</identifier>\n" +
		"    <filedata>" + filedata + "</filedata>\n" +
    	"</transaction_item>\n";
    	FileWriter fw = new FileWriter(transactionLog.transactionLogFilePath);
    	for (int i = 0; i < 100; i++) {
    		fw.write(tsItem);
    	}
		fw.close();
		class MyTSMock extends TSMock {
			public void add(String identifier, String data) {
		        actions.add("add:" + identifier);
		    }
		}
		tsMock = new MyTSMock();
		transactionLog = new TransactionLog(tsMock, tempdir);
        transactionLog.init();
        assertEquals(StringUtils.repeat(".", 50) + " 50Mb", stdout.toString(), stdout.toString());
    }

    @Test
    public void testImportTrig() throws Exception {
    	transactionLog.importTrig("<uri:subj> <uri:pred> <uri:obj>;");
    	assertEquals(0, transactionLog.getTransactionItemFiles().size());
    	assertEquals(3, tsMock.actions.size());
    	assertEquals("import:<uri:subj> <uri:pred> <uri:obj>;", tsMock.actions.get(0));
    	assertEquals("shutdown", tsMock.actions.get(1));
    	assertEquals("startup", tsMock.actions.get(2));
    	assertEquals(0, transactionLog.getTransactionItemFiles().size());
    }

    private void addFilesToTransactionLog() throws Exception {
        transactionLog.add("testRecord.rdf", "<x>ignored</x>", RDFFormat.RDFXML);
        transactionLog.delete("testRecord.rdf");
    }

    private int countTransactionItems(String filename) throws IOException {
    	return Utils.read(new File(transactionLog.getTransactionLogDir(), filename)).split("<transaction_item>").length - 1;
    }
}
