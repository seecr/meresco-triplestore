/* begin license *
 *
 * The Meresco Owlim package consists out of a HTTP server written in Java that
 * provides access to an Owlim Triple store, as well as python bindings to
 * communicate as a client with the server.
 *
 * Copyright (C) 2011-2014 Seecr (Seek You Too B.V.) http://seecr.nl
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

package org.meresco.triplestore;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.openrdf.query.parser.ParsedGraphQuery;
import org.openrdf.query.parser.QueryParserUtil;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.QueryResultIO;

import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;

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
import org.openrdf.repository.Repository;
import org.openrdf.repository.sail.SailGraphQuery;
import org.openrdf.repository.sail.SailQuery;

import org.openrdf.rio.Rio;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFHandlerException;

public class SesameTriplestore implements Triplestore {
    File directory;
    Repository repository;
    RepositoryConnection writeConnection = null;

    public SesameTriplestore() {}

    public SesameTriplestore(File directory) {
        this.directory = directory;
    }

    public void startup() {
        try {
            repository.initialize();
            this.writeConnection = repository.getConnection();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    public void add(String identifier, String rdfData) throws RDFParseException {
        URI context = new URIImpl(identifier);
        StringReader reader = new StringReader(rdfData);
        try {
            this.writeConnection.add(reader, "", RDFFormat.RDFXML, context);
            this.writeConnection.commit();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addTriple(String tripleData) {
        String[] values = tripleData.split("\\|");
        if (values.length != 3) {
            throw new IllegalArgumentException("Not a triple: \"" + tripleData + "\"");
        }
        Value object = null;
        try {
            object = new URIImpl(values[2]);
        } catch (IllegalArgumentException e) {
            object = new LiteralImpl(values[2]);
        }
        try {
            this.writeConnection.add(new URIImpl(values[0]), new URIImpl(values[1]), object);
            this.writeConnection.commit();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(String identifier) {
        URI context = new URIImpl(identifier);
        try {
            this.writeConnection.clear(context);
            this.writeConnection.commit();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeTriple(String tripleData) {
        String[] values = tripleData.split("\\|");
        Value object = null;
        try {
            object = new URIImpl(values[2]);
        } catch (IllegalArgumentException e) {
            object = new LiteralImpl(values[2]);
        }
        try {
            this.writeConnection.remove(new URIImpl(values[0]), new URIImpl(values[1]), object);
            this.writeConnection.commit();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
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

    public String executeGraphQuery(String sparQL, RDFFormat resultFormat) throws MalformedQueryException {
        RepositoryConnection conn = null;
        String result = null;
        try {
            try {
                conn = repository.getConnection();
                GraphQuery graphQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, sparQL);
                GraphQueryResult graphQueryResult = graphQuery.evaluate();
                ByteArrayOutputStream o = new ByteArrayOutputStream();
                QueryResultIO.write(graphQueryResult, resultFormat, o);
                result = o.toString("UTF-8");
                graphQueryResult.close();
            } catch (QueryEvaluationException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (RDFHandlerException e) {
                throw new RuntimeException(e);
            } finally {
                close(conn);
            }
            return result;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    public String executeTupleQuery(String sparQL, TupleQueryResultFormat resultFormat) throws MalformedQueryException {
        RepositoryConnection conn = null;
        String result = null;
        try {
            try {
                conn = repository.getConnection();
                TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, sparQL);
                TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
                ByteArrayOutputStream o = new ByteArrayOutputStream();
                QueryResultIO.write(tupleQueryResult, resultFormat, o);
                result = o.toString("UTF-8");
                tupleQueryResult.close();
            } catch (QueryEvaluationException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (TupleQueryResultHandlerException e) {
                throw new RuntimeException(e);
            } finally {
                close(conn);
            }
            return result;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
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
    	try {
            this.writeConnection.close();
            repository.shutDown();
        } catch (RepositoryException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void export(String identifier) {
        RepositoryConnection conn = null;
        OutputStream writer = null;
        RDFFormat format = RDFFormat.TRIG;
        try {
            conn = repository.getConnection();
            File backupDir = new File(directory, "backups");
            backupDir.mkdirs();
            File exportFile = new File(backupDir, "backup-" + identifier + ".trig.gz");
            writer = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(exportFile)));
            RDFWriter rdfWriter = Rio.createWriter(format, writer);
            conn.export(rdfWriter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                writer.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            close(conn);
        }
    }

    public void importTrig(String trigData) {
        StringReader reader = new StringReader(trigData);
        try {
            this.writeConnection.add(reader, "", RDFFormat.TRIG);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (RDFParseException e) {
            throw new RuntimeException(e);
        }
    }

    public void undoCommit() {}

}
