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
