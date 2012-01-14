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
package org.rdkit.knime.nodes.saltstripper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.RDKit.RWMol;
import org.knime.core.node.NodeLogger;

/**
 * This is the class for stripping salts from the input molecules for
 * RDKitSaltStripper node.
 * 
 * @author Dillip K Mohanty
 */
public class SaltStripper {

	/**
	 * Logger instance for logging purpose.
	 */
	private static final NodeLogger logger = NodeLogger
			.getLogger(SaltStripper.class);

	/**
	 * string variable for salt definition file name.
	 */
	private String saltDefnFilename = "/Salts.txt";

	/**
	 * instance of ArrayList for storing salt definition from user.
	 */
	private ArrayList<ROMol> saltDefnData = null;

	/**
	 * instance of ArrayList of salts of ROMol type.
	 */
	private ArrayList<ROMol> salts = new ArrayList<ROMol>();

	/**
	 * Parameterized constructor for initializing the salt patterns whether from
	 * user or from a file.
	 * 
	 * @param saltDefnFilename
	 * @param saltDefnData
	 */
	public SaltStripper(String saltDefnFilename, ArrayList<ROMol> saltDefnData) {
		super();
		if (saltDefnFilename != null) {
			this.saltDefnFilename = saltDefnFilename;
		}
		this.saltDefnData = saltDefnData;
		// read salt patterns
		readSaltPatterns();
	}

	/**
	 * Method used for reading the salt patterns either from a column of the
	 * optional input table contaning salt definitions or default definitions
	 * from the "Salts.txt" file. The salts are added to a list one by one.
	 */
	private void readSaltPatterns() {
		logger.debug("Enter readSaltPatterns !");
		// Check if user has provided any salt definition otherwise use default
		// salt definition.
		if (saltDefnData != null && saltDefnData.size() > 0) {
			salts.addAll(saltDefnData);
		} else {
			String txtFilePath = SaltStripper.class.getPackage().getName()
					.replace('.', '/')
					+ saltDefnFilename;
			InputStreamReader isr = null;
			BufferedReader br = null;
			InputStream is = null;
			try {
				// read default salt definition file "Salts.txt".
				is = SaltStripper.class.getClassLoader().getResourceAsStream(
						txtFilePath);
				isr = new InputStreamReader(is);
				br = new BufferedReader(isr);
				String line = br.readLine();
				while (line != null) {
					// Ignore lines starting with '//' else get inside
					if (!line.startsWith("//") && line.trim().length() != 0) {
						// ignore content after tab
						String delims = "[\t]+";
						String[] tokens = line.split(delims);
						// add salts to list
						salts.add(RWMol.MolFromSmarts(tokens[0]));
					}
					line = br.readLine();
				}
			} catch (IOException io) {
				logger.error("IO Exception Occured: " + io.getMessage(), io);
			} finally {
				try {
					// close the readers.
					if (br != null) {
						br.close();
					}
					if (isr != null) {
						isr.close();
					}
					if (is != null) {
						is.close();
					}
				} catch (IOException e) {
					// Ignored by purpose
				}
			}
		}
		logger.debug("Exit readSaltPatterns !");
	}

	/**
	 * This method is the main method used for stripping the salts from
	 * molecules. The logic is : As long as the molecule has more than one
	 * fragment loop over each of the patterns that was read from the salts list
	 * and remove the atoms matched by that pattern from the molecule.
	 * 
	 * @param mol
	 *            (ROMol)
	 * @return ROMol
	 */
	public ROMol stripSalts(ROMol mol) {
		ROMol inputMol = mol;
		ROMol iterateMol = inputMol;
		
		logger.debug("Enter stripSalts: Stripping Start... !");
		if (salts != null && salts.size() > 0) {
			// iterate over the salt patterns for each mol.
			for (int i = 0; i < salts.size(); i++) {
				if (RDKFuncs.getMolFrags(iterateMol).size() > 1) {
					ROMol salt = salts.get(i);
					// strip salt substructures
					ROMol tMol = RDKFuncs.deleteSubstructs(iterateMol, salt, true);
					// if no of atoms > 1 then take the newly found structure.
					if (tMol.getNumAtoms() != 0) {
						if (iterateMol != inputMol) {
							iterateMol.delete();
						}
						iterateMol = tMol;
					}
				}
			}
		}
		RWMol rMol = new RWMol(iterateMol);
		// sanitize the molecule. If sanitization fails then keep the original molecule. 
		try {
			RDKFuncs.sanitizeMol(rMol);
		} catch (Exception e) {
			logger.debug("Sanitization failed. Keeping the original molcule.");
			rMol = new RWMol(inputMol); 
		}
		logger.debug("Exit stripSalts: Stripping complete... !");
		return rMol;

	}

}
