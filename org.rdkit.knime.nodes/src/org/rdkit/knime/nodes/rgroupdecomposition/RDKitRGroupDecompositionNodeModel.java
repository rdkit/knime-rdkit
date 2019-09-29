/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2019
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
package org.rdkit.knime.nodes.rgroupdecomposition;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.RDKit.Atom;
import org.RDKit.RDKFuncs;
import org.RDKit.RGroupDecomposition;
import org.RDKit.RGroupDecompositionParameters;
import org.RDKit.ROMol;
import org.RDKit.ROMol_Vect;
import org.RDKit.RWMol;
import org.RDKit.StringMolMap;
import org.RDKit.StringMolMap_Vect;
import org.RDKit.StringROMol_VectMap;
import org.knime.chem.types.SmartsCell;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;
import org.rdkit.knime.nodes.mcs.MCSUtils;
import org.rdkit.knime.types.RDKitAdapterCell;
import org.rdkit.knime.types.RDKitMolCellFactory;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SafeGuardedResource;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.WarningConsolidator;
import org.rdkit.knime.util.WarningConsolidator.Context;

/**
 * This class implements the node model of the RDKitRGroups node
 * providing calculations based on the open source RDKit library.
 * 
 * Open questions:
 * - Since we process all molecules in the input table at once, at some point the table size might be too large
 *   How should we deal with this limit? No problem, only stupid users would feed in memory collapsing numbers of molecules
 * - Should we allow definition of cores as parameter AND optional second input table to combine them? No, table would take precedence, but we should warn 
 * - Is it true that input cores can be provided as SMILES or SMARTS? Also a mix?  No mix, either SMILES or SMARTS. SMARTS as parameters, SMILES or SMARTS as input table
 * - How to do the cleanup of the data structures related to the decomposition? Clean main result object and all input molecules
 * 
 * @author Manuel Schwarze
 */
public class RDKitRGroupDecompositionNodeModel extends AbstractRDKitNodeModel {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitRGroupDecompositionNodeModel.class);

	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;

	/** Input data info index for SMARTS values for defining cores (optional table). */
	protected static final int INPUT_COLUMN_SMARTS = 0;
	
	/** Row context for generating warnings, if something is incorrect in table 2 (cores). */
	protected static final WarningConsolidator.Context ROW_CONTEXT_TABLE_CORES = new Context("cores-table", "row", "rows", true);

	//
	// Members
	//

	/** Settings model for the column name of the input column. */
	private final SettingsModelString m_modelInputColumnName =
			registerSettings(RDKitRGroupDecompositionNodeDialog.createInputColumnNameModel(), "input_column", "first_column");

	/** Settings model for the column name of the SMARTS input column (cores). */
	private final SettingsModelString m_modelCoresInputColumnName =
			registerSettings(RDKitRGroupDecompositionNodeDialog.createCoresInputColumnNameModel(), "cores_input_column");

	/** Settings model for the core smarts to be used for the R-Group Decomposition. */
	private final SettingsModelString m_modelCoreSmarts =
			registerSettings(RDKitRGroupDecompositionNodeDialog.createSmartsModel());

	/** Settings model for the option to remove empty Rx columns. */
	private final SettingsModelBoolean m_modelRemoveEmptyColumns =
			registerSettings(RDKitRGroupDecompositionNodeDialog.createRemoveEmptyColumnsModel(), true);

	//
	// Intermediate Results
	//
	
	private ROMol[] m_arrSmarts = null;

	private AtomicInteger[] m_aiFilledCellCounter = null;
	
	private int m_iNumberOfRGroups = 0;

	//
	// Constructor
	//

	/**
	 * Create new node model with one data in- and one out-port.
	 */
	RDKitRGroupDecompositionNodeModel() {
		super(new PortType[] {
					// Input ports (2nd port is optional)
					PortTypeRegistry.getInstance().getPortType(BufferedDataTable.TYPE.getPortObjectClass(), false),
					PortTypeRegistry.getInstance().getPortType(BufferedDataTable.TYPE.getPortObjectClass(), true) },
				new PortType[] {
				// Output ports
					PortTypeRegistry.getInstance().getPortType(BufferedDataTable.TYPE.getPortObjectClass(), false),  
					PortTypeRegistry.getInstance().getPortType(BufferedDataTable.TYPE.getPortObjectClass(), false) 
				});

		getWarningConsolidator().registerContext(ROW_CONTEXT_TABLE_CORES);
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

		// Check core SMARTS
		if (m_modelCoreSmarts.getStringValue().isEmpty()) {
			throw new InvalidSettingsException("Please provide a core SMARTS.");
		}
		
		// Check optional cores table
		// TODO - Currently not supported

		// Consolidate all warnings and make them available to the user
		generateWarnings();

		// Generate output specs
		return m_modelRemoveEmptyColumns.getBooleanValue() ? null : getOutputTableSpecs(inSpecs);
	}

	/**
	 * This implementation generates input data info object for the input mol column
	 * and connects it with the information coming from the appropriate setting model.
	 * {@inheritDoc}
	 */
	@Override
	protected InputDataInfo[] createInputDataInfos(final int inPort, final DataTableSpec inSpec)
			throws InvalidSettingsException {

		InputDataInfo[] arrDataInfo = null;

		// Specify input of table 1 (molecules)
		if (inPort == 0) {
			arrDataInfo = new InputDataInfo[1]; // We have only one input column
			arrDataInfo[INPUT_COLUMN_MOL] = new InputDataInfo(inSpec, m_modelInputColumnName,
					InputDataInfo.EmptyCellPolicy.DeliverEmptyRow, null,
					RDKitMolValue.class);
		}
		// Specify input of optional table 2 (cores)
		else if (inPort == 1) {
			arrDataInfo = new InputDataInfo[1]; // We have only one input column
			arrDataInfo[INPUT_COLUMN_SMARTS] = new InputDataInfo(inSpec, m_modelInputColumnName,
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
	 */
	@Override
	protected DataTableSpec getOutputTableSpec(final int outPort,
			final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		DataTableSpec spec = null;

		switch (outPort) {

		case 0:
			if (m_arrSmarts != null &&  !m_modelRemoveEmptyColumns.getBooleanValue()) {
				final DataColumnSpec[] arrOutputSpec = new DataColumnSpec[m_iNumberOfRGroups + 1];
				arrOutputSpec[0] = new DataColumnSpecCreator("Core", SmartsCell.TYPE).createSpec();
	
				for (int i = 1; i < arrOutputSpec.length; i++) {
					arrOutputSpec[i] = new DataColumnSpecCreator("R" + i,
							RDKitAdapterCell.RAW_TYPE).createSpec();
				}			
	
				spec = new DataTableSpec("RGroups", arrOutputSpec);
			}
			break;
		case 1:
			// Same as input table as we just put in the same rows that did not match
			spec = DataTableSpec.mergeDataTableSpecs(new DataTableSpec("Unmatched"), inSpecs[0]);
			break;
		}

		return spec;
	}

	@Override
	protected double getPreProcessingPercentage() {
		return 0.05d;
	}
	
	/**
	 * Prepares all cores to be used as input for R Group Decomposition. They
	 * come either from a parameter or from an optional second input table.
	 * In both cases they are made available as SMARTS.
	 */
	@Override
	protected void preProcessing(BufferedDataTable[] inData, InputDataInfo[][] arrInputDataInfo, ExecutionContext exec)
			throws Exception {
		// Get the cores either from the parameter or from the input table (TODO)
		String[] arrCores = null;
		
		if (inData != null) {
			// TODO: Get cores from input table
		}
		else {
			arrCores = m_modelCoreSmarts.getStringValue().split("\n");
		}
		
		m_iNumberOfRGroups = 0;
		m_arrSmarts = new ROMol[arrCores.length];
		for (int i = 0; i < arrCores.length; i++) {
			final String coreSmarts = arrCores[i];
			m_arrSmarts[i] = markForCleanup(RWMol.MolFromSmarts(coreSmarts, 0, true));
			if (m_arrSmarts[i] == null) {
				// TODO: Better error handling
				throw new RuntimeException("Invalid SMARTS detected.");
			}
			else {
				m_iNumberOfRGroups += m_arrSmarts[i].getNumAtoms();
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] processing(final BufferedDataTable[] inData, 
			final InputDataInfo[][] arrInputDataInfo,
			final ExecutionContext exec) throws Exception {
		final DataTableSpec[] arrOutSpecs = getOutputTableSpecs(inData);

		// Contains the rows with the R Groups that matched
		final BufferedDataContainer newTableData = exec.createDataContainer(arrOutSpecs[0]);

		// Contains the rows from input which did not match
		final BufferedDataContainer unmatchedTableData = exec.createDataContainer(arrOutSpecs[1]);

		// Get settings and define data specific behavior
		final long lTotalRowCount = inData[0].size();

		// Setup decomposition
		RGroupDecompositionParameters params = new RGroupDecompositionParameters();
		ROMol_Vect vScaffolds = new ROMol_Vect(m_arrSmarts.length);
		for (ROMol scaffold : m_arrSmarts) {
			vScaffolds.add(scaffold);
		}
		RGroupDecomposition decomp = new RGroupDecomposition(vScaffolds, params);
		
		// Get all molecules to be used as input and add the for decomposition
		int rowIndex = 0;
		final ExecutionContext execSubAdd = exec.createSubExecutionContext(0.25d);
		final ExecutionContext execSubProcess = exec.createSubExecutionContext(0.5d); // TODO - Handle with Pseudo Progress
		final ExecutionContext execSubGetResults = exec.createSubExecutionContext(0.25d);
		final List<Boolean> listMatched = new ArrayList<Boolean>();
		for (final CloseableRowIterator i = inData[0].iterator(); i.hasNext(); rowIndex++) {
			final DataRow row = i.next();

			final ROMol mol = markForCleanup(arrInputDataInfo[0][INPUT_COLUMN_MOL].getROMol(row));

			// We use only cells, which are not missing
			boolean bMatched = false;
			if (mol != null) {
				bMatched = (decomp.add(mol) >= 0);
			}

			listMatched.add(bMatched);
			if (!bMatched) {
				unmatchedTableData.addRowToTable(row);
			}

			// Every 20 iterations check cancellation status and report progress
			if (rowIndex % 20 == 0) {
				AbstractRDKitNodeModel.reportProgress(execSubAdd, rowIndex, lTotalRowCount, row, " - Generating R Groups");
			}
		}

		execSubAdd.setProgress(1.0d);

		// Perform decomposition
		if (!decomp.process()) {
			// TOOD: What, if process() returns false? Shall we fail or just return everything as unmatched?
		}
		
		execSubProcess.setProgress(1.0d);

		rowIndex = 0;
		StringMolMap_Vect vResults = markForCleanup(decomp.getRGroupsAsRows());

		for (final CloseableRowIterator i = inData[0].iterator(); i.hasNext(); rowIndex++) {
			final DataRow row = i.next();
		
			if (((Boolean)listMatched.get(rowIndex)).booleanValue()) {
				StringMolMap mapResults = vResults.get(rowIndex);
				ROMol molCore = mapResults.get("Core");
				DataCell[] arrResultCells = new DataCell[m_iNumberOfRGroups + 1];
				arrResultCells[0] = 
				for (int iR = 0; iR < m_iNumberOfRGroups; iR++) {
					ROMol rGroup = mapResults.get("R" + iR);
					AbstractRDKitCellFactory.mergeDataCells(row, arrResultCells, -1);
					DefaultRow resultRow = new DefaultRow(row.getKey(), row);
					
				}
				

			}

			// Every 20 iterations check cancellation status and report progress
			if (rowIndex % 20 == 0) {
				AbstractRDKitNodeModel.reportProgress(execSubGetResults, rowIndex, lTotalRowCount, row, " - Generating R Groups");
			}
		}
		
		execSubGetResults.setProgress(1.0d);
		
		int iSuccessIndex = decomp.add(mol);
		if (iSuccessIndex >= 0) {
			boolean bSuccess = decomp.process();
			if (bSuccess) {
				StringMolMap_Vect result = decomp.getRGroupsAsRows();
				result.get(0..n).get("R1".."Rn")
				result.get(0..n).get("Core");
				result.get(0..n).
			}
			else {
				
			}
		}
		else {
			
		}
		

		
		
		// Calculate MCS
		final ExecutionContext execSub2 = exec.createSubExecutionContext(0.98d);

		final DataCell[] arrResults = MCSUtils.calculateMCS(mols, m_modelThreshold.getDoubleValue(),
				m_modelRingMatchesRingOnlyOption.getBooleanValue(), m_modelCompleteRingsOnlyOption.getBooleanValue(),
				m_modelMatchValencesOption.getBooleanValue(), m_modelAtomComparison.getValue(),
				m_modelBondComparison.getValue(), m_modelTimeout.getIntValue(), execSub2);

		newTableData.addRowToTable(new DefaultRow("MCS",
				arrResults[MCSUtils.SMARTS_INDEX],
				arrResults[MCSUtils.ATOM_NUMBER_INDEX],
				arrResults[MCSUtils.BOND_NUMBER_INDEX],
				arrResults[MCSUtils.TIMED_OUT_INDEX]));

		exec.checkCanceled();
		exec.setProgress(1.0, "Finished Processing");

		// Generate warning, if no MCS was found
		if (arrResults[MCSUtils.SMARTS_INDEX] == null || arrResults[MCSUtils.SMARTS_INDEX].isMissing()) {
			if (mols.size() > 0) {
				getWarningConsolidator().saveWarning("No MCS found - Created empty cells.");
			}
			else {
				getWarningConsolidator().saveWarning("No input molecules found - Created empty cells.");
			}
		}
		else if (arrResults[MCSUtils.TIMED_OUT_INDEX] != null &&
				!arrResults[MCSUtils.TIMED_OUT_INDEX].isMissing() &&
				((BooleanCell)arrResults[MCSUtils.TIMED_OUT_INDEX]).getBooleanValue() == true) {
			getWarningConsolidator().saveWarning("The MCS calculation timed out.");
		}

		newTableData.close();

		return new BufferedDataTable[] { newTableData.getTable() };
	}

	/**
	 * Generates the output factory that calculates the R Group Decomposition.
	 * 
	 * @return Factory instance.
	 */
	protected AbstractRDKitCellFactory createOutputFactory(final InputDataInfo[] arrInputDataInfo)
			throws InvalidSettingsException {

		// Generate factory
		final AbstractRDKitCellFactory factory = new AbstractRDKitCellFactory(this, AbstractRDKitCellFactory.RowFailurePolicy.DeliverEmptyValues,
				getWarningConsolidator(), arrInputDataInfo, arrOutputSpec) {

			@Override
			/**
			 * This method implements the calculation logic to generate the new cells based on
			 * the input made available in the first (and second) parameter.
			 * {@inheritDoc}
			 */
			public DataCell[] process(final InputDataInfo[] arrInputDataInfo, final DataRow row, final long lUniqueWaveId) throws Exception {
				// Calculate the new cells
				final DataCell[] outputCells = createEmptyCells(iCoreAtomNumber);
				final ROMol mol = markForCleanup(arrInputDataInfo[INPUT_COLUMN_MOL].getROMol(row), lUniqueWaveId);

				try {
					RGroupDecomposition decomp = new RGroupDecomposition(core.get()); // or multiple (from optional second input table)
					int iSuccessIndex = decomp.add(mol);
					if (iSuccessIndex >= 0) {
						boolean bSuccess = decomp.process();
						if (bSuccess) {
							StringMolMap_Vect result = decomp.getRGroupsAsRows();
							result.get(0..n).get("R1".."Rn")
							result.get(0..n).get("Core");
							result.get(0..n).
						}
						else {
							
						}
					}
					else {
						
					}
					
					// Get the molecule with replaced core
					final ROMol chains = markForCleanup(RDKFuncs.replaceCore(mol, core.get(), true, true), lUniqueWaveId);

					if (chains != null) {
						final ROMol_Vect frags = markForCleanup(RDKFuncs.getMolFrags(chains), lUniqueWaveId);
						final int iFragsSize = (int)frags.size();

						// Iterate through all fragments
						for (int i = 0; i < iFragsSize; i++) {
							final ROMol frag = frags.get(i);
							final int iNumAtoms = (int)frag.getNumAtoms();
							boolean found = false;

							// Iterate through all atoms
							for (int atomIndex = 0; atomIndex < iNumAtoms; atomIndex++) {
								final Atom atom = frag.getAtomWithIdx(atomIndex);
								if (atom.getAtomicNum() == 0) {
									final int iso= (int)atom.getIsotope();

									// Dummies are labeled by the zero-based atom index they're attached to.
									// To make things clearer to the user, increment these.
									atom.setIsotope(iso + 1);
									outputCells[iso] = RDKitMolCellFactory.createRDKitAdapterCell(frag);
									m_aiFilledCellCounter[iso].incrementAndGet();
									found=true;
									break;
								}
							}

							if (!found) {
								final String msg = "Attachment label not found for a side chain.";
								getWarningConsolidator().saveWarning(
										WarningConsolidator.ROW_CONTEXT.getId(), msg);
								LOGGER.warn(msg + " (Row '" + row.getKey() + "')");
							}
						}
					}
				}
				catch (final Exception e) {
					final String msg = "Could not construct a valid output molecule.";
					getWarningConsolidator().saveWarning(
							WarningConsolidator.ROW_CONTEXT.getId(), msg);
					LOGGER.warn(msg + " (Row '" + row.getKey() + "')");
				}

				return outputCells;
			}
		};

		// Enable or disable this factory to allow parallel processing
		// Note: In this implementation always parallel processing is used
		factory.setAllowParallelProcessing(true);

		return factory;
	}

	/**
	 * {@inheritDoc}
	 * This implementation returns always 0.05d.
	 * 
	 * @return Returns always 0.05d.
	 */
	@Override
	protected double getPostProcessingPercentage() {
		return 0.05d;
	}

	/**
	 * In the case that the option to remove empty Rx columns is enabled,
	 * this post processing routine will do exactly that.
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] postProcessing(final BufferedDataTable[] inData,
			final InputDataInfo[][] arrInputDataInfo,
			final BufferedDataTable[] processingResult, final ExecutionContext exec)
					throws Exception {

		BufferedDataTable[] arrResults = processingResult;

		// Determine, if the user wants to remove all empty Rx columns
		if (m_modelRemoveEmptyColumns.getBooleanValue()) {
			if (processingResult != null && processingResult.length == 1) {
				final ColumnRearranger rearranger = new ColumnRearranger(processingResult[0].getDataTableSpec());

				for (int i = 0; i < m_aiFilledCellCounter.length; i++) {
					if (m_aiFilledCellCounter[i].get() == 0) {
						rearranger.remove("R" + (i + 1));
					}
				}

				// Create the new table without empty Rx columns
				arrResults = new BufferedDataTable[] {
						exec.createColumnRearrangeTable(processingResult[0], rearranger, exec)
				};
			}
		}

		return arrResults;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanupIntermediateResults() {
		m_aiFilledCellCounter = null;
		m_arrSmarts = null;
		m_iNumberOfRGroups = 0;
	}
}
