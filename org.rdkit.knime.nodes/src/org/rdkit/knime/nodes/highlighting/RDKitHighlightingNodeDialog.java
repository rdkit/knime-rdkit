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
package org.rdkit.knime.nodes.highlighting;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.knime.base.data.aggregation.dialogutil.DataColumnSpecTableCellRenderer;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButton;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.rdkit.knime.nodes.highlighting.HighlightingDefinition.Type;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.DialogComponentColumnNameSelection;
import org.rdkit.knime.util.DialogComponentSeparator;
import org.rdkit.knime.util.DialogComponentTable;
import org.rdkit.knime.util.LayoutUtils;
import org.rdkit.knime.util.TableCellAction;
import org.rdkit.knime.util.TableColorCellEditor;

/**
 * <code>NodeDialog</code> for the "RDKitHighlighting" Node. Creates a SVG
 * column showing a molecule with highlighted atoms / bonds based on information
 * in the input table.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Manuel Schwarze
 */
public class RDKitHighlightingNodeDialog extends DefaultNodeSettingsPane {

	//
	// Constants
	//

	/** Text to show in gray in table cell with an invalid input column. */
	public static final String EMPTY_COLUMN_NAME_TEXT = "<Click to pick an input column>";

	/** Text to show in gray in table cell when using default column. */
	public static final String DEFAULT_COLOR_TEXT = "Default";

	//
	// Members
	//

	/** The model for all highlighting definitions. */
	private final SettingsModelHighlighting m_modelDefinitions;

	/** The dialog component table. */
	private final DialogComponentTable m_compTable;

	//
	// Constructor
	//

	/**
	 * Create a new dialog pane with default components to configure an input
	 * column, the name of a new column, which will contain the calculation
	 * results, an option to tell, if the source column shall be removed from
	 * the result table.
	 */
	RDKitHighlightingNodeDialog() {
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				createInputMolColumnNameModel(), "RDKit Mol column: ", 0,
				RDKitMolValue.class));

		super.addDialogComponent(new DialogComponentString(
				createNewColumnNameModel(),
				"Column name for molecule highlighting: ", false, 30));


		super.addDialogComponent(new DialogComponentSeparator());

		m_modelDefinitions = createHighlightingDefinitionsModel();
		m_compTable = createDefinitionsTable();
		super.addDialogComponent(m_compTable);

		final DialogComponentButton btnAddRow = new DialogComponentButton("Add new highlighting definition");
		btnAddRow.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				m_modelDefinitions.add(null);
			}
		});
		super.addDialogComponent(btnAddRow);

		LayoutUtils.correctKnimeDialogBorders(getPanel());
		m_compTable.getComponentPanel().setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
		m_compTable.getComponentPanel().setPreferredSize(new Dimension(500, 200));
	}

	//
	// Static Methods
	//

	/**
	 * Creates the settings model to be used for the input column.
	 * 
	 * @return Settings model for input column selection.
	 */
	static final SettingsModelString createInputMolColumnNameModel() {
		return new SettingsModelString("input_column", null);
	}

	/**
	 * Creates the settings model to be used for the highlighting definitions.
	 * 
	 * @return Settings model for highlighting definitions.
	 */
	static final SettingsModelHighlighting createHighlightingDefinitionsModel() {
		return new SettingsModelHighlighting("highlighting");
	}

	/**
	 * Creates the settings model to be used to specify the new column name.
	 * 
	 * @return Settings model for result column name.
	 */
	static final SettingsModelString createNewColumnNameModel() {
		return new SettingsModelString("new_column_name", null);
	}

	@Override
	public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
			final DataTableSpec[] specs) throws NotConfigurableException {
		super.loadAdditionalSettingsFrom(settings, specs);

		// Make the input table specification known to the definition model and table editor
		final DataTableSpec tableSpec = (specs == null || specs.length < 1 ? null : specs[0]);
		m_modelDefinitions.updateTableSpec(tableSpec);
		m_compTable.getColumn(SettingsModelHighlighting.COLUMN_INPUT_COLUMN_NAME).setCellEditor(createInputColumnCellEditor(tableSpec));
	}

	/**
	 * Creates the editor that shall be used in the type column. We use a
	 * combobox that contains the types.
	 * 
	 * @return Table cell renderer.
	 */
	protected TableCellEditor createTypeCellEditor() {
		final JComboBox<Type> comboBox = new JComboBox<Type>();
		comboBox.setRenderer(new DefaultListCellRenderer());
		((DefaultListCellRenderer) comboBox.getRenderer())
		.setHorizontalAlignment(SwingConstants.LEFT);

		// Adds the list of qualifiers to the drop down
		for (final Type type : Type.values()) {
			comboBox.addItem(type);
		}

		return new DefaultCellEditor(comboBox);
	}

	/**
	 * Creates the renderer that shall be used in the column to pick the input
	 * column.
	 * 
	 * @return Table cell renderer.
	 */
	protected TableCellRenderer createInputColumnCellRenderer() {
		return new DataColumnSpecTableCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
			public Component getTableCellRendererComponent(final JTable table,
					final Object value, final boolean isSelected, final boolean hasFocus,
					final int row, final int column) {
				final JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
						row, column);

				if (SettingsModelHighlighting.UNDEFINED_COLUMN.toString().equals("" + value)) {
					label.setText(EMPTY_COLUMN_NAME_TEXT);
					label.setIcon(null);
					label.setForeground(Color.GRAY);
				}
				else {
					label.setForeground(Color.BLACK);
				}

				return label;
			}
		};
	}

	/**
	 * Creates the editor that shall be used in the type column. We use a
	 * combobox that contains the types.
	 * 
	 * @return Table cell renderer.
	 */
	@SuppressWarnings("unchecked")
	protected TableCellEditor createInputColumnCellEditor(final DataTableSpec tableSpec) {
		final
		ColumnSelectionPanel editor = new ColumnSelectionPanel(null, new ColumnFilter() {

			@Override
			public boolean includeColumn(final DataColumnSpec colSpec) {
				return (colSpec != null && colSpec.getType().isCollectionType() &&
						(colSpec.getType().getCollectionElementType().isCompatible(IntValue.class) ||
								colSpec.getType().getCollectionElementType().isCompatible(LongValue.class)));
			}

			@Override
			public String allFilteredMsg() {
				return "Input table does not contain any integer collection columns";
			}
		}, true, false);

		if (tableSpec != null) {
			try {
				editor.update(tableSpec, null);
			}
			catch (final NotConfigurableException exc) {
				// Ignored - Should not happen
			}
		}

		JComboBox<DataTableSpec> comboBox = null;

		for (final Component c : editor.getComponents()) {
			if (c instanceof JComboBox) {
				comboBox = (JComboBox<DataTableSpec>)c;
			}
		}

		return new DefaultCellEditor(comboBox);
	}

	/**
	 * Creates the renderer that shall be used in the column to pick the
	 * highlighting color.
	 * 
	 * @return Table cell renderer.
	 */
	protected TableCellRenderer createColorCellRenderer() {
		return new DefaultTableCellRenderer() {

			private static final long serialVersionUID = 1L;

			@Override
			public Component getTableCellRendererComponent(
					final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
				Color colText = Color.BLACK;
				Color colBackground = (Color)value;
				String strText = "";

				if (value == null) {
					strText = DEFAULT_COLOR_TEXT;
					colText = Color.GRAY;
					colBackground = Color.WHITE;
				}

				final JLabel label = (JLabel)super.getTableCellRendererComponent(table,
						strText, false, hasFocus, row, column);

				label.setHorizontalAlignment(SwingConstants.CENTER);

				if (colText != null) {
					label.setForeground(colText);
				}

				if (colBackground != null) {
					label.setBackground(colBackground);
				}

				return label;
			}
		};
	}

	/**
	 * Creates the renderer that shall be used in the column to pick the
	 * highlighting color.
	 * 
	 * @return Table cell renderer.
	 */
	protected TableCellRenderer createCenteredCellRenderer() {
		return new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 8239675687955732039L;

			@Override
			public Component getTableCellRendererComponent(final JTable table,
					final Object value, final boolean isSelected, final boolean hasFocus,
					final int row, final int column) {
				final JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
						row, column);
				label.setHorizontalAlignment(SwingConstants.CENTER);
				return label;
			}
		};
	}

	/**
	 * Creates the editor that shall be used in the column to pick the
	 * highlighting color.
	 * 
	 * @return Table cell editor.
	 */
	protected TableCellEditor createColorCellEditor() {
		return new TableColorCellEditor(getPanel(), "Please select a Highlighting Color", null, true);
	}

	//
	// Private Methods
	//

	/**
	 * Creates the table component for configuring the conditions.
	 * 
	 * @return Dialog component table.
	 */
	private DialogComponentTable createDefinitionsTable() {
		final DialogComponentTable tableComp = new DialogComponentTable(
				m_modelDefinitions, "Highlighting Definitions: ") {
			@Override
			public String getToolTipTextForCell(final int iRow, final int iCol) {
				return m_modelDefinitions.getTooltip(iRow);
			}
		};
		tableComp.setMinColumnWidths(45, 60, -1, 80, 80, 20);
		tableComp.setMaxColumnWidths(45, 60, -1, 80, 80, 20);
		tableComp.setColumnWidths(45, 60, -1, 80, 80, 20);

		final JTable table = tableComp.getTable();
		table.setCellSelectionEnabled(false);
		table.setColumnSelectionAllowed(false);
		table.setRowHeight(20);
		final TableCellRenderer headerRenderer = table.getTableHeader()
				.getDefaultRenderer();
		if (headerRenderer instanceof JLabel) {
			((JLabel) headerRenderer)
			.setHorizontalAlignment(SwingConstants.CENTER);
		}

		tableComp.getColumn(SettingsModelHighlighting.COLUMN_TYPE)
		.setCellEditor(createTypeCellEditor());
		tableComp.getColumn(SettingsModelHighlighting.COLUMN_INPUT_COLUMN_NAME)
		.setCellRenderer(createInputColumnCellRenderer());
		// The editor for the input column we will set later when the table spec becomes available
		tableComp.getColumn(SettingsModelHighlighting.COLUMN_COLOR)
		.setCellRenderer(createColorCellRenderer());
		tableComp.getColumn(SettingsModelHighlighting.COLUMN_COLOR)
		.setCellEditor(createColorCellEditor());
		tableComp.getColumn(SettingsModelHighlighting.COLUMN_DELETE_ACTION)
		.setCellEditor(new TableCellAction() {
			@Override
			public void onAction(final JTable table, final int row, final int column) {
				m_modelDefinitions.remove(row);
			}
		});

		final JPopupMenu contextMenu = new JPopupMenu("Helpers ...");
		final JMenuItem itemActivateAll = new JMenuItem("Activate All");
		final JMenuItem itemDeactivateAll = new JMenuItem("Deactivate All");
		final JMenuItem itemRemoveAll = new JMenuItem("Remove All");
		contextMenu.add(itemActivateAll);
		contextMenu.add(itemDeactivateAll);
		contextMenu.add(itemRemoveAll);
		itemActivateAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				m_modelDefinitions.setAllActivated(true);
			}
		});
		itemDeactivateAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				m_modelDefinitions.setAllActivated(false);
			}
		});
		itemRemoveAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				m_modelDefinitions.removeAll();
			}
		});

		table.setComponentPopupMenu(contextMenu);

		return tableComp;
	}

}
