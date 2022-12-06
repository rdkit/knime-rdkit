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

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.core.node.NodeLogger;
import org.rdkit.knime.RDKitTypesPluginActivator;
import org.rdkit.knime.headers.HeaderPropertyHandlerRegistry;

/**
 * This is the preference page for the RDKit chemistry type definition. It
 * allows the user to change preferred renderer for all types listed in
 * {@link RDKitTypesPluginActivator#getCustomizableTypeList()}.
 *
 * @author Greg Landrum
 * @author Manuel Schwarze
 */
public class RDKitTypesPreferencePage extends FieldEditorPreferencePage
implements IWorkbenchPreferencePage {

	//
	// Constants
	//

	/** The preference key that stores a semicolon separated list of handlers for tooltip renderings. */
	public static final String PREF_KEY_HANDLERS_RENDERING_HEADERS =
			HeaderPropertyHandlerRegistry.PREF_KEY_HANDLERS_RENDERING_HEADERS;

	/** The preference key that stores a semicolon separated list of handlers for tooltip renderings. */
	public static final String PREF_KEY_HANDLERS_RENDERING_TOOLTIPS =
			HeaderPropertyHandlerRegistry.PREF_KEY_HANDLERS_RENDERING_TOOLTIPS;

	/** The preference key that stores a semicolon separated list of handlers that are disabled. */
	public static final String PREF_KEY_HANDLERS_DISABLED =
			HeaderPropertyHandlerRegistry.PREF_KEY_HANDLERS_DISABLED;

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

	//
	// Constructors
	//

	/**
	 * Creates a new preference page.
	 */
	public RDKitTypesPreferencePage() {
		super(GRID);
		// we use the pref store of the UI plugin
		setPreferenceStore(RDKitTypesPluginActivator.getDefault()
				.getPreferenceStore());
		setDescription("RDKit Preferred Renderer");
	}

	/** {@inheritDoc} */
	@Override
	protected void createFieldEditors() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(final IWorkbench workbench) {
		// nothing to do
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
				}
			}
			catch (final Exception exc) {
				LOGGER.error("Default values could not be set for the RDKit Types preferences. Plug-In or Preference Store not found.", exc);
			}
		}
	}

}
