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
package org.rdkit.knime.wizards;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The RDKit Nodes Wizards Plug-in class.
 * 
 * @author Manuel Schwarze
 */
public class RDKitNodesWizardsPlugin extends AbstractUIPlugin {

    /** The shared instance of the plugin. */
    private static RDKitNodesWizardsPlugin plugin;

    /** Plugin ID as defined in plugin XML. */
    public static final String ID = "org.rdkit.knime.wizards";

    /**
     * The constructor.
     */
    public RDKitNodesWizardsPlugin() {
        plugin = this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
    }

    /**
     * @return the shared instance
     */
    public static RDKitNodesWizardsPlugin getDefault() {
        return plugin;
    }
}
