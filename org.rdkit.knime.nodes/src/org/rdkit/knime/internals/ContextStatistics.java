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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import javax.swing.tree.TreeNode;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.Config;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class implements RDKitInternals functionality like loading, saving and merging
 * for the context statistics map which is required to generate meaningful warning
 * messages.
 * 
 * @author Manuel Schwarze
 */
public class ContextStatistics extends HashMap<String, Long> implements RDKitInternals<ContextStatistics> {

   //
   // Constants
   //
   
   /** Serial number. */
   private static final long serialVersionUID = 3891455721182535803L;
   
   /** The logging instance. */
   private static final NodeLogger LOGGER = NodeLogger.getLogger(WarningConsolidator.class);

   //
   // Constructor
   //
   
   public ContextStatistics() {
      super();
   }

   //
   // Public Methods
   //
   
   /**
    * Increases the statistics counter of the specified context by 1.
    * 
    * @param context Context to be increase the counter for. Can be null to do nothing.
    */
   public void countItem(String context) {
      if (context != null) {
         if (containsKey(context)) {
            put(context, get(context).longValue() + 1l);
         }
         else {
            put(context, 1l);
         }
      }
   }
   
   @Override
   public void load(Config settings) {
      clear();
      if (settings != null) {
         for (Enumeration<TreeNode> e = settings.children(); e.hasMoreElements(); ) {
            Config contextStats = (Config)e.nextElement();
            String strId = contextStats.getKey();
            long lCount = 0;
            try {
               lCount = contextStats.getLong("count");
            }
            catch (InvalidSettingsException exc) {
               LOGGER.error("Unable to load internal context statistics data for " + strId + " context.", exc);
            }
            put(strId, lCount);
         }
      }
   }

   @Override
   public void save(Config settings) {
      if (settings != null) {
         for (String context : keySet()) {
            settings.addLong(context, get(context));
         }
      }
   }

   @Override
   public ContextStatistics merge(List<ContextStatistics> internals) {
      ContextStatistics merged = new ContextStatistics();

      if (internals != null) {
         for (ContextStatistics item : internals) {
            for (String context : item.keySet()) {
               long lCount =  item.get(context);
               if (merged.containsKey(context)) {
                  merged.put(context, merged.get(context).longValue() + lCount);
               }
               else {
                  merged.put(context, lCount);
               }
            }
         }
      }
      
      return merged;
   }

}
