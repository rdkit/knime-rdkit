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
package org.rdkit.knime.nodes.moleculesubstructfilter;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.chem.types.SmartsValue;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.rdkit.knime.types.RDKitMolValue;

/**
 * This class contains the dialog for the dictionary based substructure filter
 * node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class RDKitMoleculeSubstructFilterNodeDialogPane extends NodeDialogPane {
    private final ColumnSelectionComboxBox m_rdkitColumn =
            new ColumnSelectionComboxBox((Border)null, RDKitMolValue.class);

    private final ColumnSelectionComboxBox m_queryColumn =
            new ColumnSelectionComboxBox((Border)null, RDKitMolValue.class);

    private final JSpinner m_minimumMatches = new JSpinner(
            new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));

    private final JRadioButton m_matchAll = new JRadioButton("All");

    private final JRadioButton m_matchCount = new JRadioButton("At least");

    private final RDKitMoleculeSubstructFilterSettings m_settings =
            new RDKitMoleculeSubstructFilterSettings();

    /**
     * Creates a new node dialog.
     */
    public RDKitMoleculeSubstructFilterNodeDialogPane() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 0, 2, 0);

        p.add(new JLabel("RDKit Mol column:   "), c);
        c.gridx = 1;
        p.add(m_rdkitColumn, c);

        c.gridx = 0;
        c.gridy++;
        p.add(new JLabel("Query Mol column:   "), c);
        c.gridx = 1;
        p.add(m_queryColumn, c);

        c.gridx = 0;
        c.gridy++;
        p.add(new JLabel("Match:   "), c);
        c.gridx = 1;
        p.add(m_matchAll, c);

        JPanel p2 = new JPanel(new GridBagLayout());
        p2.add(m_matchCount);
        p2.add(m_minimumMatches);
        c.gridy++;
        p.add(p2, c);

        ((JSpinner.DefaultEditor)m_minimumMatches.getEditor()).getTextField()
                .setColumns(4);

        ButtonGroup bg = new ButtonGroup();
        bg.add(m_matchAll);
        bg.add(m_matchCount);
        ChangeListener cl = new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_minimumMatches.setEnabled(m_matchCount.isSelected());
            }
        };
        m_matchCount.addChangeListener(cl);

        addTab("Standard Settings", p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);

        m_rdkitColumn.update(specs[0], m_settings.rdkitColumn());
        m_queryColumn.update(specs[1], m_settings.queryColumn());
        if (m_settings.minimumMatches() == 0) {
            m_matchAll.setSelected(true);
            m_matchCount.setSelected(false);
            m_minimumMatches.setEnabled(false);
        } else {
            m_matchAll.setSelected(false);
            m_matchCount.setSelected(true);
            m_minimumMatches.setValue(m_settings.minimumMatches());
            m_minimumMatches.setEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.rdkitColumn(m_rdkitColumn.getSelectedColumn());
        m_settings.queryColumn(m_queryColumn.getSelectedColumn());
        if (m_matchAll.isSelected()) {
            m_settings.minimumMatches(0);
        } else {
            m_settings.minimumMatches((Integer)m_minimumMatches.getValue());
        }
        m_settings.saveSettings(settings);
    }
}
