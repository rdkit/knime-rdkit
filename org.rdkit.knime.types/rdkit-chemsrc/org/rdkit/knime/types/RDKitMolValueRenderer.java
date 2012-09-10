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

import org.RDKit.ROMol;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.knime.base.data.xml.SvgProvider;
import org.knime.base.data.xml.SvgValueRenderer;
import org.knime.core.data.DataCell;
import org.knime.core.data.renderer.AbstractPainterDataValueRenderer;
import org.w3c.dom.svg.SVGDocument;

/**
 * This a renderer that draws nice 2D depictions of RDKit molecules.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @author Manuel Schwarze, Novartis
 */
public class RDKitMolValueRenderer extends AbstractPainterDataValueRenderer
        implements SvgProvider {
	
	// 
	// Constants
	//
	
    /** Serial number. */
	private static final long serialVersionUID = 8956038655901963406L;

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
    	m_bIsMissingCell = (value instanceof DataCell && ((DataCell)value).isMissing());
    	
        if (value instanceof RDKitMolValue) { 
        	// Try to render the cell
	        RDKitMolValue molCell = (RDKitMolValue)value;
            m_strSmiles = molCell.getSmilesValue();
	        ROMol mol = null;
	        Thread t = Thread.currentThread();
	        ClassLoader contextClassLoader = t.getContextClassLoader();
	        t.setContextClassLoader(getClass().getClassLoader());
	        
	        try {
	        	mol = molCell.readMoleculeValue();
		        String svg = mol.ToSVG(8,50);
		
		        String parserClass = XMLResourceDescriptor.getXMLParserClassName();
		        SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parserClass);
		
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
	        } 
	        catch (Exception ex) {
	        	// If conversion fails we set a null value, which will show up as error messgae
	            m_svgDocument = null;
	        	// Logging something here may swam the log files - not desired.
	        } 
	        finally {
	       		t.setContextClassLoader(contextClassLoader);
	        	if (mol != null) {
	        		mol.delete();
	        	}
	        }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "2D depiction";
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
        
        // Case 1: A missing cell
        if (m_bIsMissingCell) {
            g.setFont(MISSING_CELL_FONT);
            g.drawString("?", 2, 12);
        }
        
        // Case 2: A SVG structure is available
        else if (m_svgDocument != null) {
        	try {
        		SvgValueRenderer.paint(m_svgDocument, (Graphics2D)g, getBounds(), true);
        	}
        	catch (Exception excPainting) {
                g.setFont(NO_SVG_FONT);
                g.drawString("Painting failed for", 2, 14);
                g.setFont(SMILES_FONT);
                g.drawString(m_strSmiles, 2, 28);
        	}
        }      
        
        // Case 3: An error occurred in the RDKit
        else {
            g.setFont(NO_SVG_FONT);
            g.setColor(Color.red);
            g.drawString("2D depiction failed" + (m_strSmiles == null ? "" : " for"), 2, 14);
            if (m_strSmiles != null) {
                g.setFont(SMILES_FONT);
            	g.drawString(m_strSmiles, 2, 28);
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
}
