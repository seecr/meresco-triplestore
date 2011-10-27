/* begin license *
 * 
 * "OwlimHttpServer" provides a simple HTTP interface to an OWLim triplestore. 
 * 
 * Copyright (C) 2011 Seecr (Seek You Too B.V.) http://seecr.nl
 * Copyright (C) 2011 Seek You Too B.V. (CQ2) http://www.cq2.nl
 * 
 * This file is part of "OwlimHttpServer"
 * 
 * "OwlimHttpServer" is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * "OwlimHttpServer" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with "OwlimHttpServer"; if not, write to the Free Software
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

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import static org.meresco.owlimhttpserver.Utils.createTempDirectory;
import static org.meresco.owlimhttpserver.Utils.deleteDirectory;

public class TransactionLogTest {
    TransactionLog transactionLog;
    File tempdir;
    TripleStore tsMock;

    @Before
    public void setUp() throws Exception {
        tempdir = createTempDirectory();
        tsMock = new TSMock();
        transactionLog = new TransactionLog(tsMock, tempdir);
    }

    @After
    public void tearDown() throws Exception {
        deleteDirectory(tempdir);
    }

    @Test
    public void testAddToTransactionLog() {
        String filename = String.valueOf(System.currentTimeMillis());
        transactionLog.prepare("addRDF", "testRecord", filename, "<x>ignored</x>");
        assertTrue(new File(transactionLog.getTempLogDir().toString(), filename).isFile());
    }

    @Test
    public void testPrepareWithSameFilename() {
        String filename = String.valueOf(System.currentTimeMillis());
        transactionLog.prepare("addRDF", "testRecord", filename, "<x>ignored</x>");
        transactionLog.prepare("addRDF", "testRecord", filename, "<x>ignored</x>");
        assertEquals(2, this.transactionLog.getTempLogDir().list().length);
    }

    @Test
    public void testCommitToTransactionLog() throws FileNotFoundException {
        String filename = String.valueOf(System.currentTimeMillis());
        String xml = "<x>ignored</x>";
        transactionLog.prepare("addRDF", "testRecord", filename, xml);
        transactionLog.commit(filename);
        String[] files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.length);
        assertEquals(filename, files[0]);
        String expectedXml = "<transaction_item>" +
                "<action>addRDF</action>" +
                "<identifier>testRecord</identifier>" + 
                "<filedata>&lt;x&gt;ignored&lt;/x&gt;</filedata>" +
            "</transaction_item>";
        FileInputStream fileInputStream = new FileInputStream(new File(transactionLog.getTransactionLogDir(), files[0]));
        assertEquals(expectedXml, Utils.read(fileInputStream));
    }

    @Test
    public void testCheckIsAddedToLogWhenExists() {
        String filenameTransactionLog;
        String filename = String.valueOf(System.currentTimeMillis());
        filenameTransactionLog = transactionLog.prepare("addRDF", "testRecord", filename, "<x>ignored</x>");
        assertEquals(filename, filenameTransactionLog);
        filenameTransactionLog = transactionLog.prepare("addRDF", "testRecord", filename, "<x>ignored</x>");
        assertEquals(filename + "_1", filenameTransactionLog);
        filenameTransactionLog = transactionLog.prepare("addRDF", "testRecord", filename, "<x>ignored</x>");
        assertEquals(filename + "_1_1", filenameTransactionLog);
        transactionLog.commit(filename);
        String[] files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.length);
        assertEquals(filename, files[0]);
    }

    @Test 
    public void testCommitWithExistingName() {
        String filename = String.valueOf(System.currentTimeMillis());
        transactionLog.prepare("addRDF", "testRecord", filename, "<x>ignored</x>");
        transactionLog.commit(filename);
        transactionLog.prepare("addRDF", "testRecord", filename, "<x>ignored</x>");
        transactionLog.commit(filename);
        transactionLog.prepare("addRDF", "testRecord", filename, "<x>ignored</x>");
        transactionLog.commit(filename);
        String[] files = transactionLog.getTransactionItemFiles();
        assertEquals(3, files.length);
        assertEquals(filename, files[0]);
        assertEquals(filename + "_1", files[1]);
        assertEquals(filename + "_1_1", files[2]);
    }

    @Test
    public void testEscapeIdentifier() throws FileNotFoundException {
        String filename = String.valueOf(System.currentTimeMillis());
        transactionLog.prepare("addRDF", "<testRecord>", filename, "<x>ignored</x>");
        String expectedXml = "<transaction_item>" +
                "<action>addRDF</action>" +
                "<identifier>&lt;testRecord&gt;</identifier>" + 
                "<filedata>&lt;x&gt;ignored&lt;/x&gt;</filedata>" +
            "</transaction_item>";
        FileInputStream fileInputStream = new FileInputStream(new File(transactionLog.getTempLogDir(), filename));
        assertEquals(expectedXml, Utils.read(fileInputStream));
    }

    @Test
    public void testAddSuccesfullRecord() throws FileNotFoundException, TransactionLogException {
        transactionLog.add("record", "<x>ignored</x>");
        String[] files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.length);
        String expectedXml = "<transaction_item>" +
                "<action>addRDF</action>" +
                "<identifier>record</identifier>" + 
                "<filedata>&lt;x&gt;ignored&lt;/x&gt;</filedata>" +
            "</transaction_item>";
        FileInputStream fileInputStream = new FileInputStream(new File(transactionLog.getTransactionLogDir(), files[0]));
        assertEquals(expectedXml, Utils.read(fileInputStream));
    }

    @Test
    public void testAddDeleteRecord() throws FileNotFoundException, TransactionLogException {
        transactionLog.delete("record");
        String[] files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.length);
        String expectedXml = "<transaction_item>" +
                "<action>delete</action>" +
                "<identifier>record</identifier>" + 
                "<filedata></filedata>" +
            "</transaction_item>";
        FileInputStream fileInputStream = new FileInputStream(new File(transactionLog.getTransactionLogDir(), files[0]));
        assertEquals(expectedXml, Utils.read(fileInputStream));
    }

    @Test
    public void testAddNotWhenFailed() {
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
        assertEquals(0, files.length);
    }

    @Test
    public void testAddWhenAlreadyExistsInTemp() throws FileNotFoundException, TransactionLogException{
        final String time = String.valueOf(System.currentTimeMillis());
        class MyTransactionLog extends TransactionLog {
            public MyTransactionLog(TripleStore tripleStore, File baseDir) {
                super(tripleStore, baseDir);
            }
            String getTime() {
                return time;
            }
        }
        transactionLog = new MyTransactionLog(tsMock, tempdir);
        transactionLog.prepare("addRDF", "testRecord", time, "<x>ignored</x>");

        transactionLog.add("testRecord", "data");
        String[] files = transactionLog.getTransactionItemFiles();
        assertEquals(1, files.length);
        FileInputStream fileInputStream = new FileInputStream(new File(transactionLog.getTransactionLogDir(), files[0]));
        assertTrue(Utils.read(fileInputStream).contains("data"));
    }

    @Test
    public void testRollback() {
        String filename = String.valueOf(System.currentTimeMillis());
        transactionLog.prepare("addRDF", "record1", filename, "data");
        transactionLog.prepare("addRDF", "record1", filename + "_1", "data");
        transactionLog.rollback(filename);
        String[] tmpFiles = transactionLog.getTempLogDir().list();
        String[] expected = {filename + "_1"};
        assertArrayEquals(expected, tmpFiles); 
    }

    @Test
    public void testRollbackNothingToDo() {
        String filename = String.valueOf(System.currentTimeMillis());
        transactionLog.prepare("addRDF", "record1", filename + "_1", "data");
        transactionLog.rollback(filename);
        String[] tmpFiles = transactionLog.getTempLogDir().list();
        String[] expected = {filename + "_1"};
        assertArrayEquals(expected, tmpFiles); 
    }

    @Test
    public void testRollbackWhenPrepareFailes() {
        final List<Boolean> rollback = new ArrayList<Boolean>();
        class MyTransactionLog extends TransactionLog {
            public MyTransactionLog(TripleStore tripleStore, File baseDir) {
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
            transactionLog.add("record", "data");
            fail("Should raise an Exception");
        } catch (Exception e) {}
        assertTrue(rollback.get(0));
    }
}
