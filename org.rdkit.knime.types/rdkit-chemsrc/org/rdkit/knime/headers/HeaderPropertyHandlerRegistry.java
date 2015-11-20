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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.ExtensibleUtilityFactory;
import org.knime.core.data.renderer.DataValueRendererFactory;
import org.knime.core.node.NodeLogger;
import org.rdkit.knime.RDKitTypesPluginActivator;

/**
 * This utility class manages all handlers that can handle header properties
 * and visualize them in the RDKit Interactive View.
 * 
 * @author Manuel Schwarze
 */
public class HeaderPropertyHandlerRegistry {

	//
	// Constants
	//

	/** The logging instance. */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(HeaderPropertyHandlerRegistry.class);

	/** The extension point id of the handlers. */
	private static final String EXT_POINT_ID = "org.rdkit.knime.properties.HeaderPropertyHandler";

	/** The preference key stub to be used. */
	private static final String PREF_KEY = "org.rdkit.knime.headers.";

	/** The preference key that stores a semicolon separated list of handlers for tooltip renderings. */
	public static final String PREF_KEY_HANDLERS_RENDERING_HEADERS = PREF_KEY + "handlers.headers";

	/** The preference key that stores a semicolon separated list of handlers for tooltip renderings. */
	public static final String PREF_KEY_HANDLERS_RENDERING_TOOLTIPS = PREF_KEY + "handlers.tooltips";

	/** The preference key that stores a semicolon separated list of handlers that are disabled. */
	public static final String PREF_KEY_HANDLERS_DISABLED = PREF_KEY + "handlers.disabled";

	//
	// Globals
	//

	/** The singleton instance of the library. */
	private static HeaderPropertyHandlerRegistry g_instance= null;

	//
	// Members
	//

	/** Map that stores handlers based on their IDs. */
	private final Map<String, HeaderPropertyHandler> m_mapIdToHandler;

	/** Map that stores handlers based on the properties they support. */
	private final Map<String, List<HeaderPropertyHandler>> m_mapPropToHandler;

	/** Map that stores renderer factories based on their ID for fast access. */
	private final Map<String, DataValueRendererFactory> m_mapIdToRendererFactory;

	//
	// Constructors
	//

	private HeaderPropertyHandlerRegistry() {
		m_mapIdToHandler = new LinkedHashMap<String, HeaderPropertyHandler>();
		m_mapPropToHandler = new LinkedHashMap<String, List<HeaderPropertyHandler>>();
		m_mapIdToRendererFactory = new LinkedHashMap<String, DataValueRendererFactory>();

		createHandlers();
	}

	/**
	 * Returns all found header property handlers available in the registry.
	 * 
	 * @return Array with handlers or null, if none is registered.
	 */
	public HeaderPropertyHandler[] getAllHeaderPropertyHandlers() {
		final List<HeaderPropertyHandler> list = new ArrayList<HeaderPropertyHandler>();

		for (final String strKey : m_mapIdToHandler.keySet()) {
			list.add(m_mapIdToHandler.get(strKey));
		}

		return (list.isEmpty() ? null : list.toArray(new HeaderPropertyHandler[list.size()]));
	}

	/**
	 * Returns all header property handlers available in the registry for the passed
	 * in IDs in the very order as past in.
	 * 
	 * @param arrHandlerIds Array with IDs. Can be null.
	 * 
	 * @return Array with handlers or null, if none is found for the specified IDs.
	 */
	public HeaderPropertyHandler[] getHeaderPropertyHandlers(final String[] arrHandlerIds) {
		final List<HeaderPropertyHandler> list = new ArrayList<HeaderPropertyHandler>();

		if (arrHandlerIds != null && arrHandlerIds.length > 0) {
			for (final String strKey : arrHandlerIds) {
				final HeaderPropertyHandler handler = m_mapIdToHandler.get(strKey);
				if (handler != null) {
					list.add(handler);
				}
			}
		}

		return (list.isEmpty() ? null : list.toArray(new HeaderPropertyHandler[list.size()]));
	}

	/**
	 * Returns all header property handlers available in the registry for the passed
	 * in IDs that are represented as a semicolon separated list.
	 * 
	 * @param strHandlerIdList String with semicolon separated list with IDs. Can be null.
	 * 
	 * @return Array with handlers or null, if none is found for the specified IDs.
	 */
	public HeaderPropertyHandler[] getHeaderPropertyHandlers(
			final String strHandlerIdList) {
		return getHeaderPropertyHandlers(strHandlerIdList == null ? null :
			strHandlerIdList.split(";"));
	}

	/**
	 * Returns all header property handlers that are capable of handling the
	 * specified property.
	 * 
	 * @param strProperty
	 * 
	 * @return
	 */
	public HeaderPropertyHandler[] getHeaderPropertyHandlersForProperty(final String strProperty) {
		final List<HeaderPropertyHandler> list = m_mapPropToHandler.get(strProperty);
		return (list == null ? new HeaderPropertyHandler[0] :
			list.toArray(new HeaderPropertyHandler[list.size()]));
	}

	/**
	 * Returns all header property handlers that should be involved in rendering
	 * the column header of the specified column specification.
	 * A handler will only appear in the array, if the column contains properties that can
	 * be rendered (a handler is registered) and if the representation mode is set
	 * to HeaderAndTooltip or HeaderOnly. Every handler is only added once to the
	 * set of handlers.
	 * 
	 * @param colSpec Data column specification with properties to be evaluated for rendering.
	 * @param strHandlerIdList String with semicolon separated list with IDs. Can be null.
	 * 
	 * @return An ordered array of handlers that shall be invoked for rendering properties.
	 * 		Returns null, if no handler is needed.
	 */
	public HeaderPropertyHandler[] getHeaderPropertyHandlersForColumn(
			final DataColumnSpec colSpec, final String strHandlerIdList) {
		return getHeaderPropertyHandlersForColumn(colSpec,
				strHandlerIdList == null ? null : strHandlerIdList.split(";"));
	}

	/**
	 * Returns all header property handlers that should be involved in rendering
	 * the column header of the specified column specification.
	 * A handler will only appear in the array, if the column contains properties that can
	 * be rendered (a handler is registered) and if the representation mode is set
	 * to HeaderAndTooltip or HeaderOnly. Every handler is only added once to the
	 * set of handlers.
	 * 
	 * @param colSpec Data column specification with properties to be evaluated for rendering.
	 * @param handlerIds Ordered array of handlers that shall be considered for rendering.
	 * 		The result will be a subset of them or null. Can be null to try to use
	 * 		all registered handlers.
	 * 
	 * @return An ordered array of handlers that shall be invoked for rendering properties.
	 * 		Returns null, if no handler is needed.
	 */
	public HeaderPropertyHandler[] getHeaderPropertyHandlersForColumn(
			final DataColumnSpec colSpec, final String[] arrHandlerIds) {
		final Set<HeaderPropertyHandler> setHandlers = new LinkedHashSet<HeaderPropertyHandler>();

		final HeaderPropertyHandler[] arrHandlers = (arrHandlerIds == null ?
				getAllHeaderPropertyHandlers() : getHeaderPropertyHandlers(arrHandlerIds));

		if (arrHandlers != null && arrHandlers.length > 0) {
			for (final HeaderPropertyHandler handler : arrHandlers) {
				if (HeaderPropertyUtils.existOneProperty(colSpec,
						handler.getAcceptableProperties())) {
					setHandlers.add(handler);
				}
			}
		}

		return (setHandlers.isEmpty() ? null : setHandlers.toArray(new HeaderPropertyHandler[setHandlers.size()]));
	}

	/**
	 * Returns the header property handler with the specified ID or null, if not found.
	 * 
	 * @param strId
	 * 
	 * @return Header property handler with the specified ID or null.
	 */
	public HeaderPropertyHandler getHeaderPropertyHandlerById(final String strId) {
		return m_mapIdToHandler.get(strId);
	}

	/**
	 * Creates a default list string of handler IDs that shall be involved in rendering column
	 * headers. This information is derived from all default representation modes of
	 * the registered handlers. The order is arbitrary.
	 * 
	 * @return List.
	 */
	public String getDefaultColumnHeaderRenderers() {
		final StringBuilder sb = new StringBuilder();

		final HeaderPropertyHandler[] arrHandlers = getAllHeaderPropertyHandlers();

		if (arrHandlers != null && arrHandlers.length > 0) {
			for (final HeaderPropertyHandler handler : arrHandlers) {
				switch (handler.getDefaultRepresentationMode()) {
				case HeaderAndTooltip:
				case HeaderOnly:
					if (sb.length() > 0) {
						sb.append(";");
					}
					sb.append(handler.getId());
					break;
	
				case Disabled:
				case TooltipOnly:
					break;
				}
			}
		}

		return sb.toString();
	}

	/**
	 * Returns the current list string of handler IDs that shall be involved in rendering column
	 * headers. This information is taken from the preference store.
	 * 
	 * @return List.
	 */
	public String getColumnHeaderRenderers() {
		return RDKitTypesPluginActivator.getDefault().
				getPreferenceStore().getString(PREF_KEY_HANDLERS_RENDERING_HEADERS);
	}

	/**
	 * Creates a default list string of handler IDs that shall be involved in rendering column
	 * tooltips. This information is derived from all default representation modes of
	 * the registered handlers. The order is arbitrary.
	 * 
	 * @return List.
	 */
	public String getDefaultColumnTooltipRenderers() {
		final StringBuilder sb = new StringBuilder();

		final HeaderPropertyHandler[] arrHandlers = getAllHeaderPropertyHandlers();

		if (arrHandlers != null && arrHandlers.length > 0) {
			for (final HeaderPropertyHandler handler : arrHandlers) {
				switch (handler.getDefaultRepresentationMode()) {
				case HeaderAndTooltip:
				case TooltipOnly:
					if (sb.length() > 0) {
						sb.append(";");
					}
					sb.append(handler.getId());
					break;
					
				case Disabled:
				case HeaderOnly:
					break;
				}
			}
		}

		return sb.toString();
	}

	/**
	 * Returns the current list string of handler IDs that shall be involved in rendering column
	 * tooltips. This information is taken from the preference store.
	 * 
	 * @return List.
	 */
	public String getColumnTooltipRenderers() {
		return RDKitTypesPluginActivator.getDefault().
				getPreferenceStore().getString(PREF_KEY_HANDLERS_RENDERING_TOOLTIPS);
	}

	/**
	 * Creates a default list string of handler IDs that shall NOT be involved in rendering column
	 * headers and tooltips. This information is derived from all default representation modes of
	 * the registered handlers. The order is arbitrary.
	 * 
	 * @return List.
	 */
	public String getDefaultDisabledColumnRenderers() {
		final StringBuilder sb = new StringBuilder();

		final HeaderPropertyHandler[] arrHandlers = getAllHeaderPropertyHandlers();

		if (arrHandlers != null && arrHandlers.length > 0) {
			for (final HeaderPropertyHandler handler : arrHandlers) {
				switch (handler.getDefaultRepresentationMode()) {
				case Disabled:
					if (sb.length() > 0) {
						sb.append(";");
					}
					sb.append(handler.getId());
					break;
					
				case HeaderAndTooltip:
				case HeaderOnly:
				case TooltipOnly:
					break;
				}
			}
		}

		return sb.toString();
	}

	/**
	 * Returns the current list string of handler IDs that are disabled.
	 * This information is taken from the preference store.
	 * 
	 * @return List.
	 */
	public String getDisabledColumnRenderers() {
		return RDKitTypesPluginActivator.getDefault().
				getPreferenceStore().getString(PREF_KEY_HANDLERS_DISABLED);
	}

	/**
	 * Tries to find the data value renderer factory with the specified ID. For
	 * fast access it will cache the information.
	 * 
	 * @param strFactoryId Factory ID to search for. Can be null to return null.
	 * 
	 * @return Data value renderer factory or null, if not found.
	 */
	public DataValueRendererFactory findDataValueRendererFactory(final String strFactoryId) {
		DataValueRendererFactory rendererFactory = null;

		if (strFactoryId != null && !strFactoryId.trim().isEmpty()) {
			// First look in cache
			rendererFactory = m_mapIdToRendererFactory.get(strFactoryId);

			// If not found in cache look through all known factories
			if (rendererFactory == null) {
				for (final ExtensibleUtilityFactory facUtility : ExtensibleUtilityFactory.getAllFactories()) {
					for (final DataValueRendererFactory facRenderer : facUtility.getAvailableRenderers()) {
						if (strFactoryId.equals(facRenderer.getId())) {
							rendererFactory = facRenderer;
							break;
						}
					}

					if (rendererFactory != null) {
						break;
					}
				}

				// If found, store it in cache for later
				m_mapIdToRendererFactory.put(strFactoryId, rendererFactory);
			}
		}

		return rendererFactory;
	}

	//
	// Private Methods
	//

	/**
	 * Loads all extension point implementations of Header Property Handlers.
	 */
	private void createHandlers() {
		// Create all handlers from extension points
		final IExtensionRegistry registry = Platform.getExtensionRegistry();
		final IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
		final IExtension[] arrExtensions = point.getExtensions();
		for (final IExtension ext : arrExtensions) {
			final IConfigurationElement[] elements = ext.getConfigurationElements();
			for (final IConfigurationElement handlerElement : elements) {
				if ("handler".equals(handlerElement.getName())) {
					try {
						final HeaderPropertyHandler handler =
								(HeaderPropertyHandler)handlerElement
								.createExecutableExtension("class");

						m_mapIdToHandler.put(handler.getId(), handler);
						for (final String strProp : handler.getAcceptableProperties()) {
							List<HeaderPropertyHandler> list =
									m_mapPropToHandler.get(strProp);
							if (list == null) {
								list = new ArrayList<HeaderPropertyHandler>();
								m_mapPropToHandler.put(strProp,  list);
							}
							if (!list.contains(handler)) {
								list.add(handler);
							}
						}
					}
					catch (final CoreException ex) {
						LOGGER.error("Could not load registered Header Property Handler '"
								+ handlerElement.getAttribute("displayName") + "' with class "
								+ handlerElement.getAttribute("class")
								+ " from plug-in " + handlerElement.getNamespaceIdentifier() + ": "
								+ ex.getMessage(), ex);
					}
				}
			}
		}
	}

	//
	// Static Public Methods
	//

	/**
	 * Returns the singleton instance of the registry. If it does not
	 * exist yet it will be created and filled with information about
	 * available extension points.
	 * 
	 * @return Registry instance.
	 */
	public static synchronized HeaderPropertyHandlerRegistry getInstance() {
		if (g_instance == null) {
			g_instance = new HeaderPropertyHandlerRegistry();
		}

		return g_instance;
	}
}
