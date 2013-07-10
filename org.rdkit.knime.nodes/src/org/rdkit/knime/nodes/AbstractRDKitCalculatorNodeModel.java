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
package org.rdkit.knime.nodes;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortType;
import org.rdkit.knime.util.InputDataInfo;

/**
 * This class adds functionality common to a Calculator node. This is a node that
 * adds normally additionally columns to an input table calculating their values
 * based on input columns.
 * 
 * @author Manuel Schwarze
 */
public abstract class AbstractRDKitCalculatorNodeModel extends
AbstractRDKitNodeModel {

	//
	// Constructors
	//

	/**
	 * Creates a new node model with the specified number of input and output ports.
	 * 
	 * @param nrInDataPorts Number of input ports. Must be 0 .. n.
	 * @param nrOutDataPorts Number of output ports. Must be 0 .. m.
	 */
	protected AbstractRDKitCalculatorNodeModel(final int nrInDataPorts,
			final int nrOutDataPorts) {
		super(nrInDataPorts, nrOutDataPorts);
	}

	/**
	 * Creates a new node model with the specified input and output ports.
	 * 
	 * @param inPortTypes Input port definitions. Must not be null.
	 * @param outPortTypes  Output port definitions. Must not be null.
	 */
	protected AbstractRDKitCalculatorNodeModel(final PortType[] inPortTypes,
			final PortType[] outPortTypes) {
		super(inPortTypes, outPortTypes);
	}

	//
	// Protected Methods
	//

	/**
	 * Returns the output table specification of the specified out port. This implementation
	 * works based on a ColumnRearranger and delivers only a specification for
	 * out port 0, based on an input table on in port 0. Override this method if
	 * other behavior is needed.
	 * 
	 * @param outPort Index of output port in focus. Zero-based.
	 * @param inSpecs All input table specifications.
	 * 
	 * @return The specification of all output tables.
	 * 
	 * @throws InvalidSettingsException Thrown, if the settings are inconsistent with
	 * 		given DataTableSpec elements.
	 * 
	 * @see #createOutputFactories(int)
	 */
	@Override
	protected DataTableSpec getOutputTableSpec(final int outPort,
			final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		DataTableSpec spec = null;

		if (outPort == 0) {
			// Create the column rearranger, which will generate the spec
			spec = createColumnRearranger(0, inSpecs[0]).createSpec();
		}

		return spec;
	}

	/**
	 * This method needs to be implemented by a concrete node to generate cell factories
	 * for all output tables. As there can be more than one factory specified to generate
	 * cells for a table, it returns for one output port an array of factories. However,
	 * usually only one element will be contained in this array.
	 * 
	 * @param outPort The port number of the output table to create factories for.
	 * @param inSpec Specification of the table to be merged with new columns that
	 * 		will be generated by the factories. Can be null.
	 * 
	 * @return An array of factory objects. Should not return null. If no factories
	 * 		are used, it should return an empty array.
	 * 
	 * @throws InvalidSettingsException Thrown, if creation of output factories fails.
	 */
	protected abstract AbstractRDKitCellFactory[] createOutputFactories(int outPort, DataTableSpec inSpec)
			throws InvalidSettingsException;

	/**
	 * Creates a column rearranger based on all cell factories that are delivered
	 * by a call to {@link #createOutputFactories(int)}). These factories (usually
	 * there is just one) implement the logic to calculate new cells based on the input
	 * defined by the input data info objects delivered by a call to
	 * {@link #createInputDataInfos(DataTableSpec[])}.
	 * 
	 * @param outPort The output port of the node the column rearranger shall be created for. This
	 * 		value is used to create the output factories - a call is made to
	 * 		{@link #createOutputFactories(int)} with this outPort id.
	 * @param inSpec The specification of the table the result column rearranger shall
	 * 		be based on. Must not be null.
	 * 
	 * @return Column Rearranger for the specified input table.
	 * 
	 * @throws InvalidSettingsException Thrown, if invalid settings are encountered while creating
	 * 		and gathering all necessary information in order to create the column rearranger object.
	 */
	protected ColumnRearranger createColumnRearranger(final int outPort, final DataTableSpec inSpec) throws InvalidSettingsException {

		if (inSpec == null) {
			throw new IllegalArgumentException("No input table specification available.");
		}

		// Create column rearranger
		final InputDataInfo[] arrInputDataInfo = createInputDataInfos(0, inSpec);
		final ColumnRearranger rearranger = new ColumnRearranger(inSpec);

		// Set new Adapter type for columns that need conversion
		for (int i = 0; i < arrInputDataInfo.length; i++) {
			if (arrInputDataInfo[i] != null && arrInputDataInfo[i].needsConversion()) {
				rearranger.ensureColumnIsConverted(arrInputDataInfo[i].getConverter(), arrInputDataInfo[i].getColumnIndex());
			}
		}

		final AbstractRDKitCellFactory[] arrOutputFactories = createOutputFactories(outPort, inSpec);
		for (final AbstractRDKitCellFactory factory : arrOutputFactories) {
			factory.setInputDataInfos(arrInputDataInfo);
			rearranger.append(factory);
		}

		return rearranger;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] processing(final BufferedDataTable[] inData, final InputDataInfo[][] arrInputDataInfo,
			final ExecutionContext exec) throws Exception {
		// Generate column rearranger, which will do the work for us
		final ColumnRearranger rearranger = createColumnRearranger(0, inData[0].getDataTableSpec());

		// Generate the output table and return it.
		return new BufferedDataTable[] { exec.createColumnRearrangeTable(inData[0], rearranger, exec) };
	}
}
