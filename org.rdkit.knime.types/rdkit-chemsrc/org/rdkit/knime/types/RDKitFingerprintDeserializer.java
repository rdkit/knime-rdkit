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
