/*
 * This source code, its documentation and all related files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2012
 * Novartis Institutes for BioMedical Research
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 */
package org.rdkit.knime.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
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
	private Class<T> m_enumType;

	/** The current enumeration values. */
    private T[] m_arrValues;

    /** The default value(s) to use. */
    private T[] m_arrDefaultValues;

    /** The key used to store the enumeration array in the settings. */
    private final String m_configName;

    //
    // Constructor
    //

    /**
     * Creates a new object holding an enumeration value. The current value will
     * be set to the specified default value.
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
        SettingsModelEnumerationArray<T> clone = new SettingsModelEnumerationArray<T>(m_enumType, m_configName, m_arrDefaultValues);
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
                Set<T> current = new HashSet<T>(Arrays.asList(m_arrValues));
                same = true;
                for (T item : arrNewValues) {
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
			T[] arrNewValues = (T[])Array.newInstance(m_enumType, newValuesAsString.length);
    		for (int i = 0; i < newValuesAsString.length; i++) {

	    		try {
	    			// First try: The normal "name" value of the enumeration
	    			arrNewValues[i] = Enum.valueOf(m_enumType, newValuesAsString[i]);
	    		}
	    		catch (Exception exc) {
	    			// Second try: The toString() value of an enumeration value
	    			for (T enumValue : m_enumType.getEnumConstants()) {
	    				String strRepresentation = enumValue.toString();
	    				if (newValuesAsString[i].equals(strRepresentation)) {
	    					arrNewValues[i] = enumValue;
	    					break;
	    				}
	    			}

	    			// Third try: Use name() value, but trim() the new value first
	    			if (arrNewValues[i] == null) {
	    				try {
	    					arrNewValues[i] = Enum.valueOf(m_enumType, newValuesAsString[i].trim());
	    				}
	    				catch (Exception excIgnored) {
	    					// Ignore by purpose
	    				}
	    			}

	    			// Fourth case: Fallback to default values and break
	    			if (arrNewValues[i] == null) {
		    			LOGGER.warn("Value '" + newValuesAsString[i] +
		    					"' could not be selected. It is unknown in this version. " +
		    					"Using default values for all selections.");
		    			arrNewValues = m_arrDefaultValues;
		    			break;
	    			}
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
        	int iCount = m_arrValues.length;
        	arrRet = new String[iCount];
        	for (int i = 0; i < iCount; i++) {
        		arrRet[i] = m_arrValues[i].name();
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
            setValuesAsString(settings.getStringArray(m_configName));
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
        	String[] arrDefaultValuesAsString = getValuesAsString();
            setValuesAsString(settings.getStringArray(m_configName, arrDefaultValuesAsString));
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
        settings.addStringArray(m_configName, arrValuesAsString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }
}
