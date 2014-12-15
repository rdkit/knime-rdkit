package org.rdkit.knime.types;

import java.io.IOException;

import org.RDKit.ChemicalReaction;
import org.knime.python.typeextension.Serializer;
import org.knime.python.typeextension.SerializerFactory;

/**
 * Serializer implementation for converting RDKit Chemical Reaction Cells into exchange format for Python code.
 * 
 * @author Manuel Schwarze
 */
public class RDKitReactionSerializer implements Serializer<RDKitReactionValue> {

	//
	// Factory Class
	//

	/**
	 * Factory class for creating the RDKitReactionSerializer
	 * 
	 * @author Manuel Schwarze
	 */
	public static final class Factory extends SerializerFactory<RDKitReactionValue> {

		public Factory() {
			super(RDKitReactionValue.class);
		}

		@Override
		public Serializer<? extends RDKitReactionValue> createSerializer() {
			return new RDKitReactionSerializer();
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
	public byte[] serialize(final RDKitReactionValue value) throws IOException {
		byte[] arrBinaryReaction = null;

		if (value != null) {
			final ChemicalReaction reaction = value.getReactionValue();
			arrBinaryReaction = RDKitReactionCell.toByteArray(reaction);
			// Note: Do not delete the reaction object here, because it is a reference to real cell content
		}

		return arrBinaryReaction;
	}
}
