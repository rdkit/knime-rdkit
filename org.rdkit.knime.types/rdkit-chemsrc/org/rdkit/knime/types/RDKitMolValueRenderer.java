/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2022-2023
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
package org.rdkit.knime.types;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.StringReader;
import java.util.concurrent.locks.ReentrantLock;

import org.RDKit.Conformer;
import org.RDKit.Int_Pair;
import org.RDKit.Int_Point2D_Map;
import org.RDKit.Match_Vect;
import org.RDKit.MolDraw2DSVG;
import org.RDKit.MolDrawOptions;
import org.RDKit.MolSanitizeException;
import org.RDKit.Point2D;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.RDKit.RWMol;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.knime.base.data.xml.SvgProvider;
import org.knime.base.data.xml.SvgValueRenderer;
import org.knime.chem.types.MolValue;
import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmartsValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.renderer.AbstractDataValueRendererFactory;
import org.knime.core.data.renderer.AbstractPainterDataValueRenderer;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.util.LockedSupplier;
import org.rdkit.knime.RDKitTypesPluginActivator;
import org.rdkit.knime.types.preferences.RDKitDepicterPreferencePage;
import org.rdkit.knime.types.preferences.RDKitTypesPreferencePage;
import org.w3c.dom.svg.SVGDocument;

/**
 * This a renderer that draws nice 2D depictions of RDKit molecules.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @author Manuel Schwarze, Novartis
 * @author Paolo Tosco, Novartis
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

	/** The molecule to be rendered next. */
	private transient ROMol m_molecule;

	/** A special lock used for the interface. */
	private ReentrantLock m_reentrantLock = new ReentrantLock();

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected synchronized void setValue(final Object value) {
		// Reset values important for painting
		m_svgDocument = null;
		m_strSmiles = null;
		m_strError = null;
		m_bIsMissingCell = (value instanceof DataCell && ((DataCell)value).isMissing());

		if (m_molecule != null) {
			m_molecule.delete();
		}

		RDKitMolValue molCell = null;
		ROMol omol = null;
		boolean trySanitizing = true;
		boolean bNormalize = RDKitDepicterPreferencePage.isNormalizeDepictions();
		boolean bUseCoordGen = RDKitDepicterPreferencePage.isUsingCoordGen();
		boolean bComputeCoordinates = false;

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
		}
		catch (final Exception ex) {
			// If conversion fails we set a null value, which will show up as error messgae
			omol = null;
			// Logging something here may swamp the log files - not desired.
		}

		if (molCell != null) {
			m_strSmiles = molCell.getSmilesValue();
			omol = molCell.readMoleculeValue();
			trySanitizing = !omol.hasQuery();

			bComputeCoordinates = (omol.getNumConformers() == 0);
		}
		else {
			// See if we have a value that we can understand
			try {
				RWMol tmol = null;
				if (value instanceof SmilesValue) {
					String val = ((SmilesValue) value).getSmilesValue();
					tmol = RWMol.MolFromSmiles(val, 0, false);
					bComputeCoordinates = true;
				}
				else if (value instanceof SdfValue) {
					String val = ((SdfValue) value).getSdfValue();
					tmol = RWMol.MolFromMolBlock(val, false /* sanitize */, true /* removeHs */,
							RDKitTypesPreferencePage.isStrictParsingForRendering() /* strictParsing */);
					bComputeCoordinates = false; // We accept, if there is no conformer
				}
				else if (value instanceof MolValue) {
					String val = ((MolValue) value).getMolValue();
					tmol = RWMol.MolFromMolBlock(val, false /* sanitize */, true /* removeHs */,
							RDKitTypesPreferencePage.isStrictParsingForRendering() /* strictParsing */);
					bComputeCoordinates = false; // We accept, if there is no conformer
				}
				else if (value instanceof SmartsValue) {
					String val = ((SmartsValue) value).getSmartsValue();
					tmol = RWMol.MolFromSmarts(val);
					bComputeCoordinates = true;
					trySanitizing = false;
				}

				if (tmol != null) {
					// save a copy in case something goes badly wrong in the sanitization
					omol = new ROMol(tmol);
					if (trySanitizing) {
						try {
							RDKFuncs.sanitizeMol(tmol);
							omol.delete();
							omol = tmol;
						}
						catch (final Exception ex) {
							trySanitizing = false;
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
						if (tmol != null) {
							tmol.delete();
							tmol = null;
						}
					}
				}
			}
			catch (final Exception ex) {
				if (omol != null) {
					omol.delete();
				}
				omol = null;
			}
		}
		if (omol != null) {
			final Thread t = Thread.currentThread();
			final ClassLoader contextClassLoader = t.getContextClassLoader();
			t.setContextClassLoader(getClass().getClassLoader());

			try {
				RWMol mol = new RWMol(omol);
				if (trySanitizing) {
					try {
						RDKFuncs.prepareMolForDrawing(mol);
					}
					catch(final MolSanitizeException ex) {
						mol.delete();
						mol = new RWMol(omol);
						// Skip kekulization. If this still fails we throw up our hands
						RDKFuncs.prepareMolForDrawing(mol, false);
					}
				}
				else {
					// Skip kekulization
					RDKFuncs.prepareMolForDrawing(mol, false);
				}

				// If we draw molecules that did not have coordinates in their format (e.g. SMILES, SMARTS),
				// we will compute them either with CoordGen or native RDKit.
				// Instead, if the molecule already has coordinates, we only need to normalize
				// the scale to RDKit standards if normalization is requested in preferences
				if (bComputeCoordinates) {
					compute2DCoords(mol, bUseCoordGen, bNormalize);
				} else if (bNormalize && mol.getNumConformers() > 0) {
					mol.normalizeDepiction(-1, 0);
				}

				// Store the prepared molecule for drawing next
				m_molecule = mol;
			}
			catch (final Exception ex) {
				// If conversion fails we will not set the molecule, which will show up as error message later
				// Logging something here may swam the log files - not desired.
			}
			finally {
				t.setContextClassLoader(contextClassLoader);
				omol.delete();
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

		m_svgDocument = getSvg();

		// Set default color
		g.setColor(Color.black);

		// Case 1: A missing cell
		if (m_bIsMissingCell || m_strError != null) {
			g.setFont(MISSING_CELL_FONT);
			g.setColor(Color.red);
			if (m_strError != null) {
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

	@Override
	public LockedSupplier<SVGDocument> getSvgSupplier() {
		return new LockedSupplier<SVGDocument>(getSvg(), m_reentrantLock);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized SVGDocument getSvg() {
		if (m_molecule != null) {
			try {
				final MolDraw2DSVG molDrawing = new MolDraw2DSVG(-1, -1);

				// Apply config from preferences (since RDKit Types version 4.6.0), if available
				String strJsonConfig = RDKitDepicterPreferencePage.getJsonConfig();
				if (strJsonConfig != null) {
					RDKFuncs.updateDrawerParamsFromJSON(molDrawing, strJsonConfig);
				}

				MolDrawOptions opts = molDrawing.drawOptions();

				// We've already prepared the molecule appropriately, so don't try again
				opts.setPrepareMolsBeforeDrawing(false);

				// Apply old config, if no JSON config is provided (before RDKit Types version 4.6.0)
				if (strJsonConfig == null) {
					opts.setAddStereoAnnotation(true);
				}

				molDrawing.drawMolecule(m_molecule);
				molDrawing.finishDrawing();

				// Use flexicanvas
				String svg = molDrawing.getDrawingText();
				svg = svg.replaceAll("(width|height)(=[\"'])(\\d+px)([\"'])", "$1$2100%$4");

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
			}
			catch (final Exception ex) {
				// If conversion fails we set a null value, which will show up as error messgae
				m_svgDocument = null;
				// Logging something here may swam the log files - not desired.
			}
			finally {
				if (m_molecule != null) {
					m_molecule.delete();
				}
			}
		}

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

	//
	// Static Methods
	//

	/**
	 * Calls the compute2DCoords method on the passed in ROMol object (if not null)
	 * and passes default parameters, except the last one: forceRDKit will be set
	 * to true to overwrite the global setting of setPreferCoordGen, which is now
	 * set in {@link RDKitTypesPluginActivator#start(org.osgi.framework.BundleContext)}
	 * always to true. For backward-compatibility purposes this method can be called
	 * to have the same behavior like the ROMol.compute2DCoords(...) before the global
	 * setting was changed.
	 *
	 * @param mol Molecule to re-compute coordinates for. Can be null to do nothing.
	 *
	 * @return ID of the conformation added to the molecule containing the 2D coordinates.
	 *     -1, if it did not run properly, e.g. mol was null.
	 */
	public static int compute2DCoords(ROMol mol) {
		return compute2DCoords(mol, (Int_Point2D_Map)null, false, false);
	}

	/**
	 * Calls the compute2DCoords method on the passed in ROMol object (if not null)
	 * and passes default parameters except for the last boolean parameter, which
	 * controls, if the coordinate generation should happen with CoordGen or
	 * native RDKit. For this last parameter we use the negated bUseCoordGen parameter
	 * that is passed in.
	 *
	 * @param mol Molecule to re-compute coordinates for. Can be null to do nothing.
	 * @param bUseCoordGen Set to true to set forceRDKit parameter to false. And vice versa.
	 *
	 * @return ID of the conformation added to the molecule containing the 2D coordinates.
	 *     -1, if it did not run properly, e.g. mol was null.
	 */
	public static int compute2DCoords(ROMol mol, boolean bUseCoordGen) {
		return compute2DCoords(mol, (Int_Point2D_Map)null, bUseCoordGen, false);
	}

	/**
	 * Calls the compute2DCoords method on the passed in ROMol object (if not null)
	 * and passes default parameters except for the last boolean parameter, which
	 * controls, if the coordinate generation should happen with CoordGen or
	 * native RDKit. For this last parameter we use the negated bUseCoordGen parameter
	 * that is passed in.
	 *
	 * @param mol Molecule to re-compute coordinates for. Can be null to do nothing.
	 * @param bUseCoordGen Set to true to set forceRDKit parameter to false. And vice versa.
	 * @param bNormalize Set to true to trigger a canonical reorientation.
	 *
	 * @return ID of the conformation added to the molecule containing the 2D coordinates.
	 *     -1, if it did not run properly, e.g. mol was null.
	 */
	public static int compute2DCoords(ROMol mol, boolean bUseCoordGen, boolean bNormalize) {
		return compute2DCoords(mol, (Int_Point2D_Map)null, bUseCoordGen, bNormalize);
	}

	/**
	 * Calls the compute2DCoords method on the passed in ROMol object (if not null)
	 * and passes default parameters except for the last boolean parameter, which
	 * controls, if the coordinate generation should happen with CoordGen or
	 * native RDKit. For this last parameter we use the negated bUseCoordGen parameter
	 * that is passed in.
	 *
	 * @param mol Molecule to re-compute coordinates for. Can be null to do nothing.
	 * @param mapTemplate Coordinates to be used to align the first molecule with. Can be null to not align.
	 * @param bUseCoordGen Set to true to set forceRDKit parameter to false. And vice versa.
	 * @param bNormalize Set to true to trigger a canonical reorientation.
	 *
	 * @return ID of the conformation added to the molecule containing the 2D coordinates.
	 *     -1, if it did not run properly, e.g. mol was null.
	 */
	public static int compute2DCoords(ROMol mol, Int_Point2D_Map mapTemplate, boolean bUseCoordGen, boolean bNormalize) {
		int iRet = -1;

		if (mol != null) {
			// The defaults are used from documentation found on March 09, 2023 under this link:
			// https://github.com/rdkit/rdkit/blob/58b79c6f8e2581205007effcf53c6c3641393c07/Code/JavaWrappers/ROMol.i#L423
			iRet = (int)mol.compute2DCoords(mapTemplate /* coordMap */,
					false /* boolean canonOrient */,
				   true /* boolean clearConfs */,
				   0 /* long nFlipsPerSample */,
				   0 /* long nSamples */,
				   0 /* int sampleSeed */,
				   false /* boolean permuteDeg4Nodes */,
				   !bUseCoordGen /* boolean forceRDKit */);

			// When using CoordGen, we always rescale as the CoordGen scale is different
			// from RDKit. We leave the scale to 1.0 when using RDKit as rescaling is not
			// needed. Irrespective of the CoordGen preference, we canonicalize orientation
			// orienting the main molecule axis along the X axis when bNormalize is true.
			if (mol.getNumConformers() > 0) {
				mol.normalizeDepiction(-1, bNormalize ? 1 : 0, bUseCoordGen ? -1.0 : 1.0);
				if (bNormalize) {
					// canonicalization may flip the molecule around the Z axis,
					// so we recompute bond wedging to make sure the stereochemistry
					// is depicted correctly (KNIME-1692)
					mol.ClearSingleBondDirFlags();
					mol.WedgeMolBonds(mol.getConformer());
				}
			}
		}

		return iRet;
	}

	/**
	 * Calls the compute2DCoords method on the passed in ROMol object (if not null)
	 * and passes default parameters except for the last boolean parameter, which
	 * controls, if the coordinate generation should happen with CoordGen or
	 * native RDKit. For this last parameter we use the negated bUseCoordGen parameter
	 * that is passed in.
	 *
	 * @param mol Molecule to re-compute coordinates for. Can be null to do nothing.
	 * @param template SMARTS template to align the first molecule with. Can be null to not align.
	 * @param bUseCoordGen Set to true to set forceRDKit parameter to false. And vice versa.
	 * @param bNormalize Set to true to trigger a canonical reorientation.
	 *
	 * @return ID of the conformation added to the molecule containing the 2D coordinates.
	 *     -1, if it did not run properly, e.g. mol was null.
	 */
	public static int compute2DCoords(ROMol mol, ROMol template, boolean bUseCoordGen, boolean bNormalize) {
		int iRet = -1;

		if (mol != null) {
			Int_Point2D_Map mapTemplate = null;
			Match_Vect matchVect = null;
			Conformer conformer = null;

			try {
				if (template != null && template.getNumConformers() > 0) {
					matchVect = mol.getSubstructMatch(template);
					if (matchVect != null && !matchVect.isEmpty()) {
						mapTemplate = new Int_Point2D_Map();
						conformer = template.getConformer();
						long lSize = matchVect.size();
						for (int i = 0; i < lSize; i++) {
							Int_Pair pair = matchVect.get(i);
							mapTemplate.set(pair.getSecond(), new Point2D(conformer.getAtomPos(pair.getFirst())));
						}
					}
				}

				iRet = compute2DCoords(mol, mapTemplate, bUseCoordGen, bNormalize);
			}
			finally {
				if (conformer != null) {
					conformer.delete();
				}
				if (matchVect != null) {
					// We assume that the Int_Pair objects are being deleted with this call as well
					matchVect.delete();
				}
				if (mapTemplate != null) {
					// We assume that the Point2D objects are being deleted with this call as well
					mapTemplate.delete();
				}
			}
		}

		return iRet;
	}
}
