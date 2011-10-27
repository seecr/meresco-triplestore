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

import java.io.File;
import java.io.IOException;

public class OwlimServer {
    public static void main(String[] args) throws Exception {
        Integer port; 
        String storeLocation;
        String storeName;
        String rdfDirectory;

        try {
            port = new Integer(args[0]);
            storeLocation = args[1];
            storeName = args[2];
            rdfDirectory = args[3];
        } catch(Exception e) {
            System.out.println("Arguments: <port> <storeLocation> <storeName> <rdfFileDirectory>");
            return;
        }

        TripleStore tripleStore = new OwlimTripleStore(new File(storeLocation), storeName, new File(rdfDirectory));
        TransactionLog transactionLog = new TransactionLog(tripleStore, new File(storeLocation, "transactionLog"));
        OwlimHttpHandler handler = new OwlimHttpHandler(transactionLog, tripleStore);
        OwlimHttpServer httpServer = new OwlimHttpServer(port, 15);

        httpServer.setHandler(handler);
        httpServer.start();
    }
}
