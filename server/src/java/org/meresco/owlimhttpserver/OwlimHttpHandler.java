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
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.repository.RepositoryException;
import org.openrdf.query.MalformedQueryException;


public class OwlimHttpHandler implements HttpHandler {
    TransactionLog transactionLog;
    TripleStore tripleStore;
    RdfValidator validator;
    List<String> allowed_contenttypes;

    public OwlimHttpHandler(TransactionLog transactionLog, TripleStore tripleStore) {
        this.transactionLog = transactionLog;
        this.tripleStore = tripleStore;
        this.validator = new RdfValidator();
    }

    public void handle(HttpExchange exchange) throws IOException {
        OutputStream outputStream = exchange.getResponseBody();
        URI requestURI = exchange.getRequestURI();
        String path = requestURI.getPath();
        String rawQueryString = requestURI.getRawQuery();

        try {
            QueryParameters queryParameters = Utils.parseQS(rawQueryString);
            if ("/add".equals(path)) {
                String body = Utils.read(exchange.getRequestBody());
                try {
                    addRDF(queryParameters, body);
                } catch (RDFParseException e) {
                    exchange.sendResponseHeaders(400, 0);
                    _writeResponse(e.toString(), outputStream);
                    return;
                }
            }
            else if ("/update".equals(path)) {
                String body = Utils.read(exchange.getRequestBody());
                try {
                    updateRDF(queryParameters, body);
                } catch (RDFParseException e) {
                    exchange.sendResponseHeaders(400, 0);
                    _writeResponse(e.toString(), outputStream);
                    return;
                }
            }
            else if ("/delete".equals(path)) {
                deleteRDF(queryParameters);
            }
            else if ("/addTriple".equals(path)) {
                String body = Utils.read(exchange.getRequestBody());
                addTriple(body);
            }
            else if ("/removeTriple".equals(path)) {
                String body = Utils.read(exchange.getRequestBody());
                removeTriple(body);
            }
            else if ("/query".equals(path)) {
                String response = "";
                Headers requestHeaders = exchange.getRequestHeaders();
                TupleQueryResultFormat resultFormat = TupleQueryResultFormat.JSON;
                if (queryParameters.containsKey("mimeType")) {
                    resultFormat = TupleQueryResultFormat.forMIMEType(queryParameters.singleValue("mimeType"));
                }
                else if (requestHeaders.containsKey("Accept")) {
                    resultFormat = TupleQueryResultFormat.forMIMEType(requestHeaders.getFirst("Accept"));
                    if (resultFormat == null) {
                        String responseBody = "Supported formats:\n";
                        Iterator<TupleQueryResultFormat> i = TupleQueryResultFormat.values().iterator();
                        while (i.hasNext()) {
                            responseBody += "- " + i.next() + "\n";
                        }
                        Headers responseHeaders = exchange.getResponseHeaders();
                        responseHeaders.add("Content-Type", "text/plain");
                        exchange.sendResponseHeaders(406, 0);
                        _writeResponse(responseBody, outputStream);
                        return;
                    }
                }
                try {
                    response = executeQuery(queryParameters, resultFormat);
                    if (queryParameters.containsKey("outputContentType")) {
                        exchange.getResponseHeaders().set("Content-Type", queryParameters.singleValue("outputContentType"));
                    }
                    else {
                        exchange.getResponseHeaders().set("Content-Type", resultFormat.getMIMETypes().get(0));
                    }
                    exchange.sendResponseHeaders(200, 0);
                    _writeResponse(response, outputStream);
                } catch (MalformedQueryException e) {
                    exchange.sendResponseHeaders(400, 0);
                    _writeResponse(e.toString(), outputStream);
                }
                return;
            }
            else if ("/sparql".equals(path)) {
                String response = sparqlForm(queryParameters);
                Headers headers = exchange.getResponseHeaders();
                headers.set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, 0);
                _writeResponse(response, outputStream);
            }
            else if ("/validate".equals(path)) {
                String body = Utils.read(exchange.getRequestBody());
                exchange.sendResponseHeaders(200, 0);
                try {
                    validateRDF(queryParameters, body);
                    _writeResponse("Ok", outputStream);
                } catch (RDFParseException e) {
                    _writeResponse("Invalid\n" + e.toString(), outputStream);
                }
            }
            else if ("/export".equals(path)) {
                export(queryParameters);
            }
            else if ("/import".equals(path)) {
                String body = Utils.read(exchange.getRequestBody());
                importTrig(body);
            }
            else {
                exchange.sendResponseHeaders(404, 0);
                return;
            }
            exchange.sendResponseHeaders(200, 0);
        } catch (IllegalArgumentException e) {
            exchange.sendResponseHeaders(400, 0);
            _writeResponse(e.toString(), outputStream);
        } catch (RuntimeException e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, 0);
            String response = Utils.getStackTrace(e);
            //System.out.println(response);
            _writeResponse(response, outputStream);
            return;
        } catch (Error e) {
            e.printStackTrace();
        	exchange.sendResponseHeaders(500, 0);
        	_writeResponse(e.getMessage(), outputStream);
        	throw e;
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

    public synchronized void updateRDF(QueryParameters params, String httpBody) throws RDFParseException {
        String identifier = params.singleValue("identifier");
        transactionLog.delete(identifier);
        transactionLog.add(identifier, httpBody);
    }

    public synchronized void addRDF(QueryParameters params, String httpBody) throws RDFParseException {
        String identifier = params.singleValue("identifier");
        transactionLog.add(identifier, httpBody);
    }

    public synchronized void addTriple(String httpBody) {
        transactionLog.addTriple(httpBody);
    }

    public synchronized void deleteRDF(QueryParameters params) {
        String identifier = params.singleValue("identifier");
        transactionLog.delete(identifier);
    }

    public synchronized void removeTriple(String httpBody) {
        transactionLog.removeTriple(httpBody);
    }

    public String executeQuery(QueryParameters params, TupleQueryResultFormat resultFormat) throws MalformedQueryException {
        String query = params.singleValue("query");
        if (query == null) {
            return "";
        }
        return tripleStore.executeQuery(query, resultFormat);
    }

    public void validateRDF(QueryParameters params, String httpBody) throws RDFParseException {
        validator.validate(httpBody);
    }

    public void export(QueryParameters params) {
        String identifier = params.singleValue("identifier");
        tripleStore.export(identifier);
    }

    public synchronized void importTrig(String trig) {
        tripleStore.importTrig(trig);
        restartTripleStore();
    }

    private void restartTripleStore() {
        System.out.println("Restarting triplestore. Please wait...");
        try {
            tripleStore.shutdown();
            transactionLog.clear();
            tripleStore.startup();
            System.out.println("Restart completed.");
            System.out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.flush();
            System.out.println("Restart failed.");
            System.out.flush();
            throw new RuntimeException(e);
        }
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
            + "<input type=\"hidden\" name=\"outputContentType\" value=\"application/json\"/>\n"
            + "Format: <select name=\"mimeType\">\n"
            + "<option value=\"application/sparql-results+json\">json</option>\n"
            + "</select><br />\n"
            + "<input type=\"submit\">\n"
            + "</form>\n</body></html>";
    }
}
