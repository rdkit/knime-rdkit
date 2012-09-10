/* 
 * This source code, its documentation and all related files
 * are protected by copyright law. All rights reserved.
 *
 * (C)Copyright 2011 by Novartis Pharma AG 
 * Novartis Campus, CH-4002 Basel, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 */
package org.rdkit.knime.nodes;

import javax.swing.table.TableCellRenderer;

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
 */
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
