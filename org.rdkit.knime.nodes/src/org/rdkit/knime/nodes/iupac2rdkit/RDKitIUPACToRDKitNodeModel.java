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
package org.rdkit.knime.nodes.iupac2rdkit;

import org.RDKit.RWMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.AbstractRDKitCalculatorNodeModel;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.types.RDKitAdapterCell;
import org.rdkit.knime.types.RDKitMolCellFactory;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.SplitCondition.SplitMissingCell;
import org.rdkit.knime.util.WarningConsolidator;

import uk.ac.cam.ch.wwmm.opsin.NameToStructure;
import uk.ac.cam.ch.wwmm.opsin.NameToStructureException;

/**
 * This class implements the node model of the RDKitIUPACToRDKit node
 * providing calculations based on the open source RDKit library.
 * 
 * @author Manuel Schwarze
 */
public class RDKitIUPACToRDKitNodeModel extends AbstractRDKitCalculatorNodeModel {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitIUPACToRDKitNodeModel.class);


	/** Input data info index for IUPAC name. */
	protected static final int INPUT_COLUMN_IUPAC = 0;

	//
	// Members
	//

	/** Settings model for the column name of the input column. */
	private final SettingsModelString m_modelInputColumnName =
			registerSettings(RDKitIUPACToRDKitNodeDialog.createInputColumnNameModel());

	/** Settings model for the column name of the new column to be added to the output table. */
	private final SettingsModelString m_modelNewColumnName =
			registerSettings(RDKitIUPACToRDKitNodeDialog.createNewColumnNameModel());

	//
	// Constructor
	//

	/**
	 * Create new node model with one data in- and one out-port.
	 */
	RDKitIUPACToRDKitNodeModel() {
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

		final WarningConsolidator warningConsolidator = getWarningConsolidator();

		// Auto guess the input column name based on type and name, if it was not set yet
		if (m_modelInputColumnName.getStringValue() == null) {
			int iCountOfIUPACColumns = 0;

			for (final DataColumnSpec colSpec : inSpecs[0]) {
				if (colSpec.getType().isCompatible(StringValue.class) &&
						colSpec.getName().toUpperCase().indexOf("IUPAC") >= 0) {
					iCountOfIUPACColumns++;
					if (iCountOfIUPACColumns == 1) {
						m_modelInputColumnName.setStringValue(colSpec.getName());
					}
				}
			}

			if (iCountOfIUPACColumns > 1) {
				warningConsolidator.saveWarning("Auto guessing: Using column " +
						m_modelInputColumnName.getStringValue() + ".");
			}
		}

		// Auto guess the input column if still not set - fails if no compatible column found
		SettingsUtils.autoGuessColumn(inSpecs[0], m_modelInputColumnName, StringValue.class, 0,
				"Auto guessing: Using column %COLUMN_NAME%.",
				"No String compatible column in input table that can be used as IUPAC name.",
				warningConsolidator);

		// Determines, if the input column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[0], m_modelInputColumnName, StringValue.class,
				"Input column has not been specified yet.",
				"Input column %COLUMN_NAME% does not exist. Has the input table changed?");

		// Auto guess the new column name and make it unique
		final String strInputColumnName = m_modelInputColumnName.getStringValue();
		SettingsUtils.autoGuessColumnName(inSpecs[0], null, null,
				m_modelNewColumnName, strInputColumnName + " (RDKit Mol)");

		// Determine, if the new column name has been set and if it is really unique
		SettingsUtils.checkColumnNameUniqueness(inSpecs[0], null, null,
				m_modelNewColumnName,
				"Output column has not been specified yet.",
				"The name %COLUMN_NAME% of the new column exists already in the input.");

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
			arrDataInfo[INPUT_COLUMN_IUPAC] = new InputDataInfo(inSpec, m_modelInputColumnName,
					InputDataInfo.EmptyCellPolicy.DeliverEmptyRow, null,
					StringValue.class);
		}

		return (arrDataInfo == null ? new InputDataInfo[0] : arrDataInfo);
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
					m_modelNewColumnName.getStringValue(), RDKitAdapterCell.RAW_TYPE)
			.createSpec();

			// Create instance of OPSIN library
			NameToStructure uipacConverterTemp = null;

			try {
				uipacConverterTemp = NameToStructure.getInstance();
			}
			catch (final NameToStructureException excInit) {
				throw new InvalidSettingsException(
						"Unable to initialize OPSIN library needed for IUPAC name conversions.", excInit);
			}

			final NameToStructure uipacConverter = uipacConverterTemp;

			// Generate factory
			arrOutputFactories[0] = new AbstractRDKitCellFactory(this, AbstractRDKitCellFactory.RowFailurePolicy.DeliverEmptyValues,
					getWarningConsolidator(), null, arrOutputSpec) {

				@Override
				/**
				 * This method implements the calculation logic to generate the new cells based on
				 * the input made available in the first (and second) parameter.
				 * {@inheritDoc}
				 */
				public DataCell[] process(final InputDataInfo[] arrInputDataInfo, final DataRow row, final long lUniqueWaveId) throws Exception {
					DataCell outputCell = null;

					// Calculate the new cells
					final String strIupacName = arrInputDataInfo[INPUT_COLUMN_IUPAC].getString(row);

					// Converts the IUPAC name to SMILES
					final String strSmiles = uipacConverter.parseToSmiles(strIupacName);

					// Check for validity and convert further to RDKit Molecule
					String strErrorMsg = null;
					Exception excError = null;

					if (strSmiles != null) {
						// Create the RDKit Molecule with default settings
						try {
							final RWMol mol = markForCleanup(RWMol.MolFromSmiles(strSmiles), lUniqueWaveId);

							try {
								// Note: The SMILES that we include is the one delivered from OPSIN.
								//       It is not canonicalized.
								outputCell = RDKitMolCellFactory.createRDKitAdapterCell(mol, strSmiles);
							}
							catch (final Exception exc) {
								strErrorMsg = exc.getClass().getSimpleName() +
										" when creating RDKit Mol Cell.";
								excError = exc;
							}
						}
						catch (final Exception exc) {
							strErrorMsg = exc.getClass().getSimpleName() +
									" when creating RDKit Molecule from SMILES.";
							excError = exc;
						}
					}
					else {
						strErrorMsg = "IUPAC name parsing error.";
					}

					if (strErrorMsg != null) {
						outputCell = DataType.getMissingCell();
						final String strMsg = "Failed to process data due to " + strErrorMsg;
						LOGGER.debug(strMsg + " (Row '" + row.getKey() + "')", excError);
						getWarningConsolidator().saveWarning(WarningConsolidator.ROW_CONTEXT.getId(), strMsg);
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
	 * Returns 10% as post processing progress part.
	 * 
	 * @return 10%.
	 */
	@Override
	protected double getPostProcessingPercentage() {
		return 0.1d;
	}

	/**
	 * Post processing of conversion results. The are split into two tables here
	 * based on successful and unsuccessful conversions.
	 */
	@Override
	protected BufferedDataTable[] postProcessing(final BufferedDataTable[] inData,
			final InputDataInfo[][] arrInputDataInfo,
			final BufferedDataTable[] processingResult, final ExecutionContext exec)
					throws Exception {
		final String strNewColumnName = m_modelNewColumnName.getStringValue();

		// Split calculated table to move empty cells into a second table
		final ExecutionContext execSplit = exec.createSubExecutionContext(0.8d);
		final BufferedDataTable[] arrSplitTables =
				createSplitTables(0, processingResult[0], arrInputDataInfo[0], execSplit, null,
						new SplitMissingCell(processingResult[0].getDataTableSpec().
								findColumnIndex(strNewColumnName)));

		// Check for total failure
		if (arrSplitTables[0].size() == 0 &&
				arrSplitTables[1].size() > 0) {
			throw new Exception("Failed to process UIPAC names for all rows. Please check, if the correct column was selected.");
		}

		// Remove new (but empty) column from conversion failures table
		final ExecutionContext execRearrange = exec.createSubExecutionContext(0.2d);
		final ColumnRearranger rearranger = new ColumnRearranger(arrSplitTables[1].getDataTableSpec());
		rearranger.remove(strNewColumnName);
		final BufferedDataTable tableFailures =
				exec.createColumnRearrangeTable(arrSplitTables[1], rearranger, execRearrange);

		return new BufferedDataTable[] { arrSplitTables[0], tableFailures };
	}

	@Override
	protected DataTableSpec getOutputTableSpec(final int outPort,
			final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		DataTableSpec spec = null;

		// Successful conversions - Structure driven by calculator node type
		if (outPort == 0) {
			spec = super.getOutputTableSpec(outPort, inSpecs);
		}

		// Failed conversions - Same table structure as input table
		else if (outPort == 1){
			spec = inSpecs[0];
		}

		return spec;
	}
}
