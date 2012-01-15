/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2011
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
package org.rdkit.knime.nodes.saltstripper;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.types.RDKitMolValue;

/**
 * The NodeDialog for the "RDKitSaltStripper" Node is used to specify the user adjustable setting.
 * This node dialog is derived from DefaultNodeSettingsPane which allows
 * creation of a simple dialog with standard components
 * 
 * @author Dillip K Mohanty
 */
public class RDKitSaltStripperNodeDialog extends DefaultNodeSettingsPane {

    /**
     * Create the dialog pane components for configuring RDKitSaltStripper node.
     */
	@SuppressWarnings("unchecked")
    protected RDKitSaltStripperNodeDialog() {
    	 super.addDialogComponent(new DialogComponentColumnNameSelection(
                 createRdkitMolColumnModel(), "Input Molecule Column: ", 0, 
                 RDKitMolValue.class));
    	 super.addDialogComponent(new DialogComponentColumnNameSelection(
                 createSaltMolColumnModel(), "Salt Definition Column: ", 1, false, true,
                 RDKitMolValue.class));
         super.addDialogComponent(
         		new DialogComponentBoolean(createOrigMoleculeModel(),
         				"Keep Original Molecule Column ? "));
    	 
    }
    
    /**
     * Create the settings model for the rdkit molecule column.
     * @return settings model for first column selection
     */
    static final SettingsModelString createRdkitMolColumnModel() {
        return new SettingsModelString("molecule_input", null);
    }
    
    /**
     * Create the settings model for the salt molecule column.
     * @return settings model for second column selection
     */
    static final SettingsModelString createSaltMolColumnModel() {
        return new SettingsModelString("salt_input", null);
    }
    
    /**
     * Create the settings model for whether or not the original molecule column is to be kwpt.
     * @return settings model for checkbox
     */
    static final SettingsModelBoolean createOrigMoleculeModel() {
        return new SettingsModelBoolean("keep_molecule_column", false);
    }
}

