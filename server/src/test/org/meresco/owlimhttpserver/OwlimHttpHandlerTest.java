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
import java.io.IOException;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import static org.meresco.owlimhttpserver.Utils.parseQS;


public class OwlimHttpHandlerTest {


    public class OwlimHttpHandlerMock extends OwlimHttpHandler {
        public List<Object> actions = new ArrayList<Object>();
        private Exception _exception = null;

        public OwlimHttpHandlerMock() { super(null, null); }
        public OwlimHttpHandlerMock(Exception e) { 
            super(null, null); 
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


    @Test public void testAddRDF() throws TransactionLogException, IOException {
        TSMock tsmock = new TSMock();
        TLMock tlmock = new TLMock();
        OwlimHttpHandler h = new OwlimHttpHandler(tlmock, tsmock);
        String queryString = "identifier=identifier";
        String httpBody = "<rdf/>";
        h.addRDF(parseQS(queryString), httpBody);
        assertEquals(Arrays.asList("add:identifier" + "|" + httpBody), tlmock.actions);
    }

    @Test public void testDeleteRDF() throws TransactionLogException, IOException {
        TSMock tsmock = new TSMock();
        TLMock tlmock = new TLMock();
        OwlimHttpHandler h = new OwlimHttpHandler(tlmock, tsmock);
        String queryString = "identifier=identifier";
        h.deleteRDF(parseQS(queryString));
        assertEquals(Arrays.asList("delete:identifier"), tlmock.actions);
    }

    @Test public void testUpdateRDF() throws TransactionLogException, IOException {
        TSMock tsmock = new TSMock();
        TLMock tlmock = new TLMock();
        OwlimHttpHandler h = new OwlimHttpHandler(tlmock, tsmock);
        String httpBody = "<rdf/>";
        h.updateRDF(parseQS("identifier=id0"), httpBody);
        assertEquals(Arrays.asList(
                        "delete:id0",
                        "add:id0|<rdf/>"), 
                     tlmock.actions);
        h.updateRDF(parseQS("identifier=id1"), httpBody);
        h.updateRDF(parseQS("identifier=id0"), httpBody);
        assertEquals(Arrays.asList(
                        "delete:id0",
                        "add:id0|<rdf/>", 
                        "delete:id1",
                        "add:id1|<rdf/>",
                        "delete:id0",
                        "add:id0|<rdf/>"), 
                     tlmock.actions);
    }

    @Test public void testSparQL() throws TransactionLogException {
        TSMock tsmock = new TSMock();
        TLMock tlmock = new TLMock();
        OwlimHttpHandler h = new OwlimHttpHandler(tlmock, tsmock);
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

        HttpExchangeMock exchange = new HttpExchangeMock("/query?query=SELECT%20?x%20?y%20?z%20WHERE%20%7B%20?x%20?y%20?z%20%7D", "");
        h.handle(exchange);
        assertEquals(2, h.actions.size());
        assertEquals("executeQuery", h.actions.get(0));
        QueryParameters qp = (QueryParameters) h.actions.get(1);
        assertEquals("SELECT ?x ?y ?z WHERE { ?x ?y ?z }", qp.singleValue("query"));
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
