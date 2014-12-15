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
package org.rdkit.knime.nodes.saltstripper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.RDKit.RWMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.rdkit.knime.nodes.AbstractRDKitCalculatorNodeModel;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.types.RDKitMolCellFactory;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class implements the node model of the RDKitSaltStripper node
 * providing calculations based on the open source RDKit library.
 * This node removes various types of salts from the RDKit molecules.
 * It species the two input (one input molecule port and one
 * optional salt definitions port) and one output port
 * for salt stripped RDKit molecules.
 * 
 * @author Dillip K Mohanty
 * @author Manuel Schwarze
 */
public class RDKitSaltStripperNodeModel extends AbstractRDKitCalculatorNodeModel {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitSaltStripperNodeModel.class);

	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;

	/** Input data info index for Salt value (if second table is connected). */
	protected static final int INPUT_COLUMN_SALT = 0;

	/**
	 * Defines the resource name of the default salt definition file.
	 */
	protected static final String SALT_DEFINITION_FILE = "/org/rdkit/knime/nodes/saltstripper/Salts.txt";

	/** Warning context for salts. */
	protected static final WarningConsolidator.Context SALT_CONTEXT =
			new WarningConsolidator.Context("Salt", "salt", "salts", true);

	//
	// Members
	//

	/** Settings model for the column name of the input column. */
	private final SettingsModelString m_modelInputColumnName =
			registerSettings(RDKitSaltStripperNodeDialog.createInputColumnNameModel(), "input_column", "molecule_input");
	// Accept also deprecated keys

	/** Settings model for the column name of the new column to be added to the output table. */
	private final SettingsModelString m_modelNewColumnName =
			registerSettings(RDKitSaltStripperNodeDialog.createNewColumnNameModel(), true);

	/** Settings model for the option to remove the source column from the output table. */
	private final SettingsModelBoolean m_modelRemoveSourceColumns =
			registerSettings(RDKitSaltStripperNodeDialog.createRemoveSourceColumnsOptionModel(), true);

	/** Settings model for the column name of the salt column. */
	private final SettingsModelString m_modelOptionalSaltColumnName =
			registerSettings(RDKitSaltStripperNodeDialog.createOptionalSaltColumnNameModel(), "salt_column", "salt_input");
	// Accept also deprecated keys

	//
	// Internals
	//

	/** Used to detect changes in second input table usage. */
	private boolean m_bHadSaltInputTable = false;

	/** Pre-processing result of salt list used during main processing. */
	private List<ROMol> m_listSalts = null;

	/** Pre-processing result of number of processed salts (maybe not all are valid). */
	private int m_iProcessedSaltCount = 0;

	//
	// Constructor
	//

	/**
	 * Create new node model with one data in- and one out-port.
	 */
	RDKitSaltStripperNodeModel() {
		super(new PortType[] {
				// Input ports (2nd port is optional)
				new PortType(BufferedDataTable.TYPE.getPortObjectClass(), false),
				new PortType(BufferedDataTable.TYPE.getPortObjectClass(), true) },
				new PortType[] {
				// Output ports
				new PortType(BufferedDataTable.TYPE.getPortObjectClass(), false) });

		getWarningConsolidator().registerContext(SALT_CONTEXT);
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

		// Auto guess the new column name and make it unique
		final String strInputColumnName = m_modelInputColumnName.getStringValue();
		SettingsUtils.autoGuessColumnName(inSpecs[0], null,
				(m_modelRemoveSourceColumns.getBooleanValue() ?
						new String[] { strInputColumnName } : null),
						m_modelNewColumnName, "Salt Stripped Molecule");

		// Determine, if the new column name has been set and if it is really unique
		SettingsUtils.checkColumnNameUniqueness(inSpecs[0], null,
				(m_modelRemoveSourceColumns.getBooleanValue() ? new String[] {
					m_modelInputColumnName.getStringValue() } : null),
					m_modelNewColumnName,
					"Output column has not been specified yet.",
				"The name %COLUMN_NAME% of the new column exists already in the input.");

		// For optional salt input column
		if (hasSaltInputTable(inSpecs)) {

			// Reset salt table column name, if the table has changed, so that we can auto guess again
			if (m_bHadSaltInputTable == false && !SettingsUtils.checkColumnExistence(inSpecs[1],
					m_modelOptionalSaltColumnName, RDKitMolValue.class, null, null)) {
				m_modelOptionalSaltColumnName.setStringValue(null);
			}

			m_bHadSaltInputTable = true;

			// Auto guess the salt input column if not set - fails if no compatible column found
			SettingsUtils.autoGuessColumn(inSpecs[1], m_modelOptionalSaltColumnName, RDKitMolValue.class, 0,
					"Auto guessing: Using column %COLUMN_NAME% as salt column.",
					"No RDKit Mol, SMILES or SDF compatible column in input table. Use the \"RDKit from Molecule\" " +
							"node to convert SMARTS.", getWarningConsolidator());

			// Determines, if the salt input column exists - fails if it does not
			SettingsUtils.checkColumnExistence(inSpecs[1], m_modelOptionalSaltColumnName, RDKitMolValue.class,
					"Salt input column has not been specified yet.",
					"Salt input column %COLUMN_NAME% does not exist. Has the salt table changed?");
		}

		// Or for optional reaction SMARTS value
		else {
			m_bHadSaltInputTable = false;
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
					RDKitMolValue.class);
		}

		// Specify input of optional salt table
		else if (inPort ==  1 && inSpec != null) {
			arrDataInfo = new InputDataInfo[1]; // We have only one input column
			arrDataInfo[INPUT_COLUMN_SALT] = new InputDataInfo(inSpec, m_modelOptionalSaltColumnName,
					InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
					RDKitMolValue.class);
		}

		return (arrDataInfo == null ? new InputDataInfo[0] : arrDataInfo);
	}


	/**
	 * Returns the output table specification of the specified out port. This implementation
	 * works based on a ColumnRearranger and delivers only a specification for
	 * out port 0, based on an input table on in port 0.
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

		if (outPort == 0) {
			// Create the column rearranger, which will generate the spec
			spec = createColumnRearranger(outPort, inSpecs[0]).createSpec();
		}

		return spec;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected AbstractRDKitCellFactory[] createOutputFactories(final int outPort, final DataTableSpec inSpec)
			throws InvalidSettingsException {

		AbstractRDKitCellFactory[] arrOutputFactories = null;

		// Specify output of table 1
		if (outPort == 0) {
			// Allocate space for all factories (usually we have only one)
			arrOutputFactories = new AbstractRDKitCellFactory[1];

			// Factory 1:
			// ==========
			// Generate column specs for the output table columns produced by this factory
			final DataColumnSpec[] arrOutputSpec = new DataColumnSpec[1]; // We have only one output column
			arrOutputSpec[0] = new DataColumnSpecCreator(
					m_modelNewColumnName.getStringValue(), RDKitMolCellFactory.TYPE)
			.createSpec();

			final int iSaltCount = (m_listSalts == null || m_listSalts.isEmpty() ? 0 : m_listSalts.size());

			// Generate factory
			arrOutputFactories[0] = new AbstractRDKitCellFactory(this, AbstractRDKitCellFactory.RowFailurePolicy.DeliverEmptyValues,
					getWarningConsolidator(), null, arrOutputSpec) {

				@Override
				/**
				 * This method implements the calculation logic to generate the new cells based on
				 * the input made available in the first (and second) parameter.
				 * {@inheritDoc}
				 */
				public DataCell[] process(final InputDataInfo[] arrInputDataInfo, final DataRow row, final int iUniqueWaveId) throws Exception {
					DataCell outputCell = null;

					// Case 1: If we don't have any salts defined, reuse the input cell without any conversion
					if (iSaltCount == 0) {
						outputCell = arrInputDataInfo[INPUT_COLUMN_MOL].getCell(row);
					}

					// Case 2: Do the salt stripping
					else {
						// Calculate the new cells
						final ROMol mol = markForCleanup(arrInputDataInfo[INPUT_COLUMN_MOL].getROMol(row), iUniqueWaveId);
						ROMol molStripping = mol;

						// Iterate over the salt patterns for each molecule
						for (int i = 0; i < iSaltCount; i++) {

							// Is there still a molecule with fragments to strip?
							if (markForCleanup(RDKFuncs.getMolFrags(molStripping), iUniqueWaveId).size() > 1) {

								// Try to strip off the next salt in the list
								final ROMol molStripped = markForCleanup(RDKFuncs.deleteSubstructs(
										molStripping, m_listSalts.get(i), true), iUniqueWaveId);

								// If stripped structure is not empty, apply further stripping,
								// otherwise keep the last structure, even if it still contains a salt now
								// (subsequent stripping may reduce the structure further)
								if (molStripped.getNumAtoms() > 0) {
									molStripping = molStripped;
								}
							}
							else {
								break;
							}
						}

						// Sanitize the result molecule
						final RWMol molResult = markForCleanup(new RWMol(molStripping), iUniqueWaveId);

						try {
							RDKFuncs.sanitizeMol(molResult);
							outputCell = RDKitMolCellFactory.createRDKitMolCell(molResult);
						}
						catch (final Exception e) { // Sanitizing
							LOGGER.debug("Sanitizing failed for molecule in row '" + row.getKey() + "'. " +
									"Keeping the original molecule.");
							outputCell = arrInputDataInfo[INPUT_COLUMN_MOL].getCell(row);
						}
					}

					return new DataCell[] { outputCell };
				}
			};

			// Enable or disable this factory to allow parallel processing
			arrOutputFactories[0].setAllowParallelProcessing(true);
		}

		return (arrOutputFactories == null ? new AbstractRDKitCellFactory[0] : arrOutputFactories);
	}

	/**
	 * {@inheritDoc}
	 * This implementation removes additionally the compound source column, if specified in the settings.
	 */
	@Override
	protected ColumnRearranger createColumnRearranger(final int outPort,
			final DataTableSpec inSpec) throws InvalidSettingsException {
		// Perform normal work
		final ColumnRearranger result = super.createColumnRearranger(outPort, inSpec);

		// Remove the input column, if desired
		if (m_modelRemoveSourceColumns.getBooleanValue()) {
			result.remove(createInputDataInfos(0, inSpec)[INPUT_COLUMN_MOL].getColumnIndex());
		}

		return result;
	}

	/**
	 * Returns the percentage of pre-processing activities from the total execution.
	 *
	 * @return Percentage of pre-processing. Returns 0.1d.
	 */
	@Override
	protected double getPreProcessingPercentage() {
		return 0.1d;
	}

	/**
	 * Here we prepare the list of salts which shall be used for the salt stripping.
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
	protected void preProcessing(final BufferedDataTable[] inData, final InputDataInfo[][] arrInputDataInfo,
			final ExecutionContext exec) throws Exception {

		// Option 1: Read salt from connected salt table
		if (hasSaltInputTable(getInputTableSpecs(inData)) &&
				m_modelOptionalSaltColumnName.getStringValue() != null) {

			m_listSalts = new ArrayList<ROMol>(100);
			m_iProcessedSaltCount = inData[1].getRowCount();

			for (final DataRow row : inData[1]) {
				final ROMol molSalt = markForCleanup(arrInputDataInfo[1][INPUT_COLUMN_SALT].getROMol(row));

				if (molSalt != null) {
					m_listSalts.add(molSalt);
				}
				else {
					getWarningConsolidator().saveWarning(SALT_CONTEXT.getId(),
							"Encountered empty cell in salt table.");
				}
			}
		}

		// Option 2: Read salt from internal salt definition file
		else {
			m_listSalts = readSaltsFromFile();
		}

		// Show a warning, if we don't have any salts defined
		if (m_listSalts == null || m_listSalts.isEmpty()) {
			getWarningConsolidator().saveWarning("There are no salts defined. " +
					"Output molecules will match input molecules.");
			m_listSalts = null;
		}

		// Does not do anything by default
		exec.setProgress(1.0d);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanupIntermediateResults() {
		// Note: Salt molecules are disposed automatically at the end of processing
		if (m_listSalts != null) {
			m_listSalts.clear();
			m_listSalts = null;
		}
		m_iProcessedSaltCount = 0;
	}

	/**
	 * {@inheritDoc}
	 * This implementation considers the number of processed salts.
	 */
	@Override
	protected Map<String, Integer> createWarningContextOccurrencesMap(
			final BufferedDataTable[] inData, final InputDataInfo[][] arrInputDataInfo,
			final BufferedDataTable[] resultData) {
		final Map<String, Integer> map =  super.createWarningContextOccurrencesMap(inData, arrInputDataInfo,
				resultData);
		map.put(SALT_CONTEXT.getId(), m_iProcessedSaltCount);

		return map;
	}

	//
	// Private Methods
	//

	/**
	 * Reads the salt patterns from an internal salt resource file.
	 * 
	 * @return List of found salts. These salts have been marked for cleanup
	 * 		which will be performed at the end of node execution or
	 * 		whenever {@link #cleanupMarkedObjects()} is called.
	 */
	private List<ROMol> readSaltsFromFile() throws IOException {
		final List<ROMol> listSalts = new ArrayList<ROMol>(100);
		BufferedReader readerSalts = null;
		InputStream inSalts = null;

		try {
			inSalts = getClass().getResourceAsStream(SALT_DEFINITION_FILE);
			readerSalts = new BufferedReader(new InputStreamReader(inSalts));
			String strLine = null;

			while ((strLine = readerSalts.readLine()) != null) {

				// Remove all leading and trailing whitespaces
				strLine = strLine.trim();

				// Ignore comments and empty lines starting with '//' else get inside
				if (!strLine.isEmpty() && !strLine.startsWith("//") && !strLine.startsWith("'")) {

					// Ignore content after tab
					final String delims = "[\t]+";
					final String[] tokens = strLine.split(delims);

					// Evaluate found salt and add to list, if possible
					if (tokens.length > 0) {
						final String strSmarts = tokens[0].trim();

						try {
							m_iProcessedSaltCount++;
							listSalts.add(markForCleanup(RWMol.MolFromSmarts(strSmarts)));
						}
						catch (final Exception exc) {
							LOGGER.warn("Salt '" + strSmarts +
									"' could not be processed and will be ignored.");
							getWarningConsolidator().saveWarning(SALT_CONTEXT.getId(),
									"A salt could not be processed and will be ignored.");
						}
					}
				}
			}
		}
		finally {
			// Close reader and stream
			if (readerSalts != null) {
				try {
					readerSalts.close();
				}
				catch (final IOException e) {
					// Ignored by purpose
				}
			}

			if (inSalts != null) {
				try {
					inSalts.close();
				}
				catch (final IOException e) {
					// Ignored by purpose
				}
			}
		}

		return listSalts;
	}


	//
	// Static Public Methods
	//

	/**
	 * Determines, if the condition is fulfilled that we have a salt table with
	 * columns connected to the node according to the passed in specs.
	 * 
	 * @param inSpecs Port specifications.
	 * 
	 * @return True, if there is a salt table present at the last index of the specs,
	 * 		and if it has columns.
	 */
	public static boolean hasSaltInputTable(final PortObjectSpec[] inSpecs) {
		return (inSpecs != null && inSpecs.length >= 2 &&
				inSpecs[1] instanceof DataTableSpec &&
				((DataTableSpec)inSpecs[1]).getNumColumns() > 0);
	}
}
