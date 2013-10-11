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

import org.RDKit.ExplicitBitVect;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.RDKit.UInt_Vect;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.AbstractRDKitCalculatorNodeModel;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.properties.FingerprintSettingsHeaderProperty;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsModelEnumeration;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class implements the node model of the "RDKitFingerprint" node
 * providing calculations based on the open source RDKit library.
 * 
 * @author Greg Landrum
 * @author Manuel Schwarze
 */
public class RDKitFingerprintNodeModel extends AbstractRDKitCalculatorNodeModel {

	//
	// Enumeration
	//

	/** Defines supported fingerprint types. */
	public enum FingerprintType {
		morgan("Morgan") {
			@Override
			public FingerprintSettings getSpecification(final int iMinPath, final int iMaxPath,
					final int iNumBits, final int iRadius, final int iLayerFlags) {
				return new DefaultFingerprintSettings(toString(),
						FingerprintSettings.UNAVAILABLE,
						FingerprintSettings.UNAVAILABLE,
						iNumBits,
						iRadius,
						FingerprintSettings.UNAVAILABLE,
						FingerprintSettings.UNAVAILABLE);
			}

			@Override
			public ExplicitBitVect calculate(final ROMol mol, final FingerprintSettings settings) {
				return RDKFuncs.getMorganFingerprintAsBitVect(mol, settings.getRadius(), settings.getNumBits());
			}
		},

		featmorgan("FeatMorgan") {
			@Override
			public FingerprintSettings getSpecification(final int iMinPath, final int iMaxPath,
					final int iNumBits, final int iRadius, final int iLayerFlags) {
				return new DefaultFingerprintSettings(toString(),
						FingerprintSettings.UNAVAILABLE,
						FingerprintSettings.UNAVAILABLE,
						iNumBits,
						iRadius,
						FingerprintSettings.UNAVAILABLE,
						FingerprintSettings.UNAVAILABLE);
			}

			@Override
			public ExplicitBitVect calculate(final ROMol mol, final FingerprintSettings settings) {
				final UInt_Vect ivs= new UInt_Vect(mol.getNumAtoms());
				RDKFuncs.getFeatureInvariants(mol, ivs);
				return RDKFuncs.getMorganFingerprintAsBitVect(mol, settings.getRadius(), settings.getNumBits(), ivs);
			}
		},

		atompair("AtomPair") {
			@Override
			public FingerprintSettings getSpecification(final int iMinPath, final int iMaxPath,
					final int iNumBits, final int iRadius, final int iLayerFlags) {
				return new DefaultFingerprintSettings(toString(),
						FingerprintSettings.UNAVAILABLE,
						FingerprintSettings.UNAVAILABLE,
						iNumBits,
						FingerprintSettings.UNAVAILABLE,
						FingerprintSettings.UNAVAILABLE,
						FingerprintSettings.UNAVAILABLE);
			}

			@Override
			public ExplicitBitVect calculate(final ROMol mol, final FingerprintSettings settings) {
				return RDKFuncs.getHashedAtomPairFingerprintAsBitVect(mol, settings.getNumBits());
			}
		},

		torsion("Torsion") {
			@Override
			public FingerprintSettings getSpecification(final int iMinPath, final int iMaxPath,
					final int iNumBits, final int iRadius, final int iLayerFlags) {
				return new DefaultFingerprintSettings(toString(),
						FingerprintSettings.UNAVAILABLE,
						FingerprintSettings.UNAVAILABLE,
						iNumBits,
						FingerprintSettings.UNAVAILABLE,
						FingerprintSettings.UNAVAILABLE,
						FingerprintSettings.UNAVAILABLE);
			}

			@Override
			public ExplicitBitVect calculate(final ROMol mol, final FingerprintSettings settings) {
				return RDKFuncs.getHashedTopologicalTorsionFingerprintAsBitVect(mol, settings.getNumBits());
			}
		},

		rdkit("RDKit") {
			@Override
			public FingerprintSettings getSpecification(final int iMinPath, final int iMaxPath,
					final int iNumBits, final int iRadius, final int iLayerFlags) {
				return new DefaultFingerprintSettings(toString(),
						iMinPath,
						iMaxPath,
						iNumBits,
						FingerprintSettings.UNAVAILABLE,
						FingerprintSettings.UNAVAILABLE,
						FingerprintSettings.UNAVAILABLE);
			}


			@Override
			public ExplicitBitVect calculate(final ROMol mol, final FingerprintSettings settings) {
				return RDKFuncs.RDKFingerprintMol(
						mol, settings.getMinPath(), settings.getMaxPath(), settings.getNumBits(), 2);
			}
		},

		avalon("Avalon") {
			@Override
			public FingerprintSettings getSpecification(final int iMinPath, final int iMaxPath,
					final int iNumBits, final int iRadius, final int iLayerFlags) {
				return new DefaultFingerprintSettings(toString(),
						FingerprintSettings.UNAVAILABLE,
						FingerprintSettings.UNAVAILABLE,
						iNumBits,
						FingerprintSettings.UNAVAILABLE,
						FingerprintSettings.UNAVAILABLE,
						RDKFuncs.getAvalonSimilarityBits()); // A constant from the RDKit
			}

			@Override
			public ExplicitBitVect calculate(final ROMol mol, final FingerprintSettings settings) {
				final int bitNumber = settings.getNumBits();
				final ExplicitBitVect fingerprint = new ExplicitBitVect(bitNumber);
				synchronized (LOCK) {
					RDKFuncs.getAvalonFP(mol, fingerprint, bitNumber, false, false, settings.getSimilarityBits());
				}
				return fingerprint;
			}
		},

		layered("Layered") {
			@Override
			public FingerprintSettings getSpecification(final int iMinPath, final int iMaxPath,
					final int iNumBits, final int iRadius, final int iLayerFlags) {
				return new DefaultFingerprintSettings(toString(),
						iMinPath,
						iMaxPath,
						iNumBits,
						FingerprintSettings.UNAVAILABLE,
						iLayerFlags,
						FingerprintSettings.UNAVAILABLE);
			}

			@Override
			public ExplicitBitVect calculate(final ROMol mol, final FingerprintSettings settings) {
				return RDKFuncs.LayeredFingerprintMol(mol, settings.getLayerFlags(), settings.getMinPath(),
						settings.getMaxPath(), settings.getNumBits());
			}
		},

		maccs("MACCS") {
			@Override
			public FingerprintSettings getSpecification(final int iMinPath, final int iMaxPath,
					final int iNumBits, final int iRadius, final int iLayerFlags) {
				return new DefaultFingerprintSettings(toString(),
						FingerprintSettings.UNAVAILABLE,
						FingerprintSettings.UNAVAILABLE,
						166,
						FingerprintSettings.UNAVAILABLE,
						FingerprintSettings.UNAVAILABLE,
						FingerprintSettings.UNAVAILABLE);
			}

			/**
			 * Calculates the fingerprint for the specified molecule based on the
			 * specified settings.
			 * 
			 * @param mol Molecule to calculate fingerprint for. Must not be null.
			 * @param settings Fingerprint settings to apply. Must not be null.
			 * 
			 * @throws NullPointerException Thrown, if one of the settings is null.
			 */
			@Override
			public ExplicitBitVect calculate(final ROMol mol, final FingerprintSettings settings) {
				return RDKFuncs.MACCSFingerprintMol(mol);
			}
		};

		//
		// Members
		//

		private final String m_strName;

		//
		// Constructors
		//

		/**
		 * Creates a new fingerprint type enumeration value.
		 * 
		 * @param strName Name to be shown as string representation.
		 */
		private FingerprintType(final String strName) {
			m_strName = strName;
		}

		/**
		 * Creates a new fingerprint settings object for a fingerprint type.
		 * Not all parameters are used for all fingerprints. This method
		 * takes are that only those parameters are included in the
		 * fingerprint specification, if they are are really used.
		 * 
		 * @param iMinPath Min Path value. Can be -1 ({@link #UNAVAILABLE}.
		 * @param iMaxPath Min Path value. Can be -1 ({@link #UNAVAILABLE}.
		 * @param iNumBits Num Bits (Length) value. Can be -1 ({@link #UNAVAILABLE}.
		 * @param iRadius Radius value. Can be -1 ({@link #UNAVAILABLE}.
		 * @param iLayerFlags Layer Flags value. Can be -1 ({@link #UNAVAILABLE}.
		 * 
		 * @return Specification of the fingerprint based on the passed in
		 * 		values. Never null.
		 */
		public abstract FingerprintSettings getSpecification(final int iMinPath,
				final int iMaxPath, final int iNumBits, final int iRadius, final int iLayerFlags);

		/**
		 * Calculates the fingerprint based on the specified settings. Important:
		 * It is the responsibility of the caller of the function to free memory
		 * for the returned fingerprint when it is not needed anymore. Call
		 * the {@link ExplicitBitVect#delete()} for this purpose.
		 * 
		 * @param Fingerprint settings. Must not be null.
		 * 
		 * @return Fingerprint or null.
		 */
		public abstract ExplicitBitVect calculate(final ROMol mol, final FingerprintSettings settings);

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return m_strName;
		}

		/**
		 * Tries to determine the fingerprint type based on the passed in string. First it
		 * will try to determine it by assuming that the passed in string is the
		 * name of the fingerprint type ({@link #name()}. If this fails, it will compare the
		 * string representation trying to find a match there ({@link #toString()}.
		 * If none is found it will return null.
		 */
		public static FingerprintType parseString(String str) {
			FingerprintType type = null;

			if (str != null) {
				try {
					type = FingerprintType.valueOf(str);
				}
				catch (final IllegalArgumentException exc) {
					// Ignored here
				}

				if (type == null) {
					str = str.trim().toUpperCase();
					for (final FingerprintType typeExisting : FingerprintType.values()) {
						if (str.equals(typeExisting.toString().toUpperCase())) {
							type = typeExisting;
							break;
						}
					}
				}
			}

			return type;
		}
	}

	//
	// Constants
	//

	/** The logger instance. */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitFingerprintNodeModel.class);

	/**
	 * This lock prevents two calls at the same time into the RDKKit FeatureInvariants
	 * and Avalon Fingerprint functionality, which has caused crashes under Windows 7 before.
	 * Once there is a fix implemented in the RDKit (or somewhere else?) we can
	 * remove this LOCK again.
	 */
	private static final Object LOCK = new Object();

	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;

	//
	// Members
	//

	/** Model for the input column. */
	private final SettingsModelString m_modelInputColumnName =
			registerSettings(RDKitFingerprintNodeDialog.createSmilesColumnModel(), true);

	/** Model for the name of the new column. */
	private final SettingsModelString m_modelNewColumnName =
			registerSettings(RDKitFingerprintNodeDialog.createNewColumnModel(), true);

	/** Model for the option to remove the input column. */
	private final SettingsModelBoolean m_modelRemoveSourceColumns =
			registerSettings(RDKitFingerprintNodeDialog.createBooleanModel(), true);

	/** Model for the fingerprint type to apply. */
	private final SettingsModelEnumeration<FingerprintType> m_modelFingerprintType =
			registerSettings(RDKitFingerprintNodeDialog.createFPTypeModel(), true);

	/** Model for the minimum path length to be used for calculations. */
	private final SettingsModelIntegerBounded m_modelMinPath =
			registerSettings(RDKitFingerprintNodeDialog.createMinPathModel(), true);

	/** Model for the maximum path length to be used for calculations. */
	private final SettingsModelIntegerBounded m_modelMaxPath =
			registerSettings(RDKitFingerprintNodeDialog.createMaxPathModel(), true);

	/** Model for the number of fingerprint bits to be used for calculations. */
	private final SettingsModelIntegerBounded m_modelNumBits =
			registerSettings(RDKitFingerprintNodeDialog.createNumBitsModel(), true);

	/** Model for the radius to be used for calculations. */
	private final SettingsModelIntegerBounded m_modelRadius =
			registerSettings(RDKitFingerprintNodeDialog.createRadiusModel(), true);

	/** Model for the layer flags to be used for calculations. */
	private final SettingsModelIntegerBounded m_modelLayerFlags =
			registerSettings(RDKitFingerprintNodeDialog.createLayerFlagsModel(), true);

	//
	// Constructors
	//

	/**
	 * Create new node model with one data in- and one outport.
	 */
	RDKitFingerprintNodeModel() {
		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		// Reset warnings and check RDKit library readiness
		super.configure(inSpecs);

		// Auto guess the input column if not set - fails if no compatible column found
		SettingsUtils.autoGuessColumn(inSpecs[0], m_modelInputColumnName, RDKitMolValue.class, 0,
				"Auto guessing: Using column %COLUMN_NAME%.",
				"No RDKit Mol, SMILES or SDF compatible column in input table. Use the \"Molecule to RDKit\" " +
						"node to convert SMARTS.", getWarningConsolidator());

		// Determines, if the input column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[0], m_modelInputColumnName, RDKitMolValue.class,
				"Input column has not been specified yet.",
				"Input column %COLUMN_NAME% does not exist. Has the input table changed?");

		// Auto guess the new column name and make it unique
		final String strInputColumnName = m_modelInputColumnName.getStringValue();
		SettingsUtils.autoGuessColumnName(inSpecs[0], null,
				(m_modelRemoveSourceColumns.getBooleanValue() ?
						new String[] { strInputColumnName } : null),
						m_modelNewColumnName, strInputColumnName + " (Fingerprint)");

		// Determine, if the new column name has been set and if it is really unique
		SettingsUtils.checkColumnNameUniqueness(inSpecs[0], null,
				(m_modelRemoveSourceColumns.getBooleanValue() ? new String[] {
					m_modelInputColumnName.getStringValue() } : null),
					m_modelNewColumnName,
					"Output column has not been specified yet.",
				"The name %COLUMN_NAME% of the new column exists already in the input.");

		// Determine, if path values are in conflict
		if (m_modelMinPath.getIntValue() > m_modelMaxPath.getIntValue()) {
			throw new InvalidSettingsException("Minimum path length is larger than maximum path length.");
		}

		// Consolidate all warnings and make them available to the user
		generateWarnings();

		// Generate output specs
		return getOutputTableSpecs(inSpecs);
	}

	/**
	 * This implementation generates input data info object for the input mol column
	 * and connects it with the information coming from the appropriate setting model.
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected InputDataInfo[] createInputDataInfos(final int inPort, final DataTableSpec inSpec)
			throws InvalidSettingsException {

		InputDataInfo[] arrDataInfo = null;

		// Specify input of table 1
		if (inPort == 0) {
			arrDataInfo = new InputDataInfo[1]; // We have only one input column
			arrDataInfo[INPUT_COLUMN_MOL] = new InputDataInfo(inSpec, m_modelInputColumnName,
					InputDataInfo.EmptyCellPolicy.DeliverEmptyRow, null,
					RDKitMolValue.class);
		}

		return (arrDataInfo == null ? new InputDataInfo[0] : arrDataInfo);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected AbstractRDKitCellFactory[] createOutputFactories(final int outPort, final DataTableSpec inSpec)
			throws InvalidSettingsException {

		AbstractRDKitCellFactory[] arrOutputFactories = null;

		// Specify output of table 1
		if (outPort == 0) {
			// Allocate space for all factories (usually we have only one)
			arrOutputFactories = new AbstractRDKitCellFactory[1];

			// Factory 1:
			// ==========
			// Generate column specs for the output table columns produced by this factory
			final DataColumnSpec[] arrOutputSpec = new DataColumnSpec[1]; // We have only one output column
			final DataColumnSpecCreator creator = new DataColumnSpecCreator(
					m_modelNewColumnName.getStringValue(), DenseBitVectorCell.TYPE);
			// Add fingerprint specification properties to column header
			final FingerprintType fpType = m_modelFingerprintType.getValue();
			if (fpType != null) {
				new FingerprintSettingsHeaderProperty(fpType.getSpecification(
						m_modelMinPath.getIntValue(),
						m_modelMaxPath.getIntValue(),
						m_modelNumBits.getIntValue(),
						m_modelRadius.getIntValue(),
						m_modelLayerFlags.getIntValue())).writeToColumnSpec(creator);
			}
			arrOutputSpec[0] = creator.createSpec();

			// Generate factory
			arrOutputFactories[0] = new AbstractRDKitCellFactory(this, AbstractRDKitCellFactory.RowFailurePolicy.DeliverEmptyValues,
					getWarningConsolidator(), null, arrOutputSpec) {

				@Override
				/**
				 * This method implements the calculation logic to generate the new cells based on
				 * the input made available in the first (and second) parameter.
				 * {@inheritDoc}
				 */
				public DataCell[] process(final InputDataInfo[] arrInputDataInfo, final DataRow row, final int iUniqueWaveId) throws Exception {
					DataCell outputCell = null;

					// Calculate the new cells
					final ROMol mol = markForCleanup(arrInputDataInfo[INPUT_COLUMN_MOL].getROMol(row), iUniqueWaveId);

					// Calculate fingerprint
					final FingerprintType fpType = m_modelFingerprintType.getValue();
					final FingerprintSettings settings = fpType.getSpecification(
							m_modelMinPath.getIntValue(),
							m_modelMaxPath.getIntValue(),
							m_modelNumBits.getIntValue(),
							m_modelRadius.getIntValue(),
							m_modelLayerFlags.getIntValue());
					ExplicitBitVect fingerprint = null;

					try {
						fingerprint = markForCleanup(fpType.calculate(mol, settings), iUniqueWaveId);
					}
					catch (final Exception exc) {
						final String strMsg = "Fingerprint Type '" + m_modelFingerprintType.getValue() +
								"' could not be calculated.";
						LOGGER.error(strMsg);
						throw new RuntimeException(strMsg, exc);
					}

					// Transfer to bit vector
					if (fingerprint != null) {
						// Transfer the bitset into a dense bit vector
						final long lBits = fingerprint.getNumBits();
						final DenseBitVector bitVector = new DenseBitVector(lBits);
						for (long i = 0; i < lBits; i++) {
							if (fingerprint.getBit(i)) {
								bitVector.set(i);
							}
						}

						outputCell = new DenseBitVectorCellFactory(bitVector).createDataCell();
					}
					else {
						getWarningConsolidator().saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
								"Error computing fingerprint - Setting value as missing cell.");
						outputCell = DataType.getMissingCell();
					}

					return new DataCell[] { outputCell };
				}
			};

			// Enable this factory to allow parallel processing
			arrOutputFactories[0].setAllowParallelProcessing(true);
		}

		return (arrOutputFactories == null ? new AbstractRDKitCellFactory[0] : arrOutputFactories);
	}

	/**
	 * {@inheritDoc}
	 * This implementation removes additionally the compound source column, if specified in the settings.
	 */
	@Override
	protected ColumnRearranger createColumnRearranger(final int outPort,
			final DataTableSpec inSpec) throws InvalidSettingsException {
		// Perform normal work
		final ColumnRearranger result = super.createColumnRearranger(outPort, inSpec);

		// Remove the input column, if desired
		if (m_modelRemoveSourceColumns.getBooleanValue()) {
			result.remove(createInputDataInfos(0, inSpec)[INPUT_COLUMN_MOL].getColumnIndex());
		}

		return result;
	}
}
