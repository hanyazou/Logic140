#!/bin/bash

CXX=g++
CFLAGS="
  -I/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Headers
  -I/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers
  -I/Developer/SDKs/MacOSX10.6.sdk/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers
  `pkg-config libusb-1.0 --cflags`
"
LIBS="`pkg-config libusb-1.0 --libs`"
SRCS="
  libusb/usbdevice.cpp
"
PWD=`pwd`
JAVASRC=$PWD/../Logic140Client/src
BUILD=$PWD/build

mkdir -p $BUILD

$CXX $CFLAGS -dynamiclib -o $BUILD/libLogic140_winusb.jnilib $SRCS $LIBS || exit
javac -g -d $JAVASRC `find $JAVASRC -name \*.java` || exit

cd $BUILD && java -Djava.library.path=$BUILD  -classpath $JAVASRC logic140.Main || exit
