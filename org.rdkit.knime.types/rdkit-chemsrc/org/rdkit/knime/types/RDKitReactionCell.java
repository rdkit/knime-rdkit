/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2010
 *  Novartis Institutes for BioMedical Research
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

import java.io.DataOutput;
import java.io.IOException;

import org.RDKit.ChemicalReaction;
import org.RDKit.Int_Vect;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;

/**
 *
 * @author Greg Landrum
 */
public class RDKitReactionCell extends DataCell implements RDKitReactionValue, StringValue
{
	/**
	 * Convenience access member for
	 * <code>DataType.getType(RDKitMolCell.class)</code>.
	 *
	 * @see DataType#getType(Class)
	 */
	public static final DataType TYPE = DataType
			.getType(RDKitReactionCell.class);

	private static final long serialVersionUID = 0x1;

   /** The serializer instance. */
   @Deprecated
	private static final RDKitReactionSerializer SERIALIZER =
			new RDKitReactionSerializer();

	/**
	 * Returns the factory to read/write DataCells of this class from/to a
	 * DataInput/DataOutput. This method is called via reflection.
	 *
	 * @return a serializer for reading/writing cells of this kind
	 * @see DataCell
    * @deprecated As of KNIME 3.0 data types are registered via extension point. This method
    *     is not used anymore. The serializer is made known in the extension point configuration.
	 */
	public static final RDKitReactionSerializer getCellSerializer() {
		return SERIALIZER;
	}

	private final String m_smilesString;

	private final ChemicalReaction m_rxn;

	/**
	 * Creates a new Smiles Cell based on the given String value. <br />
	 * <b>Note</b>: The serializing technique writes the given String to a
	 * {@link DataOutput} using the {@link DataOutput#writeUTF(String)} method.
	 * The implementation is limited to string lengths of at most 64kB - (in UTF
	 * format - which may be not equal to the number of characters in the
	 * string).
	 *
	 * @param mol the ROMol value to store
	 * @throws NullPointerException if the given String value is
	 *             <code>null</code>
	 */
	public RDKitReactionCell(final ChemicalReaction rxn) {
		if (rxn == null) {
			throw new NullPointerException("Rxn value must not be null.");
		}
		m_rxn = rxn;
		m_smilesString = ChemicalReaction.ReactionToSmarts(rxn);
	}

	/**
	 * Creates a new Smiles Cell based on the given String value. <br />
	 * <b>Note</b>: The serializing technique writes the given String to a
	 * {@link DataOutput} using the {@link DataOutput#writeUTF(String)} method.
	 * The implementation is limited to string lengths of at most 64kB - (in UTF
	 * format - which may be not equal to the number of characters in the
	 * string).
	 *
	 * @param str the String value to store
	 * @throws NullPointerException if the given String value is
	 *             <code>null</code>
	 */
	public RDKitReactionCell(final String smarts) {
		if (smarts == null) {
			throw new NullPointerException("Smarts value must not be null.");
		}
		m_rxn = ChemicalReaction.ReactionFromSmarts(smarts);
		if (m_rxn == null) {
			throw new NullPointerException("could not process reaction");
		}
		m_smilesString = "sorry";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getStringValue() {
		return m_smilesString;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSmilesValue() {
		return m_smilesString;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ChemicalReaction getReactionValue() {
		return m_rxn;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return getStringValue();
	}

   @Override
   protected void finalize() throws Throwable {
      m_rxn.delete();
      super.finalize();
   }
   
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean equalsDataCell(final DataCell dc) {
		return m_smilesString.equals(((RDKitReactionCell)dc).m_smilesString);
	}

   @Override
   protected boolean equalContent(DataValue otherValue) {
      return RDKitReactionValue.equals(this, (RDKitReactionValue)otherValue);
   }
   
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return m_smilesString.hashCode();
	}

	protected static byte[] toByteArray(final ChemicalReaction reaction) {
		final Int_Vect iv = reaction.ToBinary();
		final byte[] bytes = new byte[(int)iv.size()];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte)iv.get(i);
		}
		return bytes;
	}

	protected static ChemicalReaction toChemicalReaction(final byte[] bytes) {
		final Int_Vect iv = new Int_Vect(bytes.length);
		for (int i = 0; i < bytes.length; i++) {
			iv.set(i, bytes[i]);
		}
		return ChemicalReaction.RxnFromBinary(iv);
	}

	/** Factory for (de-)serializing a RDKitMolCell. */
	public static class RDKitReactionSerializer implements
	DataCellSerializer<RDKitReactionCell> {
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void serialize(final RDKitReactionCell cell,
				final DataCellDataOutput output) throws IOException {
			// output.writeUTF(RDKFuncs.MolToBinary(cell.getMoleculeValue()));
			final Int_Vect cv = cell.getReactionValue().ToBinary();
			String pkl = "";
			for (int i = 0; i < cv.size(); ++i) {
				pkl += cv.get(i);
			}
			output.writeUTF(pkl);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public RDKitReactionCell deserialize(final DataCellDataInput input)
				throws IOException {
			final String s = input.readUTF();
			final Int_Vect cv = new Int_Vect(0);
			for (int i = 0; i < s.length(); ++i) {
				final char c = s.charAt(i);
				cv.add(c);
			}
			final ChemicalReaction rxn = ChemicalReaction.RxnFromBinary(cv);
			return new RDKitReactionCell(rxn);
		}
	}
}
