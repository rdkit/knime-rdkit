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
package org.rdkit.knime.nodes.substructfilter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.rdkit.knime.RDKitTypesPluginActivator;
import org.rdkit.knime.types.RDKitMolValue;

/**
 *
 * @author Greg Landrum
 */
public class RDKitSubstructFilterNodeModel extends NodeModel {

    private final SettingsModelString m_first =
            RDKitSubstructFilterNodeDialogPane.createFirstColumnModel();

    private final SettingsModelString m_smarts =
            RDKitSubstructFilterNodeDialogPane.createSmartsModel();

    private final SettingsModelBoolean m_exactMatch =
        RDKitSubstructFilterNodeDialogPane.createExactMatchModel();

    /**
     * Create new node model with one data in- and one outport.
     */
    RDKitSubstructFilterNodeModel() {
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
        if (m_smarts.getStringValue().equals("")) {
            throw new InvalidSettingsException("Please specify a SMARTS query.");
        }
        ROMol pattern = RDKFuncs.MolFromSmarts(m_smarts.getStringValue());
        if (pattern == null) {
            throw new InvalidSettingsException("Could not parse SMARTS query: "
                    + m_smarts.getStringValue());
        }

        // further input spec check
        findColumnIndices(inSpecs[0]);

        return new DataTableSpec[]{inSpecs[0], inSpecs[0]};
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

        BufferedDataContainer matchTable =
                exec.createDataContainer(inData[0].getDataTableSpec());
        BufferedDataContainer failTable =
                exec.createDataContainer(inData[0].getDataTableSpec());

        // check user settings against input spec here
        final int[] indices = findColumnIndices(inSpec);

        // construct an RDKit molecule from the SMARTS pattern:
        ROMol pattern = RDKFuncs.MolFromSmarts(m_smarts.getStringValue());
        final int rowCount = inData[0].getRowCount();
        int matchCount = 0;
        try {
            int count = 0;
            RowIterator it = inData[0].iterator();
            while (it.hasNext()) {
                DataRow row = it.next();
                boolean matched = false;
                count++;
                // REFACTOR: this code for grabbing smiles or RDKitMolValue data
                // from a cell
                // and converting it into an ROMol occurs in almost every node;
                // it should be pulled out
                DataCell firstCell = row.getCell(indices[0]);
                if (firstCell.isMissing()) {
                    matched = false;
                } else {
                    ROMol mol = null;
                    mol = ((RDKitMolValue)firstCell).readMoleculeValue();
                    // after all that work we can now check whether or not
                    // there is a substructure match:
                    if(!m_exactMatch.getBooleanValue() || mol.getNumAtoms()==pattern.getNumAtoms()){
	                    try {
	                        matched = mol.hasSubstructMatch(pattern);
	                    } finally {
	                        mol.delete();
	                    }
                    }
                }
                if (matched) {
                    matchTable.addRowToTable(row);
                    matchCount += 1;
                } else {
                    failTable.addRowToTable(row);
                }
                exec.setProgress(count / (double)rowCount, "Processed row "
                        + count + "/" + rowCount + " (\"" + row.getKey()
                        + "\") -- " + matchCount + " matches");
                exec.checkCanceled();
            }
        } finally {
            matchTable.close();
            failTable.close();
            pattern.delete();
        }
        return new BufferedDataTable[]{matchTable.getTable(),
                failTable.getTable()};
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
        m_smarts.loadSettingsFrom(settings);
        try{
          m_exactMatch.loadSettingsFrom(settings);
        } catch(Exception ex) {
          m_exactMatch.setBooleanValue(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_first.saveSettingsTo(settings);
        m_smarts.saveSettingsTo(settings);
        m_exactMatch.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_first.validateSettings(settings);
        m_smarts.validateSettings(settings);
        m_exactMatch.validateSettings(settings);
    }
}
