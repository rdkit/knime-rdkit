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
package org.rdkit.knime.nodes.twocomponentreaction2;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.knime.chem.types.RxnValue;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.rdkit.knime.types.RDKitMolValue;

/**
 *
 * @author Greg Landrum
 */
public class RDKitTwoComponentReactionNodeDialogPane extends NodeDialogPane {
    private final ColumnSelectionComboxBox m_firstColumn =
            new ColumnSelectionComboxBox((Border)null, RDKitMolValue.class);

    private final ColumnSelectionComboxBox m_secondColumn =
        new ColumnSelectionComboxBox((Border)null, RDKitMolValue.class);


    private final JTextField m_smarts = new JTextField();

    private final ColumnSelectionComboxBox m_rxnColumn =
            new ColumnSelectionComboxBox((Border)null, RxnValue.class);

    private final JLabel m_rxnLabel = new JLabel("Rxn column   ");

    private final JLabel m_smartsLabel = new JLabel("Reaction SMARTS   ");

    private final JCheckBox m_matrixExpansion = new JCheckBox();

    private final TwoComponentSettings m_settings = new TwoComponentSettings();

    /**
     * Create a new dialog pane with some default components.
     */
    RDKitTwoComponentReactionNodeDialogPane() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 0);

        p.add(new JLabel("Reactants 1 RDKit Mol column   "), c);
        c.gridx = 1;
        p.add(m_firstColumn, c);

        c.gridx = 0;
        c.gridy++;
        p.add(new JLabel("Reactants 2 RDKit Mol column   "), c);
        c.gridx = 1;
        p.add(m_secondColumn, c);

        c.gridx = 0;
        c.gridy++;
        p.add(m_rxnLabel, c);
        c.gridx = 1;
        p.add(m_rxnColumn, c);

        c.gridx = 0;
        c.gridy++;
        p.add(m_smartsLabel, c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        p.add(m_smarts, c);

        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        c.gridy++;
        p.add(new JLabel("Do Matrix Expansion   "), c);
        c.gridx = 1;
        p.add(m_matrixExpansion, c);

        addTab("Standard settings", p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);

        // FIXME change this once the problems has been solved in the core
        boolean rxnTablePresent = (specs[2].getNumColumns() > 0);

        m_firstColumn.update(specs[0], m_settings.firstColumn());
        m_secondColumn.update(specs[0], m_settings.secondColumn());
        m_rxnLabel.setVisible(rxnTablePresent);
        m_rxnColumn.setVisible(rxnTablePresent);
        m_smartsLabel.setVisible(!rxnTablePresent);
        m_smarts.setVisible(!rxnTablePresent);
        if (rxnTablePresent) {
            m_rxnColumn.update(specs[2], m_settings.rxnColumn());
        }
        m_matrixExpansion.setSelected(m_settings.matrixExpansion());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.firstColumn(m_firstColumn.getSelectedColumn());
        m_settings.secondColumn(m_secondColumn.getSelectedColumn());
        m_settings.rxnColumn(m_rxnColumn.getSelectedColumn());
        m_settings.reactionSmarts(m_smarts.getText());
        m_settings.matrixExpansion(m_matrixExpansion.isSelected());
        m_settings.saveSettings(settings);
    }
}
