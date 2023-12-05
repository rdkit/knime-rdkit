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
package org.rdkit.knime.nodes.functionalgroupfilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.knime.core.node.NodeLogger;
import org.rdkit.knime.util.FileUtils;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class represents the content of a functional group definition file.
 * 
 * @author Dillip K Mohanty
 * @author Manuel Schwarze
 */
public class FunctionalGroupDefinitions {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(FunctionalGroupDefinitions.class);

	/** The context for warnings that occur when reading lines of the definition file. */
	public static final WarningConsolidator.Context LINE_CONTEXT =
			new WarningConsolidator.Context("Line", "line", "lines", true);

	//
	// Members
	//

	/** The list of all functional groups. */
	private final List<FunctionalGroup> m_listFunctionalGroups;

	/** A quick access map of all functional groups based on their unique name. */
	private final Map<String, FunctionalGroup> m_mapFunctionalGroups;

	/** The total number of functional group lines read from the definition file. */
	private int m_iReadFunctionalGroupLines;

	/** The number of encountered doubles. */
	private int m_iDoublesInFunctionalGroupLines;

	/** The number of lines that failed reading. */
	private int m_iFailuresInFunctionalGroupLines;

	/** Stores all warnings that occurred during loading of definitions. */
	private final WarningConsolidator m_warnings;

	//
	// Constructors
	//

	/**
	 * Creates a new functional group definitions object and reads
	 * definitions from the specified input stream. The stream will
	 * be closed at the end.
	 * 
	 * @param in Input stream to read definitions from. Must not be null.
	 * 
	 * @throws IOException Thrown, if the input stream cannot be read.
	 */
	public FunctionalGroupDefinitions(final InputStream in) throws IOException {
		if (in == null) {
			throw new IllegalArgumentException(
					"Functional group definition input stream must not be null.");
		}

		m_listFunctionalGroups = new ArrayList<FunctionalGroup>(100);
		m_mapFunctionalGroups = new HashMap<String, FunctionalGroup>(100);
		m_warnings = new WarningConsolidator();
		m_iReadFunctionalGroupLines = 0;
		m_iDoublesInFunctionalGroupLines = 0;
		m_iFailuresInFunctionalGroupLines = 0;

		BufferedReader reader = null;

		try {
			String strLine = null;
			int iLineCounter = 0;

			reader = new BufferedReader(new InputStreamReader(in));

			while ((strLine = reader.readLine()) != null) {

				// Remove all leading and trailing whitespaces
				final String strTempLine = strLine.trim();

				// Ignore comments and empty lines starting with '//', else get inside
				if (!strTempLine.isEmpty() && !strTempLine.startsWith("//") &&
						!strTempLine.startsWith("'")) {

					try {
						m_iReadFunctionalGroupLines++;

						// Parses and creates a functional group
						final FunctionalGroup group = new FunctionalGroup(strLine);

						// Successfully created - Now register it
						m_listFunctionalGroups.add(group);
						if (m_mapFunctionalGroups.put(group.getName(), group) != null) {
							m_warnings.saveWarning("The functional group name '" +
									group.getName() + "' is not unique (line " +
									iLineCounter + "). Keeping only the last line.");
							m_iDoublesInFunctionalGroupLines++;
						}
					}
					catch (final ParseException excParse) {
						final String strMsg = excParse.getMessage();
						LOGGER.warn(strMsg + " (Line " + iLineCounter + ")");
						m_warnings.saveWarning(LINE_CONTEXT.getId(), strMsg);
						m_iFailuresInFunctionalGroupLines++;
					}
				}

				iLineCounter++;
			}
		}
		finally {
			FileUtils.close(reader);
			FileUtils.close(in);
		}
	}

	//
	// Public Methods
	//

	/**
	 * The warning consolidator that conserved warnings
	 * that occurred during loading of the definition file.
	 */
	public WarningConsolidator getWarningConsolidator() {
		return m_warnings;
	}

	/**
	 * Returns the number of all lines that have been read from the
	 * definition file as functional group definition lines.
	 * This includes all malformed lines as well and is basically the
	 * total of the results of {@link #getFunctionalGroupCount()},
	 * {@link #getDoublesInFunctionalGroupLines()} and
	 * {@link #getFailuresInFunctionalGroupLines()}.
	 * 
	 * @return Total number of lines that have been read as functional
	 * 		group definitions.
	 */
	public int getReadFunctionalGroupLines() {
		return m_iReadFunctionalGroupLines;
	}

	/**
	 * Returns the number of functional groups that
	 * had no unique id and were overridden.
	 * 
	 * @return Number of functional groups that had doubles.
	 */
	public int getDoublesInFunctionalGroupLines() {
		return m_iDoublesInFunctionalGroupLines;
	}

	/**
	 * Returns the number of functional group lines,
	 * which could not be read successfully and are
	 * not part of this object.
	 * 
	 * @return Number of failed functional groups.
	 */
	public int getFailuresInFunctionalGroupLines() {
		return m_iFailuresInFunctionalGroupLines;
	}

	/**
	 * Returns the number of functional groups that
	 * were read successfully and can be used.
	 * 
	 * @return Number of functional groups.
	 */
	public int getFunctionalGroupCount() {
		return m_listFunctionalGroups.size();
	}

	/**
	 * Returns an array of the functional groups contained in this
	 * definition.
	 * 
	 * @return Functional groups.
	 */
	public FunctionalGroup[] getFunctionalGroups() {
		return m_listFunctionalGroups.toArray(
				new FunctionalGroup[m_listFunctionalGroups.size()]);
	}

	/**
	 * Returns an array of all names of defined functional groups.
	 * 
	 * @return All functional group names.
	 */
	public String[] getFunctionalGroupNames() {
		final int iCount = m_listFunctionalGroups.size();
		final String[] arrNames = new String[iCount];

		for (int i = 0; i < iCount; i++) {
			arrNames[i] = m_listFunctionalGroups.get(i).getName();
		}

		return arrNames;
	}

	/**
	 * Returns an iterator over all names of defined functional groups.
	 * The order is undefined. Call {@link #getFunctionalGroups()}
	 * or {@link #getFunctionalGroupNames()} to
	 * get the functional groups in the order that they were defined
	 * in the file they were read from.
	 * 
	 * @return Iterator over functional group names.
	 */
	public Iterator<String> names() {
		return m_mapFunctionalGroups.keySet().iterator();
	}

	/**
	 * Retrieves the functional group with the specified name.
	 * 
	 * @param strName Name of functional group to retrieve. Can be null.
	 * 
	 * @return Functional group or null, if name does not exist or
	 * 		null was passed in.
	 */
	public FunctionalGroup get(final String strName) {
		return m_mapFunctionalGroups.get(strName);
	}
}
