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
package org.rdkit.knime.nodes.descriptorcalculation;

import org.RDKit.Double_Vect;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.RDKit.UInt_Vect;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * Defines supported descriptors, which know how to calculate the value(s) and
 * how to create associated KNIME data cells.
 * 
 * @author Manuel Schwarze
 */
public enum Descriptor {

	slogp(DoubleCell.TYPE, "Log of the octanol/water partition coefficient (including implicit hydrogens).\n" +
			"This estimated property is an atomic contribution model that calculates logP from the given structure.\n" +
			"The definition is from:\n" +
			"S. A. Wildman and G. M. Crippen: JCICS 39 868-873, 1999") {

		@Override
		public String toString() {
			return "SlogP";
		}

		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new DoubleCell[] { new DoubleCell(RDKFuncs.calcMolLogP(mol)) };
		}
	},

	smr(DoubleCell.TYPE, "Molecular Refractivity (including implicit hydrogens). This estimated property is an atomic contribution model\n" +
			"that assumes the correct protonation state (washed structures). The definition is from:\n" +
			"S. A. Wildman and G. M. Crippen: JCICS 39 868-873, 1999") {

		@Override
		public String toString() {
			return "SMR";
		}

		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new DoubleCell[] { new DoubleCell(RDKFuncs.calcMolMR(mol)) };
		}
	},

	LabuteASA(DoubleCell.TYPE, "Calculates Labute's Approximate Surface Area (ASA from MOE). The definition is from:\n" +
			"P. Labute: Article in the Journal of the Chemical Computing Group and J. Mol. Graph. Mod. _18_ 464-477, 2000") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new DoubleCell[] { new DoubleCell(RDKFuncs.calcLabuteASA(mol)) };
		}
	},

	TPSA(DoubleCell.TYPE, "Calculates the Topological Polar Surface Area (TPSA). The TPSA definition is from:\n" +
			"P. Ertl, B. Rohde, P. Selzer:\n" +
			"Fast Calculation of Molecular Polar Surface Area as a Sum of Fragment-based Contributions and\n" +
			"Its Application to the Prediction of Drug Transport Properties,\n" +
			"J.Med.Chem. 43, 3714-3717, 2000") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new DoubleCell[] { new DoubleCell(RDKFuncs.calcTPSA(mol)) };
		}
	},

	AMW(DoubleCell.TYPE, "Calculates a molecule's average molecular weight") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new DoubleCell[] { new DoubleCell(RDKFuncs.calcAMW(mol, false)) };
		}
	},

	ExactMW(DoubleCell.TYPE, "Calculates a molecule's exact molecular weight") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new DoubleCell[] { new DoubleCell(RDKFuncs.calcExactMW(mol, false)) };
		}
	},

	NumLipinskiHBA(IntCell.TYPE, "Calculates the standard Lipinski HBA definition") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new IntCell[] { new IntCell((int)RDKFuncs.calcLipinskiHBA(mol)) };
		}
	},

	NumLipinskiHBD(IntCell.TYPE, "Calculates the standard Lipinski HBD definition") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new IntCell[] { new IntCell((int)RDKFuncs.calcLipinskiHBD(mol)) };
		}
	},

	NumRotatableBonds(IntCell.TYPE, "Number of rotatable bonds. A bond is rotatable if it is not in a ring,\n" +
			"and neither atom of the bond is such that (d[i]+h[i]) < 2.") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new IntCell[] { new IntCell((int)RDKFuncs.calcNumRotatableBonds(mol)) };
		}
	},

	NumHBD(IntCell.TYPE, "Calculates the hydrogen-bond donor number (HBD)") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new IntCell[] { new IntCell((int)RDKFuncs.calcNumHBD(mol)) };
		}
	},

	NumHBA(IntCell.TYPE, "Calculates the hydrogen-bond acceptor number (HBA)") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new IntCell[] { new IntCell((int)RDKFuncs.calcNumHBA(mol)) };
		}
	},

	NumAmideBonds(IntCell.TYPE, "Calculates the number of amide bonds") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new IntCell[] { new IntCell((int)RDKFuncs.calcNumAmideBonds(mol)) };
		}
	},

	NumHeteroAtoms(IntCell.TYPE, "Number of hetero atoms") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new IntCell[] { new IntCell((int)RDKFuncs.calcNumHeteroatoms(mol)) };
		}
	},

	NumHeavyAtoms(IntCell.TYPE, "Number of heavy atoms #{Z[i] | Z[i] > 1}") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new IntCell[] { new IntCell((int)mol.getNumAtoms(true)) };
		}
	},

	NumAtoms(IntCell.TYPE, "Number of atoms") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new IntCell[] { new IntCell((int)mol.getNumAtoms(false)) };
		}
	},

	NumRings(IntCell.TYPE, "Number of rings") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new IntCell[] { new IntCell((int)RDKFuncs.calcNumRings(mol)) };
		}
	},

	NumAromaticRings(IntCell.TYPE, "Number of Aromatic rings") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new IntCell[] { new IntCell((int)RDKFuncs.calcNumAromaticRings(mol)) };
		}
	},

	NumSaturatedRings(IntCell.TYPE, "Number of Saturated rings") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new IntCell[] { new IntCell((int)RDKFuncs.calcNumSaturatedRings(mol)) };
		}
	},

	NumAliphaticRings(IntCell.TYPE, "Number of Aliphatic (containing at least one non-aromatic bond) rings") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new IntCell[] { new IntCell((int)RDKFuncs.calcNumAliphaticRings(mol)) };
		}
	},

	NumAromaticHeterocycles(IntCell.TYPE, "Number of Aromatic Heterocycles") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new IntCell[] { new IntCell((int)RDKFuncs.calcNumAromaticHeterocycles(mol)) };
		}
	},

	NumSaturatedHeterocycles(IntCell.TYPE, "Number of Saturated Heterocycles") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new IntCell[] { new IntCell((int)RDKFuncs.calcNumSaturatedHeterocycles(mol)) };
		}
	},

	NumAliphaticHeterocycles(IntCell.TYPE, "Number of Aliphatic (containing at least one non-aromatic bond) Heterocycles") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new IntCell[] { new IntCell((int)RDKFuncs.calcNumAliphaticHeterocycles(mol)) };
		}
	},

	NumAromaticCarbocycles(IntCell.TYPE, "Number of Aromatic Carbocycles") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new IntCell[] { new IntCell((int)RDKFuncs.calcNumAromaticCarbocycles(mol)) };
		}
	},

	NumSaturatedCarbocycles(IntCell.TYPE, "Number of Saturated Carbocycles") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new IntCell[] { new IntCell((int)RDKFuncs.calcNumSaturatedCarbocycles(mol)) };
		}
	},

	NumAliphaticCarbocycles(IntCell.TYPE, "Number of Aliphatic (containing at least one non-aromatic bond) Carbocycles") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new IntCell[] { new IntCell((int)RDKFuncs.calcNumAliphaticCarbocycles(mol)) };
		}
	},

	FractionCSP3(DoubleCell.TYPE, "Calculates the fraction of a molecule's carbons that are SP3 hybridized") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new DoubleCell[] { new DoubleCell(RDKFuncs.calcFractionCSP3(mol)) };
		}
	},

	Chi0v(DoubleCell.TYPE, "Calculates the Chi0v value") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new DoubleCell[] { new DoubleCell(RDKFuncs.calcChi0v(mol)) };
		}
	},

	Chi1v(DoubleCell.TYPE, "Calculates the Chi1v value") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new DoubleCell[] { new DoubleCell(RDKFuncs.calcChi1v(mol)) };
		}
	},

	Chi2v(DoubleCell.TYPE, "Calculates the Chi2v value") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new DoubleCell[] { new DoubleCell(RDKFuncs.calcChi2v(mol)) };
		}
	},

	Chi3v(DoubleCell.TYPE, "Calculates the Chi3v value") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new DoubleCell[] { new DoubleCell(RDKFuncs.calcChi3v(mol)) };
		}
	},

	Chi4v(DoubleCell.TYPE, "Calculates the Chi4v value") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new DoubleCell[] { new DoubleCell(RDKFuncs.calcChi4v(mol)) };
		}
	},

	Chi1n(DoubleCell.TYPE, "Calculates the Chi1n value") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new DoubleCell[] { new DoubleCell(RDKFuncs.calcChi1n(mol)) };
		}
	},

	Chi2n(DoubleCell.TYPE, "Calculates the Chi2n value") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new DoubleCell[] { new DoubleCell(RDKFuncs.calcChi2n(mol)) };
		}
	},

	Chi3n(DoubleCell.TYPE, "Calculates the Chi3n value") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new DoubleCell[] { new DoubleCell(RDKFuncs.calcChi3n(mol)) };
		}
	},

	Chi4n(DoubleCell.TYPE, "Calculates the Chi4n value") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new DoubleCell[] { new DoubleCell(RDKFuncs.calcChi4n(mol)) };
		}
	},
	HallKierAlpha(DoubleCell.TYPE, "Calculates the Hall-Kier alpha value") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new DoubleCell[] { new DoubleCell(RDKFuncs.calcHallKierAlpha(mol)) };
		}
	},

	kappa1(DoubleCell.TYPE, "Calculates the kappa1 value") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new DoubleCell[] { new DoubleCell(RDKFuncs.calcKappa1(mol)) };
		}
	},

	kappa2(DoubleCell.TYPE, "Calculates the kappa2 value") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new DoubleCell[] { new DoubleCell(RDKFuncs.calcKappa2(mol)) };
		}
	},

	kappa3(DoubleCell.TYPE, "Calculates the kappa3 value") {
		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return new DoubleCell[] { new DoubleCell(RDKFuncs.calcKappa3(mol)) };
		}
	},

	slogp_VSA_1_12(DoubleCell.TYPE, 12, "Captures hydrophobic and hydrophilic effects either in the receptor or on the way to the receptor:\n" +
			"SlogP_VSA1: (-inf < x < -0.40), SlogP_VSA2: (-0.40 <= x < -0.20), SlogP_VSA3: (-0.20 <= x < 0.00),\n" +
			"SlogP_VSA4: (0.00 <= x < 0.10), SlogP_VSA5: (0.10 <= x < 0.15), SlogP_VSA6: (0.15 <= x < 0.20),\n" +
			"SlogP_VSA7: (0.20 <= x < 0.25), SlogP_VSA8: (0.25 <= x < 0.30), SlogP_VSA9: (0.30 <= x < 0.40),\n" +
			"SlogP_VSA10: (0.40 <= x < 0.50), SlogP_VSA11: (0.50 <= x < 0.60), SlogP_VSA12: (0.60 <= x < inf)") {

		@Override
		public String toString() {
			return "slogp_VSA[1..12]";
		}

		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return convertToCellArray(RDKFuncs.calcSlogP_VSA(mol), warningConsolidator);
		}
	},

	smr_VSA_1_10(DoubleCell.TYPE, 10, "Captures polarizability:\n" +
			"SMR_VSA1: (-inf < x < 1.29), SMR_VSA2: (1.29 <= x < 1.82),\n" +
			"SMR_VSA3: (1.82 <= x < 2.24), SMR_VSA4: (2.24 <= x < 2.45),\n" +
			"SMR_VSA5: (2.45 <= x < 2.75), SMR_VSA6: (2.75 <= x < 3.05),\n" +
			"SMR_VSA7: (3.05 <= x < 3.63), SMR_VSA8: (3.63 <= x < 3.80),\n" +
			"SMR_VSA9: (3.80 <= x < 4.00), SMR_VSA10: (4.00 <= x < inf)") {

		@Override
		public String toString() {
			return "smr_VSA[1..10]";
		}

		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return convertToCellArray(RDKFuncs.calcSMR_VSA(mol), warningConsolidator);
		}
	},

	peoe_VSA_1_14(DoubleCell.TYPE, 14, "Captures direct electrostatic interactions:\n" +
			"PEOE_VSA1: (-inf < x < -0.30), PEOE_VSA2: (-0.30 <= x < -0.25), PEOE_VSA3: (-0.25 <= x < -0.20),\n" +
			"PEOE_VSA4: (-0.20 <= x < -0.15), PEOE_VSA5: (-0.15 <= x < -0.10), PEOE_VSA6: (-0.10 <= x < -0.05),\n" +
			"PEOE_VSA7: (-0.05 <= x < 0.00), PEOE_VSA8: (0.00 <= x < 0.05), PEOE_VSA9: (0.05 <= x < 0.10),\n" +
			"PEOE_VSA10: (0.10 <= x < 0.15), PEOE_VSA11: (0.15 <= x < 0.20), PEOE_VSA12: (0.20 <= x < 0.25),\n" +
			"PEOE_VSA13: (0.25 <= x < 0.30), PEOE_VSA14: (0.30 <= x < inf)") {

		@Override
		public String toString() {
			return "peoe_VSA[1..14]";
		}

		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return convertToCellArray(RDKFuncs.calcPEOE_VSA(mol), warningConsolidator);
		}
	},

	MQN(IntCell.TYPE, 42, "Calculates MQNs" ) {

		@Override
		public String toString() {
			return "MQN[1..42]";
		}

		@Override
		public DataCell[] calculate(final ROMol mol, final WarningConsolidator warningConsolidator) {
			return convertToCellArray(RDKFuncs.calcMQNs(mol), warningConsolidator);
		}
	},

	FlowVariablePlaceHolder1(null, 0, "Use this as placeholder for " +
			"descriptors that shall be controlled by flow variables. " +
			"It is recommended not to mix normal descriptor and placeholders.") {

		@Override
		public String toString() {
			return "Placeholder 1";
		}

		@Override
		public DataCell[] calculate(final ROMol mol,
				final WarningConsolidator warningConsolidator) {
			return new DataCell[0];
		}
	},

	FlowVariablePlaceHolder2(null, 0, "Use this as placeholder for " +
			"descriptors that shall be controlled by flow variables. " +
			"It is recommended not to mix normal descriptor and placeholders.") {

		@Override
		public String toString() {
			return "Placeholder 2";
		}

		@Override
		public DataCell[] calculate(final ROMol mol,
				final WarningConsolidator warningConsolidator) {
			return new DataCell[0];
		}
	},


	FlowVariablePlaceHolder3(null, 0, "Use this as placeholder for " +
			"descriptors that shall be controlled by flow variables. " +
			"It is recommended not to mix normal descriptor and placeholders.") {

		@Override
		public String toString() {
			return "Placeholder 3";
		}

		@Override
		public DataCell[] calculate(final ROMol mol,
				final WarningConsolidator warningConsolidator) {
			return new DataCell[0];
		}
	},

	FlowVariablePlaceHolder4(null, 0, "Use this as placeholder for " +
			"descriptors that shall be controlled by flow variables. " +
			"It is recommended not to mix normal descriptor and placeholders.") {

		@Override
		public String toString() {
			return "Placeholder 4";
		}

		@Override
		public DataCell[] calculate(final ROMol mol,
				final WarningConsolidator warningConsolidator) {
			return new DataCell[0];
		}
	},

	FlowVariablePlaceHolder5(null, 0, "Use this as placeholder for " +
			"descriptors that shall be controlled by flow variables. " +
			"It is recommended not to mix normal descriptor and placeholders.") {

		@Override
		public String toString() {
			return "Placeholder 5";
		}

		@Override
		public DataCell[] calculate(final ROMol mol,
				final WarningConsolidator warningConsolidator) {
			return new DataCell[0];
		}
	};

	//
	// Members
	//

	/** Column count for descriptors to be calculated. */
	private int m_iColumnCount;

	/** Data types of target column(s) to be calculated. */
	private DataType[] m_arrDataType;

	/** Optional description of the descriptor calculation. */
	private String m_strDescription;

	//
	// Constructor
	//

	/**
	 * Creates a new descriptor enumeration item with one output column of
	 * the specified datatype.
	 */
	private Descriptor(final DataType dataType, final String description) {
		m_iColumnCount = 1;
		m_arrDataType = new DataType[] { dataType };
		m_strDescription = description;
	}

	/**
	 * Creates a new descriptor enumeration item with the specified
	 * number of result columns and the specified datatype for
	 * all of these columns.
	 */
	private Descriptor(final DataType dataType, final int iColumnCount, final String description) {
		m_iColumnCount = iColumnCount;
		m_arrDataType = new DataType[iColumnCount];
		for (int i = 0; i < iColumnCount; i++) {
			m_arrDataType[i] = dataType;
		}
		m_strDescription = description;
	}

	/**
	 * Creates a new descriptor enumeration item with the specified
	 * result datatypes.
	 */
	private Descriptor(final DataType[] dataType, final String description) {
		m_iColumnCount = dataType.length;
		m_arrDataType = dataType.clone();
		m_strDescription = description;
	}

	//
	// Public Methods
	//

	/**
	 * Determines how many columns will be created when calculating this descriptor.
	 * This information must match with the array size of the results of
	 * {@link #getDataTypes()} and {@link #calculate(ROMol)}.
	 * 
	 * @return Number of descriptor columns when calculating this descriptor.
	 */
	public int getColumnCount() {
		return m_iColumnCount;
	}

	/**
	 * Determines the target cell data type(s) of KNIME for this
	 * descriptor calculation.
	 * 
	 * @return KNIME Data Type(s) associated with the descriptor calculation.
	 */
	public DataType[] getDataTypes() {
		return m_arrDataType;
	}

	/**
	 * Determines the preferred column title(s) for the calculated descriptor
	 * columns.
	 * 
	 * @return Column title. If they are not unique in the target table, they
	 * 		will be changed.
	 */
	public String[] getPreferredColumnTitles() {
		final int iColumnCount = getColumnCount();
		final String[] arrRet = new String[iColumnCount];
		final String strName = toString();

		if (iColumnCount == 1) {
			arrRet[0] = strName;
		}
		else {
			final String strStump = strName.split("\\[")[0];
			for (int i = 0; i < iColumnCount; i++) {
				arrRet[i] = strStump + (i + 1);
			}
		}

		return arrRet;
	}

	/**
	 * Returns, if available, the description of the Descriptor describing
	 * in one line, what the Descriptor calculates.
	 * 
	 * @return Description or null, if not set.
	 */
	public String getDescription() {
		return m_strDescription;
	}

	/**
	 * Returns the calculate descriptor values as data cells. The type
	 * of the result cells must exactly match the types returned by
	 * the method {@link #getDataTypes()}
	 * 
	 * @param mol RDKit Molecule to be used for descriptor calculation.
	 * @param warningConsolidator Warning consolidator to save and consolidate
	 * 		warning message, which may occur during descriptor calculation.
	 * 		Can be null.
	 * 
	 * @return The calculated descriptors as data cells.
	 * 		Missing cell(s), if not implemented.
	 */
	public DataCell[] calculate(final ROMol mol,
			final WarningConsolidator warningConsolidator) {
		return AbstractRDKitCellFactory.createEmptyCells(getColumnCount());
	}

	//
	// Protected Methods
	//

	/**
	 * Converts the passed in double vector (RDKit style) into a
	 * DoubleCell array filling non-existing values with missing cells.
	 * Note: This method deletes passed in Double_Vect object afterwards.
	 * It should not be registered for cleaning up before for performance
	 * reasons.
	 * 
	 * @param vecDouble RDKit double vector. Can be null.
	 * @param warningConsolidator Warning consolidator to save and consolidate
	 * 		warning message, which may occur during descriptor calculation.
	 * 		Can be null.
	 * 
	 * @return Array of data cells, which is as long as the result of
	 *  	the call of {@link #getColumnCount()}.
	 */
	protected DataCell[] convertToCellArray(final Double_Vect vecDouble,
			final WarningConsolidator warningConsolidator) {
		final int iColumns = getColumnCount();
		DataCell[] arrCells;

		if (vecDouble == null) {
			arrCells = AbstractRDKitCellFactory.createEmptyCells(iColumns);
		}
		else {
			try {
				arrCells = new DoubleCell[iColumns];
				int iLen = (int)vecDouble.size();

				// If the results are too big, truncate them and warn
				if (iLen > iColumns) {
					if (warningConsolidator != null) {
						warningConsolidator.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
								new StringBuilder("RDKit calculated more descriptor columns than expected for descriptor '")
						.append(toString()).append("': ").append(iLen).append(" instead of ").append(iColumns)
						.append(" - Truncating them.").toString());
					}

					iLen = iColumns;
				}

				// Convert Double_Vect to DoubleCell array
				for (int i = 0; i < iLen; i++) {
					arrCells[i] = new DoubleCell(vecDouble.get(i));
				}

				// Fill the rest with missing cells
				for (int i = iLen; i < iColumns; i++) {
					arrCells[i] = DataType.getMissingCell();
				}
			}
			finally {
				vecDouble.delete();
			}
		}

		return arrCells;
	}

	/**
	 * Converts the passed in int vector (RDKit style) into a
	 * IntCell array filling non-existing values with missing cells.
	 * Note: This method deletes passed in UInt_Vect object afterwards.
	 * It should not be registered for cleaning up before for performance
	 * reasons.
	 * 
	 * @param vecInt RDKit UInt vector. Can be null.
	 * @param warningConsolidator Warning consolidator to save and consolidate
	 * 		warning message, which may occur during descriptor calculation.
	 * 		Can be null.
	 * 
	 * @return Array of data cells, which is as long as the result of
	 *  	the call of {@link #getColumnCount()}.
	 */
	protected DataCell[] convertToCellArray(final UInt_Vect vecInt,
			final WarningConsolidator warningConsolidator) {
		final int iColumns = getColumnCount();
		DataCell[] arrCells;

		if (vecInt == null) {
			arrCells = AbstractRDKitCellFactory.createEmptyCells(iColumns);
		}
		else {
			try {
				arrCells = new IntCell[iColumns];
				int iLen = (int)vecInt.size();

				// If the results are too big, truncate them and warn
				if (iLen > iColumns) {
					if (warningConsolidator != null) {
						warningConsolidator.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
								new StringBuilder("RDKit calculated more descriptor columns than expected for descriptor '")
						.append(toString()).append("': ").append(iLen).append(" instead of ").append(iColumns)
						.append(" - Truncating them.").toString());
					}

					iLen = iColumns;
				}

				// Convert UInt_Vect to IntCell array
				for (int i = 0; i < iLen; i++) {
					arrCells[i] = new IntCell((int)vecInt.get(i));
				}

				// Fill the rest with missing cells
				for (int i = iLen; i < iColumns; i++) {
					arrCells[i] = DataType.getMissingCell();
				}
			}
			finally {
				vecInt.delete();
			}
		}

		return arrCells;
	}

}