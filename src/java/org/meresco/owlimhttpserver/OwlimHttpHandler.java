package org.meresco.owlimhttpserver;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.net.URI;

import org.openrdf.rio.RDFFormat;

public class OwlimHttpHandler implements HttpHandler {
    TripleStore ts;
    public OwlimHttpHandler(TripleStore ts) {
        this.ts = ts;
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
            } catch (RuntimeException e) {
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

    public void updateRDF(QueryParameters params, String httpBody) {
        String identifier = params.singleValue("identifier");
        ts.delete(identifier);
        ts.addRDF(identifier, httpBody, RDFFormat.RDFXML);
    }

    public void addRDF(QueryParameters params, String httpBody) {
        String identifier = params.singleValue("identifier");
        ts.addRDF(identifier, httpBody, RDFFormat.RDFXML);
    }

    public void deleteRDF(QueryParameters params) {
        String identifier = params.singleValue("identifier");
        ts.delete(identifier);
    }

    public String executeQuery(QueryParameters params) {
        String query = params.singleValue("query");
        return ts.executeQuery(query);
    }
}
