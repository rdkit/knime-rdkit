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
package org.rdkit.knime.nodes.fingerprintwriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;
import org.rdkit.knime.util.FileUtils;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class implements the node model of the RDKitFingerprintWriter node
 * providing calculations based on the open source RDKit library.
 * 
 * @author Sudip Ghosh
 * @author Manuel Schwarze
 */
public class RDKitFingerprintWriterNodeModel extends AbstractRDKitNodeModel {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitFingerprintWriterNodeModel.class);

	/** Input data info index for Fingerprint value. */
	protected static final int INPUT_COLUMN_FPS = 0;

	/** Input data info index for ID value. */
	protected static final int INPUT_COLUMN_ID = 1;

	/** Define hex values. */
	protected static final char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	/** Formatting the date/time using a custom FPS format: yyyy-MM-dd'T'HH:mm:ss */
	private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	/** Formatting the date/time using a custom FPS format: yyyy-MM-dd'T'00:00:00 */
	private static final SimpleDateFormat DATE_FORMATTER_WITH_SUPPRESSED_TIME = new SimpleDateFormat("yyyy-MM-dd'T'00:00:00");

	//
	// Members
	//

	/** Settings model for the column name of the input column. */
	private final SettingsModelString m_modelFingerprintColumnName =
			registerSettings(RDKitFingerprintWriterNodeDialog.createFingerprintColumnNameModel());

	/** Settings model for the column name of the ID column. */
	private final SettingsModelColumnName m_modelIdColumnName =
			registerSettings(RDKitFingerprintWriterNodeDialog.createIdColumnNameModel());

	/** Settings model for the output file. */
	private final SettingsModelString m_modelOutputFile =
			registerSettings(RDKitFingerprintWriterNodeDialog.createOutputFileModel());

	/** Settings model for the option to overwrite an existing output file. */
	private final SettingsModelBoolean m_modelOverwriteOption=
			registerSettings(RDKitFingerprintWriterNodeDialog.createOverwriteOptionModel());

	/** Settings model for the option to suppress the time in the FPS header. */
	private final SettingsModelInteger m_modelSuppressTimeOption=
			registerSettings(RDKitFingerprintWriterNodeDialog.createSuppressTimeOptionModel(), true);

	//
	// Constructor
	//

	/**
	 * Create new node model with one data in- and no out-port.
	 */
	RDKitFingerprintWriterNodeModel() {
		super(1, 0);
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

		// Fingerprint column checks
		// Auto guess the fingerprint column if not set - fails if no compatible column found
		SettingsUtils.autoGuessColumn(inSpecs[0], m_modelFingerprintColumnName, BitVectorValue.class, 0,
				"Auto guessing: Using column %COLUMN_NAME%.",
				"No BitVectorValue compatible column in input table.", getWarningConsolidator());

		// Determines, if the fingerprint column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[0], m_modelFingerprintColumnName, BitVectorValue.class,
				"Fingerprint column has not been specified yet.",
				"Fingerprint column %COLUMN_NAME% does not exist. Has the input table changed?");

		// ID column checks
		// Auto guess the input column if not set - fails if no compatible column found
		final boolean bIdColumnFound = SettingsUtils.autoGuessColumn(inSpecs[0], m_modelIdColumnName, StringValue.class, 0,
				"Auto guessing: Using column %COLUMN_NAME% as ID column.",
				null, getWarningConsolidator()); // Do not fail, if we don't find a string column
		// If no string column was found we will use the row id
		if (!bIdColumnFound) {
			m_modelIdColumnName.setSelection(m_modelIdColumnName.getColumnName(), true);
		}

		// Determines, if the input column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[0], m_modelIdColumnName, StringValue.class,
				"ID column has not been specified yet.",
				"ID column %COLUMN_NAME% does not exist. Has the input table changed?");

		// Perform checks on the specified output file
		final File fileOutput = FileUtils.convertToFile(m_modelOutputFile.getStringValue(), false, true);
		if (fileOutput.exists()) {
			if (m_modelOverwriteOption.getBooleanValue()) {
				getWarningConsolidator().saveWarning("The specified output file exists and will be overwritten.");
			}
			else {
				throw new InvalidSettingsException("The specified output file exists already. " +
						"You may remove the file or switch on the Overwrite option to grant execution.");
			}
		}
		else {
			final File dirOutput = fileOutput.getParentFile();
			if (dirOutput == null) {
				throw new InvalidSettingsException("Cannot determine parent " +
						"directory of the output file.");
			}
			else if (!dirOutput.exists()) {
				getWarningConsolidator().saveWarning(
						"Directory of specified output file does not exist " +
						"and will be created.");
			}
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
	@SuppressWarnings("unchecked")
	protected InputDataInfo[] createInputDataInfos(final int inPort, final DataTableSpec inSpec)
			throws InvalidSettingsException {

		InputDataInfo[] arrDataInfo = null;

		// Specify input of table 1
		if (inPort == 0) {
			arrDataInfo = new InputDataInfo[2]; // We have two input column
			arrDataInfo[INPUT_COLUMN_FPS] = new InputDataInfo(inSpec, m_modelFingerprintColumnName,
					InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
					BitVectorValue.class);

			// If the row key is used as column we cannot use the InputDataInfo object
			if (m_modelIdColumnName.useRowID()) {
				arrDataInfo[INPUT_COLUMN_ID] = null;
			}
			else {
				arrDataInfo[INPUT_COLUMN_ID] = new InputDataInfo(inSpec, m_modelIdColumnName,
						InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
						StringValue.class);
			}
		}

		return (arrDataInfo == null ? new InputDataInfo[0] : arrDataInfo);
	}


	/**
	 * In this implementation it returns always null as we don't have any output table.
	 * {@inheritDoc}
	 * 
	 * @return Always null.
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

		// Prepare all settings and pre-requisites
		final File fileOutput = FileUtils.convertToFile(m_modelOutputFile.getStringValue(), false, true);
		final boolean bUseRowIds = m_modelIdColumnName.useRowID();
		final boolean bSuppressTime = m_modelSuppressTimeOption.getIntValue() != 0;
		final int iTotalRowCount = inData[0].getRowCount();
		int iDefinedNumBits = -1; // Undefined

		// Create missing directories
		final File dirOutput = fileOutput.getParentFile();
		FileUtils.prepareDirectory(dirOutput); // Throws an exception, if not successful

		// Last override check (has been checked already in configure() method)
		if (fileOutput.exists() && m_modelOverwriteOption.getBooleanValue() == false) {
			throw new InvalidSettingsException("The specified output file exists already. " +
					"You may remove the file or switch on the Overwrite option to grant execution.");
		}

		// Create the output file (override existing file)
		// Output file can either be a text file or a zipped text file
		BufferedWriter writer = null;
		FileOutputStream outFile = null;

		try {
			if (fileOutput.getName().endsWith(".gz")) {
				outFile = new FileOutputStream(fileOutput);
				writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(outFile)));
			}
			else {
				writer = new BufferedWriter(new FileWriter(fileOutput));
			}

			// Iterate through all input rows and write out fingerprints
			int rowIndex = 0;
			int iWrittenFingerprints = 0;

			for (final CloseableRowIterator i = inData[0].iterator(); i.hasNext(); rowIndex++) {
				final DataRow row = i.next();

				// Get fingerprint
				final DenseBitVector dbvFingerprint = arrInputDataInfo[0][INPUT_COLUMN_FPS].getDenseBitVector(row);

				if (dbvFingerprint == null) {
					getWarningConsolidator().saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
							"Encountered empty fingerprint, which will be ignored.");
				}
				else {
					// Process id
					String strId = (bUseRowIds ?
							row.getKey().getString() :
								arrInputDataInfo[0][INPUT_COLUMN_ID].getString(row));

					// Assign an artificial ID, if missing cell was encountered
					if (strId == null) {
						getWarningConsolidator().saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
								"Encountered empty ID. Generated unique ID (MissingIdXXX) on the fly.");
						strId = "MissingId" + rowIndex;
					}

					// Write out header in the very beginning
					final int iNumBits = (int)dbvFingerprint.length();
					if (iWrittenFingerprints == 0) {
						// Determine fingerprint length
						iDefinedNumBits = (int)dbvFingerprint.length();

						writer.write("#FPS1");
						writer.newLine();
						writer.write("#num_bits=" + iNumBits);
						writer.newLine();
						writer.write("#software=Knime/" + KNIMEConstants.VERSION );
						writer.newLine();
						writer.write("#date=" + (bSuppressTime ?
								formatDate(DATE_FORMATTER_WITH_SUPPRESSED_TIME, new Date()) :
									formatDate(DATE_FORMATTER, new Date())));
						writer.newLine();
					}

					String strFpsFingerprint = null;

					try {
						// Convert fingerprint and check length consistency
						strFpsFingerprint = convertToFpsFormat(dbvFingerprint, iDefinedNumBits);

						// Write out fingerprint line
						writer.write(strFpsFingerprint);
						writer.write("\t");
						writer.write(strId);
						writer.newLine();
						iWrittenFingerprints++;
					}
					catch (final IOException excIo) {
						throw excIo;
					}
					catch (final NumberFormatException excFormat) {
						LOGGER.warn("Invalid fingerprint size encountered in row '" + row.getKey() + "'.");
						getWarningConsolidator().saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
								"Encountered an invalid fingerprint size. Skipping this fingerprint.");
					}
					catch (final Exception exc) {
						LOGGER.warn("Invalid fingerprint encountered in row '" + row.getKey() + "'.");
						getWarningConsolidator().saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
								"Encountered an invalid fingerprint. Skipping this fingerprint.");
					}
				}

				// Every 20 iterations check cancellation status and report progress
				if (rowIndex % 20 == 0) {
					AbstractRDKitNodeModel.reportProgress(exec, rowIndex, iTotalRowCount, row, " - Writing fingerprints");
				}
			};
		}
		catch (final IOException excIo) {
			throw new IOException("The fingerprint file could not be read successfully: " + excIo, excIo);
		}
		finally {
			// Close streams and writers
			FileUtils.close(writer);
			FileUtils.close(outFile);
		}
		exec.checkCanceled();
		exec.setProgress(1.0, "Finished Processing");

		return new BufferedDataTable[0];
	}

	/**
	 * Converts the passed in bit vector into an FPS file format compatible
	 * string (see http://code.google.com/p/chem-fingerprints/wiki/FPS).
	 * 
	 * @param dbvFingerprint Dense bit vector. Must not be null.
	 * @param iForceBitLength If set to a value > 0 the input is checked and
	 * 		must be equal to this bit length. Specify -1 to skip the check.
	 * 
	 * @return An FPS file format compatible fingerprint hex string.
	 * 
	 * @throws NumberFormatException Thrown, if the bit length is not
	 * 		equal to the passed in parameter (only if set to > 0).
	 */
	public static String convertToFpsFormat(final DenseBitVector dbvFingerprint,
			final int iForceBitLength) throws NumberFormatException {

		final int iNumBits = (int)dbvFingerprint.length();

		// resultHex to store hex encoded Fingerprints
		final StringBuilder sbResult = new StringBuilder(iNumBits / 4);
		final StringBuilder strTemp = new StringBuilder(2);
		int iByte = 0;
		int j = 0;

		// Traverse through each character of binary bits, processing 8 bits at
		// a time starting at the highest bit of the fingerprint
		for (int i = 0; i < iNumBits; i++) {
			iByte = iByte + (dbvFingerprint.get(iNumBits - i - 1) ? 1 << j : 0);
			j++;

			// Every 8 bits we calculate a hex string piece
			if (i % 8 == 7) {
				if (iByte == 0) {
					sbResult.append("00");
				}
				else {
					strTemp.setLength(0);

					while (iByte != 0) {
						strTemp.append(HEX[(iByte % 16)]);
						iByte = iByte >> 4;
					}

					if (strTemp.length() == 1) {
						strTemp.append("0");
					}

					sbResult.append(strTemp.reverse());
				}

				iByte = 0;
				j = 0;
			}
		}
		return sbResult.toString();
	}

	/**
	 * Formats the specified data using the first date format parameter.
	 * This method synchronizes on the date format to ensure that only
	 * one thread at a time uses it.
	 * 
	 * @param format Date format to use. Can be null to return null.
	 * @param date Date to be formatted. Can be null to return null.
	 * 
	 * @return The string with the formatted date.
	 */
	public static String formatDate(final DateFormat format, final Date date) {
		String strRet = null;

		if (format != null && date != null) {
			synchronized (format) {
				strRet = format.format(date);
			}
		}

		return strRet;
	}
}
