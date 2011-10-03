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
package org.rdkit.knime.nodes.multiplesubstrucfilter;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.RDKit.ROMol;
import org.RDKit.RWMol;
import org.knime.chem.types.SmartsValue;
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
import org.rdkit.knime.RDKitTypesPluginActivator;
import org.rdkit.knime.types.RDKitMolValue;

/**
 * This class is the model for the dictionary based substructure filter node.
 *
 * @author Greg Landrum
 * @author Thorsten Meinl, University of Konstanz
 */
public class RDKitDictSubstructFilterNodeModel extends NodeModel {
    private final RDKitDictSubstructFilterSettings m_settings =
            new RDKitDictSubstructFilterSettings();

    /**
     * Create new node model with one data in- and one outport.
     */
    RDKitDictSubstructFilterNodeModel() {
        super(2, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // check whether native RDKit library has been loaded successfully
        RDKitTypesPluginActivator.checkErrorState();

        if (m_settings.rdkitColumn() == null) {
            List<String> compatibleCols = new ArrayList<String>();
            for (DataColumnSpec c : inSpecs[0]) {
                if (c.getType().isCompatible(RDKitMolValue.class)) {
                    compatibleCols.add(c.getName());
                }
            }
            if (compatibleCols.size() == 1) {
                // auto-configure
                m_settings.rdkitColumn(compatibleCols.get(0));
            } else if (compatibleCols.size() > 1) {
                // auto-guessing
                m_settings.rdkitColumn(compatibleCols.get(0));
                setWarningMessage("Auto guessing: using column \""
                        + compatibleCols.get(0) + "\" as RDKit Mol column.");
            } else {
                throw new InvalidSettingsException("No RDKit Mol compatible "
                        + "column in input table. Use \"Molecule to RDKit\" "
                        + "node for Smiles or SDF.");
            }
        }
        if (m_settings.smartsColumn() == null) {
            List<String> compatibleCols = new ArrayList<String>();
            for (DataColumnSpec c : inSpecs[1]) {
                if (c.getType().isCompatible(SmartsValue.class)) {
                    compatibleCols.add(c.getName());
                }
            }
            if (compatibleCols.size() == 1) {
                // auto-configure
                m_settings.smartsColumn(compatibleCols.get(0));
            } else if (compatibleCols.size() > 1) {
                // auto-guessing
                m_settings.smartsColumn(compatibleCols.get(0));
                setWarningMessage("Auto guessing: using column \""
                        + compatibleCols.get(0) + "\" as SMARTS column.");
            } else {
                throw new InvalidSettingsException("No SMARTS compatible "
                        + "column in input table.");
            }
        }

        // further input spec check
        findColumnIndices(inSpecs);

        return new DataTableSpec[]{inSpecs[0], inSpecs[0]};
    }

    private int[] findColumnIndices(final DataTableSpec[] specs)
            throws InvalidSettingsException {
        String rdkit = m_settings.rdkitColumn();
        if (rdkit == null) {
            throw new InvalidSettingsException("Not configured yet");
        }
        int rdkitIndex = specs[0].findColumnIndex(rdkit);
        if (rdkitIndex < 0) {
            throw new InvalidSettingsException(
                    "No such column in input table: " + rdkit);
        }
        DataType firstType = specs[0].getColumnSpec(rdkitIndex).getType();
        if (!firstType.isCompatible(RDKitMolValue.class)) {
            throw new InvalidSettingsException("Column '" + rdkit
                    + "' does not contain RDKit molecules");
        }

        String smarts = m_settings.smartsColumn();
        if (smarts == null) {
            throw new InvalidSettingsException("Not configured yet");
        }
        int smartsIndex = specs[1].findColumnIndex(smarts);
        if (smartsIndex < 0) {
            throw new InvalidSettingsException(
                    "No such column in input table: " + smarts);
        }
        DataType smartsType = specs[1].getColumnSpec(smartsIndex).getType();
        if (!smartsType.isCompatible(SmartsValue.class)) {
            throw new InvalidSettingsException("Column '" + smarts
                    + "' does not contain SMARTS");
        }

        return new int[]{rdkitIndex, smartsIndex};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataContainer matchTable =
                exec.createDataContainer(inData[0].getDataTableSpec());
        BufferedDataContainer failTable =
                exec.createDataContainer(inData[0].getDataTableSpec());

        // check user settings against input spec here
        final int[] indices =
                findColumnIndices(new DataTableSpec[]{
                        inData[0].getDataTableSpec(),
                        inData[1].getDataTableSpec()});

        ROMol[] patterns = new ROMol[inData[1].getRowCount()];
        int i = 0;
        for (DataRow row : inData[1]) {
            SmartsValue v = (SmartsValue)row.getCell(indices[1]);
            patterns[i] = RWMol.MolFromSmarts(v.getSmartsValue(),0,true);
            if (patterns[i] == null) {
                throw new ParseException("Could not parse SMARTS '"
                        + v.getSmartsValue() + "' in row " + row.getKey(), 0);
            }
            i++;
        }

        // construct an RDKit molecule from the SMARTS pattern:
        final int rowCount = inData[0].getRowCount();
        int matchCount = 0;
        try {
            int count = 0;
            RowIterator it = inData[0].iterator();
            while (it.hasNext()) {
                DataRow row = it.next();
                int patternMatchCount = 0;
                count++;
                DataCell firstCell = row.getCell(indices[0]);
                if (!firstCell.isMissing()) {
                    ROMol mol = ((RDKitMolValue)firstCell).readMoleculeValue();
                    // after all that work we can now check whether or not
                    // there is
                    // a substructure match:
                    try {
                        for (ROMol p : patterns) {
                            if (mol.hasSubstructMatch(p)) {
                                patternMatchCount++;
                            }
                        }
                    } finally {
                        mol.delete();
                    }
                }
                if (((m_settings.minimumMatches() == 0) && (patternMatchCount == patterns.length))
                        || ((m_settings.minimumMatches() > 0)&&(patternMatchCount >= m_settings.minimumMatches()))) {
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
            for (ROMol p : patterns) {
                if (p != null) {
                    p.delete();
                }
            }
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
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        RDKitDictSubstructFilterSettings s =
                new RDKitDictSubstructFilterSettings();
        s.loadSettings(settings);
        if (s.minimumMatches() < 0) {
            throw new InvalidSettingsException("Minimum matches must be >= 0");
        }
    }
}
