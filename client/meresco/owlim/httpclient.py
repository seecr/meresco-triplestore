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

from weightless.http import httpget, httppost

from literal import Literal
from uri import Uri


JSON_EMPTY_RESULT = '{"results": {"bindings": []}}'

class HttpClient(object):
    def __init__(self, host, port):
        self.host = host
        self.port = port

    def add(self, identifier, partname, data):
        path = "/update?%s" % urlencode(dict(identifier=identifier))
        yield self._send(path=path, body=data)

    def delete(self, identifier, *args, **kwargs):
        path = "/delete?%s" % urlencode(dict(identifier=identifier))
        yield self._send(path=path, body=None)

    def executeQuery(self, query):
        path = "/query?%s" % urlencode(dict(query=query))
        response = yield httpget(self.host, self.port, path)
        header, body = response.split("\r\n\r\n", 1)
        raise StopIteration(_parseJson2Dict(body))

    def getStatements(self, subj=None, pred=None, obj=None):
        query = self._createSparQL(subj, pred, obj)
        path = "/query?%s" % urlencode(dict(query=query))
        response = yield httpget("localhost", self.port, path)
        header, body = response.split("\r\n\r\n", 1)
        self._verify200(header, response)
        try:
            jsonData = loads(body)
        except:
            print 'FAILURE'
            print 'query', query
            print 'path', path
            print 'header', header
            print 'body', body
            raise
        raise StopIteration(_results(jsonData, subj, pred, obj))

    def _send(self, path, body):
        headers = None
        if body:
            headers={'Content-Type': 'text/xml', 'Content-Length': len(body)}
        response = yield httppost(host=self.host, port=self.port, request=path, body=body, headers=headers)
        header, body = response.split("\r\n\r\n", 1)
        self._verify200(header, response)

    def _verify200(self, header, response):
        if not header.startswith('HTTP/1.1 200'):
            raise IOError("Expected status '200' from Owlim triplestore, but got: " + response)

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
        resultSubject = fromDict(i['s'])  if 's' in i else Uri(subj)
        resultPredicate = fromDict(i['p'])  if 'p' in i else Uri(pred)
        resultObject = fromDict(i['o'])  if 'o' in i else Literal(obj)
        yield resultSubject, resultPredicate, resultObject

typeMapping = {
    'literal': Literal,
    'uri': Uri,
}

def fromDict(dictionary):
    mappedType = typeMapping.get(dictionary['type'], None)
    return mappedType.fromDict(dictionary) if mappedType else dictionary['value']

def _parseJson2Dict(jsonString):
    result = []
    json = loads(jsonString)
    for i in json['results']['bindings']:
        result.append(dict([(key, fromDict(value)) for (key, value) in i.items()]))
    return result

