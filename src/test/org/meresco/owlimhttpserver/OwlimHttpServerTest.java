package org.meresco.owlimhttpserver;

import org.junit.Test;
import static org.junit.Assert.*;

public class OwlimHttpServerTest {

    @Test public void testOne() {
        OwlimHttpServer s = new OwlimHttpServer(6000);
        assertEquals(6000, s.port);
    }



}
