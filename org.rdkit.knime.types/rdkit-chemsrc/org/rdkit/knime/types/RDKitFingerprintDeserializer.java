/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C)2023
 *  Novartis Pharma AG, Switzerland
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
 * -------------------------------------------------------------------
 *
 */
package org.rdkit.knime.types;

import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;
import org.knime.core.node.NodeLogger;
import org.knime.python.typeextension.Deserializer;
import org.knime.python.typeextension.DeserializerFactory;

/**
 * Deserializer implementation for converting Python RDKit ExplicitBitVect type into KNIME Fingerprint cells.
 * 
 * @author Manuel Schwarze
 */
public class RDKitFingerprintDeserializer implements Deserializer {

	//
	// Factory Class
	//

	/**
	 * Factory class for creating the RDKitFingerprintDeserializer
	 * 
	 * @author Manuel Schwarze
	 */
	public static final class Factory extends DeserializerFactory {

		public Factory() {
			super(DenseBitVectorCell.TYPE);
		}

		@Override
		public Deserializer createDeserializer() {
			return new RDKitFingerprintDeserializer();
		}
	}

	//
	// Constants
	//

	/** The logging instance. */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(RDKitFingerprintDeserializer.class);

	//
	// Public Methods
	//

	@Override
	public DataCell deserialize(final byte[] bytes, final FileStoreFactory fileStoreFactory) throws IOException {
		DataCell cell;

		// Generate missing cell, if input is unavailable
		if (bytes == null || bytes.length == 0) {
			cell = DataType.getMissingCell();
		}

		// Generate a KNIME Fingerprint from a serialized RDKit Fingerprint in BitString Format
		else {
			try {
				final DenseBitVector bv = new DenseBitVector(bytes.length);
				final byte setBit = (byte)'1';

				// Note: RDKit BitString is read from left to right
				// while KNIME FP BitString are read from right to left!
				// It was decided to mirror fingerprints produced by RDKit
				final int lenMinus1 = bytes.length - 1;
				for (int i = 0; i < bytes.length; i++) {
					if (bytes[lenMinus1 - i] == setBit) {
						bv.set(i);
					}
				}
				cell = new DenseBitVectorCellFactory(bv).createDataCell();
			}
			catch (final Exception exc) {
				LOGGER.debug(exc);
				// In case of an error throw an IOException
				String strMsg = exc.getMessage();
				if (strMsg == null || strMsg.trim().isEmpty()) {
					strMsg = "Unknown error";
				}
				throw new IOException("Unable to interpret RDKit Fingerprint: " + strMsg);
			}
		}

		return cell;
	}

}
