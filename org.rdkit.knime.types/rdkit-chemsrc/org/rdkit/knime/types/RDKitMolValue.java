/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2010
 *  Novartis Institutes for BioMedical Research
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
 * -------------------------------------------------------------------
 *
 */
package org.rdkit.knime.types;

import java.util.Arrays;

import javax.swing.Icon;

import org.RDKit.ROMol;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.ExtensibleUtilityFactory;
import org.knime.core.data.convert.DataValueAccessMethod;
import org.knime.core.node.NodeLogger;

/**
 * Smiles Data Value interface. (Only a wrapper for the underlying string)
 *
 * @author Greg Landrum
 */
public interface RDKitMolValue extends DataValue {

	//
	// Constants
	//

	/** The logger instance. */
	static final NodeLogger LOGGER = NodeLogger.getLogger(RDKitMolValue.class);

	/**
	 * Meta information to this value type.
	 *
	 * @see DataValue#UTILITY
	 */
	public static final UtilityFactory UTILITY = new RDKUtilityFactory();

	/**
	 * Reads and returns the ROMol object represented by this value. It's the
	 * callers responsibility to call the {@link ROMol#delete()} method when
	 * done!
	 *
	 * @return a newly created {@link ROMol} object.
	 */
	@DataValueAccessMethod(name = "ROMol")
	ROMol readMoleculeValue();

	/**
	 * Returns the Smiles string of the molecule.
	 *
	 * @return a String value
	 */
	String getSmilesValue();

	/**
	 * Returns whether or not our SMILES is canonical
	 *
	 * @return a boolean value
	 */
	boolean isSmilesCanonical();

	/**
	 * Checks, if the passed in molecules are the same.
	 * 
	 * @param mol1
	 *            Molecule 1 to check. Can be null.
	 * @param mol2
	 *            Molecule 1 to check. Can be null.
	 * 
	 * @return True, if binary representations of the molecules is exactly the
	 *         same. Also true, if null was passed in for both molecules. False
	 *         otherwise.
	 */
	public static boolean equals(RDKitMolValue mol1, RDKitMolValue mol2) {
		boolean bSame = false;

		if (mol1 == null && mol2 == null) {
			bSame = true;
		} 
		else if (mol1 != null && mol2 != null) {
			ROMol molRDKit1 = null;
			ROMol molRDKit2 = null;
			byte[] byteContent1, byteContent2;

			try {

				if (mol1 instanceof RDKitMolCell2) {
					// Shortcut to get byte array representation (without
					// conversion to ROMol before
					byteContent1 = ((RDKitMolCell2) mol1).getBinaryValue();
				} 
				else {
					// Do it the "official" way
					molRDKit1 = mol1.readMoleculeValue();
					byteContent1 = RDKitMolCell2.toByteArray(molRDKit1);
				}

				if (mol2 instanceof RDKitMolCell2) {
					// Shortcut to get byte array representation (without
					// conversion to ROMol before
					byteContent2 = ((RDKitMolCell2) mol2).getBinaryValue();
				} 
				else {
					// Do it the "official" way
					molRDKit2 = mol2.readMoleculeValue();
					byteContent2 = RDKitMolCell2.toByteArray(molRDKit2);
				}

				bSame = Arrays.equals(byteContent1, byteContent2);
			} 
			catch (Exception exc) {
				// If something goes wrong the molecules are considered NOT equal
				LOGGER.error("Unable to compare two RDKit molecules.", exc);
			} 
			finally {
				if (molRDKit1 != null) {
					molRDKit1.delete();
				}
				if (molRDKit2 != null) {
					molRDKit2.delete();
				}
			}
		}

		return bSame;
	}

	/** Implementations of the meta information of this value class. */
	public static class RDKUtilityFactory extends ExtensibleUtilityFactory {
		/** Singleton icon to be used to display this cell type. */
		private static final Icon ICON = loadIcon(RDKitMolValue.class, "/rdkit_type.png");

		private static final DataValueComparator COMPARATOR = new DataValueComparator() {
			@Override
			protected int compareDataValues(final DataValue v1, final DataValue v2) {
				int atomCount1;
				int atomCount2;
				final ROMol mol1 = ((RDKitMolValue) v1).readMoleculeValue();
				try {
					atomCount1 = (int) mol1.getNumAtoms();
				} 
				finally {
					mol1.delete();
				}
				final ROMol mol2 = ((RDKitMolValue) v2).readMoleculeValue();
				try {
					atomCount2 = (int) mol2.getNumAtoms();
				} 
				finally {
					mol2.delete();
				}
				return atomCount1 - atomCount2;
			}
		};

		/** Only subclasses are allowed to instantiate this class. */
		protected RDKUtilityFactory() {
			super(RDKitMolValue.class);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Icon getIcon() {
			return ICON;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected DataValueComparator getComparator() {
			return COMPARATOR;
		}

		@Override
		public String getName() {
			return "RDKit Molecule";
		}
	}
}
