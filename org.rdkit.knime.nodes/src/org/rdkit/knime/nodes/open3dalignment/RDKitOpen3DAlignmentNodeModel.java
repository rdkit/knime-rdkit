/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2013-2023
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
package org.rdkit.knime.nodes.open3dalignment;

import org.RDKit.DistanceGeom;
import org.RDKit.Double_Pair;
import org.RDKit.ForceField;
import org.RDKit.ROMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.AbstractRDKitCalculatorNodeModel;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.types.RDKitAdapterCell;
import org.rdkit.knime.types.RDKitMolCellFactory;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.InvalidInputException;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class implements the node model of the RDKitOpen3DAlignment node
 * providing calculations based on the open source RDKit library.
 * 
 * @author Manuel Schwarze
 */
public class RDKitOpen3DAlignmentNodeModel extends AbstractRDKitCalculatorNodeModel {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitOpen3DAlignmentNodeModel.class);


	/** Input data info index for query Mol value (table 1). */
	protected static final int QUERY_INPUT_COLUMN_MOL = 0;

	/** Input data info index for reference Mol value (table 2). */
	protected static final int REFERENCE_INPUT_COLUMN_MOL = 0;

	//
	// Members
	//

	/** Settings model for the column name of the query input column (tabel 1). */
	private final SettingsModelString m_modelQueryInputColumnName =
			registerSettings(RDKitOpen3DAlignmentNodeDialog.createQueryInputColumnNameModel());

	/** Settings model for the column name of the reference input column (table 2). */
	private final SettingsModelString m_modelReferenceInputColumnName =
			registerSettings(RDKitOpen3DAlignmentNodeDialog.createReferenceInputColumnNameModel());

	/** Settings model for the column name of the new column of the aligned molecule to be added to the output table. */
	private final SettingsModelString m_modelNewAlignedColumnName =
			registerSettings(RDKitOpen3DAlignmentNodeDialog.createNewAlignedColumnNameModel());

	/** Settings model for the option to remove the source column from the output table. */
	private final SettingsModelBoolean m_modelRemoveSourceColumns =
			registerSettings(RDKitOpen3DAlignmentNodeDialog.createRemoveSourceColumnsOptionModel());

	/**
	 * Settings model for the column name of the new column of the row id of the used reference molecule
	 * to be added to the output table.
	 */
	private final SettingsModelString m_modelNewRefIdColumnName =
			registerSettings(RDKitOpen3DAlignmentNodeDialog.createNewRefIdColumnNameModel());

	/** Settings model for the column name of the new column with RMSD information to be added to the output table. */
	private final SettingsModelString m_modelNewRmsdColumnName =
			registerSettings(RDKitOpen3DAlignmentNodeDialog.createNewRmsdColumnNameModel());

	/** Settings model for the column name of the new column with score information to be added to the output table. */
	private final SettingsModelString m_modelNewScoreColumnName =
			registerSettings(RDKitOpen3DAlignmentNodeDialog.createNewScoreColumnNameModel());

	/** Settings model for the advanced option to allow reflection in the alignment process. */
	private final SettingsModelBoolean m_modelAllowReflection =
			registerSettings(RDKitOpen3DAlignmentNodeDialog.createAllowReflectionModel());

	/** Settings model for the advanced option for maximal number of iterations. */
	private final SettingsModelIntegerBounded m_modelMaxIterations =
			registerSettings(RDKitOpen3DAlignmentNodeDialog.createMaxIterationsModel());

	/** Settings model for the advanced option of specifying accuracy of the alignment process. */
	private final SettingsModelIntegerBounded m_modelAccuracy =
			registerSettings(RDKitOpen3DAlignmentNodeDialog.createAccuracyModel());

	// Intermediate values

	/** A row iterator on the reference table. Used if the reference table has multiple rows. */
	private CloseableRowIterator m_itReferenceRows;

	/** An input data info object for dealing with the reference molecule table. */
	private InputDataInfo m_inputDataReference;

	/** A single reference molecule. Used if the reference table has exactly one row. */
	private ROMol m_molReference;

	/**
	 * The row key in a re-usable string cell in case of a single reference molecule.
	 * Used if the reference table has exactly one row.
	 */
	private StringCell m_cellConstantReference;

	//
	// Constructor
	//

	/**
	 * Create new node model with one data in- and one out-port.
	 */
	RDKitOpen3DAlignmentNodeModel() {
		super(2, 1);
      registerInputTablesWithSizeLimits(0, 1); // Both input tables support only limited size
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

		// Auto guess the query input column if not set - fails if no compatible column found
		SettingsUtils.autoGuessColumn(inSpecs[0], m_modelQueryInputColumnName, RDKitMolValue.class, 0,
				"Auto guessing: Using column %COLUMN_NAME% for query molecule column in table 1.",
				"No RDKit Mol, SMILES or SDF compatible column in input table.", getWarningConsolidator());

		// Auto guess the reference input column if not set - fails if no compatible column found
		SettingsUtils.autoGuessColumn(inSpecs[1], m_modelReferenceInputColumnName, RDKitMolValue.class, 0,
				"Auto guessing: Using column %COLUMN_NAME% for reference molecule column in table 2.",
				"No RDKit Mol, SMILES or SDF compatible column in input table.", getWarningConsolidator());

		// Determines, if the query input column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[0], m_modelQueryInputColumnName, RDKitMolValue.class,
				"Query input column has not been specified yet.",
				"Query input column %COLUMN_NAME% does not exist. Has the input table 1 changed?");

		// Determines, if the reference input column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[1], m_modelReferenceInputColumnName, RDKitMolValue.class,
				"Reference input column has not been specified yet.",
				"Reference input column %COLUMN_NAME% does not exist. Has the input table 2 changed?");

		// Auto guess the new aligned molecule column name and make it unique
		final String strQueryInputColumnName = m_modelQueryInputColumnName.getStringValue();
		SettingsUtils.autoGuessColumnName(inSpecs[0], null,
				(m_modelRemoveSourceColumns.getBooleanValue() ?
						new String[] { strQueryInputColumnName } : null),
						m_modelNewAlignedColumnName, strQueryInputColumnName + " (Aligned)");

		// Auto guess the new reference id column name and make it unique
		SettingsUtils.autoGuessColumnName(inSpecs[0],
				new String[] { m_modelNewAlignedColumnName.getStringValue() },
				(m_modelRemoveSourceColumns.getBooleanValue() ?
						new String[] { strQueryInputColumnName } : null),
						m_modelNewRefIdColumnName, "Reference Row ID");

		// Auto guess the new RMSD column name and make it unique
		SettingsUtils.autoGuessColumnName(inSpecs[0],
				new String[] { m_modelNewAlignedColumnName.getStringValue(), m_modelNewRefIdColumnName.getStringValue() },
				(m_modelRemoveSourceColumns.getBooleanValue() ?
						new String[] { strQueryInputColumnName } : null),
						m_modelNewRmsdColumnName, "RMSD");

		// Auto guess the new score column name and make it unique
		SettingsUtils.autoGuessColumnName(inSpecs[0],
				new String[] { m_modelNewAlignedColumnName.getStringValue(), m_modelNewRefIdColumnName.getStringValue(),
				m_modelNewRmsdColumnName.getStringValue() },
				(m_modelRemoveSourceColumns.getBooleanValue() ?
						new String[] { strQueryInputColumnName } : null),
						m_modelNewScoreColumnName, "Score");

		// Determine, if the new aligned molecule column name has been set and if it is really unique
		SettingsUtils.checkColumnNameUniqueness(inSpecs[0], null,
				(m_modelRemoveSourceColumns.getBooleanValue() ? new String[] {
					m_modelQueryInputColumnName.getStringValue() } : null),
					m_modelNewAlignedColumnName,
					"Aligned molecule output column has not been specified yet.",
				"The name %COLUMN_NAME% of the new aligned molecule column exists already in the input.");

		// Determine, if the new reference id column name has been set and if it is really unique
		SettingsUtils.checkColumnNameUniqueness(inSpecs[0],
				new String[] { m_modelNewAlignedColumnName.getStringValue() },
				(m_modelRemoveSourceColumns.getBooleanValue() ? new String[] {
					strQueryInputColumnName } : null),
					m_modelNewRefIdColumnName,
					"Reference ID output column has not been specified yet.",
				"The name %COLUMN_NAME% of the new reference ID column exists already in the input.");

		// Determine, if the new RMSD column name has been set and if it is really unique
		SettingsUtils.checkColumnNameUniqueness(inSpecs[0],
				new String[] { m_modelNewAlignedColumnName.getStringValue(), m_modelNewRefIdColumnName.getStringValue() },
				(m_modelRemoveSourceColumns.getBooleanValue() ? new String[] {
					strQueryInputColumnName } : null),
					m_modelNewRmsdColumnName,
					"RMSD output column has not been specified yet.",
				"The name %COLUMN_NAME% of the new RMSD column exists already in the input.");

		// Determine, if the new score column name has been set and if it is really unique
		SettingsUtils.checkColumnNameUniqueness(inSpecs[0],
				new String[] { m_modelNewAlignedColumnName.getStringValue(), m_modelNewRefIdColumnName.getStringValue(),
				m_modelNewRmsdColumnName.getStringValue() },
				(m_modelRemoveSourceColumns.getBooleanValue() ? new String[] {
					strQueryInputColumnName } : null),
					m_modelNewScoreColumnName,
					"Score output column has not been specified yet.",
				"The name %COLUMN_NAME% of the new score column exists already in the input.");

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
			arrDataInfo = new InputDataInfo[1]; // We have only one input column in table 1
			arrDataInfo[QUERY_INPUT_COLUMN_MOL] = new InputDataInfo(inSpec, m_modelQueryInputColumnName,
					InputDataInfo.EmptyCellPolicy.DeliverEmptyRow, null,
					RDKitMolValue.class);
		}

		// Specify input of table 2
		if (inPort == 1) {
			arrDataInfo = new InputDataInfo[1]; // We have only one input column in table 2
			arrDataInfo[REFERENCE_INPUT_COLUMN_MOL] = new InputDataInfo(inSpec, m_modelReferenceInputColumnName,
					InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
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
			final DataColumnSpec[] arrOutputSpec = new DataColumnSpec[4]; // We have four output column
			arrOutputSpec[0] = new DataColumnSpecCreator(
					m_modelNewAlignedColumnName.getStringValue(), RDKitAdapterCell.RAW_TYPE)
			.createSpec();
			arrOutputSpec[1] = new DataColumnSpecCreator(
					m_modelNewRefIdColumnName.getStringValue(), StringCell.TYPE)
			.createSpec();
			arrOutputSpec[2] = new DataColumnSpecCreator(
					m_modelNewRmsdColumnName.getStringValue(), DoubleCell.TYPE)
			.createSpec();
			arrOutputSpec[3] = new DataColumnSpecCreator(
					m_modelNewScoreColumnName.getStringValue(), DoubleCell.TYPE)
			.createSpec();

			// Advanced options
			final boolean bAllowReflection = m_modelAllowReflection.getBooleanValue();
			final int iMaxIterations = m_modelMaxIterations.getIntValue();
			final int iAccuracy = m_modelAccuracy.getIntValue();
			final DataCell missingCell = DataType.getMissingCell();
			final WarningConsolidator warnings = getWarningConsolidator();

			// Generate factory
			arrOutputFactories[0] = new AbstractRDKitCellFactory(this, AbstractRDKitCellFactory.RowFailurePolicy.DeliverEmptyValues,
					warnings, null, arrOutputSpec) {

				@Override
				/**
				 * This method implements the calculation logic to generate the new cells based on
				 * the input made available in the first (and second) parameter.
				 * {@inheritDoc}
				 */
				public DataCell[] process(final InputDataInfo[] arrInputDataInfo, final DataRow row, final long lUniqueWaveId) throws Exception {
					DataCell outputAlignedMolecule = missingCell;
					DataCell outputRefId = missingCell;
					DataCell outputRmsd = missingCell;
					DataCell outputScore = missingCell;
					StringCell cellRefId = null;

					// Get reference molecule
					ROMol molReference = null;

					// ... either from the table row by row (this cannot happen in parallel currently)
					// There is room for improvement for the following code to allow parallel processing:
					// We could create a mechanism that makes the reference molecules
					// available for random access (by row number) - however, due to memory limitations
					// we should not do this for the entire reference table, but need to chunk it somehow,
					// which makes such a mechanism more complex (but still feasible).
					if (m_itReferenceRows != null) {
						if (m_itReferenceRows.hasNext()) {
							final DataRow rowReference = m_itReferenceRows.next();
							molReference = markForCleanup(m_inputDataReference.getROMol(rowReference), lUniqueWaveId);
							cellRefId = new StringCell(rowReference.getKey().getString());
						}
					}
					// ... or take it from the single element in the table (this can happen in parallel)
					else {
						molReference = m_molReference;
						cellRefId = m_cellConstantReference;
					}

					if (molReference != null) {
						// Align the molecule based on the found reference
						final ROMol molQuery = markForCleanup(arrInputDataInfo[QUERY_INPUT_COLUMN_MOL].getROMol(row), lUniqueWaveId);

						try {
							// Check if atom properties are set
							// 1. Check reference molecule (only if we have different reference molecules, otherwise
							// we do the check during preprocessing to save time here)
							if (m_itReferenceRows != null && !ForceField.MMFFHasAllMoleculeParams(molReference)) {
								throw new InvalidInputException("The reference molecule is missing parameters.");
							}

							// 2. Check query molecule
							if (!ForceField.MMFFHasAllMoleculeParams(molQuery)) {
								throw new InvalidInputException("The query molecule is missing parameters.");
							}

							// Check, if 3D coordinates exist in reference molecule, otherwise fail
							if (molReference.getNumConformers() != 0) {
								// Check, if 3D coordinates exist in query molecule, otherwise create them
								if (molQuery.getNumConformers() == 0) {
									DistanceGeom.EmbedMolecule(molQuery, 0, 42);
								}

								boolean bNonEmpty = false;
								double dRMSD, dScore;

								final Double_Pair result = markForCleanup(
										molQuery.O3AAlignMol(molReference, -1, -1, bAllowReflection, iMaxIterations, iAccuracy),
										lUniqueWaveId);
								dRMSD = result.getFirst();
								dScore = result.getSecond();

								// Only produce non-empty output, if we could align something that is not empty afterwards
								bNonEmpty = (molQuery.getNumAtoms() > 0);

								if (bNonEmpty) {
									outputAlignedMolecule = RDKitMolCellFactory.createRDKitAdapterCell(molQuery);
									outputRefId = cellRefId;
									outputRmsd = new DoubleCell(dRMSD);
									outputScore = new DoubleCell(dScore);
								}
								else {
									warnings.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
											"Aligned molecule is empty.");
								}
							}
							else {
								warnings.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
										"Unable to align query molecule due to missing conformation in the reference molecule.");
							}
						}
						catch (final Exception exc) {
							// Something went wrong during alignment
							String strError = exc.getMessage();
							if (strError == null) {
								strError = exc.getClass().getName();
							}

							warnings.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
									"Alignment of query molecule failed: " + strError);
						}
					}
					else {
						warnings.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
								"Unable to align query molecule due to non-existing reference molecule.");
					}

					return new DataCell[] { outputAlignedMolecule, outputRefId, outputRmsd, outputScore };
				}
			};

			// We cannot work in parallel here because this would screw up our iteration mechanism for
			// the first table in case we are using more than a single element from it
			arrOutputFactories[0].setAllowParallelProcessing(m_itReferenceRows == null);
		}

		return (arrOutputFactories == null ? new AbstractRDKitCellFactory[0] : arrOutputFactories);
	}

	/**
	 * Pre-processing takes only 1% of the time.
	 * 
	 * @return 0.01d
	 */
	@Override
	protected double getPreProcessingPercentage() {
		return 0.01d;
	}

	/**
	 * Pre-processing prepares the reference molecules.
	 */
	@Override
	protected void preProcessing(final BufferedDataTable[] inData,
			final InputDataInfo[][] arrInputDataInfo, final ExecutionContext exec)
					throws Exception {
		// Determine how to access the reference molecule during alignment processing
		final int iQueryMolCount = (int)inData[0].size();
		final int iRefMolCount = (int)inData[1].size();

		// Only perform checks, if we have at least one query molecule (otherwise the result table is empty anyway)
		if (iQueryMolCount > 0) {
			if (iRefMolCount == 0) {
				throw new Exception("Reference molecule table must not be empty.");
			}
			else {
				final CloseableRowIterator iterator = inData[1].iterator();
				if (iRefMolCount == 1) {
					final DataRow rowReference = iterator.next();
					m_molReference = markForCleanup(arrInputDataInfo[1][REFERENCE_INPUT_COLUMN_MOL].getROMol(rowReference));
					m_cellConstantReference = new StringCell(rowReference.getKey().getString());
					m_inputDataReference = null;
					m_itReferenceRows = null;

					if (m_molReference == null) {
						throw new InvalidInputException("Reference molecule is not present.");
					}

					// Check single reference molecule for necessary parameters
					if (!ForceField.MMFFHasAllMoleculeParams(m_molReference)) {
						throw new InvalidInputException("The reference molecule is missing parameters that are necessary for 3D alignment.");
					}
				}
				else {
					m_inputDataReference = createInputDataInfos(1, getInputTableSpecs(inData)[1])[REFERENCE_INPUT_COLUMN_MOL];
					m_itReferenceRows = iterator;
					m_molReference = null;
					m_cellConstantReference = null;
				}
			}

			// Warn the user, if table lengths are different between reference and query table
			if (iRefMolCount > 1 && iQueryMolCount < iRefMolCount) {
				getWarningConsolidator().saveWarning("The reference table is longer than the query table.");
			}

			if (iRefMolCount > 1 && iQueryMolCount > iRefMolCount) {
				getWarningConsolidator().saveWarning("The reference table is shorter than the query table.");
			}
		}
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
			result.remove(createInputDataInfos(0, inSpec)[QUERY_INPUT_COLUMN_MOL].getColumnIndex());
		}

		return result;
	}

	@Override
	protected void cleanupIntermediateResults() {
		if (m_itReferenceRows != null) {
			m_itReferenceRows.close();
			m_itReferenceRows = null;
		}
		m_inputDataReference = null;
		m_molReference = null;
		m_cellConstantReference = null;
	}
}
