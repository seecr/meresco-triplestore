package org.meresco.owlimhttpserver;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;
import java.util.ArrayList;

import org.openrdf.rio.RDFFormat;

public class OwlimHttpHandlerTest {
    public class TSMock implements TripleStore {
        public List<String> adds = new ArrayList<String>();

        public void addRDF(String identifier, String data, RDFFormat format) {
            adds.add(data);
        }
    }


    @Test public void testAdd() {
        TSMock tsmock = new TSMock();
        OwlimHttpHandler h = new OwlimHttpHandler(tsmock);
        String queryString = "identifier";
        String httpBody = "<rdf/>";
        h.add(queryString, httpBody);
        assertEquals(1, tsmock.adds.size());

    }



}
