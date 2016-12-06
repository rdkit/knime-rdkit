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
package org.rdkit.knime.wizards.samples.calculatorsplitter2;

import java.util.ArrayList;
import java.util.List;

import org.RDKit.Int_Int_Vect_List_Map;
import org.RDKit.Int_Vect;
import org.RDKit.Int_Vect_List;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.AbstractRDKitCalculatorNodeModel;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.types.RDKitAdapterCell;
import org.rdkit.knime.types.RDKitMolCellFactory;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.wizards.samples.calculator.RDKitMurckoScaffoldNodeModel;

/**
 * This class implements the node model of the "RDKitMolFragmenter" node
 * providing fragment calculations for RDKit Molecules based on
 * the open source RDKit library.
 *
 * @author Greg Landrum
 * @author Manuel Schwarze
 */
public class RDKitMolFragmenterNodeModel extends AbstractRDKitCalculatorNodeModel {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitMurckoScaffoldNodeModel.class);

	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;

	//
	// Members
	//

	/** Settings model for the column name of the input column. */
	private final SettingsModelString m_modelInputColumnName =
			registerSettings(RDKitMolFragmenterNodeDialog.createInputColumnNameModel(), "input_column", "first_column");
	// Accepts also old deprecated key

	/** Settings model for the minimal path length. */
	private final SettingsModelIntegerBounded m_modelMinPath =
			registerSettings(RDKitMolFragmenterNodeDialog.createMinPathModel());

	/** Settings model for the maximal path length. */
	private final SettingsModelIntegerBounded m_modelMaxPath =
			registerSettings(RDKitMolFragmenterNodeDialog.createMaxPathModel());

	//
	// Internals
	//

	/** Stores the indices of the fragments that have been found so far. */
	private final List<Int_Vect> m_listFragsSeen = new ArrayList<Int_Vect>();

	/** Stores the molecules of the fragments that have been found so far. */
	private final List<ROMol> m_listFragsMol = new ArrayList<ROMol>();

	/** Stores the smiles of the fragments that have been found so far. */
	private final List<String> m_listSmiles = new ArrayList<String>();

	/** Stores the occurrence count of a fragment that has been found. */
	private final List<Integer> m_listFragCounts = new ArrayList<Integer>();

	//
	// Constructors
	//

	/**
	 * Create new node model with one data in- and two out-ports.
	 */
	RDKitMolFragmenterNodeModel() {
		super(1, 2);
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
				"No RDKit Mol, SMILES or SDF compatible column in input table. Use the \"Molecule to RDKit\" " +
						"node to convert SMARTS.", getWarningConsolidator());

		// Determines, if the input column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[0], m_modelInputColumnName, RDKitMolValue.class,
				"Input column has not been specified yet.",
				"Input column %COLUMN_NAME% does not exist. Has the input table changed?");

		// Check constraint between min and max path length
		if(m_modelMinPath.getIntValue() > m_modelMaxPath.getIntValue() ){
			throw new InvalidSettingsException("Minimum path length is larger than maximum path length.");
		}

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
	protected InputDataInfo[] createInputDataInfos(final int inPort, final DataTableSpec inSpec)
			throws InvalidSettingsException {

		InputDataInfo[] arrDataInfo = null;

		// Specify input of table 1
		if (inPort == 0) {
			arrDataInfo = new InputDataInfo[1]; // We have only one input column
			arrDataInfo[INPUT_COLUMN_MOL] = new InputDataInfo(inSpec, m_modelInputColumnName,
					InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
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
	 * 
	 * @see #createOutputFactories(int)
	 */
	@Override
	protected DataTableSpec getOutputTableSpec(final int outPort,
			final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		DataTableSpec spec = null;
		List<DataColumnSpec> listSpecs;

		switch (outPort) {

		// Fragment Table
		case 0:
			// Define output table
			listSpecs = new ArrayList<DataColumnSpec>();
			listSpecs.add(new DataColumnSpecCreator("Fragment Index", IntCell.TYPE).createSpec());
			listSpecs.add(new DataColumnSpecCreator("Fragment", RDKitAdapterCell.RAW_TYPE).createSpec());
			listSpecs.add(new DataColumnSpecCreator("Fragment SMILES", StringCell.TYPE).createSpec());
			listSpecs.add(new DataColumnSpecCreator("Fragment Size", IntCell.TYPE).createSpec());
			listSpecs.add(new DataColumnSpecCreator("Count", IntCell.TYPE).createSpec());

			spec = new DataTableSpec("output 1", listSpecs.toArray(new DataColumnSpec[listSpecs.size()]));
			break;

			// Mol Table
		case 1:
			// Define output table through rearranger and factory
			final ColumnRearranger rearranger = createColumnRearranger(outPort, inSpecs[0]);
			spec = new DataTableSpec("output 2", rearranger.createSpec(), new DataTableSpec());
			break;
		}

		return spec;
	}

	/**
	 * {@inheritDoc}
	 * This implementation delivers an output factory that is responsible for building
	 * the second table (port 1).
	 */
	@Override
	protected AbstractRDKitCellFactory[] createOutputFactories(final int outPort, final DataTableSpec inSpec)
			throws InvalidSettingsException {
		AbstractRDKitCellFactory factory = null;

		switch (outPort) {
		case 1:
			// Generate column specs for the output table columns produced by this factory
			final DataColumnSpec[] arrOutputSpec = new DataColumnSpec[1]; // We have only one output column
			arrOutputSpec[0] = new DataColumnSpecCreator("Fragment indices", ListCell.getCollectionType(IntCell.TYPE)).createSpec();

			// Get settings
			final int iMinPathLength = m_modelMinPath.getIntValue();
			final int iMaxPathLength = m_modelMaxPath.getIntValue();

			// Generate factory
			factory = new AbstractRDKitCellFactory(this,
					AbstractRDKitCellFactory.RowFailurePolicy.DeliverEmptyValues,
					getWarningConsolidator(), null, arrOutputSpec) {

				/**
				 * {@inheritDoc}
				 * This method implements the calculation logic to generate the new cells based on
				 * the input made available in the first (and second) parameter.
				 */
				@Override
				public DataCell[] process(final InputDataInfo[] arrInputDataInfo, final DataRow row, final long lUniqueWaveId) throws Exception {
					final ROMol mol = markForCleanup(arrInputDataInfo[INPUT_COLUMN_MOL].getROMol(row), lUniqueWaveId);
					final List<IntCell> fragsHere = new ArrayList<IntCell>();

					// Only for non-missing cells we do the calculations
					if (mol != null) {

						// Decompose the molecule
						final Int_Int_Vect_List_Map pathMap = RDKFuncs.findAllSubgraphsOfLengthsMtoN(
								mol, iMinPathLength, iMaxPathLength);

						for (int length = iMinPathLength; length <= iMaxPathLength; length++) {

							final Int_Vect_List paths = pathMap.get(length);
							final List<Int_Vect> lCache = new ArrayList<Int_Vect>();

							for (int i = 0; i < paths.size(); ++i) {
								final Int_Vect ats = paths.get(i);
								int idx;

								// If we've seen this fragment in this molecule already,
								// go ahead and punt on it
								final Int_Vect discrims = RDKFuncs.calcPathDiscriminators(mol, ats);
								idx = -1;
								// .indexOf() doesn't use the correct .equals() method
								// with my SWIG classes, so we have to hack this:
								for (int iidx = 0; iidx < lCache.size(); iidx++) {
									if (lCache.get(iidx).equals(discrims)) {
										idx = iidx;
										break;
									}
								}

								if (idx >= 0) {
									continue;
								}
								else {
									lCache.add(discrims);
								}

								idx = -1;

								// Check, if we have seen this fragment already
								for (int iidx = 0; iidx < m_listFragsSeen.size(); iidx++) {
									if (m_listFragsSeen.get(iidx).equals(discrims)) {
										idx = iidx;
										break;
									}
								}

								// Fragment found - just increase the counter
								if (idx >= 0) {
									m_listFragCounts.set(idx,
											(m_listFragCounts.get(idx)) + 1);
								}
								// Fragment encountered the first time - analyze more details
								else {
									idx = m_listFragsSeen.size();
									m_listFragsSeen.add(discrims);
									// Don't use the uniqueWaveId for cleanup as we need this mol until the very end of processing
									final ROMol frag = markForCleanup(RDKFuncs.pathToSubmol(mol, ats));
									m_listFragsMol.add(frag);
									final String smiles = RDKFuncs.MolToSmiles(frag);
									m_listSmiles.add(smiles);
									m_listFragCounts.add(1);
								}

								fragsHere.add(new IntCell(idx + 1));
							}
						}
					}

					return new DataCell[] { CollectionCellFactory.createListCell(fragsHere) };
				}
			};

			// Enable or disable this factory to allow parallel processing
			factory.setAllowParallelProcessing(false); // Strictly forbidden for this algorithm

			break;
		}

		return factory != null ? new AbstractRDKitCellFactory[] { factory } : null;
	}

	/**
	 * {@inheritDoc}
	 * This implementation lets the factory find fragments within the molecules and makes
	 * intermediate results available in member variables, which will be further processed
	 * in the {@link #postProcessing(BufferedDataTable[], InputDataInfo[][], BufferedDataTable[], ExecutionContext)}
	 * method. Also, it generates the second output table.
	 */
	@Override
	protected BufferedDataTable[] processing(final BufferedDataTable[] inData,
			final InputDataInfo[][] arrInputDataInfo, final ExecutionContext exec)
					throws Exception {
		// Clean all old intermediate results in case there are still any
		m_listFragsSeen.clear();
		m_listFragsMol.clear();
		m_listSmiles.clear();
		m_listFragCounts.clear();

		// Setup the factory and the rearranger to do the work
		final ColumnRearranger rearranger = createColumnRearranger(1, inData[0].getDataTableSpec());

		// Creates the mol table and generates intermediate results
		return new BufferedDataTable[] { null, exec.createColumnRearrangeTable(inData[0], rearranger, exec) };
	}

	/**
	 * {@inheritDoc}
	 * This implementation creates based on the intermediate results from the core processing
	 * the main fragment output table.
	 */
	@Override
	protected BufferedDataTable[] postProcessing(final BufferedDataTable[] inData,
			final InputDataInfo[][] arrInputDataInfo,
			final BufferedDataTable[] processingResult, final ExecutionContext exec)
					throws Exception {

		BufferedDataTable tableResult = null;

		try {
			final BufferedDataContainer newTableData = exec
					.createDataContainer(getOutputTableSpec(0,
							getInputTableSpecs(inData)));

			final int iTotalCount = m_listFragsSeen.size();
			final int iColumnNumber = newTableData.getTableSpec().getNumColumns();

			// For each found fragment add a row
			for (int i = 0; i < iTotalCount; i++) {
				final ROMol molFragment = m_listFragsMol.get(i);

				// Create the row
				final DataCell[] cells = new DataCell[iColumnNumber];
				cells[0] = new IntCell(i + 1);
				cells[1] = RDKitMolCellFactory.createRDKitAdapterCell(molFragment, m_listSmiles.get(i));
				cells[2] = new StringCell(m_listSmiles.get(i));
				cells[3] = new IntCell((int)molFragment.getNumBonds());
				cells[4] = new IntCell(m_listFragCounts.get(i));
				final DataRow row = new DefaultRow("frag_" + (i + 1), cells);
				newTableData.addRowToTable(row);

				// Report progress and check for cancellation
				exec.setProgress((double)i / iTotalCount, "Added fragment row " + i + "/" + iTotalCount
						+ " (\"" + row.getKey() + "\")");
				exec.checkCanceled();
			}

			newTableData.close();
			tableResult = newTableData.getTable();
		}
		finally {
			// Clean all old intermediate results to free memory
			m_listFragsSeen.clear();
			m_listFragsMol.clear();
			m_listSmiles.clear();
			m_listFragCounts.clear();
		}

		return new BufferedDataTable[] { tableResult, processingResult[1] };
	}

	/**
	 * {@inheritDoc}
	 * Returns 0.05 (5%).
	 */
	@Override
	protected double getPostProcessingPercentage() {
		return 0.05d;
	}
}
