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
package org.rdkit.knime.nodes.addconformers;

import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.DialogComponentColumnNameSelection;

/**
 * <code>NodeDialog</code> for the "RDKitAddConformers" Node.
 * Creates a new table with multiple conformers per input molecule. Each conformer is a copy of the molecule with different coordinates assigned. * nEach conformer row is mapped back to the input table molecule with an identifier - usually the row id - taken from an input table column.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Manuel Schwarze
 */
public class RDKitAddConformersNodeDialog extends DefaultNodeSettingsPane {

	//
	// Constants
	//

	/** Default value to be used for number of conformers. */
	public static final int DEFAULT_NUMBER_OF_CONFORMERS = 10;

	/** Default value to be used for maximum iterations. */
	public static final int DEFAULT_MAX_ITERATIONS = 30;

	/** Default value to be used as random seed. */
	public static final int DEFAULT_RANDOM_SEED = -1;

	/** Default value to be used to prune RMS threshold. */
	public static final double DEFAULT_PRUNE_RMS_THRESHOLD = -1.0d;

	/** Default value to be used as option to use random coordinates. */
	public static final boolean DEFAULT_USE_RANDOM_COORDS = false;

	/** Default value to be used as box size multiplier. */
	public static final double DEFAULT_BOX_SIZE_MULTIPLIER = 2.0d;

	//
	// Constructor
	//

	/**
	 * Create a new dialog pane with default components to configure an input column,
	 * the name of a new column, which will contain the calculation results, an option
	 * to tell, if the source column shall be removed from the result table.
	 */
	@SuppressWarnings("unchecked")
	RDKitAddConformersNodeDialog() {
		createNewGroup("Input");
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				createMoleculeInputColumnNameModel(), "RDKit Mol column: ", 0,
				RDKitMolValue.class));
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				createReferenceInputColumnNameModel(), "Reference column (e.g. an ID): ", 0,
				true, false, StringValue.class, DoubleValue.class));

		createNewGroup("Conformer Calculation");
		super.addDialogComponent(new DialogComponentNumber(
				createNumberOfConformersModel(), "Number of conformers: ", new Integer(1), 3));
		super.addDialogComponent(new DialogComponentNumber(
				createMaxIterationsModel(), "Maximum number of tries to generate conformers: ", new Integer(1), 3));
		super.addDialogComponent(new DialogComponentNumberEdit(
				createRandomSeedModel(), "Random seed: ", 10));
		super.addDialogComponent(new DialogComponentNumberEdit(
				createPruneRmsThresholdModel(), "RMS threshold for keeping a conformer: ", 10));

		createNewGroup("Output");
		super.addDialogComponent(new DialogComponentString(
				createMoleculeOutputColumnNameModel(), "Column name for molecules with conformers: ", true, 40));
		super.addDialogComponent(new DialogComponentString(
				createReferenceOutputColumnNameModel(), "Column name for copied reference data: ", true, 40));

		createNewTab("Advanced");
		createNewGroup("Advanced Conformer Calculation Options");
		super.addDialogComponent(new DialogComponentBoolean(
				createUseRandomCoordinatesOptionModel(), "Use random coordinates as a starting point instead of distance geometry"));
		super.addDialogComponent(new DialogComponentNumberEdit(
				createBoxSizeMultiplierModel(), "Multiplier for the size of the box for random coordinates: ", 10));
	}

	//
	// Static Methods
	//

	/**
	 * Creates the settings model to be used for the input column.
	 * 
	 * @return Settings model for input column selection.
	 */
	static final SettingsModelString createMoleculeInputColumnNameModel() {
		return new SettingsModelString("input_mol_column", null);
	}

	/**
	 * Creates the settings model to be used for the id column.
	 * 
	 * @return Settings model for id column selection.
	 */
	static final SettingsModelColumnName createReferenceInputColumnNameModel() {
		final SettingsModelColumnName model = new SettingsModelColumnName("input_ref_column", null);
		model.setSelection(null, true);
		return model;
	}

	/**
	 * Creates the settings model to be used to define the number of conformers
	 * to be calculated. Default is 10.
	 * 
	 * @return Settings model for number of conformers.
	 */
	static final SettingsModelInteger createNumberOfConformersModel() {
		return new SettingsModelInteger("numberConformers", DEFAULT_NUMBER_OF_CONFORMERS);
	}

	/**
	 * Creates the settings model for the option to specify
	 * maximum number of iterations. Default is 30.
	 * 
	 * @return Settings model for specifying iterations.
	 */
	static final SettingsModelInteger createMaxIterationsModel() {
		return new SettingsModelIntegerBounded("maxIterations", DEFAULT_MAX_ITERATIONS, 1, Integer.MAX_VALUE);
	}

	/**
	 * Creates the settings model for the random seed. Default is -1 (not set).
	 * 
	 * @return Settings model for specifying random seed value.
	 */
	static final SettingsModelInteger createRandomSeedModel() {
		return new SettingsModelInteger("seed", DEFAULT_RANDOM_SEED);
	}

	/**
	 * Creates the settings model for the prune RMS threshold value. Default is -1.0.
	 * 
	 * @return Settings model for prune RMS threshold value.
	 */
	static final SettingsModelDouble createPruneRmsThresholdModel() {
		return new SettingsModelDouble("pruneRmsThreshold", DEFAULT_PRUNE_RMS_THRESHOLD);
	}

	/**
	 * Creates the settings model for the option to use random coordinates. Default is false.
	 * 
	 * @return Settings model for option to use random coordinates.
	 */
	static final SettingsModelBoolean createUseRandomCoordinatesOptionModel() {
		return new SettingsModelBoolean("useRandomCoordinates", DEFAULT_USE_RANDOM_COORDS);
	}

	/**
	 * Creates the settings model for the multiplier for the size of the box for random coordinates. Default is 2.0.
	 * 
	 * @return Settings model for the multiplier for the size of the box for random coordinates.
	 */
	static final SettingsModelDoubleBounded createBoxSizeMultiplierModel() {
		return new SettingsModelDoubleBounded("boxSizeMultiplier", DEFAULT_BOX_SIZE_MULTIPLIER, Double.MIN_VALUE, Double.MAX_VALUE);
	}

	/**
	 * Creates the settings model for new column name to be used for the conformers molecule column.
	 * 
	 * @return Settings model for the new conformers molecule column name.
	 */
	static final SettingsModelString createMoleculeOutputColumnNameModel() {
		return new SettingsModelString("output_mol_name", null);
	}

	/**
	 * Creates the settings model for new column name to be used for the reference column.
	 * 
	 * @return Settings model for the new reference column name.
	 */
	static final SettingsModelString createReferenceOutputColumnNameModel() {
		return new SettingsModelString("output_ref_name", null);
	}

}
