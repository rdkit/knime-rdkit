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

import java.util.ArrayList;
import java.util.List;

import org.RDKit.ROMol;
import org.RDKit.RWMol;
import org.knime.chem.types.SdfAdapterCell;
import org.knime.chem.types.SdfCellFactory;
import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmilesAdapterCell;
import org.knime.chem.types.SmilesCellFactory;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellTypeConverter;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.RWAdapterValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.NodeLogger;
import org.rdkit.knime.types.preferences.RDKitTypesPreferencePage;
import org.rdkit.knime.util.InputDataInfo;

/**
 * Converter for RDKit that converts Smiles or SDF cells into an adapter cell that contains RDKit cells.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @author Manuel Schwarze, Novartis
 */
public abstract class RDKitTypeConverter extends DataCellTypeConverter implements RDKitTypeConversionErrorProvider {

	//
	// Constants
	//
   
   /** The logging instance. */
   private static final NodeLogger LOGGER = NodeLogger.getLogger(RDKitTypeConverter.class);

	/** Array with the value classes that can be handled by an RDKit Adapter. */
	@SuppressWarnings("unchecked")
	public static final Class<? extends DataValue>[] ADAPTABLE_VALUE_CLASSES = new Class[] {
		RDKitMolValue.class, SdfValue.class, SmilesValue.class};

	//
	// Members
	//
	
	/** Information about the column that is subject of conversion. */
	private InputDataInfo m_inputDataInfo;

	/** The output type of the converter instance. */
	private final DataType m_outputType;
   
	/** The list of registered listeners. */
   private final List<RDKitTypeConversionErrorListener> m_listListeners;

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
		m_inputDataInfo = null;
		m_outputType = outputType;
		m_listListeners = new ArrayList<>();
	}

	//
	// Public Methods
	//
	
	/**
	 * Attaches information about the input column that is subject of conversion.
	 * 
	 * @param inputDataInfo Input data info. Can be null.
	 */
	public void setInputDataInfo(InputDataInfo inputDataInfo) {
	   m_inputDataInfo = inputDataInfo;
	}
	
	/**
	 * Returns information about the column that is subject of conversion. 
	 * 
	 * @return Input data info. Can be null.
	 */
	public InputDataInfo getInputDataInfo() {
	   return m_inputDataInfo;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataType getOutputType() {
		return m_outputType;
	}
	
	@Override
	public void onConvertException(DataCell source, Exception e) {
	   super.onConvertException(source, e);
	   onFireConversionErrorEvent(source, e);
	}
	
	@Override
	public void addTypeConversionErrorListener(RDKitTypeConversionErrorListener l) {
      if (l != null) {
         synchronized (m_listListeners) {
            if (!m_listListeners.contains(l)) {
               m_listListeners.add(l);
            }
         }
      }
	}
	
	@Override
	public void removeTypeConversionErrorListener(RDKitTypeConversionErrorListener l) {
      if (l != null) {
         synchronized (m_listListeners) {
            m_listListeners.remove(l);
         }
      }
	}
	
	//
	// Protected Methods
	//
	
	/**
    * Notifies all registered listeners that a conversion error occurred.
    * 
    * @param source Data cell that failed conversion.
    * @param exc Exception that occurred.
    */
   protected void onFireConversionErrorEvent(final DataCell source, final Exception exc) {
      // Get copy of list
      List<RDKitTypeConversionErrorListener> listCopy = null;

      // Make a copy of the listener list
      synchronized (m_listListeners) {
         listCopy = new ArrayList<RDKitTypeConversionErrorListener>(m_listListeners.size());
         for (final RDKitTypeConversionErrorListener l : m_listListeners) {
            listCopy.add(l);
         }
      }

      final InputDataInfo inputDataInfo = getInputDataInfo(); // Can be null
      
      // Notify listeners
      for (final RDKitTypeConversionErrorListener l : listCopy) {
         try {
            l.onTypeConversionError(inputDataInfo, source, exc);
         }
         catch (final Exception notificationFailed) {
            LOGGER.debug("Listener notification on conversion error failed.", notificationFailed);
         }
      }
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
		RDKitTypeConverter converter = createConverter(type);
		return converter;
	}

	/**
	 * Creates a new converter for a specific source type.
	 *
	 * @param type Source type that needs to be converted. Must not be null.
	 * 
	 * @return A new converter or null, if no converter available.
	 */
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

			if (type.isCompatible(RWAdapterValue.class) &&  type.isCompatible(SdfValue.class)) {

				// We have a writable adapter cell that contains an SDF value
				// thus we can just add the RDKitCell
			   final Class<? extends DataValue>[] arrValueClasses = determineValueClassesToSupport(type);
				return new RDKitTypeConverter(type.createNewWithAdapter(arrValueClasses)) {

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
								createRDKitAdapterCellFromSdf(strSdf), arrValueClasses);
					}
				};
			}
			
			else if (type.isCompatible(RWAdapterValue.class) && type.isCompatible(SmilesValue.class)) {

            // We have a writable adapter cell that contains a SMILES value
            // thus we can just add the RDKitCell
			   final Class<? extends DataValue>[] arrValueClasses = determineValueClassesToSupport(type);
            return new RDKitTypeConverter(type.createNewWithAdapter(arrValueClasses)) {

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

                  final String strSmiles = ((RWAdapterValue)source).getAdapter(SmilesValue.class).getSmilesValue();
                  return ((RWAdapterValue)source).cloneAndAddAdapter(
                        createRDKitAdapterCellFromSmiles(strSmiles), arrValueClasses);
               }
            };
         }

			else if (type.isAdaptable(SdfValue.class) /* but not based on a RWAdapterValue */ ) {

				// We have a read only adapter cell that contains an SDF value - we create a new SDF Adapter Cell 
				// and add the new RDKit Cell which gets created based on the SDF value
	         // Note: This case should not really happen as AdapterCells are today implementing RWAdapterValue,
	         //       which is handled above
			   final Class<? extends DataValue>[] arrValueClasses = determineValueClassesToSupport(SdfAdapterCell.RAW_TYPE);
			   return new RDKitTypeConverter(SdfAdapterCell.RAW_TYPE.createNewWithAdapter(arrValueClasses)) {

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
						return ((RWAdapterValue)SdfCellFactory.createAdapterCell(strSdf)).
						      cloneAndAddAdapter(createRDKitAdapterCellFromSdf(strSdf), arrValueClasses);
					}
				};
			}

			// We have a read only adapter cell that contains a SMILES value - we create a new SMILES Adapter Cell 
			// and add the new RDKit Cell which gets created based on the SMILES value
			// Note: This case should not really happen as AdapterCells are today implementing RWAdapterValue,
			//       which is handled above
			else if (type.isAdaptable(SmilesValue.class) /* but not based on a RWAdapterValue */ ) {
			   final Class<? extends DataValue>[] arrValueClasses = determineValueClassesToSupport(SmilesAdapterCell.RAW_TYPE);
			   return new RDKitTypeConverter(SmilesAdapterCell.RAW_TYPE.createNewWithAdapter(arrValueClasses)) {

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
						return ((RWAdapterValue)SmilesCellFactory.createAdapterCell(strSmiles)).
						      cloneAndAddAdapter(createRDKitAdapterCellFromSmiles(strSmiles), arrValueClasses);
					}
				};
			}
		}

		// Process a normal cell (no adapter cell) and create a new Adapter Cell based on the input
      // Note: These cases should not really happen anymore because since KNIME 3.0 molecule output should be done in 
		//       AdapterCells always, e.g. as SdfAdapterCells or SmilesAdapterCells, which would be handled above
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

				// We have an SDF value - we create a new SDF Adapter Cell with
				// the new RDKit Cell attached
			   final Class<? extends DataValue>[] arrValueClasses = determineValueClassesToSupport(SdfAdapterCell.RAW_TYPE);
			   return new RDKitTypeConverter(SdfAdapterCell.RAW_TYPE.createNewWithAdapter(arrValueClasses)) {

					/**
					 * {@inheritDoc}
					 * Based on the existing SDF value we create an ROMol object and a SMILES value
					 * and from these two objects an RDKit Cell which we attach to a new SdfAdapterCell.
					 */
					@Override
					public DataCell convert(final DataCell source) throws Exception {
						if (source == null || source.isMissing()) {
							return DataType.getMissingCell();
						}

						final String strSdf = ((SdfValue)source).getSdfValue();
                  return ((RWAdapterValue)SdfCellFactory.createAdapterCell(strSdf)).
                        cloneAndAddAdapter(createRDKitAdapterCellFromSdf(strSdf), arrValueClasses);
					}
				};
			}

			else if (type.isCompatible(SmilesValue.class)) {

				// We have a SMILES value - we create a new Smiles Adapter Cell with
				// the new RDKit Cell attached 
			   final Class<? extends DataValue>[] arrValueClasses = determineValueClassesToSupport(SmilesAdapterCell.RAW_TYPE);
			   return new RDKitTypeConverter(SmilesAdapterCell.RAW_TYPE.createNewWithAdapter(arrValueClasses)) {

					/**
					 * {@inheritDoc}
					 * Based on the existing SMILES value we create an ROMol object and an RDKit Cell
					 * and attach it to a new SmilesAdapterCell.
					 */
					@Override
					public DataCell convert(final DataCell source) throws Exception {
						if (source == null || source.isMissing()) {
							return DataType.getMissingCell();
						}

						final String strSmiles = ((SmilesValue)source).getSmilesValue();
                  return ((RWAdapterValue)SmilesCellFactory.createAdapterCell(strSmiles)).
                        cloneAndAddAdapter(createRDKitAdapterCellFromSmiles(strSmiles), arrValueClasses);
					}
				};
			}
		}

		return null;
	}

	//
	// Public Static Methods
	//

	/**
	 * Creates an RDKit Adapter Cell from the passed in SDF string.
	 * Based on the existing SDF value we create an ROMol object and a SMILES value
	 * and from these two objects an RDKit Cell, which is put into an RDKit Adapter Cell.
	 * 
	 * @param strSdf SDF string. Can be null.
	 * 
	 * @return RDKit Adapter Cell.
	 * 
	 * @throws RDKitTypeConverterException Thrown, if SDF could not be converted successfully.
	 */
	public static DataCell createRDKitAdapterCellFromSdf(final String strSdf) throws RDKitTypeConverterException {
		DataCell cell = DataType.getMissingCell();

		if (strSdf != null && !strSdf.trim().isEmpty()) {
			ROMol mol = null;

			try {
				Exception excCaught = null;

				// As first step try to parse the input molecule format
				try {
					mol = RWMol.MolFromMolBlock(strSdf, true /* sanitize */, true /* removeHs */, 
							RDKitTypesPreferencePage.isStrictParsingForAutoConversion() /* strictParsing */);
				}
				catch (final Exception exc) {
					// Parsing failed and RDKit molecule is null
					excCaught = exc;
				}

				// If we got an RDKit molecule, parsing was successful, now create the RDKit Cell from it 
				// and inside it a canonicalized SMILES value
				if (mol != null) {
					try {
						cell = RDKitMolCellFactory.createRDKitAdapterCell(mol);
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
	 * Creates an RDKit Adapter Cell from the passed in SMILES string.
	 * 
	 * @param strSmiles SMILES string. Can be null.
	 * 
	 * @return RDKit Adapter cell.
	 * 
	 * @throws RDKitTypeConverterException Thrown, if SMILES could not be converted successfully.
	 */
	public static DataCell createRDKitAdapterCellFromSmiles(final String strSmiles) throws RDKitTypeConverterException {
		DataCell cell = DataType.getMissingCell();

		if (strSmiles != null) {
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

				// If we got an RDKit molecule, parsing was successful, now create the cell,
				// which will contain a canonicalized SMILES in the inner RDKit Cell.
				if (mol != null) {
					try {
						cell = RDKitMolCellFactory.createRDKitAdapterCell(mol);
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
   
   /**
    * Traverses the list of RDKit supported value classes and returns only the classes that are not compatible and 
    * not adaptable with the passed in data type. Using this method we can determine, which 
    * value classes should be provided by our RDKit Adapter Cell. Example: If an cell that supports
    * SDF value gets converted into an RDKit Adapter Cell we want to provide all possible data values
    * that were not already provided by the original SDF cell. Exception: The RDKitMolValue will always
    * be added as first value in the list as an RDKit Adapter Cell ALWAYS provides that data value.
    * 
    * @param type Data type to check for already compatible / adaptable data values. Can be null, in which
    *      case we would return the array that contains only RDKitMolValue.class.
    * @param valueClasses Value classes to check.
    * 
    * @return Array with value classes that are not compatible and not adaptable to the passed in type.
    *      As first argument RDKitMolValue.class is always added.
    */
   @SuppressWarnings("unchecked")
   protected static Class<? extends DataValue>[] determineValueClassesToSupport(DataType type) {
      return determineValueClassesToSupport(type, SdfValue.class, SmilesValue.class, StringValue.class);
   }
   
	/**
	 * Traverses the passed in list of value classes and returns only the classes that are not compatible and 
	 * not adaptable with the passed in data type. Using this method we can determine, which 
	 * value classes should be provided by our RDKit Adapter Cell. Example: If an cell that supports
	 * SDF value gets converted into an RDKit Adapter Cell we want to provide all possible data values
	 * that were not already provided by the original SDF cell. Exception: The RDKitMolValue will always
	 * be added as first value in the list as an RDKit Adapter Cell ALWAYS provides that data value.
	 * 
	 * @param type Data type to check for already compatible / adaptable data values. Can be null, in which
	 *      case we would return the array that contains only RDKitMolValue.class.
	 * @param valueClasses Value classes to check.
	 * 
	 * @return Array with value classes that are not compatible and not adaptable to the passed in type.
	 *      As first argument RDKitMolValue.class is always added.
	 */
	@SuppressWarnings("unchecked")
   protected static Class<? extends DataValue>[] determineValueClassesToSupport(DataType type, Class<? extends DataValue>... valueClasses) {
	   List<Class<? extends DataValue>> listValueClasses = new ArrayList<Class<? extends DataValue>>();
	   listValueClasses.add(RDKitMolValue.class);
	   if (valueClasses != null && valueClasses.length > 0) {
	      for (Class<? extends DataValue> valueClass : valueClasses) {
	         if (!listValueClasses.contains(valueClass) && !type.isCompatible(valueClass) && !type.isAdaptable(valueClass)) {
	            listValueClasses.add(valueClass);
	         }
	      }
	   }
	   
	   return (Class<? extends DataValue>[])listValueClasses.toArray(new Class[listValueClasses.size()]);
	}
}
