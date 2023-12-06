/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2013-2023
 * Novartis Pharma AG, Switzerland
 *
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
package org.rdkit.knime.properties;

import java.util.Map;

import org.knime.chem.types.SmilesCell;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.rdkit.knime.headers.HeaderProperty;
import org.rdkit.knime.headers.HeaderPropertyUtils;

/**
 * This class knows how to write values about a SMILES into a column
 * specification and reads it.
 * 
 * @author Manuel Schwarze
 */
public class SmilesHeaderProperty extends DataCell implements HeaderProperty, SmilesValue {

	//
	// Constants
	//

	/** The serial number. */
	private static final long serialVersionUID = -4065071445185767751L;

	/** The property that holds a SMILES that can be handled with this handler. */
	public static final String PROPERTY_SMILES = "rdkit.smiles";

	/**
	 * The properties that are involved in handling SMILES header properties.
	 * The second and third one are deprecated, but can still be read.
	 */
	public static final String[] PROPERTIES_SMILES = new String [] {
		PROPERTY_SMILES, "addInfoValue", "addInfoType"
	};

	/** Smiles column specification used to find correct renderer. */
	private static final DataColumnSpec COLUMN_SPEC =
			new DataColumnSpecCreator("Smiles", SmilesCell.TYPE).createSpec();

	//
	// Members
	//

	/** The SMILES value. Can be null. */
	private String m_strSmiles;

	//
	// Constructor
	//

	/**
	 * Creates a new header property object with the specified SMILES
	 * value.
	 * 
	 * @param strSmiles SMILES value. Can be null.
	 */
	public SmilesHeaderProperty(final String strSmiles) {
		m_strSmiles = strSmiles;
	}

	/**
	 * Creates a new header property object based on SMILES
	 * value information taken from the past in data column
	 * specification.
	 * 
	 * @param colSpec Data column specification. Can be null.
	 */
	public SmilesHeaderProperty(final DataColumnSpec colSpec) {
		reset();
		readFromColumnSpec(colSpec);
	}

	/**
	 * Returns the SMILES value that is part of this object.
	 * 
	 * @return SMILES value or null, if not set.
	 */
	public synchronized String getSmiles() {
		return m_strSmiles;
	}

	/**
	 * Sets the SMILES value for this object.
	 * 
	 * @param strSmiles SMILES value to be set. Can be null.
	 */
	public synchronized void setSmiles(final String strSmiles) {
		m_strSmiles = strSmiles;
	}

	@Override
	public synchronized void readFromColumnSpec(final DataColumnSpec colSpec) {
		reset();

		final Map<String, String> mapProps =
				HeaderPropertyUtils.getProperties(colSpec, PROPERTIES_SMILES);

		if (mapProps.containsKey(PROPERTY_SMILES)) {
			m_strSmiles = mapProps.get(PROPERTY_SMILES);
		}

		// Handling deprecated properties
		else if ("Smiles".equals(mapProps.get("addInfoType")) &&
				mapProps.containsKey("addInfoValue")) {
			m_strSmiles = mapProps.get("addInfoValue");
		}
	}

	@Override
	public synchronized void writeToColumnSpec(final DataColumnSpecCreator colSpecCreator) {
		if (colSpecCreator != null) {
			HeaderPropertyUtils.writeInColumnSpec(colSpecCreator, PROPERTY_SMILES, m_strSmiles);
		}
	}

	@Override
	public synchronized void reset() {
		m_strSmiles = null;
	}

	@Override
	public synchronized boolean equals(final DataColumnSpec colSpec) {
		return equals(new SmilesHeaderProperty(colSpec));
	}

	@Override
	public DataColumnSpec getColumnSpecForRendering() {
		return COLUMN_SPEC;
	}

	@Override
	public synchronized int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((m_strSmiles == null) ? 0 : m_strSmiles.hashCode());
		return result;
	}

	@Override
	public synchronized boolean equalsDataCell(final DataCell objSpecToCompare) {
		boolean bRet = false;
		if (objSpecToCompare == this) {
			bRet = true;
		}
		else if (objSpecToCompare instanceof SmilesHeaderProperty) {
			final SmilesHeaderProperty specToCompare = (SmilesHeaderProperty)objSpecToCompare;

			if (HeaderPropertyUtils.equals(this.m_strSmiles, specToCompare.m_strSmiles)) {
				bRet = true;
			}
		}

		return bRet;
	}

	@Override
	public synchronized String getSmilesValue() {
		return getSmiles();
	}

	@Override
	public String toString() {
		return getSmiles();
	}
}
