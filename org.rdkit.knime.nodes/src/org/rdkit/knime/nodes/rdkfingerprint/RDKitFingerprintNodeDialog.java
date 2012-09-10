/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2012
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
package org.rdkit.knime.nodes.rdkfingerprint;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.rdkfingerprint.RDKitFingerprintNodeModel.FingerprintType;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.DialogComponentEnumSelection;
import org.rdkit.knime.util.SettingsModelEnumeration;

/**
 * The dialog to configure the RDKit node.
 *
 * @author Greg Landrum
 * @author Manuel Schwarze
 */
public class RDKitFingerprintNodeDialog extends DefaultNodeSettingsPane {

	/**
     * Create a new dialog pane with some default components.
     */
    @SuppressWarnings("unchecked")
    RDKitFingerprintNodeDialog() {
        super.addDialogComponent(new DialogComponentColumnNameSelection(
                createSmilesColumnModel(), "RDKit Mol column: ", 0,
                RDKitMolValue.class));
        super.addDialogComponent(new DialogComponentEnumSelection<FingerprintType>(
                createFPTypeModel(), "Fingerprint type: ", FingerprintType.morgan, FingerprintType.featmorgan, 
                FingerprintType.atompair, FingerprintType.torsion, FingerprintType.rdkit, FingerprintType.avalon,
                FingerprintType.layered));

        super.addDialogComponent(new DialogComponentString(
                createNewColumnModel(), "New column name: "));
        super.addDialogComponent(new DialogComponentBoolean(
                createBooleanModel(), "Remove source column"));

        super.addDialogComponent(new DialogComponentNumberEdit(
                createNumBitsModel(), "Num Bits: ", 4));
        super.addDialogComponent(new DialogComponentNumber(createRadiusModel(),
                "Radius: ", 1));
        super.addDialogComponent(new DialogComponentNumber(
                createMinPathModel(), "Min Path Length: ", 1));
        super.addDialogComponent(new DialogComponentNumber(
                createMaxPathModel(), "Max Path Length: ", 1));
        super.addDialogComponent(new DialogComponentNumberEdit(
                createLayerFlagsModel(), "LayerFlags: ", 8));
    }

    /**
     * @return settings model for smiles column selection
     */
    static final SettingsModelString createSmilesColumnModel() {
        return new SettingsModelString("smiles_column", null);
    }

    /**
     * @return settings model for the new appended column name
     */
    static final SettingsModelString createNewColumnModel() {
        return new SettingsModelString("new_column_name", null);
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
        return new SettingsModelIntegerBounded("layer_flags", 0xFFFF,
                1, 0xFFFF);
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
    static final SettingsModelEnumeration<FingerprintType> createFPTypeModel() {
        return new SettingsModelEnumeration<FingerprintType>(FingerprintType.class, "fp_type", FingerprintType.morgan);
    }

}
