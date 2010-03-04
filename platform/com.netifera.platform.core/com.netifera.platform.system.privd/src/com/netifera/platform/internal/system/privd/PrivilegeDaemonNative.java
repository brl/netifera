package com.netifera.platform.internal.system.privd;

public class PrivilegeDaemonNative {
	/**
	 * Return true if the privilege daemon is already running.
	 * 
	 * @return <tt>true</tt> if the daemon is running. 
	 */
	public native boolean isDaemonRunning();
	
	/**
	 * 
	 * @param path
	 * @return 0 if the daemon is already running or if it was successfully started.  -1 if an error prevented
	 * starting the daemon.
	 */
	public native int startDaemon(String path);
	
	/**
	 * 
	 * @return
	 */
	public native String getLastErrorMessage();
		
	/**
	 * 
	 * @param sendBuffer
	 * @return
	 */
	public native int sendMessage(byte[] sendBuffer);
	
	/**
	 * 
	 * @param recvBuffer
	 * @return The size of the received message in bytes or -1 if an error occurred.
	 */
	public native int receiveMessage(byte[] recvBuffer);
	
	/**
	 * 
	 * @return The file descriptor passed in the last received message or -1 if no
	 * file descriptor was received in the last message.
	 */
	public native int getReceivedFileDescriptor();
	
	/**
	 * 
	 */
	public native void exitDaemon();
	
	public native void enableDebug(boolean debugFlag);
	
	static {
		try {
			System.loadLibrary("privd");
		} catch(UnsatisfiedLinkError e) {
			System.err.println("Failed to load native library, java.library.path="
					+ System.getProperty("java.library.path"));
			e.printStackTrace();
		}
	}

}
