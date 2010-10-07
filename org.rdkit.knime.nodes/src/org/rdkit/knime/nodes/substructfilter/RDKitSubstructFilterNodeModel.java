/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2010
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
