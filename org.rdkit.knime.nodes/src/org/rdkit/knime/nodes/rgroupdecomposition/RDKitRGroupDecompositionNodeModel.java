/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2019
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
package org.rdkit.knime.nodes.rgroupdecomposition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.RDKit.Atom;
import org.RDKit.Int_Pair;
import org.RDKit.Match_Vect;
import org.RDKit.RDKFuncs;
import org.RDKit.RGroupDecomposition;
import org.RDKit.RGroupDecompositionParameters;
import org.RDKit.ROMol;
import org.RDKit.ROMol_Vect;
import org.RDKit.RWMol;
import org.RDKit.StringMolMap;
import org.RDKit.StringMolMap_Vect;
import org.RDKit.StringROMol_VectMap;
import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmartsCell;
import org.knime.chem.types.SmartsCellFactory;
import org.knime.chem.types.SmartsValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;
import org.rdkit.knime.types.RDKitAdapterCell;
import org.rdkit.knime.types.RDKitMolCellFactory;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsModelEnumeration;
import org.rdkit.knime.util.SettingsModelEnumerationArray;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.StringUtils;
import org.rdkit.knime.util.WarningConsolidator;
import org.rdkit.knime.util.WarningConsolidator.Context;

/**
 * This class implements the node model of the RDKitRGroups node
 * providing calculations based on the open source RDKit library.
 * 
 * @author Manuel Schwarze
 */
public class RDKitRGroupDecompositionNodeModel extends AbstractRDKitNodeModel {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitRGroupDecompositionNodeModel.class);

	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;

	/** Input data info index for SMARTS values for defining cores (optional table). */
	protected static final int INPUT_COLUMN_CORES = 0;
	
	/** Row context for generating warnings, if something is incorrect in table 2 (cores). */
	protected static final WarningConsolidator.Context ROW_CONTEXT_TABLE_CORES = new Context("core row", "row", "rows", true);
	
	/** Row context for generating warnings, if something is causing trouble related to the the r-groups output table. */
	protected static final WarningConsolidator.Context ROW_CONTEXT_R_GROUP_OUTPUT_TABLE = new Context("r-group row", "row", "rows", true);

	/** Allowed data types of core inputs. */
	protected static final List<Class<? extends DataValue>> CORE_INPUT_VALUE_CLASSES = new ArrayList<>();

	/** Enumeration of all possible R group column names (used for column uniqueness checking only). */
	protected static final List<String> R_GROUP_NAMES = new ArrayList<>();
	
	static {
		CORE_INPUT_VALUE_CLASSES.add(RDKitMolValue.class);
		CORE_INPUT_VALUE_CLASSES.add(SmartsValue.class);
		CORE_INPUT_VALUE_CLASSES.add(SmilesValue.class);
		CORE_INPUT_VALUE_CLASSES.add(SdfValue.class);
		
		for (int i = 0; i < 100; i++) {
			R_GROUP_NAMES.add("R" + i);
		}
	}
	
	//
	// Members
	//

	/** Settings model for the column name of the input column. */
	private final SettingsModelString m_modelInputColumnName =
			registerSettings(RDKitRGroupDecompositionNodeDialog.createInputColumnNameModel());

	/** Settings model for the column name of the SMARTS input column (cores). */
	private final SettingsModelString m_modelCoresInputColumnName =
			registerSettings(RDKitRGroupDecompositionNodeDialog.createCoresInputColumnNameModel());

	/** Settings model for the core smarts to be used for the R-Group Decomposition. */
	private final SettingsModelString m_modelCoreSmarts =
			registerSettings(RDKitRGroupDecompositionNodeDialog.createSmartsModel());

	/** Settings model for the option to remove empty Rx columns. */
	private final SettingsModelBoolean m_modelRemoveEmptyColumns =
			registerSettings(RDKitRGroupDecompositionNodeDialog.createRemoveEmptyColumnsModel());

	/** Settings model for the option to let the node fail, if there is match at all. */
	private final SettingsModelBoolean m_modelFailForNoMatch =
			registerSettings(RDKitRGroupDecompositionNodeDialog.createFailForNoMatchOptionModel());

	/** Settings model for the option to add the matching SMARTS core. This was not available in the initial version. */
	private final SettingsModelBoolean m_modelAddMatchingSmartsCore =
			registerSettings(RDKitRGroupDecompositionNodeDialog.createAddMatchingSmartsCoreModel(), true);

	/** Settings model to specify the new matching SMARTS core column name. This was not available in the initial version. */
	private final SettingsModelString m_modelNewMatchingSmartsCoreColumnName =
			registerSettings(RDKitRGroupDecompositionNodeDialog.createNewMatchingSmartsCoreColumnNameModel(m_modelAddMatchingSmartsCore), true);

	/** Settings model for the option to add the matching substructure. This was not available in the initial version. */
	private final SettingsModelBoolean m_modelAddMatchingSubstructure =
			registerSettings(RDKitRGroupDecompositionNodeDialog.createAddMatchingSubstructureModel(), true);

	/** Settings model to specify the new matching substructure column name. This was not available in the initial version. */
	private final SettingsModelString m_modelNewMatchingSubstructureColumnName =
			registerSettings(RDKitRGroupDecompositionNodeDialog.createNewMatchingSubstructureColumnNameModel(m_modelAddMatchingSubstructure), true);

	/** Settings model for the option to use atom maps when generating the matching substructure. This was not available in the initial version. */
	private final SettingsModelBoolean m_modelUseAtomMaps =
			registerSettings(RDKitRGroupDecompositionNodeDialog.createUseAtomMapsModel(m_modelAddMatchingSubstructure), true);

	/** Settings model for the option to use R labels when generating the matching substructure. This was not available in the initial version. */
	private final SettingsModelBoolean m_modelUseRLabels =
			registerSettings(RDKitRGroupDecompositionNodeDialog.createUseRLabelsModel(m_modelAddMatchingSubstructure), true);
	
	/** Settings model for R Group labels options. */
	private final SettingsModelEnumerationArray<Labels> m_modelLabels =
			registerSettings(RDKitRGroupDecompositionNodeDialog.createLabelsModel());
	
	/** Settings model for R Group matching strategy. */
	private final SettingsModelEnumeration<Matching> m_modelMatchingStrategy = 
			registerSettings(RDKitRGroupDecompositionNodeDialog.createMatchingStrategyModel());
	
	/** Settings model for R Group labeling options. */
	private final SettingsModelEnumerationArray<Labeling> m_modelLabeling =
			registerSettings(RDKitRGroupDecompositionNodeDialog.createLabelingModel());

	/** Settings model for R Group core alignment. */
	private final SettingsModelEnumeration<CoreAlignment> m_modelCoreAlignment =
			registerSettings(RDKitRGroupDecompositionNodeDialog.createCoreAlignmentModel());

	/** Settings model for the option to only match R Groups. */
	private final SettingsModelBoolean m_modelOnlyMatchAtRGroupsModel =
			registerSettings(RDKitRGroupDecompositionNodeDialog.createMatchOnlyAtRGroupsModel());

	/** Settings model for the option to remove hydrogen only R Groups. */
	private final SettingsModelBoolean m_modelRemoveHydrogenOnlyRGroupsModel =
			registerSettings(RDKitRGroupDecompositionNodeDialog.createRemoveHydrogenOnlyRGroupsModel());

	/** Settings model for the option to remove hydrogens post match. */
	private final SettingsModelBoolean m_modelRemoveHydrogensPostMatchModel = 
			registerSettings(RDKitRGroupDecompositionNodeDialog.createRemoveHydrogensPostMatchModel());

	//
	// Intermediate Results
	//
	
	/** Generated cores based on either input table or parameter. */
	private ROMol[] m_arrSmarts = null;
	
	/** Boolean array to remember which R-Group columns are non-empty. */
	private boolean[] m_arrNonEmptyColumn = null;

	//
	// Constructor
	//

	/**
	 * Create new node model with one data in- and one out-port.
	 */
	RDKitRGroupDecompositionNodeModel() {
		super(new PortType[] {
					// Input ports (2nd port is optional)
					PortTypeRegistry.getInstance().getPortType(BufferedDataTable.TYPE.getPortObjectClass(), false),
					PortTypeRegistry.getInstance().getPortType(BufferedDataTable.TYPE.getPortObjectClass(), true) },
				new PortType[] {
				// Output ports
					PortTypeRegistry.getInstance().getPortType(BufferedDataTable.TYPE.getPortObjectClass(), false),  
					PortTypeRegistry.getInstance().getPortType(BufferedDataTable.TYPE.getPortObjectClass(), false) 
				});

		getWarningConsolidator().registerContext(ROW_CONTEXT_TABLE_CORES);
	}

	//
	// Protected Methods
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		// Reset warnings and check RDKit library readiness
		super.configure(inSpecs);

		// Auto guess the input column if not set - fails if no compatible column found
		SettingsUtils.autoGuessColumn(inSpecs[0], m_modelInputColumnName, RDKitMolValue.class, 0,
				"Auto guessing input molecules in table 1: Using column %COLUMN_NAME%.",
				"No RDKit Mol, SMILES or SDF compatible column in input table 1.", getWarningConsolidator());
		
		// Determines, if the input column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[0], m_modelInputColumnName, RDKitMolValue.class,
				"Input column has not been specified yet.",
				"Input column %COLUMN_NAME% does not exist. Has the input table changed?");

		// Alternative core input table handling
		if (hasAdditionalInputTable(inSpecs)) {
			/* 
			// Disabled, because hiding the parameter if a cores table is connected makes more sense and is cleaner
			// Warn, if beside the core input table also a core SMARTS parameter is set
			if (!m_modelCoreSmarts.getStringValue().trim().isEmpty()) {
				getWarningConsolidator().saveWarning("You set a core SMARTS parameter (currently hidden), but also connected a second table "
						+ "with core inputs. The table will take precedence, the core SMARTS parameter will be ignored.");
			}
			*/

			// Auto guess the core input column if not set - fails if no compatible column found
			SettingsUtils.autoGuessColumn(inSpecs[1], m_modelCoresInputColumnName, CORE_INPUT_VALUE_CLASSES, 0,
					"Auto guessing core input column in table 2: Using column %COLUMN_NAME%.",
					"No RDKit Mol, SMARTS, SMILES or SDF compatible column in input table 2.", getWarningConsolidator());
			
			// Determines, if the core input column exists - fails if it does not
			SettingsUtils.checkColumnExistence(inSpecs[1], m_modelCoresInputColumnName, CORE_INPUT_VALUE_CLASSES,
					"Core input column has not been specified yet.",
					"Core input column %COLUMN_NAME% does not exist. Has the input table 2 changed?");
		}
		else {
			// Check core SMARTS parameter
			if (m_modelCoreSmarts.getStringValue().trim().isEmpty()) {
				throw new InvalidSettingsException("Please provide at least one valid core SMARTS as parameter or connect a second input table with cores.");
			}
		}
		
		// Validate output columns
		ArrayList<String> listMoreColumnNames = new ArrayList<String>(R_GROUP_NAMES);
		
		// Check validity uniqueness of potential SMARTS core column name
		SettingsUtils.checkColumnNameUniqueness(inSpecs[0], listMoreColumnNames.toArray(new String[listMoreColumnNames.size()]),
				null, m_modelNewMatchingSmartsCoreColumnName, 
				m_modelAddMatchingSmartsCore.getBooleanValue() ? "Output column for SMARTS core has not been specified yet." : null, 
				m_modelAddMatchingSmartsCore.getBooleanValue() ? "The name %COLUMN_NAME% of the new SMARTS core column exists already in the input or conflicts with an Rxx column." : null);

		if (m_modelAddMatchingSmartsCore.getBooleanValue()) {
			listMoreColumnNames.add(m_modelNewMatchingSmartsCoreColumnName.getStringValue());
		}

		// Check validity uniqueness of potential substructure match column name
		SettingsUtils.checkColumnNameUniqueness(inSpecs[0], listMoreColumnNames.toArray(new String[listMoreColumnNames.size()]),
				null, m_modelNewMatchingSubstructureColumnName, 
				m_modelAddMatchingSubstructure.getBooleanValue() ? "Output column for substructure match has not been specified yet." : null, 
				m_modelAddMatchingSubstructure.getBooleanValue() ? "The name %COLUMN_NAME% of the new substructure match column conflicts with an existing name." : null);
		
		// Consolidate all warnings and make them available to the user
		generateWarnings();

		// Generate output specs
		DataTableSpec[] arrSpecs = getOutputTableSpecs(inSpecs);
		
		return arrSpecs;
	}

	/**
	 * This implementation generates input data info object for the input mol column
	 * and connects it with the information coming from the appropriate setting model.
	 * {@inheritDoc}
	 */
	@Override
	protected InputDataInfo[] createInputDataInfos(final int inPort, final DataTableSpec inSpec)
			throws InvalidSettingsException {

		InputDataInfo[] arrDataInfo = null;

		// Specify input of table 1 (molecules)
		if (inPort == 0) {
			arrDataInfo = new InputDataInfo[1]; // We have only one molecule input column
			arrDataInfo[INPUT_COLUMN_MOL] = new InputDataInfo(inSpec, m_modelInputColumnName,
					InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
					RDKitMolValue.class);
		}
		
		// Specify input of optional table 2 (cores)
		else if (inPort == 1 && inSpec != null) {
			arrDataInfo = new InputDataInfo[1]; // We have only one core input column
			arrDataInfo[INPUT_COLUMN_CORES] = new InputDataInfo(inSpec, m_modelCoresInputColumnName,
					InputDataInfo.EmptyCellPolicy.TreatAsNull, null, CORE_INPUT_VALUE_CLASSES);
		}

		return (arrDataInfo == null ? new InputDataInfo[0] : arrDataInfo);
	}

	/**
	 * Returns the output table specification of the specified out port.
	 * We cannot predict what the R groups will be called, because this 
	 * depends highly on the labels of the input cores. Therefore for port 0
	 * this returns always null. For port 1 it returns always the spec
	 * of the first input table. 
	 *  
	 * @param outPort Index of output port in focus. Zero-based.
	 * @param inSpecs All input table specifications.
	 * 
	 * @return The specification of all output tables.
	 * 
	 * @throws InvalidSettingsException Thrown, if the settings are inconsistent with
	 * 		given DataTableSpec elements.
	 */
	@Override
	protected DataTableSpec getOutputTableSpec(final int outPort,
			final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		DataTableSpec spec = null;

		switch (outPort) {

		case 0:
			// We cannot predict what the R groups will be called, because this
			// depends highly on the labels of the input cores
			break;
			
		case 1:
			// Same as input table as we just put in the same rows that did not match
			DataTableSpecCreator specCreator = new DataTableSpecCreator(inSpecs[0]);
			specCreator.setName("Unmatched");
			spec = specCreator.createSpec();
			break;
		}

		return spec;
	}

	@Override
	protected double getPreProcessingPercentage() {
		return 0.05d;
	}
	
	/**
	 * Prepares all cores to be used as input for R-Group Decomposition. They
	 * come either from a parameter (with one or more SMARTS values) or 
	 * from an optional second input table which may contain RDKitMol values,
	 * SMARTS, SMILES or SDF values. A table would always take precedence.
	 */
	@Override
	protected void preProcessing(BufferedDataTable[] inData, InputDataInfo[][] arrInputDataInfo, ExecutionContext exec)
			throws Exception {
		if (inData.length > 1 && inData[1] != null) {
			if (!evaluateCores(inData[1], arrInputDataInfo[1][INPUT_COLUMN_CORES])) {
				throw new RuntimeException("No valid cores found in input table 2.");
			}
		}
		else {
			if (!evaluateCores(m_modelCoreSmarts.getStringValue())) {
				throw new RuntimeException("No valid SMARTS found in cores parameter.");
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] processing(final BufferedDataTable[] inData, 
			final InputDataInfo[][] arrInputDataInfo,
			final ExecutionContext exec) throws Exception {
		// Setup helpers
		final WarningConsolidator warnings = getWarningConsolidator();
		
		// Contains the rows from input which did not match
		final BufferedDataContainer unmatchedTableData = exec.createDataContainer(getOutputTableSpecs(inData)[1]);

		// Get settings and define data specific behavior
		final long lTotalRowCount = inData[0].size();

		// Setup decomposition parameters
		RGroupDecompositionParameters params = markForCleanup(new RGroupDecompositionParameters());
		params.setLabels(Labels.getCombinedValues(m_modelLabels.getValues()));
		params.setMatchingStrategy(m_modelMatchingStrategy.getValue().getRDKitRGroupMatching().swigValue());
		params.setRgroupLabelling(Labeling.getCombinedValues(m_modelLabeling.getValues()));
		params.setAlignment(m_modelCoreAlignment.getValue().getRDKitRGroupCoreAlignment().swigValue());
		params.setOnlyMatchAtRGroups(m_modelOnlyMatchAtRGroupsModel.getBooleanValue());
		params.setRemoveAllHydrogenRGroups(m_modelRemoveHydrogenOnlyRGroupsModel.getBooleanValue());
		params.setRemoveHydrogensPostMatch(m_modelRemoveHydrogensPostMatchModel.getBooleanValue());
		
		// Get settings for additional output columns
		boolean bAddMatchingSmartsCore = m_modelAddMatchingSmartsCore.getBooleanValue();
		boolean bAddMatchingSubstructure = m_modelAddMatchingSubstructure.getBooleanValue();
		String strCoreColumnName = m_modelNewMatchingSmartsCoreColumnName.getStringValue();
		String strSubstructureColumnName = m_modelNewMatchingSubstructureColumnName.getStringValue();
		boolean bUseAtomMaps = m_modelUseAtomMaps.getBooleanValue();
		boolean bUseRLabels = m_modelUseRLabels.getBooleanValue();
		int iNumNonRGroupCols = (bAddMatchingSmartsCore ? 1 : 0) + (bAddMatchingSubstructure ? 1 : 0);
		
		// Feed in scaffolds
		ROMol_Vect vScaffolds = markForCleanup(new ROMol_Vect());
		for (ROMol scaffold : m_arrSmarts) {
			vScaffolds.add(scaffold);
		}
		RGroupDecomposition decomp = markForCleanup(new RGroupDecomposition(vScaffolds, params));
		
		// Get all molecules to be used as input and add them for decomposition
		int rowIndex = 0;
		final ExecutionContext execSubAdd = exec.createSubExecutionContext(0.50d);
		final ExecutionContext execSubProcess = exec.createSubExecutionContext(0.25d);
		final ExecutionContext execSubGetResults = exec.createSubExecutionContext(0.25d);
		final List<Boolean> listMatched = new ArrayList<Boolean>();
		for (final CloseableRowIterator i = inData[0].iterator(); i.hasNext(); rowIndex++) {
			final DataRow row = i.next();

			final ROMol mol = markForCleanup(arrInputDataInfo[0][INPUT_COLUMN_MOL].getROMol(row));

			// We use only cells, which are not missing
			boolean bMatched = false;
			if (mol != null) {
				bMatched = (decomp.add(mol) >= 0);
			}
			else {
				warnings.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(), "Encountered empty molecule cell in table 1 - ignored it.");
			}

			listMatched.add(bMatched);
			if (!bMatched) {
				unmatchedTableData.addRowToTable(row);
			}

			// Every 20 iterations check cancellation status and report progress
			if (rowIndex % 20 == 0) {
				AbstractRDKitNodeModel.reportProgress(execSubAdd, rowIndex, lTotalRowCount, row, " - Detecting matches of cores");
			}
		}

		execSubAdd.setProgress(1.0d);

		// Perform decomposition
		execSubProcess.setMessage("Running R-Group decomposition for " + listMatched.size() + " (of " + lTotalRowCount + ") matching molecules");
		final AtomicBoolean abSuccess = new AtomicBoolean(false);
		Thread threadWorker = new Thread(new Runnable() {
			public void run() {
				abSuccess.set(decomp.process());
			}
		}, "R-Group-Decomposition");
		threadWorker.setDaemon(true);
		threadWorker.start();
		// Wait for the thread to finish or for the user to cancel
		monitorWorkingThreadExecution(threadWorker, execSubProcess, (int)(lTotalRowCount / 10.0d * 100.0d), true, false);
		
		if (!abSuccess.get() && m_modelFailForNoMatch.getBooleanValue()) {
			throw new RuntimeException("No R-Group matches found.");
		}
		
		execSubProcess.checkCanceled();
		execSubProcess.setProgress(1.0d);

		rowIndex = 0;
		int matchIndex = 0;
		
		List<String> lColNames = new ArrayList<String>();
		int iNumRGroups = 0;
		
		if (abSuccess.get()) {
			// Figure out columns that are being returned
			// TODO: Replace with decomp.getRGroupsAsColumns().keys() functionality when it becomes available
			StringROMol_VectMap vColumns = markForCleanup(decomp.getRGroupsAsColumns());
			iNumRGroups = (int)vColumns.size() - 1; // -1 for the Core column, which is always there
			for (int i = 0; i < 100 /* 99 is the largest possible reaction class number */ && lColNames.size() < iNumRGroups; i++) {
				if (vColumns.has_key("R" + i)) {
					lColNames.add("R" + i);
				}
			}
			m_arrNonEmptyColumn = (iNumRGroups > 0 ? new boolean[iNumRGroups] : null);
		}
		
		// Create output table 1 specification
		DataTableSpecCreator specCreator = new DataTableSpecCreator(inData[0].getSpec());
		specCreator.setName("RGroups");
		if (bAddMatchingSmartsCore) {
			specCreator.addColumns(new DataColumnSpecCreator(strCoreColumnName, SmartsCell.TYPE).createSpec());
		}
		if (bAddMatchingSubstructure) {
			specCreator.addColumns(new DataColumnSpecCreator(strSubstructureColumnName, RDKitAdapterCell.RAW_TYPE).createSpec());
		}
		for (int i = 0; i < iNumRGroups; i++) {
			specCreator.addColumns(new DataColumnSpecCreator(lColNames.get(i), RDKitAdapterCell.RAW_TYPE).createSpec());
		}

		// Contains the rows with the R Groups that matched
		final BufferedDataContainer matchedTableData = exec.createDataContainer(specCreator.createSpec());

		if (abSuccess.get()) { 		
			// Get all rows with R group matches		
			StringMolMap_Vect vResults = markForCleanup(decomp.getRGroupsAsRows());
			
			for (final CloseableRowIterator i = inData[0].iterator(); i.hasNext(); rowIndex++) {
				final DataRow row = i.next();
			
				if (((Boolean)listMatched.get(rowIndex)).booleanValue()) {
					int iColIndex = 0;
					StringMolMap mapResults = vResults.get(matchIndex++);
					
					// Create matching core and optionally the cell for it
					ROMol molCore = markForCleanup(mapResults.get("Core"));
					String strSmartsWithoutStereoChemistry = RDKFuncs.MolToSmarts(molCore, false); // Do not include stereo chemistry
					DataCell[] arrResultCells = AbstractRDKitCellFactory.createEmptyCells(iNumNonRGroupCols + iNumRGroups);
					if (bAddMatchingSmartsCore) {
						arrResultCells[iColIndex++] = SmartsCellFactory.create(strSmartsWithoutStereoChemistry); 
					}
					
					// Create optionally matching substructure
					if (bAddMatchingSubstructure) {
						final long lUniqueWaveId = createUniqueCleanupWaveId();
						try {
							ROMol molSubstructure = markForCleanup(generateMatchingSubstructure(
									markForCleanup(arrInputDataInfo[0][INPUT_COLUMN_MOL].getROMol(row)), 
										strSmartsWithoutStereoChemistry, bUseAtomMaps, bUseRLabels, lUniqueWaveId));
							arrResultCells[iColIndex] = RDKitMolCellFactory.createRDKitAdapterCell(molSubstructure);
						}
						catch (Exception exc) {
							warnings.saveWarning(ROW_CONTEXT_R_GROUP_OUTPUT_TABLE.getId(), 
									"Unable to generate matching substructure for SMARTS core.");
							arrResultCells[iColIndex] = DataType.getMissingCell();
						}
						finally {
							cleanupMarkedObjects(lUniqueWaveId);
						}
						iColIndex++;
					}
					
					// Create all R group cells
					for (int iR = 0; iR < iNumRGroups; iR++) {
						String strRGroup = lColNames.get(iR);
						if (mapResults.has_key(strRGroup)) {
							ROMol rGroup = markForCleanup(mapResults.get(strRGroup));
							arrResultCells[iColIndex + iR] = RDKitMolCellFactory.createRDKitAdapterCell(rGroup);
							m_arrNonEmptyColumn[iR] = true;
						}
					}
					
					// Append core and R group cells to input columns
					DataRow resultRow = AbstractRDKitCellFactory.mergeDataCells(row, arrResultCells, -1);
					matchedTableData.addRowToTable(resultRow);
		
					// Every 20 iterations check cancellation status and report progress
					if (rowIndex % 20 == 0) {
						AbstractRDKitNodeModel.reportProgress(execSubGetResults, rowIndex, lTotalRowCount, row, " - Processing R-Groups decomposition results");
					}
				}
			}
		}
		
		execSubGetResults.setProgress(1.0d);

		matchedTableData.close();
		unmatchedTableData.close();

		return new BufferedDataTable[] { matchedTableData.getTable(), unmatchedTableData.getTable() };
	}

	/**
	 * {@inheritDoc}
	 * This implementation returns always 0.05d.
	 * 
	 * @return Returns always 0.05d.
	 */
	@Override
	protected double getPostProcessingPercentage() {
		return 0.05d;
	}

	/**
	 * In the case that the option to remove empty Rx columns is enabled,
	 * this post processing routine will do exactly that.
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] postProcessing(final BufferedDataTable[] inData,
			final InputDataInfo[][] arrInputDataInfo,
			final BufferedDataTable[] processingResult, final ExecutionContext exec)
					throws Exception {

		BufferedDataTable[] arrResults = processingResult;

		// Determine, if the user wants to remove all empty Rx columns
		if (m_modelRemoveEmptyColumns.getBooleanValue()) {
			if (processingResult != null && processingResult.length > 0 && m_arrNonEmptyColumn != null) {
				final ColumnRearranger rearranger = new ColumnRearranger(processingResult[0].getDataTableSpec());
				int iOffset = processingResult[0].getDataTableSpec().getNumColumns() - m_arrNonEmptyColumn.length;
				// Rearrange from right to left, otherwise the index position would "move" due to the deletion
				for (int i = m_arrNonEmptyColumn.length - 1; i >= 0; i--) {
					if (!m_arrNonEmptyColumn[i]) {
						rearranger.remove(iOffset + i);
					}
				}

				// Create the new table without empty Rx columns
				arrResults = new BufferedDataTable[] {
						exec.createColumnRearrangeTable(processingResult[0], rearranger, exec),
						arrResults[1]
				};
			}
		}

		return arrResults;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanupIntermediateResults() {
		m_arrSmarts = null;
		m_arrNonEmptyColumn = null;
	}

	
	/**
	 * Takes a data table and input data information about RDKitMols,
	 * SMARTS, SMILES or SDF values and creates 
	 * RDKit Molecules from them, determines the number of resulting R-Groups
	 * from them (by summing up their atoms), and makes the results
	 * available as intermediate variables used during configuration
	 * to determine the output spec of the result table and during
	 * processing to use them as input cores. Existing intermediate
	 * variables will be freed and overwritten.
	 * 
	 * @param strSmartsList Comma separated list of SMARTS. Can be null.
	 * 
	 * @return True, if at least one usable core was found. False otherwise.
	 */
	protected boolean evaluateCores(BufferedDataTable inData, InputDataInfo inputDataInfo) {
		cleanupIntermediateResults();
		
		DataType type = inputDataInfo.getDataType();
		
		boolean bRDKit = (type.isCompatible(RDKitMolValue.class) || 
				(type.isCompatible(AdapterValue.class) && type.isAdaptable(RDKitMolValue.class)));
		boolean bSmarts = (type.isCompatible(SmartsValue.class) || 
				(type.isCompatible(AdapterValue.class) && type.isAdaptable(SmartsValue.class)));
		boolean bSmiles = (type.isCompatible(SmilesValue.class) || 
				(type.isCompatible(AdapterValue.class) && type.isAdaptable(SmilesValue.class)));
		boolean bSdf = (type.isCompatible(SdfValue.class) || 
				(type.isCompatible(AdapterValue.class) && type.isAdaptable(SdfValue.class)));
		
		WarningConsolidator warnings = getWarningConsolidator();
		List<ROMol> listCores = new ArrayList<>();

		for (CloseableRowIterator rows = inData.iterator(); rows.hasNext(); ) {
			ROMol molCore = null;
			DataRow row = rows.next();
			String strErrorMsg = null;
			
			if (inputDataInfo.isMissing(row)) {
				warnings.saveWarning(ROW_CONTEXT_TABLE_CORES.getId(), "Encountered empty core molecule cell in input table 2 - ignored it.");
			}
			else {
				try {
					if (bRDKit) {
						molCore = markForCleanup(inputDataInfo.getROMol(row));
					}
					else if (bSmarts) {
						String strSmarts = inputDataInfo.getSmarts(row);
						if (strSmarts != null) {
							// Merge Hs
							molCore = markForCleanup(RWMol.MolFromSmarts(strSmarts, 0, true));
						}
					}
					else if (bSmiles) {
						String strSmiles = inputDataInfo.getSmiles(row);
						if (strSmiles != null) {
							// Do not sanitize
							molCore = markForCleanup(RWMol.MolFromSmiles(strSmiles, 0, false));
						}
					}
					else if (bSdf) {
						String strSdf = inputDataInfo.getSdfValue(row);
						if (strSdf != null) {
							// Do not sanitize, do not remove Hs
							molCore = markForCleanup(RWMol.MolFromMolBlock(strSdf, false, false));
						}
					}
				}
				catch (Exception exc) {
					strErrorMsg = exc.getMessage();
				}
				
				if (molCore != null) {
					listCores.add(molCore);
				}
				else {
					warnings.saveWarning(ROW_CONTEXT_TABLE_CORES.getId(), "Invalid " +
							(bRDKit ? "RDKitMol" : bSmarts ? "SMARTS" : bSmiles ? "SMILES" : bSdf ? "SDF" : "") +
							" core found in input table 2" + 
							(StringUtils.isEmptyAfterTrimming(strErrorMsg) ? "" : " (" + strErrorMsg + ")") + ".");
				}
			}
		}	
		
		m_arrSmarts = listCores.toArray(new ROMol[listCores.size()]);

		return (m_arrSmarts.length > 0);
	}

	/**
	 * Takes a comma separated list of SMARTS values and creates 
	 * RDKit Molecules from them, determines the number of resulting R-Groups
	 * from them (by summing up their atoms), and makes the results
	 * available as intermediate variables used during configuration
	 * to determine the output spec of the result table and during
	 * processing to use them as input cores. Existing intermediate
	 * variables will be freed and overwritten.
	 * 
	 * @param strSmartsList Comma separated list of SMARTS. Can be null.
	 * 
	 * @return True, if at least one usable core was found. False otherwise.
	 */
	protected boolean evaluateCores(String strSmartsList) {
		cleanupIntermediateResults();
		
		WarningConsolidator warnings = getWarningConsolidator();
		String[] arrCores = m_modelCoreSmarts.getStringValue().trim().split("\n");
		List<ROMol> listCores = new ArrayList<>();
		
		for (String strSmarts : arrCores) {
			ROMol molCore = null;
			String strErrorMsg = null;
			
			if (!strSmarts.trim().isEmpty()) {
				strSmarts = strSmarts.trim();
				try {
					molCore = markForCleanup(RWMol.MolFromSmarts(strSmarts, 0, true));
				}
				catch (Exception exc) {
					strErrorMsg = exc.getMessage();
				}
	
				if (molCore != null) {
					listCores.add(molCore);
				}
				else {
					warnings.saveWarning(ROW_CONTEXT_TABLE_CORES.getId(), "Invalid SMARTS core found in parameter" + 
							(StringUtils.isEmptyAfterTrimming(strErrorMsg) ? "" : " (" + strErrorMsg + ")") + ".");
				}
			}
		}	
		
		m_arrSmarts = listCores.toArray(new ROMol[listCores.size()]);
		
		return (m_arrSmarts.length > 0);
	}
	
	/**
	 * Generates the matching substructure from the passed in RDKit molecule based on the passed in SMARTS core.
	 * It is responsibility of the caller to clean up the RDKit molecule returned by this method when it is 
	 * not needed anymore.
	 * 
	 * @param mol Input molecule. Can be null to return null.
	 * @param strSmartsCore SMARTS core to be used for finding the substructure. Can be null to return null.
	 * @param bUseAtomMaps True to use atom map number from scaffold to set as atom property in substructure.
	 * @param bUseRLabels True to use R Labels to set atom properties "_MolFileRLabel" and "dummyLabel".
	 * @param lUniqueWaveId Used for marking RDKit objects for cleanup.
	 * 
	 * @return Matching substructure or null, if non was matching.
	 * 
	 * @throws Exception Thrown if processing failed.
	 */
	protected ROMol generateMatchingSubstructure(ROMol mol, String strSmartsCore, boolean bUseAtomMaps, 
			boolean bUseRLabels, long lUniqueWaveId) throws Exception {
		ROMol molSubstructure = null;

		if (mol != null && strSmartsCore != null) {
			// Massage SMARTS to allow correct matches
			String repl = strSmartsCore.replaceAll("#0","*");
			ROMol molScaffold = markForCleanup(RWMol.MolFromSmarts(repl), lUniqueWaveId);
			
			// Get all atoms of matching substructure in original molecule
			Match_Vect mv;
			RWMol molWithHs = markForCleanup(new RWMol(mol.addHs(false, true)), lUniqueWaveId);
			mv = markForCleanup(molWithHs.getSubstructMatch(molScaffold), lUniqueWaveId);	
	
			// Initially mark all atoms to be potentially removed
			boolean[] arrAtomsToRemove = new boolean[(int)molWithHs.getNumAtoms()];
			for (int i = 0; i < (int)molWithHs.getNumAtoms(); i++){
				arrAtomsToRemove[i] = true;
			}
			
			// Start by updating atom identities
			for (int i = 0; i < mv.size(); i++) {
				Int_Pair mi = mv.get(i);
				Atom sAtom = markForCleanup(molScaffold.getAtomWithIdx(i), lUniqueWaveId);
				
				// The R group attachment points are labeled, so we can recognize them
				if (sAtom.getAtomMapNum() != 0) {
					Atom mAtom = markForCleanup(molWithHs.getAtomWithIdx(mi.getSecond()), lUniqueWaveId);
					mAtom.setAtomicNum(0);
					mAtom.setIsAromatic(false);
					
					if(bUseAtomMaps) {
						mAtom.setAtomMapNum(sAtom.getAtomMapNum());
					}
					
					if(bUseRLabels) {
						mAtom.setProp("_MolFileRLabel", String.valueOf(sAtom.getAtomMapNum()));
						mAtom.setProp("dummyLabel", "R" + sAtom.getAtomMapNum());
					}
				}
		    	
				arrAtomsToRemove[mi.getSecond()] = false;
			}
			
			// Remove all atoms that are not part of the substructure match
			for (int i = (int)molWithHs.getNumAtoms() - 1; i >= 0; i--) {
				if (arrAtomsToRemove[i]) {
					molWithHs.removeAtom(i);
				}
			}
			
			molWithHs.updatePropertyCache(false);
			molSubstructure = molWithHs;
		}
		
		return molSubstructure;
	}
	
	@Override
	protected Map<String, Long> createWarningContextOccurrencesMap(BufferedDataTable[] inData,
	      InputDataInfo[][] arrInputDataInfo, BufferedDataTable[] resultData) {
		final Map<String, Long> mapContextOccurrences = super.createWarningContextOccurrencesMap(inData, arrInputDataInfo,
				resultData);

		if (hasAdditionalInputTable(getInputTableSpecs(inData))) {
			mapContextOccurrences.put(ROW_CONTEXT_TABLE_CORES.getId(), inData[1].size());
		}
		else {
			String[] arrCores = m_modelCoreSmarts.getStringValue().trim().split("\n");
			long lCount = 0;
			for (String strSmarts : arrCores) {
				if (!strSmarts.trim().isEmpty()) {
					lCount++;
				}
			}
			mapContextOccurrences.put(ROW_CONTEXT_TABLE_CORES.getId(), lCount);
		}
		
		mapContextOccurrences.put(ROW_CONTEXT_R_GROUP_OUTPUT_TABLE.getId(), resultData[0].size());

		return mapContextOccurrences;
	}

	//
	// Static Public Methods
	//

	/**
	 * Determines, if the condition is fulfilled that we have an additional input table with
	 * cores connected to the node according to the passed in specs.
	 * 
	 * @param inSpecs Port specifications.
	 * 
	 * @return True, if there is an additional input table present in the last index of the specs,
	 * 		and if it has columns.
	 */
	public static boolean hasAdditionalInputTable(final PortObjectSpec[] inSpecs) {
		return (inSpecs != null && inSpecs.length >= 2 &&
				inSpecs[1] instanceof DataTableSpec &&
				((DataTableSpec)inSpecs[1]).getNumColumns() > 0);
	}
}
