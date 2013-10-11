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

import org.rdkit.knime.nodes.rdkfingerprint.RDKitFingerprintNodeModel.FingerprintType;

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
	 * Returns the similarity bits setting if set or {@link #UNAVAILABLE} if not set.
	 * 
	 * @return the Similarity Bits value or {@link #UNAVAILABLE}.
	 */
	int getSimilarityBits();

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
	 * Sets the similarity bits setting or {@link #UNAVAILABLE}.
	 * 
	 * @param iSimilarityBits the Similarity Bits value or {@link #UNAVAILABLE}.
	 */
	void setSimilarityBits(final int iSimilarityBits);

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
