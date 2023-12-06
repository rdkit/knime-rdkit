/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2014-2023
 * Novartis Pharma AG, Switzerland
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
package org.rdkit.knime.extensions.aggregration;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.core.node.NodeLogger;
import org.rdkit.knime.nodes.RDKitNodePlugin;
import org.rdkit.knime.nodes.mcs.AtomComparison;
import org.rdkit.knime.nodes.mcs.BondComparison;
import org.rdkit.knime.util.EclipseUtils;

/**
 * This is the preference page for the RDKit MCS Aggregration.
 *
 * @author Manuel Schwarze
 */
public class RDKitMcsAggregationPreferencePage extends FieldEditorPreferencePage
implements IWorkbenchPreferencePage {

	//
	// Constants
	//

	/** The logger instance. */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(
			RDKitMcsAggregationPreferencePage.class);

	/** The id of this preference page. */
	public static final String ID = "org.rdkit.knime.extensions.aggregration.mcs";

	public static final String PREFIX = "mcsAggregation.";

	/** The preference key for the threshold. */
	public static final String PREF_KEY_THRESHOLD = PREFIX + "threshold";

	/** The preference key for the ring matches ring only option to be used. */
	public static final String PREF_KEY_RING_MATCHES_RING_ONLY_OPTION = PREFIX + "ringMatchesRingOnlyOption";

	/**The preference key for the complete rings only option to be used. */
	public static final String PREF_KEY_COMPLETE_RINGS_ONLY_OPTION = PREFIX + "completeRingsOnlyOption";

	/** The preference key for the match valences option to be used. */
	public static final String PREF_KEY_MATCH_VALENCES_OPTION = PREFIX + "matchValencesOption";

	/** The preference key for the atom comparison option to be used. */
	public static final String PREF_KEY_ATOM_COMPARISON = PREFIX + "atomComparison";

	/** The preference key for the bond comparison option to be used. */
	public static final String PREF_KEY_BOND_COMPARISON = PREFIX + "bondComparison";

	/** The preference key for the timeout in seconds to be used. */
	public static final String PREF_KEY_TIMEOUT = PREFIX + "timeout";

	/** The preference key for the threshold to be used. */
	public static final double DEFAULT_THRESHOLD = 1.0d;

	/** The default ring matches ring only option to be used. */
	public static final boolean DEFAULT_RING_MATCHES_RING_ONLY_OPTION = false;

	/** The default complete rings only option to be used. */
	public static final boolean DEFAULT_COMPLETE_RINGS_ONLY_OPTION = false;

	/** The default match valences option to be used. */
	public static final boolean DEFAULT_MATCH_VALENCES_OPTION = false;

	/** The default atom comparison option to be used. */
	public static final AtomComparison DEFAULT_ATOM_COMPARISON = AtomComparison.CompareElements;

	/** The default bond comparison option to be used. */
	public static final BondComparison DEFAULT_BOND_COMPARISON = BondComparison.CompareOrder;

	/** The default timeout in seconds to be used. */
	public static final int DEFAULT_TIMEOUT = 300;

	//
	// Globals
	//

	/**
	 * Flag to determine, that defaults have been initialized already to avoid double init
	 * after such default may have been overridden from the outside.
	 */
	private static boolean g_bDefaultInitializationDone = false;

	//
	// Members
	//

	/** The editor for the threshold. */
	private StringFieldEditor m_editorThreshold;

	/** The editor for the ringMatchesRingOnlyOption. */
	private BooleanFieldEditor m_editorRingMatchesRingOnlyOption;

	/** The editor for the completeRingsOnlyOption. */
	private BooleanFieldEditor m_editorCompleteRingsOnlyOption;

	/** The editor for the matchValencesOption. */
	private BooleanFieldEditor m_editorMatchValencesOption;

	/** The editor for the atom comparison mode. */
	private ComboFieldEditor m_editorAtomComparison;

	/** The editor for the bond comparison mode. */
	private ComboFieldEditor m_editorBondComparison;

	/** The editor for the timeout. */
	private IntegerFieldEditor m_editorTimeout;

	//
	// Constructors
	//

	/**
	 * Creates a new preference page.
	 */
	public RDKitMcsAggregationPreferencePage() {
		super(GRID);

		setImageDescriptor(new ImageDescriptor() {

			/**
			 * {@inheritDoc}
			 */
			@Override
			public ImageData getImageData() {
				return EclipseUtils.loadImageData(RDKitMcsAggregationPreferencePage.class,
						"/org/rdkit/knime/nodes/mcs/default.png");
			}
		});

		// We use the pref store of the UI plugin
		setPreferenceStore(RDKitNodePlugin.getDefault().getPreferenceStore());
		setDescription("The section defines preferences for the RDKit MCS (Maximum Common Substructure) Aggregation");
	}

	/** {@inheritDoc} */
	@Override
	protected void createFieldEditors() {
		m_editorThreshold = new StringFieldEditor(PREF_KEY_THRESHOLD,
				"Threshold (0.0 < t <= 1.0): ", StringFieldEditor.UNLIMITED,
				StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent());
		addField(m_editorThreshold);

		m_editorRingMatchesRingOnlyOption = new BooleanFieldEditor(PREF_KEY_RING_MATCHES_RING_ONLY_OPTION,
				"Ring matches ring only", getFieldEditorParent());
		addField(m_editorRingMatchesRingOnlyOption);

		m_editorCompleteRingsOnlyOption = new BooleanFieldEditor(PREF_KEY_COMPLETE_RINGS_ONLY_OPTION,
				"Complete rings only", getFieldEditorParent());
		addField(m_editorCompleteRingsOnlyOption);

		m_editorMatchValencesOption = new BooleanFieldEditor(PREF_KEY_MATCH_VALENCES_OPTION,
				"Match valences", getFieldEditorParent());
		addField(m_editorMatchValencesOption);

		final String[][] arrAtomComparisons = new String[AtomComparison.values().length][2];
		int i = 0;
		for (final AtomComparison comp : AtomComparison.values()) {
			arrAtomComparisons[i][0] = comp.toString();
			arrAtomComparisons[i][1] = comp.name();
			i++;
		}
		m_editorAtomComparison = new ComboFieldEditor(PREF_KEY_ATOM_COMPARISON,
				"Atom comparison: ", arrAtomComparisons, getFieldEditorParent());
		addField(m_editorAtomComparison);


		final String[][] arrBondComparisons = new String[BondComparison.values().length][2];
		i = 0;
		for (final BondComparison comp : BondComparison.values()) {
			arrBondComparisons[i][0] = comp.toString();
			arrBondComparisons[i][1] = comp.name();
			i++;
		}
		m_editorBondComparison = new ComboFieldEditor(PREF_KEY_BOND_COMPARISON,
				"Bond comparison: ", arrBondComparisons, getFieldEditorParent());
		addField(m_editorBondComparison);

		m_editorTimeout = new IntegerFieldEditor(PREF_KEY_TIMEOUT, "Timeout (in seconds): ", getFieldEditorParent());
		m_editorTimeout.setValidRange(1, Integer.MAX_VALUE);;
		addField(m_editorTimeout);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(final IWorkbench workbench) {
		// We initialize here the preference store of the RDKit Nodes plugin
		final RDKitNodePlugin plugin = RDKitNodePlugin.getDefault();

		if (plugin == null) {
			setErrorMessage("The RDKit Nodes Plug-In could not be loaded.");
		}
		else {
			// Set the preference store
			final IPreferenceStore prefStore = plugin.getPreferenceStore();
			setPreferenceStore(prefStore);
		}
	}

	/**
	 * {@inheritDoc}
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
	 * {@inheritDoc}
	 * We use this method to trim the text entered as URL and call the super method afterwards.
	 * 
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		boolean bRet = false;

		// Check the threshold value
		String strError = null;
		m_editorThreshold.setStringValue(m_editorThreshold.getStringValue().trim());
		try {
			final double dThreshold = Double.parseDouble(m_editorThreshold.getStringValue());
			if (dThreshold <= 0.0d || dThreshold > 1.0d) {
				strError = "Bad threshold value. Must be 0.0 < t <= 1.0!";
			}
		}
		catch (final NumberFormatException exc) {
			strError = "Threshold value is not a valid number.";
		}

		bRet = (strError == null);
		setValid(bRet);
		setErrorMessage(strError);

		// Store the values only, if they are valid
		if (bRet) {
			bRet = super.performOk();
		}

		return bRet;
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
				final RDKitNodePlugin plugin = RDKitNodePlugin.getDefault();

				if (plugin != null) {
					final IPreferenceStore prefStore = plugin.getPreferenceStore();

					// Define plug-in default values
					prefStore.setDefault(PREF_KEY_THRESHOLD, DEFAULT_THRESHOLD);
					prefStore.setDefault(PREF_KEY_RING_MATCHES_RING_ONLY_OPTION, DEFAULT_RING_MATCHES_RING_ONLY_OPTION);
					prefStore.setDefault(PREF_KEY_COMPLETE_RINGS_ONLY_OPTION, DEFAULT_COMPLETE_RINGS_ONLY_OPTION);
					prefStore.setDefault(PREF_KEY_MATCH_VALENCES_OPTION, DEFAULT_MATCH_VALENCES_OPTION);
					prefStore.setDefault(PREF_KEY_ATOM_COMPARISON, DEFAULT_ATOM_COMPARISON.name());
					prefStore.setDefault(PREF_KEY_BOND_COMPARISON, DEFAULT_BOND_COMPARISON.name());
					prefStore.setDefault(PREF_KEY_TIMEOUT, DEFAULT_TIMEOUT);
				}
			}
			catch (final Exception exc) {
				LOGGER.error("Default values could not be set for the RDKit MCS Aggregation preferences. Plug-In or Preference Store not found.");
			}
		}
	}

	//
	// Protected Methods
	//

	/**
	 * Resets the error message and performs a new validation of all settings,
	 * which may lead again to an error message.
	 * 
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#checkState()
	 */
	@Override
	protected void checkState() {
		setErrorMessage(null);
		super.checkState();
	}

}
