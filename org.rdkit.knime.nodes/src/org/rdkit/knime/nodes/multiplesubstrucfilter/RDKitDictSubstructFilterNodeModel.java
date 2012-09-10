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
package org.rdkit.knime.nodes.multiplesubstrucfilter;

import java.text.ParseException;

import org.RDKit.ROMol;
import org.RDKit.RWMol;
import org.knime.chem.types.SmartsValue;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.rdkit.knime.nodes.moleculesubstructfilter.AbstractRDKitSubstructFilterNodeModel;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsUtils;

/**
 * This class implements the node model of the RDKitDictSubstructFilter node
 * providing calculations based on the open source RDKit library.
 * 
 * @author Manuel Schwarze, Novartis
 */
public class RDKitDictSubstructFilterNodeModel extends AbstractRDKitSubstructFilterNodeModel {

	//
	// Constructor
	//
	
	/**
	 * Creates the node model for a molecule substructure filter based on a SMARTS query column.
	 */
	@SuppressWarnings("unchecked")
	public RDKitDictSubstructFilterNodeModel() {
		super(SmartsValue.class);
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
    	// Perform all checks except for the query column
    	super.configure(inSpecs);
   	
        // Auto guess the query mol column if not set - fails if no compatible column found
        SettingsUtils.autoGuessColumn(inSpecs[1], m_modelQueryColumnName, SmartsValue.class, 
        		(inSpecs[0] == inSpecs[1] ? 1 : 0), // If 1st and 2nd table equal, auto guess with second match         		
        		"Auto guessing: Using column %COLUMN_NAME% as query SMARTS column.", 
        		"No SMARTS compatible column in query table. Use \"Molecule Type Cast\" " +
        			"node to convert molecules or Strings to SMARTS values.", getWarningConsolidator()); 

        // Determines, if the query mol column exists - fails if it does not
        SettingsUtils.checkColumnExistence(inSpecs[1], m_modelQueryColumnName, SmartsValue.class,  
        		"Query SMARTS column has not been specified yet.",
        		"Query SMARTS column %COLUMN_NAME% does not exist. Has the second input table changed?");

        // Consolidate all warnings and make them available to the user
        generateWarnings();

        // Generate output specs
        return getOutputTableSpecs(inSpecs);
    }	
	
	/**
	 * This method gets called from the method {@link #execute(BufferedDataTable[], ExecutionContext)}, before
	 * the row-by-row processing starts. All necessary pre-calculations can be done here. Results of the method
	 * should be made available through member variables, which get picked up by the other methods like
	 * process(InputDataInfo[], DataRow) in the factory or 
	 * {@link #postProcessing(BufferedDataTable[], BufferedDataTable[], ExecutionContext)} in the model.
	 * 
	 * @param inData The input tables of the node.
	 * @param arrInputDataInfo Information about all columns of the input tables.
	 * @param exec The execution context, which was derived as sub-execution context based on the percentage
	 * 		setting of #getPreProcessingPercentage(). Track the progress from 0..1.
	 * 
	 * @throws Exception Thrown, if pre-processing fails.
	 */
	protected void preProcessing(final BufferedDataTable[] inData, InputDataInfo[][] arrInputDataInfo,
		final ExecutionContext exec) throws Exception {

		int i = 0;
		int iRowCount = inData[1].getRowCount();
		ROMol[] arrPatterns = new ROMol[iRowCount];
		int iTotalPatternAtomsCount = 0;
		int iTotalEmptyPatternCells = 0;
		
		exec.setMessage("Evaluate query SMARTS");
		
		// Get all query molecules (empty cells will result in null values according to our empty cell policy)
		for (DataRow row : inData[1]) {
			String strSmarts = arrInputDataInfo[1][INPUT_COLUMN_QUERY].getSmarts(row);
			
			if (strSmarts != null) {
				arrPatterns[i] = markForCleanup(RWMol.MolFromSmarts(strSmarts, 0, true));
				if (arrPatterns[i] == null) {
	                throw new ParseException("Could not parse SMARTS '"
	                        + strSmarts + "' in row " + row.getKey(), 0);
	            }
				else {
					iTotalPatternAtomsCount += arrPatterns[i].getNumAtoms();
				}
			}
			else {
				iTotalEmptyPatternCells++;
			}
			
			
			if (i % 20 == 0) {
				reportProgress(exec, i, iRowCount, row, "Evaluate query SMARTS");
			}

			i++;
		}
		
		setPreprocessingResults(arrPatterns, iTotalEmptyPatternCells, iTotalPatternAtomsCount);
		
		// Does not do anything by default
		exec.setProgress(1.0d);
	}		
}