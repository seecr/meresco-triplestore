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

import com.sun.net.httpserver.HttpHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import java.io.IOException;

import java.net.InetSocketAddress;


public class HttpServer {
    private com.sun.net.httpserver.HttpServer server = null;

    public HttpServer(int port, int backlog) throws IOException {
        server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(port), backlog);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(50, 200, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1000));
        server.setExecutor(executor);  // Note: so far not been able to express this multithreaded capability in a (unit)test
    }

    public void setHandler(HttpHandler handler) {
        server.createContext("/", handler);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(1);
    }
}
