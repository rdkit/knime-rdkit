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
package org.rdkit.knime.nodes.substructfilter;

import java.util.ArrayList;

import org.RDKit.Int_Pair;
import org.RDKit.Match_Vect;
import org.RDKit.Match_Vect_Vect;
import org.RDKit.ROMol;
import org.RDKit.RWMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SafeGuardedResource;
import org.rdkit.knime.util.SettingsModelEnumeration;
import org.rdkit.knime.util.SettingsUtils;

/**
 * This class implements the node model of the RDKitSubstructureFilter node
 * providing calculations based on the open source RDKit library.
 * 
 * @author Greg Landrum
 */
public class RDKitSubstructFilterNodeModel extends AbstractRDKitNodeModel {

	//
	// Enumeration
	//

	/** Defines several modes to tell what to do with found substructure matches. */
	public enum MatchHandling {
		DoNotAddMatchColumn, AddFirstMatchColumn, AddAllFlattenedMatchColumn;

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {

			switch (this) {
			case DoNotAddMatchColumn:
				return "Do not add column with matching atoms";
			case AddFirstMatchColumn:
				return "Add column with atom list of first match";
			case AddAllFlattenedMatchColumn:
				return "Add column with atom list of all matches (overlap possible)";
			}

			return super.toString();
		}
	}

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitSubstructFilterNodeModel.class);

	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;

	protected static final DataCell DATA_CELL_MATCH_WITHOUT_DETAILS =
			CollectionCellFactory.createListCell(new ArrayList<DataCell>());

	//
	// Members
	//

	/** Settings model for the column name of the input column. */
	private final SettingsModelString m_modelInputColumnName =
			registerSettings(RDKitSubstructFilterNodeDialog.createInputColumnNameModel(), "input_column", "first_column");

	private final SettingsModelString m_modelSmartsQuery =
			registerSettings(RDKitSubstructFilterNodeDialog.createSmartsModel());

	private final SettingsModelBoolean m_modelExactMatch =
			registerSettings(RDKitSubstructFilterNodeDialog.createExactMatchModel());

	private final SettingsModelEnumeration<MatchHandling> m_modelMatchHandling =
			registerSettings(RDKitSubstructFilterNodeDialog.createMatchHandlingModel(), true);

	private final SettingsModelString m_modelNewAtomListColumnName =
			registerSettings(RDKitSubstructFilterNodeDialog.createNewMatchColumnNameModel(m_modelMatchHandling), true);

	//
	// Internals
	//

	/**
	 * The thread separated resource that wraps the SMARTS pattern (represented
	 * as RDKit Molecule object) used during execution.
	 */
	private SafeGuardedResource<ROMol> m_pattern;

	/** Defines for execution, if the logger is logging debug information. */
	private boolean m_bDebug;

	//
	// Constructor
	//

	/**
	 * Create new node model with one data in- and one out-port.
	 */
	RDKitSubstructFilterNodeModel() {
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
				"No RDKit Mol, SMILES or SDF compatible column in input table. Use the \"RDKit from Molecule\" " +
						"node to convert SMARTS.", getWarningConsolidator());

		// Determines, if the input column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[0], m_modelInputColumnName, RDKitMolValue.class,
				"Input column has not been specified yet.",
				"Input column %COLUMN_NAME% does not exist. Has the input table changed?");

		// Check, if SMARTS query has been provided
		if (m_modelSmartsQuery.getStringValue().equals("")) {
			throw new InvalidSettingsException("Please specify a SMARTS query.");
		}

		// Check validity of SMARTS
		final ROMol pattern = markForCleanup(RWMol.MolFromSmarts(m_modelSmartsQuery.getStringValue(), 0, true));
		if (pattern == null) {
			throw new InvalidSettingsException("Could not parse SMARTS query: "
					+ m_modelSmartsQuery.getStringValue());
		}
		cleanupMarkedObjects();

		// Determine, if the new column name has been set and if it is really unique
		if (m_modelNewAtomListColumnName.isEnabled()) {

			// Auto guess the new column name and make it unique
			SettingsUtils.autoGuessColumnName(inSpecs[0], null, null,
					m_modelNewAtomListColumnName, "Matching Atom List");

			SettingsUtils.checkColumnNameUniqueness(inSpecs[0], null, null,
					m_modelNewAtomListColumnName,
					"Column for the Matching Atom List has not been specified yet.",
					"The name %COLUMN_NAME% of the new column for the Atom List of Match(es) exists already in the input.");
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

		switch (outPort) {
		case 0:
			// Check existence and proper column type
			final InputDataInfo[] arrInputDataInfos = createInputDataInfos(0, inSpecs[0]);
			arrInputDataInfos[0].getColumnIndex();

			// Take input spec as spec start
			spec = inSpecs[0];

			// Append result column(s)
			if (m_modelMatchHandling.getValue() != MatchHandling.DoNotAddMatchColumn) {
				spec = new DataTableSpec(spec, new DataTableSpec(
						new DataColumnSpecCreator(m_modelNewAtomListColumnName.getStringValue(),
								ListCell.getCollectionType(IntCell.TYPE)).createSpec()));
			}
			break;

		case 1:
			// Second table has the same structure as input table
			spec = inSpecs[0];
			break;
		}

		return spec;
	}

	/**
	 * Creates an output factory to create cells based on the passed in
	 * input.
	 * 
	 * @param arrInputDataInfos Array of input data information that is relevant
	 * 		for processing.
	 * 
	 * @see #createInputDataInfos(int, DataTableSpec)
	 */
	protected AbstractRDKitCellFactory createOutputFactory(final InputDataInfo[] arrInputDataInfos)
			throws InvalidSettingsException {
		// Generate column specs for the output table columns produced by this factory
		final DataColumnSpec[] arrOutputSpec = new DataColumnSpec[1]; // We have only one output column
		arrOutputSpec[0] = new DataColumnSpecCreator(
				m_modelNewAtomListColumnName.getStringValue().trim(), ListCell.getCollectionType(IntCell.TYPE))
		.createSpec();

		// Generate factory
		final AbstractRDKitCellFactory factory = new AbstractRDKitCellFactory(this,
				AbstractRDKitCellFactory.RowFailurePolicy.DeliverEmptyValues,
				getWarningConsolidator(), arrInputDataInfos, arrOutputSpec) {

			@Override
			/**
			 * This method implements the calculation logic to generate the new cells based on
			 * the input made available in the first (and second) parameter.
			 * {@inheritDoc}
			 */
			public DataCell[] process(final InputDataInfo[] arrInputDataInfo, final DataRow row, final long lUniqueWaveId) throws Exception {
				DataCell outputCell = DataType.getMissingCell();

				// Calculate the new cells
				final ROMol mol = markForCleanup(arrInputDataInfo[INPUT_COLUMN_MOL].getROMol(row), lUniqueWaveId);

				// Get a thread-save instance of the pattern
				final ROMol molPattern = m_pattern.get();

				// A missing cell would result in mol == null
				if (mol != null && molPattern != null &&
						(!m_modelExactMatch.getBooleanValue() ||
								(mol.getNumAtoms() == molPattern.getNumAtoms() &&
								mol.getNumBonds() == molPattern.getNumBonds()))) {

					// See if there is a match
					final boolean matched = mol.hasSubstructMatch(molPattern);

					// We found a match
					if (matched) {

						final MatchHandling matchHandling = m_modelMatchHandling.getValue();

						// Just use our standard match cell without any more details
						if (matchHandling == MatchHandling.DoNotAddMatchColumn) {
							outputCell = DATA_CELL_MATCH_WITHOUT_DETAILS;
						}

						// Deliver more details about the substructure match
						else {
							try {
								final ArrayList<DataCell> listCellAtomList = new ArrayList<DataCell>();
								final Match_Vect_Vect vecVecMatches = mol.getSubstructMatches(molPattern);
								final StringBuilder sb = new StringBuilder("( ");

								if (vecVecMatches != null) {
									final int iCountMatches = (int)vecVecMatches.size();
									for (int matchIndex = 0; matchIndex < iCountMatches; matchIndex++) {

										if (m_bDebug) {
											sb.append("[");
										}

										final Match_Vect vecMatch = vecVecMatches.get(matchIndex);
										final int iCountMatchingAtoms = (int)vecMatch.size();
										for (int atomIndex = 0; atomIndex < iCountMatchingAtoms; atomIndex++) {
											final Int_Pair pair = vecMatch.get(atomIndex);
											listCellAtomList.add(new IntCell(pair.getSecond()));

											if (m_bDebug) {
												sb.append("(").append(pair.getFirst()).
												append(",").append(pair.getSecond()).append(")");
												if (atomIndex < iCountMatchingAtoms - 1) {
													sb.append(", ");
												}
											}
										}

										if (m_bDebug) {
											sb.append("]");
										}

										if (matchHandling == MatchHandling.AddFirstMatchColumn) {
											// Do not add more than the first match
											break;
										}

										if (m_bDebug && matchIndex < iCountMatches - 1) {
											sb.append(", ");
										}
									}
								}

								if (m_bDebug) {
									sb.append(" )");
									LOGGER.debug(sb.toString());
								}

								outputCell = CollectionCellFactory.createListCell(listCellAtomList);
							}
							catch (final Exception exc) {
								LOGGER.warn("Failed to get substructure match details for row " + row.getKey(), exc);
								outputCell = DATA_CELL_MATCH_WITHOUT_DETAILS;
							}
						}
					}
				}

				return new DataCell[] { outputCell };
			}
		};

		// Enable or disable this factory to allow parallel processing
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

		// Contains the rows with the result column
		final BufferedDataContainer matchTable = exec.createDataContainer(arrOutSpecs[0]);

		// Contains the input rows if result computation fails
		final BufferedDataContainer mismatchTable = exec.createDataContainer(arrOutSpecs[1]);

		// Get settings and define data specific behavior
		final long lTotalRowCount = inData[0].size();
		final MatchHandling matchHandling = m_modelMatchHandling.getValue();

		// Construct an RDKit molecule from the SMARTS pattern - make it available as member variable
		// for the cell factory
		final String strSmartsPattern = m_modelSmartsQuery.getStringValue();
		m_pattern = markForCleanup(new SafeGuardedResource<ROMol>(!strSmartsPattern.contains("$")) {
			@Override
			protected ROMol createResource() {
				return markForCleanup(RWMol.MolFromSmarts(strSmartsPattern, 0, true));
			};
		});

		m_bDebug = LOGGER.isDebugEnabled();

		// Setup main factory
		final AbstractRDKitCellFactory factory = createOutputFactory(arrInputDataInfo[0]);
		final AbstractRDKitNodeModel.ResultProcessor resultProcessor =
				new AbstractRDKitNodeModel.ResultProcessor() {

			/**
			 * {@inheritDoc}
			 * This implementation determines, if the cell 0 in the results is missing.
			 * If it is missing and the setting tells to split the tables,
			 * then the original input row is added to table 1. Otherwise the input row
			 * gets merged with the cell 0 and is added to table 0.
			 */
			@Override
			public void processResults(final long rowIndex, final DataRow row, final DataCell[] arrResults) {
				if (arrResults[0].isMissing()) {
					mismatchTable.addRowToTable(row);
				}
				else {
					if (matchHandling == MatchHandling.DoNotAddMatchColumn) {
						matchTable.addRowToTable(row);
					}
					else { // Add also details column
						matchTable.addRowToTable(AbstractRDKitCellFactory.mergeDataCells(row, arrResults, -1));
					}
				}
			}
		};

		// Runs the multiple threads to do the work
		try {
			new AbstractRDKitNodeModel.ParallelProcessor(factory, resultProcessor, lTotalRowCount,
					getWarningConsolidator(), exec).run(inData[0]);
		}
		catch (final Exception e) {
			exec.checkCanceled();
			throw e;
		}

		matchTable.close();
		mismatchTable.close();

		return new BufferedDataTable[] { matchTable.getTable(), mismatchTable.getTable() };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected synchronized void cleanupIntermediateResults() {
		m_pattern = null;
	}
}
