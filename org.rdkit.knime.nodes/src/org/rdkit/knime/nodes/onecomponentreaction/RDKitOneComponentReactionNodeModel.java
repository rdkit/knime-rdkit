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
package org.rdkit.knime.nodes.onecomponentreaction;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.knime.base.node.io.filereader.ColProperty;
import org.knime.base.node.preproc.filter.row.RowFilterIterator;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
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
import org.rdkit.knime.types.RDKitMolCell;
import org.rdkit.knime.types.RDKitMolValue;

/**
 * 
 * @author Greg Landrum
 */
public class RDKitOneComponentReactionNodeModel extends NodeModel {
    
    private final SettingsModelString m_first = 
        RDKitOneComponentReactionNodeDialogPane.createFirstColumnModel();
    
    private final SettingsModelString m_smarts = 
        RDKitOneComponentReactionNodeDialogPane.createSmartsModel();
    
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(RDKitOneComponentReactionNodeModel.class);
    
    /**
     * Create new node model with one data in- and one outport.
     */
    RDKitOneComponentReactionNodeModel() {
        super(1, 1);
    }

    private DataTableSpec[] createOutSpecs(){
	   Vector<DataColumnSpec> cSpec = new Vector<DataColumnSpec>();
       DataColumnSpecCreator crea =
    	   new DataColumnSpecCreator("Product",RDKitMolCell.TYPE);
       cSpec.add(crea.createSpec());
       crea = new DataColumnSpecCreator("Product Index",IntCell.TYPE);
       cSpec.add(crea.createSpec());
       crea = new DataColumnSpecCreator("Reactant 1 sequence index",IntCell.TYPE);
       cSpec.add(crea.createSpec());
       crea = new DataColumnSpecCreator("Reactant 1",RDKitMolCell.TYPE);
       cSpec.add(crea.createSpec());
       DataTableSpec tSpec = new DataTableSpec("output", cSpec.toArray(new DataColumnSpec[cSpec.size()]));
       
       return new DataTableSpec[]{ tSpec };
    }
    /**
     * {@inheritDoc}
     */
   @Override
    protected DataTableSpec[] configure(DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
       if (m_smarts.toString() == "") {
           throw new InvalidSettingsException("No reaction smarts specified");
       }
	   ChemicalReaction rxn;
	   rxn = RDKFuncs.ReactionFromSmarts(m_smarts.getStringValue());
	   if(rxn==null) throw new InvalidSettingsException("unparseable reaction smarts: "+m_smarts.getStringValue());  	
	   if(rxn.getNumReactantTemplates()!=1) throw new InvalidSettingsException("reaction should only have one reactant, it has: "
				+ rxn.getNumReactantTemplates());

       final int[] indices = findColumnIndices(inSpecs[0]);

       return createOutSpecs();
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

    	BufferedDataContainer productTable = exec.createDataContainer(createOutSpecs()[0]);
   
    	// check user settings against input spec here 
    	final int[] indices = findColumnIndices(inSpec);

    	ChemicalReaction rxn = RDKFuncs.ReactionFromSmarts(m_smarts.getStringValue());
    	if(rxn==null) throw new InvalidSettingsException("unparseable reaction smarts: "+m_smarts.getStringValue());  	
        try {
            int count = 0;
            RowIterator it=inData[0].iterator();
            while (it.hasNext()) {
                DataRow row = it.next();
                count++;

    			DataCell firstCell = row.getCell(indices[0]);
    			if (firstCell.isMissing()){
    				continue;
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
	    			if(mol!=null){
	    				ROMol_Vect rs=new ROMol_Vect(1);
	    				rs.set(0,mol);
	    				ROMol_Vect_Vect prods=rxn.runReactants(rs);
	    				if(!prods.isEmpty()){
	    					for(int psetidx=0;psetidx<prods.size();psetidx++){
	    						for(int pidx=0;pidx<prods.get(psetidx).size();pidx++){
			    					DataCell[] cells = new DataCell[productTable.getTableSpec().getNumColumns()];
			    					cells[0]=new RDKitMolCell(prods.get(psetidx).get(pidx));
			    					cells[1]=new IntCell(pidx);
			    					cells[2]=new IntCell(count-1);
			    					cells[3]=new RDKitMolCell(mol);
			    					DataRow drow=new  DefaultRow(""+(count-1)+"_"+psetidx+"_"+pidx,cells);
			    					productTable.addRowToTable(drow);
	    						}
	    					}
	    				}
	    				if(ownMol) mol.delete();
	    			}
    			}
            }
        } finally {
            productTable.close();
        }
    	
    	return new BufferedDataTable[]{productTable.getTable()};
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
