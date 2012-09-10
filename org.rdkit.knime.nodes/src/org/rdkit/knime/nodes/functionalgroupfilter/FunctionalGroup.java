/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2012
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
package org.rdkit.knime.nodes.functionalgroupfilter;

import java.text.ParseException;

/**
 * The class to represent a functional group and its properties.
 * 
 * @author Dillip K Mohanty
 * @author Manuel Schwarze
 */
public class FunctionalGroup {

	//
	// Constants
	//

	public static final String INDENT = "    ";
	
	//
	// Members
	//
	
	/** The unique name of the functional group. */
	private String m_strName = null;
	
	/** The smart pattern of the functional group. */
	private String m_strSmarts = null;
	
	/** The label of the functional group. */
	private String m_strLabel = null;
	
	/** The removal reaction of the functional group. */
	private String m_strRemovalReaction = null;
	
	/** The display label of the functional group. */
	private String m_strDisplayLabel = null;
	
	//
	// Constructor
	//
	
	/**
	 * Creates a new functional group from the specified line that has been taken
	 * from a definition file. It must be in the following format:<br>
	 * Name\tSmarts\tLabel\tRemovalReaction (optional)<br>
	 * Optional leading and trailing whitespaces will be cut off.
	 * The Label will be taken as display name.
	 * 
	 * @param strLine Line of functional group definition file. Must not be null.
	 * 
	 * @throws ParseException Thrown, if the functional group could not be
	 * 		parsed from the specified line. The message will contain the error.
	 */
	public FunctionalGroup(String strLine) throws ParseException {
		if (strLine == null) {
			throw new IllegalArgumentException("Function group line must not be null.");
		}
		
		String[] tokens = strLine.split("[\t]+");

		if (tokens == null) {
			throw new ParseException("Unable to identify fields of " +
					"function group definition.", 0);
		}
		
		if (tokens.length < 2) {
			throw new ParseException("Not enough fields specified for " +
					"function group definition.", 0);
		}
		
		m_strName = tokens[0].trim();
		
		if (m_strName.isEmpty()) {
			throw new ParseException("No valid name specified for " +
					"function group definition.", 0);
		}
		
		m_strSmarts = tokens[1].trim();
		
		if (m_strSmarts.isEmpty()) {
			throw new ParseException("No valid SMARTS specified for " +
					"function group definition.", 0);
		}

		// Be gracious and ignore a missing label
		if (tokens.length > 2) {
			m_strLabel = tokens[2].trim();
		}
		if (m_strLabel == null || m_strLabel.isEmpty()) {
			m_strLabel = m_strName.replace(".", " ");
		}

		
		if (tokens.length > 3) {
			m_strRemovalReaction = tokens[3].trim();
			
			if (m_strRemovalReaction.isEmpty()) {
				m_strRemovalReaction = null;
			}
		}
		
		// Indent the display label, if the line starts with whitespaces
		m_strDisplayLabel = (!strLine.startsWith(m_strName) ? INDENT : "") + m_strLabel;
	}
	
	/**
	 * Creates a new functional group with the specified values functional group.
	 * 
	 * @param uniqueName The unique name of the group. Must not be null.
	 * @param smarts The SMARTS to identify the group. Must not be null.
	 * @param label A friendly name of the group. Usually also used as display label.
	 * 		Must not be null.
	 * @param removalReaction Reaction to remove the functional group from the molecule. 
	 * 		This is necessary for cases like boronic ethers where the full functional 
	 * 		group cannot be specified in SMARTS. Can be null.
	 */
	public FunctionalGroup(String uniqueName, String smarts, String label, 
			String removalReaction, String displayLabel) {
		m_strName = uniqueName;
		m_strSmarts = smarts;
		m_strLabel = label;
		m_strRemovalReaction = removalReaction;
		m_strDisplayLabel = displayLabel;
	}

	//
	// Public Methods
	//

	/**
	 * Returns the unique name of the functional group.
	 * 
	 * @return The unique name.
	 */
	public String getName() {
		return m_strName;
	}

	/**
	 * Returns the SMARTS to identify the functional group.
	 * 
	 * @return The SMARTS to identify the functional group.
	 */
	public String getSmarts() {
		return m_strSmarts;
	}

	/**
	 * Returns the label of the functional group.
	 * 
	 * @return The label of the functional group.
	 */
	public String getLabel() {
		return m_strLabel;
	}

	/**
	 * Returns the optional removal reaction to remove the functional 
	 * group from the molecule. This is necessary for cases like 
	 * boronic ethers where the full functional group cannot be 
	 * specified in SMARTS. 
	 * 
	 * @return The removal reaction, if set. Otherwise null.
	 */
	public String getRemovalReaction() {
		return m_strRemovalReaction;
	}

	/**
	 * Returns the label that will be used to display the functional
	 * group to the user.
	 * 
	 * @return The display label.
	 */
	public String getDisplayLabel() {
		return m_strDisplayLabel;
	}

	/**
	 * Sets an alternative display label for this functional group.
	 * 
	 * @param displayLabel The displayLabel to set. Must not be null.
	 */
	public void setDisplayLabel(String displayLabel) {
		if (displayLabel == null) {
			throw new IllegalArgumentException("The display label must not be null.");
		}
		
		m_strDisplayLabel = displayLabel;
	}
	
	/**
	 * Creates a string presentation for this functional group to
	 * be used as a tooltip.
	 * 
	 * @return Tooltip representation.
	 */
	public String getTooltip() {
		// Include name and SMARTS
		StringBuffer sb = new StringBuffer("<html><strong>")
			.append(m_strName).append(":</strong> ")
			.append(m_strSmarts);
		
		// Optionally: Include removal reaction
		if (m_strRemovalReaction != null) {
			sb.append("<br>")
			.append("<strong>Removal Reaction: </strong>")
			.append(m_strRemovalReaction)
			.append("<html>");
		}
		
		return sb.toString();
	}
}
