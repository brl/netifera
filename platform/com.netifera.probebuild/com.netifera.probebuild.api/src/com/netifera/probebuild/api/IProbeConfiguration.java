
package com.netifera.probebuild.api;

import java.io.IOException;
import java.io.FileNotFoundException;


/**
 * Probe Configuration Interface.
 */
public interface IProbeConfiguration
{
	/**
	 * Get this Configuration's directory where all data is stored.
	 */
	public String getConfigurationDirectory();


	/**
	 * Add a new resource into this Probe Configuration.
	 * Notice that a reference to the resource is added, the actual
	 * resource should exist when a Deployable Probe is about to be built.
	 * @param path Path to the resource to add.
	 * @see getResrouceList()
	 */
	public void addResource(String path);


	/**
	 * Add a new bundle into this Probe Configuration.
	 * Notice that a reference to the bundle is added, the actual
	 * bundle should exist when a Deployable Probe is about to be built.
	 * @param bundleName Name of the bundle to add;
	 * @param initLevel Installation level (if <code>0</code>
	 * the bundle should not be installed);
	 * @param startLevel Starting level (if <code>0</code>
	 * the bundle should not be started).
	 * @see getBundleList()
	 */
	public void addBundle(String bundleName, int initLevel, int startLevel);


	/**
	 * Set the command line for this Probe Configuration.
	 * The command line options are the same received by a common jvm
	 * with subtle differences:
	 * <p>
	 * For example, a normal jvm will accept the following command line:
	 * <br>
	 * <code>-Xmx512 -Dport=12345 -jar osgiFramework.jar -some_java_param</code>
	 * </p>
	 * On the other hand, a Deployable Probe embeds an osgi framework that
	 * should not be specified in the command line:<br>
	 * <code>-Xmx512 -Dport=12345 -- -some_java_param</code>
	 * </p>
	 * Notice that in this case the <code>--</code> string is used as a
	 * separator between the command line options sent to the jvm and the
	 * options for the main class.
	 * There is no need to add this separator when no options are passed
	 * to the main class.
	 * <p>
	 * It is possible to avoid the execution of the embedded osgi framework
	 * specifing a custom jar file (that should be embedded into the
	 * Deployable Probe as a resource), for example:<br>
	 * <code>-Xmx512 -Dport=12345 -jar myCustomApplication.jar -some_java_param</code>
	 * </p>
	 * The above is very useful for testing purposes.
	 * @param line Command line to set.
	 * @see getCommandLine()
	 */
	public void setCommandLine(String line);


	/**
	 * Get the list of resources inside this Probe Configuration.
	 * @see addResource(String)
	 */
	public String[] getResourceList();


	/**
	 * Get the list of bundles inside this Probe Configuration.
	 * @see addBundle(String, int, int)
	 * @see getBundleInitLevel(String)
	 * @see getBundleStartLevel(String)
	 */
	public String[] getBundleList();


	/**
	 * Get the init level of a specified bundle.
	 * @param bundleName Name of the bundle.
	 * @return The init level.
	 * @exception FileNotFoundException if the bundle is not included
	 * into this Probe Configuration.
	 * @see addBundle(String, int, int)
	 * @see getBundleList()
	 */
	public int getBundleInitLevel(String bundleName) throws FileNotFoundException;


	/**
	 * Get the start level of a specified bundle.
	 * @param bundleName Name of the bundle.
	 * @return The start level.
	 * @exception FileNotFoundException if the bundle is not included
	 * into this Probe Configuration.
	 * @see addBundle(String, int, int)
	 * @see getBundleList()
	 */
	public int getBundleStartLevel(String bundleName) throws FileNotFoundException;


	/**
	 * Get the commnd line inside this Probe Configuration.
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
	 * A new Probe Configuration objet is generated containing also the new
	 * dependency bundles with correct init and start levels.
	 * The generated configuration cannot be saved.
	 * @return A new Probe Configuration object with all dependencies resolved.
	 * @see addBundle(String, int, int)
	 * @see getBundleList()
	 */
	public IProbeConfiguration resolveDependencies();

}


