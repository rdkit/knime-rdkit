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
