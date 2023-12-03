/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C)2014-2023
 *  Novartis Pharma AG, Switzerland
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
 * -------------------------------------------------------------------
 *
 */
package org.rdkit.knime.nodes.mcs;

import java.util.HashMap;
import java.util.Map;

import org.RDKit.MCSResult;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.RDKit.ROMol_Vect;
import org.knime.chem.types.SmartsCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;

/**
 * This utility class offers MCS functionality based on the RDKit.
 * 
 * @author Manuel Schwarze
 */
public final class MCSUtils {

	//
	// Constants
	//

	/** Index of result data cells for SMARTS value. */
	public static final int SMARTS_INDEX = 0;

	/** Index of result data cells for atom number value. */
	public static final int ATOM_NUMBER_INDEX = 1;

	/** Index of result data cells for bond number value. */
	public static final int BOND_NUMBER_INDEX = 2;

	/** Index of result data cells for timed out value. */
	public static final int TIMED_OUT_INDEX = 3;

	/** The logging instance. */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(MCSUtils.class);

	//
	// Static Methods
	//

	/**
	 * Calculates the MCS of the passed in RDKit molecules.
	 * 
	 * @param mols List of RDKit molecules. It is responsibility of the caller to clean those up to free memory.
	 * @param dThreshold Fraction of molecules that the MCS must cover. Defaults to 1.0. 0.0 &lt; threshold &#8804; 1.0.
	 * @param bRingMatchesRingOnly Ring matches ring only option. Default is false.
	 * @param bCompleteRingsOnly Complete rings only option. Default is false.
	 * @param bMatchValencesOption Match valences option. Default is false.
	 * @param atomComparison Arom comparison mode. Must not be null.
	 * @param bondComparison Bond comparison mode. Must not be null.
	 * @param iTimeout Timeout for the calculation.
	 * @param exec Optional execution context. Can be null. If set, it will be used to show progress and
	 * 		to let a user interrupt the calculation.
	 * 
	 * @return Array of data cells with MCS results.
	 * 
	 * @throws Exception Thrown, if MCS could not be calculated.
	 */
	public static final DataCell[] calculateMCS(final ROMol_Vect mols, final double dThreshold,
			final boolean bRingMatchesRingOnly, final boolean bCompleteRingsOnly,
			final boolean bMatchValencesOption,
			final AtomComparison atomComparison, final BondComparison bondComparison,
			final int iTimeout, final ExecutionContext exec) throws Exception {
		final DataCell[] arrResults = new DataCell[4];

		// Calculate MCS
		if (exec != null) {
			exec.setMessage("Calculating MCS");
		}

		final int iNumberOfMolecules = (mols == null ? 0 : (int)mols.size());

		// Handle special case: No input molecules
		if (iNumberOfMolecules == 0) {
			arrResults[SMARTS_INDEX] = DataType.getMissingCell();
			arrResults[ATOM_NUMBER_INDEX] = DataType.getMissingCell();
			arrResults[BOND_NUMBER_INDEX] = DataType.getMissingCell();
			arrResults[TIMED_OUT_INDEX] = DataType.getMissingCell();
		}

		// Handle special case: Only 1 input molecule
		else if (iNumberOfMolecules == 1) {
			final ROMol molMcs = mols.get(0);

			final String strMcsSmarts = RDKFuncs.MolToSmarts(molMcs);

			if (strMcsSmarts == null) {
				arrResults[SMARTS_INDEX] = DataType.getMissingCell();
				arrResults[ATOM_NUMBER_INDEX] = DataType.getMissingCell();
				arrResults[BOND_NUMBER_INDEX] = DataType.getMissingCell();
				arrResults[TIMED_OUT_INDEX] = DataType.getMissingCell();
			}
			else {
				arrResults[SMARTS_INDEX] = SmartsCellFactory.create(strMcsSmarts);
				arrResults[ATOM_NUMBER_INDEX] = new IntCell((int)molMcs.getNumAtoms());
				arrResults[BOND_NUMBER_INDEX] = new IntCell((int)molMcs.getNumBonds());
				arrResults[TIMED_OUT_INDEX] = BooleanCellFactory.create(false);
			}
		}

		// Handle normal case: Process MCS in a separate thread
		else {
			final Map<String, Object> mapResult = new HashMap<String, Object>();
			final Thread thread = new Thread(new Runnable() {

				@Override
				public void run() {
					MCSResult mcs = null;

					try {
						mcs = RDKFuncs.findMCS(mols, true,
								dThreshold, iTimeout, false,
								bMatchValencesOption, bRingMatchesRingOnly, bCompleteRingsOnly,
								false /* Match Chiral Tag */,
								atomComparison.getRDKitComparator(), bondComparison.getRDKitComparator());
						if (mcs != null) {
							mapResult.put("MCS", mcs.getSmartsString());
							mapResult.put("NumAtoms", (int)mcs.getNumAtoms());
							mapResult.put("NumBonds", (int)mcs.getNumBonds());
							mapResult.put("Canceled", mcs.getCanceled());
						}
					}
					catch (final Throwable e) {
						// We are interested in OutOfMemory or any other error as well, but
						// we need to rethrow the following to let the Java VM do it's job properly.
						if (e instanceof ThreadDeath) {
							throw (ThreadDeath)e;
						}

						LOGGER.error("Calculating MCS failed. " + e.getMessage(), e);
						mapResult.put("Exception", e);
					}
					finally {
						if (mcs != null) {
							mcs.delete();
						}
					}
				}

			}, "MCS Calculation");

			thread.setDaemon(true);
			thread.start();

			if (exec != null) {
				// Wait for the thread to finish or for the user to cancel
				final int iSizeBased = (int)(iNumberOfMolecules / 10.0d * 100.0d);
				final int iTimeoutBased = (int)(iTimeout * 1000.0d / 100.0d);
				final int iProgressInterval = (int)(Math.max(500, Math.min(5000.0d, Math.min(iSizeBased, iTimeoutBased))));
				LOGGER.debug("Interval based on size: " + iSizeBased);
				LOGGER.debug("Interval based on timeOut: " + iTimeoutBased);
				LOGGER.debug("=> MCS Progress Update Interval: " + iProgressInterval);
				AbstractRDKitNodeModel.monitorWorkingThreadExecution(thread, exec, iProgressInterval, true, false);
			}
			else {
				try {
					thread.join();
				}
				catch (final InterruptedException excInterrupted) {
					// This gets thrown when the user cancels
					throw new CanceledExecutionException();
				}
			}

			// Re-throw an exception
			final Throwable exc = (Throwable)mapResult.get("Exception");
			if (exc != null) {
				if (exc instanceof Exception) {
					throw (Exception)exc;
				}
				else if (exc instanceof OutOfMemoryError) {
					throw new RuntimeException("KNIME has run out of memory.\n" +
							"You may increase memory by changing the parameter -Xmx " +
							"in the knime.ini file.\nA restart of KNIME is recommended now.");
				}
				else {
					throw new RuntimeException(exc.getMessage(), exc);
				}
			}

			// Process MCS results
			arrResults[SMARTS_INDEX] = mapResult.containsKey("MCS") ?
					SmartsCellFactory.create((String)mapResult.get("MCS")) :
						DataType.getMissingCell();
					arrResults[ATOM_NUMBER_INDEX] = mapResult.containsKey("NumAtoms") ?
							new IntCell(((Integer)mapResult.get("NumAtoms")).intValue()) :
								DataType.getMissingCell();
							arrResults[BOND_NUMBER_INDEX] = mapResult.containsKey("NumBonds") ?
									new IntCell(((Integer)mapResult.get("NumBonds")).intValue()) :
										DataType.getMissingCell();
									arrResults[TIMED_OUT_INDEX] = mapResult.containsKey("Canceled") ?
											BooleanCellFactory.create(((Boolean)mapResult.get("Canceled")).booleanValue()) :
												DataType.getMissingCell();
		}

		return arrResults;
	}

	//
	// Constructor
	//

	/**
	 * This constructor serves only the purpose to avoid instantiation of this class.
	 */
	private MCSUtils() {
		// To avoid instantiation of this class.
	}

}
