/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C)2010-2023
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
package org.rdkit.knime.types;

import java.util.Arrays;

import org.RDKit.ChemicalReaction;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;
import org.knime.core.node.NodeLogger;

/**
 * Smiles Data Value interface. (Only a wrapper for the underlying string)
 *
 * @author Greg Landrum
 */
public interface RDKitReactionValue extends DataValue {

   //
   // Constants
   //
   
   /** The logger instance. */
   static final NodeLogger LOGGER = NodeLogger.getLogger(RDKitMolValue.class);

    /**
     * Meta information to this value type.
     *
     * @see DataValue#UTILITY
     */
    public static final UtilityFactory UTILITY = new RDKUtilityFactory();

    /**
     * Returns the RDKit Reaction
     *
     * @return an RDKit ChemicalReaciton
     */
    ChemicalReaction getReactionValue();

    /**
     * Returns the Smiles string of the molecule.
     *
     * @return a String value
     */
    String getSmilesValue();

    /**
     * Checks, if the passed in reactions are the same.
     * 
     * @param reaction1
     *            Reaction 1 to check. Can be null.
     * @param reaction2
     *            Reaction 1 to check. Can be null.
     * 
     * @return True, if binary representations of the reactions is exactly the
     *         same. Also true, if null was passed in for both reactions. False
     *         otherwise.
     */
    public static boolean equals(RDKitReactionValue reaction1, RDKitReactionValue reaction2) {
       boolean bSame = false;

       if (reaction1 == null && reaction2 == null) {
          bSame = true;
       } 
       else if (reaction1 != null && reaction2 != null) {
          ChemicalReaction reactionRDKit1 = null;
          ChemicalReaction reactionRDKit2 = null;
          byte[] byteContent1, byteContent2;

          try {
             reactionRDKit1 = reaction1.getReactionValue();
             byteContent1 =  RDKitReactionCell.toByteArray(reactionRDKit1);
             reactionRDKit2 = reaction2.getReactionValue();
             byteContent2 = RDKitReactionCell.toByteArray(reactionRDKit2);
             bSame = Arrays.equals(byteContent1, byteContent2);
          } 
          catch (Exception exc) {
             // If something goes wrong the reactions are considered NOT equal
             LOGGER.error("Unable to compare two RDKit reactions.", exc);
          } 
          finally {
             /* The current implementation of the RDKitReactionCell holds reactions in memory and frees them in finalize()
                Hence we must not delete the value here!
             if (reactionRDKit1 != null) {
                reactionRDKit1.delete();
             }
             if (reactionRDKit2 != null) {
                reactionRDKit2.delete();
             }
             */
          }
       }

       return bSame;
    }
    
    /** Implementations of the meta information of this value class. */
    public static class RDKUtilityFactory extends UtilityFactory {
        /** Singleton icon to be used to display this cell type. */
        // private static final Icon ICON = loadIcon(RDKitMolValue.class,
        // "../icons/chem.png");

        private static final DataValueComparator COMPARATOR =
                new DataValueComparator() {
                    @Override
                    protected int compareDataValues(final DataValue v1,
                            final DataValue v2) {
                        int count1 =
                                (int)((RDKitReactionValue)v1)
                                        .getReactionValue()
                                        .getNumReactantTemplates();
                        int count2 =
                                (int)((RDKitReactionValue)v2)
                                        .getReactionValue()
                                        .getNumReactantTemplates();
                        if (count1 == count2) {
                            count1 =
                                    (int)((RDKitReactionValue)v1)
                                            .getReactionValue()
                                            .getNumProductTemplates();
                            count2 =
                                    (int)((RDKitReactionValue)v2)
                                            .getReactionValue()
                                            .getNumProductTemplates();

                        }
                        return count1 - count2;
                    }
                };

        /** Only subclasses are allowed to instantiate this class. */
        protected RDKUtilityFactory() {
        }

        /**
         * {@inheritDoc}
         */
        // @Override
        // public Icon getIcon() {
        // return ICON;
        // }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataValueComparator getComparator() {
            return COMPARATOR;
        }
    }
}
