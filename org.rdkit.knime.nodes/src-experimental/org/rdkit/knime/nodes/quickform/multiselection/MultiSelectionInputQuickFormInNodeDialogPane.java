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

import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.quickform.nodes.in.QuickFormInNodeDialogPane;
import org.rdkit.knime.util.LayoutUtils;
import org.rdkit.knime.util.MultiSelectionListPanel;

/**
 * Dialog to node.
 * 
 * @author Manuel Schwarze, based on work of Thomas Gabriel, KNIME.com, Zurich,
 *         Switzerland
 */
final class MultiSelectionInputQuickFormInNodeDialogPane extends
		QuickFormInNodeDialogPane<MultiSelectionInputQuickFormInConfiguration> {

	//
	// Constantcs
	//
	
	// Ids for panels of card layout to switch for invalid and valid model selections
    
    /** Id for the panel that is shown when the list is getting edited. */
    private static final int LIST_EDIT_PANEL = 0;    
    
    /** Id for the panel that is shown when the default selections are being defined. */
    private static final int LIST_SELECTION_PANEL = 1;    

    //
    // Members
    //
    
    /** Text field for column name. */
	private final JTextField m_tfColumnName;

	/** Panel with CardLayout to switch between list edit and selection panel. */
	private JPanel m_cardLayoutListPanel;
	
	/** The GUI component for the multi-selection list to do the default selections. */
	private MultiSelectionListPanel m_listPanel;
	
	/** The text area for editing the list values. */
	private JTextArea m_taEditableList;
	
	/** The sort button shown with the editable list (text area). */
	private JButton m_btnSort1;
	
	/** The sort button shown with the multi-selection list. */
	private JButton m_btnSort2;
	
	/** The button to switch GUI to see the multi-selection list. */
	private JButton m_btnSelectDefaults;
	
	/** The button to switch GUI to see the text area to edit the list values. */
	private JButton m_btnEditList;
	
	/** The checkbox to determine, if a small GUI component shall be used. */
	private JCheckBox m_cbUseSmallGuiComponent;
	
	//
	// Constructor
	//
	
	/** Constructors, inits fields calls layout routines. */
	MultiSelectionInputQuickFormInNodeDialogPane() {
		m_tfColumnName = new JTextField(30);
        m_cardLayoutListPanel = new JPanel(new CardLayout());
        m_cardLayoutListPanel.add(createListEditPanel(), "" + LIST_EDIT_PANEL);
        m_cardLayoutListPanel.add(createListSelectionPanel(), "" + LIST_SELECTION_PANEL);
        m_cbUseSmallGuiComponent = new JCheckBox("Use small GUI component for selection");
		createAndAddTab();
	}
	
	//
	// Protected Methods
	//
	
	/**
	 * Creates the panel shown to edit the list of choices. 
	 * This gets called from the constructor.
	 * 
	 * @return Panel with editable list components. 
	 */
	protected JPanel createListEditPanel() {
		JPanel panel = new JPanel(new GridBagLayout(), true);
		
		m_taEditableList = new JTextArea();
		JScrollPane scrollPane = new JScrollPane(m_taEditableList, 
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		m_btnSort1 = new JButton("Sort");
		m_btnSort1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateList();
				m_listPanel.sort();
				updateTextArea();
			}
		});
		m_btnSelectDefaults = new JButton("Select Default >>");
		m_btnSelectDefaults.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateList();
				switchListPanel(LIST_SELECTION_PANEL);
			}
		});
		
    	int iRow = 0;
        LayoutUtils.constrain(panel, scrollPane, 0, iRow++, LayoutUtils.REMAINDER, 1, 
        		LayoutUtils.BOTH, LayoutUtils.NORTHWEST, 1.0, 1.0, 0, 0, 0, 0);
        LayoutUtils.constrain(panel, m_btnSort1, 0, iRow, 1, LayoutUtils.REMAINDER, 
        		LayoutUtils.NONE, LayoutUtils.SOUTHWEST, 0.0, 0.0, 10, 0, 0, 10);
        LayoutUtils.constrain(panel, m_btnSelectDefaults, 1, iRow++, LayoutUtils.REMAINDER, LayoutUtils.REMAINDER, 
        		LayoutUtils.HORIZONTAL, LayoutUtils.SOUTHWEST, 1.0, 0.0, 10, 0, 0, 0);
		
		return panel;
	}
	
	/**
	 * Creates the panel shown to select the default values. 
	 * This gets called from the constructor.
	 * 
	 * @return Panel with multi-selection list components. 
	 */
	protected JPanel createListSelectionPanel() {
		JPanel panel = new JPanel(new GridBagLayout(), true);
		
		m_listPanel = new MultiSelectionListPanel(new String[0], 7, false);
		m_listPanel.getModel().addListDataListener(new ListDataListener() {
			@Override
			public void intervalRemoved(ListDataEvent e) {
				updateTextArea();
			}
			
			@Override
			public void intervalAdded(ListDataEvent e) {
				updateTextArea();
			}
			
			@Override
			public void contentsChanged(ListDataEvent e) {
				updateTextArea();
			}
		});
		m_btnSort2 = new JButton("Sort");
		m_btnSort2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				m_listPanel.sort();
				updateTextArea();
			}
		});
		m_btnEditList = new JButton("<< Edit List Items");
		m_btnEditList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				switchListPanel(LIST_EDIT_PANEL);
			}
		});
		
    	int iRow = 0;
        LayoutUtils.constrain(panel, m_listPanel, 0, iRow++, LayoutUtils.REMAINDER, 1, 
        		LayoutUtils.BOTH, LayoutUtils.NORTHWEST, 1.0, 1.0, 0, 0, 0, 0);
        LayoutUtils.constrain(panel, m_btnSort2, 0, iRow, 1, LayoutUtils.REMAINDER, 
        		LayoutUtils.NONE, LayoutUtils.SOUTHWEST, 0.0, 0.0, 10, 0, 0, 10);
        LayoutUtils.constrain(panel, m_btnEditList, 1, iRow++, LayoutUtils.REMAINDER, LayoutUtils.REMAINDER, 
        		LayoutUtils.HORIZONTAL, LayoutUtils.SOUTHWEST, 1.0, 0.0, 10, 0, 0, 0);
		
		return panel;
	}
    /**
     * Switches the main panel to the panel with the specified id. Valid values are
     * LIST_EDIT_PANEL and LIST_SELECTION_PANEL.
     * 
     * @param panelId Id of panel to show to the user. 
     * 		If unknown, it does not do anything-
     */
    protected void switchListPanel(final int panelId) {
    	switch (panelId) {
    		case LIST_EDIT_PANEL:
	    	case LIST_SELECTION_PANEL:
	    		final CardLayout cardLayout = (CardLayout)m_cardLayoutListPanel.getLayout();
				cardLayout.show(m_cardLayoutListPanel, "" + panelId);
				break;
    	}
    }

	/** {@inheritDoc} */
	@Override
	protected void fillPanel(final JPanel panelWithGBLayout,
			final GridBagConstraints gbc) {
		addPairToPanel("Output Selection Column Name: ", m_tfColumnName,
				panelWithGBLayout, gbc);
		addPairToPanel("List of Choices: ", m_cardLayoutListPanel,
				panelWithGBLayout, gbc);
		addPairToPanel("GUI Option: ", m_cbUseSmallGuiComponent,
				panelWithGBLayout, gbc);
	}

	/** {@inheritDoc} */
	@Override
	protected void saveAdditionalSettings(
			final MultiSelectionInputQuickFormInConfiguration config)
			throws InvalidSettingsException {
		updateList();
		
		config.setChoices(createSet(m_listPanel.getValues()));
		config.setColumnName(m_tfColumnName.getText());
		config.setSmallGui(m_cbUseSmallGuiComponent.isSelected());
		MultiSelectionInputQuickFormValueInConfiguration valCfg = config
				.getValueConfiguration();
		valCfg.setValues(createArray(m_listPanel.getSelections()));
	}

	/** {@inheritDoc} */
	@Override
	protected void loadAdditionalSettings(
			final MultiSelectionInputQuickFormInConfiguration config) {
		MultiSelectionInputQuickFormValueInConfiguration valCfg = config
				.getValueConfiguration();
		String strColumnName = config.getColumnName();
		String[] arrChoices = config.getChoices();
		boolean bSmallGui = config.isSmallGui();
		String[] arrSelections = valCfg.getValues();

		m_tfColumnName.setText(strColumnName == null ? "" : strColumnName);
		m_listPanel.setValues(arrChoices);
		m_listPanel.selectValues(arrSelections);
		m_cbUseSmallGuiComponent.setSelected(bSmallGui);
		
		updateTextArea();
	}
	
	//
	// Private Methods
	//
    
    /**
     * Updates the list based on the current text in the text area that
     * defines the list elements.
     */
    private void updateList() {
    	String strUpdate = m_taEditableList.getText();
    	String strExisting = m_listPanel.getMultiLineTextValues("\n");
    	
    	if (strUpdate != null && !strUpdate.equals(strExisting)) {
    		m_listPanel.setMultiLineTextValues(m_taEditableList.getText(), "\n");
    	}
    }
    
    /**
     * Updates the text area based on the elements that are currently in the list.
     */
    private void updateTextArea() {
    	m_taEditableList.setText(m_listPanel.getMultiLineTextValues("\n"));
    }

	/** {@inheritDoc} */
	@Override
	protected MultiSelectionInputQuickFormInConfiguration createConfiguration() {
		return new MultiSelectionInputQuickFormInConfiguration();
	}
	
	/** 
	 * Creates a set of strings from the passed in object array.
	 * 
	 * @param arrItems Items to be converted to strings. Can be null.
	 * 
	 * @return Set of strings. Never null, but possibly empty.
	 */
	private Set<String> createSet(Object[] arrItems) {
		Set<String> setItems = new LinkedHashSet<String>();
		if (arrItems != null) {
			for (Object item : arrItems) {
				if (item != null) {
					setItems.add(item.toString());
				}
			}
		}
		return setItems;
	}	
	
	/** 
	 * Creates a string array from the passed in object array.
	 * 
	 * @param arrItems Items to be converted to strings. Can be null.
	 * 
	 * @return Array of strings. Never null, but possibly empty.
	 */
	private String[] createArray(Object[] arrItems) {
		return createSet(arrItems).toArray(new String[0]);
	}
}
