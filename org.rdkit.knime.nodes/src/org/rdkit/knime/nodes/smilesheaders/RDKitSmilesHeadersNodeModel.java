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
package org.rdkit.knime.nodes.smilesheaders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.knime.chem.types.SmilesCell;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;
import org.rdkit.knime.nodes.AdditionalHeaderInfo;
import org.rdkit.knime.nodes.TableViewSupport;
import org.rdkit.knime.util.ChemUtils;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class implements the node model of the RDKitSmilesHeaders node
 * providing functionality to set and change SMILES to be shown in column
 * headers.
 * 
 * @author Manuel Schwarze 
 */
public class RDKitSmilesHeadersNodeModel extends AbstractRDKitNodeModel implements TableViewSupport {

	//
	// Constants
	//
	
	/** The type of an additional SMILES header information. */
	public static final String ADD_HEADER_INFO_SMILES_TYPE = "Smiles";
	
	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitSmilesHeadersNodeModel.class);
	
	/** Input data info index for the column name. */
	protected static final int INPUT_COLUMN_NAME = 0;
	
	/** Input data info index for a SMILES value. */
	protected static final int INPUT_COLUMN_SMILES = 1;
	
	//
	// Members
	//
	
	/** Settings model for the column name of the target column name. */
    private final SettingsModelString m_modelTargetColumnColumnName =
            registerSettings(RDKitSmilesHeadersNodeDialog.createTargetColumnColumnNameModel());
	
	/** Settings model for the column name of the SMILES value to be set. */
    private final SettingsModelString m_modelSmilesValueColumnName =
            registerSettings(RDKitSmilesHeadersNodeDialog.createSmilesValueColumnNameModel());
	
	/** 
	 * Settings model to be used to specify the option to remove all additional 
	 * header information for SMILES for the data table before (optionally) setting 
	 * new SMILES information. 
	 */
    private final SettingsModelBoolean m_modelCompleteResetOption =
            registerSettings(RDKitSmilesHeadersNodeDialog.createCompleteResetOptionModel());
	
	/** 
	 * Settings model to be used to specify the option
     * to use column titles of the Data Table instead of the 
     * SMILES Definition Table.
	 */
    private final SettingsModelBoolean m_modelUseColumnTitlesAsMolecules =
            registerSettings(RDKitSmilesHeadersNodeDialog.createUseColumnTitlesAsMoleculesOptionModel(), true);
    
    //
    // Constructor
    //
    
    /**
     * Create new node model with two data in- and one out-port.
     */
    RDKitSmilesHeadersNodeModel() {
		super(new PortType[] {
				// Input ports (2nd port is optional)
				new PortType(BufferedDataTable.TYPE.getPortObjectClass(), false),
				new PortType(BufferedDataTable.TYPE.getPortObjectClass(), true) }, 
		  new PortType[] {
				// Output ports
				new PortType(BufferedDataTable.TYPE.getPortObjectClass(), false),
			    new PortType(BufferedDataTable.TYPE.getPortObjectClass(), false) });
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
    	
    	if (inSpecs != null && inSpecs.length >= 2 && inSpecs[1] instanceof DataTableSpec) {
	        // Auto guess the input column (target column names) if not set - fails if no compatible column found
	        SettingsUtils.autoGuessColumn(inSpecs[1], m_modelTargetColumnColumnName, StringValue.class, 0, 
	        		"Auto guessing: Using column %COLUMN_NAME% for target column name.", 
	        		"No String compatible column in input table that is usable as target column name.", getWarningConsolidator()); 
	
	        // Determines, if the input column (target column names) exists - fails if it does not
	        SettingsUtils.checkColumnExistence(inSpecs[1], m_modelTargetColumnColumnName, StringValue.class,  
	        		"Input column for target column names has not been specified yet.",
	        		"Input column %COLUMN_NAME% for target column names does not exist. Has the input table changed?");
	    	
	        // Auto guess the input column (SMILES) if not set - fails if no compatible column found
	        SettingsUtils.autoGuessColumn(inSpecs[1], m_modelSmilesValueColumnName, SmilesValue.class, 0, 
	        		"Auto guessing: Using column %COLUMN_NAME% for the SMILES values.", 
	        		"No SMILES compatible column in input table.", getWarningConsolidator()); 
	
	        // Determines, if the input column (SMILES) exists - fails if it does not
	        SettingsUtils.checkColumnExistence(inSpecs[1], m_modelSmilesValueColumnName, SmilesValue.class,  
	        		"Input column with SMILES values has not been specified yet.",
	        		"Input column %COLUMN_NAME% with SMILES values does not exist. Has the input table changed?");
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
    @SuppressWarnings("unchecked")
	protected InputDataInfo[] createInputDataInfos(int inPort, DataTableSpec inSpec)
		throws InvalidSettingsException {
    	
    	InputDataInfo[] arrDataInfo = null;
    	
    	// Specify input of table 1
    	if (inPort == 0) {
    		arrDataInfo = new InputDataInfo[0]; // We do not process any rows, only specs
    	}
    	
    	// Specify input of table 2 (if it is connected only)
    	else if (inPort == 1 && inSpec != null) {
    		arrDataInfo = new InputDataInfo[2]; // We have two input column
    		arrDataInfo[INPUT_COLUMN_NAME] = new InputDataInfo(inSpec, m_modelTargetColumnColumnName, 
    				InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
    				StringValue.class);
    		arrDataInfo[INPUT_COLUMN_SMILES] = new InputDataInfo(inSpec, m_modelSmilesValueColumnName, 
    				InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
    				SmilesValue.class);
    	}
    	
    	return (arrDataInfo == null ? new InputDataInfo[0] : arrDataInfo);
    }

	@Override
	protected BufferedDataTable[] processing(BufferedDataTable[] inData,
			InputDataInfo[][] arrInputDataInfo, ExecutionContext exec)
			throws Exception {
		BufferedDataTable[] arrResultTable = new BufferedDataTable[2];
		WarningConsolidator warnings = getWarningConsolidator();
		DataTableSpec oldTableSpec = inData[0].getDataTableSpec();
		
		boolean bCompleteReset = m_modelCompleteResetOption.getBooleanValue();
		List<DataColumnSpec> listNewColumnSpecs = new ArrayList<DataColumnSpec>();
		
		boolean bHasSmilesDefinitionTable = hasSmilesDefinitionTable(SettingsUtils.getTableSpecs(inData));
		boolean bUseColumnTitles = m_modelUseColumnTitlesAsMolecules.getBooleanValue();
		
		// Create an ordered(!) map with all existing column names and specs
		int iColumnCount = oldTableSpec.getNumColumns();
		LinkedHashMap<String, DataColumnSpec> mapColumns = 
			new LinkedHashMap<String, DataColumnSpec>(iColumnCount);
		for (int i = 0; i < iColumnCount; i++) {
			DataColumnSpec colSpec = oldTableSpec.getColumnSpec(i);
			
			// Complete reset for all SMILES, if option is set
			if (bCompleteReset) {
				AdditionalHeaderInfo oldHeaderInfo = new AdditionalHeaderInfo(colSpec);
				if (oldHeaderInfo.isAvailable() && 
						ADD_HEADER_INFO_SMILES_TYPE.equals(oldHeaderInfo.getType())) {
					colSpec = oldHeaderInfo.removeFromColumnSpec(colSpec);
				}
			}
			
			mapColumns.put(colSpec.getName(), colSpec);
		}
		
		// Manipulate SMILES values based on a SMILES Definition Table
		if (bHasSmilesDefinitionTable) {
			// Traverse the name/SMILES table and set or change existing column specs
			for (DataRow row : inData[1]) {
	
				// Get target column name
				String strTargetColumn = arrInputDataInfo[1][INPUT_COLUMN_NAME].getString(row);
	
				// Ignore rows with an empty target column name (but show a warning)
				if (strTargetColumn == null) {
					warnings.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(), 
							"Target column name is empty.");
					LOGGER.warn("Target column name in row '" + row.getKey().getString() + "' is empty.");
				}
				else {
					// Check, if column exists in data table
					DataColumnSpec oldColSpec = mapColumns.get(strTargetColumn);
					
					// Ignore rows with an non-existing target column name (but show a warning)
					if (oldColSpec == null) {
						warnings.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(), 
							"Target column not found in data table.");
						LOGGER.warn("Target column '" + strTargetColumn + "' not found in data table.");
					}
					else {
						// Get SMILES value
						String strSmiles = arrInputDataInfo[1][INPUT_COLUMN_SMILES].getString(row);
						DataColumnSpec newColSpec = applySmilesProperty(strSmiles, oldColSpec, warnings);

						// Overwrite the old column spec with the new one
						mapColumns.put(strTargetColumn, newColSpec);
					}
				}
			}
		}
		
		// Manipulate SMILE values based on current column titles
		else if (bUseColumnTitles) {
			for (int i = 0; i < iColumnCount; i++) {
				DataColumnSpec oldColSpec = oldTableSpec.getColumnSpec(i);
				String strSmiles = oldColSpec.getName();
								
				// Try, if this is a SMILES value - Use it or forget it
				if (ChemUtils.isSmiles(strSmiles)) {
					DataColumnSpec newColSpec = applySmilesProperty(strSmiles, oldColSpec, warnings);

					// Overwrite the old column spec with the new one
					mapColumns.put(strSmiles, newColSpec);
				}
			}
		}
		
		// Get array of all result column specs
		DataColumnSpec[] arrSpecs = mapColumns.values().toArray(
				new DataColumnSpec[listNewColumnSpecs.size()]);
		
		// Rebuild new data table (spec) based on our changes (maybe there are none)	
		arrResultTable[0] = exec.createSpecReplacerTable(inData[0], new DataTableSpec(arrSpecs));
		
		// Build second table with information about column names and assigned SMILES
		BufferedDataContainer contSmilesHeaders = exec.createDataContainer(getOutputTableSpec(1, null));
		int iRowCount = 0;
		for (int i = 0; i < arrSpecs.length; i++) {
			AdditionalHeaderInfo headerInfo = new AdditionalHeaderInfo(arrSpecs[i]);
			if (headerInfo.isAvailable() && ADD_HEADER_INFO_SMILES_TYPE.equals(headerInfo.getType())) {
				DataRow row = new DefaultRow("Row" + (++iRowCount), 
						new StringCell(arrSpecs[i].getName()),
						new SmilesCell(headerInfo.getValue()));
				contSmilesHeaders.addRowToTable(row);
			}
		}
		contSmilesHeaders.close();
		arrResultTable[1] = contSmilesHeaders.getTable();
		
		return arrResultTable;
	}

	/**
	 * {@inheritDoc}
	 * Returns the spec of the first input table as first output table and
	 * a new specification for the second output table.
	 */
	@Override
	protected DataTableSpec getOutputTableSpec(int outPort,
			DataTableSpec[] inSpecs) throws InvalidSettingsException {
		DataTableSpec spec = null;
		
		switch (outPort) {
			case 0:
				spec = inSpecs[0];
				break;
			case 1:
				spec = new DataTableSpec("Smiles Headers",
					new DataColumnSpecCreator("Column", StringCell.TYPE).createSpec(),
					new DataColumnSpecCreator(ADD_HEADER_INFO_SMILES_TYPE, SmilesCell.TYPE).createSpec());
		}
		
		return spec;
	} 
	
	/**
	 * {@inheritDoc}
	 * This implementation works with the row context on table 2 instead of table 1.
	 */
	protected Map<String, Integer> createWarningContextOccurrencesMap(final BufferedDataTable[] inData,
		final InputDataInfo[][] arrInputDataInfo, final BufferedDataTable[] resultData) {

		Map<String, Integer> mapContextOccurrences = new HashMap<String, Integer>();
		
		// Generate context occurrences only, if we have a second input table connected
		if (inData != null && inData.length >= 2 && inData[1] != null) {
			mapContextOccurrences.put(WarningConsolidator.ROW_CONTEXT.getId(), inData[1].getRowCount());
		}
		
		return mapContextOccurrences;
	}
	
	//
	// Private Methods
	//
	
	/**
	 * Applies the specified SMILES value to the passed in old column specification.
	 * May generate warnings, if the column had already a different SMILES set before.
	 * 
	 * @param strSmiles The new SMILES value to be set. Must not be null.
	 * @param oldColSpec Old column specification to be manipulated. Must not be null.
	 * @param warnings Warning consolidator to take a warning. Must not be null.
	 * 
	 * @return Column specification with the SMILES values as header property.
	 */
	private DataColumnSpec applySmilesProperty(String strSmiles, DataColumnSpec oldColSpec, 
			WarningConsolidator warnings) {
		DataColumnSpec newColSpec = oldColSpec;
		AdditionalHeaderInfo oldHeaderInfo = new AdditionalHeaderInfo(oldColSpec);
		AdditionalHeaderInfo newHeaderInfo = new AdditionalHeaderInfo(
				ADD_HEADER_INFO_SMILES_TYPE, strSmiles, -1);
		boolean bSmilesExistsAlready = oldHeaderInfo.isAvailable() && 
			ADD_HEADER_INFO_SMILES_TYPE.equals(oldHeaderInfo.getType());
			
		// Remove any header SMILES value for empty SMILES
		if (strSmiles == null || strSmiles.trim().isEmpty()) {
			if (bSmilesExistsAlready) {
				newColSpec = newHeaderInfo.removeFromColumnSpec(oldColSpec);
			}
		}
	
		// Otherwise set or replace the SMILES value
		else {
			// Check, if the target column had already a SMILES value and warn
			if (bSmilesExistsAlready && 
					!SettingsUtils.equals(oldHeaderInfo.getValue(),
							newHeaderInfo.getValue())) {
				warnings.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(), 
					"Target column contained already a different SMILES value in the header - overwriting it.");
			}
	
			// Set or replace additional header information
			DataColumnSpecCreator creator = new DataColumnSpecCreator(oldColSpec);
			newHeaderInfo.writeInColumnSpec(creator, oldColSpec.getProperties());
			newColSpec = creator.createSpec();
		}
	
		return newColSpec;
	}
	
	//
	// Static Public Methods
	//
	
	/**
	 * Determines, if the condition is fulfilled that we have a 
	 * SMILES definition table connected to the node according to the 
	 * passed in specs.
	 * 
	 * @param inSpecs Port specifications.
	 * 
	 * @return True, if there is a SMILES definition table present at the last index of the specs.
	 */
	public static boolean hasSmilesDefinitionTable(PortObjectSpec[] inSpecs) {
		return (inSpecs != null && inSpecs.length >= 2 && 
				inSpecs[1] instanceof DataTableSpec);
	}
}
