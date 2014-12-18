/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2010
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
package org.rdkit.knime.nodes;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.rdkit.knime.extensions.aggregration.RDKitMcsAggregationPreferenceInitializer;
import org.rdkit.knime.nodes.preferences.RDKitNodesPreferenceInitializer;

/**
 * This is the eclipse bundle activator for the RDKit Nodes Plugin.
 *
 * @author Greg Landrum
 * @author Manuel Schwarze
 */
public class RDKitNodePlugin extends AbstractUIPlugin {

	//
	// Constants
	//

	/**
	 * The ID of the RDKit Nodes Plugin as defined also in the plug-ins
	 * MANIFEST file: org.rdkit.knime.nodes.
	 */
	public static final String PLUGIN_ID = "org.rdkit.knime.nodes";

	//
	// Global Variables.
	//

	/** The shared instance of the plugin. */
	private static RDKitNodePlugin g_instance;

	/** Flag to prevent recursive calls when initializing default preferences. */
	private static boolean g_bSettingDefaultPreferencesFinished = false;

	//
	// Constructor
	//

	/**
	 * The constructor.
	 */
	public RDKitNodePlugin() {
		super();
		setDefault(this);
	}

	//
	// Public Methods
	//

	/**
	 * This method is called upon plug-in activation.
	 *
	 * @param context The OSGI bundle context
	 * @throws Exception If this plugin could not be started
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
	}

	/**
	 * This method is called when the plug-in is stopped.
	 *
	 * @param context The OSGI bundle context
	 * @throws Exception If this plugin could not be stopped
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
		super.stop(context);
		g_instance = null;
	}

	/**
	 * Returns the shared instance.
	 *
	 * @return Singleton instance of the Plugin
	 */
	public static RDKitNodePlugin getDefault() {
		return g_instance;
	}

	@Override
	public IPreferenceStore getPreferenceStore() {
		final IPreferenceStore prefStore = super.getPreferenceStore();

		synchronized (PLUGIN_ID) {
			// Check, if default settings have already been initialized (normally
			// true when KNIME is started with GUI,
			// but not done when running regression tests) - If not done yet, do it
			if (!g_bSettingDefaultPreferencesFinished) {

				// Set to true to prevent endless loop recursion as
				// getPreferenceStore() is called also from initializers
				g_bSettingDefaultPreferencesFinished = true;

				// Call all initializer here manually
				new RDKitNodesPreferenceInitializer().initializeDefaultPreferences();
				new RDKitMcsAggregationPreferenceInitializer().initializeDefaultPreferences();
			}
		}

		return prefStore;
	}

	//
	// Static Private Methods
	//

	/**
	 * Set the static plugin variable to the instance of the plugin.
	 * 
	 * @param defaultPlugin the plugin instance to be set as default.
	 */
	private static synchronized void setDefault(final RDKitNodePlugin defaultPlugin) {
		g_instance = defaultPlugin;
	}

}
