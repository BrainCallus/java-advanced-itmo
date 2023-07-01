#!/bin/bash

PACKAGE=hello
SRC=$(realpath ../../)/java-advanced
JabaDoc=$SRC/javadoc/hello
CodeSRC=$SRC/java-solutions
Tests=$(realpath ../../../)/java-advanced-2023
TestModule=info.kgeorgiy.java.advanced.$PACKAGE
TestModulePath=$Tests/modules/$TestModule
ClassDepend=$TestModulePath/info/kgeorgiy/java/advanced/$PACKAGE
DOCS="https://docs.oracle.com/en/java/javase/17/docs/api/"

if ! [ -d "$JabaDoc" ]
then
    mkdir "$JabaDoc"
fi

# shellcheck disable=SC2086
# shellcheck disable=SC2115
rm -r $JabaDoc/* &> /dev/null

ACTIONS="-d $JabaDoc -link $DOCS -sourcepath $CodeSRC:$TestModulePath -p $Tests/artifacts:$Tests/lib --add-modules $TestModule -author -private"
MYCODE_PATH="info.kgeorgiy.ja.churakova.$PACKAGE"
MYCLASSES="$CodeSRC/info/kgeorgiy/ja/churakova/$PACKAGE/*.java"
CLASSES_DEP="$ClassDepend/HelloServer.java $ClassDepend/HelloClient.java"


javadoc $ACTIONS $MYCODE_PATH $MYCLASSES $CLASSES_DEP