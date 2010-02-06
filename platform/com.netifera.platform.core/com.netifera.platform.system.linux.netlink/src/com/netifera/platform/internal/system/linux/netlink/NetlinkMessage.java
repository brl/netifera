package com.netifera.platform.internal.system.linux.netlink;

import com.netifera.platform.api.system.ISystemService;

public class NetlinkMessage {
	//	struct nlmsghdr
	//	{
	//		__u32		nlmsg_len;	/* Length of message including header */
	//		__u16		nlmsg_type;	/* Message content */
	//		__u16		nlmsg_flags;	/* Additional flags */
	//		__u32		nlmsg_seq;	/* Sequence number */
	//		__u32		nlmsg_pid;	/* Sending process port ID */
	//	};
	//
	//	  0                   1                   2                   3
	//	  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	//	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	//	  |                          Length                             |
	//	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	//	  |            Type              |           Flags              |
	//	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	//	  |                      Sequence Number                        |
	//	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	//	  |                      Process ID (PID)                       |
	//	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

	private final static int NLMSG_HDRLEN = 16;
	private final static int NLMSG_ALIGNTO = 4;
	private int type;
	private int flags;
	private int sequence;
	private int pid;
	private byte[] payload;
	private final ISystemService system;

	NetlinkMessage(ISystemService system) {
		this.system = system;
	}
	
	int alignedLength(int length) {
		return (length + (NLMSG_ALIGNTO - 1)) & ~(NLMSG_ALIGNTO - 1);
	}
	
	int messageSize() {
		return payload.length + NLMSG_HDRLEN;
	}
	
	int totalSize() {
		return alignedLength(payload.length) + NLMSG_HDRLEN;
	}
	byte[] packMessage() {
		final byte[] messageBuffer = new byte[totalSize()];
		system.pack32(messageBuffer, 0, messageSize());
		system.pack16(messageBuffer, 4, type);
		system.pack16(messageBuffer, 6, flags);
		system.pack32(messageBuffer, 8, sequence);
		system.pack32(messageBuffer, 12, pid);
		System.arraycopy(payload, 0, messageBuffer, NLMSG_HDRLEN, payload.length);
		return messageBuffer;
	}
	
	
}
