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

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.border.Border;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.node.tableview.ColumnHeaderRenderer;
import org.rdkit.knime.headers.HeaderProperty;
import org.rdkit.knime.headers.HeaderPropertyHandler;
import org.rdkit.knime.headers.HeaderPropertyHandlerRegistry;
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
 * @deprecated Since version 2.3.0 replaced through RDKit Types Plugin class
 * 		org.rdkit.knime.headers.AdditionalHeaderInfoRenderer.
 */
@Deprecated
public class AdditionalHeaderInfoRenderer extends ColumnHeaderRenderer {

	//
	// Constants
	//

	/** Serial number. */
	private static final long serialVersionUID = -8244286663789284680L;

	/** Gap between icons and labels. */
	private final static int GAP = 2;

	/** Empty border to be applied around the additional data to ensure a margin. */
	private final static Border EMPTY_BORDER = BorderFactory.createEmptyBorder(5, 5, 5, 5);

	//
	// Members
	//

	private final HeaderPropertyHandlerRegistry m_registry = HeaderPropertyHandlerRegistry.getInstance();

	//
	// Public Methods
	//

	@Override
	public synchronized Component getTableCellRendererComponent(
			final JTable table, final Object value, final boolean isSelected,
			final boolean hasFocus, final int row, final int column) {
		final Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
				row, column);

		setVerticalAlignment(BOTTOM);

		// Check, if we render a header
		if (value instanceof DataColumnSpec) {

			final HeaderPropertyHandler[] arrHandlers =
					m_registry.getHeaderPropertyHandlersForColumn((DataColumnSpec)value,
							m_registry.getColumnHeaderRenderers());

			// Determine, if additional information is available to be shown
			if (arrHandlers != null && arrHandlers.length > 0) {
				// Change background
				setBackground(LayoutUtils.changeColor(getBackground(), 10));

				// Get cell height
				final int widthCell = table.getColumnModel().getColumn(column).getWidth();
				final int heightCell = table.getTableHeader().getHeight();

				// Manipulate header renderer
				// Calculate space for the label
				final Icon iconTypeSort = getIcon();
				final int widthLabel = widthCell - (iconTypeSort == null ? 0 :
					(iconTypeSort.getIconWidth() + GAP));
				final int heightLabel = (iconTypeSort == null ? getFontMetrics(getFont()).getHeight() :
					Math.max(iconTypeSort.getIconHeight(), getFontMetrics(getFont()).getHeight()));
				final Icon iconLabel = createTextIcon(iconTypeSort == null ? 0 : GAP, 12, widthLabel, heightLabel);

				// Create compound icon from icon and label
				final Icon iconHeader = new HorizontalCompoundIcon(iconTypeSort, iconLabel);

				// Calculate space for additional info
				final int widthAddInfo = widthCell;
				final int heightAddInfo = heightCell - (iconHeader.getIconHeight() + GAP);
				final Icon iconAddInfo = createIconWrapper(table, column, widthAddInfo, heightAddInfo,
						arrHandlers[0], (DataColumnSpec)value);

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

		final Icon iconLabel = new Icon() {

			@Override
			public void paintIcon(final Component c, final Graphics g, final int x,
					final int y) {
				final Graphics g2 = g.create(x, y, getIconWidth(), getIconHeight());
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
			final int width, final int height, final HeaderPropertyHandler handler,
			final DataColumnSpec colSpec) {
		Icon iconAddInfo = null;

		// Create header property object
		final HeaderProperty headerProperty = handler.createHeaderProperty(colSpec);

		if (headerProperty != null) {
			final DataValueRenderer renderer = handler.getPreferredRenderer(headerProperty);

			if (renderer != null) {
				final Component compAddInfo =
						renderer.getTableCellRendererComponent(table,
								headerProperty, false, false, -1, column);
				compAddInfo.setSize(width, height);
				if (compAddInfo instanceof JComponent) {
					((JComponent)compAddInfo).setBorder(EMPTY_BORDER);
				}

				iconAddInfo = new Icon() {

					/**
					 * {@inheritDoc}
					 * This implementation calls the paint method of the component that
					 * the renderer that is retrieved from the registered handler returns.
					 */
					@Override
					public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
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

};