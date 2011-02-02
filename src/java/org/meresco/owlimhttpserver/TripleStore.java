package org.meresco.owlimhttpserver;

import org.openrdf.rio.RDFFormat;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.model.Statement;


public interface TripleStore {
    void addRDF(String identifier, String httpBody, RDFFormat format);

    void delete(String identifier);

    String executeQuery(String sparQL);

    RepositoryResult<Statement> getStatements(Resource subj, URI pred, Value obj);
}

