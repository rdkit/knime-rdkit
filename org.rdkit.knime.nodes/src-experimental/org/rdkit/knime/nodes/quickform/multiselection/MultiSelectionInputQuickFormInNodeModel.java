/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2012-2023
 * Novartis Pharma AG, Switzerland
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
package org.rdkit.knime.nodes.quickform.multiselection;

import java.util.Arrays;
import java.util.LinkedHashSet;

import org.knime.base.node.io.filereader.DataCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.util.node.quickform.in.AbstractQuickFormInElement;
import org.knime.quickform.nodes.in.selection.QuickFormDataInNodeModel;

/**
 * Node for multi-selection option input quickform.
 *
 * @author Manuel Schwarze, based on work of Thomas Gabriel, KNIME.com AG, Zurich
 */
public class MultiSelectionInputQuickFormInNodeModel extends
        QuickFormDataInNodeModel<MultiSelectionInputQuickFormInConfiguration> {

	//
	// Constructor
	//
	
    /** Create a new value filter quickform node model. */
    protected MultiSelectionInputQuickFormInNodeModel() {
        super(0, 1);
    }

    //
    // Protected Methods
    //
    
    /** {@inheritDoc} */
    @Override
    protected void createAndPushFlowVariable() throws InvalidSettingsException {
        MultiSelectionInputQuickFormInConfiguration cfg =
            (MultiSelectionInputQuickFormInConfiguration) getConfiguration();
        if (cfg == null) {
            throw new InvalidSettingsException("No settings available");
        }
        String variableName = cfg.getVariableName();
        MultiSelectionInputQuickFormValueInConfiguration valCfg =
                cfg.getValueConfiguration();
        String[] values = valCfg.getValues();
        String value = Arrays.toString(values);
        value = value.substring(1, value.length() - 1);
        pushFlowVariableString(variableName, value);
    }

    /** {@inheritDoc} */
    @Override
    protected MultiSelectionInputQuickFormInConfiguration
            createConfiguration() {
        return new MultiSelectionInputQuickFormInConfiguration();
    }

    /** {@inheritDoc} */
    @Override
    public AbstractQuickFormInElement getQuickFormElement() {
        MultiSelectionInputQuickFormInConfiguration cfg =
            (MultiSelectionInputQuickFormInConfiguration) getConfiguration();
        MultiSelectionInputQuickFormInElement e =
                new MultiSelectionInputQuickFormInElement(cfg.getLabel(),
                        cfg.getDescription(), cfg.getWeight());
        String[] arrChoices = cfg.getChoices();
        if (arrChoices != null) {
            e.setChoiceValues(new LinkedHashSet<String>(Arrays.asList(cfg.getChoices())));
        }
        e.setSmallGui(cfg.isSmallGui());
        MultiSelectionInputQuickFormValueInConfiguration valCfg =
                cfg.getValueConfiguration();
        e.setValues(valCfg.getValues());
        return e;
    }

    /** {@inheritDoc} */
    @Override
    public void loadFromQuickFormElement(final AbstractQuickFormInElement e)
            throws InvalidSettingsException {
        MultiSelectionInputQuickFormInConfiguration cfg =
            (MultiSelectionInputQuickFormInConfiguration) getConfiguration();
        MultiSelectionInputQuickFormInElement vf =
                AbstractQuickFormInElement.cast(
                		MultiSelectionInputQuickFormInElement.class, e);
        MultiSelectionInputQuickFormValueInConfiguration valCfg =
                cfg.getValueConfiguration();
        valCfg.setValues(vf.getValues());
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        super.configure(inSpecs);
        DataTableSpec spec = createSpec();
        return new DataTableSpec[]{spec};
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects,
            final ExecutionContext exec) throws Exception {
        createAndPushFlowVariable();
        final DataTableSpec outSpec = createSpec();
        BufferedDataContainer cont = exec.createDataContainer(outSpec, false);
        MultiSelectionInputQuickFormInConfiguration cfg =
            (MultiSelectionInputQuickFormInConfiguration) getConfiguration();
        String[] values;
        if (cfg != null) {
            values = cfg.getValueConfiguration().getValues();
        } else {
            values = new String[0];
        }

        DataCellFactory cellFactory = new DataCellFactory();
        DataType type = outSpec.getColumnSpec(0).getType();
        for (int i = 0; i < values.length; i++) {
            DataCell result = cellFactory.createDataCellOfType(
                    type, values[i]);
            cont.addRowToTable(new DefaultRow(RowKey.createRowKey(i), result));
        }
        cont.close();
        return new PortObject[]{cont.getTable()};
    }

    //
    // Private Methods
    //
    
    /**
     * Creates the output table spec, which contains always
     * one string column. The name of this columns is based
     * on the current settings.
     * 
     * @throws InvalidSettingsException Thrown, if the column name
     * 		contained in the settings is null.
     */
    private DataTableSpec createSpec()
            throws InvalidSettingsException {
        MultiSelectionInputQuickFormInConfiguration cfg =
            (MultiSelectionInputQuickFormInConfiguration) getConfiguration();
        String strColumnName = cfg.getColumnName();
        if (strColumnName != null) {
        	DataColumnSpecCreator creator = 
        		new DataColumnSpecCreator(strColumnName, StringCell.TYPE);
            return new DataTableSpec(creator.createSpec());
        } else {
            throw new InvalidSettingsException(
            		"Invalid column name specified for user selections.");
        }
    }
}
