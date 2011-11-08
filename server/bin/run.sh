#!/bin/bash
## begin license ##
# 
# "OwlimHttpServer" provides a simple HTTP interface to an OWLim triplestore. 
# 
# Copyright (C) 2011 Seecr (Seek You Too B.V.) http://seecr.nl
# Copyright (C) 2011 Seek You Too B.V. (CQ2) http://www.cq2.nl
# 
# This file is part of "OwlimHttpServer"
# 
# "OwlimHttpServer" is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
# 
# "OwlimHttpServer" is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with "OwlimHttpServer"; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
# 
## end license ##

OWLIMJARS=$(find /usr/share/java/owlim-lite-java -type f -name "*.jar")
MY_JARS=$(find /usr/share/java/owlimhttpserver -type f -name "*.jar")

(                                               # DO_NOT_DISTRIBUTE
    cd ..                                       # DO_NOT_DISTRIBUTE
    ./build.sh                                  # DO_NOT_DISTRIBUTE
)                                               # DO_NOT_DISTRIBUTE
MY_JARS=$(find .. -type f -name "*.jar")        # DO_NOT_DISTRIBUTE

CP="$(echo ${MY_JARS} | tr ' ' ':'):$(echo ${OWLIMJARS} | tr ' ' ':')"

exec java -cp ${CP} org.meresco.owlimhttpserver.OwlimServer $@
