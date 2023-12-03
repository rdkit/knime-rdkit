/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2013-2023
 * Novartis Pharma AG, Switzerland
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
import java.util.Objects;

import org.RDKit.ROMol;
import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.AdapterCell;
import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.node.NodeLogger;

/**
 * An RDKit Adapter Cell combines multiple chemical molecule value representations in
 * one cell.
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Manuel Schwarze, Novartis
 */
public class RDKitAdapterCell extends AdapterCell implements RDKitMolValue, SdfValue, SmilesValue, StringValue {

	//
	// Constants
	//

	/** The logger instance. */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitAdapterCell.class);

	/** The serial number. */
	private static final long serialVersionUID = -994027050919072740L;

	/**
	 * The raw type of this adapter cell with only the implemented value classes. The type of the cell may change
	 * if additional adapters are added.
	 */
	public static final DataType RAW_TYPE = DataType.getType(RDKitAdapterCell.class);

	/** The serializer instance. */
	@Deprecated
	private static final RDKitAdapterCellSerializer SERIALIZER =
			new RDKitAdapterCellSerializer();

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
	public RDKitAdapterCell(final DataCell cell, final AdapterValue copy) {
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
		SdfValue sdf = null;

		try {
			sdf = (SdfValue)lookupFromAdapterMap(SdfValue.class);
		}
		catch (final IllegalArgumentException exc) {
			// Rethrow a better error message, which appears as warning in the node
			throw new IllegalArgumentException("Unable to access SDF value in RDKit Adapter Cell.", exc);
		}

		return sdf.getSdfValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getStringValue() {
		StringValue strValue = null;

		try {
			strValue = (StringValue)lookupFromAdapterMap(StringValue.class);
		}
		catch (final IllegalArgumentException exc) {
			// Rethrow a better error message, which appears as warning in the node
			throw new IllegalArgumentException("Unable to access string value in RDKit Adapter Cell.", exc);
		}

		return strValue.getStringValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ROMol readMoleculeValue() {
		RDKitMolValue rdkitValue = null;

		try {
			rdkitValue = (RDKitMolValue)lookupFromAdapterMap(RDKitMolValue.class);
		}
		catch (final IllegalArgumentException exc) {
			// Rethrow a better error message, which appears as warning in the node
			throw new IllegalArgumentException("Unable to access RDKit Mol value in RDKit Adapter Cell.", exc);
		}

		return rdkitValue.readMoleculeValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSmilesValue() {
	   SmilesValue smiles= null;

		try {
		   smiles = (SmilesValue)lookupFromAdapterMap(SmilesValue.class);
		}
		catch (final IllegalArgumentException exc) {
			// Rethrow a better error message, which appears as warning in the node
			throw new IllegalArgumentException("Unable to access SMILES value in RDKit Adapter Cell.", exc);
		}

		return smiles.getSmilesValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isSmilesCanonical() {
		boolean bIsSmilesCanonical = false;
		RDKitMolValue rdkitValue = null;
		SmilesValue smilesValue = null;

		try {
			rdkitValue = (RDKitMolValue)lookupFromAdapterMap(RDKitMolValue.class);
			smilesValue = (SmilesValue)lookupFromAdapterMap(SmilesValue.class);
		}
		catch (final IllegalArgumentException exc) {
			// Ignoring
		}

		if (rdkitValue != null && smilesValue != null) {
			if (rdkitValue == smilesValue) { // Same physical object, an RDKit cell
				bIsSmilesCanonical = rdkitValue.isSmilesCanonical();
			}
			else { // Different cell objects, so check if SMILES value matches RDKit cell's SMILES value
				final String strRdkitSmiles = rdkitValue.getSmilesValue();
				final String strAdapterCellSmiles = getSmilesValue();

				if (strRdkitSmiles != null && strRdkitSmiles.equals(strAdapterCellSmiles)) {
					bIsSmilesCanonical = rdkitValue.isSmilesCanonical();
				}
			}
		}

		return bIsSmilesCanonical;
	}

	@Override
	public String toString() {
		String strRet = null;

		RDKitMolValue rdkitValue = null;
		SdfValue sdfValue = null;
		SmilesValue smilesValue = null;

		// Lookup references to different molecule representations
		try {
			rdkitValue = (RDKitMolValue)lookupFromAdapterMap(RDKitMolValue.class);
		}
		catch (final IllegalArgumentException exc) {
			// Ignored
		}

		try {
			sdfValue = (SdfValue)lookupFromAdapterMap(SdfValue.class);
		}
		catch (final IllegalArgumentException exc) {
			// Ignored
		}

		try {
			smilesValue = (SmilesValue)lookupFromAdapterMap(SmilesValue.class);
		}
		catch (final IllegalArgumentException exc) {
			// Ignored
		}

		// Check, if the adapter cell was created from an SDF cell - in that case return the SDF value
		if (rdkitValue != sdfValue) { // Note: It is very important to compare references here, not values!
			strRet = sdfValue.getSdfValue();
		}

		// Check, if the adapter cell was created from a SMILES cell - in that case return the SMILES value
		else if (rdkitValue != smilesValue) { // Note: It is very important to compare references here, not values!
			strRet = smilesValue.getSmilesValue();
		}

		// Otherwise, just return whatever the RDKit cell returns
		else if (rdkitValue != null) {
			strRet = rdkitValue.toString();
		}

		// Otherwise it could also be null

		return strRet;
	}



	//
	// Public Static Methods
	//

	/**
	 * Returns the cell serializer for RDKit Adapter Cells.
	 * 
	 * @return Singleton serializer instance. Never null.
	 * 
    * @deprecated As of KNIME 3.0 data types are registered via extension point. This method
    *     is not used anymore. The serializer is made known in the extension point configuration.
	 */
	public static DataCellSerializer<RDKitAdapterCell> getCellSerializer() {
		return SERIALIZER;
	}

	@Override
	protected boolean equalsDataCell(DataCell dc) {
	   return Objects.equals(getSmilesValue(), (((RDKitAdapterCell)dc).getSmilesValue()));
	}

	@Override
	public int hashCode() {
		int result = 1;
		
		try {
		   final int prime = 31;
			result = prime * result + ((RDKitMolValue)getAdapterMap().get(RDKitMolValue.class)).getSmilesValue().hashCode();
		}
		catch (Exception exc) {
			LOGGER.error("Unable to calculate hash code for RDKit molecule.", exc);
		}
		
		return result;
	}
	
   /**
    * Serializer for {@link RDKitAdapterCell}s.
    *
    * @noreference This class is not intended to be referenced by clients.
    * @since 3.0
    */
   public static final class RDKitAdapterCellSerializer extends AdapterCellSerializer<RDKitAdapterCell> {
       @Override
       public RDKitAdapterCell deserialize(final DataCellDataInput input) throws IOException {
           return new RDKitAdapterCell(input);
       }
   }
}
