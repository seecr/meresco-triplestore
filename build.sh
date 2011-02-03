#!/bin/bash

OWLIMJARDIR=/
SESAME=jars/openrdf-sesame-2.3.2-onejar.jar

BUILDDIR=./build
TARGET=owlimhttpserver.jar

test -d $BUILDDIR && rm -r $BUILDDIR
mkdir $BUILDDIR

CP="$SESAME:$(ls -1 /usr/share/java/libowlim-core3-gcj9/*-.jar | tr '\n' ':')"

javaFiles=$(find src/java -name "*.java")
javac -d $BUILDDIR -cp $CP $javaFiles
if [ "$?" != "0" ]; then
    echo "Build failed"
    exit 1
fi

jar -cf $TARGET -C $BUILDDIR org

