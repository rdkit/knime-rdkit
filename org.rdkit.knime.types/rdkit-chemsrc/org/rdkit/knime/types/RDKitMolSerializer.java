package org.rdkit.knime.types;

import java.io.IOException;

import org.knime.python.typeextension.Serializer;
import org.knime.python.typeextension.SerializerFactory;

/**
 * Serializer implementation for converting RDKit Mol Cells into exchange format for Python code.
 * 
 * @author Manuel Schwarze
 */
public class RDKitMolSerializer implements Serializer<RDKitMolValue> {

	//
	// Factory Class
	//

	/**
	 * Factory class for creating the RDKitMolSerializer
	 * 
	 * @author Manuel Schwarze
	 */
	public static final class Factory extends SerializerFactory<RDKitMolValue> {

		public Factory() {
			super(RDKitMolValue.class);
		}

		@Override
		public Serializer<? extends RDKitMolValue> createSerializer() {
			return new RDKitMolSerializer();
		}
	}

	//
	// Public Methods
	//

	/**
	 * The serialization considers only the byte content of the RDKit Molecule Cell, but
	 * not the SMILES value or canonical flag of the cell.
	 * 
	 * @param value RDKit Mol Value, usually an RDKit Mol Cell. Can be null.
	 * 
	 * @return Binary representation of the molecule. Null, if null was passed in.
	 */
	@Override
	public byte[] serialize(final RDKitMolValue value) throws IOException {
		return RDKitTypeSerializationUtils.serializeMolValue(value);
	}
}
