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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.tree.TreeNode;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This class offers several utility methods to deal with KNIME settings objects.
 * 
 * @author Manuel Schwarze
 */
public class SettingsUtils {

	//
	// Constants
	//
	
	/** The logger instance. */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(SettingsUtils.class);
	
	/**
	 * Determines the node name based on a settings object.
	 * 
	 * @param settings Settings of a node.
	 * 
	 * @return The node name contained in the settings or null, if not found.
	 */
	public static String getNodeName(final ConfigRO settings) {
		String nodeName = null; 
		TreeNode treeNode = settings;
		
		// Find the highest XML structure config tag, which contains information about the node name
		while (treeNode != null && treeNode instanceof NodeSettings && treeNode.getParent() != null) {
			treeNode = treeNode.getParent();
		}
		
		// If found, read the node name
		if (treeNode instanceof NodeSettings) {
			try {
				nodeName = ((NodeSettings)treeNode).getString("name");
			}
			catch (InvalidSettingsException excNoName) {
				// Ignored by purpose
			}
		}
		
		return nodeName;
	}
	
	/**
	 * This method compares two objects and considers also the value null.
	 * If the objects are both not null, equals is called for o1 with o2.
	 * 
	 * @param o1 The first object to compare. Can be null.
	 * @param o2 The second object to compare. Can be null.
	 * 
	 * @return True, if the two objects are equal. Also true, if
	 * 		   both objects are null.
	 */
	@SuppressWarnings("rawtypes")
	public static boolean equals(Object o1, Object o2) {
		boolean bResult = false;

		if (o1 == o2) {
			bResult = true;
		}
		else if (o1 != null && o1.getClass().isArray() && 
				 o2 != null && o2.getClass().isArray() &&
				 o1.getClass().getComponentType().equals(o2.getClass().getComponentType()) &&
				 Array.getLength(o1) == Array.getLength(o2)) {
			int iLength = Array.getLength(o1);

			// Positive presumption
			bResult = true;
			
			for (int i = 0; i < iLength; i++) {
				if ((bResult &= SettingsUtils.equals(Array.get(o1, i), Array.get(o2, i))) == false) {
					break;
				}
			}
		}
		else if (o1 instanceof Collection && o2 instanceof Collection &&
				((Collection)o1).size() == ((Collection)o2).size()) {
			Iterator i1 = ((Collection)o1).iterator();
			Iterator i2 = ((Collection)o2).iterator();
			
			// Positive presumption
			if (i1.hasNext() && i2.hasNext()) {
				bResult = true;
				
				while (i1.hasNext() && i2.hasNext()) {
					if ((bResult &= SettingsUtils.equals(i1.next(), i2.next())) == false) {
						break;
					}
				}		
			}
		}
		else if (o1 != null && o2 != null) {
			bResult = o1.equals(o2);
		}
		else if (o1 == null && o2 == null) {
			bResult = true;
		}
		
		return bResult;
	}	
	
    /**
     * Auto guesses based on the passed in table specification the name of a column based
     * on its data value type. It only changes the specified settings object, if there
     * are no settings yet.
     * 
     * @param inSpec A table specification containing column specifications. Must not be null.
     * @param settingColumn A setting model standing for a column name. Must not be null.
     * @param valueClass The desired value class for the column compatibility check. Must not be null.
     * @param indexOfFindingsToBeUsed The index to be used if more than 1 column is found.
     * 		Important: This index is zero-based. Usually you would specify 0 here.
     * @param strWarningIfMoreThanOneAvailable If not set to null, this warning will
     * 		be shown in case that multiple columns would have fit the desired column type.
     * 		You may use the placeholder %COLUMN_NAME%, which
     * 		will be replaced with the auto selected column name in the warning message.
     * 		Can be null to omit a warning.
     * @param strErrorIfNotFound If not set to null, this will be used as error message when
     * 		no compatible columns were found and the setting could not be populated. Set it
     * 		to null, if no exception shall be thrown by this method.
     * @param warningConsolidator Warning consolidator instance to add warnings to. Can be null.
     * 
     * @return True, if the column was auto-determined or was already set before. 
     * 		False, if it could not be determined and the setting is still empty.
     * 
     * @throws InvalidSettingsException Thrown, if the last parameter is not null, and
     * 		if no compatible column could be found.
     */
	public static boolean autoGuessColumn(final DataTableSpec inSpec, 
    		final SettingsModelString settingColumn, 
    		final Class<? extends DataValue> valueClass, final int indexOfFindingsToBeUsed,
    		final String strWarningIfMoreThanOneAvailable, final String strErrorIfNotFound, 
    		final WarningConsolidator warningConsolidator) 
    	throws InvalidSettingsException {
		
    	// Pre-checks
     	if (valueClass == null) {
    		throw new IllegalArgumentException("Value class must not be null.");
    	}		
     	
     	List<Class<? extends DataValue>> listValueClasses = 
			new ArrayList<Class<? extends DataValue>>();
		listValueClasses.add(valueClass);
		return autoGuessColumn(inSpec, settingColumn, listValueClasses, indexOfFindingsToBeUsed,
	    		strWarningIfMoreThanOneAvailable, strErrorIfNotFound, 
	    		warningConsolidator);
	}
	
    /**
     * Auto guesses based on the passed in table specification the name of a column based
     * on its data value type. It only changes the specified settings object, if there
     * are no settings yet.
     * 
     * @param inSpec A table specification containing column specifications. Must not be null.
     * @param settingColumn A setting model standing for a column name. Must not be null.
     * @param listValueClasses a list of acceptable value classes for the column compatibility check. 
     * 		Must not be null.
     * @param indexOfFindingsToBeUsed The index to be used if more than 1 column is found.
     * 		Important: This index is zero-based. Usually you would specify 0 here.
     * @param strWarningIfMoreThanOneAvailable If not set to null, this warning will
     * 		be shown in case that multiple columns would have fit the desired column type.
     * 		You may use the placeholder %COLUMN_NAME%, which
     * 		will be replaced with the auto selected column name in the warning message.
     * 		Can be null to omit a warning.
     * @param strErrorIfNotFound If not set to null, this will be used as error message when
     * 		no compatible columns were found and the setting could not be populated. Set it
     * 		to null, if no exception shall be thrown by this method.
     * @param warningConsolidator Warning consolidator instance to add warnings to. Can be null.
     * 
     * @return True, if the column was auto-determined or was already set before. 
     * 		False, if it could not be determined and the setting is still empty.
     * 
     * @throws InvalidSettingsException Thrown, if the last parameter is not null, and
     * 		if no compatible column could be found.
     */
	public static boolean autoGuessColumn(final DataTableSpec inSpec, 
    		final SettingsModelString settingColumn, 
    		final List<Class<? extends DataValue>> listValueClasses, final int indexOfFindingsToBeUsed,
    		final String strWarningIfMoreThanOneAvailable, final String strErrorIfNotFound, 
    		final WarningConsolidator warningConsolidator) 
    	throws InvalidSettingsException {	
    	boolean bRet = false;
    	
    	// Pre-checks
    	if (inSpec == null) {
    		throw new IllegalArgumentException("Input table spec must not be null.");
    	}
    	if (settingColumn == null) {
    		throw new IllegalArgumentException("Settings model for column guessing must not be null.");
    	}
     	if (listValueClasses == null) {
    		throw new IllegalArgumentException("Value class list must not be null.");
    	}
     	if (listValueClasses.contains(null)) {
    		throw new IllegalArgumentException("Value class list must not contain null elements.");
     	}
   	
        // Auto guessing the input column name, if it was not set yet
     	if (settingColumn instanceof SettingsModelColumnName && 
     		((SettingsModelColumnName)settingColumn).useRowID()) {
     		bRet = true;	
     	}
     	else if (settingColumn.getStringValue() == null) {
            List<String> listCompatibleCols = new ArrayList<String>();
            for (DataColumnSpec colSpec : inSpec) {
            	for (Class<? extends DataValue> valueClass : listValueClasses) {
	                if (colSpec.getType().isCompatible(valueClass)) {
	                    listCompatibleCols.add(colSpec.getName());
	                    break;
	                }
            	}
            }
            
            // Use a single column, if only one is compatible, without a warning
            if (indexOfFindingsToBeUsed == listCompatibleCols.size() - 1) {
            	settingColumn.setStringValue(listCompatibleCols.get(indexOfFindingsToBeUsed));
            	bRet = true;
            } 
            
            // Auto-guessing: Use the first matching column, but generate a warning (optionally)
            else if (indexOfFindingsToBeUsed < listCompatibleCols.size() - 1) {
            	settingColumn.setStringValue(listCompatibleCols.get(indexOfFindingsToBeUsed));
            	bRet = true;
            	if (strWarningIfMoreThanOneAvailable != null) {
            		if (warningConsolidator != null) {
            			String col = listCompatibleCols.get(indexOfFindingsToBeUsed);
	            		warningConsolidator.saveWarning(strWarningIfMoreThanOneAvailable.replace(
	            				"%COLUMN_NAME%", col == null ? "null" : col));
            		}
            	} 
            }
            
            // Generate an exception dialog for the user, if no column fits
            else if (strErrorIfNotFound != null) {
                throw new InvalidSettingsException(strErrorIfNotFound);
            }
        }
        else {
        	bRet = true;
        }
        
        return bRet;
    }
    
    /**
     * Checks based on the passed in table specification if the column with 
     * the name represented in the passed in settings model exists and if it has the
     * proper value class assigned. 
     * 
     * @param inSpec A table specification containing column specifications. Must not be null.
     * @param settingColumn A setting model standing for a column name. Must not be null.
     * @param valueClass The desired value class for the column compatibility check. Can be null
     * 		to avoid the check.
     * @param strErrorIfNotSet If not set to null, this will be used as error message when
     * 		no column name has been set yet (is null). Set it
     * 		to null, if no exception shall be thrown by this method.
     * @param strErrorIfNotFound If not set to null, this will be used as error message when
     * 		no column was found matching the passed in name. Set it
     * 		to null, if no exception shall be thrown by this method.
     * 		You may use the placeholder %COLUMN_NAME%, which
     * 		will be replaced with the concrete column name that was not found.
     * 
     * @return True, if the column name was found. False otherwise.
     * 
     * @throws InvalidSettingsException Thrown, if the last parameter is not null, and
     * 		if the column could not be found or has is not compatible with the necessary
     * 		data type (value class). Also thrown, if the second last parameter is
     * 		not null, and if the column name had not been set yet.
     */
    public static boolean checkColumnExistence(final DataTableSpec inSpec, 
    		final SettingsModelString settingColumn, final Class<? extends DataValue> valueClass,  
    		final String strErrorIfNotSet, final String strErrorIfNotFound) 
		throws InvalidSettingsException {
     	
     	List<Class<? extends DataValue>> listValueClasses = 
			new ArrayList<Class<? extends DataValue>>();
		listValueClasses.add(valueClass);

		return checkColumnExistence(inSpec, settingColumn, listValueClasses,  
    			strErrorIfNotSet, strErrorIfNotFound);
    }
    
    /**
     * Checks based on the passed in table specification if the column with 
     * the name represented in the passed in settings model exists and if it has the
     * proper value class assigned. 
     * 
     * @param inSpec A table specification containing column specifications. Must not be null.
     * @param settingColumn A setting model standing for a column name. If a 
     * 		SettingModelColumnName model is passed in, the method will also return
     * 		true, if the option useRowID is checked. Must not be null.
     * @param listValueClasses A list of desired value classes for the column compatibility check. 
     * 		Can be null to avoid the check.
     * @param strErrorIfNotSet If not set to null, this will be used as error message when
     * 		no column name has been set yet (is null). Set it
     * 		to null, if no exception shall be thrown by this method.
     * @param strErrorIfNotFound If not set to null, this will be used as error message when
     * 		no column was found matching the passed in name. Set it
     * 		to null, if no exception shall be thrown by this method.
     * 		You may use the placeholder %COLUMN_NAME%, which
     * 		will be replaced with the concrete column name that was not found.
     * 
     * @return True, if the column name was found. False otherwise.
     * 
     * @throws InvalidSettingsException Thrown, if the last parameter is not null, and
     * 		if the column could not be found or has is not compatible with the necessary
     * 		data type (value class). Also thrown, if the second last parameter is
     * 		not null, and if the column name had not been set yet.
     */
    public static boolean checkColumnExistence(final DataTableSpec inSpec, 
    		final SettingsModelString settingColumn, 
    		final List<Class<? extends DataValue>> listValueClasses,  
    		final String strErrorIfNotSet, final String strErrorIfNotFound) 
		throws InvalidSettingsException {
    	boolean bRet = false;
    	
    	// Pre-checks
    	if (inSpec == null) {
    		throw new IllegalArgumentException("Input table spec must not be null.");
    	}
    	if (settingColumn == null) {
    		throw new IllegalArgumentException("Settings model for column name must not be null.");
    	}

    	// Consider that the user selected to use the row id as column
    	if (settingColumn instanceof SettingsModelColumnName && 
    		((SettingsModelColumnName)settingColumn).useRowID()) {
    		bRet = true;
    	}
    	
    	// Process other column names
    	else {
	    	String strColumnName = settingColumn.getStringValue();
	        
	    	// Check, if we have no setting yet
	    	if (strColumnName == null) {
	    		if (strErrorIfNotSet != null) {
	    			throw new InvalidSettingsException(strErrorIfNotSet);
	    		}
	    	}
	
	    	// Check, if the column name exists
	    	else if (inSpec.containsName(strColumnName)) {
	    		// Perform an additional type check, if requested
	    		if (listValueClasses != null && !listValueClasses.isEmpty()) {
	    			for (Class<? extends DataValue> valueClass : listValueClasses) {
		    			if (inSpec.getColumnSpec(strColumnName).getType().isCompatible(valueClass)) {
		    				bRet = true;
		    				break;
		    			}
	    			}
	    		}
	    		else {
	    			bRet = true;
	    		}
	        }
	       	
	    	// Not found
	        if (!bRet && strErrorIfNotFound != null) {
	        	throw new InvalidSettingsException(strErrorIfNotFound.replace(
	                	"%COLUMN_NAME%", strColumnName == null ? "null" : strColumnName));
	        }    
    	}
 
    	return bRet;
    }

    /**
     * Auto guesses based on the passed in table specification the name of a column based
     * on its data value type. It only changes the specified settings object, if there
     * are no settings yet.
     * 
     * @param inSpec A table specification containing column specifications, which shall
     * 		be considered in the uniqueness check. Must not be null.
     * @param arrMoreColumnNames Array of additional column names, which would exist in 
     * 		the new name space and should be considered for uniqueness check. Can be null.
     * 		It is also ok, if some elements in the array are null - they will be ignored.
     * @param arrExclColumnNames Array of column names to exclude from the uniqueness check,
     * 		e.g. because these columns will be removed. Can be null.
     * 		It is also ok, if some elements in the array are null - they will be ignored.
     * @param settingNewColumnName The setting model of the new column name. Must not be null.
     * @param strSuggestedName The suggested name for the new column. Must not be null.
     * 
     * @return Resulting name which is unique considering the passed in parameters. If
     * 		the column name was already set, it returns it but without checking it for
     * 		uniqueness. 
     */
    public static String autoGuessColumnName(final DataTableSpec inSpec, final String[] arrMoreColumnNames,
    		final String[] arrExclColumnNames, final SettingsModelString settingNewColumnName, 
    		String strSuggestedName) {
        
    	String result = strSuggestedName;
   	
    	// Pre-checks
    	if (inSpec == null) {
    		throw new IllegalArgumentException("Input table spec must not be null.");
    	}
    	if (settingNewColumnName == null) {
    		throw new IllegalArgumentException("Settings model for new column name must not be null.");
    	}
    	if (strSuggestedName == null) {
    		throw new IllegalArgumentException("Suggested name for new column name must not be null.");
    	}
    	
    	// Make the name unique and set it, if new column name is still empty
    	String strNewColumnName = settingNewColumnName.getStringValue();
        if (strNewColumnName == null || strNewColumnName.isEmpty()) {
        	// Create list of all existing names
        	List<String> listNames = createMergedColumnNameList(inSpec, arrMoreColumnNames, 
        			arrExclColumnNames);

        	// Unify the name
        	int uniquifier = 1;
            
            while (listNames.contains(result)) {
                result = strSuggestedName + " (#" + uniquifier + ")";
                uniquifier++;
            }

            settingNewColumnName.setStringValue(result);
        }
        else {
        	result = settingNewColumnName.getStringValue();
        }
        
        return result;
    }
    
    /**
     * Determines based on the passed in table specification and column names, if
     * the specified column name would cause a name conflict. 
     * 
     * @param inSpec A table specification containing column specifications, which shall
     * 		be considered in the uniqueness check. Must not be null.
     * @param arrMoreColumnNames Array of additional column names, which would exist in 
     * 		the new name space and should be considered for uniqueness check. Can be null.
     * 		It is also ok, if some elements in the array are null - they will be ignored.
     * @param arrExclColumnNames Array of column names to exclude from the uniqueness check,
     * 		e.g. because these columns will be removed. Can be null.
     * 		It is also ok, if some elements in the array are null - they will be ignored.
     * @param settingNewColumnName The setting model for the name of a new column that shall 
     * 		be checked for uniqueness. Must not be null.
     * @param strErrorIfNotSet If not set to null, this will be used as error message when
     * 		no column name has been set yet (is null). Set it
     * 		to null, if no exception shall be thrown by this method.
     * @param strErrorIfNotUnique If not set to null, this will be used as error message when
     * 		no column was found matching the passed in name (name not unique). Set it
     * 		to null, if no exception shall be thrown by this method.
     * 		You may use the placeholder %COLUMN_NAME%, which
     * 		will be replaced with the concrete column name that was not found.
     * 
     * @return True, if the name is unique. False otherwise.
     * 
     * @throws InvalidSettingsException Thrown, if the last parameter is not null, and
     * 		if the column name is not unique. Also thrown, if the second last parameter is
     * 		not null, and if the column name had not been set yet.
     */
    public static boolean checkColumnNameUniqueness(final DataTableSpec inSpec, 
    		final String[] arrMoreColumnNames,
    		final String[] arrExclColumnNames, final SettingsModelString settingNewColumnName, 
    		String strErrorIfNotSet, String strErrorIfNotUnique) 
    	throws InvalidSettingsException {

    	if (settingNewColumnName == null) {
    		throw new IllegalArgumentException("Settings model for new column name must not be null.");
    	}
    	
    	return checkColumnNameUniqueness(inSpec, arrMoreColumnNames, arrExclColumnNames, 
    			settingNewColumnName.getStringValue(), strErrorIfNotSet, strErrorIfNotUnique);
    }
    
    /**
     * Determines based on the passed in table specification and column names, if
     * the specified column name would cause a name conflict. 
     * 
     * @param inSpec A table specification containing column specifications, which shall
     * 		be considered in the uniqueness check. Must not be null.
     * @param arrMoreColumnNames Array of additional column names, which would exist in 
     * 		the new name space and should be considered for uniqueness check. Can be null.
     * 		It is also ok, if some elements in the array are null - they will be ignored.
     * @param arrExclColumnNames Array of column names to exclude from the uniqueness check,
     * 		e.g. because these columns will be removed. Can be null.
     * 		It is also ok, if some elements in the array are null - they will be ignored.
     * @param strNewColumnName The name of a new column that shall be checked for uniqueness. 
     * 		Can be null, if not set yet, but results in an exception if the strErrorIfNotSet
     * 		parameter is set.
     * @param strErrorIfNotSet If not set to null, this will be used as error message when
     * 		no column name has been set yet (is null). Set it
     * 		to null, if no exception shall be thrown by this method.
     * @param strErrorIfNotUnique If not set to null, this will be used as error message when
     * 		no column was found matching the passed in name (name not unique). Set it
     * 		to null, if no exception shall be thrown by this method.
     * 		You may use the placeholder %COLUMN_NAME%, which
     * 		will be replaced with the concrete column name that was not found.
     * 
     * @return True, if the name is unique. False otherwise.
     * 
     * @throws InvalidSettingsException Thrown, if the last parameter is not null, and
     * 		if the column name is not unique. Also thrown, if the second last parameter is
     * 		not null, and if the column name had not been set yet.
     */
    public static boolean checkColumnNameUniqueness(final DataTableSpec inSpec, 
    		final String[] arrMoreColumnNames,
    		final String[] arrExclColumnNames, final String strNewColumnName, 
    		String strErrorIfNotSet, String strErrorIfNotUnique) 
    	throws InvalidSettingsException {
    	
    	boolean bRet = true;
    	String strColumnNameToCheck = (strNewColumnName == null ? null : strNewColumnName.trim());
    	
    	// Pre-checks
    	if (inSpec == null) {
    		throw new IllegalArgumentException("Input table spec must not be null.");
    	}
    	
    	// Check, if we have no setting yet
     	if (strColumnNameToCheck == null || strColumnNameToCheck.isEmpty()) {
    		if (strErrorIfNotSet != null) {
    			throw new InvalidSettingsException(strErrorIfNotSet);
    		}
    	}

    	// Check, if the column name is not existing yet (means, it is unique)
    	else {
    		List<String> listColumnNames = createMergedColumnNameList(inSpec, 
    				arrMoreColumnNames, arrExclColumnNames);
    		
			// Column is not unique - throw an exception if requested
    		if (listColumnNames.contains(strColumnNameToCheck)) {
    			bRet = false; 
        		if (strErrorIfNotUnique != null) {
                	throw new InvalidSettingsException(strErrorIfNotUnique.replace(
                			"%COLUMN_NAME%", strColumnNameToCheck == null ? "null" : strColumnNameToCheck));
        		}
    		}
        }
 
    	return bRet;
    }
    
	/**
	 * Creates a unique column name based on the passed in newName. It considers
	 * existing columns names, which must be passed in as existing table spec and/or
	 * string collection. If the new name does not exist yet, it is simply returned. If it exists,
	 * the method appends " (#...)" an increasing number until a unique name
	 * has been found.
	 * 
	 * @param newColumnName The optimal new column name. Must not be null.
	 * @param tableSpec Existing table spec of a table that will be merged. Can be null.
	 * @param colNames Collection of existing column names. Can be null.
	 * 
	 * @return A unified column name based on the passed in new name. Never null.
	 */
    public static String makeColumnNameUnique(final String newColumnName, 
    		final DataTableSpec tableSpec, final Collection<String> colNames) {
    	if (newColumnName == null) {
    		throw new IllegalArgumentException("New name must not be null.");
    	}
    	
    	String uniqueName = newColumnName;
    	
        int uniquifier = 1;
        while ((tableSpec != null && tableSpec.containsName(uniqueName) ||
        	   (colNames != null && colNames.contains(uniqueName)))) {
        	uniqueName = newColumnName + " (#" + uniquifier + ")";
            uniquifier++;
        }
    	
    	return uniqueName;
    }    
    
    /**
     * Creates a list of strings taking all column names of the passed in table
     * specification, merging it with the second parameter array and removing all
     * occurrences of string passed in with the third parameter array.
     * 
     * @param inSpec A table specification. Must not be null.
     * @param arrMoreColumnNames Array of additional column names, which would exist in 
     * 		the new name space and should be considered for uniqueness check. Can be null.
     * 		It is also ok, if some elements in the array are null - they will be ignored.
     * @param arrExclColumnNames Array of column names to exclude from the uniqueness check,
     * 		e.g. because these columns will be removed. Can be null.
     * 		It is also ok, if some elements in the array are null - they will be ignored.

     * @return Merged list. 
     */
    public static List<String> createMergedColumnNameList(final DataTableSpec inSpec, 
    		final String[] arrMoreColumnNames, final String[] arrExclColumnNames) {
       	
    	// Pre-checks
    	if (inSpec == null) {
    		throw new IllegalArgumentException("Input table spec must not be null.");
    	}
     	
    	// Create list of all existing names
    	ArrayList<String> listNames = new ArrayList<String>();
    	// 1. Use names from the table spec
    	for (Iterator<DataColumnSpec> i = inSpec.iterator(); i.hasNext(); ) {
    		listNames.add(i.next().getName());
    	}
    	// 2. Use additional names which may come from additional settings (other new columns)
    	if (arrMoreColumnNames != null) {
        	for (String strAddName : arrMoreColumnNames) {
        		if (strAddName != null) {
        			listNames.add(strAddName);
        		}
        	}
    	}
    	// 3. Exclude names, which may get deleted during execution
    	if (arrExclColumnNames != null) {
        	for (String strExcludeName : arrExclColumnNames) {
        		if (strExcludeName != null) {
	        		while (listNames.remove(strExcludeName)) {
	        			// Empty by purpose - we just cycle until all matching names are gone
	        		}
        		}
        	}
    	}
    	
    	return listNames;
    }
    
    /**
     * Creates an array of table specifications retrieved from the passed
     * in array of tables.
     * 
     * @param arrTables Array of tables. Can be null.
     * 
     * @return Array of specifications of the passed in tables. Null, 
     * 		if null was passed in. Empty array, if an empty array was passed in.
     * 		Elements are null for optional tables, which don't exist.
     */
    public static DataTableSpec[] getTableSpecs(DataTable[] arrTables) {
    	DataTableSpec[] arrSpecs = null;
    	
    	if (arrTables != null) {
    		arrSpecs = new DataTableSpec[arrTables.length];
    		for (int i = 0; i < arrTables.length; i++) {
    			arrSpecs[i] = (arrTables[i] != null ? arrTables[i].getDataTableSpec() : null);
    		}
    	}
    	
    	return arrSpecs;
    }
    
    /**
     * Determines the enumeration value based on a string.
     * This string must match an enumeration name or toString() representation of the 
     * passed in enumeration class. If not, the default value will be used instead
     * and a warning will be logged.
     *
     * @param enumType Enumeration class. Must not be null.
     * @param valueAsString The new value to store.
     * @param defaultValue Default enumeration value, if string cannot be recognized as 
     * 		any valid enumeration value.
     * 
     * @return Enumeration value, if found. If not found, it will return the 
     * 		specified defaultValue. If null was passed in as string, it will return null.
     */
	public static <T extends Enum<T>> T getEnumValueFromString(Class<T> enumType, String valueAsString, T defaultValue) {
    	T retValue = null;
		
		if (valueAsString != null) {
    		try {
    			// First try: The normal "name" value of the enumeration
    			retValue = Enum.valueOf(enumType, valueAsString);
    		}
    		catch (Exception exc) {
    			// Second try: The toString() value of an enumeration value - this comes handy when using FlowVariables
    			for (T enumValue : enumType.getEnumConstants()) {
    				String strRepresentation = enumValue.toString();
    				if (valueAsString.equals(strRepresentation)) {
    					retValue = enumValue;
    					break;
    				}
    			}
    			
    			// Third case: Fallback to default value
    			if (retValue == null) {
	    			LOGGER.warn("Value '" + valueAsString + "' could not be selected. " +
	    					"It is unknown in this version. Using default value '" + defaultValue + "'.");
	    			retValue = defaultValue;
    			}
    		}
     	}
		
		return retValue;
    }    
	
	//
	// Constructor
	//
	
	/**
	 * This constructor serves only the purpose to avoid instantiation of this class. 
	 */
	private SettingsUtils() {
		// To avoid instantiation of this class.
	}	
}
