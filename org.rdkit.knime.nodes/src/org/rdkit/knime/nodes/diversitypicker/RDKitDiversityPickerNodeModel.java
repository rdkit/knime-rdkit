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
package org.rdkit.knime.nodes.diversitypicker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import org.RDKit.EBV_Vect;
import org.RDKit.ExplicitBitVect;
import org.RDKit.Int_Vect;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.RDKit.RWMol;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.util.MultiThreadWorker;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;
import org.rdkit.knime.nodes.AbstractRDKitSplitterNodeModel;
import org.rdkit.knime.nodes.rdkfingerprint.DefaultFingerprintSettings;
import org.rdkit.knime.nodes.rdkfingerprint.FingerprintSettings;
import org.rdkit.knime.nodes.rdkfingerprint.FingerprintType;
import org.rdkit.knime.properties.FingerprintSettingsHeaderProperty;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.WarningConsolidator;
import org.rdkit.knime.util.WarningConsolidator.Context;

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
	protected static final int INPUT_COLUMN_MAIN = 0;

	/** Input data info index for second optional table that contains either a structure or a fingerprint. */
	protected static final int INPUT_COLUMN_ADDITIONAL = 0;

	/** For convenience reasons we store here acceptable values for the second optional input table. */
	protected static final List<Class<? extends DataValue>> ACCEPTABLE_VALUE_CLASSES = new ArrayList<Class<? extends DataValue>>(2);

	/** The default fingerprint settings to use, if a molecule column is selected from the first input table. */
	protected static final FingerprintSettings DEFAULT_FINGERPRINT_SETTINGS = new DefaultFingerprintSettings(
			"Morgan", FingerprintSettings.UNAVAILABLE,
			FingerprintSettings.UNAVAILABLE, FingerprintSettings.UNAVAILABLE,
			FingerprintSettings.UNAVAILABLE, FingerprintSettings.UNAVAILABLE,
			2048, 2, // NumBits, Radius
			FingerprintSettings.UNAVAILABLE, FingerprintSettings.UNAVAILABLE, false);

	/** Row context for generating warnings, if something is incorrect in table 2. */
	protected static final WarningConsolidator.Context ROW_CONTEXT_TABLE_2 = new Context("rowTable2", "row", "rows", true);

	static {
		ACCEPTABLE_VALUE_CLASSES.add(BitVectorValue.class);
		ACCEPTABLE_VALUE_CLASSES.add(RDKitMolValue.class);
	}

	//
	// Members
	//

	/** Settings model for the column name of the input column. */
	private final SettingsModelString m_modelInputColumnName =
			registerSettings(RDKitDiversityPickerNodeDialog.createInputColumnNameModel(), "input_column", "first_column");
	// Accepts also old deprecated key

	/** Settings model for the optional column name of the input column in the optional second table (molecules or fingerprints). */
	private final SettingsModelString m_modelAdditionalInputColumnName =
			registerSettings(RDKitDiversityPickerNodeDialog.createAdditionalInputColumnNameModel());

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
		super(new PortType[] {
				// Input ports (2nd port is optional)
				PortTypeRegistry.getInstance().getPortType(BufferedDataTable.TYPE.getPortObjectClass(), false),
				PortTypeRegistry.getInstance().getPortType(BufferedDataTable.TYPE.getPortObjectClass(), true) },
				new PortType[] {
				// Output ports
						PortTypeRegistry.getInstance().getPortType(BufferedDataTable.TYPE.getPortObjectClass(), false) 
				});

		registerInputTablesWithSizeLimits(0, 1); // As we pre-calculate fingerprints and store them in a collection we cannot support large tables
		getWarningConsolidator().registerContext(ROW_CONTEXT_TABLE_2);
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

		final WarningConsolidator warnings = getWarningConsolidator();

		// Auto guess the input column if not set - fails if no compatible column found
		SettingsUtils.autoGuessColumn(inSpecs[0], m_modelInputColumnName, ACCEPTABLE_VALUE_CLASSES, 0,
				"Auto guessing: Using column %COLUMN_NAME% in table 1.",
				"No molecules or fingerprints (Bit Vector compatible column) in input table 1.", warnings);

		// Determines, if the input column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[0], m_modelInputColumnName, ACCEPTABLE_VALUE_CLASSES,
				"Input column has not been specified yet.",
				"Input column %COLUMN_NAME% does not exist in table 1. Has the input table 1 changed?");

		// Determines, if fingerprint information is available in table 1
		final String strInputColumn = m_modelInputColumnName.getStringValue();
		FingerprintSettingsHeaderProperty fpSpec1 = null;
		FingerprintSettingsHeaderProperty fpSpec2 = null;

		final DataColumnSpec colSpec1 = inSpecs[0].getColumnSpec(strInputColumn);

		// Check, if we have already a fingerprint column
		if (colSpec1.getType().isCompatible(BitVectorValue.class)) {
			fpSpec1 = new FingerprintSettingsHeaderProperty(inSpecs[0].getColumnSpec(strInputColumn));
			// Treat as if not existing, if the fingerprint is not one of the RDKit generated fingerprints
			if (fpSpec1.getRdkitFingerprintType() == null) {
				fpSpec1 = null;
			}
		}
		else { // Otherwise it must be a molecule column - We will generate fingerprints on the fly based on default settings
			fpSpec1 = new FingerprintSettingsHeaderProperty(DEFAULT_FINGERPRINT_SETTINGS);
		}

		// Checks optional second info table
		if (hasAdditionalInputTable(inSpecs)) {
			final String strAdditionalInputColumn = m_modelAdditionalInputColumnName.getStringValue();

			if (strAdditionalInputColumn == null) {
				warnings.saveWarning("There is no column selected for the second input table. It will be ignored.");
			}
			else {
				// Determines, if the input column exists - fails if it does not
				SettingsUtils.checkColumnExistence(inSpecs[1], m_modelAdditionalInputColumnName,
						ACCEPTABLE_VALUE_CLASSES, null,
						"Input column %COLUMN_NAME% does not exist in the second table. Has the second input table changed?");

				final DataColumnSpec colSpec2 = inSpecs[1].getColumnSpec(strAdditionalInputColumn);

				// Check, if we deal with an additional fingerprint column
				if (colSpec2.getType().isCompatible(BitVectorValue.class)) {
					fpSpec2 = new FingerprintSettingsHeaderProperty(colSpec2);
					// Treat as if not existing, if the fingerprint is not one of the RDKit generated fingerprints
					if (fpSpec2.getRdkitFingerprintType() == null) {
						fpSpec2 = null;
					}

					// Only if both input columns have a fingerprint spec attached compare its settings
					if (fpSpec1 != null && fpSpec2 != null && !fpSpec1.equals(fpSpec2)) {
						if (colSpec1.getType().isCompatible(BitVectorValue.class)) {
							throw new InvalidSettingsException("Fingerprints in table 1 and 2 were generated differently.");
						}
						else {
							throw new InvalidSettingsException("Fingerprints in table 2 are not compatible with " +
									fpSpec1.getStringValue().replaceAll("\n", ", ") + ".");
						}
					}
				}

				// Otherwise it must be a molecule column - Check if we have all information to generate fingerprints on the fly
				else {
					// Determine, if fingerprint setting information is available and compatible in table 1
					boolean bCompatible = true;

					if (fpSpec1 == null || fpSpec1.getRdkitFingerprintType() == null) {
						bCompatible = false;
					}

					// Incompatible, if fingerprints of table 1 are rooted fingerprints
					else if (fpSpec1.isRooted()) {
						bCompatible = false;
					}

					// Incompatible, if fingerprints of table 1 are count-based fingerprints
					else if (fpSpec1.isCountBased()) {
						bCompatible = false;
					}

					// Determine, if fingerprint setting information of table 1 can be used for a new fingerprint
					else {
						final FingerprintType fpType = fpSpec1.getRdkitFingerprintType();

						// Calculate the sample fingerprint
						ROMol mol = null;
						ExplicitBitVect	fingerprint = null;

						try {
							mol = RWMol.MolFromSmiles("CCO", 0, true);
							fingerprint = fpType.calculate(mol, fpSpec1);
						}
						catch (final Exception exc) {
							LOGGER.debug("Unable to calculate fingerprint for sample CCO based on the fingerprint settings found in table 1: " +
									fpSpec1, exc);
							bCompatible = false;
						}
						finally {
							// Delete the molecule manually to free memory quickly
							if (mol != null) {
								mol.delete();
							}
							if (fingerprint != null) {
								fingerprint.delete();
							}
						}
					}

					if (!bCompatible) {
						throw new InvalidSettingsException("Unable to calculate fingerprints for molecules of table 2 " +
								"due to missing or incompatible fingerprint setting information in the fingerprint column of table 1. " +
								"You may either regenerate the table 1 fingerprint with the RDKit Fingerprint Node or " +
								"you pick a compatible fingerprint column in table 2 instead of a molecule column.");
					}
				}
			}
		}

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
	protected InputDataInfo[] createInputDataInfos(final int inPort, final DataTableSpec inSpec)
			throws InvalidSettingsException {

		InputDataInfo[] arrDataInfo = null;

		// Specify input of table 1
		if (inPort == 0) {
			arrDataInfo = new InputDataInfo[1]; // We have only one input column
			arrDataInfo[INPUT_COLUMN_MAIN] = new InputDataInfo(inSpec, m_modelInputColumnName,
					InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
					BitVectorValue.class, RDKitMolValue.class);
		}

		// Specify input of optional additional table
		else if (inPort == 1 && inSpec != null && m_modelAdditionalInputColumnName.getStringValue() != null) {
			arrDataInfo = new InputDataInfo[1]; // We have only one input column
			arrDataInfo[INPUT_COLUMN_ADDITIONAL] = new InputDataInfo(inSpec, m_modelAdditionalInputColumnName,
					InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
					BitVectorValue.class, RDKitMolValue.class);
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
		final WarningConsolidator warnings = getWarningConsolidator();

		// Reset old intermediate values
		m_ebvRowsToKeep = null;

		// Create sub execution contexts for pre-processing steps
		final ExecutionContext subExecReadingFingerprints = exec.createSubExecutionContext(0.75d);
		final ExecutionContext subExecReadingAdditionalFingerprints = exec.createSubExecutionContext(0.05d);
		final ExecutionContext subExecCheckDiversity = exec.createSubExecutionContext(0.10d);
		final ExecutionContext subExecProcessingDiversity = exec.createSubExecutionContext(0.10d);

		final long lInputRowCount = inData[0].size();
		final List<Long> listIndicesUsed = new ArrayList<Long>();
		final EBV_Vect vFingerprints = markForCleanup(new EBV_Vect());
		Int_Vect firstPicks = null;
		AtomicLong alFpLength = new AtomicLong(-1);

		// 1. Get all fingerprints in a form that we can process further
		final boolean bNeedsCalculation1 = arrInputDataInfo[0][INPUT_COLUMN_MAIN].isCompatibleOrAdaptable(RDKitMolValue.class);
		final FingerprintSettingsHeaderProperty fpSpec1 = (bNeedsCalculation1 ?
				new FingerprintSettingsHeaderProperty(DEFAULT_FINGERPRINT_SETTINGS) :
					new FingerprintSettingsHeaderProperty(arrInputDataInfo[0][INPUT_COLUMN_MAIN].getColumnSpec()));
		FingerprintSettingsHeaderProperty fpSpec2 = null;

		// Parallel processing to prepare fingerprints from main table (first table)
        prepareFingerprints(1, inData[0], arrInputDataInfo[0][INPUT_COLUMN_MAIN], 
        		bNeedsCalculation1, DEFAULT_FINGERPRINT_SETTINGS.getRdkitFingerprintType(), 
        		listIndicesUsed, vFingerprints, alFpLength, 
        		warnings, subExecReadingFingerprints);

		// Check, if parameters of user make sense based on the found fingerprints in table 1 (NOT combined yet with table 2)
		final int iNumberToPick = m_modelNumberToPick.getIntValue();
		final boolean bKeepAll = (listIndicesUsed.size() == iNumberToPick);
		if (listIndicesUsed.size() < iNumberToPick) {
			throw new InvalidSettingsException("Number of diverse points requested ("+ iNumberToPick
					+ ") exceeds number of valid fingerprints (" + listIndicesUsed.size() + ")");
		}

		subExecReadingFingerprints.setProgress(1.0d);

		// Add fingerprints from optional second input table to bias away from
		if (!bKeepAll && hasAdditionalInputTable(getInputTableSpecs(inData)) && arrInputDataInfo[1].length > 0) {
			final InputDataInfo inputDataInfo2 = arrInputDataInfo[1][INPUT_COLUMN_ADDITIONAL];

			if (inputDataInfo2.isCompatible(BitVectorValue.class) || inputDataInfo2.isCompatibleOrAdaptable(RDKitMolValue.class)) {
				final boolean bNeedsCalculation2 = inputDataInfo2.isCompatibleOrAdaptable(RDKitMolValue.class);
				final FingerprintType fpType = fpSpec1.getRdkitFingerprintType(); // The configure() method ensures that this is not null
				firstPicks = new Int_Vect();
				final RowIterator it2 = inData[1].iterator();
				int iAdditionalRowIndex = 0;
				int iBiasAwayIndex = (int)vFingerprints.size();
				final long lAdditionalRowCount = inData[1].size();
				final String strInfoForProgress = (bNeedsCalculation2 ? " - Calculating additional fingerprints" : " - Reading additional fingerprints");

				if (!bNeedsCalculation2) {
					fpSpec2 = new FingerprintSettingsHeaderProperty(
							arrInputDataInfo[1][INPUT_COLUMN_ADDITIONAL].getColumnSpec());

					if (fpSpec1 == null || fpSpec2 == null) {
						getWarningConsolidator().saveWarning("The fingerprints in table 1 and 2 might not be compatible, which may lead to wrong results.");
					}
					else if (!FingerprintType.isCompatible(fpSpec1, fpSpec2)) {
						getWarningConsolidator().saveWarning("The fingerprints in table 1 and 2 are not compatible, which may lead to wrong results.");
					}
				}

				while (it2.hasNext()) {
					final DataRow row = it2.next();
					ExplicitBitVect expBitVector = null;

					if (bNeedsCalculation2) {
						// Calculate the fingerprint for the molecule on the fly
						ROMol mol = null;

						try {
							mol = arrInputDataInfo[1][INPUT_COLUMN_ADDITIONAL].getROMol(row);
							if (mol != null) {
								expBitVector = markForCleanup(fpType.calculate(mol, fpSpec1));
							}
							else {
								warnings.saveWarning(ROW_CONTEXT_TABLE_2.getId(), "Encountered empty molecule cell in table 2 - ignored it.");
							}
						}
						finally {
							// Delete the molecule manually to free memory quickly
							if (mol != null) {
								mol.delete();
							}
						}
					}
					else {
						// Use the existing fingerprint
						expBitVector = markForCleanup(arrInputDataInfo[1][INPUT_COLUMN_ADDITIONAL].getExplicitBitVector(row));
						if (expBitVector == null) {
							warnings.saveWarning(ROW_CONTEXT_TABLE_2.getId(), "Encountered empty fingerprint cell in table 2 - ignored it.");
						}
					}

					// Just add the fingerprint to bias away from and mark it as part of first picks
					if (expBitVector != null) {
						final long lNumBits = expBitVector.getNumBits();
						if (alFpLength.get() == -1) {
							alFpLength.set(lNumBits);
						}

						if (alFpLength.get() == lNumBits){
							vFingerprints.add(expBitVector);
							firstPicks.add(iBiasAwayIndex);
							iBiasAwayIndex++;
						}
						else {
							warnings.saveWarning(ROW_CONTEXT_TABLE_2.getId(),
									"Encountered fingerprint with invalid length (" +
											lNumBits + " instead of " + alFpLength.get() + " bits) in table 2 - ignoring it.");
						}
					}

					// Every 1000 iterations report progress and check for cancel
					if (iAdditionalRowIndex % 1000 == 0) {
						AbstractRDKitNodeModel.reportProgress(subExecReadingAdditionalFingerprints, iAdditionalRowIndex, lAdditionalRowCount, row, strInfoForProgress);
					}

					iAdditionalRowIndex++;
				}
			}
		}

		subExecReadingAdditionalFingerprints.setProgress(1.0d);

		// 2. Doing diversity pick from fingerprints
		subExecCheckDiversity.setProgress(0.25d, "Doing diversity pick");
		Int_Vect intVector = null;

		if (bKeepAll) {
			warnings.saveWarning("Number of diverse points requested (" + iNumberToPick
					+ ") is equal to the number of valid fingerprints (" + listIndicesUsed.size() + ") - " +
					"Output table will contain all rows of table 1 with non-empty input.");
			intVector = new Int_Vect();
			for (int i = 0; i < iNumberToPick; i++) {
				intVector.set(i,  i);
			}
		}
		else if (firstPicks == null || firstPicks.isEmpty()) {
			firstPicks = new Int_Vect();
		}
		// the distance cache just slows things down with the new diversity picker implementation
		Boolean useDistanceCache = false;
		intVector = markForCleanup(RDKFuncs.pickUsingFingerprints(vFingerprints,
				iNumberToPick + firstPicks.size(), m_randomSeed.getIntValue(), firstPicks, useDistanceCache));

		subExecCheckDiversity.setProgress(1.0d);
		subExecCheckDiversity.checkCanceled();

		// 3. Store, what rows to keep
		m_ebvRowsToKeep = markForCleanup(new ExplicitBitVect(lInputRowCount));
		final int iDiversityCount = (int)intVector.size();
		for(int i = 0; i < iDiversityCount; i++) {
			final int pickedFingerprintIndex = intVector.get(i);
			if (pickedFingerprintIndex < listIndicesUsed.size()) {
				final long pickedRowIndex = listIndicesUsed.get(pickedFingerprintIndex);
				if (pickedRowIndex < lInputRowCount) {
					m_ebvRowsToKeep.setBit(pickedRowIndex);
				}
			}

			// Every 1000 iterations report progress and check for cancel
			if (i % 1000 == 0) {
				AbstractRDKitNodeModel.reportProgress(subExecProcessingDiversity, i, iDiversityCount,
						null, " - Processing diversity results");
			}
		}

		subExecProcessingDiversity.setProgress(1.0d);
	}

	/**
	 * Prepares fingerprints for diversity picking from an input table, either with a molecule column
	 * or with a fingerprint column.
	 * 
	 * @param iTableNumber Table index. Only used for warning generations.
	 * @param inData Table data. Must not be null.
	 * @param inputDataInfo Input data definition for the column to process. Must not be null.
	 * @param bNeedsCalculation True to calculate fingerprints from molecules. False otherwise.
	 * @param fpTypeDefault Fingerprint type used when we need to calculate fingerprints from molecules.
	 * 		Must not be null.
	 * @param listIndicesUsed IN/OUT: List of indices that will be filled with row indexes.
	 * @param vFingerprints IN/OUT: List of fingerprints that will be filled with fingerprints.
	 * @param alFpLength IN/OUT: Length of processed fingerprints. Must not be null.
	 * @param warnings Warning consolidator. Must not be null.
	 * @param subExecReadingFingerprints Execution context. Must not be null.
	 * 
	 * @throws Exception Thrown, if something goes wrong.
	 */
	protected void prepareFingerprints(final int iTableNumber, final BufferedDataTable inData, final InputDataInfo inputDataInfo, 
			final boolean bNeedsCalculation, final FingerprintType fpTypeDefault, 
			final List<Long> listIndicesUsed, final EBV_Vect vFingerprints, final AtomicLong alFpLength,
			final WarningConsolidator warnings, final ExecutionContext subExecReadingFingerprints) throws Exception {
        
        // Get settings and define data specific behavior
        final int iMaxParallelWorkers = (int)Math.ceil(1.5 * Runtime.getRuntime().availableProcessors());
        final int iQueueSize = 1000 * iMaxParallelWorkers;
        final long lTotalRowCount = inData.size();
        
		// Calculate RDKit Fingerprints from molecule, or convert them from KNIME Fingerprints
		new MultiThreadWorker<DataRow, ExplicitBitVect>(iQueueSize, iMaxParallelWorkers) {

			/**
			 * Prepares a fingerprint from first table.
			 * 
			 * @param row   Input row.
			 * @param index Index of row.
			 * 
			 * @return Null, if fingerprint could not be determined.
			 * 		   Result fingerprint, if we have a valid fingerprint
			 *         to be used for diversity picking.
			 */
			@Override
			protected ExplicitBitVect compute(final DataRow row, final long index) throws Exception {
				ExplicitBitVect expBitVector = null;

				if (bNeedsCalculation) {
					// Calculate the fingerprint for the molecule on the fly
					ROMol mol = null;

					try {
						mol = inputDataInfo.getROMol(row);
						if (mol != null) {
							expBitVector = markForCleanup(fpTypeDefault.calculate(mol, DEFAULT_FINGERPRINT_SETTINGS));
						} 
						else {
							warnings.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
									"Encountered empty molecule cell in table " + iTableNumber + " - ignored it.");
						}
					} 
					finally {
						// Delete the molecule manually to free memory quickly
						if (mol != null) {
							mol.delete();
						}
					}
				} 
				else {
					expBitVector = markForCleanup(inputDataInfo.getExplicitBitVector(row));
					if (expBitVector == null) {
						warnings.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
								"Encountered empty fingerprint cell in table " + iTableNumber + " - ignored it.");
					}
				}
				
				return expBitVector;
			}

			/**
			 * Adds the fingerprint results to the fingerprint list for further processing.
			 * 
			 * @param task Processing result for a row.
			 */
			@Override
			protected void processFinished(final ComputationTask task)
					throws ExecutionException, CancellationException, InterruptedException {
				final ExplicitBitVect expBitVector = task.get();
				final long lRowIndex = task.getIndex();

				if (expBitVector != null) {
					final long lNumBits = expBitVector.getNumBits();
					if (alFpLength.get() == -1) {
						alFpLength.set(lNumBits);
					}

					if (alFpLength.get() == lNumBits) {
						listIndicesUsed.add(lRowIndex);
						vFingerprints.add(expBitVector);
					} else {
						warnings.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
								"Encountered fingerprint with invalid length (" + lNumBits + " instead of " + alFpLength.get()
										+ " bits) in table " + iTableNumber + " - ignoring it.");
					}
				}
				
				// Check, if user pressed cancel (however, we will finish the method
				// nevertheless)
				// Update the progress only every 1000 rows
				if (task.getIndex() % 1000 == 0) {
					try {
						AbstractRDKitNodeModel.reportProgress(subExecReadingFingerprints, lRowIndex, lTotalRowCount, null, 
								" - " + (bNeedsCalculation ? "Calculating" : "Reading") + " fingerprints");
					} catch (final CanceledExecutionException e) {
						cancel(true);
					}
				}
			};
		}.run(inData);		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanupIntermediateResults() {
		m_ebvRowsToKeep = null;
	}

	@Override
	protected Map<String, Long> createWarningContextOccurrencesMap(
			final BufferedDataTable[] inData, final InputDataInfo[][] arrInputDataInfo,
			final BufferedDataTable[] resultData) {

		final Map<String, Long> mapContextOccurrences = super.createWarningContextOccurrencesMap(inData, arrInputDataInfo,
				resultData);

		if (hasAdditionalInputTable(getInputTableSpecs(inData))) {
			mapContextOccurrences.put(ROW_CONTEXT_TABLE_2.getId(), inData[1].size());
		}

		return mapContextOccurrences;
	}

	/**
	 * {@inheritDoc}
	 * This implementation filters out all rows, which are not contained in the rowsToKeep member variable, which
	 * was calculated during the pre-processing phase.

	 * @param arrInputDataInfo Input data information about all columns of the table at the input port. Not used
	 * 		in this implementation.
	 * @param lUniqueWaveId A unique id that should be used for marking RDKit objects for cleanup. Marked
	 * 		objects will be cleaned up automatically at the end of this call. If this is not wanted,
	 * 		the objects should either not be marked for cleanup or they should be marked without an id,
	 * 		which would lead to a cleanup at the end of the entire execution process. Not used in this implementation.
	 * 
	 * @return 0 to keep the row, or -1 if row shall be filtered out completely.
	 */
	@Override
	public int determineTargetTable(final int iInPort, final long lRowIndex, final DataRow row, final InputDataInfo[] arrInputDataInfo, final long lUniqueWaveId) {
		return (m_ebvRowsToKeep.getBit(lRowIndex) ? 0 : -1);
	}

	//
	// Static Public Methods
	//

	/**
	 * Determines, if the condition is fulfilled that we have an additional input table with
	 * molecules or fingerprints connected to the node according to the passed in specs.
	 * 
	 * @param inSpecs Port specifications.
	 * 
	 * @return True, if there is an additional input table present in the last index of the specs,
	 * 		and if it has columns.
	 */
	public static boolean hasAdditionalInputTable(final PortObjectSpec[] inSpecs) {
		return (inSpecs != null && inSpecs.length >= 2 &&
				inSpecs[1] instanceof DataTableSpec &&
				((DataTableSpec)inSpecs[1]).getNumColumns() > 0);
	}
}
