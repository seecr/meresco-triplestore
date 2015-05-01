## begin license ##
#
# The Meresco Triplestore package consists out of a HTTP server written in Java that
# provides access to an Triplestore with a Sesame Interface, as well as python bindings to
# communicate as a client with the server.
#
# Copyright (C) 2015 Seecr (Seek You Too B.V.) http://seecr.nl
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

from seecr.test import SeecrTestCase
from seecr.test.mockserver import MockServer
from random import randint
from meresco.components.http.utils import CRLF
from meresco.triplestore import HttpClient
from triplestorerequesttest import RESULT_JSON, RESULT_HEADER, PARSED_RESULT_JSON
from urllib2 import urlopen
from weightless.core import retval
from weightless.io.utils import asProcess

class HttpClientTest(SeecrTestCase):
    def setUp(self):
        super(HttpClientTest, self).setUp()
        self.port = randint(50000, 60000)
        self.server = MockServer(port=self.port)
        self.requests = []
        _buildResponse = self.server.buildResponse
        def buildResponse(**kwargs):
            self.requests.append(kwargs)
            return _buildResponse(**kwargs)
        self.server.buildResponse = buildResponse
        self.server.response = RESULT_HEADER + CRLF*2 + RESULT_JSON
        self.server.start()

    def tearDown(self):
        self.server.halt = True
        super(HttpClientTest, self).tearDown()

    def testMockSetup(self):
        result = urlopen('http://localhost:%s/a/path' % self.port).read()
        self.assertEquals(RESULT_JSON, result)
        kwargs = self.requests[0]
        self.assertEquals('/a/path', kwargs['path'])

    def testExecuteQuery(self):
        result = asProcess(HttpClient(host='localhost', port=self.port).executeQuery('SPARQL'))
        self.assertEquals(PARSED_RESULT_JSON, result)

    def testExecuteQuerySynchronous(self):
        result = retval(HttpClient(host='localhost', port=self.port, synchronous=True).executeQuery('SPARQL'))
        self.assertEquals(PARSED_RESULT_JSON, result)
