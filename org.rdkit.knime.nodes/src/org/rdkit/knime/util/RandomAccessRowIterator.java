/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2011
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

import java.util.NoSuchElementException;

import org.knime.core.data.DataRow;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;

/**
 * This class iterates over the rows of a DataTable and is capable of
 * jumping ahead to absolute row indices. If an index is requested that
 * is smaller than the current one, the internal iterator is closed
 * and a new one gets created to deliver the requested position.
 * Such a scenarios is not good for performance and not recommended.
 * This class should mainly be used to iterate to absolute
 * subsequential row positions.
 *
 * @author Manuel Schwarze
 */
public class RandomAccessRowIterator extends CloseableRowIterator {

	//
	// Members
	//

	/** Determines, if this iterator was closed already. */
	private boolean m_bClosed;

	/** The wrapped iterator to be used for delivering data rows. */
	private CloseableRowIterator m_iterator;

	/** Stores the next row index that gets delivered when calling next(). -1, if there is no more row. */
	private long m_lNextRowIndex;

	/** Table to iterate over. */
	private final BufferedDataTable m_table;

	//
	// Constructor
	//

	/**
	 * Creates a new row iterator based on the passed in row iterator.
	 *
	 * @param table The underlying table of this new iterator instance. Must not be null.
	 */
	public RandomAccessRowIterator(final BufferedDataTable table) {
		super();

		if (table == null) {
			throw new IllegalArgumentException("Table must not be null.");
		}

		m_table = table;
		m_bClosed = false;
		m_iterator = null;
		m_lNextRowIndex = -1;

		resetIterator();
	}

	//
	// Public Methods
	//

	/**
	 * Returns the underlying table of this iterator.
	 *
	 * @return Buffered date table. Not null.
	 */
	public BufferedDataTable getTable() {
		return m_table;
	}

	/**
	 * Resets the internal iterator to restart at the beginning of the table.
	 * If the {@link #close()} method was called already, it does not do anything.
	 */
	public void resetIterator() {
		if (!m_bClosed) {

			// Closes the old internal iterator
			if (m_iterator != null) {
				m_iterator.close();
			}

			// Create the new iterator
			m_iterator = m_table.iterator();

			if (m_iterator != null && m_iterator.hasNext()) {
				m_lNextRowIndex = 0;
			}
			else { // No rows available at all
				m_lNextRowIndex = -1;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() {
		m_bClosed = true;
		m_lNextRowIndex = -1;

		if (m_iterator != null) {
			m_iterator.close();
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.knime.core.data.RowIterator#hasNext()
	 */
	@Override
	public boolean hasNext() {
		return !m_bClosed && m_lNextRowIndex >= 0 &&
				m_iterator != null && m_iterator.hasNext();
	}

	/**
	 * Delivers the data row at the specified row index.
	 *
	 * @param rowIndex Row index to deliver.
	 *
	 * @return Data row at the specified row index (zero-based) or null, if
	 * 		the requested position does not exist.
	 */
	public DataRow get(final long rowIndex) {
		DataRow row = null;

		try {
			if (rowIndex >= 0) {
				if (m_lNextRowIndex == rowIndex) {
					row = next();
				}
				else if (m_lNextRowIndex < rowIndex) {
					skip(rowIndex - m_lNextRowIndex);
					row = next();
				}
				else {
					resetIterator();
					if (hasNext()) {
						row = get(rowIndex);
					}
				}
			}
		}
		catch (final NoSuchElementException exc) {
			// Ignore this exception - we return null.
		}

		return row;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws NoSuchElementException Thrown, if the table has not enough rows to iterate over.
	 *
	 * @see org.knime.core.data.RowIterator#next()
	 */
	@Override
	public DataRow next() {
		DataRow dataRow = null;

		if (hasNext()) {
			dataRow = m_iterator.next();
			m_lNextRowIndex++;
		}
		else {
			m_lNextRowIndex = -1;
			throw new NoSuchElementException("The table does not contain any more data rows.");
		}

		return dataRow;
	}

	/**
	 * Skips over the specified number of rows. If the number of rows
	 * left for iteration is smaller than the specified number of rows,
	 * this method will eventually throw a NoSuchElementException.
	 *
	 * @param lNumberOfRows 	Number of rows to skip.
	 *
	 * @throws NoSuchElementException Thrown, if the table has not enough rows to iterate over.
	 */
	public void skip(final long lNumberOfRows) {
		if (m_iterator == null) {
			m_lNextRowIndex = -1;
			throw new NoSuchElementException("Iterator not usable. Cannot skip rows.");
		}

		try {
			for (long i = 0; i < lNumberOfRows; i++) {
				m_iterator.next();
				m_lNextRowIndex++;
			}
		}
		catch (final NoSuchElementException exc) {
			m_lNextRowIndex = -1;
			throw exc;
		}

	}

	/**
	 * Returns the index of the next row (zero-based).
	 *
	 * @return Index of the next row. -1, if there is no next row.
	 */
	public long getNextRowIndex() {
		return m_lNextRowIndex;
	}

	/**
	 * Returns a string representation of this object.
	 *
	 * @return String representation.
	 */
	@Override
	public String toString() {
		final StringBuilder sbRet = new StringBuilder("AbsoluteRowIterator { ");
		sbRet.append("nextRowIndex=").append(m_lNextRowIndex).
		append(" }");

		return sbRet.toString();
	}
}
