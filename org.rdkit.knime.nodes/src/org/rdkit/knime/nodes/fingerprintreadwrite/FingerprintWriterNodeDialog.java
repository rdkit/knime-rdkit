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
package org.rdkit.knime.nodes.fingerprintreadwrite;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.core.node.util.FilesHistoryPanel;

/**
 * This is the dialog for the Fingerprint writer node. The user is provided the 
 * options to specify the output file name, whether or not the row id should be 
 * written to the file, whether or not the file should be overwritten and the option 
 * to select the fingerprint column. 
 *  
 * @author Dillip K Mohanty
 */
public class FingerprintWriterNodeDialog extends NodeDialogPane {

    /** textfield to enter file name. */
    private final FilesHistoryPanel m_textBox;

    /** fingerprint column selector. */
    private final ColumnSelectionComboxBox m_fingerprintSelector;
    
    /** Id column selector. */
    private final ColumnSelectionComboxBox m_IdSelector;
    
    /** Overwrite OK checkbox. */
    private final JCheckBox m_overwriteOKChecker;
    
    /** Write Row Id checkbox. */
    private final JCheckBox m_rowHeaderChecker;
    
    
    /**
     * Constructor to create a new fingerprint writer configuration dialog pane. 
     */
    @SuppressWarnings("unchecked")
    public FingerprintWriterNodeDialog() {
        final JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.Y_AXIS));
        filePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Output file:"));
        final JPanel textBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        m_textBox = new FilesHistoryPanel(
                FingerprintWriterNodeModel.HISTORY_ID, ".fps");
        textBoxPanel.add(m_textBox);
        filePanel.add(textBoxPanel);
        filePanel.add(Box.createVerticalStrut(5));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Writer options:"));

        ActionListener al = new ActionListener() {
        	/** {@inheritDoc} */
        	@Override
        	public void actionPerformed(final ActionEvent e) {
		          checkCheckerState();
		    }
		};
        m_IdSelector = new ColumnSelectionComboxBox((Border)null,
                StringValue.class);
        m_IdSelector.addItem("None");
        m_rowHeaderChecker = new JCheckBox("Write row ID");
        m_rowHeaderChecker.addActionListener(al);
        final JPanel rowHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rowHeaderPanel.add(Box.createHorizontalGlue());
        rowHeaderPanel.add(m_rowHeaderChecker);
        JLabel IdLabel = new JLabel("                Id column:");
        rowHeaderPanel.add(IdLabel);
        rowHeaderPanel.add(Box.createHorizontalGlue());
        rowHeaderPanel.add(m_IdSelector);
        optionsPanel.add(rowHeaderPanel);
        
        m_overwriteOKChecker = new JCheckBox("Overwrite if file exists");
        final JPanel overwriteOKPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        overwriteOKPanel.add(Box.createHorizontalGlue());
        overwriteOKPanel.add(m_overwriteOKChecker);
        optionsPanel.add(overwriteOKPanel);
        
        m_fingerprintSelector = new ColumnSelectionComboxBox((Border)null,
                BitVectorValue.class);
        
        final JPanel columnPanel = new JPanel();
        columnPanel.setLayout(new BoxLayout(columnPanel, BoxLayout.Y_AXIS));
        columnPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Column selection:"));
        
        JPanel fpsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel fpsLabel = new JLabel("Fingerprint column :");
        fpsPanel.add(fpsLabel);
        fpsPanel.add(Box.createHorizontalGlue());
        fpsPanel.add(m_fingerprintSelector);
        columnPanel.add(fpsPanel);
        columnPanel.add(Box.createVerticalGlue());
        columnPanel.add(Box.createVerticalGlue());
        
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(filePanel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(optionsPanel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(columnPanel);
        panel.add(Box.createVerticalGlue());
        addTab("Settings", panel);
    }

    /** Checks whether or not the "id column" dropbox should be enabled. */
    private void checkCheckerState() {
    	if(!m_rowHeaderChecker.isSelected()){
	        m_IdSelector.setEnabled(true);
    	} else {
    		 m_IdSelector.setEnabled(false);
    		 m_IdSelector.setSelectedItem("None");
    	}
    }
    
    /**
     * Method used for load configuration settings.
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        if (!specs[0].containsCompatibleType(BitVectorValue.class)) {
            throw new NotConfigurableException(
                    "Unable to configure, no fingerprint column in input table.");
        }
        String fileName = settings.getString(
                FingerprintWriterNodeModel.CFG_TARGET_FILE, null);
        String column = settings.getString(
                FingerprintWriterNodeModel.CFG_FPS_COLUMN, "");
        String id = settings.getString(
                FingerprintWriterNodeModel.CFG_ID_COLUMN, "");
        boolean overwriteOK = settings.getBoolean(
                FingerprintWriterNodeModel.CFG_OVERWRITE_OK, false);
        boolean writeRowid = settings.getBoolean(
                FingerprintWriterNodeModel.CFG_WRITE_ROWID, false);
        m_textBox.updateHistory();
        m_textBox.setSelectedFile(fileName);
        m_fingerprintSelector.update(specs[0], column);
        m_IdSelector.update(specs[0], id);
        m_overwriteOKChecker.setSelected(overwriteOK);
        m_rowHeaderChecker.setSelected(writeRowid);
        checkCheckerState();
    }

    /**
     * Method used to save configuration settings.
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        String fileName = m_textBox.getSelectedFile();
        if (!fileName.equals("")) {
            File file = FilesHistoryPanel.getFile(fileName);
            settings.addString(FingerprintWriterNodeModel.CFG_TARGET_FILE, file
                    .getAbsolutePath());
        }
        String colName = m_fingerprintSelector.getSelectedColumn();
        settings.addString(FingerprintWriterNodeModel.CFG_FPS_COLUMN, colName);
        
        String id = m_IdSelector.getSelectedColumn();
        settings.addString(FingerprintWriterNodeModel.CFG_ID_COLUMN, id);
        
        boolean overwriteOK = m_overwriteOKChecker.isSelected();
        settings.addBoolean(FingerprintWriterNodeModel.CFG_OVERWRITE_OK, overwriteOK);
        
        boolean writeRowid = m_rowHeaderChecker.isSelected();
        settings.addBoolean(FingerprintWriterNodeModel.CFG_WRITE_ROWID, writeRowid);
    }

}

