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

import java.io.File;
import java.nio.charset.Charset;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;

public class OwlimServer {
    public static void main(String[] args) throws Exception {
        Integer port; 
        String storeLocation;
        String storeName;
        Option option;

        Options options = new Options();

        // Port Number
        option = new Option("p", "port", true, "Port number");
        option.setType(Integer.class);
        option.setRequired(true);
        options.addOption(option);

        // Triplestore name
        option = new Option("n", "name", true, "Name of the triplestore");
        option.setType(String.class);
        option.setRequired(true);
        options.addOption(option);

        // Triplestore location
        option = new Option("d", "stateDir", true, "Directory in which triplestore is located");
        option.setType(String.class);
        option.setRequired(true);
        options.addOption(option);

        PosixParser parser = new PosixParser();
        CommandLine commandLine = null;
        try {
            commandLine = parser.parse(options, args);
        } catch (MissingOptionException e) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("start-owlimhttpserver" , options);
            System.exit(1);
        }

        port = new Integer(commandLine.getOptionValue("p"));
        storeLocation = commandLine.getOptionValue("d");
        storeName = commandLine.getOptionValue("n");
        
        if (Charset.defaultCharset() != Charset.forName("UTF-8")) {
        	System.err.println("file.encoding must be UTF-8.");
            System.exit(1);
        }

        OwlimTripleStore tripleStore = new OwlimTripleStore(new File(storeLocation), storeName);
        TransactionLog transactionLog = new TransactionLog(tripleStore, new File(storeLocation));
        transactionLog.init();
        OwlimHttpHandler handler = new OwlimHttpHandler(transactionLog, tripleStore);
        OwlimHttpServer httpServer = new OwlimHttpServer(port, 15);

        registerShutdownHandler(tripleStore, transactionLog);

        System.out.println("Triplestore started with " + String.valueOf(tripleStore.size()) + " statements");
        System.out.flush();

        httpServer.setHandler(handler);
        httpServer.start();
    }

    static void registerShutdownHandler(final TripleStore tripleStore, final TransactionLog transactionLog) {
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                System.out.println("Shutting down triplestore. Please wait...");
                try {
                    tripleStore.shutdown();
                    transactionLog.clear();
                    System.out.println("Shutdown completed.");
                    System.out.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.flush();
                    System.out.println("Shutdown failed.");
                    System.out.flush();
                } 
            }
        });        
    }
}
