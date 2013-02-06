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
package org.rdkit.knime.nodes;

import java.awt.Component;
import java.awt.Graphics;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.knime.chem.types.SmilesCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.rdkit.knime.util.SettingsUtils;

/**
 * Additional header information is a set of data that is attached to a 
 * column header specification. It gives a view the opportunity to
 * render this additional information into the header of a column.
 * This could be for instance a molecule structure, an image, etc.
 * The information for this class comes from the table column
 * specification properties. The following properties are currently
 * processed, if found:<br/>
 * 1. addInfoType: The type of the information defines what the information
 * value described. A handler can be registered for a type which knows
 * how to deal with a value of this type when it is time to render it.<br/>
 * 2. addInfoValue: The value of the information, which must be compatible
 * with the specified type (and the handler that handles the type). Due
 * to KNIME property definitions we are limited here to String values.<br/>
 * 3. addInfoInitHeight: Optional. The initial desired height for the
 * additional information. This is only evaluated, if a valid value can
 * be rendered.<br/>
 * More optional information that could influence the rendering process
 * might be added in the same style in the future.
 * 
 * @author Manuel Schwarze
 */
public final class AdditionalHeaderInfo {
	
	//
	// Constants
	//
	
	/** 
	 * The property key of a header specification that contains the additional 
	 * information type.
	 */
	public static final String HEADER_PROP_TYPE = "addInfoType";
	
	/** 
	 * The property key of a header specification that contains the additional 
	 * information value.
	 */
	public static final String HEADER_PROP_VALUE = "addInfoValue";
	
	/** 
	 * The property key of a header specification that contains the additional 
	 * information initial height (optional information).
	 */
	public static final String HEADER_PROP_INITIAL_HEIGHT = "addInfoInitHeight";
	
	//
	// Static Variables
	//
	
	private final static Map<String, AdditionalHeaderInfoHandler> g_mapHandlers = 
		new HashMap<String, AdditionalHeaderInfoHandler>();
	
	/** Registers default handlers for certain additional header information types. */
	static {
		registerHandler(new AdditionalHeaderInfoHandler() {

			/** Smiles column specification used to find correct renderer. */
			private final DataColumnSpec m_spec = 
				new DataColumnSpecCreator("Smiles", SmilesCell.TYPE).createSpec();
			
			/**
			 * {@inheritDoc} 
			 * This handler returns "Smiles".
			 */
			@Override
			public String getType() {
				return "Smiles";
			}
			
			/**
			 * {@inheritDoc} 
			 * This handler creates a SmilesCell from a Smiles string.
			 * Correctness is not checked.
			 */
			@Override
			public Object prepareValue(String value) {
				return (value == null ? null : new SmilesCell(value));
			}
						
			/**
			 * {@inheritDoc} 
			 * This handler returns the currently configured default
			 * renderer of a Smiles cell.
			 */
			@Override
			public TableCellRenderer getRenderer() {
				return SmilesCell.TYPE.getRenderer(m_spec);
			}
		});
	}
	
	//
	// Members
	//
	
	/** 
	 * The type of the additional header information. 
	 *
	 * @see #HEADER_PROP_TYPE
	 */
	private String m_strAddInfoType = null;

	/** 
	 * The value of the additional header information. 
	 *
	 * @see #HEADER_PROP_VALUE
	 */
	private String m_strAddInfoValue = null;

	/** 
	 * The preferred initial height of the additional header information. 
	 *
	 * @see #HEADER_PROP_INITIAL_HEIGHT
	 */
	private int m_iAddInfoInitialHeight = -1;
	
	//
	// Constructor
	//
	
	/** 
	 * Creates a new additional header information object by reading the different
	 * information from the specified column header specifications.
	 * If the passed in column specification is null, or does not contain
	 * additional header information, or there is no renderer found
	 * to deal with the information, then the method {@link #isAvailable()} returns 
	 * false.
	 * 
	 * @param colSpec Column specification. Can be null.
	 */
	public AdditionalHeaderInfo(DataColumnSpec colSpec) {
		if (colSpec != null) {
			DataColumnProperties props = colSpec.getProperties();

			m_strAddInfoType = props.getProperty(HEADER_PROP_TYPE);
			
			if (m_strAddInfoType != null) {
				m_strAddInfoValue = props.getProperty(HEADER_PROP_VALUE, "");
				
				String addInfoInitHeight = props.getProperty(HEADER_PROP_INITIAL_HEIGHT);
				
				if (addInfoInitHeight != null) {
					try {
						m_iAddInfoInitialHeight = Integer.valueOf(addInfoInitHeight);
					}
					catch (NumberFormatException exc) {
						// Gracefully ignored
					}		
				}
			}
		}
	}
	
	/**
	 * Creates a new additional header info object directly using the specified values.
	 * 
	 * @param strType Type of the additional information.
	 * @param strValue Value of the additional information.
	 * @param iInitialHeight Initial height (preferred size) for the column header.
	 * 		If not >= 0, it will be corrected to -1, which will use the default size.
	 */
	public AdditionalHeaderInfo(String strType, String strValue, int iInitialHeight) {
		m_strAddInfoType = strType;
		m_strAddInfoValue = strValue;
		m_iAddInfoInitialHeight = (iInitialHeight >= 0 ? iInitialHeight : -1);
	}
	
	//
	// Public Methods
	//
	
	/**
	 * Removes the additional header information that matches the type of
	 * this object from the specified column specification. Additional
	 * header information with a different type will not be touched.
	 * 
	 * @param spec Column specification from which we will remove the
	 * 		additional header information.
	 */
	public DataColumnSpec removeFromColumnSpec(DataColumnSpec spec) {
		DataColumnSpec specRet = spec;
		
		if (spec != null) {
			AdditionalHeaderInfo headerInfo = new AdditionalHeaderInfo(spec);
			
			// Check, if the addition header information contained in the
			// passed in spec has the same type as the current one
			if (headerInfo.isAvailable() && SettingsUtils.equals(headerInfo.getType(), getType())) {
				DataColumnProperties oldProps = spec.getProperties();
				DataColumnSpecCreator creator = new DataColumnSpecCreator(spec);
				Map<String, String> mapNewProps = new HashMap<String, String>();
				
				for (Enumeration<String> e = oldProps.properties(); e.hasMoreElements(); ) {
					String strPropKey = e.nextElement();
					
					// Add an old property only back to the new properties, if it is
					// not an additional value item
					if (!HEADER_PROP_TYPE.equals(strPropKey) && 
						!HEADER_PROP_VALUE.equals(strPropKey) && 
						!HEADER_PROP_INITIAL_HEIGHT.equals(strPropKey)) {
						mapNewProps.put(strPropKey, oldProps.getProperty(strPropKey));
					}
				}
				
				creator.setProperties(new DataColumnProperties(mapNewProps));
				specRet = creator.createSpec();
			}
		}
		
		return specRet;
	}
		
	/**
	 * Writes the additional header information of this instance into the
	 * passed in column spec creator overwriting already set properties.
	 * If there are other properties that shall be maintained, call instead
	 * {@link #writeInColumnSpec(DataColumnSpecCreator, DataColumnProperties)}.
	 * 
	 * @param colSpecCreator The column spec creator that shall receive the additional
	 * 		header information. Must not be null.
	 * 
	 * @return The properties object which has been set for the column spec creator.
	 *    	Returning it gives the caller the chance to add other properties again.
	 *    
	 * @see #writeInColumnSpec(DataColumnSpecCreator, DataColumnProperties)
	 */
	public DataColumnProperties writeInColumnSpec(final DataColumnSpecCreator colSpecCreator) {
		return writeInColumnSpec(colSpecCreator, null);
	}
	
	/**
	 * Writes the additional header information of this instance into the
	 * passed in column spec creator, adding them to already existing properties,
	 * if specified. The properties are set for the column spec creator.
	 * 
	 * @param colSpecCreator The column spec creator that shall receive the additional
	 * 		header information. Must not be null.
	 * @param propsExisting Optionally these existing column properties will be used
	 * 		as basis and the additional header information is merged in.
	 * 
	 * @return The properties object which has been set for the column spec creator.
	 *    	Returning it gives the caller the chance to add other properties again.
	 */
	public DataColumnProperties writeInColumnSpec(final DataColumnSpecCreator colSpecCreator, 
			final DataColumnProperties propsExisting) {
		if (colSpecCreator == null) {
			throw new IllegalArgumentException("Column spec creator must not be null.");
		}
		
		// Define properties with additional information
		final Map<String, String> newProps = new HashMap<String, String>();
		newProps.put(AdditionalHeaderInfo.HEADER_PROP_TYPE, m_strAddInfoType);
		newProps.put(AdditionalHeaderInfo.HEADER_PROP_VALUE, m_strAddInfoValue);
		if (m_iAddInfoInitialHeight >= 0) {
			newProps.put(AdditionalHeaderInfo.HEADER_PROP_INITIAL_HEIGHT, "" + m_iAddInfoInitialHeight);
		}
		
		DataColumnProperties newColumnProps;
		
		if (propsExisting != null) {
			newColumnProps = propsExisting.cloneAndOverwrite(newProps);
		}
		else {
			newColumnProps = new DataColumnProperties(newProps);
		}
		
		colSpecCreator.setProperties(newColumnProps);
		return newColumnProps;
	}
	
	/**
	 * Returns true, if there is additional header information available, 
	 * which can be rendered.
	 * 
	 * @return True, if this object contains valuable information. False otherwise.
	 */
	public boolean isAvailable() {
		return (m_strAddInfoValue != null && getRenderer() != null);
	}
	
	/**
	 * Returns the type of the additional header information.
	 * 
	 * @return Type of information. Null, if not set.
	 */
	public String getType() {
		return m_strAddInfoType;
	}
	
	/**
	 * Returns the string value of the additional header information.
	 * 
	 * @return Value of information. Null, if not set.
	 */
	public String getValue() {
		return m_strAddInfoValue;
	}
	
	/**
	 * Returns the initial height to be used for the additional 
	 * header information. This is only a preferred height value.
	 * It will not be guaranteed that the view takes it into account.
	 * 
	 * @return Initial height. -1, if not set.
	 */
	public int getInitialHeight() {
		return m_iAddInfoInitialHeight;
	}
	
	/**
	 * Converts the string value into an object (if necessary) that the
	 * renderer of the information can handle. This method invokes a
	 * registered handler, if one is found.
	 * 
	 * @return Object to render the additional information in a form
	 * 		that the renderer will understand or null, if no handler was found.
	 */
	public Object prepareValue() {
		AdditionalHeaderInfoHandler handler = getHandler(getType());
		return (handler == null ? null : handler.prepareValue(getValue()));
	}

	/**
	 * Returns a renderer for the information, if there is a registered
	 * handler found.
	 * 
	 * @return Renderer to render the additional information or null, 	
	 * 		if none was found, e.g. because no handler was registered.
	 */
	public TableCellRenderer getRenderer() {
		AdditionalHeaderInfoHandler handler = getHandler(getType());
		return (handler == null ? null : handler.getRenderer());
	}
	
	/**
	 * Creates an icon instance based on painting logic that the renderer of 
	 * the additional header information (see {@link #getRenderer()} provides.
	 * 
	 * @param table Table subject of rendering. Must not be null.
	 * @param column Column index in focus of rendering.
	 * @param width Width of the space that can be filled out with the additional
	 * 		header information.
	 * @param height Height of the space that can be filled out with the additional
	 * 		header information.
	 * 
	 * @return Icon wrapper with capability to paint the additional header 
	 * 		information. If no information is available or no renderer was
	 * 		found, it returns null.
	 */
	public Icon createIconWrapper(final JTable table, final int column, 
			final int width, final int height) {
		Icon iconAddInfo = null;

		if (isAvailable()) {
			
			// Create structure image
			TableCellRenderer renderer = getRenderer();

			if (renderer != null) {
				final Component compAddInfo = 
					renderer.getTableCellRendererComponent(table, prepareValue(), false, false, -1, column);
				compAddInfo.setSize(width, height);
				
				iconAddInfo = new Icon() {
					
					/**
					 * {@inheritDoc}
					 * This implementation calls the paint method of the component that
					 * the renderer that is retrieved from the registered handler returns.
					 */
					@Override
					public void paintIcon(Component c, Graphics g, int x, int y) {
						compAddInfo.paint(g);
					}
					
					/**
					 * {@inheritDoc}
					 */
					@Override
					public int getIconWidth() {
						return width;
					}
					
					/**
					 * {@inheritDoc}
					 */
					@Override
					public int getIconHeight() {
						return height;
					}
				};
			}
		}
		
		return iconAddInfo;
	}		
	
	//
	// Static Public Methods
	//
	
	/**
	 * Registers the specified handler to be used for rendering additional
	 * header information in capable table views. If a handler with the
	 * same type has been registered already it will be overwritten.
	 * 
	 * @param handler Additional header info handler to be registered.
	 * 		Can be null to do nothing.
	 */
	public static void registerHandler(AdditionalHeaderInfoHandler handler) {
		if (handler != null && handler.getType() != null) {
			g_mapHandlers.put(handler.getType(), handler);
		}
	}
	
	/**
	 * Unregisters a handler of the specified type. If no handler with the
	 * type has been registered it does nothing.
	 * 
	 * @param type Additional information type of the handler that shall
	 * 		be unregistered. Can be null to do nothing.
	 */
	public static void unregisterHandler(String type) {
		if (type != null) {
			g_mapHandlers.remove(type);
		}
	}
	
	/**
	 * Determines, if a handler for the specified type has been registered.
	 * 
	 * @param type Additional information type to check, if a handler exists.
	 * 
	 * @return True, if a handler was found that claims to be capable to
	 * 		handle the specified type. False otherwise.
	 */
	public static boolean canHandle(String type) {
		return g_mapHandlers.containsKey(type);
	}
	
	/**
	 * Returns the registered handler for the specified type or null,
	 * if none had been registered.
	 * 
	 * @param type Additional information type to be processed.
	 * 
	 * @return Handler for the specified type or null, if none had
	 * 		been registered.
	 */
	public static AdditionalHeaderInfoHandler getHandler(String type) {
		return g_mapHandlers.get(type);
	}	
}
