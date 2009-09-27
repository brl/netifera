
package com.netifera.probebuild.api;

import java.io.InputStream;

import java.io.IOException;


/**
 * Deployable Probe Interface.
 * @see IProbeConfiguration
 */
public interface IProbeDeployable
{
	/**
	 * Get the Deployable Probe's input stream.
	 * @param config Configuration object that should be used to
	 * build the stream.
	 * @return The input stream.
	 * @exception IOException
	 * @see IProbeConfiguration
	 */
	public InputStream getInputStream(IProbeConfiguration config) throws IOException;


	/**
	 * Get the name of this Deployable Probe.
	 */
	public String getName();

}


