/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2014
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
package org.rdkit.knime.nodes.mcs;

import org.RDKit.AtomComparator;

/**
 * This enumeration defines different atom comparators based on the RDKit atom comparators.
 * 
 * @author Manuel Schwarze
 */
public enum AtomComparison {

	CompareAny("Compare Any", AtomComparator.AtomCompareAny), 
	CompareElements("Compare Elements", AtomComparator.AtomCompareElements), 
	CompareIsotopes("Compare Isotopes", AtomComparator.AtomCompareIsotopes);

	//
	// Members
	//

	/** Enumeration value of RDKit. */
	private final AtomComparator m_rdkitComparator;
	
	/** The name to be shown to the user. */
	private final String m_strName;

	//
	// Constructors
	//

	/**
	 * Creates a new atom comparison type enumeration value.
	 * 
	 * @param strName Name to be shown as string representation.
	 */
	private AtomComparison(final String strName, final AtomComparator rdkitComparator) {
		m_rdkitComparator = rdkitComparator;
		m_strName = strName;
	}

	/**
	 * Returns the associated RDKit comparator.
	 * @return
	 */
	public AtomComparator getRDKitComparator() {
		return m_rdkitComparator;
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
	 * Tries to determine the atom comparison type based on the passed in string. First it
	 * will try to determine it by assuming that the passed in string is the
	 * name of the AtomComparison type ({@link #name()}. If this fails, it will compare the
	 * string representation trying to find a match there ({@link #toString()}.
	 * If none is found it will return null.
	 */
	public static AtomComparison parseString(String str) {
		AtomComparison comparison = null;

		if (str != null) {
			try {
				comparison = AtomComparison.valueOf(str);
			}
			catch (final IllegalArgumentException exc) {
				// Ignored here
			}

			if (comparison == null) {
				str = str.trim().toUpperCase();
				for (final AtomComparison comparisonExisting : AtomComparison.values()) {
					if (str.equals(comparisonExisting.toString().toUpperCase()) ||
						str.equals(comparisonExisting.getRDKitComparator().name().toUpperCase())) {
						comparison = comparisonExisting;
						break;
					}
				}
			}
		}

		return comparison;
	}

}
