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
package org.rdkit.knime.nodes.functionalgroupfilter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.RDKit.ROMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DefaultRow;
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
import org.rdkit.knime.nodes.functionalgroupfilter.FunctionalGroupNodeSettings.LineProperty;
import org.rdkit.knime.types.RDKitMolValue;


/**
 * This is the model implementation of FunctionalGroupFilter node. It is used to
 * define the tasks of the FunctionalGroupFilter node i.e to filter the
 * molecules containing specified number of functional groups into a table and
 * the rest to another table. It species the input/output ports of the node and
 * calculates the output structure and data.
 * 
 * @author Dillip K Mohanty
 */
public class FunctionalGroupFilterNodeModel extends NodeModel {

	/**
	 * Logger instance for logging purpose.
	 */
	private static final NodeLogger logger = NodeLogger
			.getLogger(FunctionalGroupFilterNodeModel.class);

	/**
	 * Node Settings instance to transfer values.
	 */
	private final FunctionalGroupNodeSettings m_settings = new FunctionalGroupNodeSettings();

	/**
	 * Constructor for the node model. Specifies one incoming port and two
	 * outgoing ports (one for molecules that pass the filter and another for
	 * molecules that don't pass the filter)
	 */
	protected FunctionalGroupFilterNodeModel() {

		super(1, 2);
	}

	/**
	 * {@inheritDoc} This method creates two output tables, one table containing
	 * all the molecules that pass the specified filter and the other table
	 * containing molecules that don't pass the filter.
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {

		logger.debug("Enter Execute: Filtering funtional groups....");

		DataTableSpec inSpec = inData[0].getDataTableSpec();
		// Specify the spec of the first output table
		DataTableSpec outSpecPass = inSpec;
		// Specify the spec of the second output table
		DataTableSpec outSpecFail = inSpec;
		// find column index of the molecule column
		int index = findColumnIndices(inSpec);
		int nrCols = outSpecPass.getNumColumns();
		BufferedDataContainer contPass = exec.createDataContainer(outSpecPass);
		BufferedDataContainer contFail = exec.createDataContainer(outSpecFail);
		try {
			// initialize the functional group patterns with the user specified
			// definition file
			// or the default one in case none is mentioned and store selected group
			// filters in a list.
			FunctionalGroupFilter filter = new FunctionalGroupFilter();
			filter.setFilterPatternList(m_settings.properties(),
					filter.readFuncGroupPatterns(m_settings.getFileUrl()), exec.createSubExecutionContext(0.5));
			//create the output tables. One for molecules passing the filter 
			//and other for molecules failing the filter.
			createOutput(inData, exec.createSubExecutionContext(0.5), index, nrCols, contPass, contFail,
					filter);
		} finally {
			// Close data containers
			contPass.close();
			contFail.close();
		}
		logger.debug("Exit Execute: Filtering funtional groups....");
		return new BufferedDataTable[] { contPass.getTable(),
				contFail.getTable() };
	}

	/**
	 * This method is used for creating the output tables. The first table contains 
	 * molecules passing the filter and the second table contains molecules failing the filter.
	 * It also calculates the progress using the execution context.
	 * 
	 * @param inData
	 * @param exec
	 * @param index
	 * @param nrCols
	 * @param contPass
	 * @param contFail
	 * @param filter
	 * @throws CanceledExecutionException
	 */
	private void createOutput(final BufferedDataTable[] inData,
			final ExecutionContext exec, int index, int nrCols,
			BufferedDataContainer contPass, BufferedDataContainer contFail,
			FunctionalGroupFilter filter)
			throws CanceledExecutionException {
		boolean isFound = false;
		int rowIdx = -1;
        int rows = inData[0].getRowCount();
		// iterate over the each row in the input data table.
		for (DataRow row : inData[0]) {
			DataCell[] copy = new DataCell[nrCols];
			for (int j = 0; j < nrCols; j++) {
				copy[j] = row.getCell(j);
			}
			ROMol mol = null;
			DataCell cell = copy[index];
			if (!cell.isMissing()) {
				// read each cell value to get RDKit molecules
				RDKitMolValue rdkit = (RDKitMolValue) cell;
				mol = rdkit.readMoleculeValue();
				try {
					// check whether the filter pattern is found or not
					isFound = filter.checkFunctionalGroup(mol);
				} finally {
                    mol.delete();
                }
			}
			// If found, store is molecule one table else store molecule in
			// other.
			if (isFound) {
				contPass.addRowToTable(new DefaultRow(row.getKey(), copy));
			} else {
				contFail.addRowToTable(new DefaultRow(row.getKey(), copy));
			}
			rowIdx++;
			exec.checkCanceled();
			exec.setProgress(rowIdx / (double)rows, "Adding row " + rowIdx
                    + " of " + rows);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// do nothing
	}

	/**
	 * {@inheritDoc} The configure method is used to create table spec for out
	 * tables. The output table spec is same as input table spec.
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		if (null == m_settings.getColName()) {
            List<String> compatibleCols = new ArrayList<String>();
            for (DataColumnSpec colSpec : inSpecs[0]) {
                if (colSpec.getType().isCompatible(RDKitMolValue.class)) {
                    compatibleCols.add(colSpec.getName());
                }
            }
            if (compatibleCols.size() == 1) {
                //Auto-configuring.One RDkit column found 
            	m_settings.setColName(compatibleCols.get(0));
            } else if (compatibleCols.size() > 1) {
                //Auto-guessing.More than one RDkit column found. Selecting the first one.
            	m_settings.setColName(compatibleCols.get(0));
                setWarningMessage("Auto guessing: using column \""
                        + compatibleCols.get(0) + "\".");
            } else {
            	//no RDkit columns found
                throw new InvalidSettingsException("No RDKit compatible "
                        + "column in input table.");
            }
        }
		boolean checked = false;
		if(!m_settings.properties().isEmpty()) {
			for (LineProperty p : m_settings.properties()) {
				LineProperty property = new LineProperty(p);
				if (property.isSelect()) {
					checked = true;
				}
			}
			if(!checked) {
				setWarningMessage("No functional groups selected. No filter will be applied on molecules.");
			}
		} else {
			setWarningMessage("No functional groups loaded. No filter will be applied on molecules.");
		}
		return new DataTableSpec[] { inSpecs[0], inSpecs[0] };
	}

	/**
	 * This method is used to find the index of the data column specified by the
	 * user.
	 * 
	 * @param spec
	 * @return int array
	 * @throws InvalidSettingsException
	 */
	private int findColumnIndices(final DataTableSpec spec)
			throws InvalidSettingsException {
		String first = m_settings.getColName();// change it to actual
		if (first == null) {
			throw new InvalidSettingsException("Not configured yet.");
		}
		int firstIndex = spec.findColumnIndex(first);
		if (firstIndex < 0) {
			throw new InvalidSettingsException(
					"No such column in input table: " + first);
		}
		DataType firstType = spec.getColumnSpec(firstIndex).getType();
		if (!firstType.isCompatible(RDKitMolValue.class)) {
			throw new InvalidSettingsException("Column '" + first
					+ "' does not contain strings");
		}
		return firstIndex;
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
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {

		m_settings.loadSettings(settings);
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {

		FunctionalGroupNodeSettings m_settings = new FunctionalGroupNodeSettings();
		m_settings.loadSettings(settings);
		
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
