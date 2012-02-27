#!/bin/bash
## begin license ##
# 
# All rights reserved.
# 
# Copyright (C) 2011 Seecr (Seek You Too B.V.) http://seecr.nl
# 
## end license ##

export LANG=en_US.UTF-8
export PYTHONPATH=.:$PYTHONPATH
python2.5 _integrationtest.py "$@"
