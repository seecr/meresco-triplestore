#!/bin/bash

OWLIMJARS="/usr/share/java/libowlim-core3-gcj9/*-.jar"
SESAME=jars/openrdf-sesame-2.3.2-onejar.jar

CP="$SESAME:$(ls -1 $OWLIMJARS | tr '\n' ':')owlimhttpserver.jar"

java -cp $CP org.meresco.owlimhttpserver.OwlimServer $@

