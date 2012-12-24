## begin license ##
# 
# The Meresco Owlim package consists out of a HTTP server written in Java that
# provides access to an Owlim Triple store, as well as python bindings to
# communicate as a client with the server. 
# 
# Copyright (C) 2010-2011 Maastricht University Library http://www.maastrichtuniversity.nl/web/Library/home.htm
# Copyright (C) 2010-2011 Seek You Too B.V. (CQ2) http://www.cq2.nl
# Copyright (C) 2011-2012 Seecr (Seek You Too B.V.) http://seecr.nl
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

from weightless.core import compose
from weightless.io import Suspend
from meresco.owlim import HttpClient, InvalidRdfXmlException, Uri, Literal


class HttpClientTest(SeecrTestCase):
    def testAdd(self):
        client = HttpClient(host="localhost", port=9999)
        g = compose(client.add(identifier="id", partname="ignored", data=RDFDATA))
        self._resultFromServerResponse(g, "SOME RESPONSE")

        g = compose(client.add(identifier="id", partname="ignored", data=RDFDATA))
        self.assertRaises(
            IOError, 
            lambda: self._resultFromServerResponse(g, "Error description", responseStatus='500'))

        toSend = []
        client._send = lambda path, body: toSend.append((path, body))
        list(compose(client.add(identifier="id", partname="ignored", data=RDFDATA)))
        self.assertEquals([("/update?identifier=id", RDFDATA)], toSend)

    def testAddTriple(self):
        client = HttpClient(host="localhost", port=9999)
        toSend = []
        client._send = lambda path, body: toSend.append((path, body))
        list(compose(client.addTriple(subj="uri:subj", pred="uri:pred", obj="uri:obj")))
        self.assertEquals([("/addTriple", 'uri:subj|uri:pred|uri:obj')], toSend)

    def testRemoveTriple(self):
        client = HttpClient(host="localhost", port=9999)
        toSend = []
        client._send = lambda path, body: toSend.append((path, body))
        list(compose(client.removeTriple(subj="uri:subj", pred="uri:pred", obj="uri:obj")))
        self.assertEquals([("/removeTriple", 'uri:subj|uri:pred|uri:obj')], toSend)

    def testDelete(self):
        client = HttpClient(host="localhost", port=9999)
        g = compose(client.delete(identifier="id"))
        self._resultFromServerResponse(g, "SOME RESPONSE")

        g = compose(client.delete(identifier="id"))
        self.assertRaises(
            IOError, 
            lambda: self._resultFromServerResponse(g, "Error description", responseStatus="500"))

        toSend = []
        client._send = lambda path, body: toSend.append((path, body))
        list(compose(client.delete(identifier="id")))
        self.assertEquals([("/delete?identifier=id", None)], toSend)

    def testValidate(self):
        client = HttpClient(host="localhost", port=9999)
        g = compose(client.validate(data=RDFDATA))
        self._resultFromServerResponse(g, "Ok")

        g = compose(client.validate(data=RDFDATA))
        try:
            self._resultFromServerResponse(g, "Invalid\nError description")
            self.fail("should not get here.")
        except InvalidRdfXmlException, e:
            self.assertEquals("Invalid\nError description", str(e))

        toSend = []
        def mockSend(path, body):
            toSend.append((path, body))
            raise StopIteration('header', 'body')
        client._send = mockSend
        list(compose(client.validate(data=RDFDATA)))
        self.assertEquals([("/validate", RDFDATA)], toSend)

    def testCreateSparQL(self):
        client = HttpClient(host="localhost", port=9999)
        self.assertEquals("SELECT DISTINCT ?s ?p ?o WHERE { ?s ?p ?o }", client._createSparQL(subj=None, pred=None, obj=None))

        self.assertEquals("SELECT DISTINCT ?p ?o WHERE { <http://cq2.org/person/0001> ?p ?o }", client._createSparQL(subj="http://cq2.org/person/0001"))
        
        self.assertEquals("SELECT DISTINCT ?o WHERE { <http://cq2.org/person/0001> <http://xmlns.com/foaf/0.1/name> ?o }", client._createSparQL(subj="http://cq2.org/person/0001", pred="http://xmlns.com/foaf/0.1/name"))

        self.assertEquals("SELECT DISTINCT * WHERE { <http://cq2.org/person/0001> <http://xmlns.com/foaf/0.1/name> \"object\" }", client._createSparQL(subj="http://cq2.org/person/0001", pred="http://xmlns.com/foaf/0.1/name", obj="object"))


    def testExecuteQuery(self):
        client = HttpClient(host="localhost", port=9999)
        gen = compose(client.executeQuery('SPARQL'))
        result = self._resultFromServerResponse(gen, RESULT_JSON)
        self.assertEquals(PARSED_RESULT_JSON, result)

    def testExecuteQueryWithQueryResultFormat(self):
        client = HttpClient(host="localhost", port=9999)
        gen = compose(client.executeQuery('SPARQL', queryResultFormat="application/sparql-results+xml"))
        suspend = gen.next()
        self.assertEquals(Suspend, type(suspend))
        httpgetHeaders = suspend._doNext.__self__.gi_frame.f_locals['headers']
        self.assertEquals({'Accept': 'application/sparql-results+xml'}, httpgetHeaders)

    def testDontParseIfNotJSON(self):
        SPARQL_XML = """<sparql xmlns='http://www.w3.org/2005/sparql-results#'>
    <head>
        <variable name='x'/>
    </head>
    <results>
        <result>
            <binding name='x'>
                <bnode>node1</bnode>
            </binding>
        </result>
    </results>
</sparql>"""
        client = HttpClient(host="localhost", port=9999)
        gen = compose(client.executeQuery('SPARQL', queryResultFormat="application/sparql-results+xml"))
        result = self._resultFromServerResponse(gen, SPARQL_XML)
        self.assertEquals(SPARQL_XML, result)


    def testGetStatements(self):
        client = HttpClient(host="localhost", port=9999)
        gen = compose(client.getStatements(subj='uri:subject'))
        result = self._resultFromServerResponse(gen, RESULT_JSON)
        self.assertEquals(RESULT_SPO, list(result))

    def testExecuteQuerySynchronous(self):
        client = HttpClient(host="localhost", port=9999, synchronous=True)
        client._urlopen = lambda *args, **kwargs: RESULT_JSON
        gen = compose(client.executeQuery('SPARQL'))
        try:
            gen.next()
        except StopIteration, e:
            result = e.args[0]
        self.assertEquals(PARSED_RESULT_JSON, result)

    def testAddSynchronous(self):
        client = HttpClient(host="localhost", port=9999, synchronous=True)
        client._urlopen = lambda *args, **kwargs: "SOME RESPONSE"
        list(compose(client.add(identifier="id", partname="ignored", data=RDFDATA)))

        # list(compose(client.add(identifier="id", partname="ignored", data=RDFDATA)))
        # self.assertRaises(
        #     IOError, 
        #     lambda: self._resultFromServerResponse(g, "Error description", responseStatus='500'))

        toSend = []
        client._urlopen = lambda url, data: toSend.append((url, data))
        list(compose(client.add(identifier="id", partname="ignored", data=RDFDATA)))
        self.assertEquals([("http://localhost:9999/update?identifier=id", RDFDATA)], toSend)

    def _resultFromServerResponse(self, g, data, responseStatus='200'):
        s = g.next()
        self.assertEquals(Suspend, type(s))
        s(CallTrace('reactor'), lambda: None)
        s.resume('HTTP/1.1 %s\r\n\r\n%s' % (responseStatus, data))
        try:
            g.next()
            self.fail("expected StopIteration")
        except StopIteration, e:
            if len(e.args) > 0:
                return e.args[0]

    def to_be_moved_to_integrationtest_testGetStatements(self):
        client = HttpClient(host="localhost", port=9999)
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

RDFDATA = "<rdf>should be RDF</rdf>"
