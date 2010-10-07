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
package org.rdkit.knime.nodes.twocomponentreaction;

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
import org.rdkit.knime.nodes.canonsmiles.RDKitCanonicalSmilesNodeDialogPane;
import org.rdkit.knime.types.RDKitMolCell;
import org.rdkit.knime.types.RDKitMolValue;

/**
 * 
 * @author Greg Landrum
 */
public class RDKitTwoComponentReactionNodeModel extends NodeModel {
    
    private final SettingsModelString m_reactant1Col = 
        RDKitTwoComponentReactionNodeDialogPane.createReactant1ColumnModel();
    private final SettingsModelString m_reactant2Col = 
        RDKitTwoComponentReactionNodeDialogPane.createReactant2ColumnModel();
    private final SettingsModelString m_smarts = 
        RDKitTwoComponentReactionNodeDialogPane.createSmartsModel();
    private final SettingsModelBoolean m_doMatrix =
    	RDKitTwoComponentReactionNodeDialogPane.createBooleanModel();
    
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(RDKitTwoComponentReactionNodeModel.class);
    
    /**
     * Create new node model with one data in- and one outport.
     */
    RDKitTwoComponentReactionNodeModel() {
        super(2, 1);
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
       crea = new DataColumnSpecCreator("Reactant 2 sequence index",IntCell.TYPE);
       cSpec.add(crea.createSpec());
       crea = new DataColumnSpecCreator("Reactant 2",RDKitMolCell.TYPE);
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
           throw new InvalidSettingsException("No reaction smarts provided");
       }
	   ChemicalReaction rxn = RDKFuncs.ReactionFromSmarts(m_smarts.getStringValue());
	   if(rxn==null) throw new InvalidSettingsException("unparseable reaction smarts: "+m_smarts.getStringValue());  	
	   if(rxn.getNumReactantTemplates()!=2) throw new InvalidSettingsException("reaction should only have two reactants, it has: "
			+ rxn.getNumReactantTemplates());

       final int[] indices = findColumnIndices(inSpecs);

       return createOutSpecs();
    }
   
   
   private int[] findColumnIndices(final DataTableSpec[] specs)
           throws InvalidSettingsException {
	   	String first = m_reactant1Col.getStringValue();
		if (first == null ){
			throw new InvalidSettingsException("Not configured yet");
		}
		int firstIndex = specs[0].findColumnIndex(first);
		if (firstIndex < 0) {
			throw new InvalidSettingsException(
					"No such column in first input table: " + first);
		}
		DataType firstType = specs[0].getColumnSpec(firstIndex).getType();
		if (!firstType.isCompatible(SmilesValue.class) && !firstType.isCompatible(RDKitMolValue.class)) {
			throw new InvalidSettingsException(
					"Column '" + first + "' does not contain SMILES");
		}
	   	String second = m_reactant2Col.getStringValue();
		if (second == null ){
			throw new InvalidSettingsException("Not configured yet");
		}
		int secondIndex = specs[1].findColumnIndex(second);
		if (secondIndex < 0) {
			throw new InvalidSettingsException(
					"No such column in second input table: " + second);
		}
		DataType secondType = specs[1].getColumnSpec(secondIndex).getType();
		if (!secondType.isCompatible(SmilesValue.class) && !secondType.isCompatible(RDKitMolValue.class)) {
			throw new InvalidSettingsException(
					"Column '" + second + "' does not contain SMILES");
		}
		return new int[]{firstIndex,secondIndex};
   }

   /**
    * {@inheritDoc}
    */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	DataTableSpec inSpec1 = inData[0].getDataTableSpec();
    	DataTableSpec inSpec2 = inData[1].getDataTableSpec();
    	
    	BufferedDataContainer productTable = exec.createDataContainer(createOutSpecs()[0]);
   
    	// check user settings against input spec here 
    	final int[] indices = findColumnIndices(new DataTableSpec[] {inSpec1,inSpec2});

    	ChemicalReaction rxn = RDKFuncs.ReactionFromSmarts(m_smarts.getStringValue());
    	if(rxn==null) throw new InvalidSettingsException("unparseable reaction smarts: "+m_smarts.getStringValue());  	

    	boolean doMatrix=m_doMatrix.getBooleanValue();
        try {
        	int r1Count=0;
            RowIterator it1=inData[0].iterator();
        	int r2Count=0;
            RowIterator it2=inData[1].iterator();
            while (it1.hasNext()) {
                DataRow row1 = it1.next();
                r1Count++;
                DataCell r1Cell = row1.getCell(indices[0]);
                if(!doMatrix){
                	if(!it2.hasNext()) {
                		break;
                	}
                	if(r1Cell.isMissing()){
    					DataRow foorow=it2.next();
                		r2Count++;
                	}
                } else {
                	r2Count=0;
                	it2=inData[1].iterator();
                }

    			if (r1Cell.isMissing()){
    				continue;
    			}

    			boolean ownMol1=false;
    			ROMol mol1=null;
    			if(inSpec1.getColumnSpec(indices[0]).getType().isCompatible(RDKitMolValue.class)){
    				mol1=((RDKitMolValue)r1Cell).getMoleculeValue();
    				ownMol1=false;
    			} else {
    				String smiles=((StringValue)r1Cell).toString();
    				mol1=RDKFuncs.MolFromSmiles(smiles);
    				ownMol1=true;
    			}
    			if(mol1==null){
    				if(!doMatrix){
    					DataRow foorow=it2.next();
                		r2Count++;
    				}
    				continue;
    			}
				ROMol_Vect rs=new ROMol_Vect(2);
				rs.set(0,mol1);
    			while(it2.hasNext()){
                    DataRow row2=it2.next();
                    r2Count++;
                    DataCell r2Cell = row2.getCell(indices[1]);
                    if(!r2Cell.isMissing()){ 
            			boolean ownMol2=false;
            			ROMol mol2=null;
            			if(inSpec2.getColumnSpec(indices[1]).getType().isCompatible(RDKitMolValue.class)){
            				mol2=((RDKitMolValue)r2Cell).getMoleculeValue();
            				ownMol2=false;
            			} else {
            				String smiles=((StringValue)r2Cell).toString();
            				mol2=RDKFuncs.MolFromSmiles(smiles);
            				ownMol2=true;
            			}
                    	if(mol2!=null){
		    				rs.set(1,mol2);
		    				ROMol_Vect_Vect prods=rxn.runReactants(rs);
		    				if(!prods.isEmpty()){
		    					for(int psetidx=0;psetidx<prods.size();psetidx++){
		    						for(int pidx=0;pidx<prods.get(psetidx).size();pidx++){
				    					DataCell[] cells = new DataCell[productTable.getTableSpec().getNumColumns()];
				    					cells[0]=new RDKitMolCell(prods.get(psetidx).get(pidx));
				    					cells[1]=new IntCell(pidx);
				    					cells[2]=new IntCell(r1Count-1);
				    					cells[3]=new RDKitMolCell(rs.get(0));
				    					cells[4]=new IntCell(r2Count-1);
				    					cells[5]=new RDKitMolCell(rs.get(1));
				    					DataRow drow=new  DefaultRow(""+(r1Count-1)+"_"+(r2Count-1)+"_"+psetidx+"_"+pidx,cells);
				    					productTable.addRowToTable(drow);
		    						}
		    					}
		    				}
		    				if(ownMol2) mol2.delete();
		    			}
	    			}
	    			if(!doMatrix) break;
    			}
				if(ownMol1) mol1.delete();
    			
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
        m_reactant1Col.loadSettingsFrom(settings);
        m_reactant2Col.loadSettingsFrom(settings);
        m_smarts.loadSettingsFrom(settings);
        m_doMatrix.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_reactant1Col.saveSettingsTo(settings);
        m_reactant2Col.saveSettingsTo(settings);
        m_smarts.saveSettingsTo(settings);
        m_doMatrix.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_reactant1Col.validateSettings(settings);
        m_reactant2Col.validateSettings(settings);
        m_smarts.validateSettings(settings);
        m_doMatrix.validateSettings(settings);
    }
}
