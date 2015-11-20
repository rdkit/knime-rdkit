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
package org.rdkit.knime.nodes.rdkfingerprint;

import org.RDKit.ROMol;
import org.RDKit.UInt_Vect;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.IntValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.AbstractRDKitCalculatorNodeModel;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.properties.FingerprintSettingsHeaderProperty;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsModelEnumeration;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.StringUtils;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class implements the node model of the "RDKitFingerprint" node
 * providing calculations based on the open source RDKit library.
 * 
 * @author Greg Landrum
 * @author Manuel Schwarze
 */
public abstract class AbstractRDKitFingerprintNodeModel extends AbstractRDKitCalculatorNodeModel {

	//
	// Constants
	//

	/** The logger instance. */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(AbstractRDKitFingerprintNodeModel.class);

	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;

	/** Input data info index for an optional atom list (used for rooted fingerprints only). */
	protected static final int INPUT_COLUMN_ATOM_LIST = 1;

	/** Empty atom list to be used if an empty atom list cell is encountered. */
	protected static final UInt_Vect EMPTY_ATOM_LIST = new UInt_Vect(0);

	//
	// Members
	//

	/** Model for the molecule input column. */
	protected final SettingsModelString m_modelInputColumnName =
			registerSettings(RDKitFingerprintNodeDialog.createSmilesColumnModel(), true);

	/** Model for the name of the new column. */
	protected final SettingsModelString m_modelNewColumnName =
			registerSettings(RDKitFingerprintNodeDialog.createNewColumnModel(), true);

	/** Model for the option to remove the input column. */
	protected final SettingsModelBoolean m_modelRemoveSourceColumns =
			registerSettings(RDKitFingerprintNodeDialog.createRemoveSourceColumnOptionModel(), true);

	/** Model for the fingerprint type to apply. */
	protected final SettingsModelEnumeration<FingerprintType> m_modelFingerprintType =
			registerSettings(RDKitFingerprintNodeDialog.createFPTypeModel(), true);

	/** Model for the Torsion path length to be used for calculations. */
	protected final SettingsModelIntegerBounded m_modelTorsionPathLength =
			registerSettings(RDKitFingerprintNodeDialog.createTorsionPathLengthModel(), true);

	/** Model for the minimum path length to be used for calculations. */
	protected final SettingsModelIntegerBounded m_modelMinPath =
			registerSettings(RDKitFingerprintNodeDialog.createMinPathModel(), true);

	/** Model for the maximum path length to be used for calculations. */
	protected final SettingsModelIntegerBounded m_modelMaxPath =
			registerSettings(RDKitFingerprintNodeDialog.createMaxPathModel(), true);

	/** Model for the AtomPair minimum path length to be used for calculations. */
	protected final SettingsModelIntegerBounded m_modelAtomPairMinPath =
			registerSettings(RDKitFingerprintNodeDialog.createAtomPairMinPathModel(), true);

	/** Model for the AtomPair maximum path length to be used for calculations. */
	protected final SettingsModelIntegerBounded m_modelAtomPairMaxPath =
			registerSettings(RDKitFingerprintNodeDialog.createAtomPairMaxPathModel(), true);

	/** Model for the number of fingerprint bits to be used for calculations. */
	protected final SettingsModelIntegerBounded m_modelNumBits =
			registerSettings(RDKitFingerprintNodeDialog.createNumBitsModel(), true);

	/** Model for the radius to be used for calculations. */
	protected final SettingsModelIntegerBounded m_modelRadius =
			registerSettings(RDKitFingerprintNodeDialog.createRadiusModel(), true);

	/** Model for the layer flags to be used for calculations. */
	protected final SettingsModelIntegerBounded m_modelLayerFlags =
			registerSettings(RDKitFingerprintNodeDialog.createLayerFlagsModel(), true);

	/** Model for the flag to create rooted fingerprints. */
	protected final SettingsModelBoolean m_modelRootedOption =
			registerSettings(RDKitFingerprintNodeDialog.createRootedOptionModel(), true);

	/** Model for the atom list input column. */
	protected final SettingsModelString m_modelAtomListColumnName =
			registerSettings(RDKitFingerprintNodeDialog.createAtomListColumnModel(m_modelFingerprintType, m_modelRootedOption), true);

	/** Model for the flag to include atom list for calculation of rooted fingerprints. */
	protected final SettingsModelBoolean m_modelAtomListHandlingIncludeOption =
			registerSettings(RDKitFingerprintNodeDialog.createAtomListHandlingIncludeOptionModel(m_modelFingerprintType, m_modelRootedOption), true);

	//
	// Constructors
	//

	/**
	 * Create new node model with one data in- and one outport.
	 */
	AbstractRDKitFingerprintNodeModel() {
		super(1, 1);
	}

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
						m_modelNewColumnName, strInputColumnName + " (Fingerprint)");

		// Determine, if the new column name has been set and if it is really unique
		SettingsUtils.checkColumnNameUniqueness(inSpecs[0], null,
				(m_modelRemoveSourceColumns.getBooleanValue() ? new String[] {
					m_modelInputColumnName.getStringValue() } : null),
					m_modelNewColumnName,
					"Output column has not been specified yet.",
				"The name %COLUMN_NAME% of the new column exists already in the input.");

		// Check fingerprint settings
		final FingerprintType fpType = m_modelFingerprintType.getValue();
		if (fpType != null) {
			final FingerprintSettings settings = createFingerprintSettings();
			fpType.validateSpecification(settings, inSpecs[0]);
		}
		else {
			throw new InvalidSettingsException("No fingerprint type selected yet.");
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
			arrDataInfo = new InputDataInfo[2]; // We may have two input columns, but at least one
			arrDataInfo[INPUT_COLUMN_MOL] = new InputDataInfo(inSpec, null, m_modelInputColumnName, "molecule",
					InputDataInfo.EmptyCellPolicy.DeliverEmptyRow, null,
					RDKitMolValue.class);

			// Check, if we should add additional column information for rooted fingerprints
			final FingerprintType fpType = m_modelFingerprintType.getValue();
			if (fpType != null && fpType.canCalculateRootedFingerprint() && m_modelRootedOption.getBooleanValue()) {
				arrDataInfo[INPUT_COLUMN_ATOM_LIST] = new InputDataInfo(inSpec, null, m_modelAtomListColumnName, "atom list",
						InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
						CollectionDataValue.class, IntValue.class);
			}
		}

		return (arrDataInfo == null ? new InputDataInfo[0] : arrDataInfo);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected AbstractRDKitCellFactory[] createOutputFactories(final int outPort, final DataTableSpec inSpec)
			throws InvalidSettingsException {

		final WarningConsolidator warnings = getWarningConsolidator();
		AbstractRDKitCellFactory[] arrOutputFactories = null;
		final boolean bIsRooted = m_modelRootedOption.getBooleanValue();

		// Specify output of table 1
		if (outPort == 0) {
			// Allocate space for all factories (usually we have only one)
			arrOutputFactories = new AbstractRDKitCellFactory[1];

			// Factory 1:
			// ==========
			// Generate column specs for the output table columns produced by this factory
			final DataColumnSpec[] arrOutputSpec = new DataColumnSpec[1]; // We have only one output column
			final DataColumnSpecCreator creator = new DataColumnSpecCreator(
					m_modelNewColumnName.getStringValue(), getFingerprintColumnType());

			// Generate fingerprint settings
			final FingerprintType fpType = m_modelFingerprintType.getValue();
			final FingerprintSettings settings = createFingerprintSettings();

			// Add fingerprint specification properties to column header
			new FingerprintSettingsHeaderProperty(settings).writeToColumnSpec(creator);
			arrOutputSpec[0] = creator.createSpec();

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
					final ROMol mol = markForCleanup(arrInputDataInfo[INPUT_COLUMN_MOL].getROMol(row), lUniqueWaveId);

					try {
						// Calculate rooted fingerprint
						if (bIsRooted && fpType.canCalculateRootedFingerprint()) {

							UInt_Vect atomList = null;

							// Check if we have a single atom index column (no collection)
							if (arrInputDataInfo[INPUT_COLUMN_ATOM_LIST].getDataType().isCompatible(IntValue.class)) {

								// Need to check for empty cells specially as it would return 0, which is also a valud atom index
								if (!arrInputDataInfo[INPUT_COLUMN_ATOM_LIST].isMissing(row)) {
									final int iAtomIndex = arrInputDataInfo[INPUT_COLUMN_ATOM_LIST].getInt(row);
									atomList = markForCleanup(new UInt_Vect(1), lUniqueWaveId);
									atomList.add(iAtomIndex);
								}
							}

							// We have a collection column
							else {
								atomList = markForCleanup(arrInputDataInfo[INPUT_COLUMN_ATOM_LIST].getRDKitUIntegerVector(row), lUniqueWaveId);
							}

							if (atomList == null) {
								LOGGER.warn("Encountered empty atom list in row '" + row.getKey() + "'");
								warnings.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(), "Encountered empty atom list cell. Using empty atom list.");
								atomList = EMPTY_ATOM_LIST; // This must not be cleaned up, it is a constant
							}

							outputCell = createFingerprintCell(mol, atomList, settings);
						}

						// Calculate normal fingerprint
						else {
							outputCell = createFingerprintCell(mol, settings);
						}
					}
					catch (final Exception exc) {
						final String strError = exc.getMessage();
						final String strMsg = "Fingerprint Type '" + m_modelFingerprintType.getValue() +
								"' could not be calculated: " + (StringUtils.isEmptyAfterTrimming(strError) ?
										"An unknown error occurred." : strError);
						LOGGER.error(strMsg);
						throw new RuntimeException(strMsg, exc);
					}

					// Check, if fingerprint could not be calculated properly (only if no exception was thrown)
					if (outputCell == null) {
						getWarningConsolidator().saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
								"Error computing fingerprint - Setting value as missing cell.");
						outputCell = DataType.getMissingCell();
					}

					return new DataCell[] { outputCell };
				}
			};

			// Enable this factory to allow parallel processing
			arrOutputFactories[0].setAllowParallelProcessing(true);
		}

		return (arrOutputFactories == null ? new AbstractRDKitCellFactory[0] : arrOutputFactories);
	}

	/**
	 * Returns an array of all fingerprint types that are supported for the fingerprint calculation.
	 * 
	 * @return Array of fingerprint types.
	 */
	protected abstract FingerprintType[] getSupportedFingerprintTypes();

	/**
	 * Returns the target fingerprint column type used in KNIME. It must match the implementation
	 * of the method {@link AbstractRDKitFingerprintNodeModel#createFingerprintCell(ROMol, FingerprintSettings) and
	 * {@link #createFingerprintCell(ROMol, UInt_Vect, FingerprintSettings)}.
	 * 
	 * @return Data type of target fingerprint column.
	 */
	protected abstract DataType getFingerprintColumnType();

	/**
	 * Creates based on the node's settings model the fingerprint settings model object.
	 * 
	 * @return Fingerprint settings. Can be null, if the fingerprint type could not be determined / is invalid.
	 */
	protected abstract FingerprintSettings createFingerprintSettings();

	/**
	 * Creates a non-rooted fingerprint cell based on the passed in settings.
	 * 
	 * @param mol RDKit molecule. Must not be null.
	 * @param settings Fingerprint settings to be applied. Must not be null.
	 * 
	 * @return Fingerprint cell or null, if it cannot be calculated.
	 */
	protected abstract DataCell createFingerprintCell(ROMol mol, FingerprintSettings settings);

	/**
	 * Creates a rooted fingerprint cell based on the passed in settings.
	 * 
	 * @param mol RDKit molecule. Must not be null.
	 * @param atomList Optional atom list (inclusive or exclusive is determined by the settings object). Can be null.
	 * @param settings Fingerprint settings to be applied. Must not be null.
	 * 
	 * @return Fingerprint cell or null, if it cannot be calculated.
	 */
	protected abstract DataCell createFingerprintCell(ROMol mol, UInt_Vect atomList, FingerprintSettings settings);

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
