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
package org.rdkit.knime.nodes.rdkfingerprint;

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
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.NodeLogger;
import org.RDKit.*;
import org.rdkit.knime.types.RDKitMolValue;

/**
 * 
 * @author Greg Landrum
 */
public class RDKitFingerprintNodeModel extends NodeModel {
    
    private final SettingsModelString m_smiles = 
        RDKitFingerprintNodeDialogPane.createSmilesColumnModel();
    
    private final SettingsModelString m_concate = 
        RDKitFingerprintNodeDialogPane.createNewColumnModel();
    
    private final SettingsModelBoolean m_removeSourceCols =
    	RDKitFingerprintNodeDialogPane.createBooleanModel();

    private final SettingsModelString m_fpType = 
        RDKitFingerprintNodeDialogPane.createFPTypeModel();
    private final SettingsModelIntegerBounded m_minPath = 
        RDKitFingerprintNodeDialogPane.createMinPathModel();
    private final SettingsModelIntegerBounded m_maxPath = 
        RDKitFingerprintNodeDialogPane.createMaxPathModel();
    private final SettingsModelIntegerBounded m_numBits = 
        RDKitFingerprintNodeDialogPane.createNumBitsModel();
    private final SettingsModelIntegerBounded m_radius = 
        RDKitFingerprintNodeDialogPane.createRadiusModel();
    private final SettingsModelIntegerBounded m_layerFlags = 
        RDKitFingerprintNodeDialogPane.createLayerFlagsModel();
    
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(RDKitFingerprintNodeModel.class);
    
    /**
     * Create new node model with one data in- and one outport.
     */
    RDKitFingerprintNodeModel() {
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
	   	String first = m_smiles.getStringValue();
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
    		new DataColumnSpecCreator(newName, DenseBitVectorCell.TYPE);
    	result.append(new SingleCellFactory(appendSpec.createSpec()) {
    		@Override
    		public DataCell getCell(final DataRow row) {
    			DataCell firstCell = row.getCell(indices[0]);
    			if (firstCell.isMissing()){
    				return DataType.getMissingCell();
    			}
    			DataType firstType = spec.getColumnSpec(indices[0]).getType();
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
    				return DataType.getMissingCell();
    			} else {
                    // transfer the bitset into a dense bit vector
                    DenseBitVector bitVector =
                        new DenseBitVector(m_numBits.getIntValue());
	                try {
	                    if(m_fpType.getStringValue()=="rdkit"){
		                    ExplicitBitVect fingerprint;
		                    fingerprint = RDKFuncs.RDKFingerprintMol(mol,m_minPath.getIntValue(),
	                    			m_maxPath.getIntValue(),m_numBits.getIntValue());
		                    for (int i=0;i<fingerprint.getNumBits();i++){
		                        if(fingerprint.getBit(i)) bitVector.set(i);
		                    }
		                    fingerprint.delete();
	                    } else if(m_fpType.getStringValue()=="morgan"){
	                    	SparseIntVectu32 mfp=RDKFuncs.MorganFingerprintMol(mol,m_radius.getIntValue());
	                    	UInt_Pair_Vect obs=mfp.getNonzero();
	                    	for(int i=0;i<obs.size();i++){
	                    		bitVector.set(obs.get(i).getFirst()%m_numBits.getIntValue());
	                    	}
		                    mfp.delete();
                        } else if(m_fpType.getStringValue()=="layered"){
		                    ExplicitBitVect fingerprint;
		                    fingerprint = RDKFuncs.LayeredFingerprintMol(mol,m_layerFlags.getIntValue(),
		                    		m_minPath.getIntValue(),
	                    			m_maxPath.getIntValue(),m_numBits.getIntValue());
		                    for (int i=0;i<fingerprint.getNumBits();i++){
		                        if(fingerprint.getBit(i)) bitVector.set(i);
		                    }
		                    fingerprint.delete();
	                    }
	                    if(ownMol) mol.delete();
	                } catch (Exception ex) {
	                    LOGGER.error("Error while creating fingerprint", ex);
	                	return DataType.getMissingCell();
	                }
                    DenseBitVectorCellFactory fact =
                        new DenseBitVectorCellFactory(bitVector);
                    return fact.createDataCell();
    			}
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
        m_smiles.loadSettingsFrom(settings);
        m_concate.loadSettingsFrom(settings);
        m_removeSourceCols.loadSettingsFrom(settings);
        m_fpType.loadSettingsFrom(settings);
        m_minPath.loadSettingsFrom(settings);
        m_maxPath.loadSettingsFrom(settings);
        m_numBits.loadSettingsFrom(settings);
        m_radius.loadSettingsFrom(settings);
        m_layerFlags.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_smiles.saveSettingsTo(settings);
        m_concate.saveSettingsTo(settings);
        m_removeSourceCols.saveSettingsTo(settings);
        m_fpType.saveSettingsTo(settings);
        m_minPath.saveSettingsTo(settings);
        m_maxPath.saveSettingsTo(settings);
        m_numBits.saveSettingsTo(settings);
        m_radius.saveSettingsTo(settings);
        m_layerFlags.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_smiles.validateSettings(settings);
        m_concate.validateSettings(settings);
        m_removeSourceCols.validateSettings(settings);
        m_fpType.validateSettings(settings);
        m_minPath.validateSettings(settings);
        m_maxPath.validateSettings(settings);
        m_numBits.validateSettings(settings);
        m_radius.validateSettings(settings);
        m_layerFlags.validateSettings(settings);
    }
}
