## begin license ##
#
# The Meresco Triplestore package consists out of a HTTP server written in Java that
# provides access to an Triplestore with a Sesame Interface, as well as python bindings to
# communicate as a client with the server.
#
# Copyright (C) 2010-2011 Maastricht University Library http://www.maastrichtuniversity.nl/web/Library/home.htm
# Copyright (C) 2010-2011 Seek You Too B.V. (CQ2) http://www.cq2.nl
# Copyright (C) 2011-2014 Seecr (Seek You Too B.V.) http://seecr.nl
#
# This file is part of "Meresco Triplestore"
#
# "Meresco Triplestore" is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# "Meresco Triplestore" is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with "Meresco Triplestore"; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
#
## end license ##

from urllib import urlencode

from seecr.test import SeecrTestCase, CallTrace

from weightless.core import compose
from weightless.io import Suspend
from meresco.triplestore import HttpClient, InvalidRdfXmlException, Uri, Literal, BNode
from meresco.triplestore.httpclient import X_MERESCO_TRIPLESTORE_QUERYTIME
from decimal import Decimal
from time import sleep


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
        client._urlopen = lambda *args, **kwargs: (RESULT_HEADER, RESULT_JSON)
        gen = compose(client.executeQuery('SPARQL'))
        try:
            gen.next()
        except StopIteration, e:
            result = e.args[0]
        self.assertEquals(PARSED_RESULT_JSON, result)

    def testAddSynchronous(self):
        client = HttpClient(host="localhost", port=9999, synchronous=True)
        client._urlopen = lambda *args, **kwargs: (RESULT_HEADER, "SOME RESPONSE")
        list(compose(client.add(identifier="id", partname="ignored", data=RDFDATA)))

        toSend = []
        client._urlopen = lambda url, data: (RESULT_HEADER, toSend.append((url, data)))
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

    def testExecuteQueryWithtriplestoreHostPortFromObserver(self):
        triplestoreClient = HttpClient()
        observer = CallTrace(returnValues={'triplestoreServer': ('localhost', 1234)})
        triplestoreClient.addObserver(observer)
        kwargs = []
        def httpget(**_kwargs):
            kwargs.append(_kwargs)
            sleep(0.1)
            s = Suspend()
            response = yield s
            result = s.getResult()
            raise StopIteration(result)
        triplestoreClient._httpget = httpget

        g = compose(triplestoreClient.executeQuery("select ?x where {}"))
        self._resultFromServerResponse(g, RESULT_JSON)
        self.assertEquals(['triplestoreServer', 'handleQueryTimes'], observer.calledMethodNames())
        self.assertEquals("/query?" + urlencode(dict(query='select ?x where {}')), kwargs[0]['request'])
        self.assertEquals('localhost', kwargs[0]['host'])
        self.assertEquals(1234, kwargs[0]['port'])
        self.assertEquals(['index', 'queryTime'], observer.calledMethods[1].kwargs.keys())
        self.assertEquals(Decimal('0.042'), observer.calledMethods[1].kwargs['index'])
        self.assertTrue(0.1 < float(observer.calledMethods[1].kwargs['queryTime']) < 0.11)

    def testUpdateWithtriplestoreHostPortFromObserver(self):
        triplestoreClient = HttpClient()
        observer = CallTrace(returnValues={'triplestoreServer': ('localhost', 1234)})
        triplestoreClient.addObserver(observer)
        kwargs = []
        def httppost(**_kwargs):
            kwargs.append(_kwargs)
            s = Suspend()
            response = yield s
            result = s.getResult()
            raise StopIteration(result)
        triplestoreClient._httppost = httppost

        g = compose(triplestoreClient.addTriple("uri:subject", "uri:predicate", "value"))
        self._resultFromServerResponse(g, "")
        self.assertEquals(['triplestoreServer'], observer.calledMethodNames())
        self.assertEquals("/addTriple", kwargs[0]['request'])
        self.assertEquals('localhost', kwargs[0]['host'])
        self.assertEquals(1234, kwargs[0]['port'])

    def testErrorInHttpGet(self):
        triplestoreClient = HttpClient()
        observer = CallTrace(returnValues={'triplestoreServer': ('localhost', 1234)})
        triplestoreClient.addObserver(observer)
        def httpget(**_kwargs):
            raise ValueError("error")
            yield
        triplestoreClient._httpget = httpget

        g = compose(triplestoreClient.executeQuery("select ?x where {}"))
        self.assertRaises(ValueError, lambda: self._resultFromServerResponse(g, RESULT_JSON))

    def testErrorInAdd(self):
        triplestoreClient = HttpClient()
        observer = CallTrace(returnValues={'triplestoreServer': ('localhost', 1234)})
        triplestoreClient.addObserver(observer)
        def httppost(**_kwargs):
            raise ValueError("error")
            yield
        triplestoreClient._httppost = httppost

        g = compose(triplestoreClient.addTriple("uri:subject", "uri:predicate", "value"))
        self.assertRaises(ValueError, lambda: self._resultFromServerResponse(g, ""))

    def _resultFromServerResponse(self, g, data, responseStatus='200'):
        s = g.next()
        self.assertEquals(Suspend, type(s))
        s(CallTrace('reactor'), lambda: None)
        s.resume(_RESULT_HEADER % responseStatus + '\r\n\r\n' + data)
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
    }, {
        u'p': Uri(u'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'),
        u's': Uri(u'http://www.w3.org/1999/02/22-rdf-syntax-ns#subject'),
        u'o': Literal(u'3.14')
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
                        },
                        {
                                "o": { "type": "typed-literal", "value": "3.14", "datatype": "http://www.w3.org/2001/XMLSchema#double"},
                                "p": { "type": "uri", "value": "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#type" },
                                "s": { "type": "uri", "value": "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#subject" }
                        }
                ]
        }
}"""

_RESULT_HEADER = '\r\n'.join([
    'HTTP/1.1 %s Ok',
    '%s: 42' % X_MERESCO_TRIPLESTORE_QUERYTIME
    ])
RESULT_HEADER = _RESULT_HEADER % 200

RDFDATA = "<rdf>should be RDF</rdf>"
