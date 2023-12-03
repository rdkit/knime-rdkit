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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * This is the model implementation of FingerprintWriter node.
 * It species the input/output ports of the node. It implements
 * the logic to write the fingerprints to an ouput FPS file.
 *
 * @author Dillip K Mohanty
 */
@Deprecated
public class FingerprintWriterNodeModel extends NodeModel {

	//
	// Constants
	//

	/**
	 * Logger instance for logging purpose.
	 */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(FingerprintWriterNodeModel.class);

	/** String history identifier **/
	static final String HISTORY_ID = "fps_writer";

	/** Config identifier for fps column. */
	static final String CFG_FPS_COLUMN = "fps_column";
	/** Config identifier for id column. */
	static final String CFG_ID_COLUMN = "id_column";
	/** Config identifier for target file. */
	static final String CFG_TARGET_FILE = "output_file";
	/** Config identifier for overwrite OK. */
	static final String CFG_OVERWRITE_OK = "overwriteOK";
	/** Config identifier for overwrite OK. */
	static final String CFG_WRITE_ROWID = "writeRowid";


	//
	// Members
	//

	/**
	 * Instance for selected fingerprint column name.
	 */
	private String m_fpsColumn;

	/**
	 * Instance for selected id column name.
	 */
	private String m_idColumn;

	/**
	 * Instance for output file name.
	 */
	private String m_outputFile;

	/**
	 * boolean Instance for overwrite checkbox.
	 */
	private boolean m_overwriteOK;

	/**
	 * boolean Instance for write row id checkbox.
	 */
	private boolean m_writeRowid;


	//
	// Constructor
	//

	/** constructor to specify the input/output ports. This node contains one input, no output ports. */
	public FingerprintWriterNodeModel() {
		super(1, 0);
	}

	//
	// Protected Methods
	//

	/**
	 * The configure method is used to auto-configure and auto-guessing of the configuration settings.
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		final DataTableSpec in = inSpecs[0];
		if (!in.containsCompatibleType(BitVectorValue.class)) {
			throw new InvalidSettingsException("No fingerprint column in input table.");
		}
		final StringBuilder warningMessage = new StringBuilder();
		final File file = FPSReadWriteUtil.checkFile(m_outputFile, false, false);
		if (file.isDirectory()) {
			throw new InvalidSettingsException("Specified location  is a "
					+ "directory (\"" + file.getAbsolutePath() + "\").");
		}
		if (file.exists()) {
			if (!file.canWrite()) {
				throw new InvalidSettingsException("Cannot write to existing "
						+ "file \"" + file.getAbsolutePath() + "\".");
			}
			if (m_overwriteOK) {
				warningMessage.append("Output file \""
						+ file.getAbsolutePath()
						+ "\" exists and will be overwritten.");
			} else {
				throw new InvalidSettingsException("File exists and can't be "
						+ "overwritten, check dialog settings");
			}
		} else {
			final File parentDir = file.getParentFile();
			if (parentDir == null) {
				throw new InvalidSettingsException("Can't determine parent "
						+ "directory of file \"" + file + "\"");
			}
			if (!parentDir.exists()) {
				warningMessage.append(
						"Directory of specified output file doesn't exist"
								+ " and will be created.");
			}
		}


		if (m_fpsColumn == null) { // auto - configure
			int fpsColCount = 0;
			for (int i = 0; i < in.getNumColumns(); i++) {
				final DataColumnSpec s = in.getColumnSpec(i);
				if (s.getType().isCompatible(BitVectorValue.class)) {
					if (m_fpsColumn == null) {
						m_fpsColumn = in.getColumnSpec(i).getName();
					}
				}
				fpsColCount++;
			}
			if (fpsColCount > 1) {
				if (warningMessage.length() > 0) {
					// that hardly ever happens, the file location is not null
					// but the m_fpsColumn is
					warningMessage.append('\n');
				}
				warningMessage.append("More than one fingerprint(DenseBitVector) compatible column in "
						+ "input, using column \"" + m_fpsColumn + "\".");
			}
		}
		assert m_fpsColumn != null;
		final DataColumnSpec target = in.getColumnSpec(m_fpsColumn);
		if (target == null) {
			throw new InvalidSettingsException(
					"Column \"" + m_fpsColumn
					+ "\" not contained in input, please configure");
		}
		if (!target.getType().isCompatible(BitVectorValue.class)) {
			throw new InvalidSettingsException(
					"Invalid type of selected column \"" + m_fpsColumn
					+ "\", expected fingerprint(DenseBitVector) compatible type");
		}
		if (warningMessage.length() > 0) {
			setWarningMessage(warningMessage.toString());
		}
		return new DataTableSpec[0];
	}

	/**
	 * The execute method reads the fingerprints from the input table,
	 * converts it into hex encoding string and writes it to a FPS file.
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {

		LOGGER.debug("Enter Execute: Writing fingerprints to a FPS file....");
		final File outFile = FPSReadWriteUtil.checkFile(m_outputFile, false, false);
		//Check if file exists and can be overwritten
		if (outFile.exists() && !m_overwriteOK) {
			throw new InvalidSettingsException("File exists and can't be "
					+ "overwritten, check dialog settings");
		}
		//Check if parent folder exists. Create one if not present.
		final File parentDir = outFile.getParentFile();
		if (!parentDir.exists()) {
			if (!parentDir.mkdirs()) {
				throw new IllegalStateException("Unable to create directory"
						+ " for specified output file: "
						+ parentDir.getAbsolutePath());
			}
			LOGGER.info("Created directory for specified output file: " + parentDir.getAbsolutePath());
		}

		final BufferedWriter outWriter = new BufferedWriter(new FileWriter(outFile));
		final BufferedDataTable in = inData[0];
		final int colIndexFps = in.getDataTableSpec().findColumnIndex(m_fpsColumn);
		final int colIndexId = in.getDataTableSpec().findColumnIndex(m_idColumn);
		final double count = in.size();
		int i = 0;
		int readCount = 0;
		String id = "";
		try {
			//Iterate over the data rows to read the fingerprints from input table
			for (final DataRow r : in) {

				final DataCell cell = r.getCell(colIndexFps);
				//Read the ids either from rowkey or from id column.
				if(!m_writeRowid && colIndexId != -1) {
					final DataCell idCell = r.getCell(colIndexId);
					if (!idCell.isMissing()) {
						id = ((StringCell)idCell).getStringValue();
					} else {
						id = "id" + Math.random();
					}
				} else {
					id = r.getKey().getString();
				}
				if (!cell.isMissing()) {
					//increase the read count
					readCount++;
					//Read the fingerprint cell.
					final DenseBitVectorCell bvc = (DenseBitVectorCell)cell;
					final DenseBitVector bv = bvc.getBitVectorCopy();
					final int len = (int) bv.length();
					final long[] bitArr = new long[len];
					//prepare the bit array to pass to FPDReadWriteUtil for conversion to hex encoded strings.
					for(int k=0; k <len; k++){
						if(bv.get(k)) {
							bitArr[(len-1)-k] = 1;
						} else {
							bitArr[(len-1)-k] = 0;
						}
					}
					//Convert Binary fingerprints to Hex encoded Strings

					final String toString = FPSReadWriteUtil.convertHexFingerprintsFromBin(bitArr);
					//Write the fingerprint records and headers to FPS file
					if(i == 0) {
						//Write the headers first.
						outWriter.write("#FPS1");
						outWriter.newLine();
						outWriter.write("#num_bits=" + len);
						outWriter.newLine();
						outWriter.write("#software=Knime/" + KNIMEConstants.VERSION );
						outWriter.newLine();
						outWriter.write("#date="+ FPSReadWriteUtil.getDateInFPSFormat());
						outWriter.newLine();
					}
					//Write fingerprint records
					outWriter.write(toString + "\t" + id);
					outWriter.newLine();
				}
				i++;
				exec.checkCanceled();
				exec.setProgress(i / count, "Writing row " + i + " (\"" + r.getKey() + "\")");
			}
			//Set warning of empty FPS file creation due to no input fingerprints.
			if (readCount == 0) {
				setWarningMessage("Node created an empty FPS file.");
			}
		} finally {
			//Close the writer
			outWriter.close();
		}
		LOGGER.debug("Exit Execute: Writing fingerprints to a FPS file....");
		return new BufferedDataTable[0];
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File nodeInternDir,
			final ExecutionMonitor exec)
					throws IOException, CanceledExecutionException {
		// nothing to do
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
			final ExecutionMonitor exec)
					throws IOException, CanceledExecutionException {
		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		if (m_fpsColumn != null) {
			settings.addString(CFG_FPS_COLUMN, m_fpsColumn);
			settings.addString(CFG_ID_COLUMN, m_idColumn);
			settings.addString(CFG_TARGET_FILE, m_outputFile);
			settings.addBoolean(CFG_OVERWRITE_OK, m_overwriteOK);
			settings.addBoolean(CFG_WRITE_ROWID, m_writeRowid);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		settings.getString(CFG_FPS_COLUMN);
		settings.getString(CFG_ID_COLUMN);
		settings.getString(CFG_TARGET_FILE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_fpsColumn = settings.getString(CFG_FPS_COLUMN);
		m_idColumn = settings.getString(CFG_ID_COLUMN);
		m_outputFile = settings.getString(CFG_TARGET_FILE);
		m_overwriteOK = settings.getBoolean(CFG_OVERWRITE_OK, true);
		m_writeRowid = settings.getBoolean(CFG_WRITE_ROWID, true);
	}

}


