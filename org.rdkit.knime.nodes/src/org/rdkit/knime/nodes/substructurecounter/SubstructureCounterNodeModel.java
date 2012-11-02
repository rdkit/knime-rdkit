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
package org.rdkit.knime.nodes.substructurecounter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.RDKit.Match_Vect_Vect;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.AbstractRDKitCalculatorNodeModel;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.nodes.AdditionalHeaderInfo;
import org.rdkit.knime.nodes.TableViewSupport;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsUtils;

/**
 * This class implements the node model of the RDKitSubstructureCounter node
 * providing calculations based on the open source RDKit library.
 *
 * @author Swarnaprava Singh
 * @author Manuel Schwarze
 */
public class SubstructureCounterNodeModel extends AbstractRDKitCalculatorNodeModel
	implements TableViewSupport {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(SubstructureCounterNodeModel.class);

	/** Input data info index for Mol value (first table). */
	protected static final int INPUT_COLUMN_MOL = 0;

	/** Input data info index for Query Molecule (second table). */
	protected static final int INPUT_COLUMN_QUERY = 0;

	//
	// Members
	//

	/** Settings model for the column name of the input column. */
    private final SettingsModelString m_modelInputColumnName =
    	registerSettings(SubstructureCounterNodeDialog.createInputColumnNameModel(), "input_column", "inputMolCol");
    // Accept also deprecated keys

	/** Settings model for the column name of the query molecule column. */
	private final SettingsModelString m_modelQueryColumnName =
		registerSettings(SubstructureCounterNodeDialog.createQueryInputModel());

	/** Settings model for the option to count unique matches only. */
	private final SettingsModelBoolean m_modelUniqueMatchesOnly =
		registerSettings(SubstructureCounterNodeDialog.createUniqueMatchesOnlyModel());

	//
	// Intermediate Results
	//

	/** Percentage for pre-processing. Set when execution starts up. */
	private double m_dPreProcessingShare;

	/** Column names. Result of pre-processing. */
	private String[] m_arrResultColumnNames;

	/** SMILES strings of query molecules read from second input table. Result of pre-processing. */
	private String[] m_arrQueriesAsSmiles;

	/** ROMol objects of query molecules read from second input table. Result of pre-processing. */
	private ROMol[] m_arrQueriesAsRDKitMols;

    //
    // Constructor
    //

    /**
     * Create new node model with one data in- and one out-port.
     */
    SubstructureCounterNodeModel() {
        super(2, 1);
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

        // Auto guess the query mol column if not set - fails if no compatible column found
        SettingsUtils.autoGuessColumn(inSpecs[1], m_modelQueryColumnName, RDKitMolValue.class,
        		(inSpecs[0] == inSpecs[1] ? 1 : 0), // If 1st and 2nd table equal, auto guess with second match
        		"Auto guessing: Using column %COLUMN_NAME% as query molecule column.",
        		"No RDKit Mol compatible column in query molecule table. Use \"Molecule to RDKit\" " +
        			"node to convert Smiles or SDF.", getWarningConsolidator());

        // Determines, if the query mol column exists - fails if it does not
        SettingsUtils.checkColumnExistence(inSpecs[1], m_modelQueryColumnName, RDKitMolValue.class,
        		"Query molecule column has not been specified yet.",
        		"Query molecule column %COLUMN_NAME% does not exist. Has the second input table changed?");

        // Consolidate all warnings and make them available to the user
        generateWarnings();

        // We cannot know how many columns we will generate before execution
        return new DataTableSpec[] { null };
    }

    /**
     * This implementation generates input data info object for the input mol column
     * as well as the query molecule column and connects it with the information coming
     * from the appropriate setting models.
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
	protected InputDataInfo[] createInputDataInfos(final int inPort, final DataTableSpec inSpec)
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
	    				RDKitMolValue.class);
	    		break;
    	}

    	return (arrDataInfo == null ? new InputDataInfo[0] : arrDataInfo);
    }


    /**
     * Returns the output table specification of the specified out port. This implementation
     * works based on a ColumnRearranger and delivers only a specification for
     * out port 0, based on an input table on in port 0. Override this method if
     * other behavior is needed.
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
    @Override
    protected DataTableSpec getOutputTableSpec(final int outPort,
    		final DataTableSpec[] inSpecs) throws InvalidSettingsException {
    	DataTableSpec spec = null;

    	if (outPort == 0) {
    		// Create the column rearranger, which will generate the spec
	        spec = createColumnRearranger(outPort, inSpecs[0]).createSpec();
    	}

    	return spec;
    }

    /**
     * {@inheritDoc}
     * Calculates additionally to the normal execution procedure the pre-processing
     * percentage.
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
    		final ExecutionContext exec) throws Exception {
    	// Calculate pre-processing share based on overall rows
    	m_dPreProcessingShare = (double)inData[1].getRowCount() /
    		(double)(inData[0].getRowCount() + inData[1].getRowCount());

    	// Perform normalized execution
    	return super.execute(inData, exec);
    }

    /**
     * This implementation generates input data info object for the input mol column
     * and connects it with the information coming from the appropriate setting model.
     * {@inheritDoc}
     */
	@Override
    protected AbstractRDKitCellFactory[] createOutputFactories(final int outPort, final DataTableSpec inSpec)
		throws InvalidSettingsException {

		AbstractRDKitCellFactory[] arrOutputFactories = null;

    	// Specify output of table 1
    	if (outPort == 0) {
    		// Allocate space for all factories (usually we have only one)
    		arrOutputFactories = new AbstractRDKitCellFactory[1];

    		// Factory 1:
    		// ==========
    		// Generate column specs for the output table columns produced by this factory
    		// This is only possible, if pre-processing took already place and we know about
    		// query molecules
    		final int iResultColumnCount = m_arrResultColumnNames.length;
    		DataColumnSpec[] arrOutputSpec = new DataColumnSpec[iResultColumnCount];
    		for (int i = 0; i < iResultColumnCount; i++) {
    			// Create spec with additional information
    			DataColumnSpecCreator creator = new DataColumnSpecCreator(m_arrResultColumnNames[i], IntCell.TYPE);
    			new AdditionalHeaderInfo("Smiles", m_arrQueriesAsSmiles[i], -1).writeInColumnSpec(creator);
        		arrOutputSpec[i] = creator.createSpec();
    		}

    		// Provide unique matches only option
    		final boolean bUniqueMatchesOnly = m_modelUniqueMatchesOnly.getBooleanValue();

    		// Generate factory
    	    arrOutputFactories[0] = new AbstractRDKitCellFactory(this, AbstractRDKitCellFactory.RowFailurePolicy.DeliverEmptyValues,
           		getWarningConsolidator(), null, arrOutputSpec) {

	   			@Override
	   		    /**
	   		     * This method implements the calculation logic to generate the new cells based on
	   		     * the input made available in the first (and second) parameter.
	   		     * {@inheritDoc}
	   		     */
	   		    public DataCell[] process(final InputDataInfo[] arrInputDataInfo, final DataRow row, final int iUniqueWaveId) throws Exception {
	   		    	DataCell[] arrOutputCells = createEmptyCells(iResultColumnCount);

	   		    	// Calculate the new cells
	   		    	ROMol mol = markForCleanup(arrInputDataInfo[INPUT_COLUMN_MOL].getROMol(row),
	   		    			iUniqueWaveId);

	   		    	for (int i = 0; i < iResultColumnCount; i++) {
	   		    		final ROMol query = m_arrQueriesAsRDKitMols[i];
	   		    		if (mol != null && query != null) {
	   		    			Match_Vect_Vect ms = markForCleanup(
	   		    					mol.getSubstructMatches(query, bUniqueMatchesOnly), iUniqueWaveId);
	   		    			arrOutputCells[i] = new IntCell((int)ms.size());
	   		    		}
	   		    	}

	   		        return arrOutputCells;
	   		    }
	   		};

	   		// Enable or disable this factory to allow parallel processing
	   		arrOutputFactories[0].setAllowParallelProcessing(true);
    	}

    	return (arrOutputFactories == null ? new AbstractRDKitCellFactory[0] : arrOutputFactories);
    }

	/**
	 * Returns the percentage of pre-processing activities from the total execution.
	 *
	 * @return Percentage of pre-processing.
	 */
	@Override
    protected double getPreProcessingPercentage() {
		return m_dPreProcessingShare;
	}

	/**
	 * Converts the query molecule cells into SMILES and ROMol values. The SMILES values will
	 * be used to name the new target columns. The ROMol values will be used to count the
	 * substructures using RDKit functionality. For invalid query cells a warning will be
	 * generated and they will not be taken into account when counting substructures.<br />
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
	 *
	 * @see #m_arrResultColumnNames
	 * @see #m_arrQueriesAsSmiles
	 * @see #m_arrQueriesAsRDKitMols
	 */
	@Override
    protected void preProcessing(final BufferedDataTable[] inData, final InputDataInfo[][] arrInputDataInfo,
		final ExecutionContext exec) throws Exception {

		int iQueryRowCount = inData[1].getRowCount();
		List<String> listColumnNames = new ArrayList<String>(iQueryRowCount);
		List<String> listQueriesAsSmiles = new ArrayList<String>(iQueryRowCount);
		List<ROMol> listQueriesAsRDKitMols = new ArrayList<ROMol>(iQueryRowCount);
		List<RowKey> listEmptyQueries = new ArrayList<RowKey>();
		List<RowKey> listInvalidQueries = new ArrayList<RowKey>();
		List<RowKey> listDuplicatedQueries = new ArrayList<RowKey>();
		Map<String, Integer> mapDuplicates = new HashMap<String, Integer>();

		int iRow = 0;

		// Creating an arrays of SMILES and ROMol query molecules
		for (DataRow row : inData[1]) {

			// Get ROMol value
			ROMol mol = markForCleanup(arrInputDataInfo[1][INPUT_COLUMN_QUERY].getROMol(row));

			// Generate column labels (SMILES)
			// If we cannot get a mol, we remember the empty column
			if (mol == null) {
				listEmptyQueries.add(row.getKey());
			}
			// Otherwise: Get canonical SMILES from the query molecule
			else {
				String strQueryMolString = null;
				
				// Check (by heuristics) if we have a SMARTS as query molecule
				if (mol.getNumAtoms() > 0 && mol.getAtomWithIdx(0).hasQuery()) {
					strQueryMolString = RDKFuncs.MolToSmarts(mol);
				}
				
				// Or a SMILES (or SMARTS convertion failed)
				if (strQueryMolString == null) {
					strQueryMolString = mol.MolToSmiles(true);
				}

				// Fallback, if SMARTS/SMILES conversion failed
				if (strQueryMolString == null) {
					listInvalidQueries.add(row.getKey());
				}
				// Otherwise: Everything is fine - use this query
				else {
					String strColumnName = strQueryMolString;

					// Check for duplicate, still include it, but warn
					Integer intCount = mapDuplicates.get(strQueryMolString);
					if (intCount != null) {
						int iDuplicate = intCount + 1;
						mapDuplicates.put(strQueryMolString, iDuplicate);
						listDuplicatedQueries.add(row.getKey());
						strColumnName += " (Duplicate " + iDuplicate + ")";
					}
					else {
						mapDuplicates.put(strQueryMolString, 0);
					}

					// Ensure that our target column name is unique
					strColumnName = SettingsUtils.makeColumnNameUnique(strColumnName, 
							inData[0].getDataTableSpec(), listColumnNames);
					listColumnNames.add(strColumnName);
					listQueriesAsRDKitMols.add(mol);
					listQueriesAsSmiles.add(strQueryMolString);
				}
			}

			iRow++;

			exec.setProgress(new StringBuilder("Analyzing query molecules (")
				.append(iRow)
				.append(" of ")
				.append(iQueryRowCount)
				.append(")").toString());
		}

		// Store queries and column labels (SMILES) in intermediate result member variables
		m_arrResultColumnNames = listColumnNames.toArray(
				new String[listColumnNames.size()]);
		m_arrQueriesAsRDKitMols = listQueriesAsRDKitMols.toArray(
				new ROMol[listQueriesAsRDKitMols.size()]);
		m_arrQueriesAsSmiles = listQueriesAsSmiles.toArray(
				new String[listQueriesAsSmiles.size()]);

		// Generate warnings
		generateWarning(listEmptyQueries, iQueryRowCount, "Ignoring empty");
		generateWarning(listInvalidQueries, iQueryRowCount, "Ignoring invalid");
		generateWarning(listDuplicatedQueries, iQueryRowCount, "Found duplicated");

		// Show the warning already immediately
		generateWarnings();

		// Help the garbage collector
		listColumnNames.clear();
		listQueriesAsRDKitMols.clear();
		listQueriesAsSmiles.clear();
		listEmptyQueries.clear();
		listInvalidQueries.clear();
		listDuplicatedQueries.clear();
		mapDuplicates.clear();

		exec.setProgress(1.0d);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanupIntermediateResults() {
		m_arrQueriesAsRDKitMols = null;
		m_arrQueriesAsSmiles = null;
		m_arrResultColumnNames = null;
		m_dPreProcessingShare = 0;
	}

	//
	// Private Methods
	//

	/**
	 * Generates and saves a warning message, if applicable, based on the
	 * specified list of row keys (can be empty) and the message stump.
	 *
	 * @param listRowKeys List of row keys. Must not be null.
	 * @param iTotalQueries Total number of processed queries.
	 * @param strMsgStump Stump of the warning message to be generated.
	 */
	private void generateWarning(final List<RowKey> listRowKeys, final int iTotalQueries,
			final String strMsgStump) {
		int iQueryCount = listRowKeys.size();

		if (iQueryCount > 0) {
			String strMsg = strMsgStump + " quer";
			if (iQueryCount <= 10) {
				String rowKeyList = listRowKeys.toString();
				strMsg += "y in the following rows: " +
					rowKeyList.substring(1, rowKeyList.length() - 1);
			}
			else {
				strMsg +="ies.";
			}

			strMsg += " [" + iQueryCount + " of " + iTotalQueries +
					(iTotalQueries == 1 ? " query]" : " queries]");

			getWarningConsolidator().saveWarning(strMsg);
		}
	}
}
