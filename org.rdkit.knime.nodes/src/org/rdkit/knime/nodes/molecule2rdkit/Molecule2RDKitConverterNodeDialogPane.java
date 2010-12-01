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
package org.rdkit.knime.nodes.molecule2rdkit;

import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 *
 * @author Greg Landrum
 */
public class Molecule2RDKitConverterNodeDialogPane extends
        DefaultNodeSettingsPane {

    /**
     * the key when table split is selected - also the label displayed next to
     * the corresponding radio button.
     */
    static final String SPLIT_BAD_ROWS =
            "Create table with erroneous input rows at second output";

    /**
     * the key when table split is disabled - also the label displayed next to
     * the corresponding radio button.
     */
    static final String MISSVAL_FOR_FOR_BAD_ROWS =
            "Insert a missing value as result in erroneous rows";

    /**
     * Create a new dialog pane with some default components.
     */
    @SuppressWarnings("unchecked")
    Molecule2RDKitConverterNodeDialogPane() {
        super.addDialogComponent(new DialogComponentColumnNameSelection(
                createFirstColumnModel(), "Molecule column: ", 0,
                SmilesValue.class, SdfValue.class));
        super.addDialogComponent(new DialogComponentString(
                createNewColumnModel(), "New column name: "));
        super.addDialogComponent(new DialogComponentBoolean(
                createBooleanModel(), "Remove source column"));
        super.addDialogComponent(new DialogComponentButtonGroup(
                createSeparateRowsModel(), true,
                "Separate erroneous input rows", SPLIT_BAD_ROWS,
                MISSVAL_FOR_FOR_BAD_ROWS));
    }

    /**
     * @return settings model for first column selection
     */
    static final SettingsModelString createFirstColumnModel() {
        return new SettingsModelString("first_column", null);
    }

    /**
     * @return settings model for the new appended column name
     */
    static final SettingsModelString createNewColumnModel() {
        return new SettingsModelString("new_column_name", null);
    }

    /**
     * @return settings model for check box whether to remove source columns
     */
    static final SettingsModelBoolean createBooleanModel() {
        return new SettingsModelBoolean("remove_source_columns", false);
    }

    /**
     * @return new settings model for the flag 'send bad rows to port1'
     */
    static final SettingsModelString createSeparateRowsModel() {
        return new SettingsModelString("bad_rows_to_port1", SPLIT_BAD_ROWS);
    };

}
