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
package org.rdkit.knime.headers;

import java.awt.Dimension;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.ExtensibleUtilityFactory;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.renderer.DataValueRendererFactory;
import org.knime.core.node.NodeLogger;
import org.rdkit.knime.RDKitTypesPluginActivator;

/**
 * This class implements basic functionality needed in all
 * Header Property Handler implementations.
 * 
 * @author Manuel Schwarze
 */
public abstract class AbstractHeaderPropertyHandler implements HeaderPropertyHandler {

	//
	// Constants
	//

	/** The logging instance. */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(AbstractHeaderPropertyHandler.class);

	//
	// Members
	//

	/** The handler id. */
	private String m_strId;

	/** The display name. */
	private String m_strDisplayName;

	/** The data value class. */
	private Class<? extends DataValue> m_dataValueClass;

	/** The default representation mode. */
	private RepresentationMode m_defaultMode;

	/** The set of acceptable properties that can be handled. */
	Set<String> m_setProperties;

	//
	// Constructors
	//

	/**
	 * The default constructor, which gets called from the extension point
	 * initializer. Member variables are filled when the method
	 * {@link #setInitializationData(IConfigurationElement, String, Object)}
	 * is gettsing called afterwards.
	 */
	public AbstractHeaderPropertyHandler() {
		m_strId = "Uninitialized ID";
		m_strDisplayName = "Uninitialized Header Property Handler";
		m_defaultMode = RepresentationMode.Disabled;
		m_dataValueClass = null;
		m_setProperties = new LinkedHashSet<String>();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setInitializationData(final IConfigurationElement config,
			final String propertyName, final Object data) throws CoreException {
		try {
			m_strId = config.getAttribute("id");
			m_strDisplayName = config.getAttribute("displayName");

			// Determine DataValue class
			final String strDataValueClassName =
					config.getChildren("dataValue")[0].getAttribute("valueClass");
			try {
				final Class<?> clazz = Class.forName(strDataValueClassName);
				if (DataValue.class.isAssignableFrom(clazz)) {
					m_dataValueClass = (Class<? extends DataValue>)clazz;
				}
				else {
					m_dataValueClass = null;
					LOGGER.error("Header Property Handler '" + getDisplayName() +
							"' uses the DataValue class '" + strDataValueClassName +
							"', which is invalid. The handler cannot be used.");
				}
			}
			catch (final ClassNotFoundException e) {
				m_dataValueClass = null;
				LOGGER.error("Header Property Handler '" + getDisplayName() +
						"' uses the DataValue class '" + strDataValueClassName +
						"', which could not be found. The handler cannot be used.");
			}

			// Determine default representation mode
			final String strDefaultRepresentation =
					config.getChildren("defaultRepresentation")[0].getAttribute("mode");
			try {
				m_defaultMode = RepresentationMode.valueOf(strDefaultRepresentation);
			}
			catch (final Exception exc) {
				m_defaultMode = RepresentationMode.Disabled;
				LOGGER.warn("Header Property Handler '" + getDisplayName() +
						"' uses as default representation mode '" + strDefaultRepresentation +
						"', which is invalid. The handler will be disabled by default.");
			}

			// Determine acceptable properties
			final Set<String> setProps = new LinkedHashSet<String>();
			for (final IConfigurationElement elemProp :
				config.getChildren("acceptableProperties")[0].getChildren()) {
				setProps.add(elemProp.getAttribute("key"));
			}
			m_setProperties = setProps;

			// Post-checks
			if (m_strId == null) {
				throw new IllegalArgumentException("Unique id must not be null.");
			}
			if (m_strDisplayName == null) {
				throw new IllegalArgumentException("Display name must not be null.");
			}
			if (m_setProperties.size() < 1) {
				throw new IllegalArgumentException("The handler does not handle any properies.");
			}
		}
		catch (final Exception exc) {
			final Status status = new Status(IStatus.ERROR, RDKitTypesPluginActivator.PLUGIN_ID,
					"The Header Property Handler '" +
							this.getClass().getName() + "' failed to initialize.",
							exc);
			throw new CoreException(status);
		}
	}

	@Override
	public String getId() {
		return m_strId;
	}

	@Override
	public String getDisplayName() {
		return m_strDisplayName;
	}

	@Override
	public String[] getAcceptableProperties() {
		return m_setProperties.toArray(new String[m_setProperties.size()]);
	}

	@Override
	public Class<? extends DataValue> getDataValueClass() {
		return m_dataValueClass;
	}

	@Override
	public RepresentationMode getDefaultRepresentationMode() {
		return m_defaultMode;
	}

	@Override
	public synchronized DataValueRendererFactory[] getRendererFactories() {
		Collection<DataValueRendererFactory> list = null;

		if (m_dataValueClass != null) {
			try {
				final DataValue.UtilityFactory fac =
						(DataValue.UtilityFactory)m_dataValueClass.getField("UTILITY").get(null);
				if (fac instanceof ExtensibleUtilityFactory) {
					list = ((ExtensibleUtilityFactory)fac).getAvailableRenderers();
				}
			}
			catch (final Exception exc) {
				m_dataValueClass = null;
				LOGGER.error("Header Property Handler '" + getDisplayName() +
						"' uses the DataValue class '" + m_dataValueClass.getName() +
						"' with no access to renderers. The handler cannot be used.", exc);
			}
		}

		return list == null ? new DataValueRendererFactory[0] :
			list.toArray(new DataValueRendererFactory[list.size()]);
	}

	@Override
	public DataValueRenderer getPreferredRenderer(final HeaderProperty headerProperty) {
		DataValueRenderer renderer = null;
		DataValueRendererFactory rendererFactory = null;

		// If no renderer is set as preference use the renderer that is set in the KNIME preferences for the Data Type
		if (m_dataValueClass != null && headerProperty != null) {
			try {
				final DataValue.UtilityFactory fac =
						(DataValue.UtilityFactory)m_dataValueClass.getField("UTILITY").get(null);
				if (fac instanceof ExtensibleUtilityFactory) {
					rendererFactory = ((ExtensibleUtilityFactory)fac).getPreferredRenderer();
					if (rendererFactory == null) {
						rendererFactory = ((ExtensibleUtilityFactory)fac).getDefaultRenderer();
					}
				}
			}
			catch (final Exception exc) {
				m_dataValueClass = null;
				LOGGER.error("Header Property Handler '" + getDisplayName() +
						"' uses the DataValue class '" + m_dataValueClass.getName() +
						"' with no access to renderers. The handler cannot be used.", exc);
			}
		}

		if (rendererFactory != null) {
			try {
				renderer = rendererFactory.createRenderer(
						headerProperty.getColumnSpecForRendering());
			}
			catch (final Exception exc) {
				LOGGER.debug("Unable to create renderer for column header: ", exc);
			}
		}
		else {
			LOGGER.debug("No renderer factory found for column header property handler " + getId());
		}

		return renderer;
	}

	@Override
	public Dimension getPreferredDimension(final HeaderProperty headerProperty) {
		final Dimension dimPref = null;
		final DataValueRenderer renderer = getPreferredRenderer(headerProperty);

		if (renderer != null) {
			renderer.getRendererComponent(headerProperty).getPreferredSize();
		}

		return dimPref;
	}

	@Override
	public boolean isCompatible(final DataColumnSpec colSpec) {
		return getDataValueClass() != null && !getAcceptableProperties(colSpec).isEmpty();
	}

	@Override
	public abstract HeaderProperty createHeaderProperty(final DataColumnSpec colSpec);

	//
	// Protected Methods
	//

	/**
	 * Extracts from the specified column specification all properties
	 * that can be handled by this handler. Such a property must have
	 * a key that is contained in the list of acceptable properties
	 * and the value must not be null and not empty after trimming.
	 * Otherwise it will not be added to the map that gets returned.
	 * 
	 * @param colSpec Column specification with properties.
	 * 		Can be null.
	 * 
	 * @return Map with properties that can be handled by this handler.
	 */
	protected Map<String, String> getAcceptableProperties(final DataColumnSpec colSpec) {
		final Map<String, String> mapProps = new HashMap<String, String>();

		if (colSpec != null) {
			final DataColumnProperties props = colSpec.getProperties();
			if (props != null) {
				for (final String strProp : m_setProperties) {
					if (props.containsProperty(strProp)) {
						final String strValue =  props.getProperty(strProp);
						if (strValue != null && !strValue.trim().isEmpty()) {
							mapProps.put(strProp, strValue);
						}
					}
				}
			}
		}

		return mapProps;
	}

}
