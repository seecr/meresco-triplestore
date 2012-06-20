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

import java.util.List;
import java.io.File;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import static org.meresco.owlimhttpserver.Utils.createTempDirectory;
import static org.meresco.owlimhttpserver.Utils.deleteDirectory;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.LiteralImpl;

public class OwlimTripleStoreTest {
    OwlimTripleStore ts;
    File tempdir;

    @Before
    public void setUp() throws Exception {
        tempdir = createTempDirectory();
        ts = new OwlimTripleStore(tempdir, "storageName");
    }

    @After
    public void tearDown() throws Exception {
        deleteDirectory(tempdir);
    }

    @Test
    public void testOne() {
        assertEquals(tempdir.getAbsolutePath(), ts.dir.getAbsolutePath());
        assertTrue(new File(new File(tempdir, "storageName"), "entities").isFile());
    }

    static final String rdf = "<?xml version='1.0'?>" +
        "<rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'" +
        "             xmlns:exterms='http://www.example.org/terms/'>" + 
        "  <rdf:Description rdf:about='http://www.example.org/index.html'>" +
        "      <exterms:creation-date>August 16, 1999</exterms:creation-date>" +
        "      <rdf:value>A.M. Özman Yürekli</rdf:value>" + 
        "  </rdf:Description>" +
        "</rdf:RDF>";

    @Test
    public void testAddRDFGetStatements() throws Exception {
        long startingPoint = ts.size();
        ts.addRDF("uri:id0", rdf);
        RepositoryResult<Statement> statements = ts.getStatements(null, null, null);
        assertEquals(startingPoint + 2, statements.asList().size());
        List<Statement> statementList = ts.getStatements(new URIImpl("http://www.example.org/index.html"), null, null).asList();
        assertEquals(2, statementList.size());
        assertEquals(new LiteralImpl("August 16, 1999"), statementList.get(0).getObject());
        assertEquals(new LiteralImpl("A.M. Özman Yürekli"), statementList.get(1).getObject());
    }

    @Test
    public void testDelete() throws Exception {
        ts.addRDF("uri:id0", rdf);
        long startingPoint = ts.size();
        ts.delete("uri:id0");
        assertEquals(startingPoint - 2, ts.size());
    }

    @Test
    public void testSparql() throws Exception {
        String answer = null;

        ts.addRDF("uri:id0", rdf);
        answer = ts.executeQuery("SELECT ?x ?y ?z WHERE {?x ?y ?z}");
        assertTrue(answer.indexOf("\"z\": { \"type\": \"literal\", \"value\": \"A.M. Özman Yürekli\" },") > -1);
        assertTrue(answer.endsWith("\n}"));
    }
    
    @Test
    public void testSparqlResultInXml() throws Exception {
        String answer = null;

        ts.addRDF("uri:id0", rdf);
        answer = ts.executeQuery("SELECT ?x ?y ?z WHERE {?x ?y ?z}", TupleQueryResultFormat.SPARQL);
        assertTrue(answer.startsWith("<?xml"));
        assertTrue(answer.indexOf("<literal>A.M. Özman Yürekli</literal>") > -1);
        assertTrue(answer.endsWith("</sparql>\n"));
    }

    @Test
    public void testShutdown() throws Exception {
        ts.addRDF("uri:id0", rdf);
        ts.shutdown();
        OwlimTripleStore ts = new OwlimTripleStore(tempdir, "storageName");
        RepositoryResult<Statement> statements = ts.getStatements(null, null, null);
        assertEquals(2, statements.asList().size());
    }

    @Test
    public void testShutdownFails() throws Exception {
        File tsPath = new File(tempdir, "anotherOne");
        ts = new OwlimTripleStore(tempdir, "anotherOne");
        ts.shutdown();
        ts.startup();
        File contextFile = new File(tsPath, "Contexts.ids");
        Process process = Runtime.getRuntime().exec("chmod 0000 " + contextFile);
        process.waitFor();
        try {
            ts.shutdown();
            fail("Triplestore shouldn't shutdown correctly");
        } catch (Exception e) {
            assertTrue(e.toString().contains("org.openrdf.repository.RepositoryException"));
        }
    }
}
