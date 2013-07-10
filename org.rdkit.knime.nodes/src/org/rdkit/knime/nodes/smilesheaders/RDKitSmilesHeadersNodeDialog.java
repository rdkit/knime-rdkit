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
package org.rdkit.knime.nodes.smilesheaders;

import org.knime.core.data.StringValue;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.rdkit.knime.util.DialogComponentColumnNameSelection;

/**
 * <code>NodeDialog</code> for the "RDKitSmilesHeaders" Node.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Manuel Schwarze
 */
public class RDKitSmilesHeadersNodeDialog extends DefaultNodeSettingsPane {

	//
	// Members
	//

	/** The dialog component to show a hint label to the user, if no second table is connected. */
	private final DialogComponentLabel m_compHintLabel;

	/** The dialog component to set the option to use column titles as molecules. */
	private final DialogComponentBoolean m_compUseColumnTitles;

	/** The dialog component to defined the target column name. */
	private final DialogComponentColumnNameSelection m_compTargetColumn;

	/** The dialog component to defined the SMILES column name. */
	private final DialogComponentColumnNameSelection m_compSmilesColumn;

	//
	// Constructor
	//

	/**
	 * Create a new dialog pane with default components to configure an input column,
	 * the name of a new column, which will contain the calculation results, an option
	 * to tell, if the source column shall be removed from the result table.
	 */
	@SuppressWarnings("unchecked")
	RDKitSmilesHeadersNodeDialog() {
		m_compUseColumnTitles = new DialogComponentBoolean(
				createUseColumnTitlesAsMoleculesOptionModel(),
				"Use column titles of Data Table as SMILES property definitions");

		m_compHintLabel = new DialogComponentLabel(
				"Hint: Connect a SMILES Definition Table to define target columns and their SMILES values.");

		m_compTargetColumn = new DialogComponentColumnNameSelection(
				createTargetColumnColumnNameModel(), "Column with target column names: ", 1, false,
				StringValue.class);

		m_compSmilesColumn = new DialogComponentColumnNameSelection(
				createSmilesValueColumnNameModel(), "Column with new SMILES values: ", 1, false,
				StringValue.class) {

			/**
			 * Hides or shows the optional components depending on
			 * the existence of a second input table.
			 */
			@Override
			protected void checkConfigurabilityBeforeLoad(
					final PortObjectSpec[] specs)
							throws NotConfigurableException {

				final boolean bHasSmilesDefinitionTable =
						RDKitSmilesHeadersNodeModel.hasSmilesDefinitionTable(specs);

				// Only check correctness of second input table if it is there
				if (bHasSmilesDefinitionTable) {
					super.checkConfigurabilityBeforeLoad(specs);
				}

				// Always show or hide proper components
				updateVisibilityOfOptionalComponents(bHasSmilesDefinitionTable);
			}
		};

		super.addDialogComponent(m_compUseColumnTitles);
		super.addDialogComponent(m_compHintLabel);
		super.addDialogComponent(m_compTargetColumn);
		super.addDialogComponent(m_compSmilesColumn);
		super.addDialogComponent(new DialogComponentBoolean(createCompleteResetOptionModel(),
				"Remove existing SMILES values in all headers first"));
	}

	//
	// Protected Methods
	//

	/**
	 * Show or hides settings based on the condition if a SMILES definition
	 * table is connected or not.
	 * 
	 * @param bHasSmilesDefinitionTable True, if a definition table is connected.
	 */
	protected void updateVisibilityOfOptionalComponents(final boolean bHasSmilesDefinitionTable) {
		m_compTargetColumn.getComponentPanel().setVisible(bHasSmilesDefinitionTable);
		m_compSmilesColumn.getComponentPanel().setVisible(bHasSmilesDefinitionTable);
		m_compHintLabel.getComponentPanel().setVisible(!bHasSmilesDefinitionTable);
		m_compUseColumnTitles.getComponentPanel().setVisible(!bHasSmilesDefinitionTable);
	}

	//
	// Static Methods
	//

	/**
	 * Creates the settings model to be used to specify the target column names.
	 * 
	 * @return Settings model for input column selection.
	 */
	static final SettingsModelString createTargetColumnColumnNameModel() {
		return new SettingsModelString("names_column", null);
	}

	/**
	 * Creates the settings model to be used to specify the SMILES column.
	 * 
	 * @return Settings model for result column name.
	 */
	static final SettingsModelString createSmilesValueColumnNameModel() {
		return new SettingsModelString("smiles_column", null);
	}

	/**
	 * Creates the settings model to be used to specify the option
	 * to use column titles of the Data Table instead of the
	 * SMILES Definition Table. This only takes effect if no
	 * SMILES Definition Table is connected.
	 * 
	 * @return Settings model for result column name.
	 */
	static final SettingsModelBoolean createUseColumnTitlesAsMoleculesOptionModel() {
		return new SettingsModelBoolean("use_column_titles_as_smiles", true);
	}

	/**
	 * Creates the settings model to be used to specify the option
	 * to remove all additional header information for SMILES for the
	 * data table before (optionally) setting new SMILES information.
	 * 
	 * @return Settings model for complete SMILES removal option.
	 */
	static final SettingsModelBoolean createCompleteResetOptionModel() {
		return new SettingsModelBoolean("complete_reset", false);
	}
}
