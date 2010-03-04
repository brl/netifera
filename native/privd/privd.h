#ifndef _PRIVD_H_
#define _PRIVD_H_

#include <stdint.h>
#include <sys/types.h>

#define PRIVD_FD 5
#define MONITOR_FD 6
#define PRIVD_PROTOCOL_VERSION 0
#define PRIVD_MAX_MSG_SIZE  0xFFFF
#define PRIVD_ERROR_BUFFER_SIZE 512
enum {
	PRIVD_PING,
	PRIVD_AUTHENTICATE,
	PRIVD_OPEN_SOCKET,
	PRIVD_OPEN_BPF,
	PRIVD_MESSAGE_MAX
};

enum {
	PRIVD_RESPONSE_OK,
	PRIVD_RESPONSE_ERROR,
	PRIVD_RESPONSE_STARTUP,
	PRIVD_RESPONSE_FD,
	PRIVD_RESPONSE_AUTH_FAILED
};

enum {
	PRIVD_STARTUP_OK,
	PRIVD_STARTUP_AUTHENTICATION_REQUIRED,
	PRIVD_STARTUP_NOT_ROOT,
	PRIVD_STARTUP_INITIALIZATION_FAILED,
	PRIVD_STARTUP_CONFIG_NOT_FOUND,
	PRIVD_STARTUP_CONFIG_BAD_PERMS,
	PRIVD_STARTUP_CONFIG_BAD_DATA
};

enum {
	PRIVD_ARG_INTEGER,
	PRIVD_ARG_STRING
};

struct privd_arghdr {
	uint8_t type;
	uint8_t pad;
	uint16_t length;
	uint8_t data[];
};

struct privd_msghdr {
	uint8_t version;
	uint8_t type;
	uint16_t length;
};

#define PRIVD_HEADER_SIZE sizeof(struct privd_msghdr)


#define DEBUG(...)  if(privd->debug_flag) { fprintf(stderr,  __VA_ARGS__); fprintf(stderr, "\n"); }

struct privd_instance {
	int socket_fd;
	int monitor_fd;
	int authenticated;
	int debug_flag;
	int fd_to_send;
	uint8_t message_buffer[PRIVD_MAX_MSG_SIZE];
	uint8_t *message_ptr;
	size_t message_space;
	struct message_handler *message_handlers;
	int auth_disabled;
	char *auth_hash;
};
struct message_handler {
	int (*handler)(struct privd_instance *privd);
};

/* bcrypt.c */
char * bcrypt_gensalt(u_int8_t log_rounds);
char * bcrypt(const char *key, const char *salt);

/* config.c */
void generate_config(const char *username);
/* privd.c */
void shutdown_daemon(struct privd_instance *privd);
void abort_daemon(struct privd_instance *privd, const char *fmt, ...);

/* initialize.c */
void initialize(struct privd_instance *privd);

/* install.c */
void install_privd(const char *destination_path, const char *source_path);

/* message.c */
void send_ok(struct privd_instance *privd);
void send_fd(struct privd_instance *privd, int fd);
void send_error(struct privd_instance *privd, const char *fmt, ...);
void send_startup(struct privd_instance *privd, int message_type, const char *fmt, ...);
void send_auth_failed(struct privd_instance *privd);
int add_string_argument(struct privd_instance *privd, const char *string);
int add_integer_argument(struct privd_instance *privd, uint32_t value);
int get_integer_argument(struct privd_instance *privd, uint32_t *value);
char *get_string_argument(struct privd_instance *privd);
void initialize_message(struct privd_instance *privd, int message_type);
int finalize_message(struct privd_instance *privd);

/* dispatch.c */

void dispatch_message(struct privd_instance *privd);

/* socket.c */
void recv_message(struct privd_instance *privd);
void send_message(struct privd_instance *privd);

/* authentication.c */
int authenticate(struct privd_instance *privd, const char *password);
void read_authentication_data(struct privd_instance *privd);


#endif
