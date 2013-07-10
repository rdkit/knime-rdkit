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
package org.rdkit.knime.util;

import org.knime.core.data.DataValue;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

/**
 * Provides a standard component for a dialog that allows to select a column in
 * a given {@link org.knime.core.data.DataTableSpec}. Provides label and list
 * (possibly filtered by a given {@link org.knime.core.data.DataCell} type) as
 * well as functionality to load/store into a settings model.
 * The column name selection list will provide a RowID option if the provided
 * settings model object is an instance of {@link SettingsModelColumnName} which
 * provides the additional method <code>useRowID</code> to check if the
 * RowID was selected.<br>
 * This derived implementation handles compatibility of molecule types
 * properly when filtering column data types. All types that are adaptable
 * to RDKit Mol Cells are also accepted, if the RDKit Mol Cell type is accepted.
 *
 * @author Manuel Schwarze
 */
public class DialogComponentColumnNameSelection extends
org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection {

	//
	// Constructors
	//

	/**
	 * Constructor that puts label and combobox into the panel.
	 *
	 * @param model the model holding the value of this component. If the model
	 * is an instance of {@link SettingsModelColumnName} a RowID option is
	 * added to the select list.
	 * @param label label for dialog in front of checkbox
	 * @param specIndex index of (input) port listing available columns
	 * @param isRequired true, if the component should throw an exception in
	 *            case of no available compatible column, false otherwise.
	 * @param addNoneCol true, if a none option should be added to the column
	 * list
	 * @param classFilter which classes are available for selection
	 */
	public DialogComponentColumnNameSelection(final SettingsModelString model,
			final String label, final int specIndex, final boolean isRequired,
			final boolean addNoneCol, final Class<? extends DataValue>... classFilter) {
		super(model, label, specIndex, isRequired, addNoneCol, RDKitAdapterCellSupport.expandByAdaptableTypes(classFilter));
	}

	/**
	 * Constructor that puts label and combobox into the panel.
	 *
	 * @param model the model holding the value of this component. If the model
	 * is an instance of {@link SettingsModelColumnName} a RowID option is
	 * added to the select list.
	 * @param label label for dialog in front of checkbox
	 * @param specIndex index of (input) port listing available columns
	 * @param isRequired true, if the component should throw an exception in
	 *            case of no available compatible column, false otherwise.
	 * @param addNoneCol true, if a none option should be added to the column
	 * list
	 * @param columnFilter {@link ColumnFilter}. The combo box
	 *            will allow to select only columns compatible with the
	 *            column filter. All other columns will be ignored.
	 */
	public DialogComponentColumnNameSelection(final SettingsModelString model,
			final String label, final int specIndex, final boolean isRequired,
			final boolean addNoneCol, final ColumnFilter columnFilter) {
		super(model, label, specIndex, isRequired, addNoneCol, RDKitAdapterCellSupport.expandByAdaptableTypes(columnFilter));
	}

	/**
	 * Constructor that puts label and combobox into the panel.
	 *
	 * @param model the model holding the value of this component. If the model
	 * is an instance of {@link SettingsModelColumnName} a RowID option is
	 * added to the select list.
	 * @param label label for dialog in front of checkbox
	 * @param specIndex index of (input) port listing available columns
	 * @param isRequired true, if the component should throw an exception in
	 *            case of no available compatible column, false otherwise.
	 * @param classFilter which classes are available for selection
	 */
	public DialogComponentColumnNameSelection(final SettingsModelString model,
			final String label, final int specIndex, final boolean isRequired,
			final Class<? extends DataValue>... classFilter) {
		super(model, label, specIndex, isRequired, RDKitAdapterCellSupport.expandByAdaptableTypes(classFilter));
	}

	/**
	 * Constructor that puts label and combobox into the panel.
	 *
	 * @param model the model holding the value of this component. If the model
	 * is an instance of {@link SettingsModelColumnName} a RowID option is
	 * added to the select list.
	 * @param label label for dialog in front of checkbox
	 * @param specIndex index of (input) port listing available columns
	 * @param isRequired true, if the component should throw an exception in
	 *            case of no available compatible column, false otherwise.
	 * @param columnFilter {@link ColumnFilter}. The combo box
	 *            will allow to select only columns compatible with the
	 *            column filter. All other columns will be ignored.
	 */
	public DialogComponentColumnNameSelection(final SettingsModelString model,
			final String label, final int specIndex, final boolean isRequired,
			final ColumnFilter columnFilter) {
		super(model, label, specIndex, isRequired, RDKitAdapterCellSupport.expandByAdaptableTypes(columnFilter));
	}

	/**
	 * Constructor that puts label and combobox into the panel. The dialog will
	 * not open until the incoming table spec contains a column compatible to
	 * one of the specified {@link DataValue} classes.
	 *
	 * @param model the model holding the value of this component. If the model
	 * is an instance of {@link SettingsModelColumnName} a RowID option is
	 * added to the select list.
	 * @param label label for dialog in front of checkbox
	 * @param specIndex index of (input) port listing available columns
	 * @param classFilter which classes are available for selection
	 */
	public DialogComponentColumnNameSelection(final SettingsModelString model,
			final String label, final int specIndex,
			final Class<? extends DataValue>... classFilter) {
		super(model, label, specIndex, RDKitAdapterCellSupport.expandByAdaptableTypes(classFilter));
	}

	/**
	 * Constructor that puts label and combobox into the panel. The dialog will
	 * not open until the incoming table spec contains a column compatible to
	 * one of the specified {@link DataValue} classes.
	 *
	 * @param model the model holding the value of this component. If the model
	 * is an instance of {@link SettingsModelColumnName} a RowID option is
	 * added to the select list.
	 * @param label label for dialog in front of checkbox
	 * @param specIndex index of (input) port listing available columns
	 * @param columnFilter {@link ColumnFilter}. The combo box
	 *            will allow to select only columns compatible with the
	 *            column filter. All other columns will be ignored.
	 */
	public DialogComponentColumnNameSelection(final SettingsModelString model,
			final String label, final int specIndex, final ColumnFilter columnFilter) {
		super(model, label, specIndex, RDKitAdapterCellSupport.expandByAdaptableTypes(columnFilter));
	}

}
