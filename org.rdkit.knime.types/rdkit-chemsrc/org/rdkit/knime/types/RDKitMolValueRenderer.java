/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *
 * History
 *   19.12.2010 (meinl): created
 */
package org.rdkit.knime.types;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.StringReader;

import org.RDKit.GenericRDKitException;
import org.RDKit.MolDraw2DSVG;
import org.RDKit.MolDrawOptions;
import org.RDKit.MolSanitizeException;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.RDKit.RWMol;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.knime.base.data.xml.SvgProvider;
import org.knime.base.data.xml.SvgValueRenderer;
import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.renderer.AbstractDataValueRendererFactory;
import org.knime.core.data.renderer.AbstractPainterDataValueRenderer;
import org.knime.core.data.renderer.DataValueRenderer;
import org.w3c.dom.svg.SVGDocument;

import org.knime.chem.types.MolValue;
import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmartsValue;
import org.knime.chem.types.SmilesValue;


/**
 * This a renderer that draws nice 2D depictions of RDKit molecules.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @author Manuel Schwarze, Novartis
 */
public class RDKitMolValueRenderer extends AbstractPainterDataValueRenderer
implements SvgProvider {

	/**
	 * Factory for {@link RDKitMolValueRenderer}.
	 */
	public static final class Factory extends AbstractDataValueRendererFactory {
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getDescription() {
			return DESCRIPTION;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public DataValueRenderer createRenderer(final DataColumnSpec colSpec) {
			return new RDKitMolValueRenderer();
		}
	}

	//
	// Constants
	//

	/** Serial number. */
	private static final long serialVersionUID = 8956038655901963406L;

	/** Description / Renderer name. */
	private static final String DESCRIPTION = "RDKit 2D depiction";

	/** The font used for drawing empty cells. */
	private static final Font MISSING_CELL_FONT = new Font("Helvetica", Font.PLAIN, 12);

	/** The font used for drawing error messages. */
	private static final Font NO_SVG_FONT = new Font("Helvetica", Font.ITALIC, 12);

	/** The font used for drawing Smiles in error conditions, if available. */
	private static final Font SMILES_FONT = new Font("Helvetica", Font.PLAIN, 12);

	//
	// Members
	//

	/** Flag to tell the painting method that the cell is a missing cell. */
	private boolean m_bIsMissingCell;

	/** Smiles value of the currently painted cell. Only used in error conditions. */
	private String m_strSmiles;

	/** An error string. Only used in error conditions. */
	private String m_strError;

	/** The SVG structure to paint, if it could be determined properly. */
	private SVGDocument m_svgDocument;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void setValue(final Object value) {
		// Reset values important for painting
		m_svgDocument = null;
		m_strSmiles = null;
		m_strError = null;
		m_bIsMissingCell = (value instanceof DataCell && ((DataCell)value).isMissing());

		RDKitMolValue molCell = null;
		ROMol omol = null;
		boolean trySanitizing = true;
		try {
			// We have an old plain RDKit Mol Value
			if (value instanceof RDKitMolValue) {
				molCell = (RDKitMolValue)value;
			}
	
			// We have a wrapped RDKit Mol Value (or an error)
			else if (value instanceof AdapterValue) {
				final AdapterValue adapter = (AdapterValue)value;
	
				try {
					if (adapter.getAdapterError(RDKitMolValue.class) != null) {
						m_bIsMissingCell = true;
						m_strError = adapter.getAdapterError(RDKitMolValue.class).getError();
					}
					else {
						molCell = adapter.getAdapter(RDKitMolValue.class);
					}
				} catch (final IllegalArgumentException ex) {
					// we land here if there's no adapter in place
					molCell = null;
				}
			}
			// We just have a missing cell (which might be caused by some error)
			else {
				m_bIsMissingCell = (value instanceof DataCell && ((DataCell)value).isMissing());
				if (value instanceof MissingCell) {
					m_strError = ((MissingCell)value).getError();
				}
			}
		} catch (final Exception ex) {
			// If conversion fails we set a null value, which will show up as error messgae
			omol = null;
			// Logging something here may swamp the log files - not desired.

		}
		if (molCell != null) {
			m_strSmiles = molCell.getSmilesValue();
			omol = molCell.readMoleculeValue();
		} else {
			// see if we have a value that we can understand
			try {
				RWMol tmol = null;
				if (value instanceof SmilesValue) {
		            String val = ((SmilesValue) value).getSmilesValue();
		            tmol = RWMol.MolFromSmiles(val,0,false);
		        }
				else if (value instanceof SdfValue) {
		            String val = ((SdfValue) value).getSdfValue();
		            tmol = RWMol.MolFromMolBlock(val,false);
		        }
				else if (value instanceof MolValue) {
		            String val = ((MolValue) value).getMolValue();
		            tmol = RWMol.MolFromMolBlock(val,false);
		        }
				else if (value instanceof SmartsValue) {
		            String val = ((SmartsValue) value).getSmartsValue();
		            tmol = RWMol.MolFromSmarts(val);
		            trySanitizing=false;
		        }
				if(tmol != null) {
					// save a copy in case something goes badly wrong in the sanitization
					omol = new ROMol(tmol);
					if(trySanitizing) {
						try {
							RDKFuncs.sanitizeMol(tmol);
							omol.delete();
							omol = tmol;
						} catch (final Exception ex) {
							trySanitizing=false;					
							tmol.delete();
							tmol = null;
						}
					}
					// don't put this in an "else", we want to execute it if sanitization fails above.
					if(!trySanitizing) {
						// do a minimal amount of sanitization so that we can draw things properly
						omol.updatePropertyCache(false);
						RDKFuncs.symmetrizeSSSR(omol);
						RDKFuncs.setHybridization(omol);
						tmol.delete();
					}
				}
			} catch (final Exception ex) {
				omol = null;
			}
		}
		if(omol != null) {
			// Try to render the cell
			final Thread t = Thread.currentThread();
			final ClassLoader contextClassLoader = t.getContextClassLoader();
			t.setContextClassLoader(getClass().getClassLoader());

			try {
				RWMol mol;
				mol = new RWMol(omol);
				if(trySanitizing) {
					try {
						RDKFuncs.prepareMolForDrawing(mol);
					} catch(final MolSanitizeException ex) {
						mol.delete();
						mol = new RWMol(omol);
						// skip kekulization. If this still fails we throw up our hands
						RDKFuncs.prepareMolForDrawing(mol,false);
					}
				} else {
					// skip kekulization
					RDKFuncs.prepareMolForDrawing(mol,false);
				}
				final MolDraw2DSVG molDrawing = new MolDraw2DSVG(300, 300);
				MolDrawOptions opts = molDrawing.drawOptions();
				// we've already prepared the molecule appropriately, so don't try again:
				opts.setPrepareMolsBeforeDrawing(false);
				opts.setAddStereoAnnotation(true);
				molDrawing.drawMolecule((ROMol)mol);
				molDrawing.finishDrawing();

				final String svg = molDrawing.getDrawingText();
				if(mol != omol){
					mol.delete();
				}
				molDrawing.delete();
				
				final String parserClass = XMLResourceDescriptor.getXMLParserClassName();
				final SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parserClass);

				/*
				 * The document factory loads the XML parser
				 * (org.apache.xerces.parsers.SAXParser), using the thread's context
				 * class loader. In KNIME desktop (and batch) this is correctly set, in
				 * the KNIME server the thread is some TCP-socket-listener-thread, which
				 * fails to load the parser class (class loading happens in
				 * org.xml.sax.helpers.XMLReaderFactory# createXMLReader(String) ...
				 * follow the call)
				 */
				m_svgDocument = f.createSVGDocument(null, new StringReader(svg));
				// remove xml:space='preserved' attribute because it causes atom
				// labels to be printed off their places
				m_svgDocument.getRootElement().removeAttributeNS(
						"http://www.w3.org/XML/1998/namespace", "space");
			}catch (final Exception ex) {
				// If conversion fails we set a null value, which will show up as error messgae
				m_svgDocument = null;
				// Logging something here may swam the log files - not desired.
			}
			finally {
				t.setContextClassLoader(contextClassLoader);
				if (omol != null) {
					omol.delete();
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return DESCRIPTION;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(90, 90);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void paintComponent(final Graphics g) {
		super.paintComponent(g);

		// Set default color
		g.setColor(Color.black);

		// Case 1: A missing cell
		if (m_bIsMissingCell || m_strError != null) {
			g.setFont(MISSING_CELL_FONT);
			if (m_strError != null) {
				g.setColor(Color.red);
				drawString(g, m_strError, 2, 12);
			}
			else {
				drawString(g, "?", 2, 12);
			}
		}

		// Case 2: A SVG structure is available
		else if (m_svgDocument != null) {
			try {
				SvgValueRenderer.paint(m_svgDocument, (Graphics2D)g, getBounds(), true);
			}
			catch (final Throwable excPainting) {
				if (excPainting instanceof ThreadDeath) {
					throw (ThreadDeath)excPainting;
				}
				g.setFont(NO_SVG_FONT);
				drawString(g, "Painting failed for", 2, 14);
				g.setFont(SMILES_FONT);
				drawString(g, m_strSmiles, 2, 28);
			}
		}

		// Case 3: An error occurred in the RDKit
		else {
			g.setFont(NO_SVG_FONT);
			g.setColor(Color.red);
			drawString(g, "2D depiction failed" + (m_strSmiles == null ? "" : " for"), 2, 14);
			if (m_strSmiles != null) {
				g.setFont(SMILES_FONT);
				drawString(g, m_strSmiles, 2, 28);
			}
			g.setColor(Color.black);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SVGDocument getSvg() {
		return m_svgDocument;
	}

	//
	// Private Methods
	//

	/**
	 * Draws a multiline string to the specified graphics context at the position (x;y).
	 * 
	 * @param g Graphics context. Can be null to do nothing.
	 * @param str String to be drawn. Can be null to do nothing.
	 * @param x X position.
	 * @param y Y position.
	 */
	private void drawString(final Graphics g, final String str, final int x, final int y) {
		if (g != null && str != null) {
			int iFontHeight = g.getFontMetrics().getHeight() - 2;
			if (iFontHeight < 1) {
				iFontHeight = 1;
			}
			int iOffset = 0;
			final String[] arrLines = str.split("\n");
			for (final String strLine : arrLines) {
				g.drawString(strLine, x, y + iOffset);
				iOffset += iFontHeight;
			}
		}
	}

}
