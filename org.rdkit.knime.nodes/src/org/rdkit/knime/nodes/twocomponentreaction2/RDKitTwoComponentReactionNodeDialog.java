/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2010
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
package org.rdkit.knime.nodes.twocomponentreaction2;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.onecomponentreaction2.AbstractRDKitReactionNodeDialog;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.DialogComponentColumnNameSelection;

/**
 * <code>NodeDialog</code> for the "RDKitTwoComponentReaction" Node.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Greg Landrum
 * @author Manuel Schwarze
 */
public class RDKitTwoComponentReactionNodeDialog extends AbstractRDKitReactionNodeDialog {

	//
	// Constructor
	//

	/**
	 * Creates a new dialog settings panel.
	 */
	public RDKitTwoComponentReactionNodeDialog() {
		super(2);
	}

	//
	// Protected Methods
	//

	/**
	 * {@inheritDoc}
	 * This implementation adds the input mol column selection components for the reactants.
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void addDialogComponentsBeforeReactionSettings() {
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				createReactant1ColumnNameModel(), "Reactants 1 RDKit Mol column: ", 0,
				RDKitMolValue.class));
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				createReactant2ColumnNameModel(), "Reactants 2 RDKit Mol column: ", 1,
				RDKitMolValue.class));
	}

	/**
	 * {@inheritDoc}
	 * This implementation adds the matrix expansion option.
	 */
	@Override
	protected void addDialogComponentsAfterReactionSettings() {
		super.addDialogComponent(new DialogComponentBoolean(
				createDoMatrixExpansionModel(), "Do matrix expansion"));
	}

	//
	// Static Methods
	//

	/**
	 * Creates the settings model to be used for the input column for reactant 1.
	 * 
	 * @return Settings model for input column selection for reactant 1.
	 */
	static final SettingsModelString createReactant1ColumnNameModel() {
		return new SettingsModelString("reactant1_column", null);
	}

	/**
	 * Creates the settings model to be used for the input column for reactant 2.
	 * 
	 * @return Settings model for input column selection for reactant 2.
	 */
	static final SettingsModelString createReactant2ColumnNameModel() {
		return new SettingsModelString("reactant2_column", null);
	}

	/**
	 * Creates the settings model for the matrix expansion option.
	 * 
	 * @return Settings model for for the matrix expansion option.
	 */
	static final SettingsModelBoolean createDoMatrixExpansionModel() {
		return new SettingsModelBoolean("matrixExpansion", false);
	}
}
