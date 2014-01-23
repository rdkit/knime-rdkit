package org.rdkit.knime.nodes.rdkfingerprint;

import org.RDKit.ExplicitBitVect;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.RDKit.UInt_Vect;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.IntValue;
import org.knime.core.node.InvalidSettingsException;
import org.rdkit.knime.util.ChemUtils;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.StringUtils;

/** Defines supported fingerprint types. */
public enum FingerprintType {

	morgan("Morgan") {
		@Override
		public FingerprintSettings getSpecification(final int iTorsionPathLength, final int iMinPath,
				final int iMaxPath, final int iAtomPairMinPath, final int iAtomPairMaxPath,
				final int iNumBits, final int iRadius, final int iLayerFlags,
				final boolean bIsRooted, final String strAtomListColumnName,
				final boolean bTreatAtomListAsIncludeList) {
			return new DefaultFingerprintSettings(toString(),
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					iNumBits,
					iRadius,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					bIsRooted, strAtomListColumnName, bTreatAtomListAsIncludeList);
		}

		@Override
		public void validateSpecification(final FingerprintSettings settings, final DataTableSpec tableSpec)
				throws InvalidSettingsException {
			super.validateSpecification(settings, tableSpec);
			if (settings.getNumBits() <= 0) {
				throw new InvalidSettingsException("Number of bits must be a positive number > 0.");
			}
			if (settings.getRadius() <= 0) {
				throw new InvalidSettingsException("Radius must be a positive number > 0.");
			}
		}

		@Override
		public boolean canCalculateRootedFingerprint() {
			return true;
		}

		@Override
		public ExplicitBitVect calculate(final ROMol mol, final FingerprintSettings settings) {
			return RDKFuncs.getMorganFingerprintAsBitVect(mol, settings.getRadius(), settings.getNumBits());
		}

		@Override
		public ExplicitBitVect calculateRooted(final ROMol mol,
				UInt_Vect atomList, final FingerprintSettings settings) {
			UInt_Vect atomListToFree = null;

			try {
				if (settings.isTreatAtomListAsExcludeList()) {
					atomList = atomListToFree = ChemUtils.reverseAtomList(mol, atomList);
				}

				return RDKFuncs.getMorganFingerprintAsBitVect(mol, settings.getRadius(), settings.getNumBits(),
						null /* Invariants */, atomList);
			}
			finally {
				if (atomListToFree != null) {
					atomListToFree.delete();
				}
			}
		}
	},

	featmorgan("FeatMorgan") {
		@Override
		public FingerprintSettings getSpecification(final int iTorsionPathLength, final int iMinPath,
				final int iMaxPath, final int iAtomPairMinPath, final int iAtomPairMaxPath,
				final int iNumBits, final int iRadius, final int iLayerFlags,
				final boolean bIsRooted, final String strAtomListColumnName,
				final boolean bTreatAtomListAsIncludeList) {
			return new DefaultFingerprintSettings(toString(),
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					iNumBits,
					iRadius,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					bIsRooted, strAtomListColumnName, bTreatAtomListAsIncludeList);
		}

		@Override
		public void validateSpecification(final FingerprintSettings settings, final DataTableSpec tableSpec)
				throws InvalidSettingsException {
			super.validateSpecification(settings, tableSpec);
			if (settings.getNumBits() <= 0) {
				throw new InvalidSettingsException("Number of bits must be a positive number > 0.");
			}
			if (settings.getRadius() <= 0) {
				throw new InvalidSettingsException("Radius must be a positive number > 0.");
			}
		}

		@Override
		public boolean canCalculateRootedFingerprint() {
			return true;
		}

		@Override
		public ExplicitBitVect calculate(final ROMol mol, final FingerprintSettings settings) {
			final UInt_Vect ivs= new UInt_Vect(mol.getNumAtoms());

			try {
				RDKFuncs.getFeatureInvariants(mol, ivs);
				return RDKFuncs.getMorganFingerprintAsBitVect(mol, settings.getRadius(), settings.getNumBits(), ivs);
			}
			finally {
				ivs.delete();
			}
		}

		@Override
		public ExplicitBitVect calculateRooted(final ROMol mol,
				UInt_Vect atomList, final FingerprintSettings settings) {
			final UInt_Vect ivs= new UInt_Vect(mol.getNumAtoms());
			UInt_Vect atomListToFree = null;

			try {
				if (settings.isTreatAtomListAsExcludeList()) {
					atomList = atomListToFree = ChemUtils.reverseAtomList(mol, atomList);
				}

				RDKFuncs.getFeatureInvariants(mol, ivs);
				return RDKFuncs.getMorganFingerprintAsBitVect(mol, settings.getRadius(), settings.getNumBits(),
						ivs, atomList);
			}
			finally {
				if (atomListToFree != null) {
					atomListToFree.delete();
				}
				ivs.delete();
			}
		}
	},

	atompair("AtomPair") {
		@Override
		public FingerprintSettings getSpecification(final int iTorsionPathLength, final int iMinPath,
				final int iMaxPath, final int iAtomPairMinPath, final int iAtomPairMaxPath,
				final int iNumBits, final int iRadius, final int iLayerFlags,
				final boolean bIsRooted, final String strAtomListColumnName,
				final boolean bTreatAtomListAsIncludeList) {
			return new DefaultFingerprintSettings(toString(),
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					iAtomPairMinPath,
					iAtomPairMaxPath,
					iNumBits,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					bIsRooted, strAtomListColumnName, bTreatAtomListAsIncludeList);
		}

		@Override
		public void validateSpecification(final FingerprintSettings settings, final DataTableSpec tableSpec)
				throws InvalidSettingsException {
			super.validateSpecification(settings, tableSpec);
			if (settings.getNumBits() <= 0) {
				throw new InvalidSettingsException("Number of bits must be a positive number > 0.");
			}
			if (settings.getAtomPairMinPath() != FingerprintSettings.UNAVAILABLE && settings.getAtomPairMinPath() <= 0) {
				throw new InvalidSettingsException("AtomPair minimal path must be a positive number > 0.");
			}
			if (settings.getAtomPairMaxPath() != FingerprintSettings.UNAVAILABLE && settings.getAtomPairMaxPath() <= 0) {
				throw new InvalidSettingsException("AtomPair maximal path must be a positive number > 0.");
			}
			if (settings.getAtomPairMaxPath() < settings.getAtomPairMinPath()) {
				throw new InvalidSettingsException("AtomPair maximal path must be greater than or equal to AtomPair minimal path.");
			}
		}

		@Override
		public boolean canCalculateRootedFingerprint() {
			return true;
		}

		@Override
		public ExplicitBitVect calculate(final ROMol mol, final FingerprintSettings settings) {
			int iAtomPairMinPath = settings.getAtomPairMinPath();
			int iAtomPairMaxPath = settings.getAtomPairMaxPath();

			// Use old default values, if the value is undefined
			if (!settings.isAvailable(iAtomPairMinPath)) {
				iAtomPairMinPath = 1;
			}
			if (!settings.isAvailable(iAtomPairMaxPath)) {
				iAtomPairMaxPath = ((1 << 5) - 1) - 1;
			}

			return RDKFuncs.getHashedAtomPairFingerprintAsBitVect(mol, settings.getNumBits(),
					iAtomPairMinPath, iAtomPairMaxPath);
		}

		@Override
		public ExplicitBitVect calculateRooted(final ROMol mol,
				UInt_Vect atomList, final FingerprintSettings settings) {
			int iAtomPairMinPath = settings.getAtomPairMinPath();
			int iAtomPairMaxPath = settings.getAtomPairMaxPath();

			// Use old default values, if the value is undefined
			if (!settings.isAvailable(iAtomPairMinPath)) {
				iAtomPairMinPath = 1;
			}
			if (!settings.isAvailable(iAtomPairMaxPath)) {
				iAtomPairMaxPath = 30;
			}

			UInt_Vect atomListToFree = null;

			try {
				if (settings.isTreatAtomListAsExcludeList()) {
					atomList = atomListToFree = ChemUtils.reverseAtomList(mol, atomList);
				}

				return RDKFuncs.getHashedAtomPairFingerprintAsBitVect(mol, settings.getNumBits(),
						iAtomPairMinPath, iAtomPairMaxPath, atomList);
			}
			finally {
				if (atomListToFree != null) {
					atomListToFree.delete();
				}
			}
		}
	},

	torsion("Torsion") {
		@Override
		public FingerprintSettings getSpecification(final int iTorsionPathLength, final int iMinPath,
				final int iMaxPath, final int iAtomPairMinPath, final int iAtomPairMaxPath,
				final int iNumBits, final int iRadius, final int iLayerFlags,
				final boolean bIsRooted, final String strAtomListColumnName,
				final boolean bTreatAtomListAsIncludeList) {
			return new DefaultFingerprintSettings(toString(),
					iTorsionPathLength,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					iNumBits,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					bIsRooted, strAtomListColumnName, bTreatAtomListAsIncludeList);
		}

		@Override
		public void validateSpecification(final FingerprintSettings settings, final DataTableSpec tableSpec)
				throws InvalidSettingsException {
			super.validateSpecification(settings, tableSpec);
			if (settings.getNumBits() <= 0) {
				throw new InvalidSettingsException("Number of bits must be a positive number > 0.");
			}
			if (settings.getTorsionPathLength() != FingerprintSettings.UNAVAILABLE && settings.getTorsionPathLength() <= 0) {
				throw new InvalidSettingsException("Torsion path length must be a positive number > 0.");
			}
		}

		@Override
		public boolean canCalculateRootedFingerprint() {
			return true;
		}

		@Override
		public ExplicitBitVect calculate(final ROMol mol, final FingerprintSettings settings) {
			int iTorsionPathLength = settings.getTorsionPathLength();

			// Use old default value, if the value is undefined
			if (!settings.isAvailable(iTorsionPathLength)) {
				iTorsionPathLength = 4;
			}

			return RDKFuncs.getHashedTopologicalTorsionFingerprintAsBitVect(mol, settings.getNumBits(), iTorsionPathLength);
		}

		@Override
		public ExplicitBitVect calculateRooted(final ROMol mol,
				UInt_Vect atomList, final FingerprintSettings settings) {
			int iTorsionPathLength = settings.getTorsionPathLength();

			// Use old default value, if the value is undefined
			if (!settings.isAvailable(iTorsionPathLength)) {
				iTorsionPathLength = 4;
			}

			UInt_Vect atomListToFree = null;

			try {
				if (settings.isTreatAtomListAsExcludeList()) {
					atomList = atomListToFree = ChemUtils.reverseAtomList(mol, atomList);
				}

				return RDKFuncs.getHashedTopologicalTorsionFingerprintAsBitVect(mol, settings.getNumBits(), iTorsionPathLength, atomList);
			}
			finally {
				if (atomListToFree != null) {
					atomListToFree.delete();
				}
			}
		}
	},

	rdkit("RDKit") {
		@Override
		public FingerprintSettings getSpecification(final int iTorsionPathLength, final int iMinPath,
				final int iMaxPath, final int iAtomPairMinPath, final int iAtomPairMaxPath,
				final int iNumBits, final int iRadius, final int iLayerFlags,
				final boolean bIsRooted, final String strAtomListColumnName,
				final boolean bTreatAtomListAsIncludeList) {
			return new DefaultFingerprintSettings(toString(),
					FingerprintSettings.UNAVAILABLE,
					iMinPath,
					iMaxPath,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					iNumBits,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					bIsRooted, strAtomListColumnName, bTreatAtomListAsIncludeList);
		}

		@Override
		public void validateSpecification(final FingerprintSettings settings, final DataTableSpec tableSpec)
				throws InvalidSettingsException {
			super.validateSpecification(settings, tableSpec);
			if (settings.getNumBits() <= 0) {
				throw new InvalidSettingsException("Number of bits must be a positive number > 0.");
			}
			if (settings.getMinPath() <= 0) {
				throw new InvalidSettingsException("Minimal path must be a positive number > 0.");
			}
			if (settings.getMaxPath() <= 0) {
				throw new InvalidSettingsException("Maximal path must be a positive number > 0.");
			}
			if (settings.getMaxPath() < settings.getMinPath()) {
				throw new InvalidSettingsException("Maximal path must be greater than or equal to minimal path.");
			}
		}

		@Override
		public boolean canCalculateRootedFingerprint() {
			return true;
		}

		@Override
		public ExplicitBitVect calculate(final ROMol mol, final FingerprintSettings settings) {
			return RDKFuncs.RDKFingerprintMol(
					mol, settings.getMinPath(), settings.getMaxPath(), settings.getNumBits(), 2);
		}
		@Override
		public ExplicitBitVect calculateRooted(final ROMol mol,
				UInt_Vect atomList, final FingerprintSettings settings) {
			UInt_Vect atomListToFree = null;

			try {
				if (settings.isTreatAtomListAsExcludeList()) {
					atomList = atomListToFree = ChemUtils.reverseAtomList(mol, atomList);
				}

				return RDKFuncs.RDKFingerprintMol(
						mol, settings.getMinPath(), settings.getMaxPath(), settings.getNumBits(), 2,
						true /* Use Hs */, 0.0d /* Tagged density */, 128 /* Min size */,
						true /* Branched paths */, true /* Use bond order */, null /* Atom invariants */,
						atomList);
			}
			finally {
				if (atomListToFree != null) {
					atomListToFree.delete();
				}
			}
		}
	},

	avalon("Avalon") {
		@Override
		public FingerprintSettings getSpecification(final int iTorsionPathLength, final int iMinPath,
				final int iMaxPath, final int iAtomPairMinPath, final int iAtomPairMaxPath,
				final int iNumBits, final int iRadius, final int iLayerFlags,
				final boolean bIsRooted, final String strAtomListColumnName,
				final boolean bTreatAtomListAsIncludeList) {
			return new DefaultFingerprintSettings(toString(),
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					iNumBits,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					RDKFuncs.getAvalonSimilarityBits(), // A constant from the RDKit
					false, null, false);
		}

		@Override
		public void validateSpecification(final FingerprintSettings settings, final DataTableSpec tableSpec)
				throws InvalidSettingsException {
			super.validateSpecification(settings, tableSpec);
			if (settings.getNumBits() <= 0) {
				throw new InvalidSettingsException("Number of bits must be a positive number > 0.");
			}
		}

		@Override
		public boolean canCalculateRootedFingerprint() {
			return false;
		}

		@Override
		public ExplicitBitVect calculate(final ROMol mol, final FingerprintSettings settings) {
			final int bitNumber = settings.getNumBits();
			final ExplicitBitVect fingerprint = new ExplicitBitVect(bitNumber);
			synchronized (AVALON_FP_LOCK) {
				RDKFuncs.getAvalonFP(mol, fingerprint, bitNumber, false, false, settings.getSimilarityBits());
			}
			return fingerprint;
		}

		@Override
		public ExplicitBitVect calculateRooted(final ROMol mol,
				final UInt_Vect atomList, final FingerprintSettings settings) {
			throw new UnsupportedOperationException("Avalon fingerprints cannot be calculated as rooted fingerprints.");
		}
	},

	layered("Layered") {
		@Override
		public FingerprintSettings getSpecification(final int iTorsionPathLength, final int iMinPath,
				final int iMaxPath, final int iAtomPairMinPath, final int iAtomPairMaxPath,
				final int iNumBits, final int iRadius, final int iLayerFlags,
				final boolean bIsRooted, final String strAtomListColumnName,
				final boolean bTreatAtomListAsIncludeList) {
			return new DefaultFingerprintSettings(toString(),
					FingerprintSettings.UNAVAILABLE,
					iMinPath,
					iMaxPath,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					iNumBits,
					FingerprintSettings.UNAVAILABLE,
					iLayerFlags,
					FingerprintSettings.UNAVAILABLE,
					bIsRooted, strAtomListColumnName, bTreatAtomListAsIncludeList);
		}

		@Override
		public void validateSpecification(final FingerprintSettings settings, final DataTableSpec tableSpec)
				throws InvalidSettingsException {
			super.validateSpecification(settings, tableSpec);
			if (settings.getNumBits() <= 0) {
				throw new InvalidSettingsException("Number of bits must be a positive number > 0.");
			}
			if (settings.getMinPath() <= 0) {
				throw new InvalidSettingsException("Minimal path must be a positive number > 0.");
			}
			if (settings.getMaxPath() <= 0) {
				throw new InvalidSettingsException("Maximal path must be a positive number > 0.");
			}
			if (settings.getMaxPath() < settings.getMinPath()) {
				throw new InvalidSettingsException("Maximal path must be greater than or equal to minimal path.");
			}
			if (settings.getLayerFlags() <= 0) {
				throw new InvalidSettingsException("Layer flags must be a positive number > 0.");
			}
		}

		@Override
		public boolean canCalculateRootedFingerprint() {
			return true;
		}

		@Override
		public ExplicitBitVect calculate(final ROMol mol, final FingerprintSettings settings) {
			return RDKFuncs.LayeredFingerprintMol(mol, settings.getLayerFlags(), settings.getMinPath(),
					settings.getMaxPath(), settings.getNumBits());
		}

		@Override
		public ExplicitBitVect calculateRooted(final ROMol mol,
				UInt_Vect atomList, final FingerprintSettings settings) {
			UInt_Vect atomListToFree = null;

			try {
				if (settings.isTreatAtomListAsExcludeList()) {
					atomList = atomListToFree = ChemUtils.reverseAtomList(mol, atomList);
				}

				return RDKFuncs.LayeredFingerprintMol(mol, settings.getLayerFlags(), settings.getMinPath(),
						settings.getMaxPath(), settings.getNumBits(),
						null /* Atom counts */, null /* Set only bits */, true /* Branched paths */,
						atomList);
			}
			finally {
				if (atomListToFree != null) {
					atomListToFree.delete();
				}
			}
		}
	},

	maccs("MACCS") {
		@Override
		public FingerprintSettings getSpecification(final int iTorsionPathLength, final int iMinPath,
				final int iMaxPath, final int iAtomPairMinPath, final int iAtomPairMaxPath,
				final int iNumBits, final int iRadius, final int iLayerFlags,
				final boolean bIsRooted, final String strAtomListColumnName,
				final boolean bTreatAtomListAsIncludeList) {
			return new DefaultFingerprintSettings(toString(),
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					166,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					FingerprintSettings.UNAVAILABLE,
					false, null, false);
		}

		@Override
		public void validateSpecification(final FingerprintSettings settings, final DataTableSpec tableSpec)
				throws InvalidSettingsException {
			super.validateSpecification(settings, tableSpec);
			if (settings.getNumBits() <= 0) {
				throw new InvalidSettingsException("Number of bits must be a positive number > 0.");
			}
		}

		@Override
		public boolean canCalculateRootedFingerprint() {
			return false;
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
			synchronized (MACCS_FP_LOCK) {
				return RDKFuncs.MACCSFingerprintMol(mol);
			}
		}

		@Override
		public ExplicitBitVect calculateRooted(final ROMol mol,
				final UInt_Vect atomList, final FingerprintSettings settings) {
			throw new UnsupportedOperationException("MACCS fingerprints cannot be calculated as rooted fingerprints.");
		}
	};

	//
	// Constants
	//

	/**
	 * This lock prevents two calls at the same time into the MACCS Fingerprint functionality,
	 * which has caused crashes under Linux before.
	 * Once there is a fix implemented in the RDKit (or somewhere else?) we can
	 * remove this lock again.
	 */
	public static final Object MACCS_FP_LOCK = new Object();

	/**
	 * This lock prevents two calls at the same time into the Avalon Fingerprint functionality,
	 * which has caused crashes under Windows 7 before.
	 * Once there is a fix implemented in the RDKit (or somewhere else?) we can
	 * remove this lock again.
	 */
	public static final Object AVALON_FP_LOCK = new Object();

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
	 * @param iTorsionPathLength Torsion Path Length value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iMinPath Min Path value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iMaxPath Min Path value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iAtomPairMinPath Min Path value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iAtomPairMaxPath Min Path value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iNumBits Num Bits (Length) value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iRadius Radius value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param iLayerFlags Layer Flags value. Can be -1 ({@link #UNAVAILABLE}.
	 * @param bIsRooted Flag to set if a rooted fingerprint is desired.
	 * @param strAtomListColumnName Atom list column name for rooted fingerprints.
	 * @param bTreatAtomListAsIncludeList Flag to tell if atom list atoms shall be included (true) or excluded (false).
	 * 
	 * @return Specification of the fingerprint based on the passed in
	 * 		values. Never null.
	 */
	public abstract FingerprintSettings getSpecification(final int iTorsionPathLength, final int iMinPath,
			final int iMaxPath, final int iAtomPairMinPath, final int iAtomPairMaxPath,
			final int iNumBits, final int iRadius, final int iLayerFlags,
			final boolean bIsRooted, final String strAtomListColumn, final boolean bTreatAtomListAsIncludeList);

	/**
	 * Validates the passed in settings for a fingerprint type. This basis method checks two things:
	 * 1. That the setting object is not null, 2. If the fingerprint type can calculate rooted
	 * fingerprints and a rooted fingerprint is desired, it checks that the atom list column name
	 * is set and if a table specification is provided it checks if that column exists.
	 * 
	 * @param settings Fingerprint settings to be validated.
	 * @param tableSpec Table specification that will be used to validate rooted fingerprint settings.
	 * 		This method will look if a rooted fingerprint is desired that the specified atom list column
	 * 		is contained in the specified table specification (if set) and if the data type is correct.
	 * 
	 * @throws InvalidSettingsException Thrown, if settings are invalid and cannot be used.
	 */
	public void validateSpecification(final FingerprintSettings settings, final DataTableSpec tableSpec) throws InvalidSettingsException {
		if (settings == null) {
			throw new InvalidSettingsException("No fingerprint settings available.");
		}
		if (canCalculateRootedFingerprint() && settings.isRooted()) {
			final String strAtomListColumnName = settings.getAtomListColumnName();
			if (StringUtils.isEmptyAfterTrimming(strAtomListColumnName)) {
				throw new InvalidSettingsException("No atom list column name specified for rooted fingerprint calculation.");
			}
			if (tableSpec != null) {
				final DataColumnSpec colSpec = tableSpec.getColumnSpec(strAtomListColumnName);
				if (colSpec == null) {
					throw new InvalidSettingsException("No atom list column found with the name '" + strAtomListColumnName + "'.");
				}
				final DataType dataType = colSpec.getType();
				if ((dataType.isCompatible(IntValue.class) ||
						(dataType.isCollectionType() && dataType.getCollectionElementType().isCompatible(IntValue.class))) == false) {
					throw new InvalidSettingsException("Defined atom list column '" + strAtomListColumnName + "' is no integer collection nor an integer column.");
				}
			}
		}
	}

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
	 * Calculates the fingerprint based on the specified settings. Important:
	 * It is the responsibility of the caller of the function to free memory
	 * for the returned fingerprint when it is not needed anymore. Call
	 * the {@link ExplicitBitVect#delete()} for this purpose.
	 * 
	 * @param Fingerprint settings. Must not be null.
	 * 
	 * @return Fingerprint or null.
	 */
	public abstract ExplicitBitVect calculateRooted(final ROMol mol, final UInt_Vect atomList, final FingerprintSettings settings);

	/**
	 * Determines, if it is possible for a fingerprint type to calculate rooted fingerprints.
	 * 
	 * @return True, if it is possible to calculate rooted fingerprints. False otherwise.
	 */
	public abstract boolean canCalculateRootedFingerprint();

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

	/**
	 * Determines, if the two specified fingerprint setting objects are compatible.
	 * They are compatible if they are both not null and if the settings are the same
	 * except for the detail information for rooted fingerprints (atom list column).
	 * 
	 * @param fps1 Fingerprint settings 1. Can be null.
	 * @param fps2 Fingerprint settings 2. Can be null.
	 * 
	 * @return True, if both settings are compatible. False otherwise.
	 */
	public static boolean isCompatible(final FingerprintSettings fps1, final FingerprintSettings fps2) {
		boolean bRet = false;

		if (fps1 != null && fps2 != null) {
			final DefaultFingerprintSettings fpsCopy1 = new DefaultFingerprintSettings(fps1);
			final DefaultFingerprintSettings fpsCopy2 = new DefaultFingerprintSettings(fps2);

			// Reset the unimportant information
			fpsCopy1.setAtomListColumnName(null);
			fpsCopy2.setAtomListColumnName(null);

			// Compare
			bRet = SettingsUtils.equals(fpsCopy1, fpsCopy2);
		}

		return bRet;
	}
}