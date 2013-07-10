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
package org.rdkit.knime.nodes.diversitypicker;

import java.util.ArrayList;
import java.util.List;

import org.RDKit.EBV_Vect;
import org.RDKit.ExplicitBitVect;
import org.RDKit.Int_Vect;
import org.RDKit.RDKFuncs;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;
import org.rdkit.knime.nodes.AbstractRDKitSplitterNodeModel;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsUtils;

/**
 * This class implements the node model of the RDKitDiversityPicker node
 * providing filtering based on the open source RDKit library.
 * 
 * @author Greg Landrum
 * @author Manuel Schwarze
 */
public class RDKitDiversityPickerNodeModel extends AbstractRDKitSplitterNodeModel {

	//
	// Constants
	//

	/** Input data info index for fingerprint value. */
	protected static final int INPUT_COLUMN_FINGERPRINT = 0;

	//
	// Members
	//

	/** Settings model for the column name of the input column. */
	private final SettingsModelString m_modelInputColumnName =
			registerSettings(RDKitDiversityPickerNodeDialog.createInputColumnNameModel(), "input_column", "first_column");
	// Accepts also old deprecated key

	/** Settings model for the value of the number to pick. */
	private final SettingsModelInteger m_modelNumberToPick =
			registerSettings(RDKitDiversityPickerNodeDialog.createNumberToPickModel());

	/** Settings model for the value of the random seed. */
	private final SettingsModelInteger m_randomSeed =
			registerSettings(RDKitDiversityPickerNodeDialog.createRandomSeedModel(), true);

	/** Pre-processing result to tell what columns to keep. */
	private transient ExplicitBitVect m_ebvRowsToKeep;

	//
	// Constructor
	//

	/**
	 * Create new node model with one data in- and one outport.
	 */
	RDKitDiversityPickerNodeModel() {
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
		SettingsUtils.autoGuessColumn(inSpecs[0], m_modelInputColumnName, BitVectorValue.class, 0,
				"Auto guessing: Using column %COLUMN_NAME%.",
				"No fingerprints (Bit Vector compatible column) in input table.", getWarningConsolidator());

		// Determines, if the input column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[0], m_modelInputColumnName, BitVectorValue.class,
				"Input column has not been specified yet.",
				"Input column %COLUMN_NAME% does not exist. Has the input table changed?");

		// Consolidate all warnings and make them available to the user
		generateWarnings();

		// Generate output specs
		return getOutputTableSpecs(inSpecs);
	}

	/**
	 * {@inheritDoc}
	 * This implementation generates input data info object for the input column
	 * and connects it with the information coming from the appropriate setting model.
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected InputDataInfo[] createInputDataInfos(final int inPort, final DataTableSpec inSpec)
			throws InvalidSettingsException {

		InputDataInfo[] arrDataInfo = null;

		// Specify input of table 1
		if (inPort == 0) {
			arrDataInfo = new InputDataInfo[1]; // We have only one input column
			arrDataInfo[INPUT_COLUMN_FINGERPRINT] = new InputDataInfo(inSpec, m_modelInputColumnName,
					InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
					BitVectorValue.class);
		}

		return (arrDataInfo == null ? new InputDataInfo[0] : arrDataInfo);
	}

	/**
	 * {@inheritDoc}
	 * This implementation returns 0.8 (80%).
	 */
	@Override
	protected double getPreProcessingPercentage() {
		return 0.80d;
	}

	/**
	 * {@inheritDoc}
	 * This implementation calculates what rows to keep.
	 */
	@Override
	protected void preProcessing(final BufferedDataTable[] inData,
			final InputDataInfo[][] arrInputDataInfo, final ExecutionContext exec)
					throws Exception {
		// Reset old intermediate values
		m_ebvRowsToKeep = null;

		// Create sub execution contexts for pre-processing steps
		final ExecutionContext subExecReadingFingerprints = exec.createSubExecutionContext(0.33d);
		final ExecutionContext subExecCheckDiversity = exec.createSubExecutionContext(0.33d);
		final ExecutionContext subExecProcessingDiversity = exec.createSubExecutionContext(0.34d);

		// 1. Get all fingerprints in a form that we can process further
		final int iRowCount = inData[0].getRowCount();
		final List<Integer> listIndicesUsed = new ArrayList<Integer>();
		final EBV_Vect vFingerprints = markForCleanup(new EBV_Vect());

		int iRowIndex = 0;
		final RowIterator it = inData[0].iterator();
		while (it.hasNext()) {
			final DataRow row = it.next();
			final ExplicitBitVect expBitVector = markForCleanup(arrInputDataInfo[0][INPUT_COLUMN_FINGERPRINT].getExplicitBitVector(row));
			if (expBitVector != null) {
				listIndicesUsed.add(iRowIndex);
				vFingerprints.add(expBitVector);
			}

			// Every 20 iterations report progress and check for cancel
			if (iRowIndex % 20 == 0) {
				AbstractRDKitNodeModel.reportProgress(subExecReadingFingerprints, iRowIndex, iRowCount, row, " - Reading fingerprints");
			}

			iRowIndex++;
		}
		subExecReadingFingerprints.setProgress(1.0d);

		// Check, if parameters of user make sense based on the found fingerprints
		if (listIndicesUsed.size() < m_modelNumberToPick.getIntValue()) {
			throw new InvalidSettingsException("Number of diverse points requested ("+ m_modelNumberToPick.getIntValue()
					+ ") exceeds number of valid fingerprints (" + listIndicesUsed.size() + ")");
		}

		// 2. Doing diversity pick from fingerprints
		subExecCheckDiversity.setProgress(0.25d, "Doing diversity pick");
		final Int_Vect intVector = markForCleanup(RDKFuncs.pickUsingFingerprints(vFingerprints,
				m_modelNumberToPick.getIntValue(), m_randomSeed.getIntValue()));
		subExecCheckDiversity.setProgress(1.0d);
		subExecCheckDiversity.checkCanceled();

		// 3. Store, what rows to keep - FIX: there has to be a better way to do this
		m_ebvRowsToKeep = markForCleanup(new ExplicitBitVect(iRowCount));
		final int iDiversityCount = (int)intVector.size();
		for(int i = 0; i < iDiversityCount; i++) {
			m_ebvRowsToKeep.setBit(listIndicesUsed.get(intVector.get(i)));
			// Every 20 iterations report progress and check for cancel
			if (i % 20 == 0) {
				AbstractRDKitNodeModel.reportProgress(subExecProcessingDiversity, i, iDiversityCount,
						null, " - Processing diversity results");
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanupIntermediateResults() {
		m_ebvRowsToKeep = null;
	}

	/**
	 * {@inheritDoc}
	 * This implementation filters out all rows, which are not contained in the rowsToKeep member variable, which
	 * was calculated during the pre-processing phase.

	 * @param arrInputDataInfo Input data information about all columns of the table at the input port. Not used
	 * 		in this implementation.
	 * @param iUniqueWaveId A unique id that should be used for marking RDKit objects for cleanup. Marked
	 * 		objects will be cleaned up automatically at the end of this call. If this is not wanted,
	 * 		the objects should either not be marked for cleanup or they should be marked without an id,
	 * 		which would lead to a cleanup at the end of the entire execution process. Not used in this implementation.
	 * 
	 * @return 0 to keep the row, or -1 if row shall be filtered out completely.
	 */
	@Override
	public int determineTargetTable(final int iInPort, final int iRowIndex, final DataRow row, final InputDataInfo[] arrInputDataInfo, final int iUniqueWaveId) {
		return (m_ebvRowsToKeep.getBit(iRowIndex) ? 0 : -1);
	}
}
