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
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.StringReader;

import org.RDKit.RDKFuncs;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.gvt.renderer.StaticRenderer;
import org.apache.batik.util.XMLResourceDescriptor;
import org.knime.base.data.xml.SvgProvider;
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

        /* The document factory loads the XML parser
         * (org.apache.xerces.parsers.SAXParser), using the thread's context
         * class loader. In KNIME desktop (and batch) this is correctly set,
         * in the KNIME server the thread is some TCP-socket-listener-thread,
         * which fails to load the parser class
         * (class loading happens in org.xml.sax.helpers.XMLReaderFactory#
         *   createXMLReader(String) ... follow the call)
         */
        Thread t = Thread.currentThread();
        ClassLoader contextClassLoader = t.getContextClassLoader();
        t.setContextClassLoader(getClass().getClassLoader());

        try {
            m_svgDocument = f.createSVGDocument(null, new StringReader(svg));
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

        paint(m_svgDocument, (Graphics2D)g, getBounds(), true);
    }


    // FIXME: removes this method and use the one from SvgValueRenderer once
    // KNIME 2.3.1 is released
    private static final UserAgent UA = new UserAgentAdapter();

    private static final RenderingHints R_HINTS = new RenderingHints(
            RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    private static void paint(final SVGDocument doc, final Graphics2D g,
            final Rectangle componentBounds, final boolean keepAspectRatio) {
        if ((componentBounds.getHeight() < 1)
                || (componentBounds.getWidth() < 1)) {
            return;
        }

        GVTBuilder gvtBuilder = new GVTBuilder();
        BridgeContext bridgeContext = new BridgeContext(UA);
        GraphicsNode gvtRoot = gvtBuilder.build(bridgeContext, doc);

        Rectangle2D svgBounds = gvtRoot.getBounds();
        if (svgBounds == null) {
            g.setFont(NO_SVG_FONT);
            g.drawString("Invalid SVG", 2, 14);
            return;
        }

        double scaleX = (componentBounds.getWidth() - 20) / svgBounds.getWidth();
        double scaleY =
                (componentBounds.getHeight() - 20) / svgBounds.getHeight();
        if (keepAspectRatio) {
            scaleX = Math.min(scaleX, scaleY);
            scaleY = Math.min(scaleX, scaleY);
        }

        AffineTransform transform = new AffineTransform();
        transform.scale(scaleX, scaleY);
        transform.translate(-svgBounds.getX(), -svgBounds.getY());

        StaticRenderer renderer = new StaticRenderer(R_HINTS, transform);
        renderer.setTree(gvtRoot);
        renderer.updateOffScreen((int)componentBounds.getWidth(),
                (int)componentBounds.getHeight());
        renderer.clearOffScreen();
        renderer.repaint(componentBounds);
        final BufferedImage image = renderer.getOffScreen();

        double heightDiff =
                componentBounds.getHeight() - scaleY * svgBounds.getHeight();

        double widthDiff =
                componentBounds.getWidth() - scaleX * svgBounds.getWidth();

        g.drawImage(image, (int)(widthDiff / 2), (int)(heightDiff / 2), null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SVGDocument getSvg() {
        return m_svgDocument;
    }
}
