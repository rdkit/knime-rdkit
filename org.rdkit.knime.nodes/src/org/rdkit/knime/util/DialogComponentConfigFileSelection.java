/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2023
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

package org.rdkit.knime.util;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.util.Objects;

/**
 * A custom dialog component to be used to layout configuration file selection widget in node settings dialog.
 *
 * @author Roman Balabanov
 */
public class DialogComponentConfigFileSelection extends DialogComponent {

    //
    // Inner classes
    //

    /**
     * Functional interface to be implemented in order to define configuration file reader.
     */
    @FunctionalInterface
    public interface ConfigFileReader {

        /**
         * Reads a configuration file using {@link SettingsModelReaderFileChooser} instance provided and
         * returns its content as string.
         *
         * @param modelFileChooser {@link SettingsModelReaderFileChooser} instance to be used.
         *                         Mustn't be null.
         * @return Configuration file data data as string.
         * @throws InvalidSettingsException if {@code modelFileChooser} instance provided is invalid.
         * @throws IOException              if happened during file reading.
         */
        String read(SettingsModelReaderFileChooser modelFileChooser) throws InvalidSettingsException, IOException;

    }

    //
    // Constants
    //

    /**
     * The logger instance.
     */
    protected static final NodeLogger LOGGER = NodeLogger.getLogger(DialogComponentConfigFileSelection.class);

    /**
     * Pseudo file name to show when default configuration shall be used for Structure Normalizer.
     */
    protected static final String DEFAULT_CONFIGURATION_OPTION = "Default Configuration";

    /**
     * Button image for showing definition info.
     */
    private static final Icon INFO_ICON = LayoutUtils.createImageIcon(DialogComponentConfigFileSelection.class,
            "/icons/common_rdkit_info.png", null);

    /**
     * An error border to be shown, if the custom definition file name is not existing.
     */
    private static final Border ERROR_BORDER = BorderFactory.createLineBorder(Color.RED);

    //
    // Members
    //

    /**
     * Wrapped {@link DialogComponentReaderFileChooser} instance.
     */
    private final DialogComponentReaderFileChooser m_compFileChooser;

    /**
     * Title string to be used for dialogs and also in log messages.
     */
    private final String m_strTitle;

    /**
     * {@link ConfigFileReader} instance to be used to read configuration file.
     */
    private final ConfigFileReader m_configFileReader;

    /**
     * File path UI component.
     */
    private final JComboBox<String> m_cbFilePath;

    //
    // Constructors
    //

    /**
     * Constructs {@code DialogComponentLogFileSelection}
     *
     * @param compFileChooser Wrapped {@link DialogComponentReaderFileChooser} instance.
     *                        Mustn't be null.
     * @param strTitle         Title string to be used for dialogs and also in log messages.
     *                         Mustn't be null.
     * @param configFileReader {@link ConfigFileReader} instance to be used to read configuration file.
     *                         Mustn't be null.
     * @throws IllegalArgumentException If any of {@code compFileChooser}, {@code strTitle} or {@code configFileReader} parameters is null.
     */
    public DialogComponentConfigFileSelection(DialogComponentReaderFileChooser compFileChooser,
                                              String strTitle,
                                              ConfigFileReader configFileReader) {
        super(new EmptySettingsModel());

        if (compFileChooser == null) {
            throw new IllegalArgumentException("File Chooser parameters must not be null.");
        }
        if (strTitle == null) {
            throw new IllegalArgumentException("Title parameter must not be null.");
        }
        if (configFileReader == null) {
            throw new IllegalArgumentException("Configuration file reader parameter must not be null.");
        }

        m_compFileChooser = compFileChooser;
        m_strTitle = strTitle;
        m_configFileReader = configFileReader;

        m_cbFilePath = new JComboBox<>();
        m_cbFilePath.addItem(DEFAULT_CONFIGURATION_OPTION);
        m_cbFilePath.addItemListener(e -> {
            if (ItemEvent.SELECTED == e.getStateChange()) {
                final String strSelectedItem = (String) e.getItem();
                if (DEFAULT_CONFIGURATION_OPTION.equals(strSelectedItem)) {
                    onLoadDefaults();
                }
                else {
                    final String strPath = strSelectedItem.replaceFirst(
                            compFileChooser.getSettingsModel().getFileSystemName() + ": ", "");
                    compFileChooser.getSettingsModel().setPath(strPath);
                }
            }
        });
        compFileChooser.getSettingsModel().addChangeListener(e -> {
            final String path = compFileChooser.getSettingsModel().getPath();
            if (path == null || path.isBlank()) {
                m_cbFilePath.setSelectedItem(DEFAULT_CONFIGURATION_OPTION);
            }
            else {
                // Here we maintain drop-down list, and we do it in a bit tricky way in order
                // to avoid unnecessary cascade item selection changed events (see comments below).
                final String strNewItem = compFileChooser.getSettingsModel().getFileSystemName() + ": " + path;
                // 1. Inserting a new list item at index 1 if not already there.
                if (m_cbFilePath.getItemCount() < 2 || !Objects.equals(m_cbFilePath.getModel().getElementAt(1), strNewItem)) {
                    m_cbFilePath.insertItemAt(strNewItem, 1);
                }
                // 2. Selecting it.
                m_cbFilePath.setSelectedItem(strNewItem);
                // 3. Removing the item deselected if there is one.
                while (m_cbFilePath.getItemCount() > 2) {
                    m_cbFilePath.removeItemAt(m_cbFilePath.getItemCount() - 1);
                }
            }
        });

        final JButton btnFilePathChooser = new JButton("Load Custom...");
        final JPanel panelFilePathChooser = new JPanel(new BorderLayout());
        panelFilePathChooser.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Select File"),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)
                )
        );
        panelFilePathChooser.setPreferredSize(new Dimension(765, 160));
        compFileChooser.getComponentPanel().setMaximumSize(new Dimension(760, 155));
        panelFilePathChooser.add(compFileChooser.getComponentPanel());
        btnFilePathChooser.addActionListener(e -> {
            final int iResult = JOptionPane.showOptionDialog(
                    getComponentPanel(),
                    panelFilePathChooser,
                    m_strTitle + " file",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    new Object[]{"Close", "Load Defaults"},
                    null
            );
            if (iResult == JOptionPane.NO_OPTION) {
                onLoadDefaults();
            }
        });

        final JButton btnLoadDefaults = new JButton("Load Defaults");
        btnLoadDefaults.addActionListener(e -> onLoadDefaults());

        final JButton btnShowConfiguration = new JButton(INFO_ICON);
        btnShowConfiguration.setToolTipText("Show configuration defined here.");
        btnShowConfiguration.addActionListener(e -> onShowConfiguration());

        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(m_strTitle + " (Optional)"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        LayoutUtils.constrain(panel, m_cbFilePath,
                0, 0, 1, LayoutUtils.REMAINDER,
                LayoutUtils.HORIZONTAL, LayoutUtils.CENTER, 1.0d, 0.0d,
                0, 10, 0, 7);
        LayoutUtils.constrain(panel, btnFilePathChooser,
                1, 0, 1, LayoutUtils.REMAINDER,
                LayoutUtils.NONE, LayoutUtils.CENTER, 0.0d, 0.0d,
                0, 0, 0, 7);
        LayoutUtils.constrain(panel, btnLoadDefaults,
                2, 0, 1, LayoutUtils.REMAINDER,
                LayoutUtils.NONE, LayoutUtils.EAST, 0.0d, 0.0d,
                0, 0, 0, 7);
        LayoutUtils.constrain(panel, btnShowConfiguration,
                3, 0, LayoutUtils.REMAINDER, LayoutUtils.REMAINDER,
                LayoutUtils.NONE, LayoutUtils.EAST, 0.0d, 0.0d,
                0, 0, 0, 10);

        getComponentPanel().setLayout(new BorderLayout());
        getComponentPanel().add(panel, BorderLayout.NORTH);
    }

    //
    // Public methods
    //

    /**
     * Sets or removes configuration file path error flag. Indicates it to user if True provided.
     *
     * @param bError New configuration file path error flag value.
     */
    public void setFileError(boolean bError) {
        if (m_cbFilePath != null) {
            final Border border = m_cbFilePath.getBorder();

            if (bError) {
                // Check, if our error border is already set
                final boolean bErrorBorderFound = border == ERROR_BORDER ||
                        (border instanceof CompoundBorder compoundBorder && compoundBorder.getOutsideBorder() == ERROR_BORDER);
                if (!bErrorBorderFound) {
                    if (border == null) {
                        m_cbFilePath.setBorder(ERROR_BORDER);
                    }
                    else {
                        m_cbFilePath.setBorder(BorderFactory.createCompoundBorder(ERROR_BORDER, border));
                    }
                }
            }
            else { // Disable error
                // Check, if our error border is still set
                if (border == ERROR_BORDER) {
                    m_cbFilePath.setBorder(null);
                }
                else if (border instanceof CompoundBorder compoundBorder && compoundBorder.getOutsideBorder() == ERROR_BORDER) {
                    m_cbFilePath.setBorder(((CompoundBorder) border).getInsideBorder());
                }
            }
        }
    }

    @Override
    public void setToolTipText(String text) {
        // nothing to be done
    }

    //
    // Protected methods
    //

    /**
     * Called to select default configuration file.
     */
    protected void onLoadDefaults() {
        m_compFileChooser.getSettingsModel().setPath("");
    }

    /**
     * Called to show selected configuration file content.
     */
    protected void onShowConfiguration() {
        try {
            final String strContent = m_configFileReader.read(m_compFileChooser.getSettingsModel());

            if (strContent != null) {
                try {
                    final JTextArea ta = new JTextArea(strContent, 25, 90);
                    ta.setEditable(false);

                    JOptionPane.showOptionDialog(getComponentPanel(), new JScrollPane(ta), m_strTitle,
                            JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                            new Object[] { "Close" }, "Close");
                }
                catch (final Exception exc) {
                    final String strMsg = "The configuration file '" + m_compFileChooser.getSettingsModel().getPath() + "'" +
                            " could not be opened" +
                            (exc.getMessage() != null ? " for the following reason:\n" + exc.getMessage() : ".");
                    LOGGER.warn(strMsg, exc);
                    JOptionPane.showMessageDialog(getComponentPanel(), strMsg,
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        catch (final InvalidSettingsException | IOException exc) {
            JOptionPane.showMessageDialog(getComponentPanel(),
                    "The following error occurred: " + exc.getMessage(),
                    m_strTitle + " - Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    protected void updateComponent() {
        // nothing to be done
    }

    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        // nothing to be done
    }

    @Override
    protected void checkConfigurabilityBeforeLoad(PortObjectSpec[] specs) throws NotConfigurableException {
        // nothing to be done
    }

    @Override
    protected void setEnabledComponents(boolean enabled) {
        // nothing to be done
    }

}
