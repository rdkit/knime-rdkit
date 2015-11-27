/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2013
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
package org.rdkit.knime.nodes.structurenormalizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.RDKit.RDKFuncs;
import org.RDKit.StringInt_Pair;
import org.knime.chem.types.SdfAdapterCell;
import org.knime.chem.types.SdfCellFactory;
import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.IntCell;
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
import org.rdkit.knime.util.FileUtils;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsModelEnumerationArray;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.StringUtils;

/**
 * This class implements the node model of the RDKitStructureNormalizer node
 * providing calculations based on the open source RDKit library.
 * 
 * @author Manuel Schwarze
 */
public class RDKitStructureNormalizerNodeModel extends AbstractRDKitNodeModel {

	//
	// Enumeration
	//

	/** Defines input types for the structure checker. */
	public enum Input { SDF, SMILES };

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitStructureNormalizerNodeModel.class);

	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;

	/** Pseudo file name to show when default configuration shall be used for Structure Normalizer. */
	protected static final String DEFAULT_CONFIGURATION_ID = "Default Configuration";

	/** The default switches to be used for StruCheck. */
	protected static final StruCheckSwitch[] DEFAULT_SWITCHES = new StruCheckSwitch[] {
		StruCheckSwitch.cc,
		StruCheckSwitch.cs,
		StruCheckSwitch.tm
	};

	/** The default advanced switches to be used for StruCheck. */
	protected static final String DEFAULT_ADVANCED_OPTIONS = "-cl 3\n-cn 999";

	/** The resource name of the default transformation configuration file. */
	protected static final String DEFAULT_TRANSFORMATION_CONFIGURATION_FILE =
			"/org/rdkit/knime/nodes/structurenormalizer/checkfgs-rdkit.trn";

	/** The resource name of the default transformation configuration file. */
	protected static final String DEFAULT_AUGMENTED_ATOMS_CONFIGURATION_FILE =
			"/org/rdkit/knime/nodes/structurenormalizer/checkfgs-rdkit.chk";

	/** The lock to ensure that only one thread works with the structure checker at a time. */
	public static final Object STRUCTURE_CHECKER_LOCK = new Object();

	/** Default postfix for output column. */
	public static final String DEFAULT_POSTFIX_PASSED_CORRECTED = "Corrected";

	/** Default postfix for output column. */
	public static final String DEFAULT_POSTFIX_PASSED_FLAGS = "Warning Flags";

	/** Default postfix for output column. */
	public static final String DEFAULT_POSTFIX_PASSED_WARNINGS = "Warnings";

	/** Default postfix for output column. */
	public static final String DEFAULT_POSTFIX_FAILED_FLAGS = "Flags";

	/** Default postfix for output column. */
	public static final String DEFAULT_POSTFIX_FAILED_ERRORS = "Errors";

	/** The required postfix for an SDF value to be processed by KNIME. */
	private static final String SDF_POSTFIX = "\n$$$$\n";

	/** Internally used factory column id for the corrected structure. */
	private static final int COL_ID_CORRECTED_STRUCTURE = 0;

	/** Internally used factory column id for the flags. */
	private static final int COL_ID_FLAGS = 1;

	/** Internally used factory column id for the warning messages. */
	private static final int COL_ID_WARNINGS = 2;

	/** Internally used factory column id for the error messages. */
	private static final int COL_ID_ERRORS = 3;

	//
	// Members
	//

	/** Settings model for the column name of the input column. */
	private final SettingsModelString m_modelInputColumnName =
			registerSettings(RDKitStructureNormalizerNodeDialog.createInputColumnNameModel());

	/** Settings model for the column name of the corrected structure column. */
	private final SettingsModelString m_modelCorrectedStructureColumnName =
			registerSettings(RDKitStructureNormalizerNodeDialog.createPassedCorrectedStructureColumnNameModel());

	/** Settings model for the column name of the passed flags column. */
	private final SettingsModelString m_modelPassedFlagsColumnName =
			registerSettings(RDKitStructureNormalizerNodeDialog.createPassedFlagsColumnNameModel());

	/** Settings model for the column name of the passed warning messages column. */
	private final SettingsModelString m_modelPassedWarningMessagesColumnName =
			registerSettings(RDKitStructureNormalizerNodeDialog.createPassedWarningMessagesColumnNameModel());

	/** Settings model for the column name of the failed flags column. */
	private final SettingsModelString m_modelFailedFlagsColumnName =
			registerSettings(RDKitStructureNormalizerNodeDialog.createFailedFlagsColumnNameModel());

	/** Settings model for the column name of the passed warning messages column. */
	private final SettingsModelString m_modelFailedErrorMessagesColumnName =
			registerSettings(RDKitStructureNormalizerNodeDialog.createFailedErrorMessagesColumnNameModel());

	/** Settings model for optional switches. */
	private final SettingsModelEnumerationArray<StruCheckSwitch> m_modelSwitchOptions =
			registerSettings(RDKitStructureNormalizerNodeDialog.createSwitchOptionsModel());

	/** Settings model for optional advanced switches. */
	private final SettingsModelString m_modelAdvancedOptions =
			registerSettings(RDKitStructureNormalizerNodeDialog.createAdvancedOptionsModel(), true); // Was added later

	/** Settings model for the transformation configuration of the structure checker. */
	private final SettingsModelString m_modelTransformationConfigurationFile =
			registerSettings(RDKitStructureNormalizerNodeDialog.createTransformationConfigurationFileModel());

	/** Settings model for the augmented atoms configuration of the structure checker. */
	private final SettingsModelString m_modelAugmentedAtomsConfigurationFile =
			registerSettings(RDKitStructureNormalizerNodeDialog.createAugmentedAtomsConfigurationFileModel());

	/** Settings model for the log file name. */
	private final SettingsModelString m_modelLogFile =
			registerSettings(RDKitStructureNormalizerNodeDialog.createLogFileModel());

	/** Settings model for the option to overwrite an existing log file. */
	private final SettingsModelBoolean m_modelOverwriteOption =
			registerSettings(RDKitStructureNormalizerNodeDialog.createOverwriteOptionModel());

	/** Settings model to define additional codes that shall be treated as failures instead of warnings. */
	private final SettingsModelEnumerationArray<StruCheckCode> m_modelAdditionalFailureCodesConfiguration =
			registerSettings(RDKitStructureNormalizerNodeDialog.createAdditionalFailureCodesConfigurationModel());

	// Intermediate results

	/** Quick access to input type during processing. */
	private Input m_inputType;

	//
	// Constructor
	//

	/**
	 * Create new node model with one data in- and one out-port.
	 */
	RDKitStructureNormalizerNodeModel() {
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
		final List<Class<? extends DataValue>> listTypes = new ArrayList<Class<? extends DataValue>>();
		listTypes.add(SdfValue.class);
		listTypes.add(SmilesValue.class);
		SettingsUtils.autoGuessColumn(inSpecs[0], m_modelInputColumnName, listTypes, 0,
				"Auto guessing: Using column %COLUMN_NAME%.",
				"No RDKit Mol, SMILES or SDF compatible column in input table.", getWarningConsolidator());

		// Determines, if the input column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[0], m_modelInputColumnName, listTypes,
				"Input column has not been specified yet.",
				"Input column %COLUMN_NAME% does not exist. Has the input table changed?");

		// Auto guess the new column names for table 1 and make them unique
		final String strInputColumnName = m_modelInputColumnName.getStringValue();
		SettingsUtils.autoGuessColumnName(inSpecs[0], null, null,
				m_modelCorrectedStructureColumnName, strInputColumnName + " - " + DEFAULT_POSTFIX_PASSED_CORRECTED);
		SettingsUtils.autoGuessColumnName(inSpecs[0],
				new String[] { m_modelCorrectedStructureColumnName.getStringValue() }, null,
				m_modelPassedFlagsColumnName, strInputColumnName + " - " + DEFAULT_POSTFIX_PASSED_FLAGS);
		SettingsUtils.autoGuessColumnName(inSpecs[0],
				new String[] { m_modelCorrectedStructureColumnName.getStringValue(),
				m_modelPassedFlagsColumnName.getStringValue() }, null,
				m_modelPassedWarningMessagesColumnName, strInputColumnName + " - " + DEFAULT_POSTFIX_PASSED_WARNINGS);

		// Auto guess the new column names for table 2 and make them unique
		SettingsUtils.autoGuessColumnName(inSpecs[0], null, null,
				m_modelFailedFlagsColumnName, strInputColumnName + " - " + DEFAULT_POSTFIX_FAILED_FLAGS);
		SettingsUtils.autoGuessColumnName(inSpecs[0],
				new String[] { m_modelFailedFlagsColumnName.getStringValue() }, null,
				m_modelFailedErrorMessagesColumnName, strInputColumnName + " - " + DEFAULT_POSTFIX_FAILED_ERRORS);

		// Determine, if the new column names have been set and if they are really unique in table 1
		SettingsUtils.checkColumnNameUniqueness(inSpecs[0], null, null,
				m_modelCorrectedStructureColumnName,
				"Corrected structure output column name (table 1) has not been specified yet.",
				"The name %COLUMN_NAME% of the new corrected structure column (table 1) exists already in the input.");
		SettingsUtils.checkColumnNameUniqueness(inSpecs[0],
				new String[] { m_modelCorrectedStructureColumnName.getStringValue() }, null,
				m_modelPassedFlagsColumnName,
				"Flags column name (table 1) has not been specified yet.",
				"The name %COLUMN_NAME% of the new flags column (table 1) exists already in the input.");
		SettingsUtils.checkColumnNameUniqueness(inSpecs[0],
				new String[] { m_modelCorrectedStructureColumnName.getStringValue(),
				m_modelPassedFlagsColumnName.getStringValue()}, null,
				m_modelPassedWarningMessagesColumnName,
				"Warnings column name (table 1) has not been specified yet.",
				"The name %COLUMN_NAME% of the new warnings column (table 1) exists already in the input.");

		// Determine, if the new column names have been set and if they are really unique in table 2
		SettingsUtils.checkColumnNameUniqueness(inSpecs[0], null, null,
				m_modelFailedFlagsColumnName,
				"Flags column name (table 2) has not been specified yet.",
				"The name %COLUMN_NAME% of the new flags column (table 2) exists already in the input.");
		SettingsUtils.checkColumnNameUniqueness(inSpecs[0],
				new String[] { m_modelFailedFlagsColumnName.getStringValue() }, null,
				m_modelFailedErrorMessagesColumnName,
				"Errors column name (table 2) has not been specified yet.",
				"The name %COLUMN_NAME% of the new errors column (table 2) exists already in the input.");

		// Check if the transformation configuration files can be read
		if (m_modelTransformationConfigurationFile.getStringValue() == null || m_modelTransformationConfigurationFile.getStringValue().isEmpty()) {
			m_modelTransformationConfigurationFile.setStringValue(DEFAULT_CONFIGURATION_ID);
		}
		final String strTrnFile = m_modelTransformationConfigurationFile.getStringValue();
		final String strTransformationConfiguration = getConfiguration(strTrnFile, DEFAULT_TRANSFORMATION_CONFIGURATION_FILE);
		if (StringUtils.isEmptyAfterTrimming(strTransformationConfiguration)) {
			throw new InvalidSettingsException("Transformation configuration defined in " +
					strTrnFile + " is empty.");
		}

		// Check if the augmented atoms configuration files can be read
		if (m_modelAugmentedAtomsConfigurationFile.getStringValue() == null || m_modelAugmentedAtomsConfigurationFile.getStringValue().isEmpty()) {
			m_modelAugmentedAtomsConfigurationFile.setStringValue(DEFAULT_CONFIGURATION_ID);
		}
		final String strChkFile = m_modelAugmentedAtomsConfigurationFile.getStringValue();
		final String strAugmentedAtomsConfiguration = getConfiguration(strChkFile, DEFAULT_AUGMENTED_ATOMS_CONFIGURATION_FILE);
		if (StringUtils.isEmptyAfterTrimming(strAugmentedAtomsConfiguration)) {
			throw new InvalidSettingsException("Augmented atoms configuration defined in " +
					strChkFile + " is empty.");
		}

		// Perform checks on the specified log output file
		final String strLogFile = m_modelLogFile.getStringValue();
		if (!StringUtils.isEmptyAfterTrimming(strLogFile)) {
			final File fileLog = FileUtils.convertToFile(strLogFile, false, true);
			if (fileLog.exists()) {
				if (m_modelOverwriteOption.getBooleanValue()) {
					getWarningConsolidator().saveWarning("The specified log file exists and will be overwritten.");
				}
				else {
					throw new InvalidSettingsException("The specified log file exists already. " +
							"You may remove the file or switch on the Overwrite option to grant execution.");
				}
			}
			else {
				final File dirLog = fileLog.getParentFile();
				if (dirLog == null) {
					throw new InvalidSettingsException("Cannot determine parent " +
							"directory of the output file.");
				}
				else if (!dirLog.exists()) {
					getWarningConsolidator().saveWarning(
							"Directory of specified output file does not exist " +
							"and will be created.");
				}
			}
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
					InputDataInfo.EmptyCellPolicy.DeliverEmptyRow, null,
					SdfValue.class, SmilesValue.class);
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
			// Check existence and proper column type
			final InputDataInfo[] arrInputDataInfos = createInputDataInfos(0, inSpecs[0]);
			arrInputDataInfos[0].getColumnIndex();

			// Copy all specs from input table
			final ArrayList<DataColumnSpec> newColSpecs = new ArrayList<DataColumnSpec>();
			for (final DataColumnSpec inCol : inSpecs[0]) {
				newColSpecs.add(inCol);
			}

			// Append result column(s)
			if (outPort == 0) {
				newColSpecs.add(new DataColumnSpecCreator(m_modelCorrectedStructureColumnName.getStringValue(), SdfAdapterCell.RAW_TYPE).createSpec());
				newColSpecs.add(new DataColumnSpecCreator(m_modelPassedFlagsColumnName.getStringValue(), IntCell.TYPE).createSpec());
				newColSpecs.add(new DataColumnSpecCreator(m_modelPassedWarningMessagesColumnName.getStringValue(),
						ListCell.getCollectionType(StringCell.TYPE)).createSpec());
			}
			else { // Port 1
				newColSpecs.add(new DataColumnSpecCreator(m_modelFailedFlagsColumnName.getStringValue(), IntCell.TYPE).createSpec());
				newColSpecs.add(new DataColumnSpecCreator(m_modelFailedErrorMessagesColumnName.getStringValue(),
						ListCell.getCollectionType(StringCell.TYPE)).createSpec());
			}

			spec = new DataTableSpec(
					newColSpecs.toArray(new DataColumnSpec[newColSpecs.size()]));
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
		final DataColumnSpec[] arrOutputSpec = new DataColumnSpec[4]; // We have four output columns, which will be reduced later to three
		arrOutputSpec[COL_ID_CORRECTED_STRUCTURE] = new DataColumnSpecCreator("Corrected Structure", SdfAdapterCell.RAW_TYPE).createSpec();
		arrOutputSpec[COL_ID_FLAGS] = new DataColumnSpecCreator("Flags", IntCell.TYPE).createSpec();
		arrOutputSpec[COL_ID_WARNINGS] = new DataColumnSpecCreator("Warning Messages",
				CollectionCellFactory.getElementType(new DataType[] { StringCell.TYPE })).createSpec();
		arrOutputSpec[COL_ID_ERRORS] = new DataColumnSpecCreator("Error Messages",
				CollectionCellFactory.getElementType(new DataType[] { StringCell.TYPE })).createSpec();

		final DataCell missingCell = DataType.getMissingCell();

		// Flag masks
		final int iErrorMask = StruCheckCode.getErrorCodeMask(m_modelAdditionalFailureCodesConfiguration.getValues());
		final int iNonErrorMask = StruCheckCode.getNonErrorCodeMask(m_modelAdditionalFailureCodesConfiguration.getValues());

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
				String strMol = null;
				String strData = null;

				switch (m_inputType) {
				case SDF:
					strMol = arrInputDataInfo[INPUT_COLUMN_MOL].getSdfValue(row);

					// Standardize line endings
					strMol = strMol.replace("\r\n", "\n");

					// Cut off properties and $$$$ and append later again to corrected structure
					final String strEndTag = "M  END\n";
					final int iIndexEnd = strMol.indexOf(strEndTag);

					if (iIndexEnd >= 0) {
						strData = strMol.substring(iIndexEnd + strEndTag.length());
						strMol = strMol.substring(0, iIndexEnd + strEndTag.length());
					}

					break;
				case SMILES:
					strMol = arrInputDataInfo[INPUT_COLUMN_MOL].getSmiles(row);
					break;
				default:
					throw new Exception("Invalid input type.");
				}

				final StringInt_Pair results = markForCleanup(
						RDKFuncs.checkMolString(strMol, m_inputType == Input.SMILES), lUniqueWaveId);

				// Read corrected structure and add back properties
				String strCorrectedStructure = results.getFirst();
				if (strCorrectedStructure != null) {
					strCorrectedStructure += strData;
				}

				// Evaluate flags
				final int iFlags = results.getSecond();
				final StruCheckCode[] arrErrorCodes = StruCheckCode.getCodes(iFlags, iErrorMask);
				final StruCheckCode[] arrWarningCodes = StruCheckCode.getCodes(iFlags, iNonErrorMask);
				final DataCell cellCorrectedStructure = createSdfCell(strCorrectedStructure);
				final DataCell cellFlags = new IntCell(iFlags);

				DataCell cellErrorMessages = missingCell;
				if (arrErrorCodes.length > 0) {
					final List<DataCell> cells = new ArrayList<DataCell>(arrErrorCodes.length);
					for (final StruCheckCode code : arrErrorCodes) {
						cells.add(new StringCell(code.getMessage()));
					}
					cellErrorMessages = CollectionCellFactory.createListCell(cells);
				}

				DataCell cellWarningMessages = missingCell;
				if (arrWarningCodes.length > 0) {
					final List<DataCell> cells = new ArrayList<DataCell>(5);
					for (final StruCheckCode code : arrWarningCodes) {
						cells.add(new StringCell(code.getMessage()));
					}
					cellWarningMessages = CollectionCellFactory.createListCell(cells);
				}

				return new DataCell[] { cellCorrectedStructure, cellFlags, cellWarningMessages, cellErrorMessages};
			}
		};

		// Enable or disable this factory to allow parallel processing
		factory.setAllowParallelProcessing(false);

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
		final BufferedDataContainer port0 = exec.createDataContainer(arrOutSpecs[0]);

		// Contains the input rows if result computation fails
		final BufferedDataContainer port1 = exec.createDataContainer(arrOutSpecs[1]);

		synchronized (STRUCTURE_CHECKER_LOCK) {

			// Setup warning/failure treatment
			final int iErrorCodeMask = StruCheckCode.getErrorCodeMask(m_modelAdditionalFailureCodesConfiguration.getValues());

			// Initialize (depends on GLOBAL_CONFIGURATION_MODE flag)
			initialize();

			// Check input type
			m_inputType = determineStructureCheckerInputType(arrInputDataInfo[0][INPUT_COLUMN_MOL].getDataType());

			// Get settings and define data specific behavior
			final long lTotalRowCount = inData[0].size();

			// Setup main factory
			final AbstractRDKitCellFactory factory = createOutputFactory(arrInputDataInfo[0]);

			// Iterate through all input rows, calculate results and split the output
			long rowIndex = 0;
			for (final CloseableRowIterator i = inData[0].iterator(); i.hasNext(); rowIndex++) {
				final DataRow row = i.next();
				final DataCell[] arrResults = factory.getCells(row);

				// Check what goes into the second table (empty cells, failures and warnings treated like failures)
				if (arrResults[1] == null || arrResults[1].isMissing() ||
						(((IntCell)arrResults[1]).getIntValue() & iErrorCodeMask) != 0) {
					port1.addRowToTable(AbstractRDKitCellFactory.mergeDataCells(row,
							new DataCell[] {
							arrResults[COL_ID_FLAGS],
							arrResults[COL_ID_ERRORS]
					}, -1));
				}
				else {
					port0.addRowToTable(AbstractRDKitCellFactory.mergeDataCells(row,
							new DataCell[] {
							arrResults[COL_ID_CORRECTED_STRUCTURE],
							arrResults[COL_ID_FLAGS],
							arrResults[COL_ID_WARNINGS]
					}, -1));
				}

				// Every 20 iterations check cancellation status and report progress
				if (rowIndex % 20 == 0) {
					AbstractRDKitNodeModel.reportProgress(exec, rowIndex, lTotalRowCount, row);
				}
			};

			exec.checkCanceled();
			exec.setProgress(1.0, "Finished Processing");

			port0.close();
			port1.close();
		}

		return new BufferedDataTable[] { port0.getTable(), port1.getTable() };
	}

	/**
	 * Initializes the structure checker. Configuration depends on the global flag
	 * {@link #GLOBAL_CONFIGURATION_MODE}. If true, it will use the default
	 * configuration for Transformation and Augmented Atoms Configuration files
	 * as well as StruChk switches. Also, it would initialize only if it was not
	 * done before. If false, it will use always the node settings and would
	 * initialize any time this method gets called.
	 * 
	 * @throws Exception Thrown, if initializing failed.
	 */
	protected void initialize() throws Exception {
		synchronized (STRUCTURE_CHECKER_LOCK) {
			FileOutputStream outTrans = null;
			FileOutputStream outAtoms = null;

			// Create temporary transformation configuration file
			final String strTransformationConfigurationOriginal =
					(isDefaultConfigurationFile(m_modelTransformationConfigurationFile.getStringValue()) ?
							DEFAULT_TRANSFORMATION_CONFIGURATION_FILE :
								m_modelTransformationConfigurationFile.getStringValue());
			String strTransformationConfiguration = getConfiguration(m_modelTransformationConfigurationFile.getStringValue(),
					DEFAULT_TRANSFORMATION_CONFIGURATION_FILE);
			strTransformationConfiguration = strTransformationConfiguration.replace("\r\n", "\n");
			final File fileTempTransformationConfigFile = File.createTempFile("checkfgs", ".trn");
			fileTempTransformationConfigFile.deleteOnExit();

			try {
				outTrans = new FileOutputStream(fileTempTransformationConfigFile, false);
				outTrans.write(strTransformationConfiguration.getBytes());
				outTrans.flush();
			}
			catch (final Exception exc) {
				throw new Exception("Unable to create temporary transformation configuration file for Structure Normalizer.", exc);
			}
			finally {
				FileUtils.close(outTrans);
			}

			// Create temporary augmented atoms configuration file
			final String strAugmentedAtomsConfigurationOriginal =
					(isDefaultConfigurationFile(m_modelAugmentedAtomsConfigurationFile.getStringValue()) ?
							DEFAULT_AUGMENTED_ATOMS_CONFIGURATION_FILE :
								m_modelAugmentedAtomsConfigurationFile.getStringValue());
			String strAugmentedAtomsConfiguration = getConfiguration(m_modelAugmentedAtomsConfigurationFile.getStringValue(),
					DEFAULT_AUGMENTED_ATOMS_CONFIGURATION_FILE);
			strAugmentedAtomsConfiguration = strAugmentedAtomsConfiguration.replace("\r\n", "\n");
			final File fileTempAugmentedAtomsConfigFile = File.createTempFile("checkfgs", ".chk");
			fileTempAugmentedAtomsConfigFile.deleteOnExit();

			try {
				outAtoms = new FileOutputStream(fileTempAugmentedAtomsConfigFile, false);
				outAtoms.write(strAugmentedAtomsConfiguration.getBytes());
				outAtoms.flush();
			}
			catch (final Exception exc) {
				throw new Exception("Unable to create temporary augmented atoms configuration file for Structure Normalizer.", exc);
			}
			finally {
				FileUtils.close(outAtoms);
			}

			// Create log file
			final String strLogFile = m_modelLogFile.getStringValue();
			File fileLog;
			if (!StringUtils.isEmptyAfterTrimming(strLogFile)) {
				fileLog = FileUtils.convertToFile(strLogFile, false, true);
			}
			else {
				fileLog = File.createTempFile("StructureNormalizer", ".log");
				fileLog.delete(); // Delete it again, it will be recreated by StruChk
			}

			// Create missing directories
			final File dirLog = fileLog.getParentFile();
			FileUtils.prepareDirectory(dirLog); // Throws an exception, if not successful

			// Initialize the structure checker
			String strAdvancedOptions = m_modelAdvancedOptions.getStringValue();
			if (strAdvancedOptions != null) {
				strAdvancedOptions = strAdvancedOptions.trim();
				if (!strAdvancedOptions.isEmpty()) {
					strAdvancedOptions += "\n";
				}
			}
			else {
				strAdvancedOptions = "";
			}

			String strOptions = "StruCheck\n" + // Add this dummy argument to work around an issue
					// in RDKit/StruCheck that throws the first parameter away
					StruCheckSwitch.generateSwitches(m_modelSwitchOptions.getValues()) +
					strAdvancedOptions +
					"-or\n" +
					"-ta \"{0}\"\n" +
					"-ca \"{1}\"\n" +
					"-l \"{2}\"";
			strOptions = strOptions.replace("{0}", fileTempTransformationConfigFile.getAbsolutePath());
			strOptions = strOptions.replace("{1}", fileTempAugmentedAtomsConfigFile.getAbsolutePath());
			strOptions = strOptions.replace("{2}", fileLog.getAbsolutePath());
			LOGGER.info("Initializing StruChk of RDKit with the following options:\n" + strOptions);
			LOGGER.info(fileTempTransformationConfigFile.getName() + " copied from " + strTransformationConfigurationOriginal);
			LOGGER.info(fileTempAugmentedAtomsConfigFile.getName() + " copied from " + strAugmentedAtomsConfigurationOriginal);

			final int iError = RDKFuncs.initCheckMol(strOptions);
			if (iError != 0) {
				throw new Exception("Configuring the Structure Normalizer failed with error code #" + iError +
						" - Please check your configuration files and settings.");
			}
		}
	}

	@Override
	protected void cleanupIntermediateResults() {
		super.cleanupIntermediateResults();
		m_inputType = null;
	}

	/**
	 * Creates an SDF Cell from the passed in SDF Value. It checks, if the SDF Value
	 * has the required $$$ postfix. If not, it adds it.
	 * 
	 * @param strSdfValue The SDF string value to be transformed into an SDF cell. Can be null.
	 * 
	 * @return Data cell, either SdfAdapterCell or a missing cell, if null was passed in.
	 */
	protected DataCell createSdfCell(String strSdfValue) {
		DataCell cellRet;

		if (StringUtils.isEmptyAfterTrimming(strSdfValue)) {
			cellRet = DataType.getMissingCell();
		}
		else {
			// KNIME SDF type requires string to be terminated
			// by $$$$ -- see org.knime.chem.types.SdfValue for details
			if (!strSdfValue.endsWith(SDF_POSTFIX)) {
				strSdfValue += SDF_POSTFIX;
			}

			cellRet = SdfCellFactory.createAdapterCell(strSdfValue);
		}

		return cellRet;
	}

	//
	// Static Public Methods
	//

	/**
	 * Reads a configuration file either from the specified file, or - if it is null or
	 * equal to the {@link #DEFAULT_CONFIGURATION_ID} - it reads it from the defined
	 * default resource.
	 * 
	 * @param strFileName The file name of a file to be read or {@link #DEFAULT_CONFIGURATION_ID}
	 * 		to use the default resource.
	 * @param strDefaultResource The path the default resource (in JAR file).
	 * 
	 * @return Configuration string (content of the file).
	 * 
	 * @throws InvalidSettingsException Thrown, if configuration could not be read.
	 */
	public static String getConfiguration(final String strFileName, final String strDefaultResource) throws InvalidSettingsException {
		String strConfiguration = null;
		String strErrorMsgStart = null;

		InputStream in = null;

		// Check for default and use it
		if (isDefaultConfigurationFile(strFileName)) {
			strErrorMsgStart = "The default configuration file resource '" +
					strDefaultResource + "' ";
			in = RDKitStructureNormalizerNodeModel.class.getClassLoader().
					getResourceAsStream(strDefaultResource);
			if (in == null) {
				throw new InvalidSettingsException(strErrorMsgStart + "could not be found.");
			}
		}

		// Load custom file and use it
		else {
			final File file = FileUtils.convertToFile(strFileName, true, false);
			strErrorMsgStart = "The custom configuration file '" +
					file.getAbsolutePath() + "' ";
			try {
				in = new FileInputStream(file);
			}
			catch (final FileNotFoundException exc) {
				throw new InvalidSettingsException(strErrorMsgStart + "could not be found.", exc);
			}
		}

		// Load the file
		try {
			strConfiguration = FileUtils.getContentFromResource(in); // Closes the input stream
		}
		catch (final IOException exc) {
			throw new InvalidSettingsException(
					strErrorMsgStart + "could not be read successfully: " + exc.getMessage(), exc);
		}

		return strConfiguration;
	}

	/**
	 * Determines, if the the specified file name (of a StruChecker configuration
	 * file) defines a default configuration file or not. It returns true, if
	 * the file name is either null or is equal to {@link #DEFAULT_CONFIGURATION_ID}.
	 *
	 * @param strInputFile Either a path name to the input file or the value {@link #DEFAULT_CONFIGURATION_ID}.
	 *
	 * @return True, if the passed in value is null or the {@link #DEFAULT_CONFIGURATION_ID}. False otherwise.
	 */
	public static boolean isDefaultConfigurationFile(final String strInputFile) {
		return (strInputFile == null || DEFAULT_CONFIGURATION_ID.equals(strInputFile));
	}

	/**
	 * Determines the input type that will be used for the structure checking.
	 * 
	 * @param type Column type of the input. Can be null.
	 * 
	 * @return Input type for the checker to be used. Null, if null was passed in.
	 */
	public Input determineStructureCheckerInputType(final DataType type) throws InvalidSettingsException {
		Input ret = null;

		if (type != null) {
		   // 1. We find an SDF cell
         if (type.isCompatible(SdfValue.class) || type.isAdaptable(SdfValue.class)) {
				ret = Input.SDF;
			}

			// 2. We find a SMILES cell ...
			else if (type.isCompatible(SmilesValue.class) || type.isAdaptable(SmilesValue.class)) {
				ret = Input.SMILES;
			}			
		}

		return ret;
	}

}
