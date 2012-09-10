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
 * This interface can be implemented to express a filter condition.
 * 
 * @author Manuel Schwarze
 */
public interface FilterCondition {

    /**
     * This method implements the condition on which data are filtered.
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
     * @return True to keep the row, false to filter it out.
     * 
     * @throws org.rdkit.knime.util.InputDataInfo.EmptyCellException Thrown, if an empty cell is encountered
     * 		in the input column, and if the empty cell handling policy in the InputDataInfo object is set
     * 		accordingly. This exception will be caught and handled properly in the processing method.
     */
    boolean include(int iInPort, int iRowIndex, 
    		DataRow row, InputDataInfo[] arrInputDataInfo, 
    		int iUniqueWaveId) throws InputDataInfo.EmptyCellException;

	/**
	 * For convenience reasons this filter is provided to filter out rows that
	 * contain empty cells in a certain column. This column needs to be specified
	 * when calling the constructor.
	 * 
	 * @author Manuel Schwarze
	 */
	public static class ExcludeMissingCell implements FilterCondition {
		
		/** The index of the column in focus of this filter. */
		final int m_iColIndex;
		
		//
		// Constructor
		//
		
		/**
		 * Creates a new Missing Cell Filter for the specified column index.
		 * 
		 * @param iColIndex Column index that will be checked by this filter.
		 */
		public ExcludeMissingCell(int iColIndex) {
			m_iColIndex = iColIndex;
		}
		
	    /**
	     * This method implements the condition on which data are filtered.
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
	     * @return True to keep the row, false to filter it out.
	     * 
	     * @throws org.rdkit.knime.util.InputDataInfo.EmptyCellException Thrown, if an empty cell is encountered
	     * 		in the input column, and if the empty cell handling policy in the InputDataInfo object is set
	     * 		accordingly. This exception will be caught and handled properly in the processing method.
	     */
	    public boolean include(int iInPort, int iRowIndex, 
	    		DataRow row, InputDataInfo[] arrInputDataInfo, 
	    		int iUniqueWaveId) throws InputDataInfo.EmptyCellException {
			return !row.getCell(m_iColIndex).isMissing();
		}
	}
}
