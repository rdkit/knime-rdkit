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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.tree.TreeNode;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.Config;
import org.rdkit.knime.internals.RDKitInternals;

/**
 * This class gathers warnings from different situations and produces on request
 * a summary warning message taking into account how often a warning occurred
 * and in what context it occurred. It is also capable of merging multiple
 * warning consolidator objects together and generating one summary as well.
 * A context can be a row, a batch, a list of images, etc. When warnings are saved,
 * they can be assigned to such an context (specified only as contextId later on).
 * The warning consolidator then tracks how often a warning occurred within a context.
 * 
 * @author Manuel Schwarze
 */
public class WarningConsolidator implements RDKitInternals<WarningConsolidator> {

   /**
	 * A context consists of an id, a name (e.g. row) and a plural name (e.g. rows)
	 * and is used to separate warnings and assign them to specific contexts, e.g.
	 * rows and batches.
	 * 
	 * @author Manuel Schwarze
	 */
	public static class Context {

      //
		// Members
		//

		/** A calculated hash code for this context. This is returned when calling {@link #hashCode()}. */
		private final int m_iHashCode;

		/**
		 * Determines, if context information shall also be included in the generated consolidation
		 * of all context warnings, if there is only a single warning logged for this context.
		 */
		private final boolean m_bShowAlsoIfJustSingle;

		/** A unique id of this context. */
		private final String m_id;

		/** A friendly name of the context, e.g. Row, Batch, Line, Result, etc. Used in summary. */
		private final String m_name;

		/** A friendly plural name of the context, e.g. Rows, Batches, Lines, Results, etc. Used in summary. */
		private final String m_pluralName;

		//
		// Constructor
		//
		
		protected Context() {
		   // Used when class is deserialized
         m_id = null;
         m_name = null;
         m_pluralName = null;
         m_bShowAlsoIfJustSingle = false;

         m_iHashCode = (m_id + m_name + m_pluralName + m_bShowAlsoIfJustSingle).hashCode();
		}

		/**
		 * Creates a new context.
		 * 
		 * @param contextId Id of the context to be used later when referring to it. Must not be null.
		 * @param contextName Name of the context. Must not be null.
		 * @param contextNamePlural Plural name of the context. Must not be null.
		 * @param bShowAlsoIfJustSingle Set to false, if the summary should not mention the context, if there is only one total context.
		 */
		public Context(final String contextId, final String contextName, final String contextNamePlural,
				final boolean bShowAlsoIfJustSingle) {
			if (contextId == null || contextName == null || contextNamePlural == null) {
				throw new IllegalArgumentException("Context parameters must not be null.");
			}

			m_id = contextId;
			m_name = contextName;
			m_pluralName = contextNamePlural;
			m_bShowAlsoIfJustSingle = bShowAlsoIfJustSingle;

			m_iHashCode = (m_id + m_name + m_pluralName + m_bShowAlsoIfJustSingle).hashCode();
		}

		//
		// Public Methods
		//

		/**
		 * Returns the context id.
		 * 
		 * @return Context id.
		 */
		public String getId() {
			return m_id;
		}

		/**
		 * Returns the context name.
		 * 
		 * @return Context name.
		 */
		public String getName() {
			return m_name;
		}

		/**
		 * Returns the context plural name.
		 * 
		 * @return Context plural name.
		 */
		public String getPluralName() {
			return m_pluralName;
		}

		/**
		 * Returns true, if the context summary should also be shown, if there is just a single
		 * total context, e.g. one single batch.
		 * 
		 * @return True or false.
		 */
		public boolean isShownAlsoIfJustSingle() {
			return m_bShowAlsoIfJustSingle;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int hashCode() {
			return m_iHashCode;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(final Object o) {
			boolean bRet = false;

			if (o instanceof Context) {
				bRet = (m_iHashCode == ((Context)o).m_iHashCode);
			}

			return bRet;
		}
	}

	//
	// Constants
	//
	
	/** The logging instance. */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(WarningConsolidator.class);

	/** Pre-defined context for rows. */
	private static final Context NO_CONTEXT = new Context("noContext", "", "", true);

	/** Pre-defined context for rows. */
	public static final Context ROW_CONTEXT = new Context("row", "row", "rows", true);

	/** Pre-defined context for batches. */
	public static final Context BATCH_CONTEXT = new Context("batch", "batch", "batches", false);

	/** Prefix of a search string to be used for matching full strings only. */
	private static final String SEARCH_POLICY_FULL = "FULL:";

	/** Prefix of a search string to be used to treat it as regular expression. */
	private static final String SEARCH_POLICY_REGEX = "REGEX:";

	/** Prefix of a search string to be used for matching also sub strings. */
	private static final String SEARCH_POLICY_SUBSTRING = "SUB:";


	//
	// Members
	//

	/**
	 * List of all registered context, mapped from Context ID to Context object.
	 */
	private Map<String, Context> m_mapContexts;

	/**
	 * Stores warnings and how often they occurred in a certain context.
	 */
	private Map<String, Map<String, Integer>> m_hWarningOccurrences;

	//
	// Constructors
	//

	/**
	 * Creates a new warning consolidator without any specified contexts.
	 */
	public WarningConsolidator() {
		this(new Context[0]);
	}

	/**
	 * Creates a new warning consolidator with the specified contexts.
	 * 
	 * @param contexts Arbitrary list of contexts that will be used for registering
	 * 		warnings in the future.
	 */
	public WarningConsolidator(final Context... contexts) {
		m_mapContexts = new HashMap<String, Context>();
		m_hWarningOccurrences = new HashMap<String, Map<String, Integer>>();

		for (final Context context : contexts) {
			registerContext(context);
		}

		registerContext(NO_CONTEXT);
	}

	/**
	 * Creates a new warning consolidator based on existing warning consolidators.
	 * All warnings and contexts will be merged together into the new one.
	 * Later added warnings in the passed in warning consolidators will not be
	 * considered anymore.
	 * 
	 * @param consolidators Arbitrary list of warning consolidators.
	 */
	public WarningConsolidator(final WarningConsolidator... consolidators) {
		this();

		for (final WarningConsolidator wc : consolidators) {
			for (final Context context : wc.getContexts()) {
				registerContext(context);
			}
			for (final String contextId : wc.m_hWarningOccurrences.keySet()) {
				final Map<String, Integer> mapContextWarnings = m_hWarningOccurrences.get(contextId);
				merge(contextId, mapContextWarnings);
			}
		}
	}

	//
	// Public Methods
	//

	/**
	 * Registers the specified context. A context can be a row, a batch, an list of images, etc.
	 * When warnings are saved later they can be assigned to such an context (specified only
	 * as contextId later on). The warning consolidator then tracks how often a warning
	 * occurred within a context. If a context with the same contextId was already registered,
	 * it will override it.
	 * 
	 * @param context Context of future warnings.
	 */
	public void registerContext(final Context context) {
		if (context != null) {
			m_mapContexts.put(context.getId(), context);
		}
	}

	/**
	 * Returns all registered contexts.
	 * 
	 * @return Array of contexts. Never null, but maybe empty.
	 */
	public Context[] getContexts() {
		return m_mapContexts.values().toArray(new Context[m_mapContexts.size()]);
	}

	/**
	 * Determines the registered context for the specified contextId.
	 * 
	 * @param contextId 	Context Id of interest.
	 * 
	 * @return Context or null, if not registered.
	 */
	public Context getContext(final String contextId) {
		return m_mapContexts.get(contextId);
	}

	/**
	 * Deletes all warning messages.
	 */
	public void clear() {
		m_hWarningOccurrences.clear();
	}

	/**
	 * Saves a warning message for later summarizing all of them into a single warning message.
	 * This warning will be stored without a context and will not track occurrences.
	 * 
	 * @param warning Warning message to save. Can be null to do nothing.
	 */
	public synchronized void saveWarning(final String warning) {
		saveWarning(null, warning, 1);
	}

	/**
	 * Saves a warning message for later summarizing all of them into a single warning message.
	 * It will remember how often a certain warning message occurred.
	 * 
	 * @param contextId Context of the warning. Can be null, if warning does not belong to any context.
	 * @param warning Warning message to save. Can be null to do nothing.
	 */
	public synchronized void saveWarning(final String contextId, final String warning) {
		saveWarning(contextId, warning, 1);
	}

	/**
	 * Saves all warning messages from the passed in consolidator. Also, takes over
	 * all contexts of the passed in consolidator as well.
	 * 
	 * @param consolidator Warning consolidator. Can be null to do nothing.
	 */
	public synchronized void saveWarnings(final WarningConsolidator anotherConsolidator) {
		if (anotherConsolidator != null && anotherConsolidator != this) {
			for (final Context context : anotherConsolidator.getContexts()) {
				registerContext(context);
			}
			for (final String contextId : anotherConsolidator.m_hWarningOccurrences.keySet()) {
				final Map<String, Integer> mapContextWarnings = m_hWarningOccurrences.get(contextId);
				merge(contextId, mapContextWarnings);
			}
		}
	}

	/**
	 * Generates a string that contains all warning messages that occurred
	 * during the factoring process.
	 * 
	 * @param mapContextOccurrences Maps context ids to number of occurrences (e.g. number of rows). Can be null.
	 * 
	 * @return Warnings or null, if no warnings occurred.
	 */
	public String getWarnings(final Map<String, Long> mapContextOccurrences) {
		return getWarnings(mapContextOccurrences, null, null);
	}

	/**
	 * Generates a string that contains warning messages that occurred
	 * during the factoring process, but gives additionally the option to suppress
	 * certain warnings, if they are matching search criteria specified in the second and third
	 * parameter.
	 * 
	 * @param mapContextOccurrences Maps context ids to number of occurrences (e.g. number of rows). Can be null.
	 * @param listSuppressWarnings List of search strings. Every warning will be checked before consolidation,
	 * 		if it contains a search string that is part of the list. Can be null.
	 * 		Every search string in the list can start
	 * 		with either "FULL:" (default when nothing is specified), "REGEX:" or "SUB:",
	 * 		which influences the search and matching behavior.
	 * 		If "FULL:" (or nothing) is used the search string must match the entire warning string. If "REGEX:" is
	 * 		used Search strings will be interpreted as regular expression and succeeds if the result is not empty.
	 * 		If "SUB:" is used the search string is interpreted as substring.
	 * @param listSuppressContexts List of context ids to be suppressed. Can be null. Only warnings that don't belong to the specified
	 * 		context will be returned. To address warnings without a context use {@link #NO_CONTEXT}.getId().
	 * 
	 * @return Warnings or null, if no (non-suppressed) warnings occurred.
	 */
	public String getWarnings(final Map<String, Long> mapContextOccurrences, final List<String> listSuppressWarnings,
			final List<String> listSuppressContexts) {
		final StringBuilder sbSummary = new StringBuilder();
		final StringBuilder sbWarnings = new StringBuilder();
		final StringBuilder sbContextStats = new StringBuilder();

		// Sort contexts by id
		final List<String> listContextIds = StringUtils.sort(m_hWarningOccurrences.keySet());

		// Put warnings without any context first
		if (listContextIds.remove(NO_CONTEXT.getId())) {
			listContextIds.add(0, NO_CONTEXT.getId());
		}

		// Consolidate
		for (final String contextId : listContextIds) {
			sbWarnings.setLength(0); // Reset

			final Context context = m_mapContexts.get(contextId);
			final Map<String, Integer> m_hWarningOccurrencesInContext = m_hWarningOccurrences.get(contextId);

			// Determine, if we want to suppress the warning based on a passed in context id
			if (m_hWarningOccurrencesInContext != null && !m_hWarningOccurrencesInContext.isEmpty() &&
					(listSuppressContexts == null || !listSuppressContexts.contains(contextId))) {

				// Sort all warnings alphabetically
				SortedSet<String> setContextWarnings = new TreeSet<String>(m_hWarningOccurrencesInContext.keySet());
								
				// Process all sorted warnings
				for (final String warning : setContextWarnings) {

					// Determine, if we want to suppress the warning based on a passed in search criteria
					if (shouldWarningBeIncluded(warning, listSuppressWarnings)) {
						if (sbWarnings.length() > 0) {
							sbWarnings.append("\n");
						}

						// Find out how many times a warning occurred within a context
						long processed = -1; // Default is unknown
						final long occurred = m_hWarningOccurrencesInContext.get(warning);

						if (mapContextOccurrences != null) {
							final Long longProcessed = mapContextOccurrences.get(contextId);
							if (longProcessed != null) {
								processed = longProcessed.longValue();
							}
						}

						sbWarnings.append(warning);

						// Append context occurrence statistics
						if (!NO_CONTEXT.getId().equals(context.getId()) &&
								(context.isShownAlsoIfJustSingle() || processed == -1 || processed > 1)) {
							sbContextStats.setLength(0);

							if (processed == occurred) {
								sbContextStats.append("All " + context.getPluralName());
							}
							else if (processed > occurred){
								sbContextStats.append(occurred).append(" of ").append(processed).append(" ").
								append(processed == 1 ? context.getName() : context.getPluralName());
							}
							else {
								sbContextStats.append(occurred).append(" times");
							}

							sbWarnings.append(" [").append(sbContextStats.toString()).append("]");
						}
					}
				}

				if (sbSummary.length() > 0 && sbWarnings.length() > 0) {
					sbSummary.append("\n");
				}

				sbSummary.append(sbWarnings.toString());
			}
		}

		return (sbSummary.length() > 0 ? sbSummary.toString() : null);
	}

	/**
	 * Determines for the specific warning, if it should be included in the warning
	 * consolidation, or if it should be suppressed based on the second parameter.
	 * 
	 * @param warning Warning to check. Must not be null.
	 * @param listSuppressWarnings List of search strings. The warning will be checked,
	 * 		if it contains a search string that is part of the list. Can be null.
	 * 		Every search string in the list can start
	 * 		with either "FULL:" (default when nothing is specified), "REGEX:" or "SUB:",
	 * 		which influences the search and matching behavior.
	 * 		If "FULL:" (or nothing) is used the search string must match the entire warning string. If "REGEX:" is
	 * 		used Search strings will be interpreted as regular expression and succeeds if the result is not empty.
	 * 		If "SUB:" is used the search string is interpreted as substring.
	 * 
	 * @return True, if warning shall be included. False, if it should be suppressed.
	 */
	protected boolean shouldWarningBeIncluded(final String warning, final List<String> listSuppressWarnings) {
		// Pre-check
		if (warning == null) {
			throw new IllegalArgumentException("Warning must not be null.");
		}

		boolean bInclude = true;

		if (listSuppressWarnings != null) {
			for (final String criteria : listSuppressWarnings) {
				if (criteria.startsWith(SEARCH_POLICY_REGEX)) {
					if (warning.matches(criteria.substring(SEARCH_POLICY_REGEX.length()))) {
						bInclude = false;
						break;
					}
				}
				else if (criteria.startsWith(SEARCH_POLICY_SUBSTRING)) {
					if (warning.indexOf(criteria.substring(SEARCH_POLICY_SUBSTRING.length())) != -1) {
						bInclude = false;
						break;
					}
				}
				else if (criteria.startsWith(SEARCH_POLICY_FULL)) {
					if (warning.equals(criteria.substring(SEARCH_POLICY_FULL.length()))) {
						bInclude = false;
						break;
					}
				}
				else {
					if (warning.equals(criteria)) {
						bInclude = false;
						break;
					}
				}
			}
		}

		return bInclude;
	}
	
	//
	// Streaming Operator Internal API 
	//
	
	/**
	 * Loads internal content from a KNIME setting object and 
	 * restores the state of the internal object fully.
	 * 
	 * @param settings Settings to read internal content from.
	 *      Can be null to do reset the internal content without restoring it.
	 */
   public void load(Config settings) {
      if (settings != null) {
         m_hWarningOccurrences = new HashMap<>();
         m_mapContexts = new HashMap<>();
         
         try {
            Config contexts = settings.getConfig("contextMap");
            for (Enumeration<TreeNode> e = contexts.children(); e.hasMoreElements(); ) {
               Config context = (Config)e.nextElement();
               String strId = context.getKey();
               String strName = context.getString("name");
               String strPluralName = context.getString("pluralName");
               boolean bShownAlsoIfJustSingle = context.getBoolean("shownAlsoIfJustSingle");
               m_mapContexts.put(strId, new Context(strId, strName, strPluralName, bShownAlsoIfJustSingle));
            }
            
            Config warnings = settings.getConfig("warningsMap");
            for (Enumeration<TreeNode> e1 = warnings.children(); e1.hasMoreElements(); ) {
               Config context = (Config)e1.nextElement();
               String contextId = context.getKey();
               for (Enumeration<TreeNode> e2 = context.children(); e2.hasMoreElements(); ) {
                  Config warning = (Config)e2.nextElement();
                  String strWarning = warning.getString("warning");
                  int iOccurrences = warning.getInt("occurrences");
                  Map<String, Integer> mapWarnings = m_hWarningOccurrences.get(contextId);
                  if (mapWarnings == null) {
                     mapWarnings = new HashMap<>();
                     m_hWarningOccurrences.put(contextId, mapWarnings);
                  }
                  mapWarnings.put(strWarning, iOccurrences);
               }
            }
         }
         catch (InvalidSettingsException exc) {
            LOGGER.debug("Unable to load all WarningConsolidator settings from Internals object.");
         }
      }
      else { // Use defaults
         m_hWarningOccurrences = new HashMap<>();
         m_mapContexts = new HashMap<>();
      }
   }
	
   /**
    * Saves the full state of internal content into a KNIME setting object.
    * Calling {@link #load(Config)} with the setting object will restore the full state.
    * 
    * @param settings Settings to store the internal content into. Can be null to do nothing.
    */
	public void save(Config settings) {
      if (settings != null) {
         Config warnings = settings.addConfig("warningsMap");
         for (String strContextId : m_hWarningOccurrences.keySet()) {
            Map<String, Integer> mapWarningsInContext = m_hWarningOccurrences.get(strContextId);
            Config warningMap = warnings.addConfig(strContextId);
            int iCount = 0;
            for (String strWarning : mapWarningsInContext.keySet()) {
               Config warningItem = warningMap.addConfig("warning_" + (iCount++));
               warningItem.addString("warning", strWarning);
               warningItem.addInt("occurrences", mapWarningsInContext.get(strWarning));
            }
         }         
         Config contexts = settings.addConfig("contextMap");
         for (String strKey : m_mapContexts.keySet()) {
            Context context = m_mapContexts.get(strKey);
            Config contextItem = contexts.addConfig(context.getId());
            contextItem.addString("name", context.getName());
            contextItem.addString("pluralName", context.getPluralName());
            contextItem.addBoolean("shownAlsoIfJustSingle", context.isShownAlsoIfJustSingle());
         }
      }
	}
	
	@Override
	public WarningConsolidator merge(List<WarningConsolidator> internals) {
	   WarningConsolidator merged = new WarningConsolidator(internals.toArray(new WarningConsolidator[0]));
	   return merged;
	}

	//
	// Private Methods
	//

	/**
	 * Saves a warning message for later summarizing all of them into a single warning message.
	 * It will consider the specified number of occurrences.
	 * 
	 * @param contextId Context of the warning. Can be null, if warning does not be belong to any context.
	 * @param warning Warning message to save. Can be null to do nothing.
	 * @param occurrences Number of occurrences.
	 */
	private synchronized void saveWarning(final String contextId, final String warning, final int occurrences) {
		if (warning != null) {
			Context context = NO_CONTEXT;

			if (contextId != null) {
				context = getContext(contextId);
			}

			// Register context, if not found
			if (context == null) {
				context = new Context(contextId, contextId, contextId + "s", true);
				registerContext(context);
			}

			// Find warning map for context, create if not found
			Map<String, Integer> mapContextWarnings = m_hWarningOccurrences.get(context.getId());
			if (mapContextWarnings == null) {
				mapContextWarnings = new HashMap<String, Integer>();
				m_hWarningOccurrences.put(context.getId(), mapContextWarnings);
			}

			// Find warning and increase occurrence, create if not found
			final Integer occurred = mapContextWarnings.get(warning);
			int occurredNew = occurrences;

			if (occurred != null) {
				occurredNew = occurred.intValue() + occurrences;
			}

			mapContextWarnings.put(warning, occurredNew);
		}
	}

	/**
	 * Merges the specified warnings and their occurrences into this Warning Consolidator object.
	 * 
	 * @param contextId Context ID of warnings. Can be null to use the NO_CONTEXT.
	 * @param mapContextWarnings Warnings and their occurrences. Can be null.
	 */
	private void merge(String contextId, final Map<String, Integer> mapContextWarnings) {
		if (mapContextWarnings != null) {
			if (contextId == null) {
				contextId = NO_CONTEXT.getId();
			}
			for (final String warning : mapContextWarnings.keySet()) {
				saveWarning(warning, contextId, mapContextWarnings.get(warning));
			}
		}
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException {
      WarningConsolidator w1 = new WarningConsolidator(new WarningConsolidator.Context("Rows", "Row", "Rows", true),
            new WarningConsolidator.Context("Columns", "Column", "Columns", true));
      w1.saveWarning("Rows", "This is a row test warning");
      w1.saveWarning("Rows", "This is a row test warning");
      w1.saveWarning("Columns", "This is a column test warning");
      w1.saveWarning("Columns", "This is a column test warning");
      w1.saveWarning("Just a simple general test warning");
      
      Config config = new NodeSettings("Test");
      w1.save(config);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      config.saveToXML(out);
      out.close();
      byte[] arrSaved = out.toByteArray();
      
      ByteArrayInputStream in = new ByteArrayInputStream(arrSaved);
      Config config2 = new NodeSettings("Test");
      config2.load(in);
      WarningConsolidator w2 = new WarningConsolidator();
      w2.load(config2);
      
      System.out.println(w2);
   }
}
