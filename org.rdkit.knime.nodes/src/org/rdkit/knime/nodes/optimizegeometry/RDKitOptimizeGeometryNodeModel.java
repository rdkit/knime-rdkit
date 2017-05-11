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
package org.rdkit.knime.nodes.optimizegeometry;

import org.RDKit.DistanceGeom;
import org.RDKit.EmbedParameters;
import org.RDKit.ForceField;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.StreamableOperator;
import org.rdkit.knime.nodes.AbstractRDKitCalculatorNodeModel;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.types.RDKitAdapterCell;
import org.rdkit.knime.types.RDKitMolCellFactory;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsModelEnumeration;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class implements the node model of the RDKitOptimizeGeometry node
 * providing calculations based on the open source RDKit library.
 * 
 * @author Manuel Schwarze
 */
public class RDKitOptimizeGeometryNodeModel extends AbstractRDKitCalculatorNodeModel {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitOptimizeGeometryNodeModel.class);


	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;

	//
	// Members
	//

	/** Settings model for the column name of the input column. */
	private final SettingsModelString m_modelInputColumnName =
			registerSettings(RDKitOptimizeGeometryNodeDialog.createInputColumnNameModel());

	/** Settings model for the force field. */
	private final SettingsModelEnumeration<ForceFieldType> m_modelForceField =
			registerSettings(RDKitOptimizeGeometryNodeDialog.createForceFieldModel());

	/** Settings model for the column name of the new updated molecule column to be added to the output table. */
	private final SettingsModelString m_modelNewMoleculeColumnName =
			registerSettings(RDKitOptimizeGeometryNodeDialog.createNewMoleculeColumnNameModel());

	/** Settings model for the option to remove the source column from the output table. */
	private final SettingsModelBoolean m_modelRemoveSourceColumns =
			registerSettings(RDKitOptimizeGeometryNodeDialog.createRemoveSourceColumnsOptionModel());

	/** Settings model for the column name of the new converge column to be added to the output table. */
	private final SettingsModelString m_modelNewConvergeColumnName =
			registerSettings(RDKitOptimizeGeometryNodeDialog.createNewConvergeColumnNameModel());

	/** Settings model for the column name of the new energy column to be added to the output table. */
	private final SettingsModelString m_modelNewEnergyColumnName =
			registerSettings(RDKitOptimizeGeometryNodeDialog.createNewEnergyColumnNameModel());

	/** Settings model for the advanced option iterations. */
	private final SettingsModelInteger m_modelIterations =
			registerSettings(RDKitOptimizeGeometryNodeDialog.createIterationsModel());

	/** Settings model for the advanced option to remove starting coordinates before optimizing the molecule. */
	private final SettingsModelBoolean m_modelRemoveStartingCoordinates =
			registerSettings(RDKitOptimizeGeometryNodeDialog.createRemoveStartingCoordinatesOptionModel(), true);

	//
	// Constructor
	//

	/**
	 * Create new node model with one data in- and one out-port.
	 */
	RDKitOptimizeGeometryNodeModel() {
		super(1, 1);
	}

	//
	// Protected Methods
	//

   /**
    * Enable distribution and streaming for this node.
    * {@inheritDoc}
    */
   @Override
   public StreamableOperator createStreamableOperator(PartitionInfo partitionInfo, PortObjectSpec[] inSpecs)
         throws InvalidSettingsException {
      return createStreamableOperatorForCalculator(partitionInfo, inSpecs);
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

		// Auto guess the new column names and make them unique
		final String strInputColumnName = m_modelInputColumnName.getStringValue();
		SettingsUtils.autoGuessColumnName(inSpecs[0], null,
				(m_modelRemoveSourceColumns.getBooleanValue() ?
						new String[] { strInputColumnName } : null),
						m_modelNewMoleculeColumnName, strInputColumnName + " (Optimized Geometry)");
		SettingsUtils.autoGuessColumnName(inSpecs[0],
				new String[] { m_modelNewMoleculeColumnName.getStringValue() },
				(m_modelRemoveSourceColumns.getBooleanValue() ?
						new String[] { strInputColumnName } : null),
						m_modelNewConvergeColumnName, "Converged");
		SettingsUtils.autoGuessColumnName(inSpecs[0],
				new String[] { m_modelNewMoleculeColumnName.getStringValue(), m_modelNewConvergeColumnName.getStringValue() },
				(m_modelRemoveSourceColumns.getBooleanValue() ?
						new String[] { strInputColumnName } : null),
						m_modelNewEnergyColumnName, "Energy");

		// Determine, if the new column names have been set and if they are really unique
		SettingsUtils.checkColumnNameUniqueness(inSpecs[0], null,
				(m_modelRemoveSourceColumns.getBooleanValue() ? new String[] {
					m_modelInputColumnName.getStringValue() } : null),
					m_modelNewMoleculeColumnName,
					"Molecule output column has not been specified yet.",
				"The name %COLUMN_NAME% of the new molecule column exists already in the input.");
		SettingsUtils.checkColumnNameUniqueness(inSpecs[0],
				new String[] { m_modelNewMoleculeColumnName.getStringValue() },
				(m_modelRemoveSourceColumns.getBooleanValue() ? new String[] {
					m_modelInputColumnName.getStringValue() } : null),
					m_modelNewConvergeColumnName,
					"Converge output column has not been specified yet.",
				"The name %COLUMN_NAME% of the new converge column exists already in the input.");
		SettingsUtils.checkColumnNameUniqueness(inSpecs[0],
				new String[] { m_modelNewMoleculeColumnName.getStringValue(), m_modelNewConvergeColumnName.getStringValue() },
				(m_modelRemoveSourceColumns.getBooleanValue() ? new String[] {
					m_modelInputColumnName.getStringValue() } : null),
					m_modelNewEnergyColumnName,
					"Energy output column has not been specified yet.",
				"The name %COLUMN_NAME% of the new energy column exists already in the input.");

		// Force field check
		if (m_modelForceField.getValue() == null) {
			throw new IllegalArgumentException("No force field type specified yet.");
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
			final DataColumnSpec[] arrOutputSpec = new DataColumnSpec[3]; // We have three output columns
			arrOutputSpec[0] = new DataColumnSpecCreator(
					m_modelNewMoleculeColumnName.getStringValue(), RDKitAdapterCell.RAW_TYPE)
			.createSpec();
			arrOutputSpec[1] = new DataColumnSpecCreator(
					m_modelNewConvergeColumnName.getStringValue(), BooleanCell.TYPE)
			.createSpec();
			arrOutputSpec[2] = new DataColumnSpecCreator(
					m_modelNewEnergyColumnName.getStringValue(), DoubleCell.TYPE)
			.createSpec();

			final WarningConsolidator warnings = getWarningConsolidator();
			final ForceFieldType forceFieldType = m_modelForceField.getValue();
			final int iIterations = m_modelIterations.getIntValue();
			final DataCell missingCell = DataType.getMissingCell();
			final boolean bRemoveCoordinates = m_modelRemoveStartingCoordinates.getBooleanValue();

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
					DataCell outputMolCell = null;
					DataCell outputConvergeCell = null;
					DataCell outputEnergyCell = null;

					// Calculate the new cells
					final ROMol mol = markForCleanup(arrInputDataInfo[INPUT_COLUMN_MOL].getROMol(row), lUniqueWaveId);

					// Remove starting coordinates, if desired
					if (bRemoveCoordinates) {
						mol.clearConformers();
					}

					// Check, if 3D coordinates exist, otherwise create them
					if (mol.getNumConformers() == 0) {
						EmbedParameters pms = RDKFuncs.getETKDG();
						pms.setRandomSeed(42);
						DistanceGeom.EmbedMolecule(mol, pms);
					}

					// Calculate force field
					int iConverge = -1;
					double dEnergy = 0.0d;
					if(mol.getNumConformers()>=1){
						final ForceField forceField = markForCleanup(forceFieldType.generateForceField(mol), lUniqueWaveId);
	
						if (forceField == null) {
							warnings.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(), "Force field creation failed. Creating empty output.");
						}
						else {
							forceField.initialize();
							if (iIterations > 0) {
								iConverge = forceField.minimize(iIterations);
							}
							else {
								iConverge = 1; // Translates to false later on
							}
							dEnergy = forceField.calcEnergy();
						}
					} else {
						warnings.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(), "Molecule has no coordinates. Creating empty output.");
					}

					// Create output cells
					if (iConverge == -1) {
						outputMolCell = missingCell;
						outputConvergeCell = missingCell;
						outputEnergyCell = missingCell;
					}
					else {
						if (mol.getNumAtoms() > 0) {
							outputMolCell = RDKitMolCellFactory.createRDKitAdapterCell(mol);
						}
						else {
							outputMolCell = missingCell;
						}

						outputConvergeCell = iConverge == 0 ? BooleanCell.TRUE : BooleanCell.FALSE;
						outputEnergyCell = new DoubleCell(dEnergy);
					}

					return new DataCell[] { outputMolCell, outputConvergeCell, outputEnergyCell };
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
