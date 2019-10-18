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


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;

/**
 * This component has similar functionality as the column filter panel. The difference is
 * that it takes enumeration items as the include/exclude elements rather than columns.
 *
 * @author Dillip K Mohanty
 * @author Manuel Schwarze
 */
public class DialogComponentEnumFilterPanel<T extends Enum<T>> extends DialogComponent {

	//
	// Constants
	//

	/** Add button id. */
	public static final int BUTTON_ADD = 1;

	/** Add All button id. */
	public static final int BUTTON_ADD_ALL = 2;

	/** Remove button id. */
	public static final int BUTTON_REMOVE = 4;

	/** Remove All button id. */
	public static final int BUTTON_REMOVE_ALL = 8;

	/** Line border for include list. */
	private static final Border INCLUDE_BORDER =
			BorderFactory.createLineBorder(new Color(0, 221, 0), 2);

	/** Line border for exclude list. */
	private static final Border EXCLUDE_BORDER =
			BorderFactory.createLineBorder(new Color(240, 0, 0), 2);

	//
	// Members
	//

	/** The main panel of this component. */
	private final JPanel m_panelMain;

	/** The header label of this component. Can be null. */
	private JLabel m_lbHeader;

	/** Include list. */
	private JList<T> m_listInclude;

	/** Include model. */
	private GenericListModel<T> m_listModelInclude;

	/** Exclude list. */
	private JList<T> m_listExclude;

	/** Exclude model. */
	private GenericListModel<T> m_listModelExclude;

	/** Highlight all search hits in the include model. */
	private JCheckBox m_cbMarkAllHitsInclude;

	/** Highlight all search hits in the exclude model. */
	private JCheckBox m_cbMarkAllHitsExclude;

	/** Remove all button. */
	private JButton m_btnRemoveAll;

	/** Remove button. */
	private JButton m_btnRemove;

	/** Add all button. */
	private JButton m_btnAddAll;

	/** Add button. */
	private JButton m_btnAdd;

	/** Search Field in include list. */
	private JTextField m_tfSearchInclude;

	/** Search Button for include list. */
	private JButton m_btnSearchInclude;

	/** Search Field in exclude list. */
	private JTextField m_tfSearchExclude;

	/** Search Button for exclude list. */
	private JButton m_btnSearchExclude;

	/** List of values to keep initial ordering. */
	private final LinkedHashSet<T> m_setOrderedValues =
			new LinkedHashSet<T>();

	/** Border of the include panel, keep it so we can change the title. */
	private TitledBorder m_borderIncl;

	/** Border of the exclude panel, keep it so we can change the title. */
	private TitledBorder m_borderExcl;

	/** Values to hide in both lists. */
	private final HashSet<T> m_setHiddenValues =
			new HashSet<T>();

	/** List of registered ChangeListeners. */
	private List<ChangeListener> m_listeners;

	/** Stores the list of enumerations that can be used in this filter component. */
	private List<T> m_listEnums;

	/** Determines, if we allow an empty selection (empty include list). */
	private final boolean m_bAllowEmptySelection;

	//
	// Constructor
	//

	/**
	 * Creates a new enumeration filter panel with three components which are the
	 * include list, button panel to shift elements between the two lists, and
	 * the exclude list. The include list then will contain all values to
	 * filter.
	 *
	 * @param enumArrayModel The model that stores the values for this component. Must not be null.
	 * @param label Label for dialog on to of the main component. Can be null.
	 * @param list List of items for the combobox. If no list or an empty list is provided all
	 * 		possible values will be taken from the Enumeration used in the settings model.
	 * @param bAllowEmptySelection Set to true to allow that the user selects nothing
	 * 		(include list is empty in that case).
	 */
	@SuppressWarnings("unchecked")
	public DialogComponentEnumFilterPanel(final SettingsModelEnumerationArray<T> enumArrayModel,
			final String label, final List<T> listItems, final boolean bAllowEmptySelection) {
		super(enumArrayModel);

		enumArrayModel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				setEnabledComponents(getModel().isEnabled());
			}
		});
		
		m_listEnums = listItems;

		if ((m_listEnums == null) || (m_listEnums.size() == 0)) {
			m_listEnums = Arrays.asList(((SettingsModelEnumerationArray<T>)getModel()).
					getEnumerationType().getEnumConstants());
		}

		m_bAllowEmptySelection = bAllowEmptySelection;

		// Create main components
		final JPanel buttonPanel = createButtonPanel();
		final JPanel includePanel = createIncludeList(enumArrayModel);
		final JPanel excludePanel = createExcludeList(enumArrayModel);

		// Layout main components
		m_panelMain = new JPanel(new GridBagLayout());

		int iRow = 0;

		// Add header label, if desired
		if (label != null && !label.isEmpty()) {
			m_lbHeader = new JLabel(label);
			LayoutUtils.constrain(m_panelMain, m_lbHeader, 0, iRow++, LayoutUtils.REMAINDER, 1,
					LayoutUtils.BOTH, LayoutUtils.NORTHWEST, 1.0d, 0.0d, 0, 5, 5, 5);
		}
		else {
			m_lbHeader = null;
		}

		// Add exclude and include lists as well as button panel
		LayoutUtils.constrain(m_panelMain, excludePanel, 0, iRow, 1, LayoutUtils.REMAINDER,
				LayoutUtils.BOTH, LayoutUtils.NORTHWEST, 0.5d, 1.0d, 0, 0, 0, 0);
		LayoutUtils.constrain(m_panelMain, buttonPanel, 1, iRow, 1, LayoutUtils.REMAINDER,
				LayoutUtils.VERTICAL, LayoutUtils.NORTH, 0.0d, 1.0d, 0, 5, 0, 5);
		LayoutUtils.constrain(m_panelMain, includePanel, 2, iRow++, LayoutUtils.REMAINDER, LayoutUtils.REMAINDER,
				LayoutUtils.BOTH, LayoutUtils.NORTHEAST, 0.5d, 1.0d, 0, 0, 0, 0);


		final JPanel panel = getComponentPanel();
		panel.setLayout(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
		panel.add(m_panelMain, BorderLayout.CENTER);
		panel.setPreferredSize(new Dimension(500,200));
		panel.setMinimumSize(new Dimension(400,200));

		updateComponent();
	}

	/**
	 * Enables or disables all components on this panel.
	 * {@inheritDoc}
	 */
	@Override
	protected void setEnabledComponents(final boolean enabled) {
		m_tfSearchInclude.setEnabled(enabled);
		m_btnSearchInclude.setEnabled(enabled);
		m_tfSearchExclude.setEnabled(enabled);
		m_btnSearchExclude.setEnabled(enabled);
		m_listInclude.setEnabled(enabled);
		m_listExclude.setEnabled(enabled);
		m_cbMarkAllHitsInclude.setEnabled(enabled);
		m_cbMarkAllHitsExclude.setEnabled(enabled);
		m_btnRemoveAll.setEnabled(enabled);
		m_btnRemove.setEnabled(enabled);
		m_btnAddAll.setEnabled(enabled);
		m_btnAdd.setEnabled(enabled);
	}

	/**
	 * Adds a listener which gets informed whenever the enumeration filtering
	 * changes.
	 * @param listener the listener
	 */
	public void addChangeListener(final ChangeListener listener) {
		if (m_listeners == null) {
			m_listeners = new ArrayList<ChangeListener>();
		}
		m_listeners.add(listener);
	}

	/**
	 * Removes the given listener from this enumeration filter panel.
	 * @param listener the listener.
	 */
	public void removeChangeListener(final ChangeListener listener) {
		if (m_listeners != null) {
			m_listeners.remove(listener);
		}
	}

	/**
	 * Removes all descriptor filter change listener.
	 */
	public void removeAllDescriptorFilterChangeListener() {
		if (m_listeners != null) {
			m_listeners.clear();
		}
	}

	//
	// Private Methods
	//

	/**
	 * Inform all registered listeners about a change in the set of
	 * filtered enumerations.
	 */
	private void fireFilteringChangedEvent() {
		if (m_listeners != null) {
			for (final ChangeListener listener : m_listeners) {
				listener.stateChanged(new ChangeEvent(this));
			}
		}
	}

	/**
	 * Called by the 'remove >>' button to exclude the selected elements from
	 * the include list.
	 */
	private void onRemIt() {
		// add all selected elements from the include to the exclude list
		final List<T> o = m_listInclude.getSelectedValuesList();
		final HashSet<T> hash = new HashSet<T>();
		hash.addAll(o);

		for (final Enumeration<T> e = m_listModelExclude.elements(); e.hasMoreElements();) {
			hash.add(e.nextElement());
		}

		boolean changed = false;

		for (int i = 0; i < o.size(); i++) {
			changed |= m_listModelInclude.removeElement(o.get(i));
		}

		m_listModelExclude.removeAllElements();

		for (final T item : m_setOrderedValues) {
			if (hash.contains(item)) {
				m_listModelExclude.addElement(item);
			}
		}

		if (changed) {
			fireFilteringChangedEvent();
		}
	}



	/**
	 * Called by the 'remove >>' button to exclude all elements from the include
	 * list.
	 */
	private void onRemAll() {
		final boolean changed = m_listModelInclude.elements().hasMoreElements();
		m_listModelInclude.removeAllElements();
		m_listModelExclude.removeAllElements();

		for (final T item : m_setOrderedValues) {
			if (!m_setHiddenValues.contains(item)) {
				m_listModelExclude.addElement(item);
			}
		}

		if (changed) {
			fireFilteringChangedEvent();
		}
	}

	/**
	 * Called by the '<< add' button to include the selected elements from the
	 * exclude list.
	 */
	private void onAddIt() {
		// add all selected elements from the exclude to the include list
		final List<T> o = m_listExclude.getSelectedValuesList();
		final HashSet<T> hash = new HashSet<T>();
		hash.addAll(o);

		for (final Enumeration<T> e = m_listModelInclude.elements(); e.hasMoreElements();) {
			hash.add(e.nextElement());
		}

		boolean changed = false;

		for (int i = 0; i < o.size(); i++) {
			changed |= m_listModelExclude.removeElement(o.get(i));
		}

		m_listModelInclude.removeAllElements();

		for (final T item : m_setOrderedValues) {
			if (hash.contains(item)) {
				m_listModelInclude.addElement(item);
			}
		}

		if (changed) {
			fireFilteringChangedEvent();
		}
	}

	/**
	 * Called by the '<< add all' button to include all elements from the
	 * exclude list.
	 */
	private void onAddAll() {
		final boolean changed = m_listModelExclude.elements().hasMoreElements();
		m_listModelInclude.removeAllElements();
		m_listModelExclude.removeAllElements();

		for (final T item : m_setOrderedValues) {
			if (!m_setHiddenValues.contains(item)) {
				m_listModelInclude.addElement(item);
			}
		}

		if (changed) {
			fireFilteringChangedEvent();
		}
	}

	/**
	 * Returns all enumerations from the exclude list.
	 *
	 * @return Set of all enumerations from the exclude list.
	 */
	public Set<T> getExcludedEnumerationsSet() {
		return getItemSet(m_listModelExclude);
	}

	/**
	 * Returns all enumerations from the include list.
	 *
	 * @return Set of all enumerations from the include list.
	 */
	public Set<T> getIncludedEnumerationsSet() {
		return getItemSet(m_listModelInclude);
	}

	/**
	 * Helper for the get***DescriptorList methods.
	 *
	 * @param model The list from which to retrieve the elements.
	 */
	private Set<T> getItemSet(final ListModel<T> model) {
		final Set<T> set = new LinkedHashSet<T>();

		for (int i = 0; i < model.getSize(); i++) {
			final T cell = model.getElementAt(i);
			set.add(cell);
		}

		return set;
	}

	/**
	 * This method is called when the user wants to search the given
	 * {@link JList} for the text of the given {@link JTextField}.
	 *
	 * @param list the list to search in
	 * @param model the list model on which the list is based on
	 * @param searchField the text field with the text to search for
	 * @param markAllHits if set to <code>true</code> the method will mark all
	 *            occurrences of the given search text in the given list. If set
	 *            to <code>false</code> the method will mark the next
	 *            occurrences of the search text after the current marked list
	 *            element.
	 */
	private void onSearch(final JList<T> list,
			final GenericListModel<T> model, final JTextField searchField,
			final boolean markAllHits) {

		if (list == null || model == null || searchField == null) {
			return;
		}

		final String searchStr = searchField.getText().trim();

		if (model.isEmpty() || searchStr.equals("")) {
			list.clearSelection();
			return;
		}

		if (markAllHits) {
			final int[] searchHits = getAllSearchHits(list, searchStr);
			list.clearSelection();
			if (searchHits.length > 0) {
				list.setSelectedIndices(searchHits);
				list.scrollRectToVisible(list.getCellBounds(searchHits[0],
						searchHits[0]));
			}
		}
		else {
			int start = Math.max(0, list.getSelectedIndex() + 1);
			if (start >= model.getSize()) {
				start = 0;
			}
			final int f = searchInList(list, searchStr, start);
			if (f >= 0) {
				list.scrollRectToVisible(list.getCellBounds(f, f));
				list.setSelectedIndex(f);
			}
		}
	}

	/**
	 * Finds in the list any occurrence of the argument string (as substring).
	 */
	private int searchInList(final JList<T> list, final String str,
			final int startIndex) {
		// this method was (slightly modified) copied from
		// JList#getNextMatch
		final ListModel<T> model = list.getModel();
		final int max = model.getSize();
		String prefix = str;

		if (prefix == null) {
			throw new IllegalArgumentException();
		}

		if (startIndex < 0 || startIndex >= max) {
			throw new IllegalArgumentException();
		}
		prefix = prefix.toUpperCase();

		int index = startIndex;
		do {
			final Object o = model.getElementAt(index);

			if (o != null) {

				String string;

				if (o instanceof String) {
					string = ((String)o).toUpperCase();
				}
				else {
					string = o.toString();
					if (string != null) {
						string = string.toUpperCase();
					}
				}

				if (string != null && string.indexOf(prefix) >= 0) {
					return index;
				}
			}
			index = (index + 1 + max) % max;
		}
		while (index != startIndex);
		return -1;
	}

	/**
	 * Uses the {@link #searchInList(JList, String, int)} method to get all
	 * occurrences of the given string in the given list and returns the index
	 * off all occurrences as a <code>int[]</code>.
	 *
	 * @see #searchInList(JList, String, int)
	 * @param list the list to search in
	 * @param str the string to search for
	 * @return <code>int[]</code> with the indices off all objects from the
	 *         given list which match the given string. If no hits exists the
	 *         method returns an empty <code>int[]</code>.
	 *
	 */
	private int[] getAllSearchHits(final JList<T> list, final String str) {
		int[] resultArray = new int[0];
		final ListModel<T> model = list.getModel();
		final int max = model.getSize();
		final ArrayList<Integer> hits = new ArrayList<Integer>(max);
		int index = 0;

		do {
			final int tempIndex = searchInList(list, str, index);
			// if the search returns no hit or returns a hit before the
			// current search position exit the while loop
			if (tempIndex < index || tempIndex < 0) {
				break;
			}
			index = tempIndex;
			hits.add(index);
			// increase the index to start the search from the next position
			// after the current hit
			index++;
		}
		while (index < max);

		if (hits.size() > 0) {
			resultArray = new int[hits.size()];

			for (int i = 0, length = hits.size(); i < length; i++) {
				resultArray[i] = hits.get(i).intValue();
			}
		}

		return resultArray;
	}

	/**
	 * Set the renderer that is used for both list in this panel.
	 *
	 * @param renderer the new renderer being used
	 * @see JList#setCellRenderer(javax.swing.ListCellRenderer)
	 */
	public final void setListCellRenderer(final ListCellRenderer<Object> renderer) {
		m_listInclude.setCellRenderer(renderer);
		m_listExclude.setCellRenderer(renderer);
	}

	/**
	 * Removes the specified enumeration items from either include or exclude list and
	 * notifies all listeners. Does not throw an exception if the argument
	 * contains <code>null</code> elements or is not contained in any of the
	 * lists.
	 *
	 * @param items The items to be hidden.
	 */
	@SafeVarargs
	public final void hideItems(final T... items) {
		boolean changed = false;

		for (final T item : items) {
			if (m_listModelInclude.contains(item)) {
				m_setHiddenValues.add(item);
				changed |= m_listModelInclude.removeElement(item);
			}
			else if (m_listModelExclude.contains(item)) {
				m_setHiddenValues.add(item);
				changed |= m_listModelExclude.removeElement(item);
			}
		}

		if (changed) {
			fireFilteringChangedEvent();
		}
	}

	/**
	 * Re-adds all hidden values back to the exclude list.
	 */
	public final void resetHiding() {
		if (!m_setHiddenValues.isEmpty()) {
			// Add all selected elements from the include to the exclude list
			final HashSet<Object> hash = new HashSet<Object>();
			hash.addAll(m_setHiddenValues);
			for (final Enumeration<?> e = m_listModelExclude.elements(); e.hasMoreElements();) {
				hash.add(e.nextElement());
			}

			m_listModelExclude.removeAllElements();
			for (final T item : m_setOrderedValues) {
				if (hash.contains(item)) {
					m_listModelExclude.addElement(item);
				}
			}

			m_setHiddenValues.clear();
		}
	}

	/**
	 * Sets the include list border color.
	 * 
	 * @param color Color. Set to null to use default.
	 */
	public final void setIncludeBorderColor(final Color color) {
		if (color == null) {
			m_borderIncl.setBorder(INCLUDE_BORDER);
		}
		else {
			m_borderIncl.setBorder(BorderFactory.createLineBorder(color, 2));
		}
	}

	/**
	 * Sets the exclude list border color.
	 * 
	 * @param color Color. Set to null to use default.
	 */
	public final void setExcludeBorderColor(final Color color) {
		if (color == null) {
			m_borderExcl.setBorder(EXCLUDE_BORDER);
		}
		else {
			m_borderExcl.setBorder(BorderFactory.createLineBorder(color, 2));
		}
	}

	/**
	 * Sets the visibility of search GUI components.
	 * 
	 * @param bVisible True to show search GUI components. False to hide them.
	 */
	public final void setSearchVisible(final boolean bVisible) {
		m_tfSearchInclude.setVisible(bVisible);
		m_tfSearchExclude.setVisible(bVisible);
		m_btnSearchInclude.setVisible(bVisible);
		m_btnSearchExclude.setVisible(bVisible);
		m_cbMarkAllHitsInclude.setVisible(bVisible);
		m_cbMarkAllHitsExclude.setVisible(bVisible);
	}

	/**
	 * Sets the visibility of buttons.
	 * 
	 * @param iButtonId One of the following constants: {@link #BUTTON_ADD},
	 * 		{@link #BUTTON_ADD_ALL}, {@link #BUTTON_REMOVE}, {@link #BUTTON_REMOVE_ALL}.
	 * @param bVisible True to show a button. False to hide it.
	 */
	public final void setButtonVisible(final int iButtonId, final boolean bVisible) {
		JButton btn = null;

		switch (iButtonId) {
		case BUTTON_ADD: btn = m_btnAdd; break;
		case BUTTON_ADD_ALL: btn = m_btnAddAll; break;
		case BUTTON_REMOVE: btn = m_btnRemove; break;
		case BUTTON_REMOVE_ALL: btn = m_btnRemoveAll; break;
		}

		if (btn != null) {
			btn.setVisible(bVisible);
		}
	}

	/**
	 * Sets the title of the include panel.
	 *
	 * @param title the new title
	 */
	public final void setIncludeTitle(final String title) {
		m_borderIncl.setTitle(title);
	}

	/**
	 * Sets the title of the exclude panel.
	 *
	 * @param title the new title
	 */
	public final void setExcludeTitle(final String title) {
		m_borderExcl.setTitle(title);
	}

	/**
	 * Setter for the original "Remove All" button.
	 *
	 * @param text the new button title
	 */
	public void setRemoveAllButtonText(final String text) {
		m_btnRemoveAll.setText(text);
	}

	/**
	 * Setter for the original "Add All" button.
	 *
	 * @param text the new button title
	 */
	public void setAddAllButtonText(final String text) {
		m_btnAddAll.setText(text);
	}

	/**
	 * Setter for the original "Remove" button.
	 *
	 * @param text the new button title
	 */
	public void setRemoveButtonText(final String text) {
		m_btnRemove.setText(text);
	}

	/**
	 * Setter for the original "Add" button.
	 *
	 * @param text the new button title
	 */
	public void setAddButtonText(final String text) {
		m_btnAdd.setText(text);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void updateComponent() {
		final SettingsModelEnumerationArray<T> model = ((SettingsModelEnumerationArray<T>)getModel());
		final T[] arrIncluded = model.getValues();
		final Set<T> setIncluded = new LinkedHashSet<T>();

		if (arrIncluded != null) {
			for (final T item : arrIncluded) {
				setIncluded.add(item);
			}
		}

		m_setOrderedValues.clear();
		m_listModelInclude.removeAllElements();
		m_listModelExclude.removeAllElements();

		for (int i = 0; i < m_listEnums.size(); i++) {
			final T item = m_listEnums.get(i);

			// Add only items that are not considered flow variable placeholders
			if (!model.isFlowVariablePlaceholder(item)) {
				m_setOrderedValues.add(item);

				if (!m_setHiddenValues.contains(item)) {
					if (setIncluded != null && setIncluded.contains(item)) {
						m_listModelInclude.addElement(item);
					}
					else {
						m_listModelExclude.addElement(item);
					}
				}
			}
		}

		setEnabledComponents(model.isEnabled());
		getComponentPanel().repaint();

		// Make sure the model is in sync (in case a model value isn't selected)
		final Set<T> setIncl = getIncludedEnumerationsSet();

		try {
			if ((setIncl == null && setIncluded != null)
					|| (setIncl != null && !setIncl.equals(setIncluded))) {
				// if the (initial) value in the model is not matching the list
				updateModel();
			}
		} catch (final InvalidSettingsException e) {
			// ignore invalid values here
		}

	}


	/**
	 * Transfers the current values from the component into the model.
	 *
	 * @throws InvalidSettingsException Thrown, if no item is selected.
	 */
	@SuppressWarnings("unchecked")
	private void updateModel() throws InvalidSettingsException {
		// Check, if empty selections are allowed
		if (!m_bAllowEmptySelection && m_listModelInclude.isEmpty()) {
			((SettingsModelEnumerationArray<T>)getModel()).setValues((T[])null);
			String strLabel = (m_lbHeader == null ? null : m_lbHeader.getText());
			if (strLabel != null) {
				strLabel = strLabel.trim();
				if (strLabel.endsWith(":")) {
					strLabel = "'" + strLabel.substring(0, strLabel.length() - 1) + "'";
				}
			}
			throw new InvalidSettingsException(
					"Please select a at least one item" + (strLabel == null ? "" : " of " + strLabel) + ".");
		}

		// We transfer the values from the include list into the model
		((SettingsModelEnumerationArray<T>)getModel()).setValues(m_listModelInclude.toArray());
	}


	/**
	 * [{@inheritDoc}
	 */
	@Override
	protected void validateSettingsBeforeSave() throws InvalidSettingsException {
		updateModel();
	}

	/**
	 * [{@inheritDoc}
	 */
	@Override
	protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
			throws NotConfigurableException {
		// We are always good.
	}

	/**
	 * [{@inheritDoc}
	 */
	@Override
	public void setToolTipText(final String text) {
		if (m_lbHeader != null) {
			m_lbHeader.setToolTipText(text);
		}

		m_panelMain.setToolTipText(text);
	}

	private JPanel createButtonPanel() {
		final JPanel buttonPanel = new JPanel(new GridBagLayout());
		buttonPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(" Select "),
				BorderFactory.createEmptyBorder(0, 5, 5, 5)));

		m_btnAdd = new JButton("Add >>");
		m_btnAdd.setMaximumSize(new Dimension(125, 25));
		m_btnAdd.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent ae) {
				onAddIt();
			}
		});

		m_btnAddAll = new JButton("Add All >>");
		m_btnAddAll.setMaximumSize(new Dimension(125, 25));
		m_btnAddAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent ae) {
				onAddAll();
			}
		});

		m_btnRemove = new JButton("<< Remove");
		m_btnRemove.setMaximumSize(new Dimension(125, 25));
		m_btnRemove.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent ae) {
				onRemIt();
			}
		});

		m_btnRemoveAll = new JButton("<< Remove All");
		m_btnRemoveAll.setMaximumSize(new Dimension(125, 25));
		m_btnRemoveAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent ae) {
				onRemAll();
			}
		});

		int iRow = 0;
		LayoutUtils.constrain(buttonPanel, new JPanel(), 0, iRow++, LayoutUtils.REMAINDER, 1,
				LayoutUtils.HORIZONTAL, LayoutUtils.NORTH, 1.0d, 1.0d, 0, 0, 0, 0);
		LayoutUtils.constrain(buttonPanel, m_btnAdd, 0, iRow++, LayoutUtils.REMAINDER, 1,
				LayoutUtils.HORIZONTAL, LayoutUtils.NORTH, 1.0d, 0.25d, 0, 0, 0, 0);
		LayoutUtils.constrain(buttonPanel, m_btnAddAll, 0, iRow++, LayoutUtils.REMAINDER, 1,
				LayoutUtils.HORIZONTAL, LayoutUtils.NORTH, 1.0d, 0.25d, 0, 0, 0, 0);
		LayoutUtils.constrain(buttonPanel, m_btnRemove, 0, iRow++, LayoutUtils.REMAINDER, 1,
				LayoutUtils.HORIZONTAL, LayoutUtils.NORTH, 1.0d, 0.25d, 0, 0, 0, 0);
		LayoutUtils.constrain(buttonPanel, m_btnRemoveAll, 0, iRow++, LayoutUtils.REMAINDER, LayoutUtils.REMAINDER,
				LayoutUtils.HORIZONTAL, LayoutUtils.NORTH, 1.0d, 0.25d, 0, 0, 0, 0);
		LayoutUtils.constrain(buttonPanel, new JPanel(), 0, iRow++, LayoutUtils.REMAINDER, 1,
				LayoutUtils.HORIZONTAL, LayoutUtils.NORTH, 1.0d, 1.0d, 0, 0, 0, 0);

		return buttonPanel;
	}

	private JPanel createExcludeList(final SettingsModelEnumerationArray<T> model) {
		// Create list model
		m_listModelExclude = new GenericListModel<T>(model.getEnumerationType());

		// Create list GUI
		m_listExclude = new JList<T>(m_listModelExclude);
		m_listExclude.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		m_listExclude.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent me) {
				if (me.getClickCount() == 2) {
					onAddIt();
					me.consume();
				}
			}
		});

		final JScrollPane scrollPane = new JScrollPane(m_listExclude);
		scrollPane.setMinimumSize(new Dimension(100, 100));
		scrollPane.setPreferredSize(new Dimension(300, 200));

		m_tfSearchExclude = new JTextField(8);
		m_btnSearchExclude = new JButton("Search");
		final ActionListener actionListenerSearchExclude = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				onSearch(m_listExclude, m_listModelExclude, m_tfSearchExclude,
						m_cbMarkAllHitsExclude.isSelected());
			}
		};

		m_tfSearchExclude.addActionListener(actionListenerSearchExclude);
		m_btnSearchExclude.addActionListener(actionListenerSearchExclude);

		m_cbMarkAllHitsExclude = new JCheckBox("Select all search hits");
		m_cbMarkAllHitsExclude.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				m_listExclude.clearSelection();
				onSearch(m_listExclude, m_listModelExclude, m_tfSearchExclude,
						m_cbMarkAllHitsExclude.isSelected());
			}
		});

		final JPanel excludePanel = new JPanel(new GridBagLayout());
		m_borderExcl = BorderFactory.createTitledBorder(EXCLUDE_BORDER, " Exclude ");
		excludePanel.setBorder(BorderFactory.createCompoundBorder(m_borderExcl,
				BorderFactory.createEmptyBorder(0, 5, 5, 5)));

		LayoutUtils.constrain(excludePanel, m_tfSearchExclude, 0, 0, 1, 1,
				LayoutUtils.HORIZONTAL, LayoutUtils.WEST, 1.0d, 0.0d, 0, 5, 0, 0);
		LayoutUtils.constrain(excludePanel, m_btnSearchExclude, 1, 0, LayoutUtils.REMAINDER, 1,
				LayoutUtils.NONE, LayoutUtils.WEST, 0.0d, 0.0d, 0, 5, 0, 0);
		LayoutUtils.constrain(excludePanel, m_cbMarkAllHitsExclude, 0, 1, LayoutUtils.REMAINDER, 1,
				LayoutUtils.NONE, LayoutUtils.NORTHWEST, 0.0d, 0.0d, 0, 0, 0, 0);
		LayoutUtils.constrain(excludePanel, scrollPane, 0, 2, LayoutUtils.REMAINDER, LayoutUtils.REMAINDER,
				LayoutUtils.BOTH, LayoutUtils.CENTER, 1.0d, 1.0d, 0, 0, 0, 0);

		return excludePanel;
	}


	private JPanel createIncludeList(final SettingsModelEnumerationArray<T> model) {
		// Create list model
		m_listModelInclude = new GenericListModel<T>(model.getEnumerationType());

		// Create list GUI
		m_listInclude = new JList<T>(m_listModelInclude);
		m_listInclude.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		m_listInclude.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent me) {
				if (me.getClickCount() == 2) {
					onRemIt();
					me.consume();
				}
			}
		});

		final JScrollPane scrollPane = new JScrollPane(m_listInclude);
		scrollPane.setMinimumSize(new Dimension(100, 100));
		scrollPane.setPreferredSize(new Dimension(300, 200));

		m_tfSearchInclude = new JTextField(8);
		m_btnSearchInclude = new JButton("Search");
		final ActionListener actionListenerSearchInclude = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				onSearch(m_listInclude, m_listModelInclude, m_tfSearchInclude,
						m_cbMarkAllHitsInclude.isSelected());
			}
		};
		m_tfSearchInclude.addActionListener(actionListenerSearchInclude);
		m_btnSearchInclude.addActionListener(actionListenerSearchInclude);

		m_cbMarkAllHitsInclude = new JCheckBox("Select all search hits");
		m_cbMarkAllHitsInclude.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				m_listInclude.clearSelection();
				onSearch(m_listInclude, m_listModelInclude, m_tfSearchInclude,
						m_cbMarkAllHitsInclude.isSelected());
			}
		});

		final JPanel includePanel = new JPanel(new GridBagLayout());
		m_borderIncl = BorderFactory.createTitledBorder(INCLUDE_BORDER, " Include ");
		includePanel.setBorder(BorderFactory.createCompoundBorder(m_borderIncl,
				BorderFactory.createEmptyBorder(0, 5, 5, 5)));

		LayoutUtils.constrain(includePanel, m_tfSearchInclude, 0, 0, 1, 1,
				LayoutUtils.HORIZONTAL, LayoutUtils.WEST, 1.0d, 0.0d, 0, 5, 0, 0);
		LayoutUtils.constrain(includePanel, m_btnSearchInclude, 1, 0, LayoutUtils.REMAINDER, 1,
				LayoutUtils.NONE, LayoutUtils.WEST, 0.0d, 0.0d, 0, 5, 0, 0);
		LayoutUtils.constrain(includePanel, m_cbMarkAllHitsInclude, 0, 1, LayoutUtils.REMAINDER, 1,
				LayoutUtils.NONE, LayoutUtils.NORTHWEST, 0.0d, 0.0d, 0, 0, 0, 0);
		LayoutUtils.constrain(includePanel, scrollPane, 0, 2, LayoutUtils.REMAINDER, LayoutUtils.REMAINDER,
				LayoutUtils.BOTH, LayoutUtils.CENTER, 1.0d, 1.0d, 0, 0, 0, 0);

		return includePanel;
	}
}
