package org.kaleidoscope;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 
 * The BasicRandomRoutingTable is a basic thread safe implementation of the
 * RandomRoutingTable interface that follows the behaviors for maintaining 
 * random routes outlined in section 4.2 of of TR2008-918
 *
 * Each neighbor is paired with a different neighbor except when there is
 * exactly one neighbor. Routes are not symmetric. The random ordering 
 * of neighbors is built incrementally and may change with the addition 
 * of new neighbors.
 *
 * This routing table also exposes operations to snapshot and rebuild
 * the mappings in the table, but no direct persistence mechanism is provided.
 *
 * To preserve the repeatability of random routes, the table must be
 * persistent across runs of the software. This property is an important
 * part of limiting the ability of an adversary to gain additional knowledge
 * by creating "sybil nodes." Subclasses or external observers are expected
 * to provide appropriate persistence.
 *
 * In this implementation, priority is given to ensuring correct, consistent
 * non-blocking reads to threads performing routing lookups. Changes to the
 * table are expected to be very infrequent compared to reads and not
 * particularly contentious. Overall table size is expected to be very
 * small / negligible. 
 * 
 * A more complex implementation maybe appropriate if these expectations are
 * violated.
 *
 * A node may temporarily be pointed to by multiple routes during an in progress
 * operation but never becomes unreachable nor is unmapped during any modification
 * operation. Snapshot methods always produce results that represent some 
 * valid state that existed between modification operations and never contain 
 * multiple routings for the same neighbor.
 *
 * @see RandomRoutingTable
 */
public class BasicRandomRoutingTable implements RandomRoutingTable {

    /** routingTable contains a mapping between TrustGraphNeighbors.  Each entry
     * in the map (Key,Val) represents that the next hop for a message received
     * from the neighbor Key is the neighbor Val.
     */
    private final ConcurrentMap<TrustGraphNodeId, TrustGraphNodeId> routingTable;
    
    /** this list represents a random ordering of the neighbors in the table 
     * used to repeatably pick a random subset of neighbors to advertise to.
     * 
     * Note, it is not a threadsafe list. Operations accessing this list 
     * are synchronized externally and no iterators are exposed. It is intended
     * for synchronized / snapshot operations only. 
     */
    private final List<TrustGraphNodeId> orderedNeighbors;
    

    /** this Random source is used to shuffle and pick random routes when
     * modifying the routing table. 
     */
    private final Random rng;

    /**
     * Construct an empty BasicRandomRoutingTable with the default source of
     * randomness.
     */
    public BasicRandomRoutingTable() {
        this(new SecureRandom());
    }

    /**
     * Construct an empty BasicRandomRoutingTable with a given source of
     * randomness. 
     * 
     * @param rng source of randomness used for shuffling and random 
     *            route insertions.
     */
    public BasicRandomRoutingTable(final SecureRandom rng) {
        this(null, rng);
    }

    /** 
     * Construct a BasicRandomRoutingTable with the specified snapshot of the 
     * routing table state and the default source of randomness.
     *
     * @param snapshot a snapshot of the routing table state. If not valid,
     *        IllegalArgumentException is raised.
     * @throws IllegalArgumentException if snapshot is not valid
     * 
     */
    public BasicRandomRoutingTable(final RandomRoutingTable.Snapshot snapshot) {
        this(snapshot, new SecureRandom());
    }

    /** 
     * Construct a BasicRandomRoutingTable with the specified snapshot of the 
     * routing table state and the given source of randomness.
     *
     * @param snapshot a snapshot of the routing table state.  
     * @param rng source of randomness used for shuffling and random route
     *            insertions
     * @throws IllegalArgumentException if snapshot is not valid
     *
     */
    private BasicRandomRoutingTable(final RandomRoutingTable.Snapshot snapshot, 
        final SecureRandom rng) {
        if (snapshot != null) {
            // only construct from valid snapshots (throws IllegalArgumentException if invalid)
            validateSnapshot(snapshot);
            this.routingTable =
                new ConcurrentHashMap<TrustGraphNodeId,TrustGraphNodeId>(snapshot.getRoutes());
            this.orderedNeighbors = 
                new ArrayList<TrustGraphNodeId>(snapshot.getOrderedNeighbors());
        }
        else {
            this.routingTable =
                new ConcurrentHashMap<TrustGraphNodeId,TrustGraphNodeId>();
            this.orderedNeighbors = new ArrayList<TrustGraphNodeId>();
        }
        this.rng = rng;
    }

    
    /**
     * Determine the next hop for a message.
     *
     * @param message the message to determine the next hop for
     * @return the next TrustGraphNodeId to send the message to or null if
     *         the next hop cannot be determined.
     * @see RandomRoutingTable.getNextHop(TrustGraphAdvertisement)
     */
    @Override
    public TrustGraphNodeId getNextHop(final TrustGraphAdvertisement message) {
        final TrustGraphNodeId prev = message.getSender();
        return getNextHop(prev);
    }

    /**
     * Determine the next TrustGraphNodeId in a route containing
     * a given neighbor as the prior node.  The next hop is the
     * TrustGraphNodeId paired with the given neighbor in the table.
     * 
     * @param priorNeighbor the prior node on the route
     * @return the next TrustGraphNodeId to route a message to
     *         or null if the next hop cannot be determined.
     * @see RandomRoutingTable.getNextHop(TrustGraphNodeId)
     */
    @Override
    public TrustGraphNodeId getNextHop(final TrustGraphNodeId priorNeighbor) {
        if (priorNeighbor != null) {
            return routingTable.get(priorNeighbor);
        }
        else {
            return null;
        }
    }

    /**
     * Add a single TrustGraphNodeId to the routing table.
     * 
     * A random existing route X->Y is split into two routes, 
     * X -> neighbor, neighbor -> Y to accommodate the new neighbor.
     * If there are no existing routes, the neighbor is mapped
     * to itself.
     * 
     * If there is an existing route of the form neighbor -> X in 
     * the routing table, this operation has no effect.
     * 
     * @param neighbor the TrustGraphNieghbor to add
     * @see RandomRoutingTable.addNeighbor(TrustGraphNodeId)
     */
    @Override
    public void addNeighbor(final TrustGraphNodeId neighbor) {
        if (neighbor == null) {
            return;
        }

        // all modification operations are serialized
        synchronized(this) {

            // do not add this neighbor if it is already present
            if (contains(neighbor)) {
                return;
            }

            /* If there is nothing in the table, route the neighbor to itself.
             * this condition is fixed during the next single addition since
             * the route is always selected to be split. The bulk add
             * operation also takes special care to perform a single addition
             * if this state exists before adding additional routes.
             */
            if (routingTable.isEmpty()) {
                routingTable.put(neighbor, neighbor);
            }
            /* otherwise, pick a random existing route X->Y and 
             * split it into two routes, X->neighbor and neighbor->Y
             */
            else {
                Map.Entry<TrustGraphNodeId,TrustGraphNodeId> split =
                    randomRoute();      
                TrustGraphNodeId splitKey = split.getKey();
                TrustGraphNodeId splitVal = split.getValue();

                /*
                 * The new route neighbor->Y is inserted first. This 
                 * preserves the existing routing behavior for readers
                 * until the entire operation is complete.
                 */ 
                routingTable.put(neighbor, splitVal);
                routingTable.replace(splitKey, neighbor);
            }
            
            // add the neighbor to the ordering
            addNeighborToOrdering(neighbor);
        }
    }

    /** 
     * internal helper method to pick a random route from the table.
     */
    protected Map.Entry<TrustGraphNodeId,TrustGraphNodeId> randomRoute() {
        final int routeNumber = rng.nextInt(routingTable.size()); 
        final Iterator<Map.Entry<TrustGraphNodeId,TrustGraphNodeId>> routes =
                routingTable.entrySet().iterator();
        for (int i = 0; i < routeNumber; i++) { routes.next(); }
        return routes.next();
    }
    
    /**
     * Add a group of TrustGraphNeighbors to the routing table.
     *
     * A maximum of one route is disrupted by this operation, 
     * as many routes as possible are assigned within the group.
     * 
     * Any previously mapped neighbors will be ignored. 
     * 
     * @param neighbor the set of TrustGraphNieghbors to add
     * @see addNeighbor
     * @see RandomRoutingTable.addNeighbors(Collection<TrustGraphNeighbor>)
     */
    @Override
    public void addNeighbors(final Collection<TrustGraphNodeId> neighborsIn) {
        if (neighborsIn.isEmpty()) {
            return;
        }

        // all modification operations are serialized
        synchronized (this) {

            /* filter out any neighbors that are already in the routing table
             * and the new ones to the newNeighbors list
             */
            final LinkedList<TrustGraphNodeId> newNeighbors =
                new LinkedList<TrustGraphNodeId>();
            for (TrustGraphNodeId n : neighborsIn) {
                if (!contains(n)) {
                    newNeighbors.add(n);
                }
            }
            
            // if there is nothing new, we're done.
            if (newNeighbors.size() == 0) {
                return;
            }
            // handle list of length 1 the same way as a single insertion
            else if (newNeighbors.size() == 1) {
                addNeighbor(newNeighbors.get(0));
                return;
            }
            // otherwise there is more than one new neighbor to add.

            /* if there are existing routes, a random route in the 
             * table will be split.  It is picked prior to adding 
             * any of the new routes.
             */
            Map.Entry<TrustGraphNodeId,TrustGraphNodeId> split = null;
            if (!routingTable.isEmpty()) {      
                // Pick a random existing route X->Y to split
                split = randomRoute();
            }

            /* Create a random permutation of the list.
             * Add in new neighbors from this permutation, routing
             * i->i+1. These routes do not disturb any existing
             * routes and create no self references.
             */
            Collections.shuffle(newNeighbors, rng);
            final Iterator<TrustGraphNodeId> i = newNeighbors.iterator();
            TrustGraphNodeId key = i.next();
            while (i.hasNext()) {
                final TrustGraphNodeId val = i.next();
                routingTable.put(key,val);
                key = val;
            }

            /* if there was nothing in the routing table yet, the first 
             * item in the permutation is routed to the last.
             */
            if (split == null) {
                // loop around, bind the last to the first
                routingTable.put(newNeighbors.getLast(), newNeighbors.getFirst());
            }

            /* Otherwise, split the route chosen beforehand.  Map the
             * key to the first node in the chain, and map the
             * last node in the chain to the value. ie X->Y becomes 
             * X->first->....->last->Y.  Similarly to the single 
             * neighbor add method above, this preserves the structure
             * of the routing table forming some circular chain of 
             * nodes of length equal to the size of the table.
             */
            else {
                TrustGraphNodeId splitKey = split.getKey();
                TrustGraphNodeId splitVal = split.getValue();

                /*
                 * Add routes X-> first and last->Y. The new route last->Y is
                 * inserted first. This preserves the existing routing behavior
                 * for readers until the entire operation is complete.
                */ 
                routingTable.put(newNeighbors.getLast(), splitVal);
                routingTable.replace(splitKey, newNeighbors.getFirst());
            }

            /* add the new neighbors into the ordering */
            addNeighborsToOrdering(newNeighbors);
        }
    }

    /** 
     * Internal helper method.
     * 
     * implements the policy for removing a single node from the routing table.
     *
     * If the node is mapped to itself, the route is removed. Otherwise this 
     * operation merges the two routes of the form X -> neighbor ,
     * neighbor -> Y into X -> Y.  
     * 
     * If the table does not contain the referenced neighbor, 
     * this operation has no effect.
     *
     * Assumes any necessary locking is done externally.
     */
    protected void removeNeighborFromRoutingTable(final TrustGraphNodeId neighbor) {

        /* find the neighbor that the node being removed is mapped to
         * ie the route neighbor -> mergeVal
         */
        final TrustGraphNodeId mergeVal = routingTable.get(neighbor);

        // if it is mapped to itself, just remove it and return.
        if (mergeVal.equals(neighbor)) {
            routingTable.remove(neighbor);
            // this should only happen when there was only a single entry.
            if (!routingTable.isEmpty()) {
                assert false;//routingTable.isEmpty();
            }
            return;
        }

        /* If it wasn't mapped to itself, find the neighbor that is
         * currently mapped to the neighbor being removed, ie the 
         * route mergeKey -> neighbor.
         */
        TrustGraphNodeId mergeKey = null;
        for (Map.Entry<TrustGraphNodeId,TrustGraphNodeId> e : routingTable.entrySet()) {
            if (e.getValue().equals(neighbor)) {
                mergeKey = e.getKey();
                break;
            }
        }
        assert mergeKey != null;

        /**
         * Atomically merge the route X->neighbor, neighbor->Y into the
         * route X->Y.This preserves the ability to route to Y. Finally,
         * remove the mapping neighbor->Y.
         */
        routingTable.replace(mergeKey, mergeVal);
        routingTable.remove(neighbor);

    }

    /**
     * Remove a single TrustGraphNodeId from the routing table
     * 
     * If the node is mapped to itself, the route is removed. Otherwise this 
     * operation merges the two routes of the form X -> neighbor ,
     * neighbor -> Y into X -> Y.  
     * 
     * If the table does not contain the referenced neighbor, 
     * this operation has no effect.
     *
     * @param neighbor the TrustGraphNodeId to remove
     * @see RandomRoutingTable.removeNeighbor
     */
    @Override
    public void removeNeighbor(final TrustGraphNodeId neighbor) {
        // all modification operations are serialized
        synchronized(this) {
            // do nothing if there is no entry for the neighbor specified
            if (!contains(neighbor)) {
                return;
            }

            /* first remove the neighbor from the ordering. This will 
             * prevent it from being advertised to.
             */
            removeNeighborFromOrdering(neighbor);
            removeNeighborFromRoutingTable(neighbor);
        }
    }

    /**
     * Remove a set of TrustGraphNeighbors from the routing table.
     *
     * This operation does not occur atomically, each neighbor is removed in
     * turn equivalently to a series of calls to removeNeighbor.
     * 
     * @param neighbors the TrustGraphNeighbors to remove
     * @see RandomRoutingTable.removeNeighbors
     */
    @Override
    public void removeNeighbors(final Collection<TrustGraphNodeId> neighbors) {
        synchronized(this) {
            // remove the neighbors from the ordering in bulk
            removeNeighborsFromOrdering(neighbors);

            // just loop over the neighbors and use the single removal operation.
            for (TrustGraphNodeId n : neighbors) { removeNeighborFromRoutingTable(n); }
        }
    }

    /**
     * Remove all entries from the routing table.
     * @see RandomRoutingTable.clear
     */
    @Override
    public void clear() {
        routingTable.clear();
    }

    /**
     * Internal policy method.
     * 
     * Implements the policy for updating the random ordering of 
     * neighbors when a new neighbor is added. By default this 
     * inserts at the back and then swaps with a random neighbor.
     * 
     * Note: When using this policy, adding neighbors may distrupt the 
     * set of neighbors that are advertised to in the case that
     * only a subset is used.
     *
     * It is assumed that the neighbor is not already in the list.
     *
     */
    protected void addNeighborToOrdering(TrustGraphNodeId neighbor) {

        int position = rng.nextInt(orderedNeighbors.size()+1);
        
        if (position == orderedNeighbors.size()) {
            orderedNeighbors.add(neighbor);
        }
        else {
            orderedNeighbors.add(position, neighbor);
        }
    }

    /**
     * Internal helper method.
     * 
     * Implements the policy for updating the random ordering of 
     * neighbors when new neighbors are added. By default this 
     * calls the single add neighbor rule on each neighbor in the 
     * order specified.
     * 
     * It is assumed that none of the neighbors added are already 
     * in the list and that any necessary locking is performed externally. 
     */
    protected void addNeighborsToOrdering(List<TrustGraphNodeId> neighbors) {
        for (TrustGraphNodeId n : neighbors) { addNeighborToOrdering(n); }
    }

    /**
     * Internal helper method.
     * 
     * Implements the policy for removing a neighbor from the random  
     * ordering.  By default, this just removes the neighbor.
     * 
     * This method assumes that any necessary locking is performed externally. 
     */
    protected void removeNeighborFromOrdering(TrustGraphNodeId neighbor) {
        ListIterator<TrustGraphNodeId> i = orderedNeighbors.listIterator();
        while (i.hasNext()) {
            TrustGraphNodeId cur = i.next();
            if (cur.equals(neighbor)) {
                i.remove();
            }
        }
    }

    /**
     * Internal helper method. 
     *
     * Implements the policy for removing multiple neighbors from the random  
     * ordering.  By default, this removes all the neighbors specified and 
     * leaves the list otherwise in the same ordering.
     *
     * This method assumes that any necessary locking is performed externally. 
     */
    protected void removeNeighborsFromOrdering(
        final Collection<TrustGraphNodeId> neighbors) {
        final Set<TrustGraphNodeId> killSet = 
            new HashSet<TrustGraphNodeId>(neighbors);
        ListIterator<TrustGraphNodeId> i = orderedNeighbors.listIterator();
        while (i.hasNext()) {
            if (killSet.contains(i.next())) {
                i.remove();
            }
        }
    }

    /**
     *
     * Retrieve a snapshot of the list of TrustGraphNeighbors in the routing
     * table.  The list is randomly ordered, but the ordering does not 
     * change between calls unless there are modifications to the list of 
     * neighbors.
     *
     * @return the random ordering of TrustGraphNeighbors in the routing table
     * @see RandomRoutingTable.getOrderedNeighbors()
     */
    @Override
    public List<TrustGraphNodeId> getOrderedNeighbors() {
        // block modifications while constructing the snapshot
        synchronized(this) {
            return new ArrayList<TrustGraphNodeId>(orderedNeighbors);
        }
    }

    // class for holding a snapshot of this routing table's state
    public static class Snapshot implements RandomRoutingTable.Snapshot {
        private Map<TrustGraphNodeId,TrustGraphNodeId> routes;
        private List<TrustGraphNodeId> neighbors;
        public Snapshot(Map<TrustGraphNodeId,TrustGraphNodeId> routes, List<TrustGraphNodeId> neighbors) {
            this.routes = new HashMap<TrustGraphNodeId, TrustGraphNodeId>(routes);
            this.neighbors = new ArrayList<TrustGraphNodeId>(neighbors);
        }
        @Override
        public Map<TrustGraphNodeId,TrustGraphNodeId> getRoutes() {return routes;}
        @Override
        public List<TrustGraphNodeId> getOrderedNeighbors() {return neighbors;}
    }

    /**
     * Creates a snapshot of the current state of the routing table. 
     * A mapping X->Y between two TrustGraphNeighbors represents that 
     * the next hop of a message received from the neighbor X is Y. 
     * 
     * This snapshot will contain each neighbor exactly once as a key
     * and once as a value.
     * 
     * @return a snapshot of the current state of the routing table 
     *         represented as a Map between TrustGraphNeigbors. 
     * @see RandomRoutingTable.snapshot()
     */
    @Override
    public RandomRoutingTable.Snapshot snapshot() {
        
        // block modification operations while creating the snapshot
        synchronized (this) {
            return new Snapshot(new HashMap<TrustGraphNodeId,TrustGraphNodeId>(routingTable),
                                new ArrayList<TrustGraphNodeId>(orderedNeighbors));
        }
    }

    /**
     * @return true if and only if the neighbor specified is in the routing
     *         table.
     * @see RandomRoutingTable.contains()
     */
    @Override
    public boolean contains(TrustGraphNodeId neighbor) {
        return routingTable.containsKey(neighbor);
    }

    /**
     * @return the number of neighbors/routes in the routing table.
     *
     */
    @Override
    public int size() { return routingTable.size(); }

    /**
     * @return true if and only if there are no routes in the table
     */ 
    @Override
    public boolean isEmpty() { return routingTable.isEmpty(); }


    /** 
     * 
     * @return true if and only if the snapshot is valid according to
     *         BasicRandomRoutingTable.validateSnapshot(snapshot)
     */
    public static boolean isValidSnapshot(RandomRoutingTable.Snapshot snapshot) {
        try {
            BasicRandomRoutingTable.validateSnapshot(snapshot);
            return true;
        }
        catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     *
     * verifies that the map given constitutes a valid state of a
     * BasicRandomRouting table.  verifies the following properties:
     * 
     * - Each neighbor in the map has a next hop and appears as the
     *   of some neighbor, ie the set of keys is equal to the set 
     *   of values.
     *   
     * - contains a node referencing itself only if the size of the 
     *   map is exactly one.
     * 
     * - contains one cycle of keys/values equal to the number of routes
     * 
     * - set of routing keys is the same as the set of ordered neighbors
     * 
     * - ordered neighbors does not contain any duplicates
     *
     * Other routing table strategies may enforce other properties, 
     * these qualities are required for this table's expectations.
     *
     * @param snapshot the snapshot to validate 
     * @throws IllegalArgumentException if the snapshot is invalid
     */
    public static void validateSnapshot(RandomRoutingTable.Snapshot snapshot) {


        /* Check that the set of keys is equal to the set of values.  This 
         * shows that each neighbor appearing as either a key or a value 
         * has a next hop and appears as the next hop of some neighbor.
         */
        final Map<TrustGraphNodeId,TrustGraphNodeId> routes = snapshot.getRoutes();
        final Set<TrustGraphNodeId> keys = routes.keySet();
        final Set<TrustGraphNodeId> values = 
            new HashSet<TrustGraphNodeId>(routes.values());
        if (!(keys.size() == values.size()) ||
            !(keys.containsAll(values) && values.containsAll(keys))) {
            throw new IllegalArgumentException("Snapshot keys and values are not the same set.");
        }

        /* If the table contains more than one route, return false if 
         * any neighbor appears as its own next hop or the routing 
         * does not form a cycle of length equal to the size.
         */
        if (routes.size() > 1) {
            // check for anything routed to itself
            for (Map.Entry<TrustGraphNodeId, TrustGraphNodeId> e :
                    routes.entrySet()) {
                if (e.getKey().equals(e.getValue())) {
                    throw new IllegalArgumentException("Snapshot contains self-routed neighbors.");
                }
            }

            /* The routing should form a single cycle of length equal to
             * the size of the routes.  The table depends on this property
             * to avoid forming self references.  To test this property,
             * the chain of keys and values k->k1->k2->k3...ksize is 
             * followed and checked for any repetition.
             */
            final Set<TrustGraphNodeId> seen = new HashSet<TrustGraphNodeId>();
            TrustGraphNodeId firstKey = routes.keySet().iterator().next();
            TrustGraphNodeId key = firstKey;
            for (int i = 0; i < routes.size() - 1; i++) {
                seen.add(key);
                key = routes.get(key);
                if (seen.contains(key)) {
                    // cycle was too short, not valid.
                    throw new IllegalArgumentException("Snapshot contains cycles smaller than the size of the table.");
                }
            }
            // the cycle must close.
            if (!routes.get(key).equals(firstKey)) {
                throw new IllegalArgumentException("Unclosed cycle of neighbors.");
            }
        }


        // check that the ordered list contains no duplicates
        final List<TrustGraphNodeId> orderedNeighbors = snapshot.getOrderedNeighbors();
        final Set<TrustGraphNodeId> orderSet = new HashSet<TrustGraphNodeId>(orderedNeighbors);
        if (orderedNeighbors.size() != orderSet.size()) {
            throw new IllegalArgumentException("Ordered neighbors contains duplicates.");
        }

        // check that the routed neighbors and ordered neighbors are the same set
        if (orderedNeighbors.size() != keys.size()) {
            throw new IllegalArgumentException("Routed neighbors and ordered neighbors do not match.");
        }
        for (TrustGraphNodeId n : orderedNeighbors) {
            if (!keys.contains(n)) {
                throw new IllegalArgumentException("Routed neighbors and ordered neighbors do not match.");
            }
        }
    }

    @Override
    public String toString() {
        return "BasicRandomRoutingTable [routingTable=" + routingTable
                + ", orderedNeighbors=" + orderedNeighbors + "]";
    }
}