#!/bin/bash
## begin license ##
#
# The Meresco Triplestore package consists out of a HTTP server written in Java that
# provides access to an Triplestore with a Sesame Interface, as well as python bindings to
# communicate as a client with the server.
#
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

export LANG=en_US.UTF-8
export PYTHONPATH=.:$PYTHONPATH

tests="client server"

pyversions="python3.7"

option=$1
if [ "${option:0:8}" == "--python" ]; then
    shift
    pyversions="${option:2}"
fi

for type in $tests; do
    if [ "$type" == "client" ]; then
        echo 'Client test'
        echo Found Python versions: $pyversions
        for pycmd in $pyversions; do
            echo "================ $pycmd _alltests.py $@ ================"
            $pycmd _alltests.py "$@"
        done
    fi
    if [ "$type" == "server" ]; then
        echo 'Server test'
        (
            cd ../server/src/test
            ./alltests.sh "$@"
        )
    fi
done
