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
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

/**
 * A table cell editor for colors.
 * 
 * @author Manuel Schwarze
 */
public class TableColorCellEditor extends AbstractCellEditor implements TableCellEditor,
ActionListener {

	//
	// Constants
	//

	/** The serial number. */
	private static final long serialVersionUID = 7732160891831685256L;

	/** The highlighting color chooser to be used and reused. */
	public static final JColorChooser COLOR_CHOOSER = new JColorChooser();

	/** The text for the default color button. */
	public static final String DEFAULT_COLOR_BUTTON = "Use Default Color";

	//
	// Members
	//

	private Color m_currentColor;

	private final JButton m_button;
	private final Component m_parent;
	private final String m_strDialogTitle;
	private final Color m_colDefault;
	private final boolean m_bAllowDefaultSelection;

	protected static final String EDIT = "Edit";

	//
	// Constructor
	//

	/**
	 * Creates a new table color cell editor.
	 * 
	 * @param parentComponent The component that shall be used as parent for the color chooser dialog
	 * 		when it comes up. Can be null.
	 * @param strDialogTitle The dialog title of the color chooser dialog.
	 * @param colDefault Default color that a user can select. Can be null.
	 * @param bAllowDefaultSelection Flag to determine, if we allow the user to specify a default color
	 * 		(with an extra button in the dialog).
	 */
	public TableColorCellEditor(final Component parentComponent, final String strDialogTitle, final Color colDefault,
			final boolean bAllowDefaultSelection) {
		m_button = new JButton() {
			private static final long serialVersionUID = 2756388236946111058L;

			@Override
			protected void paintComponent(final Graphics g) {
				g.setColor(m_currentColor == null ? Color.WHITE : m_currentColor);
				g.fillRect(0, 0, g.getClipBounds().width, g.getClipBounds().height);
			}
		};
		m_button.setActionCommand(EDIT);
		m_button.setBorder(null);
		m_button.addActionListener(this);

		m_parent = parentComponent;
		m_strDialogTitle = strDialogTitle;
		m_bAllowDefaultSelection = bAllowDefaultSelection;
		m_colDefault = colDefault;
		m_currentColor = colDefault;
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		if (EDIT.equals(e.getActionCommand())) {
			// The user has clicked the cell, so
			// bring up the dialog.
			final boolean bIsDefaultColor = SettingsUtils.equals(m_colDefault, m_currentColor);
			COLOR_CHOOSER.setColor(bIsDefaultColor && m_bAllowDefaultSelection ? null : m_currentColor);

			// Show a color chooser dialog with a default color button
			if (m_bAllowDefaultSelection) {
				final int iRet = JOptionPane.showOptionDialog(m_parent,
						COLOR_CHOOSER, m_strDialogTitle, JOptionPane.NO_OPTION,
						JOptionPane.PLAIN_MESSAGE, null, new Object[] { DEFAULT_COLOR_BUTTON, "OK", "Cancel" },
						SettingsUtils.equals(m_colDefault, m_currentColor) ? DEFAULT_COLOR_BUTTON : "OK");
				if (iRet == 0) {
					m_currentColor = m_colDefault;
				}
				else if (iRet == 1) {
					m_currentColor = COLOR_CHOOSER.getColor();
				}

			}

			// Show a color chooser dialog without the default option
			else {
				if (JOptionPane.showOptionDialog(m_parent,
						COLOR_CHOOSER, m_strDialogTitle, JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.DEFAULT_OPTION, null, null, null) == JOptionPane.OK_OPTION) {
					m_currentColor = COLOR_CHOOSER.getColor();
				}
			}

			fireEditingStopped(); // Make the renderer reappear.
		}
	}

	// Implement the one CellEditor method that AbstractCellEditor doesn't.
	@Override
	public Object getCellEditorValue() {
		return m_currentColor;
	}

	// Implement the one method defined by TableCellEditor.
	@Override
	public Component getTableCellEditorComponent(final JTable table, final Object value,
			final boolean isSelected, final int row, final int column) {
		m_currentColor = (Color) value;
		return m_button;
	}
}