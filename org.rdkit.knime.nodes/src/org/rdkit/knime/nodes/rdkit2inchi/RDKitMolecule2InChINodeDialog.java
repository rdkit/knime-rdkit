/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2012
 * Novartis Institutes for BioMedical Research
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
package org.rdkit.knime.nodes.rdkit2inchi;

import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentMultiLineString;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.types.RDKitMolValue;

/**
 * <code>NodeDialog</code> for the "RDKitMolecule2InChI" Node.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Manuel Schwarze
 */
public class RDKitMolecule2InChINodeDialog extends DefaultNodeSettingsPane {

	//
	// Constructor
	//
	
    /**
     * Create a new dialog pane with default components to configure an input column,
     * the name of a new column, which will contain the calculation results, an option
     * to tell, if the source column shall be removed from the result table.
     */
    @SuppressWarnings("unchecked")
	RDKitMolecule2InChINodeDialog() {
        super.addDialogComponent(new DialogComponentColumnNameSelection(
                createInputColumnNameModel(), "RDKit Mol column: ", 0,
                RDKitMolValue.class));
        super.addDialogComponent(new DialogComponentBoolean(
                createRemoveSourceColumnsOptionModel(), "Remove source column"));
        
        super.createNewGroup("InChI Code Generation");
        super.addDialogComponent(new DialogComponentString(
                createNewInChICodeColumnNameModel(), "New column name for InChI codes: "));

        super.createNewGroup("InChI Key Generation");
        SettingsModelBoolean modelGenerateInChIKeysOption = createGenerateInChIKeyOptionModel();
        super.addDialogComponent(new DialogComponentBoolean(
        		modelGenerateInChIKeysOption, "Generate also InChI keys"));
        super.addDialogComponent(new DialogComponentString(
                createNewInChIKeyColumnNameModel(modelGenerateInChIKeysOption), "New column name for InChI keys: "));
        
        super.createNewGroup("Extra InChI Generation Information");
        super.addDialogComponent(new DialogComponentString(
                createExtraInformationColumnNamePrefixModel(), "New column name prefix for extra information: "));
        super.setHorizontalPlacement(true);
        super.addDialogComponent(new DialogComponentBoolean(
        		createExtraReturnCodeOptionModel(), "Return Code Column"));
        super.addDialogComponent(new DialogComponentBoolean(
        		createExtraAuxInfoOptionModel(), "Aux Info Column"));
        super.addDialogComponent(new DialogComponentBoolean(
        		createExtraMessageOptionModel(), "Message Column"));
        super.addDialogComponent(new DialogComponentBoolean(
        		createExtraLogOptionModel(), "Log Column"));
        super.setHorizontalPlacement(false);
        JPanel panelParent = super.getPanel();
        Dimension dimPref = panelParent.getPreferredSize();
        panelParent.setPreferredSize(new Dimension(650, dimPref.height + 20));
        
        super.createNewTab("Advanced");
        super.createNewGroup("InChI Code Generation Switches");
        super.addDialogComponent(new DialogComponentLabel("You may specify here switches (starting with / or -) to influence the InChI code generation."));
        super.addDialogComponent(new DialogComponentLabel("Note: Some of them make the resulting identifier Non-standard."));
        super.addDialogComponent(new DialogComponentMultiLineString(
                createAdvancedOptionsModel(), null, false, 40, 5));
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
     * Creates the settings model to be used to specify the new column name
     * for the InChI code data.
     * 
     * @return Settings model for result column name (InChI Codes).
     */
    static final SettingsModelString createNewInChICodeColumnNameModel() {
        return new SettingsModelString("new_inchi_code_column_name", null);
    }

    /**
     * Creates the settings model for the boolean flag to determine, if
     * the source column shall be removed from the result table.
     * The default is false.
     * 
     * @return Settings model for check box whether to remove source columns.
     */
    static final SettingsModelBoolean createRemoveSourceColumnsOptionModel() {
        return new SettingsModelBoolean("remove_source_columns", false);
    }

    /**
     * Creates the settings model for the boolean flag to determine, if
     * the source column shall be removed from the result table.
     * The default is false.
     * 
     * @return Settings model for check box whether to remove source columns.
     */
    static final SettingsModelBoolean createGenerateInChIKeyOptionModel() {
        return new SettingsModelBoolean("generate_inchi_keys", false);
    }

    /**
     * Creates the settings model to be used to specify the new column name
     * for the InChI Key data.
     * 
     * @param modelGenerateInChIKeysOption Option
     * 
     * @return Settings model for result column name (InChI Keys).
     */
    static final SettingsModelString createNewInChIKeyColumnNameModel(final SettingsModelBoolean modelGenerateInChIKeysOption) {
    	final SettingsModelString modelWithDependency = new SettingsModelString("new_inchi_key_column_name", null);
    	
    	// React on any changes in match handling model
    	modelGenerateInChIKeysOption.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				// Enable or disable the model
				modelWithDependency.setEnabled(modelGenerateInChIKeysOption.getBooleanValue());
			}
		});
        
        // Enable this model based on the dependent model's state
        modelWithDependency.setEnabled(modelGenerateInChIKeysOption.getBooleanValue());
    	
        return modelWithDependency;
    }
    
    /**
     * Creates the settings model for the boolean flag to determine, if
     * we want to generate the Return Code column as extra information 
     * about InChI code generation. The default is false.
     * Note: This option works only if the overall extra information option 
     * is switched on.
     * 
     * @return Settings model for check box whether to generate the Return Code column.
     */
    static final SettingsModelBoolean createExtraReturnCodeOptionModel() {
    	return new SettingsModelBoolean("generate_return_code", false);
    }
    
    /**
     * Creates the settings model for the boolean flag to determine, if
     * we want to generate the Aux Info column as extra information 
     * about InChI code generation. The default is false.
     * Note: This option works only if the overall extra information option 
     * is switched on.
     * 
     * @return Settings model for check box whether to generate the Aux Info column.
     */
    static final SettingsModelBoolean createExtraAuxInfoOptionModel() {
    	return  new SettingsModelBoolean("generate_aux_info", false);
    }
    
    /**
     * Creates the settings model for the boolean flag to determine, if
     * we want to generate the Message column as extra information 
     * about InChI code generation. The default is false.
     * Note: This option works only if the overall extra information option 
     * is switched on.
     * 
     * @return Settings model for check box whether to generate the Message column.
     */
    static final SettingsModelBoolean createExtraMessageOptionModel() {
    	return  new SettingsModelBoolean("generate_message", false);
    }
    
    /**
     * Creates the settings model for the boolean flag to determine, if
     * we want to generate the Log column as extra information 
     * about InChI code generation. The default is false.
     * Note: This option works only if the overall extra information option 
     * is switched on.
     * 
     * @return Settings model for check box whether to generate the Log column.
     */
    static final SettingsModelBoolean createExtraLogOptionModel() {
    	return new SettingsModelBoolean("generate_log", false);
    }

    /**
     * Creates the settings model to be used to specify the new column name
     * for extra information about InChI code generation.
     * 
     * @param m_modelExtraInformationOption Option to switch on/off extra information
     * 		generation. Must not be null.
     * 
     * @return Settings model for result column name (Extra Information).
     */
    static final SettingsModelString createExtraInformationColumnNamePrefixModel() {
    	return new SettingsModelString("new_extra_info_column_name_prefix", null);
    }
    
    /**
     * Creates the settings model to be used to specify advanced options
     * for the InChI code generation.
     * 
     * @return Settings model for result column name (InChI Codes).
     */
    static final SettingsModelString createAdvancedOptionsModel() {
        return new SettingsModelString("advanced_opions", "");
    }
}
