/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2012-2023
 * Novartis Pharma AG, Switzerland
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
package org.rdkit.knime.nodes.functionalgroupfilter;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.RDKit.Match_Vect_Vect;
import org.RDKit.ROMol;
import org.RDKit.RWMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;
import org.rdkit.knime.nodes.functionalgroupfilter.SettingsModelFunctionalGroupConditions.FunctionalGroupCondition;
import org.rdkit.knime.nodes.functionalgroupfilter.SettingsModelFunctionalGroupConditions.Qualifier;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.FileSystemsUtils;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SafeGuardedResource;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class implements the node model of the RDKitFunctionalGroupFilter node
 * providing a row filter based on the open source RDKit library.
 *
 * @author Dillip K Mohanty
 * @author Manuel Schwarze
 * @author Roman Balabanov
 */
public class FunctionalGroupFilterV2NodeModel extends AbstractRDKitNodeModel {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(FunctionalGroupFilterV2NodeModel.class);

	/** The resource name of the default definition file. */
	public static final String DEFAULT_DEFINITION_FILE =
			"/org/rdkit/knime/nodes/functionalgroupfilter/Functional_Group_Hierarchy.txt";

	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;

	/** An often used empty string. */
	private static final String MISSING_INPUT = "";

	/** An often used special string used as failed pattern, if we don't record it. */
	private static final String IGNORE_FAILED_PATTERN = "-";

	/** An often used special string used to indicate a processing error for a row. */
	private static final String ERROR = "e";

	//
	// Members
	//

	/** The input molecules port index */
	private final int m_iInputMoleculesPortIdx;

	/** The passed molecules output table port index */
	private final int m_iPassedMoleculesPortIdx;

	/** The failed molecules output table port index */
	private final int m_iFailedMoleculesPortIdx;

	/** Settings model for the column name of the input column. */
	private final SettingsModelString m_modelInputColumnName =
			registerSettings(FunctionalGroupFilterV2NodeDialog.createInputColumnNameModel(), "input_column", "colName");
	// Accept also deprecated keys

	/** Settings model for the file path of a custom functional group definition file. */
	private final SettingsModelReaderFileChooser m_modelInputPath;
	// Accept also deprecated keys

	/** Settings model for the definition of functional group filter conditions. */
	private final SettingsModelFunctionalGroupConditions m_modelFunctionGroupConditions =
			registerSettings(FunctionalGroupFilterV2NodeDialog.createFunctionalGroupConditionsModel(true), "conditions", "properties");
	// Accept also deprecated keys

	/**
	 * Settings model for the option to determine, if the pattern that failed to
	 * match is getting recorded in a new column.
	 */
	private final SettingsModelBoolean m_modelRecordFailedPatternOption =
			registerSettings(FunctionalGroupFilterV2NodeDialog.createRecordFailedPatternOptionModel());

	/** Settings model for the column name of the new column to be added to the output table. */
	private final SettingsModelString m_modelNewFailedPatternColumnName =
			registerSettings(FunctionalGroupFilterV2NodeDialog.
					createNewFailedPatternColumnNameModel(m_modelRecordFailedPatternOption), true);

	//
	// Internals
	//

	/**
	 * Intermediate pre-processing result, which will be used in processing phase.
	 * The representation of the functional group definitions file content.
	 */
	private FunctionalGroupDefinitions m_definitions;

	/**
	 * Intermediate pre-processing result, which will be used in processing phase.
	 * It contains all activated functional group conditions. The indices
	 * of this array match with {@link #m_arrMolSmarts}.
	 */
	private FunctionalGroupCondition[] m_arrActivatedConditions;

	/**
	 * Intermediate pre-processing result, which will be used in processing phase.
	 * It contains all generated ROMol patterns as SafeGuarded resources. The indices
	 * of this array match with {@link #m_arrActivatedConditions}.
	 */
	private SafeGuardedResource<ROMol>[] m_arrMolSmarts;

	/**
	 * This map is used for communication between parallel execution threads that determine, if
	 * a molecule fulfills the matching criteria, and the code that performs the splitting.
	 * It contains only row keys of non-matching rows and as value it records here the
	 * first non-matching pattern. We will synchronize on that object to ensure that
	 * only one thread is accessing at a time.
	 */
	private final Map<RowKey, String> m_mapNonMatches = new HashMap<>(50);

	//
	// Constructor
	//

	/**
	 * Constructs new {@code FunctionalGroupFilterV2NodeModel} instance with configuration specified.
	 *
	 * @param nodeCreationConfig Node Creation Configuration instance.
	 *                           Mustn't be null.
	 */
	FunctionalGroupFilterV2NodeModel(NodeCreationConfiguration nodeCreationConfig) {
		super(nodeCreationConfig);

		m_iInputMoleculesPortIdx = getInputTablePortIndexes(nodeCreationConfig,
				FunctionalGroupFilterV2NodeFactory.INPUT_PORT_GRP_ID_MOLECULES)[0];
		m_iPassedMoleculesPortIdx = getOutputTablePortIndexes(nodeCreationConfig,
				FunctionalGroupFilterV2NodeFactory.OUTPUT_PORT_GRP_ID_MOLECULES_PASSED)[0];
		m_iFailedMoleculesPortIdx = getOutputTablePortIndexes(nodeCreationConfig,
				FunctionalGroupFilterV2NodeFactory.OUTPUT_PORT_GRP_ID_MOLECULES_FAILED)[0];

		m_modelInputPath = registerSettings(FunctionalGroupFilterV2NodeDialog.createInputPathModel(nodeCreationConfig));
	}

	//
	// Protected Methods
	//

	/**
	 * KNIME File Handling API messages handler.
	 *
	 * @param statusMessage A message received from KNIME File Handling API.
	 *                      Can be null.
	 */
	protected void onStatusMessage(StatusMessage statusMessage) {
        if (statusMessage != null && statusMessage.getMessage() != null) {
            switch (statusMessage.getType()) {
                case ERROR -> getWarningConsolidator().saveWarning(statusMessage.getMessage());
                case WARNING -> LOGGER.warn(statusMessage.getMessage());
                case INFO -> LOGGER.info(statusMessage.getMessage());
            }
        }
    }

	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
			throws InvalidSettingsException
	{
		try {
			m_modelInputPath.configureInModel(inSpecs, this::onStatusMessage);
		}
		catch (InvalidSettingsException e) {
			// ignoring it here
		}

		return super.configure(inSpecs);
	}

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		// Reset warnings and check RDKit library readiness
		super.configure(inSpecs);

		// Auto guess the input column if not set - fails if no compatible column found
		SettingsUtils.autoGuessColumn(inSpecs[m_iInputMoleculesPortIdx], m_modelInputColumnName, RDKitMolValue.class, 0,
				"Auto guessing: Using column %COLUMN_NAME%.",
				"No RDKit Mol, SMILES or SDF compatible column in input table. Use the \"RDKit from Molecule\" " +
						"node to convert SMARTS.", getWarningConsolidator());

		// Determines, if the input column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[m_iInputMoleculesPortIdx], m_modelInputColumnName, RDKitMolValue.class,
				"Input column has not been specified yet.",
				"Input column %COLUMN_NAME% does not exist. Has the input table changed?");

		// Check if the functional group definitions can be read and update the conditions
		final FunctionalGroupDefinitions definitions = createDefinitionsFromFile(m_modelInputPath);
		m_modelFunctionGroupConditions.updateConditions(definitions);
		getWarningConsolidator().saveWarnings(definitions.getWarningConsolidator());

		final FunctionalGroupCondition[] arrConditions = m_modelFunctionGroupConditions.getConditions();
		if (arrConditions == null || arrConditions.length == 0) {
			getWarningConsolidator().saveWarning("No filter conditions found.");
		}
		else {
			boolean bFoundActiveCondition = false;

            for (FunctionalGroupCondition arrCondition : arrConditions) {
                if (arrCondition.isActive()) {
                    bFoundActiveCondition = true;
                    break;
                }
            }

			if (!bFoundActiveCondition) {
				getWarningConsolidator().saveWarning("No functional groups selected. No filter will be applied on molecules.");
			}
		}

		// Auto guess the new column name and make it unique
		// We do this even if the option to record this is currently disabled
		SettingsUtils.autoGuessColumnName(inSpecs[m_iInputMoleculesPortIdx], null, null,
				m_modelNewFailedPatternColumnName, "First Non-Matching Pattern");

		// If the option to record the first failed pattern is selected, check
		// configuration of the new column name
		if (m_modelRecordFailedPatternOption.getBooleanValue()) {
			// Determine, if the new column name has been set and if it is unique
			SettingsUtils.checkColumnNameUniqueness(inSpecs[m_iInputMoleculesPortIdx], null, null,
					m_modelNewFailedPatternColumnName,
					"Failed pattern column has not been specified yet.",
					"The name %COLUMN_NAME% of the new column exists already in the input.");
		}

		// Consolidate all warnings and make them available to the user
		final Map<String, Long> mapContextOccurrences = new HashMap<>();
		mapContextOccurrences.put(FunctionalGroupDefinitions.LINE_CONTEXT.getId(),
				(long)definitions.getReadFunctionalGroupLines());
		generateWarnings(mapContextOccurrences);

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
		if (inPort == m_iInputMoleculesPortIdx) {
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
	 * @see #createOutputFactory(InputDataInfo[])
	 */
	@Override
	protected DataTableSpec getOutputTableSpec(final int outPort,
			final DataTableSpec[] inSpecs) throws InvalidSettingsException
	{
        if (outPort == m_iPassedMoleculesPortIdx) {
            return inSpecs[m_iInputMoleculesPortIdx];
        }
		else if (outPort == m_iFailedMoleculesPortIdx) {
            if (m_modelRecordFailedPatternOption.getBooleanValue()) {
                return new DataTableSpec("Failed molecules",
                        inSpecs[m_iInputMoleculesPortIdx], new DataTableSpec(new DataColumnSpecCreator(
                        m_modelNewFailedPatternColumnName.getStringValue(),
                        StringCell.TYPE).createSpec()));
            }
			else {
                return inSpecs[m_iInputMoleculesPortIdx];
            }
        }
        return null;
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
		final DataColumnSpec[] arrOutputSpec = new DataColumnSpec[0]; // We have no output columns

		final boolean bAdd = m_modelRecordFailedPatternOption.getBooleanValue();
		final int iCount = m_arrActivatedConditions.length;
		final DataCell[] arrNoCells = new DataCell[0];

		// Generate factory
		final AbstractRDKitCellFactory factory = new AbstractRDKitCellFactory(this,
				AbstractRDKitCellFactory.RowFailurePolicy.DeliverEmptyValues,
				getWarningConsolidator(), arrInputDataInfos, arrOutputSpec) {

			/**
			 * This method implements the calculation logic to generate the new cells based on
			 * the input made available in the first (and second) parameter.
			 * {@inheritDoc}
			 */
			@Override
			public DataCell[] process(final InputDataInfo[] arrInputDataInfo, final DataRow row, final long lUniqueWaveId) throws Exception {
				String strNonMatchPattern = null;
				final ROMol mol = markForCleanup(arrInputDataInfo[INPUT_COLUMN_MOL].getROMol(row), lUniqueWaveId);

				if (mol != null) {
					for (int i = 0; i < iCount; i++) {
						try {
							// Find substructure matches
							final Match_Vect_Vect mvvMatches =
									markForCleanup(mol.getSubstructMatches(m_arrMolSmarts[i].get()), lUniqueWaveId);
							int iFoundMatches = 0;
							if (mvvMatches != null) {
								iFoundMatches = (int)mvvMatches.size();
							}

							// Check, if condition is NOT met
							final Qualifier qualifier = m_arrActivatedConditions[i].getQualifier();
							final int iCondCount = m_arrActivatedConditions[i].getCount();
							if (!qualifier.test(iFoundMatches, iCondCount)) {
								// Record first non-matching pattern
								if (bAdd) {
									strNonMatchPattern = m_definitions.get(m_arrActivatedConditions[i].getName()).getDisplayLabel() +
											' ' + qualifier +
											' ' + iCondCount;
								}
								// Record just a dummy to save time
								else {
									strNonMatchPattern = IGNORE_FAILED_PATTERN;
								}
								// Do not check the rest as we have failed anyway
								break;
							}
						}
						catch (final Exception exc) {
							LOGGER.debug("Failed to check condition " + m_arrActivatedConditions[i].toString(), exc);
							strNonMatchPattern = ERROR;
							break;
						}
					}
				}
				else {
					strNonMatchPattern = MISSING_INPUT;
				}

				if (strNonMatchPattern != null) {
					synchronized (m_mapNonMatches) {
						m_mapNonMatches.put(row.getKey(), strNonMatchPattern);
					}
				}

				return arrNoCells;
			}
		};

		// Enable or disable this factory to allow parallel processing
		factory.setAllowParallelProcessing(true);

		return factory;
	}

	/**
	 * Returns the percentage of pre-processing activities from the total execution.
	 *
	 * @return Percentage of pre-processing. Returns 0.2d.
	 */
	@Override
	protected double getPreProcessingPercentage() {
		return 0.02d;
	}

	/**
	 * This method gets called from the method {@link #execute(BufferedDataTable[], ExecutionContext)}, before
	 * the row-by-row processing starts. All necessary pre-calculations can be done here. Results of the method
	 * should be made available through member variables, which get picked up by the other methods like
	 * process(InputDataInfo[], DataRow) in the factory or
	 * {@link #postProcessing(BufferedDataTable[], InputDataInfo[][], BufferedDataTable[], ExecutionContext)} in the model.
	 *
	 * @param inData The input tables of the node.
	 * @param arrInputDataInfo Information about all columns of the input tables.
	 * @param exec The execution context, which was derived as sub-execution context based on the percentage
	 * 		setting of #getPreProcessingPercentage(). Track the progress from 0..1.
	 *
	 * @throws Exception Thrown, if pre-processing fails.
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected void preProcessing(final BufferedDataTable[] inData, final InputDataInfo[][] arrInputDataInfo,
			final ExecutionContext exec) throws Exception {
		// Load the file and update the condition list
		m_definitions = createDefinitionsFromFile(m_modelInputPath);
		m_modelFunctionGroupConditions.updateConditions(m_definitions);

		// Create RDKit Molecules for all SMARTS for every single activated condition
		m_arrActivatedConditions = m_modelFunctionGroupConditions.getActivatedConditions();
		final int iCount = m_arrActivatedConditions.length;
		m_arrMolSmarts = new SafeGuardedResource[iCount];

		for (int i = 0; i < iCount; i++) {
			final String strSmartsPattern =
					m_definitions.get(m_arrActivatedConditions[i].getName()).getSmarts();
			m_arrMolSmarts[i] = markForCleanup(
					new SafeGuardedResource<ROMol>(!strSmartsPattern.contains("$")) {
						@Override
						protected ROMol createResource() {
							return markForCleanup(RWMol.MolFromSmarts(strSmartsPattern, 0, true));
						}
                    });
			reportProgress(exec, i, iCount, null, "Evaluate activated functional groups");
		}

		// Does not do anything by default
		exec.setProgress(1.0d);
	}

	@Override
	protected BufferedDataTable[] processing(final BufferedDataTable[] inData, final InputDataInfo[][] arrInputDataInfo,
			final ExecutionContext exec) throws Exception {
		final BufferedDataTable[] arrResultTables;

		final DataTableSpec[] arrOutSpecs = getOutputTableSpecs(inData);
		final DataCell missingCell = DataType.getMissingCell();
		final DataCell errorCell = new StringCell("Processing Error");
		final boolean bAdd = m_modelRecordFailedPatternOption.getBooleanValue();

		if (m_arrActivatedConditions == null || m_arrActivatedConditions.length == 0) {
			getWarningConsolidator().saveWarning(
					"No active filter conditions found. No filter was applied on molecules.");
			// Keep input table for port 0 and create an empty table for port 1
			final BufferedDataContainer emptyTable = exec.createDataContainer(arrOutSpecs[m_iFailedMoleculesPortIdx]);
			emptyTable.close();
			arrResultTables = new BufferedDataTable[] { inData[m_iInputMoleculesPortIdx], emptyTable.getTable() };
		}
		else {
			// Contains the rows with the matching molecules
			final BufferedDataContainer tableMatch = exec.createDataContainer(arrOutSpecs[m_iPassedMoleculesPortIdx]);

			// Contains the rows with non-matching molecules
			final BufferedDataContainer tableNoMatch = exec.createDataContainer(arrOutSpecs[m_iFailedMoleculesPortIdx]);

			// Reset the "communication medium" for multi-threading
			m_mapNonMatches.clear();

			// Setup main factory
			final AbstractRDKitCellFactory factory = createOutputFactory(arrInputDataInfo[m_iInputMoleculesPortIdx]);
			final AbstractRDKitNodeModel.ResultProcessor resultProcessor =
					new AbstractRDKitNodeModel.ResultProcessor() {

				/**
				 * {@inheritDoc}
				 * This implementation determines, if the cell 0 in the results is missing.
				 * If it is missing and the setting tells to split the tables,
				 * then the original input row is added to table 1. Otherwise, the input row
				 * gets merged with the cell 0 and is added to table 0.
				 */
				@Override
				public void processResults(final long rowIndex, final DataRow row, final DataCell[] arrResults) {
					final String strNonMatchingPattern;

					synchronized (m_mapNonMatches) {
						strNonMatchingPattern = m_mapNonMatches.remove(row.getKey());
					}

					// Add the row to the matching table
					if (strNonMatchingPattern == null) {
						tableMatch.addRowToTable(row);
					}
					else {
						// We really care about references in the following comparisons
						DataCell newCell = null;

						// Check, if we had an empty input cell
						if (MISSING_INPUT.equals(strNonMatchingPattern)) {
							getWarningConsolidator().saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
									"Encountered an empty input cell, which will be counted as no match.");
							newCell = missingCell;
						}

						// Check, if a processing error occurred
						else if (ERROR.equals(strNonMatchingPattern)) {
							getWarningConsolidator().saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
									"Encountered an input molecule, which caused an error.");
							newCell = errorCell;
						}

						// Everything is fine, just record the non-matching pattern
						else if (bAdd) {
							newCell = new StringCell(strNonMatchingPattern);
						}

						// Add the row to the non-matching table
						if (bAdd) {
							tableNoMatch.addRowToTable(AbstractRDKitCellFactory.mergeDataCells(row,
									new DataCell[] { newCell }, -1));
						}
						else {
							tableNoMatch.addRowToTable(row);
						}
					}
				}
			};

			// Runs the multiple threads to do the work
			try {
				new AbstractRDKitNodeModel.ParallelProcessor(factory, resultProcessor, inData[m_iInputMoleculesPortIdx].size(),
						getWarningConsolidator(), exec).run(inData[m_iInputMoleculesPortIdx]);
			}
			catch (final Exception e) {
				exec.checkCanceled();
				throw e;
			}

			tableMatch.close();
			tableNoMatch.close();

			arrResultTables = new BufferedDataTable[] { tableMatch.getTable(), tableNoMatch.getTable() };
		}
		return arrResultTables;
	}

	@Override
	protected void cleanupIntermediateResults() {
		if (m_mapNonMatches != null) {
			m_mapNonMatches.clear();
		}
		m_arrActivatedConditions = null;
		m_arrMolSmarts = null;
		m_definitions = null;
	}

	//
	// Static Public Methods
	//

	/**
	 * Determines where to read the functional group definition file from, reads it using a reader function provided
	 * and returns its output as the result.
	 *
	 * @param modelInputPath {@code SettingsModelReaderFileChooser} instance representing source file path.
	 *                       Can be null.
	 * @param funcReader     {@code ReaderFunction} implementation to used to read/convert file data to resulting representation desired.
	 *                       Mustn't be null unless {@code model} is null or represents default Functional Groups file path.
	 * @param logger         {@code NodeLogger} instance to be used among IO operations.
	 *                       Mustn't be null unless {@code model} is null or represents default Functional Groups file path.
	 * @param <T>            Type of the resulting instance. Inflicted by {@code funcReader} parameter.
	 * @return Functional Group Definitions object. The type is inflicted by {@code funcReader} parameter.
	 * @throws InvalidSettingsException Thrown if settings are incorrect or the file
	 *                                  could not be found (also an incorrect setting).
	 * @throws IOException              Thrown if an exception occurred during file reading.
	 * @throws IllegalArgumentException Thrown if {@code function} or {@code logger} parameters required but provided as null.
	 * @see org.rdkit.knime.util.FileSystemsUtils.ReaderFunction
	 */
	static <T> T readDefinitionsFile(final SettingsModelReaderFileChooser modelInputPath,
									 final FileSystemsUtils.ReaderFunction<T> funcReader,
									 final NodeLogger logger)
			throws IOException, InvalidSettingsException
	{
		final T result;

        if (modelInputPath == null || modelInputPath.getPath().isBlank()) {
            try (final InputStream inputStream = FunctionalGroupDefinitions.class.getClassLoader().getResourceAsStream(DEFAULT_DEFINITION_FILE)) {
                result = funcReader.read(inputStream);
            }
        }
		else {
			result = FileSystemsUtils.readFile(modelInputPath, funcReader, logger);
        }

		return result;
    }

	/**
	 * Reads the functional group definitions based on the passed in settings, either
	 * from a custom file or from the default definition file.
	 *
	 * @param modelInputPath {@code SettingsModelReaderFileChooser} instance.
	 *                       Can be null.
	 * @return Functional Group Definitions object.
	 *
	 * @throws InvalidSettingsException Thrown, if settings are incorrect or the file
	 * 		could not be found (also an incorrect setting). Also thrown, if reading
	 * 		of the file fails, although it is there. In this case the file could be corrupted.
	 */
	public static FunctionalGroupDefinitions createDefinitionsFromFile(
			final SettingsModelReaderFileChooser modelInputPath) throws InvalidSettingsException
	{
		// Load the functional group definitions file and return the result
		try {
			return readDefinitionsFile(modelInputPath, FunctionalGroupDefinitions::new, LOGGER);
		}
		catch (final IOException exc) {
			throw new InvalidSettingsException(
					"The functional group definitions could not be read successfully.", exc);
		}
	}

}
