/* begin license *
 *
 * The Meresco Owlim package consists out of a HTTP server written in Java that
 * provides access to an Owlim Triple store, as well as python bindings to
 * communicate as a client with the server.
 *
 * Copyright (C) 2014 Seecr (Seek You Too B.V.) http://seecr.nl
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

package org.meresco.triplestore;

import com.ontotext.trree.owlim_ext.SailImpl;
import org.openrdf.repository.sail.SailRepository;
import java.io.File;

class OwlimTriplestore extends SesameTriplestore {

    public OwlimTriplestore(File directory, String storageName) {
        super(directory);
        SailImpl owlimSail = new SailImpl();
        this.repository = new SailRepository(owlimSail);
        owlimSail.setParameter(com.ontotext.trree.owlim_ext.Repository.PARAM_STORAGE_FOLDER, storageName);
        owlimSail.setParameter("ruleset", "empty");
        this.repository.setDataDir(directory);
        startup();
    }
}