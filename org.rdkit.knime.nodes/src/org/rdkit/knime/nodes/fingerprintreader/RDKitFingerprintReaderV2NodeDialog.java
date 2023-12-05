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

package org.rdkit.knime.nodes.fingerprintreader;

import java.awt.*;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.filehandling.core.connections.FSLocationUtil;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode;

/**
 * {@code NodeDialog} for the "RDKitFingerprintReader" Node.
 *<br><br>
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Manuel Schwarze
 * @author Roman Balabanov
 */
public class RDKitFingerprintReaderV2NodeDialog extends DefaultNodeSettingsPane {

	//
	// Constructor
	//

	/**
	 * Create a new dialog pane with components to configure an input file and
	 * the option to use IDs from the fingerprint file as row IDs.
	 *
	 * @param nodeCreationConfig Node Creation Configuration instance.
	 *                           Mustn't be null.
	 * @throws IllegalArgumentException When {@code nodeCreationConfig} parameter is null.
	 */
	RDKitFingerprintReaderV2NodeDialog(NodeCreationConfiguration nodeCreationConfig) {
		final SettingsModelReaderFileChooser modelInputPath = createInputPathModel(nodeCreationConfig);
		final DialogComponentReaderFileChooser fileChooser = new DialogComponentReaderFileChooser(
				modelInputPath,
				"FpsReaderHistory",
				createFlowVariableModel(modelInputPath.getKeysForFSLocation(), FSLocationVariableType.INSTANCE)
		);
		fileChooser.getComponentPanel().setBorder(
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Input File"),
						BorderFactory.createEmptyBorder(5, 5, 5, 5)
				)
		);
		super.addDialogComponent(fileChooser);

		super.addDialogComponent(new DialogComponentBoolean(
				createUseIdsFromFileAsRowIdsModel(),
				"Use IDs from file as row IDs (Requires unique IDs!)"));

		final JPanel panelOptions = (JPanel) super.getTab("Options");
		panelOptions.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		panelOptions.setPreferredSize(new Dimension(790, 190));
	}

	//
	// Static Methods
	//

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
				"input_file",
				nodeCreationConfig.getPortConfig().orElseThrow(IllegalStateException::new),
				RDKitFingerprintReaderV2NodeFactory.INPUT_PORT_GRP_ID_FS_CONNECTION,
				EnumConfig.create(
						SettingsModelFilterMode.FilterMode.FILE
				),
                ".fps", ".fps.gz");

		nodeCreationConfig.getURLConfig().ifPresent(urlConfiguration ->
				modelResult.setLocation(FSLocationUtil.createFromURL(urlConfiguration.getUrl().toString()))
		);

		return modelResult;
	}

	/**
	 * Creates the settings model to be used for the option to use IDs read from the fingerprint file as row IDs.
	 * 
	 * @return Settings model for using file IDs as row IDs.
	 */
	static SettingsModelBoolean createUseIdsFromFileAsRowIdsModel() {
		return new SettingsModelBoolean("use_file_ids_as_row_ids", false);
	}
}
