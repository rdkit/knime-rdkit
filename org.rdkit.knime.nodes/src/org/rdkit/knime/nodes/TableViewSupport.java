/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2012
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
