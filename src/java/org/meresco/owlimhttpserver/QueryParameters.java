/* begin license *
 * 
 * "OwlimHttpServer" provides a simple HTTP interface to an OWLim triplestore. 
 * 
 * Copyright (C) 2011 Seecr (Seek You Too B.V.) http://seecr.nl
 * Copyright (C) 2011 Seek You Too B.V. (CQ2) http://www.cq2.nl
 * 
 * This file is part of "OwlimHttpServer"
 * 
 * "OwlimHttpServer" is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * "OwlimHttpServer" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with "OwlimHttpServer"; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * end license */

package org.meresco.owlimhttpserver;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

class QueryParameters extends HashMap<String, ArrayList<String>> {
    public String singleValue(String key) {
        List<String> l = get(key);
        if (l != null && l.size() == 1) {
            return l.get(0);
        }
        return null;
    }
}
