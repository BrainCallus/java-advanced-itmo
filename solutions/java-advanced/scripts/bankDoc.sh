#!/bin/bash

PACKAGE=bank
SRC=$(realpath ../../)/java-advanced
JabaDoc=$SRC/javadoc/bank
CodeSRC=$SRC/java-solutions
Tests=$(realpath ../../../)/solutions/java-advanced/java-solutions
TestModule=info.kgeorgiy.ja.churakova.$PACKAGE.$PACKAGE
TestModulePath=$Tests/modules/$TestModule
ClassDepend=$TestModulePath/info/kgeorgiy/java/advanced/implementor
MyClassesPath=$CodeSRC/info/kgeorgiy/ja/churakova/$PACKAGE
DOCS="https://docs.oracle.com/en/java/javase/17/docs/api/"

if ! [ -d "$JabaDoc" ]
then
    mkdir "$JabaDoc"
fi

# shellcheck disable=SC2086
# shellcheck disable=SC2115
rm -r $JabaDoc/* &> /dev/null

ACTIONS="-d $JabaDoc -link $DOCS -sourcepath $CodeSRC -author -private"
MYCODE_PATH="info.kgeorgiy.ja.churakova.$PACKAGE"
MYCLASSES="$MyClassesPath/*.java $MyClassesPath/account/*.java $MyClassesPath/bank/*.java $MyClassesPath/exceptions/*.java $MyClassesPath/person/*.java"
CLASSES_DEP="$ClassDepend/Impler.java $ClassDepend/JarImpler.java $ClassDepend/ImplerException.java"


javadoc $ACTIONS $MYCODE_PATH $MYCLASSES