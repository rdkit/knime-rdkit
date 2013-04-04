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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.RDKit.ChemicalReaction;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.RDKit.ROMol_Vect;
import org.RDKit.ROMol_Vect_Vect;
import org.RDKit.RWMol;
import org.knime.chem.types.RxnValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;
import org.rdkit.knime.types.RDKitMolCellFactory;
import org.rdkit.knime.util.ChemUtils;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.InputDataInfo.EmptyCellException;
import org.rdkit.knime.util.SafeGuardedResource;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This abstract class provides base functionality for nodes that 
 * deal with reactions. 
 * 
 * @author Greg Landrum
 * @author Manuel Schwarze 
 */
public abstract class AbstractRDKitReactionNodeModel<T extends AbstractRDKitReactionNodeDialog> 
	extends AbstractRDKitNodeModel {

	//
	// Constants
	//
	
	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(AbstractRDKitReactionNodeModel.class);
	
	protected static final WarningConsolidator.Context PRODUCT_CONTEXT = 
		new WarningConsolidator.Context("Product", "product", "products", true);
	
	/** Input data info index for Reaction value. */
	protected static final int INPUT_COLUMN_REACTION = 0;
	
	//
	// Members
	//
	
	/** Settings model for the column name of the reaction column (if a second input table is used). */
    protected final SettingsModelString m_modelOptionalReactionColumnName =
            registerSettings(T.createOptionalReactionColumnNameModel());
	
	/** Settings model for the reaction smarts pattern (if no second input table is used). */
    protected final SettingsModelString m_modelOptionalReactionSmartsPattern =
            registerSettings(T.createOptionalReactionSmartsPatternModel());
    
    /** Should products be uniquified? */
    protected SettingsModelBoolean m_modelUniquifyProducts = registerSettings(T.createUniquifyProductsModel(),true);
    
    //
    // Internals
    //
    
    /** Used to detect changes in second input table usage. */
    private boolean m_bHadReactionInputTable = false;
    
    /** Counts all products. */
    protected AtomicInteger m_aiProductCounter;
    
    /** The input port index of the reaction table. */
    protected int m_iReactionTableIndex;
 
    //
    // Constructor
    //
    
    /**
     * Creates new node model. Registers the product context to the warning consolidator.
     */
    public AbstractRDKitReactionNodeModel(PortType[] in, PortType[] out, int iReactionTableIndex) {
        super(in, out);
        
        if (iReactionTableIndex < 0 || iReactionTableIndex >= in.length) {
        	throw new IllegalArgumentException("Input port index of reaction table is incorrect.");
        }
        
        m_iReactionTableIndex = iReactionTableIndex;
        getWarningConsolidator().registerContext(PRODUCT_CONTEXT);
    }

    //
    // Protected Methods
    //
    
    /**
     * Returns the number of reactants this node is working with.
     * 
     * @return Number of reactants.
     */
    protected abstract int getNumberOfReactants();
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
    	// Reset warnings and check RDKit library readiness
    	super.configure(inSpecs);
    	
        // For optional reaction input column
        if (hasReactionInputTable(inSpecs, m_iReactionTableIndex)) {
        	
        	// Reset reaction table column name, if the table has changed, so that we can auto guess again
        	if (m_bHadReactionInputTable == false && !SettingsUtils.checkColumnExistence(inSpecs[m_iReactionTableIndex], 
        			m_modelOptionalReactionColumnName, RxnValue.class, null, null)) {
        		m_modelOptionalReactionColumnName.setStringValue(null);
        	}
        	
        	m_bHadReactionInputTable = true;
        	
	        // Auto guess the rxn input column if not set - fails if no compatible column found
	        SettingsUtils.autoGuessColumn(inSpecs[m_iReactionTableIndex], m_modelOptionalReactionColumnName, RxnValue.class, 0, 
	        		"Auto guessing: Using column %COLUMN_NAME%.", 
	        		"No RDKit Reaction compatible column in input table.", getWarningConsolidator()); 
	
	        // Determines, if the rxn input column exists - fails if it does not
	        SettingsUtils.checkColumnExistence(inSpecs[m_iReactionTableIndex], m_modelOptionalReactionColumnName, RxnValue.class,  
	        		"RDKit Reaction input column has not been specified yet.",
	        		"RDKit Reaction input column %COLUMN_NAME% does not exist. Has the input table changed?");
        }
        
        // Or for optional reaction SMARTS value
        else {
        	m_bHadReactionInputTable = false;

        	// Reads and validates the value, and deletes it right afterwards
        	ChemUtils.createReactionFromSmarts(
        			m_modelOptionalReactionSmartsPattern.getStringValue(), 
        				getNumberOfReactants()).delete();
        }

        // Generate output specs
        return null;
    }
    
    /**
     * This implementation generates an input data info object for the reaction column.
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    protected InputDataInfo[] createInputDataInfos(int inPort,
    		DataTableSpec inSpec) throws InvalidSettingsException {
    	
    	InputDataInfo[] arrDataInfo = null;

    	if (inPort ==  m_iReactionTableIndex && inSpec != null) {
			arrDataInfo = new InputDataInfo[1]; // We have only one input column
			arrDataInfo[INPUT_COLUMN_REACTION] = new InputDataInfo(inSpec, m_modelOptionalReactionColumnName, 
					InputDataInfo.EmptyCellPolicy.StopExecution, null,
					RxnValue.class);
		}

		return (arrDataInfo == null ? new InputDataInfo[0] : arrDataInfo);
    }
    
    /**
     * Creates the chemical reaction to be applied as safe guarded resource to avoid corruption
	 * by multiple thread processing.
	 *
     * @param inData All input data tables. The last table must be the reaction table by definition.
     * @param arrInputDataInfo  Information about all columns of the input tables.
     * 
     * @return Chemical reaction wrapped ina save guarded resource component.
     */
	protected SafeGuardedResource<ChemicalReaction> createSafeGuardedReactionResource(
			final BufferedDataTable[] inData, final InputDataInfo[][] arrInputDataInfo) {
		return markForCleanup(new SafeGuardedResource<ChemicalReaction>() {
				
			@Override
			protected ChemicalReaction createResource() {
				try {
					return markForCleanup(hasReactionInputTable(getInputTableSpecs(inData), m_iReactionTableIndex) ?
							ChemUtils.readReactionFromTable(inData[m_iReactionTableIndex], 
									arrInputDataInfo[m_iReactionTableIndex][INPUT_COLUMN_REACTION], getNumberOfReactants()) :
							ChemUtils.createReactionFromSmarts(
									m_modelOptionalReactionSmartsPattern.getStringValue(), 
									getNumberOfReactants()));
				}
				catch (EmptyCellException excNoReactionCell) {
					// Not thrown normally, because we declare a different EmptyCellPolicy
					throw new RuntimeException(excNoReactionCell.getMessage(), excNoReactionCell);
				}
				catch (InvalidSettingsException excSetting) {
					// Not thrown normally, because we check this already in configure()
					throw new RuntimeException(excSetting.getMessage(), excSetting);					
				}
			}
		});
	}
  
    /**
     * {@inheritDoc}
     * This implementation considers the number of processed products.
     */
    @Override
    protected Map<String, Integer> createWarningContextOccurrencesMap(
    		BufferedDataTable[] inData, InputDataInfo[][] arrInputDataInfo,
    		BufferedDataTable[] resultData) {
    	Map<String, Integer> map =  super.createWarningContextOccurrencesMap(inData, arrInputDataInfo,
    			resultData);
    	map.put(PRODUCT_CONTEXT.getId(), m_aiProductCounter.get());
    	
    	return map;
    }
    
    /**
     * Resets the product counter. This should be called when execution of this node is started.
     */
    protected void resetProductCounter() {
    	 m_aiProductCounter = new AtomicInteger();    	
    }
    
    /**
     * Runs the passed in reaction with the specified reactants, if both parameters
     * are not null. From the results it generates result rows containing the different products.
     * The vector of vectors that is created internally as result of the reaction
     * contains as outer vector reactions matching reactants multiple times, and the 
     * inner vector contains the products of each reaction.
     * 
     * @param reaction Chemical reaction to run. Can be null.
     * @param reactants Reactants to be used as parameters for the reaction. Can be null.
     * @param listToAddTo A list to be used to add new rows. Can be null to create a new list.
     * @param uniqueWaveId Id to register RDKit objects for cleanup.
     * @param indicesReactants Indices of reactants passed in using the reactants mol vector.
     * 		The length of this array must match the size of the reactants vector.
     * 
     * @return List of new creates data rows with information about reaction products.
     * 		Can be empty, but never null.
     */
    protected List<DataRow> processReactionResults(ChemicalReaction reaction, ROMol_Vect reactants, 
    		List<DataRow> listToAddTo, int uniqueWaveId, int... indicesReactants) {
    	List<DataRow> listNewRows =  (listToAddTo == null ? new ArrayList<DataRow>(20) : listToAddTo);
    	boolean bUniquifyProducts = m_modelUniquifyProducts.getBooleanValue();
    	
    	if (reaction != null && reactants != null) {  
    		assert(reactants.size() == indicesReactants.length);
    		
	    	ROMol_Vect_Vect vvProducts = reaction.runReactants(reactants);
	    	
	        // If the reaction could be applied to the
	        // reactants, we got a non-empty vector
	        if (vvProducts != null && !vvProducts.isEmpty()) {
	           	final StringBuffer sbRowKey = new StringBuffer();
	           	int iReactionCount = (int)vvProducts.size();
	           	HashSet<String> setProductSmilesSeen = new HashSet<String>();
	        	
	        	// Iterate through reactions 
	            for (int i = 0; i < iReactionCount; i++) {
	            	ROMol_Vect vProds = vvProducts.get(i);
	            	int iProdsCount = (int)vProds.size();
            		
	            	// Iterate through reaction products
	                for (int j = 0; j < iProdsCount; j++) {
	                	ROMol prodMol = markForCleanup(vProds.get(j), uniqueWaveId);
	                    RWMol prod = markForCleanup(new RWMol(prodMol), uniqueWaveId);
	                    
	                    try {
	                        RDKFuncs.sanitizeMol(prod);
	                        
	                        // Optional check for product uniqueness
	                		if (bUniquifyProducts) {
	                			String prodSmiles = RDKFuncs.MolToSmiles(prod, true);
	                			if (setProductSmilesSeen.contains(prodSmiles)) {
	                				// We encountered the product before, now just skipping it
	                				continue;
	                			}
	                			setProductSmilesSeen.add(prodSmiles);
	                		}

	                		m_aiProductCounter.incrementAndGet();
	                        sbRowKey.setLength(0);
	                        List<DataCell> listCells = new ArrayList<DataCell>(2 + indicesReactants.length * 2);

	                        // Add product information to row
	                        listCells.add(RDKitMolCellFactory.createRDKitMolCell(prod));
	                        listCells.add(new IntCell(j));
	                        
	                        // Add reactant information to row
	                        for (int index = 0;  index < indicesReactants.length; index++) {
	                        	int indexReactants = indicesReactants[index];
	                        	sbRowKey.append(indexReactants).append('_');
	                        	listCells.add(new IntCell(indexReactants));
	                        	listCells.add(RDKitMolCellFactory.createRDKitMolCell(
	                        			markForCleanup(reactants.get(index), uniqueWaveId)));
	                        }
	                        
	                        // Create row
	                        listNewRows.add(new DefaultRow(
	                        	sbRowKey.append(i).append('_').append(j).toString(), listCells));
	                    } 
	                    catch (Exception exc) {
	                    	getWarningConsolidator().saveWarning(PRODUCT_CONTEXT.getId(),
	            				"A product molecule could not be sanitized successfully - Skipping it.");
	
	                    	// Output warning in console
	                    	String smiles = "Unknown SMILES";
	
	                    	try {
	                    		smiles = RDKFuncs.MolToSmiles(prod, false, false, 0, false);
	                    	}
	                    	catch (Exception excSmiles) {
	                    		// Ignore 
	                    	}
	                    	
	                    	LOGGER.warn("The following product could not be sanitized " +
	                    			"and will be skipped: " + smiles);
	                    }
	                }
	            }
	        }
    	}
    	
    	return listNewRows;
    }
    
    //
    // Static Public Methods
    //
	
	/**
	 * Determines, if the condition is fulfilled that we have a reaction table with 
	 * columns connected to the node according to the passed in specs.
	 * 
	 * @param inSpecs Port specifications.
	 * @param iReactionTableIndex
	 * 
	 * @return True, if there is a reaction table present at the last index of the specs,
	 * 		and if it has columns.
	 */
	public static boolean hasReactionInputTable(PortObjectSpec[] inSpecs, int iReactionTableIndex) {
		return (inSpecs != null && inSpecs.length >= iReactionTableIndex && 
				inSpecs[iReactionTableIndex] instanceof DataTableSpec &&
				((DataTableSpec)inSpecs[iReactionTableIndex]).getNumColumns() > 0);
	}

}
