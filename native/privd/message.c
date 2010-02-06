
#include <stdio.h>
#include <stdarg.h>
#include <errno.h>
#include <syslog.h>
#include <sys/types.h>
#include <string.h>
#include "privd.h"

static int check_space(struct privd_instance *, size_t);
static struct privd_arghdr *init_argument(struct privd_instance *, int, size_t);
static void append_error_message(struct privd_instance *, const char *, ...);
static void append_errno_message(char *, size_t);
void
send_ok(struct privd_instance *privd)
{
	initialize_message(privd, PRIVD_RESPONSE_OK);
	send_message(privd);
}

void
send_fd(struct privd_instance *privd, int fd)
{
	initialize_message(privd, PRIVD_RESPONSE_FD);
	privd->fd_to_send = fd;
	send_message(privd);
}

void
send_error(struct privd_instance *privd, const char *fmt, ...)
{
	initialize_message(privd, PRIVD_RESPONSE_ERROR);

	va_list args;
	va_start(args, fmt);
	append_error_message(privd, fmt, args);
	va_end(args);

	send_message(privd);
}

void
send_startup(struct privd_instance *privd, int message_type, const char *fmt, ...)
{
	initialize_message(privd, PRIVD_RESPONSE_STARTUP);

	add_integer_argument(privd, message_type);

	if(fmt != NULL) {
		va_list args;
		va_start(args, fmt);
		append_error_message(privd, fmt, args);
		va_end(args);
	}

	send_message(privd);

}

static void
append_error_message(struct privd_instance *privd, const char *fmt, ...)
{
	char message_buffer[PRIVD_ERROR_BUFFER_SIZE];
	va_list args;
	va_start(args, fmt);
	if(vsnprintf(message_buffer, sizeof(message_buffer), fmt, args) >= sizeof(message_buffer))
		syslog(LOG_WARNING, "Error message was truncated by overflow.");
	va_end(args);

	if(errno)
		append_errno_message(message_buffer, sizeof(message_buffer));

	if(add_string_argument(privd, message_buffer))
		abort_daemon(privd, "Message buffer overflowed sending error message.");

}

static void
append_errno_message(char *buffer, size_t buffer_size)
{
	if(!errno)
		return;
	const char *errno_msg = strerror(errno);
	size_t msg_length = strlen(errno_msg);
	size_t buffer_length = strlen(buffer);

	if((msg_length + buffer_length + 3) >= buffer_size) {
		syslog(LOG_WARNING, "No space to append errno message");
		return;
	}
	strcat(buffer, " (");
	strcat(buffer, errno_msg);
	strcat(buffer, ")");
}


int
add_string_argument(struct privd_instance *privd, const char *string)
{
	size_t size = strlen(string) + 1;
	struct privd_arghdr *arghdr = init_argument(privd, PRIVD_ARG_STRING, size);
	if(arghdr == NULL)
		return -1;
	strcpy((char *)arghdr->data, string);
	return 0;
}

int
add_integer_argument(struct privd_instance *privd, uint32_t value)
{
	size_t size = sizeof(uint32_t);
	struct privd_arghdr *arghdr = init_argument(privd, PRIVD_ARG_INTEGER, size);
	if(arghdr == NULL)
		return -1;
	(*(uint32_t *)arghdr->data) = htonl(value);
	return 0;
}

int
get_integer_argument(struct privd_instance *privd, uint32_t *value)
{
	size_t size = sizeof(struct privd_arghdr) + sizeof(uint32_t);
	if(!check_space(privd, size))
		return -1;

	struct privd_arghdr *arghdr = privd->message_ptr;

	if(arghdr->type != PRIVD_ARG_INTEGER)
		return -1;
	if(ntohs(arghdr->length) != sizeof(uint32_t))
		return -1;

	privd->message_ptr += size;

	*value = ntohl((*(uint32_t *)arghdr->data));
	return 0;

}
static struct privd_arghdr *
init_argument(struct privd_instance *privd, int type, size_t size)
{
	if(!check_space(privd, size + sizeof(struct privd_arghdr)))
		return NULL;
	struct privd_arghdr *arghdr = (struct privd_arghdr *)privd->message_ptr;
	privd->message_ptr += (size + sizeof(struct privd_arghdr));
	arghdr->type = type;
	arghdr->pad = 0;
	arghdr->length = htons(size);
	return arghdr;
}

static int
check_space(struct privd_instance *privd, size_t size)
{
	ssize_t consumed = privd->message_ptr - privd->message_buffer;
	if(consumed < 0) {
		return 0;
	}
	ssize_t remaining = sizeof(privd->message_buffer) - consumed;
	if((remaining < 0) || (remaining < size))
		return 0;
	else
		return 1;
}

void
initialize_message(struct privd_instance *privd, int message_type)
{
	struct privd_msghdr *header = (struct privd_msghdr *)privd->message_buffer;
	header->version = PRIVD_PROTOCOL_VERSION;
	header->type = message_type;
	header->length = 0;
	privd->fd_to_send = -1;
	privd->message_ptr = privd->message_buffer + sizeof(struct privd_msghdr);
}

int
finalize_message(struct privd_instance *privd)
{
	int length = privd->message_ptr - privd->message_buffer;
	if(length < 0 || length < PRIVD_HEADER_SIZE || length > PRIVD_MAX_MSG_SIZE) {
		syslog(LOG_WARNING, "Attempt to send message with an illegal length: %d", length);
		return -1;
	}
	struct privd_msghdr *header = (struct privd_msghdr *)privd->message_buffer;
	header->length = htons(length);
	return length;
}
