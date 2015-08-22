#!/bin/bash

CPP=g++
INCS='
  -I/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Headers
  -I/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers
  -I/Developer/SDKs/MacOSX10.6.sdk/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers
'
SRCS='
  libusb/usbdevice.cpp
'
PWD=`pwd`
JAVASRC=$PWD/../Logic140Client/src
BUILD=$PWD/build

mkdir -p $BUILD

$CPP $INCS -dynamiclib -o $BUILD/libLogic140_winusb.jnilib $SRCS || exit
javac -g -d $JAVASRC `find $JAVASRC -name \*.java` || exit

cd $BUILD && java -Djava.library.path=$BUILD  -classpath $JAVASRC logic140.Main || exit
