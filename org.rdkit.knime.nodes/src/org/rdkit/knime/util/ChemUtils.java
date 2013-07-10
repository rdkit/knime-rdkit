/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2012
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
			catch (final Exception e) {
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
			catch (final Exception e) {
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
	public static ChemicalReaction createReactionFromSmarts(final String strRxnSmarts, final int iRequiredReactantsTemplates)
			throws InvalidSettingsException {

		ChemicalReaction rxn = null;

		if (strRxnSmarts == null || strRxnSmarts.trim().length() == 0) {
			throw new InvalidSettingsException("No Reaction SMARTS provided.");
		}

		try {
			rxn = ChemicalReaction.ReactionFromSmarts(strRxnSmarts);
		}
		catch (final Exception exc) {
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
			final InputDataInfo inputDataInfo, final int iRequiredReactantsTemplates)
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

		final CloseableRowIterator iterator = table.iterator();
		final DataRow row = iterator.next();
		iterator.close();

		// Validate the found reaction
		final ChemicalReaction rxn = inputDataInfo.getChemicalReaction(row);
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
	public static void validateReaction(final ChemicalReaction rxn, final int iRequiredReactantsTemplates)
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
