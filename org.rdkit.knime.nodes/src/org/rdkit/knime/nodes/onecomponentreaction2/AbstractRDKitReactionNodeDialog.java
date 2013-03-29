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
package org.rdkit.knime.nodes.onecomponentreaction2;

import java.awt.Component;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

import org.knime.chem.types.RxnValue;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

/**
 * <code>NodeDialog</code> for the "RDKitOneComponentReaction" Node.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Greg Landrum
 * @author Manuel Schwarze
 */
public abstract class AbstractRDKitReactionNodeDialog extends DefaultNodeSettingsPane {

	//
	// Members
	//

	/** Setting model component for the reaction column selector. */
	private final DialogComponent m_compReactionColumnName;
    
	/** Setting model component for the SMARTS reaction field. */
    private final DialogComponent m_compSmartsReactionField;

	/** Setting model component for the uniquify products field. */
    private final DialogComponent m_uniquifyProductsField;
    
    /** The input port index of the reaction table. */
    private final int m_iReactionTableIndex;
    
	//
	// Constructor
	//
	
    /**
     * Create a new dialog pane with default components to configure an input column,
     * the name of a new column, which will contain the calculation results, an option
     * to tell, if the source column shall be removed from the result table.
     */
    @SuppressWarnings("unchecked")
	public AbstractRDKitReactionNodeDialog(int iReactionTableIndex) {
    	m_iReactionTableIndex = iReactionTableIndex;
    	
    	addDialogComponentsBeforeReactionSettings();
        
    	super.addDialogComponent(m_compReactionColumnName = new DialogComponentColumnNameSelection(
        		createOptionalReactionColumnNameModel(), "RDKit Rxn column: ", m_iReactionTableIndex,
                RxnValue.class) {
        	
        	/**
        	 * Hides or shows the optional components depending on
        	 * the existence of a second input table.
        	 */
        	@Override
        	protected void checkConfigurabilityBeforeLoad(
        			PortObjectSpec[] specs)
        			throws NotConfigurableException {
        		
        		boolean bHasReactionTable = 
        			AbstractRDKitReactionNodeModel.hasReactionInputTable(specs, m_iReactionTableIndex);
        		
        		// Only check correctness of second input table if it is there
        		if (bHasReactionTable) {
        			super.checkConfigurabilityBeforeLoad(specs);
        		}
        
        		// Always show or hide proper components
        		updateVisibilityOfOptionalComponents(bHasReactionTable);
        	}
        });
        super.addDialogComponent(m_compSmartsReactionField = new DialogComponentString(
        		createOptionalReactionSmartsPatternModel(), "Reaction SMARTS: ", false, 30));

        super.addDialogComponent(m_uniquifyProductsField = new DialogComponentBoolean(
        		createUniquifyProductsModel(), "Uniquify products?"));
        
    	addDialogComponentsAfterReactionSettings();

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
     * This method adds all dialog components, which shall appear before the
     * reaction based settings.
     */
    protected abstract void addDialogComponentsBeforeReactionSettings();

    /**
     * This method adds all dialog components, which shall appear after the
     * reaction based settings.
     */
    protected abstract void addDialogComponentsAfterReactionSettings();

    /**
     * Show or hides reaction based settings based on the input method for
     * the reaction (SMARTS text field or table).
     * 
     * @param bHasSecondTable
     */
    protected void updateVisibilityOfOptionalComponents(boolean bHasSecondTable) {
    	m_compReactionColumnName.getComponentPanel().setVisible(bHasSecondTable);
    	m_compSmartsReactionField.getComponentPanel().setVisible(!bHasSecondTable);
    }

    //
    // Static Methods
    //

    /**
     * Creates the settings model for the name of the reaction column 
     * (if a second input table is used).
     * 
     * @return Settings model for the name of the reaction column.
     */
    static final SettingsModelString createOptionalReactionColumnNameModel() {
        return new SettingsModelString("rxnColumn", null);
    }

    /**
     * Creates the settings model for the reaction smarts pattern 
     * (if no second input table is used).
     * 
     * @return Settings model for the reaction smarts pattern.
     */
    static final SettingsModelString createOptionalReactionSmartsPatternModel() {
        return new SettingsModelString("reactionSmarts", "");
    }
    /**
     * @return new settings model whether to also compute coordinates
     */
    static final SettingsModelBoolean createUniquifyProductsModel() {
        return new SettingsModelBoolean("uniquifyProducts", false);
    }

 

}
