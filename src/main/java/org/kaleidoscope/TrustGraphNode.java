package org.kaleidoscope; 

import java.util.Iterator;
import java.util.List;

/** 
 * TrustGraphNode is the abstract base class for implementations of a node
 * participating in the Kaleidoscope limited advertisement protocol.  
 * It embodies the network-neutral behaviors that the protocol specifies.
 *
 * Particular implementations are expected to provide particular 
 * network and persistence implementations.
 * 
 * The TrustGraphNode interface also specifies some default values for
 * determining protocol parameters that implementations may wish to
 * override to suit a particular circumstance.
 *
 */
public abstract class TrustGraphNode {

    /*
     * These are the default values for the basic parameters of a 
     * kaleidoscope node's behavior.
     */ 
    public static final int DEFAULT_IDEAL_REACH      = 100; // aka "r"
    public static final int DEFAULT_MAX_ROUTE_LENGTH =  20; // aka "w_max"
    public static final int DEFAULT_MIN_ROUTE_LENGTH =   7; // aka "w_min"

    private final RandomRoutingTable routingTable;

    /** 
     *
     * Constructs a TrustGraphNode with an empty BasicRandomRoutingTable.
     *
     */
    public TrustGraphNode() {
        this(new BasicRandomRoutingTable());
    }


    /** 
     * Constructs a TrustGraphNode with the given RandomRoutingTable.
     * @param routingTable the RandomRoutingTable used to determine the 
     *        next hop of an inbound message.
     */
    public TrustGraphNode(RandomRoutingTable routingTable) {
        this.routingTable = routingTable;
    }

    /**
     *
     * The sendAdvertisement method represents the specific mechanism 
     * used to communicate a message to a neighbor on the trust graph.
     * This method is called to forward advertisements and to advertise
     * this node.
     *
     * The implementation should deliver the payload of the message 
     * specified to the neighbor specified and mark the message as 
     * having the ttl given.
     * 
     * @param mesage the message to deliver
     * @param neighbor the id of the neighbor to send the message to 
     * @param ttl the new ttl for the message
     *
     */
    public abstract void sendAdvertisement(TrustGraphAdvertisement message, 
                                           TrustGraphNodeId neighbor,
                                           int ttl);


    public RandomRoutingTable getRoutingTable() { return routingTable; }

    /**
     *
     * Called to perform processing of a received TrustGraphAdvertisement.
     * The default behavior is to forward the message to the next hop 
     * according to the routing table and protocol.
     * 
     */
    public void handleAdvertisement(TrustGraphAdvertisement message) {
        forwardAdvertisement(message);
    }

    /** 
     * represents the ideal fixed number of distinct recipients that an
     * advertisement reaches.  This value defaults to 100 but may be 
     * overridden to suit particular needs.
     * 
     * This parameter is referred to as "r" in TR2008-918
     */
    public int getIdealReach() { return DEFAULT_IDEAL_REACH;  }

    /** 
     * Represents the maximum individual route length (ttl)
     * that a node will choose and tolerate for an advertisement.
     * If the ttl of a message is greater than this value, it is dropped
     * by default.  This value is related to the number of 
     * nodes that can be reached by an adverserial node over 
     * one edge and the number of nodes reached by low degree nodes.
     * 
     * The maximum and minimum should be at least 1 apart.
     *
     * This parameter defaults to 20.
     * 
     * This parameter is referred to as "w_max" in TR2008-918
     */
    public int getMaxRouteLength() { return DEFAULT_MAX_ROUTE_LENGTH;  }

    /** 
     *
     * Represents the minimum route length that a node will 
     * choose.  This parameter is related to how local of a 
     * neighborhood a message is intended to reach. eg many 
     * short routes vs fewer longer routes leads to differently
     * "spread-out" delivery neighborhoods.
     *
     * The maximum and minimum should be at least 1 apart.
     *
     * This parameter defaults to 7
     *
     * This parameter is referred to as "w_min" in TR2008-918
     */
    public int getMinRouteLength() { return DEFAULT_MIN_ROUTE_LENGTH; }


    /**
     * called to perform limited advertisement of this node's  
     * information (represented by message).  The advertisement
     * will try to target the number of nodes specified in 
     * getIdealReach() by sending the message down some number 
     * of random routes with.  The inboundTTL and sender of the 
     * message are ignored. 
     *
     * @param message the TrustGraphAdvertisement to send 
     *        through the trust network.
     */
    public void advertiseSelf(TrustGraphAdvertisement message) {


        /* 
         * Choose the route length and number of neighbors to 
         * advertise to based on the number of neighbors this 
         * node has.  The product of these two values determines 
         * the intended reach.  The route length is bounded by 
         * the minimum and maximum route length paramters and the 
         * number of neighbors is bounded by the degree.  
         * 
         * If there are more neighbors available than are needed, 
         * the random ordering kept by the routing table is used 
         * to select the subset to advertise to.  
         *
         * If there are too many neighbors to use (ie more than 
         * reach/min route len) The largest subset of neighbors 
         * is chosen and each is sent a route approximately 
         * reach/min in length.
         * 
         * TR2008-918 does not explicitly outline how to allocate
         * inexact divisions of route length or what to do in the 
         * case that neither boundary condition is violated.
         *
         * This implementation assumes that all neighbors should 
         * be advertised to and that the strategy for allocating 
         * route lengths to sum to the intended reach should 
         * be repeatable in keeping with the intent of the 
         * limited advertisement. 
         * 
         */
        
        final int idealReach = getIdealReach(); 
        final int minRouteLength = getMinRouteLength();
        final int maxRouteLength = getMaxRouteLength();

        final List<TrustGraphNodeId> neighbors =
            getRoutingTable().getOrderedNeighbors();
        final int neighborCount = neighbors.size();

        /* if there are not enough neighbors to reach the 
         * ideal number with maximum route length per neighbor, 
         * just use them all.
         */ 
        if ( neighborCount * maxRouteLength < idealReach ) {
            for (TrustGraphNodeId n : neighbors) {
                sendAdvertisement(message, n, maxRouteLength);
            }
        }
        /* Otherwise, use as many neigbors as possible. 
         * The number of usable neighbors is bounded above 
         * by idealReach/minRouteLength.
         */
        else {
            // use as many neighbors as possible
            final int routes;
            // too many neighbors, if all were used at min route length
            if (neighborCount * minRouteLength > idealReach ) {
                routes = idealReach / minRouteLength;
            }
            // can use all
            else {
                routes = neighborCount;
            }

            /* distribute route lengths between min and max that sum to
             * idealReach.
             * 
             * floor(idealReach/routes is the base length of each route, and 
             * any remainder is spread out as one extra hop on each of the 
             * first 'remainder' routes.
             */ 
            final int stdLen = idealReach / routes;
            final int remainder = idealReach % routes;

            /* send the actual adverstisements.  Use the first 'routes' neighbors
             * in the random ordering assigned by the routing table. 
             */
            Iterator<TrustGraphNodeId> it = neighbors.iterator();
            for (int i = 0; i < routes; i++) {
                int routeLength = stdLen;
                if (i < remainder) {
                    routeLength += 1;
                }
                sendAdvertisement(message, it.next(), routeLength);
            }
        }
    }

    /** 
     * Determines the policy for forwarding received messages. 
     * By default this policy drops messages with ttl higher than 
     * getMaxRouteLength() or there are no more hops (ttl <= 1)
     * 
     * @return true if the message should be forwarded
     *
     */
    protected boolean shouldForward(TrustGraphAdvertisement message) {
        final int ttl = message.getInboundTTL();

        if (ttl <= 1 || ttl > getMaxRouteLength()) {
            return false;
        }
        return true;
    }

    /**
     * This method performs the forwarding behavior for a received 
     * message.  The message is forwarded to the next hop on the
     * route according to the routing table, with ttl decreased by 1.
     * 
     * The message is dropped if it has an invalid hop count or
     * the sender of the message is not a known peer (ie is not 
     * paired to another outbound neighbor)
     *
     * @return true if and only if the message was forwarded
     */
    protected boolean forwardAdvertisement(TrustGraphAdvertisement message) {
        
        // don't forward if the forwarding policy rejects
        if (!shouldForward(message)) {
            return false;
        }

        // determine the next hop to send to
        TrustGraphNodeId nextHop = getRoutingTable().getNextHop(message);
        
        // if there is no next hop, reject
        if (nextHop == null) {
            return false;
        }

        // forward the message to the next node on the route 
        // with ttl decreased by 1
        sendAdvertisement(message, nextHop, message.getInboundTTL() - 1);
        return true;
    }



}