/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2013-2023
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
package org.rdkit.knime.nodes.structurenormalizer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentMultiLineString;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.filehandling.core.connections.FSLocationUtil;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.DialogComponentWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode;
import org.rdkit.knime.util.DialogComponentColumnNameSelection;
import org.rdkit.knime.util.DialogComponentConfigFileSelection;
import org.rdkit.knime.util.DialogComponentEnumFilterPanel;
import org.rdkit.knime.util.DialogComponentLogFileSelection;
import org.rdkit.knime.util.LayoutUtils;
import org.rdkit.knime.util.SettingsModelEnumerationArray;

/**
 * {@code NodeDialog} for the "RDKitStructureNormalizer" Node.
 * <br><br>
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Manuel Schwarze
 * @author Roman Balabanov
 */
public class RDKitStructureNormalizerV2NodeDialog extends DefaultNodeSettingsPane {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitStructureNormalizerV2NodeDialog.class);

	//
	// Members
	//

	/** The model for the input column name. */
	private final SettingsModelString m_modelInputColumnName;

	/** The model for the output column name Passed Corrected Structure. */
	private final SettingsModelString m_modelOutputPassedCorrectedStructureName;

	/** The model for the output column name Passed Flag. */
	private final SettingsModelString m_modelOutputPassedFlagName;

	/** The model for the output column name Passed Warning. */
	private final SettingsModelString m_modelOutputPassedWarningMessagesName;

	/** The model for the output column name Failed Flag. */
	private final SettingsModelString m_modelOutputFailedFlagName;

	/** The model for the output column name Failed Error. */
	private final SettingsModelString m_modelOutputFailedErrorMessagesName;

	/** The model for setting the transformation configuration file. */
	private final SettingsModelReaderFileChooser m_modelTransformationConfigurationPath;

	/** The GUI component to select the transformation configuration file path. */
	private final DialogComponentConfigFileSelection m_compTransformationConfigurationPath;

	/** The model for setting the augmented atoms configuration file. */
	private final SettingsModelReaderFileChooser m_modelAugmentedAtomsConfigurationPath;

	/** The GUI component to select the augmented atoms configuration file path. */
	private final DialogComponentConfigFileSelection m_compAugmentedAtomsConfigurationPath;

	/** Remembers the last selected input column name. */
	private String m_strLastInputColumnName;

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
	@SuppressWarnings({"ReassignedVariable", "UnusedAssignment"})
	RDKitStructureNormalizerV2NodeDialog(NodeCreationConfiguration nodeCreationConfig) {
		int iPortInputTable = RDKitStructureNormalizerV2NodeModel.getInputTablePortIndexes(nodeCreationConfig,
				RDKitStructureNormalizerV2NodeFactory.INPUT_PORT_GRP_ID_INPUT_TABLE)[0];

		createNewGroup("Input");
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				m_modelInputColumnName = createInputColumnNameModel(), "SDF, SMILES or RDKit Mol column: ", iPortInputTable,
				SdfValue.class, SmilesValue.class));

		createNewGroup("Passed Output");
		super.addDialogComponent(new DialogComponentString(
				m_modelOutputPassedCorrectedStructureName = createPassedCorrectedStructureColumnNameModel(),
				"Corrected structure column name: ", true, 50));
		super.addDialogComponent(new DialogComponentString(
				m_modelOutputPassedFlagName = createPassedFlagsColumnNameModel(),
				"Flags column name: ", true, 50));
		super.addDialogComponent(new DialogComponentString(
				m_modelOutputPassedWarningMessagesName = createPassedWarningMessagesColumnNameModel(),
				"Warning messages column name: ", true, 50));

		createNewGroup("Failed Output");
		super.addDialogComponent(new DialogComponentString(
				m_modelOutputFailedFlagName = createFailedFlagsColumnNameModel(),
				"Flags column name: ", true, 50));
		super.addDialogComponent(new DialogComponentString(
				m_modelOutputFailedErrorMessagesName = createFailedErrorMessagesColumnNameModel(),
				"Error messages column name: ", true, 50));

		closeCurrentGroup();
		final SettingsModelWriterFileChooser modelLogPath = createLogPathModel(nodeCreationConfig);
		final DialogComponentWriterFileChooser fileChooser = new DialogComponentWriterFileChooser(
				modelLogPath,
				"LogFileHistory",
				createFlowVariableModel(modelLogPath.getKeysForFSLocation(), FSLocationVariableType.INSTANCE)
		);
		super.addDialogComponent(fileChooser);
		final DialogComponentLogFileSelection compLogFileChooser = new DialogComponentLogFileSelection(
				fileChooser
		);
		super.addDialogComponent(compLogFileChooser);

		m_modelInputColumnName.addChangeListener(e -> {
            final String strNewInputColumnName = m_modelInputColumnName.getStringValue();

            // When the input column change auto-update the output column, if they start with the old input column name
            updateOutputColumnName(m_modelOutputPassedCorrectedStructureName,
                    m_strLastInputColumnName, strNewInputColumnName,
                    RDKitStructureNormalizerV2NodeModel.DEFAULT_POSTFIX_PASSED_CORRECTED);
            updateOutputColumnName(m_modelOutputPassedFlagName,
                    m_strLastInputColumnName, strNewInputColumnName,
                    RDKitStructureNormalizerV2NodeModel.DEFAULT_POSTFIX_PASSED_FLAGS);
            updateOutputColumnName(m_modelOutputPassedWarningMessagesName,
                    m_strLastInputColumnName, strNewInputColumnName,
                    RDKitStructureNormalizerV2NodeModel.DEFAULT_POSTFIX_PASSED_WARNINGS);
            updateOutputColumnName(m_modelOutputFailedFlagName,
                    m_strLastInputColumnName, strNewInputColumnName,
                    RDKitStructureNormalizerV2NodeModel.DEFAULT_POSTFIX_FAILED_FLAGS);
            updateOutputColumnName(m_modelOutputFailedErrorMessagesName,
                    m_strLastInputColumnName, strNewInputColumnName,
                    RDKitStructureNormalizerV2NodeModel.DEFAULT_POSTFIX_FAILED_ERRORS);

            // Remember this change for later
            m_strLastInputColumnName = strNewInputColumnName;
        });

		final JPanel panelOptions = (JPanel) getTab("Options");
		panelOptions.setBorder(
				BorderFactory.createCompoundBorder(
						BorderFactory.createEmptyBorder(15, 15, 15, 15),
						panelOptions.getBorder()
				)
		);
		panelOptions.setPreferredSize(new Dimension(880, 450));

		createNewTab("Handling Failures");
		createNewTab("Advanced");

		// Component to define switches for StruCheck
		final DialogComponentEnumFilterPanel<StruCheckSwitch> compSwitches =
                new DialogComponentEnumFilterPanel<>(createSwitchOptionsModel(), null, null, true);
		compSwitches.setExcludeTitle("Not Used");
		compSwitches.setExcludeBorderColor(Color.RED);
		compSwitches.setIncludeTitle("To Be Used");
		compSwitches.setIncludeBorderColor(Color.GREEN);
		compSwitches.setSearchVisible(false);
		compSwitches.setListCellRenderer(new DefaultListCellRenderer() {

			//
			// Constants
			//

			/** Serial number. */
			@Serial
			private static final long serialVersionUID = -3432112669821820183L;

			//
			// Public Methods
			//

			@Override
			public Component getListCellRendererComponent(
					final JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus) {
				// The super method will reset the icon if we call this method
				// last. So we let super do its job first, and then we take care
				// that everything is properly set.
				final Component c =  super.getListCellRendererComponent(list, value, index,
						isSelected, cellHasFocus);

				assert (c == this);

				if (value instanceof StruCheckSwitch switchCode) {
					// Set text
					setText(switchCode.toString());

					// Set tooltip
					final String strTooltip = "<html><body><b>" + switchCode.name() + "</b> - " +
							switchCode.getLongDescription() + "</body></html>";

					list.setToolTipText(strTooltip);
				}

				return this;
			}
		});

		super.addDialogComponent(compSwitches);

		final DialogComponentMultiLineString compAdvancedOptions =
				new DialogComponentMultiLineString(createAdvancedOptionsModel(), "Additional options:", false, 40, 5);
		compAdvancedOptions.getComponentPanel()
				.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		super.addDialogComponent(compAdvancedOptions);

		// Create components for defining the transformation configuration file
		m_modelTransformationConfigurationPath = createTransformationConfigurationPathModel(nodeCreationConfig);
		m_modelTransformationConfigurationPath.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				checkConfigurationFile(m_modelTransformationConfigurationPath,
						RDKitStructureNormalizerV2NodeModel.DEFAULT_TRANSFORMATION_CONFIGURATION_FILE,
						m_compTransformationConfigurationPath);
			}
		});

		final DialogComponentReaderFileChooser compTransformationsConfigurationFileChooser = new DialogComponentReaderFileChooser(
				m_modelTransformationConfigurationPath,
				"TransformationConfigurationFileHistory",
				createFlowVariableModel(m_modelTransformationConfigurationPath.getKeysForFSLocation(), FSLocationVariableType.INSTANCE)
		);
		super.addDialogComponent(compTransformationsConfigurationFileChooser);
		m_compTransformationConfigurationPath = new DialogComponentConfigFileSelection(
				compTransformationsConfigurationFileChooser,
				"Transformation Configuration",
				modelFileChooser -> RDKitStructureNormalizerV2NodeModel.getConfiguration(modelFileChooser,
						RDKitStructureNormalizerV2NodeModel.DEFAULT_TRANSFORMATION_CONFIGURATION_FILE)
		);
		super.addDialogComponent(m_compTransformationConfigurationPath);

		// Create components for defining the augmented atoms configuration file
		m_modelAugmentedAtomsConfigurationPath = createAugmentedAtomsConfigurationPathModel(nodeCreationConfig);
		m_modelAugmentedAtomsConfigurationPath.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				checkConfigurationFile(m_modelAugmentedAtomsConfigurationPath,
						RDKitStructureNormalizerV2NodeModel.DEFAULT_AUGMENTED_ATOMS_CONFIGURATION_FILE,
						m_compAugmentedAtomsConfigurationPath);
			}
		});

		final DialogComponentReaderFileChooser compAugmentedAtomsConfigurationFileChooser = new DialogComponentReaderFileChooser(
				m_modelAugmentedAtomsConfigurationPath,
				"AugmentedAtomsConfigurationFileHistory",
				createFlowVariableModel(m_modelAugmentedAtomsConfigurationPath.getKeysForFSLocation(), FSLocationVariableType.INSTANCE)
		);
		super.addDialogComponent(compAugmentedAtomsConfigurationFileChooser);
		m_compAugmentedAtomsConfigurationPath = new DialogComponentConfigFileSelection(
				compAugmentedAtomsConfigurationFileChooser,
				"Augmented Atoms Configuration",
				modelFileChooser -> RDKitStructureNormalizerV2NodeModel.getConfiguration(modelFileChooser,
						RDKitStructureNormalizerV2NodeModel.DEFAULT_AUGMENTED_ATOMS_CONFIGURATION_FILE)
		);
		super.addDialogComponent(m_compAugmentedAtomsConfigurationPath);

		// Component to define additional failure treatments
		final DialogComponentEnumFilterPanel<StruCheckCode> compFailureCodes = new DialogComponentEnumFilterPanel<>
                (createAdditionalFailureCodesConfigurationModel(),
                        "<HTML><BODY>Define here what warning flags shall be considered a failure and should be put into table 2:<BR>" +
                                "(Hover your mouse over a code to show a description and the code value)</BODY></HTML>",
                        getDefaultNonErrorCodes(), true);
		compFailureCodes.setExcludeTitle("Treat as Warning");
		compFailureCodes.setExcludeBorderColor(Color.ORANGE);
		compFailureCodes.setIncludeTitle("Treat as Failure");
		compFailureCodes.setIncludeBorderColor(Color.RED);
		compFailureCodes.setSearchVisible(false);
		compFailureCodes.setListCellRenderer(new DefaultListCellRenderer() {

			//
			// Constants
			//

			/** Serial number. */
			@Serial
			private static final long serialVersionUID = -3432112669822820183L;

			//
			// Public Methods
			//

			@Override
			public Component getListCellRendererComponent(
					final JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus) {
				// The super method will reset the icon if we call this method
				// last. So we let super do its job first, and then we take care
				// that everything is properly set.
				final Component c =  super.getListCellRendererComponent(list, value, index,
						isSelected, cellHasFocus);

				assert (c == this);

				if (value instanceof StruCheckCode code) {
					// Set text
					setText(code.toString());

					// Set tooltip
					String strTooltip = "<html>" +
							(code.getMessage() + " (" + code.getValue() + ")").
									replace("<=", "&le;").
									replace(">=", "&ge;").
									replace("<", "&lt;").
									replace(">", "&gt;").
									replace("\n", "<br>") +
							"</html>";

                    list.setToolTipText(strTooltip);
				}

				return this;
			}
		});

		super.addDialogComponent(compFailureCodes);

		// Relayout the Configuration components
		final JPanel panel = (JPanel)getTab("Advanced");
		panel.setLayout(new GridBagLayout());
		panel.removeAll();

		final JPanel panelSubSwitches = new JPanel(new GridBagLayout());
		panelSubSwitches.setPreferredSize(new Dimension(400, 300));
		compSwitches.getComponentPanel().setPreferredSize(new Dimension(350, 200));

		panelSubSwitches.setBorder(BorderFactory.createTitledBorder("Advanced Settings (Optional)"));
		LayoutUtils.constrain(panelSubSwitches, compSwitches.getComponentPanel(),
				0, 0, 1, 1,
				LayoutUtils.BOTH, LayoutUtils.CENTER, 1.0d, 1.0d,
				0, 10, 0, 7);
		LayoutUtils.constrain(panelSubSwitches, compAdvancedOptions.getComponentPanel(),
				0, 1, 1, LayoutUtils.REMAINDER,
				LayoutUtils.BOTH, LayoutUtils.CENTER, 1.0d, 1.0d,
				0, 10, 0, 7);

		int iRow = 0;
		LayoutUtils.constrain(panel, m_compTransformationConfigurationPath.getComponentPanel(),
				0, iRow++, LayoutUtils.REMAINDER, 1,
				LayoutUtils.HORIZONTAL, LayoutUtils.CENTER, 1.0d, 0.0d,
				7, 7, 0, 7);
		LayoutUtils.constrain(panel, m_compAugmentedAtomsConfigurationPath.getComponentPanel(),
				0, iRow++, LayoutUtils.REMAINDER, 1,
				LayoutUtils.HORIZONTAL, LayoutUtils.CENTER, 1.0d, 0.0d,
				7, 7, 0, 7);
		LayoutUtils.constrain(panel, panelSubSwitches,
				0, iRow++, LayoutUtils.REMAINDER, LayoutUtils.REMAINDER,
				LayoutUtils.BOTH, LayoutUtils.CENTER, 1.0d, 1.0d,
				7, 7, 7, 7);

		// "Handling Failures" TAB
		final JPanel panelFailures = (JPanel)getTab("Handling Failures");
		panelFailures.setLayout(new BorderLayout());
		panelFailures.removeAll();
		panelFailures.setBorder(
				BorderFactory.createCompoundBorder(
						BorderFactory.createEmptyBorder(15, 15, 15, 15),
						panelFailures.getBorder()
				)
		);

		final JPanel panelSubSpecialFailures = new JPanel(new GridBagLayout());
		panelSubSpecialFailures.setPreferredSize(new Dimension(400, 250));
		panelSubSpecialFailures.setBorder(BorderFactory.createTitledBorder("Special Failures (Optional)"));
		LayoutUtils.constrain(panelSubSpecialFailures, compFailureCodes.getComponentPanel(),
				0, 0, 1, LayoutUtils.REMAINDER,
				LayoutUtils.BOTH, LayoutUtils.CENTER, 1.0d, 1.0d,
				0, 10, 0, 7);

		panelFailures.add(panelSubSpecialFailures, BorderLayout.NORTH);

		LayoutUtils.correctKnimeDialogBorders(getPanel());
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
		m_strLastInputColumnName = m_modelInputColumnName.getStringValue();
		checkConfigurationFile(m_modelTransformationConfigurationPath,
				RDKitStructureNormalizerV2NodeModel.DEFAULT_TRANSFORMATION_CONFIGURATION_FILE,
				m_compTransformationConfigurationPath);
		checkConfigurationFile(m_modelAugmentedAtomsConfigurationPath,
				RDKitStructureNormalizerV2NodeModel.DEFAULT_AUGMENTED_ATOMS_CONFIGURATION_FILE,
				m_compAugmentedAtomsConfigurationPath);
	}

	/**
	 * Loads the functional group settings from the custom file or from the
	 * default definition file and refreshes the condition table with the data.
	 * 
	 * @param compFileInput Component to show a special error border.
	 */
	protected void checkConfigurationFile(final SettingsModelReaderFileChooser modelFile, final String strDefaultResource,
			final DialogComponentConfigFileSelection compFileInput) {
		try {
			RDKitStructureNormalizerV2NodeModel.getConfiguration(modelFile,
					strDefaultResource);
			compFileInput.setFileError(false);
		}
		catch (final InvalidSettingsException exc) {
			// Occurs if the specified file cannot be accessed
			// (e.g. not existing, no permissions, etc.)
			compFileInput.setFileError(true);
		}
	}

	protected void updateOutputColumnName(final SettingsModelString model,
			final String strOldInputColumnName, final String strNewInputColumnName, final String strDefaultPostfix) {
		if (model != null && strOldInputColumnName != null && strNewInputColumnName != null) {
			final String str = model.getStringValue();
			final String strOldPrefix = strOldInputColumnName + " - ";
			if (str == null || str.startsWith(strOldPrefix)) {
				model.setStringValue(strNewInputColumnName + " - " +
						(str == null ? strDefaultPostfix : str.substring(strOldPrefix.length())));
			}
		}
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
	 * Creates the settings model to be used for a passed output column.
	 * 
	 * @return Settings model for passed output column with corrected structure.
	 */
	static SettingsModelString createPassedCorrectedStructureColumnNameModel() {
		return new SettingsModelString("passed_corrected_structure_column", null);
	}

	/**
	 * Creates the settings model to be used for a passed error flags output column.
	 * 
	 * @return Settings model for passed output column with error flags.
	 */
	static SettingsModelString createPassedFlagsColumnNameModel() {
		return new SettingsModelString("passed_flags_column", null);
	}

	/**
	 * Creates the settings model to be used for a passed warning flags output column.
	 * 
	 * @return Settings model for passed output column with warning flags.
	 */
	static SettingsModelString createPassedWarningMessagesColumnNameModel() {
		return new SettingsModelString("passed_warning_messages_column", null);
	}

	/**
	 * Creates the settings model to be used for a failed error flags output column.
	 * 
	 * @return Settings model for failed output column with error flags.
	 */
	static SettingsModelString createFailedFlagsColumnNameModel() {
		return new SettingsModelString("failed_error_flags_column", null);
	}

	/**
	 * Creates the settings model to be used for a failed error messages output column.
	 * 
	 * @return Settings model for failed output column with error messages.
	 */
	static SettingsModelString createFailedErrorMessagesColumnNameModel() {
		return new SettingsModelString("failed_error_messages_column", null);
	}

	/**
	 * Creates the settings model to be used for defining switches for StruCheck.
	 * 
	 * @return Settings model for StruCheck switches.
	 */
	static SettingsModelEnumerationArray<StruCheckSwitch> createSwitchOptionsModel() {
		return new SettingsModelEnumerationArray<>(StruCheckSwitch.class, "switches",
                RDKitStructureNormalizerV2NodeModel.DEFAULT_SWITCHES);
	}

	/**
	 * Creates the settings model to be used for defining advanced switches for StruCheck.
	 * 
	 * @return Settings model for StruCheck switches.
	 */
	static SettingsModelString createAdvancedOptionsModel() {
		return new SettingsModelString("advanced_options",
				RDKitStructureNormalizerV2NodeModel.DEFAULT_ADVANCED_OPTIONS);
	}

	/**
	 * Creates the settings model to be used to specify what non error codes (usually
	 * transformation / warning codes) shall be treated as failures to make rows end
	 * up in the second output table for failed corrections.
	 * 
	 * @return Settings model for result column name.
	 */
	static SettingsModelEnumerationArray<StruCheckCode> createAdditionalFailureCodesConfigurationModel() {
		return new SettingsModelEnumerationArray<>(
                StruCheckCode.class, "additional_failure_codes", new StruCheckCode[0]);
	}

	/**
	 * Creates the settings model to be used to specify the transformation configuration.
	 *
	 * @param nodeCreationConfig Node Creation Configuration instance.
	 *                           Mustn't be null.
	 * @return Settings model for transformation configuration.
	 * @throws IllegalArgumentException When {@code nodeCreationConfig} parameter is null.
	 */
	static SettingsModelReaderFileChooser createTransformationConfigurationPathModel(NodeCreationConfiguration nodeCreationConfig) {
		if (nodeCreationConfig == null) {
			throw new IllegalArgumentException("Node Creation Configuration parameter must not be null.");
		}

		final SettingsModelReaderFileChooser modelResult = new SettingsModelReaderFileChooser(
				"transformation_configuration_file",
				nodeCreationConfig.getPortConfig().orElseThrow(IllegalStateException::new),
				RDKitStructureNormalizerV2NodeFactory.INPUT_PORT_GRP_ID_FS_CONNECTION,
				EnumConfig.create(
						SettingsModelFilterMode.FilterMode.FILE
				),
                ".trn"
		);

		nodeCreationConfig.getURLConfig().ifPresent(urlConfiguration ->
				modelResult.setLocation(FSLocationUtil.createFromURL(urlConfiguration.getUrl().toString()))
		);

		return modelResult;
	}

	/**
	 * Creates the settings model to be used to specify the augmented atoms' configuration.
	 *
	 * @param nodeCreationConfig Node Creation Configuration instance.
	 *                           Mustn't be null.
	 * @return Settings model for augmented atoms configuration.
	 * @throws IllegalArgumentException When {@code nodeCreationConfig} parameter is null.
	 */
	static SettingsModelReaderFileChooser createAugmentedAtomsConfigurationPathModel(NodeCreationConfiguration nodeCreationConfig) {
		if (nodeCreationConfig == null) {
			throw new IllegalArgumentException("Node Creation Configuration parameter must not be null.");
		}

		final SettingsModelReaderFileChooser modelResult = new SettingsModelReaderFileChooser(
				"augmented_atoms_configuration_file",
				nodeCreationConfig.getPortConfig().orElseThrow(IllegalStateException::new),
				RDKitStructureNormalizerV2NodeFactory.INPUT_PORT_GRP_ID_FS_CONNECTION,
				EnumConfig.create(
						SettingsModelFilterMode.FilterMode.FILE
				),
				".chk"
		);

		nodeCreationConfig.getURLConfig().ifPresent(urlConfiguration ->
				modelResult.setLocation(FSLocationUtil.createFromURL(urlConfiguration.getUrl().toString()))
		);

		return modelResult;
	}

	/**
	 * Creates the settings model to be used to specify the log file.
	 *
	 * @param nodeCreationConfig Node Creation Configuration instance.
	 *                           Mustn't be null.
	 * @return Settings model for StruCheck configuration.
	 * @throws IllegalArgumentException When {@code nodeCreationConfig} parameter is null.
	 */
	static SettingsModelWriterFileChooser createLogPathModel(NodeCreationConfiguration nodeCreationConfig) {
		if (nodeCreationConfig == null) {
			throw new IllegalArgumentException("Node Creation Configuration parameter must not be null.");
		}

		final SettingsModelWriterFileChooser modelResult = new SettingsModelWriterFileChooser(
				"log_file",
				nodeCreationConfig.getPortConfig().orElseThrow(IllegalStateException::new),
				RDKitStructureNormalizerV2NodeFactory.INPUT_PORT_GRP_ID_FS_CONNECTION,
				EnumConfig.create(
						SettingsModelFilterMode.FilterMode.FILE
				),
				EnumConfig.create(
						FileOverwritePolicy.FAIL,
						FileOverwritePolicy.OVERWRITE
				),
				".log"
		);

		nodeCreationConfig.getURLConfig().ifPresent(urlConfiguration ->
				modelResult.setLocation(FSLocationUtil.createFromURL(urlConfiguration.getUrl().toString()))
		);

		return modelResult;
	}

	/**
	 * Defines the default non error codes list.
	 * 
	 * @return Non error codes list.
	 */
	static List<StruCheckCode> getDefaultNonErrorCodes() {
		final List<StruCheckCode> listDefaultNonErrorCodes = new ArrayList<>();

		for (final StruCheckCode code : StruCheckCode.values()) {
			if (!code.isError()) {
				listDefaultNonErrorCodes.add(code);
			}
		}

		return listDefaultNonErrorCodes;
	}
}
