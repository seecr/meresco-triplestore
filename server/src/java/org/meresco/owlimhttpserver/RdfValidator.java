/* begin license *
 * 
 * The Meresco Owlim package consists out of a HTTP server written in Java that
 * provides access to an Owlim Triple store, as well as python bindings to
 * communicate as a client with the server. 
 * 
 * Copyright (C) 2012 Seecr (Seek You Too B.V.) http://seecr.nl
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

import org.openrdf.rio.Rio;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFHandler;
import org.openrdf.model.Statement;

import java.io.StringReader;
import java.io.IOException;

public class RdfValidator {
    RDFParser parser;

    public RdfValidator() {
        this.parser = Rio.createParser(RDFFormat.RDFXML);
        this.parser.setRDFHandler(new RDFHandler(){
             public void endRDF() {}
             public void handleComment(String comment) {} 
             public void handleNamespace(String prefix, String uri) {}
             public void handleStatement(Statement st) {}
             public void startRDF() {}
        });
        this.parser.setVerifyData(true);
    }

    public void validate(String identifier, String rdfBody) throws IOException, RDFParseException, RDFHandlerException {

        this.parser.parse(new StringReader(rdfBody), identifier);
    }
}
