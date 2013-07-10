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

import org.knime.chem.types.SdfCell;
import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmilesCell;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.core.node.util.FilesHistoryPanel;
import org.rdkit.knime.types.RDKitMolCell2;

/**
 * This is the dialog for the Fingerprint writer node. The user is provided the
 * options to specify the output file name, whether or not the row id should be
 * written to the file, whether or not the file should be overwritten and the
 * option to select the fingerprint column.
 * 
 * @author Dillip K Mohanty
 */
public class FingerprintWriterNodeDialog extends NodeDialogPane {

	//
	// Constants
	//

	/** The LOGGER instance. */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(FingerprintWriterNodeDialog.class);
	/**
	 * String datatypes can be selected by the user as columns that contain
	 * fingerprint Ids. Certain KNIME DataTypes are compatible with the String
	 * type, but are definitely not used for fingerprint Ids. This array
	 * contains such DataTypes (classes that extend DataCell), which will be
	 * filtered out when offering the user to choose fingerprint Id column.
	 */
	@SuppressWarnings("deprecation")
	public static final Class<?>[] IGNORE_STRING_COMPATIBILITY_FOR_DATATYPES = new Class<?>[] {
		org.rdkit.knime.types.RDKitMolCell.class, RDKitMolCell2.class,
		SmilesCell.class, SmilesValue.class, SdfCell.class, SdfValue.class // Only
		// add
		// classes
		// that
		// are
		// extending
		// DataCell!
	};

	private static final ColumnFilter ID_COLUMN_FILTER = new ColumnFilter() {
		@Override
		public boolean includeColumn(final DataColumnSpec colSpec) {
			boolean bRet = false;

			if (colSpec != null) {
				final DataType dataType = colSpec.getType();

				// Fingerprint IDs as String values
				if (dataType.isCompatible(StringValue.class)
						&& !ignoreStringDataType(dataType)) {
					bRet = true;
				}

			}

			return bRet;
		}

		@Override
		public String allFilteredMsg() {
			return "There are no String columns available, which contain Fingerprint IDs.";
		}
	};

	//
	// Members
	//

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


	//
	// Constructor
	//

	/**
	 * Constructor to create a new fingerprint writer configuration dialog pane.
	 */
	@SuppressWarnings("unchecked")
	public FingerprintWriterNodeDialog() {
		final JPanel filePanel = new JPanel();
		filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.Y_AXIS));
		filePanel.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(), "Output file:"));
		final JPanel textBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		m_textBox = new FilesHistoryPanel(
				FingerprintWriterNodeModel.HISTORY_ID, ".fps");
		textBoxPanel.add(m_textBox);
		filePanel.add(textBoxPanel);
		filePanel.add(Box.createVerticalStrut(5));

		final JPanel optionsPanel = new JPanel();
		optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
		optionsPanel.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(), "Writer options:"));

		final ActionListener al = new ActionListener() {
			/** {@inheritDoc} */
			@Override
			public void actionPerformed(final ActionEvent e) {
				checkCheckerState();
			}
		};
		m_IdSelector = new ColumnSelectionComboxBox((Border) null,
				ID_COLUMN_FILTER);
		m_IdSelector.addItem("None");
		m_rowHeaderChecker = new JCheckBox("Write row ID");
		m_rowHeaderChecker.addActionListener(al);
		final JPanel rowHeaderPanel = new JPanel(
				new FlowLayout(FlowLayout.LEFT));
		rowHeaderPanel.add(Box.createHorizontalGlue());
		rowHeaderPanel.add(m_rowHeaderChecker);
		final JLabel IdLabel = new JLabel("                Id column:");
		rowHeaderPanel.add(IdLabel);
		rowHeaderPanel.add(Box.createHorizontalGlue());
		rowHeaderPanel.add(m_IdSelector);
		optionsPanel.add(rowHeaderPanel);

		m_overwriteOKChecker = new JCheckBox("Overwrite if file exists");
		final JPanel overwriteOKPanel = new JPanel(new FlowLayout(
				FlowLayout.LEFT));
		overwriteOKPanel.add(Box.createHorizontalGlue());
		overwriteOKPanel.add(m_overwriteOKChecker);
		optionsPanel.add(overwriteOKPanel);

		m_fingerprintSelector = new ColumnSelectionComboxBox((Border) null,
				BitVectorValue.class);

		final JPanel columnPanel = new JPanel();
		columnPanel.setLayout(new BoxLayout(columnPanel, BoxLayout.Y_AXIS));
		columnPanel.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(), "Column selection:"));

		final JPanel fpsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final JLabel fpsLabel = new JLabel("Fingerprint column :");
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

	//
	// Private Methods
	//

	/** Checks whether or not the "id column" dropbox should be enabled. */
	private void checkCheckerState() {
		if (!m_rowHeaderChecker.isSelected()) {
			m_IdSelector.setEnabled(true);
		} else {
			m_IdSelector.setEnabled(false);
			m_IdSelector.setSelectedItem("None");
		}
	}

	//
	// Protected Methods
	//

	/**
	 * Method used for load configuration settings. {@inheritDoc}
	 */
	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings,
			final DataTableSpec[] specs) throws NotConfigurableException {
		if (!specs[0].containsCompatibleType(BitVectorValue.class)) {
			throw new NotConfigurableException(
					"Unable to configure, no fingerprint column in input table.");
		}
		final String fileName = settings.getString(
				FingerprintWriterNodeModel.CFG_TARGET_FILE, null);
		final String column = settings.getString(
				FingerprintWriterNodeModel.CFG_FPS_COLUMN, "");
		final String id = settings.getString(
				FingerprintWriterNodeModel.CFG_ID_COLUMN, "");
		final boolean overwriteOK = settings.getBoolean(
				FingerprintWriterNodeModel.CFG_OVERWRITE_OK, false);
		final boolean writeRowid = settings.getBoolean(
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
	 * Method used to save configuration settings. {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings)
			throws InvalidSettingsException {
		final String fileName = m_textBox.getSelectedFile();
		if (!fileName.equals("")) {
			final File file = FilesHistoryPanel.getFile(fileName);
			settings.addString(FingerprintWriterNodeModel.CFG_TARGET_FILE,
					file.getAbsolutePath());
		}
		final String colName = m_fingerprintSelector.getSelectedColumn();
		settings.addString(FingerprintWriterNodeModel.CFG_FPS_COLUMN, colName);

		final String id = m_IdSelector.getSelectedColumn();
		settings.addString(FingerprintWriterNodeModel.CFG_ID_COLUMN, id);

		final boolean overwriteOK = m_overwriteOKChecker.isSelected();
		settings.addBoolean(FingerprintWriterNodeModel.CFG_OVERWRITE_OK,
				overwriteOK);

		final boolean writeRowid = m_rowHeaderChecker.isSelected();
		settings.addBoolean(FingerprintWriterNodeModel.CFG_WRITE_ROWID,
				writeRowid);
	}


	//
	// Public Methods
	//

	/**
	 * Determines, if the passed in data type is one of the types we need to
	 * ignore.
	 * 
	 * @param dataType
	 *            KNIME DataType to check. Usually this is a string compatible
	 *            datatype. Can be null.
	 * 
	 * @return True, if data type should be ignored and not offered to the user
	 *         as string compatible type. False otherwise. Returns also false,
	 *         if null is passed in.
	 */
	@SuppressWarnings("unchecked")
	public static boolean ignoreStringDataType(final DataType dataType) {
		// Check our ignore list for data types, which cannot be Fingerprint IDs
		// although they are compatible with String
		boolean bFound = false;

		if (dataType != null) {
			for (final Class<?> dataCellClass : IGNORE_STRING_COMPATIBILITY_FOR_DATATYPES) {
				try {
					if (dataType
							.equals(DataType
									.getType((Class<? extends DataCell>) dataCellClass))) {
						bFound = true;
						break;
					}
				} catch (final ClassCastException exc) {
					LOGGER.warn("An non-DataCell class has been found in FingerprintWriterNodeDialog.IGNORE_STRING_COMPATIBILITY_FOR_DATATYPES list.");
				}
			}
		}

		return bFound;
	}

}
