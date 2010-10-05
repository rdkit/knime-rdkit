/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2010
 * Novartis Institutes for BioMedical Research
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder.
 * ---------------------------------------------------------------------
 */
package org.rdkit.knime.nodes.tomolconverter;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.chem.types.SmilesCell;
import org.knime.chem.types.SmilesValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.types.*;
import org.RDKit.*;
/**
 * 
 * @author Greg Landrum
 */
public class RDKitToMolConverterNodeModel extends NodeModel {
    
    private final SettingsModelString m_first = 
        RDKitToMolConverterNodeDialogPane.createFirstColumnModel();
    
    private final SettingsModelString m_concate = 
        RDKitToMolConverterNodeDialogPane.createNewColumnModel();
    
    private final SettingsModelBoolean m_removeSourceCols =
    	RDKitToMolConverterNodeDialogPane.createBooleanModel();
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(RDKitToMolConverterNodeModel.class);
    
    /**
     * Create new node model with one data in- and one outport.
     */
    RDKitToMolConverterNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
   @Override
    protected DataTableSpec[] configure(DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
	   ColumnRearranger rearranger = createColumnRearranger(inSpecs[0]);
	   return new DataTableSpec[]{rearranger.createSpec()};
    }
   
   
   private int[] findColumnIndices(final DataTableSpec spec)
           throws InvalidSettingsException {
	   	String first = m_first.getStringValue();
		if (first == null ){
			throw new InvalidSettingsException("Not configured yet");
		}
		int firstIndex = spec.findColumnIndex(first);
		if (firstIndex < 0) {
			throw new InvalidSettingsException(
					"No such column in input table: " + first);
		}
		DataType firstType = spec.getColumnSpec(firstIndex).getType();
		if (!firstType.isCompatible(SmilesValue.class)) {
			throw new InvalidSettingsException(
					"Column '" + first + "' does not contain SMILES");
		}
		return new int[]{firstIndex};
   }

   /**
    * {@inheritDoc}
    */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	DataTableSpec inSpec = inData[0].getDataTableSpec();
    	ColumnRearranger rearranger = createColumnRearranger(inSpec);
    	BufferedDataTable outTable = exec.createColumnRearrangeTable(
    			inData[0], rearranger, exec);
    	return new BufferedDataTable[]{outTable};
    }
    
    private ColumnRearranger createColumnRearranger(final DataTableSpec spec) 
    	throws InvalidSettingsException {
    	// check user settings against input spec here 
    	final int[] indices = findColumnIndices(spec);
    	String newName = m_concate.getStringValue();
    	ColumnRearranger result = new ColumnRearranger(spec);
    	DataColumnSpecCreator appendSpec = 
    		new DataColumnSpecCreator(newName, RDKitMolCell.TYPE);
    	result.append(new SingleCellFactory(appendSpec.createSpec()) {
    		@Override
    		public DataCell getCell(final DataRow row) {
    			DataCell firstCell = row.getCell(indices[0]);
    			if (firstCell.isMissing()){
    				return DataType.getMissingCell();
    			}
    			String smiles=((StringValue)firstCell).toString();
    			ROMol mol=null;
    			try {
    			  mol=RDKFuncs.MolFromSmiles(smiles);
    			} catch ( Exception e ){
    				e.printStackTrace();
    			}
    			if(mol==null){
    				LOGGER.error("Error parsing SMILES: "+smiles);
                	return DataType.getMissingCell();
    			}
    			return new RDKitMolCell(mol);
    		}
    	});
    	if (m_removeSourceCols.getBooleanValue()) {
    		result.remove(indices);
    	}
    	return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        m_first.loadSettingsFrom(settings);
        m_concate.loadSettingsFrom(settings);
        m_removeSourceCols.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_first.saveSettingsTo(settings);
        m_concate.saveSettingsTo(settings);
        m_removeSourceCols.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_first.validateSettings(settings);
        m_concate.validateSettings(settings);
        m_removeSourceCols.validateSettings(settings);
    }
}
