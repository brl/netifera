TARGET=netifera_privd
all: $(TARGET)

OBJS=privd.o authentication.o dispatch.o initialize.o message.o socket.o
CFLAGS=-Wall -D_GNU_SOURCE
CC=gcc
LD=gcc

.c.o:
	$(CC) $(CFLAGS) $(CPPFLAGS) -c $<

$(TARGET): $(OBJS)
	$(LD) -o $@ $(OBJS) $(LDFLAGS) $(LIBS)

install:
	cp $(TARGET) /usr/local/bin/$(TARGET)
	chmod 4755 /usr/local/bin/$(TARGET)
clean:
	rm -f $(OBJS) $(TARGET)

