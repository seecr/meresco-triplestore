package org.meresco.owlimhttpserver;

import org.openrdf.rio.RDFFormat;



public class OwlimHttpHandler {
    TripleStore ts;
    public OwlimHttpHandler(TripleStore ts) {
        this.ts = ts;
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

