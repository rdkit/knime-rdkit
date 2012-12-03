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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ButtonGroupEnumInterface;


/**
 * A standard component to display radio buttons. The given
 * {@link SettingsModelEnumeration} holds the value of the
 * <code>getActionCommand()</code> of the selected
 * {@link ButtonGroupEnumInterface}.
 * 
 * @author Manuel Schwarze, based on work of Tobias Koetter
 * @author Tobias Koetter, University of Konstanz
 */
public class DialogComponentEnumButtonGroup<T extends Enum<T>> extends DialogComponent {

	//
	// Members
	//
	
    private final ButtonGroup m_buttonGroup;

    //
    // Constructor
    //
    

    /**
     * Constructor for class DialogComponentEnumButtonGroup. The given
     * <code>SettingsModel</code> holds the selected element. The default
     * value of the <code>SettingModel</code> is selected per default.
     * 
     * @param enumModel The model that stores the enumeration valze of the
     * 		selected radio button.
     * @param vertical Set to <code>true</code> to have the box in a vertical
     * 		orientation.
     * @param label The optional label of the group. Set to <code>null</code>
     * 		for none label. Set an empty <code>String</code> for a border.
     * @param elements The shown enumeration values of the buttons. Labels will
     * 		be taken from the toString() result of the values. If no element
     * 		is passed, all enumeration values will be shown.
     */
    public DialogComponentEnumButtonGroup(final SettingsModelEnumeration<T> enumModel,
            final boolean vertical, final String label,
            final T... elements) {
        this(enumModel, vertical, label, enumModel.getDefaultValue(), elements);
    }
    
    /**
     * Constructor for class DialogComponentEnumButtonGroup. The given
     * <code>SettingsModel</code> holds the selected element. The default
     * value of the <code>SettingModel</code> is selected per default.
     * 
     * @param enumModel The model that stores the enumeration valze of the
     * 		selected radio button.
     * @param vertical Set to <code>true</code> to have the box in a vertical
     * 		orientation.
     * @param label The optional label of the group. Set to <code>null</code>
     * 		for none label. Set an empty <code>String</code> for a border.
     * @param defaultElement The default element which should be selected.
     * 		Can be null to take it directly from settings model.
     * @param elements The shown enumeration values of the buttons. Labels will
     * 		be taken from the toString() result of the values. If no element
     * 		is passed, all enumeration values will be shown.
     */
    public DialogComponentEnumButtonGroup(final SettingsModelEnumeration<T> enumModel,
    		final boolean vertical, final String label, final T defaultElement,  
    		final T... elements) {
        super(enumModel);
        
        T[] arrValues = elements;
        T defaultValue = (defaultElement == null ? enumModel.getDefaultValue() : defaultElement);
        
        // Use all enumeration values, if no specific elements are passed in 
        if (arrValues == null || arrValues.length < 1) {
        	arrValues = enumModel.getEnumerationType().getEnumConstants();	
        }
         
        m_buttonGroup = createButtonGroup(arrValues, defaultValue, new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                updateModel();
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
        
        final Box buttonBox = createButtonGroupBox(m_buttonGroup, label, vertical);
        getComponentPanel().add(buttonBox);
        
        updateComponent();
    }
    
    //
    // Public Methods
    //
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
    	setToolTipText(null, text);
    }

    /**
     * {@inheritDoc}
     */
    public void setToolTipText(final T element, final String text) {
        final Enumeration<AbstractButton> buttons = m_buttonGroup.getElements();
        while (buttons.hasMoreElements()) {
            AbstractButton button = buttons.nextElement();
            if (element == null || element.name().equals(button.getActionCommand())) {
            	button.setToolTipText(text);
            }
        }
    }

    //
    // Protected Methods
    //
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(
            final PortObjectSpec[] specs) {
        // Nothing to check
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        final Enumeration<AbstractButton> buttons = m_buttonGroup.getElements();
        while (buttons.hasMoreElements()) {
            buttons.nextElement().setEnabled(enabled);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() {
        updateModel();
    }
    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
	@Override
    protected void updateComponent() {
    	T value = ((SettingsModelEnumeration<T>)getModel()).getValue();
    	if (value == null) {
    		value = ((SettingsModelEnumeration<T>)getModel()).getDefaultValue();
    	}
    	
        final String val = value.name();
        final ButtonModel selectedButton = m_buttonGroup.getSelection();
        final String actionCommand;
        
        if (selectedButton != null) {
            actionCommand = selectedButton.getActionCommand();
        } 
        else {
            actionCommand = null;
        }
        
        boolean update;
        update = !val.equals(actionCommand);

        if (update) {
            final Enumeration<AbstractButton> buttons =
                m_buttonGroup.getElements();
            while (buttons.hasMoreElements()) {
                final AbstractButton button = buttons.nextElement();
                if (val.equals(button.getActionCommand())) {
                    button.setSelected(true);
                }
            }
        }
        
        // Also update the enable status
        setEnabledComponents(getModel().isEnabled());
    }    

    /**
     * Transfers the current value from the component into the model.
     */
    @SuppressWarnings("unchecked")
	protected void updateModel() {
        // We transfer the value from the button group into the model
        final ButtonModel selectedButton = m_buttonGroup.getSelection();
        final String actionCommand;
        
        if (selectedButton != null) {
            actionCommand = selectedButton.getActionCommand();
        } 
        else {
            actionCommand = null;
        }
        
        ((SettingsModelEnumeration<T>)getModel()).setValueAsString(actionCommand);
    }    
    
    /**
     * Creates a <code>ButtonGroup</code> with the labels elements as button
     * labels and the actionCommands as the button action commands.
     * 
     * @param values The enumeration values to be shown. Labels are taken
     * 		from toString() results. Action commands are taken from name(). 
     * 		results. Must not be null.
     * @param default The default value which should be selected. Must not be null.
     * @param l The action listener to add to each button. Must not be null.
     * 
     * @return The button group.
     */
    private ButtonGroup createButtonGroup(final T[] values, final T defaultValue,
            final ActionListener l) {
    	
        final ButtonGroup group = new ButtonGroup();
        boolean defaultFound = false;
        
        for (int i = 0; i < values.length; i++) {
        	// Create radio button with label and action command
            final JRadioButton button = new JRadioButton(values[i].toString());
            button.setActionCommand(values[i].name());
            
            // Select the default
            if (defaultFound == false && defaultValue == values[i]) {
                button.setSelected(true);
                defaultFound = true;
            }
            
            // Add action listener
            if (l != null) {
                button.addActionListener(l);
            }
            
            group.add(button);
        }
        
        if (!defaultFound && group.getButtonCount() > 0) {
            // Select the first button if none is by default selected
            group.getElements().nextElement().setSelected(true);
        }
        
        return group;
    }


    /**
     * Creates a <code>Box</code> with the buttons of the given
     * <code>ButtonGroup</code>. Surrounded by a border if the label is
     * not null.
     * 
     * @param group The <code>ButtonGroup</code> to create the box with.
     * @param label The optional label of the group Set to <code>null</code>
     * 		for none label.
     * @param vertical Set to <code>true</code> to have the box in a vertical
     * 		orientation.
     * 
     * @return A <code>Box</code> with all buttons of the given
     * 		<code>ButtonGroup</code>
     */
    private Box createButtonGroupBox(final ButtonGroup group,
              final String label, final boolean vertical) {
          Box buttonBox = null;
          
          // Align buttons
          if (vertical) {
              buttonBox = Box.createVerticalBox();
              buttonBox.add(Box.createVerticalGlue());
          } 
          else {
              buttonBox = Box.createHorizontalBox();
              buttonBox.add(Box.createHorizontalGlue());
          }
          
          final Dimension titleSize;
          
          // Surround with border and label
          if (label != null) {
              final TitledBorder borderTitle =
                  BorderFactory.createTitledBorder(
                      BorderFactory.createEtchedBorder(), label);
              titleSize = borderTitle.getMinimumSize(buttonBox);
              buttonBox.setBorder(borderTitle);
          } 
          else {
              titleSize = null;
          }
          
          // Add buttons
          for (final Enumeration<AbstractButton> buttons = group.getElements();
              buttons.hasMoreElements(); ) {
              final AbstractButton button = buttons.nextElement();
              buttonBox.add(button);
              
              if (vertical) {
                  buttonBox.add(Box.createVerticalGlue());
              } 
              else {
                  buttonBox.add(Box.createHorizontalGlue());
              }
          }
          
          final Dimension preferredSize = buttonBox.getPreferredSize();
          
          // Set preferred and minimum size
          if (titleSize != null && titleSize.getWidth() > preferredSize.getWidth()) {
              // Add 5 pixels to have a little space at the end
              preferredSize.setSize(titleSize.getWidth() + 5,
                      preferredSize.getHeight());
          }

          buttonBox.setPreferredSize(preferredSize);
          buttonBox.setMinimumSize(preferredSize);
          
          return buttonBox;
      }    
}
