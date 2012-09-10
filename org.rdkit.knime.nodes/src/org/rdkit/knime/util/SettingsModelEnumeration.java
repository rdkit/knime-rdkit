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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelFlowVariableCompatible;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;

/**
 * This class stores an enumeration based setting.
 *
 * @author Manuel Schwarze
 *
 * @param <T> An arbitrary Enumeration.
 */
public class SettingsModelEnumeration<T extends Enum<T>> extends SettingsModel
implements SettingsModelFlowVariableCompatible {

	//
	// Constants
	//

	/** The logger instance. */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(SettingsModelEnumeration.class);

	//
	// Members
	//

	/** Determines, if the last loading of settings failed. */
	private boolean m_bLoadingSettingsFailed = false;

	/** The enumeration class for this model. */
	private Class<T> m_enumType;

	/** The current enumeration value. */
    private T m_value;

    /** The default value to use. */
    private T m_defaultValue;

    /** The key used to store the enumeration value in the settings. */
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
     * @param defaultValue The initial value. Must not be null.
     */
    public SettingsModelEnumeration(final Class<T> enumType, final String configName,
            final T defaultValue) {
    	if (enumType == null) {
    		throw new IllegalArgumentException("Enumeration type must not be null.");
    	}
        if ((configName == null) || (configName.isEmpty())) {
            throw new IllegalArgumentException("The configName must be a "
                    + "non-empty string");
        }
        if (defaultValue == null) {
            throw new IllegalArgumentException("The default/initial value must not be null.");
        }

        m_enumType = enumType;
        m_value = defaultValue;
        m_defaultValue = defaultValue;
        m_configName = configName;
    }

    //
    // Public Methods
    //

    /**
     * Determines, if the value of this model could not be set when settings were
     * loaded, because it was not found in the settings or invalid.
     *
     * @return True, if loading of settings failed. False otherwise.
     */
    public boolean wasUndefinedInSettings() {
    	return m_bLoadingSettingsFailed;
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
    protected SettingsModelEnumeration<T> createClone() {
        SettingsModelEnumeration<T> clone = new SettingsModelEnumeration<T>(m_enumType, m_configName, m_defaultValue);
        clone.setValue(m_value);
        return clone;
    }

    /**
     * {@inheritDoc}
     * Returns SMID_string as it is compatible with a string model.
     */
    @Override
    protected String getModelTypeID() {
        return "SMID_string";
    }

    /**
     * Returns the enumeration class this setting is based on.
     *
     * @return Enumeration class.
     */
    protected Class<T> getEnumerationType() {
    	return m_enumType;
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
    public T getDefaultValue() {
    	return m_defaultValue;
    }

    /**
     * Set the value stored to the new value.
     *
     * @param newValue The new value to store.
     *
     * @see #setValueAsString(String)
     */
    public void setValue(final T newValue) {
        boolean sameValue;

        if (newValue == null) {
            sameValue = (m_value == null);
        } else {
            sameValue = newValue.equals(m_value);
        }
        m_value = newValue;

        if (!sameValue) {
            notifyChangeListeners();
        }
    }

    /**
     * Set the value stored to the new value represented as string.
     * This string must match an enumeration name of the registered
     * enumeration class. If not, the default value will be used instead
     * and a warning will be logged.
     *
     * @param newValueAsString The new value to store.
     *
     * @see #setValue(Enum)
     */
	public void setValueAsString(final String newValueAsString) {
    	if (newValueAsString == null) {
    		setValue(null);
    	}
    	else {
    		T newValue = null;
    		try {
    			// First try: The normal "name" value of the enumeration
    			newValue = Enum.valueOf(m_enumType, newValueAsString);
    		}
    		catch (Exception exc) {
    			// Second try: The toString() value of an enumeration value - this comes handy when using FlowVariables
    			for (T enumValue : m_enumType.getEnumConstants()) {
    				String strRepresentation = enumValue.toString();
    				if (newValueAsString.equals(strRepresentation)) {
    					newValue = enumValue;
    					m_bLoadingSettingsFailed = false;
    					break;
    				}
    			}

    			// Third case: Fallback to default value
    			if (newValue == null) {
	    			LOGGER.warn("Value '" + newValueAsString + "' could not be selected. It is unknown in this version. Using default value '" + m_defaultValue + "'.");
	    			newValue = m_defaultValue;
    			}
    		}

    		setValue(newValue);
    	}
    }

    /**
     * Returns the current enumeration value of this setting model.
     *
     * @return The current value stored. Never null.
     */
    public T getValue() {
        return m_value;
    }

    /**
     * @return the current value stored.
     */
    public String getValueAsString() {
        return (m_value == null ? null : m_value.name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getString(m_configName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_bLoadingSettingsFailed = true;
        try {
            // no default value, throw an exception instead
            setValueAsString(settings.getString(m_configName));
            m_bLoadingSettingsFailed = false;
        } catch (final IllegalArgumentException iae) {
            throw new InvalidSettingsException(iae.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
    	String valueAsString = getValueAsString();
        settings.addString(m_configName, valueAsString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
    	m_bLoadingSettingsFailed = true;
        try {
            // use the current value, if no value is stored in the settings
        	String defaultValueAsString = getValueAsString();
            setValueAsString(settings.getString(m_configName, defaultValueAsString));
            m_bLoadingSettingsFailed = false;
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
    	String valueAsString = getValueAsString();
        settings.addString(m_configName, valueAsString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getKey() {
        return m_configName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FlowVariable.Type getFlowVariableType() {
        return FlowVariable.Type.STRING;
    }


}
