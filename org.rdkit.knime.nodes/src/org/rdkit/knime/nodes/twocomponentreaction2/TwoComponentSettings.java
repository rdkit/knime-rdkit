/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *
 * History
 *   28.10.2011 (meinl): created
 */
package org.rdkit.knime.nodes.twocomponentreaction2;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class holds the settings for the two-component reaction node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class TwoComponentSettings {
    private String m_firstColumn;

    private String m_secondColumn;

    private String m_rxnColumn;

    private String m_reactionSmarts;

    private boolean m_maxtrixExpansion;

    /**
     * Sets the column name containing the first reactant.
     *
     * @param colName a column name
     */
    public void firstColumn(final String colName) {
        m_firstColumn = colName;
    }

    /**
     * Returns the column name containing the first reactant.
     *
     * @return a column name
     */
    public String firstColumn() {
        return m_firstColumn;
    }

    /**
     * Sets the column name containing the second reactant.
     *
     * @param colName a column name
     */
    public void secondColumn(final String colName) {
        m_secondColumn = colName;
    }

    /**
     * Returns the column name containing the second reactant.
     *
     * @return a column name
     */
    public String secondColumn() {
        return m_secondColumn;
    }

    /**
     * Sets the optional column name containing the Rxn.
     *
     * @param colName a column name
     */
    public void rxnColumn(final String colName) {
        m_rxnColumn = colName;
    }

    /**
     * Returns the optional column name containing the Rxn.
     *
     * @return a column name
     */
    public String rxnColumn() {
        return m_rxnColumn;
    }

    /**
     * Sets the optional reaction smarts pattern.
     *
     * @param smarts a reaction smarts pattern
     */
    public void reactionSmarts(final String smarts) {
        m_reactionSmarts = smarts;
    }

    /**
     * Returns the optional reaction smarts pattern.
     *
     * @return a reaction smarts pattern
     */
    public String reactionSmarts() {
        return m_reactionSmarts;
    }

    /**
     * Sets whether matrix expansion should be performed.
     *
     * @param b <code>true</code> if expansion should be performed,
     *            <code>false</code> otherwise
     */
    public void matrixExpansion(final boolean b) {
        m_maxtrixExpansion = b;
    }

    /**
     * Returns whether matrix expansion should be performed.
     *
     * @return <code>true</code> if expansion should be performed,
     *         <code>false</code> otherwise
     */
    public boolean matrixExpansion() {
        return m_maxtrixExpansion;
    }

    /**
     * Saves the settings into the settings object.
     *
     * @param settings a settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString("firstColumn", m_firstColumn);
        settings.addString("secondColumn", m_secondColumn);
        settings.addString("rxnColumn", m_rxnColumn);
        settings.addString("reactionSmarts", m_reactionSmarts);
        settings.addBoolean("matrixExpansion", m_maxtrixExpansion);
    }

    /**
     * Loads the settings from settings object using default values for missing
     * settings.
     *
     * @param settings a settings object
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_firstColumn = settings.getString("firstColumn", null);
        m_secondColumn = settings.getString("secondColumn", null);
        m_rxnColumn = settings.getString("rxnColumn", null);
        m_reactionSmarts = settings.getString("reactionSmarts", "");
        m_maxtrixExpansion = settings.getBoolean("matrixExpansion", false);
    }

    /**
     * Loads the settings from settings object.
     *
     * @param settings a settings object
     * @throws InvalidSettingsException if a required setting is missing
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_firstColumn = settings.getString("firstColumn");
        m_secondColumn = settings.getString("secondColumn");
        m_rxnColumn = settings.getString("rxnColumn");
        m_reactionSmarts = settings.getString("reactionSmarts");
        m_maxtrixExpansion = settings.getBoolean("matrixExpansion");
    }
}
