/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2016-2023
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
package org.rdkit.knime.nodes.moleculecatalogfilter;

import java.awt.Color;

import org.RDKit.FilterCatalogParams.FilterCatalogs;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.DialogComponentColumnNameSelection;
import org.rdkit.knime.util.DialogComponentEnumFilterPanel;
import org.rdkit.knime.util.DialogComponentEnumSelection;
import org.rdkit.knime.util.SettingsModelEnumeration;
import org.rdkit.knime.util.SettingsModelEnumerationArray;

/**
 * <code>NodeDialog</code> for the "RDKitMoleculeCatalogFilter" Node.
 * Filters a list of molecules by applying filters defined in standard catalogs: 
 * PAINS, PAINS A, PAINS B, PAINS C, BRENK, NIH, ZINC or ALL.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Manuel Schwarze
 */
public class RDKitMoleculeCatalogFilterNodeDialog extends DefaultNodeSettingsPane {
	
	//
	// Constants
	//
	
	/** Result column name default prefix. */
	protected static final String DEFAULT_COLUMN_PREFIX = "";

	//
	// Constructor
	//
	
    /**
     * Create a new dialog pane with default components to configure an input column,
     * the name of a new column, which will contain the calculation results, an option
     * to tell, if the source column shall be removed from the result table.
     */
	RDKitMoleculeCatalogFilterNodeDialog() {
        super.addDialogComponent(new DialogComponentColumnNameSelection(
                createInputColumnNameModel(), "RDKit Mol column: ", 0,
                RDKitMolValue.class));
        DialogComponentEnumFilterPanel<FilterCatalogs> filterPanel = new DialogComponentEnumFilterPanel<>(
                createFilterCatalogsModel(), "Filter catalogs to apply: ", null, false);
        filterPanel.setSearchVisible(false);
        filterPanel.setExcludeTitle(" Do not apply ");
        filterPanel.setExcludeBorderColor(Color.GREEN);
        filterPanel.setIncludeTitle(" Apply ");
        filterPanel.setIncludeBorderColor(Color.RED);
        super.addDialogComponent(filterPanel);
        super.addDialogComponent(new DialogComponentString(
                createOutputColumnPrefixModel(), "Prefix for result columns: "));
        super.addDialogComponent(new DialogComponentEnumSelection<AtomListHandling>(
                createGenerateAtomListOptionModel(), "Atom list handling:"));
    }

    //
    // Static Methods
    //
    
    /**
     * Creates the settings model to be used for the input column.
     * 
     * @return Settings model for input column selection.
     */
    static final SettingsModelString createInputColumnNameModel() {
        return new SettingsModelString("input_column", null);
    }

    /**
     * Creates the settings model to be used to specify the filter catalog(s) to be used.
     * 
     * @return Settings model for catalog name.
     */
    static final SettingsModelEnumerationArray<FilterCatalogs> createFilterCatalogsModel() {
        return new SettingsModelEnumerationArray<>(FilterCatalogs.class, "catalogs", new FilterCatalogs[0]);
    }
    
    /**
     * Creates the settings model to be used for the output column prefix.
     * 
     * @return Settings model for output column prefix.
     */
    static final SettingsModelString createOutputColumnPrefixModel() {
        return new SettingsModelString("output_column_prefix", DEFAULT_COLUMN_PREFIX);
    }
    
    /**
     * Creates the settings model to be used for the option to generate an atom list.
     * 
     * @return Settings model for atom list mode.
     */
    static final SettingsModelEnumeration<AtomListHandling> createGenerateAtomListOptionModel() {
        return new SettingsModelEnumeration<>(AtomListHandling.class, "generate_atom_list", AtomListHandling.None);
    }
}
