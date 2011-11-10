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

from meresco.core import Observable

from cq2utils import CQ2TestCase, CallTrace

from meresco.owlim import JsonResult2Dict, Literal, Uri

class JsonResult2DictTest(CQ2TestCase):
    def _setupResult(self, result):
        self.observable = Observable()
        self.jsonResult2Dict = JsonResult2Dict()
        self.observer = CallTrace(returnValues={'executeQuery': result})

        self.observable.addObserver(self.jsonResult2Dict)
        self.jsonResult2Dict.addObserver(self.observer)

    def testExecuteQuery(self):
        self._setupResult(RESULT_JSON)
        result = self.observable.any.executeQuery("")
        self.assertEquals(2, len(result))
        self.assertEquals(PARSED_RESULT_JSON, result)

PARSED_RESULT_JSON = [
    {
        u'y': Uri(value=u'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'), 
        u'x': Uri(value=u'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'), 
        u'z': Literal(value=u'word', lang="eng")
    }, {
        u'y': Uri(value=u'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'), 
        u'x': Uri(value=u'http://www.w3.org/1999/02/22-rdf-syntax-ns#subject'), 
        u'z': Literal(value=u'woord', lang="dut")
    }
]

RESULT_JSON = """{
        "head": {
                "vars": [ "x", "y", "z" ]
        }, 
        "results": {
                "bindings": [
                        {
                                "z": { "type": "literal", "xml:lang": "eng", "value": "word" }, 
                                "y": { "type": "uri", "value": "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#type" }, 
                                "x": { "type": "uri", "value": "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#type" }
                        }, 
                        {
                                "z": { "type": "literal", "xml:lang": "dut", "value": "woord" }, 
                                "y": { "type": "uri", "value": "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#type" }, 
                                "x": { "type": "uri", "value": "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#subject" }
                        } 
                ]
        }
}"""

