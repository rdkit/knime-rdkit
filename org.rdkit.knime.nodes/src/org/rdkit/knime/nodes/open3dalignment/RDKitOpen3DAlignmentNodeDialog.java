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
package org.rdkit.knime.nodes.open3dalignment;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.DialogComponentColumnNameSelection;

/**
 * <code>NodeDialog</code> for the "RDKitOpen3DAlignment" Node.
 * 
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Manuel Schwarze
 */
public class RDKitOpen3DAlignmentNodeDialog extends DefaultNodeSettingsPane {

	//
	// Constants
	//

	/** The default value for allowing reflection. Set to false. */
	public static final boolean DEFAULT_ALLOW_REFLECTION = false;

	/** The default value for maximum iterations. Set to 50. */
	public static final int DEFAULT_MAX_ITERATIONS = 50;

	/** The default accuracy value to be used. Set to 0. */
	public static final int DEFAULT_ACCURACY = 0;

	//
	// Constructor
	//

	/**
	 * Create a new dialog pane with default components to configure an input column,
	 * the name of a new column, which will contain the calculation results, an option
	 * to tell, if the source column shall be removed from the result table.
	 */
	RDKitOpen3DAlignmentNodeDialog() {
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				createQueryInputColumnNameModel(),
				"Query RDKit Mol column (table 1): ", 0,
				RDKitMolValue.class));
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				createReferenceInputColumnNameModel(),
				"Reference RDKit Mol column (table 2): ", 1,
				RDKitMolValue.class));
		super.addDialogComponent(new DialogComponentString(
				createNewAlignedColumnNameModel(),
				"New column name for aligned molecule: "));
		super.addDialogComponent(new DialogComponentBoolean(
				createRemoveSourceColumnsOptionModel(),
				"Remove source column"));
		super.addDialogComponent(new DialogComponentString(
				createNewRefIdColumnNameModel(),
				"New column name for Row IDs of reference used molecule: "));
		super.addDialogComponent(new DialogComponentString(
				createNewRmsdColumnNameModel(),
				"New column name for RMSD information: "));
		super.addDialogComponent(new DialogComponentString(
				createNewScoreColumnNameModel(),
				"New column name for score information: "));

		super.createNewTab("Advanced");
		super.addDialogComponent(new DialogComponentBoolean(createAllowReflectionModel(),
				"Allow reflection"));
		super.addDialogComponent(new DialogComponentNumberEdit(createMaxIterationsModel(),
				"Maximal number of iterations: ", 8));
		super.addDialogComponent(new DialogComponentNumberEdit(createAccuracyModel(),
				"Accuracy (0 - most accurate, 3 - least accurate): ", 8));
	}

	//
	// Static Methods
	//

	/**
	 * Creates the settings model to be used for the input column.
	 * 
	 * @return Settings model for input column selection.
	 */
	static final SettingsModelString createQueryInputColumnNameModel() {
		return new SettingsModelString("input_query_column", null);
	}

	/**
	 * Creates the settings model to be used for the input column.
	 * 
	 * @return Settings model for input column selection.
	 */
	static final SettingsModelString createReferenceInputColumnNameModel() {
		return new SettingsModelString("input_reference_column", null);
	}

	/**
	 * Creates the settings model to be used to specify the new column name.
	 * 
	 * @return Settings model for result column name.
	 */
	static final SettingsModelString createNewAlignedColumnNameModel() {
		return new SettingsModelString("new_aligned_column_name", null);
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
	 * Creates the settings model to be used to specify the new reference id column name.
	 * 
	 * @return Settings model for result column name taking the reference id information.
	 */
	static final SettingsModelString createNewRefIdColumnNameModel() {
		return new SettingsModelString("new_refid_column_name", null);
	}

	/**
	 * Creates the settings model to be used to specify the new RMSD column name.
	 * 
	 * @return Settings model for result column name taking the RMSD information.
	 */
	static final SettingsModelString createNewRmsdColumnNameModel() {
		return new SettingsModelString("new_rmsd_column_name", null);
	}

	/**
	 * Creates the settings model to be used to specify the new score column name.
	 * 
	 * @return Settings model for result column name taking the score information.
	 */
	static final SettingsModelString createNewScoreColumnNameModel() {
		return new SettingsModelString("new_score_column_name", null);
	}

	/**
	 * Creates the settings model to be used to specify if reflection is allowed in the alignment process.
	 * 
	 * @return Settings model for allowing reflection.
	 */
	static final SettingsModelBoolean createAllowReflectionModel() {
		return new SettingsModelBoolean("allowReflection", DEFAULT_ALLOW_REFLECTION);
	}

	/**
	 * Creates the settings model for the advanced option to specify maximum of iterations.
	 * 
	 * @return Settings model for specifying maximum iterations.
	 */
	static final SettingsModelIntegerBounded createMaxIterationsModel() {
		return new SettingsModelIntegerBounded("maxIterations", DEFAULT_MAX_ITERATIONS, 1, Integer.MAX_VALUE);
	}
	/**
	 * Creates the settings model to be used to specify the accuracy of the alignment process.
	 * 
	 * @return Settings model for accuracy value.
	 */
	static final SettingsModelIntegerBounded createAccuracyModel() {
		return new SettingsModelIntegerBounded("accuracy", DEFAULT_ACCURACY, 0, 3);
	}

}
