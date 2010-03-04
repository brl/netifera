#include <stdio.h>
#include <string.h>
#include <poll.h>
#include <sys/socket.h>
#include <syslog.h>
#include <errno.h>
#include "privd.h"
static void wait_recv_ready(struct privd_instance *privd);
static void attach_fd(struct msghdr *msg, int fd);
static void dump_message(struct privd_instance *privd);

void recv_message(struct privd_instance *privd)
{
	wait_recv_ready(privd);
	int n;
	if((n = recv(privd->socket_fd, privd->message_buffer, sizeof(privd->message_buffer), 0)) < 0)
		abort_daemon(privd, "Calling recv() failed.");

	if(n == 0)
		shutdown_daemon(privd);

	privd->message_ptr = privd->message_buffer;
	privd->message_space = n;
	if(privd->debug_flag)
		dump_message(privd);

}

static void wait_recv_ready(struct privd_instance *privd)
{
	struct pollfd pfds[2];

	memset(&pfds, 0, sizeof(pfds));
	pfds[0].fd = privd->socket_fd;
	pfds[0].events = POLLIN;
	pfds[1].fd = privd->monitor_fd;
	pfds[1].events = POLLIN;

	int rv = 0;
	while(rv == 0 && !(pfds[0].revents & POLLIN)) {
		if((rv = poll(pfds, 2, 5000)) < 0)
			abort_daemon(privd, "Calling poll() failed.");
		if(pfds[1].revents & (POLLIN|POLLERR))
			shutdown_daemon(privd);

		char c;

		if((recv(privd->socket_fd, &c, 0, MSG_PEEK)) < 0 && errno != EAGAIN)
			abort_daemon(privd, "Error in recv() : [%d] %s", errno, strerror(errno));

	}
}

void
send_message(struct privd_instance *privd)
{
	struct msghdr msg;
	struct iovec iov;
	ssize_t length;

	if((length = finalize_message(privd)) < 0)
		return;

	memset(&msg, 0, sizeof(msg));

	iov.iov_base = privd->message_buffer;
	iov.iov_len = length;
	msg.msg_iov = &iov;
	msg.msg_iovlen = 1;

	if(privd->fd_to_send > 0)
		attach_fd(&msg, privd->fd_to_send);

	if(sendmsg(privd->socket_fd, &msg, 0) != length)
		abort_daemon(privd, "Failed sending message with sendmsg().");

}

static void
attach_fd(struct msghdr *msg, int fd)
{
	static char fdbuf[CMSG_SPACE(sizeof (int))];
	struct cmsghdr *cmsg;

	msg->msg_control = (caddr_t)fdbuf;
	msg->msg_controllen = CMSG_LEN(sizeof(int));
	cmsg = CMSG_FIRSTHDR(msg);
	cmsg->cmsg_len = CMSG_LEN(sizeof(int));
	cmsg->cmsg_level = SOL_SOCKET;
	cmsg->cmsg_type = SCM_RIGHTS;
	*(int *)CMSG_DATA(cmsg) = fd;

}

static void
dump_message(struct privd_instance *privd)
{
	size_t length = privd->message_space;
	char buffer[256];
	char *p;
	uint8_t *msg_ptr = privd->message_ptr;
	int i;

	while(length > 0) {
		p = buffer;
		for(i = 0; i < 16; i++) {
			p += sprintf(p, "%.2x ", *msg_ptr++);
			length--;
			if(length == 0) break;
		}
		DEBUG("> %s", buffer);
	}
}
