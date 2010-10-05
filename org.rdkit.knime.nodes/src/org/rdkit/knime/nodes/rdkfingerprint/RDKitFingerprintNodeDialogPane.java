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
 * If you have any questions please contact the copyright holder.
 * ---------------------------------------------------------------------
 */
package org.rdkit.knime.nodes.rdkfingerprint;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

/**
 * 
 * @author Greg Landrum
 */
public class RDKitFingerprintNodeDialogPane extends DefaultNodeSettingsPane {

    /**
     * Create a new dialog pane with some default components.
     */
    RDKitFingerprintNodeDialogPane() {
        super.addDialogComponent(new DialogComponentColumnNameSelection(
                createSmilesColumnModel(), "SMILES column: ", 0, 
                StringValue.class));
        super.addDialogComponent(new DialogComponentStringSelection(
                createFPTypeModel(), "FP type ", "morgan","rdkit","layered"));

        super.addDialogComponent(new DialogComponentNumber(
                createMinPathModel(), "Min Path Length: ", 1));
        super.addDialogComponent(new DialogComponentNumber(
                createMaxPathModel(), "Max Path Length: ", 1));
        super.addDialogComponent(new DialogComponentNumber(
                createRadiusModel(), "Radius: ", 1));
        super.addDialogComponent(new DialogComponentNumberEdit(
                createLayerFlagsModel(), "LayerFlags: ", 8));
        super.addDialogComponent(new DialogComponentNumberEdit(
                createNumBitsModel(), "Num Bits: ", 4));
        super.addDialogComponent(
                new DialogComponentString(createNewColumnModel(), 
                        "New column name: "));
        super.addDialogComponent(
        		new DialogComponentBoolean(createBooleanModel(),
        				"Remove source columns"));
    }
    
    /**
     * @return settings model for smiles column selection
     */
    static final SettingsModelString createSmilesColumnModel() {
        return new SettingsModelString("smiles_column", "");
    }
    
    /**
     * @return settings model for the new appended column name
     */
    static final SettingsModelString createNewColumnModel() {
        return new SettingsModelString("new_column_name", "rdk_fingerprint");
    }
    
    /** @return settings model for check box whether to remove source columns. */
    static final SettingsModelBoolean createBooleanModel() {
    	return new SettingsModelBoolean("remove_source_columns", false);
    }

    /**
     * @return settings model
     */
    static final SettingsModelIntegerBounded createMinPathModel() {
        return new SettingsModelIntegerBounded("min_path", 1, 1, 10);
    }
    /**
     * @return settings model
     */
    static final SettingsModelIntegerBounded createMaxPathModel() {
        return new SettingsModelIntegerBounded("max_path", 7, 1, 10);
    }
    /**
     * @return settings model
     */
    static final SettingsModelIntegerBounded createRadiusModel() {
        return new SettingsModelIntegerBounded("radius", 2, 1, 6);
    }
    /**
     * @return settings model
     */
    static final SettingsModelIntegerBounded createLayerFlagsModel() {
        return new SettingsModelIntegerBounded("layer_flags",0xFFFF,1,0xFFFF);
    }
    /**
     * @return settings model
     */
    static final SettingsModelIntegerBounded createNumBitsModel() {
        return new SettingsModelIntegerBounded("num_bits", 1024, 32, 9192);
    }
    /**
     * @return settings model
     */
    static final SettingsModelString createFPTypeModel() {
        return new SettingsModelString("fp_type", "morgan");
    }
    

}
