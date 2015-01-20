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

import org.junit.Test;
import static org.junit.Assert.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.Headers;

import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.MalformedQueryException;

import java.net.InetSocketAddress;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import static org.meresco.triplestore.Utils.parseQS;

import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFFormat;


public class HttpHandlerTest {
    @Test public void testAddData() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);
        String queryString = "identifier=identifier";
        String httpBody = "<rdf/>";
        h.addData(parseQS(queryString), httpBody);
        assertEquals(Arrays.asList("add:identifier" + "|" + httpBody + "|RDF/XML"), tsmock.actions);
    }

    @Test public void testAddTriple() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);
        String httpBody = "uri:subj|uri:pred|uri:obj";
        h.addTriple(httpBody);
        assertEquals(Arrays.asList("addTriple:uri:subj|uri:pred|uri:obj"), tsmock.actions);
    }

    @Test public void testAddTripleWithStringAsObject() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);
        String httpBody = "uri:subj|uri:pred|string";
        h.addTriple(httpBody);
        assertEquals(Arrays.asList("addTriple:uri:subj|uri:pred|string"), tsmock.actions);
    }

    @Test public void testRemoveTriple() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);
        String httpBody = "uri:subj|uri:pred|string";
        h.removeTriple(httpBody);
        assertEquals(Arrays.asList("removeTriple:uri:subj|uri:pred|string"), tsmock.actions);
    }

    @Test public void testDeleteData() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);
        String queryString = "identifier=identifier";
        h.deleteData(parseQS(queryString));
        assertEquals(Arrays.asList("delete:identifier"), tsmock.actions);
    }

    @Test public void testUpdateData() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);
        String httpBody = "<rdf/>";
        h.updateData(parseQS("identifier=id0"), httpBody);
        assertEquals(Arrays.asList(
                        "delete:id0",
                        "add:id0|<rdf/>|RDF/XML"),
                     tsmock.actions);
        h.updateData(parseQS("identifier=id1"), httpBody);
        h.updateData(parseQS("identifier=id0"), httpBody);
        assertEquals(Arrays.asList(
                        "delete:id0",
                        "add:id0|<rdf/>|RDF/XML",
                        "delete:id1",
                        "add:id1|<rdf/>|RDF/XML",
                        "delete:id0",
                        "add:id0|<rdf/>|RDF/XML"),
                     tsmock.actions);
    }

    @Test public void testSparQLTuple() throws TransactionLogException, MalformedQueryException {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);
        String result = h.executeTupleQuery("SELECT ?x ?y ?z WHERE { ?x ?y ?z }", new ArrayList<String>(), new Headers());

        assertEquals(Arrays.asList("executeTupleQuery:SELECT ?x ?y ?z WHERE { ?x ?y ?z }"),
                     tsmock.actions);
    }

    @Test public void testSparQLGraph() throws TransactionLogException, MalformedQueryException {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);
        String queryString = "DESCRIBE <uri:test>";
        String result = h.executeGraphQuery(queryString, new ArrayList<String>(), new Headers());

        assertEquals(Arrays.asList("executeGraphQuery:DESCRIBE <uri:test>"),
                     tsmock.actions);
    }

    @Test public void testAddDispatch() throws Exception {
        HttpHandlerMock h = new HttpHandlerMock();

        HttpExchangeMock exchange = new HttpExchangeMock("/add?id=IDENTIFIER", "<rdf/>");
        h.handle(exchange);
        assertEquals(3, h.actions.size());
        assertEquals("addData", h.actions.get(0));
        QueryParameters qp = (QueryParameters) h.actions.get(1);
        assertEquals("IDENTIFIER", qp.singleValue("id"));
        assertEquals("<rdf/>", h.actions.get(2));

        assertEquals(200, exchange.responseCode);
    }

    @Test public void testUpdateDispatch() throws Exception {
        HttpHandlerMock h = new HttpHandlerMock();

        HttpExchangeMock exchange = new HttpExchangeMock("/update?id=IDENTIFIER", "<rdf/>");
        h.handle(exchange);
        assertEquals(3, h.actions.size());
        assertEquals("updateData", h.actions.get(0));
        QueryParameters qp = (QueryParameters) h.actions.get(1);
        assertEquals("IDENTIFIER", qp.singleValue("id"));
        assertEquals("<rdf/>", h.actions.get(2));
        assertEquals(200, exchange.responseCode);
    }

    @Test public void testDeleteDispatch() throws Exception {
        HttpHandlerMock h = new HttpHandlerMock();

        HttpExchangeMock exchange = new HttpExchangeMock("/delete?id=IDENTIFIER", "");
        h.handle(exchange);
        assertEquals(2, h.actions.size());
        assertEquals("deleteData", h.actions.get(0));
        QueryParameters qp = (QueryParameters) h.actions.get(1);
        assertEquals("IDENTIFIER", qp.singleValue("id"));
        assertEquals(200, exchange.responseCode);
    }

    @Test public void testTupleQueryDispatch() throws Exception {
        HttpHandlerMock h = new HttpHandlerMock();

        HttpExchangeMock exchange = new HttpExchangeMock("/query?query=SELECT%20?x%20?y%20?z%20WHERE%20%7B%20?x%20?y%20?z%20%7D", "");
        h.handle(exchange);
        assertEquals(3, h.actions.size());
        assertEquals("executeTupleQuery", h.actions.get(0));
        assertEquals(Arrays.asList(), h.actions.get(1));
        assertEquals("SELECT ?x ?y ?z WHERE { ?x ?y ?z }", h.actions.get(2));
        assertEquals(200, exchange.responseCode);
        assertEquals("QUERYRESULT", exchange.getOutput());
    }

    @Test public void testGraphQueryDispatch() throws Exception {
        HttpHandlerMock h = new HttpHandlerMock();

        HttpExchangeMock exchange = new HttpExchangeMock("/query?query=DESCRIBE+%3Curi:test%3E", "");
        h.handle(exchange);
        assertEquals(3, h.actions.size());
        assertEquals("executeGraphQuery", h.actions.get(0));
        assertEquals(Arrays.asList(), h.actions.get(1));
        assertEquals("DESCRIBE <uri:test>", h.actions.get(2));
        assertEquals(200, exchange.responseCode);
        assertEquals("QUERYRESULT", exchange.getOutput());
    }

    @Test public void testQueryWithResultFormatDispatch() throws Exception {
        HttpHandlerMock h = new HttpHandlerMock();

        HttpExchangeMock exchange = new HttpExchangeMock("/query?format=SPARQL&query=SELECT%20?x%20?y%20?z%20WHERE%20%7B%20?x%20?y%20?z%20%7D", "");
        h.handle(exchange);
        assertEquals(3, h.actions.size());
        assertEquals("executeTupleQuery", h.actions.get(0));
        assertEquals(Arrays.asList(), h.actions.get(1));
        assertEquals("SELECT ?x ?y ?z WHERE { ?x ?y ?z }", h.actions.get(2));
        assertEquals(200, exchange.responseCode);
        assertEquals("QUERYRESULT", exchange.getOutput());
    }

    @Test public void testQueryWithMimeType() throws Exception {
        HttpHandlerMock h = new HttpHandlerMock();

        HttpExchangeMock exchange = new HttpExchangeMock("/query?query=SELECT%20?x%20?y%20?z%20WHERE%20%7B%20?x%20?y%20?z%20%7D&mimeType=application/sparql-results+xml", "");
        h.handle(exchange);
        assertEquals(3, h.actions.size());
        assertEquals("executeTupleQuery", h.actions.get(0));
        assertEquals(Arrays.asList("application/sparql-results xml"), h.actions.get(1));
        assertEquals("SELECT ?x ?y ?z WHERE { ?x ?y ?z }", h.actions.get(2));
        assertEquals(200, exchange.responseCode);
        assertEquals("QUERYRESULT", exchange.getOutput());
    }

    @Test public void testQueryWithAcceptHeader() throws Exception {
        HttpHandlerMock h = new HttpHandlerMock();
        Headers headers = new Headers();
        headers.set("Accept", "application/sparql-results+xml");
        HttpExchangeMock exchange = new HttpExchangeMock("/query?query=SELECT%20?x%20?y%20?z%20WHERE%20%7B%20?x%20?y%20?z%20%7D", "", headers);
        h.handle(exchange);
        assertEquals(3, h.actions.size());
        assertEquals("executeTupleQuery", h.actions.get(0));
        assertEquals(Arrays.asList("application/sparql-results+xml"), h.actions.get(1));
        assertEquals("SELECT ?x ?y ?z WHERE { ?x ?y ?z }", h.actions.get(2));
        assertEquals(200, exchange.responseCode);
        assertEquals("QUERYRESULT", exchange.getOutput());
    }

    @Test public void testValidate() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);
        String queryString = "identifier=identifier";
        String notRDF = "<notrdf/>";
        try {
            h.validateRDF(parseQS(queryString), notRDF);
            fail("should not get here.");
        } catch (RDFParseException e) {
            // SUCCESS
        }

        String emptyRdfTag = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"/>";
        try {
            h.validateRDF(parseQS(queryString), emptyRdfTag);  // valid !
        } catch (RDFParseException e) {
            fail("should not get here.");
        }

        String emptyRdfDescription = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n"
                + "<rdf:Description rdf:about=\"urn:123\">\n"
                + "</rdf:Description>\n"
                + "</rdf:RDF>";
        try {
            h.validateRDF(parseQS(queryString), emptyRdfDescription);  // valid !
        } catch (RDFParseException e) {
            fail("should not get here.");
        }

        String oneTriple = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n"
                + "<rdf:Description rdf:about=\"urn:123\">\n"
                + "   <somevoc:relation xmlns:somevoc=\"uri:someuri\">Xyz</somevoc:relation>\n"
                + "</rdf:Description>\n"
                + "</rdf:RDF>";
        try {
            h.validateRDF(parseQS(queryString), oneTriple);  // valid !
        } catch (RDFParseException e) {
            fail("should not get here.");
        }
    }

    @Test public void testSparqlDispatch() throws Exception {
        HttpHandlerMock h = new HttpHandlerMock();

        HttpExchangeMock exchange = new HttpExchangeMock("/sparql", "");
        h.handle(exchange);
        assertEquals(2, h.actions.size());
        assertEquals("sparqlForm", h.actions.get(0));
        QueryParameters qp = (QueryParameters) h.actions.get(1);
        assertEquals(0, qp.size());
    }

    @Test public void testSparqlWithQueryParametersDispatch() throws Exception {
        HttpHandlerMock h = new HttpHandlerMock();

        HttpExchangeMock exchange = new HttpExchangeMock("/sparql?query=x", "");
        h.handle(exchange);
        assertEquals(2, h.actions.size());
        assertEquals("sparqlForm", h.actions.get(0));
        QueryParameters qp = (QueryParameters) h.actions.get(1);
        assertEquals(1, qp.size());
        assertEquals("x", qp.singleValue("query"));
    }

    @Test public void testValidateDispatch() throws Exception {
        HttpHandler h = new HttpHandler(null);
        HttpExchangeMock exchange = new HttpExchangeMock("/validate?identifier=IDENTIFIER", "<rdf:Description xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf\" about=\"notanuri\"/>");
        h.handle(exchange);
        assertEquals(200, exchange.responseCode);
        assertEquals("Invalid\norg.openrdf.rio.RDFParseException: Not a valid (absolute) URI: /notanuri [line 1, column 81]", exchange.getResponseBody().toString());

        exchange = new HttpExchangeMock("/validate?identifier=urn:IDENTIFIER", "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"/>");
        h.handle(exchange);
        assertEquals(200, exchange.responseCode);
        assertEquals("Ok", exchange.getResponseBody().toString());
    }

    @Test public void test404ForOtherRequests() throws Exception {
        HttpHandlerMock h = new HttpHandlerMock();

        HttpExchangeMock exchange = new HttpExchangeMock("/", "");
        h.handle(exchange);
        assertEquals(0, h.actions.size());
        assertEquals(404, exchange.responseCode);
    }

    @Test public void test500ForExceptions() throws Exception {
        HttpHandlerMock h = new HttpHandlerMock(new Exception("dummy test exception"));

        HttpExchangeMock exchange = new HttpExchangeMock("/add", "");

        PrintStream originalErrStream = System.err;
        OutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        System.setErr(ps);
        try {
            h.handle(exchange);
        }
        finally {
            System.setErr(originalErrStream);
        }
        assertTrue(os.toString().contains("dummy test exception"));
        assertEquals(0, h.actions.size());
        assertEquals(500, exchange.responseCode);
        assertTrue(exchange.getOutput().startsWith("java.lang.RuntimeException: java.lang.Exception: dummy test exception"));
    }

    @Test public void test400ForMalformedQueryExceptions() throws IOException {
        HttpHandlerMock h = new HttpHandlerMock(new MalformedQueryException("dummy test exception"));
        HttpExchangeMock exchange = new HttpExchangeMock("/query?query=SELECT+%3Fx+WHERE+%7B%7D%0A", "");
        h.handle(exchange);
        assertEquals(0, h.actions.size());
        assertEquals(400, exchange.responseCode);
        assertTrue(exchange.getOutput(), exchange.getOutput().startsWith("org.openrdf.query.MalformedQueryException: dummy test exception"));
    }

    @Test public void testDefaultSparqlForm() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);

        QueryParameters queryParameters = Utils.parseQS("");
        String sparqlForm = h.sparqlForm(queryParameters);
        String expectedQuery = "PREFIX rdf: &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;\n" +
            "PREFIX rdfs: &lt;http://www.w3.org/2000/01/rdf-schema#&gt;\n" +
            "\n" +
            "SELECT ?subject ?predicate ?object\n" +
            "WHERE { ?subject ?predicate ?object }\n" +
            "LIMIT 50";
        assertTrue(sparqlForm, sparqlForm.contains(expectedQuery));
    }

    @Test public void testSparqlFormWithQueryArgument() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);

        QueryParameters queryParameters = Utils.parseQS("query=SELECT+%3Fx+WHERE+%7B%7D%0A");
        String sparqlForm = h.sparqlForm(queryParameters);
        String expectedQuery = "SELECT ?x WHERE {}\n";
        assertTrue(sparqlForm, sparqlForm.contains(expectedQuery));
    }

     @Test public void testAllAcceptHeaderReturnsJson() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);

        Headers inputHeaders = new Headers();
        inputHeaders.add("Accept", "*/*");
        HttpExchangeMock exchange = new HttpExchangeMock("/query?query=SELECT%20?x%20?y%20?z%20WHERE%20%7B%20?x%20?y%20?z%20%7D", "", inputHeaders);
        h.handle(exchange);
        assertEquals(200, exchange.responseCode);
        assertEquals("application/sparql-results+json", exchange.getResponseHeaders().getFirst("Content-Type"));
    }

    @Test public void testMultipleAcceptHeaderReturnsKnown() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);

        Headers inputHeaders = new Headers();
        inputHeaders.add("Accept", "text/html, application/xml");
        HttpExchangeMock exchange = new HttpExchangeMock("/query?query=SELECT%20?x%20?y%20?z%20WHERE%20%7B%20?x%20?y%20?z%20%7D", "", inputHeaders);
        h.handle(exchange);
        assertEquals(200, exchange.responseCode);
        assertEquals("application/sparql-results+xml", exchange.getResponseHeaders().getFirst("Content-Type"));
    }

    @Test public void testShowContentTypes() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);

        Headers inputHeaders = new Headers();
        HttpExchangeMock exchange = new HttpExchangeMock("/query", "", inputHeaders);
        h.handle(exchange);
        assertEquals(406, exchange.responseCode);
        assertEquals("Supported formats SELECT query:"
            + "\n- SPARQL/XML (mimeTypes=application/sparql-results+xml, application/xml; ext=srx, xml)"
            + "\n- BINARY (mimeTypes=application/x-binary-rdf-results-table; ext=brt)"
            + "\n- SPARQL/JSON (mimeTypes=application/sparql-results+json, application/json; ext=srj, json)"
            + "\n- SPARQL/CSV (mimeTypes=text/csv; ext=csv)"
            + "\n- SPARQL/TSV (mimeTypes=text/tab-separated-values; ext=tsv)"
            + "\n"
            + "\nSupported formats DESCRIBE query:"
            + "\n- RDF/XML (mimeTypes=application/rdf+xml, application/xml; ext=rdf, rdfs, owl, xml)"
            + "\n- N-Triples (mimeTypes=text/plain; ext=nt)"
            + "\n- Turtle (mimeTypes=text/turtle, application/x-turtle; ext=ttl)"
            + "\n- N3 (mimeTypes=text/n3, text/rdf+n3; ext=n3)"
            + "\n- TriX (mimeTypes=application/trix; ext=xml, trix)"
            + "\n- TriG (mimeTypes=application/x-trig; ext=trig)"
            + "\n- BinaryRDF (mimeTypes=application/x-binary-rdf; ext=brf)"
            + "\n- N-Quads (mimeTypes=text/x-nquads; ext=nq)"
            + "\n- JSON-LD (mimeTypes=application/ld+json; ext=jsonld)"
            + "\n- RDF/JSON (mimeTypes=application/rdf+json; ext=rj)"
            + "\n- RDFa (mimeTypes=application/xhtml+xml, application/html, text/html; ext=xhtml, html)\n", exchange.getResponseBody().toString());
        assertEquals("text/plain", exchange.getResponseHeaders().getFirst("Content-Type"));
    }

    @Test public void testContentType() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);

        Headers inputHeaders = new Headers();
        inputHeaders.add("Accept", "application/xml");
        HttpExchangeMock exchange = new HttpExchangeMock("/query?query=SELECT+%3Fx+WHERE+%7B%7D%0A", "", inputHeaders);
        h.handle(exchange);
        assertEquals(200, exchange.responseCode);
        assertEquals(1, exchange.getResponseHeaders().get("Content-Type").size());
        assertEquals("application/sparql-results+xml", exchange.getResponseHeaders().getFirst("Content-Type"));
    }

    @Test
    public void testXMerescoTriplestoreQueryTime() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);

        Headers inputHeaders = new Headers();
        HttpExchangeMock exchange = new HttpExchangeMock("/query?query=SELECT+%3Fx+WHERE+%7B%7D%0A", "", inputHeaders);
        h.handle(exchange);
        assertEquals(200, exchange.responseCode);
        int time = Integer.parseInt(exchange.getResponseHeaders().getFirst("X-Meresco-Triplestore-QueryTime"));
        assertTrue(time >= 0);
        assertTrue(time <= 3);
    }

    @Test public void testExportDispatch() throws Exception {
        HttpHandlerMock h = new HttpHandlerMock();

        HttpExchangeMock exchange = new HttpExchangeMock("/export?identifier=identifier", "");
        h.handle(exchange);
        assertEquals(2, h.actions.size());
        assertEquals("export", h.actions.get(0));
        QueryParameters qp = (QueryParameters) h.actions.get(1);
        assertEquals(1, qp.size());
        assertEquals("identifier", qp.singleValue("identifier"));
    }

    @Test public void testExport() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);

        HttpExchangeMock exchange = new HttpExchangeMock("/export?identifier=identifier", "");
        h.handle(exchange);
        assertEquals(1, tsmock.actions.size());
        assertEquals("export:identifier", tsmock.actions.get(0));
    }

    @Test public void testImport() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);
        String trig = "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . \n" +
"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . \n" +
"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . \n" +
"\n" +
"<uri:aContext> { \n" +
"        <uri:aSubject> <uri:aPredicate> \"a literal  value\" . \n" +
"}";

        HttpExchangeMock exchange = new HttpExchangeMock("/import", trig);
        h.handle(exchange);
        assertEquals(1, tsmock.actions.size());
        assertEquals("import:" + trig, tsmock.actions.get(0));
    }


    class HttpHandlerMock extends HttpHandler {
        public List<Object> actions = new ArrayList<Object>();
        private Exception _exception = null;

        public HttpHandlerMock() { super(null); }
        public HttpHandlerMock(Exception e) {
            super(null);
            _exception = e;
        }
        @Override
        public void updateData(QueryParameters params, String httpBody) throws RDFParseException {
            if (_exception != null) {
                throw new RuntimeException(_exception);
            }
            actions.add("updateData");
            actions.add(params);
            actions.add(httpBody);
        }
        @Override
        public void addData(QueryParameters params, String httpBody) throws RDFParseException {
            if (_exception != null) {
                throw new RuntimeException(_exception);
            }
            actions.add("addData");
            actions.add(params);
            actions.add(httpBody);
        }
        @Override
        public void deleteData(QueryParameters params) {
            if (_exception != null) {
                throw new RuntimeException(_exception);
            }
            actions.add("deleteData");
            actions.add(params);
        }
        @Override
        public String executeTupleQuery(String query, List<String> responseTypes, Headers responseHeaders) throws MalformedQueryException {
            if (_exception != null) {
                if (_exception instanceof MalformedQueryException) {
                    throw (MalformedQueryException) _exception;
                }
                throw new RuntimeException(_exception);
            }
            actions.add("executeTupleQuery");
            actions.add(responseTypes);
            actions.add(query);
            return "QUERYRESULT";
        }
        @Override
        public String executeGraphQuery(String query, List<String> responseTypes, Headers responseHeaders) throws MalformedQueryException {
            if (_exception != null) {
                if (_exception instanceof MalformedQueryException) {
                    throw (MalformedQueryException) _exception;
                }
                throw new RuntimeException(_exception);
            }
            actions.add("executeGraphQuery");
            actions.add(responseTypes);
            actions.add(query);
            return "QUERYRESULT";
        }
        @Override
        public void validateRDF(QueryParameters params, String httpBody) {
            actions.add("validateRDF");
            actions.add(params);
            actions.add(httpBody);
        }
        @Override
        public String sparqlForm(QueryParameters params) {
            actions.add("sparqlForm");
            actions.add(params);
            return "SPARQLFORM";
        }
        @Override
        public void export(QueryParameters params) {
            actions.add("export");
            actions.add(params);
        }
    }


    class HttpExchangeMock extends HttpExchange {
        private java.net.URI _requestURI;
        private String _requestBody;
        private ByteArrayOutputStream _responseStream;
        private Headers _requestHeaders;
        private Headers _responseHeaders;
        public int responseCode;
        public String responseBody;

        public HttpExchangeMock(String requestURI, String requestBody, Headers requestHeaders) {
            super();
            try {
                _requestURI = new java.net.URI(requestURI);
                _requestBody = requestBody;
                _responseStream = new ByteArrayOutputStream();
                _requestHeaders = requestHeaders;
                _responseHeaders = new Headers();
            } catch (java.net.URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        public HttpExchangeMock(String requestURI, String requestBody) {
            this(requestURI, requestBody, new Headers());
        }

        public String getOutput() { return _responseStream.toString(); }
        public java.net.URI getRequestURI() { return _requestURI; }
        public HttpPrincipal getPrincipal() { return null; }
        public void setStreams(InputStream i, OutputStream o) {}
        public void setAttribute(Object o) {}
        public void setAttribute(String s, Object o) {}
        public Object getAttribute(String s) { return null; }
        public String getProtocol() { return ""; }
        public InetSocketAddress getLocalAddress() { return null; }
        public InetSocketAddress getRemoteAddress() { return null; }
        public int getResponseCode() { return 0; }
        public void sendResponseHeaders(int responseCode, long l) {
            this.responseCode = responseCode;
        }
        public OutputStream getResponseBody() { return _responseStream;  }
        public InputStream getRequestBody() { return new ByteArrayInputStream(_requestBody.getBytes()); }
        public void close() {};
        public HttpContext getHttpContext() { return null; }
        public String getRequestMethod() { return ""; }
        public Headers getResponseHeaders() { return this._responseHeaders; }
        public Headers getRequestHeaders() { return this._requestHeaders; }
    }
}
