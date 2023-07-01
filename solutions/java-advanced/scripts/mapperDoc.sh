#!/bin/bash

PACKAGE=mapper
SRC=$(realpath ../../)/java-advanced
JabaDocMapper=$SRC/javadoc/mapper
CodeSRC=$SRC/java-solutions
Tests=$(realpath ../../../)/java-advanced-2023
TestModule=info.kgeorgiy.java.advanced.$PACKAGE
TestModulePath=$Tests/modules/$TestModule
ClassDepend1=$Tests/modules/info.kgeorgiy.java.advanced.concurrent/info/kgeorgiy/java/advanced/concurrent
ClassDepend2=$TestModulePath/info/kgeorgiy/java/advanced/mapper
DOCS="https://docs.oracle.com/en/java/javase/17/docs/api/"

if ! [ -d "$JabaDocMapper" ]
then
    mkdir "$JabaDocMapper"
fi

# shellcheck disable=SC2086
# shellcheck disable=SC2115
rm -r $JabaDocMapper/* &> /dev/null

ACTIONS="-d $JabaDocMapper -link $DOCS -sourcepath $CodeSRC:$TestModulePath -p $Tests/artifacts:$Tests/lib --add-modules $TestModule -author -private"
MYCODE_PATH="info.kgeorgiy.ja.churakova.$PACKAGE"
MYCLASSES="$CodeSRC/info/kgeorgiy/ja/churakova/$PACKAGE/*.java"
CLASSES_DEP="$ClassDepend2/ParallelMapper.java"


javadoc $ACTIONS $MYCODE_PATH $MYCLASSES $CLASSES_DEP