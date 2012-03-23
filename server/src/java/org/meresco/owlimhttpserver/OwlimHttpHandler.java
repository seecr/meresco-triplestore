/* begin license *
 * 
 * The Meresco Owlim package consists out of a HTTP server written in Java that
 * provides access to an Owlim Triple store, as well as python bindings to
 * communicate as a client with the server. 
 * 
 * Copyright (C) 2011 Seecr (Seek You Too B.V.) http://seecr.nl
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

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.net.URI;

public class OwlimHttpHandler implements HttpHandler {
    TransactionLog transactionLog;
    TripleStore tripleStore;

    public OwlimHttpHandler(TransactionLog transactionLog, TripleStore tripleStore) {
        this.transactionLog = transactionLog;
        this.tripleStore = tripleStore;
    }

    public void handle(HttpExchange exchange) throws IOException {
        URI requestURI = exchange.getRequestURI();
        String path = requestURI.getPath();
        QueryParameters queryParameters = Utils.parseQS(requestURI.getQuery());
        OutputStream outputStream = exchange.getResponseBody();
       
        try {
            try {
                
                if (path.equals("/add")) {
                    String body = Utils.read(exchange.getRequestBody());
                    addRDF(queryParameters, body);
                } else if (path.equals("/update")) {
                    String body = Utils.read(exchange.getRequestBody());
                    updateRDF(queryParameters, body);
                } else if (path.equals("/delete")) {
                    deleteRDF(queryParameters);
                } else if (path.equals("/query")) {
                    String response = "";
                    response = executeQuery(queryParameters);
                    exchange.sendResponseHeaders(200, 0);
                    _writeResponse(response, outputStream);
                    return;
                } else {
                    exchange.sendResponseHeaders(404, 0);
                    return;
                }
            } catch (Exception e) {
                exchange.sendResponseHeaders(500, 0);
                String response = Utils.getStackTrace(e);
                _writeResponse(response, outputStream);
                return;

            }

            exchange.sendResponseHeaders(200, 0);
        } finally {
            exchange.close();
        }
    }

    private void _writeResponse(String response, OutputStream stream) {
        try {
            Writer writer = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"));
            writer.write(response, 0, response.length());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateRDF(QueryParameters params, String httpBody) throws TransactionLogException, IOException {
        String identifier = params.singleValue("identifier");
        transactionLog.delete(identifier);
        transactionLog.add(identifier, httpBody);
    }

    public void addRDF(QueryParameters params, String httpBody) throws TransactionLogException, IOException {
        String identifier = params.singleValue("identifier");
        transactionLog.add(identifier, httpBody);
    }

    public void deleteRDF(QueryParameters params) throws TransactionLogException, IOException {
        String identifier = params.singleValue("identifier");
        transactionLog.delete(identifier);
    }

    public String executeQuery(QueryParameters params) {
        String query = params.singleValue("query");
        return tripleStore.executeQuery(query);
    }
}
