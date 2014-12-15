/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2013
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
package org.rdkit.knime.nodes.rmsdfilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.RDKit.GenericRDKitException;
import org.RDKit.Match_Vect;
import org.RDKit.Match_Vect_Vect;
import org.RDKit.ROMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.InvalidInputException;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class implements the node model of the RDKitRMSDFilter node
 * providing calculations based on the open source RDKit library.
 * Calculates the RMSD value for RDKit molecules and filters them based on a threshold value.
 * 
 * @author Manuel Schwarze
 */
public class RDKitRMSDFilterNodeModel extends AbstractRDKitNodeModel {

	//
	// Inner class
	//

	/**
	 * Simple wrapper class to have an association between a conformer molecule and a row key.
	 * 
	 * @author Manuel Schwarze
	 */
	protected static class IncludedConformer {

		//
		// Members
		//

		private final ROMol m_molConformer;
		private final RowKey m_rowKey;

		//
		// Constructor
		//

		public IncludedConformer(final ROMol molConformer, final RowKey rowKey) {
			if (molConformer == null || rowKey == null) {
				throw new IllegalArgumentException("RDKit Molecule and Row Key must not be null.");
			}

			m_molConformer = molConformer;
			m_rowKey = rowKey;
		}

		//
		// Public Methods
		//

		public ROMol getMolConformer() {
			return m_molConformer;
		}

		public RowKey getRowKey() {
			return m_rowKey;
		}
	}

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitRMSDFilterNodeModel.class);

	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;

	/** Input data info index for ID value. */
	protected static final int INPUT_COLUMN_REFERENCE = 1;

	//
	// Members
	//

	/** Settings model for the column name of the input column. */
	private final SettingsModelString m_modelMoleculeInputColumnName =
			registerSettings(RDKitRMSDFilterNodeDialog.createMoleculeInputColumnNameModel());

	/** Settings model for the column name of the new column to be added to the output table. */
	private final SettingsModelString m_modelReferenceInputColumnName =
			registerSettings(RDKitRMSDFilterNodeDialog.createReferenceInputColumnNameModel());

	/** Settings model for the RMSD threshold to be used for splitting. */
	private final SettingsModelDoubleBounded m_modelRmsdThreshold =
			registerSettings(RDKitRMSDFilterNodeDialog.createRmsdThresholdModel());

	/** Settings model for the option to ignore Hs. */
	private final SettingsModelBoolean m_modelIgnoreHsOption =
			registerSettings(RDKitRMSDFilterNodeDialog.createIgnoreHsOptionModel(), true);

	// Intermediate results

	/** Maps reference values to the total count of occurrences in the input table. */
	private HashMap<String, Integer> m_mapReferenceToTotalCount;

	/** Maps reference values to the count of processed rows with this reference value. */
	private HashMap<String, Integer> m_mapReferenceToProcessedCount;

	/** Maps reference values to ROMol objects that are currently in processing focus and made it already into the output table. */
	private HashMap<String, List<IncludedConformer>> m_mapReferenceToIncludedList;

	/** Maps reference values to a unique wave id used for cleaning up RDKit objects that are not needed anymore. */
	private HashMap<String, Integer> m_mapReferenceToUniqueWaveId;

	//
	// Constructor
	//

	/**
	 * Create new node model with one data in- and one out-port.
	 */
	RDKitRMSDFilterNodeModel() {
		super(1, 2);
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

		// Auto guess the molecule input column if not set - fails if no compatible column found
		SettingsUtils.autoGuessColumn(inSpecs[0], m_modelMoleculeInputColumnName, RDKitMolValue.class, 0,
				"Auto guessing: Using column %COLUMN_NAME% as conformers column.",
				"No RDKit Mol, SMILES or SDF compatible column in input table. Use the \"RDKit from Molecule\" " +
						"node to convert SMARTS.", getWarningConsolidator());

		// Determines, if the molecule input column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[0], m_modelMoleculeInputColumnName, RDKitMolValue.class,
				"Molecule input column for conformers has not been specified yet.",
				"Molecule input column %COLUMN_NAME% does not exist. Has the input table changed?");

		// Auto guess the reference input column if not set - does not fail
		final String strReferenceInputColumnName = m_modelReferenceInputColumnName.getStringValue();
		if (strReferenceInputColumnName == null || strReferenceInputColumnName.isEmpty()) {
			final String[] arrColumnNames = inSpecs[0].getColumnNames();
			for (int i = 0; i < arrColumnNames.length; i++) {
				if (arrColumnNames[i] != null && arrColumnNames[i].toUpperCase().indexOf("REFERENCE") >= 0) {
					m_modelReferenceInputColumnName.setStringValue(arrColumnNames[i]);
					break;
				}
			}
		}

		// Determines, if the reference input column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[0], m_modelReferenceInputColumnName, DataValue.class,
				"Reference input column has not been specified yet.",
				"Reference input column %COLUMN_NAME% does not exist. Has the input table changed?");

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
		if (inPort == 0) {
			arrDataInfo = new InputDataInfo[2];
			arrDataInfo[INPUT_COLUMN_MOL] = new InputDataInfo(inSpec, null, m_modelMoleculeInputColumnName, "molecule",
					InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
					RDKitMolValue.class);
			arrDataInfo[INPUT_COLUMN_REFERENCE] = new InputDataInfo(inSpec, null, m_modelReferenceInputColumnName, "reference data",
					InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
					StringValue.class, DoubleValue.class);
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

		switch (outPort) {
		case 0:
		case 1:
			// Use the same structure as input table for both output tables
			spec = inSpecs[0];
			break;
		}

		return spec;
	}

	/**
	 * This method returns always null in this implementation.
	 * 
	 * @param arrInputDataInfos Array of input data information that is relevant
	 * 		for processing.
	 * 
	 * @return Always null.
	 * 
	 * @see #createInputDataInfos(int, DataTableSpec)
	 */
	protected AbstractRDKitCellFactory createOutputFactory(final InputDataInfo[] arrInputDataInfos)
			throws InvalidSettingsException {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void preProcessing(final BufferedDataTable[] inData,
			final InputDataInfo[][] arrInputDataInfo, final ExecutionContext exec)
					throws Exception {
		// Initialize intermediate results to determine what work we need to do when processing
		m_mapReferenceToTotalCount = new LinkedHashMap<String, Integer>();
		m_mapReferenceToProcessedCount = new LinkedHashMap<String, Integer>();
		m_mapReferenceToIncludedList = new LinkedHashMap<String, List<IncludedConformer>>();
		m_mapReferenceToUniqueWaveId = new LinkedHashMap<String, Integer>();

		// Walk through the input table and find out how many different groups are defined and how many rows belong to the group
		final int iTotalRowCount = inData[0].getRowCount();
		int rowInputIndex = 0;

		AbstractRDKitNodeModel.reportProgress(exec, 0, iTotalRowCount, null, " - Evaluating input");

		for (final CloseableRowIterator i = inData[0].iterator(); i.hasNext(); rowInputIndex++) {
			final DataRow row = i.next();

			final DataCell molCell = arrInputDataInfo[0][INPUT_COLUMN_MOL].getCell(row);
			final DataCell refCell = arrInputDataInfo[0][INPUT_COLUMN_REFERENCE].getCell(row);

			if (refCell != null && molCell != null) {
				final String strRef = getStringValue(refCell); // May throw an exception that leads to an execution error

				if (setOrIncreaseCount(m_mapReferenceToTotalCount, strRef) == 1) {
					m_mapReferenceToProcessedCount.put(strRef, 0);
					m_mapReferenceToIncludedList.put(strRef, new ArrayList<IncludedConformer>(10));
					m_mapReferenceToUniqueWaveId.put(strRef, createUniqueCleanupWaveId());
				}
			}

			// Every 20 iterations check cancellation status and report progress
			if (rowInputIndex % 20 == 0) {
				AbstractRDKitNodeModel.reportProgress(exec, rowInputIndex, iTotalRowCount, row, " - Evaluating input and detecting groups of conformer molecules");
			}
		}
	}

	@Override
	protected double getPreProcessingPercentage() {
		return 0.1d;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] processing(final BufferedDataTable[] inData, final InputDataInfo[][] arrInputDataInfo,
			final ExecutionContext exec) throws Exception {
		final WarningConsolidator warnings = getWarningConsolidator();
		final DataTableSpec[] arrOutSpecs = getOutputTableSpecs(inData);

		// Contains the rows that have an RMSD >= threshold value
		final BufferedDataContainer port0 = exec.createDataContainer(arrOutSpecs[0]);

		// Contains the rows that have an RMSD < threshold value
		final BufferedDataContainer port1 = exec.createDataContainer(arrOutSpecs[1]);

		// Get settings and define data specific behavior
		final int iTotalRowCount = inData[0].getRowCount();

		// Get threshold to be used for splitting
		final double dThreshold = m_modelRmsdThreshold.getDoubleValue();

		// Get option for removing Hs on the fly
		final boolean bIgnoreHs = m_modelIgnoreHsOption.getBooleanValue();

		// Iterate through all input rows and calculate results
		int rowInputIndex = 0;
		for (final CloseableRowIterator i = inData[0].iterator(); i.hasNext(); rowInputIndex++) {
			final DataRow row = i.next();

			// Get reference cell, which defined the group the conformer molecule belongs to
			final DataCell refCell = arrInputDataInfo[0][INPUT_COLUMN_REFERENCE].getCell(row);
			final DataCell molCellForNullCheck = arrInputDataInfo[0][INPUT_COLUMN_MOL].getCell(row);

			// We use only cells, which are not missing (see also createInputDataInfos(...) )
			if (refCell != null) {
				if (molCellForNullCheck != null) {
					// Create the string representation of the reference cell
					final String strRef = getStringValue(refCell);

					// Get the unique wave id that was assigned to the group for RDKit object cleanup
					final int iUniqueWaveId = m_mapReferenceToUniqueWaveId.get(strRef);

					// Get the conformers molecule
					ROMol molProbe = markForCleanup(arrInputDataInfo[0][INPUT_COLUMN_MOL].getROMol(row), iUniqueWaveId);

					// We use only cells that could be resolved into an RDKit molecule (normal case)
					if (molProbe != null) {
						// Remove Hs, if set as option
						if (bIgnoreHs) {
							molProbe = markForCleanup(molProbe.removeHs(false), iUniqueWaveId);
						}

						final int iProcessedCount = setOrIncreaseCount(m_mapReferenceToProcessedCount, strRef);

						// Check, if this is the first one of a group - if so we just add it to the output table
						if (iProcessedCount == 1) {
							port0.addRowToTable(row);
							m_mapReferenceToIncludedList.get(strRef).add(new IncludedConformer(molProbe, row.getKey()));
						}

						// If it is not the first one apply logic to calculate minimum RMSD values based on
						// all molecules of the group that are already in the include list (>= threshold)
						else if (iProcessedCount > 1) {
							double dMinRmsd = Double.MAX_VALUE;

							for (final IncludedConformer includedConformer : m_mapReferenceToIncludedList.get(strRef)) {
								try {
									final double dRmsd = getBestRMSD(
											includedConformer.getMolConformer(), includedConformer.getRowKey(),
											molProbe, row.getKey());
									if (dRmsd != -1) {
										dMinRmsd = Math.min(dRmsd, dMinRmsd);
									}
								}
								catch (final InvalidInputException exc) {
									warnings.saveWarning(exc.getMessage());
								}
							}

							// Check, if we got an RMSD value at all and if it is larger than our threshold
							if (dMinRmsd < Double.MAX_VALUE && dMinRmsd >= dThreshold) {
								port0.addRowToTable(row);
								m_mapReferenceToIncludedList.get(strRef).add(new IncludedConformer(molProbe, row.getKey()));
							}
							else {
								port1.addRowToTable(row);
							}
						}

						// Check, if we processed all molecules of a group - if so we can cleanup all RDKit objects
						if (SettingsUtils.equals(m_mapReferenceToTotalCount.get(strRef), m_mapReferenceToProcessedCount.get(strRef))) {
							m_mapReferenceToProcessedCount.remove(strRef);
							m_mapReferenceToTotalCount.remove(strRef);
							m_mapReferenceToIncludedList.remove(strRef);
							m_mapReferenceToUniqueWaveId.remove(strRef);
							cleanupMarkedObjects(iUniqueWaveId);
						}
					}
					else {
						warnings.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(), "Unable to get RDKit molecule from conformer input cell. This row will appear in the second table.");
						port1.addRowToTable(row);
					}
				}
				else {
					warnings.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(), "Encountered empty conformer input cell. This row will appear in the second table.");
					port1.addRowToTable(row);
				}
			}
			else {
				warnings.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(), "Encountered empty reference input cell. This row will appear in the second table.");
				port1.addRowToTable(row);
			}

			// Every 20 iterations check cancellation status and report progress
			if (rowInputIndex % 20 == 0) {
				AbstractRDKitNodeModel.reportProgress(exec, rowInputIndex, iTotalRowCount, row, " - Calculating best RMSD values and filtering (found already " +
						port0.size() + " matching)");
			}
		}

		exec.checkCanceled();
		exec.setProgress(1.0, "Finished Processing");

		port0.close();
		port1.close();

		return new BufferedDataTable[] { port0.getTable(), port1.getTable() };
	}

	@Override
	protected void cleanupIntermediateResults() {
		m_mapReferenceToTotalCount.clear();
		m_mapReferenceToProcessedCount.clear();
		m_mapReferenceToIncludedList.clear();
		m_mapReferenceToUniqueWaveId.clear();

		m_mapReferenceToTotalCount = null;
		m_mapReferenceToProcessedCount = null;
		m_mapReferenceToIncludedList = null;
		m_mapReferenceToUniqueWaveId = null;

	}

	/**
	 * Returns a string representation for the passed in data value. This
	 * needs to be compatible with StringValue or DoubleValue.
	 * 
	 * @param cell The data cell to be converted into a string. Can be null to return null.
	 * 
	 * @return String representation of the data cell value.
	 * 
	 * @throws InvalidInputException Thrown if the passed in value is neither of type string nor a double compatible number.
	 */
	protected String getStringValue(final DataCell cell) throws InvalidInputException {
		String strValue = null;

		if (cell != null) {
			if (cell.getType().isCompatible(DoubleValue.class)) {
				strValue = "" + (((DoubleValue) cell).getDoubleValue());
			}
			else if (cell.getType().isCompatible(StringValue.class)) {
				strValue = ((StringValue)cell).toString();
			}
			else {
				throw new InvalidInputException("Reference value is neither of type string nor a double compatible number.");
			}
		}

		return strValue;
	}

	/**
	 * Tries to find a count in the passed map under the passed in reference string.
	 * If found, it will increase the count and put the value back into the map. If
	 * not found, it will put the value 1 into the map.
	 * 
	 * @param mapReferenceToCount Map with reference to count mapping. Can be null.
	 * @param strRef Reference to use. Can be null.
	 * 
	 * @return New value or 0 if one of the input parameters was null.
	 */
	protected int setOrIncreaseCount(final Map<String, Integer> mapReferenceToCount, final String strRef) {
		int iRet = 0;

		if (mapReferenceToCount != null && strRef != null) {
			final Integer count = mapReferenceToCount.get(strRef);
			if (count == null) {
				iRet = 1;
			}
			else {
				iRet = count.intValue() + 1;
			}
			mapReferenceToCount.put(strRef, new Integer(iRet));
		}

		return iRet;
	}

	/**
	 * Calculates the best RMSD value for two molecules with a single conformer.
	 * The internally created RDKit objects are cleaned up in this method directly
	 * (not so the passed in molecule where the caller is responsible for the cleanup).
	 * 
	 * @param molRef Reference molecule. Can be null to return -1.
	 * @param keyRef Row key of the reference molecule. Used in error information. Can be null.
	 * @param molProbe Probe molecule. Can be null to return -1.
	 * @param keyProbe Row key of the probe molecule. Used in error information. Can be null.
	 * 
	 * @return Best RMSD value or -1, if it could not be determined.
	 * 
	 * @throws InvalidInputException Thrown if there is no substructure match found between reference and probe molecules.
	 * @throws GenericRDKitException Thrown if an internal RDKit error occurred.
	 */
	protected double getBestRMSD(final ROMol molRef, final RowKey keyRef,
			final ROMol molProbe, final RowKey keyProbe) throws InvalidInputException, GenericRDKitException {
		double dBestRmsd = -1.0d; // Undefined

		if (molRef != null && molProbe != null) {
			Match_Vect_Vect matches = null;

			try {
				matches = molRef.getSubstructMatches(molProbe, false /* unify */);
				if (matches == null || matches.isEmpty()) {
					LOGGER.warn("No matches found between conformer molecules of row '" + keyRef + "' and '" + keyProbe + "'");
					throw new InvalidInputException("No matches found between conformer molecules. " +
							"Please check if the reference column is set correctly. See console for details.");
				}

				dBestRmsd = Double.MAX_VALUE;

				// Find best RMDS value
				for (int i = 0; i < matches.size(); i++) {
					final Match_Vect atomMap = matches.get(i);
					final double dRmds = molProbe.alignMol(molRef, -1, -1, atomMap);
					if (dRmds < dBestRmsd) {
						dBestRmsd = dRmds;
					}
				}
			}
			finally {
				// Cleanup directly
				if (matches != null) {
					matches.delete();
				}
			}
		}

		return (dBestRmsd < Double.MAX_VALUE ? dBestRmsd : -1.0d);
	}
}
