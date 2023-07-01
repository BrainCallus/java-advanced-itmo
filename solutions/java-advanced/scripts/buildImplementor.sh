#!/bin/bash

PACKAGE=implementor
SRC=$(realpath ../../)/java-advanced # кажется, лучше использовать просто .. (у меня ваш репозиторий называется не java-advanced)
CodeSRC=$SRC/java-solutions
Tests=$(realpath ../../)/java-advanced-2023 #по условию ваш репоизторий лежит параллельно с репозиторием java-advanced-2023
TestModule=info.kgeorgiy.java.advanced.$PACKAGE
TestModulePath=$Tests/modules/$TestModule
ClassDepend=$TestModulePath/info/kgeorgiy/java/advanced/implementor

ACTIONS=" -sourcepath $CodeSRC:$TestModulePath -p $Tests/artifacts:$Tests/lib --add-modules $TestModule"
MYCLASSES="$CodeSRC/info/kgeorgiy/ja/churakova/$PACKAGE"
CLASSES_DEP="$ClassDepend/Impler.java $ClassDepend/JarImpler.java $ClassDepend/ImplerException.java"
COPMILED_CLASSES="$ClassDepend/Impler.class $ClassDepend/JarImpler.class $ClassDepend/ImplerException.class"
# shellcheck disable=SC2086
javac $ACTIONS $MYCLASSES/*.java $CLASSES_DEP
# shellcheck disable=SC2086
jar xf $Tests/artifacts/$TestModule.jar $COPMILED_CLASSES
# shellcheck disable=SC2086
jar cfm Implementor.jar $SRC/scripts/MANIFEST.MF $MYCLASSES/Implementor.class $COPMILED_CLASSES
