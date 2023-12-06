/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2012-2023
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
package org.rdkit.knime.nodes.functionalgroupfilter;

import java.awt.Dimension;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.filehandling.core.connections.FSLocationUtil;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode;
import org.rdkit.knime.nodes.functionalgroupfilter.SettingsModelFunctionalGroupConditions.Qualifier;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.DialogComponentColumnNameSelection;
import org.rdkit.knime.util.DialogComponentConfigFileSelection;
import org.rdkit.knime.util.DialogComponentTable;
import org.rdkit.knime.util.FileUtils;
import org.rdkit.knime.util.LayoutUtils;
import org.rdkit.knime.util.SpinnerEditor;

/**
 * {@code NodeDialog} for the "RDKitFunctionalGroupFilter" Node.
 * <br><br>
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Dillip K Mohanty
 * @author Manuel Schwarze
 * @author Roman Balabanov
 */
public class FunctionalGroupFilterV2NodeDialog extends DefaultNodeSettingsPane {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(FunctionalGroupFilterV2NodeDialog.class);

	//
	// Members
	//

	/** The model for setting the input file for the functional group settings. */
	private final SettingsModelReaderFileChooser m_modelGroupConfigPath;

	/** The model for all condition settings. */
	private final SettingsModelFunctionalGroupConditions m_modelConditions;

	/** The GUI component to select the custom definition file. */
	private final DialogComponentConfigFileSelection m_compGroupConfigPath;

	//
	// Constructor
	//

	/**
	 * Create a new dialog pane with default components to configure an input column,
	 * the name of a new column, which will contain the calculation results, an option
	 * to tell, if the source column shall be removed from the result table.
	 *
	 * @param nodeCreationConfig Node Creation Configuration instance.
	 *                           Mustn't be null.
	 * @throws IllegalArgumentException When {@code nodeCreationConfig} parameter is null.
	 */
	@SuppressWarnings("UnusedAssignment")
	FunctionalGroupFilterV2NodeDialog(NodeCreationConfiguration nodeCreationConfig) {
		int iPortInputTable = FunctionalGroupFilterV2NodeModel.getInputTablePortIndexes(nodeCreationConfig,
				FunctionalGroupFilterV2NodeFactory.INPUT_PORT_GRP_ID_MOLECULES)[0];

		// Create models first
		final SettingsModelString modelInputColumn = createInputColumnNameModel();
		m_modelGroupConfigPath = createInputPathModel(nodeCreationConfig);
		m_modelGroupConfigPath.addChangeListener(e -> refreshFunctionalGroupDefinitions());
		m_modelConditions = createFunctionalGroupConditionsModel(true);
		final SettingsModelBoolean modelRecordFailedPatternOption =
				createRecordFailedPatternOptionModel();
		final SettingsModelString modelNewFailedPatternColumnName =
				createNewFailedPatternColumnNameModel(modelRecordFailedPatternOption);

		// Create GUI components
		final DialogComponentColumnNameSelection compInputColumn =
				new DialogComponentColumnNameSelection(
						modelInputColumn, "", iPortInputTable,
						RDKitMolValue.class);
		super.addDialogComponent(compInputColumn);

		final DialogComponentReaderFileChooser compFileChooser = new DialogComponentReaderFileChooser(
				m_modelGroupConfigPath,
				"FunctionGroupDefinitionFileHistory",
				createFlowVariableModel(m_modelGroupConfigPath.getKeysForFSLocation(), FSLocationVariableType.INSTANCE)
		);
		super.addDialogComponent(compFileChooser);
		m_compGroupConfigPath = new DialogComponentConfigFileSelection(
				compFileChooser,
				"Functional Group Definition",
				modelFileChooser -> FunctionalGroupFilterV2NodeModel.readDefinitionsFile(
						m_modelGroupConfigPath, FileUtils::getContentFromResource, LOGGER)
		);
		super.addDialogComponent(m_compGroupConfigPath);

		final DialogComponentTable compTable = createConditionsTable();
		super.addDialogComponent(compTable);

		final DialogComponentBoolean compRecordFailedPattern =
				new DialogComponentBoolean(
						modelRecordFailedPatternOption,
						"Enable recording in the following new column:");
		super.addDialogComponent(compRecordFailedPattern);

		final DialogComponentString compNewColumnName =
				new DialogComponentString(modelNewFailedPatternColumnName, null, true, 30);
		super.addDialogComponent(compNewColumnName);

		// Relayout the components
		final JPanel panel = (JPanel)getTab("Options");
		panel.setLayout(new GridBagLayout());
		panel.removeAll();
		int iRow = 0;

		JPanel panelSub = new JPanel(new GridBagLayout());
		panelSub.setBorder(BorderFactory.createTitledBorder("Select molecule column"));
		LayoutUtils.constrain(panelSub, new JLabel("RDKit Mol column: "),
				0, 0, 1, LayoutUtils.REMAINDER,
				LayoutUtils.NONE, LayoutUtils.WEST, 0.0d, 0.0d,
				0, 10, 5, 10);
		LayoutUtils.constrain(panelSub, ((JPanel)compInputColumn.
				getComponentPanel().getComponent(1)).getComponent(0),
				1, 0, LayoutUtils.REMAINDER, LayoutUtils.REMAINDER,
				LayoutUtils.HORIZONTAL, LayoutUtils.CENTER, 1.0d, 0.0d,
				0, 10, 5, 10);

		LayoutUtils.constrain(panel, panelSub,
				0, iRow++, LayoutUtils.REMAINDER, 1,
				LayoutUtils.HORIZONTAL, LayoutUtils.CENTER, 1.0d, 0.0d,
				10, 10, 0, 10);

		LayoutUtils.constrain(panel, m_compGroupConfigPath.getComponentPanel(),
				0, iRow++, LayoutUtils.REMAINDER, 1,
				LayoutUtils.HORIZONTAL, LayoutUtils.CENTER, 1.0d, 0.0d,
				3, 10, 0, 10);

		panelSub = new JPanel(new GridBagLayout());
		panelSub.setBorder(BorderFactory.createTitledBorder("List of available functional group filters"));
		LayoutUtils.constrain(panelSub, compTable.getComponentPanel(),
				0, 0, LayoutUtils.REMAINDER, LayoutUtils.REMAINDER,
				LayoutUtils.BOTH, LayoutUtils.CENTER, 1.0d, 1.0d,
				0, 10, 10, 10);

		LayoutUtils.constrain(panel, panelSub,
				0, iRow++, LayoutUtils.REMAINDER, 1,
				LayoutUtils.BOTH, LayoutUtils.CENTER, 1.0d, 1.0d,
				3, 10, 0, 10);

		panelSub = new JPanel(new GridBagLayout());
		panelSub.setBorder(BorderFactory.createTitledBorder("Recording of first non-matching pattern in new column"));
		LayoutUtils.constrain(panelSub, compRecordFailedPattern.getComponentPanel(),
				0, 0, 1, LayoutUtils.REMAINDER,
				LayoutUtils.NONE, LayoutUtils.WEST, 0.0d, 0.0d,
				0, 0, 0, 0);
		LayoutUtils.constrain(panelSub, compNewColumnName.getComponentPanel().getComponent(1),
				1, 0, LayoutUtils.REMAINDER, LayoutUtils.REMAINDER,
				LayoutUtils.HORIZONTAL, LayoutUtils.EAST, 1.0d, 0.0d,
				0, 0, 0, 10);

		LayoutUtils.constrain(panel, panelSub,
				0, iRow++, LayoutUtils.REMAINDER, LayoutUtils.REMAINDER,
				LayoutUtils.HORIZONTAL, LayoutUtils.CENTER, 1.0d, 0.0d,
				3, 10, 10, 10);

		panel.setPreferredSize(new Dimension(650, 510));
	}

	//
	// Protected Methods
	//

	/**
	 * Refresh the functional group definitions from either the default or
	 * a custom definition file. This is called after all settings have
	 * been loaded into the dialog.
	 * {@inheritDoc}
	 */
	@Override
	public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
			final DataTableSpec[] specs) throws NotConfigurableException {
		refreshFunctionalGroupDefinitions();
	}

	/**
	 * Loads the functional group settings from the custom file or from the
	 * default definition file and refreshes the condition table with the data.
	 */
	protected void refreshFunctionalGroupDefinitions() {
		try {
			m_modelConditions.updateConditions(
					FunctionalGroupFilterV2NodeModel.createDefinitionsFromFile(m_modelGroupConfigPath));
			m_compGroupConfigPath.setFileError(false);
		}
		catch (final InvalidSettingsException exc) {
			// Occurs if the specified file cannot be accessed
			// (e.g. not existing, no permissions, etc.)
			m_compGroupConfigPath.setFileError(true);
		}
	}

	/**
	 * Creates the renderer that shall be used in the qualifier column.
	 * 
	 * @return Table cell renderer.
	 */
	protected TableCellRenderer createCenteredCellRenderer() {
		final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
		renderer.setHorizontalAlignment(SwingConstants.CENTER);
		return renderer;
	}

	/**
	 * Creates the editor that shall be used in the qualifier column.
	 * We use a combo-box that contains the qualifiers.
	 * 
	 * @return Table cell renderer.
	 */
	protected TableCellEditor createQualifierCellEditor() {
		final JComboBox<Qualifier> comboBox = new JComboBox<>();
		comboBox.setRenderer(new DefaultListCellRenderer());
		((DefaultListCellRenderer)comboBox.getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);

		// Adds the list of qualifiers to the drop-down
		for (final Qualifier qualifier : Qualifier.values()) {
			comboBox.addItem(qualifier);
		}

		return new DefaultCellEditor(comboBox);
	}

	/**
	 * Creates the editor that shall be used in the count column.
	 * We use a spinner editor.
	 * 
	 * @return Table cell renderer.
	 */
	protected TableCellEditor createCountCellEditor() {
		return new SpinnerEditor(new SpinnerNumberModel(0, 0, 100, 1));
	}

	//
	// Private Methods
	//

	/**
	 * Creates the table component for configuring the conditions.
	 * 
	 * @return Dialog component table.
	 */
	private DialogComponentTable createConditionsTable() {
		final DialogComponentTable tableComp = new DialogComponentTable(
				m_modelConditions, null) {
			@Override
			public String getToolTipTextForCell(final int iRow, final int iCol) {
				String strTooltip = null;

				if (iCol == SettingsModelFunctionalGroupConditions.COLUMN_DISPLAY_NAME) {
					strTooltip = m_modelConditions.getTooltip(iRow);
				}

				return strTooltip;
			}
		};
		tableComp.setMaxColumnWidths(60, -1, 50, 50);

		final JTable table = tableComp.getTable();
		table.setCellSelectionEnabled(false);
		table.setColumnSelectionAllowed(false);
		table.setRowHeight(20);
		final TableCellRenderer headerRenderer =
				table.getTableHeader().getDefaultRenderer();
		if (headerRenderer instanceof JLabel) {
			((JLabel)headerRenderer).setHorizontalAlignment(SwingConstants.CENTER);
		}
		tableComp.getColumn(SettingsModelFunctionalGroupConditions.
				COLUMN_QUALIFIER).setCellRenderer(createCenteredCellRenderer());
		tableComp.getColumn(SettingsModelFunctionalGroupConditions.
				COLUMN_QUALIFIER).setCellEditor(createQualifierCellEditor());
		tableComp.getColumn(SettingsModelFunctionalGroupConditions.
				COLUMN_COUNT).setCellRenderer(createCenteredCellRenderer());
		tableComp.getColumn(SettingsModelFunctionalGroupConditions.
				COLUMN_COUNT).setCellEditor(createCountCellEditor());

		final JPopupMenu contextMenu = new JPopupMenu("Helpers ...");
		final JMenuItem itemActivateAll = new JMenuItem("Activate All");
		final JMenuItem itemDeactivateAll = new JMenuItem("Deactivate All");
		final JMenuItem itemResetAll = new JMenuItem("Reset Everything");
		contextMenu.add(itemActivateAll);
		contextMenu.add(itemDeactivateAll);
		contextMenu.add(itemResetAll);
		itemActivateAll.addActionListener(e -> m_modelConditions.setAllActivated(true));
		itemDeactivateAll.addActionListener(e -> m_modelConditions.setAllActivated(false));
		itemResetAll.addActionListener(e -> m_modelConditions.resetAll());

		table.setComponentPopupMenu(contextMenu);

		return tableComp;
	}

	//
	// Static Methods
	//

	/**
	 * Creates the settings model to be used for the input column.
	 * 
	 * @return Settings model for input column selection.
	 */
	static SettingsModelString createInputColumnNameModel() {
		return new SettingsModelString("input_column", null);
	}

	/**
	 * Creates the settings model to be used for the input file path selection.
	 *
	 * @param nodeCreationConfig Node Creation Configuration instance.
	 *                           Mustn't be null.
	 * @return Settings model for input file path selection.
	 * @throws IllegalArgumentException When {@code nodeCreationConfig} parameter is null.
	 */
	static SettingsModelReaderFileChooser createInputPathModel(NodeCreationConfiguration nodeCreationConfig) {
		if (nodeCreationConfig == null) {
			throw new IllegalArgumentException("Node Creation Configuration parameter must not be null.");
		}

		final SettingsModelReaderFileChooser modelResult = new SettingsModelReaderFileChooser(
				"group_definition_file",
				nodeCreationConfig.getPortConfig().orElseThrow(IllegalStateException::new),
				FunctionalGroupFilterV2NodeFactory.INPUT_PORT_GRP_ID_FS_CONNECTION,
				EnumConfig.create(
						SettingsModelFilterMode.FilterMode.FILE
				)
        );

		nodeCreationConfig.getURLConfig().ifPresent(urlConfiguration ->
				modelResult.setLocation(FSLocationUtil.createFromURL(urlConfiguration.getUrl().toString()))
		);

		return modelResult;
	}

    /**
     * Creates the settings model defining function group filter conditions.
     *
     * @param bCacheOldSettings Set to true when instantiated by the dialog.
     *                          This will cache old settings when switching between different
     *                          definition files.
     * @return Settings model for definition of functional group filter conditions.
     */
	@SuppressWarnings("SameParameterValue")
    static SettingsModelFunctionalGroupConditions createFunctionalGroupConditionsModel(final boolean bCacheOldSettings) {
		return new SettingsModelFunctionalGroupConditions("conditions", bCacheOldSettings);
	}

	/**
	 * Creates the settings model to be used to determine, if the pattern
	 * that failed to match is getting recorded in a new column.
	 * 
	 * @return Settings model for option to record failed pattern.
	 */
	static SettingsModelBoolean createRecordFailedPatternOptionModel() {
		return new SettingsModelBoolean("record_pattern", false);
	}

	/**
	 * Creates the settings model to be used to specify the new column name
	 * for the failed pattern (only used if record failed patter option is
	 * enabled).
	 * 
	 * @param modelRecordFailedPatterns Model that determines, if the
	 * 		new column name field is enabled or disabled.
	 * 
	 * @return Settings model for failed pattern column name.
	 */
	static SettingsModelString createNewFailedPatternColumnNameModel(
			final SettingsModelBoolean modelRecordFailedPatterns) {
		final SettingsModelString result =
				new SettingsModelString("failed_pattern_column_name", null);
		modelRecordFailedPatterns.addChangeListener(e -> result.setEnabled(modelRecordFailedPatterns.getBooleanValue()));
		result.setEnabled(modelRecordFailedPatterns.getBooleanValue());
		return result;
	}
}
