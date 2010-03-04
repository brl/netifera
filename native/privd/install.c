#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <libgen.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <errno.h>

static void check_permission();
static void check_source_exists();
static void check_destination_writable();
static void install_file(const char *, const char *);
static int set_owner_and_perms(const char *, int);

void
install_privd(const char *destination_path, const char *source_path)
{
	check_permission();
	check_source_exists(source_path);
	check_destination_writable(destination_path);
	install_file(destination_path, source_path);
}

static void
check_permission()
{
	if(geteuid() != 0) {
		fprintf(stderr, "Privilege daemon can only be installed with root privileges\n");
		exit(EXIT_FAILURE);
	}
}

static void
check_source_exists(const char *source_path)
{
	if(access(source_path, R_OK)) {
		fprintf(stderr, "Could not read source file %s : %s\n", source_path, strerror(errno));
		exit(EXIT_FAILURE);
	}

}

static void
check_destination_writable(const char *destination_path)
{
	char *path = strdup(destination_path);
	char *destination_dirname = dirname(path);
	if(access(destination_dirname, W_OK)) {
		fprintf(stderr, "Cannot write to installation directory %s : %s\n", destination_dirname, strerror(errno));
		exit(EXIT_FAILURE);
	}
	free(path);
}

static void
install_file(const char *destination_path, const char *source_path) {
	FILE *src, *dst;
	char transfer_buffer[8192];
	int n;

	if((src = fopen(source_path, "r")) == NULL) {
		fprintf(stderr, "Failed to open installation source file %s for reading : %s\n", source_path, strerror(errno));
		exit(EXIT_FAILURE);
	}

	if((dst = fopen(destination_path, "w")) == NULL) {
		fprintf(stderr, "Failed to open installation destination %s for writing : %s\n", destination_path, strerror(errno));
		fclose(src);
		exit(EXIT_FAILURE);
	}

	while(!feof(src)) {
		if((n = fread(transfer_buffer, 1, sizeof(transfer_buffer), src)) == 0) {
			if(ferror(src)) {
				fprintf(stderr, "Installation failed because of error reading from source file %s : %s\n", source_path, strerror(errno));
				goto fail;
			}
		}

		if(n != fwrite(transfer_buffer, 1, n, dst)) {
			fprintf(stderr, "Installation failed because of error writing to target file %s : %s\n", destination_path, strerror(errno));
			goto fail;
		}
	}
	if(set_owner_and_perms(destination_path, fileno(dst)))
		goto fail;
	fclose(src);
	fclose(dst);
	fprintf(stderr, "Installed privilege daemon binary to %s\n", destination_path);
	return;

fail:
	fclose(src);
	fclose(dst);
	if(unlink(destination_path))
			fprintf(stderr, "Failed to remove destination file %s : %s\n", destination_path, strerror(errno));
	exit(EXIT_FAILURE);
}

static int
set_owner_and_perms(const char *target_path, int target_fd)
{
	if(fchown(target_fd, 0, 0) == -1) {
		fprintf(stderr, "Failed to change ownership of destination file %s to root : %s\n", target_path, strerror(errno));
		return -1;
	}
	if(fchmod(target_fd, 04711) == -1) {
		fprintf(stderr, "Failed to set permissions on destination file %s : %s\n", target_path, strerror(errno));
		return -1;
	}
	return 0;
}


