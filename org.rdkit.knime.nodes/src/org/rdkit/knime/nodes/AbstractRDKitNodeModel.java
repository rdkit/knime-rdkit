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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellTypeConverter;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.tableview.TableContentModel;
import org.knime.core.util.MultiThreadWorker;
import org.rdkit.knime.RDKitTypesPluginActivator;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory.RowFailurePolicy;
import org.rdkit.knime.util.FilterCondition;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.RDKitObjectCleaner;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.SplitCondition;
import org.rdkit.knime.util.WarningConsolidator;

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
 * {@link #markForCleanup(Object)} whenever an RDKit based object with a delete() method
 * gets encountered. At the end you may call {@link #cleanupMarkedObjects()}.
 * When using consequently template classes for standard situations this call
 * is done automatically at the end of the execute() method. Especially when using
 * large sets of data you may have nested loops with RDKit objects that should be
 * cleaned up as "local" variables when leaving the loop. To identify these "nested"
 * objects you may define a so-called "wave" identifier, and then just call
 * {@link #cleanupMarkedObjects(int)} with this wave identifier as parameter. This
 * will keep all other flagged objects alive.
 *
 * @author Manuel Schwarze
 */
public abstract class AbstractRDKitNodeModel extends NodeModel implements RDKitObjectCleaner {

	//
	// Constants
	//

	/** Time in milliseconds we will wait until we cleanup RDKit Objects, which are marked for delayed cleanup. */
	public static final long RDKIT_OBJECT_CLEANUP_DELAY_FOR_QUARANTINE = 60000; // 60 seconds

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(AbstractRDKitNodeModel.class);

	/** Column name for internal spec table column with information about conserved internal tables. */
	private static final String PORT_TYPE_COLUMN_NAME = "PortType";

	/** Identifier for internal spec table with information about conserved internal tables. */
	private static final String INPUT_TABLE_ID = "Input";

	/** Identifier for internal spec table with information about conserved internal tables. */
	private static final String OUTPUT_TABLE_ID = "Output";

	//
	// Statics
	//

	/** Increasing thread-safe counter wave id assignment used in the context of cleaning of RDKit objects. */
	private static AtomicLong g_nextUniqueWaveId = new AtomicLong(Long.MIN_VALUE);

	//
	// Members
	//
	
	/** Stores the indexes of all input tables that cannot be handled when they have more than Integer.MAX_VALUE rows. */
	private int[] m_arrInputTableIndexesWithSizeLimit = new int[0];

	/** List to register setting models. It's important to initialize this first. */
	private final List<SettingsModel> m_listRegisteredSettings = new ArrayList<SettingsModel>();

	/**
	 * Map with information about deprecated setting keys. SettingsModel => Array of all keys. The
	 * first element in this array must be the currently used key, all following elements are considered
	 * deprecated keys, which will still be understood.
	 */
	private final Map<SettingsModel, String[]> m_mapDeprecatedSettingKeys = new HashMap<SettingsModel, String[]>();

	/**
	 * Map with information about setting keys. SettingsModel => Boolean. If set to true
	 * for a SettingsModel we declare here that this setting was added later than version 1.0 and
	 * that there might be old nodes that don't have the setting. InvalidSettingsExceptions will
	 * be ignored for such a setting.
	 */
	private final Map<SettingsModel, Boolean> m_mapIgnoreNonExistingSettingKeys = new HashMap<SettingsModel, Boolean>();

	/** List to register RDKit objects for cleanup. It's important to initialize this first. */
	private final RDKitCleanupTracker m_rdkitCleanupTracker = new RDKitCleanupTracker();

	/** Tracks warnings during execution and consolidates them. */
	private WarningConsolidator m_warnings;

	/**
	 * This is an internally used very small table that just keeps track of generated / conserved
	 * content models used for RDKit table views. It is only used, if the node model implements
	 * the TableViewSupport interface.
	 */
	private BufferedDataTable m_tableContentTableSpecs;

	/**
	 * This array contains table content models of input tables, if the node model implements
	 * the TableViewSupport interface. A content model is a wrapper around a BufferedDataTable to work with
	 * interactive view data structures.
	 */
	private TableContentModel[] m_arrInContModel;

	/**
	 * This array contains table content models of input tables, if the node model implements
	 * the TableViewSupport interface. A content model is a wrapper around a BufferedDataTable to work with
	 * interactive view data structures.
	 */
	private TableContentModel[] m_arrOutContModel;

	/**
	 * Stores an exception that occurred when executing the node. This information is used
	 * when finishExecution is called to determine how to treat RDKit objects when cleaning up.
	 */
	private Throwable m_excEncountered;

	/**
	 * Timestamp when execution started.
	 */
	private long m_lExecutionStartTs;
   
   /** Defines input port roles to express distribution and streaming capabilities, if set. */
   private InputPortRole[] m_arrInputPortRoles = null;
   
   /** Defines output port roles to express distribution and streaming capabilities, if set. */
   private OutputPortRole[] m_arrOutputPortRoles = null;   

	//
	// Constructors
	//

	/**
	 * Creates a new node model with the specified number of input and output ports.
	 *
	 * @param nrInDataPorts Number of input ports. Must be 0 .. n.
	 * @param nrOutDataPorts Number of output ports. Must be 0 .. m.
	 */
	protected AbstractRDKitNodeModel(final int nrInDataPorts, final int nrOutDataPorts) {
		super(nrInDataPorts, nrOutDataPorts);
		initializeContentTableModels(nrInDataPorts, nrOutDataPorts);
	}

	/**
	 * Creates a new node model with the specified input and output ports.
	 *
	 * @param inPortTypes Input port definitions. Must not be null.
	 * @param outPortTypes  Output port definitions. Must not be null.
	 */
	protected AbstractRDKitNodeModel(final PortType[] inPortTypes,
                                      final PortType[] outPortTypes) {
		super(inPortTypes, outPortTypes);
      initializeContentTableModels(inPortTypes.length, outPortTypes.length);
	}

	/**
	 * Constructs new node model with configuration specified.
	 *
	 * @param nodeCreationConfig Node Creation Configuration instance.
	 *                           Mustn't be Null.
	 * @throws IllegalArgumentException if provided {@code nodeCreationConfig} is malformed.
	 */
	protected AbstractRDKitNodeModel(NodeCreationConfiguration nodeCreationConfig) {
		this(
				nodeCreationConfig.getPortConfig()
						.map(PortsConfiguration::getInputPorts)
						.orElseThrow(() -> new IllegalArgumentException("No input port types defined.")),
				nodeCreationConfig.getPortConfig()
						.map(PortsConfiguration::getOutputPorts)
						.orElseThrow(() -> new IllegalArgumentException("No output port types defined."))
		);
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
      this(nrInDataPorts, nrOutDataPorts);
      
      setPortRoles(arrInputPortRoles, arrOutputPortRoles);
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
      this(inPortTypes, outPortTypes);
      
      setPortRoles(arrInputPortRoles, arrOutputPortRoles);
   }
   
	//
	// Public Methods (basically interface implementations)
	//

	public void registerInputTablesWithSizeLimits(int... indexes) {
	   if (indexes != null && indexes.length > 0) {
	      m_arrInputTableIndexesWithSizeLimit = indexes;
	   }
	   else {
	      m_arrInputTableIndexesWithSizeLimit = new int[0];
	   }
	}
	
	/**
	 * Creates a new wave id. This id must be unique in the context of the overall runtime
	 * of the Java VM, at least in the context of the same class loader and memory area.
	 *
	 * @return Unique wave id.
	 */
	@Override
	public long createUniqueCleanupWaveId() {
		long waveId = g_nextUniqueWaveId.getAndIncrement();
		
		if (waveId == Long.MAX_VALUE - 1) {
		   g_nextUniqueWaveId.set(Long.MIN_VALUE);
		}
		
		return waveId;
	}

	/**
	 * Registers an RDKit based object, which must have a delete() method implemented
	 * for freeing up resources later. The cleanup will happen for all registered
	 * objects when the method {@link #cleanupMarkedObjects()} is called.
	 * Note: If the same rdkitObject was already registered for a wave
	 * it would be cleaned up multiple times, which may have negative side effects and may
	 * even cause errors. Hence, always mark an object only once for cleanup, or - if
	 * required in certain situations - call {@link #markForCleanup(Object, boolean)}
	 * and set the last parameter to true to remove the object from the formerly registered wave.
	 *
	 * @param <T> Any class that implements a delete() method to be called to free up resources.
	 * @param rdkitObject An RDKit related object that should free resources when not
	 * 		used anymore. Can be null.
	 *
	 * @return The same object that was passed in. Null, if null was passed in.
	 *
	 * @see #cleanupMarkedObjects(int)
	 */
	@Override
	public <T extends Object> T markForCleanup(final T rdkitObject) {
		return markForCleanup(rdkitObject, 0, false);
	}

	/**
	 * Registers an RDKit based object, which must have a delete() method implemented
	 * for freeing up resources later. The cleanup will happen for all registered
	 * objects when the method {@link #cleanupMarkedObjects()} is called.
	 * Note: If the last parameter is set to true and the same rdkitObject
	 * was already registered for another wave
	 * it will be removed from the former wave list and will exist only in the wave
	 * specified here. This can be useful for instance, if an object is first marked as
	 * part of a wave and later on it is determined that it needs to live longer (e.g.
	 * without a wave). In this case the first time this method would be called with a wave id,
	 * the second time without wave id (which would internally be wave = 0).
	 *
	 * @param <T> Any class that implements a delete() method to be called to free up resources.
	 * @param rdkitObject An RDKit related object that should free resources when not
	 * 		used anymore. Can be null.
	 * @param bRemoveFromOtherWave Checks, if the object was registered before with another wave
	 * 		id, and remove it from that former wave. Usually this should be set to false for
	 * 		performance reasons.
	 *
	 * @return The same object that was passed in. Null, if null was passed in.
	 *
	 * @see #cleanupMarkedObjects(int)
	 */
	public <T extends Object> T markForCleanup(final T rdkitObject, final boolean bRemoveFromOtherWave) {
		return markForCleanup(rdkitObject, 0, bRemoveFromOtherWave);
	}

	/**
	 * Registers an RDKit based object that is used within a certain block (wave). $
	 * This object must have a delete() method implemented for freeing up resources later.
	 * The cleanup will happen for all registered objects when the method
	 * {@link #cleanupMarkedObjects(int)} is called with the same wave.
	 * Note: If the same rdkitObject was already registered for another wave (or no wave)
	 * it would be cleaned up multiple times, which may have negative side effects and may
	 * even cause errors. Hence, always mark an object only once for cleanup, or - if
	 * required in certain situations - call {@link #markForCleanup(Object, int, boolean)}
	 * and set the last parameter to true to remove the object from the formerly registered wave.
	 *
	 * @param <T> Any class that implements a delete() method to be called to free up resources.
	 * @param rdkitObject An RDKit related object that should free resources when not
	 * 		used anymore. Can be null.
	 * @param wave A number that identifies objects registered for a certain "wave".
	 *
	 * @return The same object that was passed in. Null, if null was passed in.
	 */
	@Override
	public <T extends Object> T markForCleanup(final T rdkitObject, final long wave) {
		return markForCleanup(rdkitObject, wave, false);
	}

	/**
	 * Registers an RDKit based object that is used within a certain block (wave). $
	 * This object must have a delete() method implemented for freeing up resources later.
	 * The cleanup will happen for all registered objects when the method
	 * {@link #cleanupMarkedObjects(int)} is called with the same wave.
	 * Note: If the last parameter is set to true and the same rdkitObject
	 * was already registered for another wave (or no wave)
	 * it will be removed from the former wave list and will exist only in the wave
	 * specified here. This can be useful for instance, if an object is first marked as
	 * part of a wave and later on it is determined that it needs to live longer (e.g.
	 * without a wave). In this case the first time this method would be called with a wave id,
	 * the second time without wave id (which would internally be wave = 0).
	 *
	 * @param <T> Any class that implements a delete() method to be called to free up resources.
	 * @param rdkitObject An RDKit related object that should free resources when not
	 * 		used anymore. Can be null.
	 * @param wave A number that identifies objects registered for a certain "wave".
	 * @param bRemoveFromOtherWave Checks, if the object was registered before with another wave
	 * 		id, and remove it from that former wave. Usually this should be set to false for
	 * 		performance reasons.
	 *
	 * @return The same object that was passed in. Null, if null was passed in.
	 */
	public <T extends Object> T markForCleanup(final T rdkitObject, final long wave, final boolean bRemoveFromOtherWave) {
		return m_rdkitCleanupTracker.markForCleanup(rdkitObject, wave, bRemoveFromOtherWave);
	}

	/**
	 * Frees resources for all objects that have been registered prior to this last
	 * call using the method {@link #cleanupMarkedObjects()}.
	 */
	@Override
	public void cleanupMarkedObjects() {
		m_rdkitCleanupTracker.cleanupMarkedObjects();
	}

	/**
	 * Frees resources for all objects that have been registered prior to this last
	 * call for a certain wave using the method {@link #cleanupMarkedObjects(int)}.
	 *
	 * @param wave A number that identifies objects registered for a certain "wave".
	 */
	@Override
	public void cleanupMarkedObjects(final long wave) {
		m_rdkitCleanupTracker.cleanupMarkedObjects(wave);
	}

	/**
	 * Removes all resources for all objects that have been registered prior to this last
	 * call using the method {@link #cleanupMarkedObjects()}, but delayes the cleanup
	 * process. It basically moves the objects of interest into quarantine.
	 */
	public void quarantineAndCleanupMarkedObjects() {
		m_rdkitCleanupTracker.quarantineAndCleanupMarkedObjects();
	}

	// The following methods are pre-requisites for the interactive view implementation
	// that is capable of showing additional header information (e.g. structures)
	// for the data table.

	/**
	 * Returns the list of indices of input tables, which shall be used in
	 * table views or an empty array, if nothing shall be conserved.
	 * Note: This method must always return the same result, unless the
	 * node implementation changes (e.g. new node version). Otherwise
	 * the conservation and restoration process fails.
	 *
	 * @return This default implementation returns always an empty array as
	 * 		we will not conserve any input tables by default.
	 */
	public int[] getInputTablesToConserve() {
		return new int[0];
	}

	/**
	 * Returns the list of indices of output tables, which shall be used in
	 * table views or an empty array, if nothing shall be conserved.
	 * Note: This method must always return the same result, unless the
	 * node implementation changes (e.g. new node version). Otherwise
	 * the conservation and restoration process fails.
	 *
	 * @return This default implementation returns always only the first index
	 * 		of the output tables (0) if the node model implements TableViewSupport.
	 * 		If it does not implement that interface or there are no output tables
	 * 		it will return an empty array.
	 */
	public int[] getOutputTablesToConserve() {
		if (this instanceof TableViewSupport) {
			return (getNrOutPorts() == 0 ? new int[0] : new int[] { 0 } );
		}
		else {
			return new int[0];
		}
	}

	/**
	 * Returns the content model of table data to be used in a view.
	 * In this implementation a table content model has only be
	 * created if the node model declares to implement TableViewSupport.
	 *
	 * @param bIsInputTable Set to true, if the passed in index is from an input table.
	 * 		Set to false, if the passed in index is from an output table.
	 * @param iIndex Index of a port (table).
	 *
	 * @return Table content model or null, if unavailable.
	 *
	 * @see #getInputTablesToConserve()
	 * @see #getOutputTablesToConserve()
	 */
	public TableContentModel getContentModel(final boolean bIsInputTable, final int iIndex) {
		TableContentModel contentModel = null;

		if (this instanceof TableViewSupport) {
			final TableContentModel[] arrTarget =
					(bIsInputTable ? m_arrInContModel : m_arrOutContModel);

			if (arrTarget != null && iIndex >= 0 && iIndex < arrTarget.length) {
				contentModel = arrTarget[iIndex];
			}
		}

		return contentModel;
	}

	/**
	 * In this implementation a table content model is only
	 * created if the node model declares to implement TableViewSupport).
	 * This method returns only an array of buffered data tables, if
	 * this interface is implemented (declared). It takes them in the order
	 * of declared indices, first input, then output table indices.
	 *
	 * @return Array of BDTs which are held and used internally.
	 *
	 * @see #getInputTablesToConserve()
	 * @see #getOutputTablesToConserve()
	 */
	public BufferedDataTable[] getInternalTables() {
		BufferedDataTable[] arrRet = null;
		int iConserved = 0;

		if (this instanceof TableViewSupport) {
			final int[] arrIn = m_arrInContModel != null ? getInputTablesToConserve() : new int[0];
			final int[] arrOut = m_arrOutContModel != null ? getOutputTablesToConserve() : new int[0];
			arrRet = new BufferedDataTable[arrIn.length + arrOut.length + 1];

			// Conserve spec table (always first table in internal table array, if any)
			arrRet[iConserved++] = m_tableContentTableSpecs;

			// Conserve input tables
			for (int i = 0; i < arrIn.length; i++) {
				final DataTable dataTable = m_arrInContModel[i].getDataTable();
				if (dataTable instanceof BufferedDataTable) {
					arrRet[iConserved++] = (BufferedDataTable)dataTable;
				}
			}

			// Conserve output tables
			for (int i = 0; i < arrOut.length; i++) {
				final DataTable dataTable = m_arrOutContModel[i].getDataTable();
				if (dataTable instanceof BufferedDataTable) {
					arrRet[iConserved++] = (BufferedDataTable)dataTable;
				}
			}
		};

		// We return the tables only, if everything worked as expected
		return (iConserved > 1 && iConserved == arrRet.length ? arrRet : null);
	}

	/**
	 * Allows the WorkflowManager to set information about new BDTs, for
	 * instance after load.
	 * In this implementation table content models are only filled with data
	 * if the node model declares to implement TableViewSupport.
	 * This method does not do anything unless this interface is implemented
	 * (declared).
	 *
	 * @param arrTables The array of new internal tables
	 */
	public void setInternalTables(final BufferedDataTable[] arrTables) {
		if (this instanceof TableViewSupport) {

			// No table data received - Reset existing content models
			if (arrTables == null || arrTables.length == 0) {
				resetContentTableModels();
			}

			// Process received table data after checking their correctness
			else if (precheckReceivedInternalTables(arrTables)) {
				final int[] arrIn = m_arrInContModel != null ? getInputTablesToConserve() : new int[0];
				final int[] arrOut = m_arrOutContModel != null ? getOutputTablesToConserve() : new int[0];

				int iCount = 1; // Jump over 0. element, which is our internal spec table

				// Restore input tables
				for (int i = 0; i < arrIn.length; i++) {
					m_arrInContModel[i].setDataTable(arrTables[iCount++]);
					final HiLiteHandler inProp = getInHiLiteHandler(arrIn[i]);
					m_arrInContModel[i].setHiLiteHandler(inProp);
				}

				// Restore output tables
				for (int i = 0; i < arrOut.length; i++) {
					m_arrOutContModel[i].setDataTable(arrTables[iCount++]);
					final HiLiteHandler outProp = getOutHiLiteHandler(arrOut[i]);
					m_arrOutContModel[i].setHiLiteHandler(outProp);
				}
			}

			// Pre check failed - Let's reset the internal tables and log an error
			else {
				resetContentTableModels();
				LOGGER.error("Conserved tables for RDKit Table Views are out of sync with the current node version, please rerun the node.");
			}
		}
	}
   
   //
   // Streaming API
   //
   
   /**
    * This implementation returns RDKit node specific roles for input ports, if set for the node in the constructor
    * or by calling {@link AbstractRDKitNodeModel#setPortRoles(InputPortRole[], OutputPortRole[])}.
    * Otherwise it returns the default from KNIME.
    * {@inheritDoc}
    */
   @Override
   public InputPortRole[] getInputPortRoles() {
      return m_arrInputPortRoles == null ? super.getInputPortRoles() : m_arrInputPortRoles;
   }
   
   /**
    * This implementation returns RDKit node specific roles for output ports, if set for the node in the constructor
    * or by calling {@link AbstractRDKitNodeModel#setPortRoles(InputPortRole[], OutputPortRole[])}.
    * Otherwise it returns the default from KNIME.
    * {@inheritDoc}
    */
   @Override
   public OutputPortRole[] getOutputPortRoles() {
      return m_arrOutputPortRoles == null ? super.getOutputPortRoles() : m_arrOutputPortRoles;
   }
   
	//
	// Protected Methods
	//
   
   protected void setPortRoles(InputPortRole[] arrInputPortRoles, OutputPortRole[] arrOutputPortRoles) {
      // Check arguments
      if (arrInputPortRoles != null) {
         int nrInDataPorts = getNrInPorts();
         if ((nrInDataPorts == 0 && arrInputPortRoles.length > 0) ||
             (nrInDataPorts > 0 && nrInDataPorts != arrInputPortRoles.length)) {
            throw new IllegalArgumentException("Input port roles array must have " + nrInDataPorts + 
                  " elements, the same number as specified input ports.");
         }
      }
      
      if (arrOutputPortRoles != null) {
         int nrOutDataPorts = getNrOutPorts();
         if ((nrOutDataPorts == 0 && arrOutputPortRoles.length > 0) ||
             (nrOutDataPorts > 0 && nrOutDataPorts != arrOutputPortRoles.length)) {
            throw new IllegalArgumentException("Output port roles array must have " + nrOutDataPorts + 
                  " elements, the same number as specified output ports.");
         }
      }      

      m_arrInputPortRoles = arrInputPortRoles;
      m_arrOutputPortRoles = arrOutputPortRoles;
   }


	/**
	 * Determines, if received internal tables conform with what this node can handle.
	 *
	 * @param arrTables Tables that have been sent to the method setInternalTables().
	 * 		Can be null.
	 *
	 * @return True, if table at position 0, which is an internal spec table, is conform
	 * 		with the results that the methods  {@link #getInputTablesToConserve()} and
	 * 		{@link #getOutputTablesToConserve()} return. False otherwise.
	 */
	protected boolean precheckReceivedInternalTables(final BufferedDataTable[] arrTables) {
		boolean bPrecheckOk = false;

		final int[] arrIn = m_arrInContModel != null ? getInputTablesToConserve() : new int[0];
		final int[] arrOut = m_arrOutContModel != null ? getOutputTablesToConserve() : new int[0];

		// Check number of passed in tables
		if (arrTables != null && 1 + arrIn.length + arrOut.length == arrTables.length && arrTables.length > 1) {
			// Do a deeper check reading from the internal spec table and comparing, if
			// what was stored is still what we can handle (basically, if input and output
			// tables are still the same)
			final BufferedDataTable tableContentTableSpecs = arrTables[0];
			CloseableRowIterator specIterator = null;

			try {
				final int iColIndex = tableContentTableSpecs.getDataTableSpec().findColumnIndex(PORT_TYPE_COLUMN_NAME);
				specIterator = tableContentTableSpecs.iterator();

				// Check input tables
				for (int i = 0; i < arrIn.length; i++) {
					final String strTableType = ((StringValue)(specIterator.next().getCell(iColIndex))).getStringValue();
					if (!INPUT_TABLE_ID.equals(strTableType)) {
						throw new RuntimeException("Unexpected input table found.");
					}
				}

				// Check output tables
				for (int i = 0; i < arrOut.length; i++) {
					final String strTableType = ((StringValue)(specIterator.next().getCell(iColIndex))).getStringValue();
					if (!OUTPUT_TABLE_ID.equals(strTableType)) {
						throw new RuntimeException("Unexpected input table found.");
					}
				}

				if (specIterator.hasNext()) {
					throw new RuntimeException("More tables than expected found.");
				}

				bPrecheckOk = true;
			}
			catch (final Exception exc) {
				bPrecheckOk = false;
				LOGGER.debug("Reading internal tables failed: " + exc.getMessage());
			}
			finally {
				if (specIterator != null) {
					specIterator.close();
				}
			}
		}

		return bPrecheckOk;
	}

	/**
	 * Resets all internally used content table models which are used normally
	 * in Table Views.
	 */
	protected void resetContentTableModels() {
		if (m_arrInContModel != null) {
			for (int i = 0; i < m_arrInContModel.length; i++) {
				m_arrInContModel[i].setDataTable(null);
				m_arrInContModel[i].setHiLiteHandler(null);
			}
		}
		if (m_arrOutContModel != null) {
			for (int i = 0; i < m_arrOutContModel.length; i++) {
				m_arrOutContModel[i].setDataTable(null);
				m_arrOutContModel[i].setHiLiteHandler(null);
			}
		}
	}

	/**
	 * This method gets called in the very beginning from the constructors to setup
	 * structures for internal content table model storage. These structures are
	 * used for RDKit table views. This method executes only, if the node model
	 * implements TableViewSupport.
	 *
	 * @param inPorts Number of in ports.
	 * @param iOutPorts Number of out ports.
	 */
	protected void initializeContentTableModels(final int inPorts, final int iOutPorts) {
		if (this instanceof TableViewSupport) {
			// Create array for input table content models
			final int[] arrIn = getInputTablesToConserve();
			m_arrInContModel = new TableContentModel[arrIn == null ? 0 : arrIn.length];

			// All models have empty content in the beginning
			for (int i = 0; i < m_arrInContModel.length; i++) {
				m_arrInContModel[i] = new TableContentModel();
				m_arrInContModel[i].setSortingAllowed(true);
			}

			// Create array for input table content models
			final int[] arrOut = getOutputTablesToConserve();
			m_arrOutContModel = new TableContentModel[arrOut == null ? 0 : arrOut.length];

			// All models have empty content in the beginning
			for (int i = 0; i < m_arrOutContModel.length; i++) {
				m_arrOutContModel[i] = new TableContentModel();
				m_arrOutContModel[i].setSortingAllowed(true);
			}
		}
		else {
			m_arrInContModel = null;
			m_arrOutContModel = null;
		}
	}

	/**
	 * {@inheritDoc}
	 * This method does usually not do anything in this implementation unless the node model
	 * implements the interface BufferedDataContainer (which is extended by the interface
	 * InteractiveTableSupport). In this case it resets the internally held table content model.
	 */
	@Override
	protected void reset() {
		if (this instanceof BufferedDataTableHolder) {
			// Reset input models to have empty content and no hiliting handler attached
			for (int i = 0; i < m_arrInContModel.length; i++) {
				m_arrInContModel[i].setDataTable(null);
				m_arrInContModel[i].setHiLiteHandler(null);
				assert (!m_arrInContModel[i].hasData());
			}

			// Reset output models to have empty content and no hiliting handler attached
			for (int i = 0; i < m_arrOutContModel.length; i++) {
				m_arrOutContModel[i].setDataTable(null);
				m_arrOutContModel[i].setHiLiteHandler(null);
				assert (!m_arrOutContModel[i].hasData());
			}
		}
	}

	/**
	 * Should be called before the own implementation starts its work.
	 * This clears all warnings from the warning consolidator and checks the
	 * error state of the native RDKit library.
	 * {@inheritDoc}
	 */
	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
			throws InvalidSettingsException {
		// Reset warning and error tracker
		getWarningConsolidator().clear();

		// Check whether native RDKit library has been loaded successfully
		RDKitTypesPluginActivator.checkErrorState();

		return new PortObjectSpec[getNrOutPorts()];
	}

	/**
	 * In this implementation this method acts as director of calling
	 * {@link #preProcessing(PortObject[], InputDataInfo[][], ExecutionContext)},
	 * {@link #processing(PortObject[], InputDataInfo[][], ExecutionContext)} and
	 * {@link #postProcessing(PortObject[], InputDataInfo[][], PortObject[], ExecutionContext)}.
	 * Derived classes need to implement at least the processing() method. It also
	 * takes responsible of directing the warning generation as well as the clean up of
	 * RDKit based resources. Do not override this method. Rather override one listed
	 * under the "@see" section.
	 *
	 * {@inheritDoc}
	 *
	 * @see #createInputDataInfos(int, DataTableSpec)
	 * @see #getPreProcessingPercentage()
	 * @see #getPostProcessingPercentage()
	 * @see #preProcessing(PortObject[], InputDataInfo[][], ExecutionContext)
	 * @see #processing(PortObject[], InputDataInfo[][], ExecutionContext)
	 * @see #postProcessing(PortObject[], InputDataInfo[][], PortObject[], ExecutionContext)
	 * @see #createWarningContextOccurrencesMap(PortObject[], InputDataInfo[][], PortObject[])
	 */
	@Override
	protected PortObject[] execute(final PortObject[] inObjects,
								   final ExecutionContext exec) throws Exception {
	   // Check table size limitations
	   if (inObjects != null) {
   	   for (int i = 0; i < m_arrInputTableIndexesWithSizeLimit.length; i++) {
   	      if (m_arrInputTableIndexesWithSizeLimit[i] < inObjects.length) {
   	         if (inObjects[m_arrInputTableIndexesWithSizeLimit[i]] != null &&
   	               inObjects[m_arrInputTableIndexesWithSizeLimit[i]] instanceof BufferedDataTable dataTable &&
   	               dataTable.size() > Integer.MAX_VALUE) {
   	            throw new UnsupportedOperationException(
   	                "This RDKit Node does not support more than " + Integer.MAX_VALUE + " rows for the "
   	                   + (m_arrInputTableIndexesWithSizeLimit[i] + 1) + ". input table.");
   	         }
   	      }
   	   }
	   }
	   
		m_lExecutionStartTs = System.currentTimeMillis();
		PortObject[] arrConvertedObjects = null;
		PortObject[] arrResultObjects = null;
		m_excEncountered = null;

		try {
			// We use a nested try / catch block here as a trick to know about the
			// exception in the finally block
			try {
				// Reset warning and error tracker
				getWarningConsolidator().clear();

				// Determine progress weights
				final double dPercConv = 0.05d;
				final double dPercPre = correctPercentage(getPreProcessingPercentage());
				final double dPercPost = correctPercentage(getPostProcessingPercentage());
				final double dPercCore = correctPercentage(1.0d - dPercConv - dPercPre - dPercPost);

				// Get settings and input information, e.g. index information
				InputDataInfo[][] arrInputDataInfo = createInputDataInfos(getInputTableSpecs(objectsToTables(inObjects)));

				// Conversion of input data to adapter cells if required and recreate input data info afterwards
				arrConvertedObjects = convertInputTables(inObjects, arrInputDataInfo, exec.createSubExecutionContext(dPercConv));
				arrInputDataInfo = createInputDataInfos(getInputTableSpecs(objectsToTables(arrConvertedObjects)));

				// Pre-processing
				preProcessing(arrConvertedObjects, arrInputDataInfo, exec.createSubExecutionContext(dPercPre));

				// Core-processing
				arrResultObjects = processing(arrConvertedObjects, arrInputDataInfo, exec.createSubExecutionContext(dPercCore));

				// Post-processing
				arrResultObjects = postProcessing(arrConvertedObjects, arrInputDataInfo, arrResultObjects,
						exec.createSubExecutionContext(dPercPost));

				// Show a warning, if errors were encountered
				generateWarnings(createWarningContextOccurrencesMap(arrConvertedObjects, arrInputDataInfo, arrResultObjects));
			}
			catch (final Throwable exc) {
				m_excEncountered = exc;
			}
		}
		finally {
			finishExecution();
		}

		// Prepares conservation of certain tables
		// if the derived node implements the TableViewSupport interface.
		if (this instanceof TableViewSupport) {
			conserveTables(exec, objectsToTables(inObjects), objectsToTables(arrResultObjects));
		}

		return arrResultObjects;
	}

	/**
	 * This method gets called from the method {@link #getOutputTableSpecs(DataTableSpec[])}, which is
	 * usually called at the end of the {@link #configure(DataTableSpec[])} method. It converts adaptable
	 * column types into appropriate Adapter Cells and delivers new table specs tables that should be used
	 * for further processing in the methods {@link #getOutputTableSpec(int, DataTableSpec[])}
	 * as if they were the original input table specs. The only changes are column types.
	 *
	 * @param inSpec The input table specs of the node. Can be null.
	 * @param arrInputDataInfo Information about all columns of the input tables.
	 *
	 * @return Original table specs, if no conversion is necessary. Converted input table specs, if conversion took place.
	 * 		They have exactly the same structure as the original input table specs, only
	 * 		input columns that had values compatible with an Adapter Converter are changed, e.g. a SMILES
	 * 		cell would be an RDKit Adapter Cell now.
	 */
	protected DataTableSpec[] convertInputTables(final DataTableSpec[] inSpec, final InputDataInfo[][] arrInputDataInfo) {
		DataTableSpec[] arrConvertedSpecs = null;

		if (inSpec != null) {
			arrConvertedSpecs = new DataTableSpec[inSpec.length];

			// Setup conversions
			for (int i = 0; i < inSpec.length; i++) {
				arrConvertedSpecs[i] = inSpec[i];

				// Conversion makes only sense, if we have an input table and input columns defined
				if (inSpec[i] != null && arrInputDataInfo != null && arrInputDataInfo.length > 0 &&
						arrInputDataInfo[i] != null && arrInputDataInfo[i].length > 0) {

					// Check, if an input column is compatible with an acceptable class that needs conversion
					final List<InputDataInfo> listConversionColumns = new ArrayList<InputDataInfo>(5);

					for (final InputDataInfo inputInfo : arrInputDataInfo[i]) {
						if (inputInfo != null && inputInfo.needsConversion()) {
							listConversionColumns.add(inputInfo);
						}
					}

					// Setup a column rearranger and get new table spec, but only if changes are necessary
					if (!listConversionColumns.isEmpty()) {
						final ColumnRearranger rearranger = new ColumnRearranger(inSpec[i]);
						for (final InputDataInfo inputDataInfo : listConversionColumns) {
							final DataCellTypeConverter converter = inputDataInfo.getConverter();
							rearranger.ensureColumnIsConverted(converter, inputDataInfo.getColumnIndex());
						}
						arrConvertedSpecs[i] = rearranger.createSpec();
					}
				}
			}
		}

		return arrConvertedSpecs;
	}

	/**
	 * This method gets called from the method {@link #execute(PortObject[], ExecutionContext)}, before
	 * the pre-processing starts. It converts adaptable column types into appropriate Adapter Cells and delivers
	 * tables that should be used for further processing as if they were the original input tables. The only
	 * changes are column types.
	 *
	 * @param inData The input port objects of the node. Can be null.
	 * @param arrInputDataInfo Information about all columns of the input tables.
	 * @param exec The execution context, which was derived as sub-execution context based on 5%. Track the progress from 0..1.
	 *
	 * @return Original tables, if no conversion is necessary. Converted input tables, if conversion took place.
	 * 		They have exactly the same structure as the original input table, only
	 * 		input columns that had values compatible with an Adapter Converter are changed, e.g. a SMILES
	 * 		cell would be an RDKit Adapter Cell now.
	 *
	 * @throws Exception Thrown, if conversion fails.
	 */
	protected PortObject[] convertInputTables(final PortObject[] inData, final InputDataInfo[][] arrInputDataInfo,
			final ExecutionContext exec) throws Exception {
		PortObject[] arrConvertedTables = null;

		if (inData != null) {
			arrConvertedTables = new PortObject[inData.length];

			// Setup conversions
			final Map<Integer, ColumnRearranger> mapColumnRearrangers = new HashMap<Integer, ColumnRearranger>();
			for (int iTableIndex = 0; iTableIndex < inData.length; iTableIndex++) {
				arrConvertedTables[iTableIndex] = inData[iTableIndex];

				// Conversion makes only sense, if we have an input table and input columns defined
				if (inData[iTableIndex] != null && arrInputDataInfo != null && arrInputDataInfo.length > 0 &&
						arrInputDataInfo[iTableIndex] != null && arrInputDataInfo[iTableIndex].length > 0 &&
						inData[iTableIndex] instanceof BufferedDataTable) {

					// Check, if an input column is compatible with an acceptable class that needs conversion
					final List<InputDataInfo> listConversionColumns = new ArrayList<InputDataInfo>(5);

					for (final InputDataInfo inputInfo : arrInputDataInfo[iTableIndex]) {
						if (inputInfo != null && inputInfo.needsConversion()) {
							listConversionColumns.add(inputInfo);
						}
					}

					// Setup a column rearranger and run it for the input table, but only if changes are necessary
					if (!listConversionColumns.isEmpty()) {
						final int[] arrColumnIndex = new int[listConversionColumns.size()];
						final DataTableSpec tableSpec = ((BufferedDataTable) inData[iTableIndex]).getDataTableSpec();
						final ColumnRearranger rearranger = new ColumnRearranger(tableSpec);
						int iCount = 0;
						for (final InputDataInfo inputDataInfo : listConversionColumns) {
							final DataCellTypeConverter converter = inputDataInfo.getConverter();
							final int iColumnIndex = inputDataInfo.getColumnIndex();
							arrColumnIndex[iCount++] = iColumnIndex;
							rearranger.ensureColumnIsConverted(converter, iColumnIndex);
						}

						// Part 1 of workaround for a bug in ColumnRearranger - without adding new cells we get an
						// error from internal hashmap: Illegal initial capacity: -2147483648
						// Additionally, we use this factory to check the success of the auto conversion
						// and to generate warnings in case that conversion failed.
						rearranger.append(new AbstractCellFactory(true, new DataColumnSpecCreator(
								SettingsUtils.makeColumnNameUnique("EmptyCells", tableSpec, null), IntCell.TYPE).createSpec()) {
							private final DataCell[] EMPTY_CELLS = new DataCell[] { DataType.getMissingCell() };
							@Override
							public DataCell[] getCells(final DataRow row) {
								if (row != null) {
									for (int i = 0; i < arrColumnIndex.length; i++) {
										final DataCell cellConverted = row.getCell(arrColumnIndex[i]);
										if (cellConverted instanceof MissingCell) {
											final String strError = ((MissingCell)cellConverted).getError();
											if (strError != null) {
												try {
													generateAutoConversionError(listConversionColumns.get(i), 
													      normalizeAutoConversionErrorMessage(strError));
												}
												catch (final Exception exc) {
													LOGGER.error(exc);
													// Ignore it as this would lead to a deadlock in KNIME's rearranger handling
												}
											}
										}
									}
								}

								return EMPTY_CELLS;
							}
						});

						mapColumnRearrangers.put(iTableIndex, rearranger);
					}
				}
			}

			final int iCountTablesToBeConverted = mapColumnRearrangers.size();
			int iCount = 1;
			for (int iTableIndex = 0; iTableIndex < inData.length; iTableIndex++) {
				exec.setMessage("Converting input tables for processing (" + iCount + " of " + iCountTablesToBeConverted + ") ...");
				final ColumnRearranger rearranger = mapColumnRearrangers.get(iTableIndex);
				if (rearranger != null && inData[iTableIndex] instanceof BufferedDataTable) {
					iCount++;
					arrConvertedTables[iTableIndex] = exec.createColumnRearrangeTable(((BufferedDataTable) inData[iTableIndex]),
							rearranger, exec.createSubProgress(1.0d / iCountTablesToBeConverted / 2.0d));

					// Part 2 of workaround from above: We need to remove the last column again
					final DataTableSpec tableSpec = ((BufferedDataTable) arrConvertedTables[iTableIndex]).getDataTableSpec();
					final ColumnRearranger rearrangerWorkaround = new ColumnRearranger(tableSpec);
					rearrangerWorkaround.remove(tableSpec.getNumColumns() - 1);
					arrConvertedTables[iTableIndex] = exec.createColumnRearrangeTable(((BufferedDataTable) arrConvertedTables[iTableIndex]),
							rearrangerWorkaround, exec.createSubProgress(1.0d / iCountTablesToBeConverted / 2.0d));
				}
			}
		}

		exec.setProgress(1.0d);
		return arrConvertedTables;
	}

	/**
	 * This method gets called when the auto conversion of a SMILES or SDF cell fails and no RDKit cell
	 * could be generated. Overwriting this method gives a node the opportunity to handle the warning
	 * differently than normal, e.g. to suppress it or to change the wording.
	 * 
	 * @param inputDataInfo Input data info for the column that failed a cell conversion.
	 * @param strError Short error for logging.
	 */
	protected void generateAutoConversionError(final InputDataInfo inputDataInfo, final String strError) {
	   String strColumnInfo = (inputDataInfo == null ? "unknown column" : 
	      "column '" + inputDataInfo.getColumnSpec().getName() + "'");
		getWarningConsolidator().saveWarning(WarningConsolidator.ROW_CONTEXT.getId(), "Auto conversion in "
		      + strColumnInfo + " failed: " + strError + " - Using empty cell.");
	}
	
	/**
	 * A normal auto conversion error message contains information about the failed molecule, which
	 * is for consolidated warning messages not useful. This method normalizes such a message and gets
	 * rid of dynamic parts.
	 * 
	 * @param strError The full error message received from the conversion exception. Can be null.
	 * 
	 * @return Shortened message without molecule information.
	 */
	protected String normalizeAutoConversionErrorMessage(final String strError) {
      String strRet = (strError == null || strError.trim().isEmpty() ? "Unknown error." : strError);
      int iIndex = strRet.indexOf("\n");
      if (iIndex >= 0) {
         strRet = strRet.substring(0, iIndex);
         iIndex = strRet.lastIndexOf(" for");
         if (iIndex >= 0) {
            strRet = strRet.substring(0, iIndex);
         }
      }

      return strRet;
   }


	/**
	 * This method gets called from the method {@link #execute(BufferedDataTable[], ExecutionContext)}, before
	 * the row-by-row processing starts. All necessary pre-calculations can be done here. Results of the method
	 * should be made available through member variables, which get picked up by the other methods like
	 * process(InputDataInfo[], DataRow) in the factory or
	 * {@link #postProcessing(PortObject[], InputDataInfo[][], PortObject[], ExecutionContext)}
	 * in the model.
	 *
	 * @param inObjects The input port objects of the node.
	 * @param arrInputDataInfo Information about all columns of the input tables.
	 * @param exec The execution context, which was derived as sub-execution context based on the percentage
	 * 		setting of #getPreProcessingPercentage(). Track the progress from 0..1.
	 *
	 * @throws Exception Thrown, if pre-processing fails.
	 */
	protected void preProcessing(final PortObject[] inObjects, final InputDataInfo[][] arrInputDataInfo,
			final ExecutionContext exec) throws Exception {
		// Does not do anything be default
		exec.setProgress(1.0d);
	}

	/**
	 * This method gets called from the method {@link #execute(BufferedDataTable[], ExecutionContext)} to perform
	 * the main work of the node. It is equivalent to the original KNIME execute method. However, in this implementation
	 * the execute method acts more like a director to coordinate pre-, core and post-processing and to
	 * cleanup RDKit objects at the end. Hence, it should not be overridden. Instead a developer would
	 * override this method.
	 *
	 * @param inObjects The input port objects of the node.
	 * @param arrInputDataInfo Information about all columns of the input tables.
	 * @param exec The execution context, which was derived as sub-execution context. Track the progress from 0..1.
	 *
	 * @return The result tables to be passed to the method
	 * 		{@link #postProcessing(PortObject[], InputDataInfo[][], PortObject[], ExecutionContext)}.
	 * 		If this method is not overridden these are the same tables that will be returned by the method
	 * 		{@link #execute(PortObject[], ExecutionContext)} as the final result tables.
	 *
	 * @throws Exception Thrown, if post-processing fails.
	 */
	protected abstract PortObject[] processing(final PortObject[] inObjects, InputDataInfo[][] arrInputDataInfo,
			final ExecutionContext exec) throws Exception;

	/**
	 * This method gets called from the method {@link #execute(PortObject[], ExecutionContext)}, after
	 * the row-by-row processing has ended a new result table set has been created.
	 * All necessary post-calculations can be done here, e.g. creating a completely new table by filtering
	 * the intermediate table. The returned table array will be returned also from the execute method.
	 *
	 * @param inObjects The input port objects of the node.
	 * @param arrInputDataInfo Information about all columns of the input tables.
	 * @param processingResult Tables of the core processing.
	 * @param exec The execution context, which was derived as sub-execution context based on the percentage
	 * 		setting of #getPreProcessingPercentage(). Track the progress from 0..1.
	 *
	 * @return The final result tables to be returned by {@link #execute(PortObject[], ExecutionContext)}.
	 * 		By default it just returns the tables passed in as processingResult tables.
	 *
	 * @throws Exception Thrown, if post-processing fails.
	 */
	protected PortObject[] postProcessing(final PortObject[] inObjects, final InputDataInfo[][] arrInputDataInfo,
			final PortObject[] processingResult, final ExecutionContext exec) throws Exception {
		// Does not do anything be default
		exec.setProgress(1.0d);
		return processingResult;
	}

	/**
	 * This method gets called at the very end of the node execution. It is called also
	 * after the user canceled the execution. It can be overridden to cleanup all
	 * intermediate results, e.g. arrays, which have been created to pass
	 * over values from pre-processing to processing to post-processing steps.
	 * Always check for null before calling cleanup functionality on resources. They
	 * may have never instancianted, if the user canceled before the resource was created.
	 * By default, this method is not doing anything. Exceptions thrown
	 * from this method will be caught and logged as a warning, but will not
	 * have any effect on the process flow.
	 */
	protected void cleanupIntermediateResults() {
		// Does not do anything be default.
	}

	/**
	 * Called at the end of node execution. All overall cleanup code is placed here.
	 * Nodes which override this method must call super to avoid memory leaks.
	 */
	protected void finishExecution() throws Exception {
		// Free all RDKit resources - but carefully consider different scenarios
		try {
			// 1. Everything went well - no exception was thrown. Everything should be ready for cleanup
			if (m_excEncountered == null) {
				cleanupMarkedObjects();
			}
			// 2. Something went wrong, maybe the user canceled - if we have executed the node using
			//    multiple threads, some of them could be still using RDKit Objects
			else {
				quarantineAndCleanupMarkedObjects();
			}
		}
		catch (final Exception excCleanup) {
			LOGGER.warn("Cleanup of RDKit objects failed. " + excCleanup.getMessage());
			LOGGER.debug("Cleanup up failure stacktrace", excCleanup);
		}

		// Cleanup all intermediate results
		try {
			cleanupIntermediateResults();
		}
		catch (final Exception excCleanup) {
			LOGGER.warn("Cleanup of intermediate execution results failed. " + excCleanup.getMessage());
			LOGGER.debug("Cleanup up failure stacktrace", excCleanup);
		}

		final long lEnd = System.currentTimeMillis();
		LOGGER.info("Execution of " + getClass().getSimpleName() + " took " + (lEnd - m_lExecutionStartTs) + "ms.");

		if (m_excEncountered != null) {
			if (m_excEncountered instanceof Exception) {
				// E.g. CanceledExecutionException
				throw (Exception)m_excEncountered;
			}
			else if (m_excEncountered instanceof Error){
				// E.g. OutOfMemoryException
				throw (Error)m_excEncountered;
			}
			else {
				// Normally, you should not extend directly Throwable, so this case should not really happen
				throw new RuntimeException(m_excEncountered);
			}
		}
	}

	/**
	 * Returns the percentage of pre-processing activities from the total execution.
	 *
	 * @return Percentage of pre-processing. Default is 0.0d.
	 */
	protected double getPreProcessingPercentage() {
		return 0.0d;
	}

	/**
	 * Returns the percentage of post-processing activities from the total execution.
	 *
	 * @return Percentage of post-processing. Default is 0.0d.
	 */
	protected double getPostProcessingPercentage() {
		return 0.0d;
	}

	/**
	 * Corrects the passed in percentage value, if necessary and logs a warning.
	 *
	 * @param dPerc Percentage between 0.0 and 1.0 to be checked and corrected.
	 *
	 * @return Correct percentage value between 0.0 and 1.0.
	 */
	protected double correctPercentage(final double dPerc) {
		double dRetPerc = dPerc;

		if (dRetPerc < 0.0d) {
			dRetPerc = 0.0d;
		}
		if (dRetPerc > 1.0d) {
			dRetPerc = 1.0d;
		}
		if (dPerc != dRetPerc) {
			LOGGER.warn("Incorrect percentage value encountered: " + dPerc + " - Corrected to " + dRetPerc);
		}

		return dRetPerc;
	}

	/**
	 * Creates a map with information about occurrences of certain contexts, which are registered
	 * in the warning consolidator. Such a context could for instance be the "row context", if
	 * the warning consolidator was configured with it and consolidates warnings that happen based
	 * on certain malformed data in input rows. In this case we would list the row number of the
	 * input table (inObjects[0]) for this row context. The passed in parameters are for convenience
	 * only, as most of the time the numbers depend on them to some degree.
	 * The default implementation delivers a map, which contains only one context - the ROW_CONTEXT of
	 * the consolidator - and as total number of occurrences the number of input rows in table 0.
	 * Override this method for differing behavior.
	 *
	 * @param inObjects All input tables of the node with their data.
	 * @param arrInputDataInfo Information about all columns in the input tables.
	 * @param resultData All result tables of the node that will be returned by the execute() method.
	 *
	 * @return Map with number of occurrences of different contexts, e.g. encountered rows during processing.
	 *
	 * @see #getWarningConsolidator()
	 */
	protected Map<String, Long> createWarningContextOccurrencesMap(final PortObject[] inObjects,
			final InputDataInfo[][] arrInputDataInfo, final PortObject[] resultData) {

		final Map<String, Long> mapContextOccurrences = new HashMap<String, Long>();
		mapContextOccurrences.put(
				WarningConsolidator.ROW_CONTEXT.getId(),
				Arrays.stream(inObjects)
						.map(portObject -> portObject instanceof BufferedDataTable dataTable ? dataTable : null)
						.filter(Objects::nonNull)
						.findFirst()
						.map(BufferedDataTable::size)
						.orElse(0L)
		);

		return mapContextOccurrences;
	}

	/**
	 * {@inheritDoc}
	 * This method does not do anything in this implementation.
	 */
	@Override
	protected void loadInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// Nothing to load
	}

	/**
	 * {@inheritDoc}
	 * This method does not do anything in this implementation.
	 */
	@Override
	protected void saveInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// Nothing to save
	}

	/**
	 * {@inheritDoc}
	 * This implementation loads all setting models, which have been
	 * registered before with the method {@link #registerSettings(SettingsModel, String...)}.
	 * It is capable of handling deprecated setting keys as well as new setting keys
	 * that were not always present in node instances. How a setting is handled
	 * depends on how it was registered.
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		for (final SettingsModel setting : m_listRegisteredSettings) {
			final boolean bIgnoreNonExistingSetting = (m_mapIgnoreNonExistingSettingKeys.containsKey(setting) && m_mapIgnoreNonExistingSettingKeys.get(setting) == true);
			final String nodeName = SettingsUtils.getNodeName(settings);
			final String[] arrDeprecatedKeys = m_mapDeprecatedSettingKeys.get(setting);

			if (arrDeprecatedKeys != null && arrDeprecatedKeys.length > 1) {
				try {
					setting.loadSettingsFrom(settings);
				}
				catch (final InvalidSettingsException excOrig) {
					LOGGER.debug("Caught invalid setting for " + setting.toString() + " - Trying deprecated keys instead ...");
					final String newKey = arrDeprecatedKeys[0];
					int iErrorCounter = 0;

					// Try deprecated keys
					for (int i = 1; i < arrDeprecatedKeys.length; i++) {
						final String deprKey = arrDeprecatedKeys[i];

						try {
							if (settings.containsKey(deprKey)) {
								final ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
								settings.saveToXML(out);
								String strSettingsContent = out.toString();
								strSettingsContent = strSettingsContent.replace(
										"<config key=\"" + deprKey + "\"", "<config key=\"" + newKey + "\"");
								strSettingsContent = strSettingsContent.replace(
										"<entry key=\"" + deprKey + "\"", "<entry key=\"" + newKey + "\"");
								strSettingsContent = strSettingsContent.replace(
										"<config key=\"" + deprKey + "_Internals\"", "<config key=\"" + newKey + "_Internals\"");
								final NodeSettingsRO deprSettings = NodeSettings.loadFromXML(
										new ByteArrayInputStream(strSettingsContent.getBytes()));
								setting.loadSettingsFrom(deprSettings);
								break;
							}
						}
						catch (final InvalidSettingsException excDepr) {
							iErrorCounter++;
						}
						catch (final Exception exc) {
							LOGGER.debug("Another exception occurred when using deprecated key '" + deprKey + "'.", exc);
							iErrorCounter++;
						}
					}

					if (iErrorCounter == arrDeprecatedKeys.length - 1) {
						LOGGER.debug("Deprecated keys did not work either. - Giving up.");

						if (bIgnoreNonExistingSetting) {
							LOGGER.warn("The new setting '" + newKey + "' was not known when the node" +
									(nodeName == null ? "" : " '" + nodeName + "'") + " was saved. " +
									"Please save the workflow again to include it for the future.");
						}
						else {
							throw excOrig;
						}
					}
				}
			}
			else {
				try {
					setting.loadSettingsFrom(settings);
				}
				catch (final InvalidSettingsException excOrig) {
					if (bIgnoreNonExistingSetting) {
						LOGGER.debug("A new setting was not known when the node" +
								(nodeName == null ? "" : " '" + nodeName + "'") + " was saved.");
					}
					else {
						throw excOrig;
					}
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * This implementation saves all setting models, which have been
	 * registered before with the method {@link #registerSettings(SettingsModel, String...)}.
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		for (final SettingsModel setting : m_listRegisteredSettings) {
			setting.saveSettingsTo(settings);
		}
	}

	/**
	 * {@inheritDoc}
	 * This implementation validates all setting models, which have been
	 * registered before with the method {@link #registerSettings(SettingsModel, String...)}.
	 * It is capable of handling deprecated setting keys as well as new setting keys
	 * that were not always present in node instances. How a setting is handled
	 * depends on how it was registered.
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		for (final SettingsModel setting : m_listRegisteredSettings) {
			final boolean bIgnoreNonExistingSetting = (m_mapIgnoreNonExistingSettingKeys.containsKey(setting) &&
					m_mapIgnoreNonExistingSettingKeys.get(setting) == true);
			final String[] arrDeprecatedKeys = m_mapDeprecatedSettingKeys.get(setting);

			if (arrDeprecatedKeys != null && arrDeprecatedKeys.length > 0) {
				try {
					setting.validateSettings(settings);
				}
				catch (final InvalidSettingsException excOrig) {
					LOGGER.debug("Caught invalid setting for " + setting.toString() + " - Trying deprecated keys instead ...");
					final String newKey = arrDeprecatedKeys[0];
					int iErrorCounter = 0;

					// Try deprecated keys
					for (int i = 1; i < arrDeprecatedKeys.length; i++) {
						final String deprKey = arrDeprecatedKeys[i];

						try {
							if (settings.containsKey(deprKey)) {
								final ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
								settings.saveToXML(out);
								String strSettingsContent = out.toString();
								strSettingsContent = strSettingsContent.replace(
										"<config key=\"" + deprKey + "\"", "<config key=\"" + newKey + "\"");
								strSettingsContent = strSettingsContent.replace(
										"<entry key=\"" + deprKey + "\"", "<entry key=\"" + newKey + "\"");
								strSettingsContent = strSettingsContent.replace(
										"<config key=\"" + deprKey + "_Internals\"", "<config key=\"" + newKey + "_Internals\"");
								final NodeSettingsRO deprSettings = NodeSettings.loadFromXML(
										new ByteArrayInputStream(strSettingsContent.getBytes()));
								setting.validateSettings(deprSettings);
								break;
							}
						}
						catch (final InvalidSettingsException excDepr) {
							iErrorCounter++;
						}
						catch (final Exception exc) {
							LOGGER.debug("Another exception occurred when using deprecated key '" + deprKey + "'.", exc);
							iErrorCounter++;
						}
					}

					if (iErrorCounter == arrDeprecatedKeys.length - 1) {
						LOGGER.debug("Deprecated keys did not work either. - Giving up.");

						if (!bIgnoreNonExistingSetting) {
							throw excOrig;
						}
					}
				}
			}
			else {
				try {
					setting.validateSettings(settings);
				}
				catch (final InvalidSettingsException excOrig) {
					if (!bIgnoreNonExistingSetting) {
						throw excOrig;
					}
				}
			}
		}
	}

	/**
	 * Creates a filtered table based on the specified filter condition.
	 *
	 * @param inPort The input port of the data in focus. This will be passed on to the splitter.
	 * @param inData Input table to be filtered. Can be null. In that case null will be returned.
	 * @param arrInputDataInfo Input data information about all important input columns of
	 * 		the table at the input port. This will be passed on to the splitter.
	 * @param exec Execution context to check for cancellation and to report progress. Must not be null.
	 * @param strProgressMessage Message to append to the standard progress message. Can be null.
	 * @param filter Filter condition. Can be null. In that case the input table will be returned as is.
	 *
	 * @return The filtered table or the input table.
	 *
	 * @throws CanceledExecutionException Thrown, if the user cancelled the node execution.
	 */
	protected BufferedDataTable createFilteredTable(final int inPort, final BufferedDataTable inData,
			final InputDataInfo[] arrInputDataInfo, final ExecutionContext exec,
			final String strProgressMessage, final FilterCondition filter) throws CanceledExecutionException {

		BufferedDataTable resultTable = inData; // Default

		if (filter != null) {
			final long lRowCount = inData.size();
			long lRowIndex = 0;
			final RowIterator it = inData.iterator();

			final BufferedDataContainer resultTableData = exec.createDataContainer(inData.getDataTableSpec());

			// Filter the rows
			while (it.hasNext()) {
				final DataRow row = it.next();
				final long lUniqueWaveId = createUniqueCleanupWaveId();

				try {
					if (filter.include(0, lRowIndex, row, arrInputDataInfo, lUniqueWaveId)) {
						resultTableData.addRowToTable(row);
					}
				}
				catch (final InputDataInfo.EmptyCellException exc) {
					LOGGER.warn(exc.getMessage());
					if (exc.stopsNodeExecution()) {
						throw new RuntimeException("Creation of new data failed: " +
								exc.getMessage() != null ? exc.getMessage() : "Unknown error");
					}
					else {
						getWarningConsolidator().saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
								"Encountered empty input cell.");
						// In this case the row will be dumped (target table = -1)
					}
				}
				finally {
					cleanupMarkedObjects(lUniqueWaveId);
				}

				// Every 20 iterations report progress and check for cancel
				if (lRowIndex % 20 == 0) {
					AbstractRDKitNodeModel.reportProgress(exec, lRowIndex, lRowCount, row, strProgressMessage);
				}

				lRowIndex++;
			}

			// Create table
			resultTableData.close();
			resultTable = resultTableData.getTable();
		}

		exec.setProgress(1.0d);

		return resultTable;
	}

	/**
	 * Creates a filtered table based on the specified filter condition.
	 *
	 * @param inPort The input port of the data in focus. This will be passed on to the splitter.
	 * @param inData Input table to be filtered. Can be null. In that case null will be returned.
	 * @param arrInputDataInfo Input data information about all important input columns of
	 * 		the table at the input port. This will be passed on to the splitter.
	 * @param exec Execution context to check for cancellation and to report progress. Must not be null.
	 * @param strProgressMessage Message to append to the standard progress message. Can be null.
	 * @param splitter Split condition. Must not be null.
	 *
	 * @return The filtered table or null.
	 *
	 * @throws CanceledExecutionException Thrown, if the user cancelled the node execution.
	 */
	protected BufferedDataTable[] createSplitTables(final int inPort, final BufferedDataTable inData,
			final InputDataInfo[] arrInputDataInfo, final ExecutionContext exec,
			final String strProgressMessage, final SplitCondition splitter) throws CanceledExecutionException {
		// Pre-check
		if (splitter == null) {
			throw new IllegalArgumentException("Split condition must not be null.");
		}

		BufferedDataTable arrResultTable[] = null;

		final int iTargetTableCount = splitter.getTargetTableCount();
		final BufferedDataContainer arrPort[] = new BufferedDataContainer[iTargetTableCount];
		for (int iPort = 0; iPort < iTargetTableCount; iPort++)
		{
			arrPort[iPort] = exec.createDataContainer(inData.getDataTableSpec());
		}

		final long lRowCount = inData.size();
		long lRowIndex = 0;
		final RowIterator it = inData.iterator();

		// Filter the rows
		while (it.hasNext()) {
			final DataRow row = it.next();
			int iTargetTable = -1;
			final long lUniqueWaveId = createUniqueCleanupWaveId();

			try {
				iTargetTable = splitter.determineTargetTable(0, lRowIndex, row,
						arrInputDataInfo, lUniqueWaveId);
			}
			catch (final InputDataInfo.EmptyCellException exc) {
				LOGGER.warn(exc.getMessage());
				if (exc.stopsNodeExecution()) {
					throw new RuntimeException("Creation of new data failed: " +
							exc.getMessage() != null ? exc.getMessage() : "Unknown error");
				}
				else {
					getWarningConsolidator().saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
							"Encountered empty input cell.");
					// In this case the row will be dumped (target table = -1)
				}
			}
			finally {
				cleanupMarkedObjects(lUniqueWaveId);
			}

			if (iTargetTable >= 0) {
				arrPort[iTargetTable].addRowToTable(row);
			}

			// Every 20 iterations report progress and check for cancel
			if (lRowIndex % 20 == 0) {
				AbstractRDKitNodeModel.reportProgress(exec, lRowIndex, lRowCount, row, " - Splitting");
			}

			lRowIndex++;
		}

		exec.setProgress(1.0d, "Finished Splitting");

		arrResultTable = new BufferedDataTable[iTargetTableCount];
		for (int iPort = 0; iPort < iTargetTableCount; iPort++)
		{
			arrPort[iPort].close();
			arrResultTable[iPort] = arrPort[iPort].getTable();
		}

		return arrResultTable;
	}

	/**
	 * Call this method for every created KNIME SettingsModel. These settings will be handled
	 * automatically in the methods {@link #loadValidatedSettingsFrom(NodeSettingsRO)},
	 * {@link #validateSettings(NodeSettingsRO)} and {@link #saveSettingsTo(NodeSettingsWO)}.
	 *
	 * @param <T> The parameter class needs to be derived from SettingsModel.
	 * @param settings Settings to be registered for auto-handling. Can be null to do nothing.
	 * @param deprecatedSettingKeys List of setting keys that were used in the past. When loading the setting model
	 * 		fails, we will try to reload the model with the old keys, which are tried in the submitted
	 * 		order, if there is more than one old key. Important: If a list is specified, the first
	 * 		element must be the current key, all other elements will be treated as deprecated. Can be null.
	 *
	 * @return The same settings that have been passed in. Null, if null was passed in.
	 *
	 * @throws IllegalArgumentException Thrown, if a list of deprecated keys is provided, but
	 * 		has only one element. Note, that the first element of such a list must be the
	 * 		current key.
	 *
	 * @see #registerSettings(SettingsModel, boolean, String...)
	 */
	protected <T extends SettingsModel> T registerSettings(final T settings, final String... deprecatedSettingKeys) {
		return registerSettings(settings, false, deprecatedSettingKeys);
	}

	/**
	 * Call this method for every created KNIME SettingsModel. These settings will be handled
	 * automatically in the methods {@link #loadValidatedSettingsFrom(NodeSettingsRO)},
	 * {@link #validateSettings(NodeSettingsRO)} and {@link #saveSettingsTo(NodeSettingsWO)}.
	 *
	 * @param <T> The parameter class needs to be derived from SettingsModel.
	 * @param settings Settings to be registered for auto-handling. Can be null to do nothing.
	 * @param bIgnoreNonExistence Set to true, if this is a setting, which was not always available. The system
	 * 		will ignore any InvalidSettingsExceptions for when loading this setting. Default is false.
	 * @param deprecatedSettingKeys List of setting keys that were used in the past. When loading the setting model
	 * 		fails, we will try to reload the model with the old keys, which are tried in the submitted
	 * 		order, if there is more than one old key. Important: If a list is specified, the first
	 * 		element must be the current key, all other elements will be treated as deprecated. Can be null.
	 *
	 * @return The same settings that have been passed in. Null, if null was passed in.
	 *
	 * @throws IllegalArgumentException Thrown, if a list of deprecated keys is provided, but
	 * 		has only one element. Note, that the first element of such a list must be the
	 * 		current key.
	 *
	 * @see #registerSettings(SettingsModel, String...)
	 */
	protected <T extends SettingsModel> T registerSettings(final T settings, final boolean bIgnoreNonExistence, final String... deprecatedSettingKeys) {
		if (deprecatedSettingKeys != null && deprecatedSettingKeys.length == 1) {
			throw new IllegalArgumentException("The list of deprecated keys must contain as first element the " +
					"current key. The following elements can be acceptable deprecated keys.");
		}

		if (settings != null && !m_listRegisteredSettings.contains(settings)) {
			m_listRegisteredSettings.add(settings);
			if (deprecatedSettingKeys != null && deprecatedSettingKeys.length > 0) {
				m_mapDeprecatedSettingKeys.put(settings, deprecatedSettingKeys);
			}
			if (bIgnoreNonExistence) {
				m_mapIgnoreNonExistingSettingKeys.put(settings, bIgnoreNonExistence);
			}
		}

		return settings;
	}

	/**
	 * Returns the warning consolidator that was created initially with the call
	 * {@link #createWarningConsolidator()}. Use this object to track warnings
	 * and to consolidate them before showing them to the user.
	 *
	 * @return The instance of the warning consolidator that is used in this node.
	 */
	protected synchronized WarningConsolidator getWarningConsolidator() {
		if (m_warnings == null) {
			m_warnings = createWarningConsolidator();
		}

		return m_warnings;
	}

	/**
	 * Returns a list of all table specifications from the passed in tables.
	 *
	 * @param inData Array of tables with data.
	 *
	 * @return The specifications of the tables.
	 */
	protected DataTableSpec[] getInputTableSpecs(final BufferedDataTable[] inData) {
		DataTableSpec[] arrSpecs = null;

		if (inData != null) {
			arrSpecs = new DataTableSpec[inData.length];
			for (int i = 0; i < inData.length; i++) {
				arrSpecs[i] = (inData[i] == null ? null : inData[i].getDataTableSpec());
			}
		}

		return arrSpecs;
	}

	/**
	 * Returns the list of all output table specifications by calling
	 * for all out ports {@link #getOutputTableSpec(int, DataTableSpec[])} and
	 * concatenating the result to an array.
	 *
	 * @param inSpecs All input table specifications. Can be null.
	 *
	 * @return The specifications of all output tables.
	 *
	 * @throws InvalidSettingsException Thrown, if the settings are inconsistent with
	 * 		given DataTableSpec elements.
	 */
	protected DataTableSpec[] getOutputTableSpecs(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		final DataTableSpec[] arrConvertedSpecs = convertInputTables(inSpecs, createInputDataInfos(inSpecs));
		final DataTableSpec[] arrSpecs = new DataTableSpec[getNrOutPorts()];
		for (int i = 0; i < arrSpecs.length; i++) {
			arrSpecs[i] = getOutputTableSpec(i, arrConvertedSpecs);
		}
		return arrSpecs;
	}

	/**
	 * Returns the list of all output table specifications by calling
	 * for all out ports {@link #getOutputTableSpec(int, DataTableSpec[])} and
	 * concatenating the result to an array.
	 *
	 * @param inData Concrete input data coming from the execute() method of the node model.
	 * 		Can be null.
	 *
	 * @return The specifications of all output tables. Can be null.
	 *
	 * @throws InvalidSettingsException Thrown, if the settings are inconsistent with
	 * 		given DataTableSpec elements.
	 */
	protected DataTableSpec[] getOutputTableSpecs(final DataTable[] inData)
			throws InvalidSettingsException {
		final DataTableSpec[] arrInSpecs = SettingsUtils.getTableSpecs(inData);
		final DataTableSpec[] arrOutSpecs = new DataTableSpec[getNrOutPorts()];
		for (int i = 0; i < arrOutSpecs.length; i++) {
			arrOutSpecs[i] = getOutputTableSpec(i, arrInSpecs);
		}
		return arrOutSpecs;
	}

	/**
	 * Returns the output table specification of the specified out port. The implementation
	 * depends highly on the output generation strategy. If a ColumnRearranger
	 * is used to re-use the input table an output table specification will be
	 * based on that ColumnRearranger object. If a new table is created for
	 * an output port, it could generate a table specification directly derived from the
	 * an output factory.
	 *
	 * @param outPort Index of output port in focus. Zero-based.
	 * @param inSpecs All input table specifications. Can be null.
	 *
	 * @return The specification of all output tables.
	 *
	 * @throws InvalidSettingsException Thrown, if the settings are inconsistent with
	 * 		given DataTableSpec elements.
	 */
	protected abstract DataTableSpec getOutputTableSpec(final int outPort,
			final DataTableSpec[] inSpecs) throws InvalidSettingsException;


	/**
	 * Creates a new warning consolidator with the desired warning contexts.
	 * This implementation contains only the row context. Override this method to
	 * use a consolidator that supports more contexts, e.g. for tables, batches, etc.
	 *
	 * @return Warning consolidator used in {@link #configure(PortObjectSpec[])} and
	 * 		{@link #execute(PortObject[], ExecutionContext)}.
	 * 		Must not be null.
	 */
	protected WarningConsolidator createWarningConsolidator() {
		return new WarningConsolidator(WarningConsolidator.ROW_CONTEXT);
	}

	/**
	 * The default implementation of this method consolidates all saved warning messages
	 * and sets them as one warning message for the node. It will not show, how many
	 * contexts (e.g. rows) have been processed. You may call {@link #generateWarnings(Map)}
	 * specifying the number of executed objects in each registered context (e.g. how
	 * many rows the input table had).
	 */
	protected void generateWarnings() {
		generateWarnings(null);
	}

	/**
	 * The default implementation of this method consolidates all saved warning messages
	 * and sets them as one warning message for the node. Based on the passed in map
	 * it will show, how many contexts (e.g. rows) have been processed for each context-based
	 * warning, e.g. "Empty rows encountered [4 of 10 rows]". In this example the 10 comes
	 * from the passed in map. The 4 comes from 4 calls of the same warning during execution.
	 * You may call {@link #generateWarnings()} if total number of rows or other contexts are
	 * not available.
	 *
	 * @param mapContextOccurrences Maps context ids to number of occurrences (e.g. number of rows).
	 * 		Can be null.
	 */
	protected void generateWarnings(final Map<String, Long> mapContextOccurrences) {
	   generateWarnings(getWarningConsolidator(), mapContextOccurrences);
	}

   /**
    * The default implementation of this method consolidates all saved warning messages
    * from the passed in Warning Consolidator 
    * and sets them as one warning message for the node. Based on the passed in map
    * it will show, how many contexts (e.g. rows) have been processed for each context-based
    * warning, e.g. "Empty rows encountered [4 of 10 rows]". In this example the 10 comes
    * from the passed in map. The 4 comes from 4 calls of the same warning during execution.
    * You may call {@link #generateWarnings()} if total number of rows or other contexts are
    * not available.
    *
    * @param warnings Warning consolidator to use.
    * @param mapContextOccurrences Maps context ids to number of occurrences (e.g. number of rows).
    *       Can be null.
    */
   protected void generateWarnings(WarningConsolidator warnings, final Map<String, Long> mapContextOccurrences) {
      setWarningMessage(warnings.getWarnings(mapContextOccurrences));
   }

	/**
	 * This method generates InputDataInfo objects for all tables specified in
	 * the parameter. It will call {@link #createInputDataInfos(int, DataTableSpec)}
	 * for each of them to get InputDataInfo objects for all of them.
	 * To find the proper column, the concrete node implementation needs to
	 * use the setting models.
	 *
	 * @param inSpecs All table specifications of the input ports of the node. Should not be null.
	 *
	 * @return An array of info objects for all columns used as input for the passed table.
	 * 		Should not return null. If no input columns are used from the passed table,
	 * 		it should return an empty array.
	 *
	 * @throws InvalidSettingsException Thrown, if the current settings or the spec of the
	 * 		input table will make it impossible to process the data.
	 */
	protected InputDataInfo[][] createInputDataInfos(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		if (inSpecs == null) {
			throw new InvalidSettingsException("There is no input table available yet.");
		}

		final InputDataInfo[][] arrDataInfos = new InputDataInfo[inSpecs.length][];

		for (int i = 0; i < inSpecs.length; i++) {
			arrDataInfos[i] = createInputDataInfos(i, inSpecs[i]);
		}

		return arrDataInfos;
	}

	/**
	 * This method needs to be implemented by a concrete node to generate InputDataInfo
	 * objects for each input column for all input tables. To find the proper column,
	 * the concrete node implementation must pass in the appropriate setting model
	 * of the column name into the constructor of the InputDataInfo class.
	 *
	 * @param inPort The port number of the input table to create input data objects for.
	 * @param inSpec The table specification of the input table. Can be null.
	 *
	 * @return An array of info objects for all columns used as input for the passed table.
	 * 		Should not return null. If no input columns are used from the passed table,
	 * 		it should return an empty array.
	 *
	 * @throws InvalidSettingsException Thrown, if the current settings or the spec of the
	 * 		input table will make it impossible to process the data.
	 */
	protected abstract InputDataInfo[] createInputDataInfos(int inPort, DataTableSpec inSpec)
			throws InvalidSettingsException;

	/**
	 * Called at the end of execution, if the node implements the TableViewSupport
	 * interface. This method writes all input and output tables, which are subject
	 * of conservation (declared through results of the methods {@link #getInputTablesToConserve()}
	 * and {@link #getOutputTablesToConserve()}, into the associated table content models
	 * to make the date available to table views. Also, these tables are picked
	 * up by the core node implementation of KNIME for conservation when the
	 * workflow is stored and restoration when it is loaded. Additionally, an
	 * internally used conservation spec table gets created, which contains information
	 * about what we conserve. This table is used when the node gets loaded to
	 * see, if the later user node implementation is still compatible with what
	 * we stored.
	 *
	 * @param exec Execution context. Used to create an internal spec table.
	 * @param arrInData All input tables which were passed to the execute method.
	 * @param arrResultTable All result tables that the execute method will deliver.
	 *
	 * @see #getInputTablesToConserve()
	 * @see #getOutputTablesToConserve()
	 * @see #execute(BufferedDataTable[], ExecutionContext)
	 */
	protected void conserveTables(final ExecutionContext exec, final BufferedDataTable[] arrInData, final BufferedDataTable[] arrResultData) {
		if (this instanceof TableViewSupport) {
			final int[] arrIn = getInputTablesToConserve();
			final int[] arrOut = getOutputTablesToConserve();
			int iCount = 0;

			// Build little spec table to remember what input and what ouput tables are
			final BufferedDataContainer tableInternalDataSpec = exec.createDataContainer(new DataTableSpec(
					new DataColumnSpecCreator(PORT_TYPE_COLUMN_NAME, StringCell.TYPE).createSpec()));

			// Conserve input tables
			if (m_arrInContModel != null && arrIn != null && m_arrInContModel.length == arrIn.length) {
				for (int i = 0; i < arrIn.length; i++, iCount++) {
					m_arrInContModel[i].setDataTable(arrInData[arrIn[i]]);
					m_arrInContModel[i].setHiLiteHandler(getInHiLiteHandler(arrIn[i]));
					tableInternalDataSpec.addRowToTable(new DefaultRow("" + iCount, INPUT_TABLE_ID));
				}
			}

			// Conserve output tables
			if (m_arrOutContModel != null && arrOut != null && m_arrOutContModel.length == arrOut.length) {
				for (int i = 0; i < arrOut.length; i++, iCount++) {
					m_arrOutContModel[i].setDataTable(arrResultData[arrOut[i]]);
					m_arrOutContModel[i].setHiLiteHandler(getOutHiLiteHandler(arrOut[i]));
					tableInternalDataSpec.addRowToTable(new DefaultRow("" + iCount, OUTPUT_TABLE_ID));
				}
			}

			tableInternalDataSpec.close();
			m_tableContentTableSpecs = tableInternalDataSpec.getTable();
		}
	}

	//
	// Public Static Methods
	//

	/**
	 * Checks, if the user canceled execution and reports the progress in a
	 * standard form.
	 *
	 * @param exec Execution context to use for checks and reporting. Can be null
	 * 		to do nothing.
	 * @param lRowIndex Index of currently processed row.
	 * @param lTotalRowCount Total number of rows to be processed.
	 * @param row Currently processed row to get row key from. Can be null to
	 * 		suppress this information.
	 * @param textToAppend Additional text(s) to append directly at the end. Optional.
	 *
	 * @throws CanceledExecutionException Thrown, if the user canceled execution.
	 */
	public static void reportProgress(final ExecutionContext exec, final long lRowIndex, final long lTotalRowCount,
			final DataRow row, final String... textToAppend) throws CanceledExecutionException {
		if (exec != null) {
			exec.checkCanceled();

			final StringBuilder m = new StringBuilder("Processed row ")
			.append(lRowIndex).append('/').append(lTotalRowCount);

			if (row != null) {
				m.append(" (\"").append(row.getKey()).append("\")");
			}

			if (textToAppend != null) {
				for (final String text : textToAppend) {
					m.append(text);
				}
			}

			exec.setProgress(lRowIndex / (double)lTotalRowCount, m.toString());
		}
	}

	/**
	 * Monitors the specified thread and waits until it ends or until the user canceled
	 * the current execution as monitored by the passed in ExecutionContext.
	 * Cancellation will be checked every x milliseconds as specified in the third
	 * parameter. If the last parameter is set to true, the progress will be
	 * reported based on a mathematical function with limes of 1.0d. This function
	 * is not time dependent, but depends on number of executions, hence it
	 * is influenced by the value iCheckIntervalInMillis.
	 *
	 * @param thread A working thread that will be monitored and joined.
	 * @param exec Execution context to check for cancellation and to report (pseudo) progress.
	 * @param iCheckIntervalInMillis Interval to check for cancellation and to update
	 * 		progress if desired. In milliseconds.
	 * @param bShowPseudoProgress Set to true to update the progress value with a pseudo
	 * 		progress value.
	 * @param bStopWorkingThreadAfterCancellation Set to true to call stop() for the
	 * 		working thread if the user canceled the node execution. This can be dangerous
	 * 		if the thread is sharing objects. It may lead to a Java VM crash and is
	 * 		not really recommended to do. Use with care.
	 *
	 * @throws CanceledExecutionException Thrown, if the user canceled.
	 * @throws InterruptedException Thrown, if the current monitoring thread got interrupted
	 * 		while waiting for joining the working thread.
	 */
	@SuppressWarnings("deprecation")
	public static void monitorWorkingThreadExecution(final Thread thread, final ExecutionContext exec,
			final int iCheckIntervalInMillis, final boolean bShowPseudoProgress,
			final boolean bStopWorkingThreadAfterCancellation)
					throws CanceledExecutionException {

		// Pre-check
		if (thread == null) {
			throw new IllegalArgumentException("Thread to be monitored must not be null.");
		}

		// Wait for calculation thread to finish and check for cancellation
		int iCounter = 0;
		while (thread.isAlive()) {
			try {
				thread.join(iCheckIntervalInMillis);
			}
			catch (final InterruptedException excInterrupted) {
				// This gets thrown when the user cancels - we will check right afterwards
				// which will result in a CanceledExecutionException
			}

			try {
				// Check, if user canceled
				exec.checkCanceled();

				// Update progress bar
				if (bShowPseudoProgress) {
					exec.setProgress(1.0d - (10d / (10d + iCounter++)));
				}
			}
			catch (final CanceledExecutionException exc) {
				exec.setProgress("Cancellation in progress - Please wait ...");

				// Although it's not nice try to kill the working thread, because it may
				// run a very long time
				if (bStopWorkingThreadAfterCancellation) {
					try {
						LOGGER.debug("Stop the calculation thread ...");
						thread.stop();
						LOGGER.debug("Successfully stopped.");
					}
					catch (final SecurityException excAccessDenied) {
						LOGGER.warn("Calculation thread could not been stopped. It will run out by itself.");
					}
				}
				else {
					LOGGER.warn("Calculation thread has not been stopped when cancelling. It will run out by itself.");
				}

				throw exc;
			}
		}
	}


	//
	// Static Classes
	//

	/**
	 * This class provides a mechanism to update the node execution progress
	 * automatically every x milliseconds using a mathematical function with
	 * a limes of 1.0d.
	 *
	 * @author Manuel Schwarze
	 */
	static public class PseudoProgressUpdater {

		//
		// Members
		//

		/** Timer that provides the scheduling for the progress update. */
		private final Timer m_progressUpdater;

		//
		// Constructor
		//

		/**
		 * Creates a pseudo progress updater, which increases progress in the specified
		 * execution context every x milliseconds.
		 */
		public PseudoProgressUpdater(final ExecutionContext exec, final long updateDelayInMillis) {
			m_progressUpdater = new Timer(true);
			m_progressUpdater.schedule(new TimerTask() {
				int m_iCounter = 0;

				@Override
				public void run() {
					exec.setProgress(1.0d - (10d / (10d + m_iCounter++)));

					try {
						exec.checkCanceled();
					}
					catch (final CanceledExecutionException exc) {
						exec.setProgress("Cancellation in progress - Please wait ...");
						cancel(); // Cancels progress update
					}
				}
			}, 0, updateDelayInMillis); // Increase progress until we reach almost 100%
		}

		/**
		 * Stops updating the progress update.
		 */
		public void cancel() {
			m_progressUpdater.cancel();
		}
	}

	/**
	 * This interface implements the logic to process a result set and is passed into
	 * a ParallelProcessor constructor. Normally the results will be added to
	 * an output table. Often decisions are involved to determine, which output
	 * table should receive the results.
	 *
	 * @author Manuel Schwarze
	 */
	static public interface ResultProcessor {

		/**
		 * Contains the logic to process a result set. Normally it will be added to
		 * an output table. Often decisions are involved to determine, which output
		 * table should receive the results.
		 *
		 * @param rowIndex The index of the input row the result was calculated for.
		 * @param row The input row.
		 * @param arrResults The calculated results. Can be null.
		 */
		void processResults(long rowIndex, DataRow row, DataCell[] arrResults);
	}

	/**
	 * The ParallelProcessor implements a default configuration to use the
	 * the MultiThreadWorker for parallel processing. It uses a passed in
	 * factory to perform the computations of results and contains standard logic
	 * to handle results, to perform cancellation checks, to produce warnings
	 * or errors if something went wrong, to report progress, etc.
	 * Important is the ResultProcessor instance that must be passed in to the
	 * constructor. It will define the logic what to do with the results.
	 *
	 * @author Manuel Schwarze
	 */
	static public class ParallelProcessor extends MultiThreadWorker<DataRow, DataCell[]> {

		/**
		 * Determines the queue size considering also the currently available CPU count.
		 *
		 * @return Queue size to be used for parallel processing.
		 */
		private static int getQueueSize() {
			return 10 * getMaxParallelWorkers();
		}

		/**
		 * Determines the CPU count to be used for parallel processing.
		 *
		 * @return Number of available processors + 50%. This calculation was found
		 * 		in a MultiThreadWorker implementation of KNIME.
		 */
		private static int getMaxParallelWorkers() {
			return (int)Math.ceil(1.5 * Runtime.getRuntime().availableProcessors());
		}

		//
		// Members
		//

		/**
		 * The total number of rows that are subject of processing. This is used
		 * for progress reporting.
		 */
		private  final long m_lTotalRowCount;

		/**
		 * The output factories to be used to compute the results based on an input row.
		 */
		private  final AbstractRDKitCellFactory[] m_arrFactory;

		/**
		 * The result processor that will take an input row and the result cells
		 * an adds them to a table or processes them somehow further.
		 */
		private  final ResultProcessor m_resultProcessor;

		/**
		 * An instance of a warning consolidator that gets fed with warning messages,
		 * if they occur.
		 */
		private  final WarningConsolidator m_warningConsolidator;

		/**
		 * An execution context that is used for progress reporting.
		 */
		private  final ExecutionContext m_exec;

		/**
		 * For performance reasons we remember, if we deal only with a single or with multiple factories;
		 */
		private final boolean m_bMultiFactory;

		/**
		 * Cell count to be delivered by all factories together. This will be determined
		 * in the constructor by inquiring all factories about their column specs.
		 */
		private final int m_iCellCount;

		/**
		 * Stores the consolidated failure policy derived from all registered factories.
		 * If only one factory requires stop of execution it will be that way. Otherwise,
		 * empty cells will be delivered.
		 *
		 * @see AbstractRDKitCellFactory.RowFailurePolicy
		 */
		private final RowFailurePolicy m_consolidatedRowFailurePolicy;

		//
		// Constructor
		//

		/**
		 * Creates a new parallel processor object to perform in several threads the work
		 * that is to do. Calculations (work) will be performed by the passed in factory implementation,
		 * which needs to be derived from the AbstractRDKitCellFactory (implement here the
		 * method process(...) ). After a thread has calculated new data cells the results will
		 * be passed to the passed in resultProcessor implementation. Here the method
		 * processResults(...) needs to be implemented. The last three parameters are involved
		 * in processing warnings and progress information and in checking, if the user cancelled
		 * the execution.
		 *
		 * @param factory The factory implementation to perform the calculations. Must not be null.
		 * @param resultProcessor The result processor implementation, which could distribute the results
		 * 		to different tables, if desired. Must not be null.
		 * @param lRowCount Row count of the input table in focus of this parallel processing. This
		 * 		value is used to determine the correct progress percentage.
		 * @param warningConsolidator Warning consolidator to be used to save warning messages and
		 * 		its statistics (how often they occurred). Must not be null.
		 * @param exec Execution context to check for user cancellation and to report progress. Must not be null.
		 *
		 */
		public ParallelProcessor(final AbstractRDKitCellFactory factory,
				final ResultProcessor resultProcessor, final long lRowCount,
				final WarningConsolidator warningConsolidator, final ExecutionContext exec) {
			this(new AbstractRDKitCellFactory[] { factory }, resultProcessor, lRowCount,
					warningConsolidator, exec);
		}

		/**
		 * Creates a new parallel processor object to perform in several threads the work
		 * that is to do. Calculations (work) will be performed by the passed in factory implementation,
		 * which needs to be derived from the AbstractRDKitCellFactory (implement here the
		 * method process(...) ). After a thread has calculated new data cells the results will
		 * be passed to the passed in resultProcessor implementation. Here the method
		 * processResults(...) needs to be implemented. The last three parameters are involved
		 * in processing warnings and progress information and in checking, if the user cancelled
		 * the execution.
		 *
		 * @param arrFactory Multiple factory implementations to perform the calculations. Must not be null.
		 * @param resultProcessor The result processor implementation, which could distribute the results
		 * 		to different tables, if desired. Must not be null.
		 * @param lRowCount Row count of the input table in focus of this parallel processing. This
		 * 		value is used to determine the correct progress percentage.
		 * @param warningConsolidator Warning consolidator to be used to save warning messages and
		 * 		its statistics (how often they occurred). Must not be null.
		 * @param exec Execution context to check for user cancellation and to report progress. Must not be null.
		 *
		 */
		public ParallelProcessor(final AbstractRDKitCellFactory[] arrFactory,
				final ResultProcessor resultProcessor, final long lRowCount,
				final WarningConsolidator warningConsolidator, final ExecutionContext exec) {

			super(getQueueSize(), getMaxParallelWorkers());

			// Pre-checks
			if (arrFactory == null || arrFactory.length == 0) {
				throw new IllegalArgumentException("Factory array must neither be null nor empty.");
			}
			for (final AbstractRDKitCellFactory factory : arrFactory) {
				if (factory == null) {
					throw new IllegalArgumentException("Factory must not be null.");
				}
			}
			if (resultProcessor == null) {
				throw new IllegalArgumentException("Result Processor must not be null.");
			}
			if (warningConsolidator == null) {
				throw new IllegalArgumentException("Warning Consolidator must not be null.");
			}
			if (exec == null) {
				throw new IllegalArgumentException("Execution Context must not be null.");
			}

			m_arrFactory = arrFactory;
			m_resultProcessor = resultProcessor;
			m_lTotalRowCount = lRowCount;
			m_warningConsolidator = warningConsolidator;
			m_exec = exec;

			// Calculate total of all cells delivered by all factories and failure policy
			int iCellCount = 0;
			RowFailurePolicy rowFailurePolicy = RowFailurePolicy.DeliverEmptyValues;

			for (final AbstractRDKitCellFactory factory : m_arrFactory) {
				final DataColumnSpec[] arrColSpecs = factory.getColumnSpecs();
				if (arrColSpecs == null) {
					throw new IllegalArgumentException("Column specifications in factory must not be null.");
				}
				iCellCount += arrColSpecs.length;
				if (factory.getRowFailurePolicy() == RowFailurePolicy.StopExecution) {
					rowFailurePolicy = RowFailurePolicy.StopExecution;
				}
			}

			m_bMultiFactory = m_arrFactory.length > 1;
			m_iCellCount = iCellCount;
			m_consolidatedRowFailurePolicy = rowFailurePolicy;
		}

		//
		// Public Methods
		//

		/**
		 * Creates a column rearranger, which works with this parallel processor.
		 * Note: Since KNIME 2.5.1 a factory will automatically process results using parallel
		 * threads, if a column rearranger is used and if the used factory allows for it.
		 *
		 * @param inSpec Specification of input table.
		 * @param arrFactories Array of factories to be used to produce results cells based on an input row.
		 * @param blockingQueue A blocking queue used to communicate between parallel processor and
		 * 		the column rearranger (getCells(...) method waits for results).
		 * @param exec Execution context used for cancellation checks.
		 *
		 * @return Column rearranger that will pick up its results from the parallel processor.
		 *
		 * @see AbstractCellFactory#setParallelProcessing(boolean)
		 * 
		 * @deprecated This method is not used anymore and will be removed in the future.
		 */
		@Deprecated
		protected ColumnRearranger createColumnRearranger(final DataTableSpec inSpec,
				final AbstractRDKitCellFactory[] arrFactories,
				final BlockingQueue<DataCell[]> blockingQueue,
				final ExecutionContext exec) {

			// Create all columns specs
			final List<DataColumnSpec> listSpecs = new ArrayList<DataColumnSpec>();
			for (final AbstractRDKitCellFactory factory : arrFactories) {
				listSpecs.addAll(Arrays.asList(factory.getColumnSpecs()));
			}

			final DataColumnSpec[] arrColumnSpecs = listSpecs.toArray(new DataColumnSpec[listSpecs.size()]);

			// Create column rearranger
			final ColumnRearranger rearranger = new ColumnRearranger(inSpec);
			rearranger.append(new CellFactory() {

				@Override
				public void setProgress(final int curRowNr, final int rowCount, final RowKey lastKey,
						final ExecutionMonitor exec) {
					// Ignored by purpose as progress information is made available
					// from the parallel processing
				}

				@Override
				public DataColumnSpec[] getColumnSpecs() {
					return arrColumnSpecs;
				}

				@Override
				public DataCell[] getCells(final DataRow row) {
					DataCell[] arrResults = null;

					try {
						while ((arrResults = blockingQueue.poll(200, TimeUnit.MILLISECONDS)) == null) {
							try {
								exec.checkCanceled();
							}
							catch (final CanceledExecutionException excCancelled) {
								throw new RuntimeException("User cancelled execution.", excCancelled);
							}
						}
					}
					catch (final InterruptedException e) {
						throw new RuntimeException("Result processing failed.", e);
					}

					return arrResults;
				}
			});

			return rearranger;
		}

		/**
		 * Computes new data cells based on an input row. The results of the input row must be independent
		 * of all other input rows due to the nature of multi-thread processing. This method
		 * calls the factory method getCells(row) to actually perform the concrete calculation work.
		 *
		 * @param row Input row from an input table.
		 * @param index Index of the row. Not used in this implementation.
		 */
		@Override
		public DataCell[] compute(final DataRow row, final long index) {
			DataCell[] arrTotalResults;

			// For performance reasons we check for single vs. multi factories here
			if (m_bMultiFactory) {
				arrTotalResults = new DataCell[m_iCellCount];

				int iOffset = 0;

				for (final AbstractRDKitCellFactory factory : m_arrFactory) {
					final DataCell[] arrResults = factory.getCells(row);
					System.arraycopy(arrResults, 0, arrTotalResults, iOffset, arrResults.length);
					iOffset += arrResults.length;
				}
			}
			else {
				arrTotalResults =  m_arrFactory[0].getCells(row);
			}

			return arrTotalResults;
		}

		/**
		 * Processes the finished calculated results from one of the threads of the parallel
		 * processing instance. It handles exceptions based on the factory's exception
		 * handling policy, checks for user cancellations and call the method
		 * processResults in Result Processor that was passed in to the constructor.
		 *
		 * @param task The computation task from the MultiThreadWorker. Must not be null.
		 */
		@Override
		public void processFinished(final ComputationTask task) {
			// Pre-check
			if (task == null) {
				throw new IllegalArgumentException("Computation task must not be null.");
			}

			final long rowIndex = task.getIndex();
			final DataRow row = task.getInput();
			DataCell[] arrCells = null;

			// Pick up results
			try {
				arrCells = task.get();
			}
			catch (final Exception e) {
				String strMessage = "Exception while getting result";

				// Use empty cells
				if (m_consolidatedRowFailurePolicy == RowFailurePolicy.DeliverEmptyValues) {
					strMessage += " - Assigning missing cells.";
					m_warningConsolidator.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
							strMessage);
					AbstractRDKitNodeModel.LOGGER.warn(strMessage, e);
					arrCells = AbstractRDKitCellFactory.createEmptyCells(1);
				}
				// Or fail
				else {
					strMessage += " - Giving up.";
					AbstractRDKitNodeModel.LOGGER.error(strMessage, e);
					throw new RuntimeException(strMessage, e);
				}
			}

			// Check, if user pressed cancel (however, we will finish the method nevertheless)
			// Update the progress only every 20 rows
			if (rowIndex % 20 == 0) {
				try {
					AbstractRDKitNodeModel.reportProgress(m_exec, (int)rowIndex,
							m_lTotalRowCount, row,
							new StringBuilder(" [").append(getActiveCount()).append(" active, ")
							.append(getFinishedTaskCount()).append(" pending]").toString());
				}
				catch (final CanceledExecutionException e) {
					cancel(true);
				}
			}

			m_resultProcessor.processResults(rowIndex, row, arrCells);
		};
	}

	/**
	 * This class keeps track of RDKit objects which require cleanup when not needed
	 * anymore.
	 *
	 * @author Manuel Schwarze
	 */
	static class RDKitCleanupTracker extends HashMap<Long, HashSet<Object>> {

		//
		// Constants
		//

		/** Serial number. */
		private static final long serialVersionUID = 270959635380434185L;

		/** The logger instance. */
		protected static final NodeLogger LOGGER = NodeLogger
				.getLogger(RDKitCleanupTracker.class);

		//
		// Constructors
		//

		/**
		 * Creates a new RDKitCleanup tracker.
		 */
		public RDKitCleanupTracker() {
			super();
		}

		/**
		 * Creates a new RDKitCleanup tracker.
		 *
		 * @param initialCapacity
		 * @param loadFactor
		 */
		public RDKitCleanupTracker(final int initialCapacity, final float loadFactor) {
			super(initialCapacity, loadFactor);
		}

		/**
		 * Creates a new RDKitCleanup tracker.
		 *
		 * @param initialCapacity
		 */
		public RDKitCleanupTracker(final int initialCapacity) {
			super(initialCapacity);
		}

		/**
		 * Creates a copy of an existing RDKitCleanupTracker object.
		 *
		 * @param existing The existing object. Must not be null.
		 */
		private RDKitCleanupTracker(final RDKitCleanupTracker existing) {
			super(existing);
		}

		//
		// Public Methods
		//

		/**
		 * Registers an RDKit based object that is used within a certain block (wave). $
		 * This object must have a delete() method implemented for freeing up resources later.
		 * The cleanup will happen for all registered objects when the method
		 * {@link #cleanupMarkedObjects(int)} is called with the same wave.
		 * Note: If the last parameter is set to true and the same rdkitObject
		 * was already registered for another wave (or no wave)
		 * it will be removed from the former wave list and will exist only in the wave
		 * specified here. This can be useful for instance, if an object is first marked as
		 * part of a wave and later on it is determined that it needs to live longer (e.g.
		 * without a wave). In this case the first time this method would be called with a wave id,
		 * the second time without wave id (which would internally be wave = 0).
		 *
		 * @param <T> Any class that implements a delete() method to be called to free up resources.
		 * @param rdkitObject An RDKit related object that should free resources when not
		 * 		used anymore. Can be null.
		 * @param wave A number that identifies objects registered for a certain "wave".
		 * @param bRemoveFromOtherWave Checks, if the object was registered before with another wave
		 * 		id, and remove it from that former wave. Usually this should be set to false for
		 * 		performance reasons.
		 *
		 * @return The same object that was passed in. Null, if null was passed in.
		 */
		public synchronized <T extends Object> T markForCleanup(final T rdkitObject, final long wave, final boolean bRemoveFromOtherWave) {
			if (rdkitObject != null)  {

				// Remove object from any other list, if desired (cost performance!)
				if (bRemoveFromOtherWave) {

					// Loop through all waves to find the rdkitObject - we create a copy here, because
					// we may remove empty wave lists which may blow up our iterator
					for (final long waveExisting : new HashSet<Long>(keySet())) {
						final HashSet<Object> list = get(waveExisting);
						if (list.remove(rdkitObject) && list.isEmpty()) {
							remove(waveExisting);
						}
					}
				}

				// Get the list of the target wave
				HashSet<Object> set = get(wave);

				// Create a wave list, if not found yet
				if (set == null) {
					set = new HashSet<Object>();
					put(wave, set);
				}

				// Add the object (only once, since it is a set)
				set.add(rdkitObject);
			}

			return rdkitObject;
		}

		/**
		 * Frees resources for all objects that have been registered prior to this last
		 * call using the method {@link #cleanupMarkedObjects()}.
		 */
		public synchronized void cleanupMarkedObjects() {
			// Loop through all waves for cleanup - we create a copy here, because
			// the cleanupMarkedObjects method will remove items from our map
			for (final long wave : new HashSet<Long>(keySet())) {
				cleanupMarkedObjects(wave);
			}
		}

		/**
		 * Frees resources for all objects that have been registered prior to this last
		 * call for a certain wave using the method {@link #cleanupMarkedObjects(int)}.
		 *
		 * @param wave A number that identifies objects registered for a certain "wave".
		 */
		public synchronized void cleanupMarkedObjects(final long wave) {
			// Find the right wave list, if not found yet
			final HashSet<Object> set = get(wave);

			// If wave list was found, free all objects in it
			if (set != null) {
				for (final Object objForCleanup : set) {
					Class<?> clazz = null;

					try {
						clazz = objForCleanup.getClass();
						final Method method = clazz.getMethod("delete");
						method.invoke(objForCleanup);
					}
					catch (final NoSuchMethodException excNoSuchMethod) {
						LOGGER.error("An object had been registered for cleanup (delete() call), " +
								"which does not provide a delete() method." +
								(clazz == null ? "" : " It's of class " + clazz.getName() + "."),
								excNoSuchMethod.getCause());
					}
					catch (final SecurityException excSecurity) {
						LOGGER.error("An object had been registered for cleanup (delete() call), " +
								"which is not accessible for security reasons." +
								(clazz == null ? "" : " It's of class " + clazz.getName() + "."),
								excSecurity.getCause());
					}
					catch (final Exception exc) {
						LOGGER.error("Cleaning up a registered object (via delete() call) failed." +
								(clazz == null ? "" : " It's of class " + clazz.getName() + "."),
								exc.getCause());
					}
				}

				set.clear();
				remove(wave);
			}
		}

		/**
		 * Removes all resources for all objects that have been registered prior to this last
		 * call using the method {@link #cleanupMarkedObjects()}, but delays the cleanup
		 * process. It basically moves the objects of interest into quarantine.
		 */
		public synchronized void quarantineAndCleanupMarkedObjects() {
			final RDKitCleanupTracker quarantineRDKitObjects = new RDKitCleanupTracker(this);
			clear();

			if (!quarantineRDKitObjects.isEmpty()) {
				// Create the future cleanup task
				final TimerTask futureCleanupTask = new TimerTask() {

					/**
					 * Cleans up all marked objects, which are put into quarantine for now.
					 */
					@Override
					public void run() {
						quarantineRDKitObjects.cleanupMarkedObjects();
					}
				};

				// Schedule the cleanup task for later
				final Timer timer = new Timer("Quarantine RDKit Object Cleanup", false);
				timer.schedule(futureCleanupTask, RDKIT_OBJECT_CLEANUP_DELAY_FOR_QUARANTINE);
			}
		}
	}

	/**
	 * Returns an array containing input ports indexes for the ports group specified.
	 *
	 * @param nodeCreationConfig {@link NodeCreationConfiguration} instance.
	 *                           Mustn't be Null.
	 * @param strPortGroupId     Ports group ID.
	 *                           Mustn't be Null.
	 * @return Array containing input ports indexes. Never Null.
	 * @throws IllegalStateException if {@code nodeCreationConfig} or {@code strPortGroupId} parameter is Null.
	 */
	public static int[] getInputTablePortIndexes(NodeCreationConfiguration nodeCreationConfig, String strPortGroupId) {
		if (nodeCreationConfig == null) {
			throw new IllegalStateException("Node Creation Configuration parameter must not be Null.");
		}
		if (strPortGroupId == null) {
			throw new IllegalStateException("Input Ports Group ID parameter must not be Null.");
		}

		return nodeCreationConfig.getPortConfig()
				.map(PortsConfiguration::getInputPortLocation)
				.map(mapLocations -> mapLocations.get(strPortGroupId))
				.orElseThrow(() -> new IllegalStateException("Input ports group '" + strPortGroupId + "' is not defined."));
	}

	/**
	 * Returns an array containing output ports indexes for the ports group specified.
	 *
	 * @param nodeCreationConfig {@link NodeCreationConfiguration} instance.
	 *                           Mustn't be Null.
	 * @param strPortGroupId     Ports group ID.
	 *                           Mustn't be Null.
	 * @return Array containing output ports indexes. Never Null.
	 * @throws IllegalStateException if {@code nodeCreationConfig} or {@code strPortGroupId} parameter is Null.
	 */
	public static int[] getOutputTablePortIndexes(NodeCreationConfiguration nodeCreationConfig, String strPortGroupId) {
		if (nodeCreationConfig == null) {
			throw new IllegalStateException("Node Creation Configuration parameter must not be Null.");
		}
		if (strPortGroupId == null) {
			throw new IllegalStateException("Output Ports Group ID parameter must not be Null.");
		}

		return nodeCreationConfig.getPortConfig()
				.map(PortsConfiguration::getOutputPortLocation)
				.map(mapLocations -> mapLocations.get(strPortGroupId))
				.orElseThrow(() -> new IllegalStateException("Output ports group '" + strPortGroupId + "' is not defined."));
	}

	/**
	 * Converts {@link PortObjectSpec} array to {@link DataTableSpec} array.
	 *
	 * @param inObjectSpecs Source {@link PortObjectSpec} array.
	 *                      Can be Null.
	 * @return {@link DataTableSpec} array. Can be Null.
	 */
	protected static DataTableSpec[] objectSpecsToTableSpecs(PortObjectSpec[] inObjectSpecs) {
		return inObjectSpecs == null ? null :
				Arrays.stream(inObjectSpecs)
						.map(portObjectSpec -> portObjectSpec instanceof DataTableSpec dataTableSpec ? dataTableSpec : null)
						.toArray(DataTableSpec[]::new);
	}

	/**
	 * Converts {@link DataTableSpec} array to {@link PortObjectSpec} array.
	 *
	 * @param inTableSpecs Source {@link DataTableSpec} array.
	 *                     Can be Null.
	 * @return {@link PortObjectSpec} array. Can be Null.
	 */
	protected static PortObjectSpec[] tableSpecsToObjectSpecs(DataTableSpec[] inTableSpecs) {
		return inTableSpecs == null ? null :
				Arrays.stream(inTableSpecs)
						.toArray(PortObjectSpec[]::new);
	}

	/**
	 * Converts {@link PortObject} array to {@link BufferedDataTable} array.
	 *
	 * @param inObjects Source {@link PortObject} array.
	 *                  Can be Null.
	 * @return {@link BufferedDataTable} array. Can be Null.
	 */
	protected static BufferedDataTable[] objectsToTables(PortObject[] inObjects) {
		return inObjects == null ? null :
				Arrays.stream(inObjects)
						.map(portObject -> portObject instanceof BufferedDataTable dataTable ? dataTable : null)
						.toArray(BufferedDataTable[]::new);
	}

	/**
	 * Converts {@link BufferedDataTable} array to {@link PortObject} array.
	 *
	 * @param inTables Source {@link BufferedDataTable} array.
	 *                 Can be Null.
	 * @return {@link PortObject} array. Can be Null.
	 */
	protected static PortObject[] tablesToObjects(BufferedDataTable[] inTables) {
		return inTables == null ? null :
				Arrays.stream(inTables)
						.toArray(PortObject[]::new);
	}

}

