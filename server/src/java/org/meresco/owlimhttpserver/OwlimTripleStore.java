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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

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
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.Namespace;

import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;

import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;

public class OwlimTripleStore implements TripleStore {
    File dir;
    SailRepository repository;

    OwlimTripleStore() {}

    public OwlimTripleStore(File directory, String storageName) {
        dir = directory;
        SailImpl owlimSail = new SailImpl();
        repository = new SailRepository(owlimSail);
        owlimSail.setParameter(Repository.PARAM_STORAGE_FOLDER, storageName);
        owlimSail.setParameter("ruleset", "empty");
        startup();
    }

    public void startup() {
        try {
            repository.setDataDir(dir);
            repository.initialize();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    public void addRDF(String identifier, String rdfData) {
        URI context = new URIImpl(identifier);
        StringReader reader = new StringReader(rdfData);
        RepositoryConnection conn = null;
        try {
            conn = repository.getConnection();
            conn.setAutoCommit(false);
            conn.add(reader, "", RDFFormat.RDFXML, context);
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

    public void addTriple(String tripleData) {
        RepositoryConnection conn = null;
        String[] values = tripleData.split("\\|");
        Value object = null;
        try {
            object = new URIImpl(values[2]);
        } catch (IllegalArgumentException e) {
            object = new LiteralImpl(values[2]);
        }
        try {
            conn = repository.getConnection();
            conn.setAutoCommit(false);
            conn.add(new URIImpl(values[0]), new URIImpl(values[1]), object);
        } catch (RepositoryException e) {
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

    public void removeTriple(String tripleData) {
        RepositoryConnection conn = null;
        String[] values = tripleData.split("\\|");
        Value object = null;
        try {
            object = new URIImpl(values[2]);
        } catch (IllegalArgumentException e) {
            object = new LiteralImpl(values[2]);
        }
        try {
            conn = repository.getConnection();
            conn.setAutoCommit(false);
            conn.remove(new URIImpl(values[0]), new URIImpl(values[1]), object);
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
        return executeQuery(sparQL, TupleQueryResultFormat.JSON);
    }

    public String executeQuery(String sparQL, TupleQueryResultFormat resultFormat) {
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
                QueryResultIO.write(tupleQueryResult, resultFormat, o);
                result = o.toString("UTF-8");
                tupleQueryResult.close();
            } catch (QueryEvaluationException e) {
                throw new RuntimeException(e);
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

    public List<Namespace> getNamespaces() {
        RepositoryConnection conn = null;
        try {
            conn = repository.getConnection();
            return conn.getNamespaces().asList();
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

    public void shutdown() throws Exception {
        OutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        try {
            System.setErr(ps);
            repository.shutDown();
            if (!os.toString().equals("")) {
                throw new RepositoryException(os.toString());
            }   
        } catch (RepositoryException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(os.toString());
            throw e;
        }   
    }

    public void undoCommit() {}

}
