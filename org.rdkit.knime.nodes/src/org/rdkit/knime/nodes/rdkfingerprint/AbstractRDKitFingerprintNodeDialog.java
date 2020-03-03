/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2012
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
package org.rdkit.knime.nodes.rdkfingerprint;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.DialogComponentColumnNameSelection;
import org.rdkit.knime.util.DialogComponentEnumSelection;
import org.rdkit.knime.util.LayoutUtils;
import org.rdkit.knime.util.SettingsModelEnumeration;

/**
 * The dialog to configure the RDKit node.
 *
 * @author Greg Landrum
 * @author Manuel Schwarze
 */
public abstract class AbstractRDKitFingerprintNodeDialog extends DefaultNodeSettingsPane {

	//
	// Constants
	//

	/** The default torsion path length to be used. */
	public static int DEFAULT_TORSION_PATH_LENGTH = 4;

	/** The default min path to be used. */
	public static int DEFAULT_MIN_PATH = 1;

	/** The default max path to be used. */
	public static int DEFAULT_MAX_PATH = 7;

	/** The default AtomPair min path to be used. */
	public static int DEFAULT_ATOMPAIR_MIN_PATH = 1;

	/** The default AtomPair max path to be used. */
	public static int DEFAULT_ATOMPAIR_MAX_PATH = 30;

	/** The default radius to be used. */
	public static int DEFAULT_RADIUS = 2;

	/** The default number of bits to be used. */
	public static int DEFAULT_NUM_BITS = 1024;

	/** 
	 * The default layer flags to be used. Set to 7, which means a
	 * combination of 0x01 pure topology + 0x02 bond order + 0x04 atom types.
	 */
	public static int DEFAULT_LAYER_FLAGS = 7;

	/** The default chirality option to be used. */
	public static boolean DEFAULT_USECHIRALITY_OPTION = false;

	/** The default rooting option to be used. */
	public static boolean DEFAULT_ROOTED_OPTION = false;

	/** The default atom list handling to be used. */
	public static boolean DEFAULT_ATOM_LIST_INCLUDE_HANDLING = true;

	/** The title of the Advanced Tab. */
	public static String ADVANCED_TAB_NAME = "Advanced";

	/** The title of the Advanced Tab. */
	public static String ROOTED_PARAMS_GROUP_NAME = "Rooted Fingerprints";

	//
	// Members
	//

	/** The fingerprint type model. */
	private SettingsModelEnumeration<FingerprintType> m_modelFingerprintType;

	/** The rooted fingerprint option model. */
	private SettingsModelBoolean m_modelRootedOption;

	/** The atom list column name. */
	private SettingsModelString m_modelAtomListColumn;

	/** The atom list handling option (true - include, false - exclude). */
	private SettingsModelBoolean m_modelAtomListHandlingOption;

	/** The number of bits setting model. */
	private SettingsModelIntegerBounded m_modelNumBits;

	/** The radius setting model. */
	private SettingsModelIntegerBounded m_modelRadius;

	/** The torsion path length setting model. */
	private SettingsModelIntegerBounded m_modelTorsionPathLength;

	/** The minimum path setting model. */
	private SettingsModelIntegerBounded m_modelMinPath;

	/** The maximum path setting model. */
	private SettingsModelIntegerBounded m_modelMaxPath;

	/** The atom pairs minimum path setting model. */
	private SettingsModelIntegerBounded m_modelAtomPairMinPath;

	/** The atom pairs maximum path setting model. */
	private SettingsModelIntegerBounded m_modelAtomPairMaxPath;

	/** The layer flags setting model. */
	private SettingsModelIntegerBounded m_modelLayerFlags;

	/** List of dialog components that can be shown or hidden. */
	private final List<DialogComponent> m_listHidableDialogComponents;

	/**
	 * Create a new dialog pane with some default components.
	 */
	AbstractRDKitFingerprintNodeDialog() {
		m_listHidableDialogComponents = new ArrayList<DialogComponent>();
		super.addDialogComponent(new DialogComponentEnumSelection<FingerprintType>(
				m_modelFingerprintType = createFPTypeModel(), "Fingerprint type: ",
				getSupportedFingerprintTypes()));
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				createSmilesColumnModel(), "RDKit Mol column: ", 0,
				RDKitMolValue.class));
		super.addDialogComponent(new DialogComponentString(
				createNewColumnModel(), "New fingerprint column name: "));
		super.addDialogComponent(new DialogComponentBoolean(
				createRemoveSourceColumnOptionModel(), "Remove source column"));

		createNewTab(ADVANCED_TAB_NAME);
		createNewGroup("Fingerprint Settings");
		setHorizontalPlacement(true);
		addHidableDialogComponent(new DialogComponentNumberEdit(
				m_modelNumBits = createNumBitsModel(), "Num Bits: ", 4));
		addHidableDialogComponent(new DialogComponentNumber(
				m_modelRadius = createRadiusModel(), "Radius: ", 1));
		addHidableDialogComponent(new DialogComponentNumberEdit(
				m_modelLayerFlags = createLayerFlagsModel(), "Layer Flags: ", 8));
		setHorizontalPlacement(false);
		addHidableDialogComponent(new DialogComponentNumber(
				m_modelTorsionPathLength = createTorsionPathLengthModel(), "Path Length: ", 1, 2));
		setHorizontalPlacement(true);
		addHidableDialogComponent(new DialogComponentNumber(
				m_modelMinPath = createMinPathModel(), "Min Path Length: ", 1, 2));
		addHidableDialogComponent(new DialogComponentNumber(
				m_modelMaxPath = createMaxPathModel(), "Max Path Length: ", 1, 2));
		setHorizontalPlacement(false);
		setHorizontalPlacement(true);
		addHidableDialogComponent(new DialogComponentNumber(
				m_modelAtomPairMinPath = createAtomPairMinPathModel(), "Min Path Length: ", 1, 2));
		addHidableDialogComponent(new DialogComponentNumber(
				m_modelAtomPairMaxPath = createAtomPairMaxPathModel(), "Max Path Length: ", 1, 2));
		setHorizontalPlacement(false);
		setHorizontalPlacement(true);
		addHidableDialogComponent(new DialogComponentBoolean(createUseChiralityModel(), "Use Chirality"));
		setHorizontalPlacement(false);

		createNewGroup(ROOTED_PARAMS_GROUP_NAME);
		super.addDialogComponent(new DialogComponentBoolean(
				m_modelRootedOption = createRootedOptionModel(), "Create rooted fingerprint"));
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				m_modelAtomListColumn = createAtomListColumnModel(m_modelFingerprintType, m_modelRootedOption),
				"Atom list column for rooted fingerprints: ", 0, false, true,
				CollectionDataValue.class, IntValue.class));
		super.addDialogComponent(new DialogComponentBoolean(
				m_modelAtomListHandlingOption = createAtomListHandlingIncludeOptionModel(m_modelFingerprintType, m_modelRootedOption),
				"Include atoms (disable to exclude them)"));

		m_modelFingerprintType.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				enableOrDisableSettings(m_modelFingerprintType.getValue());
			}
		});

		enableOrDisableSettings(m_modelFingerprintType.getValue());

		getPanel().setPreferredSize(new Dimension(480, 300));
	}

	//
	// Public Methods
	//

	@Override
	public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
			throws NotConfigurableException {
		super.loadAdditionalSettingsFrom(settings, specs);
		enableOrDisableSettings(m_modelFingerprintType.getValue());
	}

	//
	// Protected Methods
	//

	/**
	 * Returns an array of all fingerprint types that are supported for the fingerprint calculation.
	 * 
	 * @return Array of fingerprint types. The order will be used in the dialog combo box.
	 */
	protected abstract FingerprintType[] getSupportedFingerprintTypes();

	/**
	 * Adds a dialog component that can be hidden if the model it is based on gets disabled.
	 * It will only be shown, if the model is enabled.
	 * 
	 * @param dialogComponent Dialog component that can be hidden. Must not be null.
	 */
	protected void addHidableDialogComponent(final DialogComponent dialogComponent) {
		m_listHidableDialogComponents.add(dialogComponent);
		super.addDialogComponent(dialogComponent);
	}

	protected void enableOrDisableSettings(final FingerprintType fpType) {		
	final FingerprintSettings dummySettings = (fpType == null ? null : fpType.getSpecification(4, 1, 100, 1, 30, 2048, 5, 42, false, false, null, false));
		final boolean bCanCalculateRootedFingerprint = (fpType == null ? false : fpType.canCalculateRootedFingerprint());

		// Enable/disable rooted fingerprint settings
		m_modelRootedOption.setEnabled(bCanCalculateRootedFingerprint);
		m_modelAtomListColumn.setEnabled(bCanCalculateRootedFingerprint && m_modelRootedOption.getBooleanValue());
		m_modelAtomListHandlingOption.setEnabled(bCanCalculateRootedFingerprint && m_modelRootedOption.getBooleanValue());

		// Enable other settings
		m_modelNumBits.setEnabled(dummySettings.isAvailable(dummySettings.getNumBits())
				&& fpType != FingerprintType.maccs);
		m_modelRadius.setEnabled(dummySettings.isAvailable(dummySettings.getRadius()));
		m_modelTorsionPathLength.setEnabled(dummySettings.isAvailable(dummySettings.getTorsionPathLength()));
		m_modelMinPath.setEnabled(dummySettings.isAvailable(dummySettings.getMinPath()));
		m_modelMaxPath.setEnabled(dummySettings.isAvailable(dummySettings.getMaxPath()));
		m_modelAtomPairMinPath.setEnabled(dummySettings.isAvailable(dummySettings.getAtomPairMinPath()));
		m_modelAtomPairMaxPath.setEnabled(dummySettings.isAvailable(dummySettings.getAtomPairMaxPath()));
		m_modelLayerFlags.setEnabled(dummySettings.isAvailable(dummySettings.getLayerFlags()));

		// Show or hide components
		final NodeDialogPane dialogPane = this;
		final Runnable showOrHideComponents = new Runnable() {

			@Override
			public void run() {
				// Show or hide settings components
				for (final DialogComponent dialogComponent : m_listHidableDialogComponents) {
					dialogComponent.getComponentPanel().setVisible(dialogComponent.getModel().isEnabled());
				}

				// Show or hide rooted fingerprints group
				final JComponent panelRootedParams = LayoutUtils.findTitleGroupPanel(dialogPane, ROOTED_PARAMS_GROUP_NAME);
				if (panelRootedParams != null) {
					panelRootedParams.setVisible(bCanCalculateRootedFingerprint);
				}

				final JPanel panel = getPanel();
				panel.invalidate();
				panel.validate();
				panel.repaint();
			}
		};

		if (SwingUtilities.isEventDispatchThread()) {
			showOrHideComponents.run();
		}
		else {
			SwingUtilities.invokeLater(showOrHideComponents);
		}
	}

	//
	// Static Methods
	//

	/**
	 * @return settings model for smiles column selection
	 */
	static final SettingsModelString createSmilesColumnModel() {
		return new SettingsModelString("smiles_column", null);
	}

	/**
	 * @return settings model for the new appended column name
	 */
	static final SettingsModelString createNewColumnModel() {
		return new SettingsModelString("new_column_name", null);
	}

	/** @return settings model for check box whether to remove source columns. */
	static final SettingsModelBoolean createRemoveSourceColumnOptionModel() {
		return new SettingsModelBoolean("remove_source_columns", false);
	}

	/**
	 * @return settings model
	 */
	static final SettingsModelIntegerBounded createTorsionPathLengthModel() {
		return new SettingsModelIntegerBounded("torsion_path_length", DEFAULT_TORSION_PATH_LENGTH, 1, 10);
	}

	/**
	 * @return settings model
	 */
	static final SettingsModelIntegerBounded createMinPathModel() {
		return new SettingsModelIntegerBounded("min_path", DEFAULT_MIN_PATH, 1, 10);
	}

	/**
	 * @return settings model
	 */
	static final SettingsModelIntegerBounded createMaxPathModel() {
		return new SettingsModelIntegerBounded("max_path", DEFAULT_MAX_PATH, 1, 10);
	}

	/**
	 * @return settings model
	 */
	static final SettingsModelIntegerBounded createAtomPairMinPathModel() {
		return new SettingsModelIntegerBounded("atompairs_min_path", DEFAULT_ATOMPAIR_MIN_PATH, 1, 30);
	}

	/**
	 * @return settings model
	 */
	static final SettingsModelIntegerBounded createAtomPairMaxPathModel() {
		return new SettingsModelIntegerBounded("atompairs_max_path", DEFAULT_ATOMPAIR_MAX_PATH, 1, 30);
	}

	/**
	 * @return settings model
	 */
	static final SettingsModelIntegerBounded createRadiusModel() {
		return new SettingsModelIntegerBounded("radius", DEFAULT_RADIUS, 0, 6);
	}

	/**
	 * @return settings model
	 */
	static final SettingsModelIntegerBounded createLayerFlagsModel() {
		return new SettingsModelIntegerBounded("layer_flags", DEFAULT_LAYER_FLAGS,
				1, 0xFFFF);
	}

	/**
	 * @return settings model
	 */
	static final SettingsModelIntegerBounded createNumBitsModel() {
		return new SettingsModelIntegerBounded("num_bits", DEFAULT_NUM_BITS, 32, Integer.MAX_VALUE);
	}

	/**
	 * @return settings model
	 */
	static final SettingsModelEnumeration<FingerprintType> createFPTypeModel() {
		return new SettingsModelEnumeration<FingerprintType>(FingerprintType.class, "fp_type", FingerprintType.morgan);
	}

	/**
	 * @return settings model
	 */
	static final SettingsModelBoolean createUseChiralityModel() {
		return new SettingsModelBoolean("use_chirality", DEFAULT_USECHIRALITY_OPTION);
	}

	/**
	 * @return settings model
	 */
	static final SettingsModelBoolean createRootedOptionModel() {
		return new SettingsModelBoolean("is_rooted", DEFAULT_ROOTED_OPTION);
	}

	/**
	 * @return settings model for atom list column selection (for rooted fingerprints)
	 */
	static final SettingsModelString createAtomListColumnModel(
			final SettingsModelEnumeration<FingerprintType> modelFingerprintType,
			final SettingsModelBoolean modelRootedOption) {
		final SettingsModelString result = new SettingsModelString("atom_list_column", null);
		modelRootedOption.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				final FingerprintType fpType = modelFingerprintType.getValue();
				final boolean bCanCalculateRootedFingerprint = (fpType == null ? false : fpType.canCalculateRootedFingerprint());
				result.setEnabled(bCanCalculateRootedFingerprint && modelRootedOption.getBooleanValue());
			}
		});
		result.setEnabled(modelRootedOption.getBooleanValue());
		return result;
	}

	/**
	 * @return settings model for atom list handling (for rooted fingerprints)
	 */
	static final SettingsModelBoolean createAtomListHandlingIncludeOptionModel(
			final SettingsModelEnumeration<FingerprintType> modelFingerprintType,
			final SettingsModelBoolean modelRootedOption) {
		final SettingsModelBoolean result = new SettingsModelBoolean("include_atoms", DEFAULT_ATOM_LIST_INCLUDE_HANDLING);
		modelRootedOption.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				final FingerprintType fpType = modelFingerprintType.getValue();
				final boolean bCanCalculateRootedFingerprint = (fpType == null ? false : fpType.canCalculateRootedFingerprint());
				result.setEnabled(bCanCalculateRootedFingerprint && modelRootedOption.getBooleanValue());
			}
		});
		result.setEnabled(modelRootedOption.getBooleanValue());
		return result;
	}


}
