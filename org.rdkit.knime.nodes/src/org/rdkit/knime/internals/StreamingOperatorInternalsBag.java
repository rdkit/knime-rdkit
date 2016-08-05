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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.tree.TreeNode;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.Config;
import org.knime.core.node.streamable.StreamableOperatorInternals;

/**
 * This class acts as a bag to make it possible to handle multiple internals objects together.
 * They need to be named when put into this bag and will be accessed by their names again.
 * All of them are required to implement the interface RDKitInternals which defines methods
 * to load and save the state of these objects.
 * 
 * @author Manuel Schwarze
 */
public class StreamingOperatorInternalsBag extends StreamableOperatorInternals 
   implements RDKitInternals<StreamingOperatorInternalsBag> {

   // 
   // Constants
   //
   
   /** The logging instance. */
   private static final NodeLogger LOGGER = NodeLogger.getLogger(StreamingOperatorInternalsBag.class);
   
   //
   // Members
   // 
   
   /** Map with internal id to internal object. */
   private Map<String, RDKitInternals<?>> m_mapInterals; 
   
   //
   // Constructor
   //
   
   /**
    * Creates a new internals bag object.
    */
   public StreamingOperatorInternalsBag() {
      m_mapInterals = new HashMap<String, RDKitInternals<?>>();
   }
   
   //
   // Public Methods
   //
   
   /**
    * Adds the specified internals object to this bag and stores it under the specified name.
    * 
    * @param strName Name to use for storing the internals object. Must not be null.
    * @param item Internals object to store. Must not be null.
    * 
    * @return Returns just the reference to the bag again for easy concatenation of additions.
    */
   public synchronized StreamingOperatorInternalsBag withItem(String strName, RDKitInternals<?> item) {
      m_mapInterals.put(strName, item);
      return this;
   }
      
   /**
    * Removes the internals object with the specified name from this bag, if it is found.
    * 
    * @param strName Name of the internals object that shall be removed. Must not be null.
    * 
    * @return Returns just the reference to the bag again for easy concatenation of additions.
    */
   public synchronized StreamingOperatorInternalsBag removeItem(String strName) {
      m_mapInterals.remove(strName);
      return this;
   }
   
   /**
    * Returns an set of names of objects that are currently contained in this bag.
    * 
    * @return Names or empty String array, if bag is empty.
    */
   public synchronized Set<String> getNames() {
      return m_mapInterals.keySet();
   }
   
   /**
    * Retrieves the internals object stored under the specified name, or null, if it is not found.
    * 
    * @param strName Name of the internals object to be retrieved. Must not be null.
    * 
    * @return The internals object, if found, or null, if it is not found.
    */
   public synchronized RDKitInternals<?> getItem(String strName) {
      return m_mapInterals.get(strName);
   }
   
   @Override
   public synchronized void load(DataInputStream input) throws IOException {
      Config config = new NodeSettings(getClass().getSimpleName());
      config.load(input);
      load(config);
   }
   
   @Override
   public synchronized void save(DataOutputStream output) throws IOException {
      Config config = new NodeSettings(getClass().getSimpleName());
      save(config);
      config.saveToXML(output);
   }
   
   @Override
   public synchronized void load(Config config) {
      m_mapInterals.clear();
      for (Enumeration<TreeNode> e = config.children(); e.hasMoreElements(); ) {
         Config configItem = (Config)e.nextElement();
         String strName = configItem.getKey();
         try {
            String strClass = configItem.getString("internals.class");
            Object objInternals = Class.forName(strClass).newInstance();
            if (objInternals instanceof RDKitInternals) {
               ((RDKitInternals<?>)objInternals).load(configItem);
               m_mapInterals.put(strName, (RDKitInternals<?>)objInternals);
            }
         }
         catch (Exception exc) {
            LOGGER.debug("Unable to load or instantiate internals with name " + strName);
         }
      }
   }
   
   @Override
   public synchronized void save(Config config) {
      for (String strInternalNames : m_mapInterals.keySet()) {
         RDKitInternals<?> internals = m_mapInterals.get(strInternalNames);
         Config configItem = config.addConfig(strInternalNames);
         configItem.addString("internals.class", internals.getClass().getName());
         internals.save(configItem);
      }         
   }

   @Override
   public StreamingOperatorInternalsBag merge(List<StreamingOperatorInternalsBag> internals) {
      StreamingOperatorInternalsBag merged = new StreamingOperatorInternalsBag();
      
      // Determine first all objects in the bag by their names
      Set<String> setAllNames = new HashSet<>();
      for (StreamingOperatorInternalsBag bag : internals) {
         if (bag instanceof StreamingOperatorInternalsBag) {
            setAllNames.addAll(((StreamingOperatorInternalsBag)bag).getNames());
         }
      }
      
      // Go through all names and merge all their objects together
      for (String strName : setAllNames) {
         List<RDKitInternals<?>> listItems = new ArrayList<>();
         for (StreamingOperatorInternalsBag bag : internals) {
            RDKitInternals<?> item = bag.getItem(strName);
            if (item != null) {
               listItems.add(item);
            }
         }
         if (listItems.size() > 0) {
            try {
               RDKitInternals<?> onTheFly = listItems.get(0).getClass().newInstance();
               RDKitInternals<?> mergedItem = (RDKitInternals<?>)onTheFly.merge(listItems);
               merged.withItem(strName, mergedItem);
            }
            catch (InstantiationException | IllegalAccessException exc) {
               LOGGER.error("Unable to merge internal states for " + strName + " ... skipping it.", exc);
            }
         }
      }
      
      
      
      return merged;
   }
}
