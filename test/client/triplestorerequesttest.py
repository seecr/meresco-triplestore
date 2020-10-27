## begin license ##
#
# The Meresco Triplestore package consists out of a HTTP server written in Java that
# provides access to an Triplestore with a Sesame Interface, as well as python bindings to
# communicate as a client with the server.
#
# Copyright (C) 2010-2011 Maastricht University Library http://www.maastrichtuniversity.nl/web/Library/home.htm
# Copyright (C) 2010-2011 Seek You Too B.V. (CQ2) http://www.cq2.nl
# Copyright (C) 2011-2016 Seecr (Seek You Too B.V.) http://seecr.nl
# Copyright (C) 2015 Koninklijke Bibliotheek (KB) http://www.kb.nl
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


from seecr.test import SeecrTestCase, CallTrace

from weightless.core import consume, retval
from meresco.components.http.utils import CRLF
from meresco.triplestore import InvalidRdfXmlException, NTRIPLES, TriplestoreRequest
from meresco.triplestore.triplestorerequest import X_MERESCO_TRIPLESTORE_QUERYTIME
from meresco.rdf import BNode, Literal, Uri
from decimal import Decimal
from time import sleep
from urllib.parse import urlparse, parse_qs


class TriplestoreRequestTest(SeecrTestCase):
    def setUp(self):
        super(TriplestoreRequestTest, self).setUp()
        self.responseStatus = '200'
        self.responseData = 'SOME RESPONSE'
        def httprequest(**kwargs):
            r = ''.join([
                    _RESULT_HEADER % self.responseStatus,
                    CRLF*2,
                    self.responseData,
                ])
            return r.encode("utf-8")
            yield
        self.observer = CallTrace(methods={'httprequest':httprequest})
        self.request = TriplestoreRequest(host='example.org', port=9999)
        self.request.addObserver(self.observer)

    def testAdd(self):
        consume(self.request.add(identifier="id", partname="ignored", data=RDFDATA))
        self.assertEqual(['httprequest'], self.observer.calledMethodNames())
        httprequestKwargs = self.observer.calledMethods[-1].kwargs
        self.assertEqual({
                'body': RDFDATA,
                'headers': {
                    'Content-Type': 'text/xml'
                },
                'method': 'POST',
                'host': 'example.org',
                'port': 9999,
                'request': '/update?identifier=id',
            }, httprequestKwargs)

    def testAddIOError(self):
        self.responseStatus = '500'
        self.responseData = 'Error Description'

        self.assertRaises(IOError, lambda: consume(self.request.add(identifier="id", partname="ignored", data=RDFDATA)))

    def testAddAsNTRIPLES(self):
        consume(self.request.add(identifier="id", partname="ignored", data=RDFDATA, format=NTRIPLES))
        self.assertEqual(['httprequest'], self.observer.calledMethodNames())
        httprequestKwargs = self.observer.calledMethods[-1].kwargs
        self.assertEqual({'Content-Type': 'text/plain'}, httprequestKwargs['headers'])

    def testAddTriple(self):
        consume(self.request.addTriple(subject="uri:subj", predicate="uri:pred", object="uri:obj"))
        self.assertEqual(['httprequest'], self.observer.calledMethodNames())
        httprequestKwargs = self.observer.calledMethods[-1].kwargs
        self.assertEqual({
                'body': 'uri:subj|uri:pred|uri:obj',
                'headers': {},
                'method': 'POST',
                'host': 'example.org',
                'port': 9999,
                'request': '/addTriple',
            }, httprequestKwargs)

    def testRemoveTriple(self):
        consume(self.request.removeTriple(subject="uri:subj", predicate="uri:pred", object="uri:obj"))
        self.assertEqual(['httprequest'], self.observer.calledMethodNames())
        httprequestKwargs = self.observer.calledMethods[-1].kwargs
        self.assertEqual({
                'body': 'uri:subj|uri:pred|uri:obj',
                'headers': {},
                'method': 'POST',
                'host': 'example.org',
                'port': 9999,
                'request': '/removeTriple',
            }, httprequestKwargs)

    def testDelete(self):
        consume(self.request.delete(identifier="id"))
        self.assertEqual(['httprequest'], self.observer.calledMethodNames())
        httprequestKwargs = self.observer.calledMethods[-1].kwargs
        self.assertEqual({
                'body': None,
                'headers': {},
                'method': 'POST',
                'host': 'example.org',
                'port': 9999,
                'request': '/delete?identifier=id',
            }, httprequestKwargs)

    def testDeleteError(self):
        self.responseStatus = '500'
        self.assertRaises(IOError, lambda: consume(self.request.delete(identifier="id")))

    def testValidate(self):
        self.responseData = 'Ok'
        consume(self.request.validate(data=RDFDATA))
        self.assertEqual(['httprequest'], self.observer.calledMethodNames())
        httprequestKwargs = self.observer.calledMethods[-1].kwargs
        self.assertEqual({
                'body': RDFDATA,
                'headers': {},
                'method': 'POST',
                'host': 'example.org',
                'port': 9999,
                'request': '/validate',
            }, httprequestKwargs)

    def testValidateError(self):
        self.responseData = 'Invalid\nError Description'
        self.assertRaises(InvalidRdfXmlException, lambda: consume(self.request.validate(data=RDFDATA)))

    def testCommit(self):
        consume(self.request.commit())
        self.assertEqual(['httprequest'], self.observer.calledMethodNames())
        httprequestKwargs = self.observer.calledMethods[-1].kwargs
        self.assertEqual({
                'body': None,
                'headers': {},
                'method': 'POST',
                'host': 'example.org',
                'port': 9999,
                'request': '/commit',
            }, httprequestKwargs)

    def testGetStatementsSparQL(self):
        self.assertEqual("SELECT DISTINCT ?s ?p ?o WHERE { ?s ?p ?o }", ''.join(self.request._getStatementsSparQL(subject=None, predicate=None, object=None)))

        self.assertEqual("SELECT DISTINCT ?p ?o WHERE { <http://cq2.org/person/0001> ?p ?o }", ''.join(self.request._getStatementsSparQL(subject="http://cq2.org/person/0001")))

        self.assertEqual("SELECT DISTINCT ?o WHERE { <http://cq2.org/person/0001> <http://xmlns.com/foaf/0.1/name> ?o }", ''.join(self.request._getStatementsSparQL(subject="http://cq2.org/person/0001", predicate="http://xmlns.com/foaf/0.1/name")))

        self.assertEqual("SELECT DISTINCT * WHERE { <http://cq2.org/person/0001> <http://xmlns.com/foaf/0.1/name> <uri:obj> }", ''.join(self.request._getStatementsSparQL(subject="http://cq2.org/person/0001", predicate="http://xmlns.com/foaf/0.1/name", object="uri:obj")))

        self.assertEqual("SELECT DISTINCT * WHERE { <http://cq2.org/person/0001> <http://xmlns.com/foaf/0.1/name> \"object\" }", ''.join(self.request._getStatementsSparQL(subject="http://cq2.org/person/0001", predicate="http://xmlns.com/foaf/0.1/name", object="object")))

    def testExecuteQuery(self):
        self.responseData = RESULT_JSON
        result = retval(self.request.executeQuery('SPARQL'))
        self.assertEqual(PARSED_RESULT_JSON, result)
        self.assertEqual(['httprequest', 'handleQueryTimes'], self.observer.calledMethodNames())
        httprequestKwargs = self.observer.calledMethods[0].kwargs
        self.assertEqual({
                'headers': None,
                'method': 'GET',
                'host': 'example.org',
                'port': 9999,
                'request': '/query?query=SPARQL',
            }, httprequestKwargs)

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
        self.responseData = SPARQL_XML
        result = retval(self.request.executeQuery('SPARQL', queryResultFormat="application/sparql-results+xml"))
        self.assertEqual(SPARQL_XML, result)

    def testGetStatements(self):
        self.responseData = RESULT_JSON
        result = retval(self.request.getStatements(subject='uri:subject'))
        self.assertEqual(RESULT_SPO, list(result))
        self.assertEqual(['httprequest', 'handleQueryTimes'], self.observer.calledMethodNames())
        httprequestKwargs = self.observer.calledMethods[0].kwargs
        request = httprequestKwargs.pop('request')
        self.assertEqual({
                'headers': None,
                'method': 'GET',
                'host': 'example.org',
                'port': 9999,
            }, httprequestKwargs)
        parsed = urlparse(request)
        self.assertEqual('/query', parsed.path)
        self.assertEqual({'query': ['''SELECT DISTINCT ?p ?o WHERE { <uri:subject> ?p ?o }''']}, parse_qs(parsed.query))

    def testGetStatementsGuards(self):
        self.assertRaises(ValueError, lambda: consume(self.request.getStatements(subject='literal')))
        self.assertRaises(ValueError, lambda: consume(self.request.getStatements(predicate='literal')))

    def testExport(self):
        consume(self.request.export(identifier="id"))
        self.assertEqual(['httprequest'], self.observer.calledMethodNames())
        httprequestKwargs = self.observer.calledMethods[0].kwargs
        self.assertEqual({
                'body': None,
                'headers': {},
                'method': 'POST',
                'host': 'example.org',
                'port': 9999,
                'request': '/export?identifier=id',
            }, httprequestKwargs)

    def testImport(self):
        trigData = """@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .

<uri:aContext> {
        <uri:aSubject> <uri:aPredicate> "a literal  value" .
}"""
        consume(self.request.importTrig(data=trigData))
        self.assertEqual(['httprequest'], self.observer.calledMethodNames())
        httprequestKwargs = self.observer.calledMethods[0].kwargs
        self.assertEqual({
                'body': trigData,
                'headers': {},
                'method': 'POST',
                'host': 'example.org',
                'port': 9999,
                'request': '/import',
            }, httprequestKwargs)

    def testExecuteQueryWithtriplestoreHostPortFromObserver(self):
        self.request = TriplestoreRequest()
        self.request.addObserver(self.observer)
        self.observer.returnValues['triplestoreServer'] = ('this.server.nl', 1234)
        self.responseData = RESULT_JSON
        consume(self.request.executeQuery("select ?x where {}"))
        self.assertEqual(['triplestoreServer', 'httprequest', 'handleQueryTimes'], self.observer.calledMethodNames())

        httprequestKwargs = self.observer.calledMethods[1].kwargs
        request = httprequestKwargs.pop('request')
        self.assertEqual({
                'headers': None,
                'method': 'GET',
                'host': 'this.server.nl',
                'port': 1234,
            }, httprequestKwargs)
        parsed = urlparse(request)
        self.assertEqual('/query', parsed.path)
        self.assertEqual({'query': ['''select ?x where {}''']}, parse_qs(parsed.query))

        handleQueryTimesKwargs = self.observer.calledMethods[2].kwargs
        self.assertEqual(['index', 'queryTime'], list(handleQueryTimesKwargs.keys()))
        self.assertEqual(Decimal('0.042'), handleQueryTimesKwargs['index'])
        qt = float(handleQueryTimesKwargs['queryTime'])
        self.assertTrue(0.0 <= qt <0.1, qt)

    def testUpdateWithtriplestoreHostPortFromObserver(self):
        self.request = TriplestoreRequest()
        self.request.addObserver(self.observer)
        self.observer.returnValues['triplestoreServer'] = ('this.server.nl', 1234)
        consume(self.request.addTriple("uri:subject", "uri:predicate", "value"))
        self.assertEqual(['triplestoreServer', 'httprequest'], self.observer.calledMethodNames())

        httprequestKwargs = self.observer.calledMethods[1].kwargs
        self.assertEqual({
                'body': 'uri:subject|uri:predicate|value',
                'headers': {},
                'method': 'POST',
                'host': 'this.server.nl',
                'port': 1234,
                'request': '/addTriple',
            }, httprequestKwargs)

    def testErrorInHttpGet(self):
        self.observer.exceptions['httprequest'] = ValueError('error')
        self.assertRaises(ValueError, lambda: consume(self.request.executeQuery("select ?x where {}")))

PARSED_RESULT_JSON = [
    {
        'p': Uri('http://www.w3.org/1999/02/22-rdf-syntax-ns#type'),
        's': Uri('http://www.w3.org/1999/02/22-rdf-syntax-ns#type'),
        'o': Literal('word', lang="eng")
    }, {
        'p': Uri('http://www.w3.org/1999/02/22-rdf-syntax-ns#type'),
        's': Uri('http://www.w3.org/1999/02/22-rdf-syntax-ns#subject'),
        'o': Literal('woord', lang="dut")
    }, {
        'p': Uri('http://www.w3.org/1999/02/22-rdf-syntax-ns#type'),
        's': Uri('http://www.w3.org/1999/02/22-rdf-syntax-ns#subject'),
        'o': BNode('node12345')
    }, {
        'p': Uri('http://www.w3.org/1999/02/22-rdf-syntax-ns#type'),
        's': Uri('http://www.w3.org/1999/02/22-rdf-syntax-ns#subject'),
        'o': Literal('3.14')
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
