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
import java.nio.charset.Charset;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;

public class OwlimServer {
    public static void main(String[] args) throws Exception {
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

        option = new Option(null, "disableTransactionLog", false, "Disable use of transactionlog; Server must be shutdown to persist.");
        option.setType(Boolean.class);
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

        Integer port = new Integer(commandLine.getOptionValue("p"));
        String storeLocation = commandLine.getOptionValue("d");
        String storeName = commandLine.getOptionValue("n");
        Boolean disableTransactionLog = commandLine.hasOption("disableTransactionLog");

        if (Charset.defaultCharset() != Charset.forName("UTF-8")) {
        	System.err.println("file.encoding must be UTF-8.");
            System.exit(1);
        }

        TripleStore tripleStore = new OwlimTripleStore(new File(storeLocation), storeName);
        if (!disableTransactionLog) {
        	tripleStore = new TripleStoreTx(tripleStore, new File(storeLocation));
        }
        OwlimHttpHandler handler = new OwlimHttpHandler(tripleStore);
        OwlimHttpServer httpServer = new OwlimHttpServer(port, 15);

        registerShutdownHandler(tripleStore);

        System.out.println("Triplestore started with " + String.valueOf(tripleStore.size()) + " statements");
        System.out.flush();

        httpServer.setHandler(handler);
        httpServer.start();
    }

    static void registerShutdownHandler(final TripleStore tripleStore) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run()
            {
                System.out.println("Shutting down triplestore. Please wait...");
                try {
                    tripleStore.shutdown();
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
