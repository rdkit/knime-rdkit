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
package org.rdkit.knime.wizards.samples.calculatorsplitter_multithread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.RDKit.RWMol;
import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;
import org.rdkit.knime.types.RDKitAdapterCell;
import org.rdkit.knime.types.RDKitMolCellFactory;
import org.rdkit.knime.types.RDKitMolValueRenderer;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.InputDataInfo.EmptyCellException;
import org.rdkit.knime.util.SettingsUtils;

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

	/** Settings model for the column name of the new column to be added to the output table. */
	private final SettingsModelString m_modelNewColumnName =
			registerSettings(Molecule2RDKitConverterNodeDialog.createNewColumnNameModel());

	/** Settings model for the option to remove the source column from the output table. */
	private final SettingsModelBoolean m_modelRemoveSourceColumns =
			registerSettings(Molecule2RDKitConverterNodeDialog.createRemoveSourceColumnsOptionModel());

	/** Settings model for the option to split output tables to two (second contains bad rows).*/
	private final SettingsModelString m_modelSeparateFails =
			registerSettings(Molecule2RDKitConverterNodeDialog.createSeparateRowsModel());

	/** Settings model for the option to compute coordinates. */
	private final SettingsModelBoolean m_modelGenerateCoordinates =
			registerSettings(Molecule2RDKitConverterNodeDialog.createGenerateCoordinatesModel());

	/** Settings model for the option to force computation of coordinates. */
	private final SettingsModelBoolean m_modelForceGenerateCoordinates =
			registerSettings(Molecule2RDKitConverterNodeDialog.
					createForceGenerateCoordinatesModel(m_modelGenerateCoordinates));

	/** Settings model for the option to suppress sanitizing a molecule. */
	private final SettingsModelBoolean m_modelQuickAndDirty =
			registerSettings(Molecule2RDKitConverterNodeDialog.
					createQuickAndDirtyModel());

	/** Settings model for the option to do aromatization (depends on quick and dirty setting). */
	private final SettingsModelBoolean m_modelAromatization =
			registerSettings(Molecule2RDKitConverterNodeDialog.
					createAromatizationModel(m_modelQuickAndDirty));

	/** Settings model for the option to do stereo chemistry (depends on quick and dirty setting). */
	private final SettingsModelBoolean m_modelStereoChem =
			registerSettings(Molecule2RDKitConverterNodeDialog.
					createStereochemistryModel(m_modelQuickAndDirty));

	//
	// Internals
	//

	/** This variable is used during execution for performance reasons. */
	private boolean m_bIsSmiles = false;

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
		listValueClasses.add(SdfValue.class);

		// Auto guess the input column if not set - fails if no compatible column found
		SettingsUtils.autoGuessColumn(inSpecs[0], m_modelInputColumnName, listValueClasses, 0,
				"Auto guessing: Using column %COLUMN_NAME%.",
				"Neither Smiles nor SDF compatible column in input table.",
				getWarningConsolidator());

		// Determines, if the input column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[0], m_modelInputColumnName, listValueClasses,
				"Input column has not been specified yet.",
				"Smiles or SDF compatible input column %COLUMN_NAME% does not exist. Has the input table changed?");

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
					SmilesValue.class, SdfValue.class);
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

			// Append result column(s)
			newColSpecs.addAll(Arrays.asList(createOutputFactory(null).getColumnSpecs()));
			spec = new DataTableSpec(
					newColSpecs.toArray(new DataColumnSpec[newColSpecs.size()]));
			break;

		case 1:
			// Second table has the same structure as input table
			spec = inSpecs[0];
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
		// Generate column specs for the output table columns produced by this factory
		final DataColumnSpec[] arrOutputSpec = new DataColumnSpec[1]; // We have only one output column
		arrOutputSpec[0] = new DataColumnSpecCreator(
				m_modelNewColumnName.getStringValue().trim(), RDKitAdapterCell.RAW_TYPE)
		.createSpec();

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
				DataCell result = null;
				ROMol mol = null;
				String smiles = null;
				final boolean sanitize = !m_modelQuickAndDirty.getBooleanValue();
				Exception excParsing = null;

				try {
					if (m_bIsSmiles) {
						final String value = arrInputDataInfo[INPUT_COLUMN_MOL].getSmiles(row);
						mol = markForCleanup(RWMol.MolFromSmiles(value, 0, sanitize), lUniqueWaveId);
						smiles = value;
					}
					else {
						final String value = arrInputDataInfo[INPUT_COLUMN_MOL].getSdfValue(row);
						mol = markForCleanup(RWMol.MolFromMolBlock(value, sanitize), lUniqueWaveId);
					}
				}
				catch (final EmptyCellException excEmpty) {
					// If the cell is empty an exception is thrown by .getXXX(row), which is rethrown here
					// and caught in the factory. The factory will deliver empty cells due to the InputDataInfo setup,
					// see also in createInputDataInfos(...).
					throw excEmpty;
				}
				catch (final Exception exc) {
					excParsing = exc;
				}

				if (mol == null) {
					final StringBuilder error = new StringBuilder()
					.append(excParsing != null ? excParsing.getClass().getSimpleName() :
						"Error parsing ").append(m_bIsSmiles ? "SMILES" : "SDF");
					throw new RuntimeException(error.toString());
				}
				else {
					if (!sanitize){
						RDKFuncs.cleanUp((RWMol)mol);
						mol.updatePropertyCache(false);
						RDKFuncs.symmetrizeSSSR(mol);

						if (m_modelAromatization.getBooleanValue()) {
							RDKFuncs.Kekulize((RWMol)mol);
							RDKFuncs.setAromaticity((RWMol)mol);
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

					if (m_modelGenerateCoordinates.getBooleanValue()) {
						if (m_modelForceGenerateCoordinates.getBooleanValue() || mol.getNumConformers() == 0) {
							RDKitMolValueRenderer.compute2DCoords(mol);
						}
					}

					result = RDKitMolCellFactory.createRDKitAdapterCell(mol, smiles);
				}

				return new DataCell[] { result };
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
		final DataTableSpec[] arrOutSpecs = getOutputTableSpecs(inData);

		// Contains the rows with the result column
		final BufferedDataContainer port0 = exec.createDataContainer(arrOutSpecs[0]);

		// Contains the input rows if result computation fails
		final BufferedDataContainer port1 = exec.createDataContainer(arrOutSpecs[1]);

		// Get settings and define data specific behavior
		final int iInputIndex = arrInputDataInfo[0][INPUT_COLUMN_MOL].getColumnIndex();
		m_bIsSmiles = arrInputDataInfo[0][INPUT_COLUMN_MOL].isCompatibleOrAdaptable(SmilesValue.class);
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
				if (arrResults[0].isMissing() && bSplitBadRowsToPort1) {
					port1.addRowToTable(row);
				}
				else {
					port0.addRowToTable(AbstractRDKitCellFactory.mergeDataCells(row, arrResults,
							m_modelRemoveSourceColumns.getBooleanValue() ? iInputIndex : -1));
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
}
