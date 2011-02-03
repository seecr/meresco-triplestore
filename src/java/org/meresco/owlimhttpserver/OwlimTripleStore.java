package org.meresco.owlimhttpserver;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

import com.ontotext.trree.owlim_ext.Repository;
import com.ontotext.trree.owlim_ext.SailImpl;

import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.QueryResultIO;

import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandlerException;

import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;

import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;


public class OwlimTripleStore implements TripleStore {
    File dir;
    SailRepository repository;

    public OwlimTripleStore(File directory, String storageName) {
        dir = directory;
        SailImpl owlimSail = new SailImpl();
        repository = new SailRepository(owlimSail);
        owlimSail.setParameter(Repository.PARAM_STORAGE_FOLDER, storageName);
        owlimSail.setParameter("ruleset", "empty");
        try {
            repository.setDataDir(directory);
            repository.initialize();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    public void addRDF(String identifier, String rdfData, RDFFormat format) {
        URI context = new URIImpl(identifier);
        StringReader reader = new StringReader(rdfData);
        RepositoryConnection conn = null;
        try {
            conn = repository.getConnection();
            conn.setAutoCommit(false);
            conn.add(reader, "", format, context);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (RDFParseException e) {
            throw new RuntimeException(e);
        } finally {
            close(conn);
        }
    }

    public void delete(String identifier) {
        URI context = new URIImpl(identifier);
        RepositoryConnection conn = null;
        try {
            conn = repository.getConnection();
            conn.setAutoCommit(false);
            conn.clear(context);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        } finally {
            close(conn);
        }
    }

    public long size() {
        RepositoryConnection conn = null;
        try {
            conn = repository.getConnection();
            return conn.size();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        } finally {
            close(conn);
        }
    }


    public String executeQuery(String sparQL) {
        RepositoryConnection conn = null;
        TupleQuery tupleQuery = null;
        TupleQueryResult tupleQueryResult = null;
        String result = null;
        try {

            try {
                conn = repository.getConnection();
                tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, sparQL);
                tupleQueryResult = tupleQuery.evaluate();
                ByteArrayOutputStream o = new ByteArrayOutputStream();
                QueryResultIO.write(tupleQueryResult, TupleQueryResultFormat.JSON, o);
                result = o.toString("UTF-8");
                tupleQueryResult.close();
            } catch (QueryEvaluationException e) {
                e.printStackTrace();
            } catch (MalformedQueryException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (TupleQueryResultHandlerException e) {
                throw new RuntimeException(e);
            }
            return result;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        } finally {
            close(conn);
        }
    }

    public RepositoryResult<Statement> getStatements(Resource subject, URI predicate, Value object) {
        RepositoryConnection conn = null;
        try {
            conn = repository.getConnection();
            return conn.getStatements(subject, predicate, object, false);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        } finally {
            close(conn);
        }
    }


    private void close(RepositoryConnection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
