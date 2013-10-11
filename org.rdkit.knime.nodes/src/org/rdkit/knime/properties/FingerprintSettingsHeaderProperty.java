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
package org.rdkit.knime.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.StringCell;
import org.rdkit.knime.headers.HeaderProperty;
import org.rdkit.knime.headers.HeaderPropertyUtils;
import org.rdkit.knime.nodes.rdkfingerprint.DefaultFingerprintSettings;
import org.rdkit.knime.nodes.rdkfingerprint.FingerprintSettings;

/**
 * This class knows how to write values about a Fingerprint specification into a column
 * specification and reads it.
 * 
 * @author Manuel Schwarze
 */
public class FingerprintSettingsHeaderProperty extends DefaultFingerprintSettings implements FingerprintSettings, HeaderProperty, StringValue {

	//
	// Constants
	//

	/** The serial number. */
	private static final long serialVersionUID = 6808143734891935432L;

	/** The property that holds a fingerprint information that can be handled with this handler. */
	public static final String PROPERTY_FP_TYPE = "rdkit.fingerprint.type";

	/** The property that holds a fingerprint information that can be handled with this handler. */
	public static final String PROPERTY_FP_MIN_PATH = "rdkit.fingerprint.minPath";

	/** The property that holds a fingerprint information that can be handled with this handler. */
	public static final String PROPERTY_FP_MAX_PATH = "rdkit.fingerprint.maxPath";

	/** The property that holds a fingerprint information that can be handled with this handler. */
	public static final String PROPERTY_FP_NUM_BITS = "rdkit.fingerprint.numBits";

	/** The property that holds a fingerprint information that can be handled with this handler. */
	public static final String PROPERTY_FP_RADIUS = "rdkit.fingerprint.radius";

	/** The property that holds a fingerprint information that can be handled with this handler. */
	public static final String PROPERTY_FP_LAYER_FLAGS = "rdkit.fingerprint.layerFlags";

	/** The property that holds a fingerprint information that can be handled with this handler. */
	public static final String PROPERTY_FP_SIMILARITY_BITS = "rdkit.fingerprint.similarityBits";

	/**
	 * The properties that are involved in handling Fingerprint specification header properties.
	 */
	public static final String[] PROPERTIES_FP = new String [] {
		PROPERTY_FP_TYPE, PROPERTY_FP_MIN_PATH, PROPERTY_FP_MAX_PATH,
		PROPERTY_FP_NUM_BITS, PROPERTY_FP_RADIUS, PROPERTY_FP_LAYER_FLAGS,
		PROPERTY_FP_SIMILARITY_BITS
	};

	/** String column specification used to find correct renderer. */
	private static final DataColumnSpec COLUMN_SPEC =
			new DataColumnSpecCreator("String", StringCell.TYPE).createSpec();

	//
	// Constructor
	//

	/**
	 * Creates a new header property object with the specified fingerprint type.
	 * 
	 * @param strType Fingerprint type value. Can be null.
	 * @param iMinPath Min Path value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iMaxPath Min Path value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iNumBits Num Bits (Length) value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iRadius Radius value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iLayerFlags Layer Flags value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iSimilarityBits Similarity bits. Can be -1 ({@link #UNAVAILABLE}.
	 */
	public FingerprintSettingsHeaderProperty(final String strType, final int iMinPath,
			final int iMaxPath, final int iNumBits, final int iRadius, final int iLayerFlags,
			final int iSimilarityBits) {
		super(strType, iMinPath, iMaxPath, iNumBits, iRadius, iLayerFlags, iSimilarityBits);
	}

	/**
	 * Creates a new header property object with the specified fingerprint settings.
	 * 
	 * @param settings Fingerprint settings. Can be null.
	 */
	public FingerprintSettingsHeaderProperty(final FingerprintSettings settings) {
		super(settings);
	}

	/**
	 * Creates a new header property object based on Fingerprint Spe
	 * value information taken from the past in data column
	 * specification.
	 * 
	 * @param colSpec Data column specification. Can be null.
	 */
	public FingerprintSettingsHeaderProperty(final DataColumnSpec colSpec) {
		super(null);
		readFromColumnSpec(colSpec);
	}

	//
	// Public Methods
	//

	@Override
	public synchronized void readFromColumnSpec(final DataColumnSpec colSpec) {
		reset();

		if (colSpec != null) {
			final String strColumnName = colSpec.getName();
			final Map<String, String> mapProps =
					HeaderPropertyUtils.getProperties(colSpec, PROPERTIES_FP);

			if (mapProps.containsKey(PROPERTY_FP_TYPE)) {
				setFingerprintType(mapProps.get(PROPERTY_FP_TYPE)); // This sets also the RDKit type if known
			}

			setMinPath(getInt(mapProps, PROPERTY_FP_MIN_PATH, strColumnName));
			setMaxPath(getInt(mapProps, PROPERTY_FP_MAX_PATH, strColumnName));
			setNumBits(getInt(mapProps, PROPERTY_FP_NUM_BITS, strColumnName));
			setRadius(getInt(mapProps, PROPERTY_FP_RADIUS, strColumnName));
			setLayerFlags(getInt(mapProps, PROPERTY_FP_LAYER_FLAGS, strColumnName));
			setSimilarityBits(getInt(mapProps, PROPERTY_FP_SIMILARITY_BITS, strColumnName));
		}
	}

	@Override
	public synchronized void writeToColumnSpec(final DataColumnSpecCreator colSpecCreator) {
		if (colSpecCreator != null) {
			final List<String> listProps = new ArrayList<String>();
			if (getFingerprintType() != null) {
				listProps.add(PROPERTY_FP_TYPE);
				listProps.add(getFingerprintType());
			}
			if (getMinPath() != UNAVAILABLE) {
				listProps.add(PROPERTY_FP_MIN_PATH);
				listProps.add("" + getMinPath());
			}
			if (getMaxPath() != UNAVAILABLE) {
				listProps.add(PROPERTY_FP_MAX_PATH);
				listProps.add("" + getMaxPath());
			}
			if (getNumBits() != UNAVAILABLE) {
				listProps.add(PROPERTY_FP_NUM_BITS);
				listProps.add("" + getNumBits());
			}
			if (getRadius() != UNAVAILABLE) {
				listProps.add(PROPERTY_FP_RADIUS);
				listProps.add("" + getRadius());
			}
			if (getLayerFlags() != UNAVAILABLE) {
				listProps.add(PROPERTY_FP_LAYER_FLAGS);
				listProps.add("" + getLayerFlags());
			}
			if (getSimilarityBits() != UNAVAILABLE) {
				listProps.add(PROPERTY_FP_SIMILARITY_BITS);
				listProps.add("" + getSimilarityBits());
			}

			HeaderPropertyUtils.writeInColumnSpec(colSpecCreator,
					listProps == null ? null : listProps.toArray(new String[listProps.size()]));
		}
	}

	@Override
	public synchronized boolean equals(final DataColumnSpec colSpec) {
		return equals(new FingerprintSettingsHeaderProperty(colSpec));
	}

	@Override
	public DataColumnSpec getColumnSpecForRendering() {
		return COLUMN_SPEC;
	}

	@Override
	public int hashCode() {
		return super.hashCode() * 13;
	}


	@Override
	public synchronized boolean equalsDataCell(final DataCell objSettingsToCompare) {
		boolean bRet = false;

		if (objSettingsToCompare == this) {
			bRet = true;
		}
		else if (objSettingsToCompare instanceof FingerprintSettingsHeaderProperty) {
			bRet = super.equalsDataCell(objSettingsToCompare);
		}

		return bRet;
	}

	@Override
	public String getStringValue() {
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

	/**
	 * Unfortunately, this method is necessary to work with the MultiLineStringValueRenderer
	 * of KNIME, which does currently not use the getStringValue() method as it should.
	 */
	@Override
	public String toString() {
		return getStringValue();
	}
}
