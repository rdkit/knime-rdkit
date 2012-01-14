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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;

import org.RDKit.ROMol;
import org.RDKit.RWMol;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.rdkit.knime.nodes.functionalgroupfilter.FunctionalGroupNodeSettings.LineProperty;


/**
 * This is the class for filtering the functional group from the input
 * molecules.
 * 
 * @author Dillip K Mohanty
 */
public class FunctionalGroupFilter {

	/**
	 * Logger instance for logging purpose.
	 */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(FunctionalGroupFilter.class);

	/**
	 * final constant for default file "Functional_Group_Hierarchy.txt"
	 */
	private final String DEFAULT_FILE = "/Functional_Group_Hierarchy.txt";

	/**
	 * instance of ArrayList for storing salt definition from user.
	 */
	private ArrayList<FunctionalGroup> selectedFuncGroupDefnList = null;

	/**
	 * Method used for reading the functional group patterns either from a user
	 * input file containing functional group definitions or default definitions
	 * from the "Functional_Group_Hierarchy.txt" file.
	 */
	public ArrayList<FunctionalGroup> readFuncGroupPatterns(
			String funcGroupDefnFilename) {

		LOGGER.debug("Enter readFuncGroupPatterns !");
		String txtFilePath = null;
		ArrayList<FunctionalGroup> funcGroupDefnList = new ArrayList<FunctionalGroup>();

		InputStreamReader isr = null;
		BufferedReader br = null;
		InputStream is = null;
		FunctionalGroup funcGrp = null;

		StringBuffer groupName = null;
		try {
			// If user has specified a file to read the definitions from then
			// use the file
			// else use default group definition file
			if (funcGroupDefnFilename != null
					&& funcGroupDefnFilename.trim().length() != 0) {
				txtFilePath = funcGroupDefnFilename;
				File file = new File(txtFilePath);
				isr = new FileReader(file);
			} else {
				txtFilePath = FunctionalGroupFilter.class.getPackage()
						.getName().replace('.', '/')
						+ DEFAULT_FILE;
				is = FunctionalGroupFilter.class.getClassLoader()
						.getResourceAsStream(txtFilePath);
				isr = new InputStreamReader(is);
			}
			br = new BufferedReader(isr);
			String line = br.readLine();
			while (line != null) {
				// Ignore lines starting with '//' else get inside
				if (!line.startsWith("//") && line.trim().length() != 0) {
					funcGrp = new FunctionalGroup();
					// ignore content after tab
					String delims = "[\t]+";
					String[] tokens = line.split(delims);
					if (tokens != null) {
						funcGrp.setName(isNullOrBlank(tokens[0]));
						funcGrp.setSmarts(isNullOrBlank(tokens[1]));
						funcGrp.setLabel(isNullOrBlank(tokens[2]));
						// find the display name from the name provided in the
						// file.
						groupName = new StringBuffer();
						if (funcGrp.getName() != null
								&& funcGrp.getName().trim().length() > 0) {
							String[] name = funcGrp.getName().trim()
									.split("\\.");
							if (name != null && name.length > 0) {
								for (int i = 1; i <= name.length - 1; i++) {
									groupName.append(name[i]);
									groupName.append(" ");
								}
								groupName.append(isNullOrBlank(name[0].trim()));
							}

							funcGrp.setDisplayLabel(groupName.toString());
							funcGrp.setFuncMol(RWMol.MolFromSmarts(tokens[1]));
						}
						// add function group to list
						funcGroupDefnList.add(funcGrp);
					}
				}
				line = br.readLine();
			}
		} catch (IOException io) {
			LOGGER.error("IO Exception Occured :" + io.getMessage(), io);
		} catch (Exception e) {
			LOGGER.error("Exception Occured :" + e.getMessage(), e);
		} finally {
			try {
				// close the readers.
				if (br != null)
					br.close();
				if (isr != null)
					isr.close();
				if (is != null)
					is.close();
			} catch (IOException e) {
				LOGGER.error("Exception Occured while closing readers.", e);
			}
		}
		LOGGER.debug("Exit readFuncGroupPatterns");
		return funcGroupDefnList;
	}

	/**
	 * Method checks for null/empty string and returns '???' if null/empty sting
	 * is found.
	 * 
	 * @param string
	 * @return
	 */
	private String isNullOrBlank(String string) {
		if (string != null && !string.trim().isEmpty()) {
			return string;
		} else {
			return "???";
		}
	}

	/**
	 * This method is used for checking whether the filter pattern is present in
	 * the input molecules or not.
	 * 
	 * @param mol
	 * @return ROMol
	 */
	public boolean checkFunctionalGroup(ROMol mol) {
		LOGGER.debug("Enter checkFunctionalGroup: !");
		boolean isFound = false;
		ROMol patternMol = null;
		long patternCount = 0;
		String qualStr = null;
		if (selectedFuncGroupDefnList != null
				&& selectedFuncGroupDefnList.size() > 0) {
			// iterate over the selected filter patterns for each mol.
			for (int i = 0; i < selectedFuncGroupDefnList.size(); i++) {
				FunctionalGroup grp = (FunctionalGroup) selectedFuncGroupDefnList
						.get(i);
				patternMol = grp.getFuncMol();
				patternCount = grp.getFuncCount();

				if (patternCount > 0) {
					// find the count of matches for the filter pattern
					long numMatches = mol.getSubstructMatches(patternMol)
							.size();
					qualStr = grp.getQualifier();
					// apply qualifier when deciding the pass criteria
					if (qualStr.equals("LessThan(<)")) {
						isFound = numMatches < patternCount;
					} else if (qualStr.equals("AtMost(<=)")) {
						isFound = numMatches <= patternCount;
					} else if (qualStr.equals("Exactly(=)")) {
						isFound = numMatches == patternCount;
					} else if (qualStr.equals("AtLeast(>=)")) {
						isFound = numMatches >= patternCount;
					} else if (qualStr.equals("MoreThan(>)")) {
						isFound = numMatches > patternCount;
					}
				}
			}
		}

		LOGGER.debug("Exit checkFunctionalGroup:  !");
		return isFound;

	}

	/**
	 * This method is used for storing the filter inputs provided by the user in
	 * a list which would later be used for checking whether or not the input
	 * molecules contains the filter pattern.
	 * 
	 * @param props
	 * @param arrAll
	 * @throws CanceledExecutionException 
	 */
	public void setFilterPatternList(final Iterable<LineProperty> props,
			ArrayList<FunctionalGroup> arrAll, ExecutionContext exec) throws CanceledExecutionException {
		LOGGER.debug("Enter setFilterPatternList: !");
		selectedFuncGroupDefnList = new ArrayList<FunctionalGroup>();
		int lineIdx = -1;
		for (LineProperty p : props) {
			LineProperty property = new LineProperty(p);
			if (property.isSelect()) {
				if (arrAll.isEmpty()) {
					LOGGER.debug("No functional group filter selected.");
				} else {
					for (Iterator<FunctionalGroup> iterator = arrAll.iterator(); iterator
							.hasNext();) {
						FunctionalGroup functionalGroup = iterator.next();
						if (functionalGroup.getDisplayLabel().equals(
								property.getName())) {
							functionalGroup.setFuncCount(property.getCount());
							functionalGroup.setQualifier(property.getQualifier());
							selectedFuncGroupDefnList.add(functionalGroup);
						}
					}
				}
			}
			if (exec != null) {
				lineIdx++;
				exec.setProgress(lineIdx / (double) arrAll.size());
		    	exec.getProgressMonitor().checkCanceled();
	    	}
		}
		LOGGER.debug("Exit setFilterPatternList: !");
	}
}
