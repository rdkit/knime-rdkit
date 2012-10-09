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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.RDKit.ROMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;
import org.rdkit.knime.types.RDKitMolCellFactory;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsModelEnumeration;
import org.rdkit.knime.util.SettingsUtils;

/**
 * This class is the base class for node models of the RDKitSubstructureFilter nodes
 * providing calculations based on the open source RDKit library.
 * 
 * @author Greg Landrum, Novartis
 * @author Thorsten Meinl, University of Konstanz
 * @author Sudip Ghosh, Novartis
 * @author Manuel Schwarze, Novartis
 */
public abstract class AbstractRDKitSubstructFilterNodeModel extends AbstractRDKitNodeModel {
	
	//
	// Enumeration
	//

	/** Defines supported matching criteria. */
    public enum MatchingCriteria {
    	All, Exact, AtLeast;
        
        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
        	
        	switch (this) {
        		case All:
        			return "All";
        		case Exact:
        			return "Exact";
        		case AtLeast:
        			return "At least";
        	}
        	
        	return super.toString();
        }
    }	
    
	//
	// Constants
	//
	
	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(AbstractRDKitSubstructFilterNodeModel.class);
	
	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;
 	
	/** Input data info index for Query Mol value. */
	protected static final int INPUT_COLUMN_QUERY = 0;
	
	//
	// Members
	//
	
	/** Settings model for the column name of the input column. */
	protected final SettingsModelString m_modelInputColumnName =
            registerSettings(RDKitMoleculeSubstructFilterNodeDialog.createInputColumnNameModel(), "input_column", "rdkitColumn");
	
	/** Settings model for the column name of the input column. */
    protected final SettingsModelString m_modelQueryColumnName =
            registerSettings(RDKitMoleculeSubstructFilterNodeDialog.createQueryColumnNameModel(), "query_column", "queryColumn");
	
	/** Settings model for the column name of the input column. */
    protected final SettingsModelEnumeration<MatchingCriteria> m_modelMatchingCriteria =
            registerSettings(RDKitMoleculeSubstructFilterNodeDialog.createMatchingCriteriaModel(), true);
	
	/** Settings model for the column name of the input column. */
    protected final SettingsModelIntegerBounded m_modelMinimumMatches =
            registerSettings(RDKitMoleculeSubstructFilterNodeDialog.createMinimumMatchesModel(m_modelMatchingCriteria));

    /** Settings model for the column name of the new column to be added to the output table. */
    protected final SettingsModelString m_modelNewColumnName =
    		registerSettings(RDKitMoleculeSubstructFilterNodeDialog.createNewColumnNameModel(), true);
     
    /** Query type class. */
    protected final Class<? extends DataValue>[] m_arrClassQueryType;
    
    //
    // Internals
    //
    
    /** 
     * Intermediate pre-processing result, which will be used in processing phase. 
     * It contains all patterns from the query table.
     */
    private ROMol[] m_arrPatterns = null;
    
    /** 
     * Intermediate pre-processing result, which will be used in processing phase. 
     * It contains the total number of atoms of all patterns from the query table.
     */
	private int m_iTotalPatternAtomsCount = 0;
   
    /** 
     * Intermediate pre-processing result, which will be used in processing phase. 
     * It contains the total number of missing pattern cells of the query table.
     */
	private int m_iTotalEmptyPatternCells = 0;
    
    /** 
     * This set is used for communication between parallel execution threads that determine, if
     * a molecule fulfills the matching criteria, and the code that performs the splitting.
     * We will synchronize on that object to ensure that only one thread is accessing at a time.
     */
    private HashSet<RowKey> m_setMatches = new HashSet<RowKey>(50);

    //
    // Constructor
    //
    
    /**
     * Create new node model with two in- and two out-ports.
     * 
     * @param classQueryType Classes of the DataValue interface that act as the query type.
     * 		Must not be null or empty.
     */
    protected AbstractRDKitSubstructFilterNodeModel(Class<? extends DataValue>... arrClassQueryType) {
     	super(2, 2);
     	
     	if (arrClassQueryType == null || arrClassQueryType.length == 0) {
     		throw new IllegalArgumentException("Array of query type classes must not be null and not empty.");
     	}
     	
     	m_arrClassQueryType = arrClassQueryType;
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
 
    	// Auto guess the input mol column if not set - fails if no compatible column found
        SettingsUtils.autoGuessColumn(inSpecs[0], m_modelInputColumnName, RDKitMolValue.class, 0, 
        		"Auto guessing: Using column %COLUMN_NAME% as Mol input column.", 
        		"No RDKit Mol compatible column in input table. Use \"Molecule to RDKit\" " +
        			"node to convert Smiles or SDF.", getWarningConsolidator()); 

        // Determines, if the mol input column exists - fails if it does not
        SettingsUtils.checkColumnExistence(inSpecs[0], m_modelInputColumnName, RDKitMolValue.class,  
        		"Input column has not been specified yet.",
        		"Input column %COLUMN_NAME% does not exist. Has the first input table changed?");
        
        // Auto guess the new column name and make it unique
        SettingsUtils.autoGuessColumnName(inSpecs[0], null, null,
        		m_modelNewColumnName, "Matched Substructs");

        // Determine, if the new column name has been set and if it is really unique
        SettingsUtils.checkColumnNameUniqueness(inSpecs[0], null, null,
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
    	
    	switch (inPort) {
    		case 0: // First table with molecule column
	    		arrDataInfo = new InputDataInfo[1]; // We have only one input mol column
	    		arrDataInfo[INPUT_COLUMN_MOL] = new InputDataInfo(inSpec, m_modelInputColumnName, 
	    				InputDataInfo.EmptyCellPolicy.DeliverEmptyRow, null,
	    				RDKitMolValue.class);
	    		break;
	    		
    		case 1: // Second table with query molecule column
	    		arrDataInfo = new InputDataInfo[1]; // We have only one query molecule column
	    		arrDataInfo[INPUT_COLUMN_QUERY] = new InputDataInfo(inSpec, m_modelQueryColumnName, 
	    				InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
	    				m_arrClassQueryType);
	    		break;
    	}
    	
    	return (arrDataInfo == null ? new InputDataInfo[0] : arrDataInfo);
    }

    /**
     * Returns the output table specification of the specified out port. 
     * 
     * @param outPort Index of output port in focus. Zero-based.
     * @param inSpecs All input table specifications.
     * 
     * @return The specification of all output tables.
     * 
     * @throws InvalidSettingsException Thrown, if the settings are inconsistent with 
     * 		given DataTableSpec elements.
     * 
     * @see #createOutputFactories(int)
     */
    protected DataTableSpec getOutputTableSpec(final int outPort, 
    		final DataTableSpec[] inSpecs) throws InvalidSettingsException {
    	DataTableSpec spec = null;
   	
    	switch (outPort) {
    		case 0:
    		case 1:
	            spec = new DataTableSpec(outPort == 0 ? "Passed molecules" : "Failed molecules",
	            		inSpecs[0], new DataTableSpec(new DataColumnSpecCreator(
	            				m_modelNewColumnName.getStringValue(), 
	            				ListCell.getCollectionType(IntCell.TYPE)).createSpec()));    	
	            break;
        }
    	
    	return spec;
    }    
 
    /**
     * Creates an output factory to create cells based on the passed in
     * input.
     * 
     * @param arrInputDataInfos Array of input data information that is relevant
     * 		for processing.
     * 
     * @see #createInputDataInfos(int, DataTableSpec)
     */
	protected AbstractRDKitCellFactory createOutputFactory(InputDataInfo[] arrInputDataInfos)
		throws InvalidSettingsException {
		// Generate column specs for the output table columns produced by this factory
		DataColumnSpec[] arrOutputSpec = new DataColumnSpec[1]; // We have only one output column
		arrOutputSpec[0] = new DataColumnSpecCreator(
				m_modelNewColumnName.getStringValue().trim(), RDKitMolCellFactory.TYPE)
				.createSpec();
	    final int iTotalPatternCount = m_arrPatterns.length - m_iTotalEmptyPatternCells;
		final int iMinimumMatches = m_modelMinimumMatches.getIntValue();
		final MatchingCriteria matchingCriteria = m_modelMatchingCriteria.getValue();
	    
		// Generate factory 
		AbstractRDKitCellFactory factory = new AbstractRDKitCellFactory(this, 
				AbstractRDKitCellFactory.RowFailurePolicy.DeliverEmptyValues,
				getWarningConsolidator(), arrInputDataInfos, arrOutputSpec) {
   			
   			@Override
   		    /**
   		     * This method implements the calculation logic to generate the new cells based on 
   		     * the input made available in the first (and second) parameter.
   		     * {@inheritDoc}
   		     */
   		    public DataCell[] process(InputDataInfo[] arrInputDataInfo, DataRow row, int iUniqueWaveId) throws Exception {
   		    	DataCell outputCell = null;
				List<IntCell> listFragments = new ArrayList<IntCell>();
   		    	
   		    	// Calculate the new cells
   		    	ROMol mol = markForCleanup(arrInputDataInfo[INPUT_COLUMN_MOL].getROMol(row), iUniqueWaveId);    	
				int iNumberOfMatchingPatterns = 0;
				
				long iMolAtomsCount = mol.getNumAtoms();
				for (int i = 0; i < m_arrPatterns.length; i++) {
					ROMol molPattern = m_arrPatterns[i];
					if (molPattern != null ) {
						if (mol.hasSubstructMatch(molPattern)) {
							listFragments.add(new IntCell(i + 1));
							iNumberOfMatchingPatterns++;
						}
					}
				}

				outputCell = CollectionCellFactory.createListCell(listFragments);

				switch (matchingCriteria) {
					case All:
						if (iNumberOfMatchingPatterns == iTotalPatternCount) {
							synchronized (m_setMatches) {
								m_setMatches.add(row.getKey());
							}
						}
						break;
					case Exact:
						if (iNumberOfMatchingPatterns == iTotalPatternCount && 
							iMolAtomsCount == m_iTotalPatternAtomsCount) {
							synchronized (m_setMatches) {
								m_setMatches.add(row.getKey());			
							}
						}
						break;
					case AtLeast:
						if (iNumberOfMatchingPatterns >= iMinimumMatches) {
							synchronized (m_setMatches) {
								m_setMatches.add(row.getKey());		
							}
						}
						break;
				}
   		    	
   		        return new DataCell[] { outputCell };
   		    }
   		};
   		
   		// Enable or disable this factory to allow parallel processing 		
   		factory.setAllowParallelProcessing(true);	   		
    	
    	return factory;
    }
	
	/**
	 * Sets the intermediate results of the pre-processing phase. These values are
	 * used in the core processing phase to filter substructure matches from the input 
	 * molecules.
	 * 
	 * @param arrPatterns RDKit molecules acting as substructure patterns. Some values
	 * 		could be null, if the origin was a missing cell.
	 * @param iTotalEmptyPatternCells Number of empty cells encountered when evaluating
	 * 		the query input column and preparing the patterns. This is the number of 
	 * 		null values in the arrPatterns array.
	 * @param iTotalPatternAtomsCount Total number of atoms in all patterns. 
	 */
	protected void setPreprocessingResults(ROMol[] arrPatterns, int iTotalEmptyPatternCells,
			int iTotalPatternAtomsCount) {
		m_arrPatterns = arrPatterns;
		m_iTotalEmptyPatternCells = iTotalEmptyPatternCells;
		m_iTotalPatternAtomsCount = iTotalPatternAtomsCount;
	}
	
	/**
	 * Returns the percentage of pre-processing activities from the total execution.
	 *
	 * @return Percentage of pre-processing. Returns 0.0d.
	 */
	protected double getPreProcessingPercentage() {
		return 0.1d;
	}
	
	/**
	 * This method pre-processes the patterns used for the substructure filtering.
	 * {@inheritDoc}
	 */
	protected abstract void preProcessing(final BufferedDataTable[] inData, InputDataInfo[][] arrInputDataInfo,
		final ExecutionContext exec) throws Exception;
	
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] processing(final BufferedDataTable[] inData, InputDataInfo[][] arrInputDataInfo,
    		final ExecutionContext exec) throws Exception {
        final DataTableSpec[] arrOutSpecs = getOutputTableSpecs(inData);
        
        // Contains the rows with the matching molecules
        final BufferedDataContainer tableMatch = exec.createDataContainer(arrOutSpecs[0]);
        
        // Contains the rows with non-matching molecules
        final BufferedDataContainer tableNoMatch = exec.createDataContainer(arrOutSpecs[1]);
        
        // Setup main factory
        final AbstractRDKitCellFactory factory = createOutputFactory(arrInputDataInfo[0]);
        final AbstractRDKitNodeModel.ResultProcessor resultProcessor = 
        	new AbstractRDKitNodeModel.ResultProcessor() {
			
        	/**
        	 * {@inheritDoc}
        	 * This implementation determines, if the cell 0 in the results is missing.
        	 * If it is missing and the setting tells to split the tables, 
        	 * then the original input row is added to table 1. Otherwise the input row 
        	 * gets merged with the cell 0 and is added to table 0.
        	 */
			@Override
			public void processResults(long rowIndex, DataRow row, DataCell[] arrResults) {      
				boolean bMatching = false;
				
				synchronized (m_setMatches) {
					bMatching = m_setMatches.remove(row.getKey());
				}
				
		        if (bMatching) {
		        	tableMatch.addRowToTable(AbstractRDKitCellFactory.mergeDataCells(row, arrResults, -1));
		        } 
		        else {
		        	tableNoMatch.addRowToTable(AbstractRDKitCellFactory.mergeDataCells(row, arrResults, -1));
		        }
			}
		};
		
        // Runs the multiple threads to do the work
        try {
        	new AbstractRDKitNodeModel.ParallelProcessor(factory, resultProcessor, inData[0].getRowCount(), 
       			getWarningConsolidator(), exec).run(inData[0]);
        } 
        catch (Exception e) {
            exec.checkCanceled();
            throw e;
        }

		tableMatch.close();
        tableNoMatch.close();
        
        return new BufferedDataTable[] { tableMatch.getTable(), tableNoMatch.getTable() };
    }	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanupIntermediateResults() {
		m_arrPatterns = null;
		m_iTotalPatternAtomsCount = 0;
		m_iTotalEmptyPatternCells = 0;
		m_setMatches.clear();
	}	 	 
}
