/* begin license *
 *
 * The Meresco Triplestore package consists out of a HTTP server written in Java that
 * provides access to an Triplestore with a Sesame Interface, as well as python bindings to
 * communicate as a client with the server.
 *
 * Copyright (C) 2011-2014, 2016 Seecr (Seek You Too B.V.) http://seecr.nl
 * Copyright (C) 2011 Seek You Too B.V. (CQ2) http://www.cq2.nl
 *
 * This file is part of "Meresco Triplestore"
 *
 * "Meresco Triplestore" is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * "Meresco Triplestore" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with "Meresco Triplestore"; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * end license */

package org.meresco.triplestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.openrdf.model.Namespace;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.parser.ParsedGraphQuery;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.QueryParserUtil;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.Rio;


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
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        String path = request.getRequestURI();
        try {
            if ("/add".equals(path)) {
                try {
                    addData(request);
                } catch (RDFParseException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(e.toString());
                    return;
                }
            }
            else if ("/update".equals(path)) {
                try {
                    updateData(request);
                } catch (RDFParseException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(e.toString());
                    return;
                }
            }
            else if ("/delete".equals(path)) {
                deleteData(request);
            }
            else if ("/addTriple".equals(path)) {
                addTriple(request);
            }
            else if ("/removeTriple".equals(path)) {
                removeTriple(request);
            }
            else if ("/query".equals(path)) {
                String responseData = "";
                Map<String, List<String>> parameters = combineArgumentsAndBody(request);
                try {
                    long start = System.currentTimeMillis();
                    String query = request.getParameter("query");
                    List<String> responseTypes = getResponseTypes(request, parameters);
                    if (query != null) {
                        ParsedQuery p = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);
                        Map<String, String> headers = new HashMap<String, String>();
                        if (p instanceof ParsedGraphQuery) {
                            responseData = executeGraphQuery(query, responseTypes, headers);
                        } else {
                            responseData = executeTupleQuery(query, responseTypes, headers);
                        }
                        for (String header : headers.keySet())
                            response.setHeader(header, headers.get(header));
                    }
                    long indexQueryTime = System.currentTimeMillis() - start;
                    if (responseData == null || responseData == "") {
                        String responseBody = "Supported formats SELECT query:\n";
                        Iterator<TupleQueryResultFormat> i = TupleQueryResultFormat.values().iterator();
                        while (i.hasNext()) {
                            responseBody += "- " + i.next() + "\n";
                        }

                        responseBody += "\nSupported formats DESCRIBE query:\n";
                        Iterator<RDFFormat> j = RDFFormat.values().iterator();
                        while (j.hasNext()) {
                            responseBody += "- " + j.next() + "\n";
                        }

                        response.setHeader("Content-Type", "text/plain");
                        response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
                        response.getWriter().write(responseBody);
                        return;
                    }
                    response.setHeader("X-Meresco-Triplestore-QueryTime", String.valueOf(indexQueryTime));
                    if (parameters.containsKey("outputContentType")) {
                        response.setHeader("Content-Type", parameters.get("outputContentType").get(0));
                    }
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write(responseData);
                } catch (MalformedQueryException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(e.toString());
                }
                return;
            }
            else if ("/sparql".equals(path)) {
                String form = sparqlForm(request);
                response.setHeader("Content-Type", "text/html");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(form);
            }
            else if ("/validate".equals(path)) {
                response.setStatus(HttpServletResponse.SC_OK);
                try {
                    validateRDF(request);
                    response.getWriter().write("Ok");
                } catch (RDFParseException e) {
                    response.getWriter().write("Invalid\n" + e.toString());
                }
            }
            else if ("/export".equals(path)) {
                export(request);
            }
            else if ("/import".equals(path)) {
                importTrig(Utils.read(request.getReader()));
            }
            else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(e.toString());
        } catch (RuntimeException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(Utils.getStackTrace(e));
            return;
        } catch (Error e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(e.getMessage());
            return;
        } finally {
            baseRequest.setHandled(true);
        }
    }

    private Map<String, List<String>> combineArgumentsAndBody(HttpServletRequest request) throws IOException {
        Map<String, List<String>> parameters = new HashMap<String, List<String>>();
        Map<String, String[]> args = request.getParameterMap();
        for (String key : args.keySet()) {
            List<String> values = Arrays.asList(args.get(key));
            if (parameters.containsKey(key))
                parameters.get(key).addAll(values);
            else
                parameters.put(key, values);
        }
        if (request.getMethod().equals("POST")) {
            parameters.putAll(Utils.parseQS(Utils.read(request.getInputStream())));
        }
        return parameters;
    }

    private RDFFormat getRdfFormat(HttpServletRequest request) {
        String accept = request.getHeader("Content-Type");
        return RDFFormat.forMIMEType(accept, RDFFormat.RDFXML);
    }

    public synchronized void updateData(HttpServletRequest request) throws RDFParseException, IOException {
        String identifier = request.getParameter("identifier");
        this.tripleStore.delete(identifier);
        this.tripleStore.add(identifier, Utils.read(request.getReader()), getRdfFormat(request));
    }

    public synchronized void addData(HttpServletRequest request) throws RDFParseException, IOException {
        String identifier = request.getParameter("identifier");
        this.tripleStore.add(identifier, Utils.read(request.getReader()), getRdfFormat(request));
    }

    public synchronized void addTriple(HttpServletRequest request) throws IOException {
    	this.tripleStore.addTriple(Utils.read(request.getReader()));
    }

    public synchronized void deleteData(HttpServletRequest request) {
        String identifier = request.getParameter("identifier");
        this.tripleStore.delete(identifier);
    }

    public synchronized void removeTriple(HttpServletRequest request) throws IOException {
    	this.tripleStore.removeTriple(request.getReader().readLine());
    }

    public List<String> getResponseTypes(HttpServletRequest request, Map<String, List<String>> parameters) {
        String acceptHeader = request.getHeader("Accept");
        if (parameters.containsKey("mimeType")) {
            return parameters.get("mimeType");
        } else if (acceptHeader != null) {
            return Arrays.asList(acceptHeader.replace(", ", ",").split(","));
        }
    	return new ArrayList<String>(0);
    }

    public String executeTupleQuery(String query, List<String> responseTypes, Map<String, String> headers) throws MalformedQueryException {
        TupleQueryResultFormat resultFormat = TupleQueryResultFormat.JSON;
        for (String responseType : responseTypes) {
        	TupleQueryResultFormat format = TupleQueryResultFormat.forMIMEType(responseType);
            if (format != null) {
                resultFormat = format;
                break;
            }
        }
        headers.put("Content-Type", resultFormat.getDefaultMIMEType());
        return this.tripleStore.executeTupleQuery(query, resultFormat);
    }

    public String executeGraphQuery(String query, List<String> responseTypes, Map<String, String> headers) throws MalformedQueryException {
    	RDFFormat resultFormat = RDFFormat.RDFXML;
        for (String responseType : responseTypes) {
    		RDFFormat format = Rio.getParserFormatForMIMEType(responseType);
            if (format != null) {
                resultFormat = format;
                break;
            }
    	}
        headers.put("Content-Type", resultFormat.getDefaultMIMEType());
        return this.tripleStore.executeGraphQuery(query, resultFormat);
    }

    public void validateRDF(HttpServletRequest request) throws RDFParseException, IOException {
        validator.validate(Utils.read(request.getReader()));
    }

    public void export(HttpServletRequest request) {
        String identifier = request.getParameter("identifier");
        this.tripleStore.export(identifier);
    }

    public synchronized void importTrig(String trig) {
    	this.tripleStore.importTrig(trig);
    }

    public String sparqlForm(HttpServletRequest request) {
        String query;
        if (request.getParameterMap().containsKey("query")) {
            query = request.getParameter("query");
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
