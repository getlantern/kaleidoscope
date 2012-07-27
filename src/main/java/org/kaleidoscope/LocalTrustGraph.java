package org.kaleidoscope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * LocalTrustGraph
 *
 * A set of TrustGraphNodes that communicates locally.
 * Intended for testing and light simulation purposes.
 * 
 */
public class LocalTrustGraph {

    private final ConcurrentHashMap<TrustGraphNodeId, LocalTrustGraphNode> nodes;
    private final AtomicInteger idSeq;
    private final int defaultIdealReach;
    private final int defaultMaxRouteLength;
    private final int defaultMinRouteLength;

    /** 
     * construct a LocalTrustGraph with default parameters.
     *
     */
    public LocalTrustGraph() {
        this(TrustGraphNode.DEFAULT_IDEAL_REACH,
             TrustGraphNode.DEFAULT_MIN_ROUTE_LENGTH,
             TrustGraphNode.DEFAULT_MAX_ROUTE_LENGTH);
    }

    /** 
     * construct a LocalTrustGraph with specific parameters.  The values 
     * given will be used by nodes created in this graph by default.
     *
     * @param defaultIdealReach the default "ideal reach" (r) value for the network.
     *        This value represents the ideal number of nodes reached when a node
     *        advertises itself on the network.
     * @param defaultMinRouteLength the shortest allowable route length for a single
     *        advertisement.  
     * @param defaultMaxRouteLength the longest allowable route length for a single 
     *        advertisement. 
     */
    public LocalTrustGraph(int defaultIdealReach, int defaultMinRouteLength, int defaultMaxRouteLength) {
        this.nodes = new ConcurrentHashMap<TrustGraphNodeId, LocalTrustGraphNode>();
        this.idSeq = new AtomicInteger(0);
        this.defaultIdealReach = defaultIdealReach;
        this.defaultMaxRouteLength = defaultMaxRouteLength;
        this.defaultMinRouteLength = defaultMinRouteLength;
    }


    /** 
     * a TrustGraphNode that routes messages locally and 
     * records messages that pass through it.
     */
    public static class LocalTrustGraphNode extends TrustGraphNode {
        final LocalTrustGraph graph;
        final TrustGraphNodeId id;
        final List<BasicTrustGraphAdvertisement> messages;

        /**
         * construct a new LocalTrustGraphNode
         *
         * @param id unique identifier for this node
         * @param graph the parent LocalTrustGraph to send on
         */
        public LocalTrustGraphNode(final TrustGraphNodeId id,
                                   final LocalTrustGraph graph) {
            this.graph = graph;
            this.id = id;
            this.messages = new LinkedList<BasicTrustGraphAdvertisement>();
        }

        /**
         * uses graph default value
         */
        @Override
        public int getIdealReach() {  return graph.getIdealReach(); }

        /** 
         * uses graph default value 
         */
        @Override
        public int getMaxRouteLength() { return graph.getMaxRouteLength();  }

        /**
         * uses graph default value
         */
        @Override
        public int getMinRouteLength() { return graph.getMinRouteLength(); }
        

        /**
         * implements sendAdvertisement in TrustGraphNode by sending to the
         * local graph.
         *
         * @param message message to send
         * @param neighbor intended next hop for message
         * @param ttl number of hops the message should live
         */
        @Override
        public void sendAdvertisement(final TrustGraphAdvertisement message,
                                      final TrustGraphNodeId neighbor,
                                      final int ttl) {
            final BasicTrustGraphAdvertisement localMessage =
                (BasicTrustGraphAdvertisement) message;
            graph.sendAdvertisement(localMessage, this.id,
                                    neighbor, ttl);
        }

        @Override
        public void handleAdvertisement(TrustGraphAdvertisement message) {
            super.handleAdvertisement(message);
            this.messages.add((BasicTrustGraphAdvertisement)message);
        }

        public int getMessageCount() {return messages.size();}

        public List<TrustGraphAdvertisement> getMessages() {
            return new ArrayList<TrustGraphAdvertisement>(messages);
        }

        public void clear() {
            this.messages.clear();
        }

        public TrustGraphNodeId getId() {return id;}
        
        public LocalTrustGraph getTrustGraph() {return graph;}
    }

    /** 
     * send a local advertisement to the node specified with 
     * the given ttl.
     * 
     * @param message advertisement to send
     * @param sender the node sending the message
     * @param toNeighbor the recipient of the message
     */
    public void sendAdvertisement(final BasicTrustGraphAdvertisement message,
                                  final TrustGraphNodeId sender,
                                  final TrustGraphNodeId toNeighbor,
                                  final int ttl) {
        final BasicTrustGraphAdvertisement outboundMessage = message.copyWith(sender, ttl);
        final TrustGraphNode toNode = nodes.get(toNeighbor);
        toNode.handleAdvertisement(outboundMessage);
    }

    /** 
     * Create and add a new trust graph node.  The 
     * node's id will be the next id in the id sequence
     * for this graph. 
     * 
     * @return the new node
     */
    public LocalTrustGraphNode addNode() {
        return addNode(nextId());
    }

    /**
     * Create and add a new trust graph node with the given id. 
     * 
     * @param nodeId the identifier for the new node
     * @returns the new node
     */
    public LocalTrustGraphNode addNode(final TrustGraphNodeId nodeId) {
        final LocalTrustGraphNode node = createNode(nodeId);
        addNode(node);
        return node;
    }

    /**
     * factory method, can be used to override the node type 
     * used in the local graph.
     */
    protected LocalTrustGraphNode createNode(final TrustGraphNodeId nodeId) {
        return new LocalTrustGraphNode(nodeId, this);
    }

    /**
     * Add a node to the trust graph.
     *  
     * @param nodeId an identifier for the node, used in routing tables 
     * @param node the node to add
     */
    public void addNode(final LocalTrustGraphNode node) {
        nodes.put(node.getId(), node);
    }

    /**
     * lookup a LocalTrustGraphNode by its id.
     */
    public LocalTrustGraphNode getNode(TrustGraphNodeId id) {
        return nodes.get(id);
    }
    
    public Collection<LocalTrustGraphNode> getAllNodes() {
        return nodes.values();
    }

    /** 
     * clear all state, routes and nodes from the graph.
     */
    public void clear() {
        clearNodeInfo();
        clearRoutes();
        nodes.clear();
    }

    /**
     * Add a directed trust graph link between two nodes.
     *
     * Note: Although this is useful for testing adverse conditions, 
     * relationships must be symmetric for the normal functioning of
     * the algorithm.  An advertising node must trust another node to 
     * send to it, and that same node must trust the sender in order 
     * to forward the message (find the next hop for the message).
     * 
     * @param from the node that is trusting
     * @param to the node that is being trused by the node 'from'
     */
    public void addDirectedRoute(final LocalTrustGraphNode from, final LocalTrustGraphNode to) {
        from.getRoutingTable().addNeighbor(to.getId());
    }


    /**
     * create a directed trust link between two nodes. The node with id 'from' will 
     * trust the node with id 'to'.
     *
     * Note: Although this is useful for testing adverse conditions, 
     * relationships must be symmetric for the normal functioning of
     * the algorithm.  An advertising node must trust another node to 
     * send to it, and that same node must trust the sender in order 
     * to forward the message (find the next hop for the message).
     *
     * @param from the id of the node that is trusting
     * @param to the id of node that is being trused by the node 'from'
     */
    public void addDirectedRoute(final TrustGraphNodeId from, final TrustGraphNodeId to) {
        final TrustGraphNode fromNode = nodes.get(from);
        fromNode.getRoutingTable().addNeighbor(to);
    }

    /**
     * create a bidirectional trust link between the nodes with ids 'a' and 'b'
     *
     * @param a id of node that will form a trust link with 'b' 
     * @param b id of node that will form a trust link with 'a'
     */
    public void addRoute(final TrustGraphNodeId a, final TrustGraphNodeId b) {
        addDirectedRoute(a,b);
        addDirectedRoute(b,a);
    }

    /**
     * add bidirectioanl trust graph links between two nodes.
     *
     * @param a node that will form a trust link with 'b' 
     * @param b node that will form a trust link with 'a'
     */
    public void addRoute(final LocalTrustGraphNode a, final LocalTrustGraphNode b) {
        addDirectedRoute(a,b);
        addDirectedRoute(b,a);
    }


    /**
     * clear all messages and other state info currently retained 
     * by nodes in the graph.
     */
    public void clearNodeInfo() {
        for (LocalTrustGraphNode n: nodes.values()) { n.clear(); }
    }

    public void clearRoutes() {
        for (LocalTrustGraphNode n: nodes.values()) { n.getRoutingTable().clear(); }
    }

    /**
     * generates a sequence of TrustGraphNodeId ids which are unique with
     * respect to each other.
     * 
     * @return the next id in the sequence
     */
    public TrustGraphNodeId nextId() {
        final int idNum = idSeq.getAndAdd(1);
        return new BasicTrustGraphNodeId("#" + idNum);
    }

    /** 
     * graph wide default "ideal reach"
     *
     * This parameter is referred to as "r" in TR2008-918
     */
    public int getIdealReach() { return defaultIdealReach;  }

    /**
     * graph wide default max route length
     *  
     * This parameter is referred to as "w_max" in TR2008-918
     */
    public int getMaxRouteLength() { return defaultMaxRouteLength;  }

    /**
     * graph wide default min route length
     *  
     * This parameter is referred to as "w_min" in TR2008-918
     */
    public int getMinRouteLength() { return defaultMinRouteLength; }
    
    /**
     * Create a random graph following the procedure of 
     * Toivonen et al, "A model for social networks"
     * using the default source of randomness
     * 
     * @see http://arxiv.org/abs/physics/0601114
     * 
     * @param networkSize number of nodes to add
     */
    public void growToivonenSocialNetwork(int networkSize) {
        growToivonenSocialNetwork(networkSize, new Random());
    }
    
    /** 
     * Create a random graph following the procedure of 
     * Toivonen et al, "A model for social networks"
     * @see http://arxiv.org/abs/physics/0601114
     * 
     * The graph should contain some "seed" network 
     * of nodes and edges to grow from.
     * 
     * Currently this just uses the fixed parameters:
     * p(nInit = 1) = 0.95
     * p(nInit = 2) = 0.05
     * n2nd ~ U[0,3] 
     * 
     * @param networkSize the number of new nodes to add
     * @param rand random source
     */
    public void growToivonenSocialNetwork(int networkSize, Random rand) {
        
        // load in the seed network.
        final ArrayList<LocalTrustGraphNode> networkNodes = 
            new ArrayList<LocalTrustGraphNode>(nodes.values());
        
        if (networkNodes.size() < 2) {
            throw new IllegalStateException("Not enough seed nodes.");
        }
        
        /* create the remaining nodes and links incrementally according
         * to the parameters.
         */
        for (int j = 0; j < networkSize; j++) {
            
            // pick the number of initial neighbors
            final int nInit = rand.nextFloat() >= 0.05 ? 1 : 2;
            
            // pick nInit initial neighbor nodes
            final Set<LocalTrustGraphNode> initialNeighbors =
                new HashSet<LocalTrustGraphNode>();
            
            /* pick nInit initial neighbors uniformly at random
             * without repetition.
             */
            while (initialNeighbors.size() < nInit) {
                initialNeighbors.add(networkNodes.get(
                    rand.nextInt(networkNodes.size())));
            }
            
            /* for each initial neighbor, pick a random number of
             * secondary neighbors ~ U[0,3]
             * 
             * Note this does not attempt to account for global 
             * repetition among all selected secondary neighbors,
             * only among a given node's neighbors.
             */
             final Set<TrustGraphNodeId> secondaryNeighbors =
                 new HashSet<TrustGraphNodeId>();
             for (LocalTrustGraphNode neighbor : initialNeighbors) {
                 /* pick the number of secondary neighbors from this
                  * neighbors set of current neighbors.
                  */
                 final int n2nd = rand.nextInt(4); // U[0,3]
                 
                 // grab the list of 2nd degree neighbors

                 final ArrayList<TrustGraphNodeId> all2nd =
                     new ArrayList<TrustGraphNodeId>(
                        neighbor.getRoutingTable().getOrderedNeighbors());
                 
                 /* If there are more than enough neighbors, 
                  * select random neighbors by shuffling and picking 
                  * the first n2nd elements.
                  */
                 if (all2nd.size() > n2nd) {
                     Collections.shuffle(all2nd,rand);
                     for (int i = 0; i < n2nd; i++) {
                         secondaryNeighbors.add(all2nd.get(i));
                     }
                 }
                 /* If there <= n2nd neighbors, take them all (saturation)
                  */
                 else {
                     for (TrustGraphNodeId n : all2nd) { secondaryNeighbors.add(n); }
                 }
             }
 
             // finally, add the new node, and form all the connections...
             final LocalTrustGraphNode newNode = addNode();
             networkNodes.add(newNode);
             final TrustGraphNodeId newId = newNode.getId();
             for (LocalTrustGraphNode n : initialNeighbors) { addRoute(newId, n.getId()); }
             for (TrustGraphNodeId n : secondaryNeighbors) { addRoute(newId, n); }
        }
     }
}