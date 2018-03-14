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
package org.rdkit.knime.nodes.moleculesubstructfilter;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.RDKit.ROMol;
import org.RDKit.RWMol;
import org.knime.chem.types.SmartsValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory.RowFailurePolicy;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;
import org.rdkit.knime.nodes.rdkfingerprint.DefaultFingerprintSettings;
import org.rdkit.knime.nodes.rdkfingerprint.FingerprintSettings;
import org.rdkit.knime.nodes.rdkfingerprint.FingerprintType;
import org.rdkit.knime.nodes.substructfilter.RDKitSubstructFilterNodeModel;
import org.rdkit.knime.types.RDKitAdapterCell;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.RDKitObjectCleaner;
import org.rdkit.knime.util.SettingsModelEnumeration;
import org.rdkit.knime.util.SettingsUtils;

/**
 * This class is the node model of the RDKitMoleculeSubstructureFilter and
 * formerly RDKitDictionarySubstructureFilter node
 * providing calculations based on the open source RDKit library.
 * 
 * @author Greg Landrum, Novartis
 * @author Thorsten Meinl, University of Konstanz
 * @author Sudip Ghosh, Novartis
 * @author Manuel Schwarze, Novartis
 */
public class RDKitMoleculeSubstructFilterNodeModel extends AbstractRDKitNodeModel implements RDKitObjectCleaner {

	//
	// Enumeration
	//

	/** Defines supported matching criteria. */
	public enum MatchingCriteria {
		All, Exact, AtLeast;

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {

			switch (this) {
			case All:
				return "All";
			case Exact:
				return "Exact";
			case AtLeast:
				return "At least";
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
	private static final int INPUT_COLUMN_MOL = 0;

	/** Input data info index for Fingerprint value (calculated during pre-processing). */
	private static final int INPUT_COLUMN_FP = 1;

	/** Input data info index for Query Mol value. */
	private static final int INPUT_COLUMN_QUERY = 0;

	/**
	 * Fingerprint screening threshold value to use always the current default value
	 * for number of query molecules that need to be present to enable the
	 * fingerprint screening optimization.
	 */
	protected static final int FINGERPRINT_SCREENING_USE_DEFAULT = -1;

	/**
	 * Fingerprint screening threshold value to turn this optimization off.
	 */
	protected static final int FINGERPRINT_SCREENING_OFF = 0;

	/**
	 * Default value for number of query molecules that need to be present to
	 * enable the fingerprint screening optimization.
	 */
	protected static final int DEFAULT_FINGERPRINT_SCREENING_THRESHOLD = 10;

	/** Settings used to calculate fingerprints for pre-screening. */
	protected static final FingerprintSettings FINGERPRINT_SETTING =
			new DefaultFingerprintSettings(FingerprintType.pattern.name(), -1, -1, -1, -1, -1, 1024, -1, -1, -1, false);

	//
	// Members
	//

	/** Settings model for the column name of the input column. */
	protected final SettingsModelString m_modelInputColumnName =
			registerSettings(RDKitMoleculeSubstructFilterNodeDialog.createInputColumnNameModel(), "input_column", "rdkitColumn");

	/** Settings model for the column name of the input column. */
	protected final SettingsModelString m_modelQueryColumnName =
			registerSettings(RDKitMoleculeSubstructFilterNodeDialog.createQueryColumnNameModel(), "query_column", "queryColumn");

	/** Settings model for the column name of the input column. */
	protected final SettingsModelEnumeration<MatchingCriteria> m_modelMatchingCriteria =
			registerSettings(RDKitMoleculeSubstructFilterNodeDialog.createMatchingCriteriaModel(), true);

	/** Settings model for the column name of the input column. */
	protected final SettingsModelIntegerBounded m_modelMinimumMatches =
			registerSettings(RDKitMoleculeSubstructFilterNodeDialog.createMinimumMatchesModel(m_modelMatchingCriteria));

	/** Settings model for the column name of the new column to be added to the output table. */
	protected final SettingsModelString m_modelNewColumnName =
			registerSettings(RDKitMoleculeSubstructFilterNodeDialog.createNewColumnNameModel(), true);

	/** Settings model for the fingerprint screening threshold value. */
	protected final SettingsModelIntegerBounded m_modelFingerprintScreeningThreshold =
			registerSettings(RDKitMoleculeSubstructFilterNodeDialog.createFingerprintScreeningThresholdModel(), true);

	/** Settings model for the row key match info option. */
	protected final SettingsModelBoolean m_modelRowKeyMatchInfoOption =
			registerSettings(RDKitMoleculeSubstructFilterNodeDialog.createRowKeyMatchInfoOptionModel(), true);

	//
	// Internals
	//

	/**
	 * Intermediate pre-processing result, which will be used in processing phase.
	 * It contains all row keys of patterns from the query table.
	 */
	private String[] m_arrQueryRowKeys = null;

	/**
	 * Intermediate pre-processing result, which will be used in processing phase.
	 * It contains all patterns from the query table.
	 */
	private ROMol[] m_arrQueryMols = null;

	/**
	 * Intermediate pre-processing result, which will be used in processing phase.
	 * It contains all fingerprints for the query table.
	 */
	private DenseBitVector[] m_arrQueryFingerprints = null;

	/**
	 * Intermediate pre-processing result, which will be used in processing phase.
	 * It contains all on bit counts of the calculated fingerprints for the query table.
	 */
	private long[] m_arrQueryFpOnBitCounts = null;

	/**
	 * Intermediate pre-processing result, which will be used in processing phase.
	 * This is the input molecule table with an added fingerprint column.
	 */
	private BufferedDataTable m_tableWithFingerprints = null;

	/**
	 * Intermediate pre-processing result, which will be used in processing phase.
	 * Settings model for the column name of the fingerprint column.
	 */
	protected SettingsModelString m_modelFingerprintColumnName = null;

	/**
	 * Intermediate pre-processing result, which will be used in processing phase.
	 * It contains the total number of atoms of all patterns from the query table.
	 */
	private int m_iTotalPatternAtomsCount = 0;

	/**
	 * Intermediate pre-processing result, which will be used in processing phase.
	 * It contains the total number of missing pattern cells of the query table.
	 */
	private int m_iTotalEmptyPatternCells = 0;

	/**
	 * This set is used for communication between parallel execution threads that determine, if
	 * a molecule fulfills the matching criteria, and the code that performs the splitting.
	 * We will synchronize on that object to ensure that only one thread is accessing at a time.
	 */
	private final HashSet<RowKey> m_setMatches = new HashSet<RowKey>(50);

	//
	// Constructor
	//

	/**
	 * Create new node model with two in- and two out-ports.
	 */
	protected RDKitMoleculeSubstructFilterNodeModel() {
		super(2, 2);
      registerInputTablesWithSizeLimits(1); // Query table supports only limited size
	}

	//
	// Protected Methods
	//

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected final DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		// Reset warnings and check RDKit library readiness
		super.configure(inSpecs);

		// Auto guess the input mol column if not set - fails if no compatible column found
		SettingsUtils.autoGuessColumn(inSpecs[0], m_modelInputColumnName, RDKitMolValue.class, 0,
				"Auto guessing: Using column %COLUMN_NAME% as Mol input column.",
				"No RDKit Mol, SMILES or SDF compatible column in input table. Use the \"RDKit from Molecule\" " +
						"node to convert SMARTS.", getWarningConsolidator());

		// Determines, if the mol input column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[0], m_modelInputColumnName, RDKitMolValue.class,
				"Input column has not been specified yet.",
				"Input column %COLUMN_NAME% does not exist. Has the first input table changed?");

		// Auto guess the new column name and make it unique
		SettingsUtils.autoGuessColumnName(inSpecs[0], null, null,
				m_modelNewColumnName, "Matched Substructs");

		// Determine, if the new column name has been set and if it is really unique
		SettingsUtils.checkColumnNameUniqueness(inSpecs[0], null, null,
				m_modelNewColumnName,
				"Output column has not been specified yet.",
				"The name %COLUMN_NAME% of the new column exists already in the input.");

		// Auto guess the query mol column if not set - fails if no compatible column found
		final Class<? extends DataValue>[] arrClassesQueryType = new Class[] { SmartsValue.class, RDKitMolValue.class };
		SettingsUtils.autoGuessColumn(inSpecs[1], m_modelQueryColumnName, Arrays.asList(arrClassesQueryType),
				(inSpecs[0] == inSpecs[1] ? 1 : 0), // If 1st and 2nd table equal, auto guess with second match
				"Auto guessing: Using column %COLUMN_NAME% as query molecule column.",
				"No RDKit Mol, SMILES, SDF or SMARTS compatible column in query molecule table. Use \"Molecule Type Cast\" " +
						"node to convert molecules or Strings to SMARTS values.", getWarningConsolidator());

		// Determines, if the query mol column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[1], m_modelQueryColumnName, Arrays.asList(arrClassesQueryType),
				"Query molecule column has not been specified yet.",
				"Query molecule column %COLUMN_NAME% does not exist. Has the second input table changed?");

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

		switch (inPort) {
		case 0: // First table with molecule column
			// We have only one input mol column unless we are optimizing with a fingerprint column
			// which gets created in the pre-processing phase. In this case we have 2 input columns
			arrDataInfo = new InputDataInfo[m_modelFingerprintColumnName == null ? 1 : 2];
			arrDataInfo[INPUT_COLUMN_MOL] = new InputDataInfo(inSpec, m_modelInputColumnName,
					InputDataInfo.EmptyCellPolicy.DeliverEmptyRow, null,
					RDKitMolValue.class);
			if (m_modelFingerprintColumnName != null) {
				arrDataInfo[INPUT_COLUMN_FP] = new InputDataInfo(inSpec, m_modelFingerprintColumnName,
						InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
						BitVectorValue.class);
			}
			break;

		case 1: // Second table with query molecule column
			final Class<? extends DataValue>[] arrClassesQueryType = new Class[] { SmartsValue.class, RDKitMolValue.class };

			arrDataInfo = new InputDataInfo[1]; // We have only one query molecule column
			arrDataInfo[INPUT_COLUMN_QUERY] = new InputDataInfo(inSpec, m_modelQueryColumnName,
					InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
					arrClassesQueryType);
			break;
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
		case 1:
			spec = new DataTableSpec(outPort == 0 ? "Passed molecules" : "Failed molecules",
					inSpecs[0], new DataTableSpec(new DataColumnSpecCreator(
							m_modelNewColumnName.getStringValue(),
							ListCell.getCollectionType(
									m_modelRowKeyMatchInfoOption.getBooleanValue() ?
											StringCell.TYPE : IntCell.TYPE)).createSpec()));
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
				m_modelNewColumnName.getStringValue().trim(), RDKitAdapterCell.RAW_TYPE)
		.createSpec();
		final int iTotalPatternCount = m_arrQueryMols.length - m_iTotalEmptyPatternCells;
		final int iMinimumMatches = m_modelMinimumMatches.getIntValue();
		final MatchingCriteria matchingCriteria = m_modelMatchingCriteria.getValue();
		final boolean bRowKeyMatchInfo = m_modelRowKeyMatchInfoOption.getBooleanValue();

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
				DataCell outputCell = null;
				final List<DataCell> listQueryRefs = new ArrayList<DataCell>();

				// Calculate the new cells
				ROMol mol = null;
				DenseBitVector fingerprintMol = null;
				boolean bFingerprintMolAvailable = true;
				int iNumberOfMatchingPatterns = 0;

				for (int i = 0; i < m_arrQueryMols.length; i++) {
					final ROMol molPattern = m_arrQueryMols[i];
					final String keyPattern = m_arrQueryRowKeys[i];

					if (molPattern != null ) {
						boolean bDeepCheck = true;

						// Pre-screening, if fingerprint usage is enabled
						if (m_arrQueryFingerprints != null && m_arrQueryFingerprints[i] != null && bFingerprintMolAvailable) {

							// Get the fingerprint only when executing this row the first time
							if (fingerprintMol == null) {
								fingerprintMol = arrInputDataInfo[INPUT_COLUMN_FP].getDenseBitVector(row);
								bFingerprintMolAvailable = (fingerprintMol != null);
							}

							// A potential SSS(A,B) match is found if OBC(FP(A)) <= OBC(FP(B)) AND OBC(FP(A) & FB(B)) == OBC(FP(A)),
							// where A is the query molecule and B is the molecule of the processed row
							bDeepCheck = (bFingerprintMolAvailable && m_arrQueryFpOnBitCounts[i] <= fingerprintMol.cardinality() &&
									(m_arrQueryFingerprints[i].and(fingerprintMol).cardinality() == m_arrQueryFpOnBitCounts[i]));
						}

						if (bDeepCheck) {
							// Get the molecule only if we really need it (this saves execution time)
							// Note, that this will throw an exception for empty cells, which will be handled by the factory
							if (mol == null) {
								mol = markForCleanup(arrInputDataInfo[INPUT_COLUMN_MOL].getROMol(row), lUniqueWaveId);
							}

							if (mol.hasSubstructMatch(molPattern)) {
								listQueryRefs.add(bRowKeyMatchInfo ? new StringCell(keyPattern) : new IntCell(i + 1));
								iNumberOfMatchingPatterns++;
							}
						}
					}
				}

				outputCell = CollectionCellFactory.createListCell(listQueryRefs);

				switch (matchingCriteria) {
				case All:
					if (iNumberOfMatchingPatterns == iTotalPatternCount) {
						synchronized (m_setMatches) {
							m_setMatches.add(row.getKey());
						}
					}
					break;
				case Exact:
					final long iMolAtomsCount = (mol == null ? 0 : mol.getNumAtoms());
					if (iNumberOfMatchingPatterns == iTotalPatternCount &&
							iMolAtomsCount == m_iTotalPatternAtomsCount) {
						synchronized (m_setMatches) {
							m_setMatches.add(row.getKey());
						}
					}
					break;
				case AtLeast:
					if (iNumberOfMatchingPatterns >= iMinimumMatches) {
						synchronized (m_setMatches) {
							m_setMatches.add(row.getKey());
						}
					}
					break;
				}

				return new DataCell[] { outputCell };
			}
		};

		// Enable or disable this factory to allow parallel processing
		factory.setAllowParallelProcessing(true);

		return factory;
	}

	/**
	 * Sets the intermediate results of the pre-processing phase. These values are
	 * used in the core processing phase to filter substructure matches from the input
	 * molecules.
	 * 
	 * @param arrPatterns RDKit molecules acting as substructure patterns. Some values
	 * 		could be null, if the origin was a missing cell.
	 * @param iTotalEmptyPatternCells Number of empty cells encountered when evaluating
	 * 		the query input column and preparing the patterns. This is the number of
	 * 		null values in the arrPatterns array.
	 * @param iTotalPatternAtomsCount Total number of atoms in all patterns.
	 */
	protected void setPreprocessingResults(final String[] arrRowKeys, final ROMol[] arrPatterns, final DenseBitVector[] arrFingerprints,
			final long[] arrOnBitCounts, final BufferedDataTable tableWithFingerprints,
			final int iTotalEmptyPatternCells, final int iTotalPatternAtomsCount) {
		m_arrQueryRowKeys = arrRowKeys;
		m_arrQueryMols = arrPatterns;
		m_arrQueryFingerprints = arrFingerprints;
		m_arrQueryFpOnBitCounts = arrOnBitCounts;
		m_tableWithFingerprints = tableWithFingerprints;
		if (tableWithFingerprints != null) {
			m_modelFingerprintColumnName = new SettingsModelString("fingerprint_column", tableWithFingerprints.
					getDataTableSpec().getColumnNames()[tableWithFingerprints.getDataTableSpec().getNumColumns() - 1]);
		}
		m_iTotalEmptyPatternCells = iTotalEmptyPatternCells;
		m_iTotalPatternAtomsCount = iTotalPatternAtomsCount;
	}

	/**
	 * Returns the percentage of pre-processing activities from the total execution.
	 *
	 * @return Percentage of pre-processing. Returns 0.0d.
	 */
	@Override
	protected double getPreProcessingPercentage() {
		return 0.1d;
	}

	/**
	 * This method pre-processes the patterns used for the substructure filtering.
	 * {@inheritDoc}
	 */
	@Override
	protected final void preProcessing(final BufferedDataTable[] inData, final InputDataInfo[][] arrInputDataInfo,
			final ExecutionContext exec) throws Exception {

		// Process SMARTS query values
		final boolean bIsSmarts = arrInputDataInfo[1][INPUT_COLUMN_QUERY].getDataType().isCompatible(SmartsValue.class);
		int iFingerprintThreshold = m_modelFingerprintScreeningThreshold.getIntValue();
		if (iFingerprintThreshold < 0) {
			iFingerprintThreshold = DEFAULT_FINGERPRINT_SCREENING_THRESHOLD;
		}

		int i = 0;
		final long lMolRowCount = inData[0].size();
		final int iQueryRowCount = (int)inData[1].size();
		final FingerprintType fpType = (iFingerprintThreshold != FINGERPRINT_SCREENING_OFF && iQueryRowCount >= iFingerprintThreshold ?
				FINGERPRINT_SETTING.getRdkitFingerprintType() : null);
		final String[] arrRowKeys = new String[iQueryRowCount];
		final ROMol[] arrPatterns = new ROMol[iQueryRowCount];
		final DenseBitVector[] arrFingerprints = (fpType != null ? new DenseBitVector[iQueryRowCount] : null);
		final long[] arrOnBitCounts = (fpType != null ? new long[iQueryRowCount] : null);
		int iTotalPatternAtomsCount = 0;
		int iTotalEmptyPatternCells = 0;
		ExecutionContext execQueryTable = exec;
		ExecutionContext execMolTable = null;

		// Prepare two execution contexts if we pre-process also the molecule table
		if (fpType != null) {
			execQueryTable = exec.createSubExecutionContext((double)iQueryRowCount / (double)(lMolRowCount + iQueryRowCount));
			execMolTable = exec.createSubExecutionContext((double)lMolRowCount / (double)(lMolRowCount + iQueryRowCount));
		}

		// PHASE 1: Evaluate query molecules and pre-calculate fingerprints for them
		execQueryTable.setMessage("Evaluating query molecules");

		// Get all query molecules (empty cells will result in null values according to our empty cell policy)
		for (final DataRow row : inData[1]) {
			// Process SMARTS
			if (bIsSmarts) {
				final String strSmarts = arrInputDataInfo[1][INPUT_COLUMN_QUERY].getSmarts(row);

				if (strSmarts == null) {
					iTotalEmptyPatternCells++;
				}
				else {
					arrPatterns[i] = markForCleanup(RWMol.MolFromSmarts(strSmarts, 0, true));
					if (arrPatterns[i] == null) {
						throw new ParseException("Could not parse SMARTS '"
								+ strSmarts + "' in row " + row.getKey(), 0);
					}
				}
			}
			// Process SMILES, SDF, RDKit Mol
			else {
				arrPatterns[i] = markForCleanup(arrInputDataInfo[1][INPUT_COLUMN_QUERY].getROMol(row));
				if (arrPatterns[i] == null) {
					iTotalEmptyPatternCells++;
				}
			}

			if (arrPatterns[i] != null) {
				arrRowKeys[i] = row.getKey().getString();
				iTotalPatternAtomsCount += arrPatterns[i].getNumAtoms();

				// Calculate fingerprint and on bit count for optimization
				if (fpType != null) {
					arrFingerprints[i] = createFingerprint(arrPatterns[i]);
					arrOnBitCounts[i] = (arrFingerprints[i] != null ? arrFingerprints[i].cardinality() : -1);
				}
			}

			if (i % 20 == 0) {
				reportProgress(execQueryTable, i, iQueryRowCount, row, " - Evaluating query molecules");
			}

			i++;
		}

		// Does not do anything by default
		execQueryTable.setProgress(1.0d);

		// PHASE 2: Pre-calculate fingerprints for molecule input table
		BufferedDataTable tableWithFingerprints = null;
		if (fpType != null) {
			execMolTable.setMessage("Creating fingerprints for input molecules");

			final ColumnRearranger rearranger = new ColumnRearranger(inData[0].getSpec());
			rearranger.append(new AbstractRDKitCellFactory(this, RowFailurePolicy.DeliverEmptyValues, null, arrInputDataInfo[0],
					new DataColumnSpecCreator(SettingsUtils.makeColumnNameUnique(
							arrInputDataInfo[0][INPUT_COLUMN_MOL].getColumnSpec().getName() + " - Fingerprint",
							arrInputDataInfo[0][INPUT_COLUMN_MOL].getTableSpec(), null),
							DenseBitVectorCell.TYPE).createSpec()) {

				@Override
				public DataCell[] process(final InputDataInfo[] arrInputDataInfos, final DataRow row,
						final long lUniqueWaveId) throws Exception {
					final ROMol mol = markForCleanup(arrInputDataInfos[INPUT_COLUMN_MOL].getROMol(row), lUniqueWaveId);
					return new DataCell[] { new DenseBitVectorCellFactory(createFingerprint(mol)).createDataCell() };
				}

				@Override
				public boolean allowsParallelProcessing() {
					return true;
				}

				@Override
				public void setProgress(final int curRowNr, final int rowCount, final RowKey lastKey, final ExecutionMonitor exec) {
					if (curRowNr % 20 == 0) {
						try {
							reportProgress((ExecutionContext)exec, curRowNr, rowCount, null, " - Creating fingerprints for input molecules");
						}
						catch (final CanceledExecutionException exc) {
							// Ignored here
						}
					}
				}
			});
			tableWithFingerprints = exec.createColumnRearrangeTable(inData[0], rearranger, execMolTable);
		}

		setPreprocessingResults(arrRowKeys, arrPatterns, arrFingerprints, arrOnBitCounts, tableWithFingerprints,
				iTotalEmptyPatternCells, iTotalPatternAtomsCount);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] processing(final BufferedDataTable[] inDataOrig, final InputDataInfo[][] arrInputDataInfoOrig,
			final ExecutionContext exec) throws Exception {
		final DataTableSpec[] arrOutSpecs = getOutputTableSpecs(inDataOrig);

		// Tweak input table, if we are using fingerprints => Replace original with intermediate result table
		final BufferedDataTable[] inData = inDataOrig;
		final InputDataInfo[][] arrInputDataInfo = arrInputDataInfoOrig;
		int iFingerprintColumn = -1;
		if (m_arrQueryFingerprints != null) {
			inData[0] = m_tableWithFingerprints;
			arrInputDataInfo[0] = createInputDataInfos(0, m_tableWithFingerprints.getDataTableSpec());
			iFingerprintColumn = arrInputDataInfo[0][INPUT_COLUMN_FP].getColumnIndex();
		}

		final int iFingerprintColumnToRemove = iFingerprintColumn;

		// Contains the rows with the matching molecules
		final BufferedDataContainer tableMatch = exec.createDataContainer(arrOutSpecs[0]);

		// Contains the rows with non-matching molecules
		final BufferedDataContainer tableNoMatch = exec.createDataContainer(arrOutSpecs[1]);

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
				boolean bMatching = false;

				synchronized (m_setMatches) {
					bMatching = m_setMatches.remove(row.getKey());
				}

				if (bMatching) {
					tableMatch.addRowToTable(AbstractRDKitCellFactory.mergeDataCells(row, arrResults, iFingerprintColumnToRemove));
				}
				else {
					tableNoMatch.addRowToTable(AbstractRDKitCellFactory.mergeDataCells(row, arrResults, iFingerprintColumnToRemove));
				}
			}
		};

		// Runs the multiple threads to do the work
		try {
			new AbstractRDKitNodeModel.ParallelProcessor(factory, resultProcessor, inData[0].size(),
					getWarningConsolidator(), exec).run(inData[0]);
		}
		catch (final Exception e) {
			exec.checkCanceled();
			throw e;
		}

		tableMatch.close();
		tableNoMatch.close();

		return new BufferedDataTable[] { tableMatch.getTable(), tableNoMatch.getTable() };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanupIntermediateResults() {
		m_arrQueryRowKeys = null;
		m_arrQueryMols = null;
		m_arrQueryFingerprints = null;
		m_arrQueryFpOnBitCounts = null;
		m_tableWithFingerprints = null;
		m_modelFingerprintColumnName = null;
		m_iTotalPatternAtomsCount = 0;
		m_iTotalEmptyPatternCells = 0;
		m_setMatches.clear();
	}

	/**
	 * Creates a fingerprint for the passed RDKit Mol value and returns it.
	 * 
	 * @param mol Molecule for calculation.
	 * 
	 * @return Fingerprint or null, if calculation failed.
	 */
	protected DenseBitVector createFingerprint(final ROMol mol) {
		return FINGERPRINT_SETTING.getRdkitFingerprintType().
				calculateBitBased(mol, FINGERPRINT_SETTING);
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		super.loadValidatedSettingsFrom(settings);

		// Exception: For old nodes we will treat the row key match info option as "false", for new nodes as "true"
		try {
			m_modelRowKeyMatchInfoOption.loadSettingsFrom(settings);
		}
		catch (final InvalidSettingsException excOrig) {
			m_modelRowKeyMatchInfoOption.setBooleanValue(false);
		}
	}
}
