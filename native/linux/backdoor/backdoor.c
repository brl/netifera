#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/uio.h>
#include <arpa/inet.h>
#include <string.h>
#include <errno.h>

#define BD_PF_PACKET	17
#define BD_SOCK_DGRAM	2
#define BD_SOCK_RAW	3
#define BD_ETH_P_ALL	0x0003

static int mm_send_fd(int sock, int fd);

/*
 * usage:
 *        backdoor 0		socket(PF_PACKET, SOCK_DGRAM, htons(ETH_P_ALL))
 *        backdoor 1		socket(PF_PACKET, SOCK_RAW, htons(ETH_P_ALL))
 */
int
main(int argc, char **argv) 
{
	unsigned long idx;
	int s;
	char *endptr;

	if(argc != 2) 
		_exit(-1);
	
	
	idx = strtoul(argv[1], &endptr, 10);

	if( (argv[1] == endptr) || (endptr[0] != '\0') )
		_exit(-1);

	switch(idx) {
	case 0:
		s = socket(BD_PF_PACKET, BD_SOCK_DGRAM, htons(BD_ETH_P_ALL));
		break;
	case 1:
		s = socket(BD_PF_PACKET, BD_SOCK_RAW, htons(BD_ETH_P_ALL));
		break;
	default:
		exit(-1);
	}

	if( (s < 0) || (mm_send_fd(0, s)) ) 
		exit(errno);
	
	exit(0);

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

