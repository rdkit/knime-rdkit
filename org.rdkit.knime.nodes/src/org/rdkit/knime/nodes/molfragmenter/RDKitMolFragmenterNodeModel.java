/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2010
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
package org.rdkit.knime.nodes.molfragmenter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.RDKit.Int_Int_Vect_List_Map;
import org.RDKit.Int_Vect;
import org.RDKit.Int_Vect_List;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.rdkit.knime.RDKitTypesPluginActivator;
import org.rdkit.knime.types.RDKitMolCellFactory;
import org.rdkit.knime.types.RDKitMolValue;

/**
 *
 * @author Greg Landrum
 */
public class RDKitMolFragmenterNodeModel extends NodeModel {

    private final SettingsModelString m_first =
            RDKitMolFragmenterNodeDialogPane.createFirstColumnModel();
    private final SettingsModelIntegerBounded m_minPath =
        RDKitMolFragmenterNodeDialogPane.createMinPathModel();

    private final SettingsModelIntegerBounded m_maxPath =
        RDKitMolFragmenterNodeDialogPane.createMaxPathModel();

    /**
     * Create new node model with one data in- and one outport.
     */
    RDKitMolFragmenterNodeModel() {
        super(1, 2);
    }

    private DataTableSpec[] createOutSpecs(final DataTableSpec[] inSpecs) {
        Vector<DataColumnSpec> cSpec = new Vector<DataColumnSpec>();
        DataColumnSpecCreator crea =
            new DataColumnSpecCreator("Fragment Index", IntCell.TYPE);
        cSpec.add(crea.createSpec());
       	crea=new DataColumnSpecCreator("Fragment", RDKitMolCellFactory.TYPE);
       	cSpec.add(crea.createSpec());
       	crea=new DataColumnSpecCreator("Fragment SMILES", StringCell.TYPE);
       	cSpec.add(crea.createSpec());
        crea=new DataColumnSpecCreator("Fragment Size", IntCell.TYPE);
        cSpec.add(crea.createSpec());
        crea=new DataColumnSpecCreator("Count", IntCell.TYPE);
        cSpec.add(crea.createSpec());
        DataTableSpec fSpec =
            new DataTableSpec("output 1",
                    cSpec.toArray(new DataColumnSpec[cSpec.size()]));

        cSpec.clear();
        for(int i=0;i<inSpecs[0].getNumColumns();i++){
        	cSpec.add(inSpecs[0].getColumnSpec(i));
        }
       	crea=new DataColumnSpecCreator("Fragment indices",ListCell.getCollectionType(IntCell.TYPE));
       	cSpec.add(crea.createSpec());
        DataTableSpec mSpec =
            new DataTableSpec("output 2",
                    cSpec.toArray(new DataColumnSpec[cSpec.size()]));

        return new DataTableSpec[]{fSpec, mSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // check whether native RDKit library has been loaded successfully
        RDKitTypesPluginActivator.checkErrorState();

        if (null == m_first.getStringValue()) {
            List<String> compatibleCols = new ArrayList<String>();
            for (DataColumnSpec c : inSpecs[0]) {
                if (c.getType().isCompatible(RDKitMolValue.class)) {
                    compatibleCols.add(c.getName());
                }
            }
            if (compatibleCols.size() == 1) {
                // auto-configure
                m_first.setStringValue(compatibleCols.get(0));
            } else if (compatibleCols.size() > 1) {
                // auto-guessing
                m_first.setStringValue(compatibleCols.get(0));
                setWarningMessage("Auto guessing: using column \""
                        + compatibleCols.get(0) + "\".");
            } else {
                throw new InvalidSettingsException("No RDKit Mol compatible "
                        + "column in input table. Use \"Molecule to RDKit\" "
                        + "node for Smiles or SDF.");
            }
        }
        if(m_minPath.getIntValue() > m_maxPath.getIntValue() ){
        	throw new InvalidSettingsException("minimum path length is larger than maximum path length.");
        }

        // further input spec check
        findColumnIndices(inSpecs[0]);

        return createOutSpecs(inSpecs);
    }

    private int[] findColumnIndices(final DataTableSpec spec)
            throws InvalidSettingsException {
        String first = m_first.getStringValue();
        if (first == null) {
            throw new InvalidSettingsException("Not configured yet");
        }
        int firstIndex = spec.findColumnIndex(first);
        if (firstIndex < 0) {
            throw new InvalidSettingsException(
                    "No such column in input table: " + first);
        }
        DataType firstType = spec.getColumnSpec(firstIndex).getType();
        if (!firstType.isCompatible(RDKitMolValue.class)) {
            throw new InvalidSettingsException("Column '" + first
                    + "' does not contain SMILES");
        }
        return new int[]{firstIndex};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec inSpec = inData[0].getDataTableSpec();
        DataTableSpec[] outSpecs=createOutSpecs(new DataTableSpec[]{inSpec});

        BufferedDataContainer fragTable =
                exec.createDataContainer(outSpecs[0]);
        BufferedDataContainer molTable =
            exec.createDataContainer(outSpecs[1]);

        // check user settings against input spec here
        final int[] indices = findColumnIndices(inSpec);

        // used to build the output fragment table
        Vector<Int_Vect> fragsSeen = new Vector<Int_Vect>();
        fragsSeen.clear();
        Vector<ROMol> frags = new Vector<ROMol>();
        frags.clear();
        Vector<String> smis = new Vector<String>();
        smis.clear();
        Vector<Integer> fragCounts = new Vector<Integer>();
        fragCounts.clear();
        //Vector< List<Integer> > fragSets=new Vector< List<Integer> >();

        final int rowCount = inData[0].getRowCount();
        try {
            int count=0;
            RowIterator it = inData[0].iterator();
            while (it.hasNext()) {
                DataRow row = it.next();
                DataCell firstCell = row.getCell(indices[0]);
                //List<Integer> fragsHere = new ArrayList<Integer>();
                List<IntCell> fragsHere = new ArrayList<IntCell>();
                fragsHere.clear();
                if (!firstCell.isMissing()) {
                    ROMol mol = null;
                    mol = ((RDKitMolValue)firstCell).readMoleculeValue();
                    // got our molecule, now decompose it:
                    Int_Int_Vect_List_Map pathMap=RDKFuncs.findAllSubgraphsOfLengthsMtoN(mol,
                    		m_minPath.getIntValue(),
                            m_maxPath.getIntValue());
                    for(int l=m_minPath.getIntValue();l<=m_maxPath.getIntValue();l++){
                    	Int_Vect_List paths=pathMap.get(l);
                    	Vector<Int_Vect> lCache = new Vector<Int_Vect>();
                    	lCache.clear();
	                    for(int i=0;i<paths.size();++i){
	                    	Int_Vect ats=paths.get(i);
                    		int idx;
                    		// if we've seen this fragment in this molecule already,
                    		// go ahead and punt on it;
                    		Int_Vect discrims=RDKFuncs.calcPathDiscriminators(mol, ats);
                    		idx=-1;
                    		// .indexOf() doesn't use the correct .equals() method
                    		// with my SWIG classes, so we have to hack this:
                    		for(int iidx=0;iidx<lCache.size();iidx++){
                    			if(lCache.get(iidx).equals(discrims)){
                    				idx=iidx;
                    				break;
                    			}
                    		}
                    		if(idx>=0){
                    			continue;
                    		} else {
                    			lCache.add(discrims);
                    		}
                    		idx=-1;
                    		for(int iidx=0;iidx<fragsSeen.size();iidx++){
                    			if(fragsSeen.get(iidx).equals(discrims)){
                    				idx=iidx;
                    				break;
                    			}
                    		}
                    		if(idx>=0){
                    			fragCounts.set(idx,(fragCounts.get(idx))+1);
                    		} else {
                    			idx=fragsSeen.size();
                    			fragsSeen.add(discrims);
                    			ROMol frag=RDKFuncs.pathToSubmol(mol, ats);
                    			frags.add(frag);
                    			String smi=RDKFuncs.MolToSmiles(frag);
                    			smis.add(smi);
                    			fragCounts.add(1);
                    		}
                    		//fragsHere.add(idx);
                    		fragsHere.add(new IntCell(idx+1));
	                    }
                    }
                }
                //fragSets.add(fragsHere);

                DataCell[] cells = new DataCell[molTable.getTableSpec().getNumColumns()];
                for(int i=0;i<cells.length-1;i++){
                	cells[i]=row.getCell(i);
                }
                cells[cells.length-1] = CollectionCellFactory.createListCell(fragsHere);

                DataRow drow= new DefaultRow(row.getKey(),cells);
                molTable.addRowToTable(drow);

                count++;
                exec.setProgress(.9*count / rowCount, "Processed row "
                        + count + "/" + rowCount + " (\"" + row.getKey()
                        + "\")");
                exec.checkCanceled();
            }
/*
            count=0;
            it = inData[0].iterator();
            while (it.hasNext()) {
                DataRow row = it.next();

                DataCell[] cells = new DataCell[molTable.getTableSpec().getNumColumns()];
                for(int i=0;i<cells.length-1;i++){
                	cells[i]=row.getCell(i);
                }
            	List<Integer> seenHere=fragSets.get(count);
            	List<IntCell> fragsHere = new ArrayList<IntCell>();
                fragsHere.clear();
            	for(int fidx=0;fidx<fragsSeen.size();++fidx){
            		if(seenHere.indexOf(fidx)>=0){
            			fragsHere.add(new IntCell(1));
            		} else {
            			fragsHere.add(new IntCell(0));
            		}
                }
                cells[cells.length-1] = CollectionCellFactory.createListCell(fragsHere);

                DataRow drow= new DefaultRow(row.getKey(),cells);
                molTable.addRowToTable(drow);
                count++;
                exec.setProgress(.7+.2*count / (double)rowCount, "Added row "
                        + count + "/" + rowCount + " (\"" + row.getKey()
                        + "\")");
                exec.checkCanceled();
            }
*/
            for(int i=0;i<fragsSeen.size();++i){
            	DataCell[] cells =
            		new DataCell[fragTable.getTableSpec().getNumColumns()];
            	cells[0]=new IntCell(i+1);
            	cells[1]=RDKitMolCellFactory.createRDKitMolCell(frags.get(i),smis.get(i));
            	cells[2]=new StringCell(smis.get(i));
            	cells[3]=new IntCell((int)frags.get(i).getNumBonds());
            	cells[4]=new IntCell(fragCounts.get(i));
                frags.get(i).delete();
            	DataRow drow =
            		new DefaultRow("frag_" + i+1,cells);
            	fragTable.addRowToTable(drow);
                exec.setProgress(.9+.1*i / fragsSeen.size(), "Added fragment row "
                        + i + "/" + fragsSeen.size() + " (\"" + drow.getKey()
                        + "\")");
            	exec.checkCanceled();
            }
        } finally {
            fragTable.close();
            molTable.close();
        }

        return new BufferedDataTable[]{fragTable.getTable(),
                molTable.getTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to reset
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // node does not have internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // node does not have internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_first.loadSettingsFrom(settings);
        m_minPath.loadSettingsFrom(settings);
        m_maxPath.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_first.saveSettingsTo(settings);
        m_minPath.saveSettingsTo(settings);
        m_maxPath.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_first.validateSettings(settings);
        m_minPath.validateSettings(settings);
        m_maxPath.validateSettings(settings);
    }
}
