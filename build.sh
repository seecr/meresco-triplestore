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

VERSION=$1

OWLIMJARS=$(find /usr/share/java/libowlim-core3-gcj9 -type f -name "*.jar")
SESAME=jars/openrdf-sesame-2.3.2-onejar.jar

BUILDDIR=./build
TARGET=owlimhttpserver.jar
if [ "${VERSION}" != "" ]; then
    TARGET=owlimhttpserver-${VERSION}.jar
fi

test -d $BUILDDIR && rm -r $BUILDDIR
mkdir $BUILDDIR

CP="$SESAME:$(echo $OWLIMJARS | tr ' ' ':')"

javaFiles=$(find src/java -name "*.java")
javac -d $BUILDDIR -cp $CP $javaFiles
if [ "$?" != "0" ]; then
    echo "Build failed"
    exit 1
fi

jar -cf $TARGET -C $BUILDDIR org

