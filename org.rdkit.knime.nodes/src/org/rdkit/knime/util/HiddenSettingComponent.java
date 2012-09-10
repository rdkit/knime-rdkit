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
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Provides a standard component for a dialog, which is hidden and
 * cannot be changed by the user.
 *
 * @author Manuel Schwarze
 */
public final class HiddenSettingComponent extends DialogComponent {

	//
    // Constructors
    //
    
    /**
     * Constructor that makes the setting known to the super class, 
     * which will take care of loading and saving it.
     *
     * @param settingsModel The model that stores the value for this component.
     */
    public HiddenSettingComponent(final SettingsModel settingsModel) {
        super(settingsModel);
    }
    
    //
    // Protected Methods
    //

    /**
     * Does not do anything.
     */
	@Override
	protected void updateComponent() {
		// Empty by purpose
	}

    /**
     * Does not do anything.
     */
	@Override
	protected void validateSettingsBeforeSave() throws InvalidSettingsException {
		// Empty by purpose
	}

    /**
     * Does not do anything.
     */
	@Override
	protected void checkConfigurabilityBeforeLoad(PortObjectSpec[] specs)
			throws NotConfigurableException {
		// Empty by purpose
	}

    /**
     * Does not do anything.
     */
	@Override
	protected void setEnabledComponents(boolean enabled) {
		// Empty by purpose
	}

    /**
     * Does not do anything.
     */
	@Override
	public void setToolTipText(String text) {
		// Empty by purpose
	}
}
