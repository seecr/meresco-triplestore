/* begin license *
 * 
 * The Meresco Owlim package consists out of a HTTP server written in Java that
 * provides access to an Owlim Triple store, as well as python bindings to
 * communicate as a client with the server. 
 * 
 * Copyright (C) 2011-2012 Seecr (Seek You Too B.V.) http://seecr.nl
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
import java.util.ArrayList;
import org.openrdf.model.URI;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;

public class TLMock extends TransactionLog {
    public List<String> actions = new ArrayList<String>();

    public void add(String identifier, String data) {
        actions.add("add:" + identifier + "|" + data);
    }

    public void delete(String identifier) {
        actions.add("delete:" + identifier);
    }

    public void addTriple(String filedata) {
        actions.add("addTriple:" + filedata);
    }

    public void removeTriple(String filedata) {
        actions.add("removeTriple:" + filedata);
    }
}

