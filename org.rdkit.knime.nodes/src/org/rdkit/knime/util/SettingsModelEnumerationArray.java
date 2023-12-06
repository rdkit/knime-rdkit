/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2012-2023
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
package org.rdkit.knime.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;

/**
 * This class stores a setting with an array of enumeration values.
 *
 * @author Manuel Schwarze
 *
 * @param <T> An arbitrary Enumeration.
 */
public class SettingsModelEnumerationArray<T extends Enum<T>> extends SettingsModel {

	//
	// Constants
	//

	/** The logger instance. */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(SettingsModelEnumerationArray.class);

	//
	// Members
	//

	/** The enumeration class for this model. */
	private final Class<T> m_enumType;

	/** The current enumeration values. */
	private T[] m_arrValues;

	/** The default value(s) to use. */
	private final T[] m_arrDefaultValues;

	/** The key used to store the enumeration array in the settings. */
	private final String m_configName;

	//
	// Constructor
	//

	/**
	 * Creates a new object holding a list of enumeration values. The current value will
	 * be set to the specified default value list.
	 *
	 * @param enumType Enumeration class this setting is based on. Must not be null.
	 * @param configName The identifier the value is stored with in the
	 *            {@link org.knime.core.node.NodeSettings} object. Must not be null.
	 * @param arrDefaultValues The initial value. Must not be null.
	 */
	public SettingsModelEnumerationArray(final Class<T> enumType, final String configName,
			final T[] arrDefaultValues) {
		if (enumType == null) {
			throw new IllegalArgumentException("Enumeration type must not be null.");
		}
		if ((configName == null) || (configName.isEmpty())) {
			throw new IllegalArgumentException("The configName must be a "
					+ "non-empty string");
		}
		if (arrDefaultValues == null) {
			throw new IllegalArgumentException("The default/initial values must not be null.");
		}

		m_enumType = enumType;
		m_arrValues = arrDefaultValues;
		m_arrDefaultValues = arrDefaultValues;
		m_configName = configName;
	}

	//
	// Protected Methods
	//

	/**
	 * Creates a clone of this object taking over all settings.
	 *
	 * @return An identical copy of this instance.
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected SettingsModelEnumerationArray<T> createClone() {
		final SettingsModelEnumerationArray<T> clone = new SettingsModelEnumerationArray<T>(m_enumType, m_configName, m_arrDefaultValues);
		clone.setValues(m_arrValues);
		return clone;
	}

	/**
	 * {@inheritDoc}
	 * Returns SMID_stringarray as it is compatible with a string array model.
	 */
	@Override
	protected String getModelTypeID() {
		return "SMID_stringarray";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getConfigName() {
		return m_configName;
	}

	/**
	 * Returns the default value of this setting.
	 *
	 * @return Default value. Never null.
	 */
	public T[] getDefaultValues() {
		return m_arrDefaultValues;
	}

	/**
	 * Returns the enumeration class this setting is based on.
	 *
	 * @return Enumeration class.
	 */
	public Class<T> getEnumerationType() {
		return m_enumType;
	}

	/**
	 * Set the value stored to the new value.
	 *
	 * @param newValues The new value to store.
	 *
	 * @see #setValueAsString(String)
	 */
	@SuppressWarnings("unchecked")
	public void setValues(final T[] arrNewValues) {
		boolean same;
		if (arrNewValues == null) {
			same = (m_arrValues == null);
		}
		else {
			if ((m_arrValues == null) || (m_arrValues.length != arrNewValues.length)) {
				same = false;
			}
			else {
				final Set<T> current = new HashSet<T>(Arrays.asList(m_arrValues));
				same = true;
				for (final T item : arrNewValues) {
					if (!current.contains(item)) {
						same = false;
						break;
					}
				}
			}
		}

		if (arrNewValues == null) {
			m_arrValues = null;
		}
		else {
			m_arrValues = (T[])Array.newInstance(m_enumType, arrNewValues.length);
			System.arraycopy(arrNewValues, 0, m_arrValues, 0, arrNewValues.length);
		}

		if (!same) {
			notifyChangeListeners();
		}
	}

	/**
	 * Set the values stored to the new values represented as strings.
	 * These strings must match enumeration names or toString() values of the registered
	 * enumeration class. If not, the default values will be used instead
	 * for ALL values and a warning will be logged.
	 *
	 * @param newValuesAsString The new values to store.
	 *
	 * @see #setValue(Enum)
	 */
	public void setValuesAsString(final String[] newValuesAsString) {
		if (newValuesAsString == null) {
			setValues(null);
		}
		else {
			@SuppressWarnings("unchecked")
			final
			T[] arrNewValues = (T[])Array.newInstance(m_enumType, newValuesAsString.length);
			final List<T> listPlaceholders = findFlowVariablePlaceholders();
			for (int i = 0; i < newValuesAsString.length; i++) {

				try {
					// First try: The normal "name" value of the enumeration
					arrNewValues[i] = Enum.valueOf(m_enumType, newValuesAsString[i]);
				}
				catch (final Exception exc) {
					// Second try: The toString() value of an enumeration value
					for (final T enumValue : m_enumType.getEnumConstants()) {
						final String strRepresentation = enumValue.toString();
						if (newValuesAsString[i].equalsIgnoreCase(strRepresentation)) {
							arrNewValues[i] = enumValue;
							break;
						}
					}

					// Third try: Use name() value, but trim() the new value first
					if (arrNewValues[i] == null) {
						try {
							arrNewValues[i] = Enum.valueOf(m_enumType, newValuesAsString[i].trim());
						}
						catch (final Exception excIgnored) {
							// Ignore by purpose
						}
					}

					// Fourth case: Use placeholder values or set to null, if totally unknown
					if (arrNewValues[i] == null) {
						if (newValuesAsString[i].isEmpty()) {
							// Note: If the new value string is empty, the reason is most likely
							// that it is controlled by a flow variable, which is currently
							// unavailable. - In this case we will use flow variable placeholders,
							// if available. Otherwise we set the value to null and issue
							// a warning.
							if (!listPlaceholders.isEmpty()) {
								arrNewValues[i] = listPlaceholders.get(0);
							}
							else {
								arrNewValues[i] = null;
								LOGGER.warn("One of the selected values which is controlled" +
										" by a flow variable could not be maintained due to" +
										" the lack of flow variable placeholders. This may" +
										" have negative side-effects when a node gets reconfigured.");
							}
						}
						else {
							String strResolution;
							if (!listPlaceholders.isEmpty()) {
								arrNewValues[i] = listPlaceholders.get(0);
								strResolution = "It has been replaced by a flow variable placeholder.";
							}
							else {
								arrNewValues[i] = null;
								strResolution = "It will be removed when reconfiguring the node.";
							}
							LOGGER.warn("Value '" + newValuesAsString[i] +
									"' could not be selected. It is unknown in this version. " +
									strResolution);
						}
						break;
					}
				}

				// Take a flow variable placeholder off the list if it was used
				if (listPlaceholders.contains(arrNewValues[i])) {
					listPlaceholders.remove(arrNewValues[i]);
				}
			}

			setValues(arrNewValues);
		}
	}

	/**
	 * Returns the current enumeration value of this setting model.
	 *
	 * @return The current value stored. Never null.
	 */
	public T[] getValues() {
		return m_arrValues;
	}

	/**
	 * @return the current value stored.
	 */
	public String[] getValuesAsString() {
		String[] arrRet = null;

		if (m_arrValues != null) {
			final int iCount = m_arrValues.length;
			arrRet = new String[iCount];
			for (int i = 0; i < iCount; i++) {
				arrRet[i] = (m_arrValues[i] == null ? "" : m_arrValues[i].name());
			}
		}

		return arrRet;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettingsForModel(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		settings.getStringArray(m_configName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsForModel(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		try {
			// no default value, throw an exception instead
			String[] arrSettings = settings.getStringArray(m_configName);
			arrSettings = removeFlowValuePlaceholders(arrSettings);
			setValuesAsString(arrSettings);
		} catch (final IllegalArgumentException iae) {
			throw new InvalidSettingsException(iae.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsForModel(final NodeSettingsWO settings) {
		String[] arrValuesAsString = getValuesAsString();
		arrValuesAsString = addFlowValuePlaceholders(arrValuesAsString);
		settings.addStringArray(m_configName, arrValuesAsString);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsForDialog(final NodeSettingsRO settings,
			final PortObjectSpec[] specs) throws NotConfigurableException {
		try {
			// use the current value, if no value is stored in the settings
			final String[] arrDefaultValuesAsString = getValuesAsString();
			String[] arrSettings = settings.getStringArray(m_configName, arrDefaultValuesAsString);
			arrSettings = removeFlowValuePlaceholders(arrSettings);
			setValuesAsString(arrSettings);
		} catch (final IllegalArgumentException iae) {
			// if the argument is not accepted: keep the old value.
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsForDialog(final NodeSettingsWO settings)
			throws InvalidSettingsException {
		String[] arrValuesAsString = getValuesAsString();
		arrValuesAsString = addFlowValuePlaceholders(arrValuesAsString);
		settings.addStringArray(m_configName, arrValuesAsString);
	}

	/**
	 * Fills up the array (null is treated like an empty array) with
	 * flow value placeholder values, if they are not existing yet in
	 * the array. The returned array will contain not more elements
	 * in total as the total number of flow variable placeholders of
	 * the enumeration. This means, if there are 2 "real" elements already
	 * and 5 placeholders exist, this method will only add 3 placeholders.
	 * 
	 * @param arrValuesAsString Existing value array. Can be null.
	 * 
	 * @return Array with all placeholders added, which were not in the array yet.
	 * 		Never null, but maybe empty (if there are no placeholders in the
	 * 		enumeration).
	 */
	protected String[] addFlowValuePlaceholders(final String[] arrValuesAsString) {
		final List<String> listRet = new ArrayList<String>();
		final List<T> listPlaceholders = findFlowVariablePlaceholders();

		// Add existing values
		if (arrValuesAsString != null) {
			for (final String item : arrValuesAsString) {
				if (item != null) {
					listRet.add(item);
				}
			}
		}

		// Add placeholders, if they are not contained in the list yet
		if (listPlaceholders != null) {
			for (final T item : listPlaceholders) {
				if (listRet.size() < listPlaceholders.size()) {
					final String strPlaceholder = item.name();
					if (!listRet.contains(strPlaceholder)) {
						listRet.add(strPlaceholder);
					}
				}
				else {
					// Do not add more elements to the list
					// as there are placeholders
					break;
				}
			}
		}

		return listRet.toArray(new String[listRet.size()]);
	}

	/**
	 * Removes all flow value placeholder values from the array
	 * (null is treated like an empty array).
	 * 
	 * @param arrValuesAsString Existing value array (potentially with
	 * 		placeholder values). Can be null.
	 * 
	 * @return Array with all placeholders removed. Returns null, if null was passed in.
	 * 		Returns an empty array, if all values were placeholders before.
	 */
	protected String[] removeFlowValuePlaceholders(final String[] arrValuesAsString) {
		String[] arrRet = null;

		if (arrValuesAsString != null) {
			final List<String> listRet = new ArrayList<String>();
			final List<T> listPlaceholders = findFlowVariablePlaceholders();

			// Remove existing placeholder values
			if (arrValuesAsString != null) {
				for (final String strItem : arrValuesAsString) {
					try {
						// Add the item only, if it is not a placeholder
						if (strItem == null ||
								!listPlaceholders.contains(Enum.valueOf(m_enumType, strItem))) {
							listRet.add(strItem);
						}
					}
					catch (final Exception exc) {
						// We ignore this exception here - it would mean that
						// an item was found, which is definitely not one of the
						// placeholder values - so just add the item
						listRet.add(strItem);
					}
				}
			}

			arrRet = listRet.toArray(new String[listRet.size()]);
		}

		return arrRet;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName() + " ('" + m_configName + "')";
	}

	/**
	 * Determines, if the passed in enumeration item is shall
	 * be treated as flow variable placeholder. This is the case,
	 * if it's name starts with "FlowVariablePlaceHolder".
	 * 
	 * @param item Enumeration item. Can be null.
	 * 
	 * @return True, if the specified item is considered one of the
	 * 	 	FlowVariablePlaceHolderXXX enumeration constants. False,
	 * 		if null was passed in or if it is not such a constant.
	 */
	public boolean isFlowVariablePlaceholder(final T item) {
		return (item == null || item.name().startsWith("FlowVariablePlaceHolder"));
	}

	/**
	 * Traverses all enumeration values of the specified enum type
	 * and filters out and returns all constants that start with
	 * "FlowVariablePlaceHolder".
	 * 
	 * @param enumType Enumeration type.
	 * 
	 * @return List of all FlowVariablePlaceHolderXXX enumeration constants.
	 */
	public List<T> findFlowVariablePlaceholders() {
		final List<T> listPlaceHolders = new ArrayList<T>();
		for (final T enumConstant : m_enumType.getEnumConstants()) {
			if (isFlowVariablePlaceholder(enumConstant)) {
				listPlaceHolders.add(enumConstant);
			}
		}
		return listPlaceHolders;
	}
}
