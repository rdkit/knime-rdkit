/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2012-2023
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

package org.rdkit.knime.nodes.fingerprintreader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.node.table.reader.preview.dialog.GenericItemAccessor;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class implements the node model of the RDKitFingerprintReader node
 * providing calculations based on the open source RDKit library.
 * 
 * @author Manuel Schwarze
 * @author Roman Balabanov
 */
public class RDKitFingerprintReaderV2NodeModel extends AbstractRDKitNodeModel {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitFingerprintReaderV2NodeModel.class);

	/** Warning context for salts. */
	protected static final WarningConsolidator.Context FP_CONTEXT =
			new WarningConsolidator.Context("Fingerprint", "fingerprint", "fingerprints", true);

	//
	// Members
	//

	/** The output table port index */
	private final int m_iOutputTablePortIdx;

	/** Settings model for the input file path. */
	private final SettingsModelReaderFileChooser m_modelFilePath;

	/** Settings model for the option to use IDs read from the fingerprint file as row IDs. */
	private final SettingsModelBoolean m_modelUseIdsFromFileAsRowIds =
			registerSettings(RDKitFingerprintReaderV2NodeDialog.createUseIdsFromFileAsRowIdsModel());

	//
	// Internals
	//

	private long m_lReadFingerprintLines = 0;

	//
	// Constructor
	//

	/**
	 * Constructs new {@code RDKitFingerprintReaderV2NodeModel} instance with configuration specified.
	 *
	 * @param nodeCreationConfig Node Creation Configuration instance.
	 *                           Mustn't be null.
	 * @throws IllegalStateException if {@code nodeCreationConfig} is null or malformed.
	 */
	RDKitFingerprintReaderV2NodeModel(NodeCreationConfiguration nodeCreationConfig) {
		super(nodeCreationConfig);

		m_iOutputTablePortIdx = getOutputTablePortIndexes(nodeCreationConfig,
				RDKitFingerprintReaderV2NodeFactory.OUTPUT_PORT_GRP_ID_FINGERPRINT_TABLE)[0];

		m_modelFilePath = registerSettings(
				RDKitFingerprintReaderV2NodeDialog.createInputPathModel(nodeCreationConfig));

		getWarningConsolidator().registerContext(FP_CONTEXT);
	}

	//
	// Protected Methods
	//

	/**
	 * KNIME File Handling API messages handler.
	 *
	 * @param statusMessage A message received from KNIME File Handling API.
	 *                      Can be null.
	 */
	protected void onStatusMessage(StatusMessage statusMessage) {
		if (statusMessage != null && statusMessage.getMessage() != null) {
			switch (statusMessage.getType()) {
				case ERROR -> getWarningConsolidator().saveWarning(statusMessage.getMessage());
				case WARNING -> LOGGER.warn(statusMessage.getMessage());
				case INFO -> LOGGER.info(statusMessage.getMessage());
			}
		}
	}

	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
			throws InvalidSettingsException {
		m_modelFilePath.configureInModel(inSpecs, this::onStatusMessage);

		return super.configure(inSpecs);
	}

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		// Reset warnings and check RDKit library readiness
		super.configure(inSpecs);

		// Perform checks on the specified input file
		try (final GenericItemAccessor<FSPath> itemAccessor = m_modelFilePath.createItemAccessor()) {
			Files.exists(itemAccessor.getRootItem(this::onStatusMessage));
		}
		catch (IOException e) {
			getWarningConsolidator().saveWarning("Failed to access input file: " + e.getMessage());
		}

		// Consolidate all warnings and make them available to the user
		generateWarnings();

		// Generate output specs
		return getOutputTableSpecs(inSpecs);
	}

	/**
	 * This implementation returns always null as we do not have any input tables.
	 * {@inheritDoc}
	 * @return Always null.
	 */
	@Override
	protected InputDataInfo[] createInputDataInfos(final int inPort, final DataTableSpec inSpec)
			throws InvalidSettingsException {
		return null;
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

        if (outPort == m_iOutputTablePortIdx) {
            // Define output table
            spec = new DataTableSpec("Fingerprints",
                    new DataColumnSpecCreator("Fingerprint", DenseBitVectorCell.TYPE).createSpec(),
                    new DataColumnSpecCreator("Identifier", StringCell.TYPE).createSpec()
            );
        }

        return spec;
	}

	@Override
	protected BufferedDataTable[] processing(final BufferedDataTable[] inData, final InputDataInfo[][] arrInputDataInfo,
			final ExecutionContext exec) throws Exception {
		final DataTableSpec[] arrOutSpecs = getOutputTableSpecs(inData);

		// Contains the rows with the result column
		final BufferedDataContainer newTableData = exec.createDataContainer(arrOutSpecs[m_iOutputTablePortIdx]);

		// Input file can either be a text file or a zipped text file
		try (final GenericItemAccessor<FSPath> pathFpsAccessor = m_modelFilePath.createItemAccessor()) {
			// Prepare all settings and pre-requisites
			final Path pathFps = pathFpsAccessor.getRootItem(this::onStatusMessage);
			final boolean bUseFileIds = m_modelUseIdsFromFileAsRowIds.getBooleanValue();

			// Read from input file
			final long iFileLength = Files.size(pathFps);
			m_lReadFingerprintLines = 0;

			// Input file can either be a text file or a zipped text file
			try (final BufferedReader reader = pathFps.endsWith(".gz")
					? new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(pathFps))))
					: Files.newBufferedReader(pathFps)
			) {
				int iNumberOfBits = -1; // Undefined
				int iLineNumber = 0;
				int iAddedFingerprints = 0;
				String strLine;

				// Process the file line by line
				while ((strLine = reader.readLine()) != null) {
					iLineNumber++;
					strLine = strLine.trim();

					// Skip empty lines
					if (strLine.isEmpty()) {
						continue;
					}

					// Process a header line
					if (strLine.startsWith("#")) {
						strLine = strLine.substring(1);
						final String[] keyValue = strLine.split("=");

						// Read number of fingerprint bit, skip all other information
						if (keyValue.length == 2 && "num_bits".equals(keyValue[0])) {
							if (iNumberOfBits != -1) {
								throw new RuntimeException(
										"The header num_bits of the fingerprint file exists multiple times.");
							}
							try {
								iNumberOfBits = Integer.parseInt(keyValue[1]);
							} catch (final NumberFormatException excParse) {
								throw new NumberFormatException(
										"The header num_bits of the fingerprint file contains " +
												"an invalid number.");
							}
						}
					}

					// Read normal line and add it to the table
					else {
						m_lReadFingerprintLines++;
						final StringTokenizer st = new StringTokenizer(strLine, "\t", false);

						if (st.countTokens() > 1) {
							final String strFingerprint = st.nextToken().trim();
							final String strId = st.nextToken().trim();

							try {
								final DenseBitVector dbvFingerprint = convertFromFpsFormat(strFingerprint, iNumberOfBits);

								// If there was no num_bits header defined, we take
								// that information from the first fingerprint
								if (iNumberOfBits == -1) {
									iNumberOfBits = (int) dbvFingerprint.length();
								}

								// Create row id
								final RowKey rowKey = (bUseFileIds ?
										new RowKey(strId) :
										new RowKey("Row" + iAddedFingerprints));

								final DataRow row = new DefaultRow(rowKey,
										new DenseBitVectorCellFactory(dbvFingerprint).createDataCell(),
										new StringCell(strId));

								try {
									newTableData.addRowToTable(row);
									iAddedFingerprints++;
								} catch (final Exception exc) {
									// If the unique row id exists already it will fail here
									LOGGER.warn("Fingerprint in line " + iLineNumber + " has a duplicated identifier - skipping it.");
									getWarningConsolidator().saveWarning(FP_CONTEXT.getId(),
											"Skipped fingerprint with duplicated identifier. Consider turning off the option to use it as row ID.");
								}
							} catch (final NumberFormatException excFormat) {
								LOGGER.warn(excFormat.getMessage() + " (Line " + iLineNumber + ")");
								getWarningConsolidator().saveWarning(FP_CONTEXT.getId(),
										"Encountered an invalid fingerprint size. Skipping this fingerprint.");
							} catch (final Exception exc) {
								LOGGER.warn("Invalid fingerprint encountered in line " + iLineNumber);
								getWarningConsolidator().saveWarning(FP_CONTEXT.getId(),
										"Encountered an invalid fingerprint. Skipping this fingerprint.");
							}

							// Check, if user cancelled and report progress every 20 lines
							if (iLineNumber % 20 == 0) {
								exec.checkCanceled();

								final String sbMsg = "Processed " + m_lReadFingerprintLines + " fingerprints " +
										"('" + (iAddedFingerprints - m_lReadFingerprintLines) + " of them are invalid)";

								exec.setProgress(iLineNumber / (double) iFileLength / strLine.length(),
										sbMsg);
							}
						} else {
							getWarningConsolidator().saveWarning(FP_CONTEXT.getId(),
									"Encountered an fingerprint without identifier - skipping it.");
						}
					}
				}
			}
		}
		catch (final IOException excIo) {
			throw new IOException("The fingerprint file could not be read successfully: " + excIo, excIo);
		}

		exec.checkCanceled();
		exec.setProgress(1.0, "Finished Processing");

		newTableData.close();

		final BufferedDataTable[] arrResult = new BufferedDataTable[arrOutSpecs.length];
		arrResult[m_iOutputTablePortIdx] = newTableData.getTable();
		return arrResult;
	}

	/**
	 * {@inheritDoc}
	 * This implementation considers the number of processed fingerprints.
	 */
	@Override
	protected Map<String, Long> createWarningContextOccurrencesMap(
			final BufferedDataTable[] inData, final InputDataInfo[][] arrInputDataInfo,
			final BufferedDataTable[] resultData) {
		// We do not call super here, because it would fail due to missing
		// input tables in this node
		final Map<String, Long> map = new HashMap<>();
		map.put(FP_CONTEXT.getId(), m_lReadFingerprintLines);

		return map;
	}

	//
	// Static Public Methods
	//

	/**
	 * Converts the passed in hex string into a bit vector. The hex string
	 * which must be compatible to the FPS file format
	 * (see <a href="http://code.google.com/p/chem-fingerprints/wiki/FPS">FPS.wiki</a>).
	 * 
	 * @param strHexFpsFormat An FPS file format compatible fingerprint hex string.
	 * 		Must not be null.
	 * @param iForceBitLength If set to a value > 0 the input is checked and
	 * 		must be equal to this bit length. Specify -1 to skip the check.
	 * 
	 * @return Dense bit vector.
	 * 
	 * @throws NumberFormatException Thrown, if the bit length is not
	 * 		equal to the passed in parameter (only if set to > 0).
	 */
	public static DenseBitVector convertFromFpsFormat(final String strHexFpsFormat,
			final int iForceBitLength) throws NumberFormatException {
		String strHexFingerprint = strHexFpsFormat.trim().toUpperCase();
		final int iLen = strHexFingerprint.length();
		final int iNumBits = iLen * 4;
		int iCount = 0;

		// Add a "4 bits" to ensure that we can read enough bits
		// (since we read byte as pairs of 2 x 4 bits)
		strHexFingerprint += "0";

		// Check fingerprint size
		if (iForceBitLength > 0 && iNumBits != iForceBitLength) {
			throw new NumberFormatException("Invalid fingerprint size encountered and ignored: " +
					iNumBits + " instead of " + iForceBitLength + ".");
		}

		final DenseBitVector bitVector = new DenseBitVector(iNumBits);

		// Traverse through each character of Fingerprint, processing 2 char at
		// a time, convert to byte value for each character and generate 4 binary
		// bits per character
		for (int i = 0; i < iLen; i = i + 2) {
			for (int n = 1; n >= 0; n--) {
				char ch = strHexFingerprint.charAt(i + n);

				if (ch >= '0' && ch <= '9') {
					ch -= '0';
				}
				else if (ch >= 'A' && ch <= 'F') {
					ch -= 'A' - 10;
				}
				else {
					throw new NumberFormatException(
							"Invalid fingerprint character encountered and ignored: '" +
									ch + "' instead of 0..F.");
				}

				for (int bit = 1; bit < 16; bit = bit << 1) {
					bitVector.set(iNumBits - iCount - 1, (ch & bit) == bit);
					iCount++;
				}
			}
		}

		return bitVector;
	}
}
