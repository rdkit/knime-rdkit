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
package org.rdkit.knime.nodes.rdkit2svg;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.RDKit.EmbedParameters;
import org.RDKit.MolDrawOptions;
import org.RDKit.RDKFuncs;
import org.knime.base.data.aggregation.dialogutil.DataColumnSpecTableCellRenderer;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButton;
import org.knime.core.node.defaultnodesettings.DialogComponentDoubleRange;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.rdkit.knime.nodes.highlighting.HighlightingDefinition.Type;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.DialogComponentColumnNameSelection;
import org.rdkit.knime.util.DialogComponentSeparator;
import org.rdkit.knime.util.DialogComponentTable;
import org.rdkit.knime.util.LayoutUtils;
import org.rdkit.knime.util.TableCellAction;
import org.rdkit.knime.util.TableColorCellEditor;

/**
 * <code>NodeDialog</code> for the "RDKit2SVG" Node. Creates a SVG column
 * showing a molecule.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Greg Landrum
 */
public class RDKit2SVGNodeDialog extends DefaultNodeSettingsPane {

	//
	// Constants
	//

	/** Default values, directly taken from RDKit binaries. */
	public static final MolDrawOptions RDKIT_DEFAULT_PARAMETERS = new MolDrawOptions();
	
	//
	// Constructor
	//

	/**
	 * Create a new dialog pane with default components to configure an input
	 * column, the name of a new column, which will contain the calculation results,
	 * an option to tell, if the source column shall be removed from the result
	 * table.
	 */
	RDKit2SVGNodeDialog() {
		super.addDialogComponent(new DialogComponentColumnNameSelection(createInputColumnNameModel(),
				"RDKit Mol column: ", 0, RDKitMolValue.class));
		super.addDialogComponent(new DialogComponentString(createNewColumnNameModel(), "New column name: "));
		super.addDialogComponent(
				new DialogComponentBoolean(createRemoveSourceColumnsOptionModel(), "Remove source column"));

		super.createNewTab("Drawing Options");
		super.addDialogComponent(
				new DialogComponentBoolean(createPrepareBeforeDrawingOptionModel(), "Prepare before drawing"));
		super.addDialogComponent(
				new DialogComponentBoolean(createCenterBeforeDrawingOptionModel(), "Center before drawing"));
		super.addDialogComponent(new DialogComponentBoolean(createClearBackgroundOptionModel(), "Clear background"));
		super.addDialogComponent(new DialogComponentBoolean(createAddAtomIndicesOptionModel(), "Add atom indices"));
		super.addDialogComponent(new DialogComponentBoolean(createAddBondIndicesOptionModel(), "Add bond indices"));
		super.addDialogComponent(
				new DialogComponentBoolean(createAddStereoAnnotationOptionModel(), "Add stereo annotations"));
		super.addDialogComponent(
				new DialogComponentBoolean(createIncludeChiralFlagOptionModel(), "Include chiral flag"));
		super.addDialogComponent(
				new DialogComponentBoolean(createSimplifiedStereoGroupsOptionModel(), "Use simplified stereo groups"));
		super.addDialogComponent(
				new DialogComponentBoolean(createSingleColorWedgeBondsOptionModel(), "Single color wedge bonds"));
		super.addDialogComponent(
				new DialogComponentBoolean(createExplicitMethylOptionModel(), "Draw explicit methyl groups"));
		super.addDialogComponent(
				new DialogComponentBoolean(createDummiesAreAttachmentsOptionModel(), "Dummies are attachment points"));
		super.addDialogComponent(new DialogComponentBoolean(createIncludeRadicalsOptionModel(), "Draw radicals"));
		super.addDialogComponent(new DialogComponentBoolean(createNoAtomLabelsOptionModel(), "No atom labels"));
		super.addDialogComponent(
				new DialogComponentBoolean(createIsotopeLabelsOptionModel(), "Include isotope labels"));
		super.addDialogComponent(
				new DialogComponentBoolean(createDummyIsotopeLabelsOptionModel(), "Include isotope labels on dummies"));
		super.addDialogComponent(new DialogComponentBoolean(createComicModeOptionModel(), "Comic mode"));
		super.addDialogComponent(new DialogComponentBoolean(createBWModeOptionModel(), "Black&White mode"));

		super.addDialogComponent(
				new DialogComponentNumber(createBondLineWidthOptionModel(), "Line width for bonds", 1));

		super.addDialogComponent(new DialogComponentNumber(createMinFontSizeOptionModel(), "Min font size", 1));
		super.addDialogComponent(new DialogComponentNumber(createMaxFontSizeOptionModel(), "Max font size", 1));
		super.addDialogComponent(
				new DialogComponentNumber(createAnnotationFontScaleOptionModel(), "Annotation font scale", 0.05));

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
	 * Creates the settings model to be used to specify the new column name.
	 * 
	 * @return Settings model for result column name.
	 */
	static final SettingsModelString createNewColumnNameModel() {
		return new SettingsModelString("new_column_name", null);
	}

	/**
	 * Creates the settings model for the boolean flag to determine if the source
	 * column shall be removed from the result table. The default is false.
	 * 
	 * @return Settings model for check box whether to remove source columns.
	 */
	static final SettingsModelBoolean createRemoveSourceColumnsOptionModel() {
		return new SettingsModelBoolean("remove_source_columns", false);
	}

	static final SettingsModelBoolean createClearBackgroundOptionModel() {
		return new SettingsModelBoolean("clear_background", RDKIT_DEFAULT_PARAMETERS.getClearBackground());
	}

	static final SettingsModelBoolean createDummiesAreAttachmentsOptionModel() {
		return new SettingsModelBoolean("dummies_are_attachments", 
				RDKIT_DEFAULT_PARAMETERS.getDummiesAreAttachments());
	}

	static final SettingsModelBoolean createAddAtomIndicesOptionModel() {
		return new SettingsModelBoolean("add_atom_indices", 
				RDKIT_DEFAULT_PARAMETERS.getAddAtomIndices());
	}

	static final SettingsModelBoolean createAddBondIndicesOptionModel() {
		return new SettingsModelBoolean("add_bond_indices", 
				RDKIT_DEFAULT_PARAMETERS.getAddBondIndices());
	}

	static final SettingsModelBoolean createIsotopeLabelsOptionModel() {
		return new SettingsModelBoolean("isotope_labels", 
				RDKIT_DEFAULT_PARAMETERS.getIsotopeLabels());
	}

	static final SettingsModelBoolean createDummyIsotopeLabelsOptionModel() {
		return new SettingsModelBoolean("dummy_isotope_labels", 
				RDKIT_DEFAULT_PARAMETERS.getDummyIsotopeLabels());
	}

	static final SettingsModelBoolean createAddStereoAnnotationOptionModel() {
		return new SettingsModelBoolean("add_stereo_annotation", 
				true); // here the RDKit default is a bad one
	}

	static final SettingsModelBoolean createCenterBeforeDrawingOptionModel() {
		return new SettingsModelBoolean("center_before_drawing", 
				RDKIT_DEFAULT_PARAMETERS.getCentreMoleculesBeforeDrawing());
	}

	static final SettingsModelBoolean createPrepareBeforeDrawingOptionModel() {
		return new SettingsModelBoolean("prepare_before_drawing", 
				RDKIT_DEFAULT_PARAMETERS.getPrepareMolsBeforeDrawing());
	}

	static final SettingsModelBoolean createExplicitMethylOptionModel() {
		return new SettingsModelBoolean("explicit_methyl", 
				RDKIT_DEFAULT_PARAMETERS.getExplicitMethyl());
	}

	static final SettingsModelBoolean createIncludeRadicalsOptionModel() {
		return new SettingsModelBoolean("include_radicals", 
				RDKIT_DEFAULT_PARAMETERS.getIncludeRadicals());
	}

	static final SettingsModelBoolean createComicModeOptionModel() {
		return new SettingsModelBoolean("comic_mode", 
				RDKIT_DEFAULT_PARAMETERS.getComicMode());
	}

	static final SettingsModelBoolean createBWModeOptionModel() {
		return new SettingsModelBoolean("bw_mode",false); // not actually an option in the RDKit API
	}

	static final SettingsModelBoolean createNoAtomLabelsOptionModel() {
		return new SettingsModelBoolean("no_atom_labels", 
				RDKIT_DEFAULT_PARAMETERS.getNoAtomLabels());
	}

	static final SettingsModelBoolean createIncludeChiralFlagOptionModel() {
		return new SettingsModelBoolean("include_chiral_flag", 
				RDKIT_DEFAULT_PARAMETERS.getIncludeChiralFlagLabel());
	}

	static final SettingsModelBoolean createSimplifiedStereoGroupsOptionModel() {
		return new SettingsModelBoolean("simplified_stereo_groups", 
				RDKIT_DEFAULT_PARAMETERS.getSimplifiedStereoGroupLabel());
	}

	static final SettingsModelBoolean createSingleColorWedgeBondsOptionModel() {
		return new SettingsModelBoolean("single_color_wedge_bonds", 
				RDKIT_DEFAULT_PARAMETERS.getSingleColourWedgeBonds());
	}

	static final SettingsModelIntegerBounded createBondLineWidthOptionModel() {
		return new SettingsModelIntegerBounded("bond_line_width", 
				RDKIT_DEFAULT_PARAMETERS.getBondLineWidth(), 0, 100);
	}

	static final SettingsModelIntegerBounded createMinFontSizeOptionModel() {
		return new SettingsModelIntegerBounded("min_font_size", 
				RDKIT_DEFAULT_PARAMETERS.getMinFontSize(), 0, 100);
	}

	static final SettingsModelIntegerBounded createMaxFontSizeOptionModel() {
		return new SettingsModelIntegerBounded("max_font_size", 
				RDKIT_DEFAULT_PARAMETERS.getMaxFontSize(), 0, 100);
	}

	static final SettingsModelDoubleBounded createAnnotationFontScaleOptionModel() {
		return new SettingsModelDoubleBounded("annotation_font_scale", 
				RDKIT_DEFAULT_PARAMETERS.getAnnotationFontScale(), 0, 2);
	}

}
