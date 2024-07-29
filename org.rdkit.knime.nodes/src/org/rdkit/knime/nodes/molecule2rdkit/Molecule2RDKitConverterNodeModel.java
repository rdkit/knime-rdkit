/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2010-2023
 * Novartis Pharma AG, Switzerland
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
package org.rdkit.knime.nodes.molecule2rdkit;

import java.util.ArrayList;
import java.util.List;

import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.RDKit.RWMol;
import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmartsValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;
import org.rdkit.knime.types.RDKitAdapterCell;
import org.rdkit.knime.types.RDKitMolCellFactory;
import org.rdkit.knime.types.RDKitMolValueRenderer;
import org.rdkit.knime.types.preferences.RDKitDepicterPreferencePage;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.InputDataInfo.EmptyCellException;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class implements the node model of the "Molecule2RDKitConverter" node
 * providing translations of a molecule column to an RDKit Molecule based on
 * the open source RDKit library.
 * 
 * @author Greg Landrum
 * @author Manuel Schwarze
 */
public class Molecule2RDKitConverterNodeModel extends AbstractRDKitNodeModel {

	//
	// Enumeration
	//

	public enum InputType {
		SMILES, SDF, SMARTS;
	}

	/**
	 * This enumeration defines how erroneous molecules, which cannot be converted
	 * into RDKit Molecules, shall be handled.
	 * 
	 * @author Greg Landrum
	 */
	public enum ParseErrorPolicy implements ButtonGroupEnumInterface {

		/** Policy to send rows with erroneous molecules to second output. */
		SPLIT_ROWS("Send error rows to second output", "The table at the second "
				+ "port contains the input rows with problematic structures"),

				/** Policy to insert missing values for an erroneous molecules. */
				MISS_VAL("Insert missing values", "If the input structure can't be "
						+ "translated, a missing value is inserted.");

		//
		// Members
		//

		/** Friendly name of the policy. Used in GUI as option text. */
		private final String m_name;

		/** Tooltip of the policy to be used in GUI as option tooltip. */
		private final String m_tooltip;

		//
		// Constructor
		//

		/**
		 * Creates a new policy enumeration value.
		 * 
		 * @param name Friendly name of the policy. Used in GUI as option text.
		 * @param tooltip Tooltip of the policy to be used in GUI as option tooltip.
		 */
		ParseErrorPolicy(final String name, final String tooltip) {
			m_name = name;
			m_tooltip = tooltip;
		}

		//
		// Public Methods
		//

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getText() {
			return m_name;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getActionCommand() {
			return this.name();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getToolTip() {
			return m_tooltip;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isDefault() {
			return this.getActionCommand().equals(SPLIT_ROWS.getActionCommand());
		}
	}


	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(Molecule2RDKitConverterNodeModel.class);

	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;

	//
	// Members
	//

	/** Settings model for the column name of the input column. */
	private final SettingsModelString m_modelInputColumnName =
			registerSettings(Molecule2RDKitConverterNodeDialog.createInputColumnNameModel(), "input_column", "first_column");
	// Accepts also old deprecated key

	/** Settings model for the option to treat input molecules as query. */
	private final SettingsModelBoolean m_modelTreatAsQuery =
			registerSettings(Molecule2RDKitConverterNodeDialog.createTreatAsQueryOptionModel(), true);

	/** Settings model for the column name of the new column to be added to the output table. */
	private final SettingsModelString m_modelNewColumnName =
			registerSettings(Molecule2RDKitConverterNodeDialog.createNewColumnNameModel());

	/** Settings model for the option to remove the source column from the output table. */
	private final SettingsModelBoolean m_modelRemoveSourceColumns =
			registerSettings(Molecule2RDKitConverterNodeDialog.createRemoveSourceColumnsOptionModel());

	/** Settings model for the option to split output tables to two (second contains bad rows).*/
	private final SettingsModelString m_modelSeparateFails =
			registerSettings(Molecule2RDKitConverterNodeDialog.createSeparateRowsModel());

	/** Settings model for the option to do add error information column. */
	private final SettingsModelBoolean m_modelGenerateErrorInformation =
			registerSettings(Molecule2RDKitConverterNodeDialog.
					createGenerateErrorInfoOptionModel(), true);

	/** Settings model for the option to do add error information column. */
	private final SettingsModelString m_modelErrorInfoColumnName =
			registerSettings(Molecule2RDKitConverterNodeDialog.
					createErrorInfoColumnNameModel(m_modelGenerateErrorInformation), true);

	/** Settings model for the option to compute coordinates. */
	private final SettingsModelBoolean m_modelGenerateCoordinates =
			registerSettings(Molecule2RDKitConverterNodeDialog.createGenerateCoordinatesModel(), true);

	/** Settings model for the option to force computation of coordinates. */
	private final SettingsModelBoolean m_modelForceGenerateCoordinates =
			registerSettings(Molecule2RDKitConverterNodeDialog.
					createForceGenerateCoordinatesModel(m_modelGenerateCoordinates), true);

	/** Settings model for the option to suppress sanitizing a molecule. */
	private final SettingsModelBoolean m_modelQuickAndDirty =
			registerSettings(Molecule2RDKitConverterNodeDialog.
					createQuickAndDirtyModel(), true, "skip_sanitization", "skip_santization");

	/** Settings model for the option to do aromatization (depends on quick and dirty setting). */
	private final SettingsModelBoolean m_modelAromatization =
			registerSettings(Molecule2RDKitConverterNodeDialog.
					createAromatizationModel(m_modelQuickAndDirty), true);

	/** Settings model for the option to do stereo chemistry (depends on quick and dirty setting). */
	private final SettingsModelBoolean m_modelStereoChem =
			registerSettings(Molecule2RDKitConverterNodeDialog.
					createStereochemistryModel(m_modelQuickAndDirty), true);

	/** Settings model for the option to do keep hydrogens. */
	private final SettingsModelBoolean m_modelKeepHs =
			registerSettings(Molecule2RDKitConverterNodeDialog.createKeepHsOptionModel(), true);

	/** Settings model for the option to do strict parsing of mol blocks. */
	private final SettingsModelBoolean m_modelStrictParsing =
			registerSettings(Molecule2RDKitConverterNodeDialog.createStrictParsingOptionModel(), true);

	//
	// Internals
	//

	/** This variable is used during execution for performance reasons. */
	private InputType m_inputType = null;

	/** This variable is used during execution for performance reasons. */
	private boolean m_bTreatAsQuery = false;

	/** This variable is used during execution for performance reasons. */
	private boolean m_bSanitize = false;

	/** This variable is used during execution for performance reasons. */
	private boolean m_bRemoveHs = false;

	/** This variable is used during execution for performance reasons. */
	private boolean m_bStrictParsing = false;

	//
	// Constructor
	//

	/**
	 * Create new node model with one data in- and two out-ports.
	 */
	Molecule2RDKitConverterNodeModel() {
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

		// Create list of acceptable input column types
		final List<Class<? extends DataValue>> listValueClasses =
				new ArrayList<Class<? extends DataValue>>();
		listValueClasses.add(SmilesValue.class);
		listValueClasses.add(SmartsValue.class);
		listValueClasses.add(SdfValue.class);

		// Auto guess the input column if not set - fails if no compatible column found
		SettingsUtils.autoGuessColumn(inSpecs[0], m_modelInputColumnName, listValueClasses, 0,
				"Auto guessing: Using column %COLUMN_NAME%.",
				"Neither SMILES, SMARTS nor SDF compatible column in input table.",
				getWarningConsolidator());

		// Determines, if the input column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[0], m_modelInputColumnName, listValueClasses,
				"Input column has not been specified yet.",
				"SMILES, SMARTS or SDF compatible input column %COLUMN_NAME% does not exist. Has the input table changed?");

		// Auto guess the new column name and make it unique
		final String strInputColumnName = m_modelInputColumnName.getStringValue();
		SettingsUtils.autoGuessColumnName(inSpecs[0], null,
				(m_modelRemoveSourceColumns.getBooleanValue() ?
						new String[] { strInputColumnName } : null),
						m_modelNewColumnName, strInputColumnName + " (RDKit Mol)");

		// Determine, if the new column name has been set and if it is really unique
		SettingsUtils.checkColumnNameUniqueness(inSpecs[0], null,
				(m_modelRemoveSourceColumns.getBooleanValue() ? new String[] {
					m_modelInputColumnName.getStringValue() } : null),
					m_modelNewColumnName,
					"Output column has not been specified yet.",
				"The name %COLUMN_NAME% of the new column exists already in the input.");

		// Handle error information column name, but only if option was enabled
		if (m_modelGenerateErrorInformation.getBooleanValue()) {
			final boolean bSplitBadRowsToPort1 = ParseErrorPolicy.SPLIT_ROWS.getActionCommand()
					.equals(m_modelSeparateFails.getStringValue());

			// Autofill error info column name
			SettingsUtils.autoGuessColumnName(inSpecs[0],
					bSplitBadRowsToPort1 ?
							null : new String[] { m_modelNewColumnName.getStringValue() },
							!bSplitBadRowsToPort1 && m_modelRemoveSourceColumns.getBooleanValue() ?
									null : new String[] { m_modelInputColumnName.getStringValue() },
									m_modelErrorInfoColumnName, strInputColumnName + " (RDKit Error Info)");

			// Determine, if the error info column name has been set and if it is really unique
			SettingsUtils.checkColumnNameUniqueness(inSpecs[0],
					bSplitBadRowsToPort1 ?
							null : new String[] { m_modelNewColumnName.getStringValue() },
							!bSplitBadRowsToPort1 && m_modelRemoveSourceColumns.getBooleanValue() ?
									null : new String[] { m_modelInputColumnName.getStringValue() },
									m_modelErrorInfoColumnName,
									"Optional error information column name has not been specified yet.",
					"The name %COLUMN_NAME% of the new error information column exists already in the input.");
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
					SmilesValue.class, SmartsValue.class, SdfValue.class);
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
	 */
	@Override
	protected DataTableSpec getOutputTableSpec(final int outPort,
			final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		DataTableSpec spec = null;
		final boolean bSplitBadRowsToPort1 = ParseErrorPolicy.SPLIT_ROWS.getActionCommand()
				.equals(m_modelSeparateFails.getStringValue());
		final boolean bIncludeErrorInfo = m_modelGenerateErrorInformation.getBooleanValue();

		switch (outPort) {
		case 0:
			// Check existence and proper column type
			final InputDataInfo[] arrInputDataInfos = createInputDataInfos(0, inSpecs[0]);
			arrInputDataInfos[0].getColumnIndex();

			// Copy all specs from input table (except input column, if configured)
			final ArrayList<DataColumnSpec> newColSpecs = new ArrayList<DataColumnSpec>();
			final String inputColumnName = m_modelInputColumnName.getStringValue().trim();
			for (final DataColumnSpec inCol : inSpecs[0]) {
				if (!m_modelRemoveSourceColumns.getBooleanValue() ||
						!inCol.getName().equals(inputColumnName)) {
					newColSpecs.add(inCol);
				}
			}

			// Append result column
			newColSpecs.add(new DataColumnSpecCreator(
					m_modelNewColumnName.getStringValue().trim(), RDKitAdapterCell.RAW_TYPE)
			.createSpec());

			// Add the optional error information column
			if (!bSplitBadRowsToPort1 && bIncludeErrorInfo) {
				newColSpecs.add(new DataColumnSpecCreator(
						m_modelErrorInfoColumnName.getStringValue().trim(), StringCell.TYPE)
				.createSpec());
			}

			spec = new DataTableSpec("Output data",
					newColSpecs.toArray(new DataColumnSpec[newColSpecs.size()]));
			break;

		case 1:
			// Check, if second output table is enabled
			if (bSplitBadRowsToPort1) {
				if (bIncludeErrorInfo) {
					// Second table has the same structure as input table + error info
					spec = new DataTableSpec("Erroneous input data", inSpecs[0],
							new DataTableSpec(new DataColumnSpecCreator(
									m_modelErrorInfoColumnName.getStringValue(),
									StringCell.TYPE).createSpec()));
				}
				else {
					// Second table has the same structure as input table
					spec = inSpecs[0];
				}
			}

			// Otherwise disable it
			else {
				spec = new DataTableSpec();
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
	 * @return The output factory to be used to calculate the values when
	 * 		the node executes.
	 * 
	 * @throws InvalidSettingsException Thrown, if the output factory could not be created
	 * 		due to invalid settings.
	 * 
	 * @see #createInputDataInfos(int, DataTableSpec)
	 */
	protected AbstractRDKitCellFactory createOutputFactory(final InputDataInfo[] arrInputDataInfos)
			throws InvalidSettingsException {
		final boolean bIncludeErrorInfo = m_modelGenerateErrorInformation.getBooleanValue();

		// Generate column specs for the output table columns produced by this factory
		// Note: In this node the resulting cells are reorganized later in the
		//       processing function based on settings for splitting bad molecules
		//       and for including error information. Hence this output table spec here
		//       serves only informational purposes and is not always the same as concrete
		//       output tables.
		final List<DataColumnSpec> listOutputSpecs = new ArrayList<DataColumnSpec>();

		// Add the always existing RDKit Molecule column
		listOutputSpecs.add(new DataColumnSpecCreator(
				m_modelNewColumnName.getStringValue().trim(), RDKitAdapterCell.RAW_TYPE)
		.createSpec());

		// Add the optional error information column (is possibly filtered out later)
		String strTempErrorInfoColumnName = m_modelErrorInfoColumnName.getStringValue().trim();
		if (strTempErrorInfoColumnName.isEmpty()) {
			strTempErrorInfoColumnName = "RDKit Error Info";
		}

		listOutputSpecs.add(new DataColumnSpecCreator(
				strTempErrorInfoColumnName, StringCell.TYPE)
		.createSpec());

		// Generate output spec array
		final DataColumnSpec[] arrOutputSpec = listOutputSpecs.toArray(
				new DataColumnSpec[listOutputSpecs.size()]);

		// For performance reasons get reference to missing cell
		final DataCell missingCell = DataType.getMissingCell();

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
				final DataCell[] arrOutputCells = new DataCell[] { missingCell, missingCell };
				RWMol mol = null;
				ROMol molFinal = null;
				String smiles = null;
				Exception excCaught = null;

				// As first step try to parse the input molecule format
				try {
				   if (m_inputType == InputType.SDF) {
                  final String value = arrInputDataInfo[INPUT_COLUMN_MOL].getSdfValue(row);

                  mol = markForCleanup(RWMol.MolFromMolBlock(value, m_bSanitize, m_bRemoveHs, m_bStrictParsing), lUniqueWaveId);
               }
				   else if (m_inputType == InputType.SMILES) {
						final String value = arrInputDataInfo[INPUT_COLUMN_MOL].getSmiles(row);
						mol = markForCleanup(RWMol.MolFromSmiles(value, 0, m_bSanitize && !m_bTreatAsQuery), lUniqueWaveId);
						smiles = value;
					}
					else if (m_inputType == InputType.SMARTS) {
						final String value = arrInputDataInfo[INPUT_COLUMN_MOL].getSmarts(row);
						mol = markForCleanup(RWMol.MolFromSmarts(value, 0, true), lUniqueWaveId);
						smiles = value;
					}
					else {
						throw new InvalidSettingsException("The molecule input type " + m_inputType +
								" is invalid or cannot be handled by this node.");
					}
				}
				catch (final EmptyCellException excEmpty) {
					// If the cell is empty an exception is thrown by .getXXX(row), which is rethrown here
					// and caught in the factory. The factory will deliver empty cells due to the InputDataInfo setup,
					// see also in createInputDataInfos(...).
					throw excEmpty;
				}
				catch (final Exception exc) {
					// Parsing failed and RDKit molecule is null
					excCaught = exc;
				}

				// If we got an RDKit molecule, parsing was successful, now massage it
				if (mol != null) {
					try {
						switch (m_inputType) {
						case SMARTS:
							mol.updatePropertyCache(false);
							molFinal = mol;
							break;
						case SMILES:
						case SDF:
							// Perform partial sanitization only
							if (!m_bSanitize) {
								RDKFuncs.cleanUp(mol);
								mol.updatePropertyCache(false);
								RDKFuncs.symmetrizeSSSR(mol);

								if (m_modelAromatization.getBooleanValue()) {
									RDKFuncs.Kekulize(mol);
									RDKFuncs.setAromaticity(mol);
								}

								RDKFuncs.setConjugation(mol);
								RDKFuncs.setHybridization(mol);

								if (m_modelStereoChem.getBooleanValue()) {
									RDKFuncs.assignStereochemistry(mol, true);
								}

								if(smiles == null){
									smiles = RDKFuncs.MolToSmiles(mol, false, false, 0, false);
								}
							}

							// Special handling to keep Hs and merge query Hs
							if (m_bTreatAsQuery) {
								// For SMILES we did not sanitize in the constructor, need to do it now
								if (m_inputType == InputType.SMILES) {
									RDKFuncs.sanitizeMol(mol);
								}

								// Merge Hs if we treat an SDF input molecule as query
								molFinal = markForCleanup(mol.mergeQueryHs(), lUniqueWaveId);
								molFinal.updatePropertyCache(false);
								RDKFuncs.fastFindRings(molFinal);
							}
							else {
								molFinal = mol;
							}
							break;
						}

						if (m_modelGenerateCoordinates.getBooleanValue()) {
							if (m_modelForceGenerateCoordinates.getBooleanValue() || molFinal.getNumConformers() == 0) {
								RDKitMolValueRenderer.compute2DCoords(molFinal,
									RDKitDepicterPreferencePage.isUsingCoordGen(),
									RDKitDepicterPreferencePage.isNormalizeDepictions());
							} else {
								if (RDKitDepicterPreferencePage.isUsingMolBlockWedging()) {
									((RWMol)molFinal).reapplyMolBlockWedging();
								}
								if (RDKitDepicterPreferencePage.isNormalizeDepictions()) {
									molFinal.normalizeDepiction(-1, 0);
								}
							}
						}

						arrOutputCells[0] = RDKitMolCellFactory.createRDKitAdapterCell(molFinal, smiles);
					}
					catch (final Exception exc) {
						excCaught = exc;
					}
				}

				// Do error handling depending on user settings
				if (molFinal == null || excCaught != null) {
					// Find error message
					final StringBuilder sbError = new StringBuilder(m_inputType.toString());

					// Specify error type
					if (molFinal == null) {
						sbError.append(" Parsing Error (");
					}
					else {
						sbError.append(" Process Error (");
					}

					// Specify exception
					if (excCaught != null) {
						sbError.append(excCaught.getClass().getSimpleName());

						// Specify error message
						final String strMessage = excCaught.getMessage();
						if (strMessage != null) {
							sbError.append(" (").append(strMessage).append(")");
						}
					}
					else {
						sbError.append("Details unknown");
					}

					sbError.append(")");

					// Generate error log cell
					final String strError = sbError.toString();
					if (bIncludeErrorInfo) {
						arrOutputCells[1] = new StringCell(strError);
					}

					// Log message as warning
					final String strMsg = "Failed to process data due to " + strError +
							" - Generating empty result cells.";
					LOGGER.debug(strMsg + " (Row '" + row.getKey() + "')", excCaught);
					getWarningConsolidator().saveWarning(WarningConsolidator.ROW_CONTEXT.getId(), strMsg);
				}

				return arrOutputCells;
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
		final DataTableSpec inSpec = inData[0].getDataTableSpec();
		final DataTableSpec[] arrOutSpecs = getOutputTableSpecs(inData);

		// Contains the rows with the result column
		final BufferedDataContainer port0 = exec.createDataContainer(arrOutSpecs[0]);

		// Contains the input rows if result computation fails
		final BufferedDataContainer port1 = exec.createDataContainer(arrOutSpecs[1]);

		// Get settings and define data specific behavior
		final int iInputIndex = arrInputDataInfo[0][INPUT_COLUMN_MOL].getColumnIndex();
		final DataType type = inSpec.getColumnSpec(iInputIndex).getType();
		final boolean bInludeErrorInfo = m_modelGenerateErrorInformation.getBooleanValue();

		// Define what type we will use as input -
		// if the input column supports more than one type it will use SDF over SMILES over SMARTS
		// Even more important: Use first - if possible - the original format, e.g. a SMILES over
		// a later attached/converted SDF representation of that SMILES. Therefore, check isCompatible()
		// first for all possible formats, and afterwards isAdaptable().
		if (type.isCompatible(SdfValue.class)) {
         m_inputType = InputType.SDF;
      }
		else if (type.isCompatible(SmilesValue.class)) {
			m_inputType = InputType.SMILES;
		}
		else if (type.isCompatible(SmartsValue.class)) {
			m_inputType = InputType.SMARTS;
		}
		else if (type.isAdaptable(SdfValue.class)) {
         m_inputType = InputType.SDF;
      }
      else if (type.isAdaptable(SmilesValue.class)) {
         m_inputType = InputType.SMILES;
      }
      else if (type.isAdaptable(SmartsValue.class)) {
         m_inputType = InputType.SMARTS;
      }

		// Defines the options and ensure they are valid for the input molecule type we process
		m_bSanitize = !m_modelQuickAndDirty.getBooleanValue();
		m_bRemoveHs = !m_modelKeepHs.getBooleanValue();
		m_bStrictParsing = m_modelStrictParsing.getBooleanValue();

		m_bTreatAsQuery = (m_modelTreatAsQuery.getBooleanValue() &&
				(m_inputType == InputType.SMILES || m_inputType == InputType.SDF));

		// Special behavior when treating input as query
		if (m_modelTreatAsQuery.getBooleanValue()) {
			// Always sanitize (no partial sanitization only)
			m_bSanitize = true;
			m_bRemoveHs = false;
		}

		final boolean bSplitBadRowsToPort1 = ParseErrorPolicy.SPLIT_ROWS.getActionCommand()
				.equals(m_modelSeparateFails.getStringValue());
		final long lTotalRowCount = inData[0].size();

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
				// Bad row found - result is missing
				if (arrResults[0].isMissing()) {
					// Move the row into the second table
					if (bSplitBadRowsToPort1) {
						if (bInludeErrorInfo) {
							// Include only the error information cell
							port1.addRowToTable(AbstractRDKitCellFactory.mergeDataCells(row,
									new DataCell[] { arrResults[1] }, -1));
						}
						else {
							port1.addRowToTable(row);
						}
					}
					// Move the row into the first table
					else {
						if (bInludeErrorInfo) {
							port0.addRowToTable(AbstractRDKitCellFactory.mergeDataCells(row, arrResults,
									m_modelRemoveSourceColumns.getBooleanValue() ? iInputIndex : -1));
						}
						else {
							// Include only the result cell
							port0.addRowToTable(AbstractRDKitCellFactory.mergeDataCells(row,
									new DataCell[] { arrResults[0] },
									m_modelRemoveSourceColumns.getBooleanValue() ? iInputIndex : -1));
						}
					}
				}

				// Good row found
				else {
					// Include also empty error information if we have only one output table
					if (!bSplitBadRowsToPort1 && bInludeErrorInfo) {
						port0.addRowToTable(AbstractRDKitCellFactory.mergeDataCells(row, arrResults,
								m_modelRemoveSourceColumns.getBooleanValue() ? iInputIndex : -1));
					}

					// Include only the result cell, if we have two output tables or no error logging
					else {
						port0.addRowToTable(AbstractRDKitCellFactory.mergeDataCells(row,
								new DataCell[] { arrResults[0] },
								m_modelRemoveSourceColumns.getBooleanValue() ? iInputIndex : -1));
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

		port0.close();
		port1.close();

		return new BufferedDataTable[] { port0.getTable(), port1.getTable() };
	}
	
	/**
	 * Corrects the strict parsing setting to "true" for all old nodes that did not have that setting.
	 * Without it we would change the behavior of existing workflows, which might not be desired.
	 */
	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		try {
			super.loadValidatedSettingsFrom(settings);
		}
		finally {
			if (!settings.containsKey("strict_parsing")) {
				m_modelStrictParsing.setBooleanValue(true); // The old default of RDKit
			}
		}		
	}
	
}
