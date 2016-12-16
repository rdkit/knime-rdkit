/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2016
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
package org.rdkit.knime.nodes.moleculecatalogfilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.RDKit.FilterCatalog;
import org.RDKit.FilterCatalogEntry;
import org.RDKit.FilterCatalogEntry_Vect;
import org.RDKit.FilterCatalogParams.FilterCatalogs;
import org.RDKit.FilterMatch;
import org.RDKit.FilterMatch_Vect;
import org.RDKit.Match_Vect;
import org.RDKit.ROMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsModelEnumeration;
import org.rdkit.knime.util.SettingsModelEnumerationArray;
import org.rdkit.knime.util.SettingsUtils;

/**
 * This class implements the node model of the RDKitMoleculeCatalogFilter node
 * providing calculations based on the open source RDKit library.
 * Filters a list of molecules by applying filters defined in standard catalogs: 
 * PAINS, PAINS A, PAINS B, PAINS C, BRENK, NIH, ZINC or ALL.
 * 
 * @author Manuel Schwarze 
 */
public class RDKitMoleculeCatalogFilterNodeModel extends AbstractRDKitNodeModel {

	//
	// Constants
	//
	
	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitMoleculeCatalogFilterNodeModel.class);
	
	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;
	
	/** Result column name for match count column. Optional column. */
	protected static final String RESULT_COLUMN_TOTAL_MATCH_COUNT = "Total match counts";
	
	/** Result column name for match count column. Optional column. */
	protected static final String RESULT_COLUMN_CATALOG = "Catalogs";
	
	/** Result column name for match count column. Optional column. */
	protected static final String RESULT_COLUMN_MATCH_COUNT = "Match counts";
	
	/** Result column name for filters. */
	protected static final String RESULT_COLUMN_RULES = "Rules";
	
	/** Result column name for descriptions. */
	protected static final String RESULT_COLUMN_DESCRIPTION = "Descriptions";
	
	/** Result column name for references. */
	protected static final String RESULT_COLUMN_REFERENCE = "References";
	
	/** Result column name for atom list. */
	protected static final String RESULT_COLUMN_ATOM_LIST = "Atom List";
	
	/** Result column count without atom lists. */
	protected static final int CORE_RESULT_COLUMN_COUNT = 6;
	
	//
	// Members
	//
	
	/** Settings model for the column name of the input column. */
    private final SettingsModelString m_modelInputColumnName =
            registerSettings(RDKitMoleculeCatalogFilterNodeDialog.createInputColumnNameModel());

    /** Settings model to be used to specify the filter catalog(s) to be used. */
    private final SettingsModelEnumerationArray<FilterCatalogs> m_modelFilterCatalogs =
    		registerSettings(RDKitMoleculeCatalogFilterNodeDialog.createFilterCatalogsModel());
	
	/** Settings model to be used for the output column prefix. */
    private final SettingsModelString m_modelOutputColumnPrefix =
            registerSettings(RDKitMoleculeCatalogFilterNodeDialog.createOutputColumnPrefixModel());
	
	/** Settings model to be used for the option to generate an atom list. */
    private final SettingsModelEnumeration<AtomListHandling> m_modelAtomListHandlingOption =
            registerSettings(RDKitMoleculeCatalogFilterNodeDialog.createGenerateAtomListOptionModel());
    
    //
    // Constructor
    //
    
    /**
     * Create new node model with one data in- and one out-port.
     */
    RDKitMoleculeCatalogFilterNodeModel() {
    	super(1, 2);
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
        		"No RDKit Mol, SMILES or SDF compatible column in input table. Use the \"RDKit from Molecule\" " +
        			"node to convert SMARTS.", getWarningConsolidator()); 

        // Determines, if the input column exists - fails if it does not
        SettingsUtils.checkColumnExistence(inSpecs[0], m_modelInputColumnName, RDKitMolValue.class,  
        		"Input column has not been specified yet.",
        		"Input column %COLUMN_NAME% does not exist. Has the input table changed?");
        
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
    			// First table has the same structure as input table
    			spec = inSpecs[0];
    			break;
    			
    		case 1:
    			// Copy all specs from input table
	            ArrayList<DataColumnSpec> newColSpecs = new ArrayList<DataColumnSpec>();
	            for (DataColumnSpec inCol : inSpecs[0]) {
                	newColSpecs.add(inCol);
	            }
	            
	            // Append result column(s)
	            newColSpecs.addAll(Arrays.asList(createOutputFactory(null).getColumnSpecs()));
	            spec = new DataTableSpec(
	                    newColSpecs.toArray(new DataColumnSpec[newColSpecs.size()]));    	
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
		String strColumnPrefix = m_modelOutputColumnPrefix.getStringValue();
		AtomListHandling atomListHandling = m_modelAtomListHandlingOption.getValue();
		FilterCatalogs[] arrFilterCatalogs = m_modelFilterCatalogs.getValues();
		
		// Generate column specs for the output table columns produced by this factory
		DataColumnSpec[] arrOutputSpec = new DataColumnSpec[atomListHandling == AtomListHandling.None ? 
				CORE_RESULT_COLUMN_COUNT : 
			atomListHandling == AtomListHandling.Combined ? 
					CORE_RESULT_COLUMN_COUNT + 1 : 
				CORE_RESULT_COLUMN_COUNT + arrFilterCatalogs.length]; 
		arrOutputSpec[0] = new DataColumnSpecCreator(strColumnPrefix + RESULT_COLUMN_TOTAL_MATCH_COUNT, 
				IntCell.TYPE).createSpec();
		arrOutputSpec[1] = new DataColumnSpecCreator(strColumnPrefix + RESULT_COLUMN_CATALOG, 
				ListCell.getCollectionType(StringCell.TYPE)).createSpec();
		arrOutputSpec[2] = new DataColumnSpecCreator(strColumnPrefix + RESULT_COLUMN_MATCH_COUNT, 
				ListCell.getCollectionType(IntCell.TYPE)).createSpec();
		arrOutputSpec[3] = new DataColumnSpecCreator(strColumnPrefix + RESULT_COLUMN_RULES, 
				ListCell.getCollectionType(StringCell.TYPE)).createSpec();
		arrOutputSpec[4] = new DataColumnSpecCreator(strColumnPrefix + RESULT_COLUMN_DESCRIPTION, 
				ListCell.getCollectionType(StringCell.TYPE)).createSpec();
		arrOutputSpec[5] = new DataColumnSpecCreator(strColumnPrefix + RESULT_COLUMN_REFERENCE, 
				ListCell.getCollectionType(StringCell.TYPE)).createSpec();

		switch (atomListHandling) {
		case Combined:
			arrOutputSpec[CORE_RESULT_COLUMN_COUNT] = new DataColumnSpecCreator(strColumnPrefix + RESULT_COLUMN_ATOM_LIST, 
					ListCell.getCollectionType(IntCell.TYPE)).createSpec();
			break;
		case Separate:
			for (int i = 0; i < arrFilterCatalogs.length; i++) {
				arrOutputSpec[CORE_RESULT_COLUMN_COUNT + i] = new DataColumnSpecCreator(strColumnPrefix + 
						arrFilterCatalogs[i].name() + " " + RESULT_COLUMN_ATOM_LIST, 
						ListCell.getCollectionType(IntCell.TYPE)).createSpec();
			}
			break;
		default:
			break;
		}
		
		// Generate filter catalogs of choice
		final FilterCatalog[] arrFilters = new FilterCatalog[arrFilterCatalogs.length];
		for (int i = 0; i < arrFilterCatalogs.length; i++) {
			arrFilters[i] = markForCleanup(new FilterCatalog(arrFilterCatalogs[i]));
		}
		
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
   		    public DataCell[] process(InputDataInfo[] arrInputDataInfo, DataRow row, long lUniqueWaveId) throws Exception {
   		    	DataCell[] arrOutputCell = new DataCell[atomListHandling == AtomListHandling.None ? 
   		    			CORE_RESULT_COLUMN_COUNT : 
   					atomListHandling == AtomListHandling.Combined ? 
   							CORE_RESULT_COLUMN_COUNT + 1 : 
   								CORE_RESULT_COLUMN_COUNT + arrFilterCatalogs.length];
   		    	
   		    	// Calculate the new cells
   		    	ROMol mol = markForCleanup(arrInputDataInfo[INPUT_COLUMN_MOL].getROMol(row), lUniqueWaveId);
   		    	int iTotalMatchCount = 0;
   		    	List<DataCell> listCatalogNames = new ArrayList<>();
	    		List<DataCell> listMatchCounts = new ArrayList<>();
	    		List<DataCell> listFilters = new ArrayList<>();
	    		List<DataCell> listDescriptions = new ArrayList<>();
	    		List<DataCell> listReference = new ArrayList<>();
   		    	
   		    	List<Set<Integer>> listSetAtoms = new ArrayList<>();
   		    	switch (atomListHandling) {
	   		    	case Combined:
	   		    		listSetAtoms.add(new LinkedHashSet<>());
	   		    		break;
	   		    	case Separate:
	   		    		for (int i = 0; i < arrFilterCatalogs.length; i++) {
	   		    			listSetAtoms.add(new LinkedHashSet<>());
	   		    		}
		    		default:
		    			break;
   		    	}

   		    	// Walk through all selected filter catalogs one by one,
   		    	// so that we can record also the catalog name
   		    	for (int catNo = 0; catNo < arrFilterCatalogs.length; catNo++) {
   		    		FilterCatalog currentCatalog = arrFilters[catNo];
   		    		String strCatalogName = arrFilterCatalogs[catNo].name();
	   		    	FilterCatalogEntry_Vect listMatches = markForCleanup(currentCatalog.getMatches(mol), lUniqueWaveId);    
	   		    	
	   		    	int iMatches = listMatches == null ? 0 : (int)listMatches.size();
	   		    	iTotalMatchCount += iMatches;
	   		    	arrOutputCell[0] = new IntCell(iMatches);
	   		    	
	   		    	// Only if we got any matches determine detail information
	   		    	if (iMatches > 0) {
	   		    		
	   		    		// Record catalog name and match count
	   		    		listCatalogNames.add(new StringCell(strCatalogName));
	   		    		listMatchCounts.add(new IntCell(iMatches));
	   		    		
	   		    		for (int i = 0; i < iMatches; i++) {
	   		    			FilterCatalogEntry match = markForCleanup(listMatches.get(i), lUniqueWaveId);
	   		    			String strFilter = match.getProp("Scope");
	   		    			String strDescription = match.getDescription();
	   		    			String strReference = match.getProp("Reference");
	   		    			listFilters.add(strFilter == null || strFilter.isEmpty() ? 
	   		    					DataType.getMissingCell() : new StringCell(strFilter));
	   		    			listDescriptions.add(strDescription == null || strDescription.isEmpty() ? 
	   		    					DataType.getMissingCell() : new StringCell(strDescription));
	   		    			listReference.add(strReference == null || strReference.isEmpty() ? 
	   		    					DataType.getMissingCell() : new StringCell(strReference));
	
	   		    			if (atomListHandling != AtomListHandling.None) {
	   		    				// Pick the right atom list to record matches
	   		    				Set<Integer> setAtoms = listSetAtoms.get(0); // Used for Combined mode
	   		    				if (atomListHandling == AtomListHandling.Separate) {
	   		    					setAtoms = listSetAtoms.get(catNo);
	   		    				}
	   		    				// Find atoms
	   		    				FilterMatch_Vect listFilterMatches = markForCleanup(match.getFilterMatches(mol), lUniqueWaveId);
	   		    				long lSize = listFilterMatches.size();
	   		    				for (int j = 0; j < lSize; j++) {
	   		    					FilterMatch matchInfo = markForCleanup(listFilterMatches.get(j), lUniqueWaveId);	   		    					
	   		    					Match_Vect listAtoms = markForCleanup(matchInfo.getAtomMatches(), lUniqueWaveId);
	   		    					long lCount = listAtoms.size();
	   		    					for (int k = 0; k < lCount; k++) {
	   		    						setAtoms.add(listAtoms.get(k).getSecond());
	   		    					}
	   		    				}   		    	
	   		    			}
	   		    		}
	   		    	}
   		    	}

   		    	// We return a missing cell for all output cells where no filter matched 
   		    	// (these become the good ones)
   		    	// Only exception is the total match count (here we may return 0), because
   		    	// the splitter logic needs to distinguish between the cases
   		    	// 1. That we had a valid molecule, but not filter matches, and
   		    	// 2. That we had a empty cell in the molecule column (which would result in empty output)
   		    	// The handling in the second case is that such a row is listed under bad molecules
	    		arrOutputCell[0] = new IntCell(iTotalMatchCount);
   		    	if (iTotalMatchCount == 0) {
   		    		for (int i = 1; i < arrOutputCell.length; i++) {
   		    			arrOutputCell[i] = DataType.getMissingCell();
   		    		}
   		    	}

   		    	// Generate output cells for the row based on the collected information
   		    	else {
	   		    	arrOutputCell[1] = listCatalogNames.isEmpty() ? 
		    				DataType.getMissingCell() : CollectionCellFactory.createListCell(listCatalogNames);
	   		    	arrOutputCell[2] = listMatchCounts.isEmpty() ? 
		    				DataType.getMissingCell() : CollectionCellFactory.createListCell(listMatchCounts);
		    		arrOutputCell[3] = listFilters.isEmpty() ? 
		    				DataType.getMissingCell() : CollectionCellFactory.createListCell(listFilters);
		    		arrOutputCell[4] = listDescriptions.isEmpty() ? 
		    				DataType.getMissingCell() : CollectionCellFactory.createListCell(listDescriptions);
		    		arrOutputCell[5] = listReference.isEmpty() ? 
		    				DataType.getMissingCell() : CollectionCellFactory.createListCell(listReference);
		    		
		    		switch (atomListHandling) {
			    		case Combined: 
		   		    		// Create integer cells for atom indexes
			    			arrOutputCell[6] = generateAtomListCell(listSetAtoms.get(0));
			    			break;
		
			    		case Separate:
			    			for (int i = 0; i < arrFilterCatalogs.length; i++) {
			    				arrOutputCell[6 + i] = generateAtomListCell(listSetAtoms.get(i));
			    			}
			    			break;
		
			    		default:
			    			break;
	   		    	}
   		    	}
   		    	
   		        return arrOutputCell;
   		    }
   		};
   		
   		// Enable or disable this factory to allow parallel processing 		
   		factory.setAllowParallelProcessing(true);	   		
    	
    	return factory;
    }
	
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] processing(final BufferedDataTable[] inData, InputDataInfo[][] arrInputDataInfo,
    		final ExecutionContext exec) throws Exception {
        final DataTableSpec[] arrOutSpecs = getOutputTableSpecs(inData);
        
        // Contains the rows with the result column
        final BufferedDataContainer port0 = exec.createDataContainer(arrOutSpecs[0]);
        
        // Contains the input rows if result computation fails
        final BufferedDataContainer port1 = exec.createDataContainer(arrOutSpecs[1]);
                
        // Get settings and define data specific behavior
        final long iTotalRowCount = inData[0].size();
        
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
		        if (!arrResults[0].isMissing() && ((IntCell)arrResults[0]).getIntValue() == 0) {
		            port0.addRowToTable(row);
		        } 
		        else {
		        	port1.addRowToTable(AbstractRDKitCellFactory.mergeDataCells(row, arrResults, -1));
		        }
			}
		};
		
        // Runs the multiple threads to do the work
        try {
        	new AbstractRDKitNodeModel.ParallelProcessor(factory, resultProcessor, iTotalRowCount, 
       			getWarningConsolidator(), exec).run(inData[0]);
        } 
        catch (Exception e) {
            exec.checkCanceled();
            throw e;
        }

		port0.close();
        port1.close();
        
        return new BufferedDataTable[] { port0.getTable(), port1.getTable() };
    }	
    
    /**
     * Generate a new data cell for the passed in set of atoms.
     * The set will be turned into a sorted list.
     * 
     * @return Data cell or missing cell, if the passed in set is null or empty.
     */
    protected DataCell generateAtomListCell(Set<Integer> setAtoms) {
    	DataCell output;
    	
   		if (setAtoms == null || setAtoms.isEmpty()) {
   			output = DataType.getMissingCell();
   		}
   		else {
   			List<Integer> listSortedAtoms = new ArrayList<Integer>(setAtoms);
   			Collections.sort(listSortedAtoms);
   			List<DataCell> listAtoms = new ArrayList<DataCell>();
   			for (Integer iAtomIndex : listSortedAtoms) {
   				if (iAtomIndex != null) {
   					listAtoms.add(new IntCell(iAtomIndex));
   				}
   			}
   			output = CollectionCellFactory.createListCell(listAtoms);
   		}
   		
   		return output;
    }
}
