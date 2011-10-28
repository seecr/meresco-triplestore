#!/usr/bin/env python
# -*- coding: utf-8 -*-
## begin license ##
# 
# All rights reserved.
# 
# Copyright (C) 2011 Seecr (Seek You Too B.V.) http://seecr.nl
# 
## end license ##

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
            'integration.owlimtest.OwlimTest',
        ],
        groupSetUp = lambda: globalSetUp(fastMode, 'default'),
        groupTearDown = lambda: globalTearDown())

    testnames = argv[1:]
    runner.run(testnames)
    
