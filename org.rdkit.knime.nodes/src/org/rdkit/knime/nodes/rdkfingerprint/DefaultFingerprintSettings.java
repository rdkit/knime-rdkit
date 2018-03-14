/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2013
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

import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.node.NodeLogger;
import org.rdkit.knime.util.SettingsUtils;

/**
 * This class is the default implementation of the FingerprintSettings interface.
 * 
 * @author Manuel Schwarze
 */
public class DefaultFingerprintSettings extends DataCell implements FingerprintSettings {

	//
	// Constants
	//

	/** The serial number. */
	private static final long serialVersionUID = 8340311731706138678L;

	/** The logging instance. */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(DefaultFingerprintSettings.class);

	//
	// Members
	//

	/** The Fingerprint type. Can be null. */
	private String m_strType;

	/** The RDKit Fingerprint type. Can be null if unknown or undefined. */
	private FingerprintType m_rdkitType;

	/** The Fingerprint Torsion path length. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iTorsionPathLength;

	/** The Fingerprint minimum path. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iMinPath;

	/** The Fingerprint maximum path. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iMaxPath;

	/** The Fingerprint AtomPair minimum path. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iAtomPairMinPath;

	/** The Fingerprint AtomPair maximum path. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iAtomPairMaxPath;

	/** The Fingerprint length. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iNumBits;

	/** The Fingerprint radius. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iRadius;

	/** The Fingerprint layer flags. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iLayerFlags;

	/** The Fingerprint similarity bits. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iSimilarityBits;

	/** Controls whether or not chirality is used in the fingerprint */
	private boolean m_bUseChirality;

	/** The Fingerprint rooted flag to tell that the fingerprint is or shall be a rooted fingerprint. */
	private boolean m_bIsRooted;

	/** The Fingerprint atom list column name for rooted fingerprints. Can be null, if not set. */
	private String m_strAtomListColumnName;

	/** The Fingerprint option to treat an atom list as include list for rooted fingerprints. */
	private boolean m_bTreatAtomListAsIncludeList;

	/** The Fingerprint count-based flag to tell that the fingerprint is or shall be a count-based fingerprint. */
	private boolean m_bIsCountBased;

	//
	// Constructor
	//

	/**
	 * Creates a new bit-based fingerprint settings object with the specified fingerprint type and settings.
	 * 
	 * @param strType Fingerprint type value. Can be null.
	 * @param iTorsionPathLength Torsion min path values. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iMinPath Min Path value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iMaxPath Min Path value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iAtomPairMinPath AtomPair Min Path value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iAtomPairMaxPath AtomPair Max Path value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iNumBits Num Bits (Length) value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iRadius Radius value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iLayerFlags Layer Flags value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iSimilarityBits Similarity bits. Can be -1 ({@link #UNAVAILABLE}.
	 * @param bUseChirality Use Chirality. Can be null ({@link #UNAVAILABLE}.
	 */
	public DefaultFingerprintSettings(final String strType, final int iTorsionPathLength, final int iMinPath,
			final int iMaxPath, final int iAtomPairMinPath, final int iAtomPairMaxPath,
			final int iNumBits, final int iRadius, final int iLayerFlags,
			final int iSimilarityBits, final boolean bUseChirality) {
		this(strType, iTorsionPathLength, iMinPath, iMaxPath, iAtomPairMinPath, iAtomPairMaxPath,
				iNumBits, iRadius, iLayerFlags, iSimilarityBits, bUseChirality, false, null, false);
	}

	/**
	 * Creates a new bit-based fingerprint settings object with the specified fingerprint type and settings.
	 * 
	 * @param strType Fingerprint type value. Can be null.
	 * @param iTorsionPathLength Torsion min path values. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iMinPath Min Path value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iMaxPath Min Path value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iAtomPairMinPath AtomPair Min Path value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iAtomPairMaxPath AtomPair Max Path value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iNumBits Num Bits (Length) value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iRadius Radius value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iLayerFlags Layer Flags value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iSimilarityBits Similarity bits. Can be -1 ({@link #UNAVAILABLE}.
	 * @param bUseChirality Use Chirality. Can be null ({@link #UNAVAILABLE}.
	 * @param bIsRooted Flag to set if a rooted fingerprint is desired.
	 * @param strAtomListColumnName Atom list column name for rooted fingerprints.
	 * @param bTreatAtomListAsIncludeList Flag to tell if atom list atoms shall be included (true) or excluded (false).
	 */
	public DefaultFingerprintSettings(final String strType, final int iTorsionPathLength, final int iMinPath,
			final int iMaxPath, final int iAtomPairMinPath, final int iAtomPairMaxPath,
			final int iNumBits, final int iRadius, final int iLayerFlags,
			final int iSimilarityBits, final boolean bUseChirality, 
			final boolean bIsRooted, final String strAtomListColumnName,
			final boolean bTreatAtomListAsIncludeList) {
		this(strType, iTorsionPathLength, iMinPath, iMaxPath, iAtomPairMinPath, iAtomPairMaxPath,
				iNumBits, iRadius, iLayerFlags, iSimilarityBits, bUseChirality, false, null, false, false);
	}

	/**
	 * Creates a new fingerprint settings object with the specified fingerprint type and settings.
	 * 
	 * @param strType Fingerprint type value. Can be null.
	 * @param iTorsionPathLength Torsion min path values. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iMinPath Min Path value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iMaxPath Min Path value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iAtomPairMinPath AtomPair Min Path value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iAtomPairMaxPath AtomPair Max Path value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iNumBits Num Bits (Length) value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iRadius Radius value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iLayerFlags Layer Flags value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iSimilarityBits Similarity bits. Can be -1 ({@link #UNAVAILABLE}.
	 * @param bUseChirality Use Chirality. Can be null ({@link #UNAVAILABLE}.
	 * @param bIsRooted Flag to set if a rooted fingerprint is desired.
	 * @param strAtomListColumnName Atom list column name for rooted fingerprints.
	 * @param bTreatAtomListAsIncludeList Flag to tell if atom list atoms shall be included (true) or excluded (false).
	 * @param bIsCountBased Flag to tell if the fingerprint is or shall be calculated as count-based fingerprint.
	 */
	public DefaultFingerprintSettings(final String strType, final int iTorsionPathLength, final int iMinPath,
			final int iMaxPath, final int iAtomPairMinPath, final int iAtomPairMaxPath,
			final int iNumBits, final int iRadius, final int iLayerFlags,
			final int iSimilarityBits, final boolean bUseChirality,
			final boolean bIsRooted, final String strAtomListColumnName,
			final boolean bTreatAtomListAsIncludeList, final boolean bIsCountBased) {
		m_strType = strType;
		m_rdkitType = FingerprintType.parseString(m_strType);
		m_iTorsionPathLength = iTorsionPathLength;
		m_iMinPath = iMinPath;
		m_iMaxPath = iMaxPath;
		m_iAtomPairMinPath = iAtomPairMinPath;
		m_iAtomPairMaxPath = iAtomPairMaxPath;
		m_iNumBits = iNumBits;
		m_iRadius = iRadius;
		m_iLayerFlags = iLayerFlags;
		m_iSimilarityBits = iSimilarityBits;
		m_bUseChirality = bUseChirality;
		m_bIsRooted = bIsRooted;
		m_strAtomListColumnName = strAtomListColumnName;
		m_bTreatAtomListAsIncludeList = bTreatAtomListAsIncludeList;
		m_bIsCountBased = bIsCountBased;
	}

	/**
	 * Creates a new fingerprint settings object based on an existing one.
	 * 
	 * @param existing Existing fingerprint settings object or null to start with empty values.
	 */
	public DefaultFingerprintSettings(final FingerprintSettings existing) {
		reset();

		if (existing != null) {
			m_strType = existing.getFingerprintType();
			m_rdkitType = existing.getRdkitFingerprintType();
			m_iTorsionPathLength = existing.getTorsionPathLength();
			m_iMinPath = existing.getMinPath();
			m_iMaxPath = existing.getMaxPath();
			m_iAtomPairMinPath = existing.getAtomPairMinPath();
			m_iAtomPairMaxPath = existing.getAtomPairMaxPath();
			m_iNumBits = existing.getNumBits();
			m_iRadius = existing.getRadius();
			m_iLayerFlags = existing.getLayerFlags();
			m_iSimilarityBits = existing.getSimilarityBits();
			m_bUseChirality = existing.getUseChirality();
			m_bIsRooted = existing.isRooted();
			m_strAtomListColumnName = existing.getAtomListColumnName();
			m_bTreatAtomListAsIncludeList = existing.isTreatAtomListAsIncludeList();
			m_bIsCountBased = existing.isCountBased();
		}
	}

	//
	// Public Methods
	//

	@Override
	public synchronized String getFingerprintType() {
		return m_strType;
	}

	@Override
	public synchronized FingerprintType getRdkitFingerprintType() {
		return m_rdkitType;
	}

	@Override
	public synchronized int getTorsionPathLength() {
		return m_iTorsionPathLength;
	}

	@Override
	public synchronized int getMinPath() {
		return m_iMinPath;
	}

	@Override
	public synchronized int getMaxPath() {
		return m_iMaxPath;
	}

	@Override
	public synchronized int getAtomPairMinPath() {
		return m_iAtomPairMinPath;
	}

	@Override
	public synchronized int getAtomPairMaxPath() {
		return m_iAtomPairMaxPath;
	}

	@Override
	public synchronized int getNumBits() {
		return m_iNumBits;
	}

	@Override
	public synchronized int getRadius() {
		return m_iRadius;
	}

	@Override
	public synchronized int getLayerFlags() {
		return m_iLayerFlags;
	}

	@Override
	public synchronized int getSimilarityBits() {
		return m_iSimilarityBits;
	}

	@Override
	public synchronized boolean getUseChirality() {
		return m_bUseChirality;
	}

	@Override
	public synchronized boolean isRooted() {
		return m_bIsRooted && m_strAtomListColumnName != null;
	}

	@Override
	public synchronized String getAtomListColumnName() {
		return (isRooted() ? m_strAtomListColumnName : null);
	}

	@Override
	public synchronized boolean isTreatAtomListAsIncludeList() {
		return (isRooted() ? m_bTreatAtomListAsIncludeList : false);
	}
	@Override
	public synchronized boolean isTreatAtomListAsExcludeList() {
		return (isRooted() ? !m_bTreatAtomListAsIncludeList : false);
	}

	@Override
	public synchronized boolean isBitBased() {
		return !isCountBased();
	}
	@Override
	public synchronized boolean isCountBased() {
		return m_bIsCountBased;
	}

	@Override
	public synchronized void setFingerprintType(final String strType) {
		m_strType = strType;
		m_rdkitType = FingerprintType.parseString(m_strType);
	}

	@Override
	public synchronized void setRDKitFingerprintType(final FingerprintType type) {
		m_strType = type == null ? null : type.toString();
		m_rdkitType = type;
	}

	@Override
	public synchronized void setTorsionPathLength(final int iTorsionPathLength) {
		this.m_iTorsionPathLength = iTorsionPathLength;
	}

	@Override
	public synchronized void setMinPath(final int iMinPath) {
		this.m_iMinPath = iMinPath;
	}

	@Override
	public synchronized void setMaxPath(final int iMaxPath) {
		this.m_iMaxPath = iMaxPath;
	}

	@Override
	public synchronized void setAtomPairMinPath(final int iAtomPairMinPath) {
		this.m_iAtomPairMinPath = iAtomPairMinPath;
	}

	@Override
	public synchronized void setAtomPairMaxPath(final int iAtomPairMaxPath) {
		this.m_iAtomPairMaxPath = iAtomPairMaxPath;
	}

	@Override
	public synchronized void setNumBits(final int iNumBits) {
		this.m_iNumBits = iNumBits;
	}

	@Override
	public synchronized void setRadius(final int iRadius) {
		this.m_iRadius = iRadius;
	}

	@Override
	public synchronized void setLayerFlags(final int iLayerFlags) {
		this.m_iLayerFlags = iLayerFlags;
	}

	@Override
	public synchronized void setSimilarityBits(final int iSimilarityBits) {
		this.m_iSimilarityBits = iSimilarityBits;
	}

	@Override
	public synchronized void setUseChirality(final boolean bUseChirality) {
		this.m_bUseChirality = bUseChirality;
	}

	@Override
	public void setRooted(final boolean bRooted) {
		this.m_bIsRooted = bRooted;
	}

	@Override
	public void setAtomListColumnName(final String strColumnName) {
		this.m_strAtomListColumnName = strColumnName;
	}

	@Override
	public void setTreatAtomListAsIncludeList(final boolean bIncludeList) {
		this.m_bTreatAtomListAsIncludeList = bIncludeList;
	}

	@Override
	public void setTreatAtomListAsExcludeList(final boolean bExcludeList) {
		this.m_bTreatAtomListAsIncludeList = !bExcludeList;
	}

	@Override
	public void setBitBased(final boolean bBitBased) {
		this.m_bIsCountBased = !bBitBased;
	}

	@Override
	public void setCountBased(final boolean bCountBased) {
		this.m_bIsCountBased = bCountBased;
	}

	@Override
	public synchronized boolean isAvailable(final int iNumber) {
		return iNumber != UNAVAILABLE;
	}

	public synchronized void reset() {
		m_strType = null;
		m_rdkitType = null;
		m_iTorsionPathLength = UNAVAILABLE;
		m_iMinPath = UNAVAILABLE;
		m_iMaxPath = UNAVAILABLE;
		m_iAtomPairMinPath = UNAVAILABLE;
		m_iAtomPairMaxPath = UNAVAILABLE;
		m_iNumBits = UNAVAILABLE;
		m_iRadius = UNAVAILABLE;
		m_iLayerFlags = UNAVAILABLE;
		m_iSimilarityBits = UNAVAILABLE;
		m_bUseChirality = false;
		m_bIsRooted = false;
		m_strAtomListColumnName = null;
		m_bTreatAtomListAsIncludeList = false;
		m_bIsCountBased = false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + m_iLayerFlags;
		result = prime * result + m_iSimilarityBits;
		result = prime * result + m_iTorsionPathLength;
		result = prime * result + m_iMaxPath;
		result = prime * result + m_iMinPath;
		result = prime * result + m_iAtomPairMaxPath;
		result = prime * result + m_iAtomPairMinPath;
		result = prime * result + m_iNumBits;
		result = prime * result + m_iRadius;
		result = prime * result
				+ ((m_strType == null) ? 0 : m_strType.hashCode());
		result = prime * result
				+ ((m_rdkitType == null) ? 0 : m_rdkitType.hashCode());
		result = prime * result + (m_bIsRooted ? 1 : 0);
		result = prime * result
				+ ((m_strAtomListColumnName == null) ? 0 : m_strAtomListColumnName.hashCode());
		result = prime * result + (m_bTreatAtomListAsIncludeList ? 1 : 0);
		result = prime * result + (m_bIsCountBased ? 1 : 0);
		result = prime * result + (m_bUseChirality ? 1 : 0);
		return result;
	}

	@Override
	public synchronized boolean equalsDataCell(final DataCell objSettingsToCompare) {
		boolean bRet = false;

		if (objSettingsToCompare == this) {
			bRet = true;
		}
		else if (objSettingsToCompare instanceof DefaultFingerprintSettings) {
			final DefaultFingerprintSettings specToCompare = (DefaultFingerprintSettings)objSettingsToCompare;

			if (SettingsUtils.equals(this.m_strType, specToCompare.m_strType) &&
					SettingsUtils.equals(this.m_rdkitType, specToCompare.m_rdkitType) &&
					this.m_iTorsionPathLength == specToCompare.m_iTorsionPathLength &&
					this.m_iMinPath == specToCompare.m_iMinPath &&
					this.m_iMaxPath == specToCompare.m_iMaxPath &&
					this.m_iAtomPairMinPath == specToCompare.m_iAtomPairMinPath &&
					this.m_iAtomPairMaxPath == specToCompare.m_iAtomPairMaxPath &&
					this.m_iNumBits == specToCompare.m_iNumBits &&
					this.m_iRadius == specToCompare.m_iRadius &&
					this.m_iLayerFlags == specToCompare.m_iLayerFlags &&
					this.m_iSimilarityBits == specToCompare.m_iSimilarityBits &&
					this.m_bUseChirality == specToCompare.m_bUseChirality &&
					this.m_bIsRooted == specToCompare.m_bIsRooted &&
					SettingsUtils.equals(this.m_strAtomListColumnName, specToCompare.m_strAtomListColumnName) &&
					this.m_bTreatAtomListAsIncludeList == specToCompare.m_bTreatAtomListAsIncludeList &&
					this.m_bIsCountBased == specToCompare.m_bIsCountBased) {
				bRet = true;
			}
		}

		return bRet;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(isRooted() ? "Rooted " : "");

		sb.append(isCountBased() ? "Count-Based " : "Bit-Based ");

		if (getRdkitFingerprintType() != null) {
			sb.append(getRdkitFingerprintType().toString()).append(" Fingerprint");
		}
		else if (getFingerprintType() != null) {
			sb.append(getFingerprintType()).append(" Fingerprint");
		}
		if (isAvailable(getTorsionPathLength())) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append("Torsion Path Length: ").append(getTorsionPathLength());
		}
		if (isAvailable(getMinPath())) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append("Min Path: ").append(getMinPath());
		}
		if (isAvailable(getMaxPath())) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append("Max Path: ").append(getMaxPath());
		}
		if (isAvailable(getAtomPairMinPath())) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append("AtomPair Min Path: ").append(getAtomPairMinPath());
		}
		if (isAvailable(getAtomPairMaxPath())) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append("AtomPair Max Path: ").append(getAtomPairMaxPath());
		}
		if (isAvailable(getNumBits())) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append("Num Bits: ").append(getNumBits());
		}
		if (isAvailable(getRadius())) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append("Radius: ").append(getRadius());
		}
		if (isAvailable(getLayerFlags())) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append("Layered Flags: ").append(getLayerFlags());
		}
		if (isAvailable(getSimilarityBits())) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append("Similarity Bits: ").append(getSimilarityBits());
		}
		if (getUseChirality()) {
			sb.append("Use Chirality: true");
		}
		if (isRooted()) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			final String str = getAtomListColumnName();
			final boolean bIncluded = isTreatAtomListAsIncludeList();
			sb.append(bIncluded ? "Included " : "Excluded ");
			sb.append("Atom List Column: ").append(str == null ? "Undefined" : str);
		}

		return sb.length() == 0 ? null : sb.toString();
	}

	//
	// Protected Methods
	//

	protected int getInt(final Map<String, String> mapProps, final String strKey, final String strColumnName) {
		int iRet = UNAVAILABLE;

		if (mapProps != null && mapProps.containsKey(strKey))
			try {
				iRet = Integer.parseInt(mapProps.get(strKey));
			}
		catch (final Exception exc) {
			LOGGER.warn("Header property '" + strKey + "' in column '" +
					strColumnName + "' is not representing a valid integer value: "
					+ mapProps.get(strKey) + " cannot be parsed.");
		}

		return iRet;
	}

	protected boolean getBoolean(final Map<String, String> mapProps, final String strKey, final String strColumnName) {
		boolean bRet = false;

		if (mapProps != null && mapProps.containsKey(strKey))
			try {
				bRet = Boolean.parseBoolean(mapProps.get(strKey));
			}
		catch (final Exception exc) {
			LOGGER.warn("Header property '" + strKey + "' in column '" +
					strColumnName + "' is not representing a valid boolean value: "
					+ mapProps.get(strKey) + " cannot be parsed.");
		}

		return bRet;
	}
}
