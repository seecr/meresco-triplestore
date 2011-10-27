
package org.meresco.owlimhttpserver;

import java.util.List;
import java.util.ArrayList;

public class TLMock extends TransactionLog {
    public List<String> actions = new ArrayList<String>();

    public void add(String identifier, String data) {
        actions.add("add:" + identifier + "|" + data);
    }

    public void delete(String identifier) {
        actions.add("delete:" + identifier);
    }
}

