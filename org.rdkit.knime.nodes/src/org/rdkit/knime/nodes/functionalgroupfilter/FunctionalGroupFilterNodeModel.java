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
package org.rdkit.knime.nodes.functionalgroupfilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;
import org.rdkit.knime.nodes.functionalgroupfilter.SettingsModelFunctionalGroupConditions.FunctionalGroupCondition;
import org.rdkit.knime.nodes.functionalgroupfilter.SettingsModelFunctionalGroupConditions.Qualifier;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.FileUtils;
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
 */
public class FunctionalGroupFilterNodeModel extends AbstractRDKitNodeModel {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(FunctionalGroupFilterNodeModel.class);

	public static final String DEFAULT_DEFINITION_ID = "Default Definitions";

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

	/** Settings model for the column name of the input column. */
	private final SettingsModelString m_modelInputColumnName =
			registerSettings(FunctionalGroupFilterNodeDialog.createInputColumnNameModel(), "input_column", "colName");
	// Accept also deprecated keys

	/** Settings model for the file name of a custom functional group definition file. */
	private final SettingsModelString m_modelInputFile =
			registerSettings(FunctionalGroupFilterNodeDialog.createInputFileModel(), "filename", "fileUrl");
	// Accept also deprecated keys

	/** Settings model for the definition of functional group filter conditions. */
	private final SettingsModelFunctionalGroupConditions m_modelFunctionGroupConditions =
			registerSettings(FunctionalGroupFilterNodeDialog.createFunctionalGroupConditionsModel(true), "conditions", "properties");
	// Accept also deprecated keys

	/**
	 * Settings model for the option to determine, if the pattern that failed to
	 * match is getting recorded in a new column.
	 */
	private final SettingsModelBoolean m_modelRecordFailedPatternOption =
			registerSettings(FunctionalGroupFilterNodeDialog.createRecordFailedPatternOptionModel());

	/** Settings model for the column name of the new column to be added to the output table. */
	private final SettingsModelString m_modelNewFailedPatternColumnName =
			registerSettings(FunctionalGroupFilterNodeDialog.
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
	 * It contains only row keys of non matching rows and as value it records here the
	 * first non matching pattern. We will synchronize on that object to ensure that
	 * only one thread is accessing at a time.
	 */
	private final Map<RowKey, String> m_mapNonMatches = new HashMap<RowKey, String>(50);

	//
	// Constructor
	//

	/**
	 * Create new node model with one data in- and two out-ports.
	 */
	FunctionalGroupFilterNodeModel() {
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

		// Check if the functional group definitions can be read and update the conditions
		if (m_modelInputFile.getStringValue() == null || m_modelInputFile.getStringValue().isEmpty()) {
			m_modelInputFile.setStringValue(DEFAULT_DEFINITION_ID);
		}
		final FunctionalGroupDefinitions definitions = createDefinitionsFromFile(m_modelInputFile);
		m_modelFunctionGroupConditions.updateConditions(definitions);
		getWarningConsolidator().saveWarnings(definitions.getWarningConsolidator());

		final FunctionalGroupCondition[] arrConditions = m_modelFunctionGroupConditions.getConditions();
		if (arrConditions == null || arrConditions.length == 0) {
			getWarningConsolidator().saveWarning("No filter conditions found.");
		}
		else {
			boolean bFoundActiveCondition = false;

			for (int i = 0; i < arrConditions.length; i++) {
				if (arrConditions[i].isActive()) {
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
		SettingsUtils.autoGuessColumnName(inSpecs[0], null, null,
				m_modelNewFailedPatternColumnName, "First Non-Matching Pattern");

		// If the option to record the first failed pattern is selected, check
		// configuration of the new column name
		if (m_modelRecordFailedPatternOption.getBooleanValue()) {
			// Determine, if the new column name has been set and if it is really unique
			SettingsUtils.checkColumnNameUniqueness(inSpecs[0], null, null,
					m_modelNewFailedPatternColumnName,
					"Failed pattern column has not been specified yet.",
					"The name %COLUMN_NAME% of the new column exists already in the input.");
		}

		// Consolidate all warnings and make them available to the user
		final Map<String, Long> mapContextOccurrences = new HashMap<String, Long>();
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
			spec = inSpecs[0];
			break;
		case 1:
			if (m_modelRecordFailedPatternOption.getBooleanValue()) {
				spec = new DataTableSpec("Failed molecules",
						inSpecs[0], new DataTableSpec(new DataColumnSpecCreator(
								m_modelNewFailedPatternColumnName.getStringValue(),
								StringCell.TYPE).createSpec()));
			}
			else {
				spec = inSpecs[0];
			}
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
		final DataColumnSpec[] arrOutputSpec = new DataColumnSpec[0]; // We have no output columns

		final boolean bAdd = m_modelRecordFailedPatternOption.getBooleanValue();
		final int iCount = m_arrActivatedConditions.length;
		final DataCell[] arrNoCells = new DataCell[0];

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
									strNonMatchPattern = new StringBuffer(m_definitions.get(
											m_arrActivatedConditions[i].getName()).getDisplayLabel())
									.append(' ').append(qualifier.toString())
									.append(' ').append(iCondCount).toString();
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
	 * {@link #postProcessing(BufferedDataTable[], BufferedDataTable[], ExecutionContext)} in the model.
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
		m_definitions = createDefinitionsFromFile(m_modelInputFile);
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
						};
					});
			reportProgress(exec, i, iCount, null, "Evaluate activated functional groups");
		}

		// Does not do anything by default
		exec.setProgress(1.0d);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] processing(final BufferedDataTable[] inData, final InputDataInfo[][] arrInputDataInfo,
			final ExecutionContext exec) throws Exception {
		BufferedDataTable[] arrResultTables = null;

		final DataTableSpec[] arrOutSpecs = getOutputTableSpecs(inData);
		final DataCell missingCell = DataType.getMissingCell();
		final DataCell errorCell = new StringCell("Processing Error");
		final boolean bAdd = m_modelRecordFailedPatternOption.getBooleanValue();

		if (m_arrActivatedConditions == null || m_arrActivatedConditions.length == 0) {
			getWarningConsolidator().saveWarning(
					"No active filter conditions found. No filter was applied on molecules.");
			// Keep input table for port 0 and create an empty table for port 1
			final BufferedDataContainer emptyTable = exec.createDataContainer(arrOutSpecs[1]);
			emptyTable.close();
			arrResultTables = new BufferedDataTable[] { inData[0], emptyTable.getTable() };
		}
		else {
			// Contains the rows with the matching molecules
			final BufferedDataContainer tableMatch = exec.createDataContainer(arrOutSpecs[0]);

			// Contains the rows with non-matching molecules
			final BufferedDataContainer tableNoMatch = exec.createDataContainer(arrOutSpecs[1]);

			// Reset the "communication medium" for multi-threading
			m_mapNonMatches.clear();

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
					String strNonMatchingPattern = null;

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
						else if (strNonMatchingPattern == ERROR) {
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
				new AbstractRDKitNodeModel.ParallelProcessor(factory, resultProcessor, inData[0].size(),
						getWarningConsolidator(), exec).run(inData[0]);
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

	/**
	 * {@inheritDoc}
	 */
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
	 * Determines where to read the functional group definition file from and
	 * opens up an input stream to it.
	 *
	 * @return Functional Group Definitions object.
	 *
	 * @throws InvalidSettingsException Thrown, if settings are incorrect or the file
	 * 		could not be found (also an incorrect setting).
	 */
	public static InputStream getDefinitionFileInputStream(
			final SettingsModelString modelInputFile) throws InvalidSettingsException {
		InputStream in = null;
		String strErrorMsgStart = null;

		// Check first, if there is a custom file name specified
		if (FunctionalGroupFilterNodeModel.hasCustomDefinitionFile(modelInputFile)) {
			final File file = FileUtils.convertToFile(modelInputFile.getStringValue(), true, false);
			strErrorMsgStart = "The custom definition file '" +
					file.getAbsolutePath() + "' ";
			try {
				in = new FileInputStream(file);
			}
			catch (final FileNotFoundException exc) {
				throw new InvalidSettingsException(strErrorMsgStart + "could not be found.", exc);
			}
		}

		// Or use the default definition file
		else {
			strErrorMsgStart = "The default definition file resource '" +
					DEFAULT_DEFINITION_FILE + "' ";
			in = FunctionalGroupDefinitions.class.getClassLoader().getResourceAsStream(DEFAULT_DEFINITION_FILE);
			if (in == null) {
				throw new InvalidSettingsException(strErrorMsgStart + "could not be found.");
			}
		}

		return in;
	}

	/**
	 * Reads the functional group definitions based on the passed in settings, either
	 * from a custom file or from the default definition file.
	 *
	 * @return Functional Group Definitions object.
	 *
	 * @throws InvalidSettingsException Thrown, if settings are incorrect or the file
	 * 		could not be found (also an incorrect setting). Also thrown, if reading
	 * 		of the file fails, although it is there. In this case the file could be corrupted.
	 */
	public static FunctionalGroupDefinitions createDefinitionsFromFile(
			final SettingsModelString modelInputFile) throws InvalidSettingsException {
		// Load the functional group definitions file
		final InputStream in = getDefinitionFileInputStream(modelInputFile);

		// Load the file and return the result
		FunctionalGroupDefinitions definitions = null;
		try {
			definitions = new FunctionalGroupDefinitions(in); // Closes the input stream
		}
		catch (final IOException exc) {
			throw new InvalidSettingsException(
					"The functional group definitions could not be read successfully.", exc);
		}

		return definitions;
	}

	/**
	 * Determines, if the the specified model (of a functional group definition
	 * file) defines custom definition file name or not. It returns true, if
	 * there is a value unequal to null and unequal to an empty string (trimmed before).
	 * It does not check, if the file name is valid and if the file actually exists.
	 *
	 * @param modelFile Settings model for the custom definition file name. Can be null.
	 *
	 * @return True, if there is a custom file specified. False otherwise.
	 */
	public static boolean hasCustomDefinitionFile(final SettingsModelString modelFile) {
		boolean bRet = false;

		if (modelFile != null) {
			String strFile = modelFile.getStringValue();
			if (strFile != null) {
				strFile = strFile.trim();
				bRet = !FunctionalGroupFilterNodeModel.DEFAULT_DEFINITION_ID.
						equals(strFile);
			}
		}

		return bRet;
	}
}
