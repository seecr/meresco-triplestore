/* begin license *
 * 
 * The Meresco Owlim package consists out of a HTTP server written in Java that
 * provides access to an Owlim Triple store, as well as python bindings to
 * communicate as a client with the server. 
 * 
 * Copyright (C) 2012-2013 Seecr (Seek You Too B.V.) http://seecr.nl
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


import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;
import org.openrdf.rio.RDFParseException;

public class RdfValidatorTest {
    @Test
    public void testValidateCorrect () throws Exception {
        String rdf = "<?xml version='1.0'?>" +
            "<rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'" +
            "             xmlns:exterms='http://www.example.org/terms/'>" + 
            "  <rdf:Description rdf:about='http://www.example.org/index.html'>" +
            "      <exterms:creation-date>August 16, 1999</exterms:creation-date>" +
            "      <rdf:value>Some Name</rdf:value>" + 
            "  </rdf:Description>" +
            "</rdf:RDF>";
        RdfValidator validator = new RdfValidator();
        validator.validate(rdf);
    }

    @Test
    public void testValidateBad () throws Exception {
        String rdf = "<?xml version='1.0'?>" +
            "<rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'" +
            "             xmlns:dc='http://purl.org/dc/elements/1.1/'" + 
            "             xmlns:exterms='http://www.example.org/terms/'>" + 
            "  <rdf:Description rdf:about='http://www.example.org/index.html'>" +
            "<dc:language xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:type='dcterms:ISO639-2'>dut</dc:language>" +
            "      <rdf:value>Some Name</rdf:value>" + 
            "  </rdf:Description>" +
            "</rdf:RDF>";
        RdfValidator validator = new RdfValidator();
        try {
            validator.validate(rdf);
            fail("Expected RDFParseException");
        } catch(RDFParseException e) {
        }
    }
}
