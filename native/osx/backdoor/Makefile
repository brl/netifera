all: backdoor
CFLAGS=-Wall -O2
LD=gcc
CC=gcc

# where to find the plugin projects

WORKSPACE_ROOT=../../..

# name of the package which contains the java class we are producing an JNI for

PACKAGE=com.netifera.platform.system.osx

OBJS=backdoor.o

.c.o: 
	$(CC) $(CFLAGS) -c $<

backdoor: $(OBJS)
	$(LD) $(LDFLAGS) -o backdoor $(OBJS)

install: backdoor
	cp backdoor $(WORKSPACE_ROOT)/$(PACKAGE)
clean:
	rm -f *.o backdoor
