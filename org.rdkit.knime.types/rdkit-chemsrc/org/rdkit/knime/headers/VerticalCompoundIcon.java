/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2013-2023
 * Novartis Pharma AG, Switzerland
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
package org.rdkit.knime.headers;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.SwingConstants;

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
		this(topIcon, bottomIcon, SwingConstants.LEFT, 2);
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
		case SwingConstants.LEFT:
		case SwingConstants.CENTER:
		case SwingConstants.RIGHT:
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
			final int topWidth = m_topIcon.getIconWidth();
			final int bottomWidth = m_bottomIcon.getIconWidth();
			final int maxWidth = Math.max(topWidth, bottomWidth);
			final int topHeight = m_topIcon.getIconHeight();

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
	private int getAlignedCoord(final int coord, final int iMaxTotalDimension, final int iPartDimension) {
		int iAlignedCoord = coord; // Default is LEFT

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