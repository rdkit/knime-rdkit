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
package org.rdkit.knime.nodes.rdkit2molecule;

import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.knime.chem.types.SdfCell;
import org.knime.chem.types.SdfCellFactory;
import org.knime.chem.types.SmilesCell;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.AbstractRDKitCalculatorNodeModel;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsModelEnumeration;
import org.rdkit.knime.util.SettingsUtils;

/**
 * This class implements the node model of the RDKit2Molecule node
 * providing calculations based on the open source RDKit library.
 * 
 * @author Manuel Schwarze 
 */
public class RDKit2MoleculeConverterNodeModel extends AbstractRDKitCalculatorNodeModel {

	//
	// Enumeration
	//

	/** Defines supported destination formats. */
    public enum DestinationFormat {
    	
        Smiles {
        	@Override
        	public String toString() {
        		return "Smiles";
        	}
        	
        	@Override
        	public DataType getDataType() {
        		return SmilesCell.TYPE;
        	}
        	
        	@Override
        	public DataCell convertRdkitMolecule(ROMol mol) {
        		return new SmilesCell(RDKFuncs.MolToSmiles(mol, true)); 
        	}
        },
        
        SDF {
        	@Override
        	public String toString() {
        		return "SDF";
        	}
        	@Override
        	public DataType getDataType() {
        		return SdfCell.TYPE;
        	}
        	
        	@Override
        	public DataCell convertRdkitMolecule(ROMol mol) {
    			// Calculate 2D Coordinates, if necessary
                if(mol.getNumConformers() == 0) {
                    mol.compute2DCoords();
                }
                
                // Fix SDF value
                String strSdf = RDKFuncs.MolToMolBlock(mol);
                // KNIME SDF type requires string to be terminated
                // by $$$$ -- see org.knime.chem.types.SdfValue for details
                if (!strSdf.endsWith(SDF_POSTFIX)) {
                	strSdf += SDF_POSTFIX;
                }
                
                // Create the SDF cell
                return SdfCellFactory.create(strSdf);
        	}
        };
        
        /**
         * Determines the target cell data type of KNIME for this destination format.
         * 
         * @return KNIME Data Type associated with the destination format. 
         * 		Null, if not implemented.
         */
        public DataType getDataType() {
        	return null;
        }
        
        /**
         * Returns by default a missing cell. Every enumeration value should
         * implement its own conversion.
         * 
         * @param mol RDKit Molecule to be converted.
         * 
         * @return The converted data cell. Missing cell, if not implemented.
         */
        public DataCell convertRdkitMolecule(ROMol mol) {
        	return DataType.getMissingCell();
        }
    }
	
	//
	// Constants
	//
	
	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKit2MoleculeConverterNodeModel.class);
	
	
	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;
	
	/** The string KNIME expects an SDF string to end with. */
	private static final String SDF_POSTFIX = "\n$$$$\n";
	
	//
	// Members
	//
	
	/** Settings model for the column name of the input column. */
    private final SettingsModelString m_modelInputColumnName =
            registerSettings(RDKit2MoleculeConverterNodeDialog.createInputColumnNameModel(), "input_column", "first_column");
    // Accept also deprecated keys

    /** Settings model for the column name of the new column to be added to the output table. */
    private final SettingsModelString m_modelNewColumnName =
    		registerSettings(RDKit2MoleculeConverterNodeDialog.createNewColumnNameModel());

    /** Settings model for the option to remove the source column from the output table. */
    private final SettingsModelBoolean m_modelRemoveSourceColumns =
    		registerSettings(RDKit2MoleculeConverterNodeDialog.createRemoveSourceColumnsOptionModel());

    /** Settings model for the option to remove the source column from the output table. */
    private final SettingsModelEnumeration<DestinationFormat> m_modelDestinationFormat =
    		registerSettings(RDKit2MoleculeConverterNodeDialog.createDestinationFormatModel());
     
    //
    // Constructor
    //
    
    /**
     * Create new node model with one data in- and one out-port.
     */
    RDKit2MoleculeConverterNodeModel() {
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
        		"No RDKit Mol compatible column in input table. Use \"Molecule to RDKit\" " +
        			"node to convert Smiles or SDF.", getWarningConsolidator()); 

        // Determines, if the input column exists - fails if it does not
        SettingsUtils.checkColumnExistence(inSpecs[0], m_modelInputColumnName, RDKitMolValue.class,  
        		"Input column has not been specified yet.",
        		"Input column %COLUMN_NAME% does not exist. Has the input table changed?");
        
        // Auto guess the new column name and make it unique
        String strInputColumnName = m_modelInputColumnName.getStringValue();
        SettingsUtils.autoGuessColumnName(inSpecs[0], null, 
        		(m_modelRemoveSourceColumns.getBooleanValue() ? 
        			new String[] { strInputColumnName } : null),
        		m_modelNewColumnName, strInputColumnName + " (Molecule)");

        // Determine, if the new column name has been set and if it is really unique
        SettingsUtils.checkColumnNameUniqueness(inSpecs[0], null,
        		(m_modelRemoveSourceColumns.getBooleanValue() ? new String[] { 
        			m_modelInputColumnName.getStringValue() } : null),
        		m_modelNewColumnName, 
        		"Output column has not been specified yet.",
        		"The name %COLUMN_NAME% of the new column exists already in the input.");

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
    		// Generate column specs for the output table columns produced by this factory
    		DataColumnSpec[] arrOutputSpec = new DataColumnSpec[1]; // We have only one output column
    		arrOutputSpec[0] = new DataColumnSpecCreator(
    				m_modelNewColumnName.getStringValue(), m_modelDestinationFormat.getValue().getDataType())
    				.createSpec();
    		
    		final DestinationFormat destType = m_modelDestinationFormat.getValue();
    
    		// Generate factory 
    	    arrOutputFactories[0] = new AbstractRDKitCellFactory(this, AbstractRDKitCellFactory.RowFailurePolicy.DeliverEmptyValues,
           		getWarningConsolidator(), null, arrOutputSpec) {
	   			
	   		    /**
	   		     * This method implements the calculation logic to generate the new cells based on 
	   		     * the input made available in the first (and second) parameter.
	   		     * {@inheritDoc}
	   		     */
	   			@Override
	   		    public DataCell[] process(InputDataInfo[] arrInputDataInfo, DataRow row, int iUniqueWaveId) throws Exception {
	   		    	DataCell outputCell = null;
	   		    	
	   		    	// Calculate the new cells
	   		    	ROMol mol = markForCleanup(arrInputDataInfo[INPUT_COLUMN_MOL].getROMol(row), iUniqueWaveId);    
	   		    	outputCell = destType.convertRdkitMolecule(mol);
	   		    	
	   		        return new DataCell[] { outputCell };
	   		    }
	   		};
	   		
	   		// Enable or disable this factory to allow parallel processing 		
	   		arrOutputFactories[0].setAllowParallelProcessing(true);	   		
    	}
    	
    	return (arrOutputFactories == null ? new AbstractRDKitCellFactory[0] : arrOutputFactories);
    }
	
    /**
     * {@inheritDoc}
     * This implementation removes additionally the compound source column, if specified in the settings.
     */
	protected ColumnRearranger createColumnRearranger(int outPort,
			DataTableSpec inSpec) throws InvalidSettingsException {
    	// Perform normal work 
        ColumnRearranger result = super.createColumnRearranger(outPort, inSpec);
        
        // Remove the input column, if desired
        if (m_modelRemoveSourceColumns.getBooleanValue()) {
            result.remove(createInputDataInfos(0, inSpec)[INPUT_COLUMN_MOL].getColumnIndex());
        }
        
        return result;
    } 
}
