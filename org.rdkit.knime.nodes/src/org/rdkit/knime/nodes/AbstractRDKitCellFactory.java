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
package org.rdkit.knime.nodes;

import java.util.ArrayList;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.BlobSupportDataRow;
import org.knime.core.node.NodeLogger;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.RDKitObjectCleaner;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * The root class for cell factories producing cells based on or under usage of 
 * RDKit objects.
 * 
 * @author Manuel Schwarze
 */
public abstract class AbstractRDKitCellFactory extends AbstractCellFactory {
	
	//
	// Enumerations
	//
	
	/**
	 * This enumeration defines several policies for handling failures when 
	 * processing rows during node execution.
	 */
	public enum RowFailurePolicy {
		/** 
		 * Policy when encountering an exception during execution of a row: 
		 * Don't process the row further but deliver all result cells as empty cells. 
		 */
		DeliverEmptyValues,
		
		/** 
		 * Policy when encountering an exception during execution of a row: 
		 * Let the entire execution of the node fail. This policy causes an exception 
		 * to be thrown, which should abort the node execution.
		 */
		StopExecution
	}
	
	// 
	// Constants
	//
	
	/** The logger instance. */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(AbstractRDKitCellFactory.class);
	
	//
	// Members
	//
	
	/** Flag to remember, if parallel processing is allowed or not. */
	private boolean m_bAllowParallelProcessing;
	
	/** Defines the policy how to treat an execution failures for a single row. */
	private RowFailurePolicy m_rowFailurePolicy;

	/** Contains useful data about the input data to be merged. */
	private InputDataInfo[] m_arrInputDataInfo;
	
	/** Cleaner object to register RDKit objects for freeing resources. */
	private RDKitObjectCleaner m_cleaner;
	
	/** Conserves occurring warning messages and consolidates them later. */
	private WarningConsolidator m_warnings;
	
	//
	// Constructor
	//
	
	/**
	 * Creates a new cell factory for RDKit based tables.
	 * 
	 * @param cleaner object to be used for registering RDKit objects for freeing resources. Must not be null.
	 * @param rowFailurePolicy Defines the policy to be used the calculation
	 * 		of row values failed during execution of the node. 
	 * @param warningConsolidator Conserves occurring warning messages and consolidates them later. Must not be null.
	 * @param arrInputColumnInfo Array with information about input columns. Can be null, if the factory will not merge 1:1.
	 		This value can also be set later before execution ({@link #setInputDataInfos(InputDataInfo[])}).
	 * @param outputColumnSpecs Specifications of output columns. Can be empty.
	 */
	public AbstractRDKitCellFactory(RDKitObjectCleaner cleaner, RowFailurePolicy rowFailurePolicy, WarningConsolidator warningConsolidator, 
			InputDataInfo[] arrInputColumnInfo, DataColumnSpec... outputColumnSpecs) {
		super(outputColumnSpecs);
		
		if (cleaner == null) {
			throw new IllegalArgumentException("RDKitObjectCleaner must not be null.");
		}
		
		if (warningConsolidator == null) {
			throw new IllegalArgumentException("Warning consolidator must not be null.");
		}
		
		m_arrInputDataInfo = arrInputColumnInfo;
		m_cleaner = cleaner;
		m_warnings = warningConsolidator;
		m_rowFailurePolicy = rowFailurePolicy;
		m_bAllowParallelProcessing = false;
	}

	//
	// Public Methods
	//
	
	/**
	 * Tells, if the method {@link #setAllowParallelProcessing(boolean)} was
	 * called before with "true" as argument to allow parallel processing.
	 * This method exists only, because the method {@link #setParallelProcessing(boolean)}
	 * does not make the parallel processing flag available 
	 * and cannot be overridden. 
	 * 
	 * @return True if parallel processing is allowed.
	 * 
	 * @see #setAllowParallelProcessing(boolean)
	 */
	public boolean allowsParallelProcessing() {
		return m_bAllowParallelProcessing;
	}
	
	/**
	 * Call this method to allow for this factory parallel processing.
	 * This method exists only, because the method {@link #setParallelProcessing(boolean)}
	 * does not make the parallel processing flag available anymore
	 * and cannot be overridden. This method calls directly
	 * {@link #setParallelProcessing(boolean)}.
	 * 
	 * @param value Set to true to allow parallel processing.
	 * 
	 * @see #allowsParallelProcessing()
	 * @see #setParallelProcessing(boolean)
	 */
	public void setAllowParallelProcessing(boolean value) {
		m_bAllowParallelProcessing = value;
		super.setParallelProcessing(value);
	}
	
	/**
	 * Returns the configured policy id to be applied when calculation of a row failed.
	 * 
	 * @return Configured policy id.
	 */
	public RowFailurePolicy getRowFailurePolicy() {
		return m_rowFailurePolicy;
	}
	
	/**
	 * Returns the warning consolidator, which is used to save warning messages.
	 * 
	 * @return Warning consolidator.
	 */
	public WarningConsolidator getWarningConsolidator() {
		return m_warnings;
	}
	
	/**
	 * Returns the RDKit Object Cleaner object used in this factory to register RDKit objects for 
	 * cleanup.
	 * 
	 * @return RDKit Object Cleaner.
	 */
	public RDKitObjectCleaner getRDKitObjectCleaner() {
		return m_cleaner;
	}

	/**
	 * Makes information about input columns available for the execution. This array will be passed
	 * to the {@link #process(InputDataInfo[], DataRow, int)} method for every row that needs to be
	 * generated by the factory.
	 * 
	 * @param arrInputColumnInfo Array with information about input columns. Can be null, if the factory will not merge 1:1.
	 * 		Otherwise it must not be null.
	 */
	public void setInputDataInfos(InputDataInfo[] arrInputColumnInfo) {
		m_arrInputDataInfo = arrInputColumnInfo;
	}
	
	/** 
	 * Returns the array of input data info objects describing the input data for this factory.
	 * Input data will usually merged with output data.
	 * 
	 * @return Array of input data info objects.
	 */
	public InputDataInfo[] getInputDataInfos() {
		return m_arrInputDataInfo;
	}
	
    /**
     * {@inheritDoc}
     * This method will call the method process(...) in the concrete node class,
     * which must contains the RDKit functionality.
     */
	@Override
    public DataCell[] getCells(final DataRow row) {
    	DataCell[] arrOutputCells = null;
    	
    	int iUniqueWaveId = m_cleaner.createUniqueCleanupWaveId();

    	try {
    		arrOutputCells = process(m_arrInputDataInfo, row, iUniqueWaveId);
    		
    		// Check for null cells and replace them by missing cells
    		for (int i = 0; i < arrOutputCells.length; i++) {
    			if (arrOutputCells[i] == null) {
    				arrOutputCells[i] = DataType.getMissingCell();
                    m_warnings.saveWarning("Found 'null' in a result cell - Replaced it with a missing cell.");
    			}
    		}
    	}
    	catch (InputDataInfo.EmptyCellException exc) {
            LOGGER.warn(exc.getMessage());
            if (exc.stopsNodeExecution()) {
        		throw new RuntimeException("Creation of new data failed: " + 
        				exc.getMessage() != null ? exc.getMessage() : "Unknown error");   	
            }
            else {
                m_warnings.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(), 
                		"Encountered empty input cell.");
                arrOutputCells = createEmptyCells(getColumnSpecs().length);
    		}
    	}
        catch (Exception exc) {
        	// Generate empty cells
        	if (getRowFailurePolicy() == RowFailurePolicy.DeliverEmptyValues) {
        		String strMsg = "Failed to process data" +
        			(exc.getMessage() != null ? " due to " + exc.getMessage() : "") + 
        			". Generating empty result cells.";
            	LOGGER.debug(strMsg + " (Row '" + row.getKey() + "')", exc);
            	getWarningConsolidator().saveWarning(WarningConsolidator.ROW_CONTEXT.getId(), strMsg);
                arrOutputCells = createEmptyCells(getColumnSpecs().length);
        	}
        	// Abort execution completely
        	else {
            	LOGGER.error("Creation of new data failed. Abort execution.");
        		throw new RuntimeException("Creation of new data failed: " + 
        				exc.getMessage() != null ? exc.getMessage() : "Unknown error");
        	}
        } 
        finally {
            m_cleaner.cleanupMarkedObjects(iUniqueWaveId);
        }
        
        return (arrOutputCells == null ? createEmptyCells(getColumnSpecs().length) : arrOutputCells);
    }
    
    /**
     * Creates an array of n empty cells using the Missing Cell instance of KNIME.
     * 
     * @param n Number of empty cells to return. If n < 0, an empty array will be returned.
     * 
     * @return Array with empty cells (missing cells).
     * 
     * @see DataType#getMissingCell()
     */
    public static DataCell[] createEmptyCells(int n) {
    	DataCell[] arrOutputCells = new DataCell[n >= 0 ? n : 0];
    	final DataCell missingCell = DataType.getMissingCell();
    	
    	for (int i = 0; i < arrOutputCells.length; i++) {
    		arrOutputCells[i] = missingCell;
    	}
    	
    	return arrOutputCells;
    }

    /**
     * Merges cells of an existing row with additional data cells. Provides the option
     * to eliminate a single column from the input cells. 
     *       
     * @param row Original row. Must not be null. Can be empty.
     * @param arrCells Cells to be merged. Must not be null. Can be empty.
     * @param columnToRemove Index of a column that shall be removed from the original row. -1 if all to be kept.
     * 
     * @return Result row.
     */
    public static DataRow mergeDataCells(DataRow row, DataCell[] arrCells, int columnToRemove) {
        final int iOldCellCount = row.getNumCells();
    	final ArrayList<DataCell> copyCells =
            new ArrayList<DataCell>(iOldCellCount + arrCells.length);

        // Copy the cells from the incoming row
        for (int i = 0; i < iOldCellCount; i++) {
            if (i != columnToRemove) {
	            // Respecting a blob support row has the advantage that
	            // blobs are not unwrapped (expensive)
	            // --> This is really only for performance and makes a
	            // difference only if the input row contains blobs
	            copyCells.add(row instanceof BlobSupportDataRow
	                    ? ((BlobSupportDataRow)row).getRawCell(i)
	                            : row.getCell(i));
            }
        }
        
        // Add additional cells
        for (DataCell cell : arrCells) {
        	copyCells.add(cell); 
        }
        
        // Create result row
        return new BlobSupportDataRow(row.getKey(),
        		copyCells.toArray(new DataCell[copyCells.size()]));
  }
    
    /**
     * Creates new data cells to be merged with the specified data row, if it is not null.
     * The first parameter contains information about all input values of interest and
     * makes those values easily accessible.
     * 
     * @param arrInputDataInfos Array of all relevant input data.
     * @param row Complete data row of input table to be merged. Can be null, if merge is not required.
     * @param iUniqueWaveId A unique id that should be used for marking RDKit objects for cleanup. Marked
     * 		objects will be cleaned up automatically at the end of this call. If this is not wanted,
     * 		the objects should either not be marked for cleanup or they should be marked without an id, 
     * 		which would lead to a cleanup at the end of the entire execution process.
     * 
     * @return The new data cells to be appended to the output table.
     * 
     * @throws Exception Thrown, if processing failed.
     */
    public abstract DataCell[] process(InputDataInfo[] arrInputDataInfos, DataRow row, int iUniqueWaveId) throws Exception;
}
