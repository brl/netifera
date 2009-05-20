all: libsystem.jnilib

# OS X include path for java headers

CFLAGS=-I/System/Library/Frameworks/JavaVM.framework/Headers

# Name used for source, header, and object file

PREFIX=com_netifera_platform_internal_system_SystemNative

OBJECT=$(PREFIX).o
HEADER=$(PREFIX).h
SOURCE=$(PREFIX).c

# where to find the plugin projects

WORKSPACE_ROOT=../../..

# name of the package which contains the java class we are producing an JNI for

PACKAGE=com.netifera.platform.system.osx

# name of the class itself

CLASS=com.netifera.platform.internal.system.SystemNative

# classpath for calling javah  ie: '../../com.netifera.platform.system.osx/bin'

CLASSPATH=$(WORKSPACE_ROOT)/$(PACKAGE)/bin

LIBS=-lc

libsystem.jnilib: $(OBJECT)
	$(LD) -dylib -arch i386 -o libsystem.jnilib $(LIBS) $(OBJECT)


$(HEADER):
	javah -classpath $(CLASSPATH) -d . $(CLASS)

$(OBJECT): $(HEADER) $(SOURCE)
	$(CC) $(CFLAGS) -c $(SOURCE)

install: libsystem.jnilib
	cp libsystem.jnilib $(WORKSPACE_ROOT)/$(PACKAGE)
clean:
	rm -f $(HEADER) $(OBJECT) libsystem.jnilib


