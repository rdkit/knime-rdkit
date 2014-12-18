/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2014
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
package org.rdkit.knime.nodes.mcs;

import org.RDKit.ROMol;
import org.RDKit.ROMol_Vect;
import org.knime.chem.types.SmartsCell;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsModelEnumeration;
import org.rdkit.knime.util.SettingsUtils;

/**
 * This class implements the node model of the RDKitMCS node
 * providing calculations based on the open source RDKit library.
 * 
 * @author Manuel Schwarze
 */
public class RDKitMCSNodeModel extends AbstractRDKitNodeModel {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitMCSNodeModel.class);

	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;

	//
	// Members
	//

	/** Settings model for the column name of the input column. */
	private final SettingsModelString m_modelInputColumnName =
			registerSettings(RDKitMCSNodeDialog.createInputColumnNameModel());

	/** Settings model for . */
	private final SettingsModelDoubleBounded m_modelThreshold =
			registerSettings(RDKitMCSNodeDialog.createThresholdModel());

	/** Settings model for . */
	private final SettingsModelBoolean m_modelRingMatchesRingOnlyOption =
			registerSettings(RDKitMCSNodeDialog.createRingMatchesRingOnlyOptionModel());

	/** Settings model for . */
	private final SettingsModelBoolean m_modelCompleteRingsOnlyOption =
			registerSettings(RDKitMCSNodeDialog.createCompleteRingsOnlyOptionModel());

	/** Settings model for . */
	private final SettingsModelBoolean m_modelMatchValencesOption =
			registerSettings(RDKitMCSNodeDialog.createMatchValencesOptionModel());

	/** Settings model for . */
	private final SettingsModelEnumeration<AtomComparison> m_modelAtomComparison =
			registerSettings(RDKitMCSNodeDialog.createAtomComparisonModel());

	/** Settings model for . */
	private final SettingsModelEnumeration<BondComparison> m_modelBondComparison =
			registerSettings(RDKitMCSNodeDialog.createBondComparisonModel());

	/** Settings model for . */
	private final SettingsModelIntegerBounded m_modelTimeout =
			registerSettings(RDKitMCSNodeDialog.createTimeoutModel());

	//
	// Constructor
	//

	/**
	 * Create new node model with one data in- and one out-port.
	 */
	RDKitMCSNodeModel() {
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
				"No RDKit Mol, SMILES or SDF compatible column in input table. Use the \"RDKit from Molecule\" " +
						"node to convert SMARTS.", getWarningConsolidator());

		// Determines, if the input column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[0], m_modelInputColumnName, RDKitMolValue.class,
				"Input column has not been specified yet.",
				"Input column %COLUMN_NAME% does not exist. Has the input table changed?");

		final double dThreshold = m_modelThreshold.getDoubleValue();
		if (dThreshold <= 0.0d || dThreshold > 1.0d) {
			throw new InvalidSettingsException("Threshold value must be > 0.0 and <= 1.0.");
		}

		if (m_modelTimeout.getIntValue() > 300) {
			getWarningConsolidator().saveWarning(
					"The configured timeout is greater than 5 minutes. Please note that\n" +
							"the MCS calculation continues in the background and occupies the processor\n" +
					"until it is done or that timeout is hit, even after cancelling the node.");
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
					InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
					RDKitMolValue.class);
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
			// Define output table
			spec = new DataTableSpec("MCS",
					new DataColumnSpecCreator("MCS", SmartsCell.TYPE).createSpec(),
					new DataColumnSpecCreator("Number of Atoms", IntCell.TYPE).createSpec(),
					new DataColumnSpecCreator("Number of Bonds", IntCell.TYPE).createSpec(),
					new DataColumnSpecCreator("Timed Out", BooleanCell.TYPE).createSpec());
			break;
		}

		return spec;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] processing(final BufferedDataTable[] inData, final InputDataInfo[][] arrInputDataInfo,
			final ExecutionContext exec) throws Exception {
		final DataTableSpec[] arrOutSpecs = getOutputTableSpecs(inData);

		// Contains the rows with the result column
		final BufferedDataContainer newTableData = exec.createDataContainer(arrOutSpecs[0]);

		// Get settings and define data specific behavior
		final int iTotalRowCount = inData[0].getRowCount();

		// Get all molecules to be used as input
		int rowIndex = 0;
		final ROMol_Vect mols = new ROMol_Vect();
		final ExecutionContext execSub1 = exec.createSubExecutionContext(0.02d);

		for (final CloseableRowIterator i = inData[0].iterator(); i.hasNext(); rowIndex++) {
			final DataRow row = i.next();

			final ROMol mol = markForCleanup(arrInputDataInfo[0][INPUT_COLUMN_MOL].getROMol(row));

			// We use only cells, which are not missing
			if (mol != null) {
				mols.add(mol);
			}

			// Every 20 iterations check cancellation status and report progress
			if (rowIndex % 20 == 0) {
				AbstractRDKitNodeModel.reportProgress(execSub1, rowIndex, iTotalRowCount, row, " - Gathering molecules");
			}
		}

		execSub1.setProgress(1.0d);

		// Calculate MCS
		final ExecutionContext execSub2 = exec.createSubExecutionContext(0.98d);

		final DataCell[] arrResults = MCSUtils.calculateMCS(mols, m_modelThreshold.getDoubleValue(),
				m_modelRingMatchesRingOnlyOption.getBooleanValue(), m_modelCompleteRingsOnlyOption.getBooleanValue(),
				m_modelMatchValencesOption.getBooleanValue(), m_modelAtomComparison.getValue(),
				m_modelBondComparison.getValue(), m_modelTimeout.getIntValue(), execSub2);

		newTableData.addRowToTable(new DefaultRow("MCS",
				arrResults[MCSUtils.SMARTS_INDEX],
				arrResults[MCSUtils.ATOM_NUMBER_INDEX],
				arrResults[MCSUtils.BOND_NUMBER_INDEX],
				arrResults[MCSUtils.TIMED_OUT_INDEX]));

		exec.checkCanceled();
		exec.setProgress(1.0, "Finished Processing");

		// Generate warning, if no MCS was found
		if (arrResults[MCSUtils.SMARTS_INDEX] == null || arrResults[MCSUtils.SMARTS_INDEX].isMissing()) {
			if (mols.size() > 0) {
				getWarningConsolidator().saveWarning("No MCS found - Created empty cells.");
			}
			else {
				getWarningConsolidator().saveWarning("No input molecules found - Created empty cells.");
			}
		}
		else if (arrResults[MCSUtils.TIMED_OUT_INDEX] != null &&
				!arrResults[MCSUtils.TIMED_OUT_INDEX].isMissing() &&
				((BooleanCell)arrResults[MCSUtils.TIMED_OUT_INDEX]).getBooleanValue() == true) {
			getWarningConsolidator().saveWarning("The MCS calculation timed out.");
		}

		newTableData.close();

		return new BufferedDataTable[] { newTableData.getTable() };
	}
}
