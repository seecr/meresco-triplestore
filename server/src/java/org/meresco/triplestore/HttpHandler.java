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
import java.util.concurrent.ExecutorService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;

import java.net.URI;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.model.Namespace;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.repository.RepositoryException;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;

import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.ParsedGraphQuery;
import org.openrdf.query.parser.QueryParserUtil;

import org.openrdf.rio.Rio;
import org.openrdf.rio.RDFFormat;


public class HttpHandler extends AbstractHandler {
    Triplestore tripleStore;
    RdfValidator validator;
    List<String> allowed_contenttypes;

    public HttpHandler(Triplestore tripleStore) {
        this.tripleStore = tripleStore;
        this.validator = new RdfValidator();
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        System.out.println(target);
//        OutputStream outputStream = exchange.getResponseBody();
//        URI requestURI = exchange.getRequestURI();
        String path = request.getRequestURI();
//        String rawQueryString = requestURI.getRawQuery();
//        String body = Utils.read(exchange.getRequestBody());
//    	Headers requestHeaders = exchange.getRequestHeaders();
//
        try {
//            QueryParameters httpArguments = Utils.parseQS(rawQueryString);
            if ("/add".equals(path)) {
                try {
                    addData(request);
                } catch (RDFParseException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(e.toString());
                    return;
                }
            }
//            else if ("/update".equals(path)) {
//                try {
//                    updateData(httpArguments, requestHeaders, body);
//                } catch (RDFParseException e) {
//                    exchange.sendResponseHeaders(400, 0);
//                    _writeResponse(e.toString(), outputStream);
//                    return;
//                }
//            }
//            else if ("/delete".equals(path)) {
//                deleteData(httpArguments);
//            }
//            else if ("/addTriple".equals(path)) {
//                addTriple(body);
//            }
//            else if ("/removeTriple".equals(path)) {
//                removeTriple(body);
//            }
//            else if ("/query".equals(path)) {
//                String response = "";
//                if(exchange.getRequestMethod().equals("POST"))
//                	httpArguments.putAll(Utils.parseQS(body));
//                Headers responseHeaders = exchange.getResponseHeaders();
//                try {
//                    long start = System.currentTimeMillis();
//                    String query = httpArguments.singleValue("query");
//                    List<String> responseTypes = getResponseTypes(requestHeaders, httpArguments);
//                    if (query != null) {
//                        ParsedQuery p = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);
//                        if (p instanceof ParsedGraphQuery) {
//                            response = executeGraphQuery(query, responseTypes, responseHeaders);
//                        } else {
//                            response = executeTupleQuery(query, responseTypes, responseHeaders);
//                        }
//                    }
//                    long indexQueryTime = System.currentTimeMillis() - start;
//                    if (response == null || response == "") {
//                        String responseBody = "Supported formats SELECT query:\n";
//                        Iterator<TupleQueryResultFormat> i = TupleQueryResultFormat.values().iterator();
//                        while (i.hasNext()) {
//                            responseBody += "- " + i.next() + "\n";
//                        }
//
//                        responseBody += "\nSupported formats DESCRIBE query:\n";
//                        Iterator<RDFFormat> j = RDFFormat.values().iterator();
//                        while (j.hasNext()) {
//                            responseBody += "- " + j.next() + "\n";
//                        }
//
//                        responseHeaders.set("Content-Type", "text/plain");
//                        exchange.sendResponseHeaders(406, 0);
//                        _writeResponse(responseBody, outputStream);
//                        return;
//                    }
//                    responseHeaders.set("X-Meresco-Triplestore-QueryTime", String.valueOf(indexQueryTime));
//                    if (httpArguments.containsKey("outputContentType")) {
//                        responseHeaders.set("Content-Type", httpArguments.singleValue("outputContentType"));
//                    }
//                    exchange.sendResponseHeaders(200, 0);
//                    _writeResponse(response, outputStream);
//                } catch (MalformedQueryException e) {
//                    exchange.sendResponseHeaders(400, 0);
//                    _writeResponse(e.toString(), outputStream);
//                }
//                return;
//            }
//            else if ("/sparql".equals(path)) {
//                String response = sparqlForm(httpArguments);
//                Headers headers = exchange.getResponseHeaders();
//                headers.set("Content-Type", "text/html");
//                exchange.sendResponseHeaders(200, 0);
//                _writeResponse(response, outputStream);
//            }
//            else if ("/validate".equals(path)) {
//                exchange.sendResponseHeaders(200, 0);
//                try {
//                    validateRDF(httpArguments, body);
//                    _writeResponse("Ok", outputStream);
//                } catch (RDFParseException e) {
//                    _writeResponse("Invalid\n" + e.toString(), outputStream);
//                }
//            }
//            else if ("/export".equals(path)) {
//                export(httpArguments);
//            }
//            else if ("/import".equals(path)) {
//                importTrig(body);
//            }
//            else {
//                exchange.sendResponseHeaders(404, 0);
//                return;
//            }
//            exchange.sendResponseHeaders(200, 0);
//        } catch (IllegalArgumentException e) {
//            exchange.sendResponseHeaders(400, 0);
//            _writeResponse(e.toString(), outputStream);
//        } catch (RuntimeException e) {
//            e.printStackTrace();
//            exchange.sendResponseHeaders(500, 0);
//            String response = Utils.getStackTrace(e);
//            //System.out.println(response);
//            _writeResponse(response, outputStream);
//            return;
//        } catch (Error e) {
//            e.printStackTrace();
//        	exchange.sendResponseHeaders(500, 0);
//        	_writeResponse(e.getMessage(), outputStream);
//            exchange.getHttpContext().getServer().stop(0);
//            ((ExecutorService) exchange.getHttpContext().getServer().getExecutor()).shutdownNow();
//            return;
//        } finally {
//            exchange.close();
//        }
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

    private RDFFormat getRdfFormat(HttpServletRequest request) {
        String accept = request.getHeader("Content-Type");
        return RDFFormat.forMIMEType(accept, RDFFormat.RDFXML);
    }

    public synchronized void updateData(QueryParameters httpArguments, Headers requestHeaders, String httpBody) throws RDFParseException {
        String identifier = httpArguments.singleValue("identifier");
        this.tripleStore.delete(identifier);
        this.tripleStore.add(identifier, httpBody, getRdfFormat(requestHeaders));
    }

    public synchronized void addData(HttpServletRequest request) throws RDFParseException, IOException {
        String identifier = request.getParameter("identifier");
        this.tripleStore.add(identifier, request.getReader().readLine(), getRdfFormat(request));
    }

    public synchronized void addTriple(String httpBody) {
    	this.tripleStore.addTriple(httpBody);
    }

    public synchronized void deleteData(QueryParameters httpArguments) {
        String identifier = httpArguments.singleValue("identifier");
        this.tripleStore.delete(identifier);
    }

    public synchronized void removeTriple(String httpBody) {
    	this.tripleStore.removeTriple(httpBody);
    }

    public List<String> getResponseTypes(Headers requestHeaders, QueryParameters httpArguments) {
    	if (httpArguments.containsKey("mimeType")) {
            return httpArguments.get("mimeType");
        } else if (requestHeaders.containsKey("Accept")) {
            return Arrays.asList(requestHeaders.getFirst("Accept").replace(", ", ",").split(","));
        }
    	return new ArrayList<String>(0);
    }

    public String executeTupleQuery(String query, List<String> responseTypes, Headers responseHeaders) throws MalformedQueryException {
        TupleQueryResultFormat resultFormat = TupleQueryResultFormat.JSON;
        for (String responseType : responseTypes) {
        	TupleQueryResultFormat format = TupleQueryResultFormat.forMIMEType(responseType);
            if (format != null) {
                resultFormat = format;
                break;
            }
        }
        responseHeaders.set("Content-Type", resultFormat.getDefaultMIMEType());
        return this.tripleStore.executeTupleQuery(query, resultFormat);
    }

    public String executeGraphQuery(String query, List<String> responseTypes, Headers responseHeaders) throws MalformedQueryException {
    	RDFFormat resultFormat = RDFFormat.RDFXML;
        for (String responseType : responseTypes) {
    		RDFFormat format = Rio.getParserFormatForMIMEType(responseType);
            if (format != null) {
                resultFormat = format;
                break;
            }
    	}
        responseHeaders.set("Content-Type", resultFormat.getDefaultMIMEType());
        return this.tripleStore.executeGraphQuery(query, resultFormat);
    }

    public void validateRDF(QueryParameters httpArguments, String httpBody) throws RDFParseException {
        validator.validate(httpBody);
    }

    public void export(QueryParameters httpArguments) {
        String identifier = httpArguments.singleValue("identifier");
        this.tripleStore.export(identifier);
    }

    public synchronized void importTrig(String trig) {
    	this.tripleStore.importTrig(trig);
    }

    public String sparqlForm(QueryParameters httpArguments) {
        String query;
        if (httpArguments.containsKey("query")) {
            query = httpArguments.singleValue("query");
        } else {
            query = "";
            for (Namespace namespace : this.tripleStore.getNamespaces()) {
                query += "PREFIX " + namespace.getPrefix() + ": <" + namespace.getName() + ">\n";
            }
            query += "\nSELECT ?subject ?predicate ?object\n";
            query += "WHERE { ?subject ?predicate ?object }\n";
            query += "LIMIT 50";
        }
        return "<html><head><title>Meresco Triplestore Sparql Form</title></head>\n"
            + "<body><form action=\"/query\">\n"
            + "<textarea cols=\"100\" rows=\"20\" name=\"query\">" + StringEscapeUtils.escapeXml(query) + "</textarea><br/>\n"
            + "<input type=\"hidden\" name=\"outputContentType\" value=\"application/json\"/>\n"
            + "Format: <select name=\"mimeType\">\n"
            + "<option value=\"application/sparql-results+json\">json</option>\n"
            + "<option value=\"application/xml\">xml</option>\n"
            + "</select><br />\n"
            + "<input type=\"submit\">\n"
            + "</form>\n</body></html>";
    }
}
