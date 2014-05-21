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

import java.awt.BorderLayout;

import javax.swing.JSeparator;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Provides a component with a separator. No setting values will be saved.
 * 
 * @author Manuel Schwarze
 */
public class DialogComponentSeparator extends DialogComponent {

	private final JSeparator m_sep;

	/**
	 * Creates new instance of <code>DialogComponentSeparator</code>
	 */
	public DialogComponentSeparator() {
		super(new EmptySettingsModel());

		m_sep = new JSeparator();
		getComponentPanel().setLayout(new BorderLayout());
		getComponentPanel().add(m_sep, BorderLayout.SOUTH);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
			throws NotConfigurableException {
		// Nothing to do ...
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void setEnabledComponents(final boolean enabled) {
		// Nothing to do ...
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setToolTipText(final String text) {
		// Nothing to do ...
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void updateComponent() {
		// Nothing to do ...
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettingsBeforeSave()
			throws InvalidSettingsException {
		// Nothing to do ...
	}

}
