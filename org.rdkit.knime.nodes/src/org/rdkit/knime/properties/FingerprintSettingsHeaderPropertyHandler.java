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
package org.rdkit.knime.properties;

import java.lang.reflect.Field;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.renderer.DataValueRendererFactory;
import org.knime.core.node.NodeLogger;
import org.rdkit.knime.headers.AbstractHeaderPropertyHandler;
import org.rdkit.knime.headers.HeaderProperty;
import org.rdkit.knime.headers.HeaderPropertyHandlerRegistry;
import org.rdkit.knime.nodes.RDKitNodePlugin;

/**
 * A Header Property Handler that knows how to render Fingerprint Spec values.
 * 
 * @author Manuel Schwarze
 */
public class FingerprintSettingsHeaderPropertyHandler extends AbstractHeaderPropertyHandler {

	//
	// Constants
	//

	/** The preference key for the renderer factory id that shall be used when rendering fingerprint information in a column. */
	public static final String PREF_KEY_RENDERER = "org.rdkit.knime.properties.FingerprintSpecHeaderPropertyHandler.renderer";

	/** The logging instance. */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(AbstractHeaderPropertyHandler.class);

	//
	// Constructors
	//

	/** The default constructor. */
	public FingerprintSettingsHeaderPropertyHandler() {
		super();
	}

	//
	// Public Methods
	//

	@Override
	public HeaderProperty createHeaderProperty(final DataColumnSpec colSpec) {
		return new FingerprintSettingsHeaderProperty(colSpec);
	}

	@Override
	public DataValueRenderer getPreferredRenderer(final HeaderProperty headerProperty) {
		DataValueRenderer renderer = null;

		// First check, if a renderer is set as the handler's preference
		final String strFactoryId = RDKitNodePlugin.getDefault().getPreferenceStore().getString(PREF_KEY_RENDERER);
		final DataValueRendererFactory rendererFactory = HeaderPropertyHandlerRegistry.getInstance().findDataValueRendererFactory(strFactoryId);

		if (rendererFactory != null) {
			try {
				renderer = rendererFactory.createRenderer(
						headerProperty.getColumnSpecForRendering());
			}
			catch (final Exception exc) {
				LOGGER.debug("Unable to create renderer for column header - trying default renderers.", exc);
			}
		}

		// If no renderer was found as explicit preference use the Data Type specific renderer (call super)
		return renderer == null ? super.getPreferredRenderer(headerProperty) : renderer;
	}

	/**
	 * Returns the InstanceScope object to work with preferences. As the implementation
	 * changed between different Eclipse versions, this utility method will try
	 * to keep the logic to get this object transparent from the caller.
	 * 
	 * @return Instance Scope.
	 */
	public static InstanceScope getInstanceScope() {
		InstanceScope scope = null;
		final Class<?> clazz = InstanceScope.class;

		// Try doing it the new way using a static singleton instance (introduced in Eclipse 3.7?)
		try {
			final Field fieldInstance = clazz.getDeclaredField("INSTANCE");
			if (fieldInstance != null && fieldInstance.getType() == clazz) {
				scope = (InstanceScope)fieldInstance.get(null);
			}
		}
		catch (final Exception exc) {
			// Ignored
		}

		// Do it the old way using a default constructor
		if (scope == null) {
			try {
				scope = (InstanceScope)clazz.getDeclaredConstructor().newInstance();
			}
			catch (final Exception exc) {
				LOGGER.error("Unable to create instance preference scope instance.");
			}
		}

		return scope;
	}

}