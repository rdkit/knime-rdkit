/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2010
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
package org.rdkit.knime.nodes.twocomponentreaction2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.RDKit.ChemicalReaction;
import org.RDKit.ROMol;
import org.RDKit.ROMol_Vect;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortType;
import org.knime.core.util.MultiThreadWorker;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;
import org.rdkit.knime.nodes.onecomponentreaction2.AbstractRDKitReactionNodeModel;
import org.rdkit.knime.types.RDKitMolCellFactory;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.InputDataInfo.EmptyCellException;
import org.rdkit.knime.util.RandomAccessRowIterator;
import org.rdkit.knime.util.SafeGuardedResource;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class implements the node model of the RDKitTwoComponentReaction node
 * providing calculations based on the open source RDKit library.
 * 
 * @author Greg Landrum
 * @author Manuel Schwarze
 */
public class RDKitTwoComponentReactionNodeModel extends AbstractRDKitReactionNodeModel<RDKitTwoComponentReactionNodeDialog> {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitTwoComponentReactionNodeModel.class);

	/** Input data info index for Mol value of reactant 1. */
	protected static final int INPUT_COLUMN_REACTANT1 = 0;

	/** Input data info index for Mol value of reactant 2. */
	protected static final int INPUT_COLUMN_REACTANT2 = 0;

	/** Internally used in multi-thread environment as result to express that we ran out of rows. */
	protected static final List<DataRow> EMPTY_RESULT_DUE_TO_LACK_OF_ROWS =
			Collections.unmodifiableList(new ArrayList<DataRow>(0));

	/**
	 * Constant used to express that there is no reaction result to be processed because the reaction is not part of
	 * randomized reaction set.
	 */
	protected static final List<DataRow> NOT_INCLUDED =
			Collections.unmodifiableList(new ArrayList<DataRow>(0));

	//
	// Members
	//

	/** Settings model for the column name of reactant 1. */
	private final SettingsModelString m_modelReactant1ColumnName =
			registerSettings(RDKitTwoComponentReactionNodeDialog.createReactant1ColumnNameModel(), "reactant1_column", "firstColumn");
	// Accept also deprecated keys

	/** Settings model for the column name of reactant 2. */
	private final SettingsModelString m_modelReactant2ColumnName =
			registerSettings(RDKitTwoComponentReactionNodeDialog.createReactant2ColumnNameModel(), "reactant2_column", "secondColumn");
	// Accept also deprecated keys

	/** Settings model for the option of performing matrix reaction calculations. */
	private final SettingsModelBoolean m_modelDoMatrixExpansion =
			registerSettings(RDKitTwoComponentReactionNodeDialog.createDoMatrixExpansionModel());

	//
	// Constructor
	//

	/**
	 * Create new node model with three in- and one out-port.
	 */
	RDKitTwoComponentReactionNodeModel() {
		super(new PortType[] {
				BufferedDataTable.TYPE,
				BufferedDataTable.TYPE,
				new PortType(BufferedDataTable.class, true)},
				new PortType[]{BufferedDataTable.TYPE},
				2);
	}

	//
	// Protected Methods
	//

	/**
	 * {@inheritDoc}
	 * This implementation returns 2.
	 * 
	 * @return Always 2.
	 */
	@Override
	protected int getNumberOfReactants() {
		return 2;
	}

	@Override
	protected boolean isExpandReactantsMatrix() {
		return m_modelDoMatrixExpansion.getBooleanValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		// Reset warnings, check RDKit library readiness and check common reaction settings
		super.configure(inSpecs);

		// For molecule input column of reactant 1
		// Auto guess the mol input column of reactant 1 if not set - fails if no compatible column found
		SettingsUtils.autoGuessColumn(inSpecs[0], m_modelReactant1ColumnName, RDKitMolValue.class, 0,
				"Auto guessing: Using column %COLUMN_NAME%.",
				"No RDKit Mol compatible column for reactant 1 in input table 1. Use \"RDKit from Molecule\" " +
						"node to convert SMARTS.", getWarningConsolidator());

		// Determines, if the mol input column of reactant 1 exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[0], m_modelReactant1ColumnName, RDKitMolValue.class,
				"RDKit Mol input column for reactant 1 has not been specified yet.",
				"RDKit Mol input column for reactant 1 ('%COLUMN_NAME%') does not exist. Has the input table changed?");

		// For molecule input column of reactant 2
		// Auto guess the mol input column of reactant 2 if not set - fails if no compatible column found
		SettingsUtils.autoGuessColumn(inSpecs[1], m_modelReactant2ColumnName, RDKitMolValue.class,
				(inSpecs[0] == inSpecs[1] ? 1 : 0), // If 1st and 2nd table equal, auto guess with second match
				"Auto guessing: Using column %COLUMN_NAME%.",
				"No RDKit Mol compatible column for reactant 2 in input table 2. Use \"RDKit from Molecule\" " +
						"node to convert SMARTS.", getWarningConsolidator());

		// Determines, if the mol input column of reactant 2 exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[1], m_modelReactant2ColumnName, RDKitMolValue.class,
				"RDKit Mol input column for reactant 2 has not been specified yet.",
				"RDKit Mol input column for reactant 2 ('%COLUMN_NAME%') does not exist. Has the input table changed?");

		// Consolidate all warnings and make them available to the user
		generateWarnings();

		// Generate output specs
		return getOutputTableSpecs(inSpecs);
	}

	/**
	 * This implementation generates input data info object for the input mol reactant columns
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
			arrDataInfo[INPUT_COLUMN_REACTANT1] = new InputDataInfo(inSpec, m_modelReactant1ColumnName,
					InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
					RDKitMolValue.class);
		}

		// Specify input of table 2
		else if (inPort == 1) {
			arrDataInfo = new InputDataInfo[1]; // We have only one input column
			arrDataInfo[INPUT_COLUMN_REACTANT2] = new InputDataInfo(inSpec, m_modelReactant2ColumnName,
					InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
					RDKitMolValue.class);
		}

		// Specify input of table 3 (Optional reaction table)
		else {
			arrDataInfo = super.createInputDataInfos(inPort, inSpec);
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
	@Override
	protected DataTableSpec getOutputTableSpec(final int outPort,
			final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		DataTableSpec spec = null;
		List<DataColumnSpec> listSpecs;

		switch (outPort) {
		case 0:
			// Define output table
			listSpecs = new ArrayList<DataColumnSpec>();
			listSpecs.add(new DataColumnSpecCreator("Product", RDKitMolCellFactory.TYPE).createSpec());
			listSpecs.add(new DataColumnSpecCreator("Product Index", IntCell.TYPE).createSpec());
			listSpecs.add(new DataColumnSpecCreator("Reactant 1 sequence index", IntCell.TYPE).createSpec());
			listSpecs.add(new DataColumnSpecCreator("Reactant 1", RDKitMolCellFactory.TYPE).createSpec());
			listSpecs.add(new DataColumnSpecCreator("Reactant 2 sequence index", IntCell.TYPE).createSpec());
			listSpecs.add(new DataColumnSpecCreator("Reactant 2", RDKitMolCellFactory.TYPE).createSpec());

			spec = new DataTableSpec("Output", listSpecs.toArray(new DataColumnSpec[listSpecs.size()]));
			break;
		}

		return spec;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] processing(final BufferedDataTable[] inData,
			final InputDataInfo[][] arrInputDataInfo, final ExecutionContext exec) throws Exception {
		final DataTableSpec[] arrOutSpecs = getOutputTableSpecs(inData);

		// Contains the rows with the result columns
		final BufferedDataContainer tableProducts = exec.createDataContainer(arrOutSpecs[0]);

		final int iTotalRowCountReactant1 = inData[0].getRowCount();
		final int iTotalRowCountReactant2 = inData[1].getRowCount();

		if (iTotalRowCountReactant1 == 0) {
			getWarningConsolidator().saveWarning("Input table 1 is empty - there are no reactants to process.");
		}
		else if (iTotalRowCountReactant2 == 0) {
			getWarningConsolidator().saveWarning("Input table 2 is empty - there are no reactants to process.");
		}
		else {
			// Get settings and define data specific behavior
			final boolean bMatrixExpansion = m_modelDoMatrixExpansion.getBooleanValue();
			final int iMaxParallelWorkers = (int)Math.ceil(1.5 * Runtime.getRuntime().availableProcessors());
			final int iQueueSize = 10 * iMaxParallelWorkers;
			final AtomicInteger aiReactionCounter = new AtomicInteger();
			final AtomicBoolean abEarlyDone = new AtomicBoolean(false);

			// Create the chemical reaction to be applied as safe guarded resource to avoid corruption
			// by multiple thread processing
			final SafeGuardedResource<ChemicalReaction> chemicalReaction =
					markForCleanup(createSafeGuardedReactionResource(inData, arrInputDataInfo));

			// Create iterator resources over table with the second reactants
			final SafeGuardedResource<RandomAccessRowIterator> rowAccessReactant2 =
					markForCleanup(new SafeGuardedResource<RandomAccessRowIterator>() {

						/**
						 * {@inheritDoc}
						 * Creates a new instance of a random row iterator that iterates over the table
						 * that contains the seconds reactants.
						 */
						@Override
						protected RandomAccessRowIterator createResource() {
							return new RandomAccessRowIterator(inData[1]);
						}

						/**
						 * {@inheritDoc}
						 * Calls close() on the iterator to free table resources.
						 */
						@Override
						protected void disposeResource(final RandomAccessRowIterator res) {
							if (res != null) {
								res.close();
							}
						}
					});

			// Calculate two component reactions
			final MultiThreadWorker<DataRow, List<DataRow>> multiWorker =
					new MultiThreadWorker<DataRow, List<DataRow>>(iQueueSize, iMaxParallelWorkers) {

				/**
				 * Computes the two component reactions.
				 * 
				 * @param row Input row with reactant 1.
				 * @param index Index of row with reactant 1.
				 * 
				 * @return Result of none (null), one or multiple data rows. If the result
				 * 		is equal to EMPTY_RESULT_DUE_TO_LACK_OF_ROWS processing will
				 * 		be stopped (successfully).  If the result is null, we had
				 * 		a missing reactant 1.
				 */
				@Override
				protected List<DataRow> compute(final DataRow row, final long index) throws Exception {
					final int uniqueWaveId = createUniqueCleanupWaveId();
					List<DataRow> listNewRows = null;

					boolean bFoundIncluded = false;
					ROMol mol1 = null;

					try {
						final RandomAccessRowIterator rowAccess = rowAccessReactant2.get();

						if (rowAccess != null) {
							// Iterate through all second reactant rows for each first reactant
							if (bMatrixExpansion) {
								rowAccess.resetIterator();
								final int subUniqueWaveId = createUniqueCleanupWaveId();
								int index2 = 0;

								try {
									while (rowAccess.hasNext()) {
										if (isReactionIncluded(index, index2)) {
											bFoundIncluded = true;
											if (mol1 == null) {
												mol1 = markForCleanup(arrInputDataInfo[0][INPUT_COLUMN_REACTANT1].getROMol(row), uniqueWaveId);
												if (mol1 == null) {
													// Nothing to calculate, if cell is empty
													break;
												}
											}
											listNewRows = processWithSecondReactant(mol1, rowAccess.next(),
													listNewRows, subUniqueWaveId, (int)index, index2);
										}
										else {
											rowAccess.skip(1);
										}
										index2++;
									}
								}
								finally {
									cleanupMarkedObjects(subUniqueWaveId);
								}
							}

							// Or: Just take the second reactant with the same row index
							else {
								if (isReactionIncluded(index, index)) {
									bFoundIncluded = true;
									if (mol1 == null) {
										mol1 = markForCleanup(arrInputDataInfo[0][INPUT_COLUMN_REACTANT1].getROMol(row), uniqueWaveId);
										if (mol1 != null) {
											final DataRow row2 = rowAccess.get((int)index);

											if (row2 != null) {
												listNewRows = processWithSecondReactant(mol1, row2,
														null, uniqueWaveId, (int)index, (int)index);
											}
											else {
												// Using this as result will cause an CancellationException to be thrown
												// See process() method
												listNewRows = EMPTY_RESULT_DUE_TO_LACK_OF_ROWS; // No more rows found
											}
										}
									}
								}
							}
						}
					}
					finally {
						cleanupMarkedObjects(uniqueWaveId);
					}

					// If we there was no reaction to be included we use a special return value
					if (listNewRows == null && !bFoundIncluded) {
						listNewRows = NOT_INCLUDED;
					}

					return listNewRows;
				}

				/**
				 * Prepares the processing with two reactants and calls processReactionResults-
				 * 
				 * @param mol1 Molecule of reactant 1. Must not be null.
				 * @param row2 Data row with reactant 2. Must not be null.
				 * @param listToAddTo A list to add new results to. Can be null.
				 * @param wave Unique wave id for RDKit object cleanup.
				 * @param indicesReactants List of indices for reaction 1 and 2.
				 * 
				 * @return Results of reaction as data rows.
				 * 
				 * @throws EmptyCellException Needed to be declared, but will never
				 * 		be thrown because the EmptyCellPolicy is set to TreatAsNull
				 * 		for retrieval of reactant 2 data.
				 */
				private List<DataRow> processWithSecondReactant(final ROMol mol1, final DataRow row2,
						final List<DataRow> listToAddTo, final int wave, final int... indicesReactants)
								throws EmptyCellException {
					List<DataRow> listNewRows = listToAddTo;

					// Empty cells will result in null items
					final ROMol mol2 = markForCleanup(
							arrInputDataInfo[1][INPUT_COLUMN_REACTANT2].getROMol(row2), wave);

					if (mol2 != null) {
						// The reaction takes a vector of reactants. For this
						// single-component reaction that vector is one long
						final ROMol_Vect rs = new ROMol_Vect(2);
						rs.set(0, mol1);
						rs.set(1, mol2);

						// Process reaction and create rows
						listNewRows = processReactionResults(chemicalReaction.get(), rs, listToAddTo,
								wave, indicesReactants);

					}

					return listNewRows;
				}

				/**
				 * Adds the results to the table.
				 * 
				 * @param task Processing result for a row.
				 */
				@Override
				protected void processFinished(final ComputationTask task)
						throws ExecutionException, CancellationException, InterruptedException {

					final List<DataRow> listResults = task.get();

					// Only consider valid results (which still could be empty)
					if (listResults != null) {

						// Check, if the end of processable rows has been reached (in non-matrix mode)
						if (listResults == EMPTY_RESULT_DUE_TO_LACK_OF_ROWS) {
							abEarlyDone.set(true);
							cancel(false);
						}

						// Process normal results
						else {
							if (!listResults.isEmpty()) {
								for (final DataRow row : listResults) {
									tableProducts.addRowToTable(row);
								}
								aiReactionCounter.addAndGet(listResults.size());
							}
						}
					}
					else {
						getWarningConsolidator().saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
								"Encountered empty molecule cell, which will be ignored.");
					}

					// Check, if user pressed cancel (however, we will finish the method nevertheless)
					// Update the progress only every 20 rows
					if (task.getIndex() % 20 == 0) {
						try {
							AbstractRDKitNodeModel.reportProgress(exec, (int)task.getIndex(),
									iTotalRowCountReactant1, task.getInput(),
									new StringBuilder(" to calculate ").append(aiReactionCounter.get())
									.append(" reactions [").append(getActiveCount()).append(" active, ")
									.append(getFinishedTaskCount()).append(" pending]").toString());
						}
						catch (final CanceledExecutionException e) {
							cancel(true);
						}
					}
				};
			};

			try {
				multiWorker.run(inData[0]);
			}
			catch (final Exception exc) {
				// Ignore cancellations or early aborts due to too few rows
				if (exc instanceof CancellationException == false || abEarlyDone.get() == false) {
					throw exc;
				}
			}

			// Check size discrepancy
			if (bMatrixExpansion == false) {
				if (iTotalRowCountReactant1 < iTotalRowCountReactant2) {
					getWarningConsolidator().saveWarning("Number of first reactants is lower than number of second reactants.");
				}
				else if (iTotalRowCountReactant1 > iTotalRowCountReactant2) {
					getWarningConsolidator().saveWarning("Number of second reactants is lower than number of first reactants.");
				}
			}

			// Check, if user cancelled
			if (abEarlyDone.get() == false) {
				exec.checkCanceled();
			}
		}

		exec.setProgress(1.0, "Finished Processing");

		tableProducts.close();

		return new BufferedDataTable[] { tableProducts.getTable() };
	}
}