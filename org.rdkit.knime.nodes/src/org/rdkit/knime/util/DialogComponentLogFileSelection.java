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
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.DialogComponentWriterFileChooser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.Objects;

/**
 * A custom dialog component to be used to layout log file selection widget in node settings dialog.
 *
 * @author Roman Balabanov
 */
public class DialogComponentLogFileSelection extends DialogComponent {

    //
    // Constants
    //

    /**
     * The logger instance.
     */
    protected static final NodeLogger LOGGER = NodeLogger.getLogger(DialogComponentLogFileSelection.class);

    /**
     * Pseudo file name to show when default configuration shall be used for Structure Normalizer.
     */
    protected static final String NO_LOGGING_OPTION = "Do not conserve log output";

    //
    // Members
    //

    private final DialogComponentWriterFileChooser m_compFileChooser;

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
     * @param compFileChooser Wrapped {@link DialogComponentWriterFileChooser} instance.
     *                        Mustn't be null.
     * @throws IllegalArgumentException When {@code compFileChooser} parameter is null.
     */
    public DialogComponentLogFileSelection(DialogComponentWriterFileChooser compFileChooser) {
        super(new EmptySettingsModel());

        if (compFileChooser == null) {
            throw new IllegalArgumentException("File Chooser parameters must not be null.");
        }

        m_compFileChooser = compFileChooser;

        m_cbFilePath = new JComboBox<>();
        m_cbFilePath.addItem(NO_LOGGING_OPTION);
        m_cbFilePath.addItemListener(e -> {
            if (ItemEvent.SELECTED == e.getStateChange()) {
                final String strSelectedItem = (String) e.getItem();
                if (NO_LOGGING_OPTION.equals(strSelectedItem)) {
                    onNoLogging();
                }
                else {
                    final String strPath = strSelectedItem.replaceFirst(
                            m_compFileChooser.getSettingsModel().getFileSystemName() + ": ", "");
                    m_compFileChooser.getSettingsModel().setPath(strPath);
                }
            }
        });
        m_compFileChooser.getSettingsModel().addChangeListener(e -> {
            final String strPath = m_compFileChooser.getSettingsModel().getLocation().getPath();
            if (strPath == null || strPath.isBlank()) {
                m_cbFilePath.setSelectedItem(NO_LOGGING_OPTION);
            }
            else {
                // Here we maintain drop-down list, and we do it in a bit tricky way in order
                // to avoid unnecessary cascade item selection changed events (see comments below).
                final String strNewItem = m_compFileChooser.getSettingsModel().getFileSystemName() + ": " + strPath;
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

        final JButton btnFilePathChooser = new JButton("Specify File...");
        final JPanel panelFilePathChooser = new JPanel(new BorderLayout());
        panelFilePathChooser.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Select File"),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)
        ));
        panelFilePathChooser.setPreferredSize(new Dimension(765, 160));
        m_compFileChooser.getComponentPanel().setMaximumSize(new Dimension(760, 155));
        panelFilePathChooser.add(m_compFileChooser.getComponentPanel());
        btnFilePathChooser.addActionListener(e -> {
            final int iResult = JOptionPane.showOptionDialog(
                    getComponentPanel(),
                    panelFilePathChooser,
                    "Select File",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    new Object[]{"Close", "Disable"},
                    null
            );
            if (iResult == JOptionPane.NO_OPTION) {
                onNoLogging();
            }
        });

        final JButton btnLoadDefaults = new JButton("Disable");
        btnLoadDefaults.addActionListener(e -> onNoLogging());

        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Logfile Output (Optional)"),
                BorderFactory.createEmptyBorder(7, 7, 7, 7)
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

        getComponentPanel().setLayout(new BorderLayout());
        getComponentPanel().add(panel, BorderLayout.NORTH);
    }

    //
    // Public Methods
    //

    @Override
    public void setToolTipText(String text) {
        // nothing to be done
    }

    //
    // Protected Methods
    //

    /**
     * Called to set log-file selection to "No Logging" state.
     */
    protected void onNoLogging() {
        m_compFileChooser.getSettingsModel().setPath("");
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
