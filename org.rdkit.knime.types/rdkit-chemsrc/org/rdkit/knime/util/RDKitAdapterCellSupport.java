/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2012
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
package org.rdkit.knime.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCellTypeConverter;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.types.RDKitTypeConversionErrorProvider;
import org.rdkit.knime.types.RDKitTypeConverter;

/**
 * This class defines for the RDKit, which data types are compatible with each
 * other and what type converters exist. It is used to provide automatically
 * type conversions for RDKit Nodes whenever necessary.
 * 
 * @author Manuel Schwarze
 */
public final class RDKitAdapterCellSupport {

	//
	// Inner classes
	//

	/**
	 * Interface to be implemented when converters register for adapter cell support.
	 * 
	 * @author Manuel Schwarze
	 */
	public static interface DataCellTypeConverterFactory {

		/**
		 * Creates a data cell type converter for the column at the specified index in the
		 * specified table spec.
		 * 
		 * @param type Data type to be converted.
		 * 
		 * @return Data cell type converter or null, if data type cannot be converted.
		 */
		DataCellTypeConverter createConverter(final DataType type);
	}

	//
	// Constants
	//
	
	/** The logging instance. */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(RDKitAdapterCellSupport.class);

	private static final Map<Class<? extends DataValue>, Class<? extends DataValue>[]>
	REGISTERED_ADAPTABLE_VALUE_CLASSES = new HashMap<Class<? extends DataValue>, Class<? extends DataValue>[]>();

	private static final Map<Class<? extends DataValue>, DataCellTypeConverterFactory>
	REGISTERED_CONVERTER_FACTORY = new HashMap<Class<? extends DataValue>, DataCellTypeConverterFactory>();

	//
	// Static Code
	//

	/**
	 * Registers all RDKit internally used type converters.
	 */
	static {
		register(RDKitMolValue.class, RDKitTypeConverter.ADAPTABLE_VALUE_CLASSES, new DataCellTypeConverterFactory() {
			@Override
			public DataCellTypeConverter createConverter(final DataType dataType) {
				return RDKitTypeConverter.createConverter(dataType);
			}
		});
	}

	//
	// Public Statis Methods
	//

	public static void register(final Class<? extends DataValue> valueClass, final Class<? extends DataValue>[]
			arrAdaptableClasses, final DataCellTypeConverterFactory converterFactory) {
		REGISTERED_ADAPTABLE_VALUE_CLASSES.put(valueClass, arrAdaptableClasses);
		REGISTERED_CONVERTER_FACTORY.put(valueClass, converterFactory);
	}

	/**
	 * Returns the registered converter factory of the specified target value class.
	 * 
	 * @param targetValueClass Target value class.
	 * 
	 * @return Converter factory or null, if not found.
	 */
	public static final DataCellTypeConverterFactory getConverterFactory(final Class<? extends DataValue> targetValueClass) {
		return REGISTERED_CONVERTER_FACTORY.get(targetValueClass);
	}

	/**
	 * Returns an array of adaptable value classes. An adaptable value class can be converted into
	 * the specified target value class.
	 * 
	 * @param targetValueClass Target value class.
	 * 
	 * @return Array of adaptable value classes for the specified target value class or null, if not found.
	 */
	public static final Class<? extends DataValue>[] getAdaptableValueClasses(final Class<? extends DataValue> targetValueClass) {
		return REGISTERED_ADAPTABLE_VALUE_CLASSES.get(targetValueClass);
	}

	/**
	 * Checks if values of the specific column in a table can be converted into the specified
	 * target value class.
	 *
	 * @param tableSpec The input table's spec. Can be null.
	 * @param columnIndex the index of the column that should be checked.
	 * 
	 * @return True, if data type of the column can be converted into target value class and if the
	 * 		value target class is not already the preferred data value class of the data type.
	 * 		False otherwise.
	 * 
	 * @see RDKitAdapterCellSupport#canBeConverted(DataType, Class)
	 */
	public static boolean canBeConverted(final DataTableSpec tableSpec, final int columnIndex,
			final Class<? extends DataValue> targetValueClass) {
		boolean bConvertable = false;

		if (targetValueClass != null && tableSpec != null && columnIndex >= 0 && columnIndex < tableSpec.getNumColumns()) {
			final DataType type = tableSpec.getColumnSpec(columnIndex).getType();
			bConvertable = canBeConverted(type, targetValueClass);
		}

		return bConvertable;
	}

	/**
	 * Checks if values of the specified data type can be converted into the specified value class.
	 * 
	 * @param dataTypeSource Source data type to check conversion for. Can be null.
	 * @param targetValueClass Target data value class. Can be null.
	 * 
	 * @return True, if data type can be converted into target value class and if the
	 * 		value target class is not already the preferred data value class of the data type.
	 * 		False otherwise.
	 */
	public static boolean canBeConverted(final DataType dataTypeSource,
			final Class<? extends DataValue> targetValueClass) {
		boolean bConvertable = false;

		if (dataTypeSource != null && targetValueClass != null) {
			final Class<? extends DataValue>[] arrValueClass = REGISTERED_ADAPTABLE_VALUE_CLASSES.get(targetValueClass);

			for (final Class<? extends DataValue> valueClass : arrValueClass) {
				if (dataTypeSource.isCompatible(valueClass) &&
						dataTypeSource.getPreferredValueClass() != targetValueClass) {
					bConvertable = true;
					break;
				}
			}
		}

		return bConvertable;
	}

   /**
    * Creates a new converter for a specific column in a table. The output type and the specific converter that is
    * used is determined automatically from the input type.
    *
    * @param inputDataInfo Input data info that describes the column that is subject of conversion.
    * 
    * @return A new converter or null, if no converter available.
    * 
    * @see RDKitAdapterCellSupport#createConverter(DataType, Class)
    */
   public static DataCellTypeConverter createConverter(InputDataInfo inputDataInfo,
         final Class<? extends DataValue> targetValueClass) {
      return createConverter(inputDataInfo, inputDataInfo.getTableSpec(), inputDataInfo.getColumnIndex(), targetValueClass);
   }

   /**
    * Creates a new converter for a specific column in a table. The output type and the specific converter that is
    * used is determined automatically from the input type.
    *
    * @param tableSpec the input table's spec
    * @param columnIndex the index of the column that should be converted.
    * 
    * @return A new converter or null, if no converter available.
    * 
    * @see RDKitAdapterCellSupport#createConverter(DataType, Class)
    */
   public static DataCellTypeConverter createConverter(final DataTableSpec tableSpec, final int columnIndex,
         final Class<? extends DataValue> targetValueClass) {
      DataCellTypeConverter converter = null;
      InputDataInfo inputDataInfo = null;
      
      try {
         inputDataInfo = new InputDataInfo(tableSpec, new SettingsModelString("columnName", 
                  tableSpec.getColumnSpec(columnIndex).getName()));
      }
      catch (InvalidSettingsException exc) {
         // Should not happen - this would be an implementation error
         LOGGER.warn("Unable to create input data info object for "
               + "table specification and column index " + columnIndex);
      }
      
      converter = createConverter(inputDataInfo, tableSpec, columnIndex, targetValueClass);
      
      return converter;
   }
   
	/**
	 * Creates a new converter for a specific column in a table. The output type and the specific converter that is
	 * used is determined automatically from the input type.
	 *
    * @param inputDataInfo Input data info that describes the column that is subject of conversion. Can be null.
	 * @param tableSpec the input table's spec
	 * @param columnIndex the index of the column that should be converted.
	 * 
	 * @return A new converter or null, if no converter available.
	 * 
	 * @see RDKitAdapterCellSupport#createConverter(DataType, Class)
	 */
	private static DataCellTypeConverter createConverter(InputDataInfo inputDataInfo, 
	      final DataTableSpec tableSpec, final int columnIndex,
			final Class<? extends DataValue> targetValueClass) {
		DataCellTypeConverter converter = null;

		if (targetValueClass != null && tableSpec != null && 
		      columnIndex >= 0 && columnIndex < tableSpec.getNumColumns()) {
			final DataType type = tableSpec.getColumnSpec(columnIndex).getType();
			converter = createConverter(type, targetValueClass);
			if (converter instanceof RDKitTypeConversionErrorProvider) {
			   ((RDKitTypeConversionErrorProvider) converter).setInputDataInfo(inputDataInfo);
			}
		}

		return converter;
	}

	/**
	 * Creates a new converter for a specific source type and the specified target value class.
	 *
	 * @param type Source type that needs to be converted. Must not be null.
	 * @param targetValueClass Target value class that the converter shall deliver as part of an adapter cell.
	 * 
	 * @return A new converter or null, if no converter available.
	 */
	public static DataCellTypeConverter createConverter(final DataType type,
			final Class<? extends DataValue> targetValueClass) {
		DataCellTypeConverter converter = null;

		if (type != null && targetValueClass != null) {
			final DataCellTypeConverterFactory factory = REGISTERED_CONVERTER_FACTORY.get(targetValueClass);
			if (factory != null) {
				converter = factory.createConverter(type);
			}
		}

		return converter;
	}

	//
	// Public Static Methods
	//

	/**
	 * Returns an array of acceptable value classes with the same class as passed in,
	 * plus additionally all classes that are adaptable to it.
	 * 
	 * @param valueClass Original value class. Can be null to return null.
	 * 
	 * @return New extended array or null, if null was passed in.
	 */
	@SuppressWarnings("unchecked")
	public static Class<? extends DataValue>[] expandByAdaptableTypes(final Class<? extends DataValue> valueClass) {
		return (valueClass == null ? null : expandByAdaptableTypes((Class<? extends DataValue>[])new Class<?>[] { valueClass }));
	}

	/**
	 * Returns an array of acceptable value classes with the same classes as passed in,
	 * plus additionally all classes that are adaptable to it.
	 * 
	 * @param arrValueClasses Original value class array. Can be null to return null.
	 * 
	 * @return New extended array or null, if null was passed in.
	 */
	@SuppressWarnings("unchecked")
	public static Class<? extends DataValue>[] expandByAdaptableTypes(final Class<? extends DataValue>[] arrValueClasses) {
		Class<? extends DataValue>[] arrRet = arrValueClasses;

		if (arrValueClasses != null) {
			final LinkedHashSet<Class<? extends DataValue>> set = new LinkedHashSet<Class<? extends DataValue>>();
			for (final Class<? extends DataValue> valueClassRegistered : REGISTERED_ADAPTABLE_VALUE_CLASSES.keySet()) {
				for (final Class<? extends DataValue> valueClass : arrValueClasses) {
					set.add(valueClass);
					if (valueClass == valueClassRegistered) {
						final Class<? extends DataValue>[] arrAdaptableValueClass =
								REGISTERED_ADAPTABLE_VALUE_CLASSES.get(valueClassRegistered);
						if (arrAdaptableValueClass != null) {
							for (final Class<? extends DataValue> adaptableValueClass : RDKitTypeConverter.ADAPTABLE_VALUE_CLASSES) {
								set.add(adaptableValueClass);
							}
						}
					}
				}
			}
			arrRet = set.toArray(new Class[set.size()]);
		}

		return arrRet;
	}

	/**
	 * Returns a list of acceptable value classes with the same classes as passed in,
	 * plus additionally all classes that are adaptable to it
	 * 
	 * @param listValueClasses Original value class array. Can be null to return null.
	 * 
	 * @return New extended array or null, if null was passed in.
	 */
	@SuppressWarnings("unchecked")
	public static List<Class<? extends DataValue>> expandByAdaptableTypes(final List<Class<? extends DataValue>> listValueClasses) {
		return listValueClasses == null ? null : Arrays.asList(expandByAdaptableTypes(
				(Class<? extends DataValue>[])listValueClasses.toArray(new Class<?>[listValueClasses.size()])));
	}

	/**
	 * Returns a column filter which accepts the same types as the passed in filter,
	 * plus additionally it accepts types that are adaptable to the specified target types.
	 * 
	 * @param columnFilter Original column filter. Can be null to return null.
	 * 
	 * @return New extended column filter or null, if null was passed in.
	 */
	public static ColumnFilter expandByAdaptableTypes(final ColumnFilter columnFilter, final DataType... arrTargetType) {
		ColumnFilter chainedColumnFilter = columnFilter;

		if (columnFilter != null && arrTargetType != null) {

			// Walk through all acceptable types
			for (final DataType targetType : arrTargetType) {

				// Check, if the column filter accepts a target type
				if (chainedColumnFilter.includeColumn(
						new DataColumnSpecCreator("Temp", targetType).createSpec())) {

					// Check, if adaptable values are available
					final Class<? extends DataValue>[] arrAdaptableValueClass =
							getAdaptableValueClasses(targetType.getPreferredValueClass());

					// Make the current column filter available as wrapped filter inside the new one
					final ColumnFilter parentColumnFilter = chainedColumnFilter;

					// Create a wrapped filter that also accepts adaptable cell types
					chainedColumnFilter = new ColumnFilter() {

						@Override
						public boolean includeColumn(final DataColumnSpec colSpec) {
							boolean bAccepted = parentColumnFilter.includeColumn(colSpec);

							if (!bAccepted) {
								final DataType type = colSpec.getType();
								for (final Class<? extends DataValue> adaptableValueClass : arrAdaptableValueClass) {
									if (type.isCompatible(adaptableValueClass)) {
										bAccepted = true;
										break;
									}
								}
							}

							return bAccepted;
						}

						@Override
						public String allFilteredMsg() {
							return columnFilter.allFilteredMsg();
						}
					};
				}
			}
		}

		return chainedColumnFilter;
	}


	//
	// Constructor
	//

	/**
	 * This constructor serves only the purpose to avoid instantiation of this class.
	 */
	private RDKitAdapterCellSupport() {
		// To avoid instantiation of this class.
	}
}
