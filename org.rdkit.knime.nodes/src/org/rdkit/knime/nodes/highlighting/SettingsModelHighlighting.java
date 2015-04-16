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
package org.rdkit.knime.nodes.highlighting;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.rdkit.knime.nodes.highlighting.HighlightingDefinition.Type;
import org.rdkit.knime.util.LayoutUtils;
import org.rdkit.knime.util.SettingsUtils;

/**
 * This settings model stores all configured highlighting definitions. It
 * contains the logic how to load and save these settings and provides table
 * model interfaces to present these values to the user in a table in the
 * settings dialog.
 * 
 * @author Manuel Schwarze
 */
public class SettingsModelHighlighting extends SettingsModel implements
TableModel {

	//
	// Constants
	//

	/** The logger instance. */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(SettingsModelHighlighting.class);

	/** Defines icon to be used for unsupported features of a model. Used in Info tab. */
	protected static final Icon DELETE_ICON = LayoutUtils.createImageIcon(SettingsModelHighlighting.class,
			"/org/rdkit/knime/nodes/highlighting/delete.png", "Delete");

	/** Column index of the active flag of the highlighting definition. */
	public static final int COLUMN_ACTIVE = 0;

	/** Column index of the type of the input column. */
	public static final int COLUMN_TYPE = 1;

	/** Column index of the input column. */
	public static final int COLUMN_INPUT_COLUMN_NAME = 2;

	/** Column index of the color to be used for highlighting. */
	public static final int COLUMN_COLOR = 3;

	/**
	 * Column index of the flag to determine to include neighborhood in
	 * highlighting.
	 */
	public static final int COLUMN_NEIGHBORHOOD = 4;

	/**
	 * Column index for the delete action (not part of settings).
	 */
	public static final int COLUMN_DELETE_ACTION = 5;

	/** Column header names. */
	public static final String[] COLUMN_HEADERS = { "Active", "Type",
		"Column with indexes", "Color", "Neighborhood", "" };

	/** Column value classes used to determine renderers and editors. */
	public static final Class<?>[] COLUMN_CLASSES = { Boolean.class,
		Type.class, String.class, Color.class, Boolean.class, ImageIcon.class };

	/** Column modify information. */
	public static final boolean[] COLUMN_EDITABLE = { true, true, true, true,
		true, true };

	/**
	 * The column title used for an undefined column. Must match the definition in the class
	 * ColumnSelectionPanel of KNIME.
	 */
	public static final String TITLE_UNDEFINED_COLUMN = "<none>";

	/**
	 * The column spec used for an undefined column. Must match the definition in the class
	 * ColumnSelectionPanel of KNIME.
	 */
	public static final DataColumnSpec UNDEFINED_COLUMN =
			new DataColumnSpecCreator(TITLE_UNDEFINED_COLUMN, DataType.getMissingCell().getType()).createSpec();

	//
	// Members
	//

	/** The key used to store the enumeration value in the settings. */
	private final String m_strConfigName;

	/**
	 * The current list of Highlighting definitions. Can be empty, but never
	 * null.
	 */
	private final List<HighlightingDefinition> m_listDefinitions;

	/** List of listeners */
	private final EventListenerList m_listenerList = new EventListenerList();

	/** Table specification used in dialog for picking columns. */
	private DataTableSpec m_tableSpec;

	//
	// Constructor
	//

	/**
	 * Creates a new settings model with the specified config name.
	 * 
	 * @param configName
	 *            Config name. Must not be null or empty.
	 */
	public SettingsModelHighlighting(final String configName) {
		if ((configName == null) || (configName.isEmpty())) {
			throw new IllegalArgumentException("The configName must be a "
					+ "non-empty string");
		}

		m_strConfigName = configName;
		m_listDefinitions = new ArrayList<HighlightingDefinition>(5);

		add(null);
	}

	//
	// Public Methods
	//

	/**
	 * Adds the specified definition to the end.
	 * 
	 * @param definition Highlighting definition to add. Can be null to create a default definition.
	 */
	public void add(final HighlightingDefinition definition) {
		final HighlightingDefinition def = (definition == null ?
				new HighlightingDefinition(true, null, Type.Atoms, null, true) : definition);
		m_listDefinitions.add(def);

		// Inform all table model listeners about the change
		fireTableChangedEvent(new TableModelEvent(this));
	}

	/**
	 * Removes the definition at the specified row index and informs the table listeners about the change.
	 * 
	 * @param iRow Row to be removed.
	 */
	public void remove(final int iRow) {
		m_listDefinitions.remove(iRow);

		// Inform all table model listeners about the change
		fireTableChangedEvent(new TableModelEvent(this));
	}

	/**
	 * Removes all definitions and informs the table listeners about the change.
	 */
	public void removeAll() {
		m_listDefinitions.clear();

		// Inform all table model listeners about the change
		fireTableChangedEvent(new TableModelEvent(this));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getRowCount() {
		return m_listDefinitions.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getColumnCount() {
		return COLUMN_HEADERS.length;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getColumnName(final int columnIndex) {
		return COLUMN_HEADERS[columnIndex];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<?> getColumnClass(final int columnIndex) {
		return COLUMN_CLASSES[columnIndex];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCellEditable(final int rowIndex, final int columnIndex) {
		return COLUMN_EDITABLE[columnIndex];
	}

	/**
	 * Defines the input column name at the specified row index.
	 * 
	 * @param rowIndex Row index to be set.
	 * @param name Column name to be set. Can be null.
	 */
	public void setInputColumn(final int rowIndex, final String strName) {
		final HighlightingDefinition definition = m_listDefinitions.get(rowIndex);

		// Only consider it, if there is a change
		if (!SettingsUtils.equals(strName, getInputColumnSpec(rowIndex))) {
			if (strName == null || strName.isEmpty()) {
				definition.setInputColumn(null);
			}
			else {
				definition.setInputColumn(strName);
			}
		}
	}


	/**
	 * Defines the input column name at the specified row index based on the passed
	 * in data column specification.
	 * 
	 * @param rowIndex Row index to be set.
	 * @param spec Data column specification to get the column name from.
	 */
	public void setInputColumnSpec(final int rowIndex, final DataColumnSpec spec) {
		final HighlightingDefinition definition = m_listDefinitions.get(rowIndex);

		// Only consider it, if there is a change
		if (!SettingsUtils.equals(spec, getInputColumnSpec(rowIndex))) {
			if (spec == null || SettingsUtils.equals(spec, UNDEFINED_COLUMN)) {
				definition.setInputColumn(null);
			}
			else {
				definition.setInputColumn(spec.getName());
			}
		}
	}

	/**
	 * Returns the input column specification for the input column, if known.
	 * 
	 * @return Input column spec or null, if currently not known.
	 */
	public DataColumnSpec getInputColumnSpec(final int rowIndex) {
		final HighlightingDefinition definition = m_listDefinitions.get(rowIndex);
		DataColumnSpec colSpec = null;

		final String strColumn = definition.getInputColumn();
		if (strColumn != null && !strColumn.isEmpty()) {
			if (m_tableSpec != null) {
				colSpec = m_tableSpec.getColumnSpec(strColumn);
			}
			else {
				colSpec = new DataColumnSpecCreator(strColumn, DataType.getMissingCell().getType()).createSpec();
			}
		}
		return (colSpec == null ? UNDEFINED_COLUMN : colSpec);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getValueAt(final int rowIndex, final int columnIndex) {
		final HighlightingDefinition definition = m_listDefinitions.get(rowIndex);
		Object retObject = null;

		switch (columnIndex) {
		case COLUMN_ACTIVE:
			retObject = definition.isActive();
			break;
		case COLUMN_TYPE:
			retObject = definition.getType();
			break;
		case COLUMN_INPUT_COLUMN_NAME:
			retObject = getInputColumnSpec(rowIndex);
			break;
		case COLUMN_COLOR:
			retObject = definition.getColor();
			break;
		case COLUMN_NEIGHBORHOOD:
			retObject = definition.isNeighborhoodIncluded();
			break;
		case COLUMN_DELETE_ACTION:
			retObject = (rowIndex == 0 ? null : DELETE_ICON);
			break;
		default:
			throw new IndexOutOfBoundsException();
		}

		return retObject;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setValueAt(final Object aValue, final int rowIndex,
			final int columnIndex) {
		final HighlightingDefinition definition = m_listDefinitions.get(rowIndex);
		boolean bChanged = false;

		// Update correct condition field, if value really changed
		switch (columnIndex) {
		case COLUMN_ACTIVE:
			final boolean bNewActive = ((Boolean) aValue).booleanValue();
			if (definition.isActive() != bNewActive) {
				definition.setActive(bNewActive);
				bChanged = true;
			}
			break;
		case COLUMN_INPUT_COLUMN_NAME:
			String strNewName = null;

			if (aValue instanceof DataColumnSpec) {
				final DataColumnSpec colSpec = (DataColumnSpec)aValue;
				if (SettingsUtils.equals(colSpec, UNDEFINED_COLUMN)) {
					strNewName = null;
				}
				else {
					strNewName = colSpec.getName();
				}
			}
			else {
				strNewName = (String)aValue;
			}

			if (!SettingsUtils.equals(definition.getInputColumn(), strNewName)) {
				definition.setInputColumn(strNewName);
				bChanged = true;
			}
			break;
		case COLUMN_TYPE:
			final Type newType = SettingsUtils
			.getEnumValueFromString(Type.class, "" + aValue,
					Type.Atoms);
			if (definition.getType() != newType) {
				definition.setType(newType);
				bChanged = true;
			}
			break;
		case COLUMN_COLOR:
			final Color newColor = (Color)aValue; // SettingsUtils.getColorFromObject(aValue, null);
			if (!SettingsUtils.equals(definition.getColor(), newColor)) {
				definition.setColor(newColor);
				bChanged = true;
			}
			break;
		case COLUMN_NEIGHBORHOOD:
			final boolean bNewNeighborhood = ((Boolean) aValue).booleanValue();
			if (definition.isNeighborhoodIncluded() != bNewNeighborhood) {
				definition.setIncludeNeighborhood(bNewNeighborhood);
				bChanged = true;
			}
			break;
		case COLUMN_DELETE_ACTION:
			break;
		default:
			throw new IndexOutOfBoundsException();
		}

		// Inform all table model listeners about the change
		if (bChanged) {
			fireTableChangedEvent(new TableModelEvent(this, rowIndex, rowIndex,
					columnIndex));
		}
	}

	/**
	 * Activates or deactivates all definitions.
	 * 
	 * @param bTargetState
	 *            True to activate all definitions. False to deactivate all
	 *            definitions.
	 * 
	 * @return True, if something has changed. False otherwise.
	 */
	public boolean setAllActivated(final boolean bTargetState) {
		boolean bChanged = false;

		for (final HighlightingDefinition def : m_listDefinitions) {
			if (def.isActive() != bTargetState) {
				def.setActive(bTargetState);
				bChanged = true;
			}
		}

		// Inform all table model listeners about the change
		if (bChanged) {
			fireTableChangedEvent(new TableModelEvent(this));
		}

		return bChanged;
	}

	/**
	 * If we have a valid definitions file, this will return a tooltip
	 * representation for the specified row.
	 * 
	 * @param rowIndex
	 *            Row index.
	 * 
	 * @return Tooltip with details about the functional group. Null, if no
	 *         definition file known or invalid row index.
	 */
	public String getTooltip(final int rowIndex) {
		String strTooltip = null;

		if (rowIndex >= 0 && rowIndex < m_listDefinitions.size()) {
			final HighlightingDefinition def = m_listDefinitions.get(rowIndex);
			if (def.getInputColumn() == null) {
				strTooltip = "Does not hightlight anything, because the input column is undefined.";
			}
			else if (!def.isActive()) {
				strTooltip = "Does not hightlight anything, because this row is deactivated.";
			}
			else {
				strTooltip = "Highlights " + def.getType().toString().toLowerCase() + " with indexes defined in the column " +
						"'" + def.getInputColumn() + "'" + (!def.isNeighborhoodIncluded() ? "." :
							" and also the " + (def.getType() == Type.Atoms ? "bonds" : "atoms") +
								" in between.");
			}
		}

		return strTooltip;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addTableModelListener(final TableModelListener l) {
		m_listenerList.add(TableModelListener.class, l);
		fireTableChangedEvent(new TableModelEvent(this));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeTableModelListener(final TableModelListener l) {
		m_listenerList.remove(TableModelListener.class, l);
	}

	/**
	 * Called when the current input table specification changes.
	 * Based on it the data column spec for the input column will be
	 * picked derived from the known input column name.
	 * 
	 * @param spec New table spec.
	 */
	public void updateTableSpec(final DataTableSpec spec) {
		m_tableSpec = spec;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(
				"SettingsModelHighlighting { ");
		sb.append("configName = ").append(getConfigName())
		.append("conditions = { ");

		final HighlightingDefinition[] arrDefinitions = getDefinitions();
		if (arrDefinitions != null && arrDefinitions.length > 0) {
			sb.append('\n');
			for (final HighlightingDefinition cond : arrDefinitions) {
				sb.append(cond.toString()).append('\n');
			}
		}
		sb.append("} }");

		return sb.toString();
	}

	//
	// Protected Methods
	//

	/**
	 * Fires the specified table model change event to all registered table
	 * model listeners.
	 */
	protected void fireTableChangedEvent(final TableModelEvent event) {
		final TableModelListener[] arrListeners = m_listenerList
				.getListeners(TableModelListener.class);

		for (final TableModelListener l : arrListeners) {
			try {
				l.tableChanged(event);
			} catch (final Exception exc) {
				LOGGER.error(
						"Table model listener of Highlighting Definitions table generated an exception.",
						exc);
			}
		}
	}

	/**
	 * Creates a new settings model with identical values for everything except
	 * the registered table model listeners.
	 * 
	 * @return a new settings model with the same configName and conditions.
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected SettingsModelHighlighting createClone() {
		final SettingsModelHighlighting newModel = new SettingsModelHighlighting(
				getConfigName());
		final HighlightingDefinition[] arrDefinitions = getDefinitions();

		for (final HighlightingDefinition def : arrDefinitions) {
			newModel.m_listDefinitions.add(new HighlightingDefinition(def));
		}

		return newModel;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getModelTypeID() {
		return "SMID_highlightingdefinitions";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getConfigName() {
		return m_strConfigName;
	}

	/**
	 * Returns a copy of all highlighting definitions stored in this model.
	 * Changing parameters in these objects will not effect the model.
	 * 
	 * @return Array of highlighting definitions.
	 */
	protected HighlightingDefinition[] getDefinitions() {
		final HighlightingDefinition[] arrDefinitions = m_listDefinitions
				.toArray(new HighlightingDefinition[m_listDefinitions.size()]);

		// Create copies of all definitions so that changes will not effect the model
		for (int i = 0; i < arrDefinitions.length; i++) {
			arrDefinitions[i] = new HighlightingDefinition(arrDefinitions[i]);
		}

		return arrDefinitions;
	}

	/**
	 * Returns a copy of all activated highlighting definitions stored in
	 * this model. Changing parameters in these objects will not effect the
	 * model.
	 * 
	 * @return Array of activated highlighting definitions.
	 */
	protected HighlightingDefinition[] getActivatedDefinitions() {
		final int iTotalCount = m_listDefinitions.size();
		final List<HighlightingDefinition> listActivatedConditions = new ArrayList<HighlightingDefinition>(
				iTotalCount);

		// Create copies of all activated definitions so that changes will not
		// effect the model
		for (int i = 0; i < iTotalCount; i++) {
			final HighlightingDefinition def = m_listDefinitions.get(i);
			if (def.isActive()) {
				listActivatedConditions.add(new HighlightingDefinition(def));
			}
		}

		return listActivatedConditions
				.toArray(new HighlightingDefinition[listActivatedConditions
				                                    .size()]);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsForDialog(final NodeSettingsRO settings,
			final PortObjectSpec[] specs) throws NotConfigurableException {
		m_listDefinitions.clear();

		try {
			final Config config = settings.getConfig(getConfigName());
			final int count = config.getInt("count");

			for (int i = 0; i < count; i++) {
				try {
					m_listDefinitions.add(new HighlightingDefinition(config,
							i));
				} catch (final InvalidSettingsException exc) {
					// Ignore this here and read as much as we can
				}
			}
		} catch (final InvalidSettingsException exc) {
			// Ignore this here
		}

		fireTableChangedEvent(new TableModelEvent(this));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsForDialog(final NodeSettingsWO settings)
			throws InvalidSettingsException {
		saveSettingsForModel(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettingsForModel(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		final Config config = settings.getConfig(getConfigName());
		final int count = config.getInt("count");

		for (int i = 0; i < count; i++) {
			new HighlightingDefinition(config, i);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsForModel(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_listDefinitions.clear();

		final Config config = settings.getConfig(getConfigName());
		final int count = config.getInt("count");

		for (int i = 0; i < count; i++) {
			m_listDefinitions.add(new HighlightingDefinition(config, i));
		}

		fireTableChangedEvent(new TableModelEvent(this));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsForModel(final NodeSettingsWO settings) {
		final HighlightingDefinition[] arrDefinitions = getDefinitions();
		final int iCount = arrDefinitions.length;

		final Config config = settings.addConfig(getConfigName());
		config.addInt("count", iCount);

		for (int i = 0; i < iCount; i++) {
			arrDefinitions[i].saveSettings(config, i);
		}
	}
}
