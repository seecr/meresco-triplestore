## begin license ##
# 
# The Meresco Owlim package consists out of a HTTP server written in Java that
# provides access to an Owlim Triple store, as well as python bindings to
# communicate as a client with the server. 
# 
# Copyright (C) 2010-2011 Maastricht University Library http://www.maastrichtuniversity.nl/web/Library/home.htm
# Copyright (C) 2010-2011 Seek You Too B.V. (CQ2) http://www.cq2.nl
# Copyright (C) 2011-2012 Seecr (Seek You Too B.V.) http://seecr.nl
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

from urllib2 import urlopen
from urllib import urlencode
from simplejson import loads
from Ft.Xml.Lib import Uri as FtUri 

from weightless.http import httpget, httppost

from literal import Literal
from uri import Uri


JSON_EMPTY_RESULT = '{"results": {"bindings": []}}'

class InvalidRdfXmlException(Exception): 
    pass

class HttpClient(object):
    def __init__(self, host, port, synchronous=False):
        self.host = host
        self.port = port
        self.synchronous = synchronous

    def add(self, identifier, data, **kwargs):
        path = "/update?%s" % urlencode(dict(identifier=identifier))
        yield self._send(path=path, body=data)

    def addTriple(self, subj, pred, obj):
        yield self._send(path="/addTriple", body='|'.join([subj, pred, obj]))

    def removeTriple(self, subj, pred, obj):
        yield self._send(path="/removeTriple", body='|'.join([subj, pred, obj]))

    def delete(self, identifier, **kwargs):
        path = "/delete?%s" % urlencode(dict(identifier=identifier))
        yield self._send(path=path, body=None)

    def validate(self, data):
        path = "/validate"
        header, body = yield self._send(path=path, body=data)
        if body.strip().lower() != 'ok':
            raise InvalidRdfXmlException(body)

    def executeQuery(self, query, queryResultFormat=None):
        queryResult = yield self._sparqlQuery(query, queryResultFormat=queryResultFormat)
        if queryResultFormat is None:
            queryResult = _parseJson2Dict(queryResult)
        raise StopIteration(queryResult)

    def getStatements(self, subj=None, pred=None, obj=None):
        query = self._createSparQL(subj, pred, obj)
        jsonString = yield self._sparqlQuery(query)
        raise StopIteration(_results(jsonString, subj, pred, obj))

    def _sparqlQuery(self, query, queryResultFormat=None):
        path = "/query?%s" % urlencode(dict(query=query))
        headers = None
        if queryResultFormat is not None:
            headers = {'Accept': queryResultFormat}
        if self.synchronous:
            body = self._urlopen("http://localhost:%s%s" % (self.port, path))
        else:
            response = yield httpget("localhost", self.port, path, headers=headers)
            header, body = response.split("\r\n\r\n", 1)
            self._verify200(header, response)
        raise StopIteration(body)

    def _send(self, path, body):
        headers = None
        if body:
            headers={'Content-Type': 'text/xml', 'Content-Length': len(body)}
        if self.synchronous:
            header, body = "", self._urlopen("http://%s:%s%s" % (self.host, self.port, path), data=body)
        else:
            response = yield httppost(host=self.host, port=self.port, request=path, body=body, headers=headers)
            header, body = response.split("\r\n\r\n", 1)
            self._verify200(header, response)
        raise StopIteration((header, body))

    def _verify200(self, header, response):
        if not header.startswith('HTTP/1.1 200'):
            raise IOError("Expected status '200' from Owlim triplestore, but got: " + response)

    def _createSparQL(self, subj=None, pred=None, obj=None):
        statement = "SELECT DISTINCT"
        if subj is None:
            statement += " ?s"
        if pred is None:
            statement += " ?p"
        if obj is None:
            statement += " ?o"
        if (subj and pred and obj):
            statement += " *"
        statement += " WHERE {"

        if subj and subj[0] != '<' and subj[-1] != '>':
            subj = '<%s>' % subj
        if pred and pred[0] != '<' and pred[-1] != '>':
            pred = '<%s>' % pred

        if obj and not FtUri.MatchesUriSyntax(obj):
            obj = '"%s"' % obj

        statement += " " + subj if subj else " ?s"
        statement += " " + pred if pred else " ?p"
        statement += " " + obj if obj else " ?o"

        statement += " }"

        return statement

    def _urlopen(self, *args, **kwargs):
        return urlopen(*args, **kwargs).read()


def _results(jsonString, subj, pred, obj):
    jsonData = loads(jsonString)
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
    jsonData = loads(jsonString)
    for i in jsonData['results']['bindings']:
        result.append(dict([(key, fromDict(value)) for (key, value) in i.items()]))
    return result

