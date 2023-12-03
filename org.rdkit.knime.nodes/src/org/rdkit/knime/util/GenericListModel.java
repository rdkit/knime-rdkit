/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2012-2023
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
package org.rdkit.knime.util;

import java.lang.reflect.Array;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.AbstractListModel;

/**
 * A typed list model.
 * 
 * @author Manuel Schwarze
 *
 * @param <T> The type of the elements to be stored in this list model.
 */
public class GenericListModel<T> extends AbstractListModel<T> {

	//
	// Constants
	//

	/** Serial number. */
	private static final long serialVersionUID = -3050564084268208029L;

	//
	// Members
	//

	/** Stores the type of the generic components. */
	private final Class<T> m_type;

	/** Delegate that we use as storage. */
	private final Vector<T> m_vDelegate = new Vector<T>();

	//
	// Constructors
	//

	public GenericListModel(final Class<T> componentType) {
		super();

		if (componentType == null) {
			throw new IllegalArgumentException("Component type must not be null.");
		}

		m_type = componentType;
	}

	//
	// Public Methods
	//

	/**
	 * Returns the number of components in this list.
	 * <p>
	 * This method is identical to <code>size</code>, which implements the
	 * <code>List</code> interface defined in the 1.2 Collections framework.
	 * This method exists in conjunction with <code>setSize</code> so that
	 * <code>size</code> is identifiable as a JavaBean property.
	 * 
	 * @return the number of components in this list
	 * @see #size()
	 */
	@Override
	public int getSize() {
		return m_vDelegate.size();
	}

	/**
	 * Returns the component at the specified index. <blockquote> <b>Note:</b>
	 * Although this method is not deprecated, the preferred method to use is
	 * <code>get(int)</code>, which implements the <code>List</code> interface
	 * defined in the 1.2 Collections framework. </blockquote>
	 * 
	 * @param index
	 *            an index into this list
	 * @return the component at the specified index
	 * @exception ArrayIndexOutOfBoundsException
	 *                if the <code>index</code> is negative or greater than the
	 *                current size of this list
	 * @see #get(int)
	 */
	@Override
	public T getElementAt(final int index) {
		return m_vDelegate.elementAt(index);
	}

	/**
	 * Returns the component type of this list model.
	 * 
	 * @return Component type.
	 */
	public Class<T> getComponentType() {
		return m_type;
	}

	/**
	 * Returns the component at the specified index using the correct
	 * type of the list model.
	 * <blockquote> <b>Note:</b>
	 * Although this method is not deprecated, the preferred method to use is
	 * <code>get(int)</code>, which implements the <code>List</code> interface
	 * defined in the 1.2 Collections framework. </blockquote>
	 * 
	 * @param index
	 *            an index into this list
	 * @return the component at the specified index
	 * @exception ArrayIndexOutOfBoundsException
	 *                if the <code>index</code> is negative or greater than the
	 *                current size of this list
	 * @see #get(int)
	 */
	public T getTypedElementAt(final int index) {
		return m_vDelegate.elementAt(index);
	}

	/**
	 * Copies the components of this list into the specified array. The array
	 * must be big enough to hold all the objects in this list, else an
	 * <code>IndexOutOfBoundsException</code> is thrown.
	 * 
	 * @param anArray
	 *            the array into which the components get copied
	 * @see Vector#copyInto(Object[])
	 */
	public void copyInto(final T anArray[]) {
		m_vDelegate.copyInto(anArray);
	}

	/**
	 * Trims the capacity of this list to be the list's current size.
	 * 
	 * @see Vector#trimToSize()
	 */
	public void trimToSize() {
		m_vDelegate.trimToSize();
	}

	/**
	 * Increases the capacity of this list, if necessary, to ensure that it can
	 * hold at least the number of components specified by the minimum capacity
	 * argument.
	 * 
	 * @param minCapacity
	 *            the desired minimum capacity
	 * @see Vector#ensureCapacity(int)
	 */
	public void ensureCapacity(final int minCapacity) {
		m_vDelegate.ensureCapacity(minCapacity);
	}

	/**
	 * Sets the size of this list.
	 * 
	 * @param newSize
	 *            the new size of this list
	 * @see Vector#setSize(int)
	 */
	public void setSize(final int newSize) {
		final int oldSize = m_vDelegate.size();
		m_vDelegate.setSize(newSize);
		if (oldSize > newSize) {
			fireIntervalRemoved(this, newSize, oldSize - 1);
		}
		else if (oldSize < newSize) {
			fireIntervalAdded(this, oldSize, newSize - 1);
		}
	}

	/**
	 * Returns the current capacity of this list.
	 * 
	 * @return the current capacity
	 * @see Vector#capacity()
	 */
	public int capacity() {
		return m_vDelegate.capacity();
	}

	/**
	 * Returns the number of components in this list.
	 * 
	 * @return the number of components in this list
	 * @see Vector#size()
	 */
	public int size() {
		return m_vDelegate.size();
	}

	/**
	 * Tests whether this list has any components.
	 * 
	 * @return <code>true</code> if and only if this list has no components,
	 *         that is, its size is zero; <code>false</code> otherwise
	 * @see Vector#isEmpty()
	 */
	public boolean isEmpty() {
		return m_vDelegate.isEmpty();
	}

	/**
	 * Returns an enumeration of the components of this list.
	 * 
	 * @return an enumeration of the components of this list
	 * @see Vector#elements()
	 */
	public Enumeration<T> elements() {
		return m_vDelegate.elements();
	}

	/**
	 * Tests whether the specified object is a component in this list.
	 * 
	 * @param elem
	 *            an object
	 * @return <code>true</code> if the specified object is the same as a
	 *         component in this list
	 * @see Vector#contains(Object)
	 */
	public boolean contains(final T elem) {
		return m_vDelegate.contains(elem);
	}

	/**
	 * Searches for the first occurrence of <code>elem</code>.
	 * 
	 * @param elem
	 *            an object
	 * @return the index of the first occurrence of the argument in this list;
	 *         returns <code>-1</code> if the object is not found
	 * @see Vector#indexOf(Object)
	 */
	public int indexOf(final T elem) {
		return m_vDelegate.indexOf(elem);
	}

	/**
	 * Searches for the first occurrence of <code>elem</code>, beginning the
	 * search at <code>index</code>.
	 * 
	 * @param elem
	 *            an desired component
	 * @param index
	 *            the index from which to begin searching
	 * @return the index where the first occurrence of <code>elem</code> is
	 *         found after <code>index</code>; returns <code>-1</code> if the
	 *         <code>elem</code> is not found in the list
	 * @see Vector#indexOf(Object,int)
	 */
	public int indexOf(final T elem, final int index) {
		return m_vDelegate.indexOf(elem, index);
	}

	/**
	 * Returns the index of the last occurrence of <code>elem</code>.
	 * 
	 * @param elem
	 *            the desired component
	 * @return the index of the last occurrence of <code>elem</code> in the
	 *         list; returns <code>-1</code> if the object is not found
	 * @see Vector#lastIndexOf(Object)
	 */
	public int lastIndexOf(final T elem) {
		return m_vDelegate.lastIndexOf(elem);
	}

	/**
	 * Searches backwards for <code>elem</code>, starting from the specified
	 * index, and returns an index to it.
	 * 
	 * @param elem
	 *            the desired component
	 * @param index
	 *            the index to start searching from
	 * @return the index of the last occurrence of the <code>elem</code> in this
	 *         list at position less than <code>index</code>; returns
	 *         <code>-1</code> if the object is not found
	 * @see Vector#lastIndexOf(Object,int)
	 */
	public int lastIndexOf(final T elem, final int index) {
		return m_vDelegate.lastIndexOf(elem, index);
	}

	/**
	 * Returns the component at the specified index. Throws an
	 * <code>ArrayIndexOutOfBoundsException</code> if the index is negative or
	 * not less than the size of the list. <blockquote> <b>Note:</b> Although
	 * this method is not deprecated, the preferred method to use is
	 * <code>get(int)</code>, which implements the <code>List</code> interface
	 * defined in the 1.2 Collections framework. </blockquote>
	 * 
	 * @param index
	 *            an index into this list
	 * @return the component at the specified index
	 * @see #get(int)
	 * @see Vector#elementAt(int)
	 */
	public T elementAt(final int index) {
		return m_vDelegate.elementAt(index);
	}

	/**
	 * Returns the first component of this list. Throws a
	 * <code>NoSuchElementException</code> if this vector has no components.
	 * 
	 * @return the first component of this list
	 * @see Vector#firstElement()
	 */
	public T firstElement() {
		return m_vDelegate.firstElement();
	}

	/**
	 * Returns the last component of the list. Throws a
	 * <code>NoSuchElementException</code> if this vector has no components.
	 * 
	 * @return the last component of the list
	 * @see Vector#lastElement()
	 */
	public T lastElement() {
		return m_vDelegate.lastElement();
	}

	/**
	 * Sets the component at the specified <code>index</code> of this list to be
	 * the specified object. The previous component at that position is
	 * discarded.
	 * <p>
	 * Throws an <code>ArrayIndexOutOfBoundsException</code> if the index is
	 * invalid. <blockquote> <b>Note:</b> Although this method is not
	 * deprecated, the preferred method to use is <code>set(int,Object)</code>,
	 * which implements the <code>List</code> interface defined in the 1.2
	 * Collections framework. </blockquote>
	 * 
	 * @param obj
	 *            what the component is to be set to
	 * @param index
	 *            the specified index
	 * @see #set(int,Object)
	 * @see Vector#setElementAt(Object,int)
	 */
	public void setElementAt(final T obj, final int index) {
		m_vDelegate.setElementAt(obj, index);
		fireContentsChanged(this, index, index);
	}

	/**
	 * Deletes the component at the specified index.
	 * <p>
	 * Throws an <code>ArrayIndexOutOfBoundsException</code> if the index is
	 * invalid. <blockquote> <b>Note:</b> Although this method is not
	 * deprecated, the preferred method to use is <code>remove(int)</code>,
	 * which implements the <code>List</code> interface defined in the 1.2
	 * Collections framework. </blockquote>
	 * 
	 * @param index
	 *            the index of the object to remove
	 * @see #remove(int)
	 * @see Vector#removeElementAt(int)
	 */
	public void removeElementAt(final int index) {
		m_vDelegate.removeElementAt(index);
		fireIntervalRemoved(this, index, index);
	}

	/**
	 * Inserts the specified object as a component in this list at the specified
	 * <code>index</code>.
	 * <p>
	 * Throws an <code>ArrayIndexOutOfBoundsException</code> if the index is
	 * invalid. <blockquote> <b>Note:</b> Although this method is not
	 * deprecated, the preferred method to use is <code>add(int,Object)</code>,
	 * which implements the <code>List</code> interface defined in the 1.2
	 * Collections framework. </blockquote>
	 * 
	 * @param obj
	 *            the component to insert
	 * @param index
	 *            where to insert the new component
	 * @exception ArrayIndexOutOfBoundsException
	 *                if the index was invalid
	 * @see #add(int,Object)
	 * @see Vector#insertElementAt(Object,int)
	 */
	public void insertElementAt(final T obj, final int index) {
		m_vDelegate.insertElementAt(obj, index);
		fireIntervalAdded(this, index, index);
	}

	/**
	 * Adds the specified component to the end of this list.
	 * 
	 * @param obj
	 *            the component to be added
	 * @see Vector#addElement(Object)
	 */
	public void addElement(final T obj) {
		final int index = m_vDelegate.size();
		m_vDelegate.addElement(obj);
		fireIntervalAdded(this, index, index);
	}

	/**
	 * Removes the first (lowest-indexed) occurrence of the argument from this
	 * list.
	 * 
	 * @param obj
	 *            the component to be removed
	 * @return <code>true</code> if the argument was a component of this list;
	 *         <code>false</code> otherwise
	 * @see Vector#removeElement(Object)
	 */
	public boolean removeElement(final T obj) {
		final int index = indexOf(obj);
		final boolean rv = m_vDelegate.removeElement(obj);
		if (index >= 0) {
			fireIntervalRemoved(this, index, index);
		}
		return rv;
	}

	/**
	 * Removes all components from this list and sets its size to zero.
	 * <blockquote> <b>Note:</b> Although this method is not deprecated, the
	 * preferred method to use is <code>clear</code>, which implements the
	 * <code>List</code> interface defined in the 1.2 Collections framework.
	 * </blockquote>
	 * 
	 * @see #clear()
	 * @see Vector#removeAllElements()
	 */
	public void removeAllElements() {
		final int index1 = m_vDelegate.size() - 1;
		m_vDelegate.removeAllElements();
		if (index1 >= 0) {
			fireIntervalRemoved(this, 0, index1);
		}
	}

	/**
	 * Returns a string that displays and identifies this object's properties.
	 * 
	 * @return a String representation of this object
	 */
	@Override
	public String toString() {
		return m_vDelegate.toString();
	}

	/*
	 * The remaining methods are included for compatibility with the Java 2
	 * platform Vector class.
	 */

	/**
	 * Returns an array containing all of the elements in this list in the
	 * correct order.
	 * 
	 * @return an array containing the elements of the list
	 * @see Vector#toArray()
	 */
	@SuppressWarnings("unchecked")
	public T[] toArray() {
		final T[] rv = (T[])Array.newInstance(getComponentType(), m_vDelegate.size());
		m_vDelegate.copyInto(rv);
		return rv;
	}

	/**
	 * Returns the element at the specified position in this list.
	 * <p>
	 * Throws an <code>ArrayIndexOutOfBoundsException</code> if the index is out
	 * of range (<code>index &lt; 0 || index &gt;= size()</code>).
	 * 
	 * @param index
	 *            index of element to return
	 */
	public T get(final int index) {
		return m_vDelegate.elementAt(index);
	}

	/**
	 * Replaces the element at the specified position in this list with the
	 * specified element.
	 * <p>
	 * Throws an <code>ArrayIndexOutOfBoundsException</code> if the index is out
	 * of range (<code>index &lt; 0 || index &gt;= size()</code>).
	 * 
	 * @param index
	 *            index of element to replace
	 * @param element
	 *            element to be stored at the specified position
	 * @return the element previously at the specified position
	 */
	public T set(final int index, final T element) {
		final T rv = m_vDelegate.elementAt(index);
		m_vDelegate.setElementAt(element, index);
		fireContentsChanged(this, index, index);
		return rv;
	}

	/**
	 * Inserts the specified element at the specified position in this list.
	 * <p>
	 * Throws an <code>ArrayIndexOutOfBoundsException</code> if the index is out
	 * of range (<code>index &lt; 0 || index &gt; size()</code>).
	 * 
	 * @param index
	 *            index at which the specified element is to be inserted
	 * @param element
	 *            element to be inserted
	 */
	public void add(final int index, final T element) {
		m_vDelegate.insertElementAt(element, index);
		fireIntervalAdded(this, index, index);
	}

	/**
	 * Removes the element at the specified position in this list. Returns the
	 * element that was removed from the list.
	 * <p>
	 * Throws an <code>ArrayIndexOutOfBoundsException</code> if the index is out
	 * of range (<code>index &lt; 0 || index &gt;= size()</code>).
	 * 
	 * @param index
	 *            the index of the element to removed
	 */
	public T remove(final int index) {
		final T rv = m_vDelegate.elementAt(index);
		m_vDelegate.removeElementAt(index);
		fireIntervalRemoved(this, index, index);
		return rv;
	}

	/**
	 * Removes all of the elements from this list. The list will be empty after
	 * this call returns (unless it throws an exception).
	 */
	public void clear() {
		final int index1 = m_vDelegate.size() - 1;
		m_vDelegate.removeAllElements();
		if (index1 >= 0) {
			fireIntervalRemoved(this, 0, index1);
		}
	}

	/**
	 * Deletes the components at the specified range of indexes. The removal is
	 * inclusive, so specifying a range of (1,5) removes the component at index
	 * 1 and the component at index 5, as well as all components in between.
	 * <p>
	 * Throws an <code>ArrayIndexOutOfBoundsException</code> if the index was
	 * invalid. Throws an <code>IllegalArgumentException</code> if
	 * <code>fromIndex &gt; toIndex</code>.
	 * 
	 * @param fromIndex
	 *            the index of the lower end of the range
	 * @param toIndex
	 *            the index of the upper end of the range
	 * @see #remove(int)
	 */
	public void removeRange(final int fromIndex, final int toIndex) {
		if (fromIndex > toIndex) {
			throw new IllegalArgumentException("fromIndex must be <= toIndex");
		}
		for (int i = toIndex; i >= fromIndex; i--) {
			m_vDelegate.removeElementAt(i);
		}
		fireIntervalRemoved(this, fromIndex, toIndex);
	}
}
