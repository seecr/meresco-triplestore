package org.meresco.owlimhttpserver;

import org.openrdf.rio.RDFFormat;

public interface TripleStore {
    void addRDF(String identifier, String httpBody, RDFFormat format);
}

