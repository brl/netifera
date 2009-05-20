#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/uio.h>
#include <string.h>
#include <errno.h>

#define MAX_BPF_DEVICE  10

static int mm_send_fd(int sock, int fd);
static int openBPF(int flags);


/*
 * usage:
 *        backdoor 0		open("/dev/bpfX", O_RDONLY);
 *        backdoor 1		open("/dev/bpfX", O_RDWR);
 */
int
main(int argc, char **argv) 
{
	unsigned long idx;
	int fd;
	char *endptr;

	if(argc != 2) 
		_exit(-1);
	
	
	idx = strtoul(argv[1], &endptr, 10);

	if( (argv[1] == endptr) || (endptr[0] != '\0') )
		_exit(-1);

	switch(idx) {
	case 0:
                fd = openBPF(O_RDONLY);
		break;
	case 1:
                fd = openBPF(O_RDWR);
		break;
	default:
		exit(-1);
	}

	if( (fd < 0) || (mm_send_fd(0, fd)) ) 
		exit(errno);
	
	exit(0);

}

static int
openBPF(int flags) {
        int fd;
        int i;
        char device_path[16];

        for(i = 0; i < MAX_BPF_DEVICE; i++) {
                snprintf(device_path, sizeof(device_path), "/dev/bpf%d", i);
                if( (fd = open(device_path, flags)) > 0) 
                        return fd;
                if(errno != EBUSY) 
                        return -1;
        }
        return -1;

}

/*
 * Stolen from openssh-4.6p1/monitor_fdpass.c
 */
static int
mm_send_fd(int sock, int fd)
{
        struct msghdr msg;
        struct iovec vec;
        char ch = '\0';
        char tmp[CMSG_SPACE(sizeof(int))];
        struct cmsghdr *cmsg;

        memset(&msg, 0, sizeof(msg));
        msg.msg_control = (caddr_t)tmp;
        msg.msg_controllen = CMSG_LEN(sizeof(int));
        cmsg = CMSG_FIRSTHDR(&msg);
        cmsg->cmsg_len = CMSG_LEN(sizeof(int));
        cmsg->cmsg_level = SOL_SOCKET;
        cmsg->cmsg_type = SCM_RIGHTS;
        *(int *)CMSG_DATA(cmsg) = fd;

        vec.iov_base = &ch;
        vec.iov_len = 1;
        msg.msg_iov = &vec;
        msg.msg_iovlen = 1;

        if (sendmsg(sock, &msg, 0) != 1)
                return -1;
        else
                return 0;

}

