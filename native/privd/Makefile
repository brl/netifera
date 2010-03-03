TARGET=netifera_privd
all: $(TARGET) bcrypt

OBJS=privd.o authentication.o dispatch.o initialize.o message.o socket.o install.o config.o
LIBS=bcrypt/bcrypt.a
CFLAGS=-Wall -D_GNU_SOURCE
CC=gcc
LD=gcc

.PHONY: $(TARGET) bcrypt

bcrypt: 
	$(MAKE) -C bcrypt

.c.o:
	$(CC) $(CFLAGS) $(CPPFLAGS) -c $<

$(TARGET): $(OBJS) bcrypt
	$(LD) -o $@ $(OBJS) $(LDFLAGS) $(LIBS)

install:
	cp $(TARGET) /usr/local/bin/$(TARGET)
	chmod 4755 /usr/local/bin/$(TARGET)
clean:
	rm -f $(OBJS) $(TARGET)
	$(MAKE) -C bcrypt clean

