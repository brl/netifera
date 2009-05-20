all: libsystem.so

# Ubuntu include path for java headers

JDK_ROOT=/opt/java

CFLAGS=-I$(JDK_ROOT)/include -fPIC -I$(JDK_ROOT)/include/linux -m32 -Wall -O2
LD=gcc
CC=gcc
LDFLAGS=-m32 -s -shared

# Name used for source, header, and object file

PREFIX=com_netifera_platform_internal_system_SystemNative

OBJECT=$(PREFIX).o
HEADER=$(PREFIX).h
SOURCE=$(PREFIX).c

# where to find the plugin projects

WORKSPACE_ROOT=../../../platform/com.netifera.platform.core

# name of the package which contains the java class we are producing an JNI for

PACKAGE=com.netifera.platform.system.linux

# name of the class itself

CLASS=com.netifera.platform.internal.system.SystemNative

# classpath for calling javah  ie: '../../com.netifera.platform.system.linux/bin'

CLASSPATH=$(WORKSPACE_ROOT)/$(PACKAGE)/bin



libsystem.so: $(OBJECT)
	$(LD) $(LDFLAGS) -o libsystem.so $(OBJECT)


$(HEADER):
	javah -classpath $(CLASSPATH) -d . $(CLASS)

$(OBJECT): $(HEADER) $(SOURCE)
	$(CC) $(CFLAGS) -c $(SOURCE)

install: libsystem.so
	cp libsystem.so $(WORKSPACE_ROOT)/$(PACKAGE)
clean:
	rm -f $(HEADER) $(OBJECT) libsystem.so


