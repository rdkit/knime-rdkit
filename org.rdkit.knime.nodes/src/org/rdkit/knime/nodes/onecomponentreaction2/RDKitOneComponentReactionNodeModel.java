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
package org.rdkit.knime.nodes.onecomponentreaction2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

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
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortType;
import org.knime.core.util.MultiThreadWorker;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;
import org.rdkit.knime.types.RDKitMolCellFactory;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SafeGuardedResource;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class implements the node model of the RDKitOneComponentReaction node
 * providing calculations based on the open source RDKit library.
 * 
 * @author Greg Landrum
 * @author Manuel Schwarze
 */
public class RDKitOneComponentReactionNodeModel extends AbstractRDKitReactionNodeModel<RDKitOneComponentReactionNodeDialog> {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitOneComponentReactionNodeModel.class);

	/** Input data info index for Mol value of the reactant. */
	protected static final int INPUT_COLUMN_REACTANT = 0;

	/** Input data info index for Reaction value. */
	protected static final int INPUT_COLUMN_REACTION = 0;

	//
	// Members
	//

	/** Settings model for the column name of the input column. */
	private final SettingsModelString m_modelInputColumnName =
			registerSettings(RDKitOneComponentReactionNodeDialog.createReactantColumnNameModel(), "input_column", "firstColumn");
	// Accept also deprecated keys

	//
	// Constructor
	//

	/**
	 * Create new node model with two in- and one out-port.
	 */
	RDKitOneComponentReactionNodeModel() {
		super(new PortType[] { BufferedDataTable.TYPE, new PortType(BufferedDataTable.class, true)},
				new PortType[] { BufferedDataTable.TYPE },
				1);
	}

	//
	// Protected Methods
	//

	/**
	 * {@inheritDoc}
	 * This implementation returns 1.
	 * 
	 * @return Always 1.
	 */
	@Override
	protected int getNumberOfReactants() {
		return 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		// Reset warnings, check RDKit library readiness and check common reaction settings
		super.configure(inSpecs);

		// For molecule input column
		// Auto guess the mol input column if not set - fails if no compatible column found
		SettingsUtils.autoGuessColumn(inSpecs[0], m_modelInputColumnName, RDKitMolValue.class, 0,
				"Auto guessing: Using column %COLUMN_NAME%.",
				"No RDKit Mol, SMILES or SDF compatible column in input table. Use the \"Molecule to RDKit\" " +
						"node to convert SMARTS.", getWarningConsolidator());

		// Determines, if the mol input column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[0], m_modelInputColumnName, RDKitMolValue.class,
				"RDKit Mol input column has not been specified yet.",
				"RDKit Mol input column %COLUMN_NAME% does not exist. Has the input table changed?");

		// Consolidate all warnings and make them available to the user
		generateWarnings();

		// Generate output specs
		return getOutputTableSpecs(inSpecs);
	}

	/**
	 * This implementation generates input data info object for the input mol column
	 * and connects it with the information coming from the appropriate setting model.
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected InputDataInfo[] createInputDataInfos(final int inPort, final DataTableSpec inSpec)
			throws InvalidSettingsException {

		InputDataInfo[] arrDataInfo = null;

		// Specify input of table 1
		if (inPort == 0) {
			arrDataInfo = new InputDataInfo[1]; // We have only one input column
			arrDataInfo[INPUT_COLUMN_REACTANT] = new InputDataInfo(inSpec, m_modelInputColumnName,
					InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
					RDKitMolValue.class);
		}

		// Specify input of table 2 (Optional reaction table)
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
		resetProductCounter();

		final DataTableSpec[] arrOutSpecs = getOutputTableSpecs(inData);

		// Contains the rows with the result columns
		final BufferedDataContainer tableProducts = exec.createDataContainer(arrOutSpecs[0]);

		final int iTotalRowCount = inData[0].getRowCount();

		if (iTotalRowCount == 0) {
			getWarningConsolidator().saveWarning("Input table 1 is empty - there are no reactants to process.");
		}
		else {
			// Get settings and define data specific behavior
			final int iMaxParallelWorkers = (int)Math.ceil(1.5 * Runtime.getRuntime().availableProcessors());
			final int iQueueSize = 10 * iMaxParallelWorkers;

			// Create the chemical reaction to be applied as safe guarded resource to avoid corruption
			// by multiple thread processing
			final SafeGuardedResource<ChemicalReaction> chemicalReaction =
					createSafeGuardedReactionResource(inData, arrInputDataInfo);

			// Calculate one component reactions
			new MultiThreadWorker<DataRow, DataRow[]>(iQueueSize, iMaxParallelWorkers) {

				/**
				 * Computes the one component reactions.
				 * 
				 * @param row Input row.
				 * @param index Index of row.
				 */
				@Override
				protected DataRow[] compute(final DataRow row, final long index) throws Exception {
					final int uniqueWaveId = createUniqueCleanupWaveId();
					List<DataRow> listNewRows = null;

					// Empty cells will result in null items
					final ROMol mol = markForCleanup(arrInputDataInfo[0][INPUT_COLUMN_REACTANT].getROMol(row), uniqueWaveId);

					try {
						if (mol != null) {
							// The reaction takes a vector of reactants. For this
							// single-component reaction that vector is one long
							final ROMol_Vect rs = new ROMol_Vect(1);
							rs.set(0, mol);

							// Process reaction and create rows
							listNewRows = processReactionResults(chemicalReaction.get(), rs, null,
									uniqueWaveId, (int)index);
						}
					}
					finally {
						cleanupMarkedObjects(uniqueWaveId);
					}

					return (listNewRows == null ? null : listNewRows.toArray(new DataRow[listNewRows.size()]));
				}

				/**
				 * Adds the results to the table.
				 * 
				 * @param task Processing result for a row.
				 */
				@Override
				protected void processFinished(final ComputationTask task)
						throws ExecutionException, CancellationException, InterruptedException {

					final DataRow[] arrResult = task.get();

					// Only consider valid results (which still could be empty)
					if (arrResult != null) {
						for (final DataRow row : arrResult) {
							tableProducts.addRowToTable(row);
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
									iTotalRowCount, task.getInput(),
									new StringBuilder(" to calculate reactions [").append(getActiveCount()).append(" active, ")
									.append(getFinishedTaskCount()).append(" pending]").toString());
						}
						catch (final CanceledExecutionException e) {
							cancel(true);
						}
					}
				};
			}.run(inData[0]);
		}

		exec.checkCanceled();
		exec.setProgress(1.0, "Finished Processing");

		tableProducts.close();

		return new BufferedDataTable[] { tableProducts.getTable() };
	}

}
