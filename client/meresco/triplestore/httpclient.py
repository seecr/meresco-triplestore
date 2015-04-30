## begin license ##
#
# The Meresco Triplestore package consists out of a HTTP server written in Java that
# provides access to an Triplestore with a Sesame Interface, as well as python bindings to
# communicate as a client with the server.
#
# Copyright (C) 2015 Koninklijke Bibliotheek (KB) http://www.kb.nl
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

from .triplestorerequest import TriplestoreRequest
from weightless.io.utils import asProcess
from weightless.http import HttpRequest

class HttpClient(object):
    def __init__(self, name=None, host=None, port=None, synchronous=False, enableCollectLog=False, pathPrefix=''):
        self._triplestoreRequest = TriplestoreRequest(name=name, host=host, port=port, enableCollectLog=enableCollectLog, pathPrefix=pathPrefix)
        self.addStrand = self._triplestoreRequest.addStrand
        self.addObserver = self._triplestoreRequest.addObserver
        self.observable_name = self._triplestoreRequest.observable_name
        observer = SyncHttpRequest() if synchronous else HttpRequest()
        self._triplestoreRequest.addObserver(observer)

    def __getatt__(self, attr):
        return getattr(self._triplestoreRequest, attr)

class SyncHttpRequest(object):
    def httprequest(self, **kwargs):
        raise StopIteration(asProcess(HttpRequest.httprequest(**kwargs)))
        yield