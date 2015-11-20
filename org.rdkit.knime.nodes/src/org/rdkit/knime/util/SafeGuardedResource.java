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
package org.rdkit.knime.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

/**
 * A safe guarded resource shields a resource from unauthorized thread access.
 * By overriding the method {@link #createResource()} the developer has full
 * freedom to make use of all necessary logic to create an instance of the
 * resource. However, this method is then called on demand. Normally, such a
 * resource shall not be shared between threads. For every thread that is
 * calling the {@link #get()} method, it creates once a new instance of
 * this resource and returns it. Subsequent calls to {@link #get()} result
 * in the very same resource if the same thread is calling. In the special
 * case that the resource could be shared between threads -- this may happen
 * in certain situations -- the {@link #get()} method will always return
 * the same resource. A protected resource can be null, if a) the method
 * {@link #createResource()} returned null, or if b) this object has been
 * disposed (see {@link #delete()}. Subsequent calls to {@link #get()}
 * will always return null. It is recommended to call {@link #delete()} when
 * the resource is not needed anymore to free up references and to help
 * the garbage collector to free heap memory.
 * 
 * @author Manuel Schwarze
 *
 * @param <T> An arbitrary object.
 */
public abstract class SafeGuardedResource<T> {

	//
	// Constants
	//

	/**
	 * The life cycle state consists of three states. After calling
	 * the constructor it is set to Planned. After the first call
	 * to createResource() it is set to Created. After the first
	 * call to dispose() it is set to Disposed.
	 */
	public enum LifeCycleState {
		Planned, Created, Disposed;
	};

	//
	// Members
	//

	/** Determines, if this wrapped resource can be shared among threads. */
	private final boolean m_bShared;

	/** Determines, if the created resource is null. If so, it will always be "shared" as null. */
	private boolean m_bIsNull;

	/**
	 * Stores the life cycle state. We need this to know that also a null resource
	 * had been "created".
	 */
	private LifeCycleState m_state;

	/** Used as resource if resource can be shared among threads. */
	private T m_resource;

	/**
	 * A hash map that helps to assign a separate resource instance for every
	 * thread accessing the resource. It is null, if the resource is shared.
	 */
	private HashMap<Thread, T> m_hResourceAccess;

	//
	// Constructor
	//

	/**
	 * Creates a new resource access object for a resource that cannot be shared
	 * between different threads.
	 */
	public SafeGuardedResource() {
		this(false);
	}

	/**
	 * Creates a new resource access object, which manages a resource that can optionally
	 * be shared between threads. This "shared" mode has been introduced for performance reasons and
	 * applies to scenarios where a resource can be shared under certain circumstances.
	 * 
	 * @param bShared Set to true, if this resource can be shared among different threads.
	 * 		Default is false.
	 */
	public SafeGuardedResource(final boolean bShared) {
		m_bShared = bShared;
		m_bIsNull = false;
		m_state = LifeCycleState.Planned;
		m_resource = null;
		m_hResourceAccess = null;
	}

	/**
	 * Determines, if the resource can be shared among different threads.
	 * Default is false.
	 * 
	 * @return True, if resource can be shared. False otherwise.
	 */
	public final boolean isShared() {
		return m_bShared;
	}

	/**
	 * Returns the resource and creates it if this is the first access.
	 * If the resource is not shared it will be created for every thread
	 * that is accessing the resource.
	 * 
	 * @return The resource. Can be null, if the factory returned null
	 * 		or if the resource was disposed.
	 */
	public final synchronized T get() {
		T resource = null;

		if (m_bShared || m_bIsNull) {
			switch (m_state) {
			case Planned:
				resource = m_resource = createResourceAndCheckForNull();
				m_state = LifeCycleState.Created;
				break;
			case Created:
				resource = m_resource;
				break;
			case Disposed:
				break;
			}
		}
		else {
			switch (m_state) {
			case Planned:
				m_hResourceAccess = new HashMap<Thread, T>();
				m_state = LifeCycleState.Created;
				// Fall through into Created case - no break!
			case Created:
				resource = m_hResourceAccess.get(Thread.currentThread());
				if (resource == null) {
					resource = createResourceAndCheckForNull();
					if (!m_bIsNull) {
						m_hResourceAccess.put(Thread.currentThread(), resource);
					}
				}
				break;
			case Disposed:
				break;
			}
		}

		return resource;
	}

	/**
	 * Determines the current life cycle state of this object.
	 * 
	 * @return Life cycle state.
	 */
	public final synchronized LifeCycleState getState() {
		return m_state;
	}

	/**
	 * Disposes this resource. It sets all references to null.
	 * Subsequent access via get() method will return null and
	 * will not recreate the resource. Additionally, it will
	 * call {@link #disposeResource(Object)} with every instance
	 * of the resource.
	 */
	public final synchronized void delete() {
		m_state = LifeCycleState.Disposed;

		if (m_hResourceAccess != null) {
			final Collection<T> collResources = new HashSet<T>(m_hResourceAccess.values());

			// Clear resource list
			m_hResourceAccess.clear();
			m_hResourceAccess = null;

			// Dispose resources
			for (final T res : collResources) {
				disposeResource(res);
			}

			collResources.clear();
		}

		if (m_resource != null) {
			final T res = m_resource;

			// Clear resource list
			m_resource = null;

			// Dispose resource list
			disposeResource(res);
		}
	}

	//
	// Protected Methods
	//

	/**
	 * Creates an instance of the resource.
	 * 
	 * @return The resource or null.
	 */
	protected abstract T createResource();

	/**
	 * Disposes an instance of the resource. This can be optionally
	 * overridden by the implementing class.
	 * 
	 * @param res The resource to dispose. Can be null.
	 */
	protected void disposeResource(final T res) {
		// Does not do anything by default
	}

	//
	// Private Methods
	//

	/**
	 * Calls the overridden method {@link #createResource()} and
	 * stores, if that created resource is null, in which case
	 * the resource will be treated as "shared" resource, since
	 * sharing null will not cause any trouble.
	 * 
	 * @return The resource or null.
	 */
	private final T createResourceAndCheckForNull() {
		final T resource = createResource();
		m_bIsNull = (resource == null);
		return resource;
	}
}
