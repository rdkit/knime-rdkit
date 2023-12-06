/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2013-2023
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
package org.rdkit.knime.nodes.structurenormalizer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentMultiLineString;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.util.DialogComponentColumnNameSelection;
import org.rdkit.knime.util.DialogComponentEnumFilterPanel;
import org.rdkit.knime.util.LayoutUtils;
import org.rdkit.knime.util.SettingsModelEnumerationArray;

/**
 * <code>NodeDialog</code> for the "RDKitStructureNormalizer" Node.
 * 
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Manuel Schwarze
 */
@Deprecated
public class RDKitStructureNormalizerNodeDialog extends DefaultNodeSettingsPane {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitStructureNormalizerNodeDialog.class);

	/** Button image for showing definition info. */
	private static final Icon INFO_ICON = LayoutUtils.createImageIcon(
			RDKitStructureNormalizerNodeDialog.class,
			"/org/rdkit/knime/nodes/structurenormalizer/info.png", null);

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

	/** An error border to be shown, if the custom definition file name is not existing. */
	private final Border m_borderInputFileError = BorderFactory.createLineBorder(Color.RED);

	/** The model for setting the transformation configuration file. */
	private final SettingsModelString m_modelTransformationConfigurationFile;

	/** The GUI component to select the transformation configuration file. */
	private final JComponent m_compTransformationConfigurationFile;

	/** The button to load default transformation configuration. */
	private final JButton m_btnLoadDefaultTransformations;

	/** The button to show current transformation configuration. */
	private final JButton m_btnShowTransformations;

	/** The model for setting the augmented atoms configuration file. */
	private final SettingsModelString m_modelAugmentedAtomsConfigurationFile;

	/** The button to load default augmented atoms configuration. */
	private final JButton m_btnLoadDefaultAugmentedAtoms;

	/** The button to show current augmented atoms configuration. */
	private final JButton m_btnShowAugmentedAtomsConfiguration;

	/** The GUI component to select the augmented atoms configuration file. */
	private final JComponent m_compAugmentedAtomsConfigurationFile;

	/** Remembers the last selected input column name. */
	private String m_strLastInputColumnName;

	//
	// Constructor
	//

	/**
	 * Create a new dialog pane with default components to configure an input column,
	 * the name of a new column, which will contain the calculation results, an option
	 * to tell, if the source column shall be removed from the result table.
	 */
	RDKitStructureNormalizerNodeDialog() {
		createNewGroup("Input");
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				m_modelInputColumnName = createInputColumnNameModel(), "SDF, SMILES or RDKit Mol column: ", 0,
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

		createNewGroup("Logfile Output (Optional)");
		final DialogComponentFileChooser compLogFile = new DialogComponentFileChooser(
				createLogFileModel(), "LogFileHistory", JFileChooser.SAVE_DIALOG, ".log") {
			@Override
			protected void validateSettingsBeforeSave() throws InvalidSettingsException {
				try {
					super.validateSettingsBeforeSave();
				}
				catch (final InvalidSettingsException exc) {
					// Ignore empty file names
				}
			}
		};
		super.addDialogComponent(compLogFile);
		super.addDialogComponent(new DialogComponentBoolean(
				createOverwriteOptionModel(), "Overwrite if file exists"));

		m_modelInputColumnName.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				final String strNewInputColumnName = m_modelInputColumnName.getStringValue();

				// When the input column change auto-update the output column, if they start with the old input column name
				updateOutputColumnName(m_modelOutputPassedCorrectedStructureName,
						m_strLastInputColumnName, strNewInputColumnName,
						RDKitStructureNormalizerNodeModel.DEFAULT_POSTFIX_PASSED_CORRECTED);
				updateOutputColumnName(m_modelOutputPassedFlagName,
						m_strLastInputColumnName, strNewInputColumnName,
						RDKitStructureNormalizerNodeModel.DEFAULT_POSTFIX_PASSED_FLAGS);
				updateOutputColumnName(m_modelOutputPassedWarningMessagesName,
						m_strLastInputColumnName, strNewInputColumnName,
						RDKitStructureNormalizerNodeModel.DEFAULT_POSTFIX_PASSED_WARNINGS);
				updateOutputColumnName(m_modelOutputFailedFlagName,
						m_strLastInputColumnName, strNewInputColumnName,
						RDKitStructureNormalizerNodeModel.DEFAULT_POSTFIX_FAILED_FLAGS);
				updateOutputColumnName(m_modelOutputFailedErrorMessagesName,
						m_strLastInputColumnName, strNewInputColumnName,
						RDKitStructureNormalizerNodeModel.DEFAULT_POSTFIX_FAILED_ERRORS);

				// Remember this change for later
				m_strLastInputColumnName = strNewInputColumnName;
			}
		});

		createNewTab("Handling Failures");
		createNewTab("Advanced");

		// Component to define switches for StruCheck
		final DialogComponentEnumFilterPanel<StruCheckSwitch> compSwitches =
				new DialogComponentEnumFilterPanel<StruCheckSwitch>(createSwitchOptionsModel(), null, null, true);
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
			private static final long serialVersionUID = -3432112669821820183L;

			//
			// Public Methods
			//

			/**
			 * {@inheritDoc}
			 */
			@Override
			public Component getListCellRendererComponent(
					final JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus) {
				// The super method will reset the icon if we call this method
				// last. So we let super do its job first and then we take care
				// that everything is properly set.
				final Component c =  super.getListCellRendererComponent(list, value, index,
						isSelected, cellHasFocus);

				assert (c == this);

				if (value instanceof StruCheckSwitch) {
					final StruCheckSwitch switchCode = (StruCheckSwitch)value;

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
		super.addDialogComponent(compAdvancedOptions);

		// Create components for defining the transformation configuration file
		m_modelTransformationConfigurationFile = createTransformationConfigurationFileModel();
		m_modelTransformationConfigurationFile.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				checkConfigurationFile(m_modelTransformationConfigurationFile,
						RDKitStructureNormalizerNodeModel.DEFAULT_TRANSFORMATION_CONFIGURATION_FILE,
						m_compTransformationConfigurationFile);
			}
		});

		final DialogComponentFileChooser compTransformationsConfigurationFile = new DialogComponentFileChooser(
				m_modelTransformationConfigurationFile, "TransformationConfigurationFileHistory",
				JFileChooser.OPEN_DIALOG);
		super.addDialogComponent(compTransformationsConfigurationFile);
		m_compTransformationConfigurationFile = (JComponent)
				((JPanel)compTransformationsConfigurationFile.getComponentPanel().getComponent(0)).getComponent(0);

		// Create a button to load a default definition file
		m_btnLoadDefaultTransformations = new JButton("Load Defaults");
		m_btnLoadDefaultTransformations.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				m_modelTransformationConfigurationFile.setStringValue(
						RDKitStructureNormalizerNodeModel.DEFAULT_CONFIGURATION_ID);
			}
		});

		m_btnShowTransformations = new JButton(INFO_ICON);
		m_btnShowTransformations.setToolTipText("Show configuration defined here.");
		m_btnShowTransformations.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					final String strConfiguration = RDKitStructureNormalizerNodeModel.getConfiguration(
							m_modelTransformationConfigurationFile.getStringValue(),
							RDKitStructureNormalizerNodeModel.DEFAULT_TRANSFORMATION_CONFIGURATION_FILE);
					onShowConfiguration(strConfiguration, m_modelTransformationConfigurationFile.getStringValue(), "Transformation Configuration");
				}
				catch (final InvalidSettingsException exc) {
					JOptionPane.showMessageDialog(m_btnShowTransformations,
							"The following error occurred: " + exc.getMessage(),
							"Transformation Configuration - Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		// Create components for defining the augmented atoms configuration file
		m_modelAugmentedAtomsConfigurationFile = createAugmentedAtomsConfigurationFileModel();
		m_modelAugmentedAtomsConfigurationFile.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				checkConfigurationFile(m_modelAugmentedAtomsConfigurationFile,
						RDKitStructureNormalizerNodeModel.DEFAULT_AUGMENTED_ATOMS_CONFIGURATION_FILE,
						m_compAugmentedAtomsConfigurationFile);
			}
		});

		final DialogComponentFileChooser compAugmentedAtomsConfigurationFile = new DialogComponentFileChooser(
				m_modelAugmentedAtomsConfigurationFile, "AugmentedAtomsConfigurationFileHistory",
				JFileChooser.OPEN_DIALOG);
		super.addDialogComponent(compAugmentedAtomsConfigurationFile);
		m_compAugmentedAtomsConfigurationFile = (JComponent)
				((JPanel)compAugmentedAtomsConfigurationFile.getComponentPanel().getComponent(0)).getComponent(0);

		// Create a button to load a default definition file
		m_btnLoadDefaultAugmentedAtoms = new JButton("Load Defaults");
		m_btnLoadDefaultAugmentedAtoms.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				m_modelAugmentedAtomsConfigurationFile.setStringValue(
						RDKitStructureNormalizerNodeModel.DEFAULT_CONFIGURATION_ID);			}
		});

		m_btnShowAugmentedAtomsConfiguration = new JButton(INFO_ICON);
		m_btnShowAugmentedAtomsConfiguration.setToolTipText("Show configuration defined here.");
		m_btnShowAugmentedAtomsConfiguration.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					final String strConfiguration = RDKitStructureNormalizerNodeModel.getConfiguration(
							m_modelAugmentedAtomsConfigurationFile.getStringValue(),
							RDKitStructureNormalizerNodeModel.DEFAULT_AUGMENTED_ATOMS_CONFIGURATION_FILE);
					onShowConfiguration(strConfiguration, m_modelAugmentedAtomsConfigurationFile.getStringValue(), "Augmented Atoms Configuration");
				}
				catch (final InvalidSettingsException exc) {
					JOptionPane.showMessageDialog(m_btnShowAugmentedAtomsConfiguration,
							"The following error occurred: " + exc.getMessage(),
							"Augmented Atoms Configuration - Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		// Component to define additional failure treatments
		final DialogComponentEnumFilterPanel<StruCheckCode> compFailureCodes = new DialogComponentEnumFilterPanel<StruCheckCode>
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
			private static final long serialVersionUID = -3432112669822820183L;

			//
			// Public Methods
			//

			/**
			 * {@inheritDoc}
			 */
			@Override
			public Component getListCellRendererComponent(
					final JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus) {
				// The super method will reset the icon if we call this method
				// last. So we let super do its job first and then we take care
				// that everything is properly set.
				final Component c =  super.getListCellRendererComponent(list, value, index,
						isSelected, cellHasFocus);

				assert (c == this);

				if (value instanceof StruCheckCode) {
					final StruCheckCode code = (StruCheckCode)value;

					// Set text
					setText(code.toString());

					// Set tooltip
					String strTooltip = code.getMessage() + " (" + code.getValue() + ")";
					if (strTooltip != null) {
						strTooltip = "<html>" +
								strTooltip.
								replace("<=", "&le;").
								replace(">=", "&ge;").
								replace("<", "&lt;").
								replace(">", "&gt;").
								replace("\n", "<br>") +
								"</html>";
					}

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

		final JPanel panelSubTransConfigFile = new JPanel(new GridBagLayout());

		panelSubTransConfigFile.setBorder(BorderFactory.createTitledBorder("Transformation Configuration File (.trn) (Optional)"));
		LayoutUtils.constrain(panelSubTransConfigFile, m_compTransformationConfigurationFile,
				0, 0, 1, LayoutUtils.REMAINDER,
				LayoutUtils.HORIZONTAL, LayoutUtils.CENTER, 1.0d, 0.0d,
				0, 10, 0, 7);
		LayoutUtils.constrain(panelSubTransConfigFile, ((JPanel)compTransformationsConfigurationFile.getComponentPanel().
				getComponent(0)).getComponent(0), // The browse button
				1, 0, 1, LayoutUtils.REMAINDER,
				LayoutUtils.NONE, LayoutUtils.CENTER, 0.0d, 0.0d,
				0, 0, 0, 7);
		LayoutUtils.constrain(panelSubTransConfigFile, m_btnLoadDefaultTransformations,
				2, 0, 1, LayoutUtils.REMAINDER,
				LayoutUtils.NONE, LayoutUtils.EAST, 0.0d, 0.0d,
				0, 0, 0, 7);
		LayoutUtils.constrain(panelSubTransConfigFile, m_btnShowTransformations,
				3, 0, LayoutUtils.REMAINDER, LayoutUtils.REMAINDER,
				LayoutUtils.NONE, LayoutUtils.EAST, 0.0d, 0.0d,
				0, 0, 0, 10);

		final JPanel panelSubAtomsConfigFile = new JPanel(new GridBagLayout());

		panelSubAtomsConfigFile.setBorder(BorderFactory.createTitledBorder("Augmented Atoms Configuration File (.chk) (Optional)"));
		LayoutUtils.constrain(panelSubAtomsConfigFile, m_compAugmentedAtomsConfigurationFile,
				0, 0, 1, LayoutUtils.REMAINDER,
				LayoutUtils.HORIZONTAL, LayoutUtils.CENTER, 1.0d, 0.0d,
				0, 10, 0, 7);
		LayoutUtils.constrain(panelSubAtomsConfigFile, ((JPanel)compAugmentedAtomsConfigurationFile.getComponentPanel().
				getComponent(0)).getComponent(0), // The browse button
				1, 0, 1, LayoutUtils.REMAINDER,
				LayoutUtils.NONE, LayoutUtils.CENTER, 0.0d, 0.0d,
				0, 0, 0, 7);
		LayoutUtils.constrain(panelSubAtomsConfigFile, m_btnLoadDefaultAugmentedAtoms,
				2, 0, 1, LayoutUtils.REMAINDER,
				LayoutUtils.NONE, LayoutUtils.EAST, 0.0d, 0.0d,
				0, 0, 0, 7);
		LayoutUtils.constrain(panelSubAtomsConfigFile, m_btnShowAugmentedAtomsConfiguration,
				3, 0, LayoutUtils.REMAINDER, LayoutUtils.REMAINDER,
				LayoutUtils.NONE, LayoutUtils.EAST, 0.0d, 0.0d,
				0, 0, 0, 10);

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
		LayoutUtils.constrain(panel, panelSubTransConfigFile,
				0, iRow++, LayoutUtils.REMAINDER, 1,
				LayoutUtils.HORIZONTAL, LayoutUtils.CENTER, 1.0d, 0.0d,
				7, 7, 0, 7);
		LayoutUtils.constrain(panel, panelSubAtomsConfigFile,
				0, iRow++, LayoutUtils.REMAINDER, 1,
				LayoutUtils.HORIZONTAL, LayoutUtils.CENTER, 1.0d, 0.0d,
				7, 7, 0, 7);
		LayoutUtils.constrain(panel, panelSubSwitches,
				0, iRow++, LayoutUtils.REMAINDER, LayoutUtils.REMAINDER,
				LayoutUtils.BOTH, LayoutUtils.CENTER, 1.0d, 1.0d,
				7, 7, 7, 7);

		// "Handling Failures" TAB
		final JPanel panelFailures = (JPanel)getTab("Handling Failures");
		panelFailures.setLayout(new GridBagLayout());
		panelFailures.removeAll();

		final JPanel panelSubSpecialFailures = new JPanel(new GridBagLayout());
		panelSubSpecialFailures.setPreferredSize(new Dimension(400, 200));

		panelSubSpecialFailures.setBorder(BorderFactory.createTitledBorder("Special Failures (Optional)"));
		LayoutUtils.constrain(panelSubSpecialFailures, compFailureCodes.getComponentPanel(),
				0, 0, 1, LayoutUtils.REMAINDER,
				LayoutUtils.BOTH, LayoutUtils.CENTER, 1.0d, 1.0d,
				0, 10, 0, 7);

		iRow = 0;
		LayoutUtils.constrain(panelFailures, panelSubSpecialFailures,
				0, iRow++, LayoutUtils.REMAINDER, LayoutUtils.REMAINDER,
				LayoutUtils.BOTH, LayoutUtils.CENTER, 1.0d, 1.0d,
				7, 7, 7, 7);

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
		checkConfigurationFile(m_modelTransformationConfigurationFile,
				RDKitStructureNormalizerNodeModel.DEFAULT_TRANSFORMATION_CONFIGURATION_FILE,
				m_compTransformationConfigurationFile);
		checkConfigurationFile(m_modelAugmentedAtomsConfigurationFile,
				RDKitStructureNormalizerNodeModel.DEFAULT_AUGMENTED_ATOMS_CONFIGURATION_FILE,
				m_compAugmentedAtomsConfigurationFile);
	}

	/**
	 * Loads the functional group settings from the custom file or from the
	 * default definition file and refreshes the condition table with the data.
	 * 
	 * @param compFileInput Component to show a special error border.
	 */
	protected void checkConfigurationFile(final SettingsModelString modelFile, final String strDefaultResource,
			final JComponent compFileInput) {
		try {
			RDKitStructureNormalizerNodeModel.getConfiguration(modelFile == null ? null : modelFile.getStringValue(),
					strDefaultResource);
			setFileError(compFileInput, false);
		}
		catch (final InvalidSettingsException exc) {
			// Occurs if the specified file cannot be accessed
			// (e.g not existing, no permissions, etc.)
			setFileError(compFileInput, true);
		}
	}

	/**
	 * Shows the definition file that is currently used.
	 * 
	 * @param modelFile The model with the file name. Must not be null.
	 * @param strTitle Dialog title for showing the content of the file.
	 */
	protected void onShowConfiguration(final String strContent, final String strFile, final String strTitle) {
		if (strContent != null) {
			try {
				final JTextArea ta = new JTextArea(strContent, 25, 90);
				ta.setEditable(false);
				final JScrollPane scrollPane = new JScrollPane(ta);

				JOptionPane.showOptionDialog(getPanel(),
						scrollPane, strTitle,
						JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
						new Object[] { "Close" }, "Close");
			}
			catch (final Exception exc) {
				final String strMsg = "The configuration file '" +
						strFile + "' could not " +
						"be opened" + (exc.getMessage() != null ?
								" for the following reason:\n" + exc.getMessage() : ".");
				LOGGER.warn(strMsg, exc);
				JOptionPane.showMessageDialog(getPanel(), strMsg,
						"Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/**
	 * Shows or hides a red border from the file input component expressing
	 * an error condition when the file could not be loaded.
	 * 
	 * @param compFileInput Component to show a special error border,
	 * @param bEnabled Set to true to show an error, false otherwise.
	 */
	protected void setFileError(final JComponent compFileInput, final boolean bEnabled) {
		if (compFileInput == null) {
			return;
		}

		final Border border = compFileInput.getBorder();

		if (bEnabled) {
			boolean bErrorBorderFound = false;

			// Check, if our error border is already set
			if (border == m_borderInputFileError) {
				bErrorBorderFound = true;
			}
			else if (border instanceof CompoundBorder &&
					((CompoundBorder)border).getOutsideBorder() ==
					m_borderInputFileError) {
				bErrorBorderFound = true;
			}

			if (!bErrorBorderFound) {
				if (border == null) {
					compFileInput.setBorder(m_borderInputFileError);
				}
				else {
					compFileInput.setBorder(BorderFactory.createCompoundBorder(
							m_borderInputFileError, border));
				}
			}
		}
		else { // Disable error
			// Check, if our error border is still set
			if (border == m_borderInputFileError) {
				compFileInput.setBorder(null);
			}
			else if (border instanceof CompoundBorder &&
					((CompoundBorder)border).getOutsideBorder() ==
					m_borderInputFileError) {
				compFileInput.setBorder(((CompoundBorder)border).getInsideBorder());
			}
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
	static final SettingsModelString createInputColumnNameModel() {
		return new SettingsModelString("input_column", null);
	}

	/**
	 * Creates the settings model to be used for a passed output column.
	 * 
	 * @return Settings model for passed output column with corrected structure.
	 */
	static final SettingsModelString createPassedCorrectedStructureColumnNameModel() {
		return new SettingsModelString("passed_corrected_structure_column", null);
	}

	/**
	 * Creates the settings model to be used for a passed error flags output column.
	 * 
	 * @return Settings model for passed output column with error flags.
	 */
	static final SettingsModelString createPassedFlagsColumnNameModel() {
		return new SettingsModelString("passed_flags_column", null);
	}

	/**
	 * Creates the settings model to be used for a passed warning flags output column.
	 * 
	 * @return Settings model for passed output column with warning flags.
	 */
	static final SettingsModelString createPassedWarningMessagesColumnNameModel() {
		return new SettingsModelString("passed_warning_messages_column", null);
	}

	/**
	 * Creates the settings model to be used for a failed error flags output column.
	 * 
	 * @return Settings model for failed output column with error flags.
	 */
	static final SettingsModelString createFailedFlagsColumnNameModel() {
		return new SettingsModelString("failed_error_flags_column", null);
	}

	/**
	 * Creates the settings model to be used for a failed error messages output column.
	 * 
	 * @return Settings model for failed output column with error messages.
	 */
	static final SettingsModelString createFailedErrorMessagesColumnNameModel() {
		return new SettingsModelString("failed_error_messages_column", null);
	}

	/**
	 * Creates the settings model to be used for defining switches for StruCheck.
	 * 
	 * @return Settings model for StruCheck switches.
	 */
	static final SettingsModelEnumerationArray<StruCheckSwitch> createSwitchOptionsModel() {
		return new SettingsModelEnumerationArray<StruCheckSwitch>(StruCheckSwitch.class, "switches",
				RDKitStructureNormalizerNodeModel.DEFAULT_SWITCHES);
	}

	/**
	 * Creates the settings model to be used for defining advanced switches for StruCheck.
	 * 
	 * @return Settings model for StruCheck switches.
	 */
	static final SettingsModelString createAdvancedOptionsModel() {
		return new SettingsModelString("advanced_options",
				RDKitStructureNormalizerNodeModel.DEFAULT_ADVANCED_OPTIONS);
	}

	/**
	 * Creates the settings model to be used to specify what non error codes (usually
	 * transformation / warning codes) shall be treated as failures to make rows end
	 * up in the second output table for failed corrections.
	 * 
	 * @return Settings model for result column name.
	 */
	static final SettingsModelEnumerationArray<StruCheckCode> createAdditionalFailureCodesConfigurationModel() {
		return new SettingsModelEnumerationArray<StruCheckCode>(
				StruCheckCode.class, "additional_failure_codes", new StruCheckCode[0]);
	}

	/**
	 * Creates the settings model to be used to specify the transformation configuration.
	 * 
	 * @return Settings model for transformation configuration.
	 */
	static final SettingsModelString createTransformationConfigurationFileModel() {
		return new SettingsModelString("transformation_configuration_file",
				RDKitStructureNormalizerNodeModel.DEFAULT_CONFIGURATION_ID);
	}

	/**
	 * Creates the settings model to be used to specify the augmented atoms configuration.
	 * 
	 * @return Settings model for augmented atoms configuration.
	 */
	static final SettingsModelString createAugmentedAtomsConfigurationFileModel() {
		return new SettingsModelString("augmented_atoms_configuration_file",
				RDKitStructureNormalizerNodeModel.DEFAULT_CONFIGURATION_ID);
	}

	/**
	 * Creates the settings model to be used to specify the log file.
	 * 
	 * @return Settings model for StruCheck configuration.
	 */
	static final SettingsModelString createLogFileModel() {
		return new SettingsModelString("log_file", null);
	}

	/**
	 * Creates the settings model to be used to declare that overriding
	 * an existing log file is desired.
	 * 
	 * @return Settings model for overriding an existing log file.
	 */
	static final SettingsModelBoolean createOverwriteOptionModel() {
		return new SettingsModelBoolean("overwriteOK", false);
	}

	/**
	 * Defines the default non error codes list.
	 * 
	 * @return Non error codes list.
	 */
	static final List<StruCheckCode> getDefaultNonErrorCodes() {
		final List<StruCheckCode> listDefaultNonErrorCodes = new ArrayList<StruCheckCode>();

		for (final StruCheckCode code : StruCheckCode.values()) {
			if (!code.isError()) {
				listDefaultNonErrorCodes.add(code);
			}
		}

		return listDefaultNonErrorCodes;
	}
}
