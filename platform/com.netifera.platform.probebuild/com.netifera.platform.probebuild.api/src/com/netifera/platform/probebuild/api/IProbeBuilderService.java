
package com.netifera.platform.probebuild.api;

import java.util.Collection;

import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * Probe Builder Service Interface.
 * @see IProbeConfiguration
 * @see IProbeDeployable
 */
public interface IProbeBuilderService
{
	/**
	 * Load an existing Probe Configuration.
	 * @param name Name of the configuration to load.
	 * @return The Probe Configuration object.
	 * @exception FileNotfoundException If the Probe Configuration does not exist.
	 * @exception IOException If the Probe Configuration cannot be loaded.
	 * @see #getNewProbeConfiguration(String)
	 * @see #listProbeConfigurations()
	 * @see IProbeConfiguration
	 */
	public IProbeConfiguration getProbeConfiguration(String name) throws FileNotFoundException, IOException;


	/**
	 * Create a new Probe Configuration.
	 * <p>If the specified configuration already exists it is
	 * truncated and a new empty one is created upon it.</p> 
	 * @param name Name assigned to the new configuration.
	 * @return The Probe Configuration object.
	 * @exception IOException If the Probe Configuration cannot be created. 
	 * @see #getProbeConfiguration(String)
	 * @see #listProbeConfigurations()
	 * @see IProbeConfiguration
	 */
	public IProbeConfiguration getNewProbeConfiguration(String name) throws IOException;


	/**
	 * Get the list of Probe Configurations.
	 * @return A collection of strings containing the
	 * names of all existing configurations.
	 */
	public Collection<String> listProbeConfigurations();


	/**
	 * Delete a Probe Configuration.
	 * <p>Embedded configurations cannot be deleted.</p>
	 * @param name Name of the configuration to delete.
	 */
	public void deleteProbeConfiguration(String name);


	/**
	 * Get the list of supported platforms.
	 * @return A collection of strings containing all supported platforms.
	 */
	public Collection<String> listSupportedPlatforms();


	/**
	 * Get the list of supported formats.
	 * @param platform The platform.
	 * @return A collection of strings containing all supported formats for the specified platform.
	 * @see #listSupportedPlatforms()
	 */
	public Collection<String> listSupportedFormats(String platform);


	/**
	 * Get a Deployable Probe.
	 * @param config Configuration object that should be used to build the probe;
	 * @param platform Target platform where the deployable probe is intended to run;
	 * @param format Binary format for the deployable probe.
	 * @return The Deployable Probe object.
	 * @exception FileNotFoundException If the specified pair platform/format are not supported.
	 * @exception IOException If the probe cannot be deployed.
	 * @see #listSupportedFormats()
	 * @see #listSupportedPlatforms()
	 * @see IProbeConfiguration
	 * @see IProbeDeployable
	 */
	public IProbeDeployable getProbeDeployable(IProbeConfiguration config, String platform, String format) throws IOException;


	/**
	 * Get a Deployable Probe.
	 * @param name Name of the configuration that should be used to build the probe;
	 * @param platform Target platform where the deployable probe is intended to run;
	 * @param format Binary format for the deployable probe.
	 * @return The Deployable Probe object.
	 * @exception FileNotFoundException If the specified configuration does not exist or
	 * the specified pair platform/format are not supported.
	 * @exception IOException If the probe cannot be deployed.
	 * @see #getProbeDeployable(IProbeConfiguration, String, String)
	 * @see #listSupportedFormats()
	 * @see #listSupportedPlatforms()
	 * @see IProbeDeployable
	 */
	public IProbeDeployable getProbeDeployable(String name, String platform, String format) throws FileNotFoundException, IOException;

}

