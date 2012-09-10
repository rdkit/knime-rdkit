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
package org.rdkit.knime.nodes.fingerprintreadwrite;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

/**
 * This is the Utility implementation of FPS Reader/Writer. This class is
 * responsible for reading and parsing the FPS file, contains conversion logic
 * from Hex to Binary and vice versa
 * 
 * @author Sudip Ghosh
 */

public class FPSReadWriteUtil {

	//
	// Constants
	//
	
	/**
	 * Logger instance for logging purpose.
	 */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(FPSReadWriteUtil.class);

	private static final String BLANK = "";
	private static final String DELIMB = "\t";

	private static final String HEX_A = "a";
	private static final String HEX_B = "b";
	private static final String HEX_C = "c";
	private static final String HEX_D = "d";
	private static final String HEX_E = "e";
	private static final String HEX_F = "f";

	
	//
	// Public Methods
	//
	
	/**
	 * This function will validate the Fingerprint records and also populate
	 * num_bits if not present at header
	 * 
	 * @param fingStr
	 *            concerned fingerprint record to validate
	 * @param mapFpsHeaders
	 *            map object to contain the header part of FPS files
	 * @param setId
	 *            boolean to indicate fingerprint id as row id.
	 * @param setIdentifier
	 *            set object to check duplicacy of fingerpint key .
	 * @return boolean after successful or unsuccessful validation
	 * @throws Exception
	 *             while duplicate id found
	 */
	public static boolean fingerprintValidator(String fingStr,
			Map<String, String> mapFpsHeaders, boolean setId,
			Set<String> setIdentifier) throws Exception {
		boolean bRetStat = false;
		StringTokenizer st = new StringTokenizer(fingStr, DELIMB);

		if (st.hasMoreTokens() && st.countTokens() == 2) {
			String strVal = st.nextToken();
			String strKey = st.nextToken();

			if (strKey != null && strKey.trim().length() > 0) {
				if (strVal == null || strVal.trim().length() > 0) {
					bRetStat = true;
					if (mapFpsHeaders.get("num_bits") == null) {
						mapFpsHeaders.put("num_bits",
								Integer.toString(strVal.trim().length() * 4));
					} else if (strVal.trim().length() * 4 != Integer
							.parseInt(mapFpsHeaders.get("num_bits"))) {
						bRetStat = false;
					}
					if (!(setIdentifier.add(strKey)) && setId) {
						LOGGER.error("Duplicate Fingerprint records found.");
						throw new InvalidSettingsException(
								"Duplicate fingerprint ID found, processing stopped. Please uncheck the \"Set Row IDs\" checkbox to continue.");
					}

				}
			}

		}
		return bRetStat;
	}

	/**
	 * This method is responsible for conversion of HexEncoded fingerprints to
	 * Binary bits
	 * 
	 * @param strHexFing
	 *            Determines the hex encoded fingerprint record
	 * @return long[] Array to return the binary bits
	 * @throws Exception
	 *             Exception thrown during Hex to Binary conversion
	 */
	public static long[] convertBinaryFingerprintsFromHex(String strHexFing)
			throws Exception {
		LOGGER.debug("Entered convertBinaryFingerprintsFromHex with strVal "
				+ strHexFing);
		int iCount = 0;
		// finalk to hold the final resultant bits
		long arrFinalk[] = new long[strHexFing.trim().length() * 4];
		int iDecimalVal = 0;

		// traverse through each character of Fingerprint , processing 2 char at
		// a time , convert to decimal
		// value for each character and generate 4 binary bits per character
		for (int i = 0; i < strHexFing.trim().length(); i++) {
			if (i % 2 == 0) {
				String strHex2dig = strHexFing.substring(i, i + 2);

				for (int n = 1; n >= 0; n--) {
					char chHexPos = strHex2dig.charAt(n);

					if (chHexPos == 'a' || chHexPos == 'A') {
						iDecimalVal = 10;
					} else if (chHexPos == 'b' || chHexPos == 'B') {
						iDecimalVal = 11;
					} else if (chHexPos == 'c' || chHexPos == 'C') {
						iDecimalVal = 12;
					} else if (chHexPos == 'd' || chHexPos == 'D') {
						iDecimalVal = 13;
					} else if (chHexPos == 'e' || chHexPos == 'E') {
						iDecimalVal = 14;
					} else if (chHexPos == 'f' || chHexPos == 'F') {
						iDecimalVal = 15;
					} else
						iDecimalVal = Integer.parseInt(Character
								.toString(chHexPos));

					List<Integer> listK = new ArrayList<Integer>();
					int j = 0;
					while (iDecimalVal != 0) {
						int iRes = iDecimalVal % 2;
						listK.add(iRes);
						iDecimalVal = iDecimalVal / 2;
						j++;
					}
					int iSizeListK = listK.size();
					for (int l = 0; l < iSizeListK; l++) {
						arrFinalk[iCount++] = listK.get(l);
					}
					if (iSizeListK < 4) {
						for (int l = 0; l < 4 - iSizeListK; l++) {
							arrFinalk[iCount++] = 0;
						}
					}
				}
			}
		}

		return arrFinalk;
	}

	/**
	 * This method is responsible for conversion of Binary bits to HexEncoded
	 * fingerprints
	 * 
	 * @param arrBinFing
	 *            Determines the binary bits for a fingerprint record
	 * @return String string to return as HexEncoded.
	 * @throws Exception
	 *             Exception thrown during Binary to Hex conversion
	 */
	public static String convertHexFingerprintsFromBin(long[] arrBinFing)
			throws Exception {

		LOGGER.debug("Entered function convertHexFingerprintsFromBin");

		// resultHex to store hex encoded Fingerprints
		String strResultHex = BLANK;
		double dDecValue = 0;
		int j = 0;

		// Traverse through each character of binary bits , processing 8 bits at
		// a time to get Decimal value,
		// and converting to 2 hex character

		int iArrCount = arrBinFing.length;
		for (int i = 0; i < iArrCount; i++) {
			// String strByte = "";
			dDecValue = dDecValue + (Math.pow(2, j) * arrBinFing[i]);
			j++;

			if (i % 8 == 7) {

				StringBuilder strTemp = new StringBuilder();
				long lDecVal = (long) dDecValue;

				if (lDecVal == 0) {
					strTemp.append("00");
				}

				while (lDecVal != 0) {

					long lRes = lDecVal % 16;

					if (lRes == 10) {
						strTemp.append(HEX_A);
					} else if (lRes == 11) {
						strTemp.append(HEX_B);
					} else if (lRes == 12) {
						strTemp.append(HEX_C);
					} else if (lRes == 13) {
						strTemp.append(HEX_D);
					} else if (lRes == 14) {
						strTemp.append(HEX_E);
					} else if (lRes == 15) {
						strTemp.append(HEX_F);
					} else {
						strTemp.append(String.valueOf(lRes));
					}
					lDecVal = lDecVal / 16;
				}

				if (strTemp.length() == 1) {
					strTemp.append("0");
				}
				strTemp.reverse();
				strResultHex = strResultHex + strTemp.toString();
				dDecValue = 0;
				j = 0;
			}
		}
		return strResultHex;
	}

	/**
	 * This method returns the current date and time as per the FPS format. This
	 * date string will be written to the FPS header.
	 * 
	 * @return String representation of formatted FPS date
	 */
	public static String getDateInFPSFormat() {

		// Get default locale
		Locale locale = Locale.getDefault();
		// Get today's date
		Date date = new Date();
		// Formatting the date/time using a custom FPS format :
		// [YYYY]-[MM]-[DD]T[hh]:[mm]:[ss]
		Format formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", locale);
		String strDate = formatter.format(date);

		return strDate;
	}

	/**
	 * 
	 * This method checks if file protocol is present and returns actual file
	 * path. Also checks if the file exists and can be read from the folder.
	 * 
	 * @param fileS
	 *            The string file path.
	 * @param reader
	 *            true if used for fingerprint reader node, els false
	 * @param retError
	 *            true if exception required to be thrown, else consume
	 * @return File The returned file object.
	 * @throws InvalidSettingsException
	 *             The exception thrown in case of invalid file name is entered.
	 */

	public static File checkFile(String fileS, boolean reader, boolean retError)
			throws InvalidSettingsException {
		File tmp = null;
		if (fileS == null || fileS.isEmpty()) {
			throw new InvalidSettingsException("No fingerprint file specified");
		}
		URL url;
		try {
			url = new URL(fileS);
		} catch (Exception e) {
			// see if they specified a file without giving the protocol
			tmp = new File(fileS);
			try {
				url = tmp.getAbsoluteFile().toURI().toURL();
			} catch (MalformedURLException e1) {
				throw new InvalidSettingsException("Invalid URL: "
						+ e1.getMessage(), e1);
			}
		}
		if ("file".equals(url.getProtocol())) {

			try {
				tmp = new File(url.toURI());
				if (reader && retError) {
					if (!tmp.exists()) {
						throw new InvalidSettingsException(
								"Specified input file doesn't exist.");
					}
					if (!tmp.isFile() || !tmp.canRead()) {
						throw new InvalidSettingsException(
								"Specified input file is not a readable file.");
					}
				}
			} catch (URISyntaxException e) {
				throw new InvalidSettingsException("Invalid URL: "
						+ e.getMessage(), e);
			}
		}
		return tmp;
	}

}
