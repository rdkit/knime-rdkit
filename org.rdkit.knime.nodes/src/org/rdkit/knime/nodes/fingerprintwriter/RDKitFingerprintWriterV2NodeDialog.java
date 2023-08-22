/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2012-2023
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

package org.rdkit.knime.nodes.fingerprintwriter;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.core.data.StringValue;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.filehandling.core.connections.FSLocationUtil;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.DialogComponentWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode;
import org.rdkit.knime.util.DialogComponentColumnNameSelection;
import org.rdkit.knime.util.HiddenSettingComponent;

import java.awt.*;

/**
 * {@code NodeDialog} for the "RDKitFingerprintWriter" Node.
 * <br><br>
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Manuel Schwarze
 * @author Roman Balabanov
 */
public class RDKitFingerprintWriterV2NodeDialog extends DefaultNodeSettingsPane {

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
	 */
	RDKitFingerprintWriterV2NodeDialog(NodeCreationConfiguration nodeCreationConfig) {
		super.createNewGroup("Output file");

		final int iInputFingerprintsPortIndex = RDKitFingerprintWriterV2NodeModel.getInputTablePortIndexes(nodeCreationConfig,
				RDKitFingerprintWriterV2NodeFactory.INPUT_PORT_GRP_ID_FINGERPRINTS)[0];

		final SettingsModelWriterFileChooser modelOutputPath = createOutputPathModel(nodeCreationConfig);
		final DialogComponentWriterFileChooser fileChooser = new DialogComponentWriterFileChooser(
				modelOutputPath,
				"FpsWriterHistory",
				createFlowVariableModel(modelOutputPath.getKeysForFSLocation(), FSLocationVariableType.INSTANCE)
		);
		fileChooser.getComponentPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		super.addDialogComponent(fileChooser);

		super.createNewGroup("Column selection");
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				createFingerprintColumnNameModel(), "Fingerprint column: ", iInputFingerprintsPortIndex,
				BitVectorValue.class));
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				createIdColumnNameModel(), "ID column: ", iInputFingerprintsPortIndex, false,
				StringValue.class));
		super.addDialogComponent(new HiddenSettingComponent(createSuppressTimeOptionModel()));

		final JPanel panelOptions = (JPanel) super.getTab("Options");
		panelOptions.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		panelOptions.setPreferredSize(new Dimension(790, 290));
	}

	//
	// Static Methods
	//

	/**
	 * Creates the settings model to be used for the output file path selection.
	 *
	 * @param nodeCreationConfig Node Creation Configuration instance.
	 *                           Mustn't be null.
	 * @return Settings model for output file path selection.
	 * @throws IllegalArgumentException When {@code nodeCreationConfig} parameter is null.
	 */
	static SettingsModelWriterFileChooser createOutputPathModel(NodeCreationConfiguration nodeCreationConfig) {
		if (nodeCreationConfig == null) {
			throw new IllegalArgumentException("Node Creation Configuration parameter must not be null.");
		}

		final SettingsModelWriterFileChooser modelResult = new SettingsModelWriterFileChooser(
				"output_file",
				nodeCreationConfig.getPortConfig().orElseThrow(IllegalStateException::new),
				RDKitFingerprintWriterV2NodeFactory.INPUT_PORT_GRP_ID_FS_CONNECTION,
				EnumConfig.create(
						SettingsModelFilterMode.FilterMode.FILE
				),
				EnumConfig.create(
						FileOverwritePolicy.FAIL,
						FileOverwritePolicy.OVERWRITE
				),
                ".fps", ".fps.gz");

		nodeCreationConfig.getURLConfig().ifPresent(urlConfiguration ->
				modelResult.setLocation(FSLocationUtil.createFromURL(urlConfiguration.getUrl().toString()))
		);

		return modelResult;
	}

	/**
	 * Creates the settings model to be used for the fingerprint column.
	 * 
	 * @return Settings model for fingerprint column selection.
	 */
	static SettingsModelString createFingerprintColumnNameModel() {
		return new SettingsModelString("fps_column", null);
	}

	/**
	 * Creates the settings model to be used for the ID column.
	 * Due to the model class the option to use the RowID as identifier
	 * is included.
	 * 
	 * @return Settings model for ID column selection.
	 */
	static SettingsModelColumnName createIdColumnNameModel() {
		return new SettingsModelColumnName("id_column", null);
	}

	/**
	 * Creates the settings model for a hidden option to suppress the time
	 * when writing out the FPS file header. When switched on via flow
	 * variable the time will always be 00:00:00. This is useful for
	 * file comparisons when testing the node. As currently boolean
	 * flow variables are not supported, we are using an Integer here.
	 * 
	 * @return Settings model for suppressing the time option.
	 */
	static SettingsModelInteger createSuppressTimeOptionModel() {
		return new SettingsModelInteger("suppress_time", 0);
	}
}
