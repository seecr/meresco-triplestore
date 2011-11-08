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
import java.io.BufferedWriter;
import java.lang.RuntimeException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.xml.sax.SAXException;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import static org.meresco.owlimhttpserver.Utils.createTempDirectory;
import static org.meresco.owlimhttpserver.Utils.deleteDirectory;

public class TransactionItemTest {
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
    public void testReadTransactionItem() throws Exception {
        String action = "addRDF";
        String identifier = "record1";
        String filedata = "<x>ignored</x>";
        File filepath = new File(tempdir, identifier);

        FileWriter fstream = new FileWriter(filepath);
        BufferedWriter out = new BufferedWriter(fstream);
        out.write("<transaction_item>" +
            "<action>" + action + "</action>" +
            "<identifier>" + identifier + "</identifier>" + 
            "<filedata>" + StringEscapeUtils.escapeXml(filedata) + "</filedata>" +
            "</transaction_item>");
        out.close();

        TransactionItem transactionItem = TransactionItem.read(filepath);
        assertEquals(action, transactionItem.getAction());
        assertEquals(identifier, transactionItem.getIdentifier());
        assertEquals(filedata, transactionItem.getFiledata());
    } 
}
