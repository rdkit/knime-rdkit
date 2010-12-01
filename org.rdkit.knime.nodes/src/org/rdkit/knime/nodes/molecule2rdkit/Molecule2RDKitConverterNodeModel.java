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
package org.rdkit.knime.nodes.molecule2rdkit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.BlobSupportDataRow;
import org.knime.core.node.BufferedDataContainer;
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

/**
 *
 * @author Greg Landrum
 */
public class Molecule2RDKitConverterNodeModel extends NodeModel {

    private final SettingsModelString m_first =
            Molecule2RDKitConverterNodeDialogPane.createFirstColumnModel();

    private final SettingsModelString m_concate =
            Molecule2RDKitConverterNodeDialogPane.createNewColumnModel();

    private final SettingsModelBoolean m_removeSourceCols =
            Molecule2RDKitConverterNodeDialogPane.createBooleanModel();

    private final SettingsModelString m_separateFails =
            Molecule2RDKitConverterNodeDialogPane.createSeparateRowsModel();

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(Molecule2RDKitConverterNodeModel.class);

    /**
     * Create new node model with one data in- and one outport.
     */
    Molecule2RDKitConverterNodeModel() {
        super(1, 2);
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
                if (c.getType().isCompatible(SmilesValue.class)
                        || c.getType().isCompatible(SdfValue.class)) {
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
                throw new InvalidSettingsException("Neither Smiles nor SDF "
                        + "compatible "
                        + "column in input table.");
            }
        }
        if (null == m_concate.getStringValue()) {
            // auto-configure
            String newName;
            if (null != m_first.getStringValue()) {
                newName = m_first.getStringValue() + " (RDKit Mol)";
            } else {
                newName = "RDKit Molecule";
            }
            newName = DataTableSpec.getUniqueColumnName(inSpecs[0], newName);
            m_concate.setStringValue(newName);
        }
        return new DataTableSpec[]{createOutPort0Spec(inSpecs[0]), inSpecs[0]};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec inSpec = inData[0].getDataTableSpec();
        DataTableSpec out0Spec = createOutPort0Spec(inSpec);
        // contains the rows with the result column
        BufferedDataContainer port0 = exec.createDataContainer(out0Spec);
        // contains the input rows if result computation fails
        BufferedDataContainer port1 = exec.createDataContainer(inSpec);

        int molColIdx = getMolColIndex(inSpec, m_first.getStringValue().trim());
        final boolean smilesInput = inSpec.getColumnSpec(molColIdx).getType().
            isCompatible(SmilesValue.class);
        int parseErrorCount = 0;
        int rowsProcessed = 0;
        int totalRowCount = inData[0].getRowCount();
        for (DataRow row : inData[0]) {
            exec.checkCanceled();
            exec.setProgress(rowsProcessed / (double)totalRowCount,
                    "Processing row " + row.getKey());
            final DataCell molCell = row.getCell(molColIdx);

            DataCell result;
            boolean parseError = false;
            ROMol mol = null;
            if (molCell.isMissing()) {
                mol = null;
            } else if (smilesInput) {
                String value = ((SmilesValue)molCell).getSmilesValue();
                mol = RDKFuncs.MolFromSmiles(value);
            } else {
                String value = ((SdfValue)molCell).getSdfValue();
                mol = RDKFuncs.MolFromMolBlock(value);
            }

            if (mol == null) {
                StringBuilder error = new StringBuilder();
                error.append("Error parsing ");
                error.append(smilesInput ? "SMILES " : "SDF ");
                error.append("while processing row: \"");
                error.append(row.getKey()).append("\"");
                LOGGER.debug(error.toString());
                parseErrorCount++;
                parseError = true;
                result = DataType.getMissingCell();
            } else {
                result = RDKitMolCellFactory.createRDKitMolCell(mol);
            }

            if (parseError && splitBadRowsToPort1()) {
                port1.addRowToTable(row);
            } else {
                final ArrayList<DataCell> copyCells =
                    new ArrayList<DataCell>(row.getNumCells() + 1);

                // copy the cells from the incoming row
                for (int i = 0; i < row.getNumCells(); i++) {
                    if (i == molColIdx && m_removeSourceCols.getBooleanValue()) {
                        continue;
                    }
                    // respecting a blob support row has the advantage that
                    // blobs are not unwrapped (expensive)
                    // --> this is really only for performance and makes a
                    // difference only if the input row contains blobs
                    copyCells.add(row instanceof BlobSupportDataRow
                            ? ((BlobSupportDataRow)row).getRawCell(i)
                                    : row.getCell(i));
                }
                copyCells.add(result);
                BlobSupportDataRow outRow = new BlobSupportDataRow(row.getKey(),
                        copyCells.toArray(new DataCell[copyCells.size()]));
                port0.addRowToTable(outRow);
            }
            rowsProcessed++;
        }
        if (parseErrorCount > 0) {
            String msg =
                    "Error parsing input for " + parseErrorCount + " rows.";
            LOGGER.warn(msg);
            setWarningMessage(msg);
        }
        port0.close();
        port1.close();
        return new BufferedDataTable[]{port0.getTable(), port1.getTable()};
    }

    private boolean splitBadRowsToPort1() {
        return !Molecule2RDKitConverterNodeDialogPane.MISSVAL_FOR_FOR_BAD_ROWS
                .equals(m_separateFails.getStringValue());
    }

    private int getMolColIndex(final DataTableSpec inSpec, final String colName)
            throws InvalidSettingsException {
        if (colName == null || colName.isEmpty()) {
            throw new InvalidSettingsException("Not configured yet");
        }
        int molColIndex = inSpec.findColumnIndex(colName);
        if (molColIndex < 0) {
            throw new InvalidSettingsException(
                    "No such column in input table: " + colName);
        }
        DataType molColType = inSpec.getColumnSpec(molColIndex).getType();
        if (!molColType.isCompatible(SmilesValue.class)
                && !molColType.isCompatible(SdfValue.class)) {
            throw new InvalidSettingsException("Column '" + colName
                    + "' does not contain smiles or SDF.");
        }
        return molColIndex;
    }

    private DataTableSpec createOutPort0Spec(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        String molCol = m_first.getStringValue().trim();
        // make sure it is set and of correct type:
        getMolColIndex(inSpec, molCol);
        String newName = m_concate.getStringValue().trim();
        if (newName == null || newName.isEmpty()) {
            throw new InvalidSettingsException("No name for new column set.");
        }
        if (inSpec.containsName(newName)) {
            // only acceptable if it replaces the input column
            if (!(newName.equals(molCol) && m_removeSourceCols
                    .getBooleanValue())) {
                throw new InvalidSettingsException("Cannot create column \""
                        + newName + "\" since it is already in the input.");
            }
        }

        int newColNum = inSpec.getNumColumns() + 1;
        if (m_removeSourceCols.getBooleanValue()) {
            newColNum--;
        }
        ArrayList<DataColumnSpec> newColSpecs =
                new ArrayList<DataColumnSpec>(newColNum);
        for (DataColumnSpec inCol : inSpec) {
            if (m_removeSourceCols.getBooleanValue()
                    && inCol.getName().equals(molCol)) {
                // don't include source column in output spec
                continue;
            }
            newColSpecs.add(inCol);
        }
        // append result column
        DataColumnSpec appendSpec =
                new DataColumnSpecCreator(newName, RDKitMolCellFactory.TYPE)
                        .createSpec();
        newColSpecs.add(appendSpec);
        assert newColSpecs.size() == newColNum;
        return new DataTableSpec(
                newColSpecs.toArray(new DataColumnSpec[newColNum]));
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
        m_separateFails.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_first.saveSettingsTo(settings);
        m_concate.saveSettingsTo(settings);
        m_removeSourceCols.saveSettingsTo(settings);
        m_separateFails.saveSettingsTo(settings);
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
        m_separateFails.validateSettings(settings);
    }
}