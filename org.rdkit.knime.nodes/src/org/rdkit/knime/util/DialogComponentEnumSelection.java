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
package org.rdkit.knime.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.FlowVariableModelButton;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.StringIconListCellRenderer;

/**
 * Provides a standard component for a dialog that allows to select an enumeration value.
 *
 * @author Manuel Schwarze, based on work of Thomas Gabriel
 * @author Thomas Gabriel, University of Konstanz
 * @param <T> An arbitrary Enumeration.
 *
 */
public final class DialogComponentEnumSelection<T extends Enum<T>> extends DialogComponent {

	//
	// Members
	//
	
	/** The combobox for value selection. */
    private final JComboBox m_combobox;

    /** The label shown at the left side of the combobox. */
    private final JLabel m_label;

    /** The optional flow variable button shown at the right side of the combobox. */
    private final FlowVariableModelButton m_fvmButton;

    //
    // Constructors
    //
    
    /**
     * Constructor that puts label and combobox into panel. It expects the user
     * to make a selection, thus, at least one item in the list of selectable
     * items is required. When the settings are applied, the model stores one of
     * the enumerations of the provided list.
     *
     * @param enumModel The model that stores the value for this component.
     * @param label Label for dialog in front of combobox
     * @param list List of items for the combobox. If no list or an empty list is provided all 
     * 		possible values will be taken from the Enumeration used in the settings model.
     */
    public DialogComponentEnumSelection(
    		final SettingsModelEnumeration<T> enumModel, final String label,
            final T... list) {
        this(enumModel, label, Arrays.asList(list), null);
    }

    /**
     * Constructor that puts label and combobox into panel. It expects the user
     * to make a selection, thus, at least one item in the list of selectable
     * items is required. When the settings are applied, the model stores one of
     * the enumerations of the provided list.
     *
     * @param enumModel the model that stores the value for this component.
     * @param label label for dialog in front of combobox
     * @param list List of items for the combobox. If no list or an empty list is provided all 
     * 		possible values will be taken from the Enumeration used in the settings model.
     *
     * @throws NullPointerException if one of the strings in the list is null
     * @throws IllegalArgumentException if the list is empty or null.
     */
    public DialogComponentEnumSelection(
            final SettingsModelEnumeration<T> enumModel, final String label,
            final Collection<T> list) {
        this(enumModel, label, list, null);
    }

    /**
     * Constructor that puts label and combobox into panel. It expects the user
     * to make a selection, thus, at least one item in the list of selectable
     * items is required. When the settings are applied, the model stores one of
     * the enumerations of the provided list.
     *
     * @param enumModel the model that stores the value for this component.
     * @param label label for dialog in front of combobox
     * @param list List of items for the combobox. If no list or an empty list is provided all 
     * 		possible values will be taken from the Enumeration used in the settings model.
     * @param fvm model exposed to choose from available flow variables
     *
     * @throws NullPointerException if one of the strings in the list is null
     * @throws IllegalArgumentException if the list is empty or null.
     */
    public DialogComponentEnumSelection(
            final SettingsModelEnumeration<T> enumModel, final String label,
            		final Collection<T> list, final FlowVariableModel fvm) {
        super(enumModel);

        Collection<T> listEnums = list;
        
        if ((listEnums == null) || (listEnums.size() == 0)) {
        	listEnums = Arrays.asList(enumModel.getEnumerationType().getEnumConstants());
        }
        
        m_label = new JLabel(label);
        getComponentPanel().add(m_label);
        m_combobox = new JComboBox();
        m_combobox.setRenderer(new StringIconListCellRenderer());

        for (final T enumValue : listEnums) {
            if (enumValue == null) {
                throw new NullPointerException("Options in the selection"
                        + " list can't be null");
            }
            m_combobox.addItem(enumValue);
        }

        getComponentPanel().add(m_combobox);

        m_combobox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    // if a new item is selected update the model
                    try {
                        updateModel();
                    } catch (final InvalidSettingsException ise) {
                        // ignore it here
                    }
                }
            }
        });

        // We need to update the selection, when the model changes.
        // Originally this was using the method prependChangeListener, which is protected and not visible.
        getModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });

        // add variable editor button if so desired
        if (fvm != null) {
            fvm.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(final ChangeEvent evt) {
                     getModel().setEnabled(!fvm.isVariableReplacementEnabled());
                }
            });
            m_fvmButton = new FlowVariableModelButton(fvm);
            getComponentPanel().add(m_fvmButton);
        } else {
            m_fvmButton = null;
        }

        // Call this method to be in sync with the settings model
        updateComponent();
    }


    /**
     * {@inheritDoc}
     */
	@Override
    @SuppressWarnings("unchecked")
    protected void updateComponent() {
        final T valueModel =
            ((SettingsModelEnumeration<T>)getModel()).getValue();
        final T valueCombo = (T)m_combobox.getSelectedItem();
        
        // Check, if an update is really necessary
        boolean update;
        if (valueModel == null) {
            update = m_combobox.getSelectedItem() != null;
        } else {
            update = !valueModel.equals(valueCombo);
        }
        
        if (update) {
            m_combobox.setSelectedItem(valueModel);
        }
        
        // Also update the enable status
        setEnabledComponents(getModel().isEnabled());

        // Make sure the model is in sync (in case model value isn't selected)
        T selItem =
            (T)m_combobox.getSelectedItem();
        try {
            if ((selItem == null && valueModel != null)
                    || (selItem != null && !selItem.equals(valueModel))) {
                // if the (initial) value in the model is not in the list
                updateModel();
            }
        } catch (InvalidSettingsException e) {
            // ignore invalid values here
        }
    }

    /**
     * Transfers the current value from the component into the model.
     * 
     * @throws InvalidSettingsException Thrown, if no item is selected.
     */
    @SuppressWarnings("unchecked")
	private void updateModel() throws InvalidSettingsException {

        if (m_combobox.getSelectedItem() == null) {
            ((SettingsModelEnumeration<T>)getModel()).setValue((T)null);
            m_combobox.setBackground(Color.RED);
            // put the color back to normal with the next selection.
            m_combobox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    m_combobox.setBackground(DialogComponent.DEFAULT_BG);
                }
            });
            throw new InvalidSettingsException(
                    "Please select an item from the list.");
        }
        // we transfer the value from the field into the model
        ((SettingsModelEnumeration<T>)getModel()).setValue((T)m_combobox.getSelectedItem());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave()
            throws InvalidSettingsException {
        updateModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
            throws NotConfigurableException {
        // we are always good.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_combobox.setEnabled(enabled);
    }

    /**
     * Sets the preferred size of the internal component.
     *
     * @param width The width.
     * @param height The height.
     */
    public void setSizeComponents(final int width, final int height) {
        m_combobox.setPreferredSize(new Dimension(width, height));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_label.setToolTipText(text);
        m_combobox.setToolTipText(text);
    }

    /**
     * Replaces the list of selectable strings in the component. If
     * <code>select</code> is specified (not null) and it exists in the
     * collection it will be selected. If <code>select</code> is null, the
     * previous value will stay selected (if it exists in the new list).
     *
     * @param newItems new strings for the combo box
     * @param select the item to select after the replace. Can be null, in which
     *            case the previous selection remains - if it exists in the new
     *            list.
     */
    @SuppressWarnings("unchecked")
	public void replaceListItems(final Collection<T> newItems,
            final T select) {
        SettingsModelEnumeration<T> model = (SettingsModelEnumeration<T>)getModel();
        
        Collection<T> listEnums = newItems;        
        if ((listEnums == null) || (listEnums.size() == 0)) {
        	listEnums = Arrays.asList(model.getEnumerationType().getEnumConstants());
        }

        final T sel;
        if (select == null) {
            sel = model.getValue();
        } 
        else {
            sel = select;
        }

        m_combobox.removeAllItems();
        T selOption = null;
        for (final T enumValue : listEnums) {
            if (enumValue == null) {
                throw new NullPointerException("Options in the selection"
                        + " list can't be null");
            }
            m_combobox.addItem(enumValue);
            if (enumValue.equals(sel)) {
                selOption = enumValue;
            }
        }

        if (selOption == null) {
        	T defaultValue = model.getDefaultValue();
            m_combobox.setSelectedItem(defaultValue);
            if (m_combobox.getSelectedItem() != defaultValue) {
            	m_combobox.setSelectedIndex(0);
            }
        } else {
            m_combobox.setSelectedItem(selOption);
        }
        
        // Update the size of the comboBox and force the repainting
        // of the whole panel
        m_combobox.setSize(m_combobox.getPreferredSize());
        getComponentPanel().validate();
    }
}
