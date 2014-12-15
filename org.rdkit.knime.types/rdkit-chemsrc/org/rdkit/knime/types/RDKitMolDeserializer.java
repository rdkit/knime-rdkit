package org.rdkit.knime.types;

import java.io.IOException;

import org.RDKit.ROMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.NodeLogger;
import org.knime.python.typeextension.Deserializer;
import org.knime.python.typeextension.DeserializerFactory;

/**
 * Deserializer implementation for converting Python RDKit Molecules into RDKit Mol Cells.
 * 
 * @author Manuel Schwarze
 */
public class RDKitMolDeserializer implements Deserializer {

	//
	// Factory Class
	//

	/**
	 * Factory class for creating the RDKitMolDeserializer
	 * 
	 * @author Manuel Schwarze
	 */
	public static final class Factory extends DeserializerFactory {

		public Factory() {
			super(RDKitMolCell2.TYPE);
		}

		@Override
		public Deserializer createDeserializer() {
			return new RDKitMolDeserializer();
		}
	}

	//
	// Constants
	//

	/** The logging instance. */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(RDKitMolDeserializer.class);

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

		// Generate an RDKit Mol Cell with canonicalized SMILES attached
		else {
			ROMol mol = null;
			try {
				mol = RDKitMolCell2.toROMol(bytes);
				cell = new RDKitMolCell2(mol, null);
			}
			catch (final Exception exc) {
				LOGGER.debug(exc);

				// In case of an error throw an IOException
				String strMsg = exc.getMessage();
				if (strMsg == null || strMsg.trim().isEmpty()) {
					strMsg = "Unknown error";
				}
				throw new IOException("Unable to interpret RDKit Molecule: " + strMsg);

			}
			finally {
				if (mol != null) {
					mol.delete();
				}
			}
		}

		return cell;
	}

}
