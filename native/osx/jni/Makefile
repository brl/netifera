all: libsystem.jnilib

# OS X include path for java headers

CFLAGS=-I/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers -mmacosx-version-min=10.4 -Wall -arch ppc -arch i386 -arch x86_64

LD=gcc
CC=gcc

# Name used for source, header, and object file

PREFIX=com_netifera_platform_internal_system_SystemNative

OBJECT=$(PREFIX).o
HEADER=$(PREFIX).h
SOURCE=$(PREFIX).c

# where to find the plugin projects

WORKSPACE_ROOT=../../../platform/com.netifera.platform.core

# name of the package which contains the java class we are producing an JNI for

PACKAGE=com.netifera.platform.system.osx

# name of the class itself

CLASS=com.netifera.platform.internal.system.SystemNative

# classpath for calling javah  ie: '../../../platform/com.netifera.platform.core/com.netifera.platform.system.osx/bin'

CLASSPATH=$(WORKSPACE_ROOT)/$(PACKAGE)/bin

LIBS=-lc
LDFLAGS=-dynamiclib -arch i386 -arch ppc -arch x86_64 -mmacosx-version-min=10.4

libsystem.jnilib: $(OBJECT)
	$(LD) $(LDFLAGS) -o libsystem.jnilib $(LIBS) $(OBJECT)


$(HEADER):
	javah -classpath $(CLASSPATH) -d . $(CLASS)

$(OBJECT): $(HEADER) $(SOURCE)
	$(CC) $(CFLAGS) -c $(SOURCE)

install: libsystem.jnilib
	cp libsystem.jnilib $(WORKSPACE_ROOT)/$(PACKAGE)
clean:
	rm -f $(HEADER) $(OBJECT) libsystem.jnilib


