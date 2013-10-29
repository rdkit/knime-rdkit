/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2013
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
package org.rdkit.knime.nodes.optimizegeometry;

import org.RDKit.ForceField;
import org.RDKit.ROMol;

/**
 * This enumeration lists different force fields supported by the RDKit.
 * Force fields are used for optimizing the geometry of a 3D molecule.
 * 
 * @author Manuel Schwarze
 */
public enum ForceFieldType {
	UFF {
		@Override
		public ForceField generateForceField(final ROMol mol) {
			return mol == null ? null : ForceField.UFFGetMoleculeForceField(mol);
		}

		@Override
		public int optimizeMolecule(final ROMol mol, final int iIterations) {
			return ForceField.UFFOptimizeMolecule(mol, iIterations);
		}
	},

	MMFF94 {
		@Override
		public ForceField generateForceField(final ROMol mol) {
			return mol == null ? null : ForceField.MMFFGetMoleculeForceField(mol, "MMFF94");
		}

		@Override
		public int optimizeMolecule(final ROMol mol, final int iIterations) {
			return ForceField.MMFFOptimizeMolecule(mol, "MMFF94", iIterations);
		}
	},

	MMFF94S {
		@Override
		public ForceField generateForceField(final ROMol mol) {
			return mol == null ? null : ForceField.MMFFGetMoleculeForceField(mol, "MMFF94S");
		}

		@Override
		public int optimizeMolecule(final ROMol mol, final int iIterations) {
			return ForceField.MMFFOptimizeMolecule(mol, "MMFF94S", iIterations);
		}
	};

	/**
	 * Tries to generate a force field for the passed in molecule.
	 * It is responsibility of the caller to call the delete() method on the returned
	 * force field when it is not needed anymore.
	 * 
	 * @param mol Molecule to generate a force field for. Can be null to return null.
	 * 
	 * @return Force field or null, if generation failed.
	 */
	public abstract ForceField generateForceField(ROMol mol);

	/**
	 * Optimizes the passed in molecule for the force field using the number
	 * of specified iterations to do so.
	 * 
	 * @param mol Molecule to be optimized. This object will be changed.
	 * @param iIterations Iterations to be used for optimization.
	 * 
	 * @return Returns whether or not more iterations are required. 0 means the calculation converged.
	 */
	public abstract int optimizeMolecule(ROMol mol, int iIterations);
}
