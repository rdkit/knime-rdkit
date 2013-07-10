/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2012
 * Novartis Institutes for BioMedical Research
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
package org.rdkit.knime.nodes.rdkit2inchi;

import java.util.ArrayList;
import java.util.List;

import org.RDKit.ExtraInchiReturnValues;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.AbstractRDKitCalculatorNodeModel;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsUtils;

/**
 * This class implements the node model of the RDKitMolecule2InChI node
 * providing calculations based on the open source RDKit library.
 * 
 * @author Manuel Schwarze
 */
public class RDKitMolecule2InChINodeModel extends AbstractRDKitCalculatorNodeModel {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitMolecule2InChINodeModel.class);

	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;

	/** Lock to prevent access of multiple threads into the InChI calculation as it would crash the Java VM. */
	private static final Object LOCK = new Object();

	/** Postfix for the column name for extra InChI generation information: return value. */
	public static final String POSTFIX_RETURN_VALUE = " - Return Value";

	/** Postfix for the column name for extra InChI generation information: aux info. */
	public static final String POSTFIX_AUX_INFO = " - Aux Info";

	/** Postfix for the column name for extra InChI generation information: message. */
	public static final String POSTFIX_MESSAGE = " - Message";

	/** Postfix for the column name for extra InChI generation information: log. */
	public static final String POSTFIX_LOG = " - Log";


	//
	// Members
	//

	/** Settings model for the column name of the input column. */
	private final SettingsModelString m_modelInputColumnName =
			registerSettings(RDKitMolecule2InChINodeDialog.createInputColumnNameModel());

	/** Settings model for the column name of the new InChI code column to be added to the output table. */
	private final SettingsModelString m_modelNewInChICodeColumnName =
			registerSettings(RDKitMolecule2InChINodeDialog.createNewInChICodeColumnNameModel());

	/** Settings model for the option to remove the source column from the output table. */
	private final SettingsModelBoolean m_modelRemoveSourceColumns =
			registerSettings(RDKitMolecule2InChINodeDialog.createRemoveSourceColumnsOptionModel());

	/** Settings model for the option to generate also InChI keys beside the InChI codes. */
	private final SettingsModelBoolean m_modelGenerateInChIKeysOption =
			registerSettings(RDKitMolecule2InChINodeDialog.createGenerateInChIKeyOptionModel());

	/** Settings model for the column name of the new InChI key column to be added to the output table. */
	private final SettingsModelString m_modelNewInChIKeyColumnName =
			registerSettings(RDKitMolecule2InChINodeDialog.createNewInChIKeyColumnNameModel(m_modelGenerateInChIKeysOption));

	/** Settings model for the column name of the new InChI key column to be added to the output table. */
	private final SettingsModelString m_modelExtraInformationColumnNamePrefix =
			registerSettings(RDKitMolecule2InChINodeDialog.createExtraInformationColumnNamePrefixModel());

	/** Settings model for the option to generate extra information: Return value. */
	private final SettingsModelBoolean m_modelExtraReturnCodeOption =
			registerSettings(RDKitMolecule2InChINodeDialog.createExtraReturnCodeOptionModel());

	/** Settings model for the option to generate extra information: Aux Info. */
	private final SettingsModelBoolean m_modelExtraAuxInfoOption =
			registerSettings(RDKitMolecule2InChINodeDialog.createExtraAuxInfoOptionModel());

	/** Settings model for the option to generate extra information: Message. */
	private final SettingsModelBoolean m_modelExtraMessageOption =
			registerSettings(RDKitMolecule2InChINodeDialog.createExtraMessageOptionModel());

	/** Settings model for the option to generate extra information: Log. */
	private final SettingsModelBoolean m_modelExtraLogOption =
			registerSettings(RDKitMolecule2InChINodeDialog.createExtraLogOptionModel());

	/** Settings model for the advanced generation options. */
	private final SettingsModelString m_modelAdvancedOptions =
			registerSettings(RDKitMolecule2InChINodeDialog.createAdvancedOptionsModel());

	//
	// Constructor
	//

	/**
	 * Create new node model with one data in- and one out-port.
	 */
	RDKitMolecule2InChINodeModel() {
		super(1, 1);
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

		// Auto guess the new InChI code column name and make it unique
		final String strInputColumnName = m_modelInputColumnName.getStringValue();
		SettingsUtils.autoGuessColumnName(inSpecs[0], null,
				(m_modelRemoveSourceColumns.getBooleanValue() ?
						new String[] { strInputColumnName } : null),
						m_modelNewInChICodeColumnName, strInputColumnName + " (InChI Code)");

		// Auto guess the new InChI key column name and make it unique
		SettingsUtils.autoGuessColumnName(inSpecs[0],
				new String[] { m_modelNewInChICodeColumnName.getStringValue() },
				(m_modelRemoveSourceColumns.getBooleanValue() ?
						new String[] { strInputColumnName } : null),
						m_modelNewInChIKeyColumnName, strInputColumnName + " (InChI Key)");

		// Generate lists of column names that will be there additionally or can be excluded
		final List<String> listAddColumns = new ArrayList<String>();
		final List<String> listExcludeColumns = new ArrayList<String>();
		listAddColumns.add(m_modelNewInChICodeColumnName.getStringValue());
		if (m_modelGenerateInChIKeysOption.getBooleanValue()) {
			listAddColumns.add(m_modelNewInChIKeyColumnName.getStringValue());
		}
		if (m_modelRemoveSourceColumns.getBooleanValue()) {
			listExcludeColumns.add(m_modelInputColumnName.getStringValue());
		}
		final String[] arrAddColumns = listAddColumns.toArray(new String[listAddColumns.size()]);
		final String[] arrExclColumns = listExcludeColumns.toArray(new String[listExcludeColumns.size()]);

		// Auto guess the new extra information column prefix
		String strExtraInfoColumnPrefix = m_modelExtraInformationColumnNamePrefix.getStringValue();
		if ((m_modelExtraReturnCodeOption.getBooleanValue() ||
				m_modelExtraAuxInfoOption.getBooleanValue() ||
				m_modelExtraMessageOption.getBooleanValue() ||
				m_modelExtraLogOption.getBooleanValue()) &&
				(strExtraInfoColumnPrefix == null || strExtraInfoColumnPrefix.isEmpty())) {
			final String strSuggestedName = strInputColumnName + " (InChI Extra) ";
			String strResult = strSuggestedName;

			// Create list of all existing names
			final List<String> listNames = SettingsUtils.createMergedColumnNameList(inSpecs[0],
					arrAddColumns, arrExclColumns);

			// Unify the name
			int uniquifier = 1;

			while (listNames.contains(strResult + POSTFIX_RETURN_VALUE) ||
					listNames.contains(strResult + POSTFIX_AUX_INFO) ||
					listNames.contains(strResult + POSTFIX_MESSAGE) ||
					listNames.contains(strResult + POSTFIX_LOG))
			{
				strResult = strSuggestedName + " (#" + uniquifier + ")";
				uniquifier++;
			}

			m_modelExtraInformationColumnNamePrefix.setStringValue(strResult);
		}

		// Update for further checks
		strExtraInfoColumnPrefix = m_modelExtraInformationColumnNamePrefix.getStringValue();

		// Determine, if the new InChI code column name has been set and if it is really unique
		SettingsUtils.checkColumnNameUniqueness(inSpecs[0], null,
				(m_modelRemoveSourceColumns.getBooleanValue() ? new String[] {
					m_modelInputColumnName.getStringValue() } : null),
					m_modelNewInChICodeColumnName,
					"InChI code output column has not been specified yet.",
				"The name %COLUMN_NAME% of the new InChI code column exists already in the input.");

		// More checks, if InChI key will also be calculated
		if (m_modelGenerateInChIKeysOption.getBooleanValue()) {
			// Determine, if the new InChI key column name has been set and if it is really unique
			SettingsUtils.checkColumnNameUniqueness(inSpecs[0],
					new String[] { m_modelNewInChICodeColumnName.getStringValue() }, arrExclColumns,
					m_modelNewInChIKeyColumnName,
					"InChI key output column has not been specified yet.",
					"The name %COLUMN_NAME% of the new InChI key column exists already in the input.");

		}

		// More checks, if extra information about InChI code generation will also be added
		// For return value column
		if (m_modelExtraReturnCodeOption.getBooleanValue()) {
			// Determine, if the new return value column name has been set and if it is really unique
			SettingsUtils.checkColumnNameUniqueness(inSpecs[0],
					arrAddColumns, arrExclColumns,
					strExtraInfoColumnPrefix == null ? null :
						strExtraInfoColumnPrefix + POSTFIX_RETURN_VALUE,
						"Extra information output column name prefix has not been specified yet.",
					"The name %COLUMN_NAME% of the extra return value column exists already in the input.");
		}
		// For aux info column
		if (m_modelExtraReturnCodeOption.getBooleanValue()) {
			// Determine, if the new aux info column name has been set and if it is really unique
			SettingsUtils.checkColumnNameUniqueness(inSpecs[0],
					arrAddColumns, arrExclColumns,
					strExtraInfoColumnPrefix == null ? null :
						strExtraInfoColumnPrefix + POSTFIX_AUX_INFO,
						"Extra information output column name prefix has not been specified yet.",
					"The name %COLUMN_NAME% of the extra aux info column exists already in the input.");
		}
		// For message column
		if (m_modelExtraReturnCodeOption.getBooleanValue()) {
			// Determine, if the new message column name has been set and if it is really unique
			SettingsUtils.checkColumnNameUniqueness(inSpecs[0],
					arrAddColumns, arrExclColumns,
					strExtraInfoColumnPrefix == null ? null :
						strExtraInfoColumnPrefix + POSTFIX_MESSAGE,
						"Extra information output column name prefix has not been specified yet.",
					"The name %COLUMN_NAME% of the extra message column exists already in the input.");
		}
		// For log column
		if (m_modelExtraReturnCodeOption.getBooleanValue()) {
			// Determine, if the new logcolumn name has been set and if it is really unique
			SettingsUtils.checkColumnNameUniqueness(inSpecs[0],
					arrAddColumns, arrExclColumns,
					strExtraInfoColumnPrefix == null ? null :
						strExtraInfoColumnPrefix + POSTFIX_LOG,
						"Extra information output column name prefix has not been specified yet.",
					"The name %COLUMN_NAME% of the extra log column exists already in the input.");
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
	@SuppressWarnings("unchecked")
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
			final boolean bGenerateInChIKeys= m_modelGenerateInChIKeysOption.getBooleanValue();
			final boolean bGenerateReturnValue = m_modelExtraReturnCodeOption.getBooleanValue();
			final boolean bGenerateAuxInfo = m_modelExtraAuxInfoOption.getBooleanValue();
			final boolean bGenerateMessage = m_modelExtraMessageOption.getBooleanValue();
			final boolean bGenerateLog = m_modelExtraLogOption.getBooleanValue();
			final String strExtraColumnPrefix = m_modelExtraInformationColumnNamePrefix.getStringValue();

			final List<DataColumnSpec> listOutputSpec = new ArrayList<DataColumnSpec>();

			// Mandatory column
			listOutputSpec.add(new DataColumnSpecCreator(
					m_modelNewInChICodeColumnName.getStringValue(), StringCell.TYPE)
			.createSpec());

			// Optional columns
			if (bGenerateInChIKeys) {
				listOutputSpec.add(new DataColumnSpecCreator(
						m_modelNewInChIKeyColumnName.getStringValue(), StringCell.TYPE)
				.createSpec());
			}
			if (bGenerateReturnValue) {
				listOutputSpec.add(new DataColumnSpecCreator(
						strExtraColumnPrefix + POSTFIX_RETURN_VALUE, IntCell.TYPE)
				.createSpec());
			}
			if (bGenerateAuxInfo) {
				listOutputSpec.add(new DataColumnSpecCreator(
						strExtraColumnPrefix + POSTFIX_AUX_INFO, StringCell.TYPE)
				.createSpec());
			}
			if (bGenerateMessage) {
				listOutputSpec.add(new DataColumnSpecCreator(
						strExtraColumnPrefix + POSTFIX_MESSAGE, StringCell.TYPE)
				.createSpec());
			}
			if (bGenerateLog) {
				listOutputSpec.add(new DataColumnSpecCreator(
						strExtraColumnPrefix + POSTFIX_LOG, StringCell.TYPE)
				.createSpec());
			}

			final DataColumnSpec[] arrOutputSpec = listOutputSpec.toArray(new DataColumnSpec[listOutputSpec.size()]);

			// Check advanced options
			String strAdvancedOptions = m_modelAdvancedOptions.getStringValue();
			if (strAdvancedOptions != null) {
				strAdvancedOptions = strAdvancedOptions.trim();
				if (strAdvancedOptions.isEmpty()) {
					strAdvancedOptions = null;
				}
			}
			final String strUseAdvancedOptions = strAdvancedOptions;

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
					final List<DataCell> listOutput = new ArrayList<DataCell>(3);

					// Calculate the new cells
					final ROMol mol = markForCleanup(arrInputDataInfo[INPUT_COLUMN_MOL].getROMol(row), iUniqueWaveId);
					final ExtraInchiReturnValues extraInfo = markForCleanup(new ExtraInchiReturnValues(), iUniqueWaveId);
					String strInChICode;

					// Generate InChI Code
					synchronized (LOCK) {
						strInChICode = (strUseAdvancedOptions == null ?
								RDKFuncs.MolToInchi(mol, extraInfo) :
									RDKFuncs.MolToInchi(mol, extraInfo, strUseAdvancedOptions));
					}

					if (strInChICode == null || strInChICode.isEmpty()) {
						listOutput.add(DataType.getMissingCell());
					}
					else {
						listOutput.add(new StringCell(strInChICode));
					}

					// Generate InChI Key
					if (bGenerateInChIKeys) {
						if (strInChICode == null || strInChICode.isEmpty()) {
							listOutput.add(null);
						}
						else {
							final String strInChIKey = RDKFuncs.InchiToInchiKey(strInChICode);

							if (strInChICode == null || strInChICode.isEmpty()) {
								listOutput.add(null);
							}
							else {
								listOutput.add(new StringCell(strInChIKey));
							}
						}
					}

					// Generate Extra Information

					// Add return code
					if (bGenerateReturnValue) {
						listOutput.add(new IntCell(extraInfo.getReturnCode()));
					}

					// Add aux info
					if (bGenerateAuxInfo) {
						final String str = extraInfo.getAuxInfoPtr();
						listOutput.add(str == null ? DataType.getMissingCell() : new StringCell(str.trim()));
					}

					// Add message
					if (bGenerateMessage) {
						final String str = extraInfo.getMessagePtr();
						listOutput.add(str == null ? DataType.getMissingCell() : new StringCell(str.trim()));
					}

					// Add log
					if (bGenerateLog) {
						final String str = extraInfo.getLogPtr();
						listOutput.add(str == null ? DataType.getMissingCell() : new StringCell(str.trim()));
					}

					return listOutput.toArray(new DataCell[listOutput.size()]);
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
}
