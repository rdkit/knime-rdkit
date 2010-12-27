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
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;

/**
 * Default implementation of a Smiles Cell. This cell stores only the Smiles
 * string but does not do any checks or interpretation of the contained
 * information.
 *
 * @author Greg Landrum
 */
public class RDKitMolCell2 extends DataCell implements StringValue,
        RDKitMolValue {

    /**
     * Convenience access member for
     * <code>DataType.getType(RDKitMolCell2.class)</code>.
     *
     * @see DataType#getType(Class)
     */
    static final DataType TYPE = DataType.getType(RDKitMolCell2.class);

    /**
     * Returns the preferred value class of this cell implementation. This
     * method is called per reflection to determine which is the preferred
     * renderer, comparator, etc.
     *
     * @return SmilesValue.class
     */
    public static final Class<? extends DataValue> getPreferredValueClass() {
        return RDKitMolValue.class;
    }

    private static final RDKitSerializer SERIALIZER =
            new RDKitSerializer();

    /**
     * Returns the factory to read/write DataCells of this class from/to a
     * DataInput/DataOutput. This method is called via reflection.
     *
     * @return a serializer for reading/writing cells of this kind
     * @see DataCell
     */
    public static final RDKitSerializer getCellSerializer() {
        return SERIALIZER;
    }

    private final String m_smilesString;

    private final byte[] m_byteContent;

    /** Package scope constructor that wraps the argument molecule.
     * @param mol The molecule to wrap.
     * @param canonSmiles RDKit canonical smiles for the molecule.
     *        Leave this empty if you have any doubts how to generate it.
     */
    RDKitMolCell2(final ROMol mol, final String canonSmiles) {
        if(canonSmiles == null || canonSmiles.length() == 0){
        	m_smilesString = RDKFuncs.MolToSmiles(mol, true);
        } else {
        	m_smilesString = canonSmiles;
        }
        m_byteContent = toByteArray(mol);
    }

    /** Deserialisation constructor.
     * @param byteContent The byte content
     * @param canonSmiles RDKit canonical smiles for the molecule.
     */
    private RDKitMolCell2(final byte[] byteContent, final String canonSmiles) {
        if (byteContent == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_byteContent = byteContent;
        if(canonSmiles == null || canonSmiles.length() == 0){
            ROMol mol = toROMol(byteContent);
            try {
                m_smilesString = RDKFuncs.MolToSmiles(mol, true);
            } finally {
                mol.delete();
            }
        } else {
            m_smilesString = canonSmiles;
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
    public int hashCode() {
        return m_smilesString.hashCode();
    }

    private static byte[] toByteArray(final ROMol mol) {
        Int_Vect iv = RDKFuncs.MolToBinary(mol);
        byte[] bytes = new byte[(int)iv.size()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte)iv.get(i);
        }
        return bytes;
    }

    private static ROMol toROMol(final byte[] bytes) {
        Int_Vect iv = new Int_Vect(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            iv.set(i, bytes[i]);
        }
        return RDKFuncs.MolFromBinary(iv);
    }

    /** Factory for (de-)serializing a RDKitMolCell. */
    private static class RDKitSerializer implements
            DataCellSerializer<RDKitMolCell2> {
        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(final RDKitMolCell2 cell,
                final DataCellDataOutput output) throws IOException {
            output.writeInt(-1);
            output.writeUTF(cell.getSmilesValue());
            byte[] bytes = cell.m_byteContent;
            output.writeInt(bytes.length);
            output.write(bytes);
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
            byte[] bytes = new byte[length];
            input.readFully(bytes);
            return new RDKitMolCell2(bytes, smiles);
        }
    }

}
