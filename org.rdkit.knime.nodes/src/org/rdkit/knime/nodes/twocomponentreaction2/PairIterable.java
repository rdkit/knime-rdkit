/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 *
 */
package org.rdkit.knime.nodes.twocomponentreaction2;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterable that iterates over all pairings of two given iterables.
 * 
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 *
 * @param <T> The type of the first iterable
 * @param <U> The type of the second iterable
 */
class PairIterable<T, U> implements Iterable<Pair<Pair<T, Long>, Pair<U, Long>>> {
	private final Iterable<T> firstIterable;
	private final Iterable<U> secondIterable;

	public PairIterable(Iterable<T> firstIterable, Iterable<U> secondIterable) {
		this.firstIterable = firstIterable;
		this.secondIterable = secondIterable;
	}

	@Override
	public Iterator<Pair<Pair<T, Long>, Pair<U, Long>>> iterator() {
		return new PairIterator<>(firstIterable.iterator(), secondIterable);
	}

	private static class PairIterator<T, U> implements Iterator<Pair<Pair<T, Long>, Pair<U, Long>>> {
		private final Iterator<T> firstIterator;
		private final Iterable<U> secondIterable;
		private Iterator<U> secondIterator = null;
		private T currentFirst;
		private Long firstIndex = 0L;
		private Long secondIndex = 0L;

		public PairIterator(Iterator<T> firstIterator, Iterable<U> secondIterable) {
			this.firstIterator = firstIterator;
			this.secondIterable = secondIterable;
			if (firstIterator.hasNext()) {
				currentFirst = firstIterator.next();
				secondIterator = secondIterable.iterator();
			}
		}

		@Override
		public boolean hasNext() {
			return ((secondIterator != null && secondIterator.hasNext())) || firstIterator.hasNext();
		}

		@Override
		public Pair<Pair<T, Long>, Pair<U, Long>> next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}

			if (!secondIterator.hasNext()) {
				currentFirst = firstIterator.next();
				firstIndex++;
				secondIterator = secondIterable.iterator();
				secondIndex = 0L;
			}

			return new Pair<>(new Pair<>(currentFirst, firstIndex), new Pair<>(secondIterator.next(), secondIndex++));
		}
	}
}