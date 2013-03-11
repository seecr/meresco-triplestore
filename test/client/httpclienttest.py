## begin license ##
#
# The Meresco Owlim package consists out of a HTTP server written in Java that
# provides access to an Owlim Triple store, as well as python bindings to
# communicate as a client with the server.
#
# Copyright (C) 2010-2011 Maastricht University Library http://www.maastrichtuniversity.nl/web/Library/home.htm
# Copyright (C) 2010-2011 Seek You Too B.V. (CQ2) http://www.cq2.nl
# Copyright (C) 2011-2013 Seecr (Seek You Too B.V.) http://seecr.nl
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

from urllib import urlencode

from seecr.test import SeecrTestCase, CallTrace

from weightless.core import compose
from weightless.io import Suspend
from meresco.owlim import HttpClient, InvalidRdfXmlException, Uri, Literal, BNode


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
        list(compose(client.addTriple(subject="uri:subj", predicate="uri:pred", object="uri:obj")))
        self.assertEquals([("/addTriple", 'uri:subj|uri:pred|uri:obj')], toSend)

    def testRemoveTriple(self):
        client = HttpClient(host="localhost", port=9999)
        toSend = []
        client._send = lambda path, body: toSend.append((path, body))
        list(compose(client.removeTriple(subject="uri:subj", predicate="uri:pred", object="uri:obj")))
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

    def testGetStatementsSparQL(self):
        client = HttpClient(host="localhost", port=9999)
        self.assertEquals("SELECT DISTINCT ?s ?p ?o WHERE { ?s ?p ?o }", ''.join(client._getStatementsSparQL(subject=None, predicate=None, object=None)))

        self.assertEquals("SELECT DISTINCT ?p ?o WHERE { <http://cq2.org/person/0001> ?p ?o }", ''.join(client._getStatementsSparQL(subject="http://cq2.org/person/0001")))

        self.assertEquals("SELECT DISTINCT ?o WHERE { <http://cq2.org/person/0001> <http://xmlns.com/foaf/0.1/name> ?o }", ''.join(client._getStatementsSparQL(subject="http://cq2.org/person/0001", predicate="http://xmlns.com/foaf/0.1/name")))

        self.assertEquals("SELECT DISTINCT * WHERE { <http://cq2.org/person/0001> <http://xmlns.com/foaf/0.1/name> <uri:obj> }", ''.join(client._getStatementsSparQL(subject="http://cq2.org/person/0001", predicate="http://xmlns.com/foaf/0.1/name", object="uri:obj")))

        self.assertEquals("SELECT DISTINCT * WHERE { <http://cq2.org/person/0001> <http://xmlns.com/foaf/0.1/name> \"object\" }", ''.join(client._getStatementsSparQL(subject="http://cq2.org/person/0001", predicate="http://xmlns.com/foaf/0.1/name", object="object")))


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
        gen = compose(client.getStatements(subject='uri:subject'))
        result = self._resultFromServerResponse(gen, RESULT_JSON)
        self.assertEquals(RESULT_SPO, list(result))

    def testGetStatementsGuards(self):
        client = HttpClient(host="localhost", port=9999)
        self.assertRaises(ValueError, lambda: list(compose(client.getStatements(subject='literal'))))
        self.assertRaises(ValueError, lambda: list(compose(client.getStatements(predicate='literal'))))

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

        toSend = []
        client._urlopen = lambda url, data: toSend.append((url, data))
        list(compose(client.add(identifier="id", partname="ignored", data=RDFDATA)))
        self.assertEquals([("http://localhost:9999/update?identifier=id", RDFDATA)], toSend)

    def testExport(self):
        client = HttpClient(host="localhost", port=9999)
        toSend = []
        client._send = lambda path, body: toSend.append((path, body))
        list(compose(client.export(identifier="id")))
        self.assertEquals([("/export?identifier=id", None)], toSend)

    def testImport(self):
        client = HttpClient(host="localhost", port=9999)
        toSend = []
        client._send = lambda path, body: toSend.append((path, body))
        trigData = """@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .

<uri:aContext> {
        <uri:aSubject> <uri:aPredicate> "a literal  value" .
}"""
        list(compose(client.importTrig(data=trigData)))
        self.assertEquals([("/import", trigData)], toSend)

    def testExecuteQueryWithOwlimHostPortFromObserver(self):
        owlimClient = HttpClient()
        observer = CallTrace(returnValues={'owlimServer': ('localhost', 1234)})
        owlimClient.addObserver(observer)
        kwargs = []
        def httpget(**_kwargs):
            kwargs.append(_kwargs)
            s = Suspend()
            response = yield s
            result = s.getResult()
            raise StopIteration(result)
        owlimClient._httpget = httpget

        g = compose(owlimClient.executeQuery("select ?x where {}"))
        self._resultFromServerResponse(g, RESULT_JSON)
        self.assertEquals(['owlimServer'], observer.calledMethodNames())
        self.assertEquals("/query?" + urlencode(dict(query='select ?x where {}')), kwargs[0]['request'])
        self.assertEquals('localhost', kwargs[0]['host'])
        self.assertEquals(1234, kwargs[0]['port'])

    def testUpdateWithOwlimHostPortFromObserver(self):
        owlimClient = HttpClient()
        observer = CallTrace(returnValues={'owlimServer': ('localhost', 1234)})
        owlimClient.addObserver(observer)
        kwargs = []
        def httppost(**_kwargs):
            kwargs.append(_kwargs)
            s = Suspend()
            response = yield s
            result = s.getResult()
            raise StopIteration(result)
        owlimClient._httppost = httppost

        g = compose(owlimClient.addTriple("uri:subject", "uri:predicate", "value"))
        self._resultFromServerResponse(g, "")
        self.assertEquals(['owlimServer'], observer.calledMethodNames())
        self.assertEquals("/addTriple", kwargs[0]['request'])
        self.assertEquals('localhost', kwargs[0]['host'])
        self.assertEquals(1234, kwargs[0]['port'])


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


PARSED_RESULT_JSON = [
    {
        u'p': Uri(u'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'),
        u's': Uri(u'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'),
        u'o': Literal(u'word', lang="eng")
    }, {
        u'p': Uri(u'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'),
        u's': Uri(u'http://www.w3.org/1999/02/22-rdf-syntax-ns#subject'),
        u'o': Literal(u'woord', lang="dut")
    }, {
        u'p': Uri('http://www.w3.org/1999/02/22-rdf-syntax-ns#type'),
        u's': Uri('http://www.w3.org/1999/02/22-rdf-syntax-ns#subject'),
        u'o': BNode('node12345')
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
                        },
                        {
                                "o": { "type": "bnode", "value": "node12345" },
                                "p": { "type": "uri", "value": "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#type" },
                                "s": { "type": "uri", "value": "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#subject" }
                        }
                ]
        }
}"""

RDFDATA = "<rdf>should be RDF</rdf>"
