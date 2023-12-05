/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C)2022-2023
 *  Novartis Pharma AG, Switzerland
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
 * -------------------------------------------------------------------
 *
 */
package org.rdkit.knime.util;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * An abstract field editor for a string type preference that presents a string
 * input field with a change button to its right to edit the input field's
 * content. When the user presses the change button, the abstract framework
 * method <code>changePressed()</code> gets called to compute a new string.
 * 
 * @author Manuel Schwarze
 */
public abstract class PreferenceButton extends FieldEditor {

	/**
	 * The change button, or <code>null</code> if none (before creation and
	 * after disposal).
	 */
	private Button m_button;

	/**
	 * The text for the change button, or <code>null</code> if missing.
	 */
	private String m_strLabel;

	/**
	 * Creates a new string button field editor
	 */
	protected PreferenceButton() {
		m_strLabel = null;
	}

	/**
	 * Creates a string button field editor.
	 * 
	 * @param labelText
	 *            the label text of the button
	 * @param parent
	 *            the parent of the field editor's control
	 */
	protected PreferenceButton(String labelText, Composite parent) {
		m_strLabel = labelText;
		init(labelText, labelText);
		createControl(parent);
	}

	protected void adjustForNumColumns(int numColumns) {
        GridData gd = (GridData) m_button.getLayoutData();
        gd.horizontalSpan = numColumns;
	}
	
	@Override
	protected void doLoad() {
		// Ignored as this button does not store any 
	}
	
	@Override
	protected void doLoadDefault() {
		// Ignored as this button does not store any 
	}
	
	@Override
	protected void doStore() {
		// Ignored as this button does not store any 
	}

	/**
	 * Notifies that the button has been pressed.
	 * <p>
	 * Subclasses must implement this method to provide an action.
	 * </p>
	 * 
	 * @return the new string to display, or <code>null</code> to leave the old
	 *         string showing
	 */
	protected abstract void onButtonClicked();

	protected void doFillIntoGrid(Composite parent, int numColumns) {
		m_button = getButtonControl(parent);
		GridData gd = new GridData();
		gd.horizontalAlignment = GridData.END;
		gd.verticalAlignment = GridData.CENTER;
		int widthHint = convertHorizontalDLUsToPixels(m_button,
				IDialogConstants.BUTTON_WIDTH);
		gd.widthHint = Math.max(widthHint,
				m_button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		m_button.setLayoutData(gd);
	}

	/**
	 * Get the button control. Create it in parent if required.
	 * 
	 * @param parent
	 * @return Button
	 */
	protected Button getButtonControl(Composite parent) {
		if (m_button == null) {
			m_button = new Button(parent, SWT.PUSH);
			if (m_strLabel == null) {
				m_strLabel = "Perform Action";
			}
			m_button.setText(m_strLabel);
			m_button.setFont(parent.getFont());
			m_button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					onButtonClicked();
				}
			});
			m_button.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent event) {
					m_button = null;
				}
			});
		}
		else {
			checkParent(m_button, parent);
		}
		return m_button;
	}

	/*
	 * (non-Javadoc) Method declared on FieldEditor.
	 */
	public int getNumberOfControls() {
		return 1;
	}

	/**
	 * Returns this field editor's shell.
	 * 
	 * @return the shell
	 */
	protected Shell getShell() {
		if (m_button == null) {
			return null;
		}
		return m_button.getShell();
	}

	/**
	 * Sets the text of the change button.
	 * 
	 * @param text
	 *            the new text
	 */
	public void setButtonLabel(String text) {
		Assert.isNotNull(text);
		m_strLabel = text;
		if (m_button != null) {
			m_button.setText(text);
			Point prefSize = m_button.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			GridData data = (GridData) m_button.getLayoutData();
			data.widthHint = Math.max(SWT.DEFAULT, prefSize.x);
		}
	}

	public void setEnabled(boolean enabled, Composite parent) {
		if (m_button != null) {
			m_button.setEnabled(enabled);
		}
	}

}
