/* 
 * This source code, its documentation and all related files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2012
 * Novartis Institutes for BioMedical Research
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
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
