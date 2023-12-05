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
package org.rdkit.knime.nodes.sdfdifferencechecker;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicLong;

import org.knime.chem.types.SdfValue;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.InvalidInputException;
import org.rdkit.knime.util.SettingsUtils;

/**
 * This class implements the node model of the RDKitSDFDifferenceChecker node
 * providing difference checks for SDF strings that take certain granted differences
 * into account.
 * 
 * @author Manuel Schwarze 
 */
public class RDKitSDFDifferenceCheckerNodeModel extends AbstractRDKitNodeModel {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitSDFDifferenceCheckerNodeModel.class);

	/** Input data info index for SDF value. */
	protected static final int INPUT_COLUMN_SDF_1 = 0;

	/** Input data info index for SDF value. */
	protected static final int INPUT_COLUMN_SDF_2 = 0;

	/** The currently set floating point type based on System LOCALE. */
	protected static final String SYSTEM_LOCALE_FLOATING_POINT = ("" + 1.234d).substring(1, 2);


	//
	// Members
	//

	/** Settings model for the column name of the input column in table 1. */
	private final SettingsModelString m_modelInputColumn1Name =
			registerSettings(RDKitSDFDifferenceCheckerNodeDialog.createInputColumn1NameModel());

	/** Settings model for the column name of the input column in table 1. */
	private final SettingsModelString m_modelInputColumn2Name =
			registerSettings(RDKitSDFDifferenceCheckerNodeDialog.createInputColumn2NameModel());

	/** Settings model for the tolerance to be applied to floating point numbers. */
	private final SettingsModelDouble m_modelTolerance =
			registerSettings(RDKitSDFDifferenceCheckerNodeDialog.createToleranceModel());

	/** Settings model for the option to fail on the first encountered difference. */
	private final SettingsModelBoolean m_modelFailOnFirstDifferenceOption =
			registerSettings(RDKitSDFDifferenceCheckerNodeDialog.createFailOnFirstDifferenceOptionModel(), true);

	/** Settings model for the option to specify a limit for console output of differences. */
	private final SettingsModelIntegerBounded m_modelLimitConsoleOutputOption =
			registerSettings(RDKitSDFDifferenceCheckerNodeDialog.createLimitConsoleOutputOptionModel(), true);

	// Intermediate results

	private AtomicLong m_diffRowCounter = null;

	/** The first encountered row of table 1 that showed a difference. */
	private DataRow m_errorRow1 = null;

	/** The first encountered row of table 2 that showed a difference. */
	private DataRow m_errorRow2 = null;

	/** The first encountered error. */
	private String m_strErrorMessage = null;

	/** The first encountered error cause. */
	private Throwable m_errorCause = null;

	//
	// Constructor
	//

	/**
	 * Create new node model with one data in- and one out-port.
	 */
	RDKitSDFDifferenceCheckerNodeModel() {
		super(2, 0);
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

		final List<Class<? extends DataValue>> listValueClasses =
				new ArrayList<Class<? extends DataValue>>();
		listValueClasses.add(SdfValue.class);
		listValueClasses.add(StringValue.class);

		// Auto guess the input column 1 if not set - fails if no compatible column found
		SettingsUtils.autoGuessColumn(inSpecs[0], m_modelInputColumn1Name, SdfValue.class, 0,
				"Auto guessing: Using column %COLUMN_NAME% for input table 1.",
				null, null);
		SettingsUtils.autoGuessColumn(inSpecs[0], m_modelInputColumn1Name, StringValue.class, 0,
				"Auto guessing: Using column %COLUMN_NAME% for input table 1.",
				"No SDF nor String compatible column in input table 1.", getWarningConsolidator());

		// Determines, if the input column 1 exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[0], m_modelInputColumn1Name, listValueClasses,
				"Input column of table 1 has not been specified yet.",
				"Input column %COLUMN_NAME% of table 1 does not exist. Has the input table 1 changed?");

		// Auto guess the input column 2 if not set - fails if no compatible column found
		SettingsUtils.autoGuessColumn(inSpecs[1], m_modelInputColumn2Name, SdfValue.class,
				(inSpecs[0] == inSpecs[1] ? 1 : 0),
				"Auto guessing: Using column %COLUMN_NAME% for input table 2.",
				null, null);
		SettingsUtils.autoGuessColumn(inSpecs[1], m_modelInputColumn2Name, StringValue.class,
				(inSpecs[0] == inSpecs[1] ? 1 : 0),
				"Auto guessing: Using column %COLUMN_NAME% for input table 2.",
				"No SDF nor String compatible column in input table 2.", getWarningConsolidator());

		// Determines, if the input column 2 exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[1], m_modelInputColumn2Name, listValueClasses,
				"Input column of table 2 has not been specified yet.",
				"Input column %COLUMN_NAME% of table 2 does not exist. Has the input table 2 changed?");

		// Consolidate all warnings and make them available to the user
		generateWarnings();

		// Generate output specs
		return null;
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
			arrDataInfo[INPUT_COLUMN_SDF_1] = new InputDataInfo(inSpec, m_modelInputColumn1Name,
					InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
					SdfValue.class, StringValue.class);
		}

		// Specify input of table 2
		else if (inPort == 1) {
			arrDataInfo = new InputDataInfo[1]; // We have only one input column
			arrDataInfo[INPUT_COLUMN_SDF_2] = new InputDataInfo(inSpec, m_modelInputColumn2Name,
					InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
					SdfValue.class, StringValue.class);
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
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] processing(final BufferedDataTable[] inData, final InputDataInfo[][] arrInputDataInfo,
			final ExecutionContext exec) throws Exception {
		// Get settings and define data specific behavior
		final boolean bFailOnFirstDifference = m_modelFailOnFirstDifferenceOption.getBooleanValue();
		final double dTolerance = m_modelTolerance.getDoubleValue();
		final long lTotalRowCount1 = inData[0].size();
		final long lTotalRowCount2 = inData[1].size();
		m_diffRowCounter = new AtomicLong(0);

		if (lTotalRowCount1 != lTotalRowCount2) {
			throw new InvalidInputException("Columns have a different number of rows: Table 1 has " + lTotalRowCount1 +
					" row and Table 2 has " + lTotalRowCount2 + " rows.");
		}

		// Iterate through all input rows and calculate results
		long rowInputIndex = 0;
		final CloseableRowIterator i1 = inData[0].iterator();
		final CloseableRowIterator i2 = inData[1].iterator();

		while (i1.hasNext() && i2.hasNext() && (!bFailOnFirstDifference || !hasDifference())) {
			final DataRow row1 = i1.next();
			final DataRow row2 = i2.next();

			String strSdf1 = null;
			String strSdf2 = null;

			try {
				strSdf1 = arrInputDataInfo[0][INPUT_COLUMN_SDF_1].getSdfValue(row1);
			}
			catch (final IllegalArgumentException exc) {
				strSdf1 = arrInputDataInfo[0][INPUT_COLUMN_SDF_1].getString(row1);
			}

			try {
				strSdf2 = arrInputDataInfo[1][INPUT_COLUMN_SDF_2].getSdfValue(row2);
			}
			catch (final IllegalArgumentException exc) {
				strSdf2 = arrInputDataInfo[1][INPUT_COLUMN_SDF_2].getString(row2);
			}

			if (strSdf1 != null && strSdf2 != null) {
				final String strWorkingSdf1 = normalizeString(strSdf1);
				final String strWorkingSdf2 = normalizeString(strSdf2);

				final StringTokenizer st1 = new StringTokenizer(strWorkingSdf1, " ", false);
				final StringTokenizer st2 = new StringTokenizer(strWorkingSdf2, " ", false);

				if (st1.countTokens() != st2.countTokens()) {
					recordDifference(row1, strSdf1, row2, strSdf2, "Length of SDF information in table 1 is " +
							(st1.countTokens() > st2.countTokens() ? "greater" : "smaller") +
							" than in table 2.", null);
				}
				else {
					boolean bFurtherDifferences = false;
					while (st1.hasMoreTokens() && st2.hasMoreTokens()) {
						final String strToken1 = st1.nextToken();
						final String strToken2 = st2.nextToken();

						if (!strToken1.equals(strToken2)) {
							final double number1 = normalizeNumber(strToken1);
							final double number2 = normalizeNumber(strToken2);

							// Note: Integers are here also treated as NaN, because they can be string compared
							if (Double.isNaN(number1) || Double.isNaN(number2) || Math.abs(number1 - number2) > dTolerance) {
								recordDifference(row1, strSdf1, row2, strSdf2, "'" + strToken1 +
										"' is different from '" + strToken2 + "'.", null, bFurtherDifferences);
								bFurtherDifferences = true;
							}
						}
					}
				}
			}
			else if (strSdf1 != null || strSdf2 != null) {
				recordDifference(row1, strSdf1, row2, strSdf2, strSdf1 != null ?
						"Cell in table 1 is empty, but cell in table 2 has content." :
							"Cell in table 2 is empty, but cell in table 1 has content.", null);
			}

			rowInputIndex++;

			// Every 20 iterations check cancellation status and report progress
			if (rowInputIndex % 20 == 0) {
				AbstractRDKitNodeModel.reportProgress(exec, rowInputIndex, lTotalRowCount1, row1, " - Comparing SDF strings");
			}
		}

		if (hasDifference()) {
			throw new InvalidInputException(
					(bFailOnFirstDifference ? "" :
						m_diffRowCounter.get() + " rows with differences found, e.g. ") +
						"Difference found in row " + m_errorRow1.getKey() + " (table 1) and row " +
						m_errorRow2.getKey() + " (table 2): " + m_strErrorMessage, m_errorCause);
		}

		return new BufferedDataTable[] { };
	}

	@Override
	protected void cleanupIntermediateResults() {
		m_errorRow1 = null;
		m_errorRow2 = null;
		m_strErrorMessage = null;
		m_errorCause = null;
		m_diffRowCounter = null;
	}

	//
	// Private Methods
	//

	/**
	 * Determines if a difference was found during node execution.
	 * 
	 * @return True, if there was a difference.
	 */
	private boolean hasDifference() {
		return (m_errorRow1 != null || m_errorRow2 != null || m_strErrorMessage != null || m_errorCause != null);
	}

	/**
	 * Normalizes the passed in string and removes all special characters.
	 * 
	 * @param str String to be normalized. Can be null.
	 * 
	 * @return Normalized string.
	 */
	private String normalizeString(String str) {
		if (str != null) {
			str = str.replace("\r", "");
			str = str.replace("\n", "");
			str = str.replace("\f", "");
			str = str.replace("\b", "");
			str = str.replace("\t", "");
			str = str.trim().replaceAll(" +", " ");
		}
		return str;
	}

	/**
	 * Normalizes a number parsed from the specified string.
	 * 
	 * @param str String with a number to be normalized.
	 * 
	 * @return Double number or NaN, if the string could not be parsed successfully.
	 */
	private double normalizeNumber(final String str) {
		double dRet = Double.NaN;

		if (str != null && (str.indexOf(".") >= 0 || str.indexOf(",") >= 0)) {
			String strTokenTemp = str.replace(".", SYSTEM_LOCALE_FLOATING_POINT);
			strTokenTemp = strTokenTemp.replace(",", SYSTEM_LOCALE_FLOATING_POINT);

			try {
				dRet = Double.parseDouble(strTokenTemp);
			}
			catch (final NumberFormatException exc) {
				// Ignored
			}
		}

		return dRet;
	}

	private void recordDifference(final DataRow row1, final String strSdf1, final DataRow row2, final String strSdf2,
			final String strMessage, final Throwable cause) {
		recordDifference(row1, strSdf1, row2, strSdf2, strMessage, cause, true);
	}

	private void recordDifference(final DataRow row1, final String strSdf1, final DataRow row2, final String strSdf2,
			final String strMessage, final Throwable cause, final boolean bFurtherDifferenceInSDFs) {
		// Remember the first difference for later
		if (!hasDifference()) {
			m_errorRow1 = row1;
			m_errorRow2 = row2;
			m_strErrorMessage = strMessage;
			m_errorCause = cause;
		}

		if (!bFurtherDifferenceInSDFs) {
			m_diffRowCounter.incrementAndGet();
		}

		if (m_diffRowCounter.get() < m_modelLimitConsoleOutputOption.getIntValue()) {
			final StringBuilder sb = new StringBuilder(
					(bFurtherDifferenceInSDFs ? "Further d" : "D") + ("ifference found in row "))
			.append(row1.getKey()).append(" (table 1) and row ")
			.append(row2.getKey()).append(" (table 2): " )
			.append(strMessage);

			// Record SDFs only for first difference
			if (!bFurtherDifferenceInSDFs) {
				sb.append("\nSDF (table 1):\n").append(strSdf1);
				sb.append("\nvs. SDF (table 2):\n").append(strSdf2);
			}

			LOGGER.warn(sb.toString(), cause);
		}
	}
}
