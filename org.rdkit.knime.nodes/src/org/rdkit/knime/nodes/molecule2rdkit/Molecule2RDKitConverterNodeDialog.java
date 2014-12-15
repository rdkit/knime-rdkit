/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2010
 * Novartis Institutes for BioMedical Research
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
package org.rdkit.knime.nodes.molecule2rdkit;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmartsValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.molecule2rdkit.Molecule2RDKitConverterNodeModel.ParseErrorPolicy;
import org.rdkit.knime.util.DialogComponentColumnNameSelection;

/**
 * The dialog to configure the RDKit node.
 * 
 * @author Greg Landrum
 * @author Manuel Schwarze
 */
public class Molecule2RDKitConverterNodeDialog extends DefaultNodeSettingsPane {

	//
	// Members
	//

	/** The model for the option to keep hydrogens (only valid for SDF input). */
	private SettingsModelBoolean m_modelKeepHs;

	/** The dialog component for picking input columns. */
	private DialogComponentColumnNameSelection m_compInputColumn;

	//
	// Constructor
	//

	/**
	 * Create a new dialog pane with default components to configure an input column,
	 * the name of a new column, which will contain the calculation results, an option
	 * to tell, if the source column shall be removed from the result table.
	 */
	Molecule2RDKitConverterNodeDialog() {
		final SettingsModelString modelInputColumn = createInputColumnNameModel();
		super.addDialogComponent(m_compInputColumn = new DialogComponentColumnNameSelection(
				modelInputColumn, "Molecule column: ", 0,
				SmilesValue.class, SmartsValue.class, SdfValue.class));
		modelInputColumn.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				updateOptionsAvailability();
			}
		});

		super.addDialogComponent(new DialogComponentString(
				createNewColumnNameModel(), "New column name: "));
		super.addDialogComponent(new DialogComponentBoolean(
				createRemoveSourceColumnsOptionModel(), "Remove source column"));

		super.createNewGroup("Error Handling");
		super.addDialogComponent(new DialogComponentButtonGroup(
				createSeparateRowsModel(), null, true,
				ParseErrorPolicy.values()));
		final SettingsModelBoolean generateErrorInformationColumn =
				createGenerateErrorInfoOptionModel();
		super.addDialogComponent(new DialogComponentBoolean(
				generateErrorInformationColumn, "Generate error information column"));
		super.addDialogComponent(new DialogComponentString(
				createErrorInfoColumnNameModel(generateErrorInformationColumn),
				"Error Information Column Name: "));

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
				m_modelKeepHs = createKeepHsOptionModel(), "Keep Hydrogens"));
		super.addDialogComponent(new DialogComponentBoolean(
				quickAndDirtyModel, "Partial Sanitization"));
		super.createNewGroup("Partial Sanitization Options");
		super.addDialogComponent(new DialogComponentBoolean(
				createAromatizationModel(quickAndDirtyModel), "Reperceive Aromaticity"));
		super.addDialogComponent(new DialogComponentBoolean(
				createStereochemistryModel(quickAndDirtyModel), "Correct Stereochemistry"));
		super.addDialogComponent(new DialogComponentLabel(""));
		super.closeCurrentGroup();
	}

	@Override
	public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
			final DataTableSpec[] specs) throws NotConfigurableException {
		super.loadAdditionalSettingsFrom(settings, specs);
		updateOptionsAvailability();
	}

	//
	// Protected Methods
	//

	/**
	 * Enables or disables the Keep Hydrogens option based on the selected input column.
	 * Only for SDF compatible input columns the Keep Hydrogens option will be selected.
	 */
	protected void updateOptionsAvailability() {
		final DataColumnSpec specInput = m_compInputColumn.getSelectedAsSpec();
		m_modelKeepHs.setEnabled(specInput != null && specInput.getType().isCompatible(SdfValue.class));
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
	 * Creates the radio button option how to deal with rows that fail
	 * the conversion. Either set a missing value (false) or put them into separate
	 * table at port 1 (true).
	 * 
	 * @return new settings model for the flag 'send bad rows to port1'
	 */
	static final SettingsModelString createSeparateRowsModel() {
		return new SettingsModelString("bad_rows_to_port1",
				ParseErrorPolicy.SPLIT_ROWS.getActionCommand());
	}

	/**
	 * Creates the checkbox option if an error column shall be added
	 * for rows that fail the conversion.
	 * 
	 * @return new settings model for the option to generate an error column.
	 */
	static final SettingsModelBoolean createGenerateErrorInfoOptionModel() {
		return new SettingsModelBoolean("generateErrorInfo", false);
	}

	/**
	 * Creates the model to specify a column name for the optional
	 * error information column. This option is dependent on the passed
	 * in model state.
	 * 
	 * @param modelGenerateErrorInfo Model that determines, if the
	 * 		error information column name model is enabled or disabled.
	 * 
	 * @return The error information column name model.
	 */
	static final SettingsModelString createErrorInfoColumnNameModel(
			final SettingsModelBoolean modelGenerateErrorInfo) {
		final SettingsModelString result =
				new SettingsModelString("errorInfoColumnName", "");
		modelGenerateErrorInfo.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				result.setEnabled(modelGenerateErrorInfo.getBooleanValue());
			}
		});
		result.setEnabled(modelGenerateErrorInfo.getBooleanValue());
		return result;
	}

	/**
	 * @return new settings model whether to also compute coordinates
	 */
	static final SettingsModelBoolean createGenerateCoordinatesModel() {
		return new SettingsModelBoolean("generateCoordinates", false);
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
				new SettingsModelBoolean("forceGenerateCoordinates", false);
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
		return new SettingsModelBoolean("skip_sanitization", false);
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
				new SettingsModelBoolean("do_aromaticity", true);
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
	 * @return The stereo chemistry option model.
	 */
	static final SettingsModelBoolean createStereochemistryModel(
			final SettingsModelBoolean quickAndDirtyModel) {
		final SettingsModelBoolean result =
				new SettingsModelBoolean("do_stereochem", true);
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
	 * Creates the model to select the option Keep Hydrogens.
	 * The default is false.
	 * 
	 * @return The Keep Hs option model.
	 */
	static final SettingsModelBoolean createKeepHsOptionModel() {
		return new SettingsModelBoolean("keepHs", false);
	}}
