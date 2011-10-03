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
package org.rdkit.knime.nodes.moleculesubstructfilter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.RDKit.ROMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
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
public class RDKitMoleculeSubstructFilterNodeModel extends NodeModel {
    private final RDKitMoleculeSubstructFilterSettings m_settings =
            new RDKitMoleculeSubstructFilterSettings();

    /**
     * Create new node model with one data in- and one outport.
     */
    RDKitMoleculeSubstructFilterNodeModel() {
        super(2, 2);
    }

    private DataTableSpec[] createOutSpecs(final DataTableSpec[] inSpecs) {
        Vector<DataColumnSpec> cSpec = new Vector<DataColumnSpec>();
        cSpec.clear();
        for(int i=0;i<inSpecs[0].getNumColumns();i++){
        	cSpec.add(inSpecs[0].getColumnSpec(i));
        }
        DataColumnSpecCreator crea=new DataColumnSpecCreator("Matched Substructs",ListCell.getCollectionType(IntCell.TYPE));
       	cSpec.add(crea.createSpec());
        return new DataTableSpec[]{
        		new DataTableSpec("Passed molecules",
        				cSpec.toArray(new DataColumnSpec[cSpec.size()])),
                new DataTableSpec("Failed molecules",
                        cSpec.toArray(new DataColumnSpec[cSpec.size()]))
        		};
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
        if (m_settings.queryColumn() == null) {
            List<String> compatibleCols = new ArrayList<String>();
            for (DataColumnSpec c : inSpecs[1]) {
                if (c.getType().isCompatible(RDKitMolValue.class)) {
                    compatibleCols.add(c.getName());
                }
            }
            if (compatibleCols.size() == 1) {
                // auto-configure
                m_settings.queryColumn(compatibleCols.get(0));
            } else if (compatibleCols.size() > 1) {
                // auto-guessing
                m_settings.queryColumn(compatibleCols.get(0));
                setWarningMessage("Auto guessing: using column \""
                        + compatibleCols.get(0) + "\" as query molecule column.");
            } else {
                throw new InvalidSettingsException("No RDKit Mol compatible "
                        + "column in input table. Use \"Molecule to RDKit\" "
                        + "node for Smiles or SDF.");
            }
        }

        // further input spec check
        findColumnIndices(inSpecs);

        return createOutSpecs(inSpecs);
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

        String query = m_settings.queryColumn();
        if (query == null) {
            throw new InvalidSettingsException("Not configured yet");
        }
        int queryIndex = specs[1].findColumnIndex(query);
        if (queryIndex < 0) {
            throw new InvalidSettingsException(
                    "No such column in input table: " + query);
        }
        DataType queryType = specs[1].getColumnSpec(queryIndex).getType();
        if (!queryType.isCompatible(RDKitMolValue.class)) {
            throw new InvalidSettingsException("Column '" + query
                    + "' does not contain RDKit molecules");
        }

        return new int[]{rdkitIndex, queryIndex};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	DataTableSpec inSpec = inData[0].getDataTableSpec();
        DataTableSpec[] outSpecs=createOutSpecs(new DataTableSpec[]{inSpec});
    	BufferedDataContainer matchTable =
            exec.createDataContainer(outSpecs[0]);
        BufferedDataContainer failTable =
            exec.createDataContainer(outSpecs[1]);

        // check user settings against input spec here
        final int[] indices =
                findColumnIndices(new DataTableSpec[]{
                        inData[0].getDataTableSpec(),
                        inData[1].getDataTableSpec()});

        ROMol[] patterns = new ROMol[inData[1].getRowCount()];
        int i = 0;
        for (DataRow row : inData[1]) {
        	if(!row.getCell(indices[1]).isMissing()){
        		patterns[i] = ((RDKitMolValue)row.getCell(indices[1])).readMoleculeValue();
        	} else {
        		patterns[i]=null;
        	}
            i++;
        }

        final int rowCount = inData[0].getRowCount();
        int matchCount = 0;
        try {
            int count = 0;
            RowIterator it = inData[0].iterator();
            while (it.hasNext()) {
                DataRow row = it.next();
                List<IntCell> fragsHere = new ArrayList<IntCell>();
                int patternMatchCount = 0;
                count++;
                DataCell firstCell = row.getCell(indices[0]);
                if (!firstCell.isMissing()) {
                    ROMol mol = ((RDKitMolValue)firstCell).readMoleculeValue();
                    try {
                        for (int pidx=0;pidx<patterns.length;pidx++){
                        	ROMol p=patterns[pidx];
                            if (p!=null && mol.hasSubstructMatch(p)) {
                            	fragsHere.add(new IntCell(pidx+1));
                                patternMatchCount++;
                            }
                        }
                    } finally {
                        mol.delete();
                    }
                }
                DataCell[] cells = new DataCell[outSpecs[0].getNumColumns()];
                for(int idx=0;idx<cells.length-1;idx++){
                	cells[idx]=row.getCell(idx);
                }
                cells[cells.length-1] = CollectionCellFactory.createListCell(fragsHere);
                DataRow drow= new DefaultRow(row.getKey(),cells);

                if (((m_settings.minimumMatches() == 0) && (patternMatchCount == patterns.length))
                        || ((m_settings.minimumMatches() > 0)&&(patternMatchCount >= m_settings.minimumMatches()))) {
                    matchTable.addRowToTable(drow);
                    matchCount += 1;
                } else {
                    failTable.addRowToTable(drow);
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
        RDKitMoleculeSubstructFilterSettings s =
                new RDKitMoleculeSubstructFilterSettings();
        s.loadSettings(settings);
        if (s.minimumMatches() < 0) {
            throw new InvalidSettingsException("Minimum matches must be >= 0");
        }
    }
}
