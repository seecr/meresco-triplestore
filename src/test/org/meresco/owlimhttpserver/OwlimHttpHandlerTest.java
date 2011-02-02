package org.meresco.owlimhttpserver;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import org.openrdf.rio.RDFFormat;


public class OwlimHttpHandlerTest {
    public class TSMock implements TripleStore {
        public List<String> actions = new ArrayList<String>();

        public void addRDF(String identifier, String data, RDFFormat format) {
            actions.add("add:" + identifier + "|" + data);
        }

        public void delete(String identifier) {
            actions.add("delete:" + identifier);
        }

        public String executeQuery(String sparQL) {
            actions.add("query:" + sparQL);
            return "<result/>";
        }

    }


    @Test public void testAddRDF() {
        TSMock tsmock = new TSMock();
        OwlimHttpHandler h = new OwlimHttpHandler(tsmock);
        String queryString = "identifier";
        String httpBody = "<rdf/>";
        h.addRDF(queryString, httpBody);
        assertEquals(Arrays.asList("add:" + queryString + "|" + httpBody), tsmock.actions);
    }

    @Test public void testDeleteRDF() {
        TSMock tsmock = new TSMock();
        OwlimHttpHandler h = new OwlimHttpHandler(tsmock);
        final String queryString = "identifier";
        h.deleteRDF(queryString);
        assertEquals(Arrays.asList("delete:" + queryString), tsmock.actions);
    }

    @Test public void testUpdateRDF() {
        TSMock tsmock = new TSMock();
        OwlimHttpHandler h = new OwlimHttpHandler(tsmock);
        String httpBody = "<rdf/>";
        h.updateRDF("id0", httpBody);
        assertEquals(Arrays.asList(
                        "delete:id0",
                        "add:id0|<rdf/>"), 
                     tsmock.actions);
        h.updateRDF("id1", httpBody);
        h.updateRDF("id0", httpBody);
        assertEquals(Arrays.asList(
                        "delete:id0",
                        "add:id0|<rdf/>", 
                        "delete:id1",
                        "add:id1|<rdf/>",
                        "delete:id0",
                        "add:id0|<rdf/>"), 
                     tsmock.actions);
    }

    @Test public void testSparQL() {
        TSMock tsmock = new TSMock();
        OwlimHttpHandler h = new OwlimHttpHandler(tsmock);
        String queryString = "query=SELECT+%3Fx+%3Fy+%3Fz+WHERE+%7B+%3Fx+%3Fy+%3Fz+%7D"; 
        String result = h.executeQuery(queryString);
        
        assertEquals(Arrays.asList("query:SELECT ?x ?y ?z WHERE { ?x ?y ?z}"), 
                     tsmock.actions);

    }


}
