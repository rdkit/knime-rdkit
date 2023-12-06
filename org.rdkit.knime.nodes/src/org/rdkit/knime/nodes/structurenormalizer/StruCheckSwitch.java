/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C)2023
 *  Novartis Pharma AG, Switzerland
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
package org.rdkit.knime.nodes.structurenormalizer;

public enum StruCheckSwitch {
	cc("Check for collisions", "Check for collisions of atoms with other atoms or bonds"),
	cs("Check stereo conventions", "Check stereo conventions"),
	da("Convert atom text strings to properties", "Convert atom text strings to properties"),
	dg("Convert ISIS groups to S-Groups", "Convert ISIS groups to S-Groups"),
	ds("Convert CPSS STEXT to data fields", "Convert CPSS STEXT to data fields"),
	dw("Squeeze whitespace out of identifiers", "Squeeze whitespace out of identifiers"),
	dz("Strip most of the trailing zeros", "Strip most of the trailing zeros"),
	tm("Split off minor fragments", "Split off minor fragments and keep only largest one"),
	;
	
	//
	// Members
	//

	/** The short description. */
	private String m_strShortDescription;
	
	/** The long description. */
	private String m_strLongDescription;

	//
	// Constructor
	//
	
	/**
	 * Creates a new switch representation.
	 *  
	 * @param strShortDescription Short description.
	 * @param strLongDescription Long description.
	 */
	private StruCheckSwitch(final String strShortDescription, final String strLongDescription) {
		m_strShortDescription = strShortDescription;
		m_strLongDescription = strLongDescription;
	}
	
	//
	// Public Methods
	//
	
	/**
	 * Returns the short description of a switch.
	 * 
	 * @return Short description.
	 */
	public String getShortDescription() {
		return m_strShortDescription;
	}
	
	/**
	 * Returns the long description of a switch.
	 * 
	 * @return Long description.
	 */
	public String getLongDescription() {
		return m_strLongDescription;
	}

	/**
	 * Returns the string representation of the code, the short description.
	 * 
	 * @return String representation.
	 */
	@Override
	public String toString() {
		return getShortDescription();
	}
	
	//
	// Static Public Methods
	//
	
	/**
	 * Generates a string with all passed in switches. Each switch will be
	 * put on a separate line and will start with a minus.
	 * 
	 * @param switches Switches to be put in return string. Can be null.
	 * 
	 * @return Always not null. Can be empty.
	 */
	public static String generateSwitches(StruCheckSwitch... switches) {
		StringBuilder sb = new StringBuilder();
		
		for (StruCheckSwitch switchCode : switches) {
			sb.append("-").append(switchCode.name()).append("\n");
		}
		
		return sb.toString();
	}
}
