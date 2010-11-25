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
package org.rdkit.knime.nodes.addcoordinates;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.types.RDKitMolValue;

/**
 *
 * @author Greg Landrum
 */
public class RDKitAddCoordinatesNodeDialogPane
        extends DefaultNodeSettingsPane {
    /**
     * Possible Values of the settings model return by createDimensionModel().
     */
    static String DIMENSION_2D = "2D coordinates";
    static String DIMENSION_3D = "3D coordinates";

    private DialogComponentString m_smartsComp;
    private SettingsModelString m_dimModel;
    /**
     * Create a new dialog pane with some default components.
     */
    RDKitAddCoordinatesNodeDialogPane() {
        super.addDialogComponent(new DialogComponentColumnNameSelection(
                createFirstColumnModel(), "RDKit Mol column:", 0,
                RDKitMolValue.class));
        super.addDialogComponent(new DialogComponentString(
                createNewColumnModel(), "New column name:"));
        super.addDialogComponent(new DialogComponentBoolean(
                createBooleanModel(), "Remove source column"));
        m_dimModel = createDimensionModel();
        DialogComponentButtonGroup dimComp = new DialogComponentButtonGroup(
                m_dimModel, true, "Dimension",
                DIMENSION_2D, DIMENSION_3D);
        m_smartsComp = new DialogComponentString(
                createTemplateSmartsModel(), "Template Smarts:");

        dimComp.getModel().addChangeListener(new ChangeListener() {
            /** Invoked when 2D/3D choice is changed. */
            @Override
            public void stateChanged(final ChangeEvent e) {
                boolean do2D = m_dimModel.getStringValue().equals(DIMENSION_2D);
                m_smartsComp.getModel().setEnabled(do2D);
            }
        });
        super.addDialogComponent(dimComp);
        super.addDialogComponent(m_smartsComp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOpen() {
        boolean do2D = m_dimModel.getStringValue().equals(DIMENSION_2D);
        m_smartsComp.getModel().setEnabled(do2D);
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
     * @return settings model for check box whether to remove source columns.
     */
    static final SettingsModelBoolean createBooleanModel() {
        return new SettingsModelBoolean("remove_source_columns", false);
    }

    /**
     * @return settings model for the new appended column name
     */
    static final SettingsModelString createTemplateSmartsModel() {
        return new SettingsModelString("template_smarts_value", "");
    }

    /**
     * @return settings model for radio buttons whether compute 2D or 3D
     * coordinates.
     */
    static final SettingsModelString createDimensionModel() {
        return new SettingsModelString("dimension", null);
    }
}
