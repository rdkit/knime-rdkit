/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C)2010-2023
 *  Novartis Pharma AG, Switzerland
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ----------------------------------------------------------------------------
 */
package org.rdkit.knime;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.RDKit.RDKFuncs;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.rdkit.knime.types.preferences.RDKitDepicterPreferencePage;
import org.rdkit.knime.types.preferences.RDKitTypesPreferencePage;

/**
 * This is the activator for this plugin that is instantiated by the Eclipse
 * framework once the plugin is loaded. It does some initialization stuff.
 *
 * @author Greg Landrum
 * @author Manuel Schwarze
 */
@SuppressWarnings("restriction")
public class RDKitTypesPluginActivator extends AbstractUIPlugin {

	//
	// Constants
	//

	/**
	 * The ID of the RDKit Type Plugin as defined also in the plug-ins MANIFEST
	 * file: org.rdkit.knime.types.
	 */
	public static final String PLUGIN_ID = "org.rdkit.knime.types";

	/** The logging instance. */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitTypesPluginActivator.class);

	/**
	 * List of libraries to be loaded for different operating systems (lib order
	 * is important).
	 */
	private static final Map<String, String[]> LIBRARIES = new HashMap<String, String[]>();

	/**
	 * We define here what libraries are necessary to run the RDKit for the
	 * different supported platforms.
	 */
	static {
      LIBRARIES.put(Platform.OS_WIN32 + "." + Platform.ARCH_X86_64,
            new String[] { "GraphMolWrap" });
      LIBRARIES.put(Platform.OS_LINUX + "." + Platform.ARCH_X86,
            new String[] { "GraphMolWrap" });
      LIBRARIES.put(Platform.OS_LINUX + "." + Platform.ARCH_X86_64,
            new String[] { "GraphMolWrap" });
      LIBRARIES.put(Platform.OS_MACOSX + "." + Platform.ARCH_X86_64,
            new String[] { "GraphMolWrap" });
      LIBRARIES.put(Platform.OS_MACOSX + "." + "aarch64" /* Platform.ARCH_AARCH64, only available in Eclipse 3.22+ (06/2021) */,
            new String[] { "GraphMolWrap" });
	}

	//
	// Global Variables
	//

	/** The shared instance. */
	public static RDKitTypesPluginActivator g_instance;

	/** Error status in case of an error. */
	private static IStatus g_error;

	//
	// Constructor
	//

	/**
	 * Creates the new plugin instance to be hosted in Eclipse.
	 */
	public RDKitTypesPluginActivator() {
		super();
		g_instance = this;
	}

	/**
	 * This method is called upon plug-in activation.
	 *
	 * @param context
	 *            the OSGI bundle context
	 * @throws Exception
	 *             if this plugin could not be started
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);

		final String msg = EnvironmentChecker.checkEnvironment();
		if (msg != null) {
			LOGGER.error(msg);
		}

		try {
			g_error = null;

			final String[] arrLibraries = LIBRARIES.get(Platform.getOS() + "."
					+ Platform.getOSArch());

			if (arrLibraries == null) {
				throw new UnsatisfiedLinkError(
						"Unsupported operating system or architecture.");
			} else {
				// Load libraries
				for (final String strLibName : arrLibraries) {
					System.loadLibrary(strLibName);
				}
			}
		} catch (final Throwable e) {
			g_error = new Status(IStatus.ERROR, context.getBundle()
					.getSymbolicName(), "Could not load native RDKit library: "
					+ e.getMessage(), e);
			LOGGER.error(g_error.getMessage(), g_error.getException());
			Platform.getLog(context.getBundle()).log(g_error);
			investigateBinariesIssue();
		}
		
		// Setup default settings for RDKit
		RDKFuncs.setPreferCoordGen(true);
		
		// Setup preference change listeners for our own preferences
		getPreferenceStore()
				.addPropertyChangeListener(new IPropertyChangeListener() {

					/**
					 * Called whenever a preference of the RDKit Types
					 * Plug-In changes. 
					 */
					@Override
					public void propertyChange(
							final PropertyChangeEvent event) {
						switch (event.getProperty()) {
							case RDKitDepicterPreferencePage.PREF_KEY_CONFIG_FILE:
							case RDKitDepicterPreferencePage.PREF_KEY_CONFIG_JSON:
							case RDKitDepicterPreferencePage.PREF_KEY_NORMALIZE_DEPICTIONS:
							case RDKitDepicterPreferencePage.PREF_KEY_USE_COORDGEN:
								RDKitDepicterPreferencePage.clearConfigCacheAndResetFailure();
								break;
							case RDKitTypesPreferencePage.PREF_KEY_STRICT_PARSING_AUTO_CONVERSION:
							case RDKitTypesPreferencePage.PREF_KEY_STRICT_PARSING_RENDERING:
							case RDKitTypesPreferencePage.PREF_KEY_STRICT_PARSING_NODE_SETTINGS_DEFAULT:
								RDKitTypesPreferencePage.updateConfigCache();
								break;
						}
					}
				});
	}

	/**
	 * This method is called when the plug-in is stopped.
	 *
	 * @param context
	 *            the OSGI bundle context
	 * @throws Exception
	 *             if this plugin could not be stopped
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
		super.stop(context);
		g_instance = null;
	}

	//
	// Protected Methods
	//
	
	/**
	 * This method gets called when the RDKit Binaries failed to initialize
	 * properly. It tries to find out the cause and suggests to the user (via
	 * console) how to fix the issues.
	 */
	protected void investigateBinariesIssue() {
		final String strOsAndArch = Platform.getOS() + "."
				+ Platform.getOSArch();
		final String strBinaryBundleName = "org.rdkit.knime.bin."
				+ strOsAndArch;
		final String[] arrLibraries = LIBRARIES.get(strOsAndArch);
		final String strSupportedSystems = LIBRARIES.keySet().toString();
		final Version versionTypes = getDefault().getBundle().getVersion();

		try {
			// Check, if our software supports the operating system
			if (arrLibraries == null) {
				LOGGER.error("The operating system/architecture "
						+ strOsAndArch
						+ " is not supported. You may run the RDKit Nodes on: "
						+ strSupportedSystems);
			} else {
				LOGGER.info("The operating system/architecture " + strOsAndArch
						+ " is supported.");

				// Find binary plugin
				final Bundle bundle = Platform.getBundle(strBinaryBundleName);
				if (bundle == null) {
					LOGGER.error("The RDKit Binary Plugin "
							+ strBinaryBundleName
							+ " is not properly installed. "
							+ "Please uninstall and reinstall the RDKit Nodes.");
				} else {
					LOGGER.info("The RDKit Binary Plugin "
							+ strBinaryBundleName + " has been found.");

					// Check version of the binary plugin - it should be in sync
					// with the RDKit Types plugin version
					final Version versionBinaries = bundle.getVersion();
					if (versionTypes == null
							|| versionBinaries == null
							|| (versionTypes.getMajor() != versionBinaries
									.getMajor())
							|| (versionTypes.getMinor() != versionBinaries
									.getMinor())
							|| (versionTypes.getMicro() != versionBinaries
									.getMicro())) {
						LOGGER.error("The RDKit Binary Plugin "
								+ strBinaryBundleName
								+ " Version ("
								+ versionBinaries
								+ ") is different from the RDKit Types Version ("
								+ versionTypes
								+ "). Please uninstall and reinstall the RDKit Nodes.");
					} else {
						LOGGER.info("The versions of the RDKit Binary and Types Plugin are matching: "
								+ versionTypes);

						// Determine paths where libraries are loaded from
						// (combining Bundle path and java.library.path sources)
						final String[] arrPaths = getLibraryPaths(bundle);

						// Go through libraries and try to load them
						int iError = 0;
						int iMissingFile = 0;
						int iDependencyIssue = 0;
						int iSecurityIssue = 0;

						for (final String strLibName : arrLibraries) {
							final String strAbsoluteLibPath = getBundleLibraryPath(
									bundle, strLibName);
							final String strLibFileName = System
									.mapLibraryName(strLibName);
							final File[] arrLibPaths = findLibrary(
									strLibFileName, arrPaths);

							if (arrLibPaths.length > 1) {
								LOGGER.warn("Library file "
										+ strLibFileName
										+ " exists in multiple places - please try some cleanup: ");
								for (final File filePath : arrLibPaths) {
									LOGGER.warn("  "
											+ filePath.getAbsolutePath());
								}
							} else if (arrLibPaths.length == 1) {
								LOGGER.warn("Library file " + strLibFileName
										+ " found: "
										+ arrLibPaths[0].getAbsolutePath());
							}

							try {
								// Load the library
								System.loadLibrary(strLibName);
								LOGGER.info("Library " + strLibFileName
										+ " successfully loaded from "
										+ strAbsoluteLibPath + ".");
							} catch (final SecurityException exc) {
								LOGGER.error(
										"Loading of library "
												+ strLibFileName
												+ " failed for security reasons"
												+ (iError > 0 ? " (possibly a subsequent error)"
														: "") + ": "
												+ exc.getMessage(), exc);
								LOGGER.error("The library "
										+ strLibFileName
										+ " cannot be accessed due to missing permission.");
								iSecurityIssue++;
								iError++;
							} catch (final UnsatisfiedLinkError exc) {
								LOGGER.error(
										"Loading of library "
												+ strLibFileName
												+ " failed"
												+ (iError > 0 ? " (possibly a subsequent error)"
														: "") + ": "
												+ exc.getMessage(), exc);

								if (arrLibPaths.length == 0) {
									LOGGER.error("The library "
											+ strLibFileName + " is missing.");
									iMissingFile++;
								} else {
									LOGGER.error("The library "
											+ strLibFileName
											+ " has dependency issues. "
											+ "Please run a dependency walker on this file to find out what is missing.");
									iDependencyIssue++;
								}

								iError++;
							}
						}

						if (iSecurityIssue > 0) {
							LOGGER.error("Suggestion for fix: Please check your read permissions on the listed files.");
						}

						else if (iMissingFile > 0) {
                     // On Windows systems we encountered very strange behavior, if the VS2010 redistributables
                     // are not installed - For some reason that we do not understand in the moment in certain setups,
                     // e.g. in the cloud, in an install folder "C:\Program Files\..., it does not find the DLLs
                     // of RDKit that come with the plugin, even if they are definitely there. 
                     // We will suggest to the user in that case that the VS2010 Redistributables should be installed.
                     if (Platform.OS_WIN32.equals(Platform.getOS())) {
                        if( Platform.ARCH_X86.equals(Platform.getOSArch()) ) {
                           LOGGER.error("Suggestion for fix: Please install the VS2017 Redistributables from https://go.microsoft.com/fwlink/?LinkId=746572 and then restart KNIME.");                        
                        } 
                        else if( Platform.ARCH_X86_64.equals(Platform.getOSArch()) ) {
                           LOGGER.error("Suggestion for fix: Please install the VS2017 Redistributables from https://go.microsoft.com/fwlink/?LinkId=746572 and then restart KNIME.");                        
                        } 
                        else {
                           LOGGER.error("Suggestion for fix: Please install the VS2017 Redistributables for your system and then restart KNIME.");                      
                        }
                     }
                     else {
                        LOGGER.error("Suggestion for fix: Please uninstall and reinstall the RDKit Nodes.");
                     }
						}

						else if (iDependencyIssue > 0) {
							LOGGER.error("Suggestion for fix: Please correct your system libraries based on the outcome of the dependency walker.");
						}

					}
				}
			}
		} catch (final Throwable exc) {
			LOGGER.error("Investigation of RDKit Binaries issues failed: "
					+ exc.getMessage(), exc);
		}
	}

	/**
	 * Returns the path to a library for the specified Bundle, or null if not
	 * found.
	 * 
	 * @param bundleBinary
	 *            Bundle of the binaries. Can be null.
	 * @param strLibName
	 *            Library name (without OS specifics). Can be null.
	 * 
	 * @return Absolute library path or null, if not found.
	 */
	protected String getBundleLibraryPath(final Bundle bundleBinary,
			final String strLibName) {
		String strPath = null;

		// Add path of bundle
		if (bundleBinary != null && strLibName != null) {
			if (bundleBinary instanceof EquinoxBundle) {
				final File fileLibrary = ((EquinoxBundle) bundleBinary)
						.getDataFile(System.mapLibraryName(strLibName));
				if (fileLibrary != null) {
					strPath = fileLibrary.getAbsolutePath();
				}
			}
		}

		return strPath;
	}

	/**
	 * Returns an array of all (known) paths where the system would look for
	 * RDKit Libraries.
	 * 
	 * @param bundleBinary
	 *            Bundle of the binaries. Can be null.
	 * 
	 * @return Library paths incl. the paths to the RDKit Binary Plugin.
	 */
	protected String[] getLibraryPaths(final Bundle bundleBinary) {
		final List<String> listPaths = new ArrayList<String>();

		// Add path of bundle
		if (bundleBinary != null) {
			if (bundleBinary instanceof EquinoxBundle) {
				String strLocation = ((EquinoxBundle) bundleBinary)
						.getLocation();
				if (strLocation != null) {
					try {
						if (strLocation.startsWith("reference:")) {
							strLocation = strLocation.substring("reference:"
									.length());
						}
						File fileDir = new File(URLDecoder.decode(new URL(strLocation).getFile().substring(1), "UTF-8"));
						if (fileDir.isDirectory()) { 
							final File fileLibraryPath = new File(new File(new File(fileDir, "os"), Platform.getOS()), Platform.getOSArch()); 
							if (fileLibraryPath.isDirectory()) {
								listPaths.add(fileLibraryPath.getAbsolutePath()); 
							} 
						}						
					} catch (Exception exc) {
						LOGGER.warn("Unable to evaluate RDKit Binary Bundle path properly.");
					}
				}
			}
		}
	
		// Add Java library path
		final String[] arrPaths = System.getProperty("java.library.path", "")
				.split(File.pathSeparator);
		for (final String strPath : arrPaths) {
			listPaths.add(strPath);
		}

		return listPaths.toArray(new String[listPaths.size()]);
	}

	/**
	 * Tries to determine where the specified library (specific OS dependent
	 * name) is getting loaded from.
	 * 
	 * @param strLibFileName
	 *            Library file name.
	 * 
	 * @return Full paths if found. Can be more than one, if it is found in more
	 *         than one path. That would be suspicious.
	 */
	protected File[] findLibrary(final String strLibFileName,
			final String[] arrLibPaths) {
		final List<File> listPaths = new ArrayList<File>();

		for (final String strPath : arrLibPaths) {
			final File fileDir = new File(strPath);
			if (fileDir.isDirectory()) {
				final File fileLib = new File(fileDir, strLibFileName);
				try {
					if (fileLib.isFile() && fileLib.canRead()) {
						listPaths.add(fileLib);
					}
				} catch (final SecurityException exc) {
					// Thrown, if we do not have read access at all - ignore
					// this
				}
			}
		}

		return listPaths.toArray(new File[listPaths.size()]);
	}

	//
	// Static Public Methods
	//

	/**
	 * Checks if native RDKit library was successfully loaded upon plug-in
	 * activation. Throws {@link InvalidSettingsException} otherwise.
	 *
	 * @throws InvalidSettingsException
	 *             when an error occurred upon plug-in activation
	 */
	public static void checkErrorState() throws InvalidSettingsException {
		if (null != g_error) {
			throw new InvalidSettingsException(g_error.getMessage(),
					g_error.getException());
		}
	}

	/**
	 * Returns the shared instance.
	 *
	 * @return singleton instance of the Plugin.
	 */
	public static RDKitTypesPluginActivator getDefault() {
		return g_instance;
	}

}
