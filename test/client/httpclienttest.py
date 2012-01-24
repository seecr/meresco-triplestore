## begin license ##
# 
# The Meresco Owlim package consists out of a HTTP server written in Java that
# provides access to an Owlim Triple store, as well as python bindings to
# communicate as a client with the server. 
# 
# Copyright (C) 2010-2011 Maastricht University Library http://www.maastrichtuniversity.nl/web/Library/home.htm
# Copyright (C) 2010-2011 Seek You Too B.V. (CQ2) http://www.cq2.nl
# Copyright (C) 2011 Seecr (Seek You Too B.V.) http://seecr.nl
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

from seecr.test import SeecrTestCase, CallTrace

from meresco.core import be, Observable
from meresco.owlim import HttpClient, Uri, Literal


class HttpClientTest(SeecrTestCase):
    def testCreateSparQL(self):
        client = HttpClient(port=9999)
        self.assertEquals("SELECT ?s ?p ?o WHERE { ?s ?p ?o }", client._createSparQL(subj=None, pred=None, obj=None))

        self.assertEquals("SELECT ?p ?o WHERE { <http://cq2.org/person/0001> ?p ?o }", client._createSparQL(subj="http://cq2.org/person/0001"))
        
        self.assertEquals("SELECT ?o WHERE { <http://cq2.org/person/0001> <http://xmlns.com/foaf/0.1/name> ?o }", client._createSparQL(subj="http://cq2.org/person/0001", pred="http://xmlns.com/foaf/0.1/name"))

    def testExecuteQuery(self):
        client = HttpClient(port=9999)
        gen = client.executeQuery('SPARQL')
        result = self.retrieveResult(gen, serverResult=RESULT_JSON)
        self.assertEquals(PARSED_RESULT_JSON, result)

    def testGetStatements(self):
        client = HttpClient(port=9999)
        gen = client.getStatements(subj='uri:subject')
        result = self.retrieveResult(gen, serverResult=RESULT_JSON)
        self.assertEquals(RESULT_SPO, list(result))

    def retrieveResult(self, gen, serverResult):
        httpget = gen.next()
        try:
            gen.send('HEADER\r\n\r\n%s' % serverResult)
            self.fail('expect StopIteration')
        except StopIteration, e:
            return e.args[0]

    def to_be_moved_to_integrationtest_testGetStatements(self):
        client = HttpClient(port=9999)
        def _executeQuery(*args, **kwargs):
            raise StopIteration(RESULT_JSON)
        result = list(client.getStatements())
        self.assertEquals([
            (   u'http://www.w3.org/1999/02/22-rdf-syntax-ns#type', 
                u'http://www.w3.org/1999/02/22-rdf-syntax-ns#type', 
                u'http://www.w3.org/1999/02/22-rdf-syntax-ns#Property'
            ), (
                u'http://www.w3.org/1999/02/22-rdf-syntax-ns#subject', 
                u'http://www.w3.org/1999/02/22-rdf-syntax-ns#type', 
                u'http://www.w3.org/1999/02/22-rdf-syntax-ns#Property'
            )], result)
        
PARSED_RESULT_JSON = [
    {
        u'p': Uri(u'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'), 
        u's': Uri(u'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'), 
        u'o': Literal(u'word', lang="eng")
    }, {
        u'p': Uri(u'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'), 
        u's': Uri(u'http://www.w3.org/1999/02/22-rdf-syntax-ns#subject'), 
        u'o': Literal(u'woord', lang="dut")
    }
]
RESULT_SPO = [ (d['s'], d['p'], d['o']) for d in PARSED_RESULT_JSON]

RESULT_JSON = """{
        "head": {
                "vars": [ "s", "p", "o" ]
        }, 
        "results": {
                "bindings": [
                        {
                                "o": { "type": "literal", "xml:lang": "eng", "value": "word" }, 
                                "p": { "type": "uri", "value": "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#type" }, 
                                "s": { "type": "uri", "value": "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#type" }
                        }, 
                        {
                                "o": { "type": "literal", "xml:lang": "dut", "value": "woord" }, 
                                "p": { "type": "uri", "value": "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#type" }, 
                                "s": { "type": "uri", "value": "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#subject" }
                        } 
                ]
        }
}"""

