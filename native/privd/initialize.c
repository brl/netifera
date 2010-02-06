#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <syslog.h>
#include <sys/stat.h>
#include "privd.h"

static void initialize_instance(struct privd_instance *);
static void close_descriptors(struct privd_instance *);
static void dupfd(struct privd_instance *, int, int);
static void daemonize(struct privd_instance *);

void
initialize(struct privd_instance *privd)
{
	initialize_instance(privd);
	close_descriptors(privd);
	openlog("netifera_privd", 0, LOG_DAEMON);
	syslog(LOG_INFO, "Starting Netifera privilege daemon v1.0");
	daemonize(privd);


	if(fcntl(privd->socket_fd, F_SETFL, O_NONBLOCK) < 0) {
		char *error_msg = "Failed setting non-blocking mode on socket.";
		send_startup(privd, PRIVD_STARTUP_INITIALIZATION_FAILED, error_message);
		abort_daemon(privd, error_msg);
	}

	if(geteuid() != 0) {
		char *error_msg =
			"Failed to start because privd not installed setuid root. euid=%d";
		send_startup(privd, PRIVD_LAUNCH_ERROR_NOTROOT, error_msg, geteuid());
		abort_daemon(privd, error_msg, geteuid());
	}

	read_authentication_data(privd);

}

static void
initialize_instance(struct privd_instance *privd)
{
	privd->socket_fd = PRIVD_FD;
	privd->authenticated = 0;
	privd->message_ptr = privd->message_buffer;
	privd->fd_to_send = -1;
}

static void
close_descriptors(struct privd_instance *privd)
{
	int i;
	for(i = getdtablesize(); i>=0; --i) {
		if(privd->debug_flag && i == STDERR_FILENO)
			continue;
		if(i != privd->socket_fd)
			close(i);
	}
	int fd = open("/dev/null", O_RDWR);
	if(fd == -1)
		return;
	dupfd(privd, fd, STDIN_FILENO);
	dupfd(privd, fd, STDOUT_FILENO);
	dupfd(privd, fd, STDERR_FILENO);
	if(fd > STDERR_FILENO)
		close(fd);
}

static void
dupfd(struct privd_instance *privd, int from, int to)
{
	if(privd->debug_flag && to == STDERR_FILENO)
		return;
	if(to == privd->socket_fd)
		return;
	dup2(from, to);
}

static void
daemonize(struct privd_instance *privd)
{
	pid_t pid = fork();

	if(pid  < 0)
		abort_daemon(privd, "Failed to fork()");

	if(pid > 0)
		exit(0);

	umask(0);

	if(chdir("/") < 0)
		abort_daemon(privd, "Failed to chdir() to root directory.");

	if(setsid() == -1)
		abort_daemon(privd, "Failed to create a session with setsid().");

}
