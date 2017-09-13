/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2013-2017
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
package org.rdkit.knime.nodes.addconformers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import org.RDKit.DistanceGeom;
import org.RDKit.ForceField;
import org.RDKit.Int_Vect;
import org.RDKit.ROMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.util.MultiThreadWorker;
import org.rdkit.knime.nodes.AbstractRDKitNodeModel;
import org.rdkit.knime.types.RDKitAdapterCell;
import org.rdkit.knime.types.RDKitMolCellFactory;
import org.rdkit.knime.types.RDKitMolValue;
import org.rdkit.knime.util.InputDataInfo;
import org.rdkit.knime.util.SettingsUtils;
import org.rdkit.knime.util.WarningConsolidator;

/**
 * This class implements the node model of the RDKitAddConformers node
 * providing calculations based on the open source RDKit library.
 * Creates a new table with multiple conformers per input molecule. Each conformer is a copy of the molecule with different coordinates assigned.
 * nEach conformer row is mapped back to the input table molecule with an identifier - usually the row id - taken from an input table column.
 * @author Manuel Schwarze
 */
public class RDKitAddConformersNodeModel extends AbstractRDKitNodeModel {

   //
   // Constants
   //

   /** The logger instance. */
   protected static final NodeLogger LOGGER = NodeLogger
         .getLogger(RDKitAddConformersNodeModel.class);

   /** Input data info index for Mol value. */
   protected static final int INPUT_COLUMN_MOL = 0;

   /** Input data info index for ID value. */
   protected static final int INPUT_COLUMN_REFERENCE = 1;

   //
   // Members
   //

   /** Settings model for the column name of the input column. */
   private final SettingsModelString m_modelMoleculeInputColumnName =
         registerSettings(RDKitAddConformersNodeDialog.createMoleculeInputColumnNameModel());

   /** Settings model for the column name of the id column. */
   private final SettingsModelColumnName m_modelReferenceInputColumnName =
         registerSettings(RDKitAddConformersNodeDialog.createReferenceInputColumnNameModel());

   /** Settings model for the number of conformers. */
   private final SettingsModelInteger m_modelNumberOfConformers =
         registerSettings(RDKitAddConformersNodeDialog.createNumberOfConformersModel());

   /** Settings model for the maximum number of iterations. */
   private final SettingsModelInteger m_modelMaxIterations =
         registerSettings(RDKitAddConformersNodeDialog.createMaxIterationsModel());

   /** Settings model for the random seed. */
   private final SettingsModelInteger m_modelRandomSeed =
         registerSettings(RDKitAddConformersNodeDialog.createRandomSeedModel());

   /** Settings model for the prune RMS threshold. */
   private final SettingsModelDouble m_modelPruneRmsThreshold =
         registerSettings(RDKitAddConformersNodeDialog.createPruneRmsThresholdModel());

   /** Settings model for the option to use random coordinates. */
   private final SettingsModelBoolean m_modelUseRandomCoordinatesOption =
         registerSettings(RDKitAddConformersNodeDialog.createUseRandomCoordinatesOptionModel());

   /** Settings model for the option to enforce chirality. */
   private final SettingsModelBoolean m_modelEnforceChiralityOption =
         registerSettings(RDKitAddConformersNodeDialog.createEnforceChiralityOptionModel(),true);

   /** Settings model for the option to use experimental torsion angles. */
   private final SettingsModelBoolean m_modelUseExpTorsionAnglesOption =
         registerSettings(RDKitAddConformersNodeDialog.createUseExpTorsionAnglesOptionModel(),true);

   /** Settings model for the option to use random coordinates. */
   private final SettingsModelBoolean m_modelUseBasicKnowledgeOption =
         registerSettings(RDKitAddConformersNodeDialog.createUseBasicKnowledgeOptionModel(),true);

   /** Settings model for the box size multiplier. */
   private final SettingsModelDoubleBounded m_modelBoxSizeMultiplier =
         registerSettings(RDKitAddConformersNodeDialog.createBoxSizeMultiplierModel());

   /** Settings model for the option to use random coordinates. */
   private final SettingsModelString m_modelMoleculeOutputColumnName =
         registerSettings(RDKitAddConformersNodeDialog.createMoleculeOutputColumnNameModel());

   /** Settings model for the box size multiplier. */
   private final SettingsModelString m_modelReferenceOutputColumnName =
         registerSettings(RDKitAddConformersNodeDialog.createReferenceOutputColumnNameModel());

   /** Settings model for the option to cleanup a conformer after its calculation. */
   private final SettingsModelBoolean m_modelCleanupOption =
         registerSettings(RDKitAddConformersNodeDialog.createCleanupWithUffOptionModel(), true);

   //
   // Constructor
   //

   /**
    * Create new node model with one data in- and one out-port.
    */
   RDKitAddConformersNodeModel() {
      super(1, 1);
   }

   //
   // Protected Methods
   //

   /**
    * {@inheritDoc}
    */
   @Override
   protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
         throws InvalidSettingsException {
      // Reset warnings and check RDKit library readiness
      super.configure(inSpecs);

      // Auto guess the input column if not set - fails if no compatible column found
      SettingsUtils.autoGuessColumn(inSpecs[0], m_modelMoleculeInputColumnName, RDKitMolValue.class, 0,
            "Auto guessing: Using column %COLUMN_NAME% as molecule input.",
            "No RDKit Mol, SMILES or SDF compatible column in input table. Use the \"RDKit from Molecule\" " +
                  "node to convert SMARTS.", getWarningConsolidator());

      // Determines, if the input column exists - fails if it does not
      SettingsUtils.checkColumnExistence(inSpecs[0], m_modelMoleculeInputColumnName, RDKitMolValue.class,
            "Molecule input column has not been specified yet.",
            "Molecule input column %COLUMN_NAME% does not exist. Has the input table changed?");

      if (!m_modelReferenceInputColumnName.useRowID()) {
         final List<Class<? extends DataValue>> listValueClasses =
               new ArrayList<Class<? extends DataValue>>();
         listValueClasses.add(StringValue.class);
         listValueClasses.add(DoubleValue.class);
         SettingsUtils.checkColumnExistence(inSpecs[0], m_modelReferenceInputColumnName, listValueClasses,
               "ID column has not been specified yet.",
               "ID column %COLUMN_NAME% does not exist. Has the input table changed?");
      }

      // Auto guess the new molecule column name and make it unique
      final String strOutputMolColumnName = m_modelMoleculeOutputColumnName.getStringValue();
      if (strOutputMolColumnName == null || strOutputMolColumnName.isEmpty()) {
         m_modelMoleculeOutputColumnName.setStringValue(m_modelMoleculeInputColumnName.getStringValue() + " (Conformers)");
      }

      // Auto guess the new reference column name
      final String strOutputRefColumnName = m_modelReferenceOutputColumnName.getStringValue();
      if (strOutputRefColumnName == null || strOutputRefColumnName.isEmpty()) {
         m_modelReferenceOutputColumnName.setStringValue("Reference");
      }

      if (SettingsUtils.equals(m_modelMoleculeOutputColumnName.getStringValue(), m_modelReferenceOutputColumnName.getStringValue())) {
         throw new InvalidSettingsException("Both output columns cannot have the same name '" +
               m_modelMoleculeOutputColumnName.getStringValue() + "'.");
      }

      // Consolidate all warnings and make them available to the user
      generateWarnings();

      // Generate output specs
      return getOutputTableSpecs(inSpecs);
   }

   /**
    * This implementation generates input data info object for the input mol column
    * and connects it with the information coming from the appropriate setting model.
    * {@inheritDoc}
    */
   @Override
   protected InputDataInfo[] createInputDataInfos(final int inPort, final DataTableSpec inSpec)
         throws InvalidSettingsException {

      InputDataInfo[] arrDataInfo = null;

      // Specify input of table 1
      if (inPort == 0) {
         arrDataInfo = new InputDataInfo[2];
         arrDataInfo[INPUT_COLUMN_MOL] = new InputDataInfo(inSpec, null, m_modelMoleculeInputColumnName, "molecule",
               InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
               RDKitMolValue.class);
         arrDataInfo[INPUT_COLUMN_REFERENCE] = new InputDataInfo(inSpec, null, m_modelReferenceInputColumnName, "reference data",
               InputDataInfo.EmptyCellPolicy.TreatAsNull, null,
               StringValue.class, DoubleValue.class);
      }

      return (arrDataInfo == null ? new InputDataInfo[0] : arrDataInfo);
   }


   /**
    * Returns the output table specification of the specified out port.
    * 
    * @param outPort Index of output port in focus. Zero-based.
    * @param inSpecs All input table specifications.
    * 
    * @return The specification of all output tables.
    * 
    * @throws InvalidSettingsException Thrown, if the settings are inconsistent with
    *       given DataTableSpec elements.
    * 
    * @see #createOutputFactories(int)
    */
   @Override
   protected DataTableSpec getOutputTableSpec(final int outPort,
         final DataTableSpec[] inSpecs) throws InvalidSettingsException {
      DataTableSpec spec = null;
      List<DataColumnSpec> listSpecs;

      switch (outPort) {

      case 0:
         // Define output table
         listSpecs = new ArrayList<DataColumnSpec>();
         final InputDataInfo[] inputDataInfo = createInputDataInfos(0, inSpecs[0]);
         listSpecs.add(new DataColumnSpecCreator(m_modelMoleculeOutputColumnName.getStringValue(), RDKitAdapterCell.RAW_TYPE).createSpec());
         listSpecs.add(new DataColumnSpecCreator(m_modelReferenceOutputColumnName.getStringValue(), inputDataInfo[INPUT_COLUMN_REFERENCE].getDataType()).createSpec());

         spec = new DataTableSpec("Conformers", listSpecs.toArray(new DataColumnSpec[listSpecs.size()]));
         break;
      }

      return spec;
   }

   @Override
   protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
         throws InvalidSettingsException {
      super.loadValidatedSettingsFrom(settings);

      // Exception: For old nodes we will treat the row key match info option as "false", for new nodes as "true"
      try {
         m_modelUseExpTorsionAnglesOption.loadSettingsFrom(settings);
      }
      catch (final InvalidSettingsException excOrig) {
         m_modelUseExpTorsionAnglesOption.setBooleanValue(false);
      }
      try {
         m_modelUseBasicKnowledgeOption.loadSettingsFrom(settings);
      }
      catch (final InvalidSettingsException excOrig) {
         m_modelUseBasicKnowledgeOption.setBooleanValue(false);
      }
   }
   
   
   
   /**
    * {@inheritDoc}
    */
   @Override
   protected BufferedDataTable[] processing(final BufferedDataTable[] inData, final InputDataInfo[][] arrInputDataInfo,
         final ExecutionContext exec) throws Exception {
      final WarningConsolidator warnings = getWarningConsolidator();
      final DataTableSpec[] arrOutSpecs = getOutputTableSpecs(inData);

      // Contains the rows with the result column
      final BufferedDataContainer newTableData = exec.createDataContainer(arrOutSpecs[0]);

      // Get settings and define data specific behavior
      final long lTotalRowCount = inData[0].size();

      // Get calculation parameters
      final int iNumberOfConformers = m_modelNumberOfConformers.getIntValue();
      final int iMaxIterations = m_modelMaxIterations.getIntValue();
      final int iRandomSeed = m_modelRandomSeed.getIntValue();
      final double dPruneRmsThreshold = m_modelPruneRmsThreshold.getDoubleValue();
      final boolean bUseRandomCoordinates = m_modelUseRandomCoordinatesOption.getBooleanValue();
      final boolean bEnforceChirality = m_modelEnforceChiralityOption.getBooleanValue();
      final boolean bUseExpTorsionAngles = m_modelUseExpTorsionAnglesOption.getBooleanValue();
      final boolean bUseBasicKnowldge = m_modelUseBasicKnowledgeOption.getBooleanValue();
      final double dBoxSizeMultiplier = m_modelBoxSizeMultiplier.getDoubleValue();
      final boolean bCleanup = m_modelCleanupOption.getBooleanValue();

      if (lTotalRowCount > 0) {
         // Get settings and define data specific behavior
         final int iMaxParallelWorkers = (int)Math.ceil(1.5 * Runtime.getRuntime().availableProcessors());
         final int iQueueSize = 10 * iMaxParallelWorkers;
         final AtomicLong rowOutputIndex = new AtomicLong(0);
         
         // Calculate conformers
         new MultiThreadWorker<DataRow, DataRow[]>(iQueueSize, iMaxParallelWorkers) {

            /**
             * Computes the conformers.
             * 
             * @param row Input row.
             * @param index Index of row.
             * 
             * @return Null, if an empty input cell was encountered. Empty, if we should ignore the row
             *       (e.g. if randomization is used and the conformer is not calculated). Result row, if
             *       we have a valid conformer to be added to the result table.
             */
            @Override
            protected DataRow[] compute(final DataRow row, final long index) throws Exception {
               List<DataRow> listNewRows = null;

               // Get a unique wave id to mark RDKit Objects for cleanup
               final long lUniqueWaveId = createUniqueCleanupWaveId();

               try {
                  DataCell molCell = null;
                  final ROMol mol = markForCleanup(arrInputDataInfo[0][INPUT_COLUMN_MOL].getROMol(row), lUniqueWaveId);
                  final DataCell refCell = arrInputDataInfo[0][INPUT_COLUMN_REFERENCE].getCell(row);

                  // We use only cells, which are not missing (see also createInputDataInfos(...) )
                  if (mol != null) {
                     final ROMol molTemp = markForCleanup(new ROMol(mol), lUniqueWaveId);
                     final Int_Vect listConformerIds;
                     listConformerIds = markForCleanup(DistanceGeom.EmbedMultipleConfs(molTemp, iNumberOfConformers, 
                           iMaxIterations, iRandomSeed,
                           true /* clearConfs */, bUseRandomCoordinates, dBoxSizeMultiplier,
                           true /* randNegEig */, 1 /* numZeroFail */, dPruneRmsThreshold, 
                           null /* coordMap */, 1e-3 /* optmizerForceTol */,
                           false /* ignoreSmoothingFailures */,
                           bEnforceChirality,bUseExpTorsionAngles,bUseBasicKnowldge), lUniqueWaveId);

                     // Note: There will be no output row, if there are no conformers at all, only a warning
                     if (listConformerIds != null) {
                        // Loop through number of conformers and create molecules that target exactly one conformer
                        final int iSize = (int)listConformerIds.size();
                        for (int indexTarget = 0; indexTarget < iSize; indexTarget++) {

                           // Make a copy of the calculated molecule that still contains all conformers
                           final ROMol output = markForCleanup(new ROMol(molTemp), lUniqueWaveId);
                           final int iTargetConformerId = listConformerIds.get(indexTarget);

                           // Remove all conformers that are out of focus
                           for (int indexConf = 0; indexConf < iSize; indexConf++) {
                              final int iOtherConformerId = listConformerIds.get(indexConf);
                              if (iTargetConformerId != iOtherConformerId) {
                                 output.removeConformer(iOtherConformerId);
                              }
                           }

                           // Create a data row, if we have meaningful output (other checks could be added here)
                           if (output != null) {

                              // Cleanup the conformer molecule, if desired
                              if (bCleanup) {
                                 ForceField.UFFOptimizeMolecule(output);
                              }

                              molCell = RDKitMolCellFactory.createRDKitAdapterCell(output);
                              final DataRow rowNew = new DefaultRow(RowKey.createRowKey((long)indexTarget),
                                    new DataCell[] { molCell, refCell });
                              
                              if (listNewRows == null) {
                                 listNewRows = new ArrayList<>(5);
                              }
                              listNewRows.add(rowNew);
                           }
                        }
                     }
                     else {
                        warnings.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(), "Unable to calculate any conformers.");
                     }
                  }
                  else {
                     warnings.saveWarning(WarningConsolidator.ROW_CONTEXT.getId(), "Encountered empty input cell. It will be ignored.");
                  }
               }
               finally {
                  // Cleanup RDKit Objects
                  cleanupMarkedObjects(lUniqueWaveId);
               }

               return (listNewRows == null ? null : listNewRows.toArray(new DataRow[listNewRows.size()]));
            }
            

            /**
             * Adds the results to the table.
             * 
             * @param task Processing result for a row.
             */
            @Override
            protected void processFinished(final ComputationTask task)
                  throws ExecutionException, CancellationException, InterruptedException {
               // Null, if an empty input cell was encountered.
               // Empty, if we should ignore the row (e.g. if randomization is used and the conformer is not calculated).
               // Non-empty, if we have a valid conformer to be added to the result table.
               final DataRow[] arrResult = task.get();

               // Only consider valid results (which still could be empty)
               if (arrResult != null) {
                  if (arrResult.length > 0) {
                     for (final DataRow row : arrResult) {
                        newTableData.addRowToTable(new DefaultRow(RowKey.createRowKey(rowOutputIndex.getAndIncrement()), row));
                     }
                  }
               }

               // Check, if user pressed cancel (however, we will finish the method nevertheless)
               // Update the progress only every 20 rows
               if (task.getIndex() % 20 == 0) {
                  try {
                     AbstractRDKitNodeModel.reportProgress(exec, (int)task.getIndex(),
                           lTotalRowCount, task.getInput(),
                           new StringBuilder(" to calculate conformers [").append(getActiveCount()).append(" active, ")
                           .append(getFinishedTaskCount()).append(" pending]").toString());
                  }
                  catch (final CanceledExecutionException e) {
                     cancel(true);
                  }
               }
            };
         }.run(inData[0]);
      }

      exec.checkCanceled();
      exec.setProgress(1.0, "Finished Processing");

      newTableData.close();

      return new BufferedDataTable[] { newTableData.getTable() };
   }
}
