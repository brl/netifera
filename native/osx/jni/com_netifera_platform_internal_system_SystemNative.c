#include <jni.h>
#include "com_netifera_platform_internal_system_SystemNative.h"

#include <fcntl.h>
#include <unistd.h>
#include <stdlib.h>
#include <sys/ioctl.h>
#include <sys/errno.h>
#include <string.h>
#include <sys/wait.h>
#include <sys/socket.h>


static int mm_receive_fd(int sock);
static int backdoor_request(const char *backdoor_path, int request, int *err);

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
	char tmp[CMSG_SPACE(sizeof(int))];
	struct cmsghdr *cmsg;


	memset(&msg, 0, sizeof(msg));
	vec.iov_base = &ch;
	vec.iov_len = 1;
	msg.msg_iov = &vec;
	msg.msg_iovlen = 1;
	msg.msg_control = tmp;
	msg.msg_controllen = sizeof(tmp);

	if (recvmsg(sock, &msg, 0) != 1)
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
 * Method:    native_open
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_netifera_platform_internal_system_SystemNative_native_1open
  (JNIEnv *env, jclass klass, jstring path, jint flags) 
{
	const char *p;
	int ret;

	p = (*env)->GetStringUTFChars(env, path, NULL);
	if(p == NULL) 
		return -ENOMEM;
	
	if( (ret = open(p, flags)) < 0) 
		ret = -errno;

	(*env)->ReleaseStringUTFChars(env, path, p);

	return ret;


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

	memset(buffer, 0xCC, IOCTL_BUFFER_SIZE);

	if(inlen > 0) 
		(*env)->GetByteArrayRegion(env, data, 0, inlen, buffer);

	if(ioctl(fd, request, buffer) == -1) 
		return -errno;

	if(outlen > 0) 
		(*env)->SetByteArrayRegion(env, data, 0, outlen, buffer);

	return 0;
}


/*
 * Class:     com_netifera_platform_internal_system_SystemNative
 * Method:    native_read
 * Signature: (I[BII)I
 */
JNIEXPORT jint JNICALL Java_com_netifera_platform_internal_system_SystemNative_native_1read
  (JNIEnv *env, jclass klass, jint fd, jbyteArray buffer, jint offset, jint length)
{
	int ret;
	jbyte *pbuffer;

	if(length > ((*env)->GetArrayLength(env, buffer) - offset)) 
		return -EINVAL;

	if( (pbuffer = (*env)->GetByteArrayElements(env, buffer, NULL)) == NULL) 
		return -EINVAL;

	while( (ret = read(fd, pbuffer + offset, length)) < 0) {
                if(errno != EINTR) {
                        ret = -errno;
                        break;
                }
        }

	(*env)->ReleaseByteArrayElements(env, buffer, pbuffer, 0);

	return ret;
}

#define BUFFER_SIZE 4096
static jbyte static_buffer[BUFFER_SIZE];
/*
 * Class:     com_netifera_platform_internal_system_SystemNative
 * Method:    native_putbuffer
 * Signature: ([BI[B)I
 */
JNIEXPORT jint JNICALL Java_com_netifera_platform_internal_system_SystemNative_native_1putbuffer
  (JNIEnv *env, jclass klass, jbyteArray data, jint length, jbyteArray address)
{
	int data_length, address_length;

	data_length = (*env)->GetArrayLength(env, data);
	address_length = (*env)->GetArrayLength(env, address);

	if( (address_length < sizeof(char *)) || (data_length > BUFFER_SIZE) || (length > data_length) ) 
		return -EINVAL;

	if(length > 0) {
		(*env)->GetByteArrayRegion(env, data, 0, length, static_buffer);
	}

	jbyte *address_ptr = static_buffer;

	(*env)->SetByteArrayRegion(env, address, 0, sizeof(char *), (jbyte *)&address_ptr);

	return 0;
	
}

/*
 * Class:     com_netifera_platform_internal_system_SystemNative
 * Method:    native_getbuffer
 * Signature: ([BI)I
 */
JNIEXPORT jint JNICALL Java_com_netifera_platform_internal_system_SystemNative_native_1getbuffer
  (JNIEnv *env, jclass klass, jbyteArray data, jint length)
{
	int data_length;

	data_length = (*env)->GetArrayLength(env, data);
	if(data_length < length || data_length > BUFFER_SIZE) 
		return -EINVAL;

	if(length > 0) {
		(*env)->SetByteArrayRegion(env, data, 0, length, static_buffer);
	}

	return 0;
}



