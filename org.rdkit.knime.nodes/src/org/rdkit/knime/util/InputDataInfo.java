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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.RDKit.ChemicalReaction;
import org.RDKit.ExplicitBitVect;
import org.RDKit.Int_Vect;
import org.RDKit.ROMol;
import org.RDKit.UInt_Vect;
import org.knime.chem.types.RxnValue;
import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmartsValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.AdapterValue;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellTypeConverter;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bytevector.DenseByteVector;
import org.knime.core.data.vector.bytevector.DenseByteVectorCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.types.RDKitMolValue;

/**
 * Makes information available for easy access of input data required for node execution.
 * It is also used to perform validity and compatibility checks. As the information
 * covered in this object is dynamic, this object should be short living and
 * created when it is needed, e.g. for execution.
 * 
 * @author Manuel Schwarze
 */
public class InputDataInfo {

	//
	// Constants
	//

	/**
	 * This enumeration defines policies for handling of empty cells when the value
	 * of a cell is requested from an InputDataInfo object.
	 */
	public enum EmptyCellPolicy {

		/**
		 * Policy when encountering an empty input cell: Treat the converted cell
		 * value as null or zero (for numbers) or false (for booleans).
		 */
		TreatAsNull,

		/**
		 * Policy when encountering an empty input cell: Use in this case
		 * the value of a configured default cell.
		 */
		UseDefault,

		/**
		 * Policy when encountering an empty input cell: Don't process
		 * the row further but deliver all result cells as empty cells.
		 * This policy causes an exception to be thrown, which should
		 * be caught to deliver the empty return cells. This is mainly meant
		 * for factories only. In case of filtering or splitting, such
		 * a row would be thrown away. Use Custom
		 * as policy instead, if custom handling is desired.
		 */
		DeliverEmptyRow,

		/**
		 * Policy when encountering an empty input cell: Let the entire
		 * execution of the node fail. This policy causes an exception
		 * to be thrown, which should abort the node execution.
		 */
		StopExecution,

		/**
		 * Policy when encountering an empty input cell: Return
		 * a missing cell and let the caller handle the issue.
		 */
		Custom

	}

	/** Logger instance. */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(InputDataInfo.class);

	//
	// Members
	//

	/** Specification of the table the column belongs to. */
	private final DataTableSpec m_tableSpec;

	/** Description for the table to be used in (e.g. error) messages. */
	private final String m_strTableDescription;

	/** Specification of the identified column. */
	private final DataColumnSpec m_colSpec;

	/** Description for the column to be used in (e.g. error) messages. */
	private final String m_strColumnDescription;

	/** Data type of the identified column. */
	private final DataType m_dataType;

	/** Defined a default cell, which can be used when a cell is empty. Can be null. */
	private final DataCell m_defaultCell;

	/** Index of the identified column. */
	private final int m_iColIndex;

	/** Defines the policy how to treat an empty input cell. */
	private final EmptyCellPolicy m_emptyCellPolicy;

	/** The model which holds the column name as a user setting. */
	private final SettingsModelString m_modelAsUniqueId;

	/** Name of the identified column. */
	private final String m_strColumnName;

	/** Flag to tell that the column is the row key column. */
	private final boolean m_bUseRowKey;

	/**
	 * Converter for columns that are not matching the preferred value classes, but
	 * can be converted using Adapter Cells.
	 */
	private DataCellTypeConverter m_converter;


	/** For performance reasons we store here results of compatibility tests. */
	private final Map<Class<? extends DataValue>, Boolean> m_mapCompatibilityCache =
			new HashMap<Class<? extends DataValue>, Boolean>();

   /** For performance reasons we store here results of compatibility tests. */
   private final Map<Class<? extends DataValue>, Boolean> m_mapCompatibilityAndAdaptibilityCache =
         new HashMap<Class<? extends DataValue>, Boolean>();

	//
	// Constructor
	//

	/**
	 * Creates a new input data info object based on a concrete table spec,
	 * a column name within a setting model object as well as a data value class
	 * that specifies the expected type of value. There are several checks performed
	 * to ensure that data are compatible with the processing that will happen later.
	 * The most important information that is delivered is the index of the
	 * column within the specified table. This must be easily accessible during
	 * processing.
	 * 
	 * @param inSpec Table specification. Can be null to throw an InvalidSettingsException.
	 * @param modelColumnName Column model, which act as container for the column name
	 * 		and afterwards as unique id to distinguish between different data info objects.
	 * 		Must not be null.
	 * @param emptyCellPolicy Defines the policy to be used when an empty cell
	 * 		is encountered during processing.
	 * @param defaultCell A default cell value, which can be used when an empty cell
	 * 		is encountered during processing. Can be null.
	 * @param arrDataValueClasses A list of acceptable data types. Optional. If specified,
	 * 		then one needs to be at least compatible with the column spec, otherwise
	 * 		an exception is thrown.
	 * 
	 * @throws InvalidSettingsException Thrown, if something is not set or not compatible
	 * 		with the data types that are expected.
	 */
	@SafeVarargs
	public InputDataInfo(final DataTableSpec inSpec, final SettingsModelString modelColumnName,
			final EmptyCellPolicy emptyCellPolicy, final DataCell defaultCell,
			final Class<? extends DataValue>... arrDataValueClasses) throws InvalidSettingsException {
		this(inSpec, null, modelColumnName, null, emptyCellPolicy, defaultCell, arrDataValueClasses);
	}

	/**
	 * Creates a new input data info object based on a concrete table spec,
	 * a column name within a setting model object as well as a data value class
	 * that specifies the expected type of value. There are several checks performed
	 * to ensure that data are compatible with the processing that will happen later.
	 * The most important information that is delivered is the index of the
	 * column within the specified table. This must be easily accessible during
	 * processing.
	 * 
	 * @param inSpec Table specification. Can be null to throw an InvalidSettingsException.
	 * @param strTableDescription Optional table description to be used in error message that
	 * 		concern the table. Can be null or empty to show "input table" in messages. Otherwise it
	 * 		will show "strTableDescription table" in messages.
	 * @param modelColumnName Column model, which act as container for the column name
	 * 		and afterwards as unique id to distinguish between different data info objects.
	 * 		Must not be null.
	 * @param strColumnDescription Optional column description to be used in error message that
	 * 		concern the column. Can be null or empty to show "input column" in messages. Otherwise it
	 * 		will show "strColumnDescription column" in messages.
	 * @param emptyCellPolicy Defines the policy to be used when an empty cell
	 * 		is encountered during processing.
	 * @param defaultCell A default cell value, which can be used when an empty cell
	 * 		is encountered during processing. Can be null.
	 * @param arrDataValueClasses A list of acceptable data types. Optional. If specified,
	 * 		then one needs to be at least compatible with the column spec, otherwise
	 * 		an exception is thrown.
	 * 
	 * @throws InvalidSettingsException Thrown, if something is not set or not compatible
	 * 		with the data types that are expected.
	 */
	@SafeVarargs
	public InputDataInfo(final DataTableSpec inSpec, final String strTableDescription,
			final SettingsModelString modelColumnName, final String strColumnDescription,
			final EmptyCellPolicy emptyCellPolicy, final DataCell defaultCell,
			final Class<? extends DataValue>... arrDataValueClasses) throws InvalidSettingsException {
		m_strColumnDescription = strColumnDescription;
		m_strTableDescription = strTableDescription;

		// Pre-checks
		if (inSpec == null) {
			throw new InvalidSettingsException("There is no " + getTableIdentification() + " available yet.");
		}
		if (modelColumnName == null) {
			throw new IllegalArgumentException("The " + getColumnIdentification() + " name model must not be null.");
		}

		// Store parameters
		m_modelAsUniqueId = modelColumnName;
		m_bUseRowKey = (modelColumnName instanceof SettingsModelColumnName && ((SettingsModelColumnName)modelColumnName).useRowID());
		m_tableSpec = inSpec;

		// Check, if the input column has been configured
		m_strColumnName = modelColumnName.getStringValue();
		if (m_strColumnName == null && !m_bUseRowKey) {
			throw new InvalidSettingsException("There is no " + getColumnIdentification() + " configured yet.");
		}

		// Try to find the column in the table
		m_iColIndex = (m_bUseRowKey ? -1 : m_tableSpec.findColumnIndex(m_strColumnName));
		m_colSpec = (m_bUseRowKey ? null : m_tableSpec.getColumnSpec(m_strColumnName));
		if (!m_bUseRowKey && (m_iColIndex == -1 || m_colSpec == null)) {
			throw new InvalidSettingsException("No such " + getColumnIdentification() + " in input table '" +
					m_tableSpec.getName() + "': " + m_strColumnName);
		}

		// Perform compatibility check
		if (m_bUseRowKey) {
			m_dataType = StringCell.TYPE;
		}
		else {
			m_dataType = m_colSpec.getType(); // The existing type of the input column
			final List<Class<? extends DataValue>> listPreferredValueClasses = (arrDataValueClasses == null ?
					new ArrayList<Class<? extends DataValue>>(1) :
						Arrays.asList(arrDataValueClasses));
			final List<Class<? extends DataValue>> m_listCompatibleValueClasses =
					RDKitAdapterCellSupport.expandByAdaptableTypes(listPreferredValueClasses);

			Class<? extends DataValue> compatiblePreferredValueClass = null;
			if (!listPreferredValueClasses.isEmpty()) {
				// Look first for preferred value classes
				for (final Class<? extends DataValue> valueClass : listPreferredValueClasses) {
					if (m_dataType.isCompatible(valueClass) || m_dataType.isAdaptable(valueClass)) {
						compatiblePreferredValueClass = valueClass;
						break;
					}
				}

				// If not found look if conversion is possible
				if (compatiblePreferredValueClass == null) {
					for (final Class<? extends DataValue> valueClass : listPreferredValueClasses) {
						for (final Class<? extends DataValue> valueClassCompatible :
							RDKitAdapterCellSupport.expandByAdaptableTypes(valueClass)) {
							if (m_dataType.isCompatible(valueClassCompatible)) {
								compatiblePreferredValueClass = valueClass;
								m_converter = RDKitAdapterCellSupport.createConverter(
										getTableSpec(), getColumnIndex(), compatiblePreferredValueClass);
								break;
							}
						}

						if (compatiblePreferredValueClass != null) {
							break;
						}
					}

					if (compatiblePreferredValueClass == null) {
						final StringBuilder sb = new StringBuilder("Column '");
						sb.append(m_strColumnName).
						append("' has an unexpected type. Acceptable types are: ");
						for (final Class<? extends DataValue> valueType : m_listCompatibleValueClasses) {
							sb.append(valueType.getSimpleName()).append(", ");
						}
						sb.setLength(sb.length() - 2);
						throw new InvalidSettingsException(sb.toString());
					}
				}
			}
		}

		// Set empty cell policy
		m_emptyCellPolicy = emptyCellPolicy;
		m_defaultCell = defaultCell;
		if (m_emptyCellPolicy == EmptyCellPolicy.UseDefault && defaultCell == null) {
			throw new IllegalArgumentException("If the empty cell policy is set to " +
					"UseDefault, the default cell value must not be null.");
		}
	}

	//
	// Public Methods
	//

	/**
	 * Returns the model with the column name.
	 * 
	 * @return Model with the column name.
	 */
	public SettingsModelString getModel() {
		return m_modelAsUniqueId;
	}

	/**
	 * Determines, if this input column information is representing a row key column.
	 * 
	 * @return True, if this is a row key. False otherwise.
	 */
	public boolean isRowKey() {
		return m_bUseRowKey;
	}

	/**
	 * Returns a valid index to the column in the input table.
	 * 
	 * @return Column index. -1, if this is a row key column.
	 */
	public int getColumnIndex() {
		return m_iColIndex;
	}

	/**
	 * Returns the valid column specification of the column in the input table.
	 * 
	 * @return Column specification. Null, if this is a row key column.
	 */
	public DataColumnSpec getColumnSpec() {
		return m_colSpec;
	}

	/**
	 * Returns the optional column description.
	 * 
	 * @return Column description, e.g. "molecule" or "atom list". Can be null or empty.
	 */
	public String getColumnDescription() {
		return m_strColumnDescription;
	}
	/**
	 * Returns the valid table specification of the table the column belongs to.
	 * 
	 * @return Table specification.
	 */
	public DataTableSpec getTableSpec() {
		return m_tableSpec;
	}

	/**
	 * Returns the optional table description.
	 * 
	 * @return Table description, e.g. "first" or "molecule". Can be null or empty.
	 */
	public String getTableDescription() {
		return m_strTableDescription;
	}

	/**
	 * Returns the valid data type of the column in the input table.
	 * 
	 * @return Column data type. StringCell.TYPE, if this is a row key column.
	 */
	public DataType getDataType() {
		return m_dataType;
	}

	/**
	 * Returns true, if the input data column is not matching any
	 * of the preferred data values, but is convertible using
	 * an adapter cell.
	 * 
	 * @return True, if conversion is necessary to use this column.
	 * 		False otherwise.
	 */
	public boolean needsConversion() {
		return m_converter != null;
	}

	/**
	 * Returns the converter that can be used to convert the input column
	 * into an Adapter Cell column to make it compatible with the desired
	 * types.
	 * 
	 * @return Data cell type converter or null, if not available.
	 */
	public DataCellTypeConverter getConverter() {
		return m_converter;
	}

	/**
	 * Returns the configured policy id to be applied when empty cells are encountered.
	 * 
	 * @return The empty cell policy.
	 */
	public EmptyCellPolicy getEmptyCellPolicy() {
		return m_emptyCellPolicy;
	}

	/**
	 * Returns the default cell value that shall be used, if an empty cell is encountered
	 * and the Empty Cell Policy ({@link #getEmptyCellPolicy()}) is set to
	 * {@link EmptyCellPolicy#UseDefault}.
	 * 
	 * @return Default cell.
	 */
	public DataCell getDefaultCell() {
		return m_defaultCell;
	}

	/**
	 * Determines the cell of the passed in row that is specified by this
	 * input data info object. It returns the cell as is without converting it
	 * into a datatype. Empty cell policy is not applied when calling
	 * this method. This means an empty cell can be returned, which may
	 * contain an error message from auto-conversion.
	 * 
	 * @param row The data row with concrete data cells. This data row must
	 * 		belong to the table, which spec was used in the constructor.
	 * 		Otherwise the behavior is undefined and will probably cause an
	 * 		undefined Exception. Must not be null.
	 * 
	 * @return The original cell.
	 */
	public DataCell getOriginalCell(final DataRow row) {
		return row.getCell(getColumnIndex());
	}

	/**
	 * Determines the cell of the passed in row that is specified by this
	 * input data info object. When an empty cell is encountered, it applies
	 * the defined empty cell policy. An explicit type check is not performed
	 * anymore at this point, because it was done already when this input data info
	 * object was created.
	 * 
	 * @param row The data row with concrete data cells. This data row must
	 * 		belong to the table, which spec was used in the constructor.
	 * 		Otherwise the behavior is undefined and will probably cause an
	 * 		undefined Exception. Must not be null.
	 * 
	 * @return The correct cell. The active empty cell policy has been applied
	 * 		before the cell gets returned. If the policy is set to
	 * 		TreatAsNull it will return null, if it is set to UseDefault the configured
	 * 		default cell is returned. If a Custom policy is defined it will
	 * 		return the MissingDataCell in this case and it is up to the caller
	 * 		to handle it properly.
	 * 
	 * @throws EmptyCellException Thrown, if an empty cell is encountered and
	 * 		the empty cell policy is set to either DeliverEmptyValue
	 * 		or StopExecution.
	 */
	public DataCell getCell(final DataRow row) throws EmptyCellException {
		DataCell retCell;

		if (isRowKey()) {
			retCell = new StringCell(row.getKey().toString());
		}
		else {
			retCell = row.getCell(getColumnIndex());

			// Handle an empty / missing cell
			if (retCell.isMissing()) {
				switch (getEmptyCellPolicy()) {
				case TreatAsNull:
					retCell = null;
					break;
				case UseDefault:
					retCell = m_defaultCell;
					break;
				case DeliverEmptyRow:
					throw new EmptyCellException("Empty cell in ('" +
							getColumnSpec().getName() + "', '" + row.getKey() + "'). All result cells will be empty.",
							this, row.getKey());
				case StopExecution:
					throw new EmptyCellException("An empty cell has been encountered in ('" +
							getColumnSpec().getName() + "', '" + row.getKey() + "'). Execution failed.",
							this, row.getKey());
				case Custom:
					break;
				}
			}
		}

		return retCell;
	}

	/**
	 * Determines, if the cell of the passed in row that is specified by this
	 * input data info object, is a missing cell. This method does not apply
	 * any empty cell policy. If this is desired, please call {@link #getCell(DataRow)}
	 * instead and do the comparison manually.
	 * 
	 * @param row The data row with concrete data cells. This data row must
	 * 		belong to the table, which spec was used in the constructor.
	 * 		Otherwise the behavior is undefined and will probably cause an
	 * 		undefined Exception. Must not be null.
	 * 
	 * @return True, if the cell is missing, false otherwise.
	 */
	public boolean isMissing(final DataRow row) {
		if (!isRowKey()) {
			final DataCell retCell = row.getCell(getColumnIndex());
			return retCell.isMissing();
		}
		return false;
	}

	/**
	 * Convenience method to check, if the column is compatible with the specified
	 * value class. It does not look at a particular cell, but at the column
	 * specification. For performance reasons we cache the result. 
	 * The method takes the isCompatible() information for the cell's
	 * datatype into account. It basically determines, if a cell can be cast to 
	 * the valueClass.
	 * 
	 * @param valueClass Value class for compatibility check. Can be null.
	 * 
	 * @return True, if column type is compatible with specified value class.
	 * 		False otherwise. Also false, if null was passed in.
	 */
	public boolean isCompatible(final Class<? extends DataValue> valueClass) {
		Boolean bRet = false;

		if (isRowKey()) {
         bRet = getDataType().isCompatible(valueClass);
		}
		else {
		   synchronized (m_mapCompatibilityCache) {
   			bRet = m_mapCompatibilityCache.get(valueClass);
   
   			if (bRet == null) {
   				bRet = false;
   				
   				if (valueClass != null) {
   					   DataType type = getTableSpec().getColumnSpec(getColumnIndex()).getType();
   					   bRet = type.isCompatible(valueClass);
   				}
   				
   				m_mapCompatibilityCache.put(valueClass, bRet);
   			}
		   }
		}

		return bRet;
	}
	
	  /**
    * Convenience method to check, if the column is compatible with the specified
    * value class. It does not look at a particular cell, but at the column
    * specification. For performance reasons we cache the result. 
    * The method takes the isCompatible() and isAdaptable() information for the cell's
    * datatype into account, so it works with normal cells as well as with adapter cells.
    * 
    * @param valueClass Value class for compatibility check. Can be null.
    * 
    * @return True, if column type is compatible with specified value class.
    *       False otherwise. Also false, if null was passed in.
    */
   public boolean isCompatibleOrAdaptable(final Class<? extends DataValue> valueClass) {
      Boolean bRet = false;

      if (isRowKey()) {
         bRet = getDataType().isCompatible(valueClass);
      }
      else {
         synchronized (m_mapCompatibilityAndAdaptibilityCache) {
            bRet = m_mapCompatibilityAndAdaptibilityCache.get(valueClass);
   
            if (bRet == null) {
               bRet = false;
               
               if (valueClass != null) {
                     DataType type = getTableSpec().getColumnSpec(getColumnIndex()).getType();
                     bRet = type.isCompatible(valueClass) || type.isAdaptable(valueClass);
               }
               
               m_mapCompatibilityAndAdaptibilityCache.put(valueClass, bRet);
            }
         }
      }

      return bRet;
   }

	/**
	 * Determines, if the cell of the passed in row that is specified by this
	 * input data info object, is matching the default cell. This method does not apply
	 * any empty cell policy. If this is desired, please call {@link #getCell(DataRow)}
	 * instead and do the comparison manually.
	 * 
	 * @param row The data row with concrete data cells. This data row must
	 * 		belong to the table, which spec was used in the constructor.
	 * 		Otherwise the behavior is undefined and will probably cause an
	 * 		undefined Exception. Must not be null.
	 * 
	 * @return True, if the cell is matching the configured default cell, false otherwise.
	 */
	public boolean isDefault(final DataRow row) {
		final DataCell retCell = (isRowKey() ? new StringCell(row.getKey().toString()) : row.getCell(getColumnIndex()));
		return retCell.equals(getDefaultCell());
	}

	/**
	 * Convenience method that converts the result of {@link #getCell(DataRow)} into a string.
	 * 
	 * @param row The data row with concrete data cells. This data row must
	 * 		belong to the table, which spec was used in the constructor.
	 * 		Otherwise the behavior is undefined and will probably cause an
	 * 		undefined Exception. Must not be null.
	 * 
	 * @return The string value of the correct cell.
	 * 
	 * @throws EmptyCellException See {@link #getCell(DataRow)}.
	 * @throws IllegalArgumentException Thrown, if the cell is not compatible
	 * 		with the StringCell. This is usually an implementation error.
	 * 
	 * @see #getCell(DataRow)
	 */
	public String getString(final DataRow row) throws EmptyCellException {
		String strRet = null;

		if (isRowKey()) {
			strRet = row.getKey().toString();
		}
		else {
			final DataCell cell = getCell(row);

			if (cell != null) {
				if (cell.getType().isCompatible(StringValue.class)) {
					strRet = ((StringValue)cell).getStringValue();
				}
				else {
					throw new IllegalArgumentException("The cell in column " + getColumnSpec().getName() +
							" is not compatible with a StringCell. This is usually an implementation error.");
				}
			}
		}

		return strRet;
	}

	/**
	 * Convenience method that converts the result of {@link #getCell(DataRow)} into a boolean.
	 * 
	 * @param row The data row with concrete data cells. This data row must
	 * 		belong to the table, which spec was used in the constructor.
	 * 		Otherwise the behavior is undefined and will probably cause an
	 * 		undefined Exception. Must not be null.
	 * 
	 * @return The boolean value of the correct cell.
	 * 
	 * @throws EmptyCellException See {@link #getCell(DataRow)}.
	 * @throws IllegalArgumentException Thrown, if the cell is not compatible
	 * 		with the BooleanCell. This is usually an implementation error.
	 * 
	 * @see #getCell(DataRow)
	 */
	public boolean getBoolean(final DataRow row) throws EmptyCellException {
		final DataCell cell = getCell(row);
		boolean bRet = false;

		if (cell == null) {
			bRet = false;
		}
		else if (cell.getType().isCompatible(BooleanValue.class)) {
			bRet = ((BooleanValue)cell).getBooleanValue();
		}
		else {
			throw new IllegalArgumentException("The cell in column " + getColumnSpec().getName() +
					" is not compatible with a BooleanCell. This is usually an implementation error.");
		}

		return bRet;
	}

	/**
	 * Convenience method that converts the result of {@link #getCell(DataRow)} into a double.
	 * 
	 * @param row The data row with concrete data cells. This data row must
	 * 		belong to the table, which spec was used in the constructor.
	 * 		Otherwise the behavior is undefined and will probably cause an
	 * 		undefined Exception. Must not be null.
	 * 
	 * @return The double value of the correct cell.
	 * 
	 * @throws EmptyCellException See {@link #getCell(DataRow)}.
	 * @throws IllegalArgumentException Thrown, if the cell is not compatible
	 * 		with the DoubleCell. This is usually an implementation error.
	 * 
	 * @see #getCell(DataRow)
	 */
	public double getDouble(final DataRow row) throws EmptyCellException {
		final DataCell cell = getCell(row);
		double dRet = 0;

		if (cell == null) {
			dRet = 0;
		}
		else if (cell.getType().isCompatible(DoubleValue.class)) {
			dRet = ((DoubleValue)cell).getDoubleValue();
		}
		else {
			throw new IllegalArgumentException("The cell in column " + getColumnSpec().getName() +
					" is not compatible with a DoubleCell. This is usually an implementation error.");
		}

		return dRet;
	}

	/**
	 * Convenience method that converts the result of {@link #getCell(DataRow)} into a int.
	 * 
	 * @param row The data row with concrete data cells. This data row must
	 * 		belong to the table, which spec was used in the constructor.
	 * 		Otherwise the behavior is undefined and will probably cause an
	 * 		undefined Exception. Must not be null.
	 * 
	 * @return The int value of the correct cell.
	 * 
	 * @throws EmptyCellException See {@link #getCell(DataRow)}.
	 * @throws IllegalArgumentException Thrown, if the cell is not compatible
	 * 		with the IntCell. This is usually an implementation error.
	 * 
	 * @see #getCell(DataRow)
	 */
	public int getInt(final DataRow row) throws EmptyCellException {
		final DataCell cell = getCell(row);
		int iRet = 0;

		if (cell == null) {
			iRet = 0;
		}
		else if (cell.getType().isCompatible(IntValue.class)) {
			iRet = ((IntValue)cell).getIntValue();
		}
		else {
			throw new IllegalArgumentException("The cell in column " + getColumnSpec().getName() +
					" is not compatible with an IntCell. This is usually an implementation error.");
		}

		return iRet;
	}

	/**
	 * Convenience method that converts the result of {@link #getCell(DataRow)} into a long.
	 * 
	 * @param row The data row with concrete data cells. This data row must
	 * 		belong to the table, which spec was used in the constructor.
	 * 		Otherwise the behavior is undefined and will probably cause an
	 * 		undefined Exception. Must not be null.
	 * 
	 * @return The long value of the correct cell.
	 * 
	 * @throws EmptyCellException See {@link #getCell(DataRow)}.
	 * @throws IllegalArgumentException Thrown, if the cell is not compatible
	 * 		with the LongCell. This is usually an implementation error.
	 * 
	 * @see #getCell(DataRow)
	 */
	public long getLong(final DataRow row) throws EmptyCellException {
		final DataCell cell = getCell(row);
		long lRet = 0;

		if (cell == null) {
			lRet = 0;
		}
		else if (cell.getType().isCompatible(LongValue.class)) {
			lRet = ((LongValue)cell).getLongValue();
		}
		else {
			throw new IllegalArgumentException("The cell in column " + getColumnSpec().getName() +
					" is not compatible with an LongCell. This is usually an implementation error.");
		}

		return lRet;
	}

	/**
	 * Convenience method that converts the result of {@link #getCell(DataRow)} into a DenseByteVector object.
	 * 
	 * @param row The data row with concrete data cells. This data row must
	 * 		belong to the table, which spec was used in the constructor.
	 * 		Otherwise the behavior is undefined and will probably cause an
	 * 		undefined Exception. Must not be null.
	 * 
	 * @return The DenseByteVector value of the correct cell.
	 * 
	 * @throws EmptyCellException See {@link #getCell(DataRow)}.
	 * @throws IllegalArgumentException Thrown, if the cell is not compatible
	 * 		with the DenseByteVectorCell. This is usually an implementation error.
	 * 
	 * @see #getCell(DataRow)
	 */
	public DenseByteVector getDenseByteVector(final DataRow row) throws EmptyCellException {
		final DataCell cell = getCell(row);
		DenseByteVector dbv = null;

		if (cell != null) {
			if (cell.getType().isCompatible(DenseByteVectorCell.class)) {
				dbv = ((DenseByteVectorCell)cell).getByteVectorCopy();
			}
			else {
				throw new IllegalArgumentException("The cell in column " + getColumnSpec().getName() +
						" is not compatible with a DenseByteVectorCell. This is usually an implementation error.");
			}
		}

		return dbv;
	}

	/**
	 * Convenience method that converts the result of {@link #getCell(DataRow)} into a DenseBitVector object.
	 * 
	 * @param row The data row with concrete data cells. This data row must
	 * 		belong to the table, which spec was used in the constructor.
	 * 		Otherwise the behavior is undefined and will probably cause an
	 * 		undefined Exception. Must not be null.
	 * 
	 * @return The DenseBitVector value of the correct cell.
	 * 
	 * @throws EmptyCellException See {@link #getCell(DataRow)}.
	 * @throws IllegalArgumentException Thrown, if the cell is not compatible
	 * 		with the DenseBitVectorCell. This is usually an implementation error.
	 * 
	 * @see #getCell(DataRow)
	 */
	public DenseBitVector getDenseBitVector(final DataRow row) throws EmptyCellException {
		final DataCell cell = getCell(row);
		DenseBitVector dbv = null;

		if (cell != null) {
			if (cell instanceof DenseBitVectorCell) {
				dbv = ((DenseBitVectorCell)cell).getBitVectorCopy();
			}
			else {
				throw new IllegalArgumentException("The cell in column " + getColumnSpec().getName() +
						" is not compatible with a DenseBitVectorCell. This is usually an implementation error.");
			}
		}

		return dbv;
	}

	/**
	 * Convenience method that converts the result of {@link #getCell(DataRow)} into an ExplicitBitVect object.
	 * 
	 * @param row The data row with concrete data cells. This data row must
	 * 		belong to the table, which spec was used in the constructor.
	 * 		Otherwise the behavior is undefined and will probably cause an
	 * 		undefined Exception. Must not be null.
	 * 
	 * @return The ExplicitBitVect value of the correct cell.
	 * 
	 * @throws EmptyCellException See {@link #getCell(DataRow)}.
	 * @throws IllegalArgumentException Thrown, if the cell is not compatible
	 * 		with the BitVectorValue. This is usually an implementation error.
	 * 
	 * @see #getCell(DataRow)
	 */
	public ExplicitBitVect getExplicitBitVector(final DataRow row) throws EmptyCellException {
		final DataCell cell = getCell(row);
		ExplicitBitVect expBitVector = null;

		if (cell != null) {
			if (cell.getType().isCompatible(BitVectorValue.class)) {
				final BitVectorValue bitVector = ((BitVectorValue)cell);

				// Convert the bit vector to RDKit style explicit bit vector
				expBitVector = new ExplicitBitVect(bitVector.length());
				long nextBit = bitVector.nextSetBit(0);

				while (nextBit >= 0) {
					expBitVector.setBit(nextBit);
					nextBit = bitVector.nextSetBit(nextBit+1);
				}
			}
			else {
				throw new IllegalArgumentException("The cell in column " + getColumnSpec().getName() +
						" is not compatible with a BitVectorValue. This is usually an implementation error.");
			}
		}

		return expBitVector;
	}


	/**
	 * Convenience method that converts the result of {@link #getCell(DataRow)} into a ROMol object.
	 * 
	 * @param row The data row with concrete data cells. This data row must
	 * 		belong to the table, which spec was used in the constructor.
	 * 		Otherwise the behavior is undefined and will probably cause an
	 * 		undefined Exception. Must not be null.
	 * 
	 * @return The ROMol value of the correct cell.
	 * 
	 * @throws EmptyCellException See {@link #getCell(DataRow)}.
	 * @throws IllegalArgumentException Thrown, if the cell is not compatible
	 * 		with the RDKitMolValue. This is usually an implementation error.
	 * 
	 * @see #getCell(DataRow)
	 */
	public ROMol getROMol(final DataRow row) throws EmptyCellException {
		final DataCell cell = getCell(row);
		ROMol mol = null;

		if (cell != null) {
			if (cell.getType().isCompatible(RDKitMolValue.class)) {
				mol = ((RDKitMolValue)cell).readMoleculeValue();
			}
			else if (cell.getType().isCompatible(AdapterValue.class)
					&& ((AdapterValue)cell).isAdaptable(RDKitMolValue.class)) {
				mol = ((AdapterValue)cell).getAdapter(RDKitMolValue.class).readMoleculeValue();
			}
			else {
				throw new IllegalArgumentException("The cell in column " + getColumnSpec().getName() +
						" is not compatible with a RDKitMolValue. This is usually an implementation error.");
			}
		}

		return mol;
	}

	/**
	 * Convenience method that converts the result of {@link #getCell(DataRow)} into
	 * a ChemicalReaction object. The cell must be compatible with RxnValue.
	 * 
	 * @param row The data row with concrete data cells. This data row must
	 * 		belong to the table, which spec was used in the constructor.
	 * 		Otherwise the behavior is undefined and will probably cause an
	 * 		undefined Exception. Must not be null.
	 * 
	 * @return The ChemicalReaction value of the correct cell.
	 * 
	 * @throws EmptyCellException See {@link #getCell(DataRow)}.
	 * @throws IllegalArgumentException Thrown, if the cell is not compatible
	 * 		with the ChemicalReaction class.
	 * 
	 * @see #getCell(DataRow)
	 */
	public ChemicalReaction getChemicalReaction(final DataRow row) throws EmptyCellException {
		final DataCell cell = getCell(row);
		ChemicalReaction rxn = null;

		if (cell != null) {
			if (cell.getType().isCompatible(RxnValue.class)) {
				// Read the Rxn string value
				final String strRxn = ((RxnValue)cell).getRxnValue();

				// Convert the Rxn value into a ChemicalReaction
				try {
					rxn = ChemicalReaction.ReactionFromRxnBlock(strRxn);
				}
				catch (final Exception exc) {
					throw new IllegalArgumentException("Unable to parse reaction value found in the table.", exc);
				}

				if (rxn == null) {
					throw new RuntimeException(
							"Unable to parse reaction value found in the table (RDKit lib returned null).");
				}
			}
			else if (cell.getType().isCompatible(SmartsValue.class) || cell.getType().isAdaptable(SmartsValue.class)) {
				// Read the SMARTS value
				String strSmarts = null;
				
				if (cell.getType().isCompatible(SmartsValue.class)) {
				   strSmarts = ((SmartsValue)cell).getSmartsValue();
				}
				else {
				   strSmarts = ((AdapterValue)cell).getAdapter(SmartsValue.class).getSmartsValue();
				}
				
				// Convert the SMARTS value into a ChemicalReaction
				try {
					rxn = ChemicalReaction.ReactionFromSmarts(strSmarts);
				}
				catch (final Exception exc) {
					throw new IllegalArgumentException("Unable to parse SMARTS value found in the table.", exc);
				}

				if (rxn == null) {
					throw new RuntimeException(
							"Unable to parse SMARTS value found in the table (RDKit lib returned null).");
				}
			}
			else {
				throw new IllegalArgumentException("The cell in column " + getColumnSpec().getName() +
						" is not compatible with an RxnValue or SmartsValue. This is usually an implementation error.");
			}
		}

		return rxn;
	}

	/**
	 * Convenience method that converts the result of {@link #getCell(DataRow)} into a Smiles string.
	 * 
	 * @param row The data row with concrete data cells. This data row must
	 * 		belong to the table, which spec was used in the constructor.
	 * 		Otherwise the behavior is undefined and will probably cause an
	 * 		undefined Exception. Must not be null.
	 * 
	 * @return The Smiles string value of the correct cell.
	 * 
	 * @throws EmptyCellException See {@link #getCell(DataRow)}.
	 * @throws IllegalArgumentException Thrown, if the cell is not compatible
	 * 		with the SmilesValue. This is usually an implementation error.
	 * 
	 * @see #getCell(DataRow)
	 */
	public String getSmiles(final DataRow row) throws EmptyCellException {
		final DataCell cell = getCell(row);
		String strSmiles = null;

		if (cell != null) {
			if (cell.getType().isCompatible(SmilesValue.class)) {
				strSmiles = ((SmilesValue)cell).getSmilesValue();
			}
         else if (cell.getType().isAdaptable(SmilesValue.class)) {
            strSmiles = ((AdapterValue)cell).getAdapter(SmilesValue.class).getSmilesValue();
         }
			else {
				throw new IllegalArgumentException("The cell in column " + getColumnSpec().getName() +
						" is not compatible with a SmilesValue. This is usually an implementation error.");
			}
		}

		return strSmiles;
	}

	/**
	 * Convenience method that converts the result of {@link #getCell(DataRow)} into a Smarts string.
	 * 
	 * @param row The data row with concrete data cells. This data row must
	 * 		belong to the table, which spec was used in the constructor.
	 * 		Otherwise the behavior is undefined and will probably cause an
	 * 		undefined Exception. Must not be null.
	 * 
	 * @return The Smarts string value of the correct cell.
	 * 
	 * @throws EmptyCellException See {@link #getCell(DataRow)}.
	 * @throws IllegalArgumentException Thrown, if the cell is not compatible
	 * 		with the SmartsValue. This is usually an implementation error.
	 * 
	 * @see #getCell(DataRow)
	 */
	public String getSmarts(final DataRow row) throws EmptyCellException {
		final DataCell cell = getCell(row);
		String strSmarts = null;

		if (cell != null) {
			if (cell.getType().isCompatible(SmartsValue.class)) {
				strSmarts = ((SmartsValue)cell).getSmartsValue();
			}
			else {
				throw new IllegalArgumentException("The cell in column " + getColumnSpec().getName() +
						" is not compatible with a SmartsValue. This is usually an implementation error.");
			}
		}

		return strSmarts;
	}

	/**
	 * Convenience method that converts the result of {@link #getCell(DataRow)} into an SDF string.
	 * 
	 * @param row The data row with concrete data cells. This data row must
	 * 		belong to the table, which spec was used in the constructor.
	 * 		Otherwise the behavior is undefined and will probably cause an
	 * 		undefined Exception. Must not be null.
	 * 
	 * @return The SDF value of the correct cell.
	 * 
	 * @throws EmptyCellException See {@link #getCell(DataRow)}.
	 * @throws IllegalArgumentException Thrown, if the cell is not compatible
	 * 		with the SmilesValue. This is usually an implementation error.
	 * 
	 * @see #getCell(DataRow)
	 */
	public String getSdfValue(final DataRow row) throws EmptyCellException {
		final DataCell cell = getCell(row);
		String strSdfValue = null;

		if (cell != null) {
			if (cell.getType().isCompatible(SdfValue.class)) {
				strSdfValue = ((SdfValue)cell).getSdfValue();
			}
			else if (cell.getType().isAdaptable(SdfValue.class)) {
			   strSdfValue = ((AdapterValue)cell).getAdapter(SdfValue.class).getSdfValue();
			}
			else {
				throw new IllegalArgumentException("The cell in column " + getColumnSpec().getName() +
						" is not compatible with a SdfValue. This is usually an implementation error.");
			}
		}

		return strSdfValue;
	}

	/**
	 * Convenience method that converts the result of {@link #getCell(DataRow)} into an integer list.
	 * If the cell contains a collection, but some of the values are not compatible with an integer
	 * or double (which will be casted to int), these values will be ignored and a warning is logged.
	 * 
	 * @param row The data row with concrete data cells. This data row must
	 * 		belong to the table, which spec was used in the constructor.
	 * 		Otherwise the behavior is undefined and will probably cause an
	 * 		undefined Exception. Must not be null.
	 * 
	 * @return The integer list contained in the correct cell.
	 * 
	 * @throws EmptyCellException See {@link #getCell(DataRow)}.
	 * @throws IllegalArgumentException Thrown, if the cell is not compatible
	 * 		with the collection type. This is usually an implementation error.
	 * 
	 * @see #getCell(DataRow)
	 */
	public List<Integer> getIntegerList(final DataRow row) throws EmptyCellException {
		List<Integer> listIntegers = null;
		final DataCell cell = getCell(row);

		if (cell != null) {
			listIntegers = new ArrayList<Integer>(20);
			if (cell.getType().isCollectionType()) {
				boolean bIncompatibleValuesFound = false;

				// Create integer list from data cell
				for (final Iterator<DataCell> i = ((CollectionDataValue)cell).iterator(); i.hasNext(); ) {
					final DataCell listElement = i.next();

					// Missing cells are ignored and lead to empty lists
					if (!listElement.isMissing()) {
						if (listElement.getType().isCompatible(IntValue.class)) {
							listIntegers.add(((IntValue)listElement).getIntValue());
						}
						else if (listElement.getType().isCompatible(DoubleValue.class)) {
							listIntegers.add((int)((DoubleValue)listElement).getDoubleValue());
						}
						else {
							bIncompatibleValuesFound = true;
						}
					}
				}

				if (bIncompatibleValuesFound) {
					LOGGER.warn("Encountered a collection cell in column " + getColumnSpec().getName() +
							" with none integer values when expecting integer values. Skipping these values.");
				}
			}
			else {
				throw new IllegalArgumentException("The cell in column " + getColumnSpec().getName() +
						" is not based on a collection type. This is usually an implementation error.");
			}
		}

		return listIntegers;
	}

	/**
	 * Convenience method that converts the result of {@link #getCell(DataRow)} into an RDKit Int_Vect object.
	 * If the cell contains a collection, but some of the values are not compatible with an integer,
	 * these values will be ignored and a warning is logged.
	 * 
	 * @param row The data row with concrete data cells. This data row must
	 * 		belong to the table, which spec was used in the constructor.
	 * 		Otherwise the behavior is undefined and will probably cause an
	 * 		undefined Exception. Must not be null.
	 * 
	 * @return The Int_Vect list based on the correct cell.
	 * 
	 * @throws EmptyCellException See {@link #getCell(DataRow)}.
	 * @throws IllegalArgumentException Thrown, if the cell is not compatible
	 * 		with the collection type. This is usually an implementation error.
	 * 
	 * @see #getCell(DataRow)
	 */
	public Int_Vect getRDKitIntegerVector(final DataRow row) throws EmptyCellException {
		Int_Vect vectInt = null;
		final List<Integer> listIntegers = getIntegerList(row);

		if (listIntegers != null) {
			vectInt = new Int_Vect();
			for (final Integer i : listIntegers) {
				vectInt.add(i);
			}
		}

		return vectInt;
	}

	/**
	 * Convenience method that converts the result of {@link #getCell(DataRow)} into an RDKit Int_Vect object.
	 * If the cell contains a collection, but some of the values are not compatible with an integer,
	 * these values will be ignored and a warning is logged.
	 * 
	 * @param row The data row with concrete data cells. This data row must
	 * 		belong to the table, which spec was used in the constructor.
	 * 		Otherwise the behavior is undefined and will probably cause an
	 * 		undefined Exception. Must not be null.
	 * 
	 * @return The UInt_Vect list based on the correct cell.
	 * 
	 * @throws EmptyCellException See {@link #getCell(DataRow)}.
	 * @throws IllegalArgumentException Thrown, if the cell is not compatible
	 * 		with the collection type. This is usually an implementation error.
	 * 
	 * @see #getCell(DataRow)
	 */
	public UInt_Vect getRDKitUIntegerVector(final DataRow row) throws EmptyCellException {
		UInt_Vect vectInt = null;
		final List<Integer> listIntegers = getIntegerList(row);

		if (listIntegers != null) {
			vectInt = new UInt_Vect(listIntegers.size());
			for (final Integer i : listIntegers) {
				vectInt.add(i);
			}
		}

		return vectInt;
	}

	//
	// Protected Methods
	//

	/**
	 * Returns based on the table description passed in to the constructor
	 * a short table info. If no table description is defined, it returns
	 * "input table", otherwise "columndescription input table".
	 * 
	 * @return Table identification. Never null.
	 */
	protected String getTableIdentification() {
		return (!StringUtils.isEmptyAfterTrimming(m_strColumnDescription) ?
				m_strColumnDescription + " table" : "input table");
	}

	/**
	 * Returns based on the column description passed in to the constructor
	 * a short table info. If no column description is defined, it returns
	 * "input column", otherwise "columndescription input column".
	 * 
	 * @return Table identification. Never null.
	 */
	protected String getColumnIdentification() {
		return (!StringUtils.isEmptyAfterTrimming(m_strColumnDescription) ?
				m_strColumnDescription + " column" : "input column");
	}

	/**
	 * This exception is thrown, when an empty cell was encountered during processing,
	 * which had an Empty Cell Policy DeliverEmptyValue or StopExecution assigned.
	 * 
	 * @author Manuel Schwarze
	 */
	public static class EmptyCellException extends Exception {

		//
		// Constants
		//

		/** Serial number. */
		private static final long serialVersionUID = -6913351823963828561L;

		//
		// Members
		//

		/**
		 * Stores information about the input column responsible for the failure.
		 */
		private final InputDataInfo m_inputDataInfo;

		/**
		 * Stores information about the input row responsible for the failure.
		 */
		private final RowKey m_rowKey;

		//
		// Constructor
		//

		/**
		 * Creates a new empty cell policy exception, which states that an empty input
		 * cell was encountered with the specified empty cell policy, that lead
		 * to this exception.
		 * 
		 * @param message The error message.
		 * @param inputDataInfo Information about the column. Must not be null.
		 * @param rowKey Information about the row. Can be null.
		 */
		public EmptyCellException(final String message, final InputDataInfo inputDataInfo, final RowKey rowKey) {
			super(message);

			if (inputDataInfo == null) {
				throw new IllegalArgumentException("Input data information must not be null.");
			}

			m_inputDataInfo = inputDataInfo;
			m_rowKey = rowKey;
		}

		//
		// Public Methods
		//

		/**
		 * Returns the input data info object of the column that caused the failure.
		 * 
		 * @return Input data info object. Not null.
		 */
		public InputDataInfo getInputDataInfo() {
			return m_inputDataInfo;
		}

		/**
		 * Convenience method to get the column name of the column that caused the failure
		 * from the input data info object.
		 * 
		 * @return Column name. Returns "Row Key" if it is the row key instead of a real column.
		 * 
		 * @see #getInputDataInfo()
		 */
		public String getColumnName() {
			return m_inputDataInfo.isRowKey() ? "Row Key" : m_inputDataInfo.getColumnSpec().getName();
		}

		/**
		 * Convenience method the empty cell policy that was encountered when processing an empty cell
		 * from the input data info object.
		 * 
		 * @return Empty cell policy of the empty cell that was tried to be processed.
		 * 
		 * @see #getInputDataInfo()
		 */
		public EmptyCellPolicy getEmptyCellPolicy() {
			return m_inputDataInfo.getEmptyCellPolicy();
		}

		/**
		 * Returns the row key of the row that caused the failure. Might not be set.
		 * 
		 * @return Row key. Can be null.
		 */
		public RowKey getRowKey() {
			return m_rowKey;
		}

		/**
		 * Determines, if the exception should lead to a full stop of processing or not.
		 * 
		 * @return True, if execution should fully stop. False, if it is good enough to
		 * 		create a full set of empty result cells.
		 */
		public boolean stopsNodeExecution() {
			return getEmptyCellPolicy() == EmptyCellPolicy.StopExecution;
		}
	}
}
