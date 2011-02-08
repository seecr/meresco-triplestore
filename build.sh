#!/bin/bash

OWLIMJARS=$(find /usr/share/java/libowlim-core3-gcj9 -type f -name "*.jar")
SESAME=jars/openrdf-sesame-2.3.2-onejar.jar

BUILDDIR=./build
TARGET=owlimhttpserver.jar

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

