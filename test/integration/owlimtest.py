## begin license ##
# 
# The Meresco Owlim package consists out of a HTTP server written in Java that
# provides access to an Owlim Triple store, as well as python bindings to
# communicate as a client with the server. 
# 
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

from shutil import rmtree, copyfile
from os.path import join, abspath, isdir
from os import kill, waitpid, WNOHANG, system, symlink, makedirs
from simplejson import loads
from urllib import urlencode
from urllib2 import urlopen, Request
from signal import SIGKILL
from time import time
from threading import Thread

from seecr.test.utils import getRequest, postRequest
from seecr.test.integrationtestcase import IntegrationTestCase


class OwlimTest(IntegrationTestCase):
    def testOne(self):
        self.assertTrue('"vars": [ "x" ]' in urlopen("http://localhost:%s/query?%s" % (self.owlimPort, urlencode(dict(query='SELECT ?x WHERE {}')))).read())

    def testKillTripleStoreSavesState(self):
        postRequest(self.owlimPort, "/add?identifier=uri:record", """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <rdf:Description>
            <rdf:type>uri:testKillTripleStoreSavesState</rdf:type>
        </rdf:Description>
    </rdf:RDF>""", parse=False)
        json = self.query('SELECT ?x WHERE {?x ?y "uri:testKillTripleStoreSavesState"}')
        self.assertEquals(1, len(json['results']['bindings']))

        rmtree(join(self.owlimDataDir, "transactionLog"))
        self.restartOwlimServer()

        json = self.query('SELECT ?x WHERE {?x ?y "uri:testKillTripleStoreSavesState"}')
        self.assertEquals(1, len(json['results']['bindings']))

    def testKillTripleStoreRecoversFromTransactionLog(self):
        postRequest(self.owlimPort, "/add?identifier=uri:record", """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <rdf:Description>
            <rdf:type>uri:testKillTripleStoreRecoversFromTransactionLog</rdf:type>
        </rdf:Description>
    </rdf:RDF>""", parse=False)
        json = self.query('SELECT ?x WHERE {?x ?y "uri:testKillTripleStoreRecoversFromTransactionLog"}')
        self.assertEquals(1, len(json['results']['bindings']))

        kill(self.pids['owlim'], SIGKILL)
        waitpid(self.pids['owlim'], WNOHANG)
        self.startOwlimServer()

        json = self.query('SELECT ?x WHERE {?x ?y "uri:testKillTripleStoreRecoversFromTransactionLog"}')
        self.assertEquals(1, len(json['results']['bindings']))

    def xxxtestKillAndRestoreLargeTransactionLogTiming(self):
        postRequest(self.owlimPort, "/add?identifier=uri:record", """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <rdf:Description>
            <rdf:type>uri:testKillTripleStoreRecoversFromTransactionLog</rdf:type>
        </rdf:Description>
    </rdf:RDF>""", parse=False)
        json = self.query('SELECT ?x WHERE {?x ?y "uri:testKillTripleStoreRecoversFromTransactionLog"}')
        self.assertEquals(1, len(json['results']['bindings']))

        kill(self.pids['owlim'], SIGKILL)
        waitpid(self.pids['owlim'], WNOHANG)

        bigTestTransactionLogPath = '/home/zp/owlim_translog_1348054481457000' # or whatever path to big transaction log

        rmtree(join(self.integrationTempdir, 'owlim-data/transactionLog'))
        isdir("integration/transactionLog") or makedirs("integration/transactionLog")
        symlink(abspath("integration/transactionLog"), join(self.integrationTempdir, 'owlim-data/transactionLog'))
        target = join(self.integrationTempdir, 'owlim-data/transactionLog/current')
        copyfile(bigTestTransactionLogPath, target)
        print time()
        self.startOwlimServer()
        print time()

    def testDeleteRecord(self):
        postRequest(self.owlimPort, "/add?identifier=uri:record", """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <rdf:Description>
            <rdf:type>uri:testDelete</rdf:type>
        </rdf:Description>
    </rdf:RDF>""", parse=False)
        json = self.query('SELECT ?x WHERE {?x ?y "uri:testDelete"}')
        self.assertEquals(1, len(json['results']['bindings']))

        postRequest(self.owlimPort, "/update?identifier=uri:record", """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <rdf:Description>
            <rdf:type>uri:testDeleteUpdated</rdf:type>
        </rdf:Description>
    </rdf:RDF>""", parse=False)
        json = self.query('SELECT ?x WHERE {?x ?y "uri:testDelete"}')
        self.assertEquals(0, len(json['results']['bindings']))
        json = self.query('SELECT ?x WHERE {?x ?y "uri:testDeleteUpdated"}')
        self.assertEquals(1, len(json['results']['bindings']))

        postRequest(self.owlimPort, "/delete?identifier=uri:record", "", parse=False)
        json = self.query('SELECT ?x WHERE {?x ?y "uri:testDelete"}')
        self.assertEquals(0, len(json['results']['bindings']))
        json = self.query('SELECT ?x WHERE {?x ?y "uri:testDeleteUpdated"}')
        self.assertEquals(0, len(json['results']['bindings']))

        postRequest(self.owlimPort, "/add?identifier=uri:record", """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <rdf:Description>
            <rdf:type>uri:testDelete</rdf:type>
        </rdf:Description>
    </rdf:RDF>""", parse=False)
        json = self.query('SELECT ?x WHERE {?x ?y "uri:testDelete"}')
        self.assertEquals(1, len(json['results']['bindings']))

    def testAddAndRemoveTriple(self):
        json = self.query('SELECT ?obj WHERE { <uri:subject> <uri:predicate> ?obj }')
        self.assertEquals(0, len(json['results']['bindings']))

        header, body = postRequest(self.owlimPort, "/addTriple", "uri:subject|uri:predicate|uri:object", parse=False)
        self.assertTrue("200" in header, header)

        json = self.query('SELECT ?obj WHERE { <uri:subject> <uri:predicate> ?obj }')
        self.assertEquals(1, len(json['results']['bindings']))

        header, body = postRequest(self.owlimPort, "/removeTriple", "uri:subject|uri:predicate|uri:object", parse=False)
        self.assertTrue("200" in header, header)
        json = self.query('SELECT ?obj WHERE { <uri:subject> <uri:predicate> ?obj }')
        self.assertEquals(0, len(json['results']['bindings']))

    def testAddPerformance(self):
        totalTime = 0
        try:
            for i in range(10):
                postRequest(self.owlimPort, "/add?identifier=uri:record", """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
            <rdf:Description>
                <rdf:type>uri:testFirst%s</rdf:type>
            </rdf:Description>
        </rdf:RDF>""" % i, parse=False)
            number = 1000
            for i in range(number):
                start = time()
                postRequest(self.owlimPort, "/add?identifier=uri:record", """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
            <rdf:Description>
                <rdf:type>uri:testSecond%s</rdf:type>
            </rdf:Description>
        </rdf:RDF>""" % i, parse=False)
                totalTime += time() - start

            self.assertTiming(0.00015, totalTime / number, 0.0075)
        finally:
            postRequest(self.owlimPort, "/delete?identifier=uri:record", "")

    def testAddPerformanceInCaseOfThreads(self):
        number = 25
        threads = []
        responses = []
        try:
            for i in range(number):
                def doAdd(i=i):
                    header, body = postRequest(self.owlimPort, "/add?identifier=uri:record", """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
            <rdf:Description>
                <rdf:type>uri:testSecond%s</rdf:type>
            </rdf:Description>
        </rdf:RDF>""" % i, parse=False)
                    responses.append((header, body))
                threads.append(Thread(target=doAdd))

            for thread in threads:
                thread.start()
            for thread in threads:
                thread.join()

            for header, body in responses:
                self.assertTrue('200 OK' in header, header + '\r\n\r\n' + body)
        finally:
            postRequest(self.owlimPort, "/delete?identifier=uri:record", "")

    def testFailingCommitKillsTripleStore(self):
        postRequest(self.owlimPort, "/add?identifier=uri:record", """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <rdf:Description>
            <rdf:type>uri:testFailingCommitKillsTripleStore</rdf:type>
        </rdf:Description>
    </rdf:RDF>""", parse=False)

        headers, body = getRequest(self.owlimPort, "/query", arguments={'query': 'SELECT ?x WHERE {?x ?y "uri:testFailingCommitKillsTripleStore"}'}, parse=False)
        json = loads(body)
        self.assertEquals(1, len(json['results']['bindings']))

        system("chmod 0555 %s" % self.owlimDataDir)
        try:
            header, body = postRequest(self.owlimPort, "/add?identifier=uri:record", """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
            <rdf:Description>
                <rdf:type>uri:testFailingCommitKillsTripleStore2</rdf:type>
            </rdf:Description>
        </rdf:RDF>""", parse=False)
            self.assertTrue("500" in header, header)
        finally:
            system("chmod 0755 %s" % self.owlimDataDir)
        self.startOwlimServer()

        headers, body = getRequest(self.owlimPort, "/query", arguments={'query': 'SELECT ?x WHERE {?x ?y "uri:testFailingCommitKillsTripleStore"}'}, parse=False)
        json = loads(body)
        self.assertEquals(1, len(json['results']['bindings']))


        headers, body = getRequest(self.owlimPort, "/query", arguments={'query': 'SELECT ?x WHERE {?x ?y "uri:testFailingCommitKillsTripleStore2"}'}, parse=False)
        json = loads(body)
        self.assertEquals(1, len(json['results']['bindings']))

    def testAcceptHeaders(self):
        postRequest(self.owlimPort, "/add?identifier=uri:record", """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <rdf:Description>
            <rdf:type>uri:test:acceptHeaders</rdf:type>
        </rdf:Description>
    </rdf:RDF>""", parse=False)

        request = Request('http://localhost:%s/query?%s' % (self.owlimPort, urlencode({'query': 'SELECT ?x WHERE {?x ?y "uri:test:acceptHeaders"}'})), headers={"Accept" : "application/xml"})
        contents = urlopen(request).read()
        self.assertEqualsWS("""<?xml version='1.0' encoding='UTF-8'?>
<sparql xmlns='http://www.w3.org/2005/sparql-results#'>
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
</sparql>""", contents)

        headers, body = getRequest(self.owlimPort, "/query", arguments={'query': 'SELECT ?x WHERE {?x ?y "uri:test:acceptHeaders"}'}, additionalHeaders={"Accept" : "image/jpg"}, parse=False)

        self.assertEquals(["HTTP/1.1 406 Not Acceptable", "Content-type: text/plain"], headers.split('\r\n')[:2])
        self.assertEqualsWS("""Supported formats:
- SPARQL/XML (mimeTypes=application/sparql-results+xml, application/xml; ext=srx, xml)
- BINARY (mimeTypes=application/x-binary-rdf-results-table; ext=brt)
- SPARQL/JSON (mimeTypes=application/sparql-results+json; ext=srj)""", body)

    def testMimeTypeArgument(self):
        postRequest(self.owlimPort, "/add?identifier=uri:record", """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <rdf:Description rdf:about="uri:test:mimeType">
            <rdf:value>Value</rdf:value>
        </rdf:Description>
    </rdf:RDF>""", parse=False)

        request = Request('http://localhost:%s/query?%s' % (self.owlimPort, urlencode({'query': 'SELECT ?x WHERE {?x ?y "Value"}', 'mimeType': 'application/sparql-results+xml'})))
        contents = urlopen(request).read()
        self.assertEqualsWS("""<?xml version='1.0' encoding='UTF-8'?>
<sparql xmlns='http://www.w3.org/2005/sparql-results#'>
    <head>
        <variable name='x'/>
    </head>
    <results>
        <result>
            <binding name='x'>
                <uri>uri:test:mimeType</uri>
            </binding>
        </result>
    </results>
</sparql>""", contents)

    def query(self, query):
        return loads(urlopen('http://localhost:%s/query?%s' % (self.owlimPort,
            urlencode(dict(query=query)))).read())

