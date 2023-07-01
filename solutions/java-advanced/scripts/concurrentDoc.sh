#!/bin/bash

PACKAGE=concurrent
SRC=$(realpath ../../)/java-advanced
JabaDocConcurrent=$SRC/javadoc/concurrent
CodeSRC=$SRC/java-solutions
Tests=$(realpath ../../../)/java-advanced-2023
TestModule=info.kgeorgiy.java.advanced.$PACKAGE
AdditionModule=info.kgeorgiy.java.advanced.mapper
TestModulePath=$Tests/modules/$TestModule
AdditionModulePath=$Tests/modules/$AdditionModule
ClassDepend=$TestModulePath/info/kgeorgiy/java/advanced/concurrent
ClassDepend2=$AdditionModulePath/info/kgeorgiy/java/advanced/mapper
DOCS="https://docs.oracle.com/en/java/javase/17/docs/api/"

if ! [ -d "$JabaDocConcurrent" ]
then
    mkdir "$JabaDocConcurrent"
fi

# shellcheck disable=SC2086
# shellcheck disable=SC2115
rm -r $JabaDocConcurrent/* &> /dev/null

ACTIONS="-d $JabaDocConcurrent -link $DOCS -sourcepath $CodeSRC:$TestModulePath:$AdditionModulePath -p $Tests/artifacts:$Tests/lib --add-modules $TestModule -author -private"
MYCODE_PATH="info.kgeorgiy.ja.churakova.$PACKAGE"
MYCLASSES="$CodeSRC/info/kgeorgiy/ja/churakova/$PACKAGE/*.java"
CLASSES_DEP="$ClassDepend/ScalarIP.java $ClassDepend/ListIP.java $ClassDepend/AdvancedIP.java $ClassDepend2/ParallelMapper.java"


javadoc $ACTIONS $MYCODE_PATH $MYCLASSES $CLASSES_DEP