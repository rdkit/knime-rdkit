/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2022
 * Novartis Institutes for BioMedical Research
 *
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
 * ---------------------------------------------------------------------
 */
package org.rdkit.knime.types.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.core.node.NodeLogger;
import org.rdkit.knime.RDKitTypesPluginActivator;
import org.rdkit.knime.headers.HeaderPropertyHandlerRegistry;
import org.rdkit.knime.util.EclipseUtils;

/**
 * This is the preference page for the RDKit chemistry type definition.
 *
 * @author Greg Landrum
 * @author Manuel Schwarze
 */
public class RDKitTypesPreferencePage extends FieldEditorPreferencePage
implements IWorkbenchPreferencePage {

	//
	// Constants
	//

	/** The id of this preference page. */
	public static final String ID = "org.rdkit.knime.types.preferences.RDKitTypes";

	/** The preference key that stores a semicolon separated list of handlers for tooltip renderings. */
	public static final String PREF_KEY_HANDLERS_RENDERING_HEADERS =
			HeaderPropertyHandlerRegistry.PREF_KEY_HANDLERS_RENDERING_HEADERS;

	/** The preference key that stores a semicolon separated list of handlers for tooltip renderings. */
	public static final String PREF_KEY_HANDLERS_RENDERING_TOOLTIPS =
			HeaderPropertyHandlerRegistry.PREF_KEY_HANDLERS_RENDERING_TOOLTIPS;

	/** The preference key that stores a semicolon separated list of handlers that are disabled. */
	public static final String PREF_KEY_HANDLERS_DISABLED =
			HeaderPropertyHandlerRegistry.PREF_KEY_HANDLERS_DISABLED;

	/** The preference key that stores the flag to enable/disable strict parsing for mol blocks when auto-converting SDFs. */
	public static final String PREF_KEY_STRICT_PARSING_AUTO_CONVERSION = "mol2rdkit.strictparsing.autoconversion";

	/** The preference key that stores the flag to enable/disable strict parsing for mol blocks when rendering. */
	public static final String PREF_KEY_STRICT_PARSING_RENDERING = "mol2rdkit.strictparsing.rendering";

	/** The preference key that stores the default flag to enable/disable strict parsing for mol blocks in node settings (new nodes). */
	public static final String PREF_KEY_STRICT_PARSING_NODE_SETTINGS_DEFAULT = "mol2rdkit.strictparsing.nodesettings.default";
	
	/** The default for the flag to enable/disable strict parsing for mol blocks when auto-converting SDFs (true). */
	public static final boolean DEFAULT_STRICT_PARSING_AUTO_CONVERSION = true;
	
	/** The default for the flag to enable/disable strict parsing for mol blocks when rendering (false). */
	public static final boolean DEFAULT_STRICT_PARSING_RENDERING = false;
	
	/** The default for the flag to enable/disable strict parsing for mol blocks in node settings (new nodes) (true). */
	public static final boolean DEFAULT_STRICT_PARSING_NODE_SETTINGS = true;
	
	/** The logger instance. */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(
			RDKitTypesPreferencePage.class);

	//
	// Globals
	//

	/**
	 * Flag to determine, that defaults have been initialized already to avoid double init
	 * after such default may have been overridden from the outside.
	 */
	private static boolean g_bDefaultInitializationDone = false;

	/** Cached setting. */
	private static boolean g_bStrictParsingAutoConversion = DEFAULT_STRICT_PARSING_AUTO_CONVERSION;

	/** Cached setting. */
	private static boolean g_bStrictParsingRendering = DEFAULT_STRICT_PARSING_RENDERING;

	/** Cached setting. */
	private static boolean g_bStrictParsingNodeSettingsDefault = DEFAULT_STRICT_PARSING_NODE_SETTINGS;
	
	//
	// Members
	//
	
	/** The editor for setting strict parsing for auto conversion. */
	private BooleanFieldEditor m_editorStrictParsingAutoConversion;
	
	/** The editor for setting strict parsing for rendering. */
	private BooleanFieldEditor m_editorStrictParsingRendering;
	
	/** The editor for setting strict parsing for node settings defaults (new nodes). */
	private BooleanFieldEditor m_editorStrictParsingNodeSettings;

	//
	// Constructors
	//

	/**
	 * Creates a new preference page.
	 */
	public RDKitTypesPreferencePage() {
		super(GRID);

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
				"The RDKit Nodes use their own molecule representation. When parsing mol blocks from SDF format in different "
				+ "scenarios the tolerance level for correctness can be set here and in some nodes.");
	}

	/** {@inheritDoc} */
	@Override
	protected void createFieldEditors() {
		m_editorStrictParsingAutoConversion = new BooleanFieldEditor(PREF_KEY_STRICT_PARSING_AUTO_CONVERSION,
				"Enable strict parsing when auto-converting SDF format to RDKit Molecules", getFieldEditorParent());
		addField(m_editorStrictParsingAutoConversion);

		m_editorStrictParsingRendering = new BooleanFieldEditor(PREF_KEY_STRICT_PARSING_RENDERING,
				"Enable strict parsing when rendering SDF molecules with RDKit 2D Depiction", getFieldEditorParent());
		addField(m_editorStrictParsingRendering);

		m_editorStrictParsingNodeSettings = new BooleanFieldEditor(PREF_KEY_STRICT_PARSING_NODE_SETTINGS_DEFAULT,
				"Enable strict parsing by default when creating new nodes that support this option", getFieldEditorParent());
		addField(m_editorStrictParsingNodeSettings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(final IWorkbench workbench) {
		// nothing to do
	}

	//
	// Static Methods
	//
	
	/**
	 * Updates the cache for boolean strict parsing values from the configuration. We store them for performance reasons
	 * separately as static members to access to very quickly without accessing the preference store again and again, 
	 * e.g. during auto-conversion or rendering, when speed matters.
	 */
	public static void updateConfigCache() {
		// We use the preference store that is defined in the UI plug-in
		final RDKitTypesPluginActivator plugin = RDKitTypesPluginActivator.getDefault();

		if (plugin != null) {
			final IPreferenceStore prefStore = plugin.getPreferenceStore();
			g_bStrictParsingAutoConversion = prefStore.getBoolean(PREF_KEY_STRICT_PARSING_AUTO_CONVERSION); 
			g_bStrictParsingRendering = prefStore.getBoolean(PREF_KEY_STRICT_PARSING_RENDERING); 
			g_bStrictParsingNodeSettingsDefault = prefStore.getBoolean(PREF_KEY_STRICT_PARSING_NODE_SETTINGS_DEFAULT); 
		}
	}
	
	/**
	 * Returns the current preference for strict parsing when auto-converting SDF into RDKit Molecules.
	 * 
	 * @return Strict parsing option.
	 */
	public static boolean isStrictParsingForAutoConversion() {
		return g_bStrictParsingAutoConversion;
	}

	/**
	 * Returns the current preference for strict parsing when rendering SDF Molecules.
	 * 
	 * @return Strict parsing option.
	 */
	public static boolean isStrictParsingForRendering() {
		return g_bStrictParsingRendering;
	}

	/**
	 * Returns the current preference for the default value of strict parsing when creating new nodes with this settings.
	 * 
	 * @return Strict parsing option.
	 */
	public static boolean isStrictParsingForNodeSettingsDefault() {
		return g_bStrictParsingNodeSettingsDefault;
	}

	/**
	 * Gets the appropriate preference store and initializes its default values.
	 * This method must be called from the subclass of AbstractPreferenceInitializer,
	 * which needs to be configured in the plugin.xml file as extension point.
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
					prefStore.setDefault(
							HeaderPropertyHandlerRegistry.PREF_KEY_HANDLERS_RENDERING_HEADERS,
							HeaderPropertyHandlerRegistry.getInstance().getDefaultColumnHeaderRenderers());
					prefStore.setDefault(
							HeaderPropertyHandlerRegistry.PREF_KEY_HANDLERS_RENDERING_TOOLTIPS,
							HeaderPropertyHandlerRegistry.getInstance().getDefaultColumnTooltipRenderers());
					prefStore.setDefault(
							HeaderPropertyHandlerRegistry.PREF_KEY_HANDLERS_DISABLED,
							HeaderPropertyHandlerRegistry.getInstance().getDefaultDisabledColumnRenderers());
					prefStore.setDefault(
							PREF_KEY_STRICT_PARSING_AUTO_CONVERSION, DEFAULT_STRICT_PARSING_AUTO_CONVERSION);
					prefStore.setDefault(
							PREF_KEY_STRICT_PARSING_RENDERING, DEFAULT_STRICT_PARSING_RENDERING);
					prefStore.setDefault(
							PREF_KEY_STRICT_PARSING_NODE_SETTINGS_DEFAULT, DEFAULT_STRICT_PARSING_NODE_SETTINGS);
					updateConfigCache();	
				}
			}
			catch (final Exception exc) {
				LOGGER.error("Default values could not be set for the RDKit Types preferences. Plug-In or Preference Store not found.", exc);
			}
		}
	}

}
