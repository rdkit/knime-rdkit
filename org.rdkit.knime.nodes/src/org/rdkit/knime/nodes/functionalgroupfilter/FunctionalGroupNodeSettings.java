/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2011
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;

/**
 * This class stores the settings the user can set in the dialog.
 * 
 * @author Dillip K Mohanty
 */
public class FunctionalGroupNodeSettings {

	/**
	 * Struct for property for each functional group.
	 * 
	 * @author Dillip K Mohanty
	 */
	public static class LineProperty {
		
		

		/**
		 * <code>true</code> if this property should be selected,
		 * <code>false</code> otherwise.
		 */
		private boolean select;

		/** The property's name. */
		private final String name;

		/** The property's qualifier. */
		private String qualifier;

		/** The property's count. */
		private int count;

		public boolean isSelect() {
			return select;
		}

		public void setSelect(boolean select) {
			this.select = select;
		}

		public String getQualifier() {
			return qualifier;
		}

		public void setQualifier(String qualifier) {
			this.qualifier = qualifier;
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public String getName() {
			return name;
		}
		
		/**
		 * Creates a new property.
		 * 
		 * @param select
		 *            <code>true</code> if this property should be selected,
		 *            <code>false</code> otherwise
		 * @param name
		 *            the property's name
		 * @param name
		 *            the property's qualifier
		 * @param type
		 *            the property's count
		 */
		public LineProperty(final boolean select, final String name,
				final String qualifier, final int count) {
			this.select = select;
			this.name = name;
			this.qualifier = qualifier;
			this.count = count;
		}

		/**
		 * Creates a new property as a copy of the given property.
		 * 
		 * @param copy
		 *            the property that should be copied
		 */
		public LineProperty(final LineProperty copy) {
			this.select = copy.select;
			this.name = copy.name;
			this.qualifier = copy.qualifier;
			this.count = copy.count;
		}
	}

	/**
	 * Logger instance for logging purpose.
	 */
	private static final NodeLogger logger = NodeLogger
			.getLogger(FunctionalGroupNodeSettings.class);

	private String colName;

	private String fileUrl;

	private List<LineProperty> linePropertyList = new ArrayList<LineProperty>();

	public String getColName() {
		return colName;
	}

	public void setColName(String colName) {
		this.colName = colName;
	}

	public List<LineProperty> getLinePropertyList() {
		return linePropertyList;
	}

	public void setLinePropertyList(List<LineProperty> linePropertyList) {
		this.linePropertyList = linePropertyList;
	}

	public String getFileUrl() {
		return fileUrl;
	}

	public void setFileUrl(String fileUrl) {
		this.fileUrl = fileUrl;
	}

	/**
	 * Clears all properties.
	 */
	public void clearProperties() {
		linePropertyList.clear();
	}

	/**
	 * Adds a new property.
	 * 
	 * @param prop
	 *            a property
	 */
	public void addProperty(final LineProperty prop) {
		linePropertyList.add(prop);
	}

	/**
	 * Returns the collection of all properties.
	 * 
	 * @return an unmodifiable collection
	 */
	public Collection<LineProperty> properties() {
		return Collections.unmodifiableCollection(linePropertyList);
	}

	/**
	 * Saves all settings into the given node settings object.
	 * 
	 * @param settings
	 *            the node settings
	 */
	public void saveSettings(final NodeSettingsWO settings) {

		settings.addString("colName", colName);
		settings.addString("fileUrl", fileUrl);

		Config props = settings.addConfig("properties");

		props.addInt("count", linePropertyList.size());
		for (int i = 0; i < linePropertyList.size(); i++) {
			props.addBoolean("select_" + i, linePropertyList.get(i).select);
			props.addString("name_" + i, linePropertyList.get(i).name);
			props.addString("qualifier_" + i, linePropertyList.get(i).qualifier);
			props.addInt("count_" + i, linePropertyList.get(i).count);
		}
	}

	/**
	 * Loads all settings from the given node settings object.
	 * 
	 * @param settings
	 *            the node settings
	 * @throws InvalidSettingsException
	 *             if a setting is missing
	 */
	public void loadSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {

		colName = settings.getString("colName");
		fileUrl = settings.getString("fileUrl");
		linePropertyList.clear();
		Config props = settings.getConfig("properties");
		int count = props.getInt("count");

		for (int i = 0; i < count; i++) {
			linePropertyList.add(new LineProperty(props.getBoolean("select_"
					+ i), props.getString("name_" + i), props
					.getString("qualifier_" + i), props.getInt("count_" + i)));
		}
	}

	/**
	 * Loads all settings from the given node settings object, using default
	 * values if a setting is missing.
	 * 
	 * @param settings
	 *            the node settings
	 */
	public void loadSettingsForDialog(final NodeSettingsRO settings) {

		colName = settings.getString("colName", null);
		fileUrl = settings.getString("fileUrl", null);
		linePropertyList.clear();

		try {
			Config props = settings.getConfig("properties");

			int count = props.getInt("count");

			for (int i = 0; i < count; i++) {
				linePropertyList.add(new LineProperty(props
						.getBoolean("select_" + i), props
						.getString("name_" + i), props.getString("qualifier_"
						+ i), props.getInt("count_" + i)));
			}
		} catch (Exception ex) {
			logger.debug("loadSettingsForDialog(): Could not load the default values.");
		}
	}

}
