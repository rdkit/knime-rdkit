/* 
 * This source code, its documentation and all related files
 * are protected by copyright law. All rights reserved.
 *
 * (C)Copyright 2011 by Novartis Pharma AG 
 * Novartis Campus, CH-4002 Basel, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 */
package org.rdkit.knime.util;

import org.RDKit.ChemicalReaction;
import org.RDKit.ROMol;
import org.RDKit.RWMol;
import org.knime.core.data.DataRow;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.rdkit.knime.util.InputDataInfo.EmptyCellException;

/**
 * This class provides chemistry related utility functions.
 * 
 * @author Manuel Schwarze
 */
public final class ChemUtils {

	//
	// Constants
	//
	
	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(ChemUtils.class);
 
	//
	// Static Public Methods
	//
	
	/**
	 * Determines for the passed in string, if it is a valid Smiles string.
	 * 
	 * @param value Potential smiles string.
	 * 
	 * @return True, if the passed in string is Smiles compatible. False otherwise.
	 */
	public static boolean isSmiles(final String value) {
		boolean bRet = false;
		
		if (value != null) {
            try {
            	final ROMol mol = RWMol.MolFromSmiles(value);
            	mol.delete();
            	bRet = true;
            } 
            catch (Exception e) { 
            	// Ignored by purpose
            }			
		}
		
		return bRet;
	}
	
	/**
	 * Determines for the passed in string, if it is a valid CTab string.
	 * 
	 * @param value Potential CTab string.
	 * 
	 * @return True, if the passed in string is CTab compatible. False otherwise.
	 */
	public static boolean isCtab(final String value) {
		boolean bRet = false;

		if (value != null) {
            try {
            	final ROMol mol = RWMol.MolFromMolBlock(value);
            	mol.delete();
            	bRet = true;
            } 
            catch (Exception e) { 
            	// Ignored by purpose
            }		
        }
		
		return bRet;
	}
	
	/**
	 * Creates an RDKit Chemical Reaction from a the SMARTS value provided
	 * by the model {@link #m_modelOptionalReactionSmartsPattern}.
	 * After creation it will be validated.
	 * 
	 * @param strRxnSmarts Reaction smarts. Can be null to fail with exception.
	 * @param iRequiredReactantsTemplates Number of reactants templates that 
     * 		are expected to be found in the chemical reaction.
	 *
	 * @return Chemical reaction object. Never null.
	 * 
     * @throws InvalidSettingsException Thrown, if the reaction could not
     * 		be created from the SMARTS value of the model or if it
     * 		is invalid for another reason.
     */
    public static ChemicalReaction createReactionFromSmarts(String strRxnSmarts, int iRequiredReactantsTemplates)
            throws InvalidSettingsException {
        
    	ChemicalReaction rxn = null;
    	
    	if (strRxnSmarts == null || strRxnSmarts.trim().length() == 0) {
            throw new InvalidSettingsException("No Reaction SMARTS provided.");
        }
    	
    	try {
	    	rxn = ChemicalReaction.ReactionFromSmarts(strRxnSmarts);
    	}
    	catch (Exception exc) {
    		LOGGER.error("Creation of Reaction from SMARTS value failed: " + exc.getMessage(), exc);
    	}
    	
        if (rxn == null) {
            throw new InvalidSettingsException("Invalid Reaction SMARTS: " + strRxnSmarts);
        }
        
        validateReaction(rxn, iRequiredReactantsTemplates);
        
        return rxn;
    }
    
	/**
	 * Reads an RDKit Chemical Reaction from a the Rxn value provided
	 * by the first row of the passed in table.
	 * After creation it will be validated.
	 *
	 * @param table Table with reaction smart.
	 * @param inputDataInfo Data info to access smarts column.
	 * 
     * @param iRequiredReactantTemplates Number of reactants templates that 
     * 		are expected to be found in the chemical reaction.
	 *
	 * @return Chemical reaction object. Never null.
	 * 
     * @throws InvalidSettingsException Thrown, if the reaction could not
     * 		be created from the SMARTS value of the model or if it
     * 		is invalid for another reason.
     */
    public static ChemicalReaction readReactionFromTable(final BufferedDataTable table, 
    		final InputDataInfo inputDataInfo, int iRequiredReactantsTemplates) 
    	throws InvalidSettingsException, EmptyCellException {
        
    	// Pre-checks
    	if (table == null) {
        	throw new IllegalArgumentException("No Reaction table found.");
        }
    	
    	if (inputDataInfo == null) {
        	throw new IllegalArgumentException("Input data info must not be null.");
        }
        
    	if (table.getRowCount() < 1) {
            throw new IllegalArgumentException(
                    "Reaction table does not have any rows.");
        }

    	CloseableRowIterator iterator = table.iterator();
        DataRow row = iterator.next();
        iterator.close();
        
        // Validate the found reaction
        ChemicalReaction rxn = inputDataInfo.getChemicalReaction(row);
        validateReaction(rxn, iRequiredReactantsTemplates);
        
        return rxn;
    }
    
    /**
     * Validates the specified reaction. One of the validations is to check the number
     * of reactants templates, which can be passed in.
     * 
     * @param rxn Reaction to be validated. Must not null to succeed.
     * @param iRequiredReactantTemplates Number of reactants templates that 
     * 		are expected to be found in the chemical reaction.
     * 
     * @throws InvalidSettingsException Thrown, if the passed in reaction
     * 		is null or if validation failed.
     */
    public static void validateReaction(final ChemicalReaction rxn, int iRequiredReactantsTemplates)
            throws InvalidSettingsException {
    	if (rxn == null) {
            throw new InvalidSettingsException("No Reaction provided.");
    	}
    	
        if (rxn.getNumReactantTemplates() != iRequiredReactantsTemplates) {
            throw new InvalidSettingsException(
                    "Reaction should have exactly " + iRequiredReactantsTemplates + " reactant(s), it has  "
                            + rxn.getNumReactantTemplates() + " instead.");
        }

        if (!rxn.validate()) {
            throw new InvalidSettingsException("Reaction SMARTS has errors and cannot be used.");
        }
    }



	//
	// Constructor
	//
	
	/**
	 * This constructor serves only the purpose to avoid instantiation of this class. 
	 */
	private ChemUtils() {
		// To avoid instantiation of this class.
	}
}
