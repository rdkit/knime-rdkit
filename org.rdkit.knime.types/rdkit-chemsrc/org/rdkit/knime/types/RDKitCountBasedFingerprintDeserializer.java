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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.data.vector.bytevector.DenseByteVector;
import org.knime.core.data.vector.bytevector.DenseByteVectorCell;
import org.knime.core.data.vector.bytevector.DenseByteVectorCellFactory;
import org.knime.core.node.NodeLogger;
import org.knime.python.typeextension.Deserializer;
import org.knime.python.typeextension.DeserializerFactory;

/**
 * Deserializer implementation for converting Python RDKit count-based fingerprint types
 * UIntSparseIntVect, IntSparseIntVect and LongSparseIntVect into KNIME Fingerprint cells.
 * 
 * @author Manuel Schwarze
 */
public class RDKitCountBasedFingerprintDeserializer implements Deserializer {

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
			super(DenseByteVectorCell.TYPE);
		}

		@Override
		public Deserializer createDeserializer() {
			return new RDKitCountBasedFingerprintDeserializer();
		}
	}

	//
	// Constants
	//

	/** The logging instance. */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(RDKitCountBasedFingerprintDeserializer.class);

	/** RegEx to parse JSON produced by Python glue code. */
	private static final Pattern JSON_REGEX = Pattern.compile(".*?\\{.*?'length':.*?(\\d+).*?'bits':.*?\\{(.*?)\\}.*\\}");

	//
	// Public Methods
	//

	@Override
	public DataCell deserialize(final byte[] bytes, final FileStoreFactory fileStoreFactory) throws IOException {
		// Generate missing cell, if input is unavailable
		DataCell cell = DataType.getMissingCell();

		// Generate a KNIME Fingerprint from a serialized RDKit Fingerprint in special KNIME serializer format
		if (bytes != null && bytes.length > 0) {
			try {
				final String strJson = new String(bytes, "UTF-8");
				final Matcher matcher = JSON_REGEX.matcher(strJson);
				if (matcher.find()) {
					final int iLength = Integer.parseInt(matcher.group(1));
					final String strNonZeros = matcher.group(2);
					final String[] arrPairs = strNonZeros.split(",");
					final DenseByteVector byteVector = new DenseByteVector(iLength);
					for (final String strPair : arrPairs) {
						final String[] arrNumbers = strPair.split(":");
						final int iIndex = Integer.parseInt(arrNumbers[0].trim());
						final int iCount = Integer.parseInt(arrNumbers[1].trim());
						byteVector.set(iIndex, iCount);
					}

					cell = new DenseByteVectorCellFactory(byteVector).createDataCell();
				}
			}
			catch (final Exception exc) {
				LOGGER.debug(exc);
				LOGGER.debug("Got the following format: " + new String(bytes, "UTF-8"));

				// In case of an error throw an IOException
				String strMsg = exc.getMessage();
				if (strMsg == null || strMsg.trim().isEmpty()) {
					strMsg = "Unknown error";
				}
				throw new IOException("Unable to interpret RDKit Count-Based Fingerprint: " + strMsg);
			}
		}

		return cell;
	}
}
