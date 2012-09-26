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

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import java.io.IOException;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.net.URI;

import org.apache.commons.lang3.StringEscapeUtils;

import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.model.Namespace;
import org.openrdf.repository.RepositoryException;

public class OwlimHttpHandler implements HttpHandler {
    TransactionLog transactionLog;
    TripleStore tripleStore;
    RdfValidator validator;
    List<String> allowed_contenttypes;

    public OwlimHttpHandler(TransactionLog transactionLog, TripleStore tripleStore) {
        this.transactionLog = transactionLog;
        this.tripleStore = tripleStore;
        this.validator = new RdfValidator();
        this.allowed_contenttypes = Arrays.asList(Consts.JSON);
    }

    public void handle(HttpExchange exchange) throws IOException {
        URI requestURI = exchange.getRequestURI();
        String path = requestURI.getPath();
        String rawQueryString = requestURI.getRawQuery();
        QueryParameters queryParameters = Utils.parseQS(rawQueryString);
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
                    Headers requestHeaders = exchange.getRequestHeaders();
                    String contentType = requestHeaders.containsKey("Accept") ? requestHeaders.getFirst("Accept") : Consts.JSON;
                    if (!this.allowed_contenttypes.contains(contentType)) {
                        String responseBody = "Supported formats:\n";
                        Iterator<String> i = this.allowed_contenttypes.iterator();
                        while (i.hasNext()) {
                            responseBody += "- " + i.next() + "\n";
                        }
                        Headers responseHeaders = exchange.getResponseHeaders();
                        responseHeaders.add("Content-Type", "text/plain");
                        exchange.sendResponseHeaders(406, 0);
                        _writeResponse(responseBody, outputStream);
                        return;
                    }
                    response = executeQuery(queryParameters, contentType);
                    exchange.sendResponseHeaders(200, 0);
                    _writeResponse(response, outputStream);
                    return;
                } else if (path.equals("/sparql")) {
                    String response = sparqlForm(queryParameters);
                    Headers headers = exchange.getResponseHeaders();
                    headers.set("Content-Type", "text/html");
                    exchange.sendResponseHeaders(200, 0);
                    _writeResponse(response, outputStream);
                } else if (path.equals("/validate")) {
                    String body = Utils.read(exchange.getRequestBody());
                    exchange.sendResponseHeaders(200, 0);
                    try {
                        validateRDF(queryParameters, body);
                        _writeResponse("Ok", outputStream);
                    } catch (RDFParseException e) {
                        _writeResponse("Invalid\n" + e.toString(), outputStream);
                    }
                } else {
                    exchange.sendResponseHeaders(404, 0);
                    return;
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, 0);
                String response = Utils.getStackTrace(e);
                _writeResponse(response, outputStream);
                return;
            } catch (Error e) {
                e.printStackTrace();
            	exchange.sendResponseHeaders(500, 0);
            	_writeResponse(e.getMessage(), outputStream);
            	throw e;
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
        return this.executeQuery(params, Consts.JSON);
    }

    public String executeQuery(QueryParameters params, String contentType) {
        String query = params.singleValue("query");
        String format = params.singleValue("format");
        TupleQueryResultFormat resultFormat = TupleQueryResultFormat.JSON;
        if (format != null && Arrays.asList("sparql", "xml", "application/sparql-results+xml").contains(format.toLowerCase())) {
            resultFormat = TupleQueryResultFormat.SPARQL;
        }
        System.out.println(tripleStore);
        return tripleStore.executeQuery(query, resultFormat);
    }

    public void validateRDF(QueryParameters params, String httpBody) throws RDFParseException {
        validator.validate(httpBody);
    }

    public String sparqlForm(QueryParameters params) {
        String query;
        if (params.containsKey("query")) {
            query = params.singleValue("query");
        } else {
            query = "";
            for (Namespace namespace : tripleStore.getNamespaces()) {
                query += "PREFIX " + namespace.getPrefix() + ": <" + namespace.getName() + ">\n";
            }
            query += "\nSELECT ?subject ?predicate ?object\n";
            query += "WHERE { ?subject ?predicate ?object }\n";
            query += "LIMIT 50";
        }
        return "<html><head><title>Meresco Owlim Sparql Form</title></head>\n"  
            + "<body><form action=\"/query\">\n"
            + "<textarea cols=\"100\" rows=\"20\" name=\"query\">" + StringEscapeUtils.escapeXml(query) + "</textarea><br/>\n"
            + "<input type=\"submit\">\n"
            + "</form>\n</body></html>";
    }
}
