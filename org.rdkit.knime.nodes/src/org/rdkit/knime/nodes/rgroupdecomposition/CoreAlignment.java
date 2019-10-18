/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2019
 * Novartis Institutes for BioMedical Research
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
package org.rdkit.knime.nodes.rgroupdecomposition;

import org.RDKit.RGroupCoreAlignment;

/**
 * This enumeration defines different R Group core alignments based on the 
 * RDKit R Group Core Alignment enumeration.
 * Details: https://www.rdkit.org/docs/cppapi/RGroupDecomp_8h_source.html
 * 
 * @author Manuel Schwarze
 */
public enum CoreAlignment {

	None("No alignment", RGroupCoreAlignment.None), 
	MCS("Align MCS", RGroupCoreAlignment.MCS);

	//
	// Members
	//

	/** Enumeration value of RDKit. */
	private final RGroupCoreAlignment m_rdkitRGroupCoreAlignment;
	
	/** The name to be shown to the user. */
	private final String m_strName;

	//
	// Constructors
	//

	/**
	 * Creates a new R Group Core Alignment enumeration value.
	 * 
	 * @param strName Name to be shown as string representation.
	 */
	private CoreAlignment(final String strName, final RGroupCoreAlignment rdkitRGroupCoreAlignment) {
		m_rdkitRGroupCoreAlignment = rdkitRGroupCoreAlignment;
		m_strName = strName;
	}

	/**
	 * Returns the associated RDKit R Group Core Alignment enumeration value.
	 * @return
	 */
	public RGroupCoreAlignment getRDKitRGroupCoreAlignment() {
		return m_rdkitRGroupCoreAlignment;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return m_strName;
	}

	//
	// Static Methods
	//

	/**
	 * Tries to determine the R Group Core Alignment type based on the passed in string. First it
	 * will try to determine it by assuming that the passed in string is the
	 * name of the R Group Core Alignment  type ({@link #name()}. If this fails, it will compare the
	 * string representation trying to find a match there ({@link #toString()}.
	 * If none is found it will return null.
	 */
	public static CoreAlignment parseString(String str) {
		CoreAlignment alignment = null;

		if (str != null) {
			try {
				alignment = CoreAlignment.valueOf(str);
			}
			catch (final IllegalArgumentException exc) {
				// Ignored here
			}

			if (alignment == null) {
				str = str.trim().toUpperCase();
				for (final CoreAlignment alignmentExisting : CoreAlignment.values()) {
					if (str.equals(alignmentExisting.toString().toUpperCase()) ||
						str.equals(alignmentExisting.getRDKitRGroupCoreAlignment().name().toUpperCase())) {
						alignment = alignmentExisting;
						break;
					}
				}
			}
		}

		return alignment;
	}
	
	/**
	 * Determines the matching CoreAlignment value for the specified RDKit based value
	 * and returns null, if undefined.
	 * 
	 * @param value RDKit based value. Can be null to return null.
	 * 
	 * @return KNIME / Java based value. Null, if undefined.
	 */
	public static CoreAlignment getValue(RGroupCoreAlignment value) {
		CoreAlignment result = null;
		
		if (value != null) {
			for (CoreAlignment alignment : values()) {
				if (alignment.getRDKitRGroupCoreAlignment().swigValue() == value.swigValue()) {
					result = alignment;
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Determines the matching CoreAlignment value for the specified RDKit based SWIG value
	 * and returns null, if undefined.
	 * 
	 * @param value RDKit based value. Can be null to return null.
	 * 
	 * @return KNIME / Java based value. Null, if undefined.
	 */
	public static CoreAlignment getValue(long value) {
		CoreAlignment result = null;
		
		for (CoreAlignment alignment : values()) {
			if (alignment.getRDKitRGroupCoreAlignment().swigValue() == value) {
				result = alignment;
			}
		}
		
		return result;
	}
}
