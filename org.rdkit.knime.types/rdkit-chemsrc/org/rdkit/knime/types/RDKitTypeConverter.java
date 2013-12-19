/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2013
 * Novartis Institutes for BioMedical Research
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
package org.rdkit.knime.types;

import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.RDKit.RWMol;
import org.knime.chem.types.SdfCellFactory;
import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmilesAdapterCell;
import org.knime.chem.types.SmilesCell;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellTypeConverter;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.RWAdapterValue;
import org.knime.core.data.StringValue;

/**
 * Converter for RDKit that converts Smiles or SDF cells into an adapter cell that contains RDKit cells.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @author Manuel Schwarze, Novartis
 */
public abstract class RDKitTypeConverter extends DataCellTypeConverter {

	//
	// Constants
	//

	/** Array with the value classes that can be handled by an RDKit Adapter. */
	@SuppressWarnings("unchecked")
	public static final Class<? extends DataValue>[] ADAPTABLE_VALUE_CLASSES = new Class[] {
		RDKitMolValue.class, SdfValue.class, SmilesValue.class};

	//
	// Members
	//

	/** The output type of the converter instance. */
	private final DataType m_outputType;

	//
	// Constructors
	//

	/**
	 * Creates a converter instance for the specified output type.
	 * 
	 * @param outputType The output type of the converter. Must not be null.
	 */
	private RDKitTypeConverter(final DataType outputType) {
		super(true); // True means that parallel processing of conversion is allowed
		m_outputType = outputType;
	}

	//
	// Public Methods
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataType getOutputType() {
		return m_outputType;
	}

	//
	// Public Static Methods
	//

	/**
	 * Creates a new converter for a specific column in a table. The output type and the specific converter that is
	 * used is determined automatically from the input type.
	 *
	 * @param tableSpec the input table's spec
	 * @param columnIndex the index of the column that should be converted.
	 * 
	 * @return A new converter or null, if no converter available.
	 */
	public static RDKitTypeConverter createConverter(final DataTableSpec tableSpec, final int columnIndex) {
		final DataType type = tableSpec.getColumnSpec(columnIndex).getType();
		return createConverter(type);
	}

	/**
	 * Creates a new converter for a specific source type.
	 *
	 * @param type Source type that needs to be converted. Must not be null.
	 * 
	 * @return A new converter or null, if no converter available.
	 */
	@SuppressWarnings("unchecked")
	public static RDKitTypeConverter createConverter(final DataType type) {
		// Process an existing adapter cell - we just want to add here an RDKit cell
		if (type.isCompatible(AdapterValue.class)) {

			if (type.isCompatible(RDKitMolValue.class)) {

				// We have already an Adapter cell that is compatible with RDKit Mol Value - we return it
				return new RDKitTypeConverter(type) {

					/**
					 * {@inheritDoc}
					 * Just returns the existing RDKit Mol Value within a new RDKit Adapter Cell.
					 */
					@Override
					public DataCell convert(final DataCell source) throws Exception {
						return source;
					}
				};
			}


			if (type.isCompatible(RWAdapterValue.class) && type.isCompatible(StringValue.class)
					&& type.isCompatible(SmilesValue.class) && type.isCompatible(SdfValue.class)) {

				// We have a writable adapter cell that already represents all value interfaces of RDKit (except RDKit)
				// thus we can just add the RDKitCell
				return new RDKitTypeConverter(type.createNewWithAdapter(RDKitMolValue.class)) {

					/**
					 * {@inheritDoc}
					 * Based on the existing SDF value we create an ROMol object and a SMILES value
					 * and from these two objects an RDKit Cell.
					 */
					@Override
					public DataCell convert(final DataCell source) throws Exception {
						if (source == null || source.isMissing()) {
							return DataType.getMissingCell();
						}

						final String strSdf = ((RWAdapterValue)source).getAdapter(SdfValue.class).getSdfValue();
						return ((RWAdapterValue)source).cloneAndAddAdapter(
								createRDKitMolCellFromSdf(strSdf), RDKitMolValue.class);
					}
				};
			}

			else if (type.isAdaptable(SdfValue.class)) {

				// We have an adapter cell that contains an SDF value - we create a new RDKit Adapter Cell with
				// the existing SDF cell and the new RDKit Cell
				return new RDKitTypeConverter(DataType.getType(RDKitAdapterCell.class, null, type.getValueClasses())) {

					/**
					 * {@inheritDoc}
					 * Based on the existing SDF value we create an ROMol object and a SMILES value
					 * and from these two objects an RDKit Cell.
					 */
					@Override
					public DataCell convert(final DataCell source) throws Exception {
						if (source == null || source.isMissing()) {
							return DataType.getMissingCell();
						}

						final String strSdf = ((AdapterValue)source).getAdapter(SdfValue.class).getSdfValue();
						return new RDKitAdapterCell(createRDKitMolCellFromSdf(strSdf), (AdapterValue)source);
					}
				};
			}

			// We have an adapter cell that contains a SMILES value - we create a new RDKit Adapter Cell with
			// the existing SMILES cell and the new RDKit Cell
			else if (type.isAdaptable(SmilesValue.class)) {
				return new RDKitTypeConverter(DataType.getType(RDKitAdapterCell.class, null, type.getValueClasses())) {

					/**
					 * {@inheritDoc}
					 * Based on the existing SMILES value we create an ROMol object and an RDKit Cell.
					 */
					@Override
					public DataCell convert(final DataCell source) throws Exception {
						if (source == null || source.isMissing()) {
							return DataType.getMissingCell();
						}

						final String strSmiles = ((AdapterValue)source).getAdapter(SmilesValue.class).getSmilesValue();
						return new RDKitAdapterCell(createRDKitMolCellFromSmiles(strSmiles), (AdapterValue)source);
					}
				};
			}
		}

		// Process a normal cell (no adapter cell) and create a new RDKit Adapter Cell
		else {

			if (type.isCompatible(RDKitMolValue.class)) {

				// We have already an RDKit Mol Value - we just create from it an RDKit Adapter Cell
				return new RDKitTypeConverter(RDKitAdapterCell.RAW_TYPE) {

					/**
					 * {@inheritDoc}
					 * Just returns the existing RDKit Mol Value within a new RDKit Adapter Cell.
					 */
					@Override
					public DataCell convert(final DataCell source) throws Exception {
						if (source == null || source.isMissing()) {
							return DataType.getMissingCell();
						}

						return new RDKitAdapterCell(source);
					}
				};
			}

			else if (type.isCompatible(SdfValue.class)) {

				// We have an SDF value - we create a new RDKit Adapter Cell with
				// the new RDKit Cell (which is also compatible with SDF value)
				return new RDKitTypeConverter(RDKitAdapterCell.RAW_TYPE) {

					/**
					 * {@inheritDoc}
					 * Based on the existing SDF value we create an ROMol object and a SMILES value
					 * and from these two objects an RDKit Cell.
					 */
					@Override
					public DataCell convert(final DataCell source) throws Exception {
						if (source == null || source.isMissing()) {
							return DataType.getMissingCell();
						}

						final String strSdf = ((SdfValue)source).getSdfValue();
						return new RDKitAdapterCell(createRDKitMolCellFromSdf(strSdf),
								(AdapterValue)SdfCellFactory.createAdapterCell(strSdf));
					}
				};
			}

			else if (type.isCompatible(SmilesValue.class)) {

				// We have a SMILES value - we create a new RDKit Adapter Cell with
				// the new RDKit Cell (which is also compatible with SMILES value)
				return new RDKitTypeConverter(RDKitAdapterCell.RAW_TYPE) {

					/**
					 * {@inheritDoc}
					 * Based on the existing SMILES value we create an ROMol object and an RDKit Cell.
					 */
					@Override
					public DataCell convert(final DataCell source) throws Exception {
						if (source == null || source.isMissing()) {
							return DataType.getMissingCell();
						}

						final String strSmiles = ((SmilesValue)source).getSmilesValue();
						return new RDKitAdapterCell(createRDKitMolCellFromSmiles(strSmiles),
								new SmilesAdapterCell(new SmilesCell(strSmiles)));
					}
				};
			}
		}

		return null;
	}

	//
	// Protected Methods
	//

	/**
	 * Creates an RDKit cell from the passed in SDF string.
	 * Based on the existing SDF value we create an ROMol object and a SMILES value
	 * and from these two objects an RDKit Cell.
	 * 
	 * @param strSdf SDF string. Can be null.
	 * 
	 * @return RDKit cell.
	 * 
	 * @throws RDKitTypeConverterException Thrown, if SDF could not be converted successfully.
	 */
	protected DataCell createRDKitMolCellFromSdf(final String strSdf) throws RDKitTypeConverterException {
		DataCell cell = DataType.getMissingCell();

		if (strSdf != null && !strSdf.trim().isEmpty()) {
			ROMol mol = null;

			try {
				Exception excCaught = null;

				// As first step try to parse the input molecule format
				try {
					mol = RWMol.MolFromMolBlock(strSdf, true);
				}
				catch (final Exception exc) {
					// Parsing failed and RDKit molecule is null
					excCaught = exc;
				}

				// If we got an RDKit molecule, parsing was successful, now create the SMILES from it and the cell
				if (mol != null) {
					try {
						// RDKit cell will not have a canonical SMILES set
						String smiles = "";
						if (mol.getNumAtoms() > 0) {
							smiles = RDKFuncs.MolToSmiles(mol, false, false, 0, false);
						}
						cell = RDKitMolCellFactory.createRDKitMolCell(mol, smiles);
					}
					catch (final Exception exc) {
						excCaught = exc;
					}
				}

				// Do error handling depending on user settings
				if (mol == null || excCaught != null) {
					// Find error message
					final StringBuilder sbError = new StringBuilder("SDF");

					// Specify error type
					if (mol == null) {
						sbError.append(" Parsing Error (");
					}
					else {
						sbError.append(" Process Error (");
					}

					// Specify exception
					if (excCaught != null) {
						sbError.append(excCaught.getClass().getSimpleName());

						// Specify error message
						final String strMessage = excCaught.getMessage();
						if (strMessage != null) {
							sbError.append(" (").append(strMessage).append(")");
						}
					}
					else {
						sbError.append("Details unknown");
					}

					sbError.append(") for\n" + strSdf);

					// Throw an exception - this will lead to a missing cell with the error message
					throw new RDKitTypeConverterException(sbError.toString(), excCaught);
				}
			}
			finally {
				if (mol != null) {
					mol.delete();
				}
			}
		}

		return cell;
	}

	/**
	 * Creates an RDKit cell from the passed in SMILES string.
	 * 
	 * @param strSmiles SMILES string. Can be null.
	 * 
	 * @return RDKit cell.
	 * 
	 * @throws RDKitTypeConverterException Thrown, if SMILES could not be converted successfully.
	 */
	protected DataCell createRDKitMolCellFromSmiles(final String strSmiles) throws RDKitTypeConverterException {
		DataCell cell = DataType.getMissingCell();

		if (strSmiles != null && !strSmiles.trim().isEmpty()) {
			ROMol mol = null;

			try {
				Exception excCaught = null;

				// As first step try to parse the input molecule format
				try {
					mol = RWMol.MolFromSmiles(strSmiles, 0, true);
				}
				catch (final Exception exc) {
					// Parsing failed and RDKit molecule is null
					excCaught = exc;
				}

				// If we got an RDKit molecule, parsing was successful, now create the cell
				if (mol != null) {
					try {
						cell = RDKitMolCellFactory.createRDKitMolCell(mol, strSmiles);
					}
					catch (final Exception exc) {
						excCaught = exc;
					}
				}

				// Do error handling depending on user settings
				if (mol == null || excCaught != null) {
					// Find error message
					final StringBuilder sbError = new StringBuilder("SMILES");

					// Specify error type
					if (mol == null) {
						sbError.append(" Parsing Error (");
					}
					else {
						sbError.append(" Process Error (");
					}

					// Specify exception
					if (excCaught != null) {
						sbError.append(excCaught.getClass().getSimpleName());

						// Specify error message
						final String strMessage = excCaught.getMessage();
						if (strMessage != null) {
							sbError.append(" (").append(strMessage).append(")");
						}
					}
					else {
						sbError.append("Details unknown");
					}

					sbError.append(") for\n" + strSmiles);

					// Throw an exception - this will lead to a missing cell with the error message
					throw new RDKitTypeConverterException(sbError.toString(), excCaught);
				}
			}
			finally {
				if (mol != null) {
					mol.delete();
				}
			}
		}

		return cell;
	}
}
