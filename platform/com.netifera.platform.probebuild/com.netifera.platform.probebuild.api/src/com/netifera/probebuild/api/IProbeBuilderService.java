
package com.netifera.probebuild.api;

import java.io.IOException;


/**
 * Probe Builder Service Interface.
 * @see IProbeConfiguration
 * @see IProbeDeployable
 */
public interface IProbeBuilderService
{
	/**
	 * Create a new Probe Configuration or load an existing one.
	 * @param name Probe's name.
	 * @return The Probe Configuration object.
	 * @exception IOException if the Probe Configuration cannot
	 * be loaded or created.
	 * @see IProbeBuilderService#listProbeConfigurations()
	 * @see IProbeConfiguration
	 */
	public IProbeConfiguration getProbeConfiguration(String name) throws IOException;


	/**
	 * Get the list of existing Probe Configurations.
	 * @return An array of strings containing the names of
	 * all existing configurations.
	 */
	public String[] listProbeConfigurations();


	/**
	 * Delete a Probe Configuration.
	 * @param name Probe's name.
	 */
	public void deleteProbeConfiguration(String name);


	/**
	 * Get a registered Deployable Probe.
	 * @param name Name of the Deployable Probe;
	 * @return The Deployable Probe object.
	 * @see listDeployableProbes()
	 * @see IProbeDeployable
	 */
	public IProbeDeployable getProbeDeployable(String name);


	/**
	 * Get the list of all registered Deployable Probes.
	 * @return An array of strings containing the names
	 * of all registered Deployable Probes.
	 */
	public String[] listDeployableProbes();

}


