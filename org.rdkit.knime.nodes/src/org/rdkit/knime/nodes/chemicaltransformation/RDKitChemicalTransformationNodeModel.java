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
package org.rdkit.knime.nodes.chemicaltransformation;

import java.util.Arrays;

import org.RDKit.ChemicalReaction;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.RDKit.ROMol_Vect;
import org.RDKit.ROMol_Vect_Vect;
import org.RDKit.RWMol;
import org.knime.chem.types.RxnValue;
import org.knime.chem.types.SmartsValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.nodes.AbstractRDKitCalculatorNodeModel;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.types.RDKitAdapterCell;
import org.rdkit.knime.types.RDKitMolCellFactory;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.ChemUtils;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SafeGuardedResource;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class implements the node model of the RDKitChemicalTransformation node
 * providing calculations based on the open source RDKit library.
 * Transforms a structure into another structure by applying several reactions provided as SMARTS values.
 * 
 * @author Manuel Schwarze
 */
public class RDKitChemicalTransformationNodeModel extends AbstractRDKitCalculatorNodeModel {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitChemicalTransformationNodeModel.class);

	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;

	/** Input data info index for Reaction value. */
	protected static final int INPUT_COLUMN_REACTION = 0;

	/** Warning context for reactions. */
	protected static final WarningConsolidator.Context REACTION_CONTEXT =
			new WarningConsolidator.Context("Reaction", "reaction", "reactions", true);

	//
	// Members
	//

	/** Settings model for the column name of the mol input column. */
	private final SettingsModelString m_modelMolInputColumnName =
			registerSettings(RDKitChemicalTransformationNodeDialog.createMolInputColumnNameModel());

	/** Settings model for the column name of the reaction input column. */
	private final SettingsModelString m_modelReactionInputColumnName =
			registerSettings(RDKitChemicalTransformationNodeDialog.createReactionInputColumnNameModel());

	/** Settings model for the column name of the new column to be added to the output table. */
	private final SettingsModelString m_modelNewColumnName =
			registerSettings(RDKitChemicalTransformationNodeDialog.createNewColumnNameModel());

	/** Settings model for the option to remove the source column from the output table. */
	private final SettingsModelBoolean m_modelRemoveSourceColumns =
			registerSettings(RDKitChemicalTransformationNodeDialog.createRemoveSourceColumnsOptionModel());

	/** Settings model for the option to set the maximum number of reaction cycles. */
	private final SettingsModelIntegerBounded m_modelMaxReactionCycles =
			registerSettings(RDKitChemicalTransformationNodeDialog.createMaxReactionCyclesOptionNameModel());

	// Intermediate results

	/** Valid reactions from second table determined during pre-processing. */
	private ChemicalReaction[] m_arrReactions;

	/** Flag to tell, if the reactions are based on SMARTS (true) or Rxn values (false). */
	private boolean m_bIsSmartsInput;

	//
	// Constructor
	//

	/**
	 * Create new node model with one data in- and one out-port.
	 */
	RDKitChemicalTransformationNodeModel() {
		super(2, 1);
		registerInputTablesWithSizeLimits(1); // Reaction table supports only limited size
		
		m_arrReactions = null;
		getWarningConsolidator().registerContext(REACTION_CONTEXT);
	}

	//
	// Protected Methods
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		// Reset warnings and check RDKit library readiness
		super.configure(inSpecs);

		// Auto guess the mol input column if not set - fails if no compatible column found
		SettingsUtils.autoGuessColumn(inSpecs[0], m_modelMolInputColumnName, RDKitMolValue.class, 0,
				"Auto guessing: Using column %COLUMN_NAME%.",
				"No RDKit Mol, SMILES or SDF compatible column in input table 1. Use the \"RDKit from Molecule\" " +
						"node to convert SMARTS.", getWarningConsolidator());

		// Determines, if the mol input column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[0], m_modelMolInputColumnName, RDKitMolValue.class,
				"Input column for input table 1 has not been specified yet.",
				"Input column %COLUMN_NAME% does not exist in table 1. Has the input table changed?");

		// Auto guess the reaction input column if not set - fails if no compatible column found
		@SuppressWarnings("unchecked")
		final
		Class<? extends DataValue>[] arrClasses = new Class[] { RxnValue.class, SmartsValue.class };
		SettingsUtils.autoGuessColumn(inSpecs[1], m_modelReactionInputColumnName, Arrays.asList(arrClasses), 0,
				"Auto guessing: Using column %COLUMN_NAME%.",
				"No Reaction or SMARTS compatible column in input table 2.", getWarningConsolidator());

		// Determines, if the reaction input column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[1], m_modelReactionInputColumnName, Arrays.asList(arrClasses),
				"Input column for input table 2 has not been specified yet.",
				"Input column %COLUMN_NAME% does not exist in table 2. Has the input table changed?");

		// Auto guess the new column name and make it unique
		final String strInputColumnName = m_modelMolInputColumnName.getStringValue();
		SettingsUtils.autoGuessColumnName(inSpecs[0], null,
				(m_modelRemoveSourceColumns.getBooleanValue() ?
						new String[] { strInputColumnName } : null),
						m_modelNewColumnName, strInputColumnName + " (Transformed)");

		// Determine, if the new column name has been set and if it is really unique
		SettingsUtils.checkColumnNameUniqueness(inSpecs[0], null,
				(m_modelRemoveSourceColumns.getBooleanValue() ? new String[] {
					m_modelMolInputColumnName.getStringValue() } : null),
					m_modelNewColumnName,
					"Output column has not been specified yet.",
				"The name %COLUMN_NAME% of the new column exists already in the input table 1.");

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
	protected InputDataInfo[] createInputDataInfos(final int inPort, final DataTableSpec inSpec)
			throws InvalidSettingsException {

		InputDataInfo[] arrDataInfo = null;

		// Specify input of table 1
		if (inPort == 0) {
			arrDataInfo = new InputDataInfo[1]; // We have only one input column in table 1
			arrDataInfo[INPUT_COLUMN_MOL] = new InputDataInfo(inSpec, m_modelMolInputColumnName,
					InputDataInfo.EmptyCellPolicy.DeliverEmptyRow, null,
					RDKitMolValue.class);
		}

		// Specify input of table 1
		if (inPort == 1) {
			arrDataInfo = new InputDataInfo[1]; // We have only one input column in table 2
			arrDataInfo[INPUT_COLUMN_REACTION] = new InputDataInfo(inSpec, m_modelReactionInputColumnName,
					InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
					RxnValue.class, SmartsValue.class);
		}

		return (arrDataInfo == null ? new InputDataInfo[0] : arrDataInfo);
	}


	/**
	 * Returns the output table specification of the specified out port. This implementation
	 * works based on a ColumnRearranger and delivers only a specification for
	 * out port 0, based on an input table on in port 0. Override this method if
	 * other behavior is needed.
	 * 
	 * @param outPort Index of output port in focus. Zero-based.
	 * @param inSpecs All input table specifications.
	 * 
	 * @return The specification of all output tables.
	 * 
	 * @throws InvalidSettingsException Thrown, if the settings are inconsistent with
	 * 		given DataTableSpec elements.
	 * 
	 * @see #createOutputFactories(int)
	 */
	@Override
	protected DataTableSpec getOutputTableSpec(final int outPort,
			final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		DataTableSpec spec = null;

		if (outPort == 0) {
			// Create the column rearranger, which will generate the spec
			spec = createColumnRearranger(outPort, inSpecs[0]).createSpec();
		}

		return spec;
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
			arrOutputSpec[0] = new DataColumnSpecCreator(
					m_modelNewColumnName.getStringValue(), RDKitAdapterCell.RAW_TYPE)
			.createSpec();

			// Create the chemical reactions to be applied as safe guarded resource to avoid corruption
			// by multiple thread processing
			final SafeGuardedResource<ChemicalReaction[]> chemicalReactions = createSafeGuardedReactionsResource();
			final int iMaxReactionCycles = m_modelMaxReactionCycles.getIntValue();
			final WarningConsolidator warnings = getWarningConsolidator();

			// Generate factory
			arrOutputFactories[0] = new AbstractRDKitCellFactory(this, AbstractRDKitCellFactory.RowFailurePolicy.DeliverEmptyValues,
					getWarningConsolidator(), null, arrOutputSpec) {

				@Override
				/**
				 * This method implements the calculation logic to generate the new cells based on
				 * the input made available in the first (and second) parameter.
				 * {@inheritDoc}
				 */
				public DataCell[] process(final InputDataInfo[] arrInputDataInfo, final DataRow row, final long lUniqueWaveId) throws Exception {
					DataCell outputCell = DataType.getMissingCell();

					// Calculate the new cells
					ROMol mol = markForCleanup(arrInputDataInfo[INPUT_COLUMN_MOL].getROMol(row), lUniqueWaveId);
					final String strSmiles = arrInputDataInfo[INPUT_COLUMN_MOL].getSmiles(row);

					int iReaction = 0;
					final ROMol_Vect vReactant = markForCleanup(new ROMol_Vect(1));

					// Process reactions one after the other
					for (final ChemicalReaction reaction : chemicalReactions.get()) {

						iReaction++;
						int iCycle = 0;

						// Run reaction until max cycle number has been reached or it cannot react anymore
						try {
							while (iCycle < iMaxReactionCycles) {
								// Prepare reactant
								vReactant.set(0, mol);

								// Run reaction
								ROMol molProduct = null;
								final ROMol_Vect_Vect vvProducts = reaction.runReactants(vReactant);

								// If the reaction could be applied to the
								// reactants, we got a non-empty vector
								if (vvProducts != null) {
									if (!vvProducts.isEmpty()) {
										final int iReactionCount = (int)vvProducts.size();

										// Iterate through reactions
										for (int i = 0; i < iReactionCount; i++) {
											final ROMol_Vect vProds = vvProducts.get(i);
											final int iProdsCount = (int)vProds.size();

											// Iterate through reaction products
											for (int j = 0; j < iProdsCount; j++) {

												// Take the very first product only and free the others immediately
												if (i == 0 && j == 0) {
													molProduct = markForCleanup(vProds.get(j), lUniqueWaveId);
												}
												else {
													final ROMol molProductToIgnore = vProds.get(j);
													if (molProductToIgnore != null) {
														molProductToIgnore.delete();
													}
												}
											}

											vProds.delete();
										}
									}

									vvProducts.delete();
								}

								// Use the found product as new input
								if (molProduct != null) {
									// Increase the cycle number as we had a valid reaction
									iCycle++;

									// Instead of sanitization just update the property cache
									molProduct.updatePropertyCache(false);
									mol = molProduct;
								}
								else {
									break;
								}
							}

							LOGGER.debug("Ran reaction " + iReaction + " ('" + getReactionString(reaction) + "') with " + iCycle + " cycles for row '" + row.getKey() + "'.");
						}
						catch (final Exception exc) {
							final String strMsg = "Reaction " + iReaction + " ('" + getReactionString(reaction) + "') failed";
							LOGGER.warn(strMsg + " for molecule '" + strSmiles + "' in row '" + row.getKey() + "', cycle " + iCycle + ".");
							warnings.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(), strMsg +
									" in " + (iCycle == 1 ? "first" : "subsequent") + " cycle.");
						}
					}

					// Use final product as result, but check it for validity sanitizing it
					if (mol != null) {
						final RWMol temp = markForCleanup(new RWMol(mol), lUniqueWaveId);

						if (temp.getNumAtoms() > 0) {
							try {
								RDKFuncs.sanitizeMol(temp);
								outputCell = RDKitMolCellFactory.createRDKitAdapterCell(temp);
							}
							catch (final Exception exc) {
								warnings.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
										"A result product molecule could not be sanitized successfully - Result will be empty.");

								// Output warning in console
								String smiles = "Unknown SMILES";

								try {
									smiles = RDKFuncs.MolToSmiles(temp, false, false, 0, false);
								}
								catch (final Exception excSmiles) {
									// Ignore
								}

								LOGGER.warn("The following result product could not be sanitized: " + smiles);
							}
						}
					}

					return new DataCell[] { outputCell };
				}
			};

			// Enable or disable this factory to allow parallel processing
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

	/**
	 * Returns the percentage of pre-processing activities from the total execution.
	 *
	 * @return Percentage of pre-processing. Returns 0.01d.
	 */
	@Override
	protected double getPreProcessingPercentage() {
		return 0.01d;
	}

	/**
	 * This method gets called from the method {@link #execute(BufferedDataTable[], ExecutionContext)}, before
	 * the row-by-row processing starts. Evaluates all reactions in the second input table and prepares them
	 * for the main processing step.
	 * 
	 * @param inData The input tables of the node.
	 * @param arrInputDataInfo Information about all columns of the input tables.
	 * @param exec The execution context, which was derived as sub-execution context based on the percentage
	 * 		setting of #getPreProcessingPercentage(). Track the progress from 0..1.
	 * 
	 * @throws Exception Thrown, if pre-processing fails.
	 */
	@Override
	protected void preProcessing(final BufferedDataTable[] inData, final InputDataInfo[][] arrInputDataInfo,
			final ExecutionContext exec) throws Exception {
		m_bIsSmartsInput = arrInputDataInfo[1][INPUT_COLUMN_REACTION].isCompatibleOrAdaptable(SmartsValue.class);
		m_arrReactions = ChemUtils.readReactionsFromTable(inData[1], arrInputDataInfo[1][INPUT_COLUMN_REACTION], 1, getWarningConsolidator(), REACTION_CONTEXT);

		// Register reactions for later cleanup
		if (m_arrReactions != null) {
			for (final ChemicalReaction rxn : m_arrReactions) {
				markForCleanup(rxn);
			}
		}

		// Does not do anything by default
		exec.setProgress(1.0d);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanupIntermediateResults() {
		m_arrReactions = null;
	}

	/**
	 * Creates the chemical reaction to be applied as safe guarded resource to avoid corruption
	 * by multiple thread processing. This is based on the intermediate result in {@link #m_arrReactions}.
	 * 
	 * @return Chemical reaction array wrapped in a save guarded resource component.
	 */
	private SafeGuardedResource<ChemicalReaction[]> createSafeGuardedReactionsResource() {
		return markForCleanup(new SafeGuardedResource<ChemicalReaction[]>() {

			@Override
			protected ChemicalReaction[] createResource() {
				final ChemicalReaction[] arrCopy = new ChemicalReaction[m_arrReactions.length];

				for (int i = 0; i < m_arrReactions.length; i++) {
					arrCopy[i] = markForCleanup(new ChemicalReaction(m_arrReactions[i]));
				}

				return arrCopy;
			}
		});
	}

	/**
	 * Returns a string representation for the passed in reaction based on the input reaction table column type.
	 * 
	 * @param reaction Reaction to be converted into a string.
	 * 
	 * @return SMARTS or Rxn value or null, if null was passed in.
	 */
	private String getReactionString(final ChemicalReaction reaction) {
		String strReaction = null;

		if (reaction != null) {
			try {
				strReaction = m_bIsSmartsInput ? ChemicalReaction.ReactionToSmarts(reaction) : ChemicalReaction.ReactionToRxnBlock(reaction);
			}
			catch (final Exception exc) {
				strReaction = "Unknown (an error occurred)";
				LOGGER.debug("Unable to convert RDKit reaction to string.", exc);
			}
		}

		return strReaction;
	}
}
