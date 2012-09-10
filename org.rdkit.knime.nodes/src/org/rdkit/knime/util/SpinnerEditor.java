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
		private JSpinner m_spinnerToMonitor;
		
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
		public TableAutoUpdate(JSpinner spinnerToMonitor) {
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
		public void setCellInfo(JTable tableForAutoUpdate, int iRow, int iColumn) {
			m_tableForAutoUpdate = tableForAutoUpdate;
			m_iRow = iRow;
			m_iCol = iColumn;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void stateChanged(ChangeEvent e) {
			try {
				m_tableForAutoUpdate.setValueAt(m_spinnerToMonitor.getValue(), m_iRow, m_iCol);
			}
			catch (Exception exc) {
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
	public SpinnerEditor(SpinnerModel spinnerModel) {
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
	public Component getTableCellEditorComponent(JTable table,
			Object value, boolean isSelected, int row, int column) {
		m_autoUpdater.setCellInfo(table, row, column);
		m_spinner.setValue(value);
		return m_spinner;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCellEditable(EventObject evt) {
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