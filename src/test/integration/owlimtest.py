## begin license ##
# 
# "OwlimHttpServer" provides a simple HTTP interface to an OWLim triplestore. 
# 
# Copyright (C) 2011 Seecr (Seek You Too B.V.) http://seecr.nl
# 
# This file is part of "OwlimHttpServer"
# 
# "OwlimHttpServer" is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
# 
# "OwlimHttpServer" is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with "OwlimHttpServer"; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
# 
## end license ##

from integration import IntegrationTestCase
from utils import postRequest

from shutil import rmtree
from os.path import join
from os import remove, kill, waitpid, WNOHANG
from simplejson import loads
from urllib import urlopen
from signal import SIGKILL

class OwlimTest(IntegrationTestCase):

    def testOne(self):
        self.assertTrue('"vars": [ "x" ]' in urlopen("http://localhost:%s/query?query=SELECT ?x WHERE {}" % self.owlimPort).read())

    def testKillTripleStoreSavesState(self):
        postRequest(self.owlimPort, "/add?identifier=uri:record", """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <rdf:Description>
            <rdf:type>uri:testKillTripleStoreSavesState</rdf:type>
        </rdf:Description>
    </rdf:RDF>""", parse=False)
        json = loads(urlopen('http://localhost:%s/query?query=SELECT ?x WHERE {?x ?y "uri:testKillTripleStoreSavesState"}' % self.owlimPort).read())
        self.assertEquals(1, len(json['results']['bindings']))

        rmtree(join(self.owlimDataDir, "transactionLog"))
        self.restartOwlimServer()

        json = loads(urlopen('http://localhost:%s/query?query=SELECT ?x WHERE {?x ?y "uri:testKillTripleStoreSavesState"}' % self.owlimPort).read())
        self.assertEquals(1, len(json['results']['bindings']))

    def testKillTripleStoreRecoversFromTransactionLog(self):
        postRequest(self.owlimPort, "/add?identifier=uri:record", """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <rdf:Description>
            <rdf:type>uri:testKillTripleStoreRecoversFromTransactionLog</rdf:type>
        </rdf:Description>
    </rdf:RDF>""", parse=False)
        json = loads(urlopen('http://localhost:%s/query?query=SELECT ?x WHERE {?x ?y "uri:testKillTripleStoreRecoversFromTransactionLog"}' % self.owlimPort).read())
        self.assertEquals(1, len(json['results']['bindings']))

        kill(self.pids['owlim'], SIGKILL)
        waitpid(self.pids['owlim'], WNOHANG)
        self.startOwlimServer()

        json = loads(urlopen('http://localhost:%s/query?query=SELECT ?x WHERE {?x ?y "uri:testKillTripleStoreRecoversFromTransactionLog"}' % self.owlimPort).read())
        self.assertEquals(1, len(json['results']['bindings']))

    def testDeleteRecord(self): 
        postRequest(self.owlimPort, "/add?identifier=uri:record", """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <rdf:Description>
            <rdf:type>uri:testDelete</rdf:type>
        </rdf:Description>
    </rdf:RDF>""", parse=False)
        json = loads(urlopen('http://localhost:%s/query?query=SELECT ?x WHERE {?x ?y "uri:testDelete"}' % self.owlimPort).read())
        self.assertEquals(1, len(json['results']['bindings']))

        postRequest(self.owlimPort, "/update?identifier=uri:record", """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <rdf:Description>
            <rdf:type>uri:testDeleteUpdated</rdf:type>
        </rdf:Description>
    </rdf:RDF>""", parse=False)
        json = loads(urlopen('http://localhost:%s/query?query=SELECT ?x WHERE {?x ?y "uri:testDelete"}' % self.owlimPort).read())
        self.assertEquals(0, len(json['results']['bindings']))
        json = loads(urlopen('http://localhost:%s/query?query=SELECT ?x WHERE {?x ?y "uri:testDeleteUpdated"}' % self.owlimPort).read())
        self.assertEquals(1, len(json['results']['bindings']))

        postRequest(self.owlimPort, "/delete?identifier=uri:record", "", parse=False) 
        json = loads(urlopen('http://localhost:%s/query?query=SELECT ?x WHERE {?x ?y "uri:testDelete"}' % self.owlimPort).read())
        self.assertEquals(0, len(json['results']['bindings']))
        json = loads(urlopen('http://localhost:%s/query?query=SELECT ?x WHERE {?x ?y "uri:testDeleteUpdated"}' % self.owlimPort).read())
        self.assertEquals(0, len(json['results']['bindings']))

        postRequest(self.owlimPort, "/add?identifier=uri:record", """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <rdf:Description>
            <rdf:type>uri:testDelete</rdf:type>
        </rdf:Description>
    </rdf:RDF>""", parse=False)
        json = loads(urlopen('http://localhost:%s/query?query=SELECT ?x WHERE {?x ?y "uri:testDelete"}' % self.owlimPort).read())
        self.assertEquals(1, len(json['results']['bindings']))
