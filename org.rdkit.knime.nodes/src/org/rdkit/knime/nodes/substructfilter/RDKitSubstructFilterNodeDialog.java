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
package org.rdkit.knime.nodes.substructfilter;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.substructfilter.RDKitSubstructFilterNodeModel.MatchHandling;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.DialogComponentEnumSelection;
import org.rdkit.knime.util.SettingsModelEnumeration;

/**
 * <code>NodeDialog</code> for the "RDKitSubstructFilter" Node.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Greg Landrum
 * @author Manuel Schwarze
 */
public class RDKitSubstructFilterNodeDialog extends DefaultNodeSettingsPane {

	//
	// Constructor
	//
	
    /**
     * Create a new dialog pane with settings components.
     */
    @SuppressWarnings("unchecked")
	RDKitSubstructFilterNodeDialog() {
        super.addDialogComponent(new DialogComponentColumnNameSelection(
                createInputColumnNameModel(), "RDKit Mol column: ", 0,
                RDKitMolValue.class));
        super.addDialogComponent(new DialogComponentString(createSmartsModel(),
                "SMARTS query: "));
        super.addDialogComponent(new DialogComponentBoolean(createExactMatchModel(),
        		"Do exact match"));
        
        final SettingsModelEnumeration<MatchHandling> matchHandlingModel = createMatchHandlingModel();
        super.addDialogComponent(new DialogComponentEnumSelection<MatchHandling>(matchHandlingModel, 
				"Match handling: "));
        super.addDialogComponent(new DialogComponentString(createNewMatchColumnNameModel(matchHandlingModel), 
        		"Column name for matching atom list: ", true, 20));
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
     * Creates the settings model to be used for specifying the SMARTS value.
     * 
     * @return settings model for the smarts query
     */
    static final SettingsModelString createSmartsModel() {
        return new SettingsModelString("smarts_value", "");
    }

    /**
     * Creates the settings model to specify the exact match option.
     * 
     * @return settings model for the exact match toggle
     */
    static final SettingsModelBoolean createExactMatchModel() {
        return new SettingsModelBoolean("exact_match", false);
    }
    
    /**
     * Creates the settings model for specifying how matches shall be handled.
     * 
     * @return settings model for the exact match toggle
     */
    static final SettingsModelEnumeration<MatchHandling> createMatchHandlingModel() {
        return new SettingsModelEnumeration<MatchHandling>(
        		MatchHandling.class, "match_handling", MatchHandling.DoNotAddMatchColumn);
    }    

    /**
     * Creates the settings model for specifying a new column name.
     * This model is dependent on the passed in match handling model.
     * 
     * @return settings model for the name of the new column that contains matches
     */
    static final SettingsModelString createNewMatchColumnNameModel(
    		final SettingsModelEnumeration<MatchHandling> matchHandlingModel) {
    	final SettingsModelString modelWithDependency = new SettingsModelString("new_match_column", "Matching Atom List");
    	
    	// React on any changes in match handling model
        matchHandlingModel.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				boolean bEnabled = matchHandlingModel.getValue() != MatchHandling.DoNotAddMatchColumn;
				
				// Enable or disable the model
				modelWithDependency.setEnabled(bEnabled);
			}
		});
        
        // Enable this model based on the dependent model's state
        modelWithDependency.setEnabled(matchHandlingModel.getValue() != MatchHandling.DoNotAddMatchColumn);
    	
        return modelWithDependency;
    }}
