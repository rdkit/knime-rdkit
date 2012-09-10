/* 
 * This source code, its documentation and all related files
 * are protected by copyright law. All rights reserved.
 *
 * (C)Copyright 2011 by Novartis Pharma AG 
 * Novartis Campus, CH-4002 Basel, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
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
	public static String getContentFromResource(InputStream input) throws IOException {
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
    public static void prepareDirectory(File dir) throws IOException {
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
		catch (Exception e) {
			// See if the file name was specified without a URL protocol
			File fileTmp = new File(strFile);
			
			try {
				url = fileTmp.getAbsoluteFile().toURI().toURL();
			} 
			catch (MalformedURLException excMalformedUrl) {
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
							throw new InvalidSettingsException("Specified file cannot be written. " +
								"The name refers to an existing directory.");
						}
					}
				}
			} 
			catch (URISyntaxException excBadUri) {
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
	public static void close(InputStream in) {
		if (in != null) {
			try {
				in.close();
			}
			catch (IOException exc) {
				// Ignored by purpose
			}
		}
	}
	
	/**
	 * Convenience method to close a reader resource without throwing any exceptions.
	 * 
	 * @param in Reader resource. Can be null.
	 */
	public static void close(Reader in) {
		if (in != null) {
			try {
				in.close();
			}
			catch (IOException exc) {
				// Ignored by purpose
			}
		}
	}
	
	/**
	 * Convenience method to close an output stream without throwing any exceptions.
	 * 
	 * @param out Output stream. Can be null.
	 */
	public static void close(OutputStream out) {
		if (out != null) {
			try {
				out.close();
			}
			catch (IOException exc) {
				// Ignored by purpose
			}
		}
	}	
	
	/**
	 * Convenience method to close a writer resource without throwing any exceptions.
	 * 
	 * @param out Writer resource. Can be null.
	 */
	public static void close(Writer out) {
		if (out != null) {
			try {
				out.close();
			}
			catch (IOException exc) {
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
