/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2010
 * Novartis Institutes for BioMedical Research
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder.
 * ---------------------------------------------------------------------
 */
package org.rdkit.knime.nodes.onecomponentreaction;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * 
 * @author Greg Landrum
 */
public class RDKitOneComponentReactionNodeFactory 
        extends NodeFactory<RDKitOneComponentReactionNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new RDKitOneComponentReactionNodeDialogPane();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RDKitOneComponentReactionNodeModel createNodeModel() {
        return new RDKitOneComponentReactionNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<RDKitOneComponentReactionNodeModel> createNodeView(
            final int viewIndex, final RDKitOneComponentReactionNodeModel nodeModel) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

}
