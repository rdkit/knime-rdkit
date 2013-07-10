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

import java.awt.Component;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellEditor;

/**
 * This class is used for handling the spinner characteristics for
 * a number to be edited in a table cell.
 * 
 * @author Dillip K Mohanty
 * @author Manuel Schwarze
 */
public class SpinnerEditor extends AbstractCellEditor implements TableCellEditor {

	//
	// Inner Classes
	//

	/**
	 * This class takes care of auto updating a value in the table and
	 * its model while the user is still editing. That way the value is
	 * not getting lost when the KNIME settings dialog is closed while
	 * the focus is still in the spinner field.
	 * 
	 * @author Dillip K Mohanty
	 * @author Manuel Schwarze
	 */
	private class TableAutoUpdate implements ChangeListener {

		//
		// Member Variables
		//

		/** The table that shall is edited and that shall be auto updated. */
		private JTable m_tableForAutoUpdate;

		/** The current row of the cell that is edited. */
		private int m_iRow;

		/** The current column of the cell that is edited. */
		private int m_iCol;

		/** The spinner component used for editing that provides new values. */
		private final JSpinner m_spinnerToMonitor;

		//
		// Constructor
		//

		/**
		 * Creates an instance of the TableAutoUpdate monitoring
		 * changes in the specified spinner component.
		 * 
		 * @param spinnerToMonitor Spinner component to monitor for
		 * 		table auto updates. Must not be null.
		 */
		public TableAutoUpdate(final JSpinner spinnerToMonitor) {
			if (spinnerToMonitor == null) {
				throw new IllegalArgumentException("Spinner component must not be null.");
			}

			m_spinnerToMonitor = spinnerToMonitor;
			m_spinnerToMonitor.addChangeListener(this);
		}

		//
		// Public Methods
		//

		/**
		 * Sets the information about the currently edited cell.
		 * 
		 * @param tableForAutoUpdate Table that is currently edited.
		 * @param iRow Row index of the cell that is currently edited.
		 * @param iColumn Column index of the cell that is currently edited.
		 */
		public void setCellInfo(final JTable tableForAutoUpdate, final int iRow, final int iColumn) {
			m_tableForAutoUpdate = tableForAutoUpdate;
			m_iRow = iRow;
			m_iCol = iColumn;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void stateChanged(final ChangeEvent e) {
			try {
				m_tableForAutoUpdate.setValueAt(m_spinnerToMonitor.getValue(), m_iRow, m_iCol);
			}
			catch (final Exception exc) {
				exc.printStackTrace();
				// Ignored
			}
		}
	}

	//
	// Constants
	//

	/** Serial number. */
	private static final long serialVersionUID = -7322590161328326802L;

	//
	// Members
	//

	/** The spinner component for this editor. */
	private final JSpinner m_spinner;

	/**
	 * This component takes care of submitted every spinner value change
	 * immediately to the table.
	 */
	private final TableAutoUpdate m_autoUpdater;

	//
	// Constructors
	//

	/**
	 * Creates a new spinner editor for the specified spinner model.
	 * 
	 * @param spinnerModel Spinner model. Must not be null.
	 */
	public SpinnerEditor(final SpinnerModel spinnerModel) {
		if (spinnerModel == null) {
			throw new IllegalArgumentException("Spinner model must not be null.");
		}

		m_spinner = new JSpinner(spinnerModel);
		m_autoUpdater = new TableAutoUpdate(m_spinner);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Component getTableCellEditorComponent(final JTable table,
			final Object value, final boolean isSelected, final int row, final int column) {
		m_autoUpdater.setCellInfo(table, row, column);
		m_spinner.setValue(value);
		return m_spinner;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCellEditable(final EventObject evt) {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getCellEditorValue() {
		return m_spinner.getValue();
	}
}