/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2013
 * Novartis Institutes for BioMedical Research
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
 * ---------------------------------------------------------------------
 */
package org.rdkit.knime.types;

import java.io.IOException;

import org.RDKit.ROMol;
import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.AdapterCell;
import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;

/**
 * An RDKit Adapter Cell combines multiple chemical molecule value representations in
 * one cell.
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Manuel Schwarze, Novartis
 */
public class RDKitAdapterCell extends AdapterCell implements RDKitMolValue, SmilesValue, StringValue, SdfValue {

	//
	// Constants
	//

	/** The serial number. */
	private static final long serialVersionUID = -994027050919072740L;

	/**
	 * The raw type of this adapter cell with only the implemented value classes. The type of the cell may change
	 * if additional adapters are added.
	 */
	public static final DataType RAW_TYPE = DataType.getType(RDKitAdapterCell.class);

	/** The serializer instance. */
	private static final AdapterCellSerializer<RDKitAdapterCell> SERIALIZER =
			new AdapterCellSerializer<RDKitAdapterCell>() {
		@Override
		public RDKitAdapterCell deserialize(final DataCellDataInput input) throws IOException {
			return new RDKitAdapterCell(input);
		}
	};

	//
	// Constructors
	//

	/**
	 * Creates a new adapter cell for the passed in data input.
	 * 
	 * @param input Input data. Must not be null.
	 * 
	 * @throws IOException Thrown, if it could not be created.
	 */
	private RDKitAdapterCell(final DataCellDataInput input) throws IOException {
		super(input);
	}

	/**
	 * Creates a new adapter cell and adds the passed in cell.
	 * 
	 * @param cell Cell to be added as first element. Must not be null.
	 */
	@SuppressWarnings("unchecked")
	public RDKitAdapterCell(final DataCell cell) {
		super(cell);
	}

	/**
	 * Creates a new adapter cell and adds the passed in cell and copies
	 * all cells that are contained in the passed in copy parameter.
	 * 
	 * @param copy An existing adapter value, from which all cells are copied
	 * 		into the new adapter cell. Can be null.
	 * @param cell Cell to be added to the existing cell collection. Must not be null.
	 */
	@SuppressWarnings("unchecked")
	public RDKitAdapterCell(final AdapterValue copy, final DataCell cell) {
		super(cell, copy);
	}

	//
	// Public Methods
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSdfValue() {
		return ((SdfValue)lookupFromAdapterMap(SdfValue.class)).getSdfValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getStringValue() {
		return ((StringValue)lookupFromAdapterMap(StringValue.class)).getStringValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ROMol readMoleculeValue() {
		return ((RDKitMolValue)lookupFromAdapterMap(RDKitMolValue.class)).readMoleculeValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSmilesValue() {
		return ((SmilesValue)lookupFromAdapterMap(SmilesValue.class)).getSmilesValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isSmilesCanonical() {
		return ((RDKitMolValue)lookupFromAdapterMap(RDKitMolValue.class)).isSmilesCanonical();
	}

	//
	// Public Static Methods
	//

	/**
	 * Returns the cell serializer for RDKit Adapter Cells.
	 * 
	 * @return Singleton serializer instance. Never null.
	 */
	public static DataCellSerializer<RDKitAdapterCell> getCellSerializer() {
		return SERIALIZER;
	}

	/**
	 * See {@link DataCell} description for details.
	 * 
	 * @return RDKitMolValue.class
	 */
	public static final Class<? extends DataValue> getPreferredValueClass() {
		return RDKitMolValue.class;
	}
}