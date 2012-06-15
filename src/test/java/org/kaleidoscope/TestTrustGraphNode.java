package org.kaleidoscope; 

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

public class TestTrustGraphNode {

    public TestTrustGraphNode() {}

    /** 
     * Tests that delivery of an advertisement on some simple trust
     * graphs reach the expected number of nodes using the
     * LocalTrustGraph implementation of TrustGraphNode.
     */
    @Test
    public void testReach() {
        

        /* Each pass creates a directed acyclic graph that is composed of 
         * a series of "layers" containing disjoint nodes. The set of 
         * neighbors for a node is exactly the complete set of nodes in the 
         * next layer.  That is, layer i and i+1 form a complete bipartite
         * graph. 
         * 
         * Enough layers to exceed the maximum route length are created. 
         * The first layer contains a single root or source node and 
         * successive layers are of some other fixed size.
         *
         * The network can form loops and retrace steps because trust 
         * graph relationships are symmetric.  The count of nodes reached
         * by a message (included repeats) is measured against the 
         * expected reach.
         * 
         */

        // the depth of the network is cosen to exceed the maximum route length
        final int depth = 1 + TrustGraphNode.DEFAULT_MAX_ROUTE_LENGTH;

        /* the maximum width is chosen to exceed number of neighbors that are
         * usable according to the parameters of the algorithm.
         */
        final int maxWidth = 1 + (TrustGraphNode.DEFAULT_IDEAL_REACH / 
                                  TrustGraphNode.DEFAULT_MIN_ROUTE_LENGTH);

        /* 
         * test with varying "widths" corresponding to the number of
         * nodes in each layer.
         * This is also half the numer of neighbors each node in an interior
         * layer has in total. (width in the prior layer, width in the next
         * layer).
         */
        for (int w = 1; w <= maxWidth; w++) {
            final List<LinkedList<LocalTrustGraph.LocalTrustGraphNode> > layers = 
                new ArrayList<LinkedList<LocalTrustGraph.LocalTrustGraphNode>>(depth);

            final LocalTrustGraph g = new LocalTrustGraph();

            LinkedList<LocalTrustGraph.LocalTrustGraphNode> lastLayer =
                new LinkedList<LocalTrustGraph.LocalTrustGraphNode>();

            // create a grid of w x depth nodes
            for (int l = 0; l < depth; l++) {
                LinkedList<LocalTrustGraph.LocalTrustGraphNode> curLayer =
                    new LinkedList<LocalTrustGraph.LocalTrustGraphNode>();
                layers.add(curLayer);
                
                // create nodes in the current layer
                for (int i = 0; i < w; i++) {
                    curLayer.add(g.addNode());
                }
                /* link each node in the prior layer to every node in the new 
                 * layer.
                 */
                for (LocalTrustGraph.LocalTrustGraphNode a : lastLayer) {
                    for (LocalTrustGraph.LocalTrustGraphNode b : curLayer) {
                        g.addRoute(a, b);
                    }
                }
                lastLayer = curLayer;
            }
            
            // find the first node in the first layer.
            final LocalTrustGraph.LocalTrustGraphNode root =
                layers.get(0).get(0);
            
            // send an advertisement through the graph from the root
            final TrustGraphAdvertisement ad =
                new BasicTrustGraphAdvertisement(root.getId(),
                                                 "Root", 0);
            root.advertiseSelf(ad);

            /* count the number of nodes message has been delivered to.
             * This may include repeated visits to the same node due 
             * to the symmetric nature of the graph.
             */
            int totalCount = 0;
            for (List<LocalTrustGraph.LocalTrustGraphNode> layer : layers) {
                for (LocalTrustGraph.LocalTrustGraphNode node: layer) {
                    totalCount += node.getMessageCount();
                }
            }
            
            /* the message should reach either the ideal reach if the
             * number of neighbors per node is high enough or
             * maxRouteLength * root neighbors nodes.
             */
            int expectedCount = Math.min(w*root.getMaxRouteLength(),
                                         root.getIdealReach());
            assertTrue(totalCount == expectedCount);
        }

    }
    
    /** 
     * tests that advertisement routes are repated for 
     * repeat messages.
     */
    @Test
    public void testRouteRepetition() {
    }
    
    /** 
     * tests that advertisements with ttls above the
     * maximum are not allowed.
     */
    @Test 
    public void testMaximumRouteLengthClipping() {
    }
    
}