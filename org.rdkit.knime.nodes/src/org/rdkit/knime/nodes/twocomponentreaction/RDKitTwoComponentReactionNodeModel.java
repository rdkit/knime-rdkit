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
package org.rdkit.knime.nodes.twocomponentreaction;

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
import org.knime.chem.types.SmilesValue;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.RDKitTypesPluginActivator;
import org.rdkit.knime.types.RDKitMolCell;
import org.rdkit.knime.types.RDKitMolValue;

/**
 *
 * @author Greg Landrum
 */
public class RDKitTwoComponentReactionNodeModel extends NodeModel {

    private final SettingsModelString m_reactant1Col =
            RDKitTwoComponentReactionNodeDialogPane
                    .createReactant1ColumnModel();

    private final SettingsModelString m_reactant2Col =
            RDKitTwoComponentReactionNodeDialogPane
                    .createReactant2ColumnModel();

    private final SettingsModelString m_smarts =
            RDKitTwoComponentReactionNodeDialogPane.createSmartsModel();

    private final SettingsModelBoolean m_doMatrix =
            RDKitTwoComponentReactionNodeDialogPane.createBooleanModel();

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(RDKitTwoComponentReactionNodeModel.class);

    /**
     * Create new node model with one data in- and one outport.
     */
    RDKitTwoComponentReactionNodeModel() {
        super(2, 1);
    }

    private DataTableSpec[] createOutSpecs() {
        Vector<DataColumnSpec> cSpec = new Vector<DataColumnSpec>();
        DataColumnSpecCreator crea =
                new DataColumnSpecCreator("Product", RDKitMolCell.TYPE);
        cSpec.add(crea.createSpec());
        crea = new DataColumnSpecCreator("Product Index", IntCell.TYPE);
        cSpec.add(crea.createSpec());
        crea =
                new DataColumnSpecCreator("Reactant 1 sequence index",
                        IntCell.TYPE);
        cSpec.add(crea.createSpec());
        crea = new DataColumnSpecCreator("Reactant 1", RDKitMolCell.TYPE);
        cSpec.add(crea.createSpec());
        crea =
                new DataColumnSpecCreator("Reactant 2 sequence index",
                        IntCell.TYPE);
        cSpec.add(crea.createSpec());
        crea = new DataColumnSpecCreator("Reactant 2", RDKitMolCell.TYPE);
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

        if (null == m_reactant1Col.getStringValue()) {
            List<String> compatibleCols = new ArrayList<String>();
            for (DataColumnSpec c : inSpecs[0]) {
                if (c.getType().isCompatible(SmilesValue.class)
                        || c.getType().isCompatible(RDKitMolValue.class)) {
                    compatibleCols.add(c.getName());
                }
            }
            if (compatibleCols.size() == 1) {
                // auto-configure
                m_reactant1Col.setStringValue(compatibleCols.get(0));
            } else if (compatibleCols.size() > 1) {
                // auto-guessing
                m_reactant1Col.setStringValue(compatibleCols.get(0));
                setWarningMessage("Auto guessing: using column \""
                        + compatibleCols.get(0) + "\".");
            } else {
                throw new InvalidSettingsException("No Smiles compatible "
                        + "column in the first input table");
            }
        }
        if (null == m_reactant2Col.getStringValue()) {
            List<String> compatibleCols = new ArrayList<String>();
            for (DataColumnSpec c : inSpecs[0]) {
                if (c.getType().isCompatible(SmilesValue.class)
                        || c.getType().isCompatible(RDKitMolValue.class)) {
                    compatibleCols.add(c.getName());
                }
            }
            if (compatibleCols.size() == 1) {
                // auto-configure
                m_reactant2Col.setStringValue(compatibleCols.get(0));
            } else if (compatibleCols.size() > 1) {
                // auto-guessing
                m_reactant2Col.setStringValue(compatibleCols.get(0));
                setWarningMessage("Auto guessing: using column \""
                        + compatibleCols.get(0) + "\".");
            } else {
                throw new InvalidSettingsException("No Smiles compatible "
                        + "column in second input table");
            }
        }
        if (m_smarts.getStringValue().isEmpty()) {
            throw new InvalidSettingsException("Please provide a reaction "
                    + "SMARTS.");
        }
        ChemicalReaction rxn =
                RDKFuncs.ReactionFromSmarts(m_smarts.getStringValue());
        if (rxn == null)
            throw new InvalidSettingsException("unparseable reaction SMARTS: "
                    + m_smarts.getStringValue());
        if (rxn.getNumReactantTemplates() != 2)
            throw new InvalidSettingsException(
                    "reaction should only have two reactants, it has: "
                            + rxn.getNumReactantTemplates());

        return createOutSpecs();
    }

    private int[] findColumnIndices(final DataTableSpec[] specs)
            throws InvalidSettingsException {
        String first = m_reactant1Col.getStringValue();
        if (first == null) {
            throw new InvalidSettingsException("Not configured yet");
        }
        int firstIndex = specs[0].findColumnIndex(first);
        if (firstIndex < 0) {
            throw new InvalidSettingsException(
                    "No such column in first input table: " + first);
        }
        DataType firstType = specs[0].getColumnSpec(firstIndex).getType();
        if (!firstType.isCompatible(SmilesValue.class)
                && !firstType.isCompatible(RDKitMolValue.class)) {
            throw new InvalidSettingsException("Column '" + first
                    + "' does not contain SMILES");
        }
        String second = m_reactant2Col.getStringValue();
        if (second == null) {
            throw new InvalidSettingsException("Not configured yet");
        }
        int secondIndex = specs[1].findColumnIndex(second);
        if (secondIndex < 0) {
            throw new InvalidSettingsException(
                    "No such column in second input table: " + second);
        }
        DataType secondType = specs[1].getColumnSpec(secondIndex).getType();
        if (!secondType.isCompatible(SmilesValue.class)
                && !secondType.isCompatible(RDKitMolValue.class)) {
            throw new InvalidSettingsException("Column '" + second
                    + "' does not contain SMILES");
        }
        return new int[]{firstIndex, secondIndex};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec inSpec1 = inData[0].getDataTableSpec();
        DataTableSpec inSpec2 = inData[1].getDataTableSpec();

        BufferedDataContainer productTable =
                exec.createDataContainer(createOutSpecs()[0]);

        // check user settings against input spec here
        final int[] indices =
                findColumnIndices(new DataTableSpec[]{inSpec1, inSpec2});

        ChemicalReaction rxn =
                RDKFuncs.ReactionFromSmarts(m_smarts.getStringValue());
        if (rxn == null)
            throw new InvalidSettingsException("unparseable reaction SMARTS: "
                    + m_smarts.getStringValue());

        int parseErrorCount1 = 0;
        int parseErrorCount2 = 0;
        boolean firstIteration = true;
        // the node has two modes of operation, determined by the doMatrix flag:
        // doMatrix=false: The two input tables are stepped through row by row.
        // i.e. the first row of table 1 is combined with the first row of table
        // 2, then
        // the second row of table 1 is combined with the second row of table 2.
        // This process repeats until one of the tables runs out of rows.
        //
        // doMatrix=true: the two input tables are combined as an NxM matrix.
        // i.e. every row in table 1 is combined with every other row of table
        // 2.
        boolean doMatrix = m_doMatrix.getBooleanValue();
        try {
            int r1Count = 0;
            RowIterator it1 = inData[0].iterator();
            int r2Count = 0;
            RowIterator it2 = inData[1].iterator();
            while (it1.hasNext()) {
                DataRow row1 = it1.next();
                r1Count++;
                DataCell r1Cell = row1.getCell(indices[0]);
                if (!doMatrix) {
                    // if we're not doing the matrix combination and we've
                    // iterated through table 2,
                    // then we're done.
                    if (!it2.hasNext()) {
                        break;
                    }
                } else {
                    r2Count = 0;
                    it2 = inData[1].iterator();
                }

                boolean ownMol1 = false;
                ROMol mol1 = null;
                if (!r1Cell.isMissing()) {
                    if (inSpec1.getColumnSpec(indices[0]).getType()
                            .isCompatible(RDKitMolValue.class)) {
                        mol1 = ((RDKitMolValue)r1Cell).getMoleculeValue();
                        ownMol1 = false;
                    } else {
                        String smiles = ((StringValue)r1Cell).toString();
                        mol1 = RDKFuncs.MolFromSmiles(smiles);
                        ownMol1 = true;
                    }
                }
                if (mol1 == null) {
                    LOGGER.debug("Error parsing Smiles "
                            + "while processing row: " + row1.getKey());
                    parseErrorCount1++;
                    // no first molecule, so might as well bail on the rest of
                    // the work
                    if (!doMatrix) {
                        // but if we aren't doing the matrix combination, we do
                        // need to
                        // increment the second table iterator:
                        it2.next();
                        r2Count++;
                    }
                    continue;
                }

                ROMol_Vect rs = new ROMol_Vect(2);
                rs.set(0, mol1);
                // make like we're going to loop, but we will break out below if
                // doMatrix is false
                while (it2.hasNext()) {
                    DataRow row2 = it2.next();
                    r2Count++;
                    DataCell r2Cell = row2.getCell(indices[1]);
                    if (!r2Cell.isMissing()) {
                        // usual boilerplate for building the second molecule:
                        boolean ownMol2 = false;
                        ROMol mol2 = null;
                        if (inSpec2.getColumnSpec(indices[1]).getType()
                                .isCompatible(RDKitMolValue.class)) {
                            mol2 = ((RDKitMolValue)r2Cell).getMoleculeValue();
                            ownMol2 = false;
                        } else {
                            String smiles = ((StringValue)r2Cell).toString();
                            mol2 = RDKFuncs.MolFromSmiles(smiles);
                            ownMol2 = true;
                        }
                        if (mol2 != null) {
                            rs.set(1, mol2);
                            // ChemicalReaction.runReactants() returns a vector
                            // of vectors,
                            // the outer vector allows the reaction queries to
                            // match reactants
                            // multiple times, the inner vectors allow each
                            // reaction to have multiple
                            // products.
                            ROMol_Vect_Vect prods = rxn.runReactants(rs);
                            if (!prods.isEmpty()) {
                                for (int psetidx = 0; psetidx < prods.size();
                                        psetidx++) {
                                    for (int pidx = 0; pidx < prods
                                            .get(psetidx).size(); pidx++) {
                                        DataCell[] cells =
                                                new DataCell[productTable
                                                        .getTableSpec()
                                                        .getNumColumns()];
                                        cells[0] =
                                                new RDKitMolCell(prods.get(
                                                        psetidx).get(pidx));
                                        cells[1] = new IntCell(pidx);
                                        cells[2] = new IntCell(r1Count - 1);
                                        cells[3] = new RDKitMolCell(rs.get(0));
                                        cells[4] = new IntCell(r2Count - 1);
                                        cells[5] = new RDKitMolCell(rs.get(1));
                                        DataRow drow =
                                                new DefaultRow(""
                                                        + (r1Count - 1) + "_"
                                                        + (r2Count - 1) + "_"
                                                        + psetidx + "_" + pidx,
                                                        cells);
                                        productTable.addRowToTable(drow);
                                    }
                                }
                            }
                            if (ownMol2) {
                                mol2.delete();
                            }
                        } else {
                            if ((firstIteration
                                    && m_doMatrix.getBooleanValue())
                                    || !m_doMatrix.getBooleanValue()) {
                                LOGGER.debug("Error parsing Smiles "
                                    + "while processing row: " + row2.getKey());
                                parseErrorCount2++;
                            }
                        }
                    }
                    if (!doMatrix) {
                        break;
                    }
                }
                if (ownMol1) {
                    mol1.delete();
                }
                firstIteration = false;
            }
        } finally {
            productTable.close();
        }
        if (parseErrorCount1 > 0 || parseErrorCount2 > 0) {
            StringBuilder msg = new StringBuilder();
            msg.append("Error parsing Smiles for ");
            if (parseErrorCount1 > 0) {
                msg.append(parseErrorCount1);
                msg.append(" rows from input 1");
                if (parseErrorCount2 > 0) {
                    msg.append(" and ");
                }
            }
            if (parseErrorCount2 > 0) {
                msg.append(parseErrorCount2);
                msg.append(" rows from input 2");
            }
            msg.append(".");
            setWarningMessage(msg.toString());
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
        m_reactant1Col.loadSettingsFrom(settings);
        m_reactant2Col.loadSettingsFrom(settings);
        m_smarts.loadSettingsFrom(settings);
        m_doMatrix.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_reactant1Col.saveSettingsTo(settings);
        m_reactant2Col.saveSettingsTo(settings);
        m_smarts.saveSettingsTo(settings);
        m_doMatrix.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_reactant1Col.validateSettings(settings);
        m_reactant2Col.validateSettings(settings);
        m_smarts.validateSettings(settings);
        m_doMatrix.validateSettings(settings);
    }
}
