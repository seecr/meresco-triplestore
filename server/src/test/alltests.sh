#!/bin/bash
## begin license ##
#
# The Meresco Triplestore package consists out of a HTTP server written in Java that
# provides access to an Triplestore with a Sesame Interface, as well as python bindings to
# communicate as a client with the server.
#
# Copyright (C) 2011-2014, 2016 Seecr (Seek You Too B.V.) http://seecr.nl
# Copyright (C) 2011 Seek You Too B.V. (CQ2) http://www.cq2.nl
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

source /usr/share/seecr-tools/functions.d/distro

BUILDDIR=../../build/
test -d ${BUILDDIR} && rm -rf ${BUILDDIR}
mkdir ${BUILDDIR}

JUNIT=/usr/share/java/junit4.jar
if [ ! -f ${JUNIT} ]; then
    echo "JUnit is not installed. Please install the junit4 package."
    exit 1
fi

JARS=$(find ../../jars -type f -name "*.jar")

CP="$JUNIT:$(echo $JARS | tr ' ' ':'):../../build"

JAVA_VERSION=8
javac=/usr/lib/jvm/java-1.${JAVA_VERSION}.0-openjdk.x86_64/bin/javac
if [ -f /etc/debian_version ]; then
    javac=/usr/lib/jvm/java-${JAVA_VERSION}-openjdk-amd64/bin/javac
fi

javaFiles=$(find ../java -name "*.java")
${javac} -d ${BUILDDIR} -cp $CP $javaFiles
if [ "$?" != "0" ]; then
    echo "Build failed"
    exit 1
fi

javaFiles=$(find . -name "*.java")
${javac} -d ${BUILDDIR} -cp $CP $javaFiles
if [ "$?" != "0" ]; then
    echo "Test Build failed"
    exit 1
fi

testClasses=$(cd ${BUILDDIR}; find . -name "*Test.class" | sed 's,.class,,g' | tr '/' '.' | sed 's,..,,')
echo "Running $testClasses"
$(dirname ${javac})/java -Xmx1024m -classpath ".:$CP" org.junit.runner.JUnitCore $testClasses

