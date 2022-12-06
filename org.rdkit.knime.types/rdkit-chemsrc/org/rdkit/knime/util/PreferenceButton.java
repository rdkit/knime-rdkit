/* 
 * This source code, its documentation and all related files
 * are protected by copyright law. All rights reserved.
 *
 * (C)Copyright 2022 by Novartis Pharma AG 
 * Novartis Campus, CH-4002 Basel, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
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
