/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2010
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
package org.rdkit.knime.nodes.addcoordinates;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.RDKitTypesPluginActivator;
import org.rdkit.knime.types.RDKitMolCellFactory;
import org.rdkit.knime.types.RDKitMolValue;

/**
 *
 * @author Greg Landrum
 */
public class RDKitAddCoordinatesNodeModel extends NodeModel {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(RDKitAddCoordinatesNodeModel.class);

    private final SettingsModelString m_first =
            RDKitAddCoordinatesNodeDialogPane.createFirstColumnModel();

    private final SettingsModelString m_concate =
            RDKitAddCoordinatesNodeDialogPane.createNewColumnModel();

    private final SettingsModelBoolean m_removeSourceCols =
            RDKitAddCoordinatesNodeDialogPane.createBooleanModel();

    private final SettingsModelString m_dimension =
            RDKitAddCoordinatesNodeDialogPane.createDimensionModel();

    private final SettingsModelString m_templateSmarts =
            RDKitAddCoordinatesNodeDialogPane.createTemplateSmartsModel();

    private ROMol m_smartsPattern;

    /**
     * Create new node model with one data in- and one outport.
     */
    RDKitAddCoordinatesNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // check whether native RDKit library has been loaded successfully
        RDKitTypesPluginActivator.checkErrorState();

        if (null == m_first.getStringValue()) {
            List<String> compatibleCols = new ArrayList<String>();
            for (DataColumnSpec c : inSpecs[0]) {
                if (c.getType().isCompatible(RDKitMolValue.class)) {
                    compatibleCols.add(c.getName());
                }
            }
            if (compatibleCols.size() == 1) {
                // auto-configure
                m_first.setStringValue(compatibleCols.get(0));
            } else if (compatibleCols.size() > 1) {
                // auto-guessing
                m_first.setStringValue(compatibleCols.get(0));
                setWarningMessage("Auto guessing: using column \""
                        + compatibleCols.get(0) + "\".");
            } else {
                throw new InvalidSettingsException("No RDKit Mol compatible "
                        + "column in input table. Use \"Molecule to RDKit\" "
                        + "node for Smiles or SDF.");
            }
        }
        if (null == m_concate.getStringValue()) {
            if (null != m_first.getStringValue()) {
                // auto-configure
                String newName =
                        DataTableSpec.getUniqueColumnName(inSpecs[0],
                                m_first.getStringValue() + " (with coord.)");
                m_concate.setStringValue(newName);
            } else {
                m_concate.setStringValue("RDKit Mol with coord.");
            }
        }
        if (null == m_dimension.getStringValue()) {
            m_dimension.setStringValue(
                    RDKitAddCoordinatesNodeDialogPane.DIMENSION_3D);
        }

        boolean do2D = m_dimension.getStringValue().equals(
                RDKitAddCoordinatesNodeDialogPane.DIMENSION_2D);
        if (!m_templateSmarts.getStringValue().isEmpty() && do2D) {
            ROMol patternTest =
                    RDKFuncs.MolFromSmarts(m_templateSmarts.getStringValue());
            if (patternTest == null) {
                throw new InvalidSettingsException(
                        "Could not parse SMARTS query for template: "
                                + m_templateSmarts.getStringValue());
            }
        }
        ColumnRearranger rearranger = createColumnRearranger(inSpecs[0]);
        return new DataTableSpec[]{rearranger.createSpec()};
    }

    private int[] findColumnIndices(final DataTableSpec spec)
            throws InvalidSettingsException {
        String first = m_first.getStringValue();
        if (first == null) {
            throw new InvalidSettingsException("Not configured yet");
        }
        int firstIndex = spec.findColumnIndex(first);
        if (firstIndex < 0) {
            throw new InvalidSettingsException(
                    "No such column in input table: " + first);
        }
        DataType firstType = spec.getColumnSpec(firstIndex).getType();
        if (!firstType.isCompatible(RDKitMolValue.class)) {
            throw new InvalidSettingsException("Column '" + first
                    + "' does not contain SMILES");
        }
        return new int[]{firstIndex};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec inSpec = inData[0].getDataTableSpec();
        ColumnRearranger rearranger = createColumnRearranger(inSpec);
        BufferedDataTable outTable =
                exec.createColumnRearrangeTable(inData[0], rearranger, exec);
        return new BufferedDataTable[]{outTable};
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec spec)
            throws InvalidSettingsException {
        // check user settings against input spec here
        final int[] indices = findColumnIndices(spec);
        String inputCol = m_first.getStringValue();
        String newName = m_concate.getStringValue();
        if ((spec.containsName(newName) && !newName.equals(inputCol))
                || (spec.containsName(newName) && newName.equals(inputCol) &&
                        !m_removeSourceCols.getBooleanValue())) {
            throw new InvalidSettingsException("Cannot create column "
                    + newName + "since it is already in the input.");
        }
        // construct an RDKit molecule from the SMARTS pattern:
        m_smartsPattern = null;
        final boolean do2D = m_dimension.getStringValue().equals(
                RDKitAddCoordinatesNodeDialogPane.DIMENSION_2D);
        if (!m_templateSmarts.getStringValue().isEmpty() && do2D) {
            m_smartsPattern = RDKFuncs.MolFromSmarts(
                    m_templateSmarts.getStringValue());
            if (m_smartsPattern != null) {
                RDKFuncs.compute2DCoords(m_smartsPattern);
            }
        }

        ColumnRearranger result = new ColumnRearranger(spec);
        DataColumnSpecCreator appendSpec =
                new DataColumnSpecCreator(newName, RDKitMolCellFactory.TYPE);
        result.append(new SingleCellFactory(appendSpec.createSpec()) {
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell firstCell = row.getCell(indices[0]);
                if (firstCell.isMissing()) {
                    return DataType.getMissingCell();
                }
                DataType firstType = spec.getColumnSpec(indices[0]).getType();
                ROMol mol;
                if (firstType.isCompatible(RDKitMolValue.class)) {
                    ROMol input =
                        ((RDKitMolValue)firstCell).readMoleculeValue();
                    try {
                        mol = new ROMol(input);
                    } finally {
                        input.delete();
                    }
                } else {
                    // it's a SMILES column, so construct an RDKit molecule
                    // from the SMILES:
                    String smiles = ((StringValue)firstCell).toString();
                    mol = RDKFuncs.MolFromSmiles(smiles);
                    if (mol == null) {
                        StringBuilder error = new StringBuilder();
                        error.append("Error parsing SMILES ");
                        error.append("while processing row: \"");
                        error.append(row.getKey()).append("\"");
                        LOGGER.warn(error.toString());
                        return DataType.getMissingCell();
                    }
                }
                // after all that work we can now add coords:
                try {
                	// keep this all isolated in a try...catch so that we don't take
                	// down knime if there's a problem on the C++ side.
	                if (!do2D) {
	                    RDKFuncs.compute3DCoords(mol);
	                } else {
	                    if (m_smartsPattern != null) {
	                        RDKFuncs.compute2DCoords(mol, m_smartsPattern);
	                    } else {
	                        RDKFuncs.compute2DCoords(mol);
	                    }
	                }
	                return RDKitMolCellFactory.createRDKitMolCell(mol);
                } catch (Exception e) {
                	LOGGER.warn("problems generating coordinates", e);
                	return DataType.getMissingCell();
                } finally {
                    mol.delete();
                }

            }
        });
        if (m_removeSourceCols.getBooleanValue()) {
            result.remove(indices);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to reset
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // node does not have internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // node does not have internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_first.loadSettingsFrom(settings);
        m_concate.loadSettingsFrom(settings);
        m_removeSourceCols.loadSettingsFrom(settings);
        m_templateSmarts.loadSettingsFrom(settings);
        m_dimension.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_first.saveSettingsTo(settings);
        m_concate.saveSettingsTo(settings);
        m_removeSourceCols.saveSettingsTo(settings);
        m_templateSmarts.saveSettingsTo(settings);
        m_dimension.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_first.validateSettings(settings);
        m_concate.validateSettings(settings);
        m_removeSourceCols.validateSettings(settings);
        m_templateSmarts.validateSettings(settings);
        m_dimension.validateSettings(settings);
    }
}
