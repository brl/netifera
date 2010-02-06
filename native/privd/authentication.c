#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <pwd.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>

#include "privd.h"

static char *get_config_path(struct privd_instance *);
static FILE *open_config(struct privd_instance *);
static int check_config_permissions(struct privd_instance *, int);

void
read_authentication_data(struct privd_instance *privd)
{
	DEBUG("READING AUTH DATA")
	FILE *fp = open_config(privd);
	if(fp == NULL)
		return;

	char line[128];
	if(fgets(line, sizeof(line), fp) == NULL) {
		send_startup(privd, PRIVD_STARTUP_CONFIG_BAD_DATA, "Error reading daemon configuration");
		return;
	}
	char *p = rindex(line, '\n');
	if(p == NULL) {
		send_startup(privd, PRIVD_STARTUP_CONFIG_BAD_DATA, "Badly formed config file. No newline found");
		return;
	}

	*p = 0;
	if(strncmp(line, "AUTH_HASH=", 10)) {
		send_startup(privd, PRIVD_STARTUP_CONFIG_BAD_DATA, "Badly formed config file.  No authentication information found.");
		return;
	}
	p = line + 10;
	if(strlen(p) == 0) {
		send_startup(privd, PRIVD_STARTUP_CONFIG_BAD_DATA, "Badly formed config file.  Authentication information empty.");
		return;
	}

	if(strcmp(p, "DISABLED") == 0) {
		privd->auth_disabled = 1;
		send_startup(privd, PRIVD_STARTUP_OK, NULL);
		return;
	}
	privd->auth_hash = strdup(p);
	send_startup(privd, PRIVD_STARTUP_AUTHENTICATION_REQUIRED, NULL);

}

static char *
get_config_path(struct privd_instance *privd)
{
	uid_t uid = getuid();
	struct passwd *pw = getpwuid(uid);
	if(pw == NULL || pw->pw_dir == NULL) {
		send_startup(privd, PRIVD_STARTUP_CONFIG_NOT_FOUND, "Failed to look up home directory in password file");
		return NULL;
	}

	static char buffer[PATH_MAX + 1];

	if(snprintf(buffer, sizeof(buffer), "%s/.netifera/privd_conf", pw->pw_dir) > sizeof(buffer)) {
		send_startup(privd, PRIVD_STARTUP_CONFIG_NOT_FOUND, "Overflow creating configuration path");
		return NULL;
	}
	return buffer;
}

static FILE *
open_config(struct privd_instance *privd)
{
	char *path = get_config_path(privd);
	if(path == NULL)
		return NULL;
	int fd = open(path, O_RDONLY|O_NOFOLLOW);
	if(fd == -1) {
		send_startup(privd, PRIVD_STARTUP_CONFIG_NOT_FOUND, "Error opening config file %s", path);
		return NULL;
	}

	if(!check_config_permissions(privd, fd)) {
		close(fd);
		return NULL;
	}
	FILE *fp = fdopen(fd, "r");
	if(fp == NULL) {
		send_startup(privd, PRIVD_STARTUP_CONFIG_NOT_FOUND, "Failed to convert descriptor to FILE with fdopen()");
		return NULL;
	}
	return fp;

}

static int
check_config_permissions(struct privd_instance *privd, int fd)
{
	struct stat st;
	if(fstat(fd, &st) < 0) {
		// XXX
		return 0;
	}
	if(st.st_uid != 0) {
		// XXX
		return 0;
	}
	mode_t mask = S_IWGRP | S_IRGRP | S_IWOTH | S_IROTH;
	if(st.st_mode & mask) {
		// XXX
		return 0;
	}

	if(st.st_nlink != 1) {
		// XXX
		return 0;
	}
	return 1;
}
