/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   Feb 17, 2007 (wiswedel): created
 */
package org.knime.base.node.preproc.correlation.pmcc;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class PMCCNodeFactory extends NodeFactory<PMCCNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new PMCCNodeDialogPane();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PMCCNodeModel createNodeModel() {
        return new PMCCNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<PMCCNodeModel> createNodeView(
            final int viewIndex, final PMCCNodeModel nodeModel) {
        return new PMCCNodeView(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

}
