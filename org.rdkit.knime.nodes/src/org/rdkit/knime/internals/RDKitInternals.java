/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2016
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
package org.rdkit.knime.internals;

import java.util.List;

import org.knime.core.node.config.Config;

/**
 * To make the concept of Streaming Operator Internals more powerful this interface defines
 * methods that must be implemented by an internals class to load and save its state.
 * Multiple of such objects can be used together when using the class {@link StreamingOperatorInternalsBag}.
 * 
 * @author Manuel Schwarze
 */
public interface RDKitInternals<T extends RDKitInternals<?>> {
   
   /**
    * Loads internal content from a KNIME setting object and 
    * restores the state of the internal object fully.
    * 
    * @param settings Settings to read internal content from.
    *      Can be null to do reset the internal content without restoring it.
    */
   public void load(Config settings);
   
   /**
    * Saves the full state of internal content into a KNIME setting object.
    * Calling {@link #load(Config)} with the setting object will restore the full state.
    * 
    * @param settings Settings to store the internal content into. Can be null to do nothing.
    */
   public void save(Config settings);
   
   /**
    * Merges multiple instances of an internals object together. It can be assumed
    * that the passed in objects are all of exactly the same class.
    * The instance on which merge is called will not be part of the merging process.
    * 
    * @param listItems Internals to be merged together. Can be null or empty to return null.
    * 
    * @return The merged version of all passed in objects or the object itself, 
    *    if nothing was passed in for merging.
    */
   public T merge(List<T> listItems);
   
}
