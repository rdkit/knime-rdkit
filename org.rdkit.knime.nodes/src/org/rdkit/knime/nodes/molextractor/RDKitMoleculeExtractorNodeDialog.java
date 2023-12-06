/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2015-2023
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
package org.rdkit.knime.nodes.molextractor;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataValue;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.rdkit.knime.nodes.AbstractRDKitNodeSettingsPane;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.DialogComponentColumnNameSelection;
import org.rdkit.knime.util.DialogComponentEnumSelection;
import org.rdkit.knime.util.SettingsModelEnumeration;

/**
 * <code>NodeDialog</code> for the "RDKitMoleculeExtractor" Node.
 * Splits up fragment molecules contained in a single RDKit molecule cell and extracts these molecules into separate cells.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Manuel Schwarze
 */
public class RDKitMoleculeExtractorNodeDialog extends AbstractRDKitNodeSettingsPane {

	//
	// Member variables
	//

	private DialogComponentString m_compInputMolecules;
	private DialogComponentString m_compInputMoleculesFormat;
	private DialogComponentColumnNameSelection m_compInputMoleculeColumn;
	private DialogComponentColumnNameSelection m_compInputReferenceColumn;
	private DialogComponentString m_compOutputReferenceColumn;

	//
	// Constructor
	//

	/**
	 * Create a new dialog pane with default components to configure an input column
	 * and the option to use a reference index column.
	 */
	RDKitMoleculeExtractorNodeDialog() {
		createNewGroup("Variable/Data Input");
		final SettingsModelString modelMolecules = createInputMoleculesModel();
		final SettingsModelString modelMoleculesFormat = createInputMoleculesFormatModel();
		super.setHorizontalPlacement(true);
		super.addDialogComponent(m_compInputMolecules = new DialogComponentString(modelMolecules, "Molecules: ", false, 30,
				createFlowVariableModel(modelMolecules)));
		super.addDialogComponent(m_compInputMoleculesFormat = new DialogComponentString(modelMoleculesFormat, "Format: ", false, 5,
				createFlowVariableModel(modelMoleculesFormat)));
		super.setHorizontalPlacement(false);
		createNewGroup("Table Input");
		super.addDialogComponent(m_compInputMoleculeColumn = new DialogComponentColumnNameSelection(
				createInputColumnNameModel(), "RDKit Mol column: ", 0, false, true,
				RDKitMolValue.class));
		final SettingsModelColumnName modelReferenceInputColumnNameModel = createReferenceInputColumnNameModel();
		super.addDialogComponent(m_compInputReferenceColumn = new DialogComponentColumnNameSelection(
				modelReferenceInputColumnNameModel, "Reference column (e.g. an ID): ", 0,
				false, true, DataValue.class));
		createNewGroup("Output");
		super.addDialogComponent(new DialogComponentString(
				createMoleculeOutputColumnNameModel(), "Column name for extracted molecules: ", true, 20));
		super.addDialogComponent(m_compOutputReferenceColumn = new DialogComponentString(
				createReferenceOutputColumnNameModel(modelReferenceInputColumnNameModel),
				"Column name for copied reference data: ", true, 20));

		createNewTab("Advanced");
		super.addDialogComponent(new DialogComponentBoolean(createSanitizeFragmentsOptionModel(), "Sanitize fragments"));
		super.addDialogComponent(new DialogComponentEnumSelection<ErrorHandling>(createErrorHandlingOptionModel(),
				"How to react on conversion errors: ", ErrorHandling.values()));
		super.addDialogComponent(new DialogComponentEnumSelection<EmptyCellHandling>(createEmptyCellHandlingOptionModel(),
				"How to react on empty (missing) cells: ", EmptyCellHandling.values()));
		super.addDialogComponent(new DialogComponentEnumSelection<EmptyMoleculeHandling>(createEmptyMoleculeHandlingOptionModel(),
				"How to react on empty (zero atom) molecules: ", EmptyMoleculeHandling.values()));
	}

	@Override
	public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
			final PortObjectSpec[] specs) throws NotConfigurableException {
		super.loadAdditionalSettingsFrom(settings, specs);

		updateVisibilityOfOptionalComponents(RDKitMoleculeExtractorNodeModel.hasMoleculesInputTable(specs));
	}

	/**
	 * Show or hides input molecule table based settings based on the input method for
	 * the molecules (variable/data driven molecule input or connected molecules table).
	 * 
	 * @param bHasMoleculesTable
	 */
	protected void updateVisibilityOfOptionalComponents(final boolean bHasMoleculesTable) {
		// Show/Hide the components that are related to molecules variable input
		m_compInputMolecules.getComponentPanel().setVisible(!bHasMoleculesTable);
		m_compInputMoleculesFormat.getComponentPanel().setVisible(!bHasMoleculesTable);

		// Show/Hide the whole logical variable input group
		m_compInputMolecules.getComponentPanel().getParent().getParent().setVisible(!bHasMoleculesTable);

		// Show/Hide the components that are related to molecules table input
		m_compInputMoleculeColumn.getComponentPanel().setVisible(bHasMoleculesTable);
		m_compInputReferenceColumn.getComponentPanel().setVisible(bHasMoleculesTable);
		m_compOutputReferenceColumn.getComponentPanel().setVisible(bHasMoleculesTable);

		// Show/Hide the whole logical table input group
		m_compInputMoleculeColumn.getComponentPanel().getParent().getParent().setVisible(bHasMoleculesTable);
	}

	//
	// Static Methods
	//

	/**
	 * Creates the settings model to be used for the input molecules.
	 * 
	 * @return Settings model for input column selection.
	 */
	static final SettingsModelString createInputMoleculesModel() {
		return new SettingsModelString("input_molecules", null);
	}

	/**
	 * Creates the settings model to be used for the input molecules format.
	 * 
	 * @return Settings model for input column selection.
	 */
	static final SettingsModelString createInputMoleculesFormatModel() {
		return new SettingsModelString("input_molecules_format", null);
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
	 * Creates the settings model for new column name to be used for the extracted molecule column.
	 * 
	 * @return Settings model for the new extracted molecule column name.
	 */
	static final SettingsModelString createMoleculeOutputColumnNameModel() {
		return new SettingsModelString("output_mol_name", null);
	}

	/**
	 * Creates the settings model for new column name to be used for the reference column.
	 * 
	 * @return Settings model for the new reference column name.
	 */
	static final SettingsModelString createReferenceOutputColumnNameModel(
			final SettingsModelColumnName modelReferenceColumnName) {
		final SettingsModelString result =
				new SettingsModelString("output_ref_name", null);
		modelReferenceColumnName.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				result.setEnabled(modelReferenceColumnName.useRowID() || modelReferenceColumnName.getStringValue() != null);
			}
		});
		result.setEnabled(modelReferenceColumnName.useRowID() || modelReferenceColumnName.getStringValue() != null);
		return result;
	}

	/**
	 * Creates the settings model for the sanitizing fragments option.
	 * 
	 * @return Settings model for the sanitizing fragments option.
	 */
	static final SettingsModelBoolean createSanitizeFragmentsOptionModel() {
		return new SettingsModelBoolean("sanitize_fragments", false);
	}
	/**
	 * Creates the settings model for the error handling option.
	 * 
	 * @return Settings model for the error handling option.
	 */
	static final SettingsModelEnumeration<ErrorHandling> createErrorHandlingOptionModel() {
		return new SettingsModelEnumeration<ErrorHandling>(ErrorHandling.class, "error_handling",
				ErrorHandling.MissingCellWithWarning);
	}

	/**
	 * Creates the settings model for the empty cell handling option.
	 * 
	 * @return Settings model for the empty cell handling option.
	 */
	static final SettingsModelEnumeration<EmptyCellHandling> createEmptyCellHandlingOptionModel() {
		return new SettingsModelEnumeration<EmptyCellHandling>(EmptyCellHandling.class, "empty_cell_handling",
				EmptyCellHandling.MissingCellWithoutWarning);
	}

	/**
	 * Creates the settings model for the empty molecule handling option.
	 * 
	 * @return Settings model for the empty molecule handling option.
	 */
	static final SettingsModelEnumeration<EmptyMoleculeHandling> createEmptyMoleculeHandlingOptionModel() {
		return new SettingsModelEnumeration<EmptyMoleculeHandling>(EmptyMoleculeHandling.class, "empty_molecule_handling",
				EmptyMoleculeHandling.SkipWithoutWarning);
	}
}
