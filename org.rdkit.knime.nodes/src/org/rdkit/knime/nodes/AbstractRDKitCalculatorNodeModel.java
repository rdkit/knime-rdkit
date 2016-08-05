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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellTypeConverter;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.MergeOperator;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.streamable.StreamableOperatorInternals;
import org.rdkit.knime.internals.ContextStatistics;
import org.rdkit.knime.internals.StreamingOperatorInternalsBag;
import org.rdkit.knime.types.RDKitTypeConversionErrorListener;
import org.rdkit.knime.types.RDKitTypeConversionErrorProvider;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class adds functionality common to a Calculator node. This is a node that
 * adds normally additionally columns to an input table calculating their values
 * based on input columns.
 * 
 * @author Manuel Schwarze
 */
public abstract class AbstractRDKitCalculatorNodeModel extends AbstractRDKitNodeModel {
   
   //
   // Members
   //
   
   /** Flag to determine, if distributed and streaming processing has been enabled for this node. */
   private boolean m_bDistributionAndStreamingEnabled = false;
   
   /** Context statistics object to count during process how many rows for instance were processed. */
   private ContextStatistics m_contextStatistics = null;
   
	//
	// Constructors
	//

	/**
	 * Creates a new node model with the specified number of input and output ports.
	 * Enables distribution and streaming by default for nodes, that have exactly one
	 * input port and one output port and have no pre-processing and post-processing
	 * implemented. If distribution and streaming shall be handled differently a node
	 * implementation should call the other constructor and specify input and output 
	 * port roles specifically.
	 * 
	 * @param nrInDataPorts Number of input ports. Must be 0 .. n.
	 * @param nrOutDataPorts Number of output ports. Must be 0 .. m.
	 */
	protected AbstractRDKitCalculatorNodeModel(final int nrInDataPorts,
			final int nrOutDataPorts) {
		super(nrInDataPorts, nrOutDataPorts);
		autoEnableDistributionAndStreaming();
	}

	/**
	 * Creates a new node model with the specified input and output ports.
	 * The node does not support distribution and streaming.
	 * 
	 * @param inPortTypes Input port definitions. Must not be null.
	 * @param outPortTypes  Output port definitions. Must not be null.
	 */
	protected AbstractRDKitCalculatorNodeModel(final PortType[] inPortTypes,
			final PortType[] outPortTypes) {
		super(inPortTypes, outPortTypes);
	}

   /**
    * Creates a new node model with the specified number of input and output ports.
    * 
    * @param nrInDataPorts Number of input ports. Must be 0 .. n.
    * @param nrOutDataPorts Number of output ports. Must be 0 .. m.
    * @param arrInputPortRoles Roles for input ports or null to use KNIME defaults. 
    *    The number must match the specified number of input data ports.
    * @param arrOutputPortRoles Roles for output ports or null to use KNIME defaults. 
    *    The number must match the specified number of output data ports.
    */
   protected AbstractRDKitCalculatorNodeModel(final int nrInDataPorts,
         final int nrOutDataPorts, final InputPortRole[] arrInputPortRoles,
         final OutputPortRole[] arrOutputPortRoles) {
      super(nrInDataPorts, nrOutDataPorts, arrInputPortRoles, arrOutputPortRoles);
   }

   /**
    * Creates a new node model with the specified input and output ports.
    * 
    * @param inPortTypes Input port definitions. Must not be null.
    * @param outPortTypes  Output port definitions. Must not be null.
    * @param arrInputPortRoles Roles for input ports or null to use KNIME defaults. 
    *    The number must match the specified number of input data ports.
    * @param arrOutputPortRoles Roles for output ports  or null to use KNIME defaults. 
    *    The number must match the specified number of output data ports.
    */
   protected AbstractRDKitCalculatorNodeModel(final PortType[] inPortTypes,
         final PortType[] outPortTypes, final InputPortRole[] arrInputPortRoles,
         final OutputPortRole[] arrOutputPortRoles) {
      super(inPortTypes, outPortTypes, arrInputPortRoles, arrOutputPortRoles);
   }

	//
	// Protected Methods
	//
   
   /**
    * Checks preconditions for distribution and streaming, and if they are fulfilled, enables
    * these capabilities for this node. The preconditions are:
    * Node overwrites method {@link #createStreamableOperator(PartitionInfo, PortObjectSpec[])}.
    * Node has one input port. 
    * Node has one output port.
    * Node does not do any preprocessing (method {@link #getPreProcessingPercentage()} returns 0.0d).
    * Node does not do any postprocessing (method {@link #getPostProcessingPercentage()} returns 0.0d)..
    * If node would support distribution and streaming, this method will call 
    * {@link #setPortRoles(InputPortRole[], OutputPortRole[])} with appropriate roles, otherwise
    * it would call this method with null values, which would lead to the KNIME default behavior 
    * (no streaming, no distribution). 
    * Derived classes which want to change the default behavior can override this method.
    * 
    * @return True, if distribution and streaming has been enabled. False otherwise.
    */
   protected boolean autoEnableDistributionAndStreaming() {
      boolean bEnableDistributionAndStreaming = false;
      if (getNrInPorts() == 1 && getNrOutPorts() == 1 && 
            getPreProcessingPercentage() == 0.0d && getPostProcessingPercentage() == 0.0d) {
         // Check, if node overwrites createStreamableOperator method
         try {
            if (getClass().getDeclaredMethod("createStreamableOperator", PartitionInfo.class, PortObjectSpec[].class) != null) {
               bEnableDistributionAndStreaming = true;
            }
         }
         catch (NoSuchMethodException | SecurityException exc) {
            // Method not found - Disable streaming
            bEnableDistributionAndStreaming = false;
         }
      }   
      
      if (bEnableDistributionAndStreaming) {
         setPortRoles(new InputPortRole[] { InputPortRole.DISTRIBUTED_STREAMABLE }, 
               new OutputPortRole[] { OutputPortRole.DISTRIBUTED });
      }
      else {
         setPortRoles(null, null);
      }
      
      m_bDistributionAndStreamingEnabled = bEnableDistributionAndStreaming;
      
      return bEnableDistributionAndStreaming;
   }
   
   /**
    * Enables programmatically distribution and streaming for all ports. Should be called by
    * nodes, which do not fulfil the conditions for auto-enabling of distribution and streaming,
    * but still provide that capability.
    */
   protected void enableDistributionAndStreaming() {
      InputPortRole[] inRoles = new InputPortRole[getNrInPorts()];
      Arrays.fill(inRoles, InputPortRole.DISTRIBUTED_STREAMABLE);
      OutputPortRole[] outRoles = new OutputPortRole[getNrOutPorts()];
      Arrays.fill(outRoles, OutputPortRole.DISTRIBUTED); 
      setPortRoles(inRoles, outRoles);
      m_bDistributionAndStreamingEnabled = true;
   }
   
   /**
    * The default implementation for providing distribution and streaming capabilities for
    * an RDKit Calculator node. This should be called from the 
    * {@link #createStreamableOperator(PartitionInfo, PortObjectSpec[])}
    * method, which must be overwritten by every node model that supports streaming.
    * This implementation checks if the node has been enabled for distribution and streaming
    * and returns the appropriate StreamableOperator.
    * {@inheritDoc}
    */
   protected StreamableOperator createStreamableOperatorForCalculator(PartitionInfo partitionInfo, PortObjectSpec[] inSpecs)
         throws InvalidSettingsException {
      if (m_bDistributionAndStreamingEnabled) {
         final StreamingOperatorInternalsBag emptyInternals = createInitialStreamableOperatorInternals();
         if (emptyInternals == null) {
             throw new NullPointerException("createInitialStreamableOperatorInternals" + " in class "
                 + getClass().getSimpleName() + " must not return null");
         }
         
         // Make the table specs of the input table available
         final DataTableSpec[] tableSpec = new DataTableSpec[inSpecs.length];
         for (int i = 0; i < inSpecs.length; i++) {
            tableSpec[i] = (inSpecs[i] instanceof DataTableSpec ? (DataTableSpec)inSpecs[i] : null);
         }
         
         // TODO: We need some preprocessing here for to chain 
         // 1. Auto-conversion of input rows
         // 2. Perform the real work of the node including the option to remove the source column
         // Currently, if both is combined in one column rearranger object a source column, which
         // is marked for removal is not being auto-converted.
         // Alternatively, this could also be changed in KNIME so that it auto-converts also columns,
         // which are marked for removal.
         
         return createColumnRearranger(0, tableSpec[0]).createStreamableFunction(emptyInternals);
      }
      else {
         return super.createStreamableOperator(partitionInfo, inSpecs);
      }
   }
   
   /**
    * Creates a merge operator, which merges internals together. Also, it is necessary to implement this method, so that
    * the method {@link #finishStreamableExecution(StreamableOperatorInternals, ExecutionContext, PortOutput[])}
    * is called, which is used to cleanup intermediate results.
    * {@inheritDoc}
    */
   @Override
   public MergeOperator createMergeOperator() {
      return new MergeOperator() { 
         @Override
         public StreamableOperatorInternals mergeFinal(StreamableOperatorInternals[] operators) {
            StreamingOperatorInternalsBag ret = null;
            
            if (operators != null && operators.length > 0) {
               List<StreamingOperatorInternalsBag> listCasted = new ArrayList<>();
               for (int i = 0; i < operators.length; i++) {
                  listCasted.add((StreamingOperatorInternalsBag)operators[i]);
               }
               ret = new StreamingOperatorInternalsBag().merge(listCasted);
            }
            
            return ret;
         }
      };
   }

   /**
    * Cleans up intermediate results.
    * {@inheritDoc}
    */
   @Override
   public void finishStreamableExecution(StreamableOperatorInternals internals, ExecutionContext exec,
         PortOutput[] output) throws Exception {
      try {
         if (internals instanceof StreamingOperatorInternalsBag) {
            WarningConsolidator warnings = (WarningConsolidator)((StreamingOperatorInternalsBag)internals).getItem("warnings");
            if (warnings != null) {
               generateWarnings((ContextStatistics)((StreamingOperatorInternalsBag)internals).getItem("contextStatistics"));
            }
         }
      }
      finally {
         finishExecution();
      }
   }
   
   @Override
   public StreamingOperatorInternalsBag createInitialStreamableOperatorInternals() {
      return new StreamingOperatorInternalsBag()
            .withItem("warnings", getWarningConsolidator())
            .withItem("contextStatistics", getContextStatistics());
   }
   
   @Override
   public boolean iterate(StreamableOperatorInternals internals) {
      return super.iterate(internals);
   }
   
   @Override
   public PortObjectSpec[] computeFinalOutputSpecs(StreamableOperatorInternals internals, PortObjectSpec[] inSpecs)
         throws InvalidSettingsException {
      return super.computeFinalOutputSpecs(internals, inSpecs);
   }

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
    * Returns the context statistics object used for tracking how many rows for instance
    * were processed. This is relevant for streaming when we do not have access to the entire
    * data tables content.
    * 
    * @return Context statistics. Can be null.
    */
   protected synchronized ContextStatistics getContextStatistics() {
      if (m_contextStatistics == null) {
         m_contextStatistics = new ContextStatistics();
      }
      return m_contextStatistics;
   }

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
	 * @param contextStatistics When a column arranger gets created for streaming a context
	 *      statistics object can be handed over here to be used in the first cell factory
	 *      to count processed rows. Can be null. 
	 * 
	 * @return Column Rearranger for the specified input table.
	 * 
	 * @throws InvalidSettingsException Thrown, if invalid settings are encountered while creating
	 * 		and gathering all necessary information in order to create the column rearranger object.
	 */
	protected ColumnRearranger createColumnRearranger(final int outPort, final DataTableSpec inSpec) 
	      throws InvalidSettingsException {
		if (inSpec == null) {
			throw new IllegalArgumentException("No input table specification available.");
		}

		// Create column rearranger
		final InputDataInfo[] arrInputDataInfo = createInputDataInfos(0, inSpec);
		final ColumnRearranger rearranger = new ColumnRearranger(inSpec);

		// Set new Adapter type for columns that need conversion
		for (int i = 0; i < arrInputDataInfo.length; i++) {
			if (arrInputDataInfo[i] != null && arrInputDataInfo[i].needsConversion()) {
			   DataCellTypeConverter converter = arrInputDataInfo[i].getConverter();
			   if (converter instanceof RDKitTypeConversionErrorProvider) {
			      ((RDKitTypeConversionErrorProvider) converter).addTypeConversionErrorListener(
			            new RDKitTypeConversionErrorListener() {
                  @Override
                  public void onTypeConversionError(InputDataInfo inputDataInfo, DataCell source, Exception exc) {
                     generateAutoConversionError(inputDataInfo, normalizeAutoConversionErrorMessage(exc.getMessage()));
                  }
               });
			   }
				rearranger.ensureColumnIsConverted(converter, arrInputDataInfo[i].getColumnIndex());
			}
		}

		final AbstractRDKitCellFactory[] arrOutputFactories = createOutputFactories(outPort, inSpec);
		
		// Add context statistics for tracking row processing only to the first factory
		// Otherwise we would count input rows multiple times
		ContextStatistics contextStatistics = getContextStatistics();
		if (contextStatistics != null && arrInputDataInfo.length > 0) {
		   arrOutputFactories[0].setContextStatistics(contextStatistics);
		}
		
		// Inform all factories about input data to be processed
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
