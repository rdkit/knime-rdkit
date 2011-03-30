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
 *   08.02.2011 (meinl): created
 */
package org.rdkit.knime.nodes.multiplesubstrucfilter;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class holds the settings for the dictionary-based substructure filter
 * node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class RDKitDictSubstructFilterSettings {
    private String m_rdkitColumn;

    private String m_smartsColumn;

    private int m_minMatches;

    /**
     * Returns the name of the column containing RDKit molecules.
     *
     * @return the column's name
     */
    public String rdkitColumn() {
        return m_rdkitColumn;
    }

    /**
     * Sets the name of the column containing RDKit molecules.
     *
     * @param colName the column's name
     */
    public void rdkitColumn(final String colName) {
        m_rdkitColumn = colName;
    }

    /**
     * Returns the name of the column containing the SMARTS patterns.
     *
     * @return the column's name
     */
    public String smartsColumn() {
        return m_smartsColumn;
    }

    /**
     * Sets the name of the column containing the SMARTS patterns.
     *
     * @param colName the column's name
     */
    public void smartsColumn(final String colName) {
        m_smartsColumn = colName;
    }


    /**
     * Returns the minimum number of pattern that must match for each molecule.
     * A value of 0 means that all patterns must match.
     *
     * @return the minimum number of matches
     */
    public int minimumMatches() {
        return m_minMatches;
    }

    /**
     * Sets the minimum number of pattern that must match for each molecule.
     * A value of 0 means that all patterns must match.
     *
     * @param num the minimum number of matches
     */
    public void minimumMatches(final int num) {
        m_minMatches = num;
    }

    /**
     * Saves the settings into the settings object.
     *
     * @param settings a settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString("rdkitColumn", m_rdkitColumn);
        settings.addString("smartsColumn", m_smartsColumn);
        settings.addInt("minimumMatches", m_minMatches);
    }


    /**
     * Loads the settings from settings object.
     *
     * @param settings a settings object
     * @throws InvalidSettingsException if a required setting is missing
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_rdkitColumn = settings.getString("rdkitColumn");
        m_smartsColumn = settings.getString("smartsColumn");
        m_minMatches = settings.getInt("minimumMatches");
    }

    /**
     * Loads the settings from settings object using default values for
     * missing settings.
     *
     * @param settings a settings object
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_rdkitColumn = settings.getString("rdkitColumn", null);
        m_smartsColumn = settings.getString("smartsColumn", null);
        m_minMatches = settings.getInt("minimumMatches", 1);
    }
}
