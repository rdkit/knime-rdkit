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
package org.rdkit.knime.nodes.substructurecounter;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.RDKit.Match_Vect_Vect;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.RDKit.RWMol;
import org.RDKit.SubstructMatchParameters;
import org.knime.chem.types.SmartsValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.headers.HeaderPropertyUtils;
import org.rdkit.knime.nodes.AbstractRDKitCalculatorNodeModel;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.nodes.TableViewSupport;
import org.rdkit.knime.properties.SmilesHeaderProperty;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsUtils;

/**
 * This class implements the node model of the RDKitSubstructureCounter node
 * providing calculations based on the open source RDKit library.
 *
 * @author Swarnaprava Singh
 * @author Manuel Schwarze
 */
public class SubstructureCounterNodeModel extends AbstractRDKitCalculatorNodeModel implements TableViewSupport {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(SubstructureCounterNodeModel.class);

	/** Input data info index for Mol value (first table). */
	protected static final int INPUT_COLUMN_MOL = 0;

	/** Input data info index for Query Molecule (second table). */
	protected static final int INPUT_COLUMN_QUERY = 0;

	/** Input data info index for Substructure Name (second table). */
	protected static final int INPUT_COLUMN_NAME = 1;

	/** Default value for total hits column (optional). */
	protected static final String DEFAULT_TOTAL_HITS_COLUMN = "Total hits";

	/** Default value for query tags column (optional). */
	protected static final String DEFAULT_QUERY_TAGS_COLUMN = "Query tags";

	//
	// Members
	//

	/** Settings model for the column name of the input column. */
	private final SettingsModelString m_modelInputColumnName =
			registerSettings(SubstructureCounterNodeDialog.createInputColumnNameModel(), "input_column", "inputMolCol");
	// Accept also deprecated keys

	/** Settings model for the column name of the query molecule column. */
	private final SettingsModelString m_modelQueryColumnName =
			registerSettings(SubstructureCounterNodeDialog.createQueryInputModel());

	/** Settings model for the option to count unique matches only. */
	private final SettingsModelBoolean m_modelUniqueMatchesOnly =
			registerSettings(SubstructureCounterNodeDialog.createUniqueMatchesOnlyModel());

	/** Settings model for the option to use stereochemistry in the match. Added in November 2020. */
	private final SettingsModelBoolean m_modelUseChirality =
			registerSettings(SubstructureCounterNodeDialog.createUseChiralityModel(), true);

	/** Settings model for the option to use enhanced stereo in the match. Added in March 2021. */
	private final SettingsModelBoolean m_modelUseEnhancedStereo =
			registerSettings(SubstructureCounterNodeDialog.createUseEnhancedStereoModel(m_modelUseChirality), true);

	/** Settings model for the option to use a query name column. */
	private final SettingsModelBoolean m_modelUseQueryNameColumn =
			registerSettings(SubstructureCounterNodeDialog.createUseQueryNameColumnModel(), true);

	/** Settings model for the column name of the query name column. */
	private final SettingsModelString m_modelQueryNameColumn =
			registerSettings(SubstructureCounterNodeDialog.createQueryNameColumnModel(m_modelUseQueryNameColumn), true);

	/** Settings model for the option to count total hits per row. */
	private final SettingsModelBoolean m_modelCountTotalHitsOption =
			registerSettings(SubstructureCounterNodeDialog.createCountTotalHitsOptionModel(), true);

	/** Settings model for the column name of the total hits count column. */
	private final SettingsModelString m_modelCountTotalHitsColumn =
			registerSettings(SubstructureCounterNodeDialog.createCountTotalHitsColumnModel(m_modelCountTotalHitsOption), true);

	/** Settings model for the option to track query tags per row. */
	private final SettingsModelBoolean m_modelTrackQueryTagsOption =
			registerSettings(SubstructureCounterNodeDialog.createTrackQueryTagsOptionModel(), true);

	/** Settings model for the column name of the query tags column. */
	private final SettingsModelString m_modelTrackQueryTagsColumn =
			registerSettings(SubstructureCounterNodeDialog.createTrackQueryTagsColumnModel(m_modelTrackQueryTagsOption), true);

	//
	// Intermediate Results
	//

	/** Percentage for pre-processing. Set when execution starts up. */
	private double m_dPreProcessingShare;

	/** Column names. Result of pre-processing. */
	private String[] m_arrResultColumnNames;

	/**
	 * SMILES or SMARTS strings of query molecules read from second input table.
	 * Result of pre-processing.
	 */
	private String[] m_arrQueriesAsSmiles;

	/** ROMol objects of query molecules read from second input table. Result of pre-processing. */
	private ROMol[] m_arrQueriesAsRDKitMols;

	//
	// Constructor
	//

	/**
	 * Create new node model with one data in- and one out-port.
	 */
	SubstructureCounterNodeModel() {
		super(2, 1);
      registerInputTablesWithSizeLimits(1); // Query table supports only limited size
	}

	//
	// Protected Methods
	//
	
	/**
   //
   // Streaming API
   //
   
   @Override
   public InputPortRole[] getInputPortRoles() {
      return new InputPortRole[] { InputPortRole.DISTRIBUTED_STREAMABLE, InputPortRole.NONDISTRIBUTED_NONSTREAMABLE };
   }
   
   @Override
   public OutputPortRole[] getOutputPortRoles() {
      return new OutputPortRole[] { OutputPortRole.DISTRIBUTED };
   }
   
   @Override
   public StreamableOperator createStreamableOperator(PartitionInfo partitionInfo, PortObjectSpec[] inSpecs)
         throws InvalidSettingsException {
      // Make the table specs of the input table available
      final DataTableSpec[] tableSpec = new DataTableSpec[inSpecs.length];
      for (int i = 0; i < inSpecs.length; i++) {
         tableSpec[i] = (inSpecs[i] instanceof DataTableSpec ? (DataTableSpec)inSpecs[i] : null);
      }
      final InputDataInfo[][] arrInputDataInfos = createInputDataInfos(tableSpec);
      
      return new StreamableOperator() {
         @Override
         public void runFinal(PortInput[] inputs, PortOutput[] outputs, ExecutionContext exec) throws Exception {
            preProcessing(inputs, arrInputDataInfos, exec);
            ColumnRearranger rearranger = createColumnRearranger(0, tableSpec[0]);
            StreamableFunction func = rearranger.createStreamableFunction();
            func.runFinal(inputs, outputs, exec);
         }
      };
   }
   
   @Override
   public MergeOperator createMergeOperator() {
      return super.createMergeOperator(); // Null
   }
   
   @Override
   public void finishStreamableExecution(StreamableOperatorInternals internals, ExecutionContext exec,
         PortOutput[] output) throws Exception {
      super.finishStreamableExecution(internals, exec, output);
   }
   
   @Override
   public StreamableOperatorInternals createInitialStreamableOperatorInternals() {
      return super.createInitialStreamableOperatorInternals();
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
   
   */
	
   /**
    * Converts the passed in data to BufferedDataTable inputs where appropriate.
    * Input that are not BufferedDataTables will be converted to null.
    * Afterwards it calls the regular {@link #preProcessing(BufferedDataTable[], InputDataInfo[][], ExecutionContext) } 
    * in the model.
    *
    * @param inData The input data of the node.
    * @param arrInputDataInfo Information about all columns of the input tables.
    * @param exec The execution context.
    *
    * @throws Exception Thrown, if pre-processing fails.
    *
    * @see #m_arrResultColumnNames
    * @see #m_arrQueriesAsSmiles
    * @see #m_arrQueriesAsRDKitMols
    */
	/*
   protected void preProcessing(final PortInput[] inData, final InputDataInfo[][] arrInputDataInfo,
         final ExecutionContext exec) throws Exception {
      if (inData != null) {
         // Get and cast input - if possible to BufferedDataTable objects for normal pre-processing
         BufferedDataTable[] arrConvertedInData = new BufferedDataTable[inData.length];
         for (int i = 0; i < inData.length; i++) {
            if (inData[i] != null && inData[i] instanceof PortObjectInput &&
                  ((PortObjectInput)inData[i]).getPortObject() instanceof BufferedDataTable) {
               arrConvertedInData[i] = (BufferedDataTable)((PortObjectInput)inData[i]).getPortObject();
            }
            else {
               arrConvertedInData[i] = null;
            }
         }

         // Conversion of input data to adapter cells if required and recreate input data info afterwards
         BufferedDataTable[] arrConvertedTables = convertInputTables(arrConvertedInData, 
               arrInputDataInfo, exec.createSubExecutionContext(0.05d));
         InputDataInfo[][] arrUpdatedInputDataInfo = createInputDataInfos(getInputTableSpecs(arrConvertedTables));
         
         // Pre-process normally, which will create intermediate results for the running node
         // In distributed computing the intermediate results will be created on all partitioned instances
         preProcessing(arrConvertedInData, arrUpdatedInputDataInfo, exec);
      }
      else {
         // This is probably not useful, if we do not have any input data to process
         preProcessing((BufferedDataTable[])null, arrInputDataInfo, exec);
      }
   }
   */
   
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		// Reset warnings and check RDKit library readiness
		super.configure(inSpecs);

		// Auto guess the input mol column if not set - fails if no compatible column found
		SettingsUtils.autoGuessColumn(inSpecs[0], m_modelInputColumnName, RDKitMolValue.class, 0,
				"Auto guessing: Using column %COLUMN_NAME% as Mol input column.",
				"No RDKit Mol, SMILES or SDF compatible column in input table. Use the \"RDKit from Molecule\" " +
						"node to convert SMARTS.", getWarningConsolidator());

		// Determines, if the mol input column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[0], m_modelInputColumnName, RDKitMolValue.class,
				"Input column has not been specified yet.",
				"Input column %COLUMN_NAME% does not exist. Has the first input table changed?");

		// Auto guess the query mol column if not set - fails if no compatible column found
		final Class<? extends DataValue>[] arrClassesQueryType = new Class[] { SmartsValue.class, RDKitMolValue.class };
		SettingsUtils.autoGuessColumn(inSpecs[1], m_modelQueryColumnName, Arrays.asList(arrClassesQueryType),
				(inSpecs[0] == inSpecs[1] ? 1 : 0), // If 1st and 2nd table equal, auto guess with second match
				"Auto guessing: Using column %COLUMN_NAME% as query molecule column.",
				"No RDKit Mol or SMARTS compatible column in query molecule table.", getWarningConsolidator());

		// Determines, if the query mol column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[1], m_modelQueryColumnName, Arrays.asList(arrClassesQueryType),
				"Query molecule column has not been specified yet.",
				"Query molecule column %COLUMN_NAME% does not exist. Has the second input table changed?");

		// Check, if query name column exists, if user wants to use it
		if (m_modelUseQueryNameColumn.getBooleanValue()) {
			// Auto guess the query name column if not set - fails if no compatible column found
			SettingsUtils.autoGuessColumn(inSpecs[1], m_modelQueryNameColumn, StringValue.class,
					(inSpecs[0] == inSpecs[1] ? 1 : 0), // If 1st and 2nd table equal, auto guess with second match
					"Auto guessing: Using column %COLUMN_NAME% as query name column.",
					"No String compatible column (to be used as query name) in query molecule table.",
					getWarningConsolidator());

			// Determines, if the query name column exists - fails if it does not
			SettingsUtils.checkColumnExistence(inSpecs[1], m_modelQueryNameColumn, StringValue.class,
					"Substructure name column has not been specified yet.",
					"Substructure name column %COLUMN_NAME% does not exist. Has the second input table changed?");
		}

		// Auto guess the new total hits column name and make it unique
		SettingsUtils.autoGuessColumnName(inSpecs[0], null, null,
				m_modelCountTotalHitsColumn, DEFAULT_TOTAL_HITS_COLUMN);

		if (m_modelCountTotalHitsOption.getBooleanValue()) {
			SettingsUtils.checkColumnNameUniqueness(inSpecs[0], null, null,
					m_modelCountTotalHitsColumn,
					"Column name for total hits column has not been specified yet.",
					"The name %COLUMN_NAME% of the new total hits column exists already in the input.");
		}

		// Auto guess the new tags column name and make it unique
		SettingsUtils.autoGuessColumnName(inSpecs[0],
				new String[] { m_modelCountTotalHitsColumn.getStringValue() }, null,
				m_modelTrackQueryTagsColumn, DEFAULT_QUERY_TAGS_COLUMN);

		if (m_modelTrackQueryTagsOption.getBooleanValue()) {
			SettingsUtils.checkColumnNameUniqueness(inSpecs[0],
					new String[] { m_modelCountTotalHitsColumn.getStringValue() }, null,
					m_modelTrackQueryTagsColumn,
					"Column name for query tags column has not been specified yet.",
					"The name %COLUMN_NAME% of the new query tags column exists already in the input.");
		}

		// Consolidate all warnings and make them available to the user
		generateWarnings();

		// We cannot know how many columns we will generate before execution
		return new DataTableSpec[] { null };
	}

	/**
	 * This implementation generates input data info object for the input mol column
	 * as well as the query molecule column and connects it with the information coming
	 * from the appropriate setting models.
	 * {@inheritDoc}
	 */
	@Override
	protected InputDataInfo[] createInputDataInfos(final int inPort, final DataTableSpec inSpec)
			throws InvalidSettingsException {

		InputDataInfo[] arrDataInfo = null;

		switch (inPort) {
		case 0: // First table with molecule column
			arrDataInfo = new InputDataInfo[1]; // We have only one input mol column
			arrDataInfo[INPUT_COLUMN_MOL] = (inSpec == null ? null : new InputDataInfo(inSpec, m_modelInputColumnName,
					InputDataInfo.EmptyCellPolicy.DeliverEmptyRow, null,
					RDKitMolValue.class));
			break;

		case 1: // Second table with query molecule column and optional name column
			final boolean bUseNameColumn = m_modelUseQueryNameColumn.getBooleanValue();
			arrDataInfo = new InputDataInfo[bUseNameColumn ? 2 : 1];
			arrDataInfo[INPUT_COLUMN_QUERY] = (inSpec == null ? null : new InputDataInfo(inSpec, m_modelQueryColumnName,
					InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
					SmartsValue.class, RDKitMolValue.class));
			if (bUseNameColumn) {
				arrDataInfo[INPUT_COLUMN_NAME] = (inSpec == null ? null : new InputDataInfo(inSpec, m_modelQueryNameColumn,
						InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
						StringValue.class));
			}
			break;
		}

		return (arrDataInfo == null ? new InputDataInfo[0] : arrDataInfo);
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
			spec = createColumnRearranger(outPort, inSpecs[0]).createSpec();
		}

		return spec;
	}

	/**
	 * {@inheritDoc}
	 * Calculates additionally to the normal execution procedure the pre-processing
	 * percentage.
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		// Calculate pre-processing share based on overall rows
		m_dPreProcessingShare = inData[1].size() /
				(inData[0].size() + inData[1].size() + 1.0d);

		// Perform normalized execution
		return super.execute(inData, exec);
	}

	/**
	 * This implementation generates input data info object for the input mol column
	 * and connects it with the information coming from the appropriate setting model.
	 * {@inheritDoc}
	 */
	@Override
	protected AbstractRDKitCellFactory[] createOutputFactories(final int outPort, final DataTableSpec inSpec)
			throws InvalidSettingsException {

		AbstractRDKitCellFactory[] arrOutputFactories = null;

		// Specify output of table 1
		if (outPort == 0) {
			// Allocate space for all factories (usually we have only one)
			arrOutputFactories = new AbstractRDKitCellFactory[1];

			// Factory 1:
			// ==========
			// Generate column specs for the output table columns produced by this factory
			// This is only possible, if pre-processing took already place and we know about
			// query molecules
			final boolean bCountTotalHits = m_modelCountTotalHitsOption.getBooleanValue();
			final boolean bTrackQueryTags = m_modelTrackQueryTagsOption.getBooleanValue();
			final int iResultColumnCount = m_arrResultColumnNames.length;
			final int iTotalColumnCount = iResultColumnCount + (bCountTotalHits ? 1 : 0) + (bTrackQueryTags ? 1: 0);
			final DataColumnSpec[] arrOutputSpec = new DataColumnSpec[iTotalColumnCount];
			int iColIndex = 0;
			for (iColIndex = 0; iColIndex < iResultColumnCount; iColIndex++) {
				// Create spec with additional information
				final DataColumnSpecCreator creator = new DataColumnSpecCreator(m_arrResultColumnNames[iColIndex], IntCell.TYPE);
				HeaderPropertyUtils.writeInColumnSpec(creator,
						SmilesHeaderProperty.PROPERTY_SMILES, m_arrQueriesAsSmiles[iColIndex]);
				arrOutputSpec[iColIndex] = creator.createSpec();
			}

			if (bCountTotalHits) {
				arrOutputSpec[iColIndex++] = new DataColumnSpecCreator(m_modelCountTotalHitsColumn.getStringValue(), IntCell.TYPE).createSpec();
			}

			if (bTrackQueryTags) {
				arrOutputSpec[iColIndex++] = new DataColumnSpecCreator(m_modelTrackQueryTagsColumn.getStringValue(),
						ListCell.getCollectionType(StringCell.TYPE)).createSpec();
			}

			final SubstructMatchParameters ps = markForCleanup(new SubstructMatchParameters());
			ps.setUseChirality(m_modelUseChirality.getBooleanValue());
    		ps.setUseEnhancedStereo(m_modelUseEnhancedStereo.getBooleanValue());
			ps.setUniquify(m_modelUniqueMatchesOnly.getBooleanValue());
			
			// Generate factory
			arrOutputFactories[0] = new AbstractRDKitCellFactory(this, AbstractRDKitCellFactory.RowFailurePolicy.DeliverEmptyValues,
					getWarningConsolidator(), null, arrOutputSpec) {

				@Override
				/**
				 * This method implements the calculation logic to generate the new cells based on
				 * the input made available in the first (and second) parameter.
				 * {@inheritDoc}
				 */
				public DataCell[] process(final InputDataInfo[] arrInputDataInfo, final DataRow row, final long lUniqueWaveId) throws Exception {
					final DataCell[] arrOutputCells = createEmptyCells(iTotalColumnCount);

					// Calculate the new cells
					final ROMol mol = markForCleanup(arrInputDataInfo[INPUT_COLUMN_MOL].getROMol(row),
							lUniqueWaveId);

					final List<StringCell> listTags = (bTrackQueryTags ? new ArrayList<StringCell>() : null);
					int iTotalHitsCount = 0;
					int iColIndex;
					for (iColIndex = 0; iColIndex < iResultColumnCount; iColIndex++) {
						final ROMol query = m_arrQueriesAsRDKitMols[iColIndex];
						if (mol != null && query != null) {
							final Match_Vect_Vect ms = markForCleanup(
									mol.getSubstructMatches(query, ps), lUniqueWaveId);
							final int iHits = (int)ms.size();
							arrOutputCells[iColIndex] = new IntCell(iHits);
							iTotalHitsCount += iHits;
							if (bTrackQueryTags && iHits > 0) {
								listTags.add(new StringCell(m_arrResultColumnNames[iColIndex]));
							}
						}
					}

					if (bCountTotalHits) {
						arrOutputCells[iColIndex++] = new IntCell(iTotalHitsCount);
					}

					if (bTrackQueryTags) {
						arrOutputCells[iColIndex++] = CollectionCellFactory.createListCell(listTags);
					}

					return arrOutputCells;
				}
			};

			// Enable or disable this factory to allow parallel processing
			arrOutputFactories[0].setAllowParallelProcessing(true);
		}

		return (arrOutputFactories == null ? new AbstractRDKitCellFactory[0] : arrOutputFactories);
	}

	/**
	 * Returns the percentage of pre-processing activities from the total execution.
	 *
	 * @return Percentage of pre-processing.
	 */
	@Override
	protected double getPreProcessingPercentage() {
		return m_dPreProcessingShare;
	}

	/**
	 * Converts the query molecule cells into SMILES and ROMol values. The SMILES values will
	 * be used to name the new target columns. The ROMol values will be used to count the
	 * substructures using RDKit functionality. For invalid query cells a warning will be
	 * generated and they will not be taken into account when counting substructures.<br />
	 * This method gets called from the method {@link #execute(BufferedDataTable[], ExecutionContext)}, before
	 * the row-by-row processing starts. All necessary pre-calculations can be done here. Results of the method
	 * should be made available through member variables, which get picked up by the other methods like
	 * process(InputDataInfo[], DataRow) in the factory or
	 * {@link #postProcessing(BufferedDataTable[], BufferedDataTable[], ExecutionContext)} in the model.
	 *
	 * @param inData The input tables of the node.
	 * @param arrInputDataInfo Information about all columns of the input tables.
	 * @param exec The execution context, which was derived as sub-execution context based on the percentage
	 * 		setting of #getPreProcessingPercentage(). Track the progress from 0..1.
	 *
	 * @throws Exception Thrown, if pre-processing fails.
	 *
	 * @see #m_arrResultColumnNames
	 * @see #m_arrQueriesAsSmiles
	 * @see #m_arrQueriesAsRDKitMols
	 */
	@Override
	protected void preProcessing(final BufferedDataTable[] inData, final InputDataInfo[][] arrInputDataInfo,
			final ExecutionContext exec) throws Exception {
		final boolean bUseNameColumn = m_modelUseQueryNameColumn.getBooleanValue();
		final boolean bIsSmarts = arrInputDataInfo[1][INPUT_COLUMN_QUERY].getDataType().isCompatible(SmartsValue.class);
		final int iQueryRowCount = (int)inData[1].size();
		final List<String> listColumnNames = new ArrayList<String>(iQueryRowCount);
		final List<String> listQueriesAsSmiles = new ArrayList<String>(iQueryRowCount);
		final List<ROMol> listQueriesAsRDKitMols = new ArrayList<ROMol>(iQueryRowCount);
		final List<RowKey> listEmptyQueries = new ArrayList<RowKey>();
		final List<RowKey> listInvalidQueries = new ArrayList<RowKey>();
		final List<RowKey> listDuplicatedQueries = new ArrayList<RowKey>();
		final List<RowKey> listEmptyNames = new ArrayList<RowKey>();
		final Map<String, Integer> mapDuplicates = new HashMap<String, Integer>();

		int iRow = 0;

		// Creating an arrays of SMILES and ROMol query molecules
		for (final DataRow row : inData[1]) {
			ROMol mol = null;
			String strSmarts = null;

			// Process SMARTS
			if (bIsSmarts) {
				strSmarts = arrInputDataInfo[1][INPUT_COLUMN_QUERY].getSmarts(row);
				if (strSmarts != null) {
					mol = markForCleanup(RWMol.MolFromSmarts(strSmarts, 0, true));
					if (mol == null) {
						throw new ParseException("Could not parse SMARTS '"
								+ strSmarts + "' in row " + row.getKey(), 0);
					}
				}
			}
			else {
				// Get ROMol value
				mol = markForCleanup(arrInputDataInfo[1][INPUT_COLUMN_QUERY].getROMol(row));
			}

			// Generate column labels (SMILES)
			// If we cannot get a mol, we remember the empty column
			if (mol == null) {
				listEmptyQueries.add(row.getKey());
			}
			// Otherwise: Get canonical SMILES from the query molecule
			else {
				String strQueryMolString = null;

				// If we have SMARTS as input, use it directly
				if (bIsSmarts) {
					strQueryMolString = strSmarts;
				}

				// Check (by heuristics) if we have a SMARTS as query molecule in form of an RDKit mol cell
				else if (mol.getNumAtoms() > 0 && mol.getAtomWithIdx(0).hasQuery()) {
					strQueryMolString = RDKFuncs.MolToSmarts(mol);
				}

				// Or a SMILES (or SMARTS conversion failed)
				if (strQueryMolString == null) {
					strQueryMolString = RDKFuncs.MolToCXSmiles(mol);					
				}

				// Fallback, if SMARTS/SMILES conversion failed
				if (strQueryMolString == null) {
					listInvalidQueries.add(row.getKey());
				}

				// Otherwise: Everything is fine - use this query
				else {
					String strColumnName = strQueryMolString; // Default name

					// Optionally use a query name as header
					if (bUseNameColumn) {
						final String strName = arrInputDataInfo[1][INPUT_COLUMN_NAME].getString(row);
						if (strName != null) {
							strColumnName = strName;
						}
						else {
							listEmptyNames.add(row.getKey());
						}
					}

					// Check for query duplicate, still include it, but warn
					final Integer intCount = mapDuplicates.get(strQueryMolString);
					if (intCount != null) {
						final int iDuplicate = intCount + 1;
						mapDuplicates.put(strQueryMolString, iDuplicate);
						listDuplicatedQueries.add(row.getKey());
						strColumnName += " (Duplicate " + iDuplicate + ")";
					}
					else {
						mapDuplicates.put(strQueryMolString, 0);
					}

					// Ensure that our target column name is unique
					strColumnName = SettingsUtils.makeColumnNameUnique(strColumnName,
							inData[0].getDataTableSpec(), listColumnNames);
					listColumnNames.add(strColumnName);
					listQueriesAsRDKitMols.add(mol);
					listQueriesAsSmiles.add(strQueryMolString);
				}
			}

			iRow++;

			exec.setProgress(new StringBuilder("Analyzing query molecules (")
			.append(iRow)
			.append(" of ")
			.append(iQueryRowCount)
			.append(")").toString());
		}

		// Store queries and column labels (SMILES) in intermediate result member variables
		m_arrResultColumnNames = listColumnNames.toArray(
				new String[listColumnNames.size()]);
		m_arrQueriesAsRDKitMols = listQueriesAsRDKitMols.toArray(
				new ROMol[listQueriesAsRDKitMols.size()]);
		m_arrQueriesAsSmiles = listQueriesAsSmiles.toArray(
				new String[listQueriesAsSmiles.size()]);

		// Generate warnings
		generateWarning(listEmptyQueries, iQueryRowCount, "Ignoring empty");
		generateWarning(listInvalidQueries, iQueryRowCount, "Ignoring invalid");
		generateWarning(listDuplicatedQueries, iQueryRowCount, "Found duplicated");
		generateWarning(listEmptyNames, iQueryRowCount, "Replacing empty name with query string for");

		// Show the warning already immediately
		generateWarnings();

		// Help the garbage collector
		listColumnNames.clear();
		listQueriesAsRDKitMols.clear();
		listQueriesAsSmiles.clear();
		listEmptyQueries.clear();
		listInvalidQueries.clear();
		listDuplicatedQueries.clear();
		mapDuplicates.clear();

		exec.setProgress(1.0d);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanupIntermediateResults() {
		m_arrQueriesAsRDKitMols = null;
		m_arrQueriesAsSmiles = null;
		m_arrResultColumnNames = null;
		m_dPreProcessingShare = 0;
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		super.loadValidatedSettingsFrom(settings);

		// For old nodes we will fill the default column names for later added optional columns
		try {
			m_modelCountTotalHitsColumn.loadSettingsFrom(settings);
		}
		catch (final InvalidSettingsException excOrig) {
			m_modelCountTotalHitsColumn.setStringValue(DEFAULT_TOTAL_HITS_COLUMN);
		}
		try {
			m_modelTrackQueryTagsColumn.loadSettingsFrom(settings);
		}
		catch (final InvalidSettingsException excOrig) {
			m_modelTrackQueryTagsColumn.setStringValue(DEFAULT_QUERY_TAGS_COLUMN);
		}
	}

	//
	// Private Methods
	//

	/**
	 * Generates and saves a warning message, if applicable, based on the
	 * specified list of row keys (can be empty) and the message stump.
	 *
	 * @param listRowKeys List of row keys. Must not be null.
	 * @param iTotalQueries Total number of processed queries.
	 * @param strMsgStump Stump of the warning message to be generated.
	 */
	private void generateWarning(final List<RowKey> listRowKeys, final int iTotalQueries,
			final String strMsgStump) {
		final int iQueryCount = listRowKeys.size();

		if (iQueryCount > 0) {
			String strMsg = strMsgStump + " quer";
			if (iQueryCount <= 10) {
				final String rowKeyList = listRowKeys.toString();
				strMsg += "y in the following rows: " +
						rowKeyList.substring(1, rowKeyList.length() - 1);
			}
			else {
				strMsg +="ies.";
			}

			strMsg += " [" + iQueryCount + " of " + iTotalQueries +
					(iTotalQueries == 1 ? " query]" : " queries]");

			getWarningConsolidator().saveWarning(strMsg);
		}
	}
}
