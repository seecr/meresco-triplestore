package org.meresco.owlimhttpserver;

import org.openrdf.rio.RDFFormat;



public class OwlimHttpHandler {
    TripleStore ts;
    public OwlimHttpHandler(TripleStore ts) {
        this.ts = ts;
    }

    public void updateRDF(String queryString, String httpBody) {
        String identifier = queryString;
        ts.delete(identifier);
        ts.addRDF(identifier, httpBody, RDFFormat.RDFXML);
    }

    public void addRDF(String queryString, String httpBody) {
        String identifier = queryString;
        ts.addRDF(identifier, httpBody, RDFFormat.RDFXML);
    }

    public void deleteRDF(String queryString) {
        String identifier = queryString;
        ts.delete(identifier);
    }

    public String executeQuery(String queryString) {
        // TODO: parse queryString properly!
        String query = queryString.substring("query=".length());
        System.out.println(query);
        return ts.executeQuery(query);
    }


}

