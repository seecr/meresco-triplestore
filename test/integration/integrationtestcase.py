## begin license ##
# 
# The Meresco Owlim package consists out of a HTTP server written in Java that
# provides access to an Owlim Triple store, as well as python bindings to
# communicate as a client with the server. 
# 
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

from __future__ import with_statement

from os.path import isdir, join, abspath, dirname, basename
from os import system, listdir
from sys import stdout
from random import randint, choice
from time import sleep, time 
from hashlib import md5
from StringIO import StringIO
from subprocess import Popen
from signal import SIGTERM
from os import waitpid, kill, WNOHANG
from urllib import urlopen, urlencode
from re import DOTALL, compile
from traceback import print_exc
from lxml.etree import XMLSyntaxError, parse

from seecr.test import SeecrTestCase
from meresco.components import readConfig

from utils import postRequest, postMultipartForm 


mypath = dirname(abspath(__file__))
binDir = join(dirname(dirname(mypath)), 'server', 'bin')

def stdoutWrite(aString):
    stdout.write(aString)
    stdout.flush()

class PortNumberGenerator(object):
    startNumber = randint(50000, 60000)

    @classmethod
    def next(cls):
        cls.startNumber += 1
        return cls.startNumber

scriptTagRegex = compile("<script[\s>].*?</script>", DOTALL)


class IntegrationTestCase(SeecrTestCase):
    def setUp(self):
        SeecrTestCase.setUp(self)
        global state
        self.state = state

    def __getattr__(self, name):
        if name.startswith('_'):
            raise AttributeError(name)
        return getattr(self.state, name)

    def parseHtmlAsXml(self, body):
        def forceXml(body):
            newBody = body 
            newBody = newBody.replace("&nbsp;", " ") 
            newBody = newBody.replace("&ndash;", "&#8211;")
            newBody = newBody.replace("&mdash;", "&#8212;")
            newBody = scriptTagRegex.sub('', newBody)
            return newBody
        try: 
            return parse(StringIO(forceXml(body)))
        except XMLSyntaxError:
            print body 
            raise


class IntegrationState(object):
    def __init__(self, stateName, fastMode):
        self.stateName = stateName
        self.pids = {}
        self.integrationTempdir = '/tmp/integrationtest-owlim-%s' % stateName 
        self.owlimDataDir = join(self.integrationTempdir, 'owlim-data')
        self.testdataDir = join(dirname(mypath), 'data')
        if not fastMode:
            system('rm -rf ' + self.integrationTempdir)
            system('mkdir --parents ' + self.owlimDataDir)
        
        self.owlimPort = PortNumberGenerator.next()

    def initialize(self):
        self.startOwlimServer()
            
    def _startServer(self, serviceName, binScript, serviceReadyUrl, redirect=True, **kwargs):
        stdoutfile = join(self.integrationTempdir, "stdouterr-%s.log" % serviceName)
        stdouterrlog = open(stdoutfile, 'w')
        args = [join(binDir, binScript)]
        fileno = stdouterrlog.fileno() if redirect else None
        for k,v in kwargs.items():
            args.append("--%s" % k)
            args.append(str(v))
        args += ["--name", serviceName]
        serverProcess = Popen(
                args=args,
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

    def startOwlimServer(self):
        self._startServer('owlim', 'start-owlim', 'http://localhost:%s/query' % self.owlimPort, port=self.owlimPort, stateDir=self.owlimDataDir)

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

