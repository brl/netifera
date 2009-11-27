
package com.netifera.platform.probebuild.api;

import java.io.InputStream;

import java.io.IOException;


/**
 * Deployable Probe Interface.
 */
public interface IProbeDeployable
{
	/**
	 * Get the name of this deployable probe.
	 */
	public String getName();


	/**
	 * Get the input stream for this deployable probe.
	 * @return The input stream.
	 * @exception IOException If the stream cannot be created.
	 * @see #getSize()
	 */
	public InputStream getInputStream() throws IOException;


	/**
	 * Get the size of this deployable probe.
	 * @return The size of this deployable probe in bytes;
	 * @exception IOException If something unexpected occurs.
	 * @see #getInputStream()
	 */
	public int getSize();

}

