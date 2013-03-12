/* begin license *
 *
 * The Meresco Owlim package consists out of a HTTP server written in Java that
 * provides access to an Owlim Triple store, as well as python bindings to
 * communicate as a client with the server.
 *
 * Copyright (C) 2011-2013 Seecr (Seek You Too B.V.) http://seecr.nl
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

import java.io.File;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

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
        String encodeFiledata = Base64.encodeBase64String(filedata.getBytes());
        String tsItem = "<transaction_item>" +
            "<action>" + action + "</action>" +
            "<identifier>" + identifier + "</identifier>" + 
            "<filedata>" + encodeFiledata + "</filedata>" +
            "</transaction_item>";

        TransactionItem transactionItem = TransactionItem.read(tsItem);
        assertEquals(action, transactionItem.getAction());
        assertEquals(identifier, transactionItem.getIdentifier());
        assertEquals(filedata, transactionItem.getFiledata());
    } 
}
