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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import static org.meresco.owlimhttpserver.Utils.createTempDirectory;
import static org.meresco.owlimhttpserver.Utils.deleteDirectory;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.LiteralImpl;


public class TransactionLogTest {
    TransactionLog transactionLog;
    File tempdir;

    @Before
    public void setUp() throws Exception {
        tempdir = createTempDirectory();
        transactionLog = new TransactionLog(tempdir);
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
                "<filedata>" + xml + "</filedata>" +
            "</transaction_item>";
        FileInputStream fileInputStream = new FileInputStream(new File(transactionLog.getTransactionLogDir(), files[0]));
        assertEquals(expectedXml, Utils.read(fileInputStream));
    }

    @Test
    public void testCheckIsAddedToLogWhenExists() {
        File filenameTransactionLog;
        String filename = String.valueOf(System.currentTimeMillis());
        filenameTransactionLog = transactionLog.prepare("addRDF", "testRecord", filename, "<x>ignored</x>");
        assertEquals(filename, filenameTransactionLog.getName());
        filenameTransactionLog = transactionLog.prepare("addRDF", "testRecord", filename, "<x>ignored</x>");
        assertEquals(filename + "_0", filenameTransactionLog.getName());
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
    }
}
