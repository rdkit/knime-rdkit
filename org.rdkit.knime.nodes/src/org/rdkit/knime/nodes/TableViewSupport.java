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

import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.tableview.TableContentModel;

/**
 * This interface must be implemented by node models when we want to be able to 
 * connect an interactive view with it. It provides functionality to
 * restore saved table data when a workflow loads ({@link BufferedDataTableHolder})
 * and also the functionality to generate a content model a view can work on.
 * 
 * @author Manuel Schwarze
 */
public interface TableViewSupport extends BufferedDataTableHolder {
	
	/**
	 * Returns the list of indices of input tables, which shall be used in
	 * table views.
	 * 
	 * @return List of indices of input tables or empty array, if nothing shall
	 * 		be conserved.
	 */
	int[] getInputTablesToConserve();
	
	/**
	 * Returns the list of indices of output tables, which shall be used in
	 * table views.
	 * 
	 * @return List of indices of output tables or empty array, if nothing shall
	 * 		be conserved.
	 */
	int[] getOutputTablesToConserve();
	
	/**
	 * Returns the content model of table data to be used in a view.
	 * 
	 * @param bIsInputTable Set to true, if the passed in index is from an input table.
	 * 		Set to false, if the passed in index is from an output table.
	 * @param iIndex Index of a port (table). 
	 * 
	 * @return Table content model or null, if unavailable.
	 * 
	 * @see #getInputTablesToConserve()
	 * @see #getOutputTablesToConserve()
	 */
	TableContentModel getContentModel(boolean bIsInputTable, int iIndex);
}
