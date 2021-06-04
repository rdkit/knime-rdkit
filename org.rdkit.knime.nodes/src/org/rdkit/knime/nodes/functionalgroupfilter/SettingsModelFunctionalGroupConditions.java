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
package org.rdkit.knime.nodes.functionalgroupfilter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import java.util.Base64;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.rdkit.knime.util.SettingsUtils;

/**
 * This settings model stores all configured functional group conditions.
 * It contains the logic how to load and save these settings and provides
 * table model interfaces to present these values to the user in a table
 * in the settings dialog.
 *
 * @author Dillip K Mohanty
 * @author Manuel Schwarze
 */
public class SettingsModelFunctionalGroupConditions extends SettingsModel implements TableModel {

	//
	// Enumerations
	//

	/**
	 * The qualifier enumeration to be applied during counting of functional
	 * groups.
	 *
	 * @author Manuel Schwarze
	 */
	public enum Qualifier {
		LessThan {
			@Override
			public boolean test(final int iNum1, final int iNum2) {
				return (iNum1 < iNum2);
			}
		},

		AtMost {
			@Override
			public boolean test(final int iNum1, final int iNum2) {
				return (iNum1 <= iNum2);
			}
		},

		Exactly {
			@Override
			public boolean test(final int iNum1, final int iNum2) {
				return (iNum1 == iNum2);
			}
		},

		AtLeast {
			@Override
			public boolean test(final int iNum1, final int iNum2) {
				return (iNum1 >= iNum2);
			}
		},

		MoreThan {
			@Override
			public boolean test(final int iNum1, final int iNum2) {
				return (iNum1 > iNum2);
			}
		};

		/**
		 * Delivers the result of putting the qualifier between
		 * parameter 1 and 2, e.g. for MoreThan it would deliver
		 * true, if iNum1 > iNum2.
		 *
		 * @param iNum1 Parameter 1 to test.
		 * @param iNum2 Parameter 2 to test.
		 *
		 * @return Result of applied qualifier.
		 */
		public abstract boolean test(int iNum1, int iNum2);

		/**
		 * Returns a mathematical representation of the qualifier.
		 *
		 * @return Qualifier in mathematical notation.
		 */
		@Override
		public String toString() {
			switch (this) {
			case LessThan:
				return "<";
			case AtMost:
				return "<=";
			case Exactly:
				return "=";
			case AtLeast:
				return ">=";
			case MoreThan:
				return ">";
			}

			return super.toString();
		}
	}

	//
	// Inner Classes
	//

	/**
	 *
	 *
	 * @author Dillip K Mohanty
	 * @author Manuel Schwarze
	 */
	public static class FunctionalGroupCondition {

		/** Flag to determine if this condition is active. */
		private boolean m_bActive;

		/** The functional group name. */
		private final String m_strName;

		/** The condition qualifier. */
		private Qualifier m_qualifier;

		/** The number of occurrences of the functional group. */
		private int m_iCount;

		//
		// Constructor
		//

		/**
		 * Creates a new functional group condition with default values for the
		 * specified functional group name.
		 *
		 * @param strName The functional group name. Must not be null.
		 */
		public FunctionalGroupCondition(final String strName) {
			this(false, strName, Qualifier.Exactly, 0);
		}

		/**
		 * Creates a new functional group condition.
		 *
		 * @param bActive True, if this condition shall be checked, false otherwise.
		 * @param strName The functional group name. Must not be null.
		 * @param qualifier The condition's qualifier. Must not be null.
		 * @param iNumber The number of occurrences (checked with the qualifier).
		 */
		public FunctionalGroupCondition(final boolean bActive, final String strName,
				final Qualifier qualifier, final int iNumber) {
			// Pre-checks
			if (strName == null) {
				throw new IllegalArgumentException("Functional group name must not be null.");
			}
			if (qualifier == null) {
				throw new IllegalArgumentException("Qualifier must not be null.");
			}

			m_bActive = bActive;
			m_strName = strName;
			m_qualifier = qualifier;
			m_iCount = iNumber;
		}

		/**
		 * Creates a new functional group condition by reading its settings
		 * from a config object from the specified index. If the name
		 * cannot be found, it will fail. If some other setting is not found
		 * it will fall back to use a default value.
		 *
		 * @param config Settings configurations. Must not be null.
		 * @param index Index of settings to read from. Must be valid.
		 *
		 * @throws InvalidSettingsException Thrown, if the name of the condition was not
		 * 		found in the config object under the specified index. Also thrown,
		 * 		if null was passed in as config parameter.
		 */
		public FunctionalGroupCondition(final Config config, final int index) throws InvalidSettingsException {
			if (config == null) {
				throw new InvalidSettingsException("No configuration found for Functional Group Condition.");
			}

			m_strName = config.getString("name_" + index); // May throw an exception
			m_bActive = config.getBoolean("select_" + index, false);

			// For backward compatibility
			String strQual = config.getString("qualifier_" + index, "=");
			if (strQual.endsWith(")")) {
				// Old version found which ended with "(qualifier)"
				final int lastIndex  = strQual.lastIndexOf("(");
				strQual = strQual.substring(lastIndex + 1, strQual.length() - 1);
			}

			m_qualifier = SettingsUtils.getEnumValueFromString(Qualifier.class,
					strQual, Qualifier.Exactly);
			m_iCount = config.getInt("count_" + index, 0);
		}

		/**
		 * Creates a new functional group condition as a copy of the given one.
		 *
		 * @param orig The condition that shall be copied. Must not be null.
		 */
		public FunctionalGroupCondition(final FunctionalGroupCondition orig) {
			m_bActive = orig.m_bActive;
			m_strName = orig.m_strName;
			m_qualifier = orig.m_qualifier;
			m_iCount = orig.m_iCount;
		}

		//
		// Public Methods
		//

		/**
		 * Resets the values of the conditions. Does not touch the name.
		 *
		 * @return True, if something has changed. False otherwise.
		 */
		public boolean reset() {
			boolean bChanged = false;

			if (m_bActive) {
				bChanged = true;
				m_bActive = false;
			}

			if (m_qualifier != Qualifier.Exactly) {
				bChanged = true;
				m_qualifier = Qualifier.Exactly;
			}

			if (m_iCount != 0) {
				bChanged = true;
				m_iCount = 0;
			}

			return bChanged;
		}

		/**
		 * Stores the condition settings in the specified config object.
		 *
		 * @param config Settings configurations. Must not be null.
		 * @param index Index of settings to write to.
		 */
		public void saveSettings(final Config config, final int index) {
			if (config == null) {
				throw new IllegalArgumentException("Configuration object must not be null.");
			}

			config.addString("name_" + index, m_strName);
			config.addBoolean("select_" + index, m_bActive);
			config.addString("qualifier_" + index, m_qualifier.name());
			config.addInt("count_" + index, m_iCount);
		}

		/**
		 * Returns the unique name of the functional group that this
		 * condition applies to.
		 *
		 * @return Unique functional group name.
		 */
		public String getName() {
			return m_strName;
		}

		/**
		 * Determines, if this condition is active.
		 *
		 * @return True, if active, false otherwise.
		 */
		public boolean isActive() {
			return m_bActive;
		}

		/**
		 * Sets the flag to activate or deactivate this condition.
		 *
		 * @param bActive True to activate, false to deactivate.
		 */
		public void setActive(final boolean bActive) {
			m_bActive = bActive;
		}

		/**
		 * Returns the qualifier of this condition.
		 *
		 * @return Qualifier of this condition.
		 */
		public Qualifier getQualifier() {
			return m_qualifier;
		}

		/**
		 * Sets the qualifier for this condition. It is used
		 * to compare the number of occurrences with the number
		 * set in this condition.
		 *
		 * @param qualifier Qualifier to be used for this condition.
		 */
		public void setQualifier(final Qualifier qualifier) {
			m_qualifier = qualifier;
		}

		/**
		 * Returns the count for comparison using the qualifier set
		 * in this condition.
		 *
		 * @return Number for comparison.
		 */
		public int getCount() {
			return m_iCount;
		}

		/**
		 * Sets the count for comparison (using the qualifier).
		 *
		 * @param iCount Count for comparison. If a value
		 * 		smaller than 0 is passed in, it gets corrected
		 * 		to 0.
		 */
		public void setCount(int iCount) {
			if (iCount < 0) {
				iCount = 0;
			}

			m_iCount = iCount;
		}

		/**
		 * Calculates the hash code based on the member variables.
		 * 
		 * @return Hash code of this object.
		 */
		@Override
		public int hashCode() {
			return getName().hashCode() |
					getQualifier().hashCode() |
					getCount() |
					(isActive() ? 1 : 2);
		}

		/**
		 * Determines, if all member variables are set to the same value.
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(final Object o) {
			boolean bRet = false;

			if (o instanceof FunctionalGroupCondition) {
				final FunctionalGroupCondition lp = (FunctionalGroupCondition)o;
				bRet = (SettingsUtils.equals(m_strName, lp.m_strName) &&
						SettingsUtils.equals(m_qualifier, lp.m_qualifier) &&
						m_iCount == lp.m_iCount && m_bActive == lp.m_bActive);
			}

			return bRet;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("FunctionalGroupCondition { ");
			sb.append("name = ").append(getName())
			.append(", qualifier = ").append(getQualifier())
			.append(", count = ").append(getCount())
			.append(", isActive = " + isActive())
			.append(" }");

			return sb.toString();
		}
	}

	//
	// Constants
	//

	/** The logger instance. */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(SettingsModelFunctionalGroupConditions.class);

	/** Column index of the active flag of the condition. */
	public static final int COLUMN_ACTIVE = 0;

	/**
	 * Column index of the display name of the condition. Note: The condition itself
	 * is based on the unique name of a functional group, not on the display name.
	 * The display will be delivered from the function group definition,
	 * unless the group with the unique name is not found. In that case the
	 * unique name will be returned as display name.
	 */
	public static final int COLUMN_DISPLAY_NAME = 1;

	/** Column index of the qualifier of the condition. */
	public static final int COLUMN_QUALIFIER = 2;

	/** Column index of the count of the condition. */
	public static final int COLUMN_COUNT = 3;

	/** Column header names. */
	public static final String[] COLUMN_HEADERS = { "Active", "Functional Group", "Qualifier", "Count" };

	/** Column value classes used to determine renderers and editors. */
	public static final Class<?>[] COLUMN_CLASSES = { Boolean.class, String.class, String.class, Integer.class };

	/** Column modify information. */
	public static final boolean[] COLUMN_EDITABLE = { true, false, true, true };

	//
	// Members
	//

	/** Flag to determine, if old settings get cached for user convenience. */
	private final boolean m_bUseOldSettingsCache;

	/** The key used to store the enumeration value in the settings. */
	private final String m_strConfigName;

	/** Functional group definition, if known. */
	private FunctionalGroupDefinitions m_definitions;

	/** The current list of functional group conditions. Can be empty, but never null. */
	private final List<FunctionalGroupCondition> m_listConditions;

	/** List of listeners */
	private final EventListenerList m_listenerList = new EventListenerList();

	/**
	 * A cache that stores condition configurations from old functional group settings,
	 * which had been replaced by updating the conditions list (usually from a definition
	 * file). When the old definition file is read again (a well-defined set of unique
	 * strings of functional group names) it is useful, if at least for a KNIME session
	 * the user will find the old settings again and does not have to enter everything
	 * again. The key of this map here is a SHA1 hash over a concatenation of all
	 * functional group names, which has been sorted alphabetically.
	 */
	private final HashMap<String, FunctionalGroupCondition[]> m_mapOldSettingsCache =
			new HashMap<String, FunctionalGroupCondition[]>();

			//
			// Constructor
			//

			/**
			 * Creates a new settings model with the specified config name.
			 *
			 * @param configName Config name. Must not be null or empty.
			 */
			public SettingsModelFunctionalGroupConditions(final String configName) {
				this(configName, false);
			}

			/**
			 * Creates a new settings model with the specified config name.
			 *
			 * @param configName Config name. Must not be null or empty.
			 * @param bUseOldSettingsCache Set to true to cache old settings when
			 * 		the user updates the settings by loading a new definition file.
			 * 		If this is enabled, the settings model can restore these settings
			 * 		in the same KNIME session (same node, same dialog) when the
			 * 		user is reloading the old definition file again.
			 */
			public SettingsModelFunctionalGroupConditions(final String configName,
					final boolean bUseOldSettingsCache) {
				if ((configName == null) || (configName.isEmpty())) {
					throw new IllegalArgumentException("The configName must be a "
							+ "non-empty string");
				}

				m_strConfigName = configName;
				m_listConditions = new ArrayList<FunctionalGroupCondition>(100);
				m_bUseOldSettingsCache = bUseOldSettingsCache;
			}

			//
			// Public Methods
			//

			/**
			 * {@inheritDoc}
			 */
			@Override
			public int getRowCount() {
				return m_listConditions.size();
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
			 * {@inheritDoc}
			 */
			@Override
			public Object getValueAt(final int rowIndex, final int columnIndex) {
				final FunctionalGroupCondition cond = m_listConditions.get(rowIndex);
				Object retObject = null;

				switch (columnIndex) {
				case COLUMN_ACTIVE:
					retObject = cond.isActive();
					break;
				case COLUMN_DISPLAY_NAME:
					// Get the display name directly from the functional group definition
					if (m_definitions != null) {
						final FunctionalGroup group = m_definitions.get(cond.getName());
						if (group != null) {
							retObject = group.getDisplayLabel();
						}
					}

					// Fallback only
					if (retObject == null) {
						retObject = cond.getName();
					}
					break;
				case COLUMN_QUALIFIER:
					retObject = cond.getQualifier();
					break;
				case COLUMN_COUNT:
					retObject = cond.getCount();
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
			public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
				final FunctionalGroupCondition cond = m_listConditions.get(rowIndex);
				boolean bChanged = false;

				// Update correct condition field, if value really changed
				switch (columnIndex) {
				case COLUMN_ACTIVE:
					final boolean bNewValue = ((Boolean)aValue).booleanValue();
					if (cond.isActive() != bNewValue) {
						cond.setActive(bNewValue);
						bChanged = true;
					}
					break;
				case COLUMN_DISPLAY_NAME:
					// This column is not modifiable via this interface
					break;
				case COLUMN_QUALIFIER:
					final Qualifier newQualifier = SettingsUtils.getEnumValueFromString(
							Qualifier.class, "" + aValue, Qualifier.Exactly);
					if (cond.getQualifier() != newQualifier) {
						cond.setQualifier(newQualifier);
						bChanged = true;
					}
					break;
				case COLUMN_COUNT:
					final int iNewValue = ((Integer)aValue).intValue();
					if (cond.getCount() != iNewValue) {
						cond.setCount(iNewValue);
						bChanged = true;
					}
					break;
				default:
					throw new IndexOutOfBoundsException();
				}

				// Inform all table model listeners about the change
				if (bChanged) {
					fireTableChangedEvent(new TableModelEvent(
							this, rowIndex, rowIndex, columnIndex));
				}
			}

			/**
			 * Resets the values of all conditions. Does not touch the names.
			 *
			 * @return True, if something has changed. False otherwise.
			 */
			public boolean resetAll() {
				boolean bChanged = false;

				for (final FunctionalGroupCondition cond : m_listConditions) {
					bChanged |= cond.reset();
				}

				// Inform all table model listeners about the change
				if (bChanged) {
					fireTableChangedEvent(new TableModelEvent(this));
				}

				return bChanged;
			}

			/**
			 * Activates or deactivates all conditions. Does not touch the names.
			 *
			 * @param bTargetState True to activate all conditions. False to
			 * 		deactivate all conditions.
			 *
			 * @return True, if something has changed. False otherwise.
			 */
			public boolean setAllActivated(final boolean bTargetState) {
				boolean bChanged = false;

				for (final FunctionalGroupCondition cond : m_listConditions) {
					if (cond.isActive() != bTargetState) {
						cond.setActive(bTargetState);
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
			 * @param rowIndex Row index.
			 *
			 * @return Tooltip with details about the functional group. Null,
			 * 		if no definition file known or invalid row index.
			 */
			public String getTooltip(final int rowIndex) {
				String strTooltip = null;

				if (m_definitions != null) {
					try {
						strTooltip = m_definitions.get(m_listConditions.get(rowIndex).getName()).getTooltip();
					}
					catch (final Exception exc) {
						// Ignored by purpose
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
			 * {@inheritDoc}
			 */
			@Override
			public String toString() {
				final StringBuilder sb = new StringBuilder("FunctionalGroupConditionsSettingsModel { ");
				sb.append("configName = ").append(getConfigName())
				.append("conditions = { ");

				final FunctionalGroupCondition[] arrConditions = getConditions();
				if (arrConditions != null && arrConditions.length > 0) {
					sb.append('\n');
					for (final FunctionalGroupCondition cond : arrConditions) {
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
			 * Fires the specified table model change event to all registered
			 * table model listeners.
			 */
			protected void fireTableChangedEvent(final TableModelEvent event) {
				final TableModelListener[] arrListeners =
						m_listenerList.getListeners(TableModelListener.class);

				for (final TableModelListener l : arrListeners) {
					try {
						l.tableChanged(event);
					}
					catch (final Exception exc) {
						LOGGER.error("Table model listener of Functional Group Conditions table generated an exception.", exc);
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
			protected SettingsModelFunctionalGroupConditions createClone() {
				final SettingsModelFunctionalGroupConditions newModel =
						new SettingsModelFunctionalGroupConditions(getConfigName());
				final FunctionalGroupCondition[] arrConditions = getConditions();

				for (final FunctionalGroupCondition cond : arrConditions) {
					newModel.m_listConditions.add(cond);
				}

				return newModel;
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			protected String getModelTypeID() {
				return "SMID_functionalgroupconditions";
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			protected String getConfigName() {
				return m_strConfigName;
			}

			/**
			 * Returns a copy of all functional group conditions stored in this model.
			 * Changing parameters in these objects will not effect the model.
			 *
			 * @return Array of functional group conditions.
			 */
			protected FunctionalGroupCondition[] getConditions() {
				final FunctionalGroupCondition[] arrConditions =
						m_listConditions.toArray(new FunctionalGroupCondition[m_listConditions.size()]);

				// Create copies of all conditions so that changes will not effect the model
				for (int i = 0; i < arrConditions.length; i++) {
					arrConditions[i] = new FunctionalGroupCondition(arrConditions[i]);
				}

				return arrConditions;
			}

			/**
			 * Returns a copy of all activated functional group conditions stored in this model.
			 * Changing parameters in these objects will not effect the model.
			 *
			 * @return Array of activated functional group conditions.
			 */
			protected FunctionalGroupCondition[] getActivatedConditions() {
				final int iTotalCount = m_listConditions.size();
				final List<FunctionalGroupCondition> listActivatedConditions =
						new ArrayList<SettingsModelFunctionalGroupConditions.
						FunctionalGroupCondition>(iTotalCount);

				// Create copies of all activated conditions so that changes will not effect the model
				for (int i = 0; i < iTotalCount; i++) {
					final FunctionalGroupCondition cond = m_listConditions.get(i);
					if (cond.isActive()) {
						listActivatedConditions.add(new FunctionalGroupCondition(cond));
					}
				}

				return listActivatedConditions.toArray(
						new FunctionalGroupCondition[listActivatedConditions.size()]);
			}

			/**
			 * Updates the conditions contained in this setting model with new functional group
			 * names. It will try to find old settings that were done in this node before
			 * for the new set of names. If not possible, it will try to keep the existing settings.
			 * This will be possible, if the name of an existing function group condition
			 * is equal to a new one that is in the list if names.
			 *
			 * @param definitions New definitions of functional groups. Only the unique names will
			 * 		be used here. Can be null, which would reset the entire list.
			 */
			protected void updateConditions(final FunctionalGroupDefinitions definitions) {
				// Save current conditions into cache
				if (m_bUseOldSettingsCache && m_listConditions != null) {
					final int iCount = m_listConditions.size();
					final String[] arrNames = new String[iCount];
					for (int i = 0; i < iCount; i++) {
						arrNames[i] = m_listConditions.get(i).getName();
					}
					putInOldSettingsCache(arrNames, getConditions());
				}

				if (definitions == null) {
					m_listConditions.clear();
					m_definitions = null;
					fireTableChangedEvent(new TableModelEvent(this));
				}
				else {
					// We are mainly interested in the unique names here
					final String[] arrNames = definitions.getFunctionalGroupNames();

					// Check, if we have old settings stored for the set of new names
					FunctionalGroupCondition[] arrOldConditions =
							m_bUseOldSettingsCache ? getFromOldSettingsCache(arrNames) : null;

							// If not found, try to conserve the current conditions
							if (arrOldConditions == null) {
								arrOldConditions = getConditions();
							}

							// Conserve conditions
							final Map<String, FunctionalGroupCondition> mapOldConditions =
									new HashMap<String, FunctionalGroupCondition>(arrOldConditions.length);
							for (final FunctionalGroupCondition cond : arrOldConditions) {
								mapOldConditions.put(cond.getName(), cond);
							}

							// Load new conditions, possibly using old values
							m_listConditions.clear();
							for (final String newName : arrNames) {
								FunctionalGroupCondition newCond = mapOldConditions.get(newName);
								if (newCond == null) {
									newCond = new FunctionalGroupCondition(newName);
								}
								m_listConditions.add(newCond);
							}

							m_definitions = definitions;
							fireTableChangedEvent(new TableModelEvent(this));
				}
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			protected void loadSettingsForDialog(final NodeSettingsRO settings,
					final PortObjectSpec[] specs) throws NotConfigurableException {
				m_listConditions.clear();

				try {
					final Config config = settings.getConfig(getConfigName());
					final int count = config.getInt("count");

					for (int i = 0; i < count; i++) {
						try {
							m_listConditions.add(new FunctionalGroupCondition(config, i));
						}
						catch (final InvalidSettingsException exc) {
							// Ignore this here and read as much as we can
						}
					}
				}
				catch (final InvalidSettingsException exc) {
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
					new FunctionalGroupCondition(config, i);
				}
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			protected void loadSettingsForModel(final NodeSettingsRO settings)
					throws InvalidSettingsException {
				m_listConditions.clear();

				final Config config = settings.getConfig(getConfigName());
				final int count = config.getInt("count");

				for (int i = 0; i < count; i++) {
					m_listConditions.add(new FunctionalGroupCondition(config, i));
				}

				fireTableChangedEvent(new TableModelEvent(this));
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			protected void saveSettingsForModel(final NodeSettingsWO settings) {
				final FunctionalGroupCondition[] arrConditions = getConditions();
				final int iCount = arrConditions.length;

				final Config config = settings.addConfig(getConfigName());
				config.addInt("count", iCount);

				for (int i = 0; i < iCount; i++) {
					arrConditions[i].saveSettings(config, i);
				}
			}

			/**
			 * Tries to find old functional group conditions for the specified name set in
			 * the old settings cache.
			 *
			 * @param arrNames Array of functional group names. Can be null.
			 *
			 * @return Array of old conditions or null, if not found.
			 */
			protected FunctionalGroupCondition[] getFromOldSettingsCache(final String[] arrNames) {
				FunctionalGroupCondition[] arrConditions = null;

				if (arrNames != null && arrNames.length > 0) {
					arrConditions = m_mapOldSettingsCache.get(createCacheHashKey(arrNames));
				}

				return arrConditions;
			}

			/**
			 * Puts old functional group conditions for the specified name set in
			 * the old settings cache.
			 *
			 * @param arrNames Array of functional group names. Can be null.
			 * @param arrConditions Array of conditions to be cached. Can be null.
			 */
			protected void putInOldSettingsCache(final String[] arrNames,
					final FunctionalGroupCondition[] arrConditions) {
				if (arrNames != null && arrNames.length > 0 && arrConditions != null) {
					m_mapOldSettingsCache.put(createCacheHashKey(arrNames), arrConditions);
				}
			}

			/**
			 * Creates a SHA1 hash over all concatenated names in the specified list.
			 * Before the hash gets calculated a copy of the list is sorted alphabetically to
			 * ensure a well-defined order. The passed in list will not be changed.
			 *
			 * @param arrNames Array of names.
			 *
			 * @return The SHA1 hash. Returns null, if the list is null.
			 */
			protected String createCacheHashKey(final String[] arrNames) {
				String strHash = null;

				if (arrNames != null) {
					final List<String> listSortedNames = new ArrayList<String>(Arrays.asList(arrNames));
					Collections.sort(listSortedNames);
					final int iLength = listSortedNames.size();
					final StringBuilder sb = new StringBuilder(iLength * 20);

					for (int i = 0; i < iLength; i++) {
						sb.append(listSortedNames.get(i));
					}

					try {
						final MessageDigest md = MessageDigest.getInstance("SHA1");
						md.update(sb.toString().getBytes());
						strHash=  Base64.getMimeEncoder().encodeToString(md.digest());
					}
					catch (final NoSuchAlgorithmException exc) {
						// Fallback - should never happen, but will also work
						strHash = sb.toString();
					}
				}

				return strHash;
			}
}
