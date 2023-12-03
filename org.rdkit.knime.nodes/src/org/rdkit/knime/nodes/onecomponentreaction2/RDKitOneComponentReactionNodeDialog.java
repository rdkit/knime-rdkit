/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2010-2023
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
package org.rdkit.knime.nodes.onecomponentreaction2;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.DialogComponentColumnNameSelection;

import javax.swing.*;

/**
 * <code>NodeDialog</code> for the "RDKitOneComponentReaction" Node.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Greg Landrum
 * @author Manuel Schwarze
 * @author Roman Balabanov
 */
public class RDKitOneComponentReactionNodeDialog extends AbstractRDKitReactionNodeDialog {

	//
	// Members
	//

	/**
	 * The settings model to be used for the additional reactant columns filter.
	 */
	private SettingsModelColumnFilter2 m_modelAdditionalColumnsFilter;

	//
	// Constructor
	//

	/**
	 * Creates a new dialog settings panel.
	 */
	public RDKitOneComponentReactionNodeDialog() {
		super(1);
	}

	//
	// Protected Methods
	//

	/**
	 * {@inheritDoc}
	 * This implementation adds the input mol column selection component.
	 */
	@Override
	protected void addDialogComponentsBeforeReactionSettings() {
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				createReactantColumnNameModel(), "Reactant RDKit Mol column: ", 0,
				RDKitMolValue.class));
	}

	/**
	 * {@inheritDoc}
	 * This implementation does not add any components.
	 */
	@Override
	protected void addDialogComponentsAfterReactionSettings() {
		// Nothing to add here
	}

	@Override
	protected String getDialogComponentAdditionalColumnsEnableLabel() {
		return "Include additional columns from reactant input table into product output table";
	}

	@Override
	protected JPanel addDialogComponentsForAdditionalColumnsSelection() {
		super.createNewGroup("Additional columns from Reactant table");

		m_modelAdditionalColumnsFilter = createAdditionalColumnsFilterModel(m_modelAdditionalColumnsEnabled);
		super.addDialogComponent(new DialogComponentColumnFilter2(
				m_modelAdditionalColumnsFilter,
				0));

		return null;
	}

	@Override
	public void loadAdditionalSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs) throws NotConfigurableException {
		super.loadAdditionalSettingsFrom(settings, specs);

		// have to do it exactly here, after columns filter model loaded in order to prevent NPE deep inside its implementation code
		m_modelAdditionalColumnsFilter.setEnabled(m_modelAdditionalColumnsEnabled.getBooleanValue());
	}

	//
	// Static Methods
	//

	/**
	 * Creates the settings model to be used for the input column.
	 * 
	 * @return Settings model for input column selection.
	 */
	static final SettingsModelString createReactantColumnNameModel() {
		return new SettingsModelString("input_column", null);
	}

	/**
	 * Creates the settings model to be used for the additional data columns filtering.
	 *
	 * @param modelAdditionalColumnsEnabled Settings model for the additional columns selection enablement flag.
	 *                                      Can be null.
	 * @return Settings mode for additional data columns filter.
	 */
	static SettingsModelColumnFilter2 createAdditionalColumnsFilterModel(SettingsModelBoolean modelAdditionalColumnsEnabled) {
		final SettingsModelColumnFilter2 modelResult = new SettingsModelColumnFilter2("additionalColumnsFilter");

		if (modelAdditionalColumnsEnabled != null) {
			modelAdditionalColumnsEnabled.addChangeListener(e -> modelResult.setEnabled(modelAdditionalColumnsEnabled.getBooleanValue()));
		}

		return modelResult;
	}

}
