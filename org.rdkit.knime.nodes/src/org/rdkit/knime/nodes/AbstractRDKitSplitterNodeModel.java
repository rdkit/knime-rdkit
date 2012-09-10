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
public abstract class AbstractRDKitSplitterNodeModel extends AbstractRDKitNodeModel
	implements SplitCondition {

	//
	// Constructors
	//
	
	/**
	 * Creates a new node model with the specified number of input and output ports.
	 * 
	 * @param nrInDataPorts Number of input ports. Must be 0 .. n.
	 * @param nrOutDataPorts Number of output ports. Must be 0 .. m.
	 */
	public AbstractRDKitSplitterNodeModel(int nrInDataPorts, int nrOutDataPorts) {
		super(nrInDataPorts, nrOutDataPorts);
	}

	/**
	 * Creates a new node model with the specified input and output ports.
	 * 
	 * @param inPortTypes Input port definitions. Must not be null.
	 * @param outPortTypes  Output port definitions. Must not be null.
	 */
	public AbstractRDKitSplitterNodeModel(PortType[] inPortTypes,
			PortType[] outPortTypes) {
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
    protected DataTableSpec getOutputTableSpec(final int outPort, 
    		final DataTableSpec[] inSpecs) throws InvalidSettingsException {
    	return inSpecs[0];
    }
        
    /**
     * {@inheritDoc}
     * This method implements the generic splitting of rows of input table at port 0.
     */
    @Override
    protected BufferedDataTable[] processing(BufferedDataTable[] inData,
    		InputDataInfo[][] arrInputDataInfo, ExecutionContext exec)
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
	public int getTargetTableCount() {
		return getNrOutPorts();
	}

	/** 
	 * {@inheritDoc} 
	 */
    public abstract int determineTargetTable(int iInPort, int iRowIndex, DataRow row, InputDataInfo[] arrInputDataInfo, 
    		int iUniqueWaveId) throws InputDataInfo.EmptyCellException;
}
