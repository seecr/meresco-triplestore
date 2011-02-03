package org.meresco.owlimhttpserver;

import java.io.File;
import java.io.IOException;

public class OwlimServer {
    public static void main(String[] args) throws Exception {
        Integer port; 
        String storeLocation;
        String storeName;

        try {
            port = new Integer(args[0]);
            storeLocation = args[1];
            storeName = args[2];
        } catch(Exception e) {
            System.out.println("Arguments: <port> <storeLocation> <storeName>");
            return;
        }

        File storeLocationFile = new File(storeLocation);
        TripleStore tripleStore = new OwlimTripleStore(storeLocationFile, storeName);
        OwlimHttpHandler handler = new OwlimHttpHandler(tripleStore);
        OwlimHttpServer httpServer = new OwlimHttpServer(port, 15);

        httpServer.setHandler(handler);
        httpServer.start();
    }
}
