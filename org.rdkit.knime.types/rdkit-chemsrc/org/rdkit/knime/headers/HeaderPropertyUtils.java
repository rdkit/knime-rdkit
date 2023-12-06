/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2013-2023
 * Novartis Pharma AG, Switzerland
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
package org.rdkit.knime.headers;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;

/**
 * This class offers utility methods to deal with header properties
 * in conjunction with Header Property Handlers.
 * 
 * @author Manuel Schwarze
 */
public class HeaderPropertyUtils {

	//
	// Static Public Methods
	//

	/**
	 * Writes the additional header information of this instance into the
	 * passed in column spec creator overwriting already set properties.
	 * If there are other properties that shall be maintained, call instead
	 * {@link #writeInColumnSpec(DataColumnSpecCreator, DataColumnProperties)}.
	 * 
	 * @param colSpecCreator The column spec creator that shall receive the additional
	 * 		header information. Must not be null.
	 * @param keyValuePairs Array of key value pairs.
	 * 
	 * @return The properties object which has been set for the column spec creator.
	 *    	Returning it gives the caller the chance to add other properties again.
	 * 
	 * @see #writeInColumnSpec(DataColumnSpecCreator, DataColumnProperties)
	 */
	public static DataColumnProperties writeInColumnSpec(final DataColumnSpecCreator colSpecCreator,
			final String... keyValuePairs) {
		return writeInColumnSpec(colSpecCreator, null, keyValuePairs);
	}

	/**
	 * Writes the specified property key / value pairs into the
	 * passed in column spec creator, adding them to already existing properties,
	 * if specified. The properties are set for the column spec creator.
	 * 
	 * @param colSpecCreator The column spec creator that shall receive the additional
	 * 		header information. Must not be null.
	 * @param propsExisting Optionally these existing column properties will be used
	 * 		as basis and the additional header information is merged in.
	 * @param keyValuePairs Array of key value pairs.
	 * 
	 * @return The properties object which has been set for the column spec creator.
	 *    	Returning it gives the caller the chance to add other properties again.
	 */
	public static DataColumnProperties writeInColumnSpec(final DataColumnSpecCreator colSpecCreator,
			final DataColumnProperties propsExisting, final String... keyValuePairs) {
		if (colSpecCreator == null) {
			throw new IllegalArgumentException("Column spec creator must not be null.");
		}

		// Define properties with additional information
		final Map<String, String> newProps = new HashMap<String, String>();

		if (keyValuePairs != null && keyValuePairs.length > 1) {
			for (int i = 0; i < keyValuePairs.length - 1; i = i + 2) {
				if (keyValuePairs[i] != null && !keyValuePairs[i].trim().isEmpty() &&
						keyValuePairs[i+1] != null && !keyValuePairs[i+1].trim().isEmpty())
					newProps.put(keyValuePairs[i], keyValuePairs[i+1]);
			}
		}

		DataColumnProperties newColumnProps;

		if (propsExisting != null) {
			newColumnProps = propsExisting.cloneAndOverwrite(newProps);
		}
		else {
			newColumnProps = new DataColumnProperties(newProps);
		}

		colSpecCreator.setProperties(newColumnProps);
		return newColumnProps;
	}

	/**
	 * Removes the specified property keys from the specified column specification.
	 * 
	 * @param spec Column specification from which we will remove the
	 * 		additional header information.
	 * @param keys Property keys to be removed.
	 */
	public static DataColumnSpec removeProperties(final DataColumnSpec spec,
			final String... keys) {
		DataColumnSpec specRet = spec;

		if (spec != null && keys != null && keys.length > 0) {
			// Check, if at least one property is found to be removed
			if (existOneProperty(spec, keys)) {
				final DataColumnProperties oldProps = spec.getProperties();
				final DataColumnSpecCreator creator = new DataColumnSpecCreator(spec);
				final Map<String, String> mapNewProps = new HashMap<String, String>();

				for (final Enumeration<String> e = oldProps.properties(); e.hasMoreElements(); ) {
					final String strPropKey = e.nextElement();
					mapNewProps.put(strPropKey, oldProps.getProperty(strPropKey));
				}

				for (final String strPropsToBeRemoved : keys) {
					mapNewProps.remove(strPropsToBeRemoved);
				}

				creator.setProperties(new DataColumnProperties(mapNewProps));
				specRet = creator.createSpec();
			}
		}

		return specRet;

	}

	public static boolean existAllProperties(final DataColumnSpec colSpec,
			final String... keys) {
		boolean bFound = false;

		if (colSpec != null && keys != null && keys.length > 0) {
			bFound = true;
			final DataColumnProperties props = colSpec.getProperties();

			for (final String strProp : keys) {
				if (props.containsProperty(strProp)) {
					final String strValue = props.getProperty(strProp);
					if (strValue == null || strValue.trim().isEmpty()) {
						bFound = false;
						break;
					}
				}
				else {
					bFound = false;
					break;
				}
			}
		}

		return bFound;
	}

	public static boolean existOneProperty(final DataColumnSpec colSpec,
			final String... keys) {
		boolean bFound = false;

		if (colSpec != null && keys != null && keys.length > 0) {
			final DataColumnProperties props = colSpec.getProperties();

			for (final String strProp : keys) {
				if (props.containsProperty(strProp)) {
					final String strValue = props.getProperty(strProp);
					if (strValue != null && !strValue.trim().isEmpty()) {
						bFound = true;
						break;
					}
				}
			}
		}

		return bFound;
	}

	public static Map<String, String> getProperties(final DataColumnSpec colSpec,
			final HeaderPropertyHandler handler) {
		if (handler == null) {
			return getProperties(colSpec);
		}
		else {
			return getProperties(colSpec, handler.getAcceptableProperties());
		}
	}

	public static Map<String, String> getProperties(final DataColumnSpec colSpec,
			final String... keys) {
		final Map<String, String> mapRet = new HashMap<String, String>();

		if (colSpec != null) {
			final DataColumnProperties props = colSpec.getProperties();

			if (keys != null) {
				for (final String strProp : keys) {
					if (props.containsProperty(strProp)) {
						final String strValue = props.getProperty(strProp);
						if (strValue != null && !strValue.trim().isEmpty()) {
							mapRet.put(strProp, strValue);
						}
					}
				}
			}
			else {
				for (final Enumeration<String> e = props.properties(); e.hasMoreElements(); ) {
					final String strProp = e.nextElement();
					final String strValue = props.getProperty(strProp);
					if (strValue != null && !strValue.trim().isEmpty()) {
						mapRet.put(strProp, strValue);
					}
				}
			}
		}

		return mapRet;
	}


	/**
	 * This method compares two objects and considers also the value null.
	 * If the objects are both not null, equals is called for o1 with o2.
	 * 
	 * @param o1 The first object to compare. Can be null.
	 * @param o2 The second object to compare. Can be null.
	 * 
	 * @return True, if the two objects are equal. Also true, if
	 * 		   both objects are null.
	 */
	@SuppressWarnings("rawtypes")
	public static boolean equals(final Object o1, final Object o2) {
		boolean bResult = false;

		if (o1 == o2) {
			bResult = true;
		}
		else if (o1 != null && o1.getClass().isArray() &&
				o2 != null && o2.getClass().isArray() &&
				o1.getClass().getComponentType().equals(o2.getClass().getComponentType()) &&
				Array.getLength(o1) == Array.getLength(o2)) {
			final int iLength = Array.getLength(o1);

			// Positive presumption
			bResult = true;

			for (int i = 0; i < iLength; i++) {
				if ((bResult &= HeaderPropertyUtils.equals(Array.get(o1, i), Array.get(o2, i))) == false) {
					break;
				}
			}
		}
		else if (o1 instanceof Collection && o2 instanceof Collection &&
				((Collection)o1).size() == ((Collection)o2).size()) {
			final Iterator i1 = ((Collection)o1).iterator();
			final Iterator i2 = ((Collection)o2).iterator();

			// Positive presumption
			if (i1.hasNext() && i2.hasNext()) {
				bResult = true;

				while (i1.hasNext() && i2.hasNext()) {
					if ((bResult &= HeaderPropertyUtils.equals(i1.next(), i2.next())) == false) {
						break;
					}
				}
			}
		}
		else if (o1 != null && o2 != null) {
			bResult = o1.equals(o2);
		}
		else if (o1 == null && o2 == null) {
			bResult = true;
		}

		return bResult;
	}

	//
	// Constructors
	//

	private HeaderPropertyUtils() {
		// To avoid instantiation of this class
	}
}
