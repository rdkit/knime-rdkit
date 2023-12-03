/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2010-2023
 * Novartis Pharma AG, Switzerland
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
package org.rdkit.knime.nodes.onecomponentreaction;

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
import org.knime.core.data.AdapterValue;
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
import org.rdkit.knime.types.RDKitAdapterCell;
import org.rdkit.knime.types.RDKitMolCellFactory;
import org.rdkit.knime.types.RDKitMolValue;

/**
 *
 * @author Greg Landrum
 */
@Deprecated
public class RDKitOneComponentReactionNodeModel extends NodeModel {

	private final SettingsModelString m_first =
			RDKitOneComponentReactionNodeDialogPane.createFirstColumnModel();

	private final SettingsModelString m_smarts =
			RDKitOneComponentReactionNodeDialogPane.createSmartsModel();

	private final SettingsModelString m_rxnFileInput =
			RDKitOneComponentReactionNodeDialogPane.createFileModel();

	private final SettingsModelBoolean m_rxnFileEnableModel =
			RDKitOneComponentReactionNodeDialogPane.createFileEnableModel(
					m_rxnFileInput, m_smarts);

	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(RDKitOneComponentReactionNodeModel.class);

	/**
	 * Create new node model with one data in- and one outport.
	 */
	RDKitOneComponentReactionNodeModel() {
		super(1, 1);
	}

	private DataTableSpec[] createOutSpecs() {
		final Vector<DataColumnSpec> cSpec = new Vector<DataColumnSpec>();
		DataColumnSpecCreator crea =
				new DataColumnSpecCreator("Product", RDKitAdapterCell.RAW_TYPE);
		cSpec.add(crea.createSpec());
		crea = new DataColumnSpecCreator("Product Index", IntCell.TYPE);
		cSpec.add(crea.createSpec());
		crea = new DataColumnSpecCreator("Reactant 1 sequence index",
				IntCell.TYPE);
		cSpec.add(crea.createSpec());
		crea = new DataColumnSpecCreator("Reactant 1",
				RDKitAdapterCell.RAW_TYPE);
		cSpec.add(crea.createSpec());
		final DataTableSpec tSpec =
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

		if (null == m_first.getStringValue()) {
			final List<String> compatibleCols = new ArrayList<String>();
			for (final DataColumnSpec c : inSpecs[0]) {
				if (c.getType().isCompatible(RDKitMolValue.class) || c.getType().isAdaptable(RDKitMolValue.class)) {
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
		// validate only
		readRxn().delete();

		// further input spec check
		findColumnIndices(inSpecs[0]);

		return createOutSpecs();
	}

	/**
	 * @throws InvalidSettingsException */
	private ChemicalReaction readRxn() throws InvalidSettingsException {
		ChemicalReaction rxn;
		if (!m_rxnFileEnableModel.getBooleanValue()) {
			// read smarts field
			final String smartsString = m_smarts.getStringValue();
			if (smartsString == null || smartsString.isEmpty()) {
				throw new InvalidSettingsException("Invalid (empty) smarts");
			}
			rxn = ChemicalReaction.ReactionFromSmarts(smartsString);
			if (rxn == null) {
				throw new InvalidSettingsException("unparseable reaction smarts: "
						+ smartsString);
			}
		} else {
			// read from rxn file
			final String rxnFileLocation = m_rxnFileInput.getStringValue();
			if (rxnFileLocation == null || rxnFileLocation.isEmpty()) {
				throw new InvalidSettingsException("Neither a smarts pattern "
						+ "nor a file location has been specified.");
			}
			if (!new File(rxnFileLocation).exists()) {
				throw new InvalidSettingsException("No such RXN file: "
						+ rxnFileLocation);
			}
			try {
				rxn = ChemicalReaction.ReactionFromRxnFile(rxnFileLocation);
			} catch (final Exception e) {
				throw new InvalidSettingsException(
						"Unable to parse rxn file ", e);
			}
			if (rxn == null) {
				throw new InvalidSettingsException(
						"Unable to parse rxn file (RDKit lib returned null)");
			}
		}
		if (rxn.getNumReactantTemplates() != 1) {
			throw new InvalidSettingsException(
					"reaction should have exactly one reactant, it has: "
							+ rxn.getNumReactantTemplates());
		}

		if(!rxn.validate()){
			throw new InvalidSettingsException("reaction smarts has errors");
		}
		return rxn;
	}

	private int[] findColumnIndices(final DataTableSpec spec)
			throws InvalidSettingsException {
		final String first = m_first.getStringValue();
		if (first == null) {
			throw new InvalidSettingsException("Not configured yet");
		}
		final int firstIndex = spec.findColumnIndex(first);
		if (firstIndex < 0) {
			throw new InvalidSettingsException(
					"No such column in input table: " + first);
		}
		final DataType firstType = spec.getColumnSpec(firstIndex).getType();
		if (!firstType.isCompatible(RDKitMolValue.class) && !firstType.isAdaptable(RDKitMolValue.class)) {
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
		final DataTableSpec inSpec = inData[0].getDataTableSpec();

		final BufferedDataContainer productTable =
				exec.createDataContainer(createOutSpecs()[0]);

		// check user settings against input spec here
		final int[] indices = findColumnIndices(inSpec);

		// get the reaction:
		final ChemicalReaction rxn = readRxn();
		int parseErrorCount = 0;
		final int rowCount = inData[0].getRowCount();
		try {
			int count = 0;
			final RowIterator it = inData[0].iterator();
			while (it.hasNext()) {
				final DataRow row = it.next();
				count++;

				final DataCell firstCell = row.getCell(indices[0]);
				if (firstCell.isMissing()) {
					continue;
				} else {
					final DataType firstType =
							inSpec.getColumnSpec(indices[0]).getType();
					ROMol mol = null;
					if (firstType.isCompatible(RDKitMolValue.class)) {
						mol = ((RDKitMolValue)firstCell).readMoleculeValue();
					} 
					else if (firstType.isAdaptable(RDKitMolValue.class)) {
                  mol = ((AdapterValue)firstCell).getAdapter(RDKitMolValue.class).readMoleculeValue();
               } 
               else{
						final String smiles = ((StringValue)firstCell).toString();
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
					final ROMol_Vect rs = new ROMol_Vect(1);
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
							for (int psetidx = 0; psetidx < prods.size();
									psetidx++) {
								for (int pidx = 0; pidx < prods.get(psetidx)
										.size(); pidx++) {
									DataCell cell;
									final RWMol prod=new RWMol(prods.get(psetidx).get(pidx));
									try{
										RDKFuncs.sanitizeMol(prod);
										cell=RDKitMolCellFactory.createRDKitMolCellAndDelete(
												prod);
									} catch (final Exception e){
										prod.delete();
										prods.get(psetidx).get(pidx).delete();
										continue;
									}
									final DataCell[] cells =
											new DataCell[productTable
											             .getTableSpec()
											             .getNumColumns()];
									cells[0] = cell;
									cells[1] = new IntCell(pidx);
									cells[2] = new IntCell(count - 1);
									final ROMol temp = rs.get(0);
									cells[3] =
											RDKitMolCellFactory.createRDKitAdapterCell(
													temp);
									temp.delete();
									final DataRow drow =
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
				exec.setProgress(count / (double)rowCount,
						"Processed row " + count + "/" + rowCount + " (\""
								+ row.getKey() + "\")");
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
		m_first.loadSettingsFrom(settings);
		try {
			m_rxnFileEnableModel.loadSettingsFrom(settings);
			m_rxnFileInput.loadSettingsFrom(settings);
		} catch (final InvalidSettingsException ise) {
			// ignore, assume smarts (rxn file input added in later version)
		}
		m_smarts.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		m_first.saveSettingsTo(settings);
		m_rxnFileEnableModel.saveSettingsTo(settings);
		m_smarts.saveSettingsTo(settings);
		m_rxnFileInput.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_first.validateSettings(settings);
		m_smarts.validateSettings(settings);
		// don't verify rxn fields -- fields were added at later state
		// (would break backward compatibility)
	}
}
