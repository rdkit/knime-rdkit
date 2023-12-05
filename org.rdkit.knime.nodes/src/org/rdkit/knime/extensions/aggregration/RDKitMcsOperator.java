/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2014-2023
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
package org.rdkit.knime.extensions.aggregration;

import org.RDKit.ROMol;
import org.RDKit.ROMol_Vect;
import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.chem.types.SmartsCell;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.node.NodeLogger;
import org.rdkit.knime.nodes.RDKitNodePlugin;
import org.rdkit.knime.nodes.mcs.AtomComparison;
import org.rdkit.knime.nodes.mcs.BondComparison;
import org.rdkit.knime.nodes.mcs.MCSUtils;
import org.rdkit.knime.types.RDKitMolValue;

/**
 * RDKit MCS Operator for aggregrations done with the KNIME GroupBy node.
 * 
 * @author Manuel Schwarze
 */
public class RDKitMcsOperator extends AggregationOperator {

	/** The logging instance. */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(MCSUtils.class);

	/** The result type of the aggregration. */
	private static final DataType TYPE = SmartsCell.TYPE;

	/** Intermediate storage for molecules to be aggregrated. */
	private ROMol_Vect m_mols = null;

	//
	// Constructor
	//

	public RDKitMcsOperator() {
		this(GlobalSettings.DEFAULT, OperatorColumnSettings.DEFAULT_EXCL_MISSING);
	}

	/**
	 * Constructor for class MedianDateOperator.
	 * 
	 * @param globalSettings the global settings
	 * @param opColSettings the operator column specific settings
	 */
	protected RDKitMcsOperator(final GlobalSettings globalSettings,
			final OperatorColumnSettings opColSettings) {
		super(new OperatorData("org.rdkit.knime.extensions.aggregation.mcs", "RDKit MCS",
				"RDKit MCS", true /* Use Limit */, false /* Do not keep the column spec */,
				RDKitMolValue.class, false /* Do not support missing values */),
				globalSettings, AggregationOperator.setInclMissingFlag(opColSettings, false));
	}

	//
	// Public Methods
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AggregationOperator createInstance(final GlobalSettings globalSettings,
			final OperatorColumnSettings opColSettings) {
		return new RDKitMcsOperator(globalSettings, opColSettings);
	}

	/**
	 * Returns the threshold to be used, which gets retrieved from the preferences.
	 * If not found it will return a default value.
	 * 
	 * @return Threshold.
	 */
	public double getThreshold() {
		double dThreshold = RDKitMcsAggregationPreferencePage.DEFAULT_THRESHOLD;

		try {
			dThreshold = RDKitNodePlugin.getDefault().getPreferenceStore().getDouble(RDKitMcsAggregationPreferencePage.PREF_KEY_THRESHOLD);
		}
		catch (final Exception exc) {
			LOGGER.error("Unable to retrieve preference for MCS Threshold. Using default.", exc);
		}

		return dThreshold;
	}

	/**
	 * Returns the ringMatchesRingOnlyOption to be used, which gets retrieved from the preferences.
	 * If not found it will return a default value.
	 * 
	 * @return RingMatchesRingOnlyOption.
	 */
	public boolean getRingMatchesRingOnlyOption() {
		boolean bOption = RDKitMcsAggregationPreferencePage.DEFAULT_RING_MATCHES_RING_ONLY_OPTION;

		try {
			bOption = RDKitNodePlugin.getDefault().getPreferenceStore().getBoolean(RDKitMcsAggregationPreferencePage.PREF_KEY_RING_MATCHES_RING_ONLY_OPTION);
		}
		catch (final Exception exc) {
			LOGGER.error("Unable to retrieve preference for MCS RingMatchesRingOnlyOption. Using default.", exc);
		}

		return bOption;
	}

	/**
	 * Returns the completeRingsOnlyOption to be used, which gets retrieved from the preferences.
	 * If not found it will return a default value.
	 * 
	 * @return CompleteRingsOnlyOption.
	 */
	public boolean getCompleteRingsOnlyOption() {
		boolean bOption = RDKitMcsAggregationPreferencePage.DEFAULT_COMPLETE_RINGS_ONLY_OPTION;

		try {
			bOption = RDKitNodePlugin.getDefault().getPreferenceStore().getBoolean(RDKitMcsAggregationPreferencePage.PREF_KEY_COMPLETE_RINGS_ONLY_OPTION);
		}
		catch (final Exception exc) {
			LOGGER.error("Unable to retrieve preference for MCS CompleteRingsOnlyOption. Using default.", exc);
		}

		return bOption;
	}

	/**
	 * Returns the matchValencesOption to be used, which gets retrieved from the preferences.
	 * If not found it will return a default value.
	 * 
	 * @return MatchValencesOption.
	 */
	public boolean getMatchValencesOption() {
		boolean bOption = RDKitMcsAggregationPreferencePage.DEFAULT_MATCH_VALENCES_OPTION;

		try {
			bOption = RDKitNodePlugin.getDefault().getPreferenceStore().getBoolean(RDKitMcsAggregationPreferencePage.PREF_KEY_MATCH_VALENCES_OPTION);
		}
		catch (final Exception exc) {
			LOGGER.error("Unable to retrieve preference for MCS MatchValencesOption. Using default.", exc);
		}

		return bOption;
	}

	/**
	 * Returns the Atom Comparison mode to be used, which gets retrieved from the preferences.
	 * If not found it will return a default value.
	 * 
	 * @return Threshold.
	 */
	public AtomComparison getAtomComparison() {
		AtomComparison atomComparison = null;

		try {
			atomComparison = AtomComparison.parseString(
					RDKitNodePlugin.getDefault().getPreferenceStore().getString(RDKitMcsAggregationPreferencePage.PREF_KEY_ATOM_COMPARISON));
		}
		catch (final Exception exc) {
			LOGGER.error("Unable to retrieve the preference for MCS Atom Comparison. Using default.", exc);
		}

		if (atomComparison == null) {
			atomComparison = RDKitMcsAggregationPreferencePage.DEFAULT_ATOM_COMPARISON;
			LOGGER.error("Unable to parse the preference for MCS Atom Comparison. Using default.");
		}

		return atomComparison;
	}

	/**
	 * Returns the Atom Comparison mode to be used, which gets retrieved from the preferences.
	 * If not found it will return a default value.
	 * 
	 * @return Threshold.
	 */
	public BondComparison getBondComparison() {
		BondComparison bondComparison = null;

		try {
			bondComparison = BondComparison.parseString(
					RDKitNodePlugin.getDefault().getPreferenceStore().getString(RDKitMcsAggregationPreferencePage.PREF_KEY_BOND_COMPARISON));
		}
		catch (final Exception exc) {
			LOGGER.error("Unable to retrieve the preference for MCS Bond Comparison. Using default.", exc);
		}

		if (bondComparison == null) {
			bondComparison = RDKitMcsAggregationPreferencePage.DEFAULT_BOND_COMPARISON;
			LOGGER.error("Unable to parse the preference for MCS Bond Comparison. Using default.");
		}

		return bondComparison;
	}

	/**
	 * Returns the timeout to be used, which gets retrieved from the preferences.
	 * If not found it will return a default value.
	 * 
	 * @return Threshold.
	 */
	public int getTimeout() {
		int iTimeout = RDKitMcsAggregationPreferencePage.DEFAULT_TIMEOUT;

		try {
			iTimeout = RDKitNodePlugin.getDefault().getPreferenceStore().getInt(RDKitMcsAggregationPreferencePage.PREF_KEY_TIMEOUT);
		}
		catch (final Exception exc) {
			LOGGER.error("Unable to retrieve preference for MCS Timeout. Using default.", exc);
		}

		return iTimeout;
	}

	//
	// Protected Methods
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected synchronized boolean computeInternal(final DataCell cell) {
		if (m_mols == null) {
			m_mols = new ROMol_Vect();
		}

		if (m_mols.size() >= getMaxUniqueValues()) {
			setSkipMessage("Group contains too many values");
			return true;
		}

		if (!cell.isMissing()) {
			try {
				final ROMol mol = ((RDKitMolValue)cell).readMoleculeValue();
				m_mols.add(mol);
			}
			catch (final OutOfMemoryError e) {
				// Cleanup all input molecules
				resetInternal();
				throw new IllegalArgumentException(
						"Maximum unique values number too big");
			}

		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataType getDataType(final DataType origType) {
		return TYPE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataCell getResultInternal() {
		DataCell cellResult = DataType.getMissingCell();

		// Get settings from preferences
		final double dThreshold = getThreshold();
		final boolean bRingMatchesRingOnly = getRingMatchesRingOnlyOption();
		final boolean bCompleteRingsOnly = getCompleteRingsOnlyOption();
		final boolean bMatchValencesOption = getMatchValencesOption();
		final AtomComparison atomComparison = getAtomComparison();
		final BondComparison bondComparison = getBondComparison();
		final int iTimeout = getTimeout();

		try {
			final DataCell[] arrResults = MCSUtils.calculateMCS(m_mols, dThreshold,
					bRingMatchesRingOnly, bCompleteRingsOnly, bMatchValencesOption,
					atomComparison, bondComparison, iTimeout, null);
			cellResult = arrResults[MCSUtils.SMARTS_INDEX];

			// Generate warning, if no MCS was found
			if (arrResults[MCSUtils.SMARTS_INDEX] == null || arrResults[MCSUtils.SMARTS_INDEX].isMissing()) {
				if (m_mols.size() > 0) {
					LOGGER.warn("RDKit MCS Aggregation: No MCS found - Created empty cell.");
				}
				else {
					LOGGER.warn("RDKit MCS Aggregation: No input molecules found - Created empty cell.");
				}
			}
			else if (arrResults[MCSUtils.TIMED_OUT_INDEX] != null &&
					!arrResults[MCSUtils.TIMED_OUT_INDEX].isMissing() &&
					((BooleanCell)arrResults[MCSUtils.TIMED_OUT_INDEX]).getBooleanValue() == true) {
				LOGGER.warn("RDKit MCS Aggregation: The MCS calculation timed out.");
			}
		}
		catch (final Exception exc) {
			LOGGER.error("RDKit MCS Aggregation: An error occurred - Created empty cell.", exc);
		}
		finally {
			// Cleanup all input molecules
			resetInternal();
		}

		return cellResult;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected synchronized void resetInternal() {
		if (m_mols != null) {
			for (int i = 0; i < m_mols.size(); i++) {
				final ROMol mol = m_mols.get(i);
				if (mol != null) {
					mol.delete();
				}
			}
			m_mols.delete();
			m_mols = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "Calculates the Most Common Substructure (MCS) with the RDKit. If the operation times out, the incomplete MCS is used as result";
	}
}