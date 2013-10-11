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

import org.eclipse.core.runtime.IExecutableExtension;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.renderer.DataValueRendererFactory;

/**
 * A header property handler is capable of rendering properties contained
 * in a column header. For this purpose the string value of one or more
 * properties is evaluated and converted into a DataValue object. That
 * object will get rendered in the RDKit Interactive View.
 * 
 * @author Manuel Schwarze
 */
public interface HeaderPropertyHandler extends IExecutableExtension {

	/**
	 * Returns the unique id of this handler.
	 * 
	 * @return Unique ID. Must not be null.
	 */
	String getId();

	/**
	 * Returns the display name of this handler.
	 * 
	 * @return Display name. Must not be null.
	 */
	String getDisplayName();

	/**
	 * Returns the property keys that this handler is capable
	 * of processing.
	 * 
	 * @return Acceptable properties. Never null, at least one element.
	 */
	String[] getAcceptableProperties();

	/**
	 * Returns the data value class that is used to visualize
	 * the property or properties.
	 * 
	 * @return Data value class. Can be null, if data value
	 * 		class could not be found (if the implementing
	 * 		plug-in is not installed.
	 */
	Class<? extends DataValue> getDataValueClass();

	/**
	 * Returns the default representation mode to be used for this handler.
	 * 
	 * @return Default representation mode. Never null.
	 */
	RepresentationMode getDefaultRepresentationMode();

	/**
	 * Returns alle renderer factories that are able to
	 * render the data value representation of the property.
	 * 
	 * @return Renderer factory. Can be empty, but never null.
	 */
	DataValueRendererFactory[] getRendererFactories();

	/**
	 * Returns the render set in the preferences to be used
	 * for visualizing the data value of the property.
	 * 
	 * @param headerProperty The header property to get a renderer for.
	 * 		It is passed in to call the method {@link HeaderProperty#getColumnSpecForRendering()}
	 * 		in order to get special information for rendering, if possible.
	 * 
	 * @return Renderer to be used, if user did not
	 * 		select a special one for a column.
	 */
	DataValueRenderer getPreferredRenderer(HeaderProperty headerProperty);

	/**
	 * Returns the preferred dimension that shall be used when
	 * visualizing the data value of the property.
	 * 
	 * @param headerProperty The header property to determine the preferred dimension for.
	 * 
	 * @return The preferred dimension. Can be null, if
	 * 		there is nothing to be rendered.
	 */
	Dimension getPreferredDimension(HeaderProperty headerProperty);

	/**
	 * Determines, if this handler is capable of handling properties
	 * that are contained in the specified column specification.
	 * 
	 * @param colSpec Column specification with properties. Can be null to
	 * 		return false.
	 *
	 * @return True, if theser are properties present that could be handled
	 * 		by this handler. False otherwise.
	 */
	boolean isCompatible(DataColumnSpec colSpec);

	/**
	 * Creates based on a property or properties found in the specified
	 * column specification a header property object, which is at the same
	 * time a data value object that will be passed to
	 * the renderer later for rendering it into a header or tooltip.
	 * 
	 * @param colSpec Column specification with properties. Can be null to
	 * 		return null.
	 * 
	 * @return Data value or null, if it could not be created.
	 */
	HeaderProperty createHeaderProperty(DataColumnSpec colSpec);
}
