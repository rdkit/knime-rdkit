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
package org.rdkit.knime.util;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

/** 
 * Merges two icons to one aligning them vertically.
 * 
 * @author Manuel Schwarze
 */
public class VerticalCompoundIcon implements Icon {

	//
	// Members
	//
	
	/** Icon at the top. */
    private final Icon m_topIcon;
	
	/** Icon at the bottom. */
    private final Icon m_bottomIcon;
	
	/** Alignment of the icons. Either SwingUtilities.LEFT, CENTER or RIGHT. */
    private final int m_iAlignment;
	
	/** Gap between icons. */
    private final int m_iGap;

    //
    // Constructor
    //
    
    /**
     * Creates a new vertical compound icon combining the two specified icons
     * to one. Default alignment is SwingUtilities.LEFT and a gap of 2.
     * 
     * @param topIcon Top icon. Can be null.
     * @param bottomIcon Bottom icon. Can be null.
     */
    public VerticalCompoundIcon(final Icon topIcon, final Icon bottomIcon) {
        this(topIcon, bottomIcon, SwingUtilities.LEFT, 2);
    }

    /**
     * Creates a new vertical compound icon combining the two specified icons
     * to one. Default alignment is SwingUtilities.LEFT and a gap of 2.
     * 
     * @param topIcon Top icon. Can be null.
     * @param bottomIcon Bottom icon. Can be null.
     * @param iAlignment Alignment of the icons. Either SwingUtilities.LEFT, 
     * 		CENTER or RIGHT.
     * @param iGap Gap between the icons. Must be >= 0.
     */
    public VerticalCompoundIcon(final Icon topIcon, final Icon bottomIcon, 
    		final int iAlignment, final int iGap) {
        m_topIcon = topIcon;
        m_bottomIcon = bottomIcon;
        
        switch (iAlignment) {
        	case SwingUtilities.LEFT:
        	case SwingUtilities.CENTER:
        	case SwingUtilities.RIGHT:
                m_iAlignment = iAlignment; 
                break;
            default:
            	throw new IllegalArgumentException("Bad alignment: Use SwingUtilities.LEFT, CENTER and RIGHT only.");
        }
        
        if (iGap >= 0) {
        	m_iGap = iGap;
        }
        else {
        	throw new IllegalArgumentException("Bad gap: Must be a number >= 0.");
        }
    }
    
    //
    // Public Methods
    //

    /** {@inheritDoc} */
    @Override
    public void paintIcon(final Component c, final Graphics g,
            final int x, final int y) {
        if (m_topIcon == null && m_bottomIcon == null) {
            // nothing to paint
        } 
        else if (m_topIcon == null) {
            m_bottomIcon.paintIcon(c, g, x, y);
        } 
        else if (m_bottomIcon == null) {
            m_topIcon.paintIcon(c, g, x, y);
        } 
        else {
            int topWidth = m_topIcon.getIconWidth();
            int bottomWidth = m_bottomIcon.getIconWidth();
            int maxWidth = Math.max(topWidth, bottomWidth);
            int topHeight = m_topIcon.getIconHeight();
            
            m_topIcon.paintIcon(c, g, getAlignedCoord(x, maxWidth, topWidth), y);
            m_bottomIcon.paintIcon(c, g, getAlignedCoord(x, maxWidth, topWidth), y + topHeight + getGap());
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getIconWidth() {
        if (m_topIcon == null && m_bottomIcon == null) {
            return 0;
        } 
        else if (m_topIcon == null) {
            return m_bottomIcon.getIconWidth();
        } 
        else if (m_bottomIcon == null) {
            return m_topIcon.getIconWidth();
        } 
        else {
            return Math.max(m_topIcon.getIconWidth(),
                    m_bottomIcon.getIconWidth());
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getIconHeight() {
        if (m_topIcon == null && m_bottomIcon == null) {
            return 0;
        } 
        else if (m_topIcon == null) {
            return m_bottomIcon.getIconHeight();
        } 
        else if (m_bottomIcon == null) {
            return m_topIcon.getIconHeight();
        } 
        else {
            return m_topIcon.getIconHeight()
            + m_bottomIcon.getIconHeight() + getGap();
        }
    }

    /**
     * Returns the alignment of the icons.
     * 
     * @return Alignment of the icons. Either SwingUtilities.LEFT, 
     * 		CENTER or RIGHT.
     */
    public int getAlignment() {
    	return m_iAlignment;
    }
    
    /**
     * Returns the gap to be used between the icons.
     * 
     * @return Gap.
     */
    public int getGap() {
    	return m_iGap;
    }    
    
    //
    // Private Methods
    //
    
    /**
     * Calculates a coordinate based on component size(s) and alignment.
     * 
     * @param coord Coordinate value (left or top edge).
     * @param iMaxTotalDimension Maximal width or height of both combined icons.
     * @param iPartDimension Width or height of the icon to be layed out.
     */
    private int getAlignedCoord(int coord, int iMaxTotalDimension, int iPartDimension) {
    	int iAlignedCoord = coord; // Default is LEFT
    	
        switch (getAlignment()) {
        	case SwingUtilities.CENTER:
    			iAlignedCoord = coord + (iMaxTotalDimension - iPartDimension) / 2;
    			break;
	    	case SwingUtilities.BOTTOM:
	    		iAlignedCoord = coord + iMaxTotalDimension - iPartDimension;
				break;        	
        }
        
    	return iAlignedCoord;
    }
}