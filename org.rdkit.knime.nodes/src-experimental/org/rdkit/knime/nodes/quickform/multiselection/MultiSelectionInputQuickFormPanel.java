/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2012
 * Novartis Institutes for BioMedical Research
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

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.quickform.QuickFormConfigurationPanel;
import org.knime.core.util.node.quickform.in.AbstractQuickFormInElement;
import org.rdkit.knime.util.MultiSelectionListPanel;

/**
 * Panel shown in meta node dialogs, displaying a multi selection list.
 *
 * @author Manuel Schwarze, based on work of Thomas Gabriel, KNIME.com, Zurich, Switzerland
 */
public class MultiSelectionInputQuickFormPanel extends
    QuickFormConfigurationPanel
        <MultiSelectionInputQuickFormValueInConfiguration> {

	//
	// Constants
	//
	
    /** Serial number. */
	private static final long serialVersionUID = 6385271997303439116L;
	
	//
	// Members
	//
	
	/** The GUI component to be shown to let the user select values. */
    private final MultiSelectionListPanel m_listPanel;

    //
    // Constructor
    //
    
    /** Creates a new multi-selection configuration.
     * @param cfg underlying config object
     */
    public MultiSelectionInputQuickFormPanel(
            final MultiSelectionInputQuickFormInConfiguration cfg) {
        super(new BorderLayout(10, 10));
        JLabel label = new JLabel(cfg.getDescription());
        add(label, BorderLayout.NORTH);
        String[] choices = cfg.getChoices();
        if (choices == null || choices.length == 0) {
            add(new JLabel("No choices available."), BorderLayout.SOUTH);
            m_listPanel = null;
        } else {
        	if (cfg.isSmallGui()) {
        		String strLabel = cfg.getLabel();
        		if (strLabel != null && !strLabel.isEmpty()) {
        			add(new JLabel(strLabel), BorderLayout.WEST);
        		}
        		final int iMaxRows = 7;
        		m_listPanel = new MultiSelectionListPanel(cfg.getChoices(), iMaxRows, true);
        		add(m_listPanel, BorderLayout.CENTER);
        	}
        	else {
        		String strLabel = cfg.getLabel();
    			JPanel panel = new JPanel(new BorderLayout());
        		if (strLabel != null && !strLabel.isEmpty()) {
	        		String borderLabel = " " + cfg.getLabel() + " ";
	    			panel.setBorder(BorderFactory.createCompoundBorder(
	    					BorderFactory.createEmptyBorder(5, 0, 5, 0), 
	    					BorderFactory.createCompoundBorder(
	    						BorderFactory.createTitledBorder(borderLabel),
	    						BorderFactory.createEmptyBorder(0, 5, 5, 5))));
        		}
        		m_listPanel = new MultiSelectionListPanel(cfg.getChoices(), 7, false);
        		panel.add(m_listPanel, BorderLayout.CENTER);
        		add(panel, BorderLayout.CENTER);
        	}

        	if (m_listPanel != null) {
        		m_listPanel.setPreferredSize(new Dimension(250, 
        				m_listPanel.getPreferredSize().height));
        	}
        }
        loadValueConfig(cfg.getValueConfiguration());
    }

    //
    // Public Methods
    //

    /** {@inheritDoc} */
    @Override
    public void saveSettings(
                final MultiSelectionInputQuickFormValueInConfiguration config)
                throws InvalidSettingsException {
        config.setValues(getSelectedValues());
    }

    /** {@inheritDoc} */
    @Override
    public void loadSettings(
                final MultiSelectionInputQuickFormValueInConfiguration cfg) {
        loadValueConfig(cfg);
    }
    
    @Override
    public void updateQuickFormInElement(final AbstractQuickFormInElement e) throws InvalidSettingsException {
        MultiSelectionInputQuickFormInElement cast = AbstractQuickFormInElement.cast(
        		MultiSelectionInputQuickFormInElement.class, e);
        if (m_listPanel != null) {
            cast.setValues(getSelectedValues());
        }
    }

    //
    // Private Methods
    //
    
    /**
     * Returns the currently selected values from the
     * multi-selection list.
     * 
     * @return Array of selected values. Never null, but possibly empty.
     */
    private String[] getSelectedValues() {
        Object[] arrSelectedValues = m_listPanel.getSelections();
        String[] arrSelectedStrings = new String[arrSelectedValues == null ? 0 :
        	arrSelectedValues.length];
        
        for (int i = 0; i < arrSelectedStrings.length; i++) {
        	arrSelectedStrings[i] = arrSelectedValues[i].toString();
        }
        
        return arrSelectedStrings;
    }

    /**
     * Populates the list with choices taken from the passed in 
     * configuration.
     * 
     * @param cfg Configuration with choices for multi-selection.
     * 		Can be null to delete all choices.
     */
    private void loadValueConfig(
            final MultiSelectionInputQuickFormValueInConfiguration cfg) {
        if (m_listPanel != null) {
        	m_listPanel.selectValues(cfg == null ? null : cfg.getValues());
        }
    }
}
