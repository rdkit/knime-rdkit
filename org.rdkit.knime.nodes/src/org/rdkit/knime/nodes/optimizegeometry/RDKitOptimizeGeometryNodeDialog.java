/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2013
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
package org.rdkit.knime.nodes.optimizegeometry;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.DialogComponentColumnNameSelection;
import org.rdkit.knime.util.DialogComponentEnumSelection;
import org.rdkit.knime.util.SettingsModelEnumeration;

/**
 * <code>NodeDialog</code> for the "RDKitOptimizeGeometry" Node.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Manuel Schwarze
 */
public class RDKitOptimizeGeometryNodeDialog extends DefaultNodeSettingsPane {

	//
	// Constants
	//

	/** The default force field to be used. */
	public static final ForceFieldType DEFAULT_FORCE_FIELD = ForceFieldType.MMFF94;

	/** Default value to be used for the advanced option iterations. */
	public static final int DEFAULT_ITERATIONS = 1000;

	//
	// Constructor
	//

	/**
	 * Create a new dialog pane with default components to configure an input column,
	 * the name of a new column, which will contain the calculation results, an option
	 * to tell, if the source column shall be removed from the result table.
	 */
	RDKitOptimizeGeometryNodeDialog() {
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				createInputColumnNameModel(), "RDKit Mol column: ", 0,
				RDKitMolValue.class));
		super.addDialogComponent(new DialogComponentEnumSelection<ForceFieldType>(
				createForceFieldModel(), "Force field: "));
		super.addDialogComponent(new DialogComponentString(
				createNewMoleculeColumnNameModel(), "New column name for optimized molecule: "));
		super.addDialogComponent(new DialogComponentBoolean(
				createRemoveSourceColumnsOptionModel(), "Remove source column"));
		super.addDialogComponent(new DialogComponentString(
				createNewConvergeColumnNameModel(), "New column name for converge information: "));
		super.addDialogComponent(new DialogComponentString(
				createNewEnergyColumnNameModel(), "New column name for energy information: "));

		super.createNewTab("Advanced");
		super.addDialogComponent(new DialogComponentNumberEdit(createIterationsModel(), "Iterations: ", 8));
		super.addDialogComponent(new DialogComponentBoolean(
				createRemoveStartingCoordinatesOptionModel(), "Remove starting coordinates before optimizing the molecule"));
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
	 * Creates the settings model to specify the force field to be used.
	 * 
	 * @return Settings model for the force field.
	 */
	static final SettingsModelEnumeration<ForceFieldType> createForceFieldModel() {
		return new SettingsModelEnumeration<ForceFieldType>(ForceFieldType.class, "forceField", DEFAULT_FORCE_FIELD);
	}

	/**
	 * Creates the settings model to be used to specify the new column name
	 * taking the updated molecule.
	 * 
	 * @return Settings model for result column name.
	 */
	static final SettingsModelString createNewMoleculeColumnNameModel() {
		return new SettingsModelString("new_molecule_column_name", null);
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
	 * Creates the settings model to be used to specify the new converge column name.
	 * 
	 * @return Settings model for result column name taking the converge information.
	 */
	static final SettingsModelString createNewConvergeColumnNameModel() {
		return new SettingsModelString("new_converge_column_name", null);
	}

	/**
	 * Creates the settings model to be used to specify the new energy column name.
	 * 
	 * @return Settings model for result column name taking the energy information.
	 */
	static final SettingsModelString createNewEnergyColumnNameModel() {
		return new SettingsModelString("new_energy_column_name", null);
	}

	/**
	 * Creates the settings model for the advanced options to specify
	 * iterations. Default is 1000.
	 * 
	 * @return Settings model for specifying iterations.
	 */
	static final SettingsModelInteger createIterationsModel() {
		return new SettingsModelIntegerBounded("iterations", DEFAULT_ITERATIONS, 1, Integer.MAX_VALUE);
	}

	/**
	 * Creates the settings model for the boolean flag to determine, if
	 * starting coordinates shall be removed before optimizing the molecule geometry.
	 * 
	 * @return Settings model for check box whether to remove starting coordinates.
	 */
	static final SettingsModelBoolean createRemoveStartingCoordinatesOptionModel() {
		return new SettingsModelBoolean("remove_starting_coordinates", false);
	}

}
