package org.rdkit.knime.types;

import java.io.IOException;

import org.RDKit.ChemicalReaction;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.NodeLogger;
import org.knime.python.typeextension.Deserializer;
import org.knime.python.typeextension.DeserializerFactory;

/**
 * Deserializer implementation for converting Python ChemicalReaction type into RDKit Chemical Reaction cells.
 * 
 * @author Manuel Schwarze
 */
public class RDKitReactionDeserializer implements Deserializer {

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
			super(RDKitReactionCell.TYPE);
		}

		@Override
		public Deserializer createDeserializer() {
			return new RDKitReactionDeserializer();
		}
	}

	//
	// Constants
	//

	/** The logging instance. */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(RDKitReactionDeserializer.class);

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

		// Generate an RDKit Reaction Cell
		else {
			try {
				final ChemicalReaction reaction = RDKitReactionCell.toChemicalReaction(bytes);
				cell = new RDKitReactionCell(reaction);
			}
			catch (final Exception exc) {
				LOGGER.debug(exc);
				// In case of an error throw an IOException
				String strMsg = exc.getMessage();
				if (strMsg == null || strMsg.trim().isEmpty()) {
					strMsg = "Unknown error";
				}
				throw new IOException("Unable to interpret RDKit Reaction: " + strMsg);
			}
			// Note: Do not delete the reaction object here, because it is used in the cell
		}

		return cell;
	}

}
