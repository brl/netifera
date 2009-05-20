#include <jni.h>
#include "com_netifera_platform_internal_system_SystemNative.h"

#include <fcntl.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <poll.h>
#include <sched.h>

static int mm_receive_fd(int sock);
static int backdoor_request(const char *backdoor_path, int request, int *err);
static int do_forkexec(const char *path, int fd, int *err);

static int
backdoor_request(const char *backdoor_path, int request, int *err)
{
	char reqbuf[16];
	char *args[3];
	int sv[2];
	pid_t child_pid;
	int sock, status;

	if(access(backdoor_path, X_OK)) {
		*err = errno;
		return -1;
	}

	if(socketpair(AF_UNIX, SOCK_STREAM, 0, sv) < 0) {
		*err = errno;
		return -1;
	}

	snprintf(reqbuf, sizeof(reqbuf), "%d", request);

	args[0] = (char *) backdoor_path;
	args[1] = reqbuf;
	args[2] = NULL;

	if((child_pid = fork()) < 0) {
		*err = errno;
		close(sv[0]);
		close(sv[1]);
		return -1;
	}

	if(child_pid == 0) { /* child */
		close(sv[1]);
		if((dup2(sv[0], 0) == -1) || execv(backdoor_path, args))
			exit(EXIT_FAILURE);
	}

	/* parent */

	close(sv[0]);
	if( (sock = mm_receive_fd(sv[1])) < 0 ) {
		*err = errno;
		close(sv[1]);
		waitpid(child_pid, &status, 0);
		return -1;
	}

	close(sv[1]);

	if(waitpid(child_pid, &status, 0) < 0) {
		*err = errno;
		close(sock);
		return -1;
	}

	if(!WIFEXITED(status)) {
		*err = EFAULT;
		close(sock);
		return -1;
	}

	if(WEXITSTATUS(status) != 0) {
		*err = -WEXITSTATUS(status);
		close(sock);
		return -1;
	}

	*err = 0;
	return sock;
}

static int
mm_receive_fd(int sock)
{
	struct msghdr msg;
	struct iovec vec;
	char ch;
	int fd;
	ssize_t size;
	char tmp[CMSG_SPACE(sizeof(int))];
	struct cmsghdr *cmsg;


	memset(&msg, 0, sizeof(msg));
	vec.iov_base = &ch;
	vec.iov_len = 1;
	msg.msg_iov = &vec;
	msg.msg_iovlen = 1;
	msg.msg_control = tmp;
	msg.msg_controllen = sizeof(tmp);

	while( (size = recvmsg(sock, &msg, 0)) == -1 && errno == EINTR)
	        sched_yield();

	if(size != 1)
	        return -1;

	cmsg = CMSG_FIRSTHDR(&msg);

	if (cmsg == NULL)
		return -1;

	if (cmsg->cmsg_type != SCM_RIGHTS)
		return -1;

	fd = (*(int *)CMSG_DATA(cmsg));

	return fd;
}

/*
 * Class:     com_netifera_platform_internal_system_SystemNative
 * Method:    native_backdoor
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_netifera_platform_internal_system_SystemNative_native_1backdoor
  (JNIEnv *env, jobject klass, jstring path, jint request)
{
	const char *p;
	jint ret;
	int err;

	p = (*env)->GetStringUTFChars(env, path, NULL);
	if(p == NULL)
		return -ENOMEM;

	if( (ret = backdoor_request(p, request, &err)) < 0) {
		ret = -err;
	}

	(*env)->ReleaseStringUTFChars(env, path, p);

	return ret;
}


/*
 * Class:     com_netifera_platform_internal_system_SystemNative
 * Method:    native_socket
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_com_netifera_platform_internal_system_SystemNative_native_1socket
  (JNIEnv *env, jclass klass, jint domain, jint type, jint protocol)
{
	int s;

	if( (s = socket(domain, type, protocol)) == -1) {
		return -errno;
	}

	return s;
}


#define IOCTL_BUFFER_SIZE 256
/*
 * Class:     com_netifera_platform_internal_system_SystemNative
 * Method:    native_ioctl
 * Signature: (II[BII)I
 */
JNIEXPORT jint JNICALL Java_com_netifera_platform_internal_system_SystemNative_native_1ioctl
  (JNIEnv *env, jclass klass, jint fd, jint request, jbyteArray data, jint inlen, jint outlen)
{
	jbyte buffer[IOCTL_BUFFER_SIZE];
	int array_length;

	array_length = (*env)->GetArrayLength(env, data);

	if( (inlen > IOCTL_BUFFER_SIZE) || (outlen > IOCTL_BUFFER_SIZE) )
		return -EINVAL;

	if( (inlen > array_length) || (outlen > array_length) )
		return -EINVAL;

	if(inlen > 0)
		(*env)->GetByteArrayRegion(env, data, 0, inlen, buffer);

	if(ioctl(fd, request, buffer) == -1) {
		return -errno;
	}

	if(outlen > 0) {
		(*env)->SetByteArrayRegion(env, data, 0, outlen, buffer);
	}

	return 0;
}

/*
 * Class:     com_netifera_platform_internal_system_SystemNative
 * Method:    native_recvfrom
 * Signature: (I[BIII[BI)I
 */
JNIEXPORT jint JNICALL Java_com_netifera_platform_internal_system_SystemNative_native_1recvfrom
  (JNIEnv *env, jclass klass, jint fd, jbyteArray buffer, jint offset, jint size, jint flags,
	jbyteArray data, jint datalen)
{
	jbyte *pdata, *pbuffer;
	socklen_t fromlen;
	int ret;

	if(datalen > (*env)->GetArrayLength(env, data))
		return -EINVAL;


	if(size > ((*env)->GetArrayLength(env, buffer) - offset))
		return -EINVAL;

	if( (pdata = (*env)->GetByteArrayElements(env, data, NULL)) == NULL)
		return -EINVAL;

	if( (pbuffer = (*env)->GetByteArrayElements(env, buffer, NULL)) == NULL) {
		(*env)->ReleaseByteArrayElements(env, data, pdata, 0);
		return -EINVAL;
	}

	fromlen = datalen;

	while( (ret = recvfrom(fd, pbuffer + offset, size, flags, (struct sockaddr *)pdata,
		&fromlen)) < 0) {
                if(errno != EINTR) {
                    ret = -errno;
                    break;
                }
	}

	(*env)->ReleaseByteArrayElements(env, data, pdata, 0);
	(*env)->ReleaseByteArrayElements(env, buffer, pbuffer, 0);

	return ret;

}

/*
 * Class:     com_netifera_platform_internal_system_SystemNative
 * Method:    native_bind
 * Signature: (I[BI)I
 */
JNIEXPORT jint JNICALL Java_com_netifera_platform_internal_system_SystemNative_native_1bind
  (JNIEnv *env, jclass klass, jint s, jbyteArray data, jint datalen)
{
	jbyte *pdata;
	int ret = 0;

	if(datalen > (*env)->GetArrayLength(env, data))
		return -EINVAL;

	if( (pdata = (*env)->GetByteArrayElements(env, data, NULL)) == NULL )
		return -EINVAL;

	if(bind(s, (struct sockaddr *)pdata, datalen) < 0)
		ret = -errno;

	(*env)->ReleaseByteArrayElements(env, data, pdata, 0);

	return ret;
}

/*
 * Class:     com_netifera_platform_internal_system_SystemNative
 * Method:    native_getsockopt
 * Signature: (III[BI)I
 */
JNIEXPORT jint JNICALL Java_com_netifera_platform_internal_system_SystemNative_native_1getsockopt
  (JNIEnv *env, jclass klass, jint s, jint level, jint optname, jbyteArray data, jint optlen)
{
	jbyte *pdata;
	socklen_t len = optlen;
	int ret = 0;

	if(optlen > (*env)->GetArrayLength(env, data))
		return -EINVAL;

	if( (pdata = (*env)->GetByteArrayElements(env, data, NULL)) == NULL)
		return -EINVAL;

	if(getsockopt(s, level, optname, pdata, &len) != 0)
		ret = -errno;

	(*env)->ReleaseByteArrayElements(env, data, pdata, 0);

	return ret;
}


/*
 * Class:     com_netifera_platform_internal_system_SystemNative
 * Method:    native_setsockopt
 * Signature: (III[BI)I
 */
JNIEXPORT jint JNICALL Java_com_netifera_platform_internal_system_SystemNative_native_1setsockopt
  (JNIEnv *env, jclass klass, jint s, jint level, jint optname, jbyteArray data, jint optlen)
{
	jbyte buffer[IOCTL_BUFFER_SIZE];
	int array_length;

	array_length = (*env)->GetArrayLength(env, data);

	if( (optlen > array_length) || (optlen > IOCTL_BUFFER_SIZE) )
		return -EINVAL;

	if(optlen > 0)
		(*env)->GetByteArrayRegion(env, data, 0, optlen, buffer);

	if(setsockopt(s, level, optname, buffer, optlen) != 0)
		return -errno;

	return 0;
}

/*
 * Class:     com_netifera_platform_internal_system_SystemNative
 * Method:    native_close
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_netifera_platform_internal_system_SystemNative_native_1close
  (JNIEnv *env, jclass klass, jint fd)
{
	if(close(fd) != 0)
		return -errno;

	return 0;
}

/*
 * Class:     com_netifera_platform_internal_system_SystemNative
 * Method:    native_poll
 * Signature: ([BI)I
 */
JNIEXPORT jint JNICALL Java_com_netifera_platform_internal_system_SystemNative_native_1poll
  (JNIEnv *env, jobject klass, jbyteArray fds, jint timeout)
{
	jbyte *pdata;
	int datalen;
	int nfds;
	int ret;

	datalen = (*env)->GetArrayLength(env, fds);

	if(datalen % sizeof(struct pollfd)) {
		return -EINVAL;
	}

	nfds = datalen / sizeof(struct pollfd);


	if( (pdata = (*env)->GetByteArrayElements(env, fds, NULL)) == NULL )
		return -EINVAL;

	while( (ret = poll( (struct pollfd *) pdata, nfds, timeout)) < 0) {
                if(errno != EINTR) {
                    ret = -errno;
                    break;
                }
        }


	(*env)->ReleaseByteArrayElements(env, fds, pdata, 0);

	return ret;
}

/*
 * Class:     com_netifera_platform_internal_system_SystemNative
 * Method:    native_forkexec
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_netifera_platform_internal_system_SystemNative_native_1forkexec
  (JNIEnv *env, jobject klass, jstring path, jint fd)
{
	const char *p;
	jint ret;
	int err;

	p = (*env)->GetStringUTFChars(env, path, NULL);
	if(p == NULL)
		return -ENOMEM;

	if((ret = do_forkexec(p, fd, &err)) < 0) {
		ret = -err;
	}
	(*env)->ReleaseStringUTFChars(env, path, p);

	return ret;

}

static int
do_forkexec(const char *path, int fd, int *err) {
	char *args[2];
	pid_t child_pid;

	if(access(path, X_OK)) {
		*err = errno;
		return -1;
	}

	args[0] = (char *) path;
	args[1] = NULL;

	if((child_pid = fork()) < 0) {
		*err = errno;
		return -1;
	}

	if(child_pid > 0)
		return child_pid;

	dup2(fd, 0);
	dup2(fd, 1);
	dup2(fd, 2);
	if(fd > 2)
		close(fd);
	execv(path, args);
	exit(EXIT_FAILURE);

}


/*
 * Class:     com_netifera_platform_internal_system_SystemNative
 * Method:    native_open
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_netifera_platform_internal_system_SystemNative_native_1open
  (JNIEnv *env, jobject klass, jstring path, jint flags)
{
	const char *p;
	jint ret;

	p = (*env)->GetStringUTFChars(env, path, NULL);
	if(p == NULL)
		return -ENOMEM;

	if((ret = open(p, flags)) < 0) {
		ret = -errno;
	}
	(*env)->ReleaseStringUTFChars(env, path, p);
	return ret;

}


/*
 * Class:     com_netifera_platform_internal_system_SystemNative
 * Method:    native_read
 * Signature: (I[BII)I
 */
JNIEXPORT jint JNICALL Java_com_netifera_platform_internal_system_SystemNative_native_1read
  (JNIEnv *env, jobject klass, jint fd, jbyteArray buffer, jint offset, jint length)
{
	jbyte *pbuffer;
	jint ret;

	if(length > ((*env)->GetArrayLength(env, buffer) - offset))
		return -EINVAL;

	if((pbuffer = (*env)->GetByteArrayElements(env, buffer, NULL)) == NULL)
		return -EINVAL;

	while((ret = read(fd, pbuffer + offset, length)) < 0) {
		if(errno != EINTR) {
			ret = -errno;
			break;
		}
	}
	(*env)->ReleaseByteArrayElements(env, buffer, pbuffer, 0);
	return ret;
}

/*
 * Class:     com_netifera_platform_internal_system_SystemNative
 * Method:    native_write
 * Signature: (I[BII)I
 */
JNIEXPORT jint JNICALL Java_com_netifera_platform_internal_system_SystemNative_native_1write
  (JNIEnv *env, jobject klass, jint fd, jbyteArray buffer, jint offset, jint length)
{
	jbyte *pbuffer;
	jint ret;

	if(length > ((*env)->GetArrayLength(env, buffer) - offset))
		return -EINVAL;

	if((pbuffer = (*env)->GetByteArrayElements(env, buffer, NULL)) == NULL)
		return -EINVAL;

	if((ret = write(fd, pbuffer + offset, length)) < 0) {
		ret = -errno;
	}

	(*env)->ReleaseByteArrayElements(env, buffer, pbuffer, 0);
	return ret;
}

/*
 * Class:     com_netifera_platform_internal_system_SystemNative
 * Method:    native_sendmsg
 * Signature: (I[B[B)I
 */
JNIEXPORT jint JNICALL Java_com_netifera_platform_internal_system_SystemNative_native_1sendmsg
  (JNIEnv *env, jobject klass, jint fd, jbyteArray message, jbyteArray address)
{
	struct iovec iov;
	struct msghdr msg;
	int ret;
	jbyte *pmessage, *paddress;

	if( (pmessage = (*env)->GetByteArrayElements(env, message, NULL)) == NULL)
		return -EINVAL;

	if( (paddress = (*env)->GetByteArrayElements(env, address, NULL)) == NULL) {
		(*env)->ReleaseByteArrayElements(env, message, pmessage, 0);
		return -EINVAL;
	}

	memset(&msg, 0, sizeof(struct msghdr));
	iov.iov_base = pmessage;
	iov.iov_len = (*env)->GetArrayLength(env, message);
	msg.msg_name = paddress;
	msg.msg_namelen = (*env)->GetArrayLength(env, address);
	msg.msg_iov = &iov;
	msg.msg_iovlen = 1;

	if((ret = sendmsg(fd, &msg, 0)) < 0)
		ret = -errno;

	(*env)->ReleaseByteArrayElements(env, message, pmessage, 0);
	(*env)->ReleaseByteArrayElements(env, address, paddress, 0);

	return ret;
}

/*
 * Class:     com_netifera_platform_internal_system_SystemNative
 * Method:    native_recvmsg
 * Signature: (I[B[B)I
 */
JNIEXPORT jint JNICALL Java_com_netifera_platform_internal_system_SystemNative_native_1recvmsg
  (JNIEnv *env, jobject klass, jint fd, jbyteArray message, jbyteArray address)
{
	struct iovec iov;
	struct msghdr msg;
	int ret;
	jbyte *pbuffer, *paddress;

	if((pbuffer = (*env)->GetByteArrayElements(env, message, 0)) == NULL)
		return -EINVAL;

	if((paddress = (*env)->GetByteArrayElements(env, address, 0)) == NULL) {
		(*env)->ReleaseByteArrayElements(env, message, pbuffer, 0);
		return -EINVAL;
	}

	size_t mlen = (*env)->GetArrayLength(env, message);
	memset(pbuffer, 0, mlen);
	memset(&msg, 0, sizeof(struct msghdr));
	iov.iov_base = pbuffer;
	iov.iov_len = mlen;
	msg.msg_name = paddress;
	msg.msg_namelen = (*env)->GetArrayLength(env, address);
	msg.msg_iov = &iov;
	msg.msg_iovlen = 1;

	while((ret = recvmsg(fd, &msg, 0)) < 0) {
		if(errno != EINTR) {
			ret = -errno;
			break;
		}
	}

	(*env)->ReleaseByteArrayElements(env, message, pbuffer, 0);
	(*env)->ReleaseByteArrayElements(env, address, paddress, 0);
	return ret;
}
