package org.knime.examples.odatatest;

import org.knime.core.node.NodeView;


/**
 * This is an example implementation of the node view of the
 * "ODataTest" node.
 * 
 * As this example node does not have a view, this is just an empty stub of the 
 * NodeView class which not providing a real view pane.
 *
 * @author Mykola Zotko
 */
public class ODataTestNodeView extends NodeView<ODataTestNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link ODataTestNodeModel})
     */
    protected ODataTestNodeView(final ODataTestNodeModel nodeModel) {
        super(nodeModel);

        // TODO instantiate the components of the view here.

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {

        // TODO retrieve the new model from your nodemodel and 
        // update the view.
        ODataTestNodeModel nodeModel = 
            (ODataTestNodeModel)getNodeModel();
        assert nodeModel != null;
        
        // be aware of a possibly not executed nodeModel! The data you retrieve
        // from your nodemodel could be null, emtpy, or invalid in any kind.
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
    
        // TODO things to do when closing the view
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {

        // TODO things to do when opening the view
    }

}

