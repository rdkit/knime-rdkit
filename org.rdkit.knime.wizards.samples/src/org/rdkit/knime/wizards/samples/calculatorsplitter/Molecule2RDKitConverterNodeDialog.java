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
package org.rdkit.knime.wizards.samples.calculatorsplitter;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.util.DialogComponentColumnNameSelection;

/**
 * The dialog to configure the RDKit node.
 * 
 * @author Greg Landrum
 * @author Manuel Schwarze
 */
public class Molecule2RDKitConverterNodeDialog extends DefaultNodeSettingsPane {

	//
	// Constructor
	//

	/**
	 * Create a new dialog pane with default components to configure an input column,
	 * the name of a new column, which will contain the calculation results, an option
	 * to tell, if the source column shall be removed from the result table.
	 */
	Molecule2RDKitConverterNodeDialog() {
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				createInputColumnNameModel(), "Molecule column: ", 0,
				SmilesValue.class, SdfValue.class));
		super.addDialogComponent(new DialogComponentString(
				createNewColumnNameModel(), "New column name: "));
		super.addDialogComponent(new DialogComponentBoolean(
				createRemoveSourceColumnsOptionModel(), "Remove source column"));

		super.addDialogComponent(new DialogComponentButtonGroup(
				createSeparateRowsModel(), "Error Handling", true,
				ParseErrorPolicy.values()));

		super.createNewGroup("2D Coordinates");
		final SettingsModelBoolean generateCoordinatesModel =
				createGenerateCoordinatesModel();
		super.addDialogComponent(new DialogComponentBoolean(
				generateCoordinatesModel, "Generate 2D Coordinates"));
		super.addDialogComponent(new DialogComponentBoolean(
				createForceGenerateCoordinatesModel(generateCoordinatesModel),
				"Force Generation"));
		super.closeCurrentGroup();

		super.createNewTab("Advanced");
		final SettingsModelBoolean quickAndDirtyModel = createQuickAndDirtyModel();
		super.addDialogComponent(new DialogComponentBoolean(
				quickAndDirtyModel, "Partial Santization"));
		super.createNewGroup("Partial Sanitization Options");
		super.addDialogComponent(new DialogComponentBoolean(
				createAromatizationModel(quickAndDirtyModel), "Reperceive Aromaticity"));
		super.addDialogComponent(new DialogComponentBoolean(
				createStereochemistryModel(quickAndDirtyModel), "Correct Stereochemistry"));
		super.closeCurrentGroup();
	}

	/**
	 * Creates the settings model to be used for the input column.
	 * 
	 * @return Settings model for input column selection.
	 */
	static final SettingsModelString createInputColumnNameModel() {
		return new SettingsModelString("input_column", null);
	}

	/**
	 * Creates the settings model to be used to specify the new column name.
	 * 
	 * @return Settings model for result column name.
	 */
	static final SettingsModelString createNewColumnNameModel() {
		return new SettingsModelString("new_column_name", null);
	}

	/**
	 * Creates the settings model for the boolean flag to determine, if
	 * the source column shall be removed from the result table.
	 * The default is false.
	 * 
	 * @return Settings model for check box whether to remove source columns.
	 */
	static final SettingsModelBoolean createRemoveSourceColumnsOptionModel() {
		return new SettingsModelBoolean("remove_source_columns", false);
	}

	/**
	 * @return new settings model for the flag 'send bad rows to port1'
	 */
	static final SettingsModelString createSeparateRowsModel() {
		return new SettingsModelString("bad_rows_to_port1",
				ParseErrorPolicy.SPLIT_ROWS.getActionCommand());
	}

	/**
	 * @return new settings model whether to also compute coordinates
	 */
	static final SettingsModelBoolean createGenerateCoordinatesModel() {
		return new SettingsModelBoolean("generateCoordinates", false) {

			/**
			 * Overridden to catch invalid settings, because this
			 * setting was added after version 1.0.
			 */
			@Override
			protected void loadSettingsForModel(final NodeSettingsRO settings)
					throws InvalidSettingsException {
				try {
					super.loadSettingsForModel(settings);
				}
				catch (final InvalidSettingsException exc) {
					// Added after 1.0 - Ignore and use default
				}
			}
		};
	}

	/**
	 * @param generateCoordinatesModel
	 * The other model (to enable/disable the returned model).
	 * @return new settings model whether to also force coordinate generation
	 * (SDF may already have coordinates).
	 */
	static final SettingsModelBoolean createForceGenerateCoordinatesModel(
			final SettingsModelBoolean generateCoordinatesModel) {
		final SettingsModelBoolean result =
				new SettingsModelBoolean("forceGenerateCoordinates", false)  {

			/**
			 * Overridden to catch invalid settings, because this
			 * setting was added after version 1.0.
			 */
			@Override
			protected void loadSettingsForModel(final NodeSettingsRO settings)
					throws InvalidSettingsException {
				try {
					super.loadSettingsForModel(settings);
				}
				catch (final InvalidSettingsException exc) {
					// Added after 1.0 - Ignore and use default
				}
			}
		};
		generateCoordinatesModel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				result.setEnabled(generateCoordinatesModel.getBooleanValue());
			}
		});
		result.setEnabled(generateCoordinatesModel.getBooleanValue());
		return result;
	}

	/**
	 * @return settings model for check box whether to turn off sanitization
	 */
	static final SettingsModelBoolean createQuickAndDirtyModel() {
		return new SettingsModelBoolean("skip_santization", false) {

			/**
			 * Overridden to catch invalid settings, because this
			 * setting was added after version 1.0.
			 */
			@Override
			protected void loadSettingsForModel(final NodeSettingsRO settings)
					throws InvalidSettingsException {
				try {
					super.loadSettingsForModel(settings);
				}
				catch (final InvalidSettingsException exc) {
					// Added after 1.0 - Ignore and use default
				}
			}
		};
	}

	/**
	 * Creates the model to select the option Aromatization.
	 * This option is dependent on the passed in model state.
	 * 
	 * @param quickAndDirtyModel Model that determines, if the
	 * Aromatization option is enabled or disabled.
	 * 
	 * @return The Aromatization option model.
	 */
	static final SettingsModelBoolean createAromatizationModel(
			final SettingsModelBoolean quickAndDirtyModel) {
		final SettingsModelBoolean result =
				new SettingsModelBoolean("do_aromaticity", true) {

			/**
			 * Overridden to catch invalid settings, because this
			 * setting was added after version 1.0.
			 */
			@Override
			protected void loadSettingsForModel(final NodeSettingsRO settings)
					throws InvalidSettingsException {
				try {
					super.loadSettingsForModel(settings);
				}
				catch (final InvalidSettingsException exc) {
					// Added after 1.0 - Ignore and use default
				}
			}
		};
		quickAndDirtyModel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				result.setEnabled(quickAndDirtyModel.getBooleanValue());
			}
		});
		result.setEnabled(quickAndDirtyModel.getBooleanValue());
		return result;
	}

	/**
	 * Creates the model to select the option StereoChemistry.
	 * This option is dependent on the passed in model state.
	 * 
	 * @param quickAndDirtyModel Model that determines, if the
	 * StereoChemistry option is enabled or disabled.
	 * 
	 * @return The Aromatization option model.
	 */
	static final SettingsModelBoolean createStereochemistryModel(
			final SettingsModelBoolean quickAndDirtyModel) {
		final SettingsModelBoolean result =
				new SettingsModelBoolean("do_stereochem", false) {

			/**
			 * Overridden to catch invalid settings, because this
			 * setting was added after version 1.0.
			 */
			@Override
			protected void loadSettingsForModel(final NodeSettingsRO settings)
					throws InvalidSettingsException {
				try {
					super.loadSettingsForModel(settings);
				}
				catch (final InvalidSettingsException exc) {
					// Added after 1.0 - Ignore and use default
				}
			}
		};
		quickAndDirtyModel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				result.setEnabled(quickAndDirtyModel.getBooleanValue());
			}
		});
		result.setEnabled(quickAndDirtyModel.getBooleanValue());
		return result;
	}
}
