/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2010
 *  Novartis Institutes for BioMedical Research
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
 * ----------------------------------------------------------------------------
 */
package org.rdkit.knime;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.BundleContext;

/**
 * This is the activator for this plugin that is instantiated by the Eclipse
 * framework once the plugin is loaded. It does some initialization stuff.
 *
 * @author Greg Landrum
 */
public class RDKitTypesPluginActivator extends AbstractUIPlugin {
    // The shared instance.
    // TODO: plugin is never initialized
    private static RDKitTypesPluginActivator plugin;
    private static IStatus error;

    /**
     * This method is called upon plug-in activation.
     *
     * @param context the OSGI bundle context
     * @throws Exception if this plugin could not be started
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        try {
            error = null;
            System.loadLibrary("GraphMolWrap");
        } catch (UnsatisfiedLinkError e) {
            error = new Status(IStatus.ERROR, context.getBundle()
                            .getSymbolicName(),
                            "Could not load native RDKit library", e);
            NodeLogger.getLogger("RDKit").error(error.getMessage(),
                    error.getException());
            Platform.getLog(context.getBundle()).log(error);
        }
//        final IPreferenceStore pStore = getPreferenceStore();
//        pStore.addPropertyChangeListener(new IPropertyChangeListener() {
//            /** {@inheritDoc} */
//            @Override
//            public void propertyChange(final PropertyChangeEvent event) {
//            }
//        });
    }

    /**
     * This method is called when the plug-in is stopped.
     *
     * @param context the OSGI bundle context
     * @throws Exception if this plugin could not be stopped
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
    }
//
//    /**
//     * Lookups the preferred renderer for a given molecular type.
//     *
//     * @param valueClass The DataValue class of interest, one of
//     *            {@link SdfValue}, {@link Mol2Value}, {@link SmilesValue}
//     * @return The class name of the stored preferred renderer or
//     *         <code>null</code> if none has been saved.
//     */
//    public String getPreferredRendererClassName(
//            final Class<? extends DataValue> valueClass) {
//        final IPreferenceStore pStore = getPreferenceStore();
//        String preferenceIdentifier = getPreferenceIdentifier(valueClass);
//        String resultClassName = pStore.getString(preferenceIdentifier);
//        if (resultClassName == null || resultClassName.length() == 0) {
//            return null;
//        }
//        return resultClassName;
//    }

    /**
     * Checks if native RDKit library was successfully loaded upon plug-in
     * activation. Throws {@link InvalidSettingsException} otherwise.
     *
     * @throws InvalidSettingsException when an error occurred upon plug-in
     * activation
     */
    public static void checkErrorState() throws InvalidSettingsException {
        if (null != error) {
            throw new InvalidSettingsException(error.getMessage(),
                    error.getException());
        }
    }

    /**
     * Returns the shared instance.
     *
     * @return singleton instance of the Plugin
     */
    public static RDKitTypesPluginActivator getDefault() {
        return plugin;
    }
//
//    /** Get preference name for a given data value class. */
//    private static String getPreferenceIdentifier(
//            final Class<? extends DataValue> valueClass) {
//        return "prefRenderer_" + valueClass.getName();
//    }
}
