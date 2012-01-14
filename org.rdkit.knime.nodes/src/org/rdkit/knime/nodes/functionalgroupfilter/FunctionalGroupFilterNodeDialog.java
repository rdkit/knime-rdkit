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
package org.rdkit.knime.nodes.functionalgroupfilter;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.core.node.util.FilesHistoryPanel;
import org.rdkit.knime.nodes.functionalgroupfilter.FunctionalGroupNodeSettings.LineProperty;
import org.rdkit.knime.types.RDKitMolValue;


/**
 * This is the dialog for the Functional Group Filter node. It lets the user
 * choose a RDKit column from the incoming table and also to load functional
 * group definitions file if the default file is not not be used for filtering
 * purposes. The user can then select which functional groups filter he wants to
 * be applied to each molecule.
 * 
 * @author Dillip K Mohanty
 */
public class FunctionalGroupFilterNodeDialog extends NodeDialogPane {

	/**
	 * This class is used for loading the functional group to the dialog pane.
	 * 
	 * @author Dillip K Mohanty
	 * 
	 */
	private class FileScanner extends
			SwingWorker<ArrayList<FunctionalGroup>, Object[]> {

		@Override
		protected ArrayList<FunctionalGroup> doInBackground() throws Exception {
			return scanFile();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void done() {
			m_filePanel.setCursor(Cursor
					.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			ArrayList<FunctionalGroup> properties;
			try {
				properties = get();
			} catch (Exception ex) {
//				JOptionPane.showMessageDialog(m_filePanel,
//						"Could not extract properties: "
//								+ ex.getCause().getMessage(), "Error",
//						JOptionPane.ERROR_MESSAGE);
				LOGGER.error("Could not properly extract properties", ex);
				return;
			}
			m_propsModel.update(properties);
		}

		/**
		 * This method makes a call to readFuncGroupPatterns() method for
		 * reading the functional group definition file and return the list of
		 * FunctionalGroup objects to be displayed on the dialog panel.
		 * 
		 * @return ArrayList<FunctionalGroup>
		 * @throws Exception
		 */
		private ArrayList<FunctionalGroup> scanFile() throws Exception {
			FunctionalGroupFilter funcGrpFilter = new FunctionalGroupFilter();
			ArrayList<FunctionalGroup> funcGroupDefnList = funcGrpFilter
					.readFuncGroupPatterns(m_funcSettings.getFileUrl());
			return funcGroupDefnList;
		}
	}

	/**
	 * This class is used for handling the spinner characteristics for each
	 * group.
	 * 
	 * @author Dillip K Mohanty
	 * 
	 */
	public class SpinnerEditor extends AbstractCellEditor implements
			TableCellEditor {

		/**
		 * SerialVersionUID
		 */
		private static final long serialVersionUID = -7322590161328326802L;

		final JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 100,
				1));

		// Prepares the spinner component and returns it.
		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int column) {
			spinner.setValue(value);
			return spinner;
		}

		// Enables the editor only for clicks.
		public boolean isCellEditable(EventObject evt) {
			return true;
		}

		// Returns the spinners current value.
		public Object getCellEditorValue() {
			return spinner.getValue();
		}
	}

	/**
	 * This class is used for handling the functional group definitions shown as
	 * a table model.
	 * 
	 * @author Dillip K Mohanty
	 * 
	 */
	private static class PropertiesTableModel extends AbstractTableModel {

		/**
		 * SerialVersionUID
		 */
		private static final long serialVersionUID = 7390119658041208690L;

		private final List<LineProperty> m_properties = new ArrayList<LineProperty>();

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getColumnName(final int column) {
			switch (column) {
			case 0:
				return "Select";
			case 1:
				return "Functional Group Name";
			case 2:
				return "Qualifier";
			case 3:
				return "Count";
			default:
				return "???";
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Class<?> getColumnClass(final int columnIndex) {
			switch (columnIndex) {
			case 0:
				return Boolean.class;
			case 1:
				return String.class;
			case 2:
				return String.class;
			case 3:
				return Integer.class;
			default:
				return Object.class;
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isCellEditable(final int rowIndex, final int columnIndex) {
			switch (columnIndex) {
			case 0:
				return true;
			case 2:
				return true;
			case 3:
				return true;
			default:
				return false;
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int getColumnCount() {
			return 4;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int getRowCount() {
			return m_properties.size();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex) {
			switch (columnIndex) {
			case 0:
				return m_properties.get(rowIndex).isSelect();
			case 1:
				return m_properties.get(rowIndex).getName();
			case 2:
				return m_properties.get(rowIndex).getQualifier();
			case 3:
				return m_properties.get(rowIndex).getCount();
			default:
				return "???";
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void setValueAt(final Object value, final int rowIndex,
				final int columnIndex) {
			switch (columnIndex) {
			case 0:
				m_properties.get(rowIndex).setSelect((Boolean) value);
				break;
			case 2:
				m_properties.get(rowIndex).setQualifier((String) value);
				break;
			case 3:
				m_properties.get(rowIndex).setCount((Integer) value);
				break;
			}
		}

		/**
		 * Updates the table model with the found properties.
		 * 
		 * @param props
		 *            a list of Functional groups
		 */
		public void update(final ArrayList<FunctionalGroup> props) {
			m_properties.clear();

			synchronized (props) {
				for (Iterator<FunctionalGroup> iterator = props.iterator(); iterator
						.hasNext();) {
					FunctionalGroup functionalGroup = iterator.next();
					m_properties.add(new LineProperty(false, functionalGroup
							.getDisplayLabel(), functionalGroup.getQualifier()
							.toString(), Long.valueOf(
							functionalGroup.getFuncCount()).intValue()));
				}
			}

			fireTableDataChanged();
		}

		/**
		 * Returns the properties shown in the table.
		 * 
		 * @return a list of the shown properties
		 */
		public List<LineProperty> getProperties() {
			return m_properties;
		}

		/**
		 * Updates the table model with the given properties.
		 * 
		 * @param props
		 *            a list of properties
		 */
		public void update(final Iterable<LineProperty> props) {
			m_properties.clear();
			for (LineProperty p : props) {
				m_properties.add(new LineProperty(p));
			}
		}
	}

	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(FunctionalGroupFilterNodeDialog.class);

	// static final FilenameFilter FILTER = new FilenameFilter() {
	// @Override
	// public boolean accept(final File dir, final String name) {
	// String s = name.toLowerCase();
	// return s.endsWith(".txt");
	// }
	// };

	/**
	 * Instance of main dialog panel
	 */
	private JPanel m_dialogPanel;

	/**
	 * Instance of file scanner (swing worker object)
	 */
	private FileScanner m_fileScanner;
	
	/**
	 * Instance of load default button 
	 */
	private final JButton m_default = new JButton("Load default");
	
	/**
	 * Node Settings instance to transfer values from dialog pane to node model.
	 */
	private final FunctionalGroupNodeSettings m_funcSettings = new FunctionalGroupNodeSettings();

	@SuppressWarnings("unchecked")
	/**
	 * Instance of molecule column combo box which shows only RDKit molecule type.
	 */
	private final ColumnSelectionComboxBox m_molColumn = new ColumnSelectionComboxBox(
			(Border) null, RDKitMolValue.class);

	/**
	 * Instance of Files History panel
	 */
	private final FilesHistoryPanel m_file = new FilesHistoryPanel(
			FunctionalGroupFilterNodeDialog.class.toString());

	/**
	 * Instance of Column Selection Combo Box panel
	 */
	private final JPanel m_columnPanel = new JPanel(new GridBagLayout());

	/**
	 * Instance of File panel
	 */
	private final JPanel m_filePanel = new JPanel(new GridBagLayout());

	/**
	 * Instance of functional group filter panel
	 */
	private final JPanel m_propertiesPanel = new JPanel(new GridBagLayout());

	/**
	 * Instance of properties model
	 */
	private final PropertiesTableModel m_propsModel = new PropertiesTableModel();

	/**
	 * Instance of table which holds properties model
	 */
	private final JTable m_propertiesTable = new JTable(m_propsModel);

	/**
	 * Create the dialog pane components for configuring Functional Group Filter
	 * node.
	 */
	protected FunctionalGroupFilterNodeDialog() {
		super();

		m_dialogPanel = new JPanel();
		m_dialogPanel.setLayout(new BoxLayout(m_dialogPanel, BoxLayout.Y_AXIS));
		m_dialogPanel.add(Box.createVerticalGlue());
		// Table Column selection panel
		m_dialogPanel.add(createColumnPanel());
		// Functional group definition file selection panel
		m_dialogPanel.add(createFileNamePanel());
		// Filter panel
		m_dialogPanel.add(createFilterPanel());
		m_dialogPanel.add(Box.createVerticalGlue());
		super.addTab("Settings", m_dialogPanel);
	}

	/**
	 * Method for creating the Column selection panel in the node dialog
	 * 
	 * @return JPanel
	 */
	private JPanel createColumnPanel() {
		JPanel p = m_columnPanel;
		// Sets a border for the panel
		p.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(), "Select molecule column"));
		JLabel commentLabel = new JLabel(" Column name :");
		p.add(commentLabel);
		p.add(Box.createHorizontalGlue());
		p.add(m_molColumn);
		return p;
	}

	/**
	 * Method for creating the File selection panel in the node dialog
	 * 
	 * @return JPanel
	 */
	private JPanel createFileNamePanel() {

		JPanel p = m_filePanel;
		// Sets a border for the panel
		p.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(),
				"Select functional group definition file (Optional)"));
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(2, 0, 2, 0);
		c.anchor = GridBagConstraints.WEST;
		// Add label
		p.add(new JLabel(" File path : "), c);
		c.gridx = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		// Add File history panel
		p.add(m_file, c);
		
		m_file.setSelectMode(JFileChooser.FILES_AND_DIRECTORIES);
		m_file.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				if (m_file.getSelectedFile() != null && !m_file.getSelectedFile().trim().equals("")) {
					m_filePanel.setCursor(Cursor
							.getPredefinedCursor(Cursor.WAIT_CURSOR));
					ArrayList<FunctionalGroup> properties = new ArrayList<FunctionalGroup>();
					m_funcSettings.setFileUrl(m_file.getSelectedFile());
					m_propsModel.update(properties);
					if (m_fileScanner != null) {
						m_fileScanner.cancel(true);
					}
					m_fileScanner = new FileScanner();
					m_fileScanner.execute();
				}
			}
		});
        c.gridx++;

        p.add(m_default, c);
        m_default.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
            	m_filePanel.setCursor(Cursor
						.getPredefinedCursor(Cursor.WAIT_CURSOR));
				ArrayList<FunctionalGroup> properties = new ArrayList<FunctionalGroup>();
				m_funcSettings.setFileUrl("");
				m_file.setSelectedFile("");
				m_propsModel.update(properties);
				if (m_fileScanner != null) {
					m_fileScanner.cancel(true);
				}
				m_fileScanner = new FileScanner();
				m_fileScanner.execute();
            }
        });
		return p;
	}

	/**
	 * Method for creating the Filter panel in the node dialog
	 * 
	 * @return JPanel
	 */
	private JPanel createFilterPanel() {

		JPanel p = m_propertiesPanel;
		// Sets a border for the panel
		p.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(),
				"List of available functional group filters"));
		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(2, 2, 2, 2);
		c.fill = GridBagConstraints.BOTH;

		JScrollPane sp = new JScrollPane(m_propertiesTable);
		p.add(sp, c);

		TableColumn selectColumn = m_propertiesTable.getColumnModel()
				.getColumn(0);
		selectColumn.setMaxWidth(50);

		TableColumn qualColumn = m_propertiesTable.getColumnModel()
				.getColumn(2);
		qualColumn.setMaxWidth(100);

		TableColumn countColumn = m_propertiesTable.getColumnModel().getColumn(
				3);
		countColumn.setMaxWidth(100);

		m_propertiesTable.setRowHeight(22);

		JComboBox comboBox = new JComboBox();
		comboBox.setRenderer(new DefaultListCellRenderer() {

			/**
			 * SerialVersionUID
			 */
			private static final long serialVersionUID = -2023841628996120350L;

			@Override
			public Component getListCellRendererComponent(final JList list,
					final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus) {
				String typeText = (String) value;
				return super.getListCellRendererComponent(list, typeText,
						index, isSelected, cellHasFocus);
			}

		});
		// Addds the list of values to the dropdown
		comboBox.addItem("LessThan(<)");
		comboBox.addItem("AtMost(<=)");
		comboBox.addItem("Exactly(=)");
		comboBox.addItem("AtLeast(>=)");
		comboBox.addItem("MoreThan(>)");

		qualColumn.setCellEditor(new DefaultCellEditor(comboBox));

		qualColumn.setCellRenderer(new DefaultTableCellRenderer() {
			/**
			 * SerialVersionUID
			 */
			private static final long serialVersionUID = 5994382641090249175L;

			/**
			 * {@inheritDoc}
			 */
			@Override
			public Component getTableCellRendererComponent(final JTable table,
					final Object value, final boolean isSelected,
					final boolean hasFocus, final int row, final int column) {
				String typeText = (String) value;
				return super.getTableCellRendererComponent(table, typeText,
						isSelected, hasFocus, row, column);
			}
		});

		countColumn.setCellEditor(new SpinnerEditor());
		return p;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings,
			final DataTableSpec[] specs) throws NotConfigurableException {

		m_funcSettings.loadSettingsForDialog(settings);
		m_molColumn.update(specs[0], m_funcSettings.getColName());

		m_file.setSelectedFile(m_funcSettings.getFileUrl());
		if (m_funcSettings.properties() != null
				&& m_funcSettings.properties().size() > 0) {
			m_propsModel.update(m_funcSettings.properties());
		} else {
			if (m_fileScanner != null) {
				m_fileScanner.cancel(true);
			}
			m_fileScanner = new FileScanner();
			m_fileScanner.execute();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings)
			throws InvalidSettingsException {

		m_funcSettings.setColName(m_molColumn.getSelectedColumn());
		m_funcSettings.setFileUrl(m_file.getSelectedFile());
		m_funcSettings.clearProperties();
		for (FunctionalGroupNodeSettings.LineProperty p : m_propsModel
				.getProperties()) {
			m_funcSettings
					.addProperty(new FunctionalGroupNodeSettings.LineProperty(p));
		}
		m_funcSettings.saveSettings(settings);
	}

	@Override
	public void onClose() {
		super.onClose();
		if (m_fileScanner != null) {
			m_fileScanner.cancel(true);
		}
	}
}
