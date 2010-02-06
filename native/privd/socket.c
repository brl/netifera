#include <stdio.h>
#include <string.h>
#include <poll.h>
#include <sys/socket.h>
#include <syslog.h>
#include "privd.h"
static void wait_recv_ready(struct privd_instance *privd);
static void attach_fd(struct msghdr *msg, int fd);

void recv_message(struct privd_instance *privd)
{
	wait_recv_ready(privd);

	int n;
	if((n = recv(privd->socket_fd, privd->message_buffer, sizeof(privd->message_buffer), 0)) < 0)
		abort_daemon(privd, "Calling recv() failed.");

	if(n == 0)
		shutdown_daemon(privd);

	privd->message_size = n;

}

static void wait_recv_ready(struct privd_instance *privd)
{
	struct pollfd pfd = {
			.fd = PRIVD_FD,
			.events = POLLIN
	};

	int rv = 0;
	while(rv == 0 && !(pfd.revents & POLLIN)) {
		if((rv = poll(&pfd, 1, 5000)) < 0)
			abort_daemon(privd, "Calling poll() failed.");

		char c;

		if(recv(privd->socket_fd, &c, 0, MSG_PEEK) < 0)
			shutdown_daemon(privd);

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
