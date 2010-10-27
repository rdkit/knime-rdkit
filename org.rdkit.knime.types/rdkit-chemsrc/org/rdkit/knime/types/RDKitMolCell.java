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

import org.RDKit.Char_Vect;
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
public class RDKitMolCell extends DataCell implements StringValue,
        RDKitMolValue {
    /**
     * Convenience access member for
     * <code>DataType.getType(RDKitMolCell.class)</code>.
     *
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = DataType.getType(RDKitMolCell.class);

    private static final long serialVersionUID = 0x1;

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

    private static final RDKitMolSerializer SERIALIZER =
            new RDKitMolSerializer();

    /**
     * Returns the factory to read/write DataCells of this class from/to a
     * DataInput/DataOutput. This method is called via reflection.
     *
     * @return a serializer for reading/writing cells of this kind
     * @see DataCell
     */
    public static final RDKitMolSerializer getCellSerializer() {
        return SERIALIZER;
    }

    private final String m_smilesString;

    private final ROMol m_mol;

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
    public RDKitMolCell(final ROMol mol) {
        if (mol == null) {
            throw new NullPointerException("Mol value must not be null.");
        }
        m_mol = mol;
        m_smilesString = RDKFuncs.MolToSmiles(m_mol, true);
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
    public RDKitMolCell(final String str) {
        if (str == null) {
            throw new NullPointerException("Smiles value must not be null.");
        }
        m_mol = RDKFuncs.MolFromSmiles(str);
        if (m_mol == null) {
            throw new NullPointerException("could not process smiles");
        }
        m_smilesString = RDKFuncs.MolToSmiles(m_mol, true);
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
    public ROMol getMoleculeValue() {
        return m_mol;
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
        return m_smilesString.equals(((RDKitMolCell)dc).m_smilesString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_smilesString.hashCode();
    }

    /** Factory for (de-)serializing a RDKitMolCell. */
    private static class RDKitMolSerializer implements
            DataCellSerializer<RDKitMolCell> {
        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(final RDKitMolCell cell,
                final DataCellDataOutput output) throws IOException {
            // output.writeUTF(RDKFuncs.MolToBinary(cell.getMoleculeValue()));
            Char_Vect cv = RDKFuncs.MolToBinary(cell.getMoleculeValue());
            StringBuilder pkl = new StringBuilder((int)cv.size());
            for (int i = 0; i < cv.size(); ++i) {
                pkl.append(cv.get(i));
            }
            output.writeUTF(pkl.toString());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public RDKitMolCell deserialize(final DataCellDataInput input)
                throws IOException {
            String s = input.readUTF();
            Char_Vect cv = new Char_Vect(0);
            for (int i = 0; i < s.length(); ++i) {
                char c = s.charAt(i);
                cv.add(c);
            }
            // ROMol m=new ROMol(s);
            // ROMol m=RDKFuncs.MolFromSmiles(s);
            ROMol m = RDKFuncs.MolFromBinary(cv);
            return new RDKitMolCell(m);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable {
        m_mol.delete();
        super.finalize();
    }
}
