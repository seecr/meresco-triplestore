## begin license ##
# 
# The Meresco Owlim package consists out of a HTTP server written in Java that
# provides access to an Owlim Triple store, as well as python bindings to
# communicate as a client with the server. 
# 
# Copyright (C) 2013 Seecr (Seek You Too B.V.) http://seecr.nl
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

from os import system
from os.path import join, dirname, abspath

from seecr.test.integrationtestcase import IntegrationState
from seecr.test.portnumbergenerator import PortNumberGenerator


myDir = dirname(abspath(__file__))
serverBinDir = join(dirname(dirname(myDir)), 'server/bin')
print serverBinDir

class OwlimIntegrationState(IntegrationState):
    def __init__(self, stateName, tests=None, fastMode=False):
        IntegrationState.__init__(self, stateName=stateName, tests=tests, fastMode=fastMode)

        self.owlimDataDir = join(self.integrationTempdir, 'owlim-data')
        self.owlimPort = PortNumberGenerator.next()
        self.testdataDir = join(dirname(myDir), 'data')
        if not fastMode:
            system('rm -rf ' + self.integrationTempdir)
            system('mkdir --parents ' + self.owlimDataDir)
        
    def setUp(self):
        self.startOwlimServer()

    def binDir(self):
        return serverBinDir

    def startOwlimServer(self):
        self._startServer('owlim', self.binPath('start-owlim'), 'http://localhost:%s/query' % self.owlimPort, port=self.owlimPort, stateDir=self.owlimDataDir, name='owlim')

    def restartOwlimServer(self):
        self._stopServer('owlim')
        self.startOwlimServer()

