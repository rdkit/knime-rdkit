/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 */
package org.rdkit.knime.types;

import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellFactory;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.DataCellFactoryMethod;

/** Factory creating {@link RDKitMolValue} compatible {@link DataCell} objects.
 * It's currently using {@link RDKitMolCell2}; future versions may return
 * different implementations (e.g. blobs).
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @author Manuel Schwarze, Novartis
 */
public final class RDKitMolCellFactory implements DataCellFactory {

    /**
     * Type representing cells implementing the {@link RDKitMolValue}
     * interface
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = RDKitMolCell2.TYPE;
    
    @Override
    public DataType getDataType() {
        return TYPE;
    }

    /**
     * Creates a new RDKit Cell based on the given molecule. The argument
     * can (and should) be {@link ROMol#delete() deleted} after this method
     * returns.
     * @param mol the ROMol value to store
     * @return A data cell implementing RDKitMolValue interface. Currently this
     * is a {@link RDKitMolCell2} but this may change in future versions.
     * @throws NullPointerException if argument is <code>null</code>
     */
    public static DataCell createRDKitMolCell(final ROMol mol) {
        if (mol == null) {
            throw new NullPointerException("Mol value must not be null.");
        }
        return new RDKitMolCell2(mol, RDKFuncs.MolToSmiles(mol, true));
    }

    /**
     * Creates a new RDKit Cell based on the given molecule and SMILES. The argument
     * can (and should) be {@link ROMol#delete() deleted} after this method
     * returns.
     * @param mol the ROMol value to store
     * @param smiles canonical SMILES for the molecule
     * @return A data cell implementing RDKitMolValue interface. Currently this
     * is a {@link RDKitMolCell2} but this may change in future versions.
     * @throws NullPointerException if argument is <code>null</code>
     */
    public static DataCell createRDKitMolCell(final ROMol mol,final String smiles) {
        if (mol == null) {
            throw new NullPointerException("Mol value must not be null.");
        }
        return new RDKitMolCell2(mol, smiles);
    }

    /**
     * Creates a new RDKit Cell based on the given molecule and discards the
     * argument using the {@link ROMol#delete()} method.
     * @param mol the ROMol value to store
     * @return A data cell implementing RDKitMolValue interface. Currently this
     * is a {@link RDKitMolCell2} but this may change in future versions.
     * @throws NullPointerException if argument is <code>null</code>
     */
    public static DataCell createRDKitMolCellAndDelete(final ROMol mol) {
        try {
            return createRDKitMolCell(mol);
        } finally {
            mol.delete();
        }
    }

    /**
     * Creates a new RDKit Adapter Cell with an RDKit Mol Cell 
     * based on the given molecule. A canonicalized SMILES will be
     * created as part of the inner RDKit Cell. The argument
     * can (and should) be {@link ROMol#delete() deleted} after this method
     * returns.
     * @param mol the ROMol value to store
     * @return A data cell implementing RDKitMolValue interface. Currently this
     * is a {@link RDKitAdapterCell} but this may change in future versions.
     * @throws NullPointerException if argument is <code>null</code>
     */
    public static DataCell createRDKitAdapterCell(final ROMol mol) {
        if (mol == null) {
            throw new NullPointerException("Mol value must not be null.");
        }
        return new RDKitAdapterCell(new RDKitMolCell2(mol, null)); // This forces the generation of a canonicalized SMILES
    }

    /**
     * Creates a new RDKit Adapter Cell with an RDKit Mol Cell 
     * based on the given molecule and SMILES. The argument
     * can (and should) be {@link ROMol#delete() deleted} after this method
     * returns.
     * @param mol the ROMol value to store
     * @param smiles SMILES for the molecule. This SMILES will be taken as is and flagged as non-canonical
     *      in the inner RDKit Cell.
     * @return A data cell implementing RDKitMolValue interface. Currently this
     * is a {@link RDKitAdapterCell} but this may change in future versions.
     * @throws NullPointerException if argument is <code>null</code>
     */
    public static DataCell createRDKitAdapterCell(final ROMol mol,final String smiles) {
        if (mol == null) {
            throw new NullPointerException("Mol value must not be null.");
        }
        return new RDKitAdapterCell(new RDKitMolCell2(mol, smiles)); 
    }

    /**
     * Creates a new RDKit Adapter Cell based on the given molecule and discards the
     * argument using the {@link ROMol#delete()} method. A canonicalized SMILES will be
     * created as part of the inner RDKit Cell.
     * @param mol the ROMol value to store
     * @return A data cell implementing RDKitMolValue interface. Currently this
     * is a {@link RDKitAdapterCell} but this may change in future versions.
     * @throws NullPointerException if argument is <code>null</code>
     */
    public static DataCell createRDKitAdapterCellAndDelete(final ROMol mol) {
        try {
            return createRDKitAdapterCell(mol);
        } finally {
            mol.delete();
        }
    }
    
    /**
     * Creates a new RDKit Adapter Cell based on the given molecule and discards the
     * argument using the {@link ROMol#delete()} method. A canonicalized SMILES will be
     * created as part of the inner RDKit Cell.
     * 
     * <p>This method is the non-static variant of 
     * {@link #createRDKitAdapterCellAndDelete(ROMol)} and used via KNIME's converter
     * framework (see also type registration in this bundle's plugin.xml). 
     * @param mol the ROMol value to store
     * @return A data cell implementing RDKitMolValue interface. Currently this
     * is a {@link RDKitAdapterCell} but this may change in future versions.
     * @throws NullPointerException if argument is <code>null</code>
     */
    @DataCellFactoryMethod(name = "ROMol")
    public DataCell createInstanceRDKitAdapterCellAndDelete(final ROMol mol) {
        return createRDKitAdapterCellAndDelete(mol);
    }
    
}
