/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 * -------------------------------------------------------------------
 *
 * History
 *   Nov 4, 2010 (wiswedel): created
 */
package org.rdkit.knime.nodes.rdkit2molecule;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.knime.chem.types.SdfCell;
import org.knime.chem.types.SdfCellFactory;
import org.knime.chem.types.SmilesCell;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.RDKitTypesPluginActivator;
import org.rdkit.knime.types.RDKitMolValue;

/**
 *
 * @author Bernd Wiswedel
 */
public class RDKit2MoleculeConverterNodeModel extends NodeModel {

    private final SettingsModelString m_first =
        RDKit2MoleculeConverterNodeDialogPane.createFirstColumnModel();

    private final SettingsModelString m_concate =
        RDKit2MoleculeConverterNodeDialogPane.createNewColumnModel();

    private final SettingsModelBoolean m_removeSourceCols =
        RDKit2MoleculeConverterNodeDialogPane.createBooleanModel();

    private final SettingsModelString m_destinationFormat =
        RDKit2MoleculeConverterNodeDialogPane.createDestinationFormatModel();

    /**
     * Create new node model with one data in- and one outport.
     */
    RDKit2MoleculeConverterNodeModel() {
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
                throw new InvalidSettingsException("No RDKit compatible "
                        + "column in input table.");
            }
        }
        if (null == m_concate.getStringValue()) {
            if (null != m_first.getStringValue()) {
                // auto-configure
                String newName = DataTableSpec.getUniqueColumnName(inSpecs[0],
                        m_first.getStringValue() + " (Molecule)");
                m_concate.setStringValue(newName);
            } else {
                m_concate.setStringValue("Molecule");
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
                    + "' does not contain RDKit.");
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
              ||  (spec.containsName(newName) && newName.equals(inputCol)
              && !m_removeSourceCols.getBooleanValue())) {
            throw new InvalidSettingsException("Cannot create column \""
                    + newName + "\" since it is already in the input.");
        }
        ColumnRearranger result = new ColumnRearranger(spec);
        DataColumnSpecCreator appendSpec = null;
        final boolean convertToSmiles = m_destinationFormat.getStringValue().
                equals(RDKit2MoleculeConverterNodeDialogPane.SMILES_FORMAT);
        if (convertToSmiles) {
            appendSpec = new DataColumnSpecCreator(newName, SmilesCell.TYPE);
        } else {
            appendSpec = new DataColumnSpecCreator(newName, SdfCell.TYPE);
        }
        result.append(new SingleCellFactory(appendSpec.createSpec()) {
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell firstCell = row.getCell(indices[0]);
                if (firstCell.isMissing()) {
                    return DataType.getMissingCell();
                }
                RDKitMolValue rdkit = (RDKitMolValue)firstCell;
                ROMol mol = rdkit.readMoleculeValue();

                try {
                    if (convertToSmiles) {
                        // Convert to Smiles
                        String value = RDKFuncs.MolToSmiles(mol, true);
                        return new SmilesCell(value);
                    } else {
                        // Convert to SDF
                        if(mol.getNumConformers() == 0){
                            RDKFuncs.compute2DCoords(mol);
                        }
                        String value = RDKFuncs.MolToMolBlock(mol);
                        // KNIME SDF type requires string to be terminated
                        // by $$$$ -- see org.knime.chem.types.SdfValue for details
                        String postfix = "\n$$$$\n";
                        if (!value.endsWith(postfix)) {
                            StringBuilder valueBuilder = new StringBuilder();
                            valueBuilder.append(value);
                            valueBuilder.append(postfix);
                            value = valueBuilder.toString();
                        }
                        return SdfCellFactory.create(value);
                    }
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
        m_destinationFormat.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_first.saveSettingsTo(settings);
        m_concate.saveSettingsTo(settings);
        m_removeSourceCols.saveSettingsTo(settings);
        m_destinationFormat.saveSettingsTo(settings);
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
        m_destinationFormat.validateSettings(settings);
    }
}
