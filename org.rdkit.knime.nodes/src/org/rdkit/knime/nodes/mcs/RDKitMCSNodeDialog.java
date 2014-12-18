/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2014
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
package org.rdkit.knime.nodes.mcs;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.DialogComponentColumnNameSelection;
import org.rdkit.knime.util.DialogComponentEnumSelection;
import org.rdkit.knime.util.SettingsModelEnumeration;

/**
 * <code>NodeDialog</code> for the "RDKit MCS" Node.
 * 
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Manuel Schwarze
 */
public class RDKitMCSNodeDialog extends DefaultNodeSettingsPane {

	//
	// Constants
	//

	/** The default threshold to be used. */
	public static final double DEFAULT_THRESHOLD = 1.0d;

	/** The default ring matches ring only option to be used. */
	public static final boolean DEFAULT_RING_MATCHES_RING_ONLY_OPTION = false;

	/** The default complete rings only option to be used. */
	public static final boolean DEFAULT_COMPLETE_RINGS_ONLY_OPTION = false;

	/** The default match valences option to be used. */
	public static final boolean DEFAULT_MATCH_VALENCES_OPTION = false;

	/** The default atom comparison option to be used. */
	public static final AtomComparison DEFAULT_ATOM_COMPARISON = AtomComparison.CompareElements;

	/** The default bond comparison option to be used. */
	public static final BondComparison DEFAULT_BOND_COMPARISON = BondComparison.CompareOrder;

	/** The default timeout in seconds to be used. */
	public static final int DEFAULT_TIMEOUT = 300;

	//
	// Constructor
	//

	/**
	 * Create a new dialog pane with default components to configure an input column,
	 * the name of a new column, which will contain the calculation results, an option
	 * to tell, if the source column shall be removed from the result table.
	 */
	RDKitMCSNodeDialog() {
		super.addDialogComponent(new DialogComponentColumnNameSelection(
				createInputColumnNameModel(), "RDKit Mol column: ", 0,
				RDKitMolValue.class));
		super.addDialogComponent(new DialogComponentNumber(createThresholdModel(),
				"Threshold:",
				new Double(0.05d), 4));
		super.addDialogComponent(new DialogComponentBoolean(createRingMatchesRingOnlyOptionModel(),
				"Ring matches ring only"));
		super.addDialogComponent(new DialogComponentBoolean(createCompleteRingsOnlyOptionModel(),
				"Complete rings only"));
		super.addDialogComponent(new DialogComponentBoolean(createMatchValencesOptionModel(),
				"Match valences"));
		super.addDialogComponent(new DialogComponentEnumSelection<AtomComparison>(createAtomComparisonModel(),
				"Atom comparison: "));
		super.addDialogComponent(new DialogComponentEnumSelection<BondComparison>(createBondComparisonModel(),
				"Bond comparison: "));
		super.addDialogComponent(new DialogComponentNumber(createTimeoutModel(),
				"Timeout (in seconds):", 60, 6));
	}

	//
	// Static Methods
	//

	/**
	 * Creates the settings model to be used for the input column.
	 * 
	 * @return Settings model for input column selection.
	 */
	static final SettingsModelString createInputColumnNameModel() {
		return new SettingsModelString("input_column", null);
	}

	/**
	 * Creates the settings model to be used for the threshold,
	 * the fraction of molecules that the MCS must cover.
	 * 
	 * @return Settings model for the threshold,
	 * 		the fraction of molecules that the MCS must cover.
	 */
	static final SettingsModelDoubleBounded createThresholdModel() {
		return new SettingsModelDoubleBounded("threshold", DEFAULT_THRESHOLD, 0.0d, 1.0d);
	}

	/**
	 * Creates the settings model to be used for the ringMatchesRingOnly option.
	 * 
	 * @return Settings model for the ringMatchesRingOnly option.
	 */
	static final SettingsModelBoolean createRingMatchesRingOnlyOptionModel() {
		return new SettingsModelBoolean("ringMatchesRingOnly", DEFAULT_RING_MATCHES_RING_ONLY_OPTION);
	}

	/**
	 * Creates the settings model to be used for the ringMatchesRingOnly option.
	 * 
	 * @return Settings model for the ringMatchesRingOnly option.
	 */
	static final SettingsModelBoolean createCompleteRingsOnlyOptionModel() {
		return new SettingsModelBoolean("completeRingsOnly", DEFAULT_COMPLETE_RINGS_ONLY_OPTION);
	}

	/**
	 * Creates the settings model to be used for the ringMatchesRingOnly option.
	 * 
	 * @return Settings model for the ringMatchesRingOnly option.
	 */
	static final SettingsModelBoolean createMatchValencesOptionModel() {
		return new SettingsModelBoolean("matchValences", DEFAULT_MATCH_VALENCES_OPTION);
	}

	/**
	 * Creates the settings model to be used for the atom comparison parameter.
	 * 
	 * @return Settings model for the atom comparison parameter.
	 */
	static final SettingsModelEnumeration<AtomComparison> createAtomComparisonModel() {
		return new SettingsModelEnumeration<AtomComparison>(AtomComparison.class, "atomComparison", DEFAULT_ATOM_COMPARISON);
	}

	/**
	 * Creates the settings model to be used for the bond comparison parameter.
	 * 
	 * @return Settings model for the bond comparison parameter.
	 */
	static final SettingsModelEnumeration<BondComparison> createBondComparisonModel() {
		return new SettingsModelEnumeration<BondComparison>(BondComparison.class, "bondComparison", DEFAULT_BOND_COMPARISON);
	}

	/**
	 * Creates the settings model to be used for the timeout of the operation.
	 * 
	 * @return Settings model for the timeout of the operation.
	 */
	static final SettingsModelIntegerBounded createTimeoutModel() {
		return new SettingsModelIntegerBounded("timeout", DEFAULT_TIMEOUT, 0, Integer.MAX_VALUE);
	}
}
