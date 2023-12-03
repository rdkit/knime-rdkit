/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2019-2023
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
package org.rdkit.knime.nodes.rgroupdecomposition;

import org.RDKit.RGroupCoreAlignment;
import org.RDKit.RGroupMatching;

/**
 * This enumeration defines different R Group matching strategies based on the 
 * RDKit R Group Matching enumeration.
 * Details: https://www.rdkit.org/docs/cppapi/RGroupDecomp_8h_source.html
 * 
 * @author Manuel Schwarze
 */
public enum Matching {

	Greedy("Greedy", RGroupMatching.Greedy),
	GreedyChunks("Greedy Chunks", RGroupMatching.GreedyChunks), 
	Exhaustive("Exhaustive", RGroupMatching.Exhaustive); // Not useful for large datasets

	//
	// Members
	//

	/** Enumeration value of RDKit. */
	private final RGroupMatching m_rdkitRGroupMatching;
	
	/** The name to be shown to the user. */
	private final String m_strName;

	//
	// Constructors
	//

	/**
	 * Creates a new R Group Core Matching enumeration value.
	 * 
	 * @param strName Name to be shown as string representation.
	 */
	private Matching(final String strName, final RGroupMatching rdkitRGroupMatching) {
		m_rdkitRGroupMatching = rdkitRGroupMatching;
		m_strName = strName;
	}

	/**
	 * Returns the associated RDKit R Group Matching enumeration value.
	 * @return
	 */
	public RGroupMatching getRDKitRGroupMatching() {
		return m_rdkitRGroupMatching;
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
	 * Tries to determine the R Group Matching type based on the passed in string. First it
	 * will try to determine it by assuming that the passed in string is the
	 * name of the R Group Matching type ({@link #name()}. If this fails, it will compare the
	 * string representation trying to find a match there ({@link #toString()}.
	 * If none is found it will return null.
	 */
	public static Matching parseString(String str) {
		Matching matching = null;

		if (str != null) {
			try {
				matching = Matching.valueOf(str);
			}
			catch (final IllegalArgumentException exc) {
				// Ignored here
			}

			if (matching == null) {
				str = str.trim().toUpperCase();
				for (final Matching matchingExisting : Matching.values()) {
					if (str.equals(matchingExisting.toString().toUpperCase()) ||
						str.equals(matchingExisting.getRDKitRGroupMatching().name().toUpperCase())) {
						matching = matchingExisting;
						break;
					}
				}
			}
		}

		return matching;
	}
	
	/**
	 * Determines the matching Matching value for the specified RDKit based value
	 * and returns null, if undefined.
	 * 
	 * @param value RDKit based value. Can be null to return null.
	 * 
	 * @return KNIME / Java based value. Null, if undefined.
	 */
	public static Matching getValue(RGroupCoreAlignment value) {
		Matching result = null;
		
		if (value != null) {
			for (Matching matching : values()) {
				if (matching.getRDKitRGroupMatching().swigValue() == value.swigValue()) {
					result = matching;
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Determines the matching Matching value for the specified RDKit based SWIG value
	 * and returns null, if undefined.
	 * 
	 * @param value RDKit based value. Can be null to return null.
	 * 
	 * @return KNIME / Java based value. Null, if undefined.
	 */
	public static Matching getValue(long value) {
		Matching result = null;
		
		for (Matching matching : values()) {
			if (matching.getRDKitRGroupMatching().swigValue() == value) {
				result = matching;
			}
		}
		
		return result;
	}
}
