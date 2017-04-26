/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2015
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
package org.rdkit.knime.nodes.molextractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.RDKit.ROMol_Vect;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;
import org.rdkit.knime.types.RDKitAdapterCell;
import org.rdkit.knime.types.RDKitMolCellFactory;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.types.RDKitTypeConverter;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsModelEnumeration;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class implements the node model of the RDKitMoleculeExtractor node
 * providing calculations based on the open source RDKit library.
 * Splits up fragment molecules contained in a single RDKit molecule cell
 * and extracts these molecules into separate cells.
 * 
 * @author Manuel Schwarze
 */
public class RDKitMoleculeExtractorNodeModel extends AbstractRDKitNodeModel {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitMoleculeExtractorNodeModel.class);

	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;

	/** Input data info index for reference value. */
	protected static final int INPUT_COLUMN_REFERENCE = 1;

	/** The SDF postfix. */
	private static final String SDF_POSTFIX = "\n$$$$\n";

	//
	// Members
	//

	/** Settings model for the column name of the molecules value (e.g. driven by a variable). */
	private final SettingsModelString m_modelInputMolecules =
			registerSettings(RDKitMoleculeExtractorNodeDialog.createInputMoleculesModel());

	/** Settings model for the column name of the molecules format value (e.g. driven by a variable). */
	private final SettingsModelString m_modelInputMoleculesFormat =
			registerSettings(RDKitMoleculeExtractorNodeDialog.createInputMoleculesFormatModel());

	/** Settings model for the column name of the input column. */
	private final SettingsModelString m_modelInputColumnName =
			registerSettings(RDKitMoleculeExtractorNodeDialog.createInputColumnNameModel());

	/** Settings model for the column name of the id column. */
	private final SettingsModelColumnName m_modelReferenceInputColumnName =
			registerSettings(RDKitMoleculeExtractorNodeDialog.createReferenceInputColumnNameModel());

	/** Settings model for the column name of the extracted molecules. */
	private final SettingsModelString m_modelMoleculeOutputColumnName =
			registerSettings(RDKitMoleculeExtractorNodeDialog.createMoleculeOutputColumnNameModel());

	/** Settings model for the column name of the reference id. */
	private final SettingsModelString m_modelReferenceOutputColumnName =
			registerSettings(RDKitMoleculeExtractorNodeDialog.createReferenceOutputColumnNameModel(m_modelReferenceInputColumnName));

	/** Settings model for sanitize fragments option. */
	private final SettingsModelBoolean m_modelSanitizeFragmentsOption =
			registerSettings(RDKitMoleculeExtractorNodeDialog.createSanitizeFragmentsOptionModel(), true); // Was added later

	/** Settings model for error handling option. */
	private final SettingsModelEnumeration<ErrorHandling> m_modelErrorHandlingOption =
			registerSettings(RDKitMoleculeExtractorNodeDialog.createErrorHandlingOptionModel());

	/** Settings model for empty cell handling option. */
	private final SettingsModelEnumeration<EmptyCellHandling> m_modelEmptyCellHandlingOption =
			registerSettings(RDKitMoleculeExtractorNodeDialog.createEmptyCellHandlingOptionModel());

	/** Settings model for empty molecule handling option. */
	private final SettingsModelEnumeration<EmptyMoleculeHandling> m_modelEmptyMoleculeHandlingOption =
			registerSettings(RDKitMoleculeExtractorNodeDialog.createEmptyMoleculeHandlingOptionModel());

	//
	// Constructor
	//

	/**
	 * Create new node model with one optional data in- and one out-port.
	 */
	RDKitMoleculeExtractorNodeModel() {
		super(new PortType[] {
				// Input ports (optional)
				PortTypeRegistry.getInstance().getPortType(BufferedDataTable.TYPE.getPortObjectClass(), true) },
				new PortType[] {
				// Output ports
						PortTypeRegistry.getInstance().getPortType(BufferedDataTable.TYPE.getPortObjectClass(), false) 
				});
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

		// Check input molecule table specific things
		if (hasMoleculesInputTable(inSpecs)) {
			// Auto guess the input column if not set - fails if no compatible column found
			SettingsUtils.autoGuessColumn(inSpecs[0], m_modelInputColumnName, RDKitMolValue.class, 0,
					"Auto guessing: Using column %COLUMN_NAME%.",
					"No RDKit Mol, SMILES or SDF compatible column in input table. Use the \"RDKit from Molecule\" " +
							"node to convert SMARTS.", getWarningConsolidator());

			// Determines, if the input column exists - fails if it does not
			SettingsUtils.checkColumnExistence(inSpecs[0], m_modelInputColumnName, RDKitMolValue.class,
					"Input column has not been specified yet.",
					"Input column %COLUMN_NAME% does not exist. Has the input table changed?");

			if (isProducingReferenceColumn() && !m_modelReferenceInputColumnName.useRowID()) {
				final List<Class<? extends DataValue>> listValueClasses =
						new ArrayList<Class<? extends DataValue>>();
				listValueClasses.add(DataValue.class);
				SettingsUtils.checkColumnExistence(inSpecs[0], m_modelReferenceInputColumnName, listValueClasses,
						"ID column has not been specified yet.",
						"ID column %COLUMN_NAME% does not exist. Has the input table changed?");
			}
		}

		// Check input variable/data specific things
		else  {
			if (detectFormat(m_modelInputMolecules.getStringValue(),
					m_modelInputMoleculesFormat.getStringValue()) == null) {
				throw new IllegalArgumentException("Unsupported molecule format '" +
						m_modelInputMoleculesFormat.getStringValue() + "'. Supported are SMILES, SDF and MOL.");
			}
		}

		// Auto guess the new molecule column name and make it unique
		final String strOutputMolColumnName = m_modelMoleculeOutputColumnName.getStringValue();
		if (strOutputMolColumnName == null || strOutputMolColumnName.isEmpty()) {
			if (hasMoleculesInputTable(inSpecs)) {
				m_modelMoleculeOutputColumnName.setStringValue(m_modelInputColumnName.getStringValue());
			}
			else {
				m_modelMoleculeOutputColumnName.setStringValue("Molecules");
			}
		}

		// Auto guess the new reference column name
		if (hasMoleculesInputTable(inSpecs)) {
			final String strOutputRefColumnName = m_modelReferenceOutputColumnName.getStringValue();
			if (strOutputRefColumnName == null || strOutputRefColumnName.isEmpty()) {
				m_modelReferenceOutputColumnName.setStringValue("Reference");
			}

			if (SettingsUtils.equals(m_modelMoleculeOutputColumnName.getStringValue(), m_modelReferenceOutputColumnName.getStringValue())) {
				throw new InvalidSettingsException("Both output columns cannot have the same name '" +
						m_modelMoleculeOutputColumnName.getStringValue() + "'.");
			}
		}

		// Consolidate all warnings and make them available to the user
		generateWarnings();

		// Generate output specs
		return getOutputTableSpecs(inSpecs);
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

		// Specify input of table 1
		if (inPort == 0 && inSpec != null) {
			final boolean bIncludeReferenceColumn = isProducingReferenceColumn();
			arrDataInfo = new InputDataInfo[bIncludeReferenceColumn ? 2 : 1]; // We have one or two input columns
			arrDataInfo[INPUT_COLUMN_MOL] = new InputDataInfo(inSpec, m_modelInputColumnName,
					InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
					RDKitMolValue.class);
			if (bIncludeReferenceColumn) {
				arrDataInfo[INPUT_COLUMN_REFERENCE] = new InputDataInfo(inSpec, m_modelReferenceInputColumnName,
						InputDataInfo.EmptyCellPolicy.UseDefault, DataType.getMissingCell(),
						DataValue.class);
			}
		}

		return (arrDataInfo == null ? new InputDataInfo[0] : arrDataInfo);
	}


	/**
	 * Returns the output table specification of the specified out port.
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
		List<DataColumnSpec> listSpecs;

		switch (outPort) {

		case 0:
			// Define output table
			listSpecs = new ArrayList<DataColumnSpec>();
			listSpecs.add(new DataColumnSpecCreator(m_modelMoleculeOutputColumnName.getStringValue(),
					RDKitAdapterCell.RAW_TYPE).createSpec());
			if (hasMoleculesInputTable(inSpecs) && isProducingReferenceColumn()) {
				DataType dataType = null;
				if (m_modelReferenceInputColumnName.useRowID()) {
					dataType = StringCell.TYPE;
				}
				else {
					final DataColumnSpec specRef = inSpecs[0].getColumnSpec(m_modelReferenceInputColumnName.getStringValue());
					if (specRef != null) {
						dataType = specRef.getType();
					}
					else {
						throw new InvalidSettingsException("Reference input column not found.");
					}
				}
				listSpecs.add(new DataColumnSpecCreator(m_modelReferenceOutputColumnName.getStringValue(), dataType).createSpec());
			}

			spec = new DataTableSpec("Output 0", listSpecs.toArray(new DataColumnSpec[listSpecs.size()]));
			break;
		}

		return spec;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] processing(final BufferedDataTable[] inData, final InputDataInfo[][] arrInputDataInfo,
			final ExecutionContext exec) throws Exception {
		final DataTableSpec[] arrOutSpecs = getOutputTableSpecs(inData);

		// Get handling options
		final boolean bSanitize = m_modelSanitizeFragmentsOption.getBooleanValue();
		final ErrorHandling errorHandling = m_modelErrorHandlingOption.getValue();
		final EmptyCellHandling emptyCellHandling = m_modelEmptyCellHandlingOption.getValue();
		final EmptyMoleculeHandling emptyMoleculeHandling = m_modelEmptyMoleculeHandlingOption.getValue();

		// Contains the rows with the result column
		final BufferedDataContainer newTableData = exec.createDataContainer(arrOutSpecs[0]);

		// Check, if we process a table
		if (inData != null && inData.length >= 1 && inData[0] != null && inData[0].getDataTableSpec().getNumColumns() > 0) {
			// Get settings and define data specific behavior
			final long lTotalRowCount = inData[0].size();

			// Determine reference cell settings
			final boolean bIncludeReferenceColumn = isProducingReferenceColumn();

			// Iterate through all input rows and calculate results
			long rowInputIndex = 0;
			long rowOutputIndex = 0;
			for (final CloseableRowIterator i = inData[0].iterator(); i.hasNext(); rowInputIndex++) {
				final DataRow row = i.next();

				// Get a unique wave id to mark RDKit Objects for cleanup
				final long lUniqueWaveId = createUniqueCleanupWaveId();

				try {
					// Get the reference cell
					DataCell cellRef = null;
					if (bIncludeReferenceColumn) {
						cellRef = arrInputDataInfo[0][INPUT_COLUMN_REFERENCE].getCell(row);
					}

					// Check for error and empty cell
					boolean bSkip = handleErrorAndEmptyCell(
							arrInputDataInfo[0][INPUT_COLUMN_MOL].getOriginalCell(row),
							errorHandling, emptyCellHandling);

					// Only proceed if we do not skip results
					if (!bSkip) {
						// Check, if the molecule is not empty (mol can be null, if input cell is a missing cell
						final ROMol mol = markForCleanup(arrInputDataInfo[0][INPUT_COLUMN_MOL].getROMol(row), lUniqueWaveId);

						// Do not check for empty molecules if we have a missing input cell or auto-conversion failed so that
						// the converted input cell would also be a missing cell (in that case with an error)
						if (mol != null) {
							bSkip = handleEmptyMolecule(arrInputDataInfo[0][INPUT_COLUMN_MOL].getSmiles(row),
									mol, emptyMoleculeHandling);
						}

						// Only proceed if we do not skip results
						if (!bSkip) {
							// Calculate mol cells
							rowOutputIndex = processMolecule(newTableData, rowOutputIndex, mol, bSanitize,
									lUniqueWaveId, arrInputDataInfo[0][INPUT_COLUMN_MOL].getCell(row), cellRef,
									errorHandling);
						}
					}

					// Every 20 iterations check cancellation status and report progress
					if (rowInputIndex % 20 == 0) {
						AbstractRDKitNodeModel.reportProgress(exec, rowInputIndex, lTotalRowCount, row, " - Calculating");
					}
				}
				finally {
					// Cleanup RDKit Objects
					cleanupMarkedObjects(lUniqueWaveId);
				}
			};
		}

		// Process molecules from variables
		else {
			// Get a unique wave id to mark RDKit Objects for cleanup
			final long lUniqueWaveId = createUniqueCleanupWaveId();

			try {
				// Calculate mol cells
				// Use the auto conversion mechanism to get an RDKit Mol Cell or a Missing Cell
				final DataCell molCell = createRDKitCell(m_modelInputMolecules.getStringValue(),
						m_modelInputMoleculesFormat.getStringValue());

				// Check for empty cell
				boolean bSkip = handleErrorAndEmptyCell(molCell, errorHandling, emptyCellHandling);

				// Only proceed if we do not skip results
				if (!bSkip) {
					// Check, if the molecule is not empty
					final ROMol mol = markForCleanup(molCell.isMissing() ? null : ((RDKitMolValue)molCell).readMoleculeValue(), lUniqueWaveId);
					bSkip = handleEmptyMolecule(molCell.isMissing() ? "" : ((SmilesValue)molCell).getSmilesValue(), mol, emptyMoleculeHandling);

					// Only proceed if we do not skip results
					if (!bSkip) {
						processMolecule(newTableData, 0, mol, bSanitize, lUniqueWaveId, molCell, null, errorHandling);
					}
				}
			}
			finally {
				// Cleanup RDKit Objects
				cleanupMarkedObjects(lUniqueWaveId);
			}
		}

		exec.checkCanceled();
		exec.setProgress(1.0, "Finished Processing");

		newTableData.close();

		return new BufferedDataTable[] { newTableData.getTable() };
	}

	/**
	 * Special error handling for auto converted cells that failed conversion. The behavior
	 * is based on the error handling settings defined in this node.
	 * 
	 * @param inputDataInfo Input data info for the column that failed a cell conversion. Can be null.
	 * @param strError Short error for logging.
	 */
	@Override
	protected void generateAutoConversionError(final InputDataInfo inputDataInfo,
			final String strError) {
		final ErrorHandling errorHandling = m_modelErrorHandlingOption.getValue();
		 String strColumnInfo = (inputDataInfo == null ? "unknown column" : 
	         "column '" + inputDataInfo.getColumnSpec().getName() + "'");
		String strMessage = "Auto conversion in " + strColumnInfo + " failed: " + strError;
		if (errorHandling == ErrorHandling.MissingCellWithWarning || errorHandling == ErrorHandling.SkipWithWarning) {
			if (errorHandling == ErrorHandling.MissingCellWithWarning) {
				strMessage += " - Using empty cell.";
			}
			else if (errorHandling == ErrorHandling.SkipWithWarning) {
				strMessage += " - Skipping cell.";
			}
			getWarningConsolidator().saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),  strMessage);
		}
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
	@Override
	protected Map<String, Long> createWarningContextOccurrencesMap(final BufferedDataTable[] inData,
			final InputDataInfo[][] arrInputDataInfo, final BufferedDataTable[] resultData) {

		final Map<String, Long> mapContextOccurrences = new HashMap<String, Long>();
		if (inData != null && inData.length >= 1 && inData[0] != null) {
			mapContextOccurrences.put(WarningConsolidator.ROW_CONTEXT.getId(), inData[0].size());
		}

		return mapContextOccurrences;
	}

	//
	// Private Methods
	//

	private boolean handleErrorAndEmptyCell(final DataCell cell, final ErrorHandling errorHandling,
			final EmptyCellHandling emptyCellHandling) throws Exception {
		boolean bSkip = false;
		final boolean bEmptyCell = (cell == null || cell.isMissing());
		final boolean bError = (cell != null && cell.isMissing() && ((MissingCell)cell).getError() != null);

		// A converted cell can be empty because an error occurred during conversion or because it was empty before
		// First we check for a recorded conversion error
		if (bError) {
			switch (errorHandling) {
			case Fail:
				throw new Exception("Input molecule could not be processed due to a conversion error. Please check the console for details.");

			case SkipWithWarning:
				bSkip = true;
				// Fall through

			case MissingCellWithWarning:
				// There is nothing to do here as the warning is already logged in method generateAutoConversionError
				break;

			case SkipWithoutWarning:
				bSkip = true;
				break;
			
			case MissingCellWithoutWarning:
			default:
				break;
			}
		}

		// Second we check for an empty cell regardless of an error (only if there was no error)
		else if (bEmptyCell) {
			switch (emptyCellHandling) {
			case Fail:
				throw new Exception("Empty molecule cell encountered.");

			case SkipWithWarning:
				bSkip = true;
				// Fall through

			case MissingCellWithWarning:
				getWarningConsolidator().saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
						"Empty molecule cell encountered.");
				break;

			case SkipWithoutWarning:
				bSkip = true;
				break;
				
			case MissingCellWithoutWarning:
			default:
				break;
			}
		}

		return bSkip;
	}

	private boolean handleEmptyMolecule(final String strSmiles, final ROMol mol,
			final EmptyMoleculeHandling emptyMoleculeHandling) throws Exception {
		boolean bSkip = false;
		final boolean bEmptyMolecule = ((strSmiles != null && strSmiles.isEmpty()) ||
				mol == null || mol.getNumAtoms() == 0);

		if (bEmptyMolecule) {
			switch (emptyMoleculeHandling) {
			case Fail:
				throw new Exception("Input molecule is empty and has no atoms.");

			case SkipWithWarning:
				bSkip = true;
				// Fall through

			case MissingCellWithWarning:
				getWarningConsolidator().saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
						"Input molecule is empty and has no atoms.");
				break;

			case SkipWithoutWarning:
				bSkip = true;
				break;
				
			case MissingCellWithoutWarning:
			default:
				break;
			}
		}

		return bSkip;
	}

	/**
	 * Determines the molecule format from the passed in format string, or
	 * if it is null it tries to determine it from the passed in molecule.
	 * If it is also null and cannot be determined it returns null.
	 * 
	 * @param strMolecule Molecule. Can be null.
	 * @param strFormat Format string. SMILES, SDF or MOL. Can be null
	 * 		to try an auto-detect.
	 * 
	 * @return Detected format.
	 */
	private String detectFormat(final String strMolecule, String strFormat) {
		String strRet = null;

		// Check format
		if (strFormat != null && !strFormat.trim().isEmpty()) {
			strFormat = strFormat.toUpperCase();
			if ("SMILES".equals(strFormat) || "SDF".equals(strFormat) || "MOL".equals(strFormat)) {
				strRet = strFormat;
			}
		}

		// Auto-detect format
		else if (strMolecule != null && !strMolecule.isEmpty()) {
			if (strMolecule.indexOf("\nM  END") > 0) {
				if (strMolecule.indexOf("$$$$") > 0) {
					strRet = "SDF";
				}
				else {
					strRet = "MOL";
				}
			}
			else {
				strRet = "SMILES";
			}
		}
		else {
			strRet = "SMILES";
		}

		return strRet;
	}

	/**
	 * Converts a molecule in string representation into an RDKit molecule using
	 * the auto-conversion mechanism that KNIME RDKit Nodes offers.
	 * 
	 * @param strMolecule Molecule to be converted. Can be null to return null.
	 * @param strFormat Format of the passed in molecule. Supported are SDF, MOL and SMILES.
	 * 		Can be null to auto-detect the format.
	 * 
	 * @return RDKit Molecule Cell.
	 */
	private DataCell createRDKitCell(String strMolecule, String strFormat) {
		DataCell cellRet = DataType.getMissingCell();

		try {
			strFormat = detectFormat(strMolecule, strFormat);

			if ("MOL".equals(strFormat)) {
				if (!strMolecule.endsWith(SDF_POSTFIX)) {
					strMolecule += SDF_POSTFIX;
				}
				cellRet = RDKitTypeConverter.createRDKitAdapterCellFromSdf(strMolecule);
			}
			else if ("SDF".equals(strFormat)) {
				cellRet = RDKitTypeConverter.createRDKitAdapterCellFromSdf(strMolecule);
			}
			else { // SMILES
				cellRet = RDKitTypeConverter.createRDKitAdapterCellFromSmiles(strMolecule);
			}
		}
		catch (final Exception exc) {
			cellRet = new MissingCell(exc.getMessage());
		}

		return cellRet;
	}

	/**
	 * Processes the passed in molecule and produces new rows for the output table, which
	 * are added to the specified buffered data container.
	 * 
	 * @param newTableData Table for adding new rows. Must not be null.
	 * @param rowOutputIndex Next output row index to apply.
	 * @param mol RDKit molecule to be split in fragments.
	 * @param lUniqueWaveId Unique wave ID for RDKit cleanup job.
	 * @param cellOrig Original cell which is used if there is no fragment. Null, if it should not be used.
	 * @param cellRef Reference cell to be added or null, if none should be added.
	 * 
	 * @return Next output row index to apply for subsequent calls.
	 */
	private long processMolecule(final BufferedDataContainer newTableData, long rowOutputIndex,
			final ROMol mol, final boolean bSanitize, final long lUniqueWaveId,
			final DataCell cellOrig, final DataCell cellRef, final ErrorHandling errorHandling) throws Exception {

		// We use only cells, which are not missing (see also createInputDataInfos(...) )
		if (mol != null) {
			try {
				// Determine all fragments (sanitize them, if desired)
				final ROMol_Vect listFragments = markForCleanup(RDKFuncs.getMolFrags(mol, bSanitize), lUniqueWaveId);
				final long lNumber = (listFragments == null ? 0 : listFragments.size());
            
				// If there is one or more than one fragment add all of them
				if (lNumber >= 1) {
					for (int iFrag = 0; iFrag < lNumber; iFrag++) {
						// Create a data row
						final DataRow rowNew = new DefaultRow("row_" + rowOutputIndex,
								createResultCells(RDKitMolCellFactory.createRDKitAdapterCell(listFragments.get(iFrag)), cellRef));
						newTableData.addRowToTable(rowNew);
						rowOutputIndex++;
					}
				}
			}
			catch (final Exception exc) {
				boolean bSkip = false;
				switch (errorHandling) {
				case Fail:
					throw new Exception("Input molecule could not be processed due to a fragmentation error.");

				case SkipWithWarning:
					bSkip = true;
					// Fall through

				case MissingCellWithWarning:
					getWarningConsolidator().saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
							"Input molecule could not be processed due to a fragmentation error.");
					break;

				case SkipWithoutWarning:
					bSkip = true;
					break;
				
				case MissingCellWithoutWarning:
				default:
					break;
				}

				if (!bSkip) {
					// Create a empty data row
					final DataRow rowNew = new DefaultRow("row_" + rowOutputIndex,
							createResultCells(cellOrig, cellRef));
					newTableData.addRowToTable(rowNew);
					rowOutputIndex++;
				}
			}
		}
		else {
			// Create a data row
			final DataRow rowNew = new DefaultRow("row_" + rowOutputIndex,
					createResultCells(cellOrig, cellRef));
			newTableData.addRowToTable(rowNew);
			rowOutputIndex++;
		}

		return rowOutputIndex;
	}

	/**
	 * Returns true, if the node is setup to produce a reference column in the output table.
	 * Unless the model for the reference input role is set to "None" (null and does not use row ID)
	 * this returns true.
	 * 
	 * @return True or false.
	 */
	private boolean isProducingReferenceColumn() {
		return (m_modelReferenceInputColumnName.getColumnName() != null || m_modelReferenceInputColumnName.useRowID());
	}

	/**
	 * Creates an array of results cells.
	 * 
	 * @param molCell Mol cell to be always included.
	 * @param refCell Reference to be included only, if it is not null.
	 * 
	 * @return DataCell array.
	 */
	private DataCell[] createResultCells(DataCell molCell, final DataCell refCell) {
		if (molCell == null) {
			molCell = DataType.getMissingCell();
		}
		if (refCell == null) {
			return new DataCell[] { molCell };
		}
		return new DataCell[] { molCell, refCell };
	}

	//
	// Static Public Methods
	//

	/**
	 * Determines, if the condition is fulfilled that we have a molecules input table with
	 * columns connected to the node according to the passed in specs.
	 * 
	 * @param inSpecs Port specifications.
	 * 
	 * @return True, if there is a molecules table present at the first index of the specs,
	 * 		and if it has columns.
	 */
	public static boolean hasMoleculesInputTable(final PortObjectSpec[] inSpecs) {
		return (inSpecs != null && inSpecs.length >= 1 &&
				inSpecs[0] instanceof DataTableSpec &&
				((DataTableSpec)inSpecs[0]).getNumColumns() > 0);
	}}
