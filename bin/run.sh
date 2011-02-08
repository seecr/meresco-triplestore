#!/bin/bash

OWLIMJARS=$(find /usr/share/java/libowlim-core3-gcj9 -type f -name "*.jar")
MY_JARS=$(find /usr/share/java/owlimhttpserver -type f -name "*.jar")

CP="$(echo ${MY_JARS} | tr ' ' ':'):$(echo ${OWLIMJARS} | tr ' ' ':')"

java -cp ${CP} org.meresco.owlimhttpserver.OwlimServer $@

