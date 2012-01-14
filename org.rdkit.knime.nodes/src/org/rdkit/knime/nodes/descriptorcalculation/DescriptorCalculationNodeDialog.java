/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2011
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
package org.rdkit.knime.nodes.descriptorcalculation;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.rdkit.knime.types.RDKitMolValue;

/**
 * The NodeDialog for the "DescriptorCalculationNode" Node is used to specify the user adjustable setting.
 * This node dialog is derived from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Dillip K Mohanty
 */
public class DescriptorCalculationNodeDialog extends NodeDialogPane {


    /*
     * The tab's name.
     */
    private static final String TAB = "Settings";

    /*
     * The settings object for descriptor node.
     */
	private final DescriptorCalcSettings _settings = new DescriptorCalcSettings();
	
	@SuppressWarnings("unchecked")
	/*
     * The dropdown box instance contains the rdkit molecule column.
     */
	private final ColumnSelectionComboxBox _molColumn = new ColumnSelectionComboxBox(
			(Border) null, RDKitMolValue.class);
    
    /**
     * Creates a new {@link NodeDialogPane} for the column filter in order to
     * set the desired columns.
     */
    DescriptorCalculationNodeDialog() {
        super();
        JPanel colPanel = new JPanel();
        colPanel.add(new JLabel("Molecule column:"));
        colPanel.add(_molColumn);
        JPanel p1 = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(2, 2, 2, 2);
		c.gridy = 0;
		c.gridx = 0;
		//Add the Column dropdown panel
		p1.add(colPanel, c, 0);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.WEST;	
		JLabel l = new JLabel("Available descriptors:");
		l.setAlignmentX(Container.LEFT_ALIGNMENT);
		//Add label
		p1.add(l, c, 1);
		c.gridy++;
        
		//Add descriptor filter panel
		JPanel p = new DescriptorFilterPanel();
		p1.add(p, c , 2);
		
        super.addTab(TAB, p1);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
    	
    	_settings.loadSettingsForDialog(settings);
    	_molColumn.update(specs[0], _settings.colName);
    	
        if (DescriptorCalculationNodeModel.names == null
                || DescriptorCalculationNodeModel.names.size() == 0) {
            throw new NotConfigurableException("No descriptors available for "
                    + "selection.");
        }
    	
    	String[] desclist = _settings.selectedDescriptors;
        HashSet<String> list = new HashSet<String>();
        for (int i = 0; i < desclist.length; i++) {
                list.add(desclist[i]);
        }
        
        // set inclusion list on the panel
        JPanel p1 = (JPanel)getTab(TAB);
        DescriptorFilterPanel p = (DescriptorFilterPanel)p1.getComponent(2);
        p.update(DescriptorCalculationNodeModel.names, list);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) 
        throws InvalidSettingsException {
    	JPanel p1 = (JPanel)getTab(TAB);
        DescriptorFilterPanel p = (DescriptorFilterPanel)p1.getComponent(2);
        Set<String> list = p.getIncludedDescriptorSet();
        _settings.colName = _molColumn.getSelectedColumn();
		_settings.selectedDescriptors = (String[]) list.toArray(new String[] {});
		_settings.saveSettings(settings);
        
    }
}
