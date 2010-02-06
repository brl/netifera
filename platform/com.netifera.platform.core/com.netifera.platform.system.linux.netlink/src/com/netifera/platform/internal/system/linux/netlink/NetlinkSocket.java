package com.netifera.platform.internal.system.linux.netlink;

import com.netifera.platform.api.log.ILogger;
import com.netifera.platform.api.system.ISystemService;

public class NetlinkSocket {
	private final static int SOCKADDR_NL_SIZE = 12;
	private final ISystemService system;
	private final ILogger logger;
	private int socket = -1;
	private int socketPid;
	private int multicastMask;
	private int seqNext;
	private int seqExpect;
	private byte[] localAddress;
	
	NetlinkSocket(ISystemService system, ILogger logger) {
		this.system = system;
		this.logger = logger;
	}
	
	public boolean connect(int protocol) {
		socket = system.syscall_socket(Constants.AF_NETLINK, Constants.SOCK_RAW, protocol);
		if(socket < 0) {
			logger.error("Error connecting netlink socket : " + system.getErrorMessage(system.getErrno()));
			return false;
		}
		
		
		return true;
		
	}
	
	private void initialize() {
		socketPid = generateLocalPort();
		seqNext = seqExpect = generateInitialSequenceNumber();
		multicastMask = 0;
		packLocalAddress();
	}
	
	private void packLocalAddress() {
		localAddress = packAddress(socketPid, multicastMask);
	}
	
	private byte[] packAddress(int pid, int multicast) {
		final byte[] addressBuffer = new byte[SOCKADDR_NL_SIZE];
		system.pack16(addressBuffer, 0, Constants.AF_NETLINK);
		system.pack32(addressBuffer, 4, pid);
		system.pack32(addressBuffer, 8, multicast);
		return addressBuffer;	                                  
	}
	
	private int generateLocalPort() {
		return (int) (Thread.currentThread().getId() & 0xFFFFFFFF);
	}
	
	private int generateInitialSequenceNumber() {
		return (int) (System.currentTimeMillis() & 0xFFFFFFFF);
	}

}
