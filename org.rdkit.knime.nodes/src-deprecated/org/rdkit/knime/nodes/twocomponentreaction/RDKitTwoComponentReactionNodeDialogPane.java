/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2010-2023
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
package org.rdkit.knime.nodes.twocomponentreaction;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.types.RDKitMolValue;

/**
 *
 * @author Greg Landrum
 */
@Deprecated
public class RDKitTwoComponentReactionNodeDialogPane
extends DefaultNodeSettingsPane {

	/**
	 * Create a new dialog pane with some default components.
	 */
	@SuppressWarnings("unchecked")
	RDKitTwoComponentReactionNodeDialogPane() {
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				createReactant1ColumnModel(), "Reactants 1 RDKit Mol column: ", 0,
				RDKitMolValue.class));
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				createReactant2ColumnModel(), "Reactants 2 RDKit Mol column: ", 1,
				RDKitMolValue.class));
		final SettingsModelString smartsModel = createSmartsModel();
		final SettingsModelString fileModel = createFileModel();
		final SettingsModelBoolean fileEnableModel =
				createFileEnableModel(fileModel, smartsModel);
		super.addDialogComponent(new DialogComponentString(
				smartsModel, "Reaction SMARTS: "));
		super.addDialogComponent(new DialogComponentBoolean(fileEnableModel,
				"Use Reaction from RXN file: "));
		super.addDialogComponent(new DialogComponentFileChooser(
				fileModel, "rdkit_twocomp_reaction", ".rxn"));
		super.addDialogComponent(new DialogComponentBoolean(
				createBooleanModel(), "Do Matrix Expansion"));
	}

	/**
	 * @return settings model for first column selection
	 */
	static final SettingsModelString createReactant1ColumnModel() {
		return new SettingsModelString("reactant1_column", null);
	}

	/**
	 * @return settings model for second column selection
	 */
	static final SettingsModelString createReactant2ColumnModel() {
		return new SettingsModelString("reactant2_column", null);
	}

	/**
	 * @return settings model for the new appended column name
	 */
	static final SettingsModelString createSmartsModel() {
		return new SettingsModelString("rxnsmarts_value", "");
	}

	static final SettingsModelBoolean createFileEnableModel(
			final SettingsModelString fileModel,
			final SettingsModelString smartsModel) {
		final boolean def = false;
		final SettingsModelBoolean result =
				new SettingsModelBoolean("use_rxn_file_input", def);
		fileModel.setEnabled(def);
		smartsModel.setEnabled(!def);
		result.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				final boolean isChecked = result.getBooleanValue();
				fileModel.setEnabled(isChecked);
				smartsModel.setEnabled(!isChecked);
			}
		});
		return result;
	}

	static final SettingsModelString createFileModel() {
		return new SettingsModelString("file_input", "");
	}
	/**
	 * @return settings model for check box whether to remove source columns
	 */
	static final SettingsModelBoolean createBooleanModel() {
		return new SettingsModelBoolean("do_matrix", false);
	}
}
