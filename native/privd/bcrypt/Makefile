TARGET=bcrypt.a
OBJ=bcrypt.o blowfish.o arc4random.o
CFLAGS=-Wall -O2
RANLIB=ranlib
all: $(TARGET)

.c.o:
	$(CC) $(CFLAGS) -c $<

$(TARGET): $(OBJ)
	@rm -f $@
	$(AR) rc $@ $(OBJ) $(LIBS)
	$(RANLIB) $@

clean:
	rm -f $(OBJ) $(TARGET)
