package org.meresco.owlimhttpserver;

import org.junit.Test;
import static org.junit.Assert.*;

public class OwlimHttpServerTest {

    @Test public void testOne() throws Exception {
        OwlimHttpServer s = new OwlimHttpServer(6000, 15);
    }
}
