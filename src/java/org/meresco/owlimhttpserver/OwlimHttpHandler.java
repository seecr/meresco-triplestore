package org.meresco.owlimhttpserver;

import org.openrdf.rio.RDFFormat;



public class OwlimHttpHandler {
    TripleStore ts;
    public OwlimHttpHandler(TripleStore ts) {
        this.ts = ts;
    }

    public void add(String queryString, String httpBody) {
        String identifier = queryString;
        ts.addRDF(identifier, httpBody, RDFFormat.RDFXML);
    }
}

