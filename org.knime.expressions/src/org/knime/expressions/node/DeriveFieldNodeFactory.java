package org.knime.expressions.node;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class DeriveFieldNodeFactory extends NodeFactory<DeriveFieldNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public DeriveFieldNodeModel createNodeModel() {
        // TODO Auto-generated method stub
        return new DeriveFieldNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<DeriveFieldNodeModel> createNodeView(final int viewIndex, final DeriveFieldNodeModel nodeModel) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        // TODO Auto-generated method stub
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        // TODO Auto-generated method stub
        return null;
    }

}
