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

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is the dialog for the Fingerprint reader node. The user is provided the
 * options to specify the input file name, whether or not the ids read from the file
 * should be set as row ids.
 * 
 * @author Sudip Ghosh
 */
@Deprecated
public class FingerprintReaderNodeDialog extends DefaultNodeSettingsPane {

	//
	// Members
	//

	/**
	 * Instance for filename setting model.
	 */
	private final SettingsModelString m_filename =
			new SettingsModelString("filename", "");

	/**
	 * Instance for SetID setting model.
	 */
	private final SettingsModelBoolean m_setID =
			new SettingsModelBoolean("setID", false);

	/**
	 * Instance for filename component.
	 */
	private final DialogComponentFileChooser m_chooser =
			new DialogComponentFileChooser(m_filename, "FpsReaderHistory",
					".fps|.fps.gz");

	/**
	 * Instance for Set row ID checkbox component .
	 */
	private final DialogComponentBoolean m_gid =
			new DialogComponentBoolean(m_setID, "Set Row Ids (Require unique Ids)");

	//
	// Constructor
	//

	/**
	 * Creates a new dialog for the FPS reader node.
	 */
	public FingerprintReaderNodeDialog() {
		addDialogComponent(m_chooser);
		addDialogComponent(m_gid);
	}


}

