/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2019
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
package org.rdkit.knime.nodes.rgroupdecomposition;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.RDKit.RGroupDecompositionParameters;
import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmartsValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentMultiLineString;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.rdkit.knime.nodes.AbstractRDKitNodeSettingsPane;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.DialogComponentColumnNameSelection;
import org.rdkit.knime.util.DialogComponentEnumFilterPanel;
import org.rdkit.knime.util.DialogComponentEnumSelection;
import org.rdkit.knime.util.DialogComponentSeparator;
import org.rdkit.knime.util.SettingsModelEnumeration;
import org.rdkit.knime.util.SettingsModelEnumerationArray;

/**
 * <code>NodeDialog</code> for the "RDKitRGroups" Node.
 * 
 * @author Manuel Schwarze
 */
public class RDKitRGroupDecompositionNodeDialog extends AbstractRDKitNodeSettingsPane {
	
	//
	// Constants
	//
	
	/** Default RGroup Decomposition Parameters as defined by the RDKit. */	
	public static final RGroupDecompositionParameters DEFAULT_RGROUP_DECOMPOSITION_PARAMETERS = 
			new RGroupDecompositionParameters(); // Will not be cleaned up
	
	//
	// Members
	//
	
	/** Setting model component for the additional column selector. */
	private final DialogComponent m_compCoreInputColumnName;

	/** Setting model component for the cores input text field. */
	private final DialogComponent m_compCoreInputTextField;
	
	/** Setting model component for the hint label to connect a second table. */
	private final DialogComponent m_compHintToConnectSecondTable;
	
	//
	// Constructor
	//

	/**
	 * Create a new dialog pane with default components to configure an input column,
	 * the name of a new column, which will contain the calculation results, an option
	 * to tell, if the source column shall be removed from the result table.
	 */
	RDKitRGroupDecompositionNodeDialog() {
		super.createNewGroup("Input Molecules");
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				createInputColumnNameModel(), "RDKit Mol column: ", 0,
				RDKitMolValue.class));
		super.createNewGroup("Input Scaffolds");
		super.addDialogComponent(m_compHintToConnectSecondTable = new DialogComponentLabel(
				"<html><body>You may connect a second input table with molecules to be used as cores<br>or you specify one or more cores in the text field below.</body></html>"));
		super.addDialogComponent(m_compCoreInputColumnName = new DialogComponentColumnNameSelection(
				createCoresInputColumnNameModel(), "Core Input Column: ", 1, false, false, 
				RDKitMolValue.class, SmilesValue.class, SmartsValue.class, SdfValue.class) {

			/**
			 * Hides or shows the optional components depending on
			 * the existence of a second input table.
			 */
			@Override
			protected void checkConfigurabilityBeforeLoad(
					final PortObjectSpec[] specs)
							throws NotConfigurableException {

				final boolean bHasAdditionalInputTable =
						RDKitRGroupDecompositionNodeModel.hasAdditionalInputTable(specs);

				// Only check correctness of second input table if it is there
				if (bHasAdditionalInputTable) {
					super.checkConfigurabilityBeforeLoad(specs);
				}

				// Always show or hide proper components
				updateVisibilityOfOptionalComponents(bHasAdditionalInputTable);
			}
		});
		super.addDialogComponent(m_compCoreInputTextField = new DialogComponentMultiLineString(
				createSmartsModel(), "Core SMARTS (separate by new line to specify multiple cores): ", 
				false, 50, 5));
		m_compCoreInputTextField.getComponentPanel().setBorder(BorderFactory.createEmptyBorder(10,  10,  10, 10));
		
		super.createNewGroup("Output Handling");
		
		super.setHorizontalPlacement(false);
		SettingsModelBoolean modelAddMatchingSmartsCore = createAddMatchingSmartsCoreModel();
		super.setHorizontalPlacement(true);
		super.addDialogComponent(new DialogComponentBoolean(
				modelAddMatchingSmartsCore, "Add matching SMARTS core"));
		super.addDialogComponent(new DialogComponentString(
				createNewMatchingSmartsCoreColumnNameModel(modelAddMatchingSmartsCore), "Core column name: ", false, 20));	
		super.setHorizontalPlacement(false);
		super.setHorizontalPlacement(true);

		super.addDialogComponent(new DialogComponentSeparator());

		super.setHorizontalPlacement(false);
		SettingsModelBoolean modelAddMatchingSubstructure = createAddMatchingSubstructureModel();
		super.setHorizontalPlacement(true);
		super.addDialogComponent(new DialogComponentBoolean(
				modelAddMatchingSubstructure, "Add matching substructure"));
		super.addDialogComponent(new DialogComponentString(
				createNewMatchingSubstructureColumnNameModel(modelAddMatchingSubstructure), "Substructure column name: ", false, 20));
		super.setHorizontalPlacement(false);
		super.setHorizontalPlacement(true);
		super.addDialogComponent(new DialogComponentBoolean(
				createUseAtomMapsModel(modelAddMatchingSubstructure), "Use atom maps"));
		super.addDialogComponent(new DialogComponentBoolean(
				createUseRLabelsModel(modelAddMatchingSubstructure), "Use R-labels"));
		super.setHorizontalPlacement(false);
		super.setHorizontalPlacement(true);
		
		super.addDialogComponent(new DialogComponentSeparator());

		super.setHorizontalPlacement(false);
		super.setHorizontalPlacement(true);
		super.addDialogComponent(new DialogComponentBoolean(
				createRemoveEmptyColumnsModel(), "Remove empty Rx columns"));
		super.addDialogComponent(new DialogComponentBoolean(
				createFailForNoMatchOptionModel(), "Fail if no cores are matching at all"));
		
		super.createNewTab("Advanced");
		
		super.setHorizontalPlacement(false);
		DialogComponentEnumFilterPanel<Labels> dcLabels = 
				new DialogComponentEnumFilterPanel<Labels>(createLabelsModel(), 
						"Labels to recognize R-Groups in scaffolds:", null, false /* disallow no selection */); 
		dcLabels.setIncludeTitle("Enabled");
		dcLabels.setExcludeTitle("Disabled");
		dcLabels.setSearchVisible(false);
		dcLabels.getComponentPanel().setPreferredSize(new Dimension(450,170));
		dcLabels.getComponentPanel().setMinimumSize(new Dimension(450,170));
		dcLabels.getComponentPanel().setMaximumSize(new Dimension(450,170));
		super.addDialogComponent(dcLabels);
		
		super.addDialogComponent(new DialogComponentEnumSelection<Matching>(createMatchingStrategyModel(), "Matching strategy:"));

		DialogComponentEnumFilterPanel<Labeling> dcLabeling = 
				new DialogComponentEnumFilterPanel<Labeling>(createLabelingModel(), 
						"Labeling for R-Groups output:", null, true /* no selection is allowed */);
		dcLabeling.setIncludeTitle("Enabled");
		dcLabeling.setExcludeTitle("Disabled");
		dcLabeling.setSearchVisible(false);
		dcLabeling.setButtonVisible(DialogComponentEnumFilterPanel.BUTTON_ADD_ALL, false);
		dcLabeling.setButtonVisible(DialogComponentEnumFilterPanel.BUTTON_REMOVE_ALL, false);
		dcLabeling.getComponentPanel().setPreferredSize(new Dimension(450,120));
		dcLabeling.getComponentPanel().setMinimumSize(new Dimension(450,120));
		dcLabeling.getComponentPanel().setMaximumSize(new Dimension(450,120));
		super.addDialogComponent(dcLabeling);

		super.addDialogComponent(new DialogComponentEnumSelection<CoreAlignment>(createCoreAlignmentModel(), "Core alignment:"));
		super.addDialogComponent(new DialogComponentBoolean(createMatchOnlyAtRGroupsModel(), "Match only at R-Groups"));
		super.setHorizontalPlacement(true);
		super.addDialogComponent(new DialogComponentBoolean(createRemoveHydrogenOnlyRGroupsModel(), "Remove hydrogen only R-Groups"));
		super.addDialogComponent(new DialogComponentBoolean(createRemoveHydrogensPostMatchModel(), "Remove hydrogens post match"));
}

	//
	// Protected Methods
	//
	
	/**
	 * Show or hides additional input based settings based on availability of the
	 * optional second input table.
	 * 
	 * @param bHasAdditionalInputTable
	 */
	protected void updateVisibilityOfOptionalComponents(final boolean bHasAdditionalInputTable) {
		m_compHintToConnectSecondTable.getComponentPanel().setVisible(!bHasAdditionalInputTable);
		m_compCoreInputColumnName.getComponentPanel().setVisible(bHasAdditionalInputTable);
		m_compCoreInputTextField.getComponentPanel().setVisible(!bHasAdditionalInputTable);
	}

	//
	// Static Methods
	//

	/**
	 * Creates the settings model to be used for the input column.
	 * 
	 * @return Settings model for input column selection.
	 */
	static final SettingsModelString createInputColumnNameModel() {
		return new SettingsModelString("input_column", null);
	}

	/**
	 * Creates the settings model to be used for the cores input column
	 * in the optional second ionput table.
	 * 
	 * @return Settings model for cores input column selection.
	 */
	static final SettingsModelString createCoresInputColumnNameModel() {
		return new SettingsModelString("cores_input_column", null);
	}

	/**
	 * Creates the settings model to be used as core SMARTS.
	 * 
	 * @return Settings model for the core SMARTS value.
	 */
	static final SettingsModelString createSmartsModel() {
		return new SettingsModelString("smarts_value", "");
	}

	/**
	 * Creates the settings model for the option to remove empty Rx columns.
	 * 
	 * @return Settings model for removing empty Rx columns.
	 */
	static final SettingsModelBoolean createRemoveEmptyColumnsModel() {
		return new SettingsModelBoolean("remove_empty_columns", true);
	}
	
	/**
	 * Creates the settings model for the option to let the node fail, if there is match at all.
	 * 
	 * @return Settings model for the option to let the node fail, if there is match at all.
	 */
	static final SettingsModelBoolean createFailForNoMatchOptionModel() {
		return new SettingsModelBoolean("fail_for_no_match", true);
	}
	
	/**
	 * Creates the settings model for the option to add the matching SMARTS core.
	 * 
	 * @return Settings model for the option to add the matching SMARTS core.
	 */
	static final SettingsModelBoolean createAddMatchingSmartsCoreModel() {
		return new SettingsModelBoolean("add_matching_smarts_core", true);
	}
	
	/**
	 * Creates the settings model to be used to specify the new matching SMARTS core column name.
	 * 
	 * @return Settings model for the new matching SMARTS core column name.
	 */
	static final SettingsModelString createNewMatchingSmartsCoreColumnNameModel(
			final SettingsModelBoolean modelAddMatchingSmartsCore) {
		final SettingsModelString result =
				new SettingsModelString("new_matching_smarts_core_column_name", "Core");
		modelAddMatchingSmartsCore.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				result.setEnabled(modelAddMatchingSmartsCore.getBooleanValue());
			}
		});
		result.setEnabled(modelAddMatchingSmartsCore.getBooleanValue());
		return result;
	}
	
	/**
	 * Creates the settings model for the option to generate the matching core.
	 * 
	 * @return Settings model for the option to generate the matching core.
	 */
	static final SettingsModelBoolean createAddMatchingSubstructureModel() {
		return new SettingsModelBoolean("add_matching_substructure", false);
	}
	
	/**
	 * Creates the settings model to be used to specify the new matching substructure column name.
	 * 
	 * @return Settings model for the new matching substructure column name.
	 */
	static final SettingsModelString createNewMatchingSubstructureColumnNameModel(
			final SettingsModelBoolean modelAddMatchingSubstructure) {
		final SettingsModelString result =
				new SettingsModelString("new_matching_substructure_column_name", "Substructure");
		modelAddMatchingSubstructure.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				result.setEnabled(modelAddMatchingSubstructure.getBooleanValue());
			}
		});
		result.setEnabled(modelAddMatchingSubstructure.getBooleanValue());
		return result;
	}
	
	/**
	 * Creates the settings model for the option to use atom maps when generating the matching substructure.
	 * 
	 * @return Settings model for the option to use atom maps when generating the matching substructure.
	 */
	static final SettingsModelBoolean createUseAtomMapsModel(
			final SettingsModelBoolean modelAddMatchingSubstructure) {
		final SettingsModelBoolean result =
				new SettingsModelBoolean("use_atom_maps_for_substructure", true);
		modelAddMatchingSubstructure.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				result.setEnabled(modelAddMatchingSubstructure.getBooleanValue());
			}
		});
		result.setEnabled(modelAddMatchingSubstructure.getBooleanValue());
		return result;
	}
	
	/**
	 * Creates the settings model for the option to use R labels when generating the matching substructure.
	 * 
	 * @return Settings model for the option to use R labels when generating the matching substructure.
	 */
	static final SettingsModelBoolean createUseRLabelsModel(
			final SettingsModelBoolean modelAddMatchingSubstructure) {
		final SettingsModelBoolean result =
				new SettingsModelBoolean("use_r_labels_for_substructure", true);
		modelAddMatchingSubstructure.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				result.setEnabled(modelAddMatchingSubstructure.getBooleanValue());
			}
		});
		result.setEnabled(modelAddMatchingSubstructure.getBooleanValue());
		return result;
	}
	
	/**
	 * Creates the settings model for the R-Group labels options.
	 * 
	 * @return Settings model for R-Group labels options.
	 */
	static final SettingsModelEnumerationArray<Labels> createLabelsModel() {
		return new SettingsModelEnumerationArray<>(Labels.class, "labels", new Labels[] { Labels.AutoDetect });
	}
	
	/**
	 * Creates the settings model for the R-Group matching strategy option.
	 * 
	 * @return Settings model for R-Group matching strategy.
	 */
	static final SettingsModelEnumeration<Matching> createMatchingStrategyModel() {
		Matching defaultValue = Matching.getValue(DEFAULT_RGROUP_DECOMPOSITION_PARAMETERS.getMatchingStrategy());
		return new SettingsModelEnumeration<>(Matching.class, "matching_strategy", 
				defaultValue == null ? Matching.GreedyChunks : defaultValue);
	}
	
	/**
	 * Creates the settings model for the R-Group labeling options.
	 * 
	 * @return Settings model for R-Group labeling options.
	 */
	static final SettingsModelEnumerationArray<Labeling> createLabelingModel() {
		return new SettingsModelEnumerationArray<>(Labeling.class, "labeling", new Labeling[] { Labeling.AtomMap, Labeling.MDLRGroup});
	}

	/**
	 * Creates the settings model for the R-Group core alignment option.
	 * 
	 * @return Settings model for R-Group core alignment.
	 */
	static final SettingsModelEnumeration<CoreAlignment> createCoreAlignmentModel() {
		CoreAlignment defaultValue = CoreAlignment.getValue(DEFAULT_RGROUP_DECOMPOSITION_PARAMETERS.getAlignment());
		return new SettingsModelEnumeration<>(CoreAlignment.class, "core_alignment", 
				defaultValue == null ? CoreAlignment.MCS : defaultValue);
	}

	/**
	 * Creates the settings model for the option to only match R-Groups.
	 * 
	 * @return Settings model for the option to only match R-Groups.
	 */
	static final SettingsModelBoolean createMatchOnlyAtRGroupsModel() {
		return new SettingsModelBoolean("match_only_at_rgroups", 
				DEFAULT_RGROUP_DECOMPOSITION_PARAMETERS.getOnlyMatchAtRGroups());
	}

	/**
	 * Creates the settings model for the option to remove hydrogen only R-Groups.
	 * 
	 * @return Settings model for the option to remove hydrogen only R-Groups.
	 */
	static final SettingsModelBoolean createRemoveHydrogenOnlyRGroupsModel() {
		return new SettingsModelBoolean("remove_hydrogen_only_rgroups", 
				DEFAULT_RGROUP_DECOMPOSITION_PARAMETERS.getRemoveAllHydrogenRGroups());
	}

	/**
	 * Creates the settings model for the option to remove hydrogens post match.
	 * 
	 * @return Settings model for the option to remove hydrogens post match.
	 */
	static final SettingsModelBoolean createRemoveHydrogensPostMatchModel() {
		return new SettingsModelBoolean("remove_hydrogens_post_match", 
				DEFAULT_RGROUP_DECOMPOSITION_PARAMETERS.getRemoveHydrogensPostMatch());
	}
}
