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
package org.rdkit.knime.nodes.substructfilter;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.preproc.filter.row.RowFilterIterator;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.chem.types.SmilesCell;
import org.knime.chem.types.SmilesValue;
import org.knime.core.node.BufferedDataContainer;
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
import org.RDKit.*;
import org.rdkit.knime.types.RDKitMolValue;

/**
 * 
 * @author Greg Landrum
 */
public class RDKitSubstructFilterNodeModel extends NodeModel {
    
    private final SettingsModelString m_first = 
        RDKitSubstructFilterNodeDialogPane.createFirstColumnModel();
    
    private final SettingsModelString m_smarts = 
        RDKitSubstructFilterNodeDialogPane.createSmartsModel();
    
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(RDKitSubstructFilterNodeModel.class);
    
    /**
     * Create new node model with one data in- and one outport.
     */
    RDKitSubstructFilterNodeModel() {
        super(1, 2);
    }

    /**
     * {@inheritDoc}
     */
   @Override
    protected DataTableSpec[] configure(DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
       if (m_smarts.toString() == "") {
           throw new InvalidSettingsException("No row filter specified");
       }
   	   final int[] indices = findColumnIndices(inSpecs[0]);

   	   return new DataTableSpec[]{inSpecs[0], inSpecs[0]};
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
		if (!firstType.isCompatible(SmilesValue.class) && !firstType.isCompatible(RDKitMolValue.class)) {
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

    	BufferedDataContainer matchTable = exec.createDataContainer(inData[0].getDataTableSpec());
    	BufferedDataContainer failTable = exec.createDataContainer(inData[0].getDataTableSpec());
   
    	// check user settings against input spec here 
    	final int[] indices = findColumnIndices(inSpec);

    	ROMol pattern = RDKFuncs.MolFromSmarts(m_smarts.getStringValue());
    	if(pattern==null) throw new InvalidSettingsException("unparseable smarts: "+m_smarts.getStringValue());
        try {
            int count = 0;
            RowIterator it=inData[0].iterator();
            while (it.hasNext()) {
                DataRow row = it.next();
                boolean matched=false;
                count++;

    			DataCell firstCell = row.getCell(indices[0]);
    			if (firstCell.isMissing()){
    				matched = false;
    			} else {
	    			DataType firstType = inSpec.getColumnSpec(indices[0]).getType();
	    			boolean ownMol;
	    			ROMol mol=null;
	    			if(firstType.isCompatible(RDKitMolValue.class)){
	    				mol=((RDKitMolValue)firstCell).getMoleculeValue();
	    				ownMol=false;
	    			} else {
	    				String smiles=((StringValue)firstCell).toString();
	    				mol=RDKFuncs.MolFromSmiles(smiles);
	    				ownMol=true;
	    			}
	    			if(mol==null){
	    				matched = false;
	    			} else {
	    				matched = mol.hasSubstructMatch(pattern);
	    				if(ownMol) mol.delete();
	    			}
    			}
                if(matched){ 
                	matchTable.addRowToTable(row);
                } else {
                	failTable.addRowToTable(row);
                }
            }
        } finally {
            matchTable.close();
            failTable.close();
        }
    	
    	return new BufferedDataTable[]{matchTable.getTable(),failTable.getTable()};
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
        m_smarts.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_first.saveSettingsTo(settings);
        m_smarts.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_first.validateSettings(settings);
        m_smarts.validateSettings(settings);
    }
}
