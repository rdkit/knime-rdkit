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
package org.rdkit.knime.nodes.diversitypicker;

import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.DialogComponentColumnNameSelection;

/**
 * <code>NodeDialog</code> for the "RDKitDiversityPicker" Node.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 *
 * @author Greg Landrum
 * @author Manuel Schwarze
 */
public class RDKitDiversityPickerNodeDialog extends DefaultNodeSettingsPane {

	//
	// Members
	//

	/** Setting model component for the additional column selector. */
	private final DialogComponent m_compAdditionalInputColumnName;

	/** Setting model component for the hint label to connect a second table. */
	private final DialogComponent m_compHintToConnectSecondTable;

	//
	// Constructor
	//

	/**
	 * Create a new dialog pane with some default components.
	 */
	RDKitDiversityPickerNodeDialog() {
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				createInputColumnNameModel(), "Molecule or fingerprint column (table 1): ", 0,
				BitVectorValue.class, RDKitMolValue.class));
		super.addDialogComponent(m_compHintToConnectSecondTable = new DialogComponentLabel(
				"You may connect a second input table with a molecule or fingerprint column to bias away from."));
		super.addDialogComponent(m_compAdditionalInputColumnName = new DialogComponentColumnNameSelection(
				createAdditionalInputColumnNameModel(), "Molecule or fingerprint column to bias away from (table 2): ", 1, false, true,
				BitVectorValue.class, RDKitMolValue.class) {

			/**
			 * Hides or shows the optional components depending on
			 * the existence of a second input table.
			 */
			@Override
			protected void checkConfigurabilityBeforeLoad(
					final PortObjectSpec[] specs)
							throws NotConfigurableException {

				final boolean bHasAdditionalInputTable =
						RDKitDiversityPickerNodeModel.hasAdditionalInputTable(specs);

				// Only check correctness of second input table if it is there
				if (bHasAdditionalInputTable) {
					super.checkConfigurabilityBeforeLoad(specs);
				}

				// Always show or hide proper components
				updateVisibilityOfOptionalComponents(bHasAdditionalInputTable);
			}
		});

		super.addDialogComponent(new DialogComponentNumber(createNumberToPickModel(),
				"Number to pick: ", 1));
		super.addDialogComponent(new DialogComponentNumberEdit(createRandomSeedModel(),
				"Random seed: ", 10));
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
		m_compAdditionalInputColumnName.getComponentPanel().setVisible(bHasAdditionalInputTable);
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
	 * Creates the settings model to be used for an optional additional input column.
	 * 
	 * @return Settings model for input column selection.
	 */
	static final SettingsModelString createAdditionalInputColumnNameModel() {
		return new SettingsModelString("additional_input_column", null);
	}

	/**
	 * Creates the settings model to be used to enter the number to pick.
	 * 
	 * @return Settings model for number to pick.
	 */
	static final SettingsModelInteger createNumberToPickModel() {
		return new SettingsModelIntegerBounded("num_picks", 10, 1, Integer.MAX_VALUE);
	}

	/**
	 * Creates the settings model to be used to enter a random seed.
	 * 
	 * @return Settings model for the optional random seed.
	 */
	static final SettingsModelInteger createRandomSeedModel() {
		return new SettingsModelInteger("random_seed", -1);
	}
}
