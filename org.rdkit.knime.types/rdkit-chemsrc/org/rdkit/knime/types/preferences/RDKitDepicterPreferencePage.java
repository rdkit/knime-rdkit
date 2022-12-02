/*
 * This source code, its documentation and all related files
 * are protected by copyright law. All rights reserved.
 *
 * (C)Copyright 2022 by Novartis Pharma AG
 * Novartis Campus, CH-4002 Basel, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 */
package org.rdkit.knime.types.preferences;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.core.node.NodeLogger;
import org.rdkit.knime.RDKitTypesPluginActivator;
import org.rdkit.knime.util.EclipseUtils;
import org.rdkit.knime.util.PreferenceButton;
import org.rdkit.knime.util.PreferenceUtils;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * This is the preference page for RDKit 2D Depicter's renderer settings.
 *
 * @author Manuel Schwarze
 */
public class RDKitDepicterPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	//
	// Constants
	//

	/** The id of this preference page. */
	public static final String ID = "org.rdkit.knime.types.preferences.RDKit2DDepiction";

	// Preference Key Definitions for Global Settings

	// RDKit Renderer / Depicter

	/**
	 * The URL / file to be used to access the JSON setting file for the renderer.
	 * If also a JSON structure is set, that one would take priority.
	 */
	public static final String PREF_KEY_CONFIG_FILE = "configFile";

	/**
	 * The JSON configuration for the renderer. If set it takes priority over
	 * loading it from a config file.
	 */
	public static final String PREF_KEY_CONFIG_JSON = "configJson";

	/**
	 * The interval in milliseconds after we should retry to read JSON config files / URL
	 * in case they failed to be accessed or read successfully. 
	 */
	public static final String PREF_KEY_RETRY_INTERVAL = "configLoadingRetryInterval";

	/** The default filename for the depiction settings, which is referring to our internal file. */
	public static final String DEFAULT_CONFIG_FILE = "[default built-in]";
	
	/** The default retry interval (10 minutes = 60000 millis). */
	public static final int DEFAULT_RETRY_INTERVAL = 60000; // 10 minutes
	
	/** The logger instance. */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(RDKitDepicterPreferencePage.class);

	//
	// Globals
	//

	/**
	 * Flag to determine, that defaults have been initialized already to avoid
	 * double init after such default may have been overridden from the outside.
	 */
	private static boolean g_bDefaultInitializationDone = false;
	
	/**
	 * The cached JSON configuration.
	 */
	private static String g_jsonConfig = null;
	
	/** The timestamp of last failure of reading the JSON config file or -1, if not set. */
	private static long g_lLastFailure = -1;

	//
	// Members
	//

	/** The editor for the RDKit 2D depiction config file. */
	private StringFieldEditor m_editorConfigFile;

	/**
	 * The editor for setting the JSON config for the RDKit 2D depiction directly.
	 */
	private StringFieldEditor m_editorConfigJson;

	//
	// Constructor
	//

	/**
	 * Creates a new preference page to configure settings.
	 */
	public RDKitDepicterPreferencePage() {
		super(GRID); // Don't use FLAT

		// We use the preference store that is defined in the UI plug-in
		final RDKitTypesPluginActivator plugin = RDKitTypesPluginActivator.getDefault();

		if (plugin == null) {
			setErrorMessage("The RDKit Types Plug-In could not be loaded.");
		} else {
			// Set the preference store
			final IPreferenceStore prefStore = plugin.getPreferenceStore();
			setPreferenceStore(prefStore);
		}

		setImageDescriptor(new ImageDescriptor() {

			/**
			 * {@inheritDoc}
			 */
			@Override
			public ImageData getImageData() {
				return EclipseUtils.loadImageData(RDKitDepicterPreferencePage.class, "/icons/category_rdkit.png");
			}
		});

		setDescription(
				"The RDKit 2D Depiction renders RDKit Molecules and plain SMILES and SDF values. The look can be controlled by a JSON configuration.");
	}

	//
	// Public Methods
	//

	/**
	 * This initialization method does not do anything in the current
	 * implementation.
	 * 
	 * @param workbench The workbench the preferences page belongs to
	 * 
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	@Override
	public void init(final IWorkbench workbench) {
		// Empty by purpose
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(final PropertyChangeEvent event) {
		super.propertyChange(event);

		if (event.getProperty().equals(FieldEditor.VALUE)) {
			checkState();
		}
	}

	/**
	 * {@inheritDoc} We use this method to trim the text entered as URL and call the
	 * super method afterwards.
	 * 
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		boolean bRet = true;

		// Trim the JSON config data (if set this takes priority)
		final String strConfigJson = m_editorConfigJson.getStringValue().trim();
		m_editorConfigJson.setStringValue(strConfigJson);

		// Verify if it is valid JSON
		String strError = getJsonError(strConfigJson);
		bRet = (strError == null);
		setErrorMessage(strError);
		
		// Trim the URL / File Path
		final String strConfigFile = m_editorConfigFile.getStringValue().trim();
		m_editorConfigFile.setStringValue(strConfigFile);

		// Store the values only, if they are valid
		if (bRet) {
			bRet = super.performOk();
		}

		return bRet;
	}

	//
	// Protected Methods
	//

	/**
	 * Resets the error message and performs a new validation of all settings, which
	 * may lead again to an error message.
	 * 
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#checkState()
	 */
	@Override
	protected void checkState() {
		setErrorMessage(null);
		super.checkState();
	}

	/**
	 * Generates all necessary editors to modify RDKit 2D Depicter settings.
	 * 
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	@Override
	protected void createFieldEditors() {
		m_editorConfigFile = new StringFieldEditor(PREF_KEY_CONFIG_FILE, "File or URL with JSON Configuration",
				getFieldEditorParent());
		addField(m_editorConfigFile);

		final PreferenceButton btnReadFile = new PreferenceButton("Read file content and use it below", getFieldEditorParent()) {
			@Override
			protected void onButtonClicked() {
				String strConfigFile = m_editorConfigFile.getStringValue();
				String strJsonConfig = null;
				
				if (DEFAULT_CONFIG_FILE.equals(strConfigFile)) {
					strJsonConfig = readFromResource("default-depiction.json");
				}
				else if (strConfigFile.startsWith("http:") || strConfigFile.startsWith("https:")) {
					strJsonConfig = readFromUrl(strConfigFile);
				}
				else {
					strJsonConfig = readFromFile(strConfigFile);
				}
				
				if (strJsonConfig != null) {
					m_editorConfigJson.setStringValue(strJsonConfig);
				}
				else {
					EclipseUtils.showMessage("Access Error", "The specified JSON definition file could not be accessed - please check the name.", 
							SWT.ICON_ERROR, false);
				}
			}
		};
		addField(btnReadFile);

		m_editorConfigJson = new StringFieldEditor(PREF_KEY_CONFIG_JSON, "JSON Configuration (takes priority, if set)",
				-1 /* width */, 20 /* height */, StringFieldEditor.VALIDATE_ON_FOCUS_LOST, getFieldEditorParent());
		addField(m_editorConfigJson);

		final PreferenceButton btnClear = new PreferenceButton("Clear", getFieldEditorParent()) {
			@Override
			protected void onButtonClicked() {
				m_editorConfigJson.setStringValue("");
			}
		};
		addField(btnClear);
	}

	//
	// Static Methods
	//

	/**
	 * Resets the JSON config cache and triggers a renewal, so that the new config data is available the next time
	 * when the JSON config is required for rendering. This gets called whenever
	 * preferences are changing.
	 */
	public static synchronized void clearConfigCacheAndResetFailure() {
		g_jsonConfig = null;
		g_lLastFailure = -1;
		getJsonConfig();
	}

	/**
	 * Determines the JSON configuration that should be applied to RDKit 2D
	 * depiction.
	 * 
	 * @return JSON config. Null, if no or empty JSON structure is available only.
	 */
	public static synchronized String getJsonConfig() {
		String strJsonConfig = g_jsonConfig;

		// If not cached yet, then generate JSON once and cache it
		if (strJsonConfig == null) {
			// We use the preference store that is defined in the UI plug-in
			final RDKitTypesPluginActivator plugin = RDKitTypesPluginActivator.getDefault();

			if (plugin != null) {
				final IPreferenceStore prefStore = plugin.getPreferenceStore();
				int iRetryInterval = prefStore.getInt(PREF_KEY_RETRY_INTERVAL);

				// Consider failures and only retry after a certain interval
				if (g_lLastFailure == -1 || (System.currentTimeMillis() - g_lLastFailure) > iRetryInterval) {
					String strConfigFile = prefStore.getString(PREF_KEY_CONFIG_FILE);
					String strConfigJsonData = prefStore.getString(PREF_KEY_CONFIG_JSON);

					// If there is no JSON structure set directly then try to read it from a file / URL
					if (!PreferenceUtils.isSet(strConfigJsonData) && PreferenceUtils.isSet(strConfigFile)) {
						if (DEFAULT_CONFIG_FILE.equals(strConfigFile)) {
							strJsonConfig = readFromResource("default-depiction.json");
						}
						else if (strConfigFile.startsWith("http:") || strConfigFile.startsWith("https:")) {
							strJsonConfig = readFromUrl(strConfigFile);
						}
						else {
							strJsonConfig = readFromFile(strConfigFile);
						}
						
						// Set to null, if we did not get anything useful - considered the same as an error
						if (!PreferenceUtils.isSet(strJsonConfig)) {
							strJsonConfig = null;
						}
						
						// A failure occurred - we will remember it and only retry after a certain time
						if (strJsonConfig == null) {
							g_lLastFailure = System.currentTimeMillis();
							LOGGER.warn("RDKit 2D Depiction configuration file '" + strConfigFile + 
									"' does not contain any content");
						}
						else {
							// Validate the JSON from a file or URL
							strJsonConfig = strJsonConfig.trim();
							String strError = getJsonError(strJsonConfig);
							if (strError != null) {
								strJsonConfig = null;
								g_lLastFailure = System.currentTimeMillis();
								LOGGER.warn("RDKit 2D Depiction configuration file '" + strConfigFile + 
										"' has errors: " + strError);
							}					
						}						
					}
					
					// Use the directly configured data structure
					else if (PreferenceUtils.isSet(strConfigJsonData)) {
						strJsonConfig = strConfigJsonData;
						
						// Validate the JSON data
						String strError = getJsonError(strJsonConfig);
						if (strError != null) {
							strJsonConfig = null;
							g_lLastFailure = System.currentTimeMillis();
							LOGGER.warn("RDKit 2D Depiction configuration JSON structure has errors: " + strError);
						}					
					}						

					g_jsonConfig = strJsonConfig;
				}
			}
		}

		return strJsonConfig;
	}

	/**
	 * Gets the appropriate preference store and initializes its default values.
	 * This method must be called from the subclass of
	 * AbstractPreferenceInitializer, which needs to be configured in the plugin.xml
	 * file as extension point.
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public static synchronized void initializeDefaultPreferences() {
		if (!g_bDefaultInitializationDone) {
			g_bDefaultInitializationDone = true;

			try {
				// We use the preference store that is defined in the UI plug-in
				final RDKitTypesPluginActivator plugin = RDKitTypesPluginActivator.getDefault();

				if (plugin != null) {
					final IPreferenceStore prefStore = plugin.getPreferenceStore();

					// Define plug-in default values
					prefStore.setDefault(PREF_KEY_CONFIG_FILE, DEFAULT_CONFIG_FILE);
					prefStore.setDefault(PREF_KEY_CONFIG_JSON, "");
					prefStore.setDefault(PREF_KEY_RETRY_INTERVAL, DEFAULT_RETRY_INTERVAL);
				}
			} 
			catch (final Exception exc) {
				LOGGER.error(
						"Default values could not be set for the RDKit 2D Depiction preferences. Plug-In or Preference Store not found.", exc);
			}
		}
	}

	//
	// Private Methods
	//
	
	/**
	 * Reads in the specified resource text file and returns it as string.
	 * 
	 * @param strResourceName
	 *            Resource to read. Behind it must be a text file for valid
	 *            results. Must not be null.
	 * 
	 * @return Read string from resource file or passed in value, if not a
	 *         resource file pointer.
	 *         
	 * @throws IOException Thrown if reading content from resource fails.
	 */
	private static String readFromResource(final String strResourceName) {
		// Check, if a file exists - however, we will reread the file every time
		// it is accessed to be flexible
		String strContent = null;
		InputStream input = null;
		URL urlResource = null;
		
		try {
			urlResource = RDKitDepicterPreferencePage.class.getResource(strResourceName);
			if (urlResource == null) {
				throw new IOException("Resource not found.");
			}
			strContent = readFromUrl(urlResource);
		}
		catch (Exception exc) {
			LOGGER.debug("Unable to access JSON config resource " + urlResource + ": " + exc.getMessage());
		}
		finally {
			if (input != null) {
				try {
					input.close();
				}
				catch (IOException excIO) {
					// Ignored by purpose
				}
			}
		}

		return strContent;
	}

	/**
	 * Reads in the specified resource text file and returns it as string.
	 * 
	 * @param strUrl
	 *            URL resource to read. Behind it must be a UTF-8 text file for valid
	 *            results. If null, it returns null and logs an error.
	 * 
	 * @return File/resource content. Null, if not found or invalid URL.
	 */
	private static String readFromUrl(final String strUrl) {
		String strContent = null;
		
		try {
			strContent = readFromUrl(new URL(strUrl));
		}
		catch (Exception exc) {
			LOGGER.error("Unable to access JSON config resource " + strUrl + ": " + exc.getMessage());
		}
		
		return strContent;
	}

	
	/**
	 * Reads in the specified resource text file and returns it as string.
	 * 
	 * @param urlResource
	 *            Resource to read. Behind it must be a UTF-8 text file for valid
	 *            results. If null, it returns null and logs an error.
	 * 
	 * @return File/resource content. Null, if not found or invalid URL.
	 */
	private static String readFromUrl(final URL urlResource)
			throws IOException {
		String strContent = null;
		InputStream input = null;

		try {
			if (urlResource == null) {
				throw new IOException("Resource is not defined.");
			}

			final StringBuilder stringBuilder = new StringBuilder(4096);
			final URLConnection conn = urlResource.openConnection();
			input = conn.getInputStream();
			final byte[] arrBuffer = new byte[4096];
			int iLen;
			while ((iLen = input.read(arrBuffer, 0, arrBuffer.length)) != -1) {
				stringBuilder.append(new String(arrBuffer, 0, iLen, Charset.forName("UTF-8")));
			}

			strContent = stringBuilder.toString();
		}
		catch (Exception exc) {
			LOGGER.error("Unable to access JSON config resource " + urlResource + ": " + exc.getMessage());
		}
		finally {
			if (input != null) {
				try {
					input.close();
				}
				catch (IOException excIO) {
					// Ignored by purpose
				}
			}
		}

		return strContent;
	}
	
	/**
	 * Read the JSON config content from the passed in file.
	 * 
	 * @param strFile File to retrieve. It should not be protected.
	 * 
	 * @return The file content or null, if not possible to read (e.g. due to an error).
	 */
	private static String readFromFile(String strFile) {
		String strJson = null;
		
		try {
			strJson = new String(Files.readAllBytes(Paths.get(strFile)), Charset.forName("UTF-8"));
		}
		catch (Exception exc) {
			LOGGER.error("Unable to load JSON config from file " + strFile + ": " + exc.getMessage());
		}
		
		return strJson;
	}
	
	/**
	 * Determines, if the passed in JSON is valid or not. If not valid, it 
	 * returns the error message of the validation method.
	 * 
	 * @param strJson JSON to check. Can be null.
	 * 
	 * @return Null, if valid or if null. Error message otherwise.
	 */
	private static String getJsonError(String strJson) {
		String strError = null;
		
		if (!strJson.isEmpty()) {
			try {
				if (!strJson.startsWith("{") || !strJson.endsWith("}")) {
					throw new JsonSyntaxException("The JSON structure must start with { and end with }.");
				} 
				else {
					JsonParser.parseString(strJson);
				}
			} 
			catch (JsonSyntaxException e) {
				String strMessage = e.getMessage();
				if (strMessage != null) {
					String strCutOffPrefix = "Exception: ";
					int index = strMessage.indexOf(strCutOffPrefix);
					strMessage = strMessage.substring(index == -1 ? 0 : index + strCutOffPrefix.length());
				}
				strError = ("The JSON configuration is invalid. " + (strMessage != null ? strMessage : ""));
			}
		}
		
		return strError;
	}
}
