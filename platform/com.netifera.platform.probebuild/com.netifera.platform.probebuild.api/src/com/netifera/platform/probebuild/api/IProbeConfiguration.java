
package com.netifera.platform.probebuild.api;

import java.net.URL;
import java.util.Collection;
import java.util.Hashtable;

import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * Probe Configuration Interface.
 * <p>The Probe Configuration interface provides an API allowing users to
 * define the components to embed into ad-hoc Deployable Probes.</p>
 * <p>Components are:
 * <li>OSGi Bundles;</li>
 * <li>Resources (data files);</li>
 * <li>Probe Configurations;</li>
 * <li>Properties (automatically set when a Deployable Probe is launched).</li>
 * </p>
 * @see IProbeDeployable
 */
public interface IProbeConfiguration
{
	/**
	 * Get the name of this configuration.
	 */
	public String getName();


	/**
	 * Add a new OSGi bundle.
	 * <p><i>Notice that a reference is added, the actual
	 * bundle should exist when a probe is being deployed.</i></p>
	 * @param name Name of the stored jar file containing the bundle.
	 * @param initLevel Installation level (if <code>0</code>
	 * the bundle should not be installed during the probe boot sequence);
	 * @param startLevel Starting level (if <code>0</code>
	 * the bundle should not be started during the probe boot sequence).
	 * @see #listBundles()
	 */
	public void addBundle(String name, int initLevel, int startLevel);


	/**
	 * Add a new resource into this configuration.
	 * <p><i>Notice that a reference to the resource is added, the actual
	 * resource should exist when a probe is being deployed.</i></p>
	 * @param name Name assigned to the resource (that should be used to
	 * reference the resource from within the deployed probe);
	 * @param path Path to the resource in the local system.
	 * @see #listResources()
	 */
	public void addResource(String name, String path);


	/**
	 * Embed a configuration into this configuration as a resource.
	 * <p>Embedded configurations allow deployed probes to build and deploy
	 * other probes using such configurations.</p>
	 * <p><i>Notice that a reference is added, the actual
	 * configuration should exist when a probe is being deployed.</i></p>
	 * @param name Name of the configuration to embed.
	 */
	public void addConfiguration(String name);


	/**
	 * Embed this configuration into itself as a resource.
	 * <p>The result is that a deployed probe will be able to deploy copies of itself.</p>
	 * @see #addConfiguration(String)
	 */
	public void addConfiguration();


	/**
	 * Add a new property into this configuration.
	 * If the property already exists its value is updated.
	 * @param prop Property name;
	 * @param value Property value. 
	 */
	public void addProperty(String prop, String value);


	/**
	 * Get the list of bundles.
	 * @return A collection of strings containing the names of all bundles.
	 * @see #addBundle(String, String, int, int)
	 * @see #getBundleInstallLevel(String)
	 * @see #getBundlePath(String)
	 * @see #getBundleStartLevel(String)
	 */
	public Collection<String> listBundles();


	/**
	 * Get the install level of a specified bundle.
	 * @param name Name assigned to the bundle.
	 * @return The install level.
	 * @exception FileNotFoundException If the specified bundle is not included
	 * into this Probe Configuration.
	 * @see #addBundle(String, String, int, int)
	 * @see #listBundles()
	 */
	public int getBundleInstallLevel(String name) throws FileNotFoundException;


	/**
	 * Get the start level of a specified bundle.
	 * @param name Name assigned to the bundle.
	 * @return The start level.
	 * @exception FileNotFoundException If the specified bundle is not included
	 * into this Probe Configuration.
	 * @see #addBundle(String, int, int)
	 * @see #listBundles()
	 */
	public int getBundleStartLevel(String name) throws FileNotFoundException;


	/**
	 * Get the list of names assigned to resources.
	 * @return A collection of strings containing the resource names.
	 * @see #addResource(String, String)
	 * @see #getResourcePath(String)
	 */
	public Collection<String> listResources();


	/**
	 * Get the local path to the specified resource.
	 * @param name Name assigned to the resource.
	 * @return Path to the resource in the local system.
	 * @throws FileNotFoundException If the specified resource is not included
	 * into this configuration.
	 * @see #addResource(String, String)
	 * @see #listResources()
	 */
	public String getResourcePath(String name) throws FileNotFoundException;


	/**
	 * Get the list of embedded configurations.
	 * @return A collection of strings containing the names
	 * of all embedded configurations.
	 * @see #addConfiguration(String)
	 * @see #addConfiguration()
	 */
	public Collection<String> listConfigurations();


	/**
	 * Get the table of properties.
	 * @return A table of property name-value pairs.
	 * @see #addProperty(String, String)
	 */
	public Hashtable<String, String> getProperties();


	/**
	 * Resolve bundle dependencies.
	 * XXX
	 * A new Probe Configuration object is generated containing also the new
	 * dependency bundles with correct install and start levels.
	 * The generated configuration cannot be saved.
	 * @return A new Probe Configuration object with all dependencies resolved.
	 * @see addBundle(String, int, int)
	 * @see getBundleList()
	 */
	public void resolveDependencies() throws FileNotFoundException, IOException;


	/**
	 * Save this configuration to disk.
	 * <p>Changes made to this configuration are kept in memory until this method is called.</p>
	 * @throws IOException If something unexpected happens.
	 */
	public void save() throws IOException;


	/**
	 * Get the URL where this configuration is saved.
	 * @return The URL object.
	 */
	public URL getURL();

}

