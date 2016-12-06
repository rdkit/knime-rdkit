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
package org.rdkit.knime.wizards.samples.calculatorsplitter_multithread;

import org.knime.core.node.util.ButtonGroupEnumInterface;

/**
 * This enumeration defines parse error policies for molecule conversion.
 * 
 * @author Greg Landrum
 */
public enum ParseErrorPolicy implements ButtonGroupEnumInterface {

	/**
	 * Use this value to send error rows to second output port.
	 */
	SPLIT_ROWS("Send error rows to second output", "The table at the second "
        + "port contains the input rows with problematic structures"),
        
	/**
	 * Use this value to insert missing values.
	 */
    MISS_VAL("Insert missing values", "If the input structure can't be "
        		+ "translated, a missing value is inserted.");

	//
	// Members
	//	
	
	/** Name of the policy returns by the method {@link #getText()}. */
    private final String m_name;

	/** Tooltip to be used when the policy element is used in a GUI element. */
    private final String m_tooltip;

    //
    // Constructors
    //
    
    /**
     * Creates a new enumeration value with the specified text (name) and tooltip.
     * 
     * @param name Name to be shown to the user.
     * @param tooltip Tooltip to be shown to the user.
     */
    ParseErrorPolicy(final String name, final String tooltip) {
        m_name = name;
        m_tooltip = tooltip;
    }

    //
    // Public Methods
    //    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return m_name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getActionCommand() {
        return this.name();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTip() {
        return m_tooltip;
    }

    /**
     * {@inheritDoc}
     * Returns true for the SPLIT_ROWS value. False otherwise.
     */
    @Override
    public boolean isDefault() {
        return this.getActionCommand().equals(SPLIT_ROWS.getActionCommand());
    }

}
