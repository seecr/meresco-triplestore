## begin license ##
# 
# The Meresco Owlim package consists out of a HTTP server written in Java that
# provides access to an Owlim Triple store, as well as python bindings to
# communicate as a client with the server. 
# 
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

from __future__ import with_statement

from os.path import isdir, join, abspath, dirname, basename
from os import system, listdir
from sys import stdout

from cq2utils import CQ2TestCase
from random import randint, choice
from time import sleep, time 
from hashlib import md5

from lxml.etree import XMLSyntaxError, parse
from StringIO import StringIO
from subprocess import Popen
from signal import SIGTERM
from os import waitpid, kill, WNOHANG
from urllib import urlopen, urlencode
from re import DOTALL, compile

from meresco.components import readConfig

from traceback import print_exc

mypath = dirname(abspath(__file__))
binDir = join(dirname(dirname(dirname(mypath))), 'bin')
if not isdir(binDir):
    binDir = '/usr/bin'

def stdoutWrite(aString):
    stdout.write(aString)
    stdout.flush()

class PortNumberGenerator(object):
    startNumber = randint(50000, 60000)

    @classmethod
    def next(cls):
        cls.startNumber += 1
        return cls.startNumber

class IntegrationTestCase(CQ2TestCase):
    def setUp(self):
        CQ2TestCase.setUp(self)
        global state
        self.state = state

    def __getattr__(self, name):
        if name.startswith('_'):
            raise AttributeError(name)
        return getattr(self.state, name)

class IntegrationState(object):
    def __init__(self, stateName, fastMode):
        self.stateName = stateName
        self.pids = {}
        self.integrationTempdir = '/tmp/integrationtest-owlimhttpserver-%s' % stateName 
        self.owlimDataDir = join(self.integrationTempdir, 'owlim-data')
        self.testdataDir = join(dirname(mypath), 'data')
        if not fastMode:
            system('rm -rf ' + self.integrationTempdir)
            system('mkdir --parents ' + self.integrationTempdir)
        
        self.owlimPort = PortNumberGenerator.next()

        #config = readConfig(join(documentationPath, 'openindex.config'))
        
        # test example config has neccessary parameters
#        def setConfig(parameter, value):
#            assert config[parameter]
#            config[parameter] = value
#
#        setConfig('solrPortNumber', str(self.solrPort))
#        setConfig('owlimPortNumber', str(self.owlimPort))
#        setConfig('host', 'openindex.search')
#
#        config['global.apacheLogStream'] = 'disabled'
#
#        self._writeConfig(config)
#        system("sed 's,^jetty\.home=.*$,jetty.home=%s,' -i %s" % (
#            self.solrDataDir,
#            join(self.solrDataDir, 'start.config')))
         

#    def _writeConfig(self, config):
#        self.configFilename = join(self.integrationTempdir, 'openindex.config')
#        with open(self.configFilename, 'w') as f:
#            f.write("""
##
## Config file for OpenIndex server
##
#""")
#            for item in config.items():
#                f.write('%s = %s\n' % item)

    def initialize(self):
        self.startOwlimServer()

        waitingTime = self._createDatabase()
        print 'Waiting for search.'
        for seconds in range(waitingTime):
            stdout.write('-')
            stdout.flush()
            sleep(1)
        stdout.write('-')
        stdout.flush()

    def _createDatabase(self):
        if fastMode:
            print "Reusing database in", self.integrationTempdir
            return 2
        start = time()
        print "Creating database in", self.integrationTempdir
        waitingTime = 3
        try: 
            if self.stateName == 'default':
                waitingTime = 0
            else:
                raise ValueError('State "%s" not supported' % self.stateName)
            print "Finished creating database in %s seconds" % (time() - start)
        except Exception, e:
            print 'Error received while creating database for', self.stateName
            print_exc()
            exit(1)
        return waitingTime

    def startOwlimServer(self):
        self._startServer('owlim', 'run.sh', 'http://localhost:%s/sparql' % self.owlimPort, port=self.owlimPort, storeLocation=self.owlimDataDir)

    def _startServer(self, serviceName, binScript, serviceReadyUrl, port, storeLocation, redirect=True):
        stdoutfile = join(self.integrationTempdir, "stdouterr-%s.log" % serviceName)
        stdouterrlog = open(stdoutfile, 'w')
        fileno = stdouterrlog.fileno() if redirect else None
        serverProcess = Popen(
                args=[join(binDir, binScript), str(port), storeLocation, serviceName, "/tmp"],
                cwd=binDir,
                stdout=fileno,
                stderr=fileno)
        self.pids[serviceName] = serverProcess.pid

        stdoutWrite("Starting service '%s', for state '%s' : v" % (serviceName, self.stateName))
        done = False
        while not done:
            try:
                stdoutWrite('r')
                sleep(0.1)
                urlopen(serviceReadyUrl).read()
                done = True
            except IOError:
                if serverProcess.poll() != None:
                    del self.pids[serviceName]
                    exit('Service "%s" died, check "%s"' % (serviceName, stdoutfile))
        stdoutWrite('oom!\n')

    def _stopServer(self, serviceName):
        kill(self.pids[serviceName], SIGTERM)
        waitpid(self.pids[serviceName], WNOHANG)

    def restartOwlimServer(self):
        self._stopServer('owlim')
        self.startOwlimServer()

    def tearDown(self):
        for serviceName in self.pids.keys():
            self._stopServer(serviceName)

def globalSetUp(fast, stateName):
    global state, fastMode
    fastMode = fast
    state = IntegrationState(stateName, fastMode)
    state.initialize()

def globalTearDown():
    global state
    state.tearDown()

