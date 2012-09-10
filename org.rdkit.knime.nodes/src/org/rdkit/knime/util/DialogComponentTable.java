/**
 * 
 */
package org.rdkit.knime.util;

import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;

/**
 * @author Manuel Schwarze
 */
public class DialogComponentTable extends DialogComponent {

	//
	// Members
	//
	
	/** The table model for convenience. Same as settings model. */
	private final TableModel m_tableModel;

    /** The label shown at the top side of the table. */
    private final JLabel m_label;
	
    /** The table GUI component. */
	private final JTable m_table;
	
	/** The table's scroll pane component. */
	private final JScrollPane m_scrollPane;
	
	// 
	// Constructor
	//
	
	/**
    * Constructor that puts label and combobox into panel. It expects the user
     * to make a selection, thus, at least one item in the list of selectable
     * items is required. When the settings are applied, the model stores one of
     * the enumerations of the provided list.
     *
     * @param model The model that stores the values for this component. This 
     * 		model is required to implement also the {@link TableModel} interface.
     * @param label Label for dialog on top of the table.
     * 
     * @throws AssertionError Thrown, if the passed in model does not implement
     * 		the  {@link TableModel} interface.
     */
	public DialogComponentTable(SettingsModel model, String label) {
		super(model);
		
		assert(model instanceof TableModel);
		
		m_tableModel = (TableModel)model;

		m_table = new JTable(m_tableModel) {
			
			/** Serial number. */
			private static final long serialVersionUID = -8101292468407244517L;

			/**
			 * This method protects this table from being used in 
			 * the wrong way.
			 */
			@Override
			public void setModel(final TableModel dataModel) {
				if (dataModel != m_tableModel) {
					throw new IllegalArgumentException("It is not allowed to change the " +
							"model of this table once it is set.");
				}
				
				super.setModel(dataModel);
			}
			
		    /**
		     * Asks the settings model for a tooltip
		     * {@inheritDoc}
		     * @see javax.swing.JTable#getToolTipText(java.awt.event.MouseEvent)
		     */
		    @Override
		    public String getToolTipText(final MouseEvent event) {
		        final Point mousePoint = event.getPoint();
		        final int rowIndex = rowAtPoint(mousePoint);
		        final int viewColIndex = columnAtPoint(mousePoint);
		        final int colIndex = convertColumnIndexToModel(viewColIndex);
		        String strTooltip = getToolTipTextForCell(rowIndex, colIndex);

		        if (strTooltip == null) {
		        	strTooltip = super.getToolTipText(event);
				}
		        
		    	return strTooltip;
		    }
		};
		
		m_scrollPane = new JScrollPane(m_table, 
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
        JPanel panel = getComponentPanel();
        panel.setLayout(new GridBagLayout());
        
        int iRow = 0;
        
        if (label != null) {
            m_label = new JLabel(label);
			LayoutUtils.constrain(panel, m_label, 0, iRow++, LayoutUtils.REMAINDER, 1, 
					LayoutUtils.HORIZONTAL, LayoutUtils.NORTHEAST, 1.0d, 0.0d, 0, 0, 10, 0);    	
        }
        else {
        	m_label = null;
        }
        
		LayoutUtils.constrain(panel, m_scrollPane, 0, iRow++, LayoutUtils.REMAINDER, LayoutUtils.REMAINDER, 
				LayoutUtils.BOTH, LayoutUtils.CENTER, 1.0d, 1.0d, 0, 0, 0, 0);
	}

	//
	// Public Methods
	//

	/**
	 * Override this method to deliver a tooltip based on the cell where the mouse
	 * hovers. By default this method returns null, which will lead to the default
	 * tooltip that is set for the entire table.
	 * 
	 * @param iRow Model based row index of cell. 
	 * @param iCol Model based column index of cell.
	 */
	public String getToolTipTextForCell(int iRow, int iCol) {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setToolTipText(String text) {
		m_table.setToolTipText(text);
	}	
	
	/**
	 * Returns the table model of this component, which is at the same
	 * time also the settings model and would be also returned with
	 * {@link #getModel()}.
	 * 
	 * @return Table model. Never null.
	 */
	public TableModel getTableModel() {
		return m_tableModel;
	}
	
	/**
	 * Returns the table GUI component for further customization.
	 * 
	 * @return Table of this component. Never null.
	 */
	public JTable getTable() {
		return m_table;
	}
	
	/**
	 * Returns the scroll pane GUI component of the table 
	 * for further customization.
	 * 
	 * @return Scroll pane of the table of this component. Never null.
	 */
	public JScrollPane getScrollPane() {
		return m_scrollPane;
	}
	
	/**
	 * Convenience method to get a table column for the specified index.
	 * 
	 * @param index Index of the table column to retrieve.
	 * 
	 * @return The table column or null, if index is invalid.
	 */
	public TableColumn getColumn(int index) {
		TableColumnModel columnModel = m_table.getColumnModel();
		int iColCount = columnModel.getColumnCount();
		return (index >= 0 && index < iColCount ? columnModel.getColumn(index) : null);
	}
	
	/**
	 * Convenience method to set widths for the columns
	 * of this table. The order of the provided values will
	 * be applied to the columns in the same order as they
	 * are returned by a call to JTable.getColumnModel().getColumn(x).
	 * 
	 * @param iColumnWidths Widths of columns. Values
	 * 		that should not be changed can be set to -1.
	 */
	public void setColumnWidths(int... iColumnWidths) {
		TableColumnModel columnModel = m_table.getColumnModel();
		int iColCount = columnModel.getColumnCount();
		
		for (int i = 0; i < iColumnWidths.length && i < iColCount; i++) {
			if (iColumnWidths[i] >= 0) {
				columnModel.getColumn(i).setWidth(iColumnWidths[i]);
			}
		}
	}
	
	/**
	 * Convenience method to set minimum widths for the columns
	 * of this table. The order of the provided values will
	 * be applied to the columns in the same order as they
	 * are returned by a call to JTable.getColumnModel().getColumn(x).
	 * 
	 * @param iMinColumnWidths Minimum widths of columns. Values
	 * 		that should not be changed can be set to -1.
	 */
	public void setMinColumnWidths(int... iMinColumnWidths) {
		TableColumnModel columnModel = m_table.getColumnModel();
		int iColCount = columnModel.getColumnCount();
		
		for (int i = 0; i < iMinColumnWidths.length && i < iColCount; i++) {
			if (iMinColumnWidths[i] >= 0) {
				columnModel.getColumn(i).setWidth(iMinColumnWidths[i]);
			}
		}
	}
	
	/**
	 * Convenience method to set maximal widths for the columns
	 * of this table. The order of the provided values will
	 * be applied to the columns in the same order as they
	 * are returned by a call to JTable.getColumnModel().getColumn(x).
	 * 
	 * @param iMaxColumnWidths Maximal widths of columns. Values
	 * 		that should not be changed can be set to -1.
	 */
	public void setMaxColumnWidths(int... iMaxColumnWidths) {
		TableColumnModel columnModel = m_table.getColumnModel();
		int iColCount = columnModel.getColumnCount();
		
		for (int i = 0; i < iMaxColumnWidths.length && i < iColCount; i++) {
			if (iMaxColumnWidths[i] >= 0) {
				columnModel.getColumn(i).setMaxWidth(iMaxColumnWidths[i]);
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
	protected void updateComponent() {
		// Force a reload from model data into the table view
		m_table.tableChanged(new TableModelEvent(getTableModel()));
		
        // Update the enable status
        setEnabledComponents(getModel().isEnabled());
	}

	/**
	 * Does not do anything in this implementation, because the table model 
	 * and the table component are directly connected.
	 */
	@Override
	protected void validateSettingsBeforeSave() throws InvalidSettingsException {
		// Does not do anything, because the table model and the table component
		// are directly connected.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void checkConfigurabilityBeforeLoad(PortObjectSpec[] specs)
			throws NotConfigurableException {
		// This works always.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void setEnabledComponents(boolean enabled) {
		if (m_label != null) {
			m_label.setEnabled(enabled);
		}
		
		m_table.setEnabled(enabled);
		m_scrollPane.setEnabled(enabled);
	}

}
