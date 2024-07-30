/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2012-2023
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
package org.rdkit.knime.nodes.highlightingatoms;

import org.RDKit.Int_Vect;
import org.RDKit.ROMol;
import org.RDKit.RWMol;
import org.knime.base.data.xml.SvgCell;
import org.knime.base.data.xml.SvgCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.AbstractRDKitCalculatorNodeModel;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.types.RDKitMolValueRenderer;
import org.rdkit.knime.types.preferences.RDKitDepicterPreferencePage;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class implements the node model of the RDKitHighlightingAtoms node
 * providing calculations based on the open source RDKit library. Creates a SVG
 * column showing a molecule with highlighted atoms based on information in the
 * input table. A molecule column as well as a column with a list of the atoms
 * to be highlighted needs to be provided.
 * 
 * @author Manuel Schwarze
 */
@Deprecated
public class RDKitHighlightingAtomsNodeModel extends AbstractRDKitCalculatorNodeModel {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitHighlightingAtomsNodeModel.class);

	/** Empty atom list to be used if an empty atom list cell is encountered. */
	protected static final Int_Vect EMPTY_ATOM_LIST = new Int_Vect(0);

	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;

	/** Input data info index for Atom List. */
	protected static final int INPUT_COLUMN_ATOM_LIST = 1;

	//
	// Members
	//

	/** Settings model for the column name of the input molecule column. */
	private final SettingsModelString m_modelInputMolColumnName = registerSettings(RDKitHighlightingAtomsNodeDialog
			.createInputMolColumnNameModel());

	/**
	 * Settings model for the column name of the input column with the list of
	 * atoms.
	 */
	private final SettingsModelString m_modelInputAtomListColumnName = registerSettings(RDKitHighlightingAtomsNodeDialog
			.createInputAtomListColumnNameModel());

	/**
	 * Settings model for the column name of the new column to be added to the
	 * output table.
	 */
	private final SettingsModelString m_modelNewColumnName = registerSettings(RDKitHighlightingAtomsNodeDialog
			.createNewColumnNameModel());

	//
	// Constructor
	//

	/**
	 * Create new node model with one data in- and one out-port.
	 */
	RDKitHighlightingAtomsNodeModel() {
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

		// Auto guess the input mol column if not set - fails if no compatible
		// column found
		SettingsUtils.autoGuessColumn(inSpecs[0], m_modelInputMolColumnName,
				RDKitMolValue.class, 0,
				"Auto guessing: Using column %COLUMN_NAME%.",
				"No RDKit Mol, SMILES or SDF compatible column in input table. Use the \"RDKit from Molecule\" "
						+ "node to convert SMARTS.",
						getWarningConsolidator());

		// Auto guess the input atom list column if not set - fails if no
		// compatible column found
		SettingsUtils
		.autoGuessColumn(
				inSpecs[0],
				m_modelInputAtomListColumnName,
				CollectionDataValue.class,
				0,
				"Auto guessing: Using column %COLUMN_NAME%.",
				"No Collection type column found in input table, which would contain atoms to be highlighted.",
				getWarningConsolidator());

		// Determines, if the input mol column exists - fails if it does not
		SettingsUtils
		.checkColumnExistence(inSpecs[0], m_modelInputMolColumnName,
				RDKitMolValue.class,
				"Input column has not been specified yet.",
				"Input column %COLUMN_NAME% does not exist. Has the input table changed?");

		// Determines, if the input atom list column exists - fails if it does
		// not
		SettingsUtils
		.checkColumnExistence(inSpecs[0],
				m_modelInputAtomListColumnName,
				CollectionDataValue.class,
				"Input column has not been specified yet.",
				"Input column %COLUMN_NAME% does not exist. Has the input table changed?");

		// Auto guess the new column name and make it unique
		final String strInputMolColumnName = m_modelInputMolColumnName
				.getStringValue();
		SettingsUtils.autoGuessColumnName(inSpecs[0], null, null,
				m_modelNewColumnName, strInputMolColumnName
				+ " (Highlighted Atoms)");

		// Determine, if the new column name has been set and if it is really
		// unique
		SettingsUtils
		.checkColumnNameUniqueness(inSpecs[0], null, null,
				m_modelNewColumnName,
				"Output column has not been specified yet.",
				"The name %COLUMN_NAME% of the new column exists already in the input.");

		// Consolidate all warnings and make them available to the user
		generateWarnings();

		// Generate output specs
		return getOutputTableSpecs(inSpecs);
	}

	/**
	 * This implementation generates input data info object for the input mol
	 * column and connects it with the information coming from the appropriate
	 * setting model. {@inheritDoc}
	 */
	@Override
	protected InputDataInfo[] createInputDataInfos(final int inPort,
			final DataTableSpec inSpec) throws InvalidSettingsException {

		InputDataInfo[] arrDataInfo = null;

		// Specify input of table 1
		if (inPort == 0) {
			arrDataInfo = new InputDataInfo[2]; // We have two input columns
			arrDataInfo[INPUT_COLUMN_MOL] = new InputDataInfo(inSpec, null,
					m_modelInputMolColumnName, "molecule",
					InputDataInfo.EmptyCellPolicy.DeliverEmptyRow, null,
					RDKitMolValue.class);
			arrDataInfo[INPUT_COLUMN_ATOM_LIST] = new InputDataInfo(inSpec, null,
					m_modelInputAtomListColumnName, "atom list",
					InputDataInfo.EmptyCellPolicy.TreatAsNull,
					null, CollectionDataValue.class);
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

		// Specify output of table 1
		if (outPort == 0) {
			// Allocate space for all factories (usually we have only one)
			arrOutputFactories = new AbstractRDKitCellFactory[1];

			// Factory 1:
			// ==========
			// Generate column specs for the output table columns produced by this factory
			final DataColumnSpec[] arrOutputSpec = new DataColumnSpec[1]; // We have only one output column
			arrOutputSpec[0] = new DataColumnSpecCreator(
					m_modelNewColumnName.getStringValue(), SvgCell.TYPE)
			.createSpec();

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
					Int_Vect vectInt  = markForCleanup(arrInputDataInfo[INPUT_COLUMN_ATOM_LIST].getRDKitIntegerVector(row), lUniqueWaveId);

					// Add 2D coordinates if there is no conformer yet (e.g. if RDKit molecule was created from a SMILES)
					// This is necessary for the RDKit changes in the SVG generation
					if (mol.getNumConformers() == 0) {
						RDKitMolValueRenderer.compute2DCoords(mol,
							RDKitDepicterPreferencePage.isUsingCoordGen(),
							RDKitDepicterPreferencePage.isNormalizeDepictions());
					} else {
						RDKitMolValueRenderer.reapplyWedgingAndNormalizeAccordingToPrefs(mol);
					}

					String xmlSvg = null;

					if (vectInt == null) {
						LOGGER.warn("Encountered empty atom list in row '" + row.getKey() + "'");
						warnings.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(), "Encountered empty atom list cell. Using empty atom list.");
						vectInt = EMPTY_ATOM_LIST; // This must not be cleaned up, it is a constant
					}

					xmlSvg = mol.ToSVG(vectInt, 8, 50);

					if (xmlSvg != null && !xmlSvg.trim().isEmpty()) {
						// Important: Use the factory here, because using the normal SvgCell contructor causes
						//			  OutOfMemory exceptions when processing many SVG structures.
						outputCell = SvgCellFactory.create(xmlSvg);
					}
					else {
						outputCell = DataType.getMissingCell();
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
	 * {@inheritDoc} This implementation removes additionally the compound
	 * source column, if specified in the settings.
	 */
	@Override
	protected ColumnRearranger createColumnRearranger(final int outPort,
			final DataTableSpec inSpec) throws InvalidSettingsException {
		// Perform normal work
		final ColumnRearranger result = super.createColumnRearranger(outPort, inSpec);
		return result;
	}
}
