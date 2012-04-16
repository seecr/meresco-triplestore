## begin license ##
# 
# The Meresco Owlim package consists out of a HTTP server written in Java that
# provides access to an Owlim Triple store, as well as python bindings to
# communicate as a client with the server. 
# 
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

from integration import IntegrationTestCase
from utils import postRequest

from shutil import rmtree
from os.path import join
from os import remove, kill, waitpid, WNOHANG, system
from simplejson import loads
from urllib import urlopen, urlencode
from signal import SIGKILL
from time import time


class OwlimTest(IntegrationTestCase):
    def testOne(self):
        self.assertTrue('"vars": [ "x" ]' in urlopen("http://localhost:%s/query?%s" % (self.owlimPort, urlencode(dict(query='SELECT ?x WHERE {}')))).read())

    def query(self, query):
        return loads(urlopen('http://localhost:%s/query?%s' % (self.owlimPort,
            urlencode(dict(query=query)))).read())

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

    def testPerformance(self):
        totalTime = 0
        for i in range(10):
            postRequest(self.owlimPort, "/add?identifier=uri:record", """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <rdf:Description>
            <rdf:type>uri:testFirst%s</rdf:type>
        </rdf:Description>
    </rdf:RDF>""" % i, parse=False)
        for i in range(1000):
            start = time()
            postRequest(self.owlimPort, "/add?identifier=uri:record", """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <rdf:Description>
            <rdf:type>uri:testSecond%s</rdf:type>
        </rdf:Description>
    </rdf:RDF>""" % i, parse=False)
            totalTime += time() - start

        self.assertTiming(0.003, totalTime / 500, 0.01)

    def testFailingCommitKillsTripleStore(self):
        postRequest(self.owlimPort, "/add?identifier=uri:record", """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <rdf:Description>
            <rdf:type>uri:testFailingCommitKillsTripleStore</rdf:type>
        </rdf:Description>
    </rdf:RDF>""", parse=False)
        json = loads(urlopen('http://localhost:%s/query?query=SELECT ?x WHERE {?x ?y "uri:testFailingCommitKillsTripleStore"}' % self.owlimPort).read())
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
        json = loads(urlopen('http://localhost:%s/query?query=SELECT ?x WHERE {?x ?y "uri:testFailingCommitKillsTripleStore"}' % self.owlimPort).read())
        self.assertEquals(1, len(json['results']['bindings']))
        json = loads(urlopen('http://localhost:%s/query?query=SELECT ?x WHERE {?x ?y "uri:testFailingCommitKillsTripleStore2"}' % self.owlimPort).read())
        self.assertEquals(1, len(json['results']['bindings']))
