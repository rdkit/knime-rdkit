/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2011
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
package org.rdkit.knime.nodes.saltstripper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.RDKit.ROMol;
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
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortType;
import org.rdkit.knime.types.RDKitMolCellFactory;
import org.rdkit.knime.types.RDKitMolValue;

/**
 * This is the model implementation of RDKitSaltStripper node. It is used to
 * define the tasks of the RDKitSaltStripper node i.e to remove the various
 * types of salts from the rdkit molecules. It species the input/output ports of
 * the node and calculates the output structure and data.
 * 
 * @author Dillip K Mohanty
 */
public class RDKitSaltStripperNodeModel extends NodeModel {

	/**
	 * final constant for blank.
	 */
	private static final String BLANK = "";

	/**
	 * final constant for salt.
	 */
	private static final String SALT = "salt";

	/**
	 * Logger instance for logging purpose.
	 */
	private static final NodeLogger logger = NodeLogger
			.getLogger(RDKitSaltStripperNodeModel.class);

	/**
	 * The SettingsModelString for the rdkit molecule column.
	 */
	private final SettingsModelString m_rdkitMolColumn = RDKitSaltStripperNodeDialog
			.createRdkitMolColumnModel();

	/**
	 * The SettingsModelString for the salt molecule column.
	 */
	private final SettingsModelString m_saltMolColumn = RDKitSaltStripperNodeDialog
			.createSaltMolColumnModel();

	/**
	 * The SettingsModelString for the checkbox for choosing whether or not the
	 * original molecule column should be kept.
	 */
	private final SettingsModelBoolean m_keepOrigMolecule = RDKitSaltStripperNodeDialog
			.createOrigMoleculeModel();

	/**
	 * Constructor for the node model. Specifies a mandatory input port for
	 * input table containing rdkit molecules and an optional input port for
	 * input table containing the salt definitions and one output ports for
	 * table contaiing the smiles for the molecules that can be processed.
	 */
	protected RDKitSaltStripperNodeModel() {

		// two incoming ports (one being optional) and one outgoing port is
		// assumed
		super(
				new PortType[] {
						// Input ports
						new PortType(
								BufferedDataTable.TYPE.getPortObjectClass(),
								false),
						new PortType(
								BufferedDataTable.TYPE.getPortObjectClass(),
								true) // Second parameter sets it to optional
				}, new PortType[] {
				// Output ports
				new PortType(BufferedDataTable.TYPE.getPortObjectClass(), false) });
	}

	/**
	 * {@inheritDoc} This method creates the output table containing an
	 * additional column for smiles for all the rows of the input table which
	 * can be processed for stripping of salts. It first reads the salt
	 * definitions (if any). If salt definition table is found then the
	 * stripping is done based on the salt definitions. But if no salt
	 * definition table is input then the default salt definitaions from
	 * "Salts.txt" file is read for stripping.
	 * 
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {

		logger.debug("Enter Execute !");
		DataTableSpec inSpecOpt = null;
		int nrCols = 0;
		DataTableSpec inSpec = inData[0].getDataTableSpec();
		if (inData[1] != null) {
			inSpecOpt = inData[1].getDataTableSpec();
			nrCols = inSpecOpt.getNumColumns();
		}
		ArrayList<ROMol> salts = new ArrayList<ROMol>();
		
		// if optional data for salt definitions are input by user then store in
		// a list.
		if (inSpecOpt != null) {
			// find column index for the salt definition
			final int[] rdkitSaltMolIndex = findColumnIndices(inSpecOpt, SALT);
			// iterate over each row of salt defn column
			for (DataRow row : inData[1]) {
				DataCell[] copy = new DataCell[nrCols];
				for (int j = 0; j < nrCols; j++) {
					copy[j] = row.getCell(j);
				}
				// iterate over the salt cells
				for (int j = 0; j < rdkitSaltMolIndex.length; j++) {
					DataCell cell = copy[rdkitSaltMolIndex[j]];
					// extract salt molecules if cell is not missing
					if (!cell.isMissing()) {
						RDKitMolValue rdkit = (RDKitMolValue) cell;
						ROMol mol = rdkit.readMoleculeValue();
						// add salt definitions to list
						salts.add(mol);
					}
				}
			}
		}
		// create a column rearranger for creating the data cells for output
		// smiles column
		// and removing the original rdkit molecule column (if specified by
		// user)
		ColumnRearranger rearranger = createColumnRearranger(inSpec, salts);
		BufferedDataTable outTable = exec.createColumnRearrangeTable(inData[0],
				rearranger, exec);
		
		// Free resources of salt list
		for (ROMol mol : salts) {
			mol.delete();
		}
		salts.clear();
		
		logger.debug("Exit Execute !");
		return new BufferedDataTable[] { outTable };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// Do nothing
	}

	/**
	 * {@inheritDoc} This method is used to specify the structure the output
	 * table. The first output table will have an additional column for smiles.
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		if (null == m_rdkitMolColumn.getStringValue()) {
            List<String> compatibleCols = new ArrayList<String>();
            for (DataColumnSpec colSpec : inSpecs[0]) {
                if (colSpec.getType().isCompatible(RDKitMolValue.class)) {
                    compatibleCols.add(colSpec.getName());
                }
            }
            if (compatibleCols.size() == 1) {
                //Auto-configuring.One RDKit column found 
            	m_rdkitMolColumn.setStringValue(compatibleCols.get(0));
            } else if (compatibleCols.size() > 1) {
                //Auto-guessing.More than one RDKit column found. Selecting the first one.
            	m_rdkitMolColumn.setStringValue(compatibleCols.get(0));
                setWarningMessage("Auto guessing: using column \""
                        + compatibleCols.get(0) + "\".");
            } else {
            	//no RDKit columns found
                throw new InvalidSettingsException("No RDKit compatible column in input table.");
            }
        }
		ColumnRearranger rearranger = createColumnRearranger(inSpecs[0], new ArrayList<ROMol>());
		return new DataTableSpec[] { rearranger.createSpec() };
	}

	/**
	 * This method is used for create output table with an added column for
	 * smiles of processed molecules. It iterates over the required columns of
	 * input table and creates the cell contents of the newly added column
	 * supplied by SingleCellFactory. Each input rdkit molecule is read and the
	 * salts are stripped from each molecule.
	 * 
	 * @param inSpec
	 * @return ColumnRearranger
	 * @throws InvalidSettingsException
	 */
	private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec, 
			final ArrayList<ROMol> salts)
			throws InvalidSettingsException {
		logger.debug("Enter createColumnRearranger !");
		
		// find the index of rdkit molecule column.
		final int[] rdkitMolIndex = findColumnIndices(inSpec, BLANK);
		
		// initialize the salt patterns
		final SaltStripper stripper = new SaltStripper(null, salts);
		ColumnRearranger result = new ColumnRearranger(inSpec);
		
		// Create a column spec for the new column to be added.
		DataColumnSpecCreator appendSpec = new DataColumnSpecCreator("Salt Stripped Molecule",
				RDKitMolCellFactory.TYPE);
		
		result.append(new SingleCellFactory(appendSpec.createSpec()) {
			@Override
			public DataCell getCell(final DataRow row) {
				DataCell firstCell = row.getCell(rdkitMolIndex[0]);
				if (firstCell.isMissing()) {
					return DataType.getMissingCell();
				}
				// read each cell value to get RDKit molecules
				RDKitMolValue rdkit = (RDKitMolValue) firstCell;
				ROMol mol = rdkit.readMoleculeValue();
				try {
					// Perform salt stripping and Convert Mol to RDKit.
					return RDKitMolCellFactory.createRDKitMolCell(stripper.stripSalts(mol));
				} finally {
					mol.delete();
				}
			}
		});
		// if the user want to remove the original input column from the table.
		if (!m_keepOrigMolecule.getBooleanValue()) {
			result.remove(rdkitMolIndex[0]);
		}
		logger.debug("Exit createColumnRearranger !");
		return result;
	}

	/**
	 * This method is used to find the index of the data column specified by the
	 * user in the options dialog. This method returns two column indices.
	 * First, for input molecule column of first input table and second, for the
	 * salt definition column of the second input table.
	 * 
	 * @param spec
	 * @param type
	 * @return int[]
	 * @throws InvalidSettingsException
	 */
	private int[] findColumnIndices(final DataTableSpec spec, String type)
			throws InvalidSettingsException {
		String column = null;
		if (type != null && type.equals(BLANK))
			column = m_rdkitMolColumn.getStringValue();
		else if (type != null && type.equals(SALT))
			column = m_saltMolColumn.getStringValue();
		if (column == null) {
			throw new InvalidSettingsException("Not configured yet.");
		}
		int columnIndex = spec.findColumnIndex(column);
		if (columnIndex < 0) {
			throw new InvalidSettingsException(
					"No such column in input table: " + column);
		}
		DataType firstType = spec.getColumnSpec(columnIndex).getType();
		if (!firstType.isCompatible(RDKitMolValue.class)) {
			throw new InvalidSettingsException("Column '" + column
					+ "' does not contain RDKit Mol values.");
		}
		return new int[] { columnIndex };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		m_rdkitMolColumn.saveSettingsTo(settings);
		m_saltMolColumn.saveSettingsTo(settings);
		m_keepOrigMolecule.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_rdkitMolColumn.loadSettingsFrom(settings);
		m_saltMolColumn.loadSettingsFrom(settings);
		m_keepOrigMolecule.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_rdkitMolColumn.validateSettings(settings);
		m_saltMolColumn.validateSettings(settings);
		m_keepOrigMolecule.validateSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
	}

}
