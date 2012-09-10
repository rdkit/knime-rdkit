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
	private int m_iNextRowIndex;

	/** Table to iterate over. */
	private BufferedDataTable m_table;

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
		m_iNextRowIndex = -1;

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
				m_iNextRowIndex = 0;
			}
			else { // No rows available at all
				m_iNextRowIndex = -1;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() {
		m_bClosed = true;
		m_iNextRowIndex = -1;
		
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
		return !m_bClosed && m_iNextRowIndex >= 0 && 
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
	public DataRow get(final int rowIndex) {
		DataRow row = null;

		try {
			if (rowIndex >= 0) {
				if (m_iNextRowIndex == rowIndex) {
					row = next();
				}
				else if (m_iNextRowIndex < rowIndex) {
					skip(rowIndex - m_iNextRowIndex);
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
		catch (NoSuchElementException exc) {
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
			m_iNextRowIndex++;
		}
		else {
			m_iNextRowIndex = -1;
			throw new NoSuchElementException("The table does not contain any more data rows.");
		}

		return dataRow;
	}

	/**
	 * Skips over the specified number of rows. If the number of rows
	 * left for iteration is smaller than the specified number of rows,
	 * this method will eventually throw a NoSuchElementException.
	 *
	 * @param iNumberOfRows 	Number of rows to skip.
	 *
	 * @throws NoSuchElementException Thrown, if the table has not enough rows to iterate over.
	 */
	public void skip(final int iNumberOfRows) {
		if (m_iterator == null) {
			m_iNextRowIndex = -1;
			throw new NoSuchElementException("Iterator not usable. Cannot skip rows.");
		}
		
		try {
			for (int i = 0; i < iNumberOfRows; i++) {
				m_iterator.next();
				m_iNextRowIndex++;
			}
		}
		catch (NoSuchElementException exc) {
			m_iNextRowIndex = -1;
			throw exc;
		}

	}

	/**
	 * Returns the index of the next row (zero-based).
	 *
	 * @return Index of the next row. -1, if there is no next row.
	 */
	public int getNextRowIndex() {
		return m_iNextRowIndex;
	}

	/**
	 * Returns a string representation of this object.
	 *
	 * @return String representation.
	 */
	@Override
    public String toString() {
		StringBuilder sbRet = new StringBuilder("AbsoluteRowIterator { ");
		sbRet.append("nextRowIndex=").append(m_iNextRowIndex).
			append(" }");

		return sbRet.toString();
	}
}
