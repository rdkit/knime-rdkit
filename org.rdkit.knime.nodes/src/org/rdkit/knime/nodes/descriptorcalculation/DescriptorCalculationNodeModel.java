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
package org.rdkit.knime.nodes.descriptorcalculation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.RDKit.Double_Vect;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.rdkit.knime.types.RDKitMolValue;

/**
 * This is the model implementation of DescriptorCalculation node. It is used to
 * define the tasks of the DescriptorCalculation node in the overridden methods
 * configure and execute. The design of the node model is such that if new
 * descriptors needs to be added, nothing needs to be changed except for adding
 * a call to addDescriptor() method for that descriptor in the static block of
 * this node model class.
 * 
 * @author Dillip K Mohanty
 */
public class DescriptorCalculationNodeModel extends NodeModel {

	/**
	 * Logger instance for logging purpose.
	 */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(DescriptorCalculationNodeModel.class);

	/**
	 * The settings object used to store the user set options.
	 */
	DescriptorCalcSettings descSettings = new DescriptorCalcSettings();

	/**
	 * Constructor for the node model. One input port and one output port is
	 * created.
	 */
	protected DescriptorCalculationNodeModel() {
		super(1, 1);
	}

	/**
	 * The list of all descriptor names for display on node dialog.
	 */
	public static final ArrayList<String> names = new ArrayList<String>();

	/**
	 * The map of name-descriptor object pairs.
	 */
	public static final Map<String, IDescriptor> descriptors = new HashMap<String, IDescriptor>();

	/**
	 * The method is used to add the descriptors to a map and a list which will
	 * later be used for display and calculation purpose.
	 * 
	 * @param name
	 * @param descObj
	 */
	private static void addDescriptor(String name, DescriptorObject descObj) {
		descriptors.put(name, descObj);
		names.add(name);
	}

	// Static block for adding all the descriptors with its functionality.
	// This is the place where one should be adding new descriptors.
	static {

		// Adding descriptor for slogp
		addDescriptor("slogp", new DescriptorObject(1, Double.class) {
			@Override
			public Object calculate(ROMol mol) {
				double slogp = RDKFuncs.calcMolLogP(mol);
				return new Double(slogp);
			}
		});
		// Adding descriptor for smr
		addDescriptor("smr", new DescriptorObject(1, Double.class) {
			@Override
			public Object calculate(ROMol mol) {
				double smr = RDKFuncs.calcMolMR(mol);
				return new Double(smr);
			}
		});
		// Adding descriptor for LabuteASA
		addDescriptor("LabuteASA", new DescriptorObject(1, Double.class) {
			@Override
			public Object calculate(ROMol mol) {
				double lasa = RDKFuncs.calcLabuteASA(mol);
				return new Double(lasa);
			}
		});
		// Adding descriptor for TPSA
		addDescriptor("TPSA", new DescriptorObject(1, Double.class) {
			@Override
			public Object calculate(ROMol mol) {
				double tpsa = RDKFuncs.calcTPSA(mol);
				return new Double(tpsa);
			}
		});
		// Adding descriptor for AMW
		addDescriptor("AMW", new DescriptorObject(1, Double.class) {
			@Override
			public Object calculate(ROMol mol) {
				double amw = RDKFuncs.calcAMW(mol, false);
				return new Double(amw);
			}
		});
		// Adding descriptor for ExactMW
		addDescriptor("ExactMW", new DescriptorObject(1, Double.class) {
			@Override
			public Object calculate(ROMol mol) {
				double emw = RDKFuncs.calcExactMW(mol, false);
				return new Double(emw);
			}
		});
		// Adding descriptor for NumLipinskiHBA
		addDescriptor("NumLipinskiHBA", new DescriptorObject(1, Long.class) {
			@Override
			public Object calculate(ROMol mol) {
				long nlhba = RDKFuncs.calcLipinskiHBA(mol);
				return new Long(nlhba);
			}
		});
		// Adding descriptor for NumLipinskiHBD
		addDescriptor("NumLipinskiHBD", new DescriptorObject(1, Long.class) {
			@Override
			public Object calculate(ROMol mol) {
				long nlhbd = RDKFuncs.calcLipinskiHBD(mol);
				return new Long(nlhbd);
			}
		});
		// Adding descriptor for NumRotatableBonds
		addDescriptor("NumRotatableBonds ",
				new DescriptorObject(1, Long.class) {
					@Override
					public Object calculate(ROMol mol) {
						long nrb = RDKFuncs.calcNumRotatableBonds(mol);
						return new Long(nrb);
					}
				});
		// Adding descriptor for NumHBD
		addDescriptor("NumHBD", new DescriptorObject(1, Long.class) {
			@Override
			public Object calculate(ROMol mol) {
				long nhbd = RDKFuncs.calcNumHBD(mol);
				return new Long(nhbd);
			}
		});
		// Adding descriptor for NumHBA
		addDescriptor("NumHBA", new DescriptorObject(1, Long.class) {
			@Override
			public Object calculate(ROMol mol) {
				long nhba = RDKFuncs.calcNumHBA(mol);
				return new Long(nhba);
			}
		});
		// Adding descriptor for NumHeteroAtoms
		addDescriptor("NumHeteroAtoms", new DescriptorObject(1, Long.class) {
			@Override
			public Object calculate(ROMol mol) {
				long nha = RDKFuncs.calcNumHeteroatoms(mol);
				return new Long(nha);
			}
		});
		// Adding descriptor for NumAmideBonds
		// addDescriptor("NumAmideBonds", new DescriptorObject(1, Long.class) {
		// @Override
		// public Object calculate(ROMol mol) {
		// long nab = RDKFuncs.calcAmideBonds(mol); //
		// RDKFuncs.calcAmideBonds(mol) given by greg
		// return new Long(nab);
		// }
		// });
		// Adding descriptor for NumRings
		addDescriptor("NumRings", new DescriptorObject(1, Long.class) {
			@Override
			public Object calculate(ROMol mol) {
				long nlhba = RDKFuncs.calcNumRings(mol);
				return new Long(nlhba);
			}
		});
		// Adding descriptor for NumHeavyAtoms
		addDescriptor("NumHeavyAtoms", new DescriptorObject(1, Long.class) {
			@Override
			public Object calculate(ROMol mol) {
				long nlhba = mol.getNumAtoms(true);
				return new Long(nlhba);
			}
		});
		// Adding descriptor for NumAtoms
		addDescriptor("NumAtoms", new DescriptorObject(1, Long.class) {
			@Override
			public Object calculate(ROMol mol) {
				long nlhba = mol.getNumAtoms(false);
				return new Long(nlhba);
			}
		});
		// Adding descriptor for slogp_VSA[1..12]
		addDescriptor("slogp_VSA[1..12]", new DescriptorObject(12,
				Double[].class) {
			@Override
			public Object calculate(ROMol mol) {
				Double_Vect slogp_VSA = RDKFuncs.calcSlogP_VSA(mol);
				Double[] slogp_VSA_dbl = new Double[12];
				// convert Double_Vect to Double array
				for (int i = 0; i < slogp_VSA.size(); i++) {
					slogp_VSA_dbl[i] = slogp_VSA.get(i);
				}
				return slogp_VSA_dbl;
			}
		});
		// Adding descriptor for smr_VSA[1..10]
		addDescriptor("smr_VSA[1..10]",
				new DescriptorObject(10, Double[].class) {
					@Override
					public Object calculate(ROMol mol) {
						Double_Vect smr_VSA = RDKFuncs.calcSMR_VSA(mol);
						Double[] smr_VSA_dbl = new Double[10];
						// convert Double_Vect to Double array
						for (int i = 0; i < smr_VSA.size(); i++) {
							smr_VSA_dbl[i] = smr_VSA.get(i);
						}
						return smr_VSA_dbl;
					}
				});
		// Adding descriptor for peoe_VSA[1..14]
		addDescriptor("peoe_VSA[1..14]", new DescriptorObject(14,
				Double[].class) {
			@Override
			public Object calculate(ROMol mol) {
				Double_Vect peoe_VSA = RDKFuncs.calcPEOE_VSA(mol);
				Double[] peoe_VSA_dbl = new Double[14];
				// convert Double_Vect to Double array
				for (int i = 0; i < peoe_VSA.size(); i++) {
					peoe_VSA_dbl[i] = peoe_VSA.get(i);
				}
				return peoe_VSA_dbl;
			}
		});
	}

	/**
	 * {@inheritDoc} Method for creating the output spec for descriptor
	 * calculation node.
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		if (null == descSettings.colName) {
			List<String> compatibleCols = new ArrayList<String>();
			for (DataColumnSpec colSpec : inSpecs[0]) {
				if (colSpec.getType().isCompatible(RDKitMolValue.class)) {
					compatibleCols.add(colSpec.getName());
				}
			}
			if (compatibleCols.size() == 1) {
				// Auto-configuring.One RDkit column found
				descSettings.colName = compatibleCols.get(0);
			} else if (compatibleCols.size() > 1) {
				// Auto-guessing.More than one RDkit column found. Selecting the
				// first one.
				descSettings.colName = compatibleCols.get(0);
				setWarningMessage("Auto guessing: using column \""
						+ compatibleCols.get(0) + "\".");
			} else {
				// no RDkit columns found
				throw new InvalidSettingsException("No RDKit compatible "
						+ "column in input table.");
			}
		}
		if (null == descSettings.selectedDescriptors) {
			// Auto Configuring. All descriptors selected by default.
			descSettings.selectedDescriptors = (String[]) names
					.toArray(new String[] {});
		}
		ColumnRearranger rearranger = createColumnRearranger(inSpecs[0]);
		return new DataTableSpec[] { rearranger.createSpec() };
	}

	/**
	 * {@inheritDoc} Method to execute the descriptor calculation node. This
	 * method calculates the values for each selected descriptor.
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		LOGGER.debug("Enter execute");

		DataTableSpec inSpec = inData[0].getDataTableSpec();
		ColumnRearranger rearranger = createColumnRearranger(inSpec);
		BufferedDataTable outTable = exec.createColumnRearrangeTable(inData[0],
				rearranger, exec);
		LOGGER.debug("Exit execute");
		return new BufferedDataTable[] { outTable };
	}

	/**
	 * The calculated descriptor values will be appended to input table. The
	 * getCells() method is overridden for creation of an array of cells.
	 * 
	 * @param inSpec
	 *            : input specification
	 * @return ColumnRearranger
	 * @throws InvalidSettingsException
	 */
	private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec)
			throws InvalidSettingsException {

		// find the index of rdkit molecule column.
		final int rdkitMolIndex = findColumnIndices(inSpec);
		ColumnRearranger result = new ColumnRearranger(inSpec);
		final DataColumnSpec[] pSpecs = getDataColumnSpecs(inSpec);

		result.append(new AbstractCellFactory(pSpecs) {
			@Override
			public DataCell[] getCells(DataRow row) {
				DataCell[] ret = new DataCell[getNoOfColumnsToAdd()];
				DataCell cell = row.getCell(rdkitMolIndex);
				if(!cell.isMissing()) {
					// read each cell value to get RDKit molecules
					RDKitMolValue rdkit = (RDKitMolValue) cell;
					ROMol mol = rdkit.readMoleculeValue();
					// create the cells for each row
					ret = createRowCells(mol);
					mol.delete();
				} else {
					for (int i = 0; i < ret.length; i++) {
						ret[i] = DataType.getMissingCell();
					}
				}
				return ret;
			}
		});

		return result;
	}

	/**
	 * This method is used to find the index of the data column specified by the
	 * user.
	 * 
	 * @param spec
	 *            input specification
	 * @return int index
	 * @throws InvalidSettingsException
	 */
	private int findColumnIndices(final DataTableSpec spec)
			throws InvalidSettingsException {
		String first = descSettings.colName;
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
	 * This method is called from the execute method for creating the array of
	 * cells for each row. The cells are populated with the calculated values of
	 * each descriptor selected by the user.
	 * 
	 * @param mol
	 * @param inputRow
	 * @return DataCell[]
	 */
	private DataCell[] createRowCells(ROMol mol) {

		LOGGER.debug("Enter createRowCells");

		DataCell result = null;
		DataCell[] arrResult = null;
		DataCell[] cells = new DataCell[getNoOfColumnsToAdd()];
		Object obj = null;
		int colCount = 0;
		// iterate over the selected descriptors for calculating the descriptor
		// values
		for (String s : descSettings.selectedDescriptors) {
			obj = descriptors.get(s).calculate(mol);
			if (obj instanceof Integer) {
				result = new IntCell(((Integer) obj).intValue());
				cells[colCount++] = result;
			} else if (obj instanceof Integer[]) {
				Integer[] intArr = (Integer[]) obj;
				arrResult = new DataCell[intArr.length];
				for (int j = 0; j < intArr.length; j++) {
					arrResult[j] = new IntCell(((Integer) intArr[j]).intValue());
					cells[colCount++] = arrResult[j];
				}
			} else if (obj instanceof Long) {
				result = new LongCell(((Long) obj).longValue());
				cells[colCount++] = result;
			} else if (obj instanceof Long[]) {
				Long[] longArr = (Long[]) obj;
				arrResult = new DataCell[longArr.length];
				for (int j = 0; j < longArr.length; j++) {
					arrResult[j] = new LongCell(((Long) longArr[j]).longValue());
					cells[colCount++] = arrResult[j];
				}
			} else if (obj instanceof Double) {
				result = new DoubleCell(((Double) obj).doubleValue());
				cells[colCount++] = result;
			} else if (obj instanceof Double[]) {
				Double[] doubleArr = (Double[]) obj;
				arrResult = new DataCell[doubleArr.length];
				for (int j = 0; j < doubleArr.length; j++) {
					arrResult[j] = new DoubleCell(
							((Double) doubleArr[j]).doubleValue());
					cells[colCount++] = arrResult[j];
				}
			} else if (obj instanceof String) {
				result = new StringCell(obj.toString());
				cells[colCount++] = result;
			} else {
				LOGGER.debug("Unexpected calculation result for the descriptor "
						+ s);
				cells[colCount++] = DataType.getMissingCell();
			}
		}
		LOGGER.debug("Exit createRowCells");
		return cells;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// do nothing
	}

	/**
	 * This method is used to create the output data table specification
	 * depending on the number and type of descriptors selected by the user.
	 * 
	 * @param inputTableSpec
	 * @return DataTableSpec Object
	 * @throws InvalidSettingsException
	 */
	// protected DataTableSpec getDataTableSpec(DataTableSpec inputTableSpec)
	protected DataColumnSpec[] getDataColumnSpecs(DataTableSpec inputTableSpec)
			throws InvalidSettingsException {

		LOGGER.debug("Enter getDataTableSpec");

		DataColumnSpec[] specs = new DataColumnSpec[getNoOfColumnsToAdd()];
		int colCount = 0;
		DescriptorObject obj = null;
		if (descSettings.selectedDescriptors != null
				&& descSettings.selectedDescriptors.length > 0) {
			// iterate over the selected descriptors and size of the result. if
			// its a single result
			// then only one column is added to the table spec.
			// If the result is an array, then number of columns added is equal
			// to the the array size.
			for (int j = 0; j < descSettings.selectedDescriptors.length; j++) {
				String descName = descSettings.selectedDescriptors[j];
				obj = (DescriptorObject) descriptors.get(descName);
				if (obj.getType() != null) {
					if (obj.getType() == Integer.class) {
						specs[colCount++] = new DataColumnSpecCreator(
								DataTableSpec.getUniqueColumnName(
										inputTableSpec, descName), IntCell.TYPE)
								.createSpec();
					} else if (obj.getType() == Integer[].class) {
						for (int k = 0; k < obj.getSize(); k++) {
							specs[colCount++] = new DataColumnSpecCreator(
									DataTableSpec
											.getUniqueColumnName(
													inputTableSpec,
													getArrayColumnName(
															descName, k + 1)),
									IntCell.TYPE).createSpec();
						}
					} else if (obj.getType() == Long.class) {
						specs[colCount++] = new DataColumnSpecCreator(
								DataTableSpec.getUniqueColumnName(
										inputTableSpec, descName),
								LongCell.TYPE).createSpec();
					} else if (obj.getType() == Long[].class) {
						for (int k = 0; k < obj.getSize(); k++) {
							specs[colCount++] = new DataColumnSpecCreator(
									DataTableSpec
											.getUniqueColumnName(
													inputTableSpec,
													getArrayColumnName(
															descName, k + 1)),
									LongCell.TYPE).createSpec();
						}
					} else if (obj.getType() == Double.class) {
						specs[colCount++] = new DataColumnSpecCreator(
								DataTableSpec.getUniqueColumnName(
										inputTableSpec, descName),
								DoubleCell.TYPE).createSpec();
					} else if (obj.getType() == Double[].class) {
						for (int k = 0; k < obj.getSize(); k++) {
							specs[colCount++] = new DataColumnSpecCreator(
									DataTableSpec
											.getUniqueColumnName(
													inputTableSpec,
													getArrayColumnName(
															descName, k + 1)),
									DoubleCell.TYPE).createSpec();
						}
					} else if (obj.getType() == String.class) {
						specs[colCount++] = new DataColumnSpecCreator(
								DataTableSpec.getUniqueColumnName(
										inputTableSpec, descName),
								StringCell.TYPE).createSpec();
					}
				}
			}
		}
		LOGGER.debug("Exit getDataTableSpec");
		return specs;
	}

	/**
	 * This method is used to assign proper names for the descriptor columns.
	 * 
	 * @param descName
	 * @param i
	 * @return String descritor column name
	 */
	private String getArrayColumnName(String descName, int i) {
		String delims = "\\[";
		String[] name = descName.split(delims);
		return name[0] + i;
	}

	/**
	 * Method to calculates the number of additional columns to add in the
	 * output table.
	 * 
	 * @return number of columns to add
	 */
	private int getNoOfColumnsToAdd() {
		DescriptorObject obj = null;
		int countColsToAdd = 0;
		if (descSettings.selectedDescriptors != null
				&& descSettings.selectedDescriptors.length > 0) {
			for (int j = 0; j < descSettings.selectedDescriptors.length; j++) {
				String descName = descSettings.selectedDescriptors[j];
				obj = (DescriptorObject) descriptors.get(descName);
				countColsToAdd = countColsToAdd + obj.getSize();
			}
		}
		return countColsToAdd;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		descSettings.saveSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		descSettings.loadSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		DescriptorCalcSettings s = new DescriptorCalcSettings();
		s.loadSettings(settings);

		if (s.colName == null || s.colName.length() < 1) {
			throw new InvalidSettingsException("column name must be specified");
		}
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
