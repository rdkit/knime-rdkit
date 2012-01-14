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


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
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

import org.knime.core.node.util.DataColumnSpecListCellRenderer;


/** 
 * Descriptor filter panel has similar functionality as column filter panel the difference being 
 * it takes available descriptors as the include/exclude elements rather than columns.
 * 
 * @author Dillip K Mohanty
 */
@SuppressWarnings("serial")
class DescriptorFilterPanel extends JPanel {
    

    /** Settings key for the excluded descriptors. */
    public static final String INCLUDED_DESCRIPTORS = "included_descriptors";

    /** Settings key for the excluded descriptors. */
    public static final String EXCLUDED_DESCRIPTORS = "excluded_descriptors";

    /** Include list. */
    private final JList m_inclList;

    /** Include model. */
    private final DefaultListModel m_inclMdl;

    /** Exclude list. */
    private final JList m_exclList;

    /** Exclude model. */
    private final DefaultListModel m_exclMdl;

    /** Highlight all search hits in the include model. */
    private final JCheckBox m_markAllHitsIncl;

    /** Highlight all search hits in the exclude model. */
    private final JCheckBox m_markAllHitsExcl;

    /** Remove all button. */
    private final JButton m_remAllButton;

    /** Remove button. */
    private final JButton m_remButton;

    /** Add all button. */
    private final JButton m_addAllButton;

    /** Add button. */
    private final JButton m_addButton;

    /** Search Field in include list. */
    private final JTextField m_searchFieldIncl;

    /** Search Button for include list. */
    private final JButton m_searchButtonIncl;

    /** Search Field in exclude list. */
    private final JTextField m_searchFieldExcl;

    /** Search Button for exclude list. */
    private final JButton m_searchButtonExcl;

    /** List of DataCellColumnSpecss to keep initial ordering of DataCells. */
    private final LinkedHashSet<String> m_order =
        new LinkedHashSet<String>();

    /** Border of the include panel, keep it so we can change the title. */
    private final TitledBorder m_includeBorder;

    /** Border of the include panel, keep it so we can change the title. */
    private final TitledBorder m_excludeBorder;

    private final HashSet<String> m_hideDescriptors =
        new HashSet<String>();

    private List<ChangeListener>m_listeners;

    /**
     * Line border for include descriptors.
     */
    private static final Border INCLUDE_BORDER =
        BorderFactory.createLineBorder(new Color(0, 221, 0), 2);

    /**
     * Line border for exclude descriptors.
     */
    private static final Border EXCLUDE_BORDER =
        BorderFactory.createLineBorder(new Color(240, 0, 0), 2);

    /**
     * Creates a new descriptor filter panel with three component which are the
     * include list, button panel to shift elements between the two lists, and
     * the exclude list. The include list then will contain all values to
     * filter.
     */
    public DescriptorFilterPanel() {
    	
        // keeps buttons such add 'add', 'add all', 'remove', and 'remove all'
        final JPanel buttonPan = new JPanel();
        buttonPan.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        buttonPan.setLayout(new BoxLayout(buttonPan, BoxLayout.Y_AXIS));
        buttonPan.add(Box.createVerticalStrut(20));

        m_addButton = new JButton("add >>");
        m_addButton.setMaximumSize(new Dimension(125, 25));
        buttonPan.add(m_addButton);
        m_addButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onAddIt();
            }
        });
        buttonPan.add(Box.createVerticalStrut(25));

        m_addAllButton = new JButton("add all >>");
        m_addAllButton.setMaximumSize(new Dimension(125, 25));
        buttonPan.add(m_addAllButton);
        m_addAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onAddAll();
            }
        });
        buttonPan.add(Box.createVerticalStrut(25));

        m_remButton = new JButton("<< remove");
        m_remButton.setMaximumSize(new Dimension(125, 25));
        buttonPan.add(m_remButton);
        m_remButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onRemIt();
            }
        });
        buttonPan.add(Box.createVerticalStrut(25));

        m_remAllButton = new JButton("<< remove all");
        m_remAllButton.setMaximumSize(new Dimension(125, 25));
        buttonPan.add(m_remAllButton);
        m_remAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onRemAll();
            }
        });
        buttonPan.add(Box.createVerticalStrut(20));
        buttonPan.add(Box.createGlue());

        // include list
        m_inclMdl = new DefaultListModel();
        m_inclList = new JList(m_inclMdl);
        m_inclList.setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        m_inclList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent me) {
                if (me.getClickCount() == 2) {
                    onRemIt();
                    me.consume();
                }
            }
        });
        final JScrollPane jspIncl = new JScrollPane(m_inclList);
        jspIncl.setMinimumSize(new Dimension(150, 155));

        m_searchFieldIncl = new JTextField(8);
        m_searchButtonIncl = new JButton("Search");
        ActionListener actionListenerIncl = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                onSearch(m_inclList, m_inclMdl, m_searchFieldIncl,
                        m_markAllHitsIncl.isSelected());
            }
        };
        m_searchFieldIncl.addActionListener(actionListenerIncl);
        m_searchButtonIncl.addActionListener(actionListenerIncl);
        JPanel inclSearchPanel = new JPanel(new BorderLayout());
        inclSearchPanel.add(new JLabel("Descriptor(s): "), BorderLayout.WEST);
        inclSearchPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15,
                15));
        inclSearchPanel.add(m_searchFieldIncl, BorderLayout.CENTER);
        inclSearchPanel.add(m_searchButtonIncl, BorderLayout.EAST);
        m_markAllHitsIncl = new JCheckBox("Select all search hits");
        ActionListener actionListenerAllIncl = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_inclList.clearSelection();
                onSearch(m_inclList, m_inclMdl, m_searchFieldIncl,
                        m_markAllHitsIncl.isSelected());
            }
        };
        m_markAllHitsIncl.addActionListener(actionListenerAllIncl);
        inclSearchPanel.add(m_markAllHitsIncl, BorderLayout.PAGE_END);
        JPanel includePanel = new JPanel(new BorderLayout());
        m_includeBorder = BorderFactory.createTitledBorder(
                INCLUDE_BORDER, " Include ");
        includePanel.setBorder(m_includeBorder);
        includePanel.add(inclSearchPanel, BorderLayout.NORTH);
        includePanel.add(jspIncl, BorderLayout.CENTER);

        // exclude list
        m_exclMdl = new DefaultListModel();
        m_exclList = new JList(m_exclMdl);
        m_exclList.setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        m_exclList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent me) {
                if (me.getClickCount() == 2) {
                    onAddIt();
                    me.consume();
                }
            }
        });
        setListCellRenderer(new DataColumnSpecListCellRenderer());
        final JScrollPane jspExcl = new JScrollPane(m_exclList);
        jspExcl.setMinimumSize(new Dimension(150, 155));

        m_searchFieldExcl = new JTextField(8);
        m_searchButtonExcl = new JButton("Search");
        ActionListener actionListenerExcl = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                onSearch(m_exclList, m_exclMdl, m_searchFieldExcl,
                        m_markAllHitsExcl.isSelected());
            }
        };
        m_searchFieldExcl.addActionListener(actionListenerExcl);
        m_searchButtonExcl.addActionListener(actionListenerExcl);
        JPanel exclSearchPanel = new JPanel(new BorderLayout());
        exclSearchPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15,
                15));
        exclSearchPanel.add(new JLabel("Descriptor(s): "), BorderLayout.WEST);
        exclSearchPanel.add(m_searchFieldExcl, BorderLayout.CENTER);
        exclSearchPanel.add(m_searchButtonExcl, BorderLayout.EAST);
        m_markAllHitsExcl = new JCheckBox("Select all search hits");
        ActionListener actionListenerAllExcl = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_exclList.clearSelection();
                onSearch(m_exclList, m_exclMdl, m_searchFieldExcl,
                        m_markAllHitsExcl.isSelected());
            }
        };
        m_markAllHitsExcl.addActionListener(actionListenerAllExcl);
        exclSearchPanel.add(m_markAllHitsExcl, BorderLayout.PAGE_END);
        JPanel excludePanel = new JPanel(new BorderLayout());
        m_excludeBorder = BorderFactory.createTitledBorder(
                EXCLUDE_BORDER, " Exclude ");
        excludePanel.setBorder(m_excludeBorder);
        excludePanel.add(exclSearchPanel, BorderLayout.NORTH);
        excludePanel.add(jspExcl, BorderLayout.CENTER);

        JPanel buttonPan2 = new JPanel(new GridLayout());
        Border border = BorderFactory.createTitledBorder(" Select ");
        buttonPan2.setBorder(border);
        buttonPan2.add(buttonPan);

        // adds include, button, exclude component
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));
        center.add(excludePanel);
        center.add(buttonPan2);
        center.add(includePanel);
        super.setLayout(new GridLayout(1, 1));
        super.add(center);
    }

    /**
     * Enables or disables all components on this panel.
     * {@inheritDoc}
     */
    @Override
    public void setEnabled(final boolean enabled) {
        m_searchFieldIncl.setEnabled(enabled);
        m_searchButtonIncl.setEnabled(enabled);
        m_searchFieldExcl.setEnabled(enabled);
        m_searchButtonExcl.setEnabled(enabled);
        m_inclList.setEnabled(enabled);
        m_exclList.setEnabled(enabled);
        m_markAllHitsIncl.setEnabled(enabled);
        m_markAllHitsExcl.setEnabled(enabled);
        m_remAllButton.setEnabled(enabled);
        m_remButton.setEnabled(enabled);
        m_addAllButton.setEnabled(enabled);
        m_addButton.setEnabled(enabled);
    }

    /**
     * Adds a listener which gets informed whenever the descriptor filtering
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
     * Removes the given listener from this descriptor filter panel.
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

    private void fireFilteringChangedEvent() {
        if (m_listeners != null) {
            for (ChangeListener listener : m_listeners) {
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
        Object[] o = m_inclList.getSelectedValues();
        HashSet<Object> hash = new HashSet<Object>();
        hash.addAll(Arrays.asList(o));
        for (Enumeration<?> e = m_exclMdl.elements(); e.hasMoreElements();) {
            hash.add(e.nextElement());
        }
        boolean changed = false;
        for (int i = 0; i < o.length; i++) {
            changed |= m_inclMdl.removeElement(o[i]);
        }
        m_exclMdl.removeAllElements();
        for (String c : m_order) {
            if (hash.contains(c)) {
                m_exclMdl.addElement(c);
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
        boolean changed = m_inclMdl.elements().hasMoreElements();
        m_inclMdl.removeAllElements();
        m_exclMdl.removeAllElements();
        for (String c : m_order) {
            if (!m_hideDescriptors.contains(c)) {
                m_exclMdl.addElement(c);
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
        Object[] o = m_exclList.getSelectedValues();
        HashSet<Object> hash = new HashSet<Object>();
        hash.addAll(Arrays.asList(o));
        for (Enumeration<?> e = m_inclMdl.elements(); e.hasMoreElements();) {
            hash.add(e.nextElement());
        }
        boolean changed = false;
        for (int i = 0; i < o.length; i++) {
            changed |= m_exclMdl.removeElement(o[i]);
        }
        m_inclMdl.removeAllElements();
        for (String c : m_order) {
            if (hash.contains(c)) {
                m_inclMdl.addElement(c);
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
        boolean changed = m_exclMdl.elements().hasMoreElements();
        m_inclMdl.removeAllElements();
        m_exclMdl.removeAllElements();
        for (String c : m_order) {
            if (!m_hideDescriptors.contains(c)) {
                m_inclMdl.addElement(c);
            }
        }
        if (changed) {
            fireFilteringChangedEvent();
        }
    }

    /**
     * Updates this filter panel by removing all current selections from the
     * include and exclude list. The include list will contains all descriptor names.
     * 
     * @param desc the list of descriptors
     * @param list the list of descriptors to exclude or include
     */
    public void update(final ArrayList<String> desc, 
            final Collection<String> list) {
        assert (desc != null && list != null);
        m_order.clear();
        m_inclMdl.removeAllElements();
        m_exclMdl.removeAllElements();
        m_hideDescriptors.clear();
        
        for (int i = 0; i < desc.size(); i++) {
            final String c = desc.get(i);
            m_order.add(c);
            if (list.contains(c)) {
                m_inclMdl.addElement(c);
            } else {
                m_exclMdl.addElement(c);
            }
        }
        repaint();
    }
    
    /**
     * Returns all descriptors from the exclude list.
     *
     * @return a set of all descriptors from the exclude list
     */
    public Set<String> getExcludedDescriptorSet() {
        return getDescList(m_exclMdl);
    }

    /**
     * Returns all descriptors from the include list.
     *
     * @return a list of all descriptors from the include list
     */
    public Set<String> getIncludedDescriptorSet() {
        return getDescList(m_inclMdl);
    }
    
    /**
     * Helper for the get***DescriptorList methods.
     *
     * @param model The list from which to retrieve the elements
     */
    private static Set<String> getDescList(final ListModel model) {
        final Set<String> list = new LinkedHashSet<String>();
        for (int i = 0; i < model.getSize(); i++) {
            String cell = (String)model.getElementAt(i);
            list.add(cell);
        }
        return list;
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
    private static void onSearch(final JList list,
            final DefaultListModel model, final JTextField searchField,
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
            int[] searchHits = getAllSearchHits(list, searchStr);
            list.clearSelection();
            if (searchHits.length > 0) {
                list.setSelectedIndices(searchHits);
                list.scrollRectToVisible(list.getCellBounds(searchHits[0],
                        searchHits[0]));
            }
        } else {
            int start = Math.max(0, list.getSelectedIndex() + 1);
            if (start >= model.getSize()) {
                start = 0;
            }
            int f = searchInList(list, searchStr, start);
            if (f >= 0) {
                list.scrollRectToVisible(list.getCellBounds(f, f));
                list.setSelectedIndex(f);
            }
        }
    }

    /*
     * Finds in the list any occurrence of the argument string (as substring).
     */
    private static int searchInList(final JList list, final String str,
            final int startIndex) {
        // this method was (slightly modified) copied from
        // JList#getNextMatch
        ListModel model = list.getModel();
        int max = model.getSize();
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
            Object o = model.getElementAt(index);

            if (o != null) {
                String string;
                if (o instanceof String) {
                    string = ((String)o).toUpperCase();
                } else {
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
        } while (index != startIndex);
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
    private static int[] getAllSearchHits(final JList list, final String str) {

        ListModel model = list.getModel();
        int max = model.getSize();
        final ArrayList<Integer> hits = new ArrayList<Integer>(max);
        int index = 0;
        do {
            int tempIndex = searchInList(list, str, index);
            // if the search returns no hit or returns a hit before the
            // current search position exit the while loop
            if (tempIndex < index || tempIndex < 0) {
                break;
            }
            index = tempIndex;
            hits.add(new Integer(index));
            // increase the index to start the search from the next position
            // after the current hit
            index++;
        } while (index < max);

        if (hits.size() > 0) {
            final int[] resultArray = new int[hits.size()];
            for (int i = 0, length = hits.size(); i < length; i++) {
                resultArray[i] = hits.get(i).intValue();
            }
            return resultArray;
        }
        return new int[0];
    }

    /**
     * Set the renderer that is used for both list in this panel.
     *
     * @param renderer the new renderer being used
     * @see JList#setCellRenderer(javax.swing.ListCellRenderer)
     */
    protected final void setListCellRenderer(final ListCellRenderer renderer) {
        m_inclList.setCellRenderer(renderer);
        m_exclList.setCellRenderer(renderer);
    }

    /**
     * Removes the given descriptors form either include or exclude list and
     * notifies all listeners. Does not throw an exception if the argument
     * contains <code>null</code> elements or is not contained in any of the
     * lists.
     *
     * @param descriptors the descriptors to remove
     */
    public final void hideDescriptors(final String... descriptors) {
        boolean changed = false;
        for (String descriptor : descriptors) {
            if (m_inclMdl.contains(descriptor)) {
            	m_hideDescriptors.add(descriptor);
                changed |= m_inclMdl.removeElement(descriptor);
            } else if (m_exclMdl.contains(descriptor)) {
            	m_hideDescriptors.add(descriptor);
                changed |= m_exclMdl.removeElement(descriptor);
            }
        }
        if (changed) {
            fireFilteringChangedEvent();
        }
    }

    /**
     * Re-adds all remove/hidden descriptor to the exclude list.
     */
    public final void resetHiding() {
        if (m_hideDescriptors.isEmpty()) {
            return;
        }
        // add all selected elements from the include to the exclude list
        HashSet<Object> hash = new HashSet<Object>();
        hash.addAll(m_hideDescriptors);
        for (Enumeration<?> e = m_exclMdl.elements(); e.hasMoreElements();) {
            hash.add(e.nextElement());
        }
        m_exclMdl.removeAllElements();
        for (String c : m_order) {
            if (hash.contains(c)) {
                m_exclMdl.addElement(c);
            }
        }
        m_hideDescriptors.clear();
    }

    /**
     * Sets the title of the include panel.
     *
     * @param title the new title
     */
    public final void setIncludeTitle(final String title) {
        m_includeBorder.setTitle(title);
    }

    /**
     * Sets the title of the exclude panel.
     *
     * @param title the new title
     */
    public final void setExcludeTitle(final String title) {
        m_excludeBorder.setTitle(title);
    }

    /**
     * Setter for the original "Remove All" button.
     *
     * @param text the new button title
     */
    public void setRemoveAllButtonText(final String text) {
        m_remAllButton.setText(text);
    }

    /**
     * Setter for the original "Add All" button.
     *
     * @param text the new button title
     */
    public void setAddAllButtonText(final String text) {
        m_addAllButton.setText(text);
    }

    /**
     * Setter for the original "Remove" button.
     *
     * @param text the new button title
     */
    public void setRemoveButtonText(final String text) {
        m_remButton.setText(text);
    }

    /**
     * Setter for the original "Add" button.
     *
     * @param text the new button title
     */
    public void setAddButtonText(final String text) {
        m_addButton.setText(text);
    }
}
