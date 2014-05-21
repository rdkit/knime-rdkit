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
package org.rdkit.knime.nodes.onecomponentreaction2;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.chem.types.RxnValue;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelLong;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.rdkit.knime.util.DialogComponentColumnNameSelection;
import org.rdkit.knime.util.LayoutUtils;

/**
 * <code>NodeDialog</code> for the "RDKitOneComponentReaction" Node.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Greg Landrum
 * @author Manuel Schwarze
 */
public abstract class AbstractRDKitReactionNodeDialog extends DefaultNodeSettingsPane {

	//
	// Members
	//

	/** Setting model component for the reaction column selector. */
	private final DialogComponent m_compReactionColumnName;

	/** Setting model component for the SMARTS reaction field. */
	private final DialogComponent m_compSmartsReactionField;

	/** The input port index of the reaction table. */
	private final int m_iReactionTableIndex;

	//
	// Constructor
	//

	/**
	 * Create a new dialog pane with default components to configure an input column,
	 * the name of a new column, which will contain the calculation results, an option
	 * to tell, if the source column shall be removed from the result table.
	 */
	@SuppressWarnings("unchecked")
	public AbstractRDKitReactionNodeDialog(final int iReactionTableIndex) {
		m_iReactionTableIndex = iReactionTableIndex;

		createNewGroup("Reaction");

		addDialogComponentsBeforeReactionSettings();

		super.addDialogComponent(m_compReactionColumnName = new DialogComponentColumnNameSelection(
				createOptionalReactionColumnNameModel(), "RDKit Rxn column: ", m_iReactionTableIndex,
				RxnValue.class) {

			/**
			 * Hides or shows the optional components depending on
			 * the existence of a second input table.
			 */
			@Override
			protected void checkConfigurabilityBeforeLoad(
					final PortObjectSpec[] specs)
							throws NotConfigurableException {

				final boolean bHasReactionTable =
						AbstractRDKitReactionNodeModel.hasReactionInputTable(specs, m_iReactionTableIndex);

				// Only check correctness of second input table if it is there
				if (bHasReactionTable) {
					super.checkConfigurabilityBeforeLoad(specs);
				}

				// Always show or hide proper components
				updateVisibilityOfOptionalComponents(bHasReactionTable);
			}
		});
		super.addDialogComponent(m_compSmartsReactionField = new DialogComponentString(
				createOptionalReactionSmartsPatternModel(), "Reaction SMARTS: ", false, 30));

		createNewGroup("Randomization");

		final SettingsModelBoolean modelRandomizeReactants = createRandomizeReactantsOptionModel();
		super.addDialogComponent(new DialogComponentBoolean(modelRandomizeReactants, "Randomize reactants"));
		super.addDialogComponent(new DialogComponentNumberEdit(createMaxNumberOfRandomizeReactionsModel(modelRandomizeReactants), "Maximum number of random reactions: ", 5));
		super.addDialogComponent(new DialogComponentNumberEdit(createRandomSeedModel(modelRandomizeReactants), "Random seed (or -1 to be ignored): ", 15));

		createNewGroup("Other Options");

		super.addDialogComponent(new DialogComponentBoolean(
				createUniquifyProductsModel(), "Uniquify products"));

		addDialogComponentsAfterReactionSettings();

		LayoutUtils.correctKnimeDialogBorders(getPanel());
	}

	//
	// Protected Methods
	//

	/**
	 * This method adds all dialog components, which shall appear before the
	 * reaction based settings.
	 */
	protected abstract void addDialogComponentsBeforeReactionSettings();

	/**
	 * This method adds all dialog components, which shall appear after the
	 * reaction based settings.
	 */
	protected abstract void addDialogComponentsAfterReactionSettings();

	/**
	 * Show or hides reaction based settings based on the input method for
	 * the reaction (SMARTS text field or table with reactions).
	 * 
	 * @param bHasReactionTable
	 */
	protected void updateVisibilityOfOptionalComponents(final boolean bHasReactionTable) {
		m_compReactionColumnName.getComponentPanel().setVisible(bHasReactionTable);
		m_compSmartsReactionField.getComponentPanel().setVisible(!bHasReactionTable);
	}

	//
	// Static Methods
	//

	/**
	 * Creates the settings model for the name of the reaction column
	 * (if a second input table is used).
	 * 
	 * @return Settings model for the name of the reaction column.
	 */
	static final SettingsModelString createOptionalReactionColumnNameModel() {
		return new SettingsModelString("rxnColumn", null);
	}

	/**
	 * Creates the settings model for the reaction smarts pattern
	 * (if no second input table is used).
	 * 
	 * @return Settings model for the reaction smarts pattern.
	 */
	static final SettingsModelString createOptionalReactionSmartsPatternModel() {
		return new SettingsModelString("reactionSmarts", "");
	}

	/**
	 * @return new settings model whether to uniquify products
	 */
	static final SettingsModelBoolean createUniquifyProductsModel() {
		return new SettingsModelBoolean("uniquifyProducts", false);
	}

	/**
	 * @return new settings model whether to randomize reactants
	 */
	static final SettingsModelBoolean createRandomizeReactantsOptionModel() {
		return new SettingsModelBoolean("randomizeReactants", false);
	}

	/**
	 * @param modelRandomizeReactantsOption Model that determines, if the
	 * 		this option is enabled or disabled.
	 * 
	 * @return new settings model for max number of randomize reactions
	 */
	static final SettingsModelIntegerBounded createMaxNumberOfRandomizeReactionsModel(
			final SettingsModelBoolean modelRandomizeReactantsOption) {
		final SettingsModelIntegerBounded result = new SettingsModelIntegerBounded("maxNumberOfRandomizedReactions", 100, 1, Integer.MAX_VALUE);

		modelRandomizeReactantsOption.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				result.setEnabled(modelRandomizeReactantsOption.getBooleanValue());
			}
		});

		result.setEnabled(modelRandomizeReactantsOption.getBooleanValue());

		return result;
	}

	/**
	 * @param modelRandomizeReactantsOption Model that determines, if the
	 * 		this option is enabled or disabled.
	 * 
	 * @return new settings model for random seed to be used when randomize reactants option is switched on
	 */
	static final SettingsModelLong createRandomSeedModel(
			final SettingsModelBoolean modelRandomizeReactantsOption) {
		final SettingsModelLong result = new SettingsModelLong("randomSeed", -1);

		modelRandomizeReactantsOption.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				result.setEnabled(modelRandomizeReactantsOption.getBooleanValue());
			}
		});

		result.setEnabled(modelRandomizeReactantsOption.getBooleanValue());

		return result;
	}

}
