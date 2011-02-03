package org.meresco.owlimhttpserver;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

import java.net.InetSocketAddress;

public class OwlimHttpServer {
    private HttpServer server = null;

    public OwlimHttpServer(int port, int backlog) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), backlog);
        server.setExecutor(null);
    }

    public void setHandler(HttpHandler handler) {
        server.createContext("/", handler);
    }

    public void start() {
        server.start();
    }
}
