/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2012-2023
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
package org.rdkit.knime.nodes.highlighting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.RDKit.Bond;
import org.RDKit.BondIterator;
import org.RDKit.ColourPalette;
import org.RDKit.DrawColour;
import org.RDKit.Int_Vect;
import org.RDKit.MolDraw2DSVG;
import org.RDKit.MolDrawOptions;
import org.RDKit.ROMol;
import org.RDKit.RWMol;
import org.knime.base.data.xml.SvgCell;
import org.knime.base.data.xml.SvgCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.StreamableOperator;
import org.rdkit.knime.nodes.AbstractRDKitCalculatorNodeModel;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.nodes.highlighting.HighlightingDefinition.Type;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.types.RDKitMolValueRenderer;
import org.rdkit.knime.types.preferences.RDKitDepicterPreferencePage;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class implements the node model of the RDKitHighlightingAtoms node
 * providing calculations based on the open source RDKit library. Creates a SVG
 * column showing a molecule with highlighted atoms based on information in the
 * input table. A molecule column as well as a column with a list of the atoms
 * to be highlighted needs to be provided.
 * 
 * @author Manuel Schwarze
 */
public class RDKitHighlightingNodeModel extends AbstractRDKitCalculatorNodeModel {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger.getLogger(RDKitHighlightingNodeModel.class);

	/** Empty atom list to be used if an empty atom list cell is encountered. */
	protected static final Int_Vect EMPTY_ATOM_LIST = new Int_Vect(0);

	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;

	/** Input data info index for Atom List. */
	protected static final int INPUT_COLUMN_ATOM_LIST = 1;

	//
	// Members
	//

	/** Settings model for the column name of the input molecule column. */
	private final SettingsModelString m_modelInputMolColumnName = registerSettings(
			RDKitHighlightingNodeDialog.createInputMolColumnNameModel());

	/**
	 * Settings model for the highlighting definitions.
	 */
	private final SettingsModelHighlighting m_modelHighlighting = registerSettings(
			RDKitHighlightingNodeDialog.createHighlightingDefinitionsModel());

	/**
	 * Settings model for the column name of the new column to be added to the
	 * output table.
	 */
	private final SettingsModelString m_modelNewColumnName = registerSettings(
			RDKitHighlightingNodeDialog.createNewColumnNameModel());

	//
	// Constructor
	//

	/**
	 * Create new node model with one data in- and one out-port.
	 */
	RDKitHighlightingNodeModel() {
		super(1, 1);
	}

	//
	// Protected Methods
	//

	/**
	 * Enable distribution and streaming for this node. {@inheritDoc}
	 */
	@Override
	public StreamableOperator createStreamableOperator(PartitionInfo partitionInfo, PortObjectSpec[] inSpecs)
			throws InvalidSettingsException {
		return createStreamableOperatorForCalculator(partitionInfo, inSpecs);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		// Reset warnings and check RDKit library readiness
		super.configure(inSpecs);

		final WarningConsolidator warnings = getWarningConsolidator();

		// Auto guess the input mol column if not set - fails if no compatible
		// column found
		SettingsUtils.autoGuessColumn(inSpecs[0], m_modelInputMolColumnName, RDKitMolValue.class, 0,
				"Auto guessing: Using column %COLUMN_NAME%.",
				"No RDKit Mol, SMILES or SDF compatible column in input table. Use the \"RDKit from Molecule\" "
						+ "node to convert SMARTS.",
				getWarningConsolidator());

		// Determines, if the input mol column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[0], m_modelInputMolColumnName, RDKitMolValue.class,
				"Input column has not been specified yet.",
				"Input column %COLUMN_NAME% does not exist. Has the input table changed?");

		// Check, if we have already one highlighting definition (initial state)
		// Auto guess the input list column if not set - fails if no
		// compatible column found
		if (m_modelHighlighting.getRowCount() == 1) {
			final HighlightingDefinition def = m_modelHighlighting.getDefinitions()[0];
			if (def.isActive() && def.getType() == Type.Atoms && def.getInputColumn() == null) {
				final SettingsModelString modelTemp = new SettingsModelString("dummy", null);
				SettingsUtils.autoGuessColumn(inSpecs[0], modelTemp, CollectionDataValue.class, 0,
						"Auto guessing: Using column %COLUMN_NAME%.",
						"No Collection type column found in input table, which would contain atoms to be highlighted.",
						getWarningConsolidator());
				m_modelHighlighting.setInputColumn(0, modelTemp.getStringValue());
			}
		}

		// Determines, if all input list columns exists for all activated rows - fails
		// if it does not
		String strWarning = null;
		int iCountWarnings = 0;
		for (final HighlightingDefinition def : m_modelHighlighting.getActivatedDefinitions()) {
			final String strName = def.getInputColumn();
			if (strName == null) {
				strWarning = "Input atom/bond index list column "
						+ "in an activated highlighting definition is not defined yet.";
				iCountWarnings++;
			} else {
				final SettingsModelString modelTemp = new SettingsModelString("dummy", strName);
				SettingsUtils.checkColumnExistence(inSpecs[0], modelTemp, CollectionDataValue.class, null,
						"Input " + def.getType().toString().toLowerCase()
								+ " index list column %COLUMN_NAME% does not exist. Has the input table changed?");
			}
		}

		if (strWarning != null) {
			warnings.saveWarning(strWarning + (iCountWarnings > 1 ? " (" + iCountWarnings + " times)" : ""));
		}

		// Auto guess the new column name and make it unique
		final String strInputMolColumnName = m_modelInputMolColumnName.getStringValue();
		SettingsUtils.autoGuessColumnName(inSpecs[0], null, null, m_modelNewColumnName,
				strInputMolColumnName + " (Highlighting)");

		// Determine, if the new column name has been set and if it is really
		// unique
		SettingsUtils.checkColumnNameUniqueness(inSpecs[0], null, null, m_modelNewColumnName,
				"Output column has not been specified yet.",
				"The name %COLUMN_NAME% of the new column exists already in the input.");

		// Consolidate all warnings and make them available to the user
		generateWarnings();

		// Generate output specs
		return getOutputTableSpecs(inSpecs);
	}

	/**
	 * This implementation generates input data info object for the input mol column
	 * and connects it with the information coming from the appropriate setting
	 * model. {@inheritDoc}
	 */
	@Override
	protected InputDataInfo[] createInputDataInfos(final int inPort, final DataTableSpec inSpec)
			throws InvalidSettingsException {

		InputDataInfo[] arrDataInfo = null;

		// Specify input of table 1
		if (inPort == 0) {
			final HighlightingDefinition[] arrDefs = m_modelHighlighting.getActivatedDefinitions();
			arrDataInfo = new InputDataInfo[1 + arrDefs.length]; // We have one molecule input columns + n index input
																	// columns
			arrDataInfo[INPUT_COLUMN_MOL] = new InputDataInfo(inSpec, null, m_modelInputMolColumnName, "molecule",
					InputDataInfo.EmptyCellPolicy.DeliverEmptyRow, null, RDKitMolValue.class);
			for (int i = 0; i < arrDefs.length; i++) {
				final String strColumnName = arrDefs[i].getInputColumn();
				arrDataInfo[i + 1] = (strColumnName == null ? null
						: new InputDataInfo(inSpec, null, new SettingsModelString("dummy", strColumnName),
								arrDefs[i].getType().toString().toLowerCase() + " list",
								InputDataInfo.EmptyCellPolicy.TreatAsNull, null, CollectionDataValue.class));
			}
		}

		return (arrDataInfo == null ? new InputDataInfo[0] : arrDataInfo);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected AbstractRDKitCellFactory[] createOutputFactories(final int outPort, final DataTableSpec inSpec)
			throws InvalidSettingsException {

		final WarningConsolidator warnings = getWarningConsolidator();
		AbstractRDKitCellFactory[] arrOutputFactories = null;

		// Specify output of table 1
		if (outPort == 0) {
			// Allocate space for all factories (usually we have only one)
			arrOutputFactories = new AbstractRDKitCellFactory[1];

			// Factory 1:
			// ==========
			// Generate column specs for the output table columns produced by this factory
			final DataColumnSpec[] arrOutputSpec = new DataColumnSpec[1]; // We have only one output column
			arrOutputSpec[0] = new DataColumnSpecCreator(m_modelNewColumnName.getStringValue(), SvgCell.TYPE)
					.createSpec();
			final HighlightingDefinition[] arrDefs = m_modelHighlighting.getActivatedDefinitions();

			// Generate factory
			arrOutputFactories[0] = new AbstractRDKitCellFactory(this,
					AbstractRDKitCellFactory.RowFailurePolicy.DeliverEmptyValues, getWarningConsolidator(), null,
					arrOutputSpec) {

				@Override
				/**
				 * This method implements the calculation logic to generate the new cells based
				 * on the input made available in the first (and second) parameter.
				 * {@inheritDoc}
				 */
				public DataCell[] process(final InputDataInfo[] arrInputDataInfo, final DataRow row,
						final long lUniqueWaveId) throws Exception {
					DataCell outputCell = null;

					// Calculate the new cells
					ROMol mol = markForCleanup(arrInputDataInfo[INPUT_COLUMN_MOL].getROMol(row), lUniqueWaveId);

					// Add 2D coordinates if there is no conformer yet (e.g. if RDKit molecule was
					// created from a SMILES)
					// This is necessary for the RDKit changes in the SVG generation
					if (mol.getNumConformers() == 0) {
						RDKitMolValueRenderer.compute2DCoords(mol,
							RDKitDepicterPreferencePage.isUsingCoordGen(),
							RDKitDepicterPreferencePage.isNormalizeDepictions());
					} else {
						// TODO: Could be improved by moving those methods into RDKitMolValueRenderer class,
						// once ROMol.reapplyMolBlockWedging() is available
						if (RDKitDepicterPreferencePage.isUsingMolBlockWedging()) {
							mol = markForCleanup(new RWMol(mol), lUniqueWaveId);
							((RWMol)mol).reapplyMolBlockWedging();
						}
						if (RDKitDepicterPreferencePage.isNormalizeDepictions()) {
							mol.normalizeDepiction(-1, 0);
						}
					}

					boolean bAppliedHighlighting = false;
					final Set<Integer> setAtoms = new HashSet<Integer>();
					final Set<Integer> setBonds = new HashSet<Integer>();
					final HashMap<Integer, DrawColour> mapAtomColors = new HashMap<Integer, DrawColour>();
					final HashMap<Integer, DrawColour> mapBondColors = new HashMap<Integer, DrawColour>();
					final int iBondCount = (int) mol.getNumBonds();

					// Walk through the definitions from bottom to top to overwrite colors in case
					// of
					// overlaps with color definition from upper definitions
					for (int i = arrDefs.length - 1; i >= 0; i--) {
						if (arrInputDataInfo[i + 1] != null) {
							final HighlightingDefinition def = arrDefs[i];
							final DrawColour col = def.getRdkitColor();
							final Int_Vect vectInt = markForCleanup(arrInputDataInfo[i + 1].getRDKitIntegerVector(row),
									lUniqueWaveId);
							bAppliedHighlighting = true;
							if (vectInt != null) {
								final int iCount = (int) vectInt.size();
								if (iCount > 0) {
									switch (def.getType()) {
									case Atoms:
										final Set<Integer> subSetAtoms = new HashSet<Integer>();
										// Add all atoms
										for (int a = 0; a < iCount; a++) {
											final int indexAtom = vectInt.get(a);
											subSetAtoms.add(indexAtom);
											setAtoms.add(indexAtom);
											mapAtomColors.put(indexAtom, col);
										}

										// Figure out which bonds to highlight between atoms, if neighborhood is
										// included
										if (def.isNeighborhoodIncluded()) {
											for (int iBondIndex = 0; iBondIndex < mol.getNumBonds(); ++iBondIndex) {
												final Bond bond = mol.getBondWithIdx(bi);
												final int iBeginAtom = (int) bond.getBeginAtomIdx();
												final int iEndAtom = (int) bond.getEndAtomIdx();
												if (subSetAtoms.contains(iBeginAtom)
														&& subSetAtoms.contains(iEndAtom)) {
													setBonds.add(iBondIndex);
													mapBondColors.put(iBondIndex, col);
												}
											}
										}
										break;
									case Bonds:
										final Set<Integer> subSetBonds = new HashSet<Integer>();
										// Add all bonds
										for (int a = 0; a < iCount; a++) {
											final int indexBond = vectInt.get(a);
											subSetBonds.add(indexBond);
											setBonds.add(indexBond);
											mapBondColors.put(indexBond, col);

											// Figure out which atoms to highlight around bonds, if neighborhood is
											// included
											if (def.isNeighborhoodIncluded() && indexBond < iBondCount) {
												final Bond bond = mol.getBondWithIdx(indexBond);
												final int iBeginAtom = (int) bond.getBeginAtomIdx();
												final int iEndAtom = (int) bond.getEndAtomIdx();
												setAtoms.add(iBeginAtom);
												setAtoms.add(iEndAtom);
												mapAtomColors.put(iBeginAtom, col);
												mapAtomColors.put(iEndAtom, col);
											}
										}
										break;
									}
								}
							}
						}
					}

					// Convert lists into RDKit vectors
					final Int_Vect ivAtoms = new Int_Vect();
					for (final int indexAtom : setAtoms) {
						ivAtoms.add(indexAtom);
					}
					final Int_Vect ivBonds = new Int_Vect();
					for (final int indexBond : setBonds) {
						ivBonds.add(indexBond);
					}
					final ColourPalette mapRdkitAtomColors = new ColourPalette();
					for (final int indexAtom : mapAtomColors.keySet()) {
						final DrawColour col = mapAtomColors.get(indexAtom);
						if (col != null) {
							mapRdkitAtomColors.set(indexAtom, col);
						}
					}
					final ColourPalette mapRdkitBondColors = new ColourPalette();
					for (final int indexBond : mapBondColors.keySet()) {
						final DrawColour col = mapBondColors.get(indexBond);
						if (col != null) {
							mapRdkitBondColors.set(indexBond, col);
						}
					}

					final MolDraw2DSVG molDrawing = markForCleanup(new MolDraw2DSVG(300, 300), lUniqueWaveId);
					MolDrawOptions opts = molDrawing.drawOptions();
					opts.setAddStereoAnnotation(true);
					molDrawing.drawMolecule(mol, "", ivAtoms, ivBonds, mapRdkitAtomColors, mapRdkitBondColors);
					molDrawing.finishDrawing();

					final String xmlSvg = molDrawing.getDrawingText().replaceAll("svg:", "").replaceAll("xmlns:svg=", "xmlns=");

					
					if (xmlSvg != null && !xmlSvg.trim().isEmpty()) {
						// Important: Use the factory here, because using the normal SvgCell contructor
						// causes
						// OutOfMemory exceptions when processing many SVG structures.
						outputCell = SvgCellFactory.create(xmlSvg);
					} else {
						outputCell = DataType.getMissingCell();
					}

					// Store a warning for the row context, if no highlighting could be applied (if
					// no column name specified at all)
					if (!bAppliedHighlighting) {
						warnings.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(),
								"Highlighting could not be applied.");
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
	 * {@inheritDoc} This implementation removes additionally the compound source
	 * column, if specified in the settings.
	 */
	@Override
	protected ColumnRearranger createColumnRearranger(final int outPort, final DataTableSpec inSpec)
			throws InvalidSettingsException {
		// Perform normal work
		final ColumnRearranger result = super.createColumnRearranger(outPort, inSpec);
		return result;
	}
}
