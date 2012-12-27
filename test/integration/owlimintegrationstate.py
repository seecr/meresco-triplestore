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

