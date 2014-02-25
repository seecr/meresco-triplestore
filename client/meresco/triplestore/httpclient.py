## begin license ##
#
# The Meresco Triplestore package consists out of a HTTP server written in Java that
# provides access to an Triplestore with a Sesame Interface, as well as python bindings to
# communicate as a client with the server.
#
# Copyright (C) 2010-2011 Maastricht University Library http://www.maastrichtuniversity.nl/web/Library/home.htm
# Copyright (C) 2010-2011 Seek You Too B.V. (CQ2) http://www.cq2.nl
# Copyright (C) 2011-2014 Seecr (Seek You Too B.V.) http://seecr.nl
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

from urllib2 import urlopen
from urllib import urlencode
from simplejson import loads

from weightless.http import httpget, httppost
from meresco.core import Observable

from literal import Literal
from uri import Uri
from bnode import BNode
from decimal import Decimal


class InvalidRdfXmlException(Exception):
    pass

class MalformedQueryException(Exception):
    pass


class HttpClient(Observable):
    def __init__(self, name=None, host=None, port=None, synchronous=False):
        Observable.__init__(self, name=name)
        self.host = host
        self.port = port
        self.synchronous = synchronous

    def add(self, identifier, data, **kwargs):
        path = "/update?%s" % urlencode(dict(identifier=identifier))
        yield self._send(path=path, body=data)

    def addTriple(self, subject, predicate, object):
        yield self._send(path="/addTriple", body='|'.join([subject, predicate, object]))

    def removeTriple(self, subject, predicate, object):
        yield self._send(path="/removeTriple", body='|'.join([subject, predicate, object]))

    def delete(self, identifier, **kwargs):
        path = "/delete?%s" % urlencode(dict(identifier=identifier))
        yield self._send(path=path, body=None)

    def validate(self, data):
        path = "/validate"
        header, body = yield self._send(path=path, body=data)
        if body.strip().lower() != 'ok':
            raise InvalidRdfXmlException(body)

    def executeQuery(self, query, queryResultFormat=None):
        header, queryResult = yield self._sparqlQuery(query, queryResultFormat=queryResultFormat)
        if queryResultFormat is None:
            queryResult = self._parseJson2Dict(queryResult)
        self._handleQueryTimes(header)
        raise StopIteration(queryResult)

    def getStatements(self, subject=None, predicate=None, object=None):
        query = ''.join(self._getStatementsSparQL(subject=subject, predicate=predicate, object=object))
        header, jsonString = yield self._sparqlQuery(query)
        self._handleQueryTimes(header)
        raise StopIteration(self._getStatementsResults(jsonString, subject=subject, predicate=predicate, object=object))

    def export(self, identifier):
        if not identifier:
            raise ValueError("identifier cannot be empty")
        path = "/export?%s" % urlencode(dict(identifier=identifier))
        yield self._send(path=path, body=None)

    def importTrig(self, data):
        yield self._send("/import", body=data)

    def _sparqlQuery(self, query, queryResultFormat=None):
        path = "/query?%s" % urlencode(dict(query=query))
        headers = None
        if queryResultFormat is not None:
            headers = {'Accept': queryResultFormat}
        host, port = self._triplestoreServer()
        body = None
        try:
            if self.synchronous:
                header, body = self._urlopen("http://%s:%s%s" % (host, port, path))
            else:
                response = yield self._httpget(host=host, port=port, request=path, headers=headers)
                header, body = response.split("\r\n\r\n", 1)
                self._verify200(header, response)
        except Exception, e:
            errorStr = e.read() if hasattr(e, 'read') else (body or str(e))
            if 'MalformedQueryException' in errorStr or 'QueryEvaluationException' in errorStr:
                raise MalformedQueryException(errorStr)
            raise e
        raise StopIteration((header, body))

    def _send(self, path, body):
        headers = None
        if body:
            headers={'Content-Type': 'text/xml', 'Content-Length': len(body)}
        host, port = self._triplestoreServer()
        responseBody = None
        try:
            if self.synchronous:
                header, responseBody = self._urlopen("http://%s:%s%s" % (host, port, path), data=body)
            else:
                response = yield self._httppost(host=host, port=port, request=path, body=body, headers=headers)
                header, responseBody = response.split("\r\n\r\n", 1)
                self._verify200(header, response)
        except Exception, e:
            errorStr = e.read() if hasattr(e, 'read') else (body or str(e))
            if 'RDFParseException' in errorStr:
                raise InvalidRdfXmlException(errorStr)
            elif 'IllegalArgumentException' in errorStr:
                raise ValueError(errorStr)
            raise e
        raise StopIteration((header, responseBody))

    def _getStatementsSparQL(self, subject=None, predicate=None, object=None):
        if not subject is None and not Uri.matchesUriSyntax(subject):
            raise ValueError('subject must be an URI')
        if not predicate is None and not Uri.matchesUriSyntax(predicate):
            raise ValueError('predicate must be an URI')
        yield "SELECT DISTINCT"
        if subject is None:
            yield " ?s"
        if predicate is None:
            yield " ?p"
        if object is None:
            yield " ?o"
        if (subject and predicate and object):
            yield " *"
        yield " WHERE {"
        yield " " + ('<%s>' % subject if subject else "?s")
        yield " " + ('<%s>' % predicate if predicate else "?p")
        yield " " + (['"%s"', '<%s>'][Uri.matchesUriSyntax(object)] % object if object else "?o")
        yield " }"

    def _parseJson2Dict(self, jsonString):
        result = []
        jsonData = loads(jsonString)
        for binding in jsonData['results']['bindings']:
            result.append(dict([(key, self._fromBinding(binding, key)) for key in binding.keys()]))
        return result

    def _getStatementsResults(self, jsonString, subject, predicate, object):
        jsonData = loads(jsonString)
        if not subject is None:
            subject = Uri(subject)
        if not predicate is None:
            predicate = Uri(predicate)
        if not object is None:
            object = Uri(object) if Uri.matchesUriSyntax(object) else Literal(object)
        for binding in jsonData['results']['bindings']:
            resultSubject = self._fromBinding(binding, 's', subject)
            resultPredicate = self._fromBinding(binding, 'p', predicate)
            resultObject = self._fromBinding(binding, 'o', object)
            yield resultSubject, resultPredicate, resultObject

    def _fromBinding(self, binding, key, default=None):
        valueDict = binding.get(key)
        if valueDict is None:
            return default
        mappedType = _typeMapping.get(valueDict['type'])
        return mappedType.fromDict(valueDict) if mappedType else valueDict['value']

    def _verify200(self, header, response):
        if not header.startswith('HTTP/1.1 200'):
            raise IOError("Expected status 'HTTP/1.1 200' from triplestore, but got: " + response)

    def _triplestoreServer(self):
        if self.host:
            return (self.host, self.port)
        return self.call.triplestoreServer()

    def _handleQueryTimes(self, header):
        times = [line.split(':',1)[-1].strip() for line in header.split('\r\n') if X_MERESCO_TRIPLESTORE_QUERYTIME in line]
        if times:
            queryTime = (Decimal(times[0]) * millis).quantize(millis)
            self.do.handleQueryTimes(index=queryTime)

    def _urlopen(self, *args, **kwargs):
        u = urlopen(*args, **kwargs)
        header = "HTTP/1.0 %s %s\r\n" % (u.code, u.msg) + str(u.headers).strip()
        return header, u.read()

    def _httpget(self, **kwargs):
        return httpget(**kwargs)

    def _httppost(self, **kwargs):
        return httppost(**kwargs)

X_MERESCO_TRIPLESTORE_QUERYTIME = 'X-Meresco-Triplestore-QueryTime'
millis = Decimal('0.001')

_typeMapping = {
    'literal': Literal,
    'typed-literal': Literal,
    'uri': Uri,
    'bnode': BNode
}

