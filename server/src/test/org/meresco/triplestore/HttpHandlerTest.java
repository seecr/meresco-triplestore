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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.apache.commons.io.output.StringBuilderWriter;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpInput;
import org.eclipse.jetty.server.Request;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.rio.RDFParseException;

public class HttpHandlerTest {
    private RequestMock requestMock;
    private ResponseMock responseMock;
    private Request baseRequest;

    @Before
    public void setUp() {
        requestMock = new RequestMock();
        responseMock = new ResponseMock();
        baseRequest = new BaseRequestMock(null, null);
    }

    @Test
    public void testAddData() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);
        requestMock.parameters.put("identifier", "identifier");
        requestMock.body = "<rdf/>";
        h.addData(requestMock);
        assertEquals(Arrays.asList("add:identifier" + "|" + requestMock.body + "|RDF/XML"), tsmock.actions);
    }

    @Test
    public void testAddDataAsNTriples() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);
        requestMock.parameters.put("identifier", "identifier");
        requestMock.body = "<uri:subject> <uri:predicate> <uri:object>";
        requestMock.headers.put("Content-Type", "text/plain");
        h.addData(requestMock);
        assertEquals(Arrays.asList("add:identifier" + "|" + requestMock.body + "|N-Triples"), tsmock.actions);
    }

    @Test
    public void testAddTriple() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);
        requestMock.body = "uri:subj|uri:pred|uri:obj";
        h.addTriple(requestMock);
        assertEquals(Arrays.asList("addTriple:uri:subj|uri:pred|uri:obj"), tsmock.actions);
    }

    @Test
    public void testAddTripleWithStringAsObject() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);
        requestMock.body = "uri:subj|uri:pred|string";
        h.addTriple(requestMock);
        assertEquals(Arrays.asList("addTriple:uri:subj|uri:pred|string"), tsmock.actions);
    }

    @Test
    public void testRemoveTriple() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);
        requestMock.body = "uri:subj|uri:pred|string";
        h.removeTriple(requestMock);
        assertEquals(Arrays.asList("removeTriple:uri:subj|uri:pred|string"), tsmock.actions);
    }

    @Test
    public void testDeleteData() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);
        requestMock.parameters.put("identifier", "identifier");
        h.deleteData(requestMock);
        assertEquals(Arrays.asList("delete:identifier"), tsmock.actions);
    }

    @Test
    public void testUpdateData() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);
        requestMock.parameters.put("identifier", "id0");
        requestMock.body = "<rdf/>";
        h.updateData(requestMock);
        assertEquals(Arrays.asList(
                        "delete:id0",
                        "add:id0|<rdf/>|RDF/XML"),
                     tsmock.actions);
        requestMock.parameters.put("identifier", "id1");
        h.updateData(requestMock);
        requestMock.parameters.put("identifier", "id0");
        h.updateData(requestMock);
        assertEquals(Arrays.asList(
                        "delete:id0",
                        "add:id0|<rdf/>|RDF/XML",
                        "delete:id1",
                        "add:id1|<rdf/>|RDF/XML",
                        "delete:id0",
                        "add:id0|<rdf/>|RDF/XML"),
                     tsmock.actions);
    }

    @Test
    public void testSparQLTuple() throws TransactionLogException, MalformedQueryException {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);
        Map<String, String> headers = new HashMap<>();
        h.executeTupleQuery("SELECT ?x ?y ?z WHERE { ?x ?y ?z }", new ArrayList<String>(), headers);
        assertEquals(Arrays.asList("executeTupleQuery:SELECT ?x ?y ?z WHERE { ?x ?y ?z }"), tsmock.actions);
    }

    @Test
    public void testSparQLGraph() throws TransactionLogException, MalformedQueryException {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);
        String queryString = "DESCRIBE <uri:test>";
        Map<String, String> headers = new HashMap<>();
        h.executeGraphQuery(queryString, new ArrayList<String>(), headers);
        assertEquals(Arrays.asList("executeGraphQuery:DESCRIBE <uri:test>"), tsmock.actions);
    }

    @Test
    public void testSparQLBoolean() throws TransactionLogException, MalformedQueryException {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);
        String queryString = "ASK { <uri:test> ?p ?o } WHERE { <uri:test> ?p ?o }";
        Map<String, String> headers = new HashMap<>();
        h.executeBooleanQuery(queryString, new ArrayList<String>(), headers);
        assertEquals(Arrays.asList("executeBooleanQuery:ASK { <uri:test> ?p ?o } WHERE { <uri:test> ?p ?o }"), tsmock.actions);
    }
    
    @Test
    public void testValidate() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);
        String notRDF = "<notrdf/>";
        requestMock.body = notRDF;
        try {
            h.validateRDF(requestMock);
            fail("should not get here.");
        } catch (RDFParseException e) {
            // SUCCESS
        }

        String emptyRdfTag = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"/>";
        requestMock.body = emptyRdfTag;
        try {
            h.validateRDF(requestMock);  // valid !
        } catch (RDFParseException e) {
            fail("should not get here.");
        }

        String emptyRdfDescription = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n"
                + "<rdf:Description rdf:about=\"urn:123\">\n"
                + "</rdf:Description>\n"
                + "</rdf:RDF>";
        requestMock.body = emptyRdfDescription;
        try {
            h.validateRDF(requestMock);  // valid !
        } catch (RDFParseException e) {
            fail("should not get here.");
        }

        String oneTriple = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n"
                + "<rdf:Description rdf:about=\"urn:123\">\n"
                + "   <somevoc:relation xmlns:somevoc=\"uri:someuri\">Xyz</somevoc:relation>\n"
                + "</rdf:Description>\n"
                + "</rdf:RDF>";
        requestMock.body = oneTriple;
        try {
            h.validateRDF(requestMock);  // valid !
        } catch (RDFParseException e) {
            fail("should not get here.");
        }
    }

    @Test
    public void testValidateDispatch() throws Exception {
        HttpHandler h = new HttpHandler(null);
        requestMock.requestUri = "/validate";
        requestMock.parameters.put("identifier", "IDENTIFIER");
        requestMock.body = "<rdf:Description xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf\" about=\"notanuri\"/>";
        h.handle(null, baseRequest, requestMock, responseMock);
        assertEquals(200, responseMock.statusCode);
        assertEquals("Invalid\norg.openrdf.rio.RDFParseException: Not a valid (absolute) URI: /notanuri [line 1, column 81]", responseMock.response.toString());

        responseMock = new ResponseMock();
        requestMock.requestUri = "/validate";
        requestMock.parameters.put("identifier", "urn:IDENTIFIER");
        requestMock.body = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"/>";
        h.handle(null, baseRequest, requestMock, responseMock);
        assertEquals(200, responseMock.statusCode);
        assertEquals("Ok", responseMock.response.toString());
    }

    @Test
    public void test404() throws Exception {
        HttpHandler h = new HttpHandler(null);
        requestMock.requestUri = "/not_existing";
        h.handle(null, baseRequest, requestMock, responseMock);
        assertEquals(404, responseMock.statusCode);
    }

    @Test
    public void testDefaultSparqlForm() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);
        String sparqlForm = h.sparqlForm(requestMock);
        String expectedQuery = "PREFIX rdf: &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;\n" +
            "PREFIX rdfs: &lt;http://www.w3.org/2000/01/rdf-schema#&gt;\n" +
            "\n" +
            "SELECT ?subject ?predicate ?object\n" +
            "WHERE { ?subject ?predicate ?object }\n" +
            "LIMIT 50";
        assertTrue(sparqlForm, sparqlForm.contains(expectedQuery));
    }

    @Test
    public void testSparqlFormWithQueryArgument() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);

        requestMock.parameters.put("query", "SELECT ?x WHERE {}\n");
        String sparqlForm = h.sparqlForm(requestMock);
        String expectedQuery = "SELECT ?x WHERE {}\n";
        assertTrue(sparqlForm, sparqlForm.contains(expectedQuery));
    }

     @Test
     public void testAllAcceptHeaderReturnsJson() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);

        requestMock.requestUri = "/query";
        requestMock.parameters.put("query", "SELECT ?x WHERE {}");
        requestMock.headers.put("Accept", "*/*");
        h.handle(null, baseRequest, requestMock, responseMock);
        assertEquals(200, responseMock.statusCode);
        assertEquals("application/sparql-results+json", responseMock.headers.get("Content-Type"));
    }

    @Test
    public void testMultipleAcceptHeaderReturnsKnown() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);

        requestMock.requestUri = "/query";
        requestMock.parameters.put("query", "SELECT ?x WHERE {}");
        requestMock.headers.put("Accept", "text/html, application/xml");
        h.handle(null, baseRequest, requestMock, responseMock);
        assertEquals(200, responseMock.statusCode);
        assertEquals("application/sparql-results+xml", responseMock.headers.get("Content-Type"));
    }

    @Test
    public void testShowContentTypes() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);

        requestMock.requestUri = "/query";
        h.handle(null, baseRequest, requestMock, responseMock);
        assertEquals(406, responseMock.statusCode);
        assertEquals("Supported formats SELECT query:"
            + "\n- SPARQL/XML (mimeTypes=application/sparql-results+xml, application/xml; ext=srx, xml)"
            + "\n- BINARY (mimeTypes=application/x-binary-rdf-results-table; ext=brt)"
            + "\n- SPARQL/JSON (mimeTypes=application/sparql-results+json, application/json; ext=srj, json)"
            + "\n- SPARQL/CSV (mimeTypes=text/csv; ext=csv)"
            + "\n- SPARQL/TSV (mimeTypes=text/tab-separated-values; ext=tsv)"
            + "\n"
            + "\nSupported formats DESCRIBE query:"
            + "\n- RDF/XML (mimeTypes=application/rdf+xml, application/xml; ext=rdf, rdfs, owl, xml)"
            + "\n- N-Triples (mimeTypes=application/n-triples, text/plain; ext=nt)"
            + "\n- Turtle (mimeTypes=text/turtle, application/x-turtle; ext=ttl)"
            + "\n- N3 (mimeTypes=text/n3, text/rdf+n3; ext=n3)"
            + "\n- TriX (mimeTypes=application/trix; ext=xml, trix)"
            + "\n- TriG (mimeTypes=application/trig, application/x-trig; ext=trig)"
            + "\n- BinaryRDF (mimeTypes=application/x-binary-rdf; ext=brf)"
            + "\n- N-Quads (mimeTypes=application/n-quads, text/x-nquads, text/nquads; ext=nq)"
            + "\n- JSON-LD (mimeTypes=application/ld+json; ext=jsonld)"
            + "\n- RDF/JSON (mimeTypes=application/rdf+json; ext=rj)"
            + "\n- RDFa (mimeTypes=application/xhtml+xml, application/html, text/html; ext=xhtml, html)\n", responseMock.response.toString());
        assertEquals("text/plain", responseMock.getHeader("Content-Type"));
    }

    @Test
    public void testContentType() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);

        requestMock.requestUri = "/query";
        requestMock.parameters.put("query", "SELECT ?x WHERE {}");
        requestMock.headers.put("Accept", "application/xml");
        h.handle(null, baseRequest, requestMock, responseMock);
        assertEquals(200, responseMock.statusCode);
        assertEquals("application/sparql-results+xml", responseMock.getHeader("Content-Type"));
    }

    @Test
    public void testXMerescoTriplestoreQueryTime() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);

        requestMock.requestUri = "/query";
        requestMock.parameters.put("query", "SELECT ?x WHERE {}");
        h.handle(null, baseRequest, requestMock, responseMock);
        assertEquals(200, responseMock.statusCode);
        int time = Integer.parseInt(responseMock.getHeader("X-Meresco-Triplestore-QueryTime"));
        assertTrue(time + "", time >= 0);
        assertTrue(time + "", time <= 1000);
    }

    @Test
    public void testExport() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);

        requestMock.requestUri = "/export";
        requestMock.parameters.put("identifier", "identifier");
        h.handle(null, baseRequest, requestMock, responseMock);
        assertEquals(1, tsmock.actions.size());
        assertEquals("export:identifier", tsmock.actions.get(0));
    }

    @Test
    public void testImport() throws Exception {
        TSMock tsmock = new TSMock();
        HttpHandler h = new HttpHandler(tsmock);
        String trig = "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . \n" +
"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . \n" +
"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . \n" +
"\n" +
"<uri:aContext> { \n" +
"        <uri:aSubject> <uri:aPredicate> \"a literal  value\" . \n" +
"}";
        requestMock.requestUri = "/import";
        requestMock.body = trig;
        h.handle(null, baseRequest, requestMock, responseMock);
        assertEquals(1, tsmock.actions.size());
        assertEquals("import:" + trig, tsmock.actions.get(0));
    }

    class RequestMock implements HttpServletRequest {

        String requestUri;
        String body;
        String method = "GET";
        Map<String, String> parameters = new HashMap<>();
        Map<String, String> headers = new HashMap<>();

        @Override
        public AsyncContext getAsyncContext() {return null;}

        @Override
        public Object getAttribute(String arg0) {return null;}

        @Override
        public Enumeration<String> getAttributeNames() {return null;}

        @Override
        public String getCharacterEncoding() {return null;}

        @Override
        public int getContentLength() {return 0;}

        @Override
        public long getContentLengthLong() {return 0;}

        @Override
        public String getContentType() {return null;}

        @Override
        public DispatcherType getDispatcherType() {return null;}

        @Override
        public ServletInputStream getInputStream() throws IOException {return null;}

        @Override
        public String getLocalAddr() {return null;}

        @Override
        public String getLocalName() {return null;}

        @Override
        public int getLocalPort() {return 0;}

        @Override
        public Locale getLocale() {return null;}

        @Override
        public Enumeration<Locale> getLocales() {return null;}

        @Override
        public String getParameter(String arg0) {
            return this.parameters.get(arg0);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> params = new HashMap<>();
            for (String key : parameters.keySet())
                params.put(key, new String[] {parameters.get(key)});
            return params;
        }

        @Override
        public Enumeration<String> getParameterNames() {return null;}

        @Override
        public String[] getParameterValues(String arg0) {return null;}

        @Override
        public String getProtocol() {return null;}

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new StringReader(body));
        }

        @Override
        public String getRealPath(String arg0) {return null;}

        @Override
        public String getRemoteAddr() {return null;}

        @Override
        public String getRemoteHost() {return null;}

        @Override
        public int getRemotePort() {return 0;}

        @Override
        public RequestDispatcher getRequestDispatcher(String arg0) {return null;}

        @Override
        public String getScheme() {return null;}

        @Override
        public String getServerName() {return null;}

        @Override
        public int getServerPort() {return 0;}

        @Override
        public ServletContext getServletContext() {return null;}

        @Override
        public boolean isAsyncStarted() {return false;}

        @Override
        public boolean isAsyncSupported() {return false;}

        @Override
        public boolean isSecure() {return false;}

        @Override
        public void removeAttribute(String arg0) {}

        @Override
        public void setAttribute(String arg0, Object arg1) {}

        @Override
        public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {}

        @Override
        public AsyncContext startAsync() throws IllegalStateException {return null;}

        @Override
        public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) throws IllegalStateException {return null;}

        @Override
        public boolean authenticate(HttpServletResponse arg0) throws IOException, ServletException {return false;}

        @Override
        public String changeSessionId() {return null;}

        @Override
        public String getAuthType() {return null;}

        @Override
        public String getContextPath() {return null;}

        @Override
        public Cookie[] getCookies() {return null;}

        @Override
        public long getDateHeader(String arg0) {return 0;}

        @Override
        public String getHeader(String arg0) {
            return headers.get(arg0);
        }

        @Override
        public Enumeration<String> getHeaderNames() {return null;}

        @Override
        public Enumeration<String> getHeaders(String arg0) {return null;}

        @Override
        public int getIntHeader(String arg0) {return 0;}

        @Override
        public String getMethod() {return method;}

        @Override
        public Part getPart(String arg0) throws IOException, ServletException {return null;}

        @Override
        public Collection<Part> getParts() throws IOException, ServletException {return null;}

        @Override
        public String getPathInfo() {return null;}

        @Override
        public String getPathTranslated() {return null;}

        @Override
        public String getQueryString() {return null;}

        @Override
        public String getRemoteUser() {return null;}

        @Override
        public String getRequestURI() {return requestUri;}

        @Override
        public StringBuffer getRequestURL() {return null;}

        @Override
        public String getRequestedSessionId() {return null;}

        @Override
        public String getServletPath() {return null;}

        @Override
        public HttpSession getSession() {return null;}

        @Override
        public HttpSession getSession(boolean arg0) {return null;}

        @Override
        public Principal getUserPrincipal() {return null;}

        @Override
        public boolean isRequestedSessionIdFromCookie() {return false;}

        @Override
        public boolean isRequestedSessionIdFromURL() {return false;}

        @Override
        public boolean isRequestedSessionIdFromUrl() {return false;}

        @Override
        public boolean isRequestedSessionIdValid() {return false;}

        @Override
        public boolean isUserInRole(String arg0) {return false;}

        @Override
        public void login(String arg0, String arg1) throws ServletException {}

        @Override
        public void logout() throws ServletException {}

        @Override
        public <T extends HttpUpgradeHandler> T upgrade(Class<T> arg0) throws IOException, ServletException {return null;}
    }

    class ResponseMock implements HttpServletResponse {

        int statusCode;
        StringBuilder response = new StringBuilder();
        Map<String, String> headers = new HashMap<>();

        @Override
        public void flushBuffer() throws IOException {}

        @Override
        public int getBufferSize() {return 0; }

        @Override
        public String getCharacterEncoding() {return null; }

        @Override
        public String getContentType() {return null; }

        @Override
        public Locale getLocale() {return null; }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {return null; }

        @Override
        public PrintWriter getWriter() throws IOException {
            return new PrintWriter(new StringBuilderWriter() {
                @Override
                public void write(String s, int off, int len) {
                    response.append(s);
                }
            });
        }

        @Override
        public boolean isCommitted() {return false; }

        @Override
        public void reset() {}

        @Override
        public void resetBuffer() {}

        @Override
        public void setBufferSize(int arg0) {}

        @Override
        public void setCharacterEncoding(String arg0) {}

        @Override
        public void setContentLength(int arg0) {}

        @Override
        public void setContentLengthLong(long arg0) {}

        @Override
        public void setContentType(String arg0) {}

        @Override
        public void setLocale(Locale arg0) {}

        @Override
        public void addCookie(Cookie arg0) {}

        @Override
        public void addDateHeader(String arg0, long arg1) {}

        @Override
        public void addHeader(String arg0, String arg1) {}

        @Override
        public void addIntHeader(String arg0, int arg1) {}

        @Override
        public boolean containsHeader(String arg0) {return false; }

        @Override
        public String encodeRedirectURL(String arg0) {return null; }

        @Override
        public String encodeRedirectUrl(String arg0) {return null; }

        @Override
        public String encodeURL(String arg0) {return null; }

        @Override
        public String encodeUrl(String arg0) {return null; }

        @Override
        public String getHeader(String arg0) {
            return headers.get(arg0);
        }

        @Override
        public Collection<String> getHeaderNames() {return null; }

        @Override
        public Collection<String> getHeaders(String arg0) {return null; }

        @Override
        public int getStatus() {return 0; }

        @Override
        public void sendError(int arg0) throws IOException {}

        @Override
        public void sendError(int arg0, String arg1) throws IOException {}

        @Override
        public void sendRedirect(String arg0) throws IOException {}

        @Override
        public void setDateHeader(String arg0, long arg1) {}

        @Override
        public void setHeader(String arg0, String arg1) {
            headers.put(arg0, arg1);
        }

        @Override
        public void setIntHeader(String arg0, int arg1) {}

        @Override
        public void setStatus(int arg0) {
            statusCode = arg0;
        }

        @Override
        public void setStatus(int arg0, String arg1) {}
    }

    class BaseRequestMock extends Request {

        public BaseRequestMock(HttpChannel<?> channel, HttpInput<?> input) {
            super(channel, input);
        }

    }
}
