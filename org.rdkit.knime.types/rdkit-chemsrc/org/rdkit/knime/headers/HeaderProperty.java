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
package org.rdkit.knime.headers;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataValue;

/**
 * A header property encapsulates one or more values and information
 * that can be stored in the properties that are part of a data colun
 * specification. Basically, it provides functionality to write
 * these values into a column specification and to read them again
 * from the specification to make them easily available. Header
 * properties are handed over to normal renderers to be drawn into
 * table headers. Some renderers expect a DataCell object, which might
 * be architecturally not 100% correct, but as we do not have control
 * we recommend that a concrete HeaderProperty derives from DataCell
 * to fulfill this expectation.
 * 
 * @author Manuel Schwarze
 */
public interface HeaderProperty extends DataValue {

	/**
	 * Reads properties from the data column specification and
	 * makes them available for getter methods.
	 * 
	 * @param colSpec Data column specification. Can be null
	 * 		to do nothing.
	 */
	public void readFromColumnSpec(final DataColumnSpec colSpec);

	/**
	 * Writes properties from the value object (most likely
	 * changed before with setter methods) into the data
	 * column specification creator object.
	 * 
	 * @param colSpec Data column specification creator. Can be null
	 * 		to do nothing.
	 */
	public void writeToColumnSpec(final DataColumnSpecCreator colSpecCreator);

	/**
	 * Resets all values in this data object to its initial values.
	 */
	public void reset();

	/**
	 * Determines, if the properties that are currently held
	 * in the value object are the same as in the past in
	 * data column specification.
	 * 
	 * @param colSpec Data column specification. Can be null
	 * 		to return false.
	 */
	public boolean equals(final DataColumnSpec colSpec);

	/**
	 * The returned spec must be of a data type that is compatible with
	 * the Header Property's data value (e.g. SmilesCell for SmilesValue).
	 * It can be used when the renderer is created to render the header
	 * of a column. IT IS NOT THE COLUMN SPECIFICATION THAT WAS USED
	 * TO CREATE HEADER PROPERTY!!! It may return null, but this may
	 * have side effects when the renderer gets created if it cannot deal
	 * with a null value.
	 * 
	 * @return
	 */
	public DataColumnSpec getColumnSpecForRendering();
}
