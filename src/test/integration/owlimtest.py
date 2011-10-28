
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
