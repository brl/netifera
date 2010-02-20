#include <stdlib.h>
#include <unistd.h>
#include <signal.h>
#include <sys/socket.h>
#include <sys/errno.h>
#include <string.h>
#include <errno.h>
#include <jni.h>

#define PRIVD_FD  5

static int is_running = 0;
static char *error_message;
static char error_buffer[512];
static int privd_socket = -1;
static int received_fd = -1;

static int launch_privd(const char *);
static int send_message(void *, size_t);
static int recv_message(void *, size_t);
static void close_daemon_connection();
/*
 * Class:     com_netifera_platform_internal_system_privd_PrivilegeDaemonNative
 * Method:    isDaemonRunning
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_netifera_platform_internal_system_privd_PrivilegeDaemonNative_isDaemonRunning
  (JNIEnv *env, jobject obj)
{
	return is_running;
}



/*
 * Class:     com_netifera_platform_internal_system_privd_PrivilegeDaemonNative
 * Method:    startDaemon
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_netifera_platform_internal_system_privd_PrivilegeDaemonNative_startDaemon
  (JNIEnv *env, jobject obj, jstring path)
{
	if(is_running)
		return 0;

	const char *p = (*env)->GetStringUTFChars(env, path, NULL);
	if(p == NULL) {
		error_message = "Failed to convert java string";
		return -1;
	}

	return launch_privd(p);

}
/*
 * Class:     com_netifera_platform_internal_system_privd_PrivilegeDaemonNative
 * Method:    getLastErrorMessage
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_netifera_platform_internal_system_privd_PrivilegeDaemonNative_getLastErrorMessage
  (JNIEnv *env, jobject obj)
{
	if(error_message) {
		return (*env)->NewStringUTF(env, error_message);
	} else {
		return (*env)->NewStringUTF(env, "No error.");
	}
}

/*
 * Class:     com_netifera_platform_internal_system_privd_PrivilegeDaemonNative
 * Method:    sendMessage
 * Signature: ([B)I
 */
JNIEXPORT jint JNICALL Java_com_netifera_platform_internal_system_privd_PrivilegeDaemonNative_sendMessage
  (JNIEnv *env, jobject obj, jbyteArray sendBuffer)
{
	jbyte *send_buffer = (*env)->GetByteArrayElements(env, sendBuffer, NULL);
	if(send_buffer == NULL) {
		error_message = "Failed to extract sendBuffer";
		return -1;
	}
	int send_size = (*env)->GetArrayLength(env, sendBuffer);
	int ret = send_message(send_buffer, send_size);
	(*env)->ReleaseByteArrayElements(env, sendBuffer, send_buffer, 0);
	return ret;
}

/*
 * Class:     com_netifera_platform_internal_system_privd_PrivilegeDaemonNative
 * Method:    receiveMessage
 * Signature: ([B)I
 */
JNIEXPORT jint JNICALL Java_com_netifera_platform_internal_system_privd_PrivilegeDaemonNative_receiveMessage
  (JNIEnv *env, jobject obj, jbyteArray recvBuffer)
{
	jbyte *recv_buffer = (*env)->GetByteArrayElements(env, recvBuffer, NULL);
	if(recv_buffer == NULL) {
		error_message = "Failed to extract recvBuffer";
		return -1;
	}
	int recv_size = (*env)->GetArrayLength(env, recvBuffer);
	int ret = recv_message(recv_buffer, recv_size);
	(*env)->ReleaseByteArrayElements(env, recvBuffer, recv_buffer, 0);
	return ret;

}

/*
 * Class:     com_netifera_platform_internal_system_privd_PrivilegeDaemonNative
 * Method:    getReceivedFileDescriptor
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_netifera_platform_internal_system_privd_PrivilegeDaemonNative_getReceivedFileDescriptor
  (JNIEnv *env, jobject obj)
{
	return received_fd;
}

/*
 * Class:     com_netifera_platform_internal_system_privd_PrivilegeDaemonNative
 * Method:    exitDaemon
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_netifera_platform_internal_system_privd_PrivilegeDaemonNative_exitDaemon
  (JNIEnv *env, jobject obj)
{

	if(is_running)
		close_daemon_connection();

}

static int
launch_privd(const char *path)
{
	int sv[2];
	pid_t pid;
	char * const args[2] = {(char *)path, NULL};
	if(access(path, X_OK)) {
		snprintf(error_buffer, sizeof(error_buffer), "Privilege daemon is not executable (%s)", strerror(errno));
		error_message = error_buffer;
		return -1;
	}

	if(socketpair(AF_UNIX, SOCK_DGRAM, 0, sv) < 0) {
		snprintf(error_buffer, sizeof(error_buffer), "Failed to create socket pair (%s)", strerror(errno));
		error_message = error_buffer;
		return -1;
	}

	signal(SIGCHLD, SIG_IGN);

	if((pid = fork()) < 0) {
		snprintf(error_buffer, sizeof(error_buffer), "Failed to fork() (%s)", strerror(errno));
		error_message = error_buffer;
		close(sv[0]);
		close(sv[1]);
		return -1;
	}

	if(pid == 0) {
		close(sv[0]);
		if(dup2(sv[1], PRIVD_FD) == -1) {
			exit(EXIT_FAILURE);
		}
		close(sv[1]);
		execv(path, args);
		exit(EXIT_FAILURE);
	}

	close(sv[1]);
	privd_socket = sv[0];
	is_running = 1;
	return 0;
}

static int
send_message(void *data, size_t size)
{
	struct msghdr msg;
	struct iovec iov;

	memset(&msg, 0, sizeof(msg));
	iov.iov_base = data;
	iov.iov_len = size;
	msg.msg_iov = &iov;
	msg.msg_iovlen = 1;

	if(sendmsg(privd_socket, &msg, 0) != size) {
		close_daemon_connection();
		snprintf(error_buffer, sizeof(error_buffer), "sendmsg() failed (%s)", strerror(errno));
		error_message = error_buffer;
		return -1;
	}
	return 0;
}

static int
recv_message(void *buffer, size_t size)
{
	struct msghdr msg;
	struct iovec iov;
	char tmp[CMSG_SPACE(sizeof(int))];
	int n;

	memset(&msg, 0, sizeof(msg));
	iov.iov_base = buffer;
	iov.iov_len = size;
	msg.msg_iov = &iov;
	msg.msg_iovlen = 1;
	msg.msg_control = tmp;
	msg.msg_controllen = sizeof(tmp);
	if((n = recvmsg(privd_socket, &msg, 0)) < 0) {
		close_daemon_connection();
		snprintf(error_buffer, sizeof(error_buffer), "recvmsg() failed (%s)", strerror(errno));
		error_message = error_buffer;
		return -1;
	}

	struct cmsghdr *cmsg = CMSG_FIRSTHDR(&msg);

	if(cmsg && cmsg->cmsg_type == SCM_RIGHTS) {
		received_fd = (*(int *)CMSG_DATA(cmsg));
		fprintf(stderr, "JNI: received file descriptor %d\n", received_fd);
	} else {
		fprintf(stderr, "JNI: no file descriptor received\n");
		received_fd = -1;
	}

	return n;

}

static void
close_daemon_connection()
{
	close(privd_socket);
	privd_socket = -1;
	is_running = 0;
}
