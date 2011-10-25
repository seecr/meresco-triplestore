#!/bin/bash
## begin license ##
#
#    OwlimHttpServer provides a simple HTTP interface to an OWLim triplestore
#    Copyright (C) 2011 Seek You Too B.V. (CQ2) http://www.cq2.nl
#
#    This file is part of OwlimHttpServer.
#
#    Storage is free software; you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation; either version 2 of the License, or
#    (at your option) any later version.
#
#    Storage is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with Storage; if not, write to the Free Software
#    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
#
## end license ##

OWLIMJARS=$(find /usr/share/java/owlim-lite-java -type f -name "*.jar")
MY_JARS=$(find /usr/share/java/owlimhttpserver -type f -name "*.jar")

CP="$(echo ${MY_JARS} | tr ' ' ':'):$(echo ${OWLIMJARS} | tr ' ' ':')"

exec java -cp ${CP} org.meresco.owlimhttpserver.OwlimServer $@

