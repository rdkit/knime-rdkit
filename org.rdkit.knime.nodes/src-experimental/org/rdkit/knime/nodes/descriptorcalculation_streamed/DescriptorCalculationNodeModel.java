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

import java.util.ArrayList;
import java.util.List;

import org.RDKit.ROMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.AbstractRDKitStreamableCalculatorNodeModel;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsModelEnumerationArray;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class implements the node model of the RDKitDescriptorCalculation node
 * providing calculations based on the open source RDKit library.
 * 
 * @author Dillip K Mohanty
 * @author Manuel Schwarze 
 */
public class DescriptorCalculationNodeModel extends AbstractRDKitStreamableCalculatorNodeModel {

	//
	// Constants
	//
	
	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(DescriptorCalculationNodeModel.class);
	
	
	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;
	
	//
	// Members
	//
	
	/** Settings model for the column name of the input column. */
    private final SettingsModelString m_modelInputColumnName =
            registerSettings(DescriptorCalculationNodeDialog.createInputColumnNameModel(), "input_column", "colName");
    // Accept also deprecated keys
    
    /** Settings model for selected descriptors. */
    private final SettingsModelEnumerationArray<Descriptor> m_modelDescriptors = 
    		registerSettings(DescriptorCalculationNodeDialog.createDescriptorsModel());
    
    //
    // Constructor
    //
    
    /**
     * Create new node model with one data in- and one out-port.
     */
    DescriptorCalculationNodeModel() {
        super(1, 1);
    }

    //
    // Protected Methods
    //

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
    	// Reset warnings and check RDKit library readiness
    	super.configure(inSpecs);
    	
        // Auto guess the input column if not set - fails if no compatible column found
        SettingsUtils.autoGuessColumn(inSpecs[0], m_modelInputColumnName, RDKitMolValue.class, 0, 
        		"Auto guessing: Using column %COLUMN_NAME%.", 
        		"No RDKit Mol, SMILES or SDF compatible column in input table. Use the \"Molecule to RDKit\" " +
        			"node to convert SMARTS.", getWarningConsolidator()); 

        // Determines, if the input column exists - fails if it does not
        SettingsUtils.checkColumnExistence(inSpecs[0], m_modelInputColumnName, RDKitMolValue.class,  
        		"Input column has not been specified yet.",
        		"Input column %COLUMN_NAME% does not exist. Has the input table changed?");

        // Check, if descriptor selection is empty
        Descriptor[] arrDescriptors = m_modelDescriptors.getValues();
        if (arrDescriptors == null || arrDescriptors.length == 0) {
        	getWarningConsolidator().saveWarning(
        			"There is no descriptor selected. The result table will be the same as the input table.");
        }
        
        // Consolidate all warnings and make them available to the user
        generateWarnings();

        // Generate output specs
        return getOutputTableSpecs(inSpecs);
    }

    /**
     * This implementation generates input data info object for the input mol column
     * and connects it with the information coming from the appropriate setting model.
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
	protected InputDataInfo[] createInputDataInfos(int inPort, DataTableSpec inSpec)
		throws InvalidSettingsException {
    	
    	InputDataInfo[] arrDataInfo = null;
    	
    	// Specify input of table 1
    	if (inPort == 0) {
    		arrDataInfo = new InputDataInfo[1]; // We have only one input column
    		arrDataInfo[INPUT_COLUMN_MOL] = new InputDataInfo(inSpec, m_modelInputColumnName, 
    				InputDataInfo.EmptyCellPolicy.DeliverEmptyRow, null,
    				RDKitMolValue.class);
    	}
    	
    	return (arrDataInfo == null ? new InputDataInfo[0] : arrDataInfo);
    }
    
    /**
     * {@inheritDoc}
     */
	protected AbstractRDKitCellFactory[] createOutputFactories(int outPort, DataTableSpec inSpec)
		throws InvalidSettingsException {
    	
		AbstractRDKitCellFactory[] arrOutputFactories = null;
    	
    	// Specify output of table 1
    	if (outPort == 0) {
    		// Allocate space for all factories (usually we have only one)
    		arrOutputFactories = new AbstractRDKitCellFactory[1]; 

    		// Factory 1:
    		// ==========
    		final Descriptor[] arrDescriptors = m_modelDescriptors.getValues();
    		final DataColumnSpec[] arrNewColumns = createDescriptorColumnSpecs(inSpec);
    		final int iNewColumnCount = arrNewColumns.length;
    		final WarningConsolidator warningConsolidator = getWarningConsolidator();
    		
    		// Generate factory 
    	    arrOutputFactories[0] = new AbstractRDKitCellFactory(this, AbstractRDKitCellFactory.RowFailurePolicy.DeliverEmptyValues,
           		getWarningConsolidator(), null, arrNewColumns) {
	   			
	   			@Override
	   		    /**
	   		     * This method implements the calculation logic to generate the new cells based on 
	   		     * the input made available in the first (and second) parameter.
	   		     * {@inheritDoc}
	   		     */
	   		    public DataCell[] process(InputDataInfo[] arrInputDataInfo, DataRow row, int iUniqueWaveId) throws Exception {
	   		    	DataCell[] arrAllResults = new DataCell[iNewColumnCount];

	   		    	// Calculate the new cells
	   		    	ROMol mol = markForCleanup(arrInputDataInfo[INPUT_COLUMN_MOL].getROMol(row), iUniqueWaveId);    

	   		    	int iOffset = 0;
	   		    	for (Descriptor descriptor : arrDescriptors) {
	   		    		if (descriptor != null) {
		   		    		for (DataCell cell : descriptor.calculate(mol, warningConsolidator)) {
		   		    			arrAllResults[iOffset++] = cell;
		   		    		}
	   		    		}
	   		    	}
	   		    	
	   		        return arrAllResults;
	   		    }
	   		};
	   		
	   		// Enable or disable this factory to allow parallel processing 		
	   		arrOutputFactories[0].setAllowParallelProcessing(true);	   		
    	}
    	
    	return (arrOutputFactories == null ? new AbstractRDKitCellFactory[0] : arrOutputFactories);
    }
	
	//
	// Private Methods
	//
	
    /**
     * Returns the column specifications for the new descriptor columns to be created
     * by this node.
     * 
     * @param inSpec Input table specification to be used to check that new column
     * 		names are unique.
     * 
     * @return The specification of all descriptor columns to be created.
     * 
     * @see #createOutputFactories(int)
     */
	private DataColumnSpec[] createDescriptorColumnSpecs(DataTableSpec inSpec) {
		final Descriptor[] arrDescriptors = m_modelDescriptors.getValues();
		final List<DataColumnSpec> listNewColumns = new ArrayList<DataColumnSpec>();
		
		if (arrDescriptors != null) {
			List<String> listNewNames = new ArrayList<String>();
			
			for (Descriptor descriptor : arrDescriptors) {
				if (descriptor != null) {
					int iColumnCount = descriptor.getColumnCount();
					DataType[] arrTypes = descriptor.getDataTypes();
					String[] arrTitles = descriptor.getPreferredColumnTitles();
					
					for (int i = 0; i < iColumnCount; i++) {
						String strUniqueColumnName = SettingsUtils.makeColumnNameUnique(
								arrTitles[i], inSpec, listNewNames);
						listNewNames.add(strUniqueColumnName);
	    				listNewColumns.add(new DataColumnSpecCreator(strUniqueColumnName, arrTypes[i]).createSpec());
					}
				}
			}
		}
		
		return listNewColumns.toArray(new DataColumnSpec[listNewColumns.size()]);
	}
}
