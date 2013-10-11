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
import org.rdkit.knime.nodes.rdkfingerprint.RDKitFingerprintNodeModel.FingerprintType;
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

	/** The Fingerprint minimum path. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iMinPath;

	/** The Fingerprint maximum path. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iMaxPath;

	/** The Fingerprint length. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iNumBits;

	/** The Fingerprint radius. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iRadius;

	/** The Fingerprint layer flags. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iLayerFlags;

	/** The Fingerprint similarity bits. Can be -1 {@link #UNAVAILABLE}, if not set. */
	private int m_iSimilarityBits;

	//
	// Constructor
	//

	/**
	 * Creates a new fingerprint settings object with the specified fingerprint type and settings.
	 * 
	 * @param strType Fingerprint type value. Can be null.
	 * @param iMinPath Min Path value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iMaxPath Min Path value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iNumBits Num Bits (Length) value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iRadius Radius value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iLayerFlags Layer Flags value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iSimilarityBits Similarity bits. Can be -1 ({@link #UNAVAILABLE}.
	 */
	public DefaultFingerprintSettings(final String strType, final int iMinPath,
			final int iMaxPath, final int iNumBits, final int iRadius, final int iLayerFlags,
			final int iSimilarityBits) {
		m_strType = strType;
		m_rdkitType = FingerprintType.parseString(m_strType);
		m_iMinPath = iMinPath;
		m_iMaxPath = iMaxPath;
		m_iNumBits = iNumBits;
		m_iRadius = iRadius;
		m_iLayerFlags = iLayerFlags;
		m_iSimilarityBits = iSimilarityBits;
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
			m_iMinPath = existing.getMinPath();
			m_iMaxPath = existing.getMaxPath();
			m_iNumBits = existing.getNumBits();
			m_iRadius = existing.getRadius();
			m_iLayerFlags = existing.getLayerFlags();
			m_iSimilarityBits = existing.getSimilarityBits();
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
	public synchronized int getMinPath() {
		return m_iMinPath;
	}

	@Override
	public synchronized int getMaxPath() {
		return m_iMaxPath;
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
	public synchronized void setMinPath(final int iMinPath) {
		this.m_iMinPath = iMinPath;
	}

	@Override
	public synchronized void setMaxPath(final int iMaxPath) {
		this.m_iMaxPath = iMaxPath;
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
	public synchronized boolean isAvailable(final int iNumber) {
		return iNumber != UNAVAILABLE;
	}

	public synchronized void reset() {
		m_strType = null;
		m_rdkitType = null;
		m_iMinPath = UNAVAILABLE;
		m_iMaxPath = UNAVAILABLE;
		m_iNumBits = UNAVAILABLE;
		m_iRadius = UNAVAILABLE;
		m_iLayerFlags = UNAVAILABLE;
		m_iSimilarityBits = UNAVAILABLE;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + m_iLayerFlags;
		result = prime * result + m_iSimilarityBits;
		result = prime * result + m_iMaxPath;
		result = prime * result + m_iMinPath;
		result = prime * result + m_iNumBits;
		result = prime * result + m_iRadius;
		result = prime * result
				+ ((m_strType == null) ? 0 : m_strType.hashCode());
		result = prime * result
				+ ((m_rdkitType == null) ? 0 : m_rdkitType.hashCode());
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
					this.m_iMinPath == specToCompare.m_iMinPath &&
					this.m_iMaxPath == specToCompare.m_iMaxPath &&
					this.m_iNumBits == specToCompare.m_iNumBits &&
					this.m_iRadius == specToCompare.m_iRadius &&
					this.m_iLayerFlags == specToCompare.m_iLayerFlags &&
					this.m_iSimilarityBits == specToCompare.m_iSimilarityBits) {
				bRet = true;
			}
		}

		return bRet;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();

		if (getRdkitFingerprintType() != null) {
			sb.append(getRdkitFingerprintType().toString()).append(" Fingerprint");
		}
		else if (getFingerprintType() != null) {
			sb.append(getFingerprintType()).append(" Fingerprint");
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
}
