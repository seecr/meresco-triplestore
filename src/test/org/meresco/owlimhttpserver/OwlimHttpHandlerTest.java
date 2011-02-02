package org.meresco.owlimhttpserver;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import org.openrdf.rio.RDFFormat;


public class OwlimHttpHandlerTest {
    public class TSMock implements TripleStore {
        public List<String> adds = new ArrayList<String>();
        public List<String> deletes = new ArrayList<String>();

        public void addRDF(String identifier, String data, RDFFormat format) {
            adds.add(data);
        }

        public void delete(String identifier) {
            deletes.add(identifier);
        }
    }


    @Test public void testAddRDF() {
        TSMock tsmock = new TSMock();
        OwlimHttpHandler h = new OwlimHttpHandler(tsmock);
        String queryString = "identifier";
        String httpBody = "<rdf/>";
        h.addRDF(queryString, httpBody);
        assertEquals(Arrays.asList(httpBody), tsmock.adds);
    }

    @Test public void testDeleteRDF() {
        TSMock tsmock = new TSMock();
        OwlimHttpHandler h = new OwlimHttpHandler(tsmock);
        final String queryString = "identifier";
        h.deleteRDF(queryString);
        assertEquals(Arrays.asList(queryString), tsmock.deletes);
    }

    @Test public void testUpdateRDF() {
        TSMock tsmock = new TSMock();
        OwlimHttpHandler h = new OwlimHttpHandler(tsmock);
        String queryString = "identifier";
        String httpBody = "<rdf/>";
        h.updateRDF(queryString, httpBody);
        assertEquals(Arrays.asList(queryString), tsmock.deletes);
        assertEquals(Arrays.asList(httpBody), tsmock.adds);
    }




}
