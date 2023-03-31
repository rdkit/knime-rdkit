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

import java.io.IOException;

import org.RDKit.Int_Vect;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.rdkit.knime.types.preferences.RDKitDepicterPreferencePage;

/**
 * Implementation of an RDKit Molecule Cell. 
 *
 * @author Greg Landrum
 */
public class RDKitMolCell2 extends DataCell implements RDKitMolValue,
 SmilesValue, SdfValue, StringValue {

	/** Serial number. */
	private static final long serialVersionUID = -5474087790061027859L;

	private static final String SDF_POSTFIX = "\n$$$$\n";

	/**
	 * Convenience access member for
	 * <code>DataType.getType(RDKitMolCell2.class)</code>.
	 *
	 * @see DataType#getType(Class)
	 */
	static final DataType TYPE = DataType.getType(RDKitMolCell2.class);

   /** The serializer instance. */
   @Deprecated
	private static final RDKitSerializer SERIALIZER =
			new RDKitSerializer();

	/**
	 * Returns the factory to read/write DataCells of this class from/to a
	 * DataInput/DataOutput. This method is called via reflection.
	 *
	 * @return a serializer for reading/writing cells of this kind
	 * @see DataCell
	 * @deprecated As of KNIME 3.0 data types are registered via extension point. This method
	 *     is not used anymore. The serializer is made known in the extension point configuration.
	 */
	public static final RDKitSerializer getCellSerializer() {
		return SERIALIZER;
	}

	private final String m_smilesString;
	private final boolean m_smilesIsCanonical;
	private final byte[] m_byteContent;

	/** Package scope constructor that wraps the argument molecule.
	 * @param mol The molecule to wrap.
	 * @param smiles smiles for the molecule.
	 */
	RDKitMolCell2(final ROMol mol, final String smiles) {
		if(smiles == null || smiles.length() == 0) {
		   // For empty molecules we create an empty SMILES and still set the canonical flag
		   if (mol.getNumAtoms() > 0) {
		      m_smilesString = RDKFuncs.MolToSmiles(mol, true);
		   }
		   else {
		      m_smilesString = "";
		   }
		   m_smilesIsCanonical = true;
		} 
		else {
			m_smilesString = smiles;
			m_smilesIsCanonical = false;
		}
		m_byteContent = toByteArray(mol);
	}

	/** Deserialisation constructor.
	 * @param byteContent The byte content
	 * @param smiles smiles for the molecule.
	 */
	private RDKitMolCell2(final byte[] byteContent, final String smiles,
			final boolean smilesIsCanonical) {
		if (byteContent == null) {
			throw new NullPointerException("Argument must not be null.");
		}
		m_byteContent = byteContent;
		if(smiles == null || smiles.length() == 0){
			final ROMol mol = toROMol(byteContent);
			try {
				m_smilesString = RDKFuncs.MolToSmiles(mol, true);
			} finally {
				mol.delete();
			}
			m_smilesIsCanonical=true;
		} else {
			m_smilesString = smiles;
			m_smilesIsCanonical=smilesIsCanonical;
		}
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

	/** {@inheritDoc} */
	@Override
	public String getSdfValue() {
		String value;

		final ROMol mol = readMoleculeValue();

		try {
			// Convert to SDF
			if (mol.getNumConformers() == 0) {
				RDKitMolValueRenderer.compute2DCoords(mol,
					RDKitDepicterPreferencePage.isUsingCoordGen(),
					RDKitDepicterPreferencePage.isNormalizeDepictions());
			}

			value = RDKFuncs.MolToMolBlock(mol);

			// KNIME SDF type requires string to be terminated
			// by $$$$ -- see org.knime.chem.types.SdfValue for details
			if (!value.endsWith(SDF_POSTFIX)) {
				value += SDF_POSTFIX;
			}
		}
		catch (final Exception exc) {
			// Converting the RDKit molecule into Sdf Value failed.
			// In that case we return an empty string value.
			// Logging something here may swam the log files - not desired.
			// Note: Implementing the SdfValue interface actually violates
			//       one of the KNIME Development Rules, saying that
			//       a DataCell should only implement Value interfaces, if
			//       the representation in that format is "loss-free".
			//       E.g. DoubleCell does NOT implement IntValue, because it
			//       would loose information. The same happens here now.
			value = "";
		}
		finally {
			mol.delete();
		}

		return value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isSmilesCanonical() {
		return m_smilesIsCanonical;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ROMol readMoleculeValue() {
		return toROMol(m_byteContent);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return getStringValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean equalsDataCell(final DataCell dc) {
		return m_smilesString.equals(((RDKitMolCell2)dc).m_smilesString);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean equalContent(DataValue otherValue) { 
		return RDKitMolValue.equals(this, (RDKitMolValue)otherValue);
	}
	
	/**
	 * Returns a copy of the binary representation of the RDKit Mol value of this cell.
	 * 
	 * @return Binary value.
	 */
	protected byte[] getBinaryValue() {
		byte[] arrCopy = null;

		if (m_byteContent != null) {
			arrCopy = new byte[m_byteContent.length];
			System.arraycopy(m_byteContent, 0, arrCopy, 0, m_byteContent.length);
		}

		return arrCopy;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return m_smilesString.hashCode();
	}

	protected static byte[] toByteArray(final ROMol mol) {
		final Int_Vect iv = mol.ToBinary();
		final byte[] bytes = new byte[(int)iv.size()];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte)iv.get(i);
		}
		return bytes;
	}

	protected static ROMol toROMol(final byte[] bytes) {
		final Int_Vect iv = new Int_Vect(bytes.length);
		for (int i = 0; i < bytes.length; i++) {
			iv.set(i, bytes[i]);
		}
		return ROMol.MolFromBinary(iv);
	}

	/** Factory for (de-)serializing a RDKitMolCell. */
	public static class RDKitSerializer implements
	DataCellSerializer<RDKitMolCell2> {
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void serialize(final RDKitMolCell2 cell,
				final DataCellDataOutput output) throws IOException {
			output.writeInt(-1);
			output.writeUTF(cell.getSmilesValue());
			final byte[] bytes = cell.m_byteContent;
			output.writeInt(bytes.length);
			output.write(bytes);
			output.writeBoolean(cell.m_smilesIsCanonical);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public RDKitMolCell2 deserialize(final DataCellDataInput input)
				throws IOException {
			int length = input.readInt();
			String smiles = "";
			if(length < 0) {
				smiles = input.readUTF();
				length = input.readInt();
			}
			final byte[] bytes = new byte[length];
			input.readFully(bytes);
			boolean isCanonical;
			try {
				isCanonical=input.readBoolean();
			} catch (final IOException e) {
				isCanonical=true;
			}
			return new RDKitMolCell2(bytes, smiles,isCanonical);
		}
	}

}
