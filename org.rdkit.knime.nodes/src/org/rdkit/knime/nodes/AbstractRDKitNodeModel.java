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

package org.rdkit.knime.nodes;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.rdkit.knime.RDKitTypesPluginActivator;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.WarningConsolidator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class can act as the super class for RDKit based KNIME nodes. It provides
 * some convenience functionality that makes it very easy to create new KNIME nodes.
 * One functionality focus is on settings, the other one on cleaning up RDKit native
 * resources. When using standard KNIME SettingModel implementations, you may just
 * call {@link #registerSettings(SettingsModel, String...)} while the setting model is
 * instantiated, and this class will take care of loading, validating and saving
 * these settings. When using RDKit based objects, native code gets executed in the
 * background. One should call delete() on these objects when they are not used anymore,
 * because garbage collection does not know anything about the native memory allocation
 * and may delay the process of freeing resources. You may just call
 * {@link #markForCleanup(Object)} whenever an RDKit based object with the delete() method
 * gets encountered. At the end you may call {@link #cleanupMarkedObjects()}.
 * When using consequently template classes for standard situations this call
 * is done automatically at the end of the execute() method. Especially when using
 * large sets of data you may have nested loops with RDKit objects that should be
 * cleaned up as "local" variables when leaving the loop. To identify these "nested"
 * objects you may define a so-called "wave" identifier, and then just call
 * {@link #cleanupMarkedObjects(long)} with this wave identifier as parameter. This
 * will keep all other flagged objects alive.
 *
 * @author Manuel Schwarze
 * @author Roman Balabanov
 */
public abstract class AbstractRDKitNodeModel extends AbstractRDKitGenericNodeModel {


    //
    // Constructors
    //

    /**
     * Creates a new node model with the specified number of input and output ports.
     *
     * @param nrInDataPorts Number of input ports. Must be 0 to n.
     * @param nrOutDataPorts Number of output ports. Must be 0 to m.
     */
    @SuppressWarnings("unused")
    protected AbstractRDKitNodeModel(final int nrInDataPorts, final int nrOutDataPorts) {
        super(nrInDataPorts, nrOutDataPorts);
    }

    /**
     * Creates a new node model with the specified input and output ports.
     *
     * @param inPortTypes Input port definitions. Must not be null.
     * @param outPortTypes  Output port definitions. Must not be null.
     */
    @SuppressWarnings("unused")
    protected AbstractRDKitNodeModel(PortType[] inPortTypes, PortType[] outPortTypes) {
        super(inPortTypes, outPortTypes);
    }
    
    /**
     * Creates a new node model with the specified number of input and output ports.
     *
     * @param nrInDataPorts Number of input ports. Must be 0 .. n.
     * @param nrOutDataPorts Number of output ports. Must be 0 .. m.
     * @param arrInputPortRoles Roles for input ports or null to use default.
     *    The number must match the specified number of input data ports.
     * @param arrOutputPortRoles Roles for output ports or null to use default.
     *    The number must match the specified number of output data ports.
     */
    protected AbstractRDKitNodeModel(final int nrInDataPorts,
                                     final int nrOutDataPorts, final InputPortRole[] arrInputPortRoles,
                                     final OutputPortRole[] arrOutputPortRoles) {
        super(nrInDataPorts, nrOutDataPorts, arrInputPortRoles, arrOutputPortRoles);
    }

    /**
     * Creates a new node model with the specified input and output ports.
     *
     * @param inPortTypes Input port definitions. Must not be null.
     * @param outPortTypes  Output port definitions. Must not be null.
     * @param arrInputPortRoles Roles for input ports. The number must match the specified
     *    number of input data ports.
     * @param arrOutputPortRoles Roles for output ports. The number must match the specified
     *    number of output data ports.
     */
    protected AbstractRDKitNodeModel(final PortType[] inPortTypes,
                                     final PortType[] outPortTypes, final InputPortRole[] arrInputPortRoles,
                                     final OutputPortRole[] arrOutputPortRoles) {
        super(inPortTypes, outPortTypes, arrInputPortRoles, arrOutputPortRoles);
    }

    /**
     * Constructs new node model with configuration specified.
     *
     * @param nodeCreationConfig Node Creation Configuration instance.
     *                           Mustn't be Null.
     * @throws IllegalArgumentException if provided {@code nodeCreationConfig} is malformed.
     */
    protected AbstractRDKitNodeModel(NodeCreationConfiguration nodeCreationConfig) {
        super(nodeCreationConfig);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        return tableSpecsToObjectSpecs(configure(objectSpecsToTableSpecs(inSpecs)));
    }

    /**
     * Should be called before the own implementation starts its work.
     * This clears all warnings from the warning consolidator and checks the
     * error state of the native RDKit library.
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // Reset warning and error tracker
        getWarningConsolidator().clear();

        // Check whether native RDKit library has been loaded successfully
        RDKitTypesPluginActivator.checkErrorState();

        return new DataTableSpec[getNrOutPorts()];
    }

    @Override
    protected void preProcessing(final PortObject[] inObjects, final InputDataInfo[][] arrInputDataInfo,
                                 final ExecutionContext exec) throws Exception {
        preProcessing(objectsToTables(inObjects), arrInputDataInfo, exec);
    }

    /**
     * This method gets called from the method {@link #execute(BufferedDataTable[], ExecutionContext)}, before
     * the row-by-row processing starts. All necessary pre-calculations can be done here. Results of the method
     * should be made available through member variables, which get picked up by the other methods like
     * process(InputDataInfo[], DataRow) in the factory or
     * {@link #postProcessing(BufferedDataTable[], InputDataInfo[][], BufferedDataTable[], ExecutionContext)}
     * in the model.
     *
     * @param inData The input tables of the node.
     * @param arrInputDataInfo Information about all columns of the input tables.
     * @param exec The execution context, which was derived as sub-execution context based on the percentage
     * 		setting of #getPreProcessingPercentage(). Track the progress from 0..1.
     *
     * @throws Exception Thrown, if pre-processing fails.
     */
    protected void preProcessing(final BufferedDataTable[] inData, final InputDataInfo[][] arrInputDataInfo,
                                 final ExecutionContext exec) throws Exception {
        // Does not do anything be default
        exec.setProgress(1.0d);
    }

    @Override
    protected PortObject[] processing(PortObject[] inData, InputDataInfo[][] arrInputDataInfo, ExecutionContext exec) throws Exception {
        return tablesToObjects(processing(objectsToTables(inData), arrInputDataInfo, exec));
    }

    /**
     * This method gets called from the method {@link #execute(BufferedDataTable[], ExecutionContext)} to perform
     * the main work of the node. It is equivalent to the original KNIME execute method. However, in this implementation
     * the execute method acts more like a director to coordinate pre-, core and post-processing and to
     * clean up RDKit objects at the end. Hence, it should not be overridden. Instead, a developer would
     * override this method.
     *
     * @param inData The input tables of the node.
     * @param arrInputDataInfo Information about all columns of the input tables.
     * @param exec The execution context, which was derived as sub-execution context. Track the progress from 0..1.
     *
     * @return The result tables to be passed to the method
     * 		{@link #postProcessing(BufferedDataTable[], InputDataInfo[][], BufferedDataTable[], ExecutionContext)}.
     * 		If this method is not overridden these are the same tables that will be returned by the method
     * 		{@link #execute(BufferedDataTable[], ExecutionContext)} as the final result tables.
     *
     * @throws Exception Thrown, if post-processing fails.
     */
    protected abstract BufferedDataTable[] processing(final BufferedDataTable[] inData, InputDataInfo[][] arrInputDataInfo,
                                                      final ExecutionContext exec) throws Exception;

    @Override
    protected PortObject[] postProcessing(final PortObject[] inObjects, final InputDataInfo[][] arrInputDataInfo,
                                          final PortObject[] processingResult, final ExecutionContext exec) throws Exception {
        return tablesToObjects(postProcessing(objectsToTables(inObjects), arrInputDataInfo, objectsToTables(processingResult), exec));
    }

    /**
     * This method gets called from the method {@link #execute(BufferedDataTable[], ExecutionContext)}, after
     * the row-by-row processing has ended a new result table set has been created.
     * All necessary post-calculations can be done here, e.g. creating a completely new table by filtering
     * the intermediate table. The returned table array will be returned also from the execute method.
     *
     * @param inData The input tables of the node.
     * @param arrInputDataInfo Information about all columns of the input tables.
     * @param processingResult Tables of the core processing.
     * @param exec The execution context, which was derived as sub-execution context based on the percentage
     * 		setting of #getPreProcessingPercentage(). Track the progress from 0..1.
     *
     * @return The final result tables to be returned by {@link #execute(BufferedDataTable[], ExecutionContext)}.
     * 		By default, it just returns the tables passed in as processingResult tables.
     *
     * @throws Exception Thrown, if post-processing fails.
     */
    @SuppressWarnings("unused")
    protected BufferedDataTable[] postProcessing(final BufferedDataTable[] inData, final InputDataInfo[][] arrInputDataInfo,
                                                 final BufferedDataTable[] processingResult, final ExecutionContext exec) throws Exception {
        // Does not do anything be default
        exec.setProgress(1.0d);
        return processingResult;
    }

    @Override
    protected Map<String, Long> createWarningContextOccurrencesMap(final PortObject[] inObjects,
                                                                   final InputDataInfo[][] arrInputDataInfo,
                                                                   final PortObject[] resultData) {
        return createWarningContextOccurrencesMap(objectsToTables(inObjects), arrInputDataInfo, objectsToTables(resultData));
    }

    /**
     * Creates a map with information about occurrences of certain contexts, which are registered
     * in the warning consolidator. Such a context could for instance be the "row context", if
     * the warning consolidator was configured with it and consolidates warnings that happen based
     * on certain malformed data in input rows. In this case we would list the row number of the
     * input table (inData[0]) for this row context. The passed in parameters are for convenience
     * only, as most of the time the numbers depend on them to some degree.
     * The default implementation delivers a map, which contains only one context - the ROW_CONTEXT of
     * the consolidator - and as total number of occurrences the number of input rows in table 0.
     * Override this method for differing behavior.
     *
     * @param inData All input tables of the node with their data.
     * @param arrInputDataInfo Information about all columns in the input tables.
     * @param resultData All result tables of the node that will be returned by the execute() method.
     *
     * @return Map with number of occurrences of different contexts, e.g. encountered rows during processing.
     *
     * @see #getWarningConsolidator()
     */
    protected Map<String, Long> createWarningContextOccurrencesMap(final BufferedDataTable[] inData,
                                                                   final InputDataInfo[][] arrInputDataInfo,
                                                                   final BufferedDataTable[] resultData) {
        final Map<String, Long> mapContextOccurrences = new HashMap<>();
        mapContextOccurrences.put(
                WarningConsolidator.ROW_CONTEXT.getId(),
                Arrays.stream(inData)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .map(BufferedDataTable::size)
                        .orElse(0L)
        );

        return mapContextOccurrences;
    }

}
