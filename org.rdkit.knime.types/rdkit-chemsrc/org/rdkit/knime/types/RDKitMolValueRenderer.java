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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.StringReader;

import org.RDKit.RDKFuncs;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.knime.base.data.xml.SvgProvider;
import org.knime.base.data.xml.SvgValueRenderer;
import org.knime.core.data.renderer.AbstractPainterDataValueRenderer;
import org.knime.core.node.NodeLogger;
import org.w3c.dom.svg.SVGDocument;

/**
 * This a renderer that draws nice 2D depictions of RDKit molecules.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class RDKitMolValueRenderer extends AbstractPainterDataValueRenderer
        implements SvgProvider {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(RDKitMolValueRenderer.class);

    private static final Font NO_SVG_FONT = new Font(Font.SANS_SERIF,
            Font.ITALIC, 12);

    private SVGDocument m_svgDocument;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setValue(final Object value) {
        if (!(value instanceof RDKitMolValue)) { // might be missing
            m_svgDocument = null;
            return;
        }

        RDKitMolValue mol = (RDKitMolValue)value;
        String svg = RDKFuncs.MolToSVG(mol.readMoleculeValue());

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
        Thread t = Thread.currentThread();
        ClassLoader contextClassLoader = t.getContextClassLoader();
        t.setContextClassLoader(getClass().getClassLoader());

        try {
            m_svgDocument = f.createSVGDocument(null, new StringReader(svg));
            // remove xml:space='preserved' attribute because it causes atom
            // labels to be printed off their places
            m_svgDocument.getRootElement().removeAttributeNS(
                    "http://www.w3.org/XML/1998/namespace", "space");
        } catch (Exception ex) {
            m_svgDocument = null;
            LOGGER.error("Could not render molecule", ex);
        } finally {
            t.setContextClassLoader(contextClassLoader);
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
        if (m_svgDocument == null) {
            g.setFont(NO_SVG_FONT);
            g.drawString("No 2D depiction available", 2, 14);
            return;
        }

        SvgValueRenderer.paint(m_svgDocument, (Graphics2D)g, getBounds(), true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SVGDocument getSvg() {
        return m_svgDocument;
    }
}
