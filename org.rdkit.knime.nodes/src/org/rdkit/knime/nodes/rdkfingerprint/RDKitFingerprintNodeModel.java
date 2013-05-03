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
package org.rdkit.knime.nodes.rdkfingerprint;

import org.RDKit.ExplicitBitVect;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.RDKit.UInt_Vect;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.AbstractRDKitCalculatorNodeModel;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsModelEnumeration;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class implements the node model of the "RDKitFingerprint" node
 * providing calculations based on the open source RDKit library.
 * 
 * @author Greg Landrum
 * @author Manuel Schwarze
 */
public class RDKitFingerprintNodeModel extends AbstractRDKitCalculatorNodeModel {
	
	//
	// Enumeration
	//

	/** Defines supported fingerprint types. */
    public enum FingerprintType {
        morgan, featmorgan, atompair, torsion, rdkit, avalon, layered, maccs;
        
        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
        	
        	switch (this) {
        		case morgan:
        			return "Morgan";
        		case featmorgan:
        			return "FeatMorgan";
        		case atompair:
        			return "AtomPair";
        		case torsion:
        			return "Torsion";
        		case rdkit:
        			return "RDKit";
        		case avalon:
        			return "Avalon";
        		case layered:
        			return "Layered";
        		case maccs:
        			return "MACCS";
        	}
        	
        	return super.toString();
        }
    }

	//
	// Constants
	//
	
	/** The logger instance. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(RDKitFingerprintNodeModel.class);

	/** 
	 * This lock prevents two calls at the same time into the RDKKit FeatureInvariants 
	 * and Avalon Fingerprint functionality, which has caused crashes under Windows 7 before. 
	 * Once there is a fix implemented in the RDKit (or somewhere else?) we can 
	 * remove this LOCK again.
	 */
	private static final Object LOCK = new Object();
	
	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;

	//
	// Members
	//
	
	/** Model for the input column. */
    private final SettingsModelString m_modelInputColumnName = 
    	registerSettings(RDKitFingerprintNodeDialog.createSmilesColumnModel(), true);

    /** Model for the name of the new column. */
    private final SettingsModelString m_modelNewColumnName =
    	registerSettings(RDKitFingerprintNodeDialog.createNewColumnModel(), true);

    /** Model for the option to remove the input column. */
    private final SettingsModelBoolean m_modelRemoveSourceColumns =
    	registerSettings(RDKitFingerprintNodeDialog.createBooleanModel(), true);

    /** Model for the fingerprint type to apply. */
    private final SettingsModelEnumeration<FingerprintType> m_modelFingerprintType = 
    	registerSettings(RDKitFingerprintNodeDialog.createFPTypeModel(), true);

    /** Model for the minimum path length to be used for calculations. */
    private final SettingsModelIntegerBounded m_modelMinPath =
    	registerSettings(RDKitFingerprintNodeDialog.createMinPathModel(), true);

    /** Model for the maximum path length to be used for calculations. */
    private final SettingsModelIntegerBounded m_modelMaxPath =
    	registerSettings(RDKitFingerprintNodeDialog.createMaxPathModel(), true);

    /** Model for the number of fingerprint bits to be used for calculations. */
    private final SettingsModelIntegerBounded m_modelNumBits =
    	registerSettings(RDKitFingerprintNodeDialog.createNumBitsModel(), true);

    /** Model for the radius to be used for calculations. */
    private final SettingsModelIntegerBounded m_modelRadius =
    	registerSettings(RDKitFingerprintNodeDialog.createRadiusModel(), true);

    /** Model for the layer flags to be used for calculations. */
    private final SettingsModelIntegerBounded m_modelLayerFlags =
    	registerSettings(RDKitFingerprintNodeDialog.createLayerFlagsModel(), true);

    //
    // Constructors
    //
    
    /**
     * Create new node model with one data in- and one outport.
     */
    RDKitFingerprintNodeModel() {
        super(1, 1);
    }

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
        		m_modelNewColumnName, strInputColumnName + " (Fingerprint)");

        // Determine, if the new column name has been set and if it is really unique
        SettingsUtils.checkColumnNameUniqueness(inSpecs[0], null,
        		(m_modelRemoveSourceColumns.getBooleanValue() ? new String[] { 
        			m_modelInputColumnName.getStringValue() } : null),
        		m_modelNewColumnName, 
        		"Output column has not been specified yet.",
        		"The name %COLUMN_NAME% of the new column exists already in the input.");
        
        // Determine, if path values are in conflict
        if (m_modelMinPath.getIntValue() > m_modelMaxPath.getIntValue()) {
        	throw new InvalidSettingsException("Minimum path length is larger than maximum path length.");
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
    		// Generate column specs for the output table columns produced by this factory
    		DataColumnSpec[] arrOutputSpec = new DataColumnSpec[1]; // We have only one output column
    		arrOutputSpec[0] = new DataColumnSpecCreator(
    				m_modelNewColumnName.getStringValue(), DenseBitVectorCell.TYPE)
    				.createSpec();
    
    		// Generate factory 
    	    arrOutputFactories[0] = new AbstractRDKitCellFactory(this, AbstractRDKitCellFactory.RowFailurePolicy.DeliverEmptyValues,
           		getWarningConsolidator(), null, arrOutputSpec) {
	   			
	   			@Override
	   		    /**
	   		     * This method implements the calculation logic to generate the new cells based on 
	   		     * the input made available in the first (and second) parameter.
	   		     * {@inheritDoc}
	   		     */
	   		    public DataCell[] process(InputDataInfo[] arrInputDataInfo, DataRow row, int iUniqueWaveId) throws Exception {
	   		    	DataCell outputCell = null;
	   		    	
	   		    	// Calculate the new cells
	   		    	ROMol mol = markForCleanup(arrInputDataInfo[INPUT_COLUMN_MOL].getROMol(row), iUniqueWaveId);    

                    ExplicitBitVect fingerprint = null;

                    // Calculate fingerprint
                	switch (m_modelFingerprintType.getValue()) {
	                	case rdkit:
	                        fingerprint = markForCleanup(RDKFuncs.RDKFingerprintMol(mol,
	                        		m_modelMinPath.getIntValue(), m_modelMaxPath.getIntValue(),
	                        		m_modelNumBits.getIntValue(), 2), iUniqueWaveId);
	                		break;
	                		
	                	case atompair:
	                        fingerprint = markForCleanup(RDKFuncs.getHashedAtomPairFingerprintAsBitVect(
	                        		mol, m_modelNumBits.getIntValue()), iUniqueWaveId);
	                		break;
	                			                		
	                	case torsion:
	                        fingerprint = markForCleanup(RDKFuncs.getHashedTopologicalTorsionFingerprintAsBitVect(
	                        		mol, m_modelNumBits.getIntValue()), iUniqueWaveId);
	                		break;
	                		
	                	case morgan:
	                        fingerprint = markForCleanup(RDKFuncs.getMorganFingerprintAsBitVect(
	                        		mol, m_modelRadius.getIntValue(), m_modelNumBits.getIntValue()), iUniqueWaveId);
	                		break;
	                		
	                	case featmorgan:
	                    	UInt_Vect ivs= new UInt_Vect(mol.getNumAtoms());
	                    	RDKFuncs.getFeatureInvariants(mol, ivs);
	                        fingerprint = markForCleanup(RDKFuncs.getMorganFingerprintAsBitVect(
	                        		mol, m_modelRadius.getIntValue(), m_modelNumBits.getIntValue(), ivs), iUniqueWaveId);
	                		break;
	                		
	                	case avalon:
	                		int bitNumber = m_modelNumBits.getIntValue();
							fingerprint = markForCleanup(new ExplicitBitVect(bitNumber), iUniqueWaveId);
							synchronized (LOCK) {
								RDKFuncs.getAvalonFP(mol, fingerprint, bitNumber, false, false,
								 		RDKFuncs.getAvalonSimilarityBits());
							}
							break;
	                		
	                	case layered:
	                        fingerprint = markForCleanup(RDKFuncs.LayeredFingerprintMol(
	                        		mol, m_modelLayerFlags.getIntValue(), m_modelMinPath.getIntValue(),
	                        		m_modelMaxPath.getIntValue(), m_modelNumBits.getIntValue()), iUniqueWaveId);
	                		break;
	                		
	                	case maccs:
							synchronized (LOCK) {
								fingerprint = markForCleanup(RDKFuncs.MACCSFingerprintMol(mol),
										iUniqueWaveId);
							}
	                        break;
	                		
	                	default:
	                		String strMsg = "Fingerprint Type '" + m_modelFingerprintType.getValue() + 
	                			"' cannot be handled by this node.";
	                		LOGGER.error(strMsg);
	                    	throw new RuntimeException(strMsg);
                	}
                    
                    // Transfer to bit vector
                    if (fingerprint != null) {
                        // Transfer the bitset into a dense bit vector
                    	final long lBits = fingerprint.getNumBits();
    	   		        DenseBitVector bitVector = new DenseBitVector(lBits);                   
                        for (long i = 0; i < lBits; i++) {
                            if (fingerprint.getBit(i)) {
                                bitVector.set(i);
                            }
                        }
                        
                        outputCell = new DenseBitVectorCellFactory(bitVector).createDataCell();
	   		        }
	   		        else {
	   		        	getWarningConsolidator().saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
	   		        		"Error computing fingerprint - Setting value as missing cell.");
	   		        	outputCell = DataType.getMissingCell();
	                }
	   		    	
	   		        return new DataCell[] { outputCell };
	   		    }
	   		};
	   		
	   		// Enable this factory to allow parallel processing	   		
	   		arrOutputFactories[0].setAllowParallelProcessing(true);
    	}
    	
    	return (arrOutputFactories == null ? new AbstractRDKitCellFactory[0] : arrOutputFactories);
    }    

    /**
     * {@inheritDoc}
     * This implementation removes additionally the compound source column, if specified in the settings.
     */
	@Override
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
