package org.meresco.owlimhttpserver;

import org.junit.Test;
import static org.junit.Assert.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.Headers;

import java.net.InetSocketAddress;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import static org.meresco.owlimhttpserver.Utils.parseQS;

import org.openrdf.rio.RDFFormat;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.model.Statement;


public class OwlimHttpHandlerTest {
    public class TSMock implements TripleStore {
        public List<String> actions = new ArrayList<String>();

        public void addRDF(String identifier, String data, RDFFormat format) {
            actions.add("add:" + identifier + "|" + data);
        }

        public void delete(String identifier) {
            actions.add("delete:" + identifier);
        }

        public String executeQuery(String sparQL) {
            actions.add("executeQuery:" + sparQL);
            return "<result/>";
        }

        public RepositoryResult<Statement> getStatements(Resource subj, URI pred, Value obj) {
            throw new UnsupportedOperationException("!");
        }
    }


    public class OwlimHttpHandlerMock extends OwlimHttpHandler {
        public List<Object> actions = new ArrayList<Object>();
        private Exception _exception = null;

        public OwlimHttpHandlerMock() { super(null); }
        public OwlimHttpHandlerMock(Exception e) { 
            super(null); 
            _exception = e; 
        }

        public void updateRDF(QueryParameters params, String httpBody) {
            if (_exception != null) {
                throw new RuntimeException(_exception);
            }
            actions.add("updateRDF");
            actions.add(params);
            actions.add(httpBody);
        }
        public void addRDF(QueryParameters params, String httpBody) {
            if (_exception != null) {
                throw new RuntimeException(_exception);
            }
            actions.add("addRDF");
            actions.add(params);
            actions.add(httpBody);
        }
        public void deleteRDF(QueryParameters params) {
            if (_exception != null) {
                throw new RuntimeException(_exception);
            }
            actions.add("deleteRDF");
            actions.add(params);
        }
        public String executeQuery(QueryParameters params) {
            if (_exception != null) {
                throw new RuntimeException(_exception);
            }
            actions.add("executeQuery");
            actions.add(params);
            return "QUERYRESULT";
        }
    }

    public class HttpExchangeMock extends HttpExchange {
        private java.net.URI _requestURI;
        private String _requestBody;
        private ByteArrayOutputStream _responseStream;
        public int responseCode;
        public String responseBody;

        public HttpExchangeMock(String requestURI, String requestBody) throws Exception {
            super();
            _requestURI = new java.net.URI(requestURI);
            _requestBody = requestBody;
            _responseStream = new ByteArrayOutputStream();
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
        public Headers getResponseHeaders() { return null; }
        public Headers getRequestHeaders() { return null; }
    }


    @Test public void testAddRDF() {
        TSMock tsmock = new TSMock();
        OwlimHttpHandler h = new OwlimHttpHandler(tsmock);
        String queryString = "identifier=identifier";
        String httpBody = "<rdf/>";
        h.addRDF(parseQS(queryString), httpBody);
        assertEquals(Arrays.asList("add:identifier" + "|" + httpBody), tsmock.actions);
    }

    @Test public void testDeleteRDF() {
        TSMock tsmock = new TSMock();
        OwlimHttpHandler h = new OwlimHttpHandler(tsmock);
        String queryString = "identifier=identifier";
        h.deleteRDF(parseQS(queryString));
        assertEquals(Arrays.asList("delete:identifier"), tsmock.actions);
    }

    @Test public void testUpdateRDF() {
        TSMock tsmock = new TSMock();
        OwlimHttpHandler h = new OwlimHttpHandler(tsmock);
        String httpBody = "<rdf/>";
        h.updateRDF(parseQS("identifier=id0"), httpBody);
        assertEquals(Arrays.asList(
                        "delete:id0",
                        "add:id0|<rdf/>"), 
                     tsmock.actions);
        h.updateRDF(parseQS("identifier=id1"), httpBody);
        h.updateRDF(parseQS("identifier=id0"), httpBody);
        assertEquals(Arrays.asList(
                        "delete:id0",
                        "add:id0|<rdf/>", 
                        "delete:id1",
                        "add:id1|<rdf/>",
                        "delete:id0",
                        "add:id0|<rdf/>"), 
                     tsmock.actions);
    }

    @Test public void testSparQL() {
        TSMock tsmock = new TSMock();
        OwlimHttpHandler h = new OwlimHttpHandler(tsmock);
        String queryString = "query=SELECT+%3Fx+%3Fy+%3Fz+WHERE+%7B+%3Fx+%3Fy+%3Fz+%7D"; 
        String result = h.executeQuery(parseQS(queryString));
        
        assertEquals(Arrays.asList("executeQuery:SELECT ?x ?y ?z WHERE { ?x ?y ?z }"), 
                     tsmock.actions);

    }

    @Test public void testAddDispatch() throws Exception {
        OwlimHttpHandlerMock h = new OwlimHttpHandlerMock();

        HttpExchangeMock exchange = new HttpExchangeMock("/add?id=IDENTIFIER", "<rdf/>");
        h.handle(exchange);
        assertEquals(3, h.actions.size());
        assertEquals("addRDF", h.actions.get(0));
        QueryParameters qp = (QueryParameters) h.actions.get(1);
        assertEquals("IDENTIFIER", qp.singleValue("id"));
        assertEquals("<rdf/>", h.actions.get(2));

        assertEquals(200, exchange.responseCode);
    }

    @Test public void testUpdateDispatch() throws Exception {
        OwlimHttpHandlerMock h = new OwlimHttpHandlerMock();

        HttpExchangeMock exchange = new HttpExchangeMock("/update?id=IDENTIFIER", "<rdf/>");
        h.handle(exchange);
        assertEquals(3, h.actions.size());
        assertEquals("updateRDF", h.actions.get(0));
        QueryParameters qp = (QueryParameters) h.actions.get(1);
        assertEquals("IDENTIFIER", qp.singleValue("id"));
        assertEquals("<rdf/>", h.actions.get(2));
        assertEquals(200, exchange.responseCode);
    }

    @Test public void testDeleteDispatch() throws Exception {
        OwlimHttpHandlerMock h = new OwlimHttpHandlerMock();

        HttpExchangeMock exchange = new HttpExchangeMock("/delete?id=IDENTIFIER", "");
        h.handle(exchange);
        assertEquals(2, h.actions.size());
        assertEquals("deleteRDF", h.actions.get(0));
        QueryParameters qp = (QueryParameters) h.actions.get(1);
        assertEquals("IDENTIFIER", qp.singleValue("id"));
        assertEquals(200, exchange.responseCode);
    }

    @Test public void testQueryDispatch() throws Exception {
        OwlimHttpHandlerMock h = new OwlimHttpHandlerMock();

        HttpExchangeMock exchange = new HttpExchangeMock("/query?query=SPARQL+STATEMENT", "");
        h.handle(exchange);
        assertEquals(2, h.actions.size());
        assertEquals("executeQuery", h.actions.get(0));
        QueryParameters qp = (QueryParameters) h.actions.get(1);
        assertEquals("SPARQL STATEMENT", qp.singleValue("query"));
        assertEquals(200, exchange.responseCode);
        assertEquals("QUERYRESULT", exchange.getOutput());
    }

    @Test public void test404ForOtherRequests() throws Exception {
        OwlimHttpHandlerMock h = new OwlimHttpHandlerMock();

        HttpExchangeMock exchange = new HttpExchangeMock("/", "");
        h.handle(exchange);
        assertEquals(0, h.actions.size());
        assertEquals(404, exchange.responseCode);
    }
    
    @Test public void test500ForExceptions() throws Exception {
        OwlimHttpHandlerMock h = new OwlimHttpHandlerMock(new Exception());

        HttpExchangeMock exchange = new HttpExchangeMock("/add", "");
        h.handle(exchange);
        assertEquals(0, h.actions.size());
        assertEquals(500, exchange.responseCode);
        assertTrue(exchange.getOutput().startsWith("java.lang.RuntimeException: java.lang.Exception"));
    }
}
