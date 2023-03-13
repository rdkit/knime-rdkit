/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2021
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
package org.rdkit.knime.nodes.rdkit2svg;

import org.RDKit.MolDraw2DSVG;
import org.RDKit.MolDrawOptions;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.knime.base.data.xml.SvgCell;
import org.knime.base.data.xml.SvgCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.StreamableOperator;
import org.rdkit.knime.nodes.AbstractRDKitCalculatorNodeModel;
import org.rdkit.knime.nodes.AbstractRDKitCellFactory;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.types.RDKitMolValueRenderer;
import org.rdkit.knime.types.preferences.RDKitDepicterPreferencePage;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class implements the node model of the RDKit2SVG node
 * providing calculations based on the open source RDKit library. Creates a SVG
 * column showing a molecule. A molecule column needs to be provided.
 * 
 * @author Greg Landrum
 */
public class RDKit2SVGNodeModel extends AbstractRDKitCalculatorNodeModel {

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger.getLogger(RDKit2SVGNodeModel.class);

	/** Input data info index for Mol value. */
	protected static final int INPUT_COLUMN_MOL = 0;


	//
	// Members
	//

	/** Settings model for the column name of the input column. */
	private final SettingsModelString m_modelInputColumnName =
			registerSettings(RDKit2SVGNodeDialog.createInputColumnNameModel(), "input_column", "first_column");
	// Accept also old deprecated keys

	/** Settings model for the column name of the new column to be added to the output table. */
	private final SettingsModelString m_modelNewColumnName =
			registerSettings(RDKit2SVGNodeDialog.createNewColumnNameModel());

	/** Settings model for the option to remove the source column from the output table. */
	private final SettingsModelBoolean m_modelRemoveSourceColumns =
			registerSettings(RDKit2SVGNodeDialog.createRemoveSourceColumnsOptionModel());

	private final SettingsModelBoolean m_modelClearBackground =
			registerSettings(RDKit2SVGNodeDialog.createClearBackgroundOptionModel());
	
	private final SettingsModelBoolean m_modelDummiesAreAttachments =
			registerSettings(RDKit2SVGNodeDialog.createDummiesAreAttachmentsOptionModel());
	
	private final SettingsModelBoolean m_modelAddAtomIndices =
			registerSettings(RDKit2SVGNodeDialog.createAddAtomIndicesOptionModel());
	
	private final SettingsModelBoolean m_modelAddBondIndices =
			registerSettings(RDKit2SVGNodeDialog.createAddBondIndicesOptionModel());
	
	private final SettingsModelBoolean m_modelIsotopeLabels =
			registerSettings(RDKit2SVGNodeDialog.createIsotopeLabelsOptionModel());
	
	private final SettingsModelBoolean m_modelDummyIsotopeLabels =
			registerSettings(RDKit2SVGNodeDialog.createDummyIsotopeLabelsOptionModel());
	
	private final SettingsModelBoolean m_modelAddStereoAnnotation =
			registerSettings(RDKit2SVGNodeDialog.createAddStereoAnnotationOptionModel());
	
	private final SettingsModelBoolean m_modelCenterBeforeDrawing =
			registerSettings(RDKit2SVGNodeDialog.createCenterBeforeDrawingOptionModel());
	
	private final SettingsModelBoolean m_modelPrepareBeforeDrawing =
			registerSettings(RDKit2SVGNodeDialog.createPrepareBeforeDrawingOptionModel());

	private final SettingsModelBoolean m_modelExplicitMethyl =
			registerSettings(RDKit2SVGNodeDialog.createExplicitMethylOptionModel());
	
	private final SettingsModelBoolean m_modelIncludeRadicals =
			registerSettings(RDKit2SVGNodeDialog.createIncludeRadicalsOptionModel());
	
	private final SettingsModelBoolean m_modelComicModeOption =
			registerSettings(RDKit2SVGNodeDialog.createComicModeOptionModel());

	private final SettingsModelBoolean m_modelBWModeOption =
			registerSettings(RDKit2SVGNodeDialog.createBWModeOptionModel());
	
	private final SettingsModelBoolean m_modelNoAtomLabelsOption =
			registerSettings(RDKit2SVGNodeDialog.createNoAtomLabelsOptionModel());
	
	private final SettingsModelBoolean m_modelIncludeChiralFlagOption =
			registerSettings(RDKit2SVGNodeDialog.createIncludeChiralFlagOptionModel());
	
	private final SettingsModelBoolean m_modelSimplifiedStereoGroupsOption =
			registerSettings(RDKit2SVGNodeDialog.createSimplifiedStereoGroupsOptionModel());
	
	private final SettingsModelBoolean m_modelSingleColorWedgeBondsOption =
			registerSettings(RDKit2SVGNodeDialog.createSingleColorWedgeBondsOptionModel());
	
	/** 
	 * Bond line width setting for backward compatibility reasons. Not directly used anymore, but converted
	 * into {@link #m_modelBondLineWidthDoubleOption} (double for finer lines).
	 */
	private final SettingsModelIntegerBounded m_modelBondLineWidthIntegerOption = 
			registerSettings(RDKit2SVGNodeDialog.createBondLineWidthIntegerOptionModel(), 
					true /* We do not save this setting anymore in the dialog, hence we need to ignore the missing setting */);
	
	/**
	 * Bond line width settings as doubles for finer lines. We ignore a missing setting for old nodes
	 * and convert the old setting to the new one.
	 */
	private final SettingsModelDoubleBounded m_modelBondLineWidthDoubleOption = 
			registerSettings(RDKit2SVGNodeDialog.createBondLineWidthDoubleOptionModel(), true);
	
	private final SettingsModelIntegerBounded m_modelMinFontSizeOption = 
			registerSettings(RDKit2SVGNodeDialog.createMinFontSizeOptionModel());
	
	private final SettingsModelIntegerBounded m_modelMaxFontSizeOption = 
			registerSettings(RDKit2SVGNodeDialog.createMaxFontSizeOptionModel());
	
	private final SettingsModelDoubleBounded m_modelAnnotationFontScaleOption = 
			registerSettings(RDKit2SVGNodeDialog.createAnnotationFontScaleOptionModel());
	
	//
	// Constructor
	//

	/**
	 * Create new node model with one data in- and one out-port.
	 */
	RDKit2SVGNodeModel() {
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
		SettingsUtils.autoGuessColumn(inSpecs[0], m_modelInputColumnName, RDKitMolValue.class, 0,
				"Auto guessing: Using column %COLUMN_NAME%.",
				"No RDKit Mol, SMILES or SDF compatible column in input table. Use the \"RDKit from Molecule\" "
						+ "node to convert SMARTS.",
						warnings);

		// Determines, if the input mol column exists - fails if it does not
		SettingsUtils.checkColumnExistence(inSpecs[0], m_modelInputColumnName, RDKitMolValue.class,
				"Input column has not been specified yet.",
				"Input column %COLUMN_NAME% does not exist. Has the input table changed?");

		// Auto guess the new column name and make it unique
		final String strInputColumnName = m_modelInputColumnName.getStringValue();
		SettingsUtils.autoGuessColumnName(inSpecs[0], null,
				(m_modelRemoveSourceColumns.getBooleanValue() ?
						new String[] { strInputColumnName } : null),
						m_modelNewColumnName, strInputColumnName + " (SVG)");

		// Determine, if the new column name has been set and if it is really unique
		SettingsUtils.checkColumnNameUniqueness(inSpecs[0], null,
				(m_modelRemoveSourceColumns.getBooleanValue() ? new String[] {
					m_modelInputColumnName.getStringValue() } : null),
					m_modelNewColumnName,
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
			arrOutputSpec[0] = new DataColumnSpecCreator(m_modelNewColumnName.getStringValue(), SvgCell.TYPE)
					.createSpec();

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
					final ROMol mol = markForCleanup(arrInputDataInfo[INPUT_COLUMN_MOL].getROMol(row), lUniqueWaveId);

					// Add 2D coordinates if there is no conformer yet (e.g. if RDKit molecule was
					// created from a SMILES)
					if (mol.getNumConformers() == 0) {
						RDKitMolValueRenderer.compute2DCoords(mol, RDKitDepicterPreferencePage.isUsingCoordGen());
					}

					final MolDraw2DSVG molDrawing = markForCleanup(new MolDraw2DSVG(300, 300), lUniqueWaveId);
					MolDrawOptions opts = molDrawing.drawOptions();
					opts.setClearBackground(m_modelClearBackground.getBooleanValue());
					opts.setAddStereoAnnotation(m_modelAddStereoAnnotation.getBooleanValue());
					opts.setAddAtomIndices(m_modelAddAtomIndices.getBooleanValue());
					opts.setAddBondIndices(m_modelAddBondIndices.getBooleanValue());
					opts.setIsotopeLabels(m_modelIsotopeLabels.getBooleanValue());
					opts.setDummiesAreAttachments(m_modelDummiesAreAttachments.getBooleanValue());
					opts.setDummyIsotopeLabels(m_modelDummyIsotopeLabels.getBooleanValue());
					opts.setCentreMoleculesBeforeDrawing(m_modelCenterBeforeDrawing.getBooleanValue());
					opts.setPrepareMolsBeforeDrawing(m_modelPrepareBeforeDrawing.getBooleanValue());
					opts.setExplicitMethyl(m_modelExplicitMethyl.getBooleanValue());
					opts.setIncludeRadicals(m_modelIncludeRadicals.getBooleanValue());
					opts.setComicMode(m_modelComicModeOption.getBooleanValue());
					opts.setNoAtomLabels(m_modelNoAtomLabelsOption.getBooleanValue());
					opts.setIncludeChiralFlagLabel(m_modelIncludeChiralFlagOption.getBooleanValue());
					opts.setSimplifiedStereoGroupLabel(m_modelSimplifiedStereoGroupsOption.getBooleanValue());
					opts.setSingleColourWedgeBonds(m_modelSingleColorWedgeBondsOption.getBooleanValue());
					opts.setBondLineWidth(m_modelBondLineWidthDoubleOption.getDoubleValue());
					opts.setMinFontSize(m_modelMinFontSizeOption.getIntValue());
					opts.setMaxFontSize(m_modelMaxFontSizeOption.getIntValue());
					opts.setAnnotationFontScale(m_modelAnnotationFontScaleOption.getDoubleValue());
					
					if(m_modelBWModeOption.getBooleanValue()) {
						RDKFuncs.assignBWPalette(opts.getAtomColourPalette());
					}
					molDrawing.drawMolecule(mol);
					molDrawing.finishDrawing();

					final String xmlSvg = molDrawing.getDrawingText();

					
					if (xmlSvg != null && !xmlSvg.trim().isEmpty()) {
						// Important: Use the factory here, because using the normal SvgCell contructor
						// causes
						// OutOfMemory exceptions when processing many SVG structures.
						outputCell = SvgCellFactory.create(xmlSvg);
					} else {
						outputCell = DataType.getMissingCell();
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
	 * Corrects the bond line width setting of old nodes, which had stored this value as integer.
	 */
	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		try {
			super.loadValidatedSettingsFrom(settings);
		}
		finally {
			// Adapt bond line width settings of old nodes, which stored it as integer
			int iBondLineWidth = m_modelBondLineWidthIntegerOption.getIntValue();
			if (iBondLineWidth >= 0) {
				LOGGER.warn("Converting integer bond line width into double: " + iBondLineWidth + " => " + (double)iBondLineWidth);
				m_modelBondLineWidthDoubleOption.setDoubleValue((double)iBondLineWidth);
				m_modelBondLineWidthIntegerOption.setIntValue(-1); // Will be saved next time as -1
			}
		}		
	}
}
