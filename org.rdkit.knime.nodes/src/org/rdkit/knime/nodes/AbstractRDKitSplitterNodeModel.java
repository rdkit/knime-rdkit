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
package org.rdkit.knime.nodes;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortType;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SplitCondition;

/**
 * This class adds functionality common to a Splitter node. This is a node that
 * is splitting an input table into one or multiple output tables and/or filters
 * out rows completely based on certain criteria. It does not perform any
 * further calculations or additions of rows in this basic implementation.
 * For more complex splitting operations derive a class directly from
 * {@link AbstractRDKitNodeModel}.
 * 
 * @author Manuel Schwarze
 * 
 * @see AbstractRDKitNodeModel
 */
public abstract class AbstractRDKitSplitterNodeModel extends AbstractRDKitNodeModel implements SplitCondition {

	//
	// Constructors
	//

	/**
	 * Creates a new node model with the specified number of input and output ports.
	 * 
	 * @param nrInDataPorts Number of input ports. Must be 0 .. n.
	 * @param nrOutDataPorts Number of output ports. Must be 0 .. m.
	 */
	public AbstractRDKitSplitterNodeModel(final int nrInDataPorts, final int nrOutDataPorts) {
		super(nrInDataPorts, nrOutDataPorts);
	}

	/**
	 * Creates a new node model with the specified input and output ports.
	 * 
	 * @param inPortTypes Input port definitions. Must not be null.
	 * @param outPortTypes  Output port definitions. Must not be null.
	 */
	public AbstractRDKitSplitterNodeModel(final PortType[] inPortTypes,
			final PortType[] outPortTypes) {
		super(inPortTypes, outPortTypes);
	}

	//
	// Protected Methods
	//

	/**
	 * {@inheritDoc}
	 * This implementation just returns the same specification as the input table at port 0
	 * for all specified output ports.
	 */
	@Override
	protected DataTableSpec getOutputTableSpec(final int outPort,
			final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		return inSpecs[0];
	}

	/**
	 * {@inheritDoc}
	 * This method implements the generic splitting of rows of input table at port 0.
	 */
	@Override
	protected BufferedDataTable[] processing(final BufferedDataTable[] inData,
			final InputDataInfo[][] arrInputDataInfo, final ExecutionContext exec)
					throws Exception {
		return createSplitTables(0, inData[0], arrInputDataInfo[0], exec,
				"- Splitting", this);
	}

	/**
	 * {@inheritDoc}
	 * This implementation returns by default the number of out ports.
	 * 
	 * @see #getNrOutPorts()
	 */
	@Override
	public int getTargetTableCount() {
		return getNrOutPorts();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public abstract int determineTargetTable(int iInPort, long lRowIndex, DataRow row, InputDataInfo[] arrInputDataInfo,
			long lUniqueWaveId) throws InputDataInfo.EmptyCellException;
}
