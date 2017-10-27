/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2017
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
 *  Extension (and in particular that are based on subclasses of No  deModel,
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.BundleDefaultsScope;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.Preferences;

/**
 * Utility class to retrieve preference values from KNIME easily using the static
 * methods in a Java Snippet node. This can be helpful for debugging purposes
 * if the bug is related to unpropagated preferences, e.g. in a HPC environments.
 * 
 * @author Manuel Schwarze
 */
public class PreferenceUtils {

   /**
    * Wraps key data about an Eclipse preference.
    */
   public static class Preference {
      public String scope;
      public String key;
      public String value;
      public boolean active;
      
      public Preference(String scope, String key, String value, boolean active) {
         this.scope = scope;
         this.key = key;
         this.value = value;
         this.active = active;
      }
      
      public String toString() {
         return (this.active ? "ACTIVE: " : "") + this.key + " [" + this.scope + "] = " + this.value;
      }
   }
   
   /**
    * Retrieves all preferences defined in KNIME as sorted Preference array. Preferences can be defined in multiple
    * scopes: bundle_defaults (= hard coded in code), configuration (= taken from settings files),
    * default (= programmatically set as defaults), instance (= set by user in preferences dialog).
    * The highest defined scope of a preference defines the active value that SHOULD be used by KNIME.
    * For this reason the list will contain also a scope called "active" to make it easy to filter
    * out what the active preference should be. If a preference store cannot be access for some reason
    * a preference object will be created that contains as key the exception class and as value the error message.
    * Such a "preference" is always flagged as not active and serves only documentation purposes. 
    * 
    * This method can be helpful to investigate current settings of KNIME when a workflow is failing
    * remotely, e.g. on a cluster node or a server.
    * 
    * @return Array of preference definitions.
    */
   public static Preference[] getPreferences() {
      Map<String, Preference> mapPrefs = new HashMap<>(5000);
      
      // Find all key / value pairs for all scopes (incl. extra entry for active ones
      for (String scope : new String[] { BundleDefaultsScope.SCOPE, 
            ConfigurationScope.SCOPE, DefaultScope.SCOPE, InstanceScope.SCOPE }) {
         Preferences prefs = Platform.getPreferencesService().getRootNode().node(scope);
         traversePreferences(prefs, scope, mapPrefs);
      }
      
      // Declare active ones explicitly and remove standalone entries for actives
      List<String> listPrefsName = new ArrayList<>(mapPrefs.size());
      for (String key : mapPrefs.keySet()) {
         if (key.contains("[active]")) {
            Preference active = mapPrefs.get(key);
            mapPrefs.get(active.key + " [" + active.scope + "]").active = true;
         }
         else {
            listPrefsName.add(key);
         }
      }
      
      Collections.sort(listPrefsName, String.CASE_INSENSITIVE_ORDER);
      List<Preference> listPrefs = new ArrayList<>(listPrefsName.size());
      for (String key : listPrefsName) {
         listPrefs.add(mapPrefs.get(key));
      }
      
      return listPrefs.toArray(new Preference[listPrefs.size()]);
   }
   
   //
   // Private Methods
   //
   
   /**
    * Recursively called method to traverse preferences in the preferences tree. 
    * 
    * @param prefs Preference node to traverse.
    * @param scope Scope to be traversed.
    * @param mapPrefs Map to add all preferences of current Preferences node.
    */
   private static void traversePreferences(Preferences prefs, String scope, Map<String, Preference> mapPrefs) {
      // Get the full node path
      String strNodePath = prefs.absolutePath();

      // Cut off the pref store type, e.g. /instance/
      final int iOffset = strNodePath.indexOf("/", 1);
      strNodePath = strNodePath.substring(iOffset + 1);
      
      // Check direct keys first
      try {
         for (final String key : prefs.keys()) {
            final String strPrefPath = strNodePath + "/" + key;
            try {
               Preference pref = new Preference(scope, strPrefPath, prefs.get(key, "Undefined"), false); 
               // Important to assign the same object twice here
               mapPrefs.put(strPrefPath + " [" + scope + "]", pref);
               mapPrefs.put(strPrefPath + " [active]", pref); // Overwrites existing value - used for later consolidation
            }
            catch (Exception exc) {
               mapPrefs.put("ERROR getting " + strPrefPath + " [" + scope + "]", new Preference(scope, exc.getClass().getName(), exc.getMessage(), false));
            }
         }
      }
      catch (final Exception exc) {
         mapPrefs.put("ERROR getting keys [" + scope + "]", new Preference(scope, exc.getClass().getName(), exc.getMessage(), false));
      }

      // Then check sub nodes
      try {
         for (final String strNode : prefs.childrenNames()) {
            traversePreferences(prefs.node(strNode), scope, mapPrefs);
         }
      }
      catch (final Exception exc) {
         mapPrefs.put("ERROR getting children [" + scope + "]", new Preference(scope, exc.getClass().getName(), exc.getMessage(), false));
      }
   }
}
