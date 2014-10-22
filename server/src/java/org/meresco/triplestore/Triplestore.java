/* begin license *
 *
 * The Meresco Owlim package consists out of a HTTP server written in Java that
 * provides access to an Owlim Triple store, as well as python bindings to
 * communicate as a client with the server.
 *
 * Copyright (C) 2011-2014 Seecr (Seek You Too B.V.) http://seecr.nl
 * Copyright (C) 2011 Seek You Too B.V. (CQ2) http://www.cq2.nl
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

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.Namespace;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.repository.RepositoryResult;
import java.util.List;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFFormat;


public interface Triplestore {
    void add(String identifier, String body) throws RDFParseException;
    void addTriple(String tripleData);

    void delete(String identifier);
    void removeTriple(String tripleData);

    String executeTupleQuery(String sparQL, TupleQueryResultFormat format) throws MalformedQueryException;
    String executeGraphQuery(String sparQL, RDFFormat format) throws MalformedQueryException;

    List<Namespace> getNamespaces();

    void shutdown() throws Exception;

    void startup();

    void export(String identifier);

    void importTrig(String trig);

    long size();
}

