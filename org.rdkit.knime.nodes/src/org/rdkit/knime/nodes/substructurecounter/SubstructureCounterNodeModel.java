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
package org.rdkit.knime.nodes.substructurecounter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.RDKit.Match_Vect_Vect;
import org.RDKit.ROMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.LongCell;
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
import org.rdkit.knime.types.RDKitMolValue;

/**
 * This is the model implementation of SubstructureCounter. This Node calculates
 * the the number of times the query molecule is present in the input molecule.
 * 
 * 
 * @author Swarnaprava Singh
 */
public class SubstructureCounterNodeModel extends NodeModel {

	// the logger instance
	private static final NodeLogger logger = NodeLogger
			.getLogger(SubstructureCounterNodeModel.class);
	// Dialog component for getting the input molecule column
	private final SettingsModelString m_MolInput = SubstructureCounterNodeDialog
			.createMoleculeInputModel();
	// Dialog component for getting the query molecule column
	private final SettingsModelString m_QueryInput = SubstructureCounterNodeDialog
			.createQueryInputModel();
	// Dialog component to choose if unique matches are to be calculated
	private final SettingsModelBoolean m_UniqueMatches = SubstructureCounterNodeDialog
			.createCBCountUniqueMatches();

	/**
	 * Constructor for the node model. This node has 2 input ports and 1 output
	 * port. Port 0 is for the input molecule table. Port 1 is for the query
	 * molecule table.
	 */
	protected SubstructureCounterNodeModel() {
		super(2, 1);
	}

	/**
	 * This method implements the business logic for the calculation of the
	 * substructures in the molecules.
	 * 
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		logger.debug("Enter execute");
		DataTableSpec inSpecMolecule = inData[0].getDataTableSpec();
		DataTableSpec inSpecQuery = inData[1].getDataTableSpec();
		ColumnRearranger rearranger;
		ROMol[] patterns = new ROMol[inData[1].getRowCount()];
		String[] queryArr = new String[inData[1].getRowCount()];
		try {
			// find the rdkit column indices in molecule table and query table
			final int[] indices = findColumnIndices(new DataTableSpec[] {
					inSpecMolecule, inSpecQuery });
			int i = 0;
			// Creating an array of the ROMol query Molecules
			for (DataRow row : inData[1]) {
				if (!row.getCell(indices[1]).isMissing()) {
					patterns[i] = ((RDKitMolValue) row.getCell(indices[1]))
							.readMoleculeValue();
					if(patterns[i]==null){
						queryArr[i]="col_"+(i+1);
					} else {
						queryArr[i]=patterns[i].MolToSmiles(true);
					}
				} else {
					patterns[i] = null;
					queryArr[i] = "col_"+(i+1);
				}
				i++;
			}
			/*
			// Creating an array of unique query molecules
			Set<String> s = new HashSet<String>();
			for (int j = 0; j < patterns.length; j++) {
				if (null != patterns[j]) {
					String smi=patterns[j].MolToSmiles();
					if(! s.contains(smi)){
						s.add(smi);
					} else {
						patterns[j]=null;
					}
				}
			String[] queryArr = (String[]) s.toArray(new String[] {});
			}*/

			// Read the rdkit molecules and find the count of the query patterns
			// for each molecule.
			HashMap<RowKey, long[]> countMap = countMatchesinMolecule(
					exec.createSubExecutionContext(0.5), patterns, inData,
					indices);
			// create column rearranger
			rearranger = createColumnRearranger(inSpecMolecule, inSpecQuery,
					queryArr, patterns, countMap);
		} finally {
			// delete the patterns ROMol objects
			for (ROMol p : patterns) {
				if (p != null) {
					p.delete();
				}
			}
		}
		BufferedDataTable outTable = exec.createColumnRearrangeTable(inData[0],
				rearranger, exec.createSubExecutionContext(0.5));
		logger.debug("Exit execute");
		return new BufferedDataTable[] { outTable };
	}

	/**
	 * Counts the number of matches for each query molecule and stores it in a
	 * map.
	 * 
	 * @param exec
	 * @param patterns
	 * @param inData
	 * @param indices
	 * @return
	 * @throws CanceledExecutionException
	 */
	private HashMap<RowKey, long[]> countMatchesinMolecule(
			ExecutionContext exec, ROMol[] patterns,
			final BufferedDataTable[] inData, int[] indices)
			throws CanceledExecutionException {

		final boolean countUniqueMatchesOnly = m_UniqueMatches
				.getBooleanValue();

		long[] counter = new long[patterns.length];
		HashMap<RowKey, long[]> countMap = new HashMap<RowKey, long[]>();
		if (inData[0].getRowCount() != 0) {
			for (DataRow row : inData[0]) {
				DataCell cell = row.getCell(indices[0]);
				if (!cell.isMissing()) {
					ROMol mol = null;
					try {
						// read each cell value to get RDKit molecules
						RDKitMolValue rdkit = (RDKitMolValue) cell;
						mol = rdkit.readMoleculeValue();
						counter = new long[patterns.length];
						for (int pidx = 0; pidx < patterns.length; pidx++) {
							ROMol p = patterns[pidx];
							if (p != null && mol != null) {
								// Get the count of matching structures.
								Match_Vect_Vect ms = mol.getSubstructMatches(p,
										countUniqueMatchesOnly);
								counter[pidx] = ms.size();
							}
						}
						countMap.put(row.getKey(), counter);
					} finally {
						// delete the ROMol object created for each rdkit
						// molecule.
						mol.delete();
					}
				}
				if (exec != null) {
					exec.setProgress(countMap.size()
							/ (double) inData[0].getRowCount());
					exec.getProgressMonitor().checkCanceled();
				}
			}
		}
		return countMap;
	}

	/**
	 * The count columns will be added to the output table. The number of
	 * columns added is equal to the number of distinct query molecules present
	 * in the input table. The getCells() method is overridden for creation of
	 * an array of long cells.
	 * 
	 * @param inSpecMolecule
	 *            : spec of the rdkit molecule column
	 * @param inSpecQuery
	 *            : spec of the query molecule column
	 * @param arrQuery
	 *            : Array of query molecules as smiles.
	 * @param patterns
	 *            : Array of query molecules ROMol objects
	 * @param countMap
	 *            : Map containing count values of matching structures.
	 * @return ColumnRearranger
	 * @throws InvalidSettingsException
	 */
	private ColumnRearranger createColumnRearranger(
			final DataTableSpec inSpecMolecule,
			final DataTableSpec inSpecQuery, String[] arrQuery,
			final ROMol[] patterns, final HashMap<RowKey, long[]> countMap)
			throws InvalidSettingsException {

		// find the index of rdkit molecule column.
		final int[] rdkitMolIndex = findColumnIndices(new DataTableSpec[] {
				inSpecMolecule, inSpecQuery });
		ColumnRearranger result = new ColumnRearranger(inSpecMolecule);
		// Create the output specs
		final DataColumnSpec[] pSpecs = createOutSpec(inSpecMolecule, arrQuery);
		// append the new columns
		result.append(new AbstractCellFactory(pSpecs) {
			@Override
			public DataCell[] getCells(DataRow row) {
				DataCell cell = row.getCell(rdkitMolIndex[0]);
				DataCell[] cells = new DataCell[patterns.length];
				long[] count = countMap.get(row.getKey());
				for (int pidx = 0; pidx < patterns.length; pidx++) {
					if (cell.isMissing()) {
						cells[pidx] = DataType.getMissingCell();
					} else {
						cells[pidx] = new LongCell(count[pidx]);
					}
				}
				return cells;
			}
		});
		return result;
	}

	/**
	 * This method creates and returns the array of column specifications for
	 * the output table. The output table is the input molecule table with one
	 * additional column for each row in the query molecule table.
	 * 
	 * @param inSpec
	 *            : Input table specification.
	 * @param arrQuery
	 *            : String array of the query molecules which will be the column
	 *            names for the output table.
	 * @return DataColumnSpec[] : Array Column Specification for the output
	 *         table.
	 */
	private DataColumnSpec[] createOutSpec(DataTableSpec inSpec,
			String[] arrQuery) {
		DataColumnSpec[] cspec = new DataColumnSpec[arrQuery.length];
		for (int i = 0; i < arrQuery.length; i++) {
			cspec[i] = new DataColumnSpecCreator(arrQuery[i], DoubleCell.TYPE)
					.createSpec();
		}
		return cspec;
	}

	/**
	 * This method calculates and return the indices of the array of indices of
	 * the column containing the RDKit column in the input table.
	 * 
	 * @param specs
	 *            : array of input table specifications.
	 * @return int[]: returns an int array of indices.
	 * @throws InvalidSettingsException
	 */
	private int[] findColumnIndices(DataTableSpec[] specs)
			throws InvalidSettingsException {
		logger.debug("Enter findColumnIndices");
		String rdkit = m_MolInput.getStringValue();

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

		String query = m_QueryInput.getStringValue();
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
		logger.debug("Exit findColumnIndices");
		return new int[] { rdkitIndex, queryIndex };
	}

	/**
	 * {@inheritDoc} This method is used to specify the structure of the output
	 * table.
	 * 
	 */
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		logger.debug("Inside configure");
		if (null == m_MolInput.getStringValue()) {
			List<String> compatibleCols = new ArrayList<String>();
			for (DataColumnSpec c : inSpecs[0]) {
				if (c.getType().isCompatible(RDKitMolValue.class)) {
					compatibleCols.add(c.getName());
				}
			}
			if (compatibleCols.size() == 1) {
				// auto-configure
				m_MolInput.setStringValue(compatibleCols.get(0));
			} else if (compatibleCols.size() > 1) {
				m_MolInput.setStringValue(compatibleCols.get(0));
				setWarningMessage("Auto guessing: using column \""
						+ compatibleCols.get(0) + "\".");
			} else {
				throw new InvalidSettingsException("No RDKit compatible "
						+ "column in input table.");
			}
		}
		if (null == m_QueryInput.getStringValue()) {
			List<String> compatibleCols1 = new ArrayList<String>();
			for (DataColumnSpec c : inSpecs[1]) {
				if (c.getType().isCompatible(RDKitMolValue.class)) {
					compatibleCols1.add(c.getName());
				}
			}
			if (compatibleCols1.size() == 1) {
				// auto-configure
				m_QueryInput.setStringValue(compatibleCols1.get(0));
			} else if (compatibleCols1.size() > 1) {
				m_QueryInput.setStringValue(compatibleCols1.get(0));
				setWarningMessage("Auto guessing: using column \""
						+ compatibleCols1.get(0) + "\".");
			} else {
				throw new InvalidSettingsException("No RDKit compatible "
						+ "column in input table.");
			}
		}
		logger.debug("Exit configure");
		return new DataTableSpec[] { null };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// Nothing to reset
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		m_MolInput.saveSettingsTo(settings);
		m_QueryInput.saveSettingsTo(settings);
		m_UniqueMatches.saveSettingsTo(settings);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_MolInput.loadSettingsFrom(settings);
		m_QueryInput.loadSettingsFrom(settings);
		m_UniqueMatches.loadSettingsFrom(settings);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_MolInput.validateSettings(settings);
		m_QueryInput.validateSettings(settings);
		m_UniqueMatches.validateSettings(settings);

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
