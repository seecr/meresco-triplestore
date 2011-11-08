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

from urllib import urlopen, urlencode
from simplejson import loads
from meresco.components.sorteditertools import WrapIterable

from weightless.http import httpget

JSON_EMPTY_RESULT = '{"results": {"bindings": []}}'

class HttpClient(object):
    def __init__(self, port):
        self.port = port

    def executeQuery(self, query):
        path = "/query?%s" % urlencode(dict(query=query))
        response = yield httpget("localhost", self.port, path)
        header,body = response.split("\r\n\r\n", 1)
        raise StopIteration(body)

    def add(self, identifier, partname, data):
        url = "http://localhost:%s/update?%s" % (self.port, urlencode(dict(identifier=identifier)))
        urlopen(url, data).read()

    def delete(self, identifier, *args, **kwargs):
        url = "http://localhost:%s/delete?%s" % (self.port, urlencode(dict(identifier=identifier)))
        urlopen(url).read()

    def getStatements(self, subj=None, pred=None, obj=None):
        results = yield self.executeQuery(self._createSparQL(subj, pred, obj))
        jsonData = loads(results)
        raise StopIteration(WrapIterable(_results(jsonData, subj, pred, obj)))

    def _createSparQL(self, subj=None, pred=None, obj=None):
        statement = "SELECT"
        if subj == None:
            statement += " ?s"
        if pred == None:
            statement += " ?p"
        if obj == None:
            statement += " ?o"
        statement += " WHERE {"

        if subj and subj[0] != '<' and subj[-1] != '>':
            subj = '<%s>' % subj
        if pred and pred[0] != '<' and pred[-1] != '>':
            pred = '<%s>' % pred

        statement += " " + subj if subj else " ?s"
        statement += " " + pred if pred else " ?p"
        statement += " " + obj if obj else " ?o"

        statement += " }"

        return statement

def _results(jsonData, subj, pred, obj):
    for i in jsonData['results']['bindings']:
        resultSubject = i['s']['value']  if 's' in i else subj
        resultPredicate = i['p']['value']  if 'p' in i else pred
        resultObject = i['o']['value']  if 'o' in i else obj
        yield resultSubject, resultPredicate, resultObject
