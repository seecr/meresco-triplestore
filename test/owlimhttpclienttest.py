## begin license ##
# 
# A Python binding using HTTP to communicate with an Owlim HTTP Server. 
# 
# Copyright (C) 2010-2011 Maastricht University Library http://www.maastrichtuniversity.nl/web/Library/home.htm
# Copyright (C) 2010-2011 Seek You Too B.V. (CQ2) http://www.cq2.nl
# Copyright (C) 2011 Seecr (Seek You Too B.V.) http://seecr.nl
# 
# This file is part of "OwlimHttpClient"
# 
# "OwlimHttpClient" is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
# 
# "OwlimHttpClient" is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with "OwlimHttpClient"; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
# 
## end license ##

from cq2utils import CQ2TestCase, CallTrace

from meresco.core import be, Observable

from owlimhttpclient import OwlimHttpClient

class OwlimHttpClientTest(CQ2TestCase):

    def testCreateSparQL(self):
        client = OwlimHttpClient(port=9999)
        self.assertEquals("SELECT ?s ?p ?o WHERE { ?s ?p ?o }", client._createSparQL(subj=None, pred=None, obj=None))

        self.assertEquals("SELECT ?p ?o WHERE { <http://cq2.org/person/0001> ?p ?o }", client._createSparQL(subj="http://cq2.org/person/0001"))
        
        self.assertEquals("SELECT ?o WHERE { <http://cq2.org/person/0001> <http://xmlns.com/foaf/0.1/name> ?o }", client._createSparQL(subj="http://cq2.org/person/0001", pred="http://xmlns.com/foaf/0.1/name"))

    def testGetStatements(self):
        client = OwlimHttpClient(port=9999)
        client.executeQuery = lambda *args, **kwargs: RESULT_JSON
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
        

RESULT_JSON = """{
        "head": {
                "vars": [ "s", "p", "o" ]
        }, 
        "results": {
                "bindings": [
                        {
                                "o": { "type": "uri", "value": "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#Property" }, 
                                "p": { "type": "uri", "value": "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#type" }, 
                                "s": { "type": "uri", "value": "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#type" }
                        }, 
                        {
                                "o": { "type": "uri", "value": "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#Property" }, 
                                "p": { "type": "uri", "value": "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#type" }, 
                                "s": { "type": "uri", "value": "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#subject" }
                        } 
                ]
        }
}"""

