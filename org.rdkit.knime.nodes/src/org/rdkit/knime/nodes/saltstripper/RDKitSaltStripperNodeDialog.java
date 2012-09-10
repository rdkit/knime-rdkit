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
package org.rdkit.knime.nodes.saltstripper;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButton;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.FileUtils;

/**
 * <code>NodeDialog</code> for the "RDKitSaltStripper" Node.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Dillip K Mohanty
 * @author Manuel Schwarze
 */
public class RDKitSaltStripperNodeDialog extends DefaultNodeSettingsPane {

	//
	// Constants
	//
	
	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitSaltStripperNodeDialog.class);

	//
	// Members
	//

	/** Setting model component for the salt column selector. */
	private final DialogComponent m_compSaltColumnName;

	/** Label component for showing information about predefined salts. */
	private final DialogComponentLabel m_compLabelNoSaltTable;

	/** Button component for letting the user open details about predefined salts. */
	private final DialogComponentButton m_compButtonShowPredefinedSalts;

	//
	// Constructor
	//
	
    /**
     * Create a new dialog pane with default components to configure an input column,
     * the name of a new column, which will contain the calculation results, an option
     * to tell, if the source column shall be removed from the result table and
     * an optional salt column (if a second table is connected to the node).
     */
    @SuppressWarnings("unchecked")
	RDKitSaltStripperNodeDialog() {
        super.addDialogComponent(new DialogComponentColumnNameSelection(
                createInputColumnNameModel(), "RDKit Mol column: ", 0,
                RDKitMolValue.class));
        super.addDialogComponent(new DialogComponentString(
                createNewColumnNameModel(), "New column name: "));
        super.addDialogComponent(new DialogComponentBoolean(
                createRemoveSourceColumnsOptionModel(), "Remove source column"));
        super.addDialogComponent(m_compLabelNoSaltTable = new DialogComponentLabel(
        		"<html><font color='red'>There is no salt table connected.</font>" +
        		"<br>Using predefined salts.</html>"));
        super.addDialogComponent(m_compButtonShowPredefinedSalts = new DialogComponentButton(
				"Show Predefined Salts..."));
    	super.addDialogComponent(m_compSaltColumnName = new DialogComponentColumnNameSelection(
    			createOptionalSaltColumnNameModel(), "Salt definition column: ", 1,
    			RDKitMolValue.class) {
        	
        	/**
        	 * Hides or shows the optional components depending on
        	 * the existence of a second input table.
        	 */
        	@Override
        	protected void checkConfigurabilityBeforeLoad(
        			PortObjectSpec[] specs)
        			throws NotConfigurableException {
        		
        		boolean bHasReactionTable = 
        			RDKitSaltStripperNodeModel.hasSaltInputTable(specs);
        		
        		// Only check correctness of second input table if it is there
        		if (bHasReactionTable) {
        			super.checkConfigurabilityBeforeLoad(specs);
        		}
        
        		// Always show or hide proper components
        		updateVisibilityOfOptionalComponents(bHasReactionTable);
        	}
        });    
    	
    	// Configure the button to show dialog with salt definitions to the user
    	m_compButtonShowPredefinedSalts.addActionListener(new ActionListener() {
			
    		/**
    		 * Opens the salt definition dialog.
    		 */
			@Override
			public void actionPerformed(ActionEvent e) {
				showSaltDefinitionDialog();
			}
		});

        // Although we are not using any GridBagLayout constraints, setting this 
        // layout manager makes it look nicer (surprisingly)
        Component comp = getTab("Options");
        if (comp instanceof JPanel) {
        	((JPanel)comp).setLayout(new GridBagLayout());
        }    	
    }
    
    //
    // Protected Methods
    //

    /**
     * Show or hides salt based settings based on the input method for
     * the salt (default internal salt table or connected salt table).
     * 
     * @param bHasSecondTable
     */
    protected void updateVisibilityOfOptionalComponents(boolean bHasSecondTable) {
    	m_compSaltColumnName.getComponentPanel().setVisible(bHasSecondTable);
    	m_compLabelNoSaltTable.getComponentPanel().setVisible(!bHasSecondTable);
    	m_compButtonShowPredefinedSalts.getComponentPanel().setVisible(!bHasSecondTable);
    }
    
    /**
     * Creates and shows the salt definitions.
     */
    protected void showSaltDefinitionDialog() {
    	String strSaltDefinitions = "Unable to access resource " + 
    		RDKitSaltStripperNodeModel.SALT_DEFINITION_FILE;
    	
    	try {
    		strSaltDefinitions = FileUtils.getContentFromResource(
    				this, RDKitSaltStripperNodeModel.SALT_DEFINITION_FILE);
    	}
    	catch (IOException exc) {
    		LOGGER.warn("Unable to access resource " + 
    				RDKitSaltStripperNodeModel.SALT_DEFINITION_FILE, exc);
    	}
    	
    	JOptionPane.showOptionDialog(m_compButtonShowPredefinedSalts.getComponentPanel(), 
    			createSaltDefinitionComponent(strSaltDefinitions), "Predefined Salt Definitions", 
    			JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, 
    			new Object[] { "Close" }, "Close");
    }
    
    /**
     * Creates a scrollable text area, which is disabled for editing. It
     * contains the passed in salt definitions.
     * 
     * @param strSaltDefinitions Salt definitions. Must not be null.
     * 
     * @return Component with salts.
     */
    protected Component createSaltDefinitionComponent(String strSaltDefinitions) {
    	JTextArea ta = new JTextArea(strSaltDefinitions, 25, 90);
    	ta.setEditable(false);
    	return new JScrollPane(ta);
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
     * Creates the settings model for the name of the salt column 
     * (if a second input table is used).
     * 
     * @return Settings model for the name of the reaction column.
     */
    static final SettingsModelString createOptionalSaltColumnNameModel() {
        return new SettingsModelString("salt_input", null);
    }


    /**
     * Creates the settings model to be used to specify the new column name.
     * 
     * @return Settings model for result column name.
     */
    static final SettingsModelString createNewColumnNameModel() {
        return new SettingsModelString("new_column_name", null);
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
}
