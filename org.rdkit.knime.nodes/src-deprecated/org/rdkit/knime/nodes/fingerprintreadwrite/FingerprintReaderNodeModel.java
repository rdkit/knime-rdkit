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
package org.rdkit.knime.nodes.fingerprintreadwrite;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is the model implementation of FingerprintReader.
 * 
 * @author Sudip Ghosh
 */
@Deprecated
public class FingerprintReaderNodeModel extends NodeModel {

	//
	// Constants
	//

	/**
	 * Logger instance for logging purpose.
	 */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(FingerprintReaderNodeModel.class);

	/**
	 * Instance for FPS Column spec.
	 */
	private static final DataColumnSpecCreator FPS = new DataColumnSpecCreator(
			"Fingerprint", DenseBitVectorCell.TYPE);

	/**
	 * Instance for ID Column spec.
	 */
	private static final DataColumnSpecCreator ID = new DataColumnSpecCreator(
			"Identifier", StringCell.TYPE);

	/**
	 * Instance for fingerprint table spec.
	 */
	private static final DataTableSpec SPEC_TWO_COLUMN = new DataTableSpec(
			FPS.createSpec(), ID.createSpec());

	private static final String BLANK = "";
	private static final String DELIMA = "=";
	private static final String DELIMB = "\t";

	//
	// Members
	//

	/**
	 * Instance for filename setting model.
	 */
	private final SettingsModelString m_filename = new SettingsModelString(
			"filename", null);

	/**
	 * Instance for SetID setting model.
	 */
	private final SettingsModelBoolean m_setID = new SettingsModelBoolean(
			"setID", false);




	//
	// Constructor
	//

	/**
	 * Creates one out port for the fingerprint reader node.
	 */
	public FingerprintReaderNodeModel() {
		super(0, 1);
	}


	//
	// Protected Methods
	//

	/**
	 * This method is used to specify the structure the output table. The output
	 * table will have two columns of type String and DenseBitVectorCell
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		final StringBuilder warningMessage = new StringBuilder();
		final String fileS = m_filename.getStringValue();
		final File file = FPSReadWriteUtil.checkFile(fileS, true, false);
		if (!file.exists()) {
			warningMessage.append("Specified input file doesn't exist.");
		}
		if (!file.isFile() || !file.canRead()) {
			warningMessage
			.append("Specified input file is not a readable file.");
		}
		if (warningMessage.length() > 0) {
			setWarningMessage(warningMessage.toString());
		}
		return new DataTableSpec[] { SPEC_TWO_COLUMN };
	}

	/**
	 * The execute method reads the hex encoded fingerprints from the fps file,
	 * converts it into binary representation and likewise populates the
	 * DenseBitVectorCell.
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		LOGGER.debug("Enter Execute: reading fingerprints....");

		BufferedDataContainer cont = null;
		final boolean setId = m_setID.getBooleanValue();

		cont = exec.createDataContainer(SPEC_TWO_COLUMN);

		final File f = FPSReadWriteUtil.checkFile(m_filename.getStringValue(), true,
				false);
		BufferedReader in;

		// the below code is to create a BufferedReader object which will
		// support zip file reading

		if (f.getName().endsWith(".gz")) {
			in = new BufferedReader(new InputStreamReader(new GZIPInputStream(
					new FileInputStream(f))));
		} else {
			in = new BufferedReader(new FileReader(f));
		}
		// To calculate the number of line in the file for progress status.
		final LineNumberReader lnr = new LineNumberReader(new FileReader(f));
		
		try {
			lnr.skip(f.length() - 1);
			final int nLines = lnr.getLineNumber();
	
			String str;
			int iCount = 1;
			int iFingCount = 0;
			final Map<String, String> mapFpsHeaders = new LinkedHashMap<String, String>();
			final Set<String> setIdentifier = new HashSet<String>();
			while ((str = in.readLine()) != null) {
	
				if (!str.trim().equals(BLANK)) {
					if (iCount == 1 && (!str.contains(DELIMA))) {
						mapFpsHeaders.put("version", "FPS1");
					}
	
					// code to read key value pairs of the FPS headers
					if (str.charAt(0) == '#') {
						final StringTokenizer st = new StringTokenizer(str, DELIMA);
						if (st.hasMoreTokens() && st.countTokens() > 1) {
							final String strKey = st.nextToken();
							final String strVal = st.nextToken();
							if (strKey != null && !strKey.trim().equals(BLANK)) {
								mapFpsHeaders.put(
										strKey.trim().substring(1,
												strKey.trim().length()),
												strVal.trim());
							}
						}
					} else {// code to read fingerprint records
	
						if (FPSReadWriteUtil.fingerprintValidator(str,
								mapFpsHeaders, setId, setIdentifier)) {
	
							final StringTokenizer st = new StringTokenizer(str, DELIMB);
							if (st.hasMoreTokens() && st.countTokens() > 1) {
								final String strVal = st.nextToken();
								final String strKey = st.nextToken();
								final long[] arrBinBits = FPSReadWriteUtil
										.convertBinaryFingerprintsFromHex(strVal
												.trim());
								DataCell[] cells = null;
	
								cells = new DataCell[2];
								cells[1] = new StringCell(strKey);
	
								final DenseBitVector bitVector = new DenseBitVector(
										Integer.parseInt(mapFpsHeaders
												.get("num_bits")));
	
								final int iLastPos = arrBinBits.length - 1;
								for (int i = 0; i <= iLastPos; i++) {
									if (arrBinBits[i] == 0) {
										bitVector.set(iLastPos - i, false);
									} else {
										bitVector.set(iLastPos - i, true);
									}
	
								}
								final DenseBitVectorCellFactory fact = new DenseBitVectorCellFactory(
										bitVector);
								cells[0] = fact.createDataCell();
	
								if (setId) {
									cont.addRowToTable(new DefaultRow(strKey, cells));
								} else {
									cont.addRowToTable(new DefaultRow(new RowKey(
											"Row" + iFingCount), cells));
								}
	
								iFingCount++;
							}
						} else {
							LOGGER.error("Fingerprint record format is not correct.");
							throw new InvalidSettingsException(
									"Fingerprint record format is not correct.");
						}
					}
				}
				exec.checkCanceled();
				exec.setProgress(iCount / (double) nLines, "Parsing FPS file row "
						+ iCount + " of " + nLines);
				iCount++;
	
			}
	
			cont.close();
			in.close();
			
			if (iFingCount == 0) {
				throw new InvalidSettingsException(
						"No fingerprint records present in the File");
			}
		}
		finally {
			lnr.close();
		}

		LOGGER.debug("Exit Execute: reading fingerprints....");
		return new BufferedDataTable[] { cont.getTable() };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_filename.loadSettingsFrom(settings);
		m_setID.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		m_filename.saveSettingsTo(settings);
		m_setID.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		final SettingsModelString temp = new SettingsModelString("filename", null);
		temp.loadSettingsFrom(settings);
		final String fileS = temp.getStringValue();
		FPSReadWriteUtil.checkFile(fileS, true, false);
	}

}
