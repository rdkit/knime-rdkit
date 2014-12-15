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
package org.rdkit.knime.nodes.rmsdfilter;

import org.knime.core.data.DataValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.DialogComponentColumnNameSelection;

/**
 * <code>NodeDialog</code> for the "RDKitRMSDFilter" Node.
 * Calculates the RMSD value for RDKit molecules and filters them based on a threshold value.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Manuel Schwarze
 */
public class RDKitRMSDFilterNodeDialog extends DefaultNodeSettingsPane {

	//
	// Constants
	//

	/** Default value to be used as RMSD threshold. */
	public static final double DEFAULT_RMSD_THRESHOLD = 0.5d;

	/** Default value to be used as option for ignoring Hs. */
	public static final boolean DEFAULT_IGNORE_HS = false;

	//
	// Constructor
	//

	/**
	 * Create a new dialog pane with default components to configure an input column,
	 * the name of a new column, which will contain the calculation results, an option
	 * to tell, if the source column shall be removed from the result table.
	 */
	RDKitRMSDFilterNodeDialog() {
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				createMoleculeInputColumnNameModel(), "RDKit Mol column with conformers: ", 0,
				RDKitMolValue.class));
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				createReferenceInputColumnNameModel(), "Reference column (e.g. an ID): ", 0,
				DataValue.class));
		super.addDialogComponent(new DialogComponentNumber(
				createRmsdThresholdModel(), "RMSD threshold", 0.1d));
		super.addDialogComponent(new DialogComponentBoolean(
				createIgnoreHsOptionModel(), "Ignore Hs (increases performance)"));
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
	static final SettingsModelString createReferenceInputColumnNameModel() {
		return new SettingsModelString("input_ref_column", null);

	}

	/**
	 * Creates the settings model to be used for the RMSD threshold value.
	 * 
	 * @return Settings model for the RMSD threshold value.
	 */
	static final SettingsModelDoubleBounded createRmsdThresholdModel() {
		return new SettingsModelDoubleBounded("rmsd_threshold", DEFAULT_RMSD_THRESHOLD, 0.0d, Double.MAX_VALUE);
	}

	/**
	 * Creates the settings model to be used for the option to ignore Hs.
	 * 
	 * @return Settings model for the option to ignore Hs.
	 */
	static final SettingsModelBoolean createIgnoreHsOptionModel() {
		return new SettingsModelBoolean("ignore_hs", DEFAULT_IGNORE_HS);
	}
}
