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


/**
 * This interface defines methods to register RDKit objects, which are subject of later
 * cleanup.
 * 
 * @author Manuel Schwarze
 */
public interface RDKitObjectCleaner {

	/**
	 * Creates a new wave id. This id must be unique in the context of the overall runtime
	 * of the Java VM, at least in the context of the same class loader and memory area.
	 * 
	 * @return Unique wave id.
	 */
	long createUniqueCleanupWaveId();

	/**
	 * Registers an RDKit based object, which must have a delete() method implemented
	 * for freeing up resources later. The cleanup will happen for all registered
	 * objects when the method {@link #cleanupMarkedObjects()} is called.
	 * 
	 * @param <T> Any class that implements a delete() method to be called to free up resources.
	 * @param rdkitObject An RDKit related object that should free resources when not
	 * 		used anymore. Can be null.
	 * 
	 * @return The same object that was passed in. Null, if null was passed in.
	 * 
	 * @see #markForCleanup(Object, int)
	 */
	<T extends Object> T markForCleanup(T rdkitObject);

	/**
	 * Registers an RDKit based object that is used within a certain block (wave). $
	 * This object must have a delete() method implemented for freeing up resources later.
	 * The cleanup will happen for all registered objects when the method
	 * {@link #cleanupMarkedObjects(int)} is called with the same wave.
	 * 
	 * @param <T> Any class that implements a delete() method to be called to free up resources.
	 * @param rdkitObject An RDKit related object that should free resources when not
	 * 		used anymore. Can be null.
	 * @param wave A number that identifies objects registered for a certain "wave".
	 * 
	 * @return The same object that was passed in. Null, if null was passed in.
	 * 
	 * @see #markForCleanup(Object)
	 */
	<T extends Object> T markForCleanup(T rdkitObject, long wave);

	/**
	 * Frees resources for all objects that have been registered prior to this last
	 * call using the method {@link #cleanupMarkedObjects()}.
	 */
	void cleanupMarkedObjects();

	/**
	 * Frees resources for all objects that have been registered prior to this last
	 * call for a certain wave using the method {@link #cleanupMarkedObjects(int)}.
	 * 
	 * @param wave A number that identifies objects registered for a certain "wave".
	 */
	void cleanupMarkedObjects(long wave);

}
