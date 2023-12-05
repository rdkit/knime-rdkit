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
package org.rdkit.knime.util;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
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
	 *            "/org/rdkit/knime/types/images/loading.gif".
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
	 * Shows a message box to the user, if possible, with the specified message.
	 * 
	 * @param strTitle The title of the message box. Must not be null.
	 * @param strMsg Message to show. Must not be null.
	 * @param swtIcon SWT code (e.g. {@link SWT#ICON_ERROR}) or 0 to use no icon.
	 * @param bSynchronous True to run synchronous via display, false for an asynchronous run.
	 */
	public static void showMessage(final String strTitle, final String strMsg,
			int swtIcon, final boolean bSynchronous) {
		if (strMsg == null) {
			throw new IllegalArgumentException("Message must not be null.");
		}

		if (EclipseUtils.isRunningHeadless()) {
			LOGGER.info(strMsg);
		}
		else {
			Display display = PlatformUI.getWorkbench().getDisplay();
			if (display == null) {
				display = Display.getDefault();
			}

			final Runnable runnable = new Runnable() {

				/**
				 * Asynchronously opens up a message dialog 
				 */
				@Override
				public void run() {
					try {
						// We need a shell as parent for the confirmation dialog
						final Shell shell = getParentShell();

						if (shell != null) {
							// Inform user about changes and if KNIME
							// shall be restarted now
							final MessageBox msgBox = new MessageBox(shell,
									swtIcon | SWT.OK | SWT.PRIMARY_MODAL);
							msgBox.setText(strTitle);
							msgBox.setMessage(strMsg);
							msgBox.open();
						}
						else {
							LOGGER.debug("Unable to show message dialog. No shell found.");
						}
					}
					catch (final Exception exc) {
						LOGGER.debug(
								"Unable to show message dialog.",
								exc);
					}
				}
			};

			if (display != null) {
				if (bSynchronous) {
					display.syncExec(runnable);
				}
				else {
					display.asyncExec(runnable);
				}
			}
		}
	}
	
	/**
	 * Retrieves a shell object to be used as parent for modal SWT dialogs. Preferably, it
	 * uses the modal dialog shell provider of the workbench. If this 
	 * 
	 * @return Parent shell.
	 */
	public static Shell getParentShell() {
		Shell shell = null;
		
		try {
			shell = PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
		}
		catch (Exception exc) {
			LOGGER.debug("Unable to determine parent shell from modal dialog shell provider.", exc);
		}
		
		if (shell == null) {
			try {
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				if (window != null) {
					shell = window.getShell();
				}
			}
			catch (Exception exc) {
				LOGGER.debug("Unable to determine parent shell from active workbench window.", exc);
			}
		}
		
		if (shell == null) {
			try {
				shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
			}
			catch (Exception exc) {
				LOGGER.debug("Unable to determine parent shell from display.", exc);
			}
		}
		
		return shell;
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
