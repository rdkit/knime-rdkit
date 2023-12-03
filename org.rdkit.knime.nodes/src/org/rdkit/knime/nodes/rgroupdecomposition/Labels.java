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

import org.RDKit.RGroupLabels;

/**
 * This enumeration defines different R Group label options based on the 
 * RDKit R Group Labels enumeration. Labels are used to recognize 
 * RGroups in the scaffolds and can be "or"ed together.
 * 
 * Details: https://www.rdkit.org/docs/cppapi/RGroupDecomp_8h_source.html
 *  
 * @author Manuel Schwarze
 */
public enum Labels {

	AutoDetect("Auto Detect", RGroupLabels.AutoDetect),
	IsotopeLabels("Isotope Labels", RGroupLabels.IsotopeLabels),
	AtomMapLabels("Atom Map Labels", RGroupLabels.AtomMapLabels), 
	AtomIndexLabels("Atom Index Labels", RGroupLabels.AtomIndexLabels),
	RelabelDuplicateLabels("Relabel Duplicate Labels", RGroupLabels.RelabelDuplicateLabels),
	DummyAtomLabels("Dummy Atom Labels", RGroupLabels.DummyAtomLabels),
	MDLRGroupLabels("MDL RGroup Labels", RGroupLabels.MDLRGroupLabels);

	//
	// Members
	//

	/** Enumeration value of RDKit. */
	private final RGroupLabels m_rdkitRGroupLabels;
	
	/** The name to be shown to the user. */
	private final String m_strName;

	//
	// Constructors
	//

	/**
	 * Creates a new R Group Core Labels enumeration value.
	 * 
	 * @param strName Name to be shown as string representation.
	 */
	private Labels(final String strName, final RGroupLabels rdkitRGroupLabels) {
		m_rdkitRGroupLabels = rdkitRGroupLabels;
		m_strName = strName;
	}

	/**
	 * Returns the associated RDKit R Group Labels enumeration value.
	 * @return
	 */
	public RGroupLabels getRDKitRGroupLabels() {
		return m_rdkitRGroupLabels;
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
	 * Tries to determine the R Group labels type based on the passed in string. First it
	 * will try to determine it by assuming that the passed in string is the
	 * name of the R Group labels type ({@link #name()}. If this fails, it will compare the
	 * string representation trying to find a match there ({@link #toString()}.
	 * If none is found it will return null.
	 */
	public static Labels parseString(String str) {
		Labels label = null;

		if (str != null) {
			try {
				label = Labels.valueOf(str);
			}
			catch (final IllegalArgumentException exc) {
				// Ignored here
			}

			if (label == null) {
				str = str.trim().toUpperCase();
				for (final Labels labelExisting : Labels.values()) {
					if (str.equals(labelExisting.toString().toUpperCase()) ||
						str.equals(labelExisting.getRDKitRGroupLabels().name().toUpperCase())) {
						label = labelExisting;
						break;
					}
				}
			}
		}

		return label;
	}

	/**
	 * Returns the OR-combined SWIG values of the RDKit enumeration behind Labels.
	 * 
	 * @param model
	 * @return
	 */
	public static long getCombinedValues(Labels[] arrLabels) {
		long lResult = 0;
		
		for (Labels labeling : arrLabels) {
			lResult |= labeling.getRDKitRGroupLabels().swigValue();
		}
		
		return lResult;		
	}
	
	/**
	 * Determines the matching Labels value for the specified RDKit based value
	 * and returns null, if undefined.
	 * 
	 * @param value RDKit based value. Can be null to return null.
	 * 
	 * @return KNIME / Java based value. Null, if undefined.
	 */
	public static Labels getValue(RGroupLabels value) {
		Labels result = null;
		
		if (value != null) {
			for (Labels label : values()) {
				if (label.getRDKitRGroupLabels().swigValue() == value.swigValue()) {
					result = label;
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Determines the matching Labels value for the specified RDKit based SWIG value
	 * and returns null, if undefined.
	 * 
	 * @param value RDKit based value. Can be null to return null.
	 * 
	 * @return KNIME / Java based value. Null, if undefined.
	 */
	public static Labels getValue(long value) {
		Labels result = null;
		
		for (Labels label : values()) {
			if (label.getRDKitRGroupLabels().swigValue() == value) {
				result = label;
			}
		}
		
		return result;
	}
}
