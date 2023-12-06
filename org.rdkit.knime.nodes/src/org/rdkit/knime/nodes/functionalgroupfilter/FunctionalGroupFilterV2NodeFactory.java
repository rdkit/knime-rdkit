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

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.filehandling.core.port.FileSystemPortObject;

import java.util.Optional;

/**
 * {@code NodeFactory} for the RDKit based "RDKitFunctionalGroupFilter" Node.
 * 
 * @author Dillip K Mohanty
 * @author Manuel Schwarze
 * @author Roman Balabanov
 */
public class FunctionalGroupFilterV2NodeFactory extends ConfigurableNodeFactory<FunctionalGroupFilterV2NodeModel> {

	//
	// Constants
	//

	/**
	 * The file system ports group id.
	 */
	protected static final String INPUT_PORT_GRP_ID_FS_CONNECTION = "File System Connection";

	/**
	 * The molecules input ports group id.
	 */
	protected static final String INPUT_PORT_GRP_ID_MOLECULES = "RDKit Molecules";

	/**
	 * The molecules passing output ports group id.
	 */
	protected static final String OUTPUT_PORT_GRP_ID_MOLECULES_PASSED = "Molecules passing the filter";

	/**
	 * The molecules failing output ports group id.
	 */
	protected static final String OUTPUT_PORT_GRP_ID_MOLECULES_FAILED = "Molecules failing the filter";

	//
	// Public methods
	//

	/**
	 * This node does not have any views.
	 *
	 * @return Always null.
	 */
	@Override
	public NodeView<FunctionalGroupFilterV2NodeModel> createNodeView(
			final int viewIndex,
			final FunctionalGroupFilterV2NodeModel nodeModel) {
		return null;
	}

	//
	// Protected methods
	//

	@Override
	protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
		PortsConfigurationBuilder result = new PortsConfigurationBuilder();
		result.addOptionalInputPortGroup(INPUT_PORT_GRP_ID_FS_CONNECTION, FileSystemPortObject.TYPE);
		result.addFixedInputPortGroup(INPUT_PORT_GRP_ID_MOLECULES, BufferedDataTable.TYPE);
		result.addFixedOutputPortGroup(OUTPUT_PORT_GRP_ID_MOLECULES_PASSED, BufferedDataTable.TYPE);
		result.addFixedOutputPortGroup(OUTPUT_PORT_GRP_ID_MOLECULES_FAILED, BufferedDataTable.TYPE);

		return Optional.of(result);
	}

	/**
	 * Creates a model for the RDKitFunctionalGroupFilter functionality
	 * of the RDKit library. The model is derived from the
	 * abstract class AbstractRDKitNodeModel, which provides
	 * common base functionality for RDKit nodes.
	 * {@inheritDoc}
	 *
	 * @see org.rdkit.knime.nodes.AbstractRDKitNodeModel
	 */
	@Override
	protected FunctionalGroupFilterV2NodeModel createNodeModel(NodeCreationConfiguration creationConfig) {
		return new FunctionalGroupFilterV2NodeModel(creationConfig);
	}

	/**
	 * This node does not have any views.
	 * 
	 * @return Always 0.
	 */
	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	/**
	 * This node possesses a configuration dialog.
	 * 
	 * @return Always true.
	 */
	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane(NodeCreationConfiguration creationConfig) {
		return new FunctionalGroupFilterV2NodeDialog(creationConfig);
	}
}

