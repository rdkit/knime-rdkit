/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2010
 * Novartis Institutes for BioMedical Research
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder
 * ---------------------------------------------------------------------
 */
package org.rdkit.knime.nodes.twocomponentreaction;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * 
 * @author Greg Landrum
 */
public class RDKitTwoComponentReactionNodeDialogPane extends DefaultNodeSettingsPane {

    /**
     * Create a new dialog pane with some default components.
     */
    RDKitTwoComponentReactionNodeDialogPane() {
        super.addDialogComponent(new DialogComponentColumnNameSelection(
                createReactant1ColumnModel(), "Reactants 1 SMILES column: ", 0, 
                StringValue.class));
        super.addDialogComponent(new DialogComponentColumnNameSelection(
                createReactant2ColumnModel(), "Reactants 2 SMILES column: ", 1, 
                StringValue.class));
        super.addDialogComponent(
                new DialogComponentString(createSmartsModel(), 
                        "Reaction SMARTS: "));
        super.addDialogComponent(
        		new DialogComponentBoolean(createBooleanModel(),
        				"Do Matrix Expansion"));
    }
    
    /**
     * @return settings model for first column selection
     */
    static final SettingsModelString createReactant1ColumnModel() {
        return new SettingsModelString("reactant1_column", "");
    }
    
    /**
     * @return settings model for second column selection
     */
    static final SettingsModelString createReactant2ColumnModel() {
        return new SettingsModelString("reactant2_column", "");
    }
    
    /**
     * @return settings model for the new appended column name
     */
    static final SettingsModelString createSmartsModel() {
        return new SettingsModelString("rxnsmarts_value", "");
    }
    /** @return settings model for check box whether to remove source columns. */
    static final SettingsModelBoolean createBooleanModel() {
    	return new SettingsModelBoolean("do_matrix", false);
    }    
}
