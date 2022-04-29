package com.jrc.tmacc.osintbrand;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * This is an example implementation of the node factory of the
 * "OsintBrand" node.
 *
 * @author JRC I.3
 */
public class OsintBrandNodeFactory 
        extends NodeFactory<OsintBrandNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public OsintBrandNodeModel createNodeModel() {
		// Create and return a new node model.
        return new OsintBrandNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
		// The number of views the node should have, in this cases there is none.
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<OsintBrandNodeModel> createNodeView(final int viewIndex,
            final OsintBrandNodeModel nodeModel) {
		// We return null as this example node does not provide a view. Also see "getNrNodeViews()".
		return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
		// Indication whether the node has a dialog or not.
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
		// This example node has a dialog, hence we create and return it here. Also see "hasDialog()".
        return new OsintBrandNodeDialog();
    }

}

