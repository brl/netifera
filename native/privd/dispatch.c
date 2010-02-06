#include <stdio.h>
#include <fcntl.h>
#include <errno.h>
#include <unistd.h>
#include <sys/socket.h>
#include "privd.h"

static int ping_handler(struct privd_instance *, void *, size_t);
static int authenticate_handler(struct privd_instance *, void *, size_t);
static int open_socket_handler(struct privd_instance *, void *, size_t);
static int open_bpf_handler(struct privd_instance *, void *, size_t);

static struct message_handler message_handlers[PRIVD_MESSAGE_MAX] = {
		[PRIVD_PING] = { .handler = ping_handler },
		[PRIVD_AUTHENTICATE] = { .handler = authenticate_handler },
		[PRIVD_OPEN_SOCKET] = { .handler = open_socket_handler },
		[PRIVD_OPEN_BPF] = { .handler = open_bpf_handler }
};

void
dispatch_message(struct privd_instance *privd)
{
	struct privd_msghdr *msg = (struct privd_msghdr *)privd->message_buffer;

	uint8_t code = msg->type;

	if(!privd->authenticated && code != PRIVD_AUTHENTICATE) {
		send_error(privd, "Not authenticated.");
		return;
	}

	if(privd->message_size < PRIVD_HEADER_SIZE) {
		send_error(privd, "Truncated message size: %d",
				privd->message_size);
		return;
	}

	if(code >= PRIVD_MESSAGE_MAX) {
		send_error(privd, "Invalid message type: %d", code);
		return;
	}

	if(message_handlers[code].handler == NULL) {
		send_error(privd, "Unhandled message type: %d", code);
		return;
	}

	size_t data_length = privd->message_size - PRIVD_HEADER_SIZE;
	void *data = NULL;
	if(data_length)
		data = privd->message_buffer + PRIVD_HEADER_SIZE;

	message_handlers[code].handler(privd, data, data_length);
}

static int
ping_handler(struct privd_instance *privd, void *data, size_t length) {
	send_ok(privd);
	return 0;
}

static int
authenticate_handler(struct privd_instance *privd, void *data, size_t length) {
	return 0;
}

static int
open_socket_handler(struct privd_instance *privd, void *data, size_t length) {

	int family, type, protocol;
	if(
		get_integer_argument(privd, &family) ||
		get_integer_argument(privd, &type) ||
		get_integer_argument(privd, &protocol)) {

		send_error(privd, "Invalid arguments in PRIVD_OPEN_SOCKET message.");
		return -1;
	}
	int fd = socket(family, type, protocol);
	if(fd < 0) {
		send_error(privd, "Could not create socket");
		return -1;
	}
	send_fd(privd, fd);
	close(fd);
	return 0;
}

#define MAX_BPF_DEVICE 10
static int
open_bpf_handler(struct privd_instance *privd, void *data, size_t length) {
	int i;
	char device_path[16];
	int fd;
	int flags = O_RDONLY;
	for(i = 0; i < MAX_BPF_DEVICE; i++) {
		snprintf(device_path, sizeof(device_path), "/dev/bpf%d", i);
		if((fd = open(device_path, flags)) > 0) {
			send_fd(privd, fd);
			close(fd);
			return 0;
		}
		if(errno != EBUSY) {
			send_error(privd, "Error opening BPF device");
			return -1;
		}
	}
	send_error(privd, "All BPF devices are busy");
	return -1;


}
