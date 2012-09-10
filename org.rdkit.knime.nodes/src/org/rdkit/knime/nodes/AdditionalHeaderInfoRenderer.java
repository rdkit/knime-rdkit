/* 
 * This source code, its documentation and all related files
 * are protected by copyright law. All rights reserved.
 *
 * (C)Copyright 2011 by Novartis Pharma AG 
 * Novartis Campus, CH-4002 Basel, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 */
package org.rdkit.knime.nodes;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.JTable;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.tableview.ColumnHeaderRenderer;
import org.rdkit.knime.util.HorizontalCompoundIcon;
import org.rdkit.knime.util.LayoutUtils;
import org.rdkit.knime.util.VerticalCompoundIcon;

/**
 * This extension of the normal column header renderer is capable of 
 * rendering also additional information of column headers.
 * 
 * @see AdditionalHeaderInfo
 * 
 * @author Manuel Schwarze
 */
public class AdditionalHeaderInfoRenderer extends ColumnHeaderRenderer {
	
	//
	// Constants
	//
	
	/** Serial number. */
	private static final long serialVersionUID = -8244286663789284680L;
	
	/** Gap between icons and labels. */
	private final static int GAP = 2;
	
	//
	// Public Methods
	//
	
	@Override
	public synchronized Component getTableCellRendererComponent(
			JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int column) {
		Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
				row, column);
        
        setVerticalAlignment(BOTTOM); 

		// Check, if we render a header
		if (value instanceof DataColumnSpec) {
			
			// Determine, if additional information is available to be shown
			AdditionalHeaderInfo addInfo = new AdditionalHeaderInfo((DataColumnSpec)value);
			if (addInfo.isAvailable()) {
    	        // Change background
				setBackground(LayoutUtils.changeColor(getBackground(), 10));

				// Get cell height
				int widthCell = table.getColumnModel().getColumn(column).getWidth();
				int heightCell = table.getTableHeader().getHeight();

				// Manipulate header renderer
				// Calculate space for the label
				Icon iconTypeSort = getIcon();
				int widthLabel = widthCell - (iconTypeSort == null ? 0 : 
					(iconTypeSort.getIconWidth() + GAP));
				int heightLabel = (iconTypeSort == null ? getFontMetrics(getFont()).getHeight() : 
					Math.max(iconTypeSort.getIconHeight(), getFontMetrics(getFont()).getHeight()));
				Icon iconLabel = createTextIcon(iconTypeSort == null ? 0 : GAP, 12, widthLabel, heightLabel);
				
				// Create compound icon from icon and label
				Icon iconHeader = new HorizontalCompoundIcon(iconTypeSort, iconLabel);

				// Calculate space for additional info
				int widthAddInfo = widthCell;
				int heightAddInfo = heightCell - (iconHeader.getIconHeight() + GAP);
    	        Icon iconAddInfo = addInfo.createIconWrapper(table, column, widthAddInfo, heightAddInfo);

    	        // Create compound icon from additional info and header
    	        setIcon(new VerticalCompoundIcon(iconAddInfo, iconHeader, RIGHT, GAP));
			}
		}
  		
		return comp;
	}

	/**
	 * Creates an icon that shows the text that the renderer currently uses.
	 * This is necessary because the normal label the master renderer is based 
	 * on cannot be us for rendering the text as it is fully used to show
	 * an icon representation.
	 * 
	 * @param x Coordinate x for rendering.
	 * @param y Coordinate y for rendering.
	 * @param width Width of the rendering area.
	 * @param height Height of the rendering area.
	 * 
	 * @return Icon capable of rendering the header text.
	 */
	public Icon createTextIcon(final int xCoord, final int yCoord, final int width, final int height) {

		Icon iconLabel = new Icon() {
			
			@Override
			public void paintIcon(Component c, Graphics g, int x,
					int y) { 
				Graphics g2 = g.create(x, y, getIconWidth(), getIconHeight());
				g2.setFont(c.getFont());
				g2.setColor(c.getForeground());
				g2.drawString(getText(), xCoord, yCoord);
			}

			@Override
			public int getIconWidth() {
				return width;
			}
			
			@Override
			public int getIconHeight() {
				return height;
			}
		};	
		
		return iconLabel;
	}

};