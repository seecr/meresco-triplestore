#!/usr/bin/env python
# -*- coding: utf-8 -*-
## begin license ##
# 
# All rights reserved.
# 
# Copyright (C) 2011 Seecr (Seek You Too B.V.) http://seecr.nl
# 
## end license ##

from os import system                               #DO_NOT_DISTRIBUTE
from glob import glob                               #DO_NOT_DISTRIBUTE
from sys import path as systemPath                  #DO_NOT_DISTRIBUTE
system('find .. -name "*.pyc" | xargs rm -f')       #DO_NOT_DISTRIBUTE
for path in glob('../client/deps.d/*'):             #DO_NOT_DISTRIBUTE
    systemPath.insert(0, path)                      #DO_NOT_DISTRIBUTE
systemPath.insert(0, '../client')                   #DO_NOT_DISTRIBUTE

from sys import argv

from testrunner import TestRunner

from integration import globalSetUp, globalTearDown

flags = ['--fast']

if __name__ == '__main__':
    fastMode = '--fast' in argv
    for flag in flags:
        if flag in argv:
            argv.remove(flag)

    runner = TestRunner()
    runner.addGroup('default', [
            'integration.communicatetest.CommunicateTest',
            'integration.owlimtest.OwlimTest',
        ],
        groupSetUp = lambda: globalSetUp(fastMode, 'default'),
        groupTearDown = lambda: globalTearDown())

    testnames = argv[1:]
    runner.run(testnames)
    
