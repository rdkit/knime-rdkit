/* 
 * This source code, its documentation and all related files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2012
 * Novartis Institutes for BioMedical Research
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 */
package org.rdkit.knime.util;

import org.knime.core.data.DataRow;

/**
 * This interface can be implemented to express split conditions.
 * 
 * @author Manuel Schwarze
 */
public interface SplitCondition {

	/**
	 * Determines the number of target tables.
	 * 
	 * @return Number of target tables. 
	 */
	int getTargetTableCount();
	
    /**
     * This method implements the condition on which data are split between multiple output tables or filtered
     * out completely. 
     * 
     * @param iInPort The input port of the data in focus. Rather an informal value.
     * @param iRowIndex The row index of the row being processed.
     * @param row The complete data row in focus. Normally a decision is made based on information in this row.
     * @param arrInputDataInfo Input data information about all important input columns of 
     * 		the table at the input port.
     * @param iUniqueWaveId A unique id that should be used for marking RDKit objects for cleanup. Marked
     * 		objects will be cleaned up automatically at the end of this call. If this is not wanted,
     * 		the objects should either not be marked for cleanup or they should be marked without an id, 
     * 		which would lead to a cleanup at the end of the entire execution process.
     * 
     * @return The index of the target table, or -1, if row shall be filtered out completely.
     * 
     * @throws org.rdkit.knime.util.InputDataInfo.EmptyCellException Thrown, if an empty cell is encountered
     * 		in the input column, and if the empty cell handling policy in the InputDataInfo object is set
     * 		accordingly. This exception will be caught and handled properly in the processing method.
     */
    int determineTargetTable(int iInPort, int iRowIndex, 
    		DataRow row, InputDataInfo[] arrInputDataInfo, 
    		int iUniqueWaveId) throws InputDataInfo.EmptyCellException;

	/**
	 * For convenience reasons this splitter is provided to split rows
	 * containing missing cells in a particular column to a second table.
	 * 
	 * @author Manuel Schwarze
	 */
	public static class SplitMissingCell implements SplitCondition {

		/** The index of the column in focus of this splitter. */
		final int m_iColIndex;
		
		//
		// Constructor
		//
		
		/**
		 * Creates a new Missing Cell Split Condition for the specified column index.
		 * 
		 * @param iColIndex Column index that will be checked by this splitter.
		 */
		public SplitMissingCell(int iColIndex) {
			m_iColIndex = iColIndex;
		}
		
		/**
		 * Determines the number of target tables.
		 * 
		 * @return Number of target tables. Returns 2.
		 */
		public int getTargetTableCount() {
			return 2;
		}
		
	    /**
	     * Returns 1, if the specified row contains a missing cell at the column in focus,
	     * or 0 otherwise.  
	     * 
	     * @param iInPort The input port of the data in focus. Rather an informal value.
	     * @param iRowIndex The row index of the row being processed.
	     * @param row The complete data row in focus. Normally a decision is made based on information in this row.
	     * @param arrInputDataInfo Input data information about all important input columns of 
	     * 		the table at the input port.
	     * @param iUniqueWaveId A unique id that should be used for marking RDKit objects for cleanup. Marked
	     * 		objects will be cleaned up automatically at the end of this call. If this is not wanted,
	     * 		the objects should either not be marked for cleanup or they should be marked without an id, 
	     * 		which would lead to a cleanup at the end of the entire execution process.
	     * 
	     * @return The index of the target table, or -1, if row shall be filtered out completely.
	     * 
	     * @throws org.rdkit.knime.util.InputDataInfo.EmptyCellException Not thrown in this implementation.
	     */
	    public int determineTargetTable(int iInPort, int iRowIndex, 
	    		DataRow row, InputDataInfo[] arrInputDataInfo, 
	    		int iUniqueWaveId) throws InputDataInfo.EmptyCellException {
	    	// Determine target table port (1 for missing cells, 0 otherwise)
	    	return (row.getCell(m_iColIndex).isMissing() ? 1 : 0);	    		
	    }
	}
}
