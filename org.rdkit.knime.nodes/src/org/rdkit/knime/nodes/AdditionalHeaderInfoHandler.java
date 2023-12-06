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
package org.rdkit.knime.nodes;

import javax.swing.table.TableCellRenderer;

import org.rdkit.knime.headers.HeaderPropertyHandler;

/**
 * A handler of additional header information knows how to render the
 * information value. Such a handler is called in two steps of the rendering
 * process. In the first step, the method {@link #prepareValue(String)} is
 * called to convert the information value, which is always a String due to the
 * KNIME column header properties specifications, into an object that a renderer
 * can understand. In the second step the handler must be able to produce a
 * renderer that can render the converted information object to a table cell.
 * Handlers can be registered in the class {@link AdditionalHeaderInfo}.
 * 
 * @author Manuel Schwarze
 * @deprecated Deprecated and replaced by an
 * 		extension point mechanism based on {@link HeaderPropertyHandler}.
 */
@Deprecated
public interface AdditionalHeaderInfoHandler {

	/**
	 * Returns the type that this handler can handle.
	 * 
	 * @return Type of additional header information. Must not be null.
	 */
	String getType();

	/**
	 * Convert the information value, which is always a String due to the KNIME
	 * column header properties specifications, into an object that the renderer
	 * returned by the method {@link #getRenderer()} can understand. If the
	 * passed in information is null, it should return null.
	 * 
	 * @param value Additional header information value. Can be null.
	 * 
	 * @return The converted value of a type that the renderer can understand.
	 * 		Should return null, if null was passed in.
	 */
	Object prepareValue(String value);

	/**
	 * Returns the appropriate renderer for the information of a specific type.
	 * 
	 * @return Renderer capable to render the object that is returned by
	 * 		a call to {@link #prepareValue(String)}.
	 */
	TableCellRenderer getRenderer();
}
