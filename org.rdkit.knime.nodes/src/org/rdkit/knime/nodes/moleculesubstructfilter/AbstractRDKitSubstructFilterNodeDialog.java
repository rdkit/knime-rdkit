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
package org.rdkit.knime.nodes.moleculesubstructfilter;

import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.moleculesubstructfilter.AbstractRDKitSubstructFilterNodeModel.MatchingCriteria;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.DialogComponentEnumButtonGroup;
import org.rdkit.knime.util.LayoutUtils;
import org.rdkit.knime.util.SettingsModelEnumeration;

/**
 * <code>NodeDialog</code> for the "RDKitMoleculeSubstructFilter" Node.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Manuel Schwarze
 */
public abstract class AbstractRDKitSubstructFilterNodeDialog extends DefaultNodeSettingsPane {

	//
	// Constructor
	//
	
    /**
     * Create a new dialog pane with default components to configure an input column,
     * the name of a new column, which will contain the calculation results, an option
     * to tell, if the source column shall be removed from the result table.
     */
    @SuppressWarnings("unchecked")
	protected AbstractRDKitSubstructFilterNodeDialog() {
    	DialogComponent compInputColumn = add(new DialogComponentColumnNameSelection(
                createInputColumnNameModel(), "RDKit Mol column: ", 0,
                RDKitMolValue.class));
    	DialogComponent compQueryColumn = add(createQueryColumnNameSelectionComponent());
        final SettingsModelEnumeration<MatchingCriteria> modelMatchingCriteria =
    		createMatchingCriteriaModel();
    	DialogComponent compMatchingCriteria = add(new DialogComponentEnumButtonGroup<MatchingCriteria>(
    			modelMatchingCriteria, true, null));
    	DialogComponent compMinimumMatches = add(new DialogComponentNumber(
        		createMinimumMatchesModel(modelMatchingCriteria), null, 1, 3));
    	DialogComponent compNewColumnName = add(new DialogComponentString(
                createNewColumnNameModel(), "New column name for matching substructures: "));
    	
    	// Relayout the components
    	JPanel panel = (JPanel)getTab("Options");
    	panel.setLayout(new GridBagLayout());
    	
    	int iRow = 0;
    	LayoutUtils.constrain(panel, compInputColumn.getComponentPanel(), 
    			0, iRow++, LayoutUtils.REMAINDER, 1, 
    			LayoutUtils.NONE, LayoutUtils.CENTER, 0.0d, 0.0d,
    			0, 10, 0, 10);
    	LayoutUtils.constrain(panel, compQueryColumn.getComponentPanel(), 
    			0, iRow++, LayoutUtils.REMAINDER, 1, 
    			LayoutUtils.NONE, LayoutUtils.CENTER, 0.0d, 0.0d,
    			0, 10, 0, 10);
    	LayoutUtils.constrain(panel, new JLabel("Match:", JLabel.RIGHT), 
    			0, iRow, 1, 1, 
    			LayoutUtils.HORIZONTAL, LayoutUtils.NORTHEAST, 1.0d, 0.0d,
    			9, 10, 0, 0);
    	LayoutUtils.constrain(panel, compMatchingCriteria.getComponentPanel(), 
    			1, iRow, 1, 1, 
    			LayoutUtils.NONE, LayoutUtils.NORTHEAST, 0.0d, 0.0d,
    			0, 10, 7, 0);
    	LayoutUtils.constrain(panel, compMinimumMatches.getComponentPanel(), 
    			2, iRow, 1, 1, 
    			LayoutUtils.NONE, LayoutUtils.SOUTHWEST, 0.0d, 0.0d,
    			0, 0, 9, 0);
    	LayoutUtils.constrain(panel, new JPanel(), 
    			3, iRow++, LayoutUtils.REMAINDER, 1, 
    			LayoutUtils.HORIZONTAL, LayoutUtils.NORTHWEST, 1.0d, 0.0d,
    			0, 0, 0, 10);
    	LayoutUtils.constrain(panel, compNewColumnName.getComponentPanel(), 
    			0, iRow++, LayoutUtils.REMAINDER, LayoutUtils.REMAINDER, 
    			LayoutUtils.NONE, LayoutUtils.CENTER, 0.0d, 0.0d,
    			0, 10, 0, 10);
    }
    
    //
    // Protected Methods
    //
    
    /**
     * Creates the dialog component to select the query column with the correct
     * data value type. 
     * 
     * @return Dialog component for selecting the query column. Must not be null.
     */
    protected abstract DialogComponentColumnNameSelection createQueryColumnNameSelectionComponent();
    
    //
    // Private Methods
    //
    
    private DialogComponent add(DialogComponent comp) {
    	addDialogComponent(comp);
    	return comp;
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
     * Creates the settings model to be used for the query column.
     * 
     * @return Settings model for query column selection.
     */
    protected static final SettingsModelString createQueryColumnNameModel() {
        return new SettingsModelString("query_column", null);
    }    
       
    /**
     * Creates the settings model for specifying the matching criteria.
     * 
     * @return Settings model for the matching criteria.
     */
    static final SettingsModelEnumeration<MatchingCriteria> createMatchingCriteriaModel() {
    	return new SettingsModelEnumeration<AbstractRDKitSubstructFilterNodeModel.MatchingCriteria>(
    			MatchingCriteria.class, "matching", MatchingCriteria.All);
    }
    
    /**
     * Creates the settings model for specifying the minimum matching number
     * in case that the criteria "At least" was selected. Default is 1.
     * The range of numbers goes from 1 to 999.
     * 
     * @return Settings model for the matching criteria number of "At least".
     */
    static final SettingsModelIntegerBounded createMinimumMatchesModel(
    		final SettingsModelEnumeration<MatchingCriteria> modelMatchingCriteria) {
    	final SettingsModelIntegerBounded model = 
    		new SettingsModelIntegerBounded("minimumMatches", 1, 1, 999) {
    		
    		/**
    		 * Backward compatibility handling.
    		 */
    		@Override
    		public void setIntValue(int newValue) {
    			// These values are out of range, but could exist from an old node version.
    			// We correct them here and apply the matching criteria they were expressing.
    			if (newValue == 0) {
    				newValue = 1;
    				modelMatchingCriteria.setValue(MatchingCriteria.All);
    			}
    			else if (newValue == -1) {
    				newValue = 1;
    				modelMatchingCriteria.setValue(MatchingCriteria.Exact);
    			}
    			else if (newValue > 0 && modelMatchingCriteria.wasUndefinedInSettings()) {
       				modelMatchingCriteria.setValue(MatchingCriteria.AtLeast);
       			}
    			
    			super.setIntValue(newValue);
    		}
    	};
    	
    	// This model will depend on the state of the matching criteria
        modelMatchingCriteria.addChangeListener(new ChangeListener() {
			
        	/**
        	 * We use this to enable or disable the minimum number
        	 * field, which makes only sense if "At least" is selected.
        	 */
			@Override
			public void stateChanged(ChangeEvent e) {
				model.setEnabled(modelMatchingCriteria.getValue() == MatchingCriteria.AtLeast);
			}
		});
        
        model.setEnabled(modelMatchingCriteria.getValue() == MatchingCriteria.AtLeast);
    	
    	return model;
    }  
    
    /**
     * Creates the settings model to be used to specify the new column name.
     * 
     * @return Settings model for result column name.
     */
    static final SettingsModelString createNewColumnNameModel() {
        return new SettingsModelString("new_column_name", null);
    }
}
