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

import java.util.ArrayList;
import java.util.List;

import org.RDKit.ChemicalReaction;
import org.RDKit.ROMol;
import org.RDKit.RWMol;
import org.RDKit.UInt_Vect;
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
	 * Reads an RDKit Chemical Reaction from a the Rxn or SMARTS value provided
	 * by the first row of the passed in table.
	 * After creation it will be validated.
	 *
	 * @param table Table with reaction smart.
	 * @param inputDataInfo Data info to access reaction column.
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

		try {
			validateReaction(rxn, iRequiredReactantsTemplates);
		}
		catch (final InvalidSettingsException exc) {
			// Delete the reaction if we cannot use it
			rxn.delete();
			throw exc;
		}

		return rxn;
	}

	/**
	 * Reads all RDKit Chemical Reactions from Rxn or SMARTS values provided
	 * by the passed in table. After creation it will be validated.
	 * If the last parameter is true it will ignore invalid reactions and
	 * just read on, otherwise it would through an exception.
	 *
	 * @param table Table with reaction smart.
	 * @param inputDataInfo Data info to access reaction column.
	 * @param iRequiredReactantTemplates Number of reactants templates that
	 * 		are expected to be found in the chemical reaction.
	 * @param warnings Optional warning consolidator. If set, invalid reactions
	 * 		will cause warnings.
	 * @param warningContext Optional warning context to be used when saving warnings.
	 *
	 * @return Array of chemical reaction objects. Never null and never empty.
	 * 
	 * @throws InvalidSettingsException Thrown, if no reaction could not
	 * 		be created from the RXn or SMARTS values of the table.
	 */
	public static ChemicalReaction[] readReactionsFromTable(final BufferedDataTable table,
			final InputDataInfo inputDataInfo, final int iRequiredReactantsTemplates,
			final WarningConsolidator warnings, final WarningConsolidator.Context warningContext)
					throws InvalidSettingsException, EmptyCellException {

		// Pre-checks
		if (table == null) {
			throw new IllegalArgumentException("No Reaction table found.");
		}

		if (inputDataInfo == null) {
			throw new IllegalArgumentException("Input data info must not be null.");
		}

		final List<ChemicalReaction> listReactions = new ArrayList<ChemicalReaction>(table.getRowCount());
		final CloseableRowIterator iterator = table.iterator();

		while (iterator.hasNext()) {
			final DataRow row = iterator.next();

			ChemicalReaction rxn = null;

			// Parse reaction into an RDKit object
			try {
				rxn = inputDataInfo.getChemicalReaction(row);

				// Validate the found reaction
				if (rxn != null) {
					try {
						validateReaction(rxn, iRequiredReactantsTemplates);
						listReactions.add(rxn);
					}
					catch (final InvalidSettingsException exc) {
						// Delete the reaction if we cannot use it
						rxn.delete();
						if (warnings != null) {
							LOGGER.warn("Invalid reaction cell encountered in row '" + row.getKey() + "'. Validation failed.");
							warnings.saveWarning(warningContext == null ? null : warningContext.getId(),
									"Invalid reaction cell encountered. Validation failed.");
						}
					}
				}
				else {
					if (warnings != null) {
						LOGGER.warn("Empty reaction cell encountered in row '" + row.getKey() + "'.");
						warnings.saveWarning(warningContext == null ? null : warningContext.getId(),
								"Empty reaction cell encountered.");
					}
				}
			}
			catch (final IllegalArgumentException exc) {
				if (warnings != null) {
					LOGGER.warn("Invalid reaction cell encountered in row '" + row.getKey() + "'. Parsing failed.");
					warnings.saveWarning(warningContext == null ? null : warningContext.getId(),
							"Invalid reaction cell encountered. Parsing failed.");
				}
			}
		}

		iterator.close();

		if (listReactions.isEmpty()) {
			throw new InvalidSettingsException(
					"Reaction table does not have any rows with valid reactions.");
		}

		return listReactions.toArray(new ChemicalReaction[listReactions.size()]);
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

	/**
	 * Reverses the specified atom index list for the specified molecule.
	 * 
	 * @param mol Molecule with atoms. Can be null to return null.
	 * @param atomList Atom index list. Can be null to return a list with all atom indexes.
	 * 
	 * @return Atom index list that contains all atom indexes that are not in the specified list.
	 */
	public static UInt_Vect reverseAtomList(final ROMol mol, final UInt_Vect atomList) {
		UInt_Vect reverseList = null;

		if (mol != null) {
			final int iAtomCount = (int)mol.getNumAtoms();

			// Extreme case: No atoms in list => Reverse includes all
			if (atomList == null || atomList.size() == 0) {
				reverseList = new UInt_Vect(iAtomCount);
				for (int i = 0; i < iAtomCount; i++) {
					reverseList.set(i, i);
				}
			}

			// Normal case
			else {
				final int iInputSize = (int)atomList.size();
				int iDistinctInputCount = 0;
				final boolean[] arr = new boolean[iAtomCount];

				for (int i = 0; i < iInputSize; i++) {
					final int index = (int)atomList.get(i);
					if (index > 0 && index < arr.length) {
						if (arr[index] == false) {
							arr[index] = true;
							iDistinctInputCount++;
						}
					}
				}

				reverseList = new UInt_Vect(iAtomCount - iDistinctInputCount);
				int index = 0;
				for (int i = 0; i < iAtomCount; i++) {
					if (!arr[i]) {
						reverseList.set(index++, i);
					}
				}
			}
		}

		return reverseList;
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
