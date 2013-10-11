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

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.SwingConstants;

/**
 * Merges two icons to one aligning them horizontally.
 * 
 * @author Manuel Schwarze
 * @deprecated Since version 2.3.0 replaced through RDKit Types Plugin class
 * 		org.rdkit.knime.headers.HorizontalCompoundIcon.
 */
@Deprecated
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
		this(leftIcon, rightIcon, SwingConstants.TOP, 2);
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
		case SwingConstants.TOP:
		case SwingConstants.CENTER:
		case SwingConstants.BOTTOM:
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
			final int leftHeight = m_leftIcon.getIconHeight();
			final int rightWidth = m_rightIcon.getIconHeight();
			final int maxHeight = Math.max(leftHeight, rightWidth);
			final int leftWidth = m_leftIcon.getIconWidth();

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
	private int getAlignedCoord(final int coord, final int iMaxTotalDimension, final int iPartDimension) {
		int iAlignedCoord = coord; // Default is TOP

		switch (getAlignment()) {
		case SwingConstants.CENTER:
			iAlignedCoord = coord + (iMaxTotalDimension - iPartDimension) / 2;
			break;
		case SwingConstants.BOTTOM:
			iAlignedCoord = coord + iMaxTotalDimension - iPartDimension;
			break;
		}

		return iAlignedCoord;
	}
}