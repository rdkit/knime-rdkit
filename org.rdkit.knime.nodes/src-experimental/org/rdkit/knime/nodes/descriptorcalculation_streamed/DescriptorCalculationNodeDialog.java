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
package org.rdkit.knime.nodes.descriptorcalculation2;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JList;

import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.DialogComponentEnumFilterPanel;
import org.rdkit.knime.util.SettingsModelEnumerationArray;

/**
 * <code>NodeDialog</code> for the "RDKitDescriptorCalculation" Node.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Dillip K Mohanty
 * @author Manuel Schwarze
 */
public class DescriptorCalculationNodeDialog extends DefaultNodeSettingsPane {

	//
	// Constructor
	//
	
    /**
     * Create a new dialog pane with default components to configure an input column,
     * the name of a new column, which will contain the calculation results, an option
     * to tell, if the source column shall be removed from the result table.
     */
    @SuppressWarnings("unchecked")
	DescriptorCalculationNodeDialog() {
        super.addDialogComponent(new DialogComponentColumnNameSelection(
                createInputColumnNameModel(), "RDKit Mol column: ", 0,
                RDKitMolValue.class));
        
        DialogComponentEnumFilterPanel<Descriptor> panelDescriptors =
        	new DialogComponentEnumFilterPanel<Descriptor>(
        		createDescriptorsModel(), "Available descriptors: (Hover your mouse over a descriptor to get a short description)", null, true);
        panelDescriptors.setListCellRenderer(new DefaultListCellRenderer() {

        	//
        	// Constants
        	//
        	
            /** Serial number. */
			private static final long serialVersionUID = -3432992669822820183L;

			/** Icon used for flow variable placeholders and unknown items. */
			private final Icon UNKNOWN_ICON = DataValue.UTILITY.getIcon();

			/** Icon used for list items of descriptors that calculate more than one column. */
			private final Icon MULTI_VALUE_ICON = CollectionDataValue.UTILITY.getIcon();
			
			//
			// Public Methods
			//
			
			/**
             * {@inheritDoc}
             */
            @Override
            public Component getListCellRendererComponent(
                    final JList list, final Object value, final int index,
                    final boolean isSelected, final boolean cellHasFocus) {
                // The super method will reset the icon if we call this method
                // last. So we let super do its job first and then we take care
                // that everything is properly set.
                Component c =  super.getListCellRendererComponent(list, value, index,
                        isSelected, cellHasFocus);
                
                assert (c == this);
                
                if (value instanceof Descriptor) {
                	Descriptor descriptor = (Descriptor)value;
                	
                	// Set text
                    setText(descriptor.toString());
                    
                    // Set icon
                    DataType[] arrDataTypes = descriptor.getDataTypes(); 
                    if (arrDataTypes != null && arrDataTypes.length == 1) {
                    	setIcon(arrDataTypes[0].getIcon());
                    }
                    else if (arrDataTypes != null && arrDataTypes.length > 1){
                    	setIcon(MULTI_VALUE_ICON);
                    }
                    else {
                        setIcon(UNKNOWN_ICON);                    	
                    }

                	// Set tooltip
                    String strTooltip = descriptor.getDescription();
                    if (strTooltip != null) {
                    	strTooltip = "<html>" + 
                    	strTooltip.
                			replace("<=", "&le;"). 
                			replace(">=", "&ge;"). 
                    		replace("<", "&lt;"). 
                    		replace(">", "&gt;"). 
                    		replace("\n", "<br>") + 
                    	"</html>";
                    }
                    
                	list.setToolTipText(strTooltip);
                }
                
                
                return this;
            }        	
        });
        
        super.addDialogComponent(panelDescriptors);
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
     * Creates the settings model to be used for the selected descriptors.
     * All descriptors are added as default value.
     * 
     * @return Settings model for selected descriptors.
     */
    static final SettingsModelEnumerationArray<Descriptor> createDescriptorsModel() {
        return new SettingsModelEnumerationArray<Descriptor>(Descriptor.class, 
        		"selectedDescriptors", Descriptor.class.getEnumConstants());
    }
}
