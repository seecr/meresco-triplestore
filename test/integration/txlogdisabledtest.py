## begin license ##
#
# The Meresco Owlim package consists out of a HTTP server written in Java that
# provides access to an Owlim Triple store, as well as python bindings to
# communicate as a client with the server.
#
# Copyright (C) 2013 Seecr (Seek You Too B.V.) http://seecr.nl
#
# This file is part of "Meresco Owlim"
#
# "Meresco Owlim" is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# "Meresco Owlim" is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with "Meresco Owlim"; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
#
## end license ##

from seecr.test import IntegrationTestCase
from seecr.test.utils import postRequest, getRequest

from simplejson import loads

from os import system

class TxLogDisabledTest(IntegrationTestCase):

    def testKillTripleStoreWithoutTsLogWontRecover(self):
        system("chmod -R u-w %s" % self.owlimDataDir) #make owlim's dir inaccessible to force failure
        try:
            header, body = postRequest(self.owlimPort, "/add?identifier=uri:record", """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
            <rdf:Description>
                <rdf:type>uri:testFailingCommitKillsTripleStore</rdf:type>
            </rdf:Description>
        </rdf:RDF>""", parse=False)
            self.assertTrue("200" in header, header)
            headers, body = getRequest(self.owlimPort, "/query", arguments={'query': 'SELECT ?x WHERE {?x ?y "uri:testFailingCommitKillsTripleStore"}'}, parse=False)
            json = loads(body)
            self.assertEquals(1, len(json['results']['bindings']))
            self.stopOwlimServer()
        finally:
            system("chmod -R u+w %s" % self.owlimDataDir)

        self.startOwlimServer()
        headers, body = getRequest(self.owlimPort, "/query", arguments={'query': 'SELECT ?x WHERE {?x ?y "uri:testFailingCommitKillsTripleStore"}'}, parse=False)
        json = loads(body)
        self.assertEquals(0, len(json['results']['bindings']))