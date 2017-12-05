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
package org.rdkit.knime.nodes.rdkfingerprint;

import org.RDKit.ROMol;
import org.RDKit.UInt_Vect;
import org.knime.core.data.DataCell;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.StreamableOperator;

/**
 * This class implements the node model of the "RDKitFingerprint" node
 * providing calculations based on the open source RDKit library.
 * 
 * @author Greg Landrum
 * @author Manuel Schwarze
 */
public class RDKitFingerprintNodeModel extends AbstractRDKitFingerprintNodeModel {

	//
	// Constructors
	//

	/**
	 * Create new node model with one data in- and one outport.
	 */
	RDKitFingerprintNodeModel() {
		super();
	}

	//
	// Protected Methods
	//

   /**
    * Enable distribution and streaming for this node.
    * {@inheritDoc}
    */
   @Override
   public StreamableOperator createStreamableOperator(PartitionInfo partitionInfo, PortObjectSpec[] inSpecs)
         throws InvalidSettingsException {
      return createStreamableOperatorForCalculator(partitionInfo, inSpecs);
   }

	@Override
	protected FingerprintType[] getSupportedFingerprintTypes() {
		return RDKitFingerprintNodeModel.getBitBasedFingerprintTypes();
	}

	@Override
	protected org.knime.core.data.DataType getFingerprintColumnType() {
		return DenseBitVectorCell.TYPE;
	}

	@Override
	protected FingerprintSettings createFingerprintSettings() {
		final FingerprintType fpType = m_modelFingerprintType.getValue();
		final FingerprintSettings settings = (fpType == null ? null : fpType.getSpecification(
				m_modelTorsionPathLength.getIntValue(),
				m_modelMinPath.getIntValue(),
				m_modelMaxPath.getIntValue(),
				m_modelAtomPairMinPath.getIntValue(),
				m_modelAtomPairMaxPath.getIntValue(),
				m_modelNumBits.getIntValue(),
				m_modelRadius.getIntValue(),
				m_modelLayerFlags.getIntValue(),
				m_modelUseChirality.getBooleanValue(),
				m_modelRootedOption.getBooleanValue(),
				m_modelAtomListColumnName.getStringValue(),
				m_modelAtomListHandlingIncludeOption.getBooleanValue(),
				false));
		return settings;
	}

	@Override
	protected DataCell createFingerprintCell(final ROMol mol,
			final FingerprintSettings settings) {
		DataCell outputCell = null;
		final FingerprintType fpType = settings.getRdkitFingerprintType();

		if (fpType != null) {
			final DenseBitVector bitVector = settings.getRdkitFingerprintType().calculateBitBased(mol, settings);

			if (bitVector != null) {
				outputCell = new DenseBitVectorCellFactory(bitVector).createDataCell();
			}
		}

		return outputCell;
	}

	@Override
	protected DataCell createFingerprintCell(final ROMol mol, final UInt_Vect atomList,
			final FingerprintSettings settings) {
		DataCell outputCell = null;
		final FingerprintType fpType = settings.getRdkitFingerprintType();

		if (fpType != null) {
			final DenseBitVector bitVector = fpType.calculateBitBasedRooted(mol, atomList, settings);

			if (bitVector != null) {
				outputCell = new DenseBitVectorCellFactory(bitVector).createDataCell();
			}
		}

		return outputCell;
	}

	//
	// Public Static Methods
	//

	/**
	 * Returns an array of all fingerprint types that support bit-based fingerprint calculation.
	 * 
	 * @return Array of fingerprint types.
	 */
	public static FingerprintType[] getBitBasedFingerprintTypes() {
		return new FingerprintType[] {
				FingerprintType.morgan, FingerprintType.featmorgan,
				FingerprintType.atompair, FingerprintType.torsion, FingerprintType.rdkit, FingerprintType.avalon,
				FingerprintType.layered, FingerprintType.maccs, FingerprintType.pattern
		};
	}
}
