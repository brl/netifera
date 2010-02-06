#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <syslog.h>
#include <errno.h>
#include "privd.h"

static void run_loop(struct privd_instance *);
static char *create_abort_syslog_format(const char *orig_format);


int
main(int argc, char **argv)
{
	struct privd_instance *privd = calloc(1, sizeof(struct privd_instance));
	privd->debug_flag = 1;
	initialize(privd);
	run_loop(privd);
	return 0;
}


static void
run_loop(struct privd_instance *privd) {

	while(1) {
		recv_message(privd);
		dispatch_message(privd);
	}
}


void
shutdown_daemon(struct privd_instance *privd)
{
	syslog(LOG_INFO, "Shutting down.");
	close(privd->socket_fd);
	closelog();
	exit(EXIT_SUCCESS);
}

void
abort_daemon(struct privd_instance *privd, const char *fmt, ...)
{
	char *format = create_abort_syslog_format(fmt);

	va_list args;
	va_start(args, fmt);
	vsyslog(LOG_WARNING, format, args);
	va_end(args);

	free(format);
	close(privd->socket_fd);
	closelog();
	exit(EXIT_FAILURE);
}

static char *
create_abort_syslog_format(const char *orig_format)
{
	char *prefix = "Shutting down on error : ";
	char *suffix = (errno != 0) ? " (%m)" : "";
	size_t format_size = strlen(prefix) + strlen(orig_format) + strlen(suffix);
	char *format_buffer = malloc(format_size);
	strcpy(format_buffer, prefix);
	strcat(format_buffer, orig_format);
	strcat(format_buffer, suffix);
	return format_buffer;
}
