package org.meresco.owlimhttpserver;

import java.util.List;
import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import static org.meresco.owlimhttpserver.Utils.createTempDirectory;
import static org.meresco.owlimhttpserver.Utils.deleteDirectory;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.LiteralImpl;

import org.openrdf.rio.RDFFormat;


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
        "  </rdf:Description>" +
        "</rdf:RDF>";

    @Test
    public void testAddRDFGetStatements() throws Exception {
        RepositoryResult<Statement> statements = ts.getStatements(null, null, null);
        int startingPoint = statements.asList().size();
        ts.addRDF("uri:id0", rdf, RDFFormat.RDFXML);
        statements = ts.getStatements(null, null, null);
        assertEquals(startingPoint + 1, statements.asList().size());
        List<Statement> statementList = ts.getStatements(new URIImpl("http://www.example.org/index.html"), null, null).asList();
        assertEquals(1, statementList.size());
        assertEquals(new LiteralImpl("August 16, 1999"), statementList.get(0).getObject());
        


    }

}
