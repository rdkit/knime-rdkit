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

import java.util.LinkedHashSet;
import java.util.Set;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.quickform.AbstractQuickFormConfiguration;

/**
 * Configuration of a multi selection input node.
 * 
 * @author Manuel Schwarze, based on work of Thomas Gabriel, KNIME.com, Zurich, Switzerland
 */
final class MultiSelectionInputQuickFormInConfiguration
    extends AbstractQuickFormConfiguration
            <MultiSelectionInputQuickFormValueInConfiguration> {

	//
	// Members
	//
	
	/** Set of choices to be selected. */
    private final Set<String> m_choices
        = new LinkedHashSet<String>();
    
    /** The name of the column in the output table that contains the user's selections. */
    private String m_strColumnName;
    
    /** Option to determine, if the GUI component should be a one-line element only. */
    private boolean m_bSmallGui;

    // 
    // Public Methods
    //
    
    /** {@inheritDoc} */
    @Override
    public MultiSelectionInputQuickFormPanel createController() {
        return new MultiSelectionInputQuickFormPanel(this);
    }

    /** {@inheritDoc} */
    @Override
    public MultiSelectionInputQuickFormValueInConfiguration
            createValueConfiguration() {
        return new MultiSelectionInputQuickFormValueInConfiguration();
    }

    /**
     * Returns the choices the user can choose from.
     * 
     * @return the choices out of which the value should be selected
     */
    String[] getChoices() {
        return m_choices.toArray(new String[0]);
    }
    
    /**
     * Returns the column name to be used in the output table.
     * 
     * @return Column name.
     */
    String getColumnName() {
    	return m_strColumnName;
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
     * Sets the choices for selections.
     * 
     * @param choiceValues Set containing choices.
     */
    void setChoices(final Set<String> choiceValues) {
        m_choices.clear();
        if (choiceValues != null) {
            m_choices.addAll(choiceValues);
        }
    }
    
    /**
     * Sets the column name to be used for the output table.
     * 
     * @param strColumnName Column name to be used.
     */
    void setColumnName(final String strColumnName) {
    	m_strColumnName = strColumnName;
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

    /** 
     * Save config to argument.
     * @param settings To save to.
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        settings.addStringArray("choices", getChoices());
        settings.addString("columnName", getColumnName());
        settings.addBoolean("smallGui", isSmallGui());
    }

    /** 
     * Load config in model.
     * @param settings To load from.
     * @throws InvalidSettingsException If that fails for any reason.
     */
    @Override
    public void loadSettingsInModel(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        super.loadSettingsInModel(settings);
        String[] choices = settings.getStringArray("choices");
        m_choices.clear();
        for (int i = 0; i < choices.length; i++) {
        	m_choices.add(choices[i]);
        }
        m_strColumnName = settings.getString("columnName");
        m_bSmallGui = settings.getBoolean("smallGui");
    }

    /** 
     * Load settings in dialog, init defaults if that fails.
     * @param settings To load from.
     */
    @Override
    public void loadSettingsInDialog(final NodeSettingsRO settings) {
        super.loadSettingsInDialog(settings);
        String[] choices = settings.getStringArray("choices", new String[0]);
        m_choices.clear();
        for (int i = 0; i < choices.length; i++) {
        	m_choices.add(choices[i]);
        }
        m_strColumnName = settings.getString("columnName", "Selections");
        m_bSmallGui = settings.getBoolean("smallGui", false);
    }
}
