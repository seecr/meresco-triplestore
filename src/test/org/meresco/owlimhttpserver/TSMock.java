
package org.meresco.owlimhttpserver;

import java.util.List;
import java.util.ArrayList;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.model.Statement;

public class TSMock implements TripleStore {
    public List<String> actions = new ArrayList<String>();

    public void addRDF(String identifier, String data) {
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

