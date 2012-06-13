package org.kaleidoscope;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * The RandomRoutingTable interface represents the randomized  per-node routing
 * table used to construct the "repeatable random walks" (random routes) used 
 * in the Kaleidoscope protocol to send messages to a targeted number of peers
 * "over" links in the underlying trust network.
 * 
 * The next hop for a message travelling over the trust graph is determined by
 * the entry in the routing table for the neighbor that the message was
 * received from.  Note this neighbor is not necessarily the originator of the
 * message, just the prior hop.
 *
 * A randomly ordered list of neighbors is also maintained for repeatably 
 * choosing a subset of neighbors to advertise to.
 * 
 * Particular implementations are expected provide an appropriate persistence
 * mechanism to ensure the repeatability of routes across runs of the software.
 * In general, the protocol relies on a high degree of stability in the routes
 * in order to function correctly (particularly with respect to "sybil"
 * resistence)
 *
 */
public interface RandomRoutingTable {

    /**
     * Determine the next hop for a message.
     * 
     * Messages are routed based on the neighbor from whom the 
     * message arrives (but not necessiarly originated)
     * 
     * @param message the message to determine the next hop for
     * @return the next TrustGraphNodeId to send the message to
     *         or null if the next hop cannot be determined.
     *         
     */
    public TrustGraphNodeId getNextHop(final TrustGraphAdvertisement message);

    /**
     * Determine the next TrustGraphNodeId in a route containing
     * a given neighbor as the prior node.  The next hop 
     * is the TrustGraphNodeId paired with the given neighbor in
     * the table.
     * 
     * @param priorNeighbor the prior node on the route
     * @return the next TrustGraphNodeId to route a message to
     *         or null if the next hop cannot be determined.
     *
     */
    public TrustGraphNodeId getNextHop(final TrustGraphNodeId priorNeighbor);


    /**
     * Add a single TrustGraphNodeId to the routing table.
     * Existing neighbors will not be re-added.
     * 
     * @param neighbor the neighbor to add 
     *
     */
    public void addNeighbor(final TrustGraphNodeId neighbor);
    
    /**
     * Add a group of TrustGraphNieghbors to the routing table.  
     * Existing neighbors will not be readded. 
     *
     * @param neighbors the collection of TrustGraphNeighbors to add
     */
    public void addNeighbors(final Collection<TrustGraphNodeId> neighbors);

    /**
     * Remove a TrustGraphNodeId from the routing table.
     *
     * @param neighbor the neighbor to remove
     *
     */
    public void removeNeighbor(final TrustGraphNodeId neighbor);
    

    /**
     * Remove a set of TrustGraphNeighbors from the routing table.
     * 
     * @param neighbors the collection neighbors to remove
     */
    public void removeNeighbors(final Collection<TrustGraphNodeId> neighbors);

    /**
     * Remove all entries from the routing table.
     */
    public void clear();

    /**
     * @return true if and only if the neighbor specified is in the routing
     *         table.
     */
    public boolean contains(TrustGraphNodeId neighbor);

    /**
     * @return the number of neighbors/routes in the routing table.
     *
     */
    public int size(); 

    /**
     * @return true if and only if there are no routes in the table
     */ 
    public boolean isEmpty();


    /**
     * Retrieve a list of TrustGraphNeighbors in the routing table in 
     * a random order. This ordering is constructed randomly, but 
     * should rarely change thereafter.  It is intended to be used
     * for the selection of a repeatable random subset of neighboring 
     * nodes for self advertisement.  It is expected to be maintatined
     * across restarts and is also represented in the snapshot method.
     *
     * @return a snapshot of the set of TrustGraphNeighbors in the routing
     *         table.
     */
    public List<TrustGraphNodeId> getOrderedNeighbors();

    /**
     * The Snapshot interface represents a snapshot of
     * the state of a RandomRoutingTable suitable for
     * persistence etc.
     */
    public interface Snapshot {
        public Map<TrustGraphNodeId, TrustGraphNodeId> getRoutes();
        public List<TrustGraphNodeId> getOrderedNeighbors();
    }

    /**
     * Creates a snapshot of the current state of the routing table.
     * 
     * A mapping X->Y between two TrustGraphNeighbors represents that 
     * the next hop of a message received from the neigbor X is Y.
     * Properties of this mapping may vary between implementations.
     * 
     * @return a snapshot of the current state of the routing table 
     *         represented as a Map between TrustGraphNeighbors. 
     */
    public Snapshot snapshot();


}