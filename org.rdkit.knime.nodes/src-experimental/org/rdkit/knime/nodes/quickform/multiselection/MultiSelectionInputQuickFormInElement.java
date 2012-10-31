/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2010
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
package org.rdkit.knime.nodes.quickform.multiselection;

import java.util.LinkedHashSet;
import java.util.Set;

import org.knime.core.util.node.quickform.in.AbstractQuickFormInElement;

/**
 * A form element to select values from a list of values.
 * 
 * This class needs to be moved into the core.util plug-in.
 *
 * @author Manuel Schwarze, based on work of Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public class MultiSelectionInputQuickFormInElement extends
        AbstractQuickFormInElement {
	
	//
	// Constants
	//
    private static final long serialVersionUID = -6117453817741563224L;

    //
    // Members
    //
    
	/** Set of choices to be selected. */
    private Set<String> m_choiceValues;
    
    /** Array with selected values. */
    private String[] m_values;

    /** Option to determine, if the GUI component should be a one-line element only. */
    private boolean m_bSmallGui;

    //
    // Constructors
    //
    
    /**
     * Create a multi value input with a given description.
     *
     * @param label The label, not null!
     * @param description The description, possibly null.
     * @param weight Weight factory,
     *        lighter value for more top-level alignment
     */
    public MultiSelectionInputQuickFormInElement(final String label,
            final String description, final int weight) {
        super(label, description, weight);
        m_choiceValues = new LinkedHashSet<String>();
        m_values = new String[0];
    }
    
    //
    // Public Methods
    //

    /** {@inheritDoc} */
    @Override
    public Type getType() {
        return Type.ValueFilterInput; // TODO: Change to new type
    }

    /**
     * Sets the choices for selections.
     * 
     * @param values Set of values to set (does not ensure that the set value is one
     *            of the choices)
     */
    public void setChoiceValues(final Set<String> values) {
        if (values == null) {
            m_choiceValues = new LinkedHashSet<String>();
        } else {
            m_choiceValues = new LinkedHashSet<String>(values);
        }
    }

    /** 
     * Returns the choices the user can choose from.
     * 
     * @return the value (not necessarily a string from the choices) 
     */
    public Set<String> getChoiceValues() {
        return m_choiceValues;
    }

    /**
     * Returns the selections.
     * 
     * @return the selected values.
     */
    public String[] getValues() {
        return m_values;
    }

    /**
     * Sets the selections.
     * 
     * @param values the values to be selected.
     */
    public void setValues(final String[] values) {
        if (values != null) {
            m_values = values;
        } else {
            m_values = new String[0];
        }
    }
    
    /**
     * Determines, if the GUI that the user sees for selections shall
     * be extra small, usually a one-line element. If set, a click
     * on such a one-line element will open a popup to see the full
     * list of choices and do the selections.
     * 
     * @return True, if small GUI component is desired. False otherwise.
     */
    boolean isSmallGui() {
    	return m_bSmallGui;
    }
    
    /**
     * Sets the option that the GUI that the user sees for selections shall
     * be extra small, usually a one-line element. If set, a click
     * on such a one-line element will open a popup to see the full
     * list of choices and do the selections.
     * 
     * @param b Set to true, if small GUI component is desired. False otherwise.
     */
    void setSmallGui(boolean b) {
    	m_bSmallGui = b;
    }
}
