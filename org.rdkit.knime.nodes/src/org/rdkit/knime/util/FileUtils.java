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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import org.knime.core.node.InvalidSettingsException;

/**
 * This utility class contains convenience methods to work with file data.
 * 
 * @author Manuel Schwarze
 */
public class FileUtils {

	/**
	 * Reads in the specified resource text file and returns it as string.
	 * 
	 * @param caller The caller of this method. The resource will be inquired based on the
	 * 		class of the caller. Can be null to use FileUtils class instead.
	 * @param urlResource Resource to read. Behind it must be a text file for valid results.
	 * 		Must not be null.
	 * 
	 * @return Read string from resource file or passed in value, if not a resource file pointer.
	 */
	public static String getContentFromResource(final Object caller, final String strResourceName) throws IOException {
		String strRet = null;

		// Check, if a file exists - however, we will reread the file every time it is accessed to be flexible
		final URL url = (caller == null? new FileUtils() : caller).getClass().getResource(strResourceName);
		strRet = getContentFromResource(url);

		return strRet;
	}

	/**
	 * Reads in the specified resource text file and returns it as string.
	 * 
	 * @param urlResource Resource to read. Behind it must be a text file for valid results. Must not be null.
	 * 
	 * @return File/resource content.
	 * 
	 * @throws IOException Thrown, if the file could not be read or if null was passed in.
	 */
	public static String getContentFromResource(final URL urlResource) throws IOException {
		if (urlResource == null) {
			throw new IOException("Resource not found.");
		}

		final URLConnection conn = urlResource.openConnection();
		return getContentFromResource(conn.getInputStream());
	}

	/**
	 * Reads in the specified text from the specified input stream and returns it as string.
	 * The input stream gets closed at the end.
	 * 
	 * @param input Input stream to read. Behind it must be a text file for valid results.
	 * 		Must not be null.
	 * 
	 * @return Content as string.
	 * 
	 * @throws IOException Thrown, if the input stream could not be read or if null was passed in.
	 */
	public static String getContentFromResource(final InputStream input) throws IOException {
		if (input == null) {
			throw new IOException("Resource not found.");
		}

		String strContent = null;

		try
		{
			final StringBuilder stringBuilder = new StringBuilder(4096);
			final byte[] arrBuffer = new byte[4096];
			int iLen;
			while ((iLen = input.read(arrBuffer, 0, arrBuffer.length)) != -1) {
				stringBuilder.append(new String(arrBuffer, 0, iLen));
			}

			strContent = stringBuilder.toString();
		}
		finally {
			close(input);
		}

		return strContent;
	}

	/**
	 * Tries to create the specified directory, if it does not exist yet.
	 * 
	 * @param dir Directory to be created.
	 * 
	 * @throws IOException Thrown, if the directory does not exist and could not be created.
	 */
	public static void prepareDirectory(final File dir) throws IOException {
		if (dir.exists()) {
			if (!dir.isDirectory()) {
				throw new IOException("'" + dir + "' is not a directory. Cannot use it.");
			}
		}
		else if (!dir.mkdirs()) {
			throw new IOException("'" + dir + "' or one of its parent directories could not be created. Cannot use it.");
		}
	}

	/**
	 * This method tries to convert the specified string into a File object.
	 * The string may contain a file: URL or a pathname. It is trimmed before
	 * the conversion.
	 * 
	 * @param fileUrlOrPath The file path or a file: URL. Can be null or empty.
	 * 		In that case an InvalidSettingsException gets thrown.
	 * 
	 * @return File The converted File object, if successful. Never null.
	 * 
	 * @throws InvalidSettingsException
	 *      Thrown, if the specified file name could not be converted.
	 */

	public static File convertToFile(final String fileUrlOrPath,
			final boolean bCheckReadAccess, final boolean bCheckWriteAccess)
					throws InvalidSettingsException {

		File fileConverted = null;
		String strFile = fileUrlOrPath;

		if (strFile == null) {
			throw new InvalidSettingsException("No file name specified.");
		}

		strFile = strFile.trim();

		if (strFile.isEmpty()) {
			throw new InvalidSettingsException("No file name specified.");
		}

		URL url;

		try {
			url = new URL(strFile);
		}
		catch (final Exception e) {
			// See if the file name was specified without a URL protocol
			final File fileTmp = new File(strFile);

			try {
				url = fileTmp.getAbsoluteFile().toURI().toURL();
			}
			catch (final MalformedURLException excMalformedUrl) {
				throw new InvalidSettingsException("Invalid file name URL: "
						+ excMalformedUrl.getMessage(), excMalformedUrl);
			}
		}

		if ("file".equals(url.getProtocol())) {
			try {
				fileConverted = new File(url.toURI());

				// Perform some checks for future read operations
				if (bCheckReadAccess) {
					// Checks, if the file denotes a real file
					if (!fileConverted.isFile()) {
						throw new InvalidSettingsException("Specified file doesn't exist.");
					}

					// Checks, if we can read from the file
					if (!fileConverted.canRead()) {
						throw new InvalidSettingsException("Specified file cannot be read.");
					}
				}

				// Perform some checks for future write operations
				if (bCheckWriteAccess) {
					if (fileConverted.exists()) {
						// Checks, if the file denotes a directory rather than a file
						if (fileConverted.isDirectory()) {
							throw new InvalidSettingsException("Specified file cannot be written. " +
									"The name refers to an existing directory.");
						}

						// Checks, if the file exists already and cannot be overridden
						else if (!fileConverted.canWrite()) {
							throw new InvalidSettingsException("Specified file cannot be overwritten.");
						}
					}
				}
			}
			catch (final URISyntaxException excBadUri) {
				throw new InvalidSettingsException("Invalid file name URI: "
						+ excBadUri.getMessage(), excBadUri);
			}
		}
		else {
			throw new InvalidSettingsException("Invalid file name '" + fileUrlOrPath + "'");
		}

		return fileConverted;
	}

	/**
	 * Convenience method to close an input stream without throwing any exceptions.
	 * 
	 * @param in Input stream. Can be null.
	 */
	public static void close(final InputStream in) {
		if (in != null) {
			try {
				in.close();
			}
			catch (final IOException exc) {
				// Ignored by purpose
			}
		}
	}

	/**
	 * Convenience method to close a reader resource without throwing any exceptions.
	 * 
	 * @param in Reader resource. Can be null.
	 */
	public static void close(final Reader in) {
		if (in != null) {
			try {
				in.close();
			}
			catch (final IOException exc) {
				// Ignored by purpose
			}
		}
	}

	/**
	 * Convenience method to close an output stream without throwing any exceptions.
	 * 
	 * @param out Output stream. Can be null.
	 */
	public static void close(final OutputStream out) {
		if (out != null) {
			try {
				out.close();
			}
			catch (final IOException exc) {
				// Ignored by purpose
			}
		}
	}

	/**
	 * Convenience method to close a writer resource without throwing any exceptions.
	 * 
	 * @param out Writer resource. Can be null.
	 */
	public static void close(final Writer out) {
		if (out != null) {
			try {
				out.close();
			}
			catch (final IOException exc) {
				// Ignored by purpose
			}
		}
	}

	//
	// Constructor
	//

	/**
	 * This constructor serves only the purpose to avoid instantiation of this class.
	 */
	private FileUtils() {
		// To avoid instantiation of this class.
	}
}
