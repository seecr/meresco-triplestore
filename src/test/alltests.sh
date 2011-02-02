#!/bin/bash

rm -r ../../build/
mkdir ../../build


JUNIT=/usr/share/java/junit4.jar

CP="$JUNIT:$(ls -1 /usr/share/java/libowlim-core3-gcj9/*-.jar | tr '\n' ':')../../build"

javaFiles=$(find ../java -name "*.java")
javac -d ../../build -cp $CP $javaFiles
if [ "$?" != "0" ]; then
    echo "Build failed"
    exit 1
fi

javaFiles=$(find . -name "*.java")
javac -d ../../build -cp $CP $javaFiles
if [ "$?" != "0" ]; then
    echo "Test Build failed"
    exit 1
fi

testClasses=$(cd ../../build; find . -name "*Test.class" | sed 's,.class,,g' | tr '/' '.' | sed 's,..,,')
echo "Running $testClasses"
java -classpath ".:$CP" org.junit.runner.JUnitCore $testClasses

