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
 * Merges two icons to one aligning them horizontally.
 * 
 * @author Manuel Schwarze
 */
public class HorizontalCompoundIcon implements Icon {

	//
	// Members
	//
	
	/** Icon at the left side. */
    private final Icon m_leftIcon;
	
	/** Icon at the right side. */
    private final Icon m_rightIcon;
	
	/** Alignment of the icons. Either SwingUtilities.TOP, CENTER or BOTTOM. */
    private final int m_iAlignment;
	
	/** Gap between icons. */
    private final int m_iGap;

    //
    // Constructor
    //
    
    /**
     * Creates a new horizontal compound icon combining the two specified icons
     * to one. Default alignment is SwingUtilities.TOP and a gap of 2.
     * 
     * @param leftIcon Left icon. Can be null.
     * @param rightIcon Right icon. Can be null.
     */
    public HorizontalCompoundIcon(final Icon leftIcon, final Icon rightIcon) {
        this(leftIcon, rightIcon, SwingUtilities.TOP, 2);
    }

    /**
     * Creates a new horizontal compound icon combining the two specified icons
     * to one. Default alignment is SwingUtilities.LEFT and a gap of 2.
     * 
     * @param leftIcon Left icon. Can be null.
     * @param rightIcon Right icon. Can be null.
     * @param iAlignment Alignment of the icons. Either SwingUtilities.TOP, 
     * 		CENTER or BOTTOM.
     * @param iGap Gap between the icons. Must be >= 0.
     */
    public HorizontalCompoundIcon(final Icon leftIcon, final Icon rightIcon, 
    		final int iAlignment, final int iGap) {
        m_leftIcon = leftIcon;
        m_rightIcon = rightIcon;
        
        switch (iAlignment) {
        	case SwingUtilities.TOP:
        	case SwingUtilities.CENTER:
        	case SwingUtilities.BOTTOM:
                m_iAlignment = iAlignment; 
                break;
            default:
            	throw new IllegalArgumentException("Bad alignment: Use TOP, CENTER and BOTTOM only.");
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
        if (m_leftIcon == null && m_rightIcon == null) {
            // nothing to paint
        } 
        else if (m_leftIcon == null) {
            m_rightIcon.paintIcon(c, g, x, y);
        } 
        else if (m_rightIcon == null) {
            m_leftIcon.paintIcon(c, g, x, y);
        } 
        else {
            int leftHeight = m_leftIcon.getIconHeight();
            int rightWidth = m_rightIcon.getIconHeight();
            int maxHeight = Math.max(leftHeight, rightWidth);
            int leftWidth = m_leftIcon.getIconWidth();
            
            m_leftIcon.paintIcon(c, g, x, getAlignedCoord(y, maxHeight, leftHeight));
            m_rightIcon.paintIcon(c, g, x + leftWidth + getGap(), getAlignedCoord(y, maxHeight, leftHeight));
        }
    }

    /**
     * Returns the alignment of the icons.
     * 
     * @return Alignment of the icons. Either SwingUtilities.TOP, 
     * 		CENTER or BOTTOM.
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

    

    /** {@inheritDoc} */
    @Override
    public int getIconWidth() {
        if (m_leftIcon == null && m_rightIcon == null) {
            return 0;
        } 
        else if (m_leftIcon == null) {
            return m_rightIcon.getIconWidth();
        } 
        else if (m_rightIcon == null) {
            return m_leftIcon.getIconWidth();
        } 
        else {
            return m_leftIcon.getIconWidth()
            	+ m_rightIcon.getIconWidth() + getGap();
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getIconHeight() {
        if (m_leftIcon == null && m_rightIcon == null) {
            return 0;
        } 
        else if (m_leftIcon == null) {
            return m_rightIcon.getIconHeight();
        } 
        else if (m_rightIcon == null) {
            return m_leftIcon.getIconHeight();
        } 
        else {
            return Math.max(m_leftIcon.getIconHeight(),
            		m_rightIcon.getIconHeight());
         }
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
    	int iAlignedCoord = coord; // Default is TOP
    	
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