/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2010-2023
 * Novartis Pharma AG, Switzerland
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
import java.awt.Container;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * A multi-selection list combining a list, a scrollbar and two
 * buttons to select all or none. With a "small GUI" option it
 * is possible to reduce the list to a one-line non-editable
 * text field that shows only the selected values. When clicking
 * in the text field the list opens as a modal popup and allows the user
 * to select the values.
 * 
 * @author Manuel Schwarze
 */
public class MultiSelectionListPanel extends JPanel {

	//
	// Enum
	//

	/** Defines sorting directions. */
	public enum SortDirection {
		None, Ascending, Descending
	}

	//
	// Constants
	//

	/** Serial number. */
	private static final long serialVersionUID = 3134789802107394822L;

	//
	// Members
	//

	/** Last applied sorting direction. */
	private SortDirection m_sorting;

	/** The list GUI element. */
	private final JList<String> m_list;

	/** The main panel that includes GUI elements. */
	private final JPanel m_panelList;

	/** In case of small GUI element, this is the field with currently selected choices. */
	private final JTextField m_tfSmallGuiChoices;

	/** A popup dialog that hosts the list in case of small GUI element option. */
	private JDialog m_dlgPopup;

	//
	// Constructors
	//

	/**
	 * Creates a new multi selection list panel for the specified
	 * elements. The list will have the visible height (rows) specified
	 * in the second parameter. If the third parameter is set the
	 * list will be hidden. Instead a text field with current selections
	 * is shown, which can be clicked to popup the list to make
	 * selections.
	 * 
	 * @param arrItems Items to be added to the list.
	 * @param iVisibleRowCount Number of visible rows in the list GUI element.
	 * @param bSmallGui Set to true to show only a text field with selections
	 * 		that needs to be clicked to open a popup list.
	 */
	public MultiSelectionListPanel(final String[] arrItems,
			final int iVisibleRowCount, final boolean bSmallGui) {
		this(createDefaultListModel(arrItems), iVisibleRowCount, bSmallGui);
	}

	/**
	 * Creates a new multi selection list panel for the specified
	 * elements. The list will have the visible height (rows) specified
	 * in the second parameter. If the third parameter is set the
	 * list will be hidden. Instead a text field with current selections
	 * is shown, which can be clicked to popup the list to make
	 * selections.
	 * 
	 * @param model Model for the list. Must not be null.
	 * @param iVisibleRowCount Number of visible rows in the list GUI element.
	 * @param bSmallGui Set to true to show only a text field with selections
	 * 		that needs to be clicked to open a popup list.
	 */
	public MultiSelectionListPanel(final DefaultListModel<String> model,
			final int iVisibleRowCount, final boolean bSmallGui) {
		m_dlgPopup = null;
		m_list = new JList<String>(model);
		m_list.setVisibleRowCount(iVisibleRowCount);
		m_list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		final JScrollPane scrollPane = new JScrollPane(m_list,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		final JButton btnSelectAll = new JButton("All");
		final JButton btnSelectNone = new JButton("None");

		btnSelectAll.setFont(new Font("Helvetica", Font.BOLD, 9));
		btnSelectNone.setFont(new Font("Helvetica", Font.BOLD, 9));

		btnSelectAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final ListModel<String> model = getModel();
				if (model != null && model.getSize() > 0) {
					m_list.setSelectionInterval(0, model.getSize() - 1);
				}
			}
		});

		btnSelectNone.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final ListModel<String> model = getModel();
				if (model != null && model.getSize() > 0) {
					m_list.removeSelectionInterval(0, model.getSize() - 1);
				}
			}
		});

		m_panelList = new JPanel(new GridBagLayout());
		LayoutUtils.constrain(m_panelList, scrollPane, 0, 0, LayoutUtils.REMAINDER, 1,
				LayoutUtils.BOTH, LayoutUtils.NORTHWEST, 1.0, 1.0, 0, 0, 0, 0);
		LayoutUtils.constrain(m_panelList, new JPanel(), 0, 1, 1, LayoutUtils.REMAINDER,
				LayoutUtils.HORIZONTAL, LayoutUtils.SOUTHEAST, 1.0, 0.0, 3, 0, 0, 0);
		LayoutUtils.constrain(m_panelList, btnSelectAll, 1, 1, 1, LayoutUtils.REMAINDER,
				LayoutUtils.NONE, LayoutUtils.SOUTHEAST, 0.0, 0.0, 3, 3, 0, 0);
		LayoutUtils.constrain(m_panelList, btnSelectNone, 2, 1, LayoutUtils.REMAINDER, LayoutUtils.REMAINDER,
				LayoutUtils.NONE, LayoutUtils.SOUTHEAST, 0.0, 0.0, 3, 3, 0, 3);

		setLayout(new BorderLayout());

		if (bSmallGui) {
			m_tfSmallGuiChoices = new JTextField();
			m_tfSmallGuiChoices.setEditable(false);
			m_tfSmallGuiChoices.setBackground(Color.white);
			m_tfSmallGuiChoices.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			m_tfSmallGuiChoices.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(final MouseEvent e) {
					if (m_dlgPopup == null) {
						final JOptionPane pane = new JOptionPane(m_panelList,
								JOptionPane.PLAIN_MESSAGE, JOptionPane.CLOSED_OPTION);
						m_dlgPopup = pane.createDialog(m_tfSmallGuiChoices, "Select Options");
						m_dlgPopup.setResizable(true);
						m_dlgPopup.setModal(true);
						m_dlgPopup.pack();
					}
					m_dlgPopup.setVisible(true); // Blocks
					updateSmallGui();
				}
			});
			add(m_tfSmallGuiChoices, BorderLayout.CENTER);
		}
		else {
			m_tfSmallGuiChoices = null;
			add(m_panelList, BorderLayout.CENTER);
		}

		m_panelList.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEtchedBorder(),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));
	}

	//
	// Public Methods
	//

	/**
	 * Returns the last applied sorting direction.
	 * 
	 * @return Sorting direction.
	 */
	public SortDirection getSorting() {
		return m_sorting;
	}

	/**
	 * Applies a new sorting direction based on the last one.
	 * It basically toggles between ascending and descending.
	 */
	public void sort() {
		if (m_sorting == SortDirection.Ascending) {
			setSorting(SortDirection.Descending);
		}
		else {
			setSorting(SortDirection.Ascending);
		}
	}

	/**
	 * Sets the new sorting direction based on the specified
	 * parameter.
	 * 
	 * @param sorting Sorting direction to be applied.
	 */
	public void setSorting(final SortDirection sorting) {
		final String[] arrValues = getValues();
		if (sorting != SortDirection.None &&
				arrValues != null && arrValues.length > 0) {
			try {
				Arrays.sort(arrValues);
				if (sorting == SortDirection.Descending) {
					final int iLen = arrValues.length;
					final int iLenHalf = iLen / 2;
					String tmp;
					for (int i = 0; i < iLenHalf; i++) {
						tmp = arrValues[i];
						arrValues[i] = arrValues[iLen - 1 - i];
						arrValues[iLen - 1 - i] = tmp;
					}
				}
				setValues(arrValues);
				m_sorting = sorting;
			}
			catch (final ClassCastException excNotComparable) {
				m_sorting = SortDirection.None;
			}
		}
		else {
			m_sorting = sorting;
		}
	}

	/**
	 * Returns the current list model.
	 * 
	 * @return List model.
	 */
	public DefaultListModel<String> getModel() {
		synchronized (m_list) {
			return (DefaultListModel<String>)m_list.getModel();
		}
	}

	/**
	 * Sets the values for the list. The sorting direction is
	 * set back to None.
	 * 
	 * @param arrItems Items to be shown in the list. Can be null.
	 */
	public void setValues(final String[] arrItems) {
		synchronized (m_list) {
			final List<String> listSelections = m_list.getSelectedValuesList();
			final int iFirstVisibleIndex = m_list.getFirstVisibleIndex();
			final DefaultListModel<String> model = getModel();
			model.removeAllElements();
			if (arrItems != null) {
				for (final String obj : arrItems) {
					model.addElement(obj);
				}
				if (listSelections != null) {
					selectValues(listSelections.toArray(new String[listSelections.size()]));
				}
				m_list.ensureIndexIsVisible(iFirstVisibleIndex);
			}
			m_sorting = SortDirection.None;
			updateSmallGui();
		}
	}

	/**
	 * Sets the values for the list based on a passed in string
	 * that contains all values separated with the specified separator
	 * chars (can be more than one). The sorting direction is set back to None.
	 * 
	 * @param strMultiLineText Values as string. Can be null.
	 * @param strSeparators Separators used for splitting up the values.
	 * 		Can be null to used \n and , as default separators.
	 */
	public void setMultiLineTextValues(final String strMultiLineText,
			final String strSeparators) {
		String[] arrItems = null;
		if (strMultiLineText != null) {
			final StringTokenizer st = new StringTokenizer(strMultiLineText,
					strSeparators == null ? "\n," : strSeparators, false);
			arrItems = new String[st.countTokens()];
			for (int i = 0; st.hasMoreTokens(); i++) {
				arrItems[i] = st.nextToken();
			}
		}
		setValues(arrItems);
	}

	/**
	 * Select the specified values in the list.
	 * 
	 * @param arrSelections Selections to be applied. Not-contained
	 * 		items will be not added to the list. They are just ignored.
	 * 		Can be null to unselect everything.
	 */
	public void selectValues(final String[] arrSelections) {
		final HashSet<Object> setIncludes = new HashSet<Object>();
		if (arrSelections != null) {
			for (final Object s : arrSelections) {
				if (s != null) {
					setIncludes.add(s);
				}
			}
		}

		// build index list of all selected values
		synchronized (m_list) {
			final DefaultListModel<String> model = getModel();
			final int iLen = model.getSize();
			int iCount = 0;
			final int[] arrSelectedIndex = new int[iLen];
			for (int i = 0; i < iLen; i++) {
				if (setIncludes.contains(model.get(i))) {
					arrSelectedIndex[iCount++] = i;
				}
			}
			m_list.setSelectedIndices(Arrays.copyOf(arrSelectedIndex, iCount));
			updateSmallGui();
		}
	}

	/**
	 * Determines from the list all current values and returns them
	 * as array.
	 * 
	 * @return List values.
	 */
	public String[] getValues() {
		synchronized (m_list) {
			final DefaultListModel<String> model = getModel();
			final int iLen = model.getSize();
			final String[] arrItems = new String[iLen];
			for (int i = 0; i < iLen; i++) {
				arrItems[i] = model.get(i);
			}
			return arrItems;
		}
	}

	/**
	 * Determines from the list all current values and returns them
	 * as one string that is separated by the specified separator.
	 * 
	 * @param strSeparator Seperator to be used to concatenate the values.
	 * 		Can be null to use , as default.
	 * 
	 * @return List items as concatenated string.
	 */
	public String getMultiLineTextValues(final String strSeparator) {
		return createString(getValues(), strSeparator == null ? "," : strSeparator);
	}

	/**
	 * Determines from the list all selected values and returns them
	 * as array.
	 * 
	 * @return Selected values.
	 */
	public String[] getSelections() {
		synchronized (m_list) {
			final List<String> listSelections = m_list.getSelectedValuesList();
			return listSelections.toArray(new String[listSelections.size()]);
		}
	}

	//
	// Protected Methods
	//

	/**
	 * Updates the small text field GUI component, if it exists, with
	 * the selections done in the popup list or by setting them.
	 */
	protected void updateSmallGui() {
		if (m_tfSmallGuiChoices != null) {
			m_tfSmallGuiChoices.setText(createString(getSelections(), ", "));
		}
	}

	/**
	 * Returns the specified items as one string that is
	 * separated by the specified separator.
	 * 
	 * @param arrItems Items to be concatenated. Can be null.
	 * @param strSeparator Separator to be used to concatenate the values.
	 * 		Must not be null.
	 * 
	 * @return Concatenated string.
	 */
	protected String createString(final String[] arrItems, final String strSeparator) {
		final StringBuilder sb = new StringBuilder();
		if (arrItems != null && arrItems.length > 0) {
			boolean bNotFirst = false;
			for (int i = 0; i < arrItems.length; i++) {
				if (bNotFirst) {
					sb.append(strSeparator);
				}
				else {
					bNotFirst = true;
				}
				sb.append(arrItems[i].toString());
			}
		}
		return sb.toString();
	}

	//
	// Static Methods
	//

	/**
	 * Creates a default list model filled with the specified items.
	 * 
	 * @param arrItems Items to be added to the list model. Can be null.
	 * 
	 * @return Default list model containing the specified items. Never null.
	 */
	public static DefaultListModel<String> createDefaultListModel(final String[] arrItems) {
		final DefaultListModel<String> model = new DefaultListModel<String>();

		if (arrItems != null) {
			for (final String o : arrItems) {
				model.addElement(o);
			}
		}

		return model;
	}

	/**
	 * Just a test method to demonstrate how to use this list panel.
	 * 
	 * @param argv Not used.
	 */
	public static void main(final String[] argv) {
		final JFrame frame = new JFrame("Test Multi Selection List");
		frame.setBounds(100, 100, 640, 400);
		final Container cont = frame.getContentPane();
		cont.setLayout(new BorderLayout());
		final MultiSelectionListPanel listPanel = new MultiSelectionListPanel(
				new String[] { "A", "B", "C" }, 3, true);
		final JTextArea textArea = new JTextArea(listPanel.getMultiLineTextValues("\n"));
		listPanel.getModel().addListDataListener(new ListDataListener() {
			private void changed() {
				textArea.setText(listPanel.getMultiLineTextValues("\n"));
			}

			@Override
			public void intervalRemoved(final ListDataEvent e) {
				changed();
			}

			@Override
			public void intervalAdded(final ListDataEvent e) {
				changed();
			}

			@Override
			public void contentsChanged(final ListDataEvent e) {
				changed();
			}
		});
		final JButton btnSet = new JButton("Set Values for List");
		btnSet.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				listPanel.setMultiLineTextValues(textArea.getText(), "\n");
			}
		});
		final JButton btnSort = new JButton("Sort");
		btnSort.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				listPanel.sort();
			}
		});
		final JPanel panelButtons = new JPanel();
		panelButtons.setLayout(new FlowLayout());
		panelButtons.add(btnSet);
		panelButtons.add(btnSort);

		cont.add(listPanel, BorderLayout.NORTH);
		cont.add(textArea, BorderLayout.CENTER);
		cont.add(panelButtons, BorderLayout.SOUTH);

		frame.setVisible(true);
	}
}
