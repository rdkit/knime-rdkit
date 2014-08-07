/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2014
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
 *   07.08.2014 (thor): created
 */
package org.rdkit.knime;

import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Checks if environment variable are set correctly for RDKit.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
class EnvironmentChecker {

    /**
     * Checks whether the environment variables are set correctly.
     *
     * @return <code>null</code> if everything is OK, an error message suitable for the user otherwise
     */
    public static String checkEnvironment() {
        String lcNumeric = System.getenv("LC_NUMERIC");
        String lcAll = System.getenv("LC_ALL");
        String lang = System.getenv("LANG");

        if (lcNumeric != null) {
            if (getDecimalSeparator(lcNumeric) == '.') {
                return null; // OK
            } else {
                return "LC_NUMERIC is set to an incompatible value (" + lcNumeric
                        + ") which causes RDKit to compute wrong results in some cases. Please "
                        + " change LC_NUMERIC to an 'en' or 'C' locale.";
            }
        } else if (lcAll != null) {
            if (getDecimalSeparator(lcAll) == '.') {
                return null ; // OK
            } else {
                return "LC_ALL is set to an incompatible value (" + lcAll
                        + ") which causes RDKit to compute wrong results in some cases. Please "
                        + " change LC_ALL to an 'en' or 'C' locale.";
            }
        } else if (lang != null) {
            if (getDecimalSeparator(lang) == '.') {
                return null; // OK
            } else {
                return "LANG is set to an incompatible value (" + lang
                        + ") which causes RDKit to compute wrong results in some cases. Please "
                        + " change LANG to an 'en' or 'C' locale.";
            }
        }
        return null;
    }

    private static char getDecimalSeparator(String localeString) {
        int index = localeString.indexOf('_');
        Locale locale;
        if (index > 0) {
            String language = localeString.substring(0, index);
            int index2 = localeString.indexOf('.');
            String variant = (index2 > 0) ? localeString.substring(index + 1, index2) : localeString.substring(index + 1);
            locale = new Locale(language, variant);
        } else {
            locale = new Locale(localeString);
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(locale);
        return symbols.getDecimalSeparator();
    }
}
