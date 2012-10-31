/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2012
 * Novartis Institutes for BioMedical Research
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

import java.util.Arrays;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.streamable.StreamableOperatorInternals;

/**
 * This class adds the capability to use the KNIME Streaming API 
 * to a Calculator node. 
 * 
 * @author Manuel Schwarze
 */
public abstract class AbstractRDKitStreamableCalculatorNodeModel extends
		AbstractRDKitCalculatorNodeModel {
	
	//
	// Constructors
	//
	
	/**
	 * Creates a new node model with the specified number of input and output ports.
	 * 
	 * @param nrInDataPorts Number of input ports. Must be 0 .. n.
	 * @param nrOutDataPorts Number of output ports. Must be 0 .. m.
	 */
	protected AbstractRDKitStreamableCalculatorNodeModel(int nrInDataPorts,
			int nrOutDataPorts) {
		super(nrInDataPorts, nrOutDataPorts);
	}

	/**
	 * Creates a new node model with the specified input and output ports.
	 * 
	 * @param inPortTypes Input port definitions. Must not be null.
	 * @param outPortTypes  Output port definitions. Must not be null.
	 */
	protected AbstractRDKitStreamableCalculatorNodeModel(PortType[] inPortTypes,
			PortType[] outPortTypes) {
		super(inPortTypes, outPortTypes);
	}
    
	//
	// Protected Methods
	//
	
    /**
     * This implementation creates a column rearranger by calling the method 
     * {@link #createColumnRearranger(int, DataTableSpec)} and creates based on
     * it a streamable operator.
     * 
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(
    		PartitionInfo partitionInfo, PortObjectSpec[] inSpecs)
    		throws InvalidSettingsException {
    	return createColumnRearranger(0, (DataTableSpec)inSpecs[0]).createStreamableFunction();
    }
    
    /**
     * This implementation calls the method {@link #finishExecution()}, which can
     * be overridden.
     * 
     * {@inheritDoc}
     */
    @Override
    public void finishStreamableExecution(
    		StreamableOperatorInternals internals, ExecutionContext exec,
    		PortOutput[] output) throws Exception {
		try {
			finishExecution();
		}
		catch (Exception exc) {
			LOGGER.error("Cleanup after node execution failed.", exc);
		}
    }
   
    /**
     * This implementation declares all input ports distributed and
     * streamable.
     * 
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        InputPortRole[] result = new InputPortRole[getNrInPorts()];
        Arrays.fill(result, InputPortRole.DISTRIBUTED_STREAMABLE);
        return result;
    }

    /**
     * This implementation declares all output ports distributed.
     * 
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        OutputPortRole[] result = new OutputPortRole[getNrOutPorts()];
        Arrays.fill(result, OutputPortRole.DISTRIBUTED);
        return result;
   }
}
