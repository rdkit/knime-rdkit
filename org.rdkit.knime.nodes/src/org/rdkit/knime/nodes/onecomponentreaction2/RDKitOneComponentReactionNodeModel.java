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
package org.rdkit.knime.nodes.onecomponentreaction2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.RDKit.ChemicalReaction;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.RDKit.ROMol_Vect;
import org.RDKit.ROMol_Vect_Vect;
import org.RDKit.RWMol;
import org.knime.chem.types.RxnValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
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
import org.knime.core.node.port.PortType;
import org.rdkit.knime.RDKitTypesPluginActivator;
import org.rdkit.knime.types.RDKitMolCellFactory;
import org.rdkit.knime.types.RDKitMolValue;

/**
 *
 * @author Greg Landrum
 */
public class RDKitOneComponentReactionNodeModel extends NodeModel {
    private final OneComponentSettings m_settings = new OneComponentSettings();

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(RDKitOneComponentReactionNodeModel.class);

    /**
     * Create new node model with one data in- and one outport.
     */
    RDKitOneComponentReactionNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE,
                new PortType(BufferedDataTable.class, true)},
                new PortType[]{BufferedDataTable.TYPE});
    }

    private DataTableSpec[] createOutSpecs() {
        Vector<DataColumnSpec> cSpec = new Vector<DataColumnSpec>();
        DataColumnSpecCreator crea =
                new DataColumnSpecCreator("Product", RDKitMolCellFactory.TYPE);
        cSpec.add(crea.createSpec());
        crea = new DataColumnSpecCreator("Product Index", IntCell.TYPE);
        cSpec.add(crea.createSpec());
        crea =
                new DataColumnSpecCreator("Reactant 1 sequence index",
                        IntCell.TYPE);
        cSpec.add(crea.createSpec());
        crea =
                new DataColumnSpecCreator("Reactant 1",
                        RDKitMolCellFactory.TYPE);
        cSpec.add(crea.createSpec());
        DataTableSpec tSpec =
                new DataTableSpec("output",
                        cSpec.toArray(new DataColumnSpec[cSpec.size()]));

        return new DataTableSpec[]{tSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // check whether native RDKit library has been loaded successfully
        RDKitTypesPluginActivator.checkErrorState();

        if (null == m_settings.firstColumn()) {
            List<String> compatibleCols = new ArrayList<String>();
            for (DataColumnSpec c : inSpecs[0]) {
                if (c.getType().isCompatible(RDKitMolValue.class)) {
                    compatibleCols.add(c.getName());
                }
            }
            if (compatibleCols.size() == 1) {
                // auto-configure
                m_settings.firstColumn(compatibleCols.get(0));
            } else if (compatibleCols.size() > 1) {
                // auto-guessing
                m_settings.firstColumn(compatibleCols.get(0));
                setWarningMessage("Auto guessing: using column \""
                        + compatibleCols.get(0) + "\" for molecules.");
            } else {
                throw new InvalidSettingsException("No RDKit Mol compatible "
                        + "column in input table. Use \"Molecule to RDKit\" "
                        + "node for Smiles or SDF.");
            }
        }
        if (inSpecs[1] == null) {
            // reaction given via Smarts from dialog
            if ((m_settings.reactionSmarts() == null)
                    || (m_settings.reactionSmarts().length() < 1)) {
                throw new InvalidSettingsException(
                        "No reactions Smarts provided");
            }
            // validate only
            readRxnFromSmarts().delete();
        } else {
            if (null == m_settings.rxnColumn()) {
                List<String> compatibleCols = new ArrayList<String>();
                for (DataColumnSpec c : inSpecs[1]) {
                    if (c.getType().isCompatible(RxnValue.class)) {
                        compatibleCols.add(c.getName());
                    }
                }
                if (compatibleCols.size() == 1) {
                    // auto-configure
                    m_settings.rxnColumn(compatibleCols.get(0));
                } else if (compatibleCols.size() > 1) {
                    // auto-guessing
                    m_settings.rxnColumn(compatibleCols.get(0));
                    setWarningMessage("Auto guessing: using column \""
                            + compatibleCols.get(0) + "\" for Rxn.");
                } else {
                    throw new InvalidSettingsException(
                            "No Rxn compatible column in input table. "
                                    + "Use \"Molecule to RDKit\" node for Smiles or SDF.");
                }
            }
            findRxnColumnIndices(inSpecs[1]);
        }

        // further input spec check
        findRDKitColumnIndices(inSpecs[0]);

        return createOutSpecs();
    }

    /**
     * @throws InvalidSettingsException
     */
    private ChemicalReaction readRxnFromSmarts()
            throws InvalidSettingsException {
        ChemicalReaction rxn;
        // read smarts field
        String smartsString = m_settings.reactionSmarts();
        if (smartsString == null || smartsString.isEmpty()) {
            throw new InvalidSettingsException("Invalid (empty) smarts");
        }
        rxn = ChemicalReaction.ReactionFromSmarts(smartsString);
        if (rxn == null) {
            throw new InvalidSettingsException("unparseable reaction smarts: "
                    + smartsString);
        }
        validateRxn(rxn);
        return rxn;
    }

    private void validateRxn(final ChemicalReaction rxn)
            throws InvalidSettingsException {
        if (rxn.getNumReactantTemplates() != 1) {
            throw new InvalidSettingsException(
                    "reaction should have exactly one reactant, it has: "
                            + rxn.getNumReactantTemplates());
        }

        if (!rxn.validate()) {
            throw new InvalidSettingsException("reaction smarts has errors");
        }
    }

    private ChemicalReaction readRxnFromTable(final BufferedDataTable table)
            throws InvalidSettingsException {
        if (table.getRowCount() < 1) {
            throw new IllegalArgumentException(
                    "Table with Rxn does not have any rows");
        }

        int colIndex = findRxnColumnIndices(table.getDataTableSpec());
        DataRow row = table.iterator().next();
        RxnValue rxnValue = (RxnValue)row.getCell(colIndex);

        ChemicalReaction rxn;
        try {
            rxn = ChemicalReaction.ReactionFromRxnBlock(rxnValue.getRxnValue());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse rxn ", e);
        }
        if (rxn == null) {
            throw new RuntimeException(
                    "Unable to parse rxn file (RDKit lib returned null)");
        }
        return rxn;
    }

    private int findRDKitColumnIndices(final DataTableSpec spec)
            throws InvalidSettingsException {
        String first = m_settings.firstColumn();
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
        return firstIndex;
    }

    private int findRxnColumnIndices(final DataTableSpec spec)
            throws InvalidSettingsException {
        String rxn = m_settings.rxnColumn();
        if (rxn == null) {
            throw new InvalidSettingsException("Not configured yet");
        }
        int rxnIndex = spec.findColumnIndex(rxn);
        if (rxnIndex < 0) {
            throw new InvalidSettingsException(
                    "No such column in input table: " + rxn);
        }
        DataType firstType = spec.getColumnSpec(rxnIndex).getType();
        if (!firstType.isCompatible(RxnValue.class)) {
            throw new InvalidSettingsException("Column '" + rxn
                    + "' does not contain Rxn");
        }
        return rxnIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec inSpec = inData[0].getDataTableSpec();

        BufferedDataContainer productTable =
                exec.createDataContainer(createOutSpecs()[0]);

        // check user settings against input spec here
        final int rdkitIndex = findRDKitColumnIndices(inSpec);

        // get the reaction:
        ChemicalReaction rxn =
                (inData[1] != null) ? readRxnFromTable(inData[1])
                        : readRxnFromSmarts();
        int parseErrorCount = 0;
        final int rowCount = inData[0].getRowCount();
        try {
            int count = 0;
            RowIterator it = inData[0].iterator();
            while (it.hasNext()) {
                DataRow row = it.next();
                count++;

                DataCell firstCell = row.getCell(rdkitIndex);
                if (firstCell.isMissing()) {
                    continue;
                } else {
                    DataType firstType =
                            inSpec.getColumnSpec(rdkitIndex).getType();
                    ROMol mol = null;
                    if (firstType.isCompatible(RDKitMolValue.class)) {
                        mol = ((RDKitMolValue)firstCell).readMoleculeValue();
                    } else {
                        String smiles = ((StringValue)firstCell).toString();
                        mol = RWMol.MolFromSmiles(smiles);
                        if (mol == null) {
                            LOGGER.debug("Error parsing Smiles "
                                    + "while processing row: " + row.getKey());
                            parseErrorCount++;
                        }
                        continue;
                    }
                    // the reaction takes a vector of reactants. For this
                    // single-component reaction that
                    // vector is one long:
                    ROMol_Vect rs = new ROMol_Vect(1);
                    rs.set(0, mol);
                    ROMol_Vect_Vect prods = null;
                    // ChemicalReaction.runReactants() returns a vector of
                    // vectors,
                    // the outer vector allows the reaction queries to match
                    // reactants
                    // multiple times, the inner vectors allow each reaction
                    // to have multiple
                    // products.
                    try {
                        prods = rxn.runReactants(rs);
                        // if the reaction could not be applied to the
                        // reactants, we get an
                        // empty vector, check for that now:
                        if (!prods.isEmpty()) {
                            for (int psetidx = 0; psetidx < prods.size(); psetidx++) {
                                for (int pidx = 0; pidx < prods.get(psetidx)
                                        .size(); pidx++) {
                                    DataCell cell;
                                    RWMol prod =
                                            new RWMol(prods.get(psetidx).get(
                                                    pidx));
                                    try {
                                        RDKFuncs.sanitizeMol(prod);
                                        cell =
                                                RDKitMolCellFactory
                                                        .createRDKitMolCellAndDelete(prod);
                                    } catch (Exception e) {
                                        prod.delete();
                                        prods.get(psetidx).get(pidx).delete();
                                        continue;
                                    }
                                    DataCell[] cells =
                                            new DataCell[productTable
                                                    .getTableSpec()
                                                    .getNumColumns()];
                                    cells[0] = cell;
                                    cells[1] = new IntCell(pidx);
                                    cells[2] = new IntCell(count - 1);
                                    ROMol temp = rs.get(0);
                                    cells[3] =
                                            RDKitMolCellFactory
                                                    .createRDKitMolCell(temp);
                                    temp.delete();
                                    DataRow drow =
                                            new DefaultRow("" + (count - 1)
                                                    + "_" + psetidx + "_"
                                                    + pidx, cells);
                                    productTable.addRowToTable(drow);
                                }
                            }
                        }
                    } finally {
                        mol.delete();
                        rs.delete();
                        if (prods != null) {
                            prods.delete();
                        }
                    }
                }
                exec.setProgress(count / (double)rowCount, "Processed row "
                        + count + "/" + rowCount + " (\"" + row.getKey()
                        + "\")");
                exec.checkCanceled();
            }
        } finally {
            productTable.close();
            rxn.delete();
        }
        if (parseErrorCount > 0) {
            setWarningMessage("Error parsing Smiles for " + parseErrorCount
                    + " rows.");
        }
        return new BufferedDataTable[]{productTable.getTable()};
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
        OneComponentSettings s = new OneComponentSettings();
        s.loadSettings(settings);
    }
}
