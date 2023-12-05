/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2015-2023
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
package org.rdkit.knime.nodes;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

/**
 * This dialog can be extended to auto-cleanup borders of inner dialog panels.
 * 
 * @author Manuel Schwarze
 */
public abstract class AbstractRDKitNodeSettingsPane extends DefaultNodeSettingsPane {

	//
	// Constructors
	//

	/**
	 * Creates a new RDKit Node Settings Pane.
	 */
	public AbstractRDKitNodeSettingsPane() {
		super();
	}

	//
	// Public Methods
	//

	/**
	 * After loading the settings this overridden method marks an
	 * IAM Cookie Provider as ready for generating IAM Cookies based on
	 * the current settings. This works only, if the dialog is
	 * using an authentication tab ({@link #addAuthenticationTab()}).
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final org.knime.core.node.port.PortObjectSpec[] specs) throws NotConfigurableException {
		super.loadAdditionalSettingsFrom(settings, specs);

		// Correct borders
		final JPanel panel = getPanel();
		for (int i = 0; i < panel.getComponentCount(); i++) {
			final Component comp = panel.getComponent(i);
			if (comp instanceof JTabbedPane) {
				final JTabbedPane tabPane = (JTabbedPane)comp;
				for (int j = 0; j < tabPane.getTabCount(); j++) {
					final Component compTab = tabPane.getComponentAt(j);
					if (compTab instanceof JScrollPane) {
						final Component compInner = ((JScrollPane)compTab).getViewport().getView();
						if (compInner instanceof JComponent) {
							correctBorder((JComponent)compInner);
						}
					}
					else if (compTab instanceof JComponent) {
						correctBorder((JComponent)compTab);
					}
				}
			}
		}
	};

	//
	// Protected Methods
	//

	/**
	 * Called for each tab panel component that is derived from a JComponent.
	 * This method is used to set borders around the inner components
	 * of a tab panel.
	 * 
	 * @param Component of a tab panel. Can be null.
	 */
	protected void correctBorder(final JComponent comp) {
		if (comp != null) {
			Border border = comp.getBorder();

			if (border == null) {
				border = BorderFactory.createEmptyBorder(7, 7, 7, 7);
			}
			else if (border instanceof EmptyBorder == false &&
					(border instanceof CompoundBorder == false ||
					((CompoundBorder)border).getOutsideBorder() instanceof EmptyBorder == false)) {
				border = BorderFactory.createCompoundBorder(
						BorderFactory.createEmptyBorder(7, 7, 7, 7), border);
			}

			comp.setBorder(border);
		}
	}
}
