/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2012-2023
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
package org.rdkit.knime.nodes.substructurecounter;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.chem.types.SmartsValue;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.DialogComponentColumnNameSelection;
import org.rdkit.knime.util.DialogComponentSeparator;
import org.rdkit.knime.util.LayoutUtils;

/**
 * <code>NodeDialog</code> for the "RDKitSubstructureCounter" Node.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Swarnaprava Singh
 * @author Manuel Schwarze
 */
public class SubstructureCounterNodeDialog extends DefaultNodeSettingsPane {

	//
	// Constructor
	//

	/**
	 * Create a new dialog pane with default components to configure an input column,
	 * the name of a new column, which will contain the calculation results, an option
	 * to tell, if the source column shall be removed from the result table.
	 */
	@SuppressWarnings("unchecked")
	SubstructureCounterNodeDialog() {
		createNewGroup("Input");
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				createInputColumnNameModel(), "RDKit Mol column: ", 0,
				RDKitMolValue.class));
		final Class<? extends DataValue>[] arrClassesQueryType = new Class[] { SmartsValue.class, RDKitMolValue.class };
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				createQueryInputModel(), "Input query column: ", 1,
				arrClassesQueryType));

		createNewGroup("Search");
		super.addDialogComponent(
				new DialogComponentBoolean(createUniqueMatchesOnlyModel(),
						"Count unique matches only"));
		final SettingsModelBoolean modelUseChiralityOption = createUseChiralityModel();
		super.addDialogComponent(
				new DialogComponentBoolean(modelUseChiralityOption,
						"Use chirality when matching"));
		super.addDialogComponent(
				new DialogComponentBoolean(createUseEnhancedStereoModel(modelUseChiralityOption),
						"Use enhanced stereochemistry when matching"));


		createNewGroup("Output");
		// Add query name option settings
		final SettingsModelBoolean modelUseQueryNameColumnOption = createUseQueryNameColumnModel();
		super.addDialogComponent(new DialogComponentBoolean(
				modelUseQueryNameColumnOption, "Instead of query molecules use names as result header titles (and tags)"));
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				createQueryNameColumnModel(modelUseQueryNameColumnOption), "Column with names for header titles: ", 1,
				StringValue.class));
		addDialogComponent(new DialogComponentSeparator());

		// Add total hits option settings
		final SettingsModelBoolean modelCountTotalHitsOption = createCountTotalHitsOptionModel();
		super.addDialogComponent(new DialogComponentBoolean(
				modelCountTotalHitsOption, "Add total hits count column"));
		super.addDialogComponent(new DialogComponentString(
				createCountTotalHitsColumnModel(modelCountTotalHitsOption), "New column name for total hits count: ", false,
				25));
		addDialogComponent(new DialogComponentSeparator());

		// Add query tags option settings
		final SettingsModelBoolean modelTrackQueryTagsOption = createTrackQueryTagsOptionModel();
		super.addDialogComponent(new DialogComponentBoolean(
				modelTrackQueryTagsOption, "Add column with tags for matching queries"));
		super.addDialogComponent(new DialogComponentString(
				createTrackQueryTagsColumnModel(modelTrackQueryTagsOption), "New column name for tags: ", false,
				25));

		LayoutUtils.correctKnimeDialogBorders(getPanel());
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
	 * Creates the settings model to be used to specify the query molecule column.
	 * 
	 * @return Settings model for query column selection.
	 */
	static final SettingsModelString createQueryInputModel() {
		return new SettingsModelString("inputQueryCol", null);
	}

	/**
	 * Creates the settings model to be used to specify the option
	 * to select only unique matches.
	 * 
	 * @return Settings model for unique matches option.
	 */
	static final SettingsModelBoolean createUniqueMatchesOnlyModel() {
		return new SettingsModelBoolean("countUniqueMatches", true);
	}

	/**
	 * Creates the settings model to be used to specify the option
	 * to use chirality in the matching.
	 * Added in November 2020.
	 * 
	 * @return Settings model for useChirality option.
	 */
	static final SettingsModelBoolean createUseChiralityModel() {
		return new SettingsModelBoolean("useChirality", false);
	}

	/**
	 * Creates the settings model to be used to specify the option
	 * to use enhanced stereo in the matching.
	 * Added in March 2021.
	 * 
	 * @return Settings model for useEnhancedStereo option.
	 */
	static final SettingsModelBoolean createUseEnhancedStereoModel(final SettingsModelBoolean modelUseChiralityOption) {
		final SettingsModelBoolean modelWithDependency = new SettingsModelBoolean("useEnhancedStereo", false);

		// React on any changes
		modelUseChiralityOption.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				// Enable or disable the model
				modelWithDependency.setEnabled(modelUseChiralityOption.getBooleanValue());
			}
		});

		// Enable this model based on the dependent model's state
		modelWithDependency.setEnabled(modelUseChiralityOption.getBooleanValue());

		return modelWithDependency;
	}

	/**
	 * Creates the settings model to be used to specify the option
	 * to use data of specific input table column as column names
	 * instead of using the SMILES or SMARTS string.
	 * 
	 * @return Settings model for using column names from input table option.
	 */
	static final SettingsModelBoolean createUseQueryNameColumnModel() {
		return new SettingsModelBoolean("useQueryNameColumn", false);
	}

	/**
	 * Creates the settings model to be used to specify the (optional)
	 * query name column.
	 * 
	 * @return Settings model for optional query name column selection.
	 */
	static final SettingsModelString createQueryNameColumnModel(final SettingsModelBoolean modelUseQueryNameColumnOption) {
		final SettingsModelString modelWithDependency = new SettingsModelString("queryNameColumn", null);

		// React on any changes
		modelUseQueryNameColumnOption.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				// Enable or disable the model
				modelWithDependency.setEnabled(modelUseQueryNameColumnOption.getBooleanValue());
			}
		});

		// Enable this model based on the dependent model's state
		modelWithDependency.setEnabled(modelUseQueryNameColumnOption.getBooleanValue());

		return modelWithDependency;
	}

	/**
	 * Creates the settings model to be used to specify the option
	 * to count the total hits of a substructure.
	 * 
	 * @return Settings model for counting total hits.
	 */
	static final SettingsModelBoolean createCountTotalHitsOptionModel() {
		return new SettingsModelBoolean("countTotalHits", false);
	}

	/**
	 * Creates the settings model to be used to specify the (optional)
	 * total hit count name column.
	 * 
	 * @return Settings model for total hit count column name.
	 */
	static final SettingsModelString createCountTotalHitsColumnModel(final SettingsModelBoolean modelCountTotalHitsOption) {
		final SettingsModelString modelWithDependency = new SettingsModelString("countTotalHitsColumn", null);

		// React on any changes
		modelCountTotalHitsOption.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				// Enable or disable the model
				modelWithDependency.setEnabled(modelCountTotalHitsOption.getBooleanValue());
			}
		});

		// Enable this model based on the dependent model's state
		modelWithDependency.setEnabled(modelCountTotalHitsOption.getBooleanValue());

		return modelWithDependency;
	}


	/**
	 * Creates the settings model to be used to specify the option
	 * to track query tags for matching substructures.
	 * 
	 * @return Settings model for using column names from input table option.
	 */
	static final SettingsModelBoolean createTrackQueryTagsOptionModel() {
		return new SettingsModelBoolean("trackQueryTags", false);
	}

	/**
	 * Creates the settings model to be used to specify the (optional)
	 * total hit count name column.
	 * 
	 * @return Settings model for optional query name column selection.
	 */
	static final SettingsModelString createTrackQueryTagsColumnModel(final SettingsModelBoolean modelTrackQueryTagsOption) {
		final SettingsModelString modelWithDependency = new SettingsModelString("trackQueryTagsColumn", null);

		// React on any changes
		modelTrackQueryTagsOption.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				// Enable or disable the model
				modelWithDependency.setEnabled(modelTrackQueryTagsOption.getBooleanValue());
			}
		});

		// Enable this model based on the dependent model's state
		modelWithDependency.setEnabled(modelTrackQueryTagsOption.getBooleanValue());

		return modelWithDependency;
	}
}
