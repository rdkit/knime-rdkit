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
package org.rdkit.knime.nodes.rdkfingerprint;


/**
 * Defines fingerprint settings used to calculate fingerprints. Not all settings
 * are used for all types of fingerprints. If a string setting is unavailable
 * null should be returned. For numeric settings this is not possible, therefore
 * the constant {@link #UNAVAILABLE} has been introduced, which shall be used
 * to express that a setting is not defined. The value of {@link #UNAVAILABLE}
 * may change over time, if the current value is needed in the future for a new
 * type of settings.
 * 
 * @author Manuel Schwarze
 */
public interface FingerprintSettings {

	/** The integer value to be used if a property is not set. (like null) */
	public static final int UNAVAILABLE = -1;

	/**
	 * Returns the Fingerprint type that is part of this object as string.
	 * 
	 * @return Fingerprint type or null, if not set.
	 */
	String getFingerprintType();

	/**
	 * Returns the Fingerprint type that is part of this object as FingerprintType
	 * object known in RDKit.
	 * 
	 * @return Fingerprint type or null, if not set or not available.
	 */
	FingerprintType getRdkitFingerprintType();

	/**
	 * Returns the Torsion path length setting if set or {@link #UNAVAILABLE} if not set.
	 * 
	 * @return the Torsion path length value or {@link #UNAVAILABLE}.
	 */
	int getTorsionPathLength();

	/**
	 * Returns the minimum path setting if set or {@link #UNAVAILABLE} if not set.
	 * 
	 * @return the MinPath value or {@link #UNAVAILABLE}.
	 */
	int getMinPath();

	/**
	 * Returns the maximum path setting if set or {@link #UNAVAILABLE} if not set.
	 * 
	 * @return the MaxPath value or {@link #UNAVAILABLE}.
	 */
	int getMaxPath();

	/**
	 * Returns the AtomPair minimum path setting if set or {@link #UNAVAILABLE} if not set.
	 * 
	 * @return the AtomPair MinPath value or {@link #UNAVAILABLE}.
	 */
	int getAtomPairMinPath();

	/**
	 * Returns the AtomPair maximum path setting if set or {@link #UNAVAILABLE} if not set.
	 * 
	 * @return the AtomPair MaxPath value or {@link #UNAVAILABLE}.
	 */
	int getAtomPairMaxPath();

	/**
	 * Returns the number of bits (fingerprint length) if set or {@link #UNAVAILABLE} if not set.
	 * 
	 * @return the NumBits (length) value or {@link #UNAVAILABLE}.
	 */
	int getNumBits();

	/**
	 * Returns the radius setting if set or {@link #UNAVAILABLE} if not set.
	 * 
	 * @return the Radius value or {@link #UNAVAILABLE}.
	 */
	int getRadius();

	/**
	 * Returns the layer flags setting if set or {@link #UNAVAILABLE} if not set.
	 * 
	 * @return the Layer Flags value or {@link #UNAVAILABLE}.
	 */
	int getLayerFlags();

	/**
	 * Returns whether or not to use chirality or {@link #UNAVAILABLE}.
	 * 
	 * @return the UseChirality value or {@link #UNAVAILABLE}.
	 */
	boolean getUseChirality();	
	
	
	/**
	 * Returns the similarity bits setting if set or {@link #UNAVAILABLE} if not set.
	 * 
	 * @return the Similarity Bits value or {@link #UNAVAILABLE}.
	 */
	int getSimilarityBits();

	/**
	 * Returns true, if the fingerprint shall be calculated as rooted fingerprint.
	 * 
	 * @return True or false.
	 */
	boolean isRooted();

	/**
	 * Returns true, if the fingerprint shall be calculated as bit-based fingerprint.
	 * 
	 * @return True or false.
	 */
	boolean isBitBased();

	/**
	 * Returns true, if the fingerprint shall be calculated as count-based fingerprint.
	 * 
	 * @return True or false.
	 */
	boolean isCountBased();

	/**
	 * Returns the atom list column name, which must be set if the fingerprint shall be
	 * calculated as rooted fingerprint.
	 * 
	 * @return Column name of the atom list or null, if not set.
	 */
	String getAtomListColumnName();

	/**
	 * Returns true, if the atom list for rooted fingerprints shall be treated as
	 * an include list, and false, if it shall be treated as an exclude list.
	 * 
	 * @return True or false. Also false, if undefined.
	 */
	boolean isTreatAtomListAsIncludeList();

	/**
	 * Returns true, if the atom list for rooted fingerprints shall be treated as
	 * an exclude list, and false, if it shall be treated as an include list.
	 * 
	 * @return True or false. Also false, if undefined.
	 */
	boolean isTreatAtomListAsExcludeList();

	/**
	 * Sets the fingerprint type and also the RDKit Fingerprint Type based on it,
	 * if known.
	 * 
	 * @param strType Fingerprint type.
	 */
	void setFingerprintType(final String strType);

	/**
	 * Sets the RDKit Fingerprint type and also the normal string type based on it.
	 * 
	 * @param type
	 */
	void setRDKitFingerprintType(final FingerprintType type);

	/**
	 * Sets the Torsion path length setting or {@link #UNAVAILABLE}.
	 * 
	 * @param iPathLength the TorsionPathLength value or {@link #UNAVAILABLE}.
	 */
	void setTorsionPathLength(final int iTorsionPathLength);

	/**
	 * Sets the minimum path setting or {@link #UNAVAILABLE}.
	 * 
	 * @param iMinPath the MinPath value or {@link #UNAVAILABLE}.
	 */
	void setMinPath(final int iMinPath);

	/**
	 * Sets the maximum path setting or {@link #UNAVAILABLE}.
	 * 
	 * @param iMaxPath the MaxPath value or {@link #UNAVAILABLE}.
	 */
	void setMaxPath(final int iMaxPath);

	/**
	 * Sets the Atom Pairs minimum path setting or {@link #UNAVAILABLE}.
	 * 
	 * @param iMinPath the MinPath value or {@link #UNAVAILABLE}.
	 */
	void setAtomPairMinPath(final int iMinPath);

	/**
	 * Sets the Atom Pairs maximum path setting or {@link #UNAVAILABLE}.
	 * 
	 * @param iMaxPath the MaxPath value or {@link #UNAVAILABLE}.
	 */
	void setAtomPairMaxPath(final int iMaxPath);

	/**
	 * Sets the number of bits (fingerprint length) or {@link #UNAVAILABLE}.
	 * 
	 * @param iNumBits the NumBits (length) value or {@link #UNAVAILABLE}.
	 */
	void setNumBits(final int iNumBits);

	/**
	 * Sets the radius setting or {@link #UNAVAILABLE}.
	 * 
	 * @param iRadius the Radius value or {@link #UNAVAILABLE}.
	 */
	void setRadius(final int iRadius);

	/**
	 * Sets the layer flags setting or {@link #UNAVAILABLE}.
	 * 
	 * @param iLayerFlags the Layer Flags value or {@link #UNAVAILABLE}.
	 */
	void setLayerFlags(final int iLayerFlags);

	/**
	 * Sets whether or not to use chirality or {@link #UNAVAILABLE}.
	 * 
	 * @param bUseChirality the UseChirality value or {@link #UNAVAILABLE}.
	 */
	void setUseChirality(final boolean bUseChirality);	
	
	/**
	 * Sets the similarity bits setting or {@link #UNAVAILABLE}.
	 * 
	 * @param iSimilarityBits the Similarity Bits value or {@link #UNAVAILABLE}.
	 */
	void setSimilarityBits(final int iSimilarityBits);

	/**
	 * Sets the option to calculate the fingerprint as rooted fingerprint.
	 * 
	 * @param bRooted True to be rooted, false otherwise.
	 */
	void setRooted(boolean bRooted);

	/**
	 * Sets the option to calculate the fingerprint as bit-based fingerprint.
	 * 
	 * @param bBitBased True to be bit-based, false otherwise.
	 */
	void setBitBased(boolean bBitBased);

	/**
	 * Sets the option to calculate the fingerprint as count-based fingerprint.
	 * 
	 * @param bCountBased True to be count-based, false otherwise.
	 */
	void setCountBased(boolean bCountBased);

	/**
	 * Sets the atom list column name, which must be set if the fingerprint shall be
	 * calculated as rooted fingerprint.
	 * 
	 * @param strColumnName Column name of the atom list or null to unset.
	 */
	void setAtomListColumnName(String strColumnName);

	/**
	 * Set to true, if the atom list for rooted fingerprints shall be treated as
	 * an include list, and false, if it shall be treated as an exclude list.
	 * 
	 * @param bIncludeList True or false. Set to false, if undefined.
	 */
	void setTreatAtomListAsIncludeList(boolean bIncludeList);

	/**
	 * Set to true, if the atom list for rooted fingerprints shall be treated as
	 * an exclude list, and false, if it shall be treated as an include list.
	 * 
	 * @param bExcludeList True or false. Set to false, if undefined.
	 */
	void setTreatAtomListAsExcludeList(boolean bExcludeList);

	/**
	 * Returns true, if the specified number is a value that is not equal
	 * to the value the represents an unavailable value.
	 * 
	 * @param iNumber A number to check.
	 * 
	 * @return True, if this value does represent a valid number. False,
	 * 		if it represents the reserved UNAVAILABLE value.
	 */
	boolean isAvailable(final int iNumber);

}
