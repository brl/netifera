
package com.netifera.platform.probebuild.api;

import java.io.IOException;
import java.io.FileNotFoundException;


/**
 * Probe Configuration Interface.
 */
public interface IProbeConfiguration
{
	/**
	 * Get this Configuration's directory (where data is stored).
	 */
	public String getConfigurationDirectory();


	/**
	 * Add a new resource into this Probe Configuration.
	 * Notice that a reference to the resource is added, the actual
	 * resource should exist when a Deployable Probe is about to be built.
	 * @param name Name assigned to the resource (this name should be used when
	 * referencing the resource from within the Deployable Probe);
	 * @param path Path to the resource in the local file system.
	 * @see getResourceList()
	 */
	public void addResource(String name, String path);


	/**
	 * Add a new bundle into this Probe Configuration.
	 * Notice that a reference to the bundle is added, the actual
	 * bundle should exist when a Deployable Probe is about to be built.
	 * @param name Name assigned to the bundle (this name should be used when
	 * referencing the bundle from within the deployable probe);
	 * @param path Path to the bundle in the local file system;
	 * @param initLevel Installation level (if <code>0</code>
	 * the bundle should not be installed);
	 * @param startLevel Starting level (if <code>0</code>
	 * the bundle should not be started).
	 * @see getBundleList()
	 */
	public void addBundle(String name, String path, int initLevel, int startLevel);


	/**
	 * Set the command line options for this Probe Configuration.
	 * The command line options are the same used with a common jvm
	 * with subtle differences.
	 * For example, a normal jvm will accept the following options:
	 * <p>
	 * <code>-Xmx512 -Dport=12345 -jar osgiFramework.jar -some_java_param</code>
	 * </p><p>
	 * On the other hand, a Deployable Probe embeds an osgi framework that
	 * should not be specified in the command line:
	 * </p><p>
	 * <code>-Xmx512 -Dport=12345 -- -some_java_param</code>
	 * </p><p>
	 * Notice that in this case the <code>--</code> string is used as a
	 * separator between the options for the jvm and those intended for the
	 * main class. This separator is not needed when the main class does not
	 * receive arguments.
	 * </p><p>
	 * It is possible to avoid the execution of the embedded osgi framework
	 * specifying a custom jar file (that should be embedded into the
	 * Deployable Probe as a resource), for example:
	 * </p><p>
	 * <code>-Xmx512 -Dport=12345 -jar myCustomApplication.jar -some_java_param</code>
	 * </p>
	 * <p>The above is very useful for testing purposes.</p>
	 * @param line Command line to set.
	 * @see addResource(String, String)
	 * @see getCommandLine()
	 */
	public void setCommandLine(String line);


	/**
	 * Get the list of names assigned to the resources into this Probe Configuration.
	 * @return A string array containing the names assigned to the resources.
	 * @see addResource(String, String)
	 * @see getResourcePath(String)
	 */
	public String[] getResourceList();


	/**
	 * Get the local path to the specified resource.
	 * @param name Name assigned to the resource.
	 * @return The path.
	 * @throws FileNotFoundException if the specified resources is not included
	 * into this Probe Configuration.
	 * @see addResource(String, String)
	 * @see getResourceList()
	 */
	public String getResourcePath(String name) throws FileNotFoundException;


	/**
	 * Get the list of bundle names into this Probe Configuration.
	 * @return A string array containing all bundle names.
	 * @see addBundle(String, String, int, int)
	 * @see getBundleInstallLevel(String)
	 * @see getBundlePath(String)
	 * @see getBundleStartLevel(String)
	 */
	public String[] getBundleList();


	/**
	 * Get the path of a specified bundle.
	 * @param name bundle's name.
	 * @return Path to the bundle in the local file system.
	 * @throws FileNotFoundException if the specified bundle is not included
	 * into this Probe Configuration.
	 * @see addBundle(String, String, int, int)
	 * @see getBundleList();
	 */
	public String getBundlePath(String name) throws FileNotFoundException;


	/**
	 * Get the install level of a specified bundle.
	 * @param name Bundle's name.
	 * @return The install level.
	 * @exception FileNotFoundException if the specified bundle is not included
	 * into this Probe Configuration.
	 * @see addBundle(String, String, int, int)
	 * @see getBundleList()
	 */
	public int getBundleInstallLevel(String name) throws FileNotFoundException;


	/**
	 * Get the start level of a specified bundle.
	 * @param name Bundle's name.
	 * @return The start level.
	 * @exception FileNotFoundException if the specified bundle is not included
	 * into this Probe Configuration.
	 * @see addBundle(String, int, int)
	 * @see getBundleList()
	 */
	public int getBundleStartLevel(String name) throws FileNotFoundException;


	/**
	 * Get the command line inside this Probe Configuration.
	 * @see setCommandLine(String)
	 */
	public String getCommandLine();


	/**
	 * Get the Probe's name.
	 */
	public String getName();


	/**
	 * Save all changes made to this Probe Configuration.
	 * @exception IOExcepton if this Probe Configuration cannot be saved.
	 */
	public void save() throws IOException;


	/**
	 * Get the signature of this Probe Configuration.
	 * @return A string with a representation of the signature.
	 */
	public String getSignatureString();


	/**
	 * Resolve bundle dependencies.
	 * A new Probe Configuration object is generated containing also the new
	 * dependency bundles with correct install and start levels.
	 * The generated configuration cannot be saved.
	 * @return A new Probe Configuration object with all dependencies resolved.
	 * @see addBundle(String, int, int)
	 * @see getBundleList()
	 */
	public IProbeConfiguration resolveDependencies();

}

