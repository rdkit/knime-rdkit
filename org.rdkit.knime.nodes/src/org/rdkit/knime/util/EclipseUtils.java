/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2014
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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.ViewPart;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.Bundle;

/**
 * This class contains utility methods to work with Eclipse functionality.
 * 
 * @author Manuel Schwarze
 */
public class EclipseUtils {

	//
	// Constants
	//

	/** The logger instance for console logging, if something goes wrong. */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(EclipseUtils.class);

	//
	// Public Methods
	//

	/**
	 * Creates an ImageData object used in Eclipse environments. It is based on the
	 * specified class and a resource path in the context of the specified class.
	 * 
	 * @param rootClass
	 *            The class which loads the image as resource.
	 * @param strResourcePath
	 *            Resource path (absolute or relative to the specified root
	 *            object). Must not be <code>null</code>. An absolute example:
	 *            "/org/rdkit/knime/nodes/highlighting/delete.png".
	 * @return
	 */
	public static ImageData loadImageData(final Class<?> rootClass,
			final String strResourcePath) {
		// Ensure the parameters are valid
		if (rootClass == null || strResourcePath == null) {
			throw new IllegalArgumentException(
					"Parameters rootClass and strResourcePath must not be null.");
		}

		ImageData imageData = null;
		final InputStream stream = rootClass.getResourceAsStream(strResourcePath);

		if (stream != null) {
			try {
				imageData = new ImageData(stream);
			}
			catch (final SWTException ex) {
				LOGGER.warn("Image resource '" + strResourcePath
						+ "' could not be loaded as ImageData. Ignored.", ex);
			}
			finally {
				try {
					stream.close();
				}
				catch (final IOException ex) {
					// Ignored by purpose
				}
			}
		}

		return imageData;
	}

	/**
	 * Determines if this plugin is currently running in a headless instance of KNIME.
	 * We need this to avoid calling GUI related functionality when running the
	 * plugin on a server.
	 */
	public static boolean isRunningHeadless() {
		boolean bHeadless = false;

		final Bundle bundleGui = Platform.getBundle("org.eclipse.ui");

		// Check first if the UI bundle was loaded
		if (bundleGui != null && bundleGui.getState() == Bundle.ACTIVE) {
			// Check further, if we have a workbench running
			// Note: We avoid this check to save memory and have checked first the bundle state
			bHeadless = !PlatformUI.isWorkbenchRunning();
		}
		else {
			bHeadless = true;
		}

		return bHeadless;
	}

	/**
	 * Hides the view with the specified id from the active workbench / window / page.
	 * 
	 * @param strViewId View id of the view to be hidden. Can be null.
	 * 
	 * @return True, if view was found and could be hidden. False otherwise.
	 */
	public static boolean hideView(final String strViewId) {
		boolean bRet = false;

		if (strViewId != null && !isRunningHeadless()) {
			final IWorkbench workbench = PlatformUI.getWorkbench();
			if (workbench != null) {
				final IWorkbenchWindow[] arrWindows = workbench.getWorkbenchWindows();
				if (arrWindows != null && arrWindows.length > 0) {
					for (final IWorkbenchWindow wnd : arrWindows) {
						final IWorkbenchPage[] arrPages = wnd.getPages();
						if (arrPages != null) {
							for (final IWorkbenchPage page : arrPages) {
								page.getViewReferences();
								final IViewReference ref = page.findViewReference(strViewId);
								if (ref != null) {
									page.hideView(ref);
									bRet |= true;
								}
							}
						}
						else {
							LOGGER.debug("View '" + strViewId + "' could not be hidden. No pages found (yet).");
						}
					}
				}
				else {
					LOGGER.debug("View '" + strViewId + "' could not be hidden. No windows found (yet).");
				}
			}
			else {
				LOGGER.debug("View '" + strViewId + "' could not be hidden. No workbench found (yet).");
			}
		}

		LOGGER.debug("View '" + strViewId + "' " + (bRet ? "has been hidden" : "could not be hidden"));

		return bRet;
	}

	/**
	 * Creates a label component that looks like a link and reacts on mouse clicks.
	 * When clicking it will open the Eclipse / KNIME preference dialog on the page
	 * with the specified ID.
	 * 
	 * @param strLabel Link text. Must not be null.
	 * @param strPreferenceId Preference page ID to be shown. Must not be null.
	 * @param callback Callback method to be called after the preference dialog has been closed.
	 * 		Can be null.
	 * 
	 * @return JLabel instance with activate mouse click listener.
	 */
	public static JLabel createPreferenceLink(final String strLabel, final String strPreferenceId,
			final Callback callback) {
		if (strLabel == null) {
			throw new IllegalArgumentException("Preference label text must not be null.");
		}
		if (strPreferenceId == null) {
			throw new IllegalArgumentException("Preference Page ID must not be null.");
		}

		final JLabel lbPreferenceLink = new JLabel(strLabel, SwingConstants.LEFT);
		lbPreferenceLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lbPreferenceLink.setForeground(Color.blue);

		@SuppressWarnings("unchecked")
		final
		Map<TextAttribute, Object> attributes = (Map<TextAttribute, Object>)lbPreferenceLink.getFont().getAttributes();
		attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_ONE_PIXEL);
		lbPreferenceLink.setFont(lbPreferenceLink.getFont().deriveFont(attributes));

		lbPreferenceLink.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				// Open the preferences dialog
				final Display d = Display.getDefault();
				// Run in UI thread
				d.syncExec(new Runnable() {
					@Override
					public void run() {
						final PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(d.getActiveShell(),
								strPreferenceId, new String[] { strPreferenceId }, null);
						if (dialog.open() == Window.OK && callback != null) {
							callback.callback();
						}
					}
				});
			}
		});

		return lbPreferenceLink;
	}

	/**
	 * Creates a label component that looks like a link and reacts on mouse clicks.
	 * When clicking it will open the specified view.
	 * 
	 * @param strLabel Link text. Must not be null.
	 * @param strPreferenceId Preference page ID to be shown. Must not be null.
	 * @param callback Callback method to be called after the preference dialog has been closed.
	 * 		Can be null.
	 * 
	 * @return JLabel instance with activate mouse click listener.
	 */
	public static JLabel createOpenViewLink(final String strLabel, final String strViewId) {
		if (strLabel == null) {
			throw new IllegalArgumentException("Open view label text must not be null.");
		}
		if (strViewId == null) {
			throw new IllegalArgumentException("View ID must not be null.");
		}

		final JLabel lbOpenViewLink = new JLabel(strLabel, SwingConstants.LEFT);
		lbOpenViewLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lbOpenViewLink.setForeground(Color.blue);

		@SuppressWarnings("unchecked")
		final
		Map<TextAttribute, Object> attributes = (Map<TextAttribute, Object>)lbOpenViewLink.getFont().getAttributes();
		attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_ONE_PIXEL);
		lbOpenViewLink.setFont(lbOpenViewLink.getFont().deriveFont(attributes));

		lbOpenViewLink.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				// Open the preferences dialog
				final Display d = Display.getDefault();
				// Run in UI thread
				d.syncExec(new Runnable() {
					@Override
					public void run() {
						showView(null, strViewId);
					}
				});
			}
		});

		return lbOpenViewLink;
	}

	/**
	 * Shows an Eclipse view with the specified id in the specified page. If null
	 * is passed in it tries to use the currently active workbench page.
	 * 
	 * @param page Page to become the parent of the KNIME Explorer View.
	 * @param strViewId Id of the view to be shown.
	 * 
	 * @return KNIME Explorer View that got activated or null.
	 */
	public static ViewPart showView(IWorkbenchPage page, final String strViewId) {
		final ViewPart[] viewRet = new ViewPart[] { null };

		if (!EclipseUtils.isRunningHeadless()) {
			if (page == null) {
				final IWorkbench workbench = PlatformUI.getWorkbench();
				if (workbench != null) {
					final IWorkbenchWindow wnd = workbench.getActiveWorkbenchWindow();
					if (wnd != null) {
						page = wnd.getActivePage();
					}
				}
			}

			if (page != null) {
				final IWorkbenchPage pageFinal = page;
				final Display display = Display.getCurrent();
				if (display != null) {
					display.asyncExec(new Runnable() {
						@Override
						public void run() {
							try {
								LOGGER.debug("Activate KNIME View with ID " + strViewId);
								final IViewPart viewPart = pageFinal.showView(strViewId);
								if (viewPart instanceof ViewPart) {
									viewRet[0] = (ViewPart)viewPart;
								}
								else {
									LOGGER.debug("Activating KNIME View with ID " + strViewId + " may have failed. " +
											"It is not derived from the type ViewPart, but " +
											(viewPart != null ? viewPart.getClass().getName() : "null") + ".");
								}
							}
							catch (final PartInitException e) {
								LOGGER.debug("Activating KNIME View with ID " + strViewId + " failed.", e);
							}
						}
					});
				}
				else {
					LOGGER.debug("Activating KNIME View with ID " + strViewId + " failed - No display found.");
				}
			}
			else {
				LOGGER.debug("Activating KNIME View with ID " + strViewId + " failed - No page found.");
			}
		}

		return viewRet[0];
	}

	//
	// Constructor
	//

	/**
	 * This constructor serves only the purpose to avoid instantiation of this class.
	 */
	public EclipseUtils() {
		// To avoid instantiation of this class.
	}
}
