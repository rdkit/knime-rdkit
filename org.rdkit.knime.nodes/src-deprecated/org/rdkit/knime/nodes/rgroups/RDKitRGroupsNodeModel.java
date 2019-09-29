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
package org.rdkit.knime.nodes.rgroups;

import java.util.concurrent.atomic.AtomicInteger;

import org.RDKit.Atom;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.RDKit.ROMol_Vect;
import org.RDKit.RWMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;
import org.rdkit.knime.types.RDKitAdapterCell;
import org.rdkit.knime.types.RDKitMolCellFactory;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SafeGuardedResource;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class implements the node model of the RDKitRGroups node
 * providing calculations based on the open source RDKit library.
 * 
 * @author Greg Landrum
 * @author Manuel Schwarze
 */
public class RDKitRGroupsNodeModel extends AbstractRDKitNodeModel {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitRGroupsNodeModel.class);

	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;

	//
	// Members
	//

	/** Settings model for the column name of the input column. */
	private final SettingsModelString m_modelInputColumnName =
			registerSettings(RDKitRGroupsNodeDialog.createInputColumnNameModel(), "input_column", "first_column");

	/** Settings model for the core smarts to be used for the R-Group Decomposition. */
	private final SettingsModelString m_modelCoreSmarts =
			registerSettings(RDKitRGroupsNodeDialog.createSmartsModel());

	/** Settings model for the option to remove empty Rx columns. */
	private final SettingsModelBoolean m_modelRemoveEmptyColumns =
			registerSettings(RDKitRGroupsNodeDialog.createRemoveEmptyColumnsModel(), true);

	//
	// Intermediate Results
	//

	private AtomicInteger[] m_aiFilledCellCounter = null;

	//
	// Constructor
	//

	/**
	 * Create new node model with one data in- and one out-port.
	 */
	RDKitRGroupsNodeModel() {
		super(1, 1);
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
	 */
	@Override
	protected DataTableSpec getOutputTableSpec(final int outPort,
			final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		DataTableSpec spec = null;

		switch (outPort) {
		case 0:
			spec = new DataTableSpec("RGroups", inSpecs[0],
					new DataTableSpec(createOutputFactory(null).getColumnSpecs()));
			break;
		}

		return spec;
	}

	/**
	 * Generates the output factory that calculates the R Group Decomposition.
	 * 
	 * @return Factory instance.
	 */
	protected AbstractRDKitCellFactory createOutputFactory(final InputDataInfo[] arrInputDataInfo)
			throws InvalidSettingsException {
		// Generate column specs for the output table columns produced by this factory
		final String coreSmarts = m_modelCoreSmarts.getStringValue();

		// Note: The cleanup will be performed only at the end of entire execution
		final SafeGuardedResource<ROMol> core = markForCleanup(new SafeGuardedResource<ROMol>(!coreSmarts.contains("$")) {
			@Override
			protected ROMol createResource() {
				return markForCleanup(RWMol.MolFromSmarts(coreSmarts, 0, true));
			};
		});

		final ROMol coreCheck = core.get();
		if (coreCheck == null)
			throw new InvalidSettingsException("Unparseable core SMARTS: '"
					+ coreSmarts + "'");

		// The number of atoms in the core SMARTS defines the number of output columns
		final int iCoreAtomNumber = (int)coreCheck.getNumAtoms();
		m_aiFilledCellCounter = new AtomicInteger[iCoreAtomNumber];
		final DataColumnSpec[] arrOutputSpec = new DataColumnSpec[iCoreAtomNumber];
		for (int i = 0; i < arrOutputSpec.length; i++) {
			m_aiFilledCellCounter[i] = new AtomicInteger();
			arrOutputSpec[i] = new DataColumnSpecCreator("R" + (i + 1),
					RDKitAdapterCell.RAW_TYPE).createSpec();
		}

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
	 */
	@Override
	protected BufferedDataTable[] processing(final BufferedDataTable[] inData, final InputDataInfo[][] arrInputDataInfo,
			final ExecutionContext exec) throws Exception {
		final DataTableSpec[] arrOutSpecs = getOutputTableSpecs(inData);

		// Contains the rows with the result columns
		final BufferedDataContainer rGroupTable = exec.createDataContainer(arrOutSpecs[0]);

		// Setup main factory
		final AbstractRDKitCellFactory factory = createOutputFactory(arrInputDataInfo[0]);

		final AbstractRDKitNodeModel.ResultProcessor resultProcessor =
				new AbstractRDKitNodeModel.ResultProcessor() {

			/**
			 * {@inheritDoc}
			 * This implementation determines, if all result cells are missing.
			 * Only if there is at least one R Group Cell filled, we will add
			 * the row to the result table. Otherwise it gets dumped.
			 */
			@Override
			public void processResults(final long rowIndex, final DataRow row, final DataCell[] arrResults) {
				for (int i = 0; i < arrResults.length; i++) {
					if (!arrResults[i].isMissing()) {
						rGroupTable.addRowToTable(AbstractRDKitCellFactory.mergeDataCells(row, arrResults, -1));
						break;
					}
				}
			}
		};

		// Get settings and define data specific behavior
		final long lTotalRowCount = inData[0].size();

		// Runs the multiple threads to do the work
		try {
			new AbstractRDKitNodeModel.ParallelProcessor(factory, resultProcessor, lTotalRowCount,
					getWarningConsolidator(), exec).run(inData[0]);
		}
		catch (final Exception e) {
			exec.checkCanceled();
			throw e;
		}

		rGroupTable.close();

		return new BufferedDataTable[] { rGroupTable.getTable() };
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
	}
}
