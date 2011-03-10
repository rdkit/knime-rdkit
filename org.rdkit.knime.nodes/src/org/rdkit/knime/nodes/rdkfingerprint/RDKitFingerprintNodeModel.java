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
package org.rdkit.knime.nodes.rdkfingerprint;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.RDKit.ExplicitBitVect;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.RDKit.UInt32_Vect;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;
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
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.RDKitTypesPluginActivator;
import org.rdkit.knime.types.RDKitMolValue;

/**
 *
 * @author Greg Landrum
 */
public class RDKitFingerprintNodeModel extends NodeModel {

    private final SettingsModelString m_smiles = RDKitFingerprintNodeDialogPane
            .createSmilesColumnModel();

    private final SettingsModelString m_concate =
            RDKitFingerprintNodeDialogPane.createNewColumnModel();

    private final SettingsModelBoolean m_removeSourceCols =
            RDKitFingerprintNodeDialogPane.createBooleanModel();

    private final SettingsModelString m_fpType = RDKitFingerprintNodeDialogPane
            .createFPTypeModel();

    private final SettingsModelIntegerBounded m_minPath =
            RDKitFingerprintNodeDialogPane.createMinPathModel();

    private final SettingsModelIntegerBounded m_maxPath =
            RDKitFingerprintNodeDialogPane.createMaxPathModel();

    private final SettingsModelIntegerBounded m_numBits =
            RDKitFingerprintNodeDialogPane.createNumBitsModel();

    private final SettingsModelIntegerBounded m_radius =
            RDKitFingerprintNodeDialogPane.createRadiusModel();

    private final SettingsModelIntegerBounded m_layerFlags =
            RDKitFingerprintNodeDialogPane.createLayerFlagsModel();

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(RDKitFingerprintNodeModel.class);

    /**
     * Temporarily used during execution to track the number of rows
     * with parsing error.
     */
    private int m_parseErrorCount;

    /**
     * Temporarily used during execution to track the number of rows
     * with where finger print could not be computed.
     */
    private int m_fingerPrintErrorCount;


    /**
     * Create new node model with one data in- and one outport.
     */
    RDKitFingerprintNodeModel() {
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

        if (null == m_smiles.getStringValue()) {
            List<String> compatibleCols = new ArrayList<String>();
            for (DataColumnSpec c : inSpecs[0]) {
                if (c.getType().isCompatible(RDKitMolValue.class)) {
                    compatibleCols.add(c.getName());
                }
            }
            if (compatibleCols.size() == 1) {
                // auto-configure
                m_smiles.setStringValue(compatibleCols.get(0));
            } else if (compatibleCols.size() > 1) {
                // auto-guessing
                m_smiles.setStringValue(compatibleCols.get(0));
                setWarningMessage("Auto guessing: using column \""
                        + compatibleCols.get(0) + "\".");
            } else {
                throw new InvalidSettingsException("No RDKit Mol compatible "
                        + "column in input table. Use \"Molecule to RDKit\" "
                        + "node for Smiles or SDF.");
            }
        }
        if(m_minPath.getIntValue() > m_maxPath.getIntValue() ){
        	throw new InvalidSettingsException("minimum path length is larger than maximum path length.");
        }

        if (null == m_concate.getStringValue()) {
            if (null != m_smiles.getStringValue()) {
                // auto-configure
                String newName = DataTableSpec.getUniqueColumnName(inSpecs[0],
                        m_smiles.getStringValue() + " (Fingerprint)");
                m_concate.setStringValue(newName);
            } else {
                m_concate.setStringValue("RDKit Fingerprint");
            }
        }
        ColumnRearranger rearranger = createColumnRearranger(inSpecs[0]);
        return new DataTableSpec[]{rearranger.createSpec()};
    }

    private int[] findColumnIndices(final DataTableSpec spec)
            throws InvalidSettingsException {
        String first = m_smiles.getStringValue();
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
        m_parseErrorCount = 0;
        m_fingerPrintErrorCount = 0;
        BufferedDataTable outTable =
                exec.createColumnRearrangeTable(inData[0], rearranger, exec);
        StringBuilder msg = new StringBuilder();
        if (m_parseErrorCount > 0) {
            msg.append("Error parsing Smiles for " + m_parseErrorCount
                    + " rows.");
        }
        if (m_fingerPrintErrorCount > 0) {
            msg.append(" ");
            msg.append("Error computing fingerprint for "
                    + m_fingerPrintErrorCount + " rows.");
        }
        if (msg.length() > 0) {
            setWarningMessage(msg.toString());
        }
        return new BufferedDataTable[]{outTable};
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec spec)
            throws InvalidSettingsException {
        // check user settings against input spec here
        final int[] indices = findColumnIndices(spec);
        String inputCol = m_smiles.getStringValue();
        String newName = m_concate.getStringValue();
        if ((spec.containsName(newName) && !newName.equals(inputCol))
              ||  (spec.containsName(newName) && newName.equals(inputCol)
              && !m_removeSourceCols.getBooleanValue())) {
            throw new InvalidSettingsException("Cannot create column "
                    + newName + "since it is already in the input.");
        }
        ColumnRearranger result = new ColumnRearranger(spec);
        DataColumnSpecCreator appendSpec =
                new DataColumnSpecCreator(newName, DenseBitVectorCell.TYPE);
        result.append(new SingleCellFactory(appendSpec.createSpec()) {
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell firstCell = row.getCell(indices[0]);
                if (firstCell.isMissing()) {
                    return DataType.getMissingCell();
                }
                DataType firstType = spec.getColumnSpec(indices[0]).getType();
                ROMol mol = null;
                if (firstType.isCompatible(RDKitMolValue.class)) {
                    mol = ((RDKitMolValue)firstCell).readMoleculeValue();
                } else {
                    String smiles = ((StringValue)firstCell).toString();
                    mol = RDKFuncs.MolFromSmiles(smiles);
                }
                if (mol == null) {
                    LOGGER.debug("Error parsing smiles "
                            + "while processing row: " + row.getKey());
                    m_parseErrorCount++;
                    return DataType.getMissingCell();
                } else {
                    // transfer the bitset into a dense bit vector
                    DenseBitVector bitVector =
                            new DenseBitVector(m_numBits.getIntValue());
                    try {
                        if ("rdkit".equals(m_fpType.getStringValue())) {
                            ExplicitBitVect fingerprint;
                            fingerprint =
                                    RDKFuncs.RDKFingerprintMol(mol,
                                            m_minPath.getIntValue(),
                                            m_maxPath.getIntValue(),
                                            m_numBits.getIntValue(),
                                            2);
                            for (int i = 0; i < fingerprint.getNumBits(); i++) {
                                if (fingerprint.getBit(i))
                                    bitVector.set(i);
                            }
                            fingerprint.delete();
                        } else if ("atompair".equals(m_fpType.getStringValue())) {
                                ExplicitBitVect fingerprint;
                                fingerprint =
                                        RDKFuncs.getHashedAtomPairFingerprintAsBitVect(mol,m_numBits.getIntValue());
                                for (int i = 0; i < fingerprint.getNumBits(); i++) {
                                    if (fingerprint.getBit(i))
                                        bitVector.set(i);
                                }
                                fingerprint.delete();
                        } else if ("torsion".equals(m_fpType.getStringValue())) {
                            ExplicitBitVect fingerprint;
                            fingerprint =
                                    RDKFuncs.getHashedTopologicalTorsionFingerprintAsBitVect(mol,m_numBits.getIntValue());
                            for (int i = 0; i < fingerprint.getNumBits(); i++) {
                                if (fingerprint.getBit(i))
                                    bitVector.set(i);
                            }
                            fingerprint.delete();
                        } else if ("morgan".equals(m_fpType.getStringValue())) {
                            ExplicitBitVect fingerprint;
                            fingerprint =
                                    RDKFuncs.getMorganFingerprintAsBitVect(mol,m_radius.getIntValue(),
                                    		m_numBits.getIntValue());
                            for (int i = 0; i < fingerprint.getNumBits(); i++) {
                                if (fingerprint.getBit(i))
                                    bitVector.set(i);
                            }
                            fingerprint.delete();

                        } else if ("featmorgan".equals(m_fpType.getStringValue())) {
                        	UInt32_Vect ivs=new UInt32_Vect(mol.getNumAtoms());
                        	RDKFuncs.getFeatureInvariants(mol, ivs);
                            ExplicitBitVect fingerprint;
                            fingerprint =
                                    RDKFuncs.getMorganFingerprintAsBitVect(mol,m_radius.getIntValue(),
                                    		m_numBits.getIntValue(),ivs);
                            for (int i = 0; i < fingerprint.getNumBits(); i++) {
                                if (fingerprint.getBit(i))
                                    bitVector.set(i);
                            }
                            fingerprint.delete();
                        } else if ("layered".equals(m_fpType.getStringValue())) {
                            ExplicitBitVect fingerprint;
                            fingerprint =
                                    RDKFuncs.LayeredFingerprintMol(mol,
                                            m_layerFlags.getIntValue(),
                                            m_minPath.getIntValue(),
                                            m_maxPath.getIntValue(),
                                            m_numBits.getIntValue());
                            for (int i = 0; i < fingerprint.getNumBits(); i++) {
                                if (fingerprint.getBit(i))
                                    bitVector.set(i);
                            }
                            fingerprint.delete();
                        }
                    } catch (Exception ex) {
                        LOGGER.debug("Error while creating fingerprint "
                            + "for row: " + row.getKey());
                        m_fingerPrintErrorCount++;
                        return DataType.getMissingCell();
                    } finally {
                        mol.delete();
                    }
                    DenseBitVectorCellFactory fact =
                            new DenseBitVectorCellFactory(bitVector);
                    return fact.createDataCell();
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
        m_smiles.loadSettingsFrom(settings);
        m_concate.loadSettingsFrom(settings);
        m_removeSourceCols.loadSettingsFrom(settings);
        m_fpType.loadSettingsFrom(settings);
        m_minPath.loadSettingsFrom(settings);
        m_maxPath.loadSettingsFrom(settings);
        m_numBits.loadSettingsFrom(settings);
        m_radius.loadSettingsFrom(settings);
        m_layerFlags.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_smiles.saveSettingsTo(settings);
        m_concate.saveSettingsTo(settings);
        m_removeSourceCols.saveSettingsTo(settings);
        m_fpType.saveSettingsTo(settings);
        m_minPath.saveSettingsTo(settings);
        m_maxPath.saveSettingsTo(settings);
        m_numBits.saveSettingsTo(settings);
        m_radius.saveSettingsTo(settings);
        m_layerFlags.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_smiles.validateSettings(settings);
        m_concate.validateSettings(settings);
        m_removeSourceCols.validateSettings(settings);
        m_fpType.validateSettings(settings);
        m_minPath.validateSettings(settings);
        m_maxPath.validateSettings(settings);
        m_numBits.validateSettings(settings);
        m_radius.validateSettings(settings);
        m_layerFlags.validateSettings(settings);
    }
}
