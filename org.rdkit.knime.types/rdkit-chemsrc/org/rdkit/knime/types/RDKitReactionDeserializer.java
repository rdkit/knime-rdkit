package org.rdkit.knime.types;

import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.filestore.FileStoreFactory;
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
	// Public Methods
	//

	@Override
	public DataCell deserialize(final byte[] bytes, final FileStoreFactory fileStoreFactory) throws IOException {
		return RDKitTypeSerializationUtils.deserializeReactionCell(bytes);
	}

}
