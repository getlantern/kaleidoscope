package org.kaleidoscope;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.kaleidoscope.Helpers.createNeighbors;
import static org.kaleidoscope.Helpers.snapshotsAreEquivalent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;


/**
 * This class contains basic unit tests for the 
 * BasicRandomRoutingTable class.
 *
 */
public class TestBasicRandomRoutingTable {

    public TestBasicRandomRoutingTable() {}
    
    /**
     * Tests the behavior or BasicRandomRoutingTable.isValidSnapshot
     * and that validation is performed during construction.
     */
    @Test
    public void testSnapshotValidation() throws Exception {

        // create a small pool of neighbor nodes to test with
        final List<TrustGraphNodeId> neighbors = createNeighbors(10);
        final int halfSize = neighbors.size() / 2;

        Map<TrustGraphNodeId,TrustGraphNodeId> routes;
        RandomRoutingTable.Snapshot snapshot;
        List<TrustGraphNodeId> orderList;

        /* The empty mapping is a valid mapping.
         */
        routes = new HashMap<TrustGraphNodeId, TrustGraphNodeId>();
        snapshot = new BasicRandomRoutingTable.Snapshot(routes,
            new ArrayList<TrustGraphNodeId>(routes.keySet()));
        assertTrue(BasicRandomRoutingTable.isValidSnapshot(snapshot));


        /* A single neighbor mapped to itself is valid
         */
        routes = new HashMap<TrustGraphNodeId, TrustGraphNodeId>();
        routes.put(neighbors.get(0), neighbors.get(0));
        snapshot = new BasicRandomRoutingTable.Snapshot(routes,
            new ArrayList<TrustGraphNodeId>(routes.keySet()));
        assertTrue(BasicRandomRoutingTable.isValidSnapshot(snapshot));

        /* The mapping that maps each neighbor to the next neighbor
         * in the list (circularly) is a valid mapping
         */

        // form the cirucular mapping i -> (i+1)%size
        routes = new HashMap<TrustGraphNodeId, TrustGraphNodeId>();
        for (int i = 0; i < neighbors.size(); i++) {
            TrustGraphNodeId from = neighbors.get(i);
            TrustGraphNodeId to = neighbors.get( (i+1) % neighbors.size() );
            routes.put(from,to);
        }
        snapshot = new BasicRandomRoutingTable.Snapshot(routes,
            new ArrayList<TrustGraphNodeId>(routes.keySet()));
        assertTrue(BasicRandomRoutingTable.isValidSnapshot(snapshot));

        /* A mapping that contains different keys and values
         * is not valid (all keys must appear as values and 
         * vice versa)
         */
        routes = new HashMap<TrustGraphNodeId, TrustGraphNodeId>();
        for (int i = 0; i < halfSize; i++) {
            routes.put(neighbors.get(i), neighbors.get(i + halfSize));
        }
        snapshot = new BasicRandomRoutingTable.Snapshot(routes,
            new ArrayList<TrustGraphNodeId>(routes.keySet()));
        assertFalse(BasicRandomRoutingTable.isValidSnapshot(snapshot));
        try {new BasicRandomRoutingTable(snapshot); fail("Expected IllegalArgumentException.");}
        catch (IllegalArgumentException e) {} 



        /* A mapping that contains a single route mapping one neighbor
         * to another is not valid. (not all keys are values and 
         * vice versa)
         */
        routes = new HashMap<TrustGraphNodeId, TrustGraphNodeId>();
        routes.put(neighbors.get(0), neighbors.get(1));
        snapshot = new BasicRandomRoutingTable.Snapshot(routes,
            new ArrayList<TrustGraphNodeId>(routes.keySet()));
        assertFalse(BasicRandomRoutingTable.isValidSnapshot(snapshot));
        try {new BasicRandomRoutingTable(snapshot); fail("Expected IllegalArgumentException.");}
        catch (IllegalArgumentException e) {} 


        /* A mapping that contains more than one neighbor mapped to 
         * itself is not valid. (this is only permitted for one 
         * neighbor, and must be fixed with any additional neighbor)
         */
        routes = new HashMap<TrustGraphNodeId, TrustGraphNodeId>();
        routes.put(neighbors.get(0), neighbors.get(0));
        routes.put(neighbors.get(1), neighbors.get(1));
        snapshot = new BasicRandomRoutingTable.Snapshot(routes,
            new ArrayList<TrustGraphNodeId>(routes.keySet()));
        assertFalse(BasicRandomRoutingTable.isValidSnapshot(snapshot));
        try {new BasicRandomRoutingTable(snapshot); fail("Expected IllegalArgumentException.");}
        catch (IllegalArgumentException e) {} 


        /* A mapping containing one neighbor mapped to iself and 
         * more than one route in total is not valid. (self mapped
         * neighbor is only permitted at size = 1)
         */
        routes = new HashMap<TrustGraphNodeId, TrustGraphNodeId>();
        routes.put(neighbors.get(0), neighbors.get(0));
        routes.put(neighbors.get(1), neighbors.get(2));
        snapshot = new BasicRandomRoutingTable.Snapshot(routes, 
            new ArrayList<TrustGraphNodeId>(routes.keySet()));
        assertFalse(BasicRandomRoutingTable.isValidSnapshot(snapshot));
        try {new BasicRandomRoutingTable(snapshot); fail("Expected IllegalArgumentException.");}
        catch (IllegalArgumentException e) {} 


        /* A mapping that has more than one cycle in it is not 
         * valid.  The routing must form one cycle of length 
         * equal to the size of the table.
         */

        // form two circular mappings using half the list each
        routes = new HashMap<TrustGraphNodeId, TrustGraphNodeId>();
        for (int i = 0; i < halfSize; i++) {
            TrustGraphNodeId from = neighbors.get(i);
            TrustGraphNodeId to = neighbors.get( (i+1) % halfSize );
            routes.put(from,to);

            from = neighbors.get(halfSize+i);
            to = neighbors.get(halfSize+((i+1) % halfSize));
            routes.put(from,to);
        }
        snapshot = new BasicRandomRoutingTable.Snapshot(routes, 
            new ArrayList<TrustGraphNodeId>(routes.keySet()));
        assertFalse(BasicRandomRoutingTable.isValidSnapshot(snapshot));
        try {new BasicRandomRoutingTable(snapshot); fail("Expected IllegalArgumentException.");}
        catch (IllegalArgumentException e) {} 


        /* An ordered neighbor list that does not match the routed neighbors 
         * is not valid.
         */
        routes = new HashMap<TrustGraphNodeId, TrustGraphNodeId>();
        routes.put(neighbors.get(0),neighbors.get(1));
        routes.put(neighbors.get(1),neighbors.get(0));
        snapshot = new BasicRandomRoutingTable.Snapshot(routes, 
            new ArrayList<TrustGraphNodeId>(routes.keySet()));
        // should be fine with the same keys and neighbors list
        assertTrue(BasicRandomRoutingTable.isValidSnapshot(snapshot));
        // should fail if neighbors are missing
        List<TrustGraphNodeId> badList = new LinkedList<TrustGraphNodeId>();
        badList.add(neighbors.get(0));
        snapshot = new BasicRandomRoutingTable.Snapshot(routes, badList);
        assertFalse(BasicRandomRoutingTable.isValidSnapshot(snapshot));
        try {new BasicRandomRoutingTable(snapshot); fail("Expected IllegalArgumentException.");}
        catch (IllegalArgumentException e) {} 
        // should fail if extra neighbors are included
        orderList = new LinkedList<TrustGraphNodeId>();
        badList.add(neighbors.get(0));
        badList.add(neighbors.get(1));
        badList.add(neighbors.get(2));
        snapshot = new BasicRandomRoutingTable.Snapshot(routes, badList);
        assertFalse(BasicRandomRoutingTable.isValidSnapshot(snapshot));
        try {new BasicRandomRoutingTable(snapshot); fail("Expected IllegalArgumentException.");}
        catch (IllegalArgumentException e) {} 

        /* An ordered neighbor list that has duplicates is not valid
         */
        routes = new HashMap<TrustGraphNodeId, TrustGraphNodeId>();
        routes.put(neighbors.get(0),neighbors.get(1));
        routes.put(neighbors.get(1),neighbors.get(0));
        // should be fine with no dupes
        orderList = new LinkedList<TrustGraphNodeId>();
        orderList.add(neighbors.get(0));
        orderList.add(neighbors.get(1));
        snapshot = new BasicRandomRoutingTable.Snapshot(routes,orderList);
        assertTrue(BasicRandomRoutingTable.isValidSnapshot(snapshot));
        // should fail if the neighbors list has duplicates
        orderList = new LinkedList<TrustGraphNodeId>();
        orderList.add(neighbors.get(0));
        orderList.add(neighbors.get(1));
        orderList.add(neighbors.get(1));
        snapshot = new BasicRandomRoutingTable.Snapshot(routes, orderList);
        assertFalse(BasicRandomRoutingTable.isValidSnapshot(snapshot));
        try {new BasicRandomRoutingTable(snapshot); fail("Expected IllegalArgumentException.");}
        catch (IllegalArgumentException e) {} 

    }


    /**
     * Tests that addNeighbors creates a valid 
     * set of routes.
     */
    @Test
    public void testAddNeighborsBasic() throws Exception {
    
        RandomRoutingTable rt = new BasicRandomRoutingTable();

        /* create a large group of neigbors and add them 
         * in a single bulk add operation.
         */
        Collection<TrustGraphNodeId> neighbors =
            createNeighbors(1000);
        rt.addNeighbors(neighbors);

        // there should be a route for every neighbor added
        for (TrustGraphNodeId n : neighbors) { assertTrue(rt.contains(n)); }

        // every neighbor should be routed to someone else
        for (TrustGraphNodeId n : neighbors) {
            TrustGraphNodeId next = rt.getNextHop(n);
            assertTrue(next != null && !next.equals(n));
        }

        // A snapshot of the resulting routing table should validate
        RandomRoutingTable.Snapshot snapshot = rt.snapshot();
        Map<TrustGraphNodeId,TrustGraphNodeId> routes = snapshot.getRoutes();
        assertTrue(routes.size() == neighbors.size());
        assertTrue(BasicRandomRoutingTable.isValidSnapshot(snapshot));
        
        // every neighbor should be in the snapshot
        for (TrustGraphNodeId n : neighbors) {
            assertTrue(routes.containsKey(n)); 
            assertTrue(routes.containsValue(n));
        }
    }

    /**
     * Tests that using the bulk addNeighbors function does not 
     * disrupt more than one previous route.
     */
    @Test
    public void testAddNeighborsPreservesRoutes() throws Exception {
    
        RandomRoutingTable rt = new BasicRandomRoutingTable();

        /* create a group of neighbors and add them to the 
         * routing table.  A snapshot of the routing is created
         * so that it can be compared to the routing table 
         * after adding additional neighbors.
         */
        Collection<TrustGraphNodeId> originalNeighbors =
            createNeighbors(500);
        rt.addNeighbors(originalNeighbors);
        RandomRoutingTable.Snapshot snapshot = rt.snapshot();
        Map<TrustGraphNodeId,TrustGraphNodeId> routes = snapshot.getRoutes();

        /* count tracks how many neighbors have been added
         * to the routing so far.
         */
        int count = originalNeighbors.size();

        /* for a few randomly chosen sizes, bulk add 
         * neighbors and check the properties 
         * of the routing.
         */
        Random rng = new Random();
        for (int i = 0; i < 10; i++) {
            int newCount = 100 + rng.nextInt(400);
            Collection<TrustGraphNodeId> newNeighbors =
                    createNeighbors(newCount);
            rt.addNeighbors(newNeighbors);

            /* check that at most one route has changed by counting
             * the number of routes that are the same as the last 
             * run.
             */
            int sameCount = 0;
            for (Map.Entry<TrustGraphNodeId,TrustGraphNodeId> e :
                    routes.entrySet()) {
                if (rt.getNextHop(e.getKey()).equals(e.getValue())) {
                    sameCount += 1;
                }
            }
            assertTrue(routes.size() - sameCount <= 1);

            // check that all the new neighors are also routed
            for (TrustGraphNodeId n : newNeighbors) { assertTrue(rt.contains(n)); }

            /* take a new snapshot and ensure that it 
             * has the correct size and is valid
             */ 
            snapshot = rt.snapshot();
            routes = snapshot.getRoutes();
            count += newCount;
            assertTrue(routes.size() == count);
            assertTrue(BasicRandomRoutingTable.isValidSnapshot(snapshot));
        }
    }   

    /**
     * Tests that a call to addNeighbors fixes any self-mapped
     * neighbors.  
     */
    @Test
    public void testAddNeighborsFixesSelfMapping() throws Exception {

        /* this tests runs with a few different low size lists being passed 
         * to the addNeighbors method to make sure that degenerate cases 
         * like a list of size 1 are handled correctly.
         */
        for (int neighborCount = 1; neighborCount < 5; neighborCount++) {

            // create a new routing table
            RandomRoutingTable rt = new BasicRandomRoutingTable();
            RandomRoutingTable.Snapshot snapshot;
            Map<TrustGraphNodeId,TrustGraphNodeId> routes;

            // create a lone neighbor and a list of other neighbors
            TrustGraphNodeId loner = createNeighbors(1).get(0);
            Collection<TrustGraphNodeId> neighbors =
                createNeighbors(neighborCount);
            
            // add the single neighbor and make sure it is mapped to itself 
            rt.addNeighbor(loner);
            snapshot = rt.snapshot();
            routes = snapshot.getRoutes();
            assertTrue(routes.size() == 1);
            assertTrue(routes.get(loner).equals(loner));

            /* add the rest of the neighbors and make sure that the loner 
             * is no longer mapped to iself and that otherwise the 
             * routing is valid.
             */
            rt.addNeighbors(neighbors);
            snapshot = rt.snapshot();
            routes = snapshot.getRoutes();
            assertTrue(routes.size() == 1 + neighbors.size());
            assertTrue(!routes.get(loner).equals(loner));
            assertTrue(BasicRandomRoutingTable.isValidSnapshot(snapshot));
        }
    }

    /**
     * Tests that the single add neighbor method 
     * addNeighbor creates a valid set of routes.
     */
    @Test
    public void testAddNeighborBasic() throws Exception {
        RandomRoutingTable rt = new BasicRandomRoutingTable();

        /* create a large group of neigbors and add them 
         * individually.
         */
        Collection<TrustGraphNodeId> neighbors =
            createNeighbors(1000);
        for (TrustGraphNodeId n : neighbors) { rt.addNeighbor(n); }

        // there should be a route for every neighbor added
        for (TrustGraphNodeId n : neighbors) { assertTrue(rt.contains(n)); }

        // every neighbor should be routed to someone else
        for (TrustGraphNodeId n : neighbors) {
            TrustGraphNodeId next = rt.getNextHop(n);
            assertTrue(next != null && !next.equals(n));
        }

        // A snapshot of the resulting routing table should validate
        RandomRoutingTable.Snapshot snapshot = rt.snapshot();
        Map<TrustGraphNodeId,TrustGraphNodeId> routes = snapshot.getRoutes();
        assertTrue(routes.size() == neighbors.size());
        assertTrue(BasicRandomRoutingTable.isValidSnapshot(snapshot));
        
        // every neighbor should be in the snapshot
        for (TrustGraphNodeId n : neighbors) {
            assertTrue(routes.containsKey(n));
            assertTrue(routes.containsValue(n));
        }

    }

    /**
     * Tests that adding neighbors via the single add 
     * method fixes self mappings.
     */
    @Test
    public void testAddNeighborFixesSelfMapping() {
        RandomRoutingTable rt = new BasicRandomRoutingTable();
        RandomRoutingTable.Snapshot snapshot;
        Map<TrustGraphNodeId,TrustGraphNodeId> routes;

        // create a set of neighbors to add
        List<TrustGraphNodeId> neighbors = createNeighbors(4);

        // add the first neighbor, it should just contain 
        // the first neighbor mapped to itself
        TrustGraphNodeId first = neighbors.get(0);
        rt.addNeighbor(first);
        assertTrue(rt.getNextHop(first).equals(first));
        snapshot = rt.snapshot();
        routes = snapshot.getRoutes();
        assertTrue(routes.size() == 1);
        assertTrue(BasicRandomRoutingTable.isValidSnapshot(snapshot));

        // add the other neighbors.  This should immediately fix
        // the self routing.
        for (int i = 1; i < neighbors.size(); i++) {
            rt.addNeighbor(neighbors.get(i));
            assertTrue(!rt.getNextHop(first).equals(first));
            snapshot = rt.snapshot();
            routes = snapshot.getRoutes();
            assertTrue(routes.size() == 1 + i);
            assertTrue(BasicRandomRoutingTable.isValidSnapshot(snapshot));          
        }
    }

    /**
     * Tests that different calls to the AddNeighbors 
     * with the same inputs produce different routings,
     * ie "random."
     *
     */
    @Test
    public void testAddNeighborsIsDynamic() {
        // create two routing tables 
        RandomRoutingTable rt1 = new BasicRandomRoutingTable();
        RandomRoutingTable rt2 = new BasicRandomRoutingTable();

        // create a set of neighbors to add
        List<TrustGraphNodeId> neighbors = createNeighbors(1000);
        RandomRoutingTable.Snapshot snapshot1;
        RandomRoutingTable.Snapshot snapshot2;
        Map<TrustGraphNodeId,TrustGraphNodeId> routes1;
        Map<TrustGraphNodeId,TrustGraphNodeId> routes2;


        // add the same thing to both tables, and snapshot them
        rt1.addNeighbors(neighbors);
        rt2.addNeighbors(neighbors);
        snapshot1 = rt1.snapshot(); 
        snapshot2 = rt2.snapshot();
        routes1 = snapshot1.getRoutes();
        routes2 = snapshot2.getRoutes();


        // the snapshots should both be valid, and 
        // contain the same neighbors, but different routes (whp)
        assertTrue(BasicRandomRoutingTable.isValidSnapshot(snapshot1));
        assertTrue(BasicRandomRoutingTable.isValidSnapshot(snapshot2));
        for (TrustGraphNodeId n: neighbors) {
            assertTrue(routes1.containsKey(n));
            assertTrue(routes2.containsKey(n));
        }

        /* test for at least one differing route 
         * (should succeed with very high probability) 
         */
        boolean mappingIsDifferent = false;
        for (TrustGraphNodeId n: neighbors) {
            // if the routing for this neighbor in 
            // snapshot1 is different than snapshot2, the 
            // test is a success.
            if (!routes1.get(n).equals(routes2.get(n))) {
                mappingIsDifferent = true;
                break;
            }
        }
        assertTrue(mappingIsDifferent);


    }

    /**
     * Tests that different seqential calls to the AddNeighbor 
     * with the same inputs produce different routings,
     * ie "random"  
     */
    @Test
    public void testAddNeighborIsDynamic() {
        // create two routing tables 
        RandomRoutingTable rt1 = new BasicRandomRoutingTable();
        RandomRoutingTable rt2 = new BasicRandomRoutingTable();

        // create a set of neighbors to add
        List<TrustGraphNodeId> neighbors = createNeighbors(1000);
        RandomRoutingTable.Snapshot snapshot1;
        RandomRoutingTable.Snapshot snapshot2;
        Map<TrustGraphNodeId,TrustGraphNodeId> routes1;
        Map<TrustGraphNodeId,TrustGraphNodeId> routes2;

        /* add the same thing to both tables in the same order, 
         * and snapshot them
         */
        for (TrustGraphNodeId n : neighbors) { rt1.addNeighbor(n); }
        for (TrustGraphNodeId n : neighbors) { rt2.addNeighbor(n); }
        snapshot1 = rt1.snapshot(); 
        snapshot2 = rt2.snapshot();
        routes1 = snapshot1.getRoutes();
        routes2 = snapshot2.getRoutes();

        // the snapshots should both be valid, and 
        // contain the same neighbors, but different routes (whp)
        assertTrue(BasicRandomRoutingTable.isValidSnapshot(snapshot1));
        assertTrue(BasicRandomRoutingTable.isValidSnapshot(snapshot2));
        for (TrustGraphNodeId n: neighbors) {
            assertTrue(routes1.containsKey(n));
            assertTrue(routes2.containsKey(n));
        }

        /* test for at least one differing route 
         * (should succeed with very high probability) 
         */
        boolean mappingIsDifferent = false;
        for (TrustGraphNodeId n: neighbors) {
            // if the routing for this neighbor in 
            // snapshot1 is different than snapshot2, the 
            // test is a success.
            if (!routes1.get(n).equals(routes2.get(n))) {
                mappingIsDifferent = true;
                break;
            }
        }
        assertTrue(mappingIsDifferent);

    }


    /**
     * tests that calling RemoveNeighbors removes only the 
     * intended neighbors and preserves valid routing.
     */
    @Test 
    public void testRemoveNeighbors() throws Exception {
        RandomRoutingTable rt = new BasicRandomRoutingTable();

        // create a set of neighbors to test with
        List<TrustGraphNodeId> neighbors = createNeighbors(120);
        rt.addNeighbors(neighbors); 

        // make sure everyone is in the table to start with
        for (TrustGraphNodeId n : neighbors) { assertTrue(rt.contains(n)); }

        /* remove the 120 neighbors in random order, in increasingly large
         * chunks from 1 to 15
         */
        LinkedList<TrustGraphNodeId> shuffled = new LinkedList(neighbors);
        Collections.shuffle(shuffled);
        for (int i = 1; i <= 15; i++) {
            /* pop i things from the shuffled list into the list remove, 
             * then remove them in a bulk operation. 
             */
            LinkedList<TrustGraphNodeId> remove = new LinkedList();
            for (int j = 0; j < i; j++) { remove.push(shuffled.pop()); }
            rt.removeNeighbors(remove);

            // make sure they're gone.
            for (TrustGraphNodeId n : remove) { assertFalse(rt.contains(n)); }
        
            // make sure everything else is still there.
            for (TrustGraphNodeId n : shuffled) { assertTrue(rt.contains(n)); }

            // make sure the routing is still valid in total.
            assertTrue(BasicRandomRoutingTable.isValidSnapshot(rt.snapshot()));
        }

        // make sure everything was eventually removed
        assertTrue(rt.isEmpty());
    }

    /**
     * Tests that removeNeighbor only removes the intended neighbor 
     * from the routing and preserves a valid routing.
     */
    @Test
    public void testRemoveNeighbor() throws Exception {
        RandomRoutingTable rt = new BasicRandomRoutingTable();

        // create a set of neighbors to test with
        List<TrustGraphNodeId> neighbors = createNeighbors(100);
        rt.addNeighbors(neighbors); 

        // make sure everyone is in the table to start with
        for (TrustGraphNodeId n : neighbors) { assertTrue(rt.contains(n)); }

        /* remove the neighbors in random order. Check that the 
         * routing remains valid, the expected item has been removed 
         * and that all other items are still present. 
         */
        LinkedList<TrustGraphNodeId> shuffled = new LinkedList(neighbors);
        Collections.shuffle(shuffled);
        for (int i = 0; i < neighbors.size(); i++) {
            // select a neighbor to remove and remove it
            TrustGraphNodeId remove = shuffled.pop();
            rt.removeNeighbor(remove);

            // make sure it's gone
            assertFalse(rt.contains(remove));
        
            // make sure everything else is still there.
            for (TrustGraphNodeId n : shuffled) { assertTrue(rt.contains(n)); }

            // make sure the routing is still valid in total.
            assertTrue(BasicRandomRoutingTable.isValidSnapshot(rt.snapshot()));
        }

        // make sure everything was eventually removed
        assertTrue(rt.isEmpty());
    }

    /**
     * Tests adding and removing neighbors in random order 
     * repeatedly emptying out the table.
     */
    @Test
    public void testAddRemoveEmpty() throws Exception {
        RandomRoutingTable rt = new BasicRandomRoutingTable();

        for (int i = 0; i < 10; i++) {
            // create i neighbors
            LinkedList<TrustGraphNodeId> neighbors =
                new LinkedList<TrustGraphNodeId>(createNeighbors(i));

            // add them, then remove in order
            for (TrustGraphNodeId n : neighbors) { rt.addNeighbor(n); }
            for (TrustGraphNodeId n : neighbors) {
                rt.removeNeighbor(n);
                assertTrue(BasicRandomRoutingTable.isValidSnapshot(rt.snapshot()));
            }
            assertTrue(rt.isEmpty());


            // add them and remove them in reverse order
            for (TrustGraphNodeId n : neighbors) { rt.addNeighbor(n); }
            Collections.reverse(neighbors);
            for (TrustGraphNodeId n : neighbors) {
                rt.removeNeighbor(n);
                assertTrue(BasicRandomRoutingTable.isValidSnapshot(rt.snapshot()));
            }
            assertTrue(rt.isEmpty());


            // add them and remove them in random order
            for (TrustGraphNodeId n : neighbors) { rt.addNeighbor(n); }
            Collections.shuffle(neighbors);
            for (TrustGraphNodeId n : neighbors) {
                rt.removeNeighbor(n);
                assertTrue(BasicRandomRoutingTable.isValidSnapshot(rt.snapshot()));
            }
            assertTrue(rt.isEmpty());
        }
    }

    /**
     * Tests that getNextHop and snapshot agree.
     */
    @Test
    public void testSnapshotVsGetNextHop() throws Exception {

        RandomRoutingTable rt = new BasicRandomRoutingTable();

        // add a group of neighbors to the routing table
        List<TrustGraphNodeId> neighbors = createNeighbors(1000);
        rt.addNeighbors(neighbors); 

        // capture a snapshot of the routing
        RandomRoutingTable.Snapshot snapshot = rt.snapshot();
        Map<TrustGraphNodeId,TrustGraphNodeId> routes = snapshot.getRoutes();
        assertTrue(routes.size() == neighbors.size());
        assertTrue(BasicRandomRoutingTable.isValidSnapshot(snapshot));

        /* make sure that each mapping in the snapshot matches up with 
         * the result of getNextHop and vice versa.
         */
        for (Map.Entry<TrustGraphNodeId, TrustGraphNodeId> e : routes.entrySet()) {
            assertTrue(rt.getNextHop(e.getKey()).equals(e.getValue()));
        }
        for (TrustGraphNodeId n : neighbors) {
            assertTrue(rt.getNextHop(n).equals(routes.get(n)));
        }
    }

    /**
     * simple test of the validity of the snapshot method.
     */
    @Test
    public void testSnapshot() throws Exception {
        RandomRoutingTable rt = new BasicRandomRoutingTable();

        // create a set of neighbors and add them to the table
        List<TrustGraphNodeId> neighbors = createNeighbors(1000);
        rt.addNeighbors(neighbors); 

        RandomRoutingTable.Snapshot snapshot = rt.snapshot();
        Map<TrustGraphNodeId,TrustGraphNodeId> routes = snapshot.getRoutes();

        // make sure it meets the table's own validity criteria
        assertTrue(BasicRandomRoutingTable.isValidSnapshot(snapshot));

        /* make sure everything we added is in the snapshot 
         * and only those things are present
         */
        assertTrue(routes.size() == neighbors.size());
        for (TrustGraphNodeId n : neighbors) {
            assertTrue(routes.containsKey(n));
            assertTrue(routes.containsValue(n));
        }
    }

    /**
     * Tests that routes are created correctly when 
     * snapshots are loaded.
     *
     */
    @Test
    public void testSnapshotLoad() throws Exception {

        // create a set of neighbors and add them to the table
        List<TrustGraphNodeId> neighbors = createNeighbors(1000);

        Map<TrustGraphNodeId,TrustGraphNodeId> routesIn =
            new HashMap<TrustGraphNodeId, TrustGraphNodeId>();

        for (int i = 0; i < neighbors.size(); i++) {
            // map each neigbor to the next, circularly
            routesIn.put(neighbors.get(i), neighbors.get((i+1) % neighbors.size()));
        }
        
        // create a routing table using the constructed snapshot
        RandomRoutingTable.Snapshot snapshotIn =
                new BasicRandomRoutingTable.Snapshot(routesIn,
                        new ArrayList(routesIn.keySet()));
        RandomRoutingTable rt = new BasicRandomRoutingTable(snapshotIn);
        Map<TrustGraphNodeId,TrustGraphNodeId> snapshotOut =
            rt.snapshot().getRoutes();

        /* verify each route that we created appears in the routing
         * according to getNextHop and a snapshot.
         */
        for (Map.Entry<TrustGraphNodeId,TrustGraphNodeId> e :
                routesIn.entrySet()) {
            TrustGraphNodeId key = e.getKey();
            TrustGraphNodeId value = e.getValue();
            // getNextHop(key) == value
            assertTrue(rt.getNextHop(key).equals(value));
            // snapshot[key] == value
            assertTrue(snapshotOut.get(key).equals(value));
        }
    }

    /**
     * Tests that the output of snapshot can be used 
     * to construct a routing table and that the 
     * routing is preserved.
     *
     */
    @Test
    public void testSnapshotReload() throws Exception {
        RandomRoutingTable rt1 = new BasicRandomRoutingTable();

        // create a set of neighbors and add them to the table
        List<TrustGraphNodeId> neighbors = createNeighbors(1000);
        rt1.addNeighbors(neighbors); 
        
        RandomRoutingTable.Snapshot snapshotIn = rt1.snapshot();

        // create a routing table using the snapshot
        RandomRoutingTable rt2 = new BasicRandomRoutingTable(snapshotIn);
        RandomRoutingTable.Snapshot snapshotOut = rt2.snapshot();

        /* verify each route that we created appears in the routing
         * according to getNextHop and a snapshot.
         */
        for (Map.Entry<TrustGraphNodeId,TrustGraphNodeId> e :
                snapshotIn.getRoutes().entrySet()) {
            TrustGraphNodeId key = e.getKey();
            TrustGraphNodeId value = e.getValue();
            // getNextHop(key) == value
            assertTrue(rt2.getNextHop(key).equals(value));
            // snapshot[key] == value
            assertTrue(snapshotOut.getRoutes().get(key).equals(value));
        }
    }

    /** 
     * Test that getOrderedNeighbors reflects the set of neighbors added to 
     * a BasicRandomRoutingTable.
     */
    @Test
    public void testGetOrderedNeighborsBasic() throws Exception  {
        RandomRoutingTable rt = new BasicRandomRoutingTable();

        // create a set of neighbors and add them to the table
        List<TrustGraphNodeId> neighbors = createNeighbors(1000);
        rt.addNeighbors(neighbors); 

        // get all the neighors
        List<TrustGraphNodeId> allNeighbors = rt.getOrderedNeighbors();

        assertTrue(allNeighbors.size() == neighbors.size());
        // test that all added neighors appear in the allNeighbors list
        for (TrustGraphNodeId n : neighbors) { assertTrue(allNeighbors.contains(n)); }
    }

    /** 
     * Test that the contains method reflects the set of neighbors added
     * to a BasicRoutingTable.
     */
    @Test 
    public void testContains() throws Exception {
        RandomRoutingTable rt = new BasicRandomRoutingTable();

        // create a set of neighbors and add them to the table
        List<TrustGraphNodeId> neighbors = createNeighbors(1000);
        rt.addNeighbors(neighbors); 

        // some neighbors that won't be added
        List<TrustGraphNodeId> notAdded = createNeighbors(3);

        /* test that contains() is true for all neighbors in the 
         * list that was added.
         */
        for (TrustGraphNodeId n : neighbors) { assertTrue(rt.contains(n)); }
        
        /* test that contains() is false for some neighors that were 
         * not added
         */
        for (TrustGraphNodeId n : notAdded) { assertFalse(rt.contains(n)); }

    }

    /**
     * Tests that adding neighbors that are already contained
     * in the routing via addNeighbors does not affect the table
     * size or any existing routes.
     */
    @Test
    public void testReAddNeighbors() throws Exception {
        RandomRoutingTable rt = new BasicRandomRoutingTable();

        // create a set of neighbors and add them to the table
        List<TrustGraphNodeId> neighbors = createNeighbors(1000);
        rt.addNeighbors(neighbors); 

        // snapshot the state of the table for later comparison
        RandomRoutingTable.Snapshot snapshot1 = rt.snapshot();
        Map<TrustGraphNodeId, TrustGraphNodeId> routes1 = snapshot1.getRoutes();
        assertTrue(routes1.size() == neighbors.size());
        assertTrue(BasicRandomRoutingTable.isValidSnapshot(snapshot1));

        // re-add the neighbors in random order
        LinkedList<TrustGraphNodeId> shuffled =
            new LinkedList<TrustGraphNodeId>(neighbors);
        Collections.shuffle(shuffled);
        rt.addNeighbors(shuffled);

        /* snapshot the table after adding and compare to 
         * the previous state.  It should be completely 
         * unchanged since all neighbors were already in 
         * the table.
         */ 
        RandomRoutingTable.Snapshot snapshot2 = rt.snapshot();
        assertTrue(snapshotsAreEquivalent(snapshot1,snapshot2));
    }

    /**
     * Tests that adding neighbors that are already contained
     * in the routing via addNeighbor does not affect the table
     * size or any existing routes.
     */
    @Test
    public void testReAddNeighbor() throws Exception {
        RandomRoutingTable rt = new BasicRandomRoutingTable();

        // create a set of neighbors and add them to the table
        List<TrustGraphNodeId> neighbors = createNeighbors(100);
        rt.addNeighbors(neighbors); 

        // snapshot the state of the table for later comparison
        RandomRoutingTable.Snapshot snapshot1 = rt.snapshot();
        Map<TrustGraphNodeId, TrustGraphNodeId> routes1 = snapshot1.getRoutes();
        assertTrue(routes1.size() == neighbors.size());
        assertTrue(BasicRandomRoutingTable.isValidSnapshot(snapshot1));

        // re-add the neighbors in random order
        LinkedList<TrustGraphNodeId> shuffled =
            new LinkedList<TrustGraphNodeId>(neighbors);
        Collections.shuffle(shuffled);
        for (TrustGraphNodeId n : shuffled) {
            rt.addNeighbor(n);

            /* snapshot the table after adding and compare to 
             * the previous state.  It should be completely 
             * unchanged since all neighbors were already in 
             * the table.
             */
            RandomRoutingTable.Snapshot snapshot2 = rt.snapshot();
            assertTrue(snapshotsAreEquivalent(snapshot1,snapshot2));
        }
    }

    /**
     * Tests the clear() method
     */
    @Test
    public void testClear() throws Exception {
        RandomRoutingTable rt = new BasicRandomRoutingTable();

        // create a set of neighbors and add them to the table
        List<TrustGraphNodeId> neighbors = createNeighbors(100);
        rt.addNeighbors(neighbors); 

        // verify that they're all there
        for (TrustGraphNodeId n : neighbors) { assertTrue(rt.contains(n)); }

        // clear the table 
        rt.clear(); 

        // verify that they're all not there 
        for (TrustGraphNodeId n : neighbors) { assertFalse(rt.contains(n)); }
        assertTrue(rt.isEmpty());
    }

    /** 
     * Tests the size() method of BasicRandomRoutingTable
     */
    @Test
    public void testSize() throws Exception {
        RandomRoutingTable rt = new BasicRandomRoutingTable();
        
        // add a random number of nodes between 100 and 400
        int number = 100 + new Random().nextInt(400); 
        List<TrustGraphNodeId> neighbors = createNeighbors(number);
        assertTrue(number == neighbors.size());

        /* check that the routing table size is the same as the 
         * number of neighbors added
         */
        rt.addNeighbors(neighbors);
        assertTrue(number == rt.size());
        assertTrue(rt.size() == rt.snapshot().getRoutes().size());
    }

    /** 
     * Tests the isEmpty() method of BasicRandomRoutingTable
     */
    @Test
    public void testIsEmpty() throws Exception {
        RandomRoutingTable rt = new BasicRandomRoutingTable();

        // initially emtpy
        assertTrue(rt.isEmpty());

        // add a neighbor -> no longer empty
        TrustGraphNodeId neighbor = createNeighbors(1).get(0);
        rt.addNeighbor(neighbor);
        assertTrue(!rt.isEmpty());

        // remove the neighbor -> empty again
        rt.removeNeighbor(neighbor);
        assertTrue(rt.isEmpty());
    }

    /**
     * This is a basic test that things stay sane if multitple 
     * threads are working with the routing table.
     * 
     * A set of neighbors that should always be present in the 
     * table are added.  Two threads peform table modification 
     * operations on a separate set of neighbors while two threads
     * check that the table is valid and contains the expected entries
     * at all times.
     * 
     */
    @Test
    public void testBasicThreadedModification() throws Exception {
        final BasicRandomRoutingTable rt = 
            new BasicRandomRoutingTable();

        /* create and add a set of neighbors to the table that 
         * should always be present (they will not be specifically
         * removed by any thread)
         */
        final List<TrustGraphNodeId> alwaysIncluded =
            createNeighbors(50);
        rt.addNeighbors(alwaysIncluded);

        // these are added and removed with bulk operations
        final List<TrustGraphNodeId> bulk = createNeighbors(25);

        // these are added and removed one by one 
        final List<TrustGraphNodeId> oneByOne = createNeighbors(25);


        final AtomicReference<Boolean> done 
            = new AtomicReference<Boolean>(Boolean.FALSE);

        /* this thread repeatedly verifies that a valid 
         * snapshot can be taken and that all expected 
         * neighbors appear somewhere as a key and 
         * somwhere as a value at all times.
         */
        final AtomicReference<Boolean> snapshotResult = 
            new AtomicReference<Boolean>(Boolean.TRUE);
        Thread snapshotVerifier = new Thread(new Runnable() {
            @Override
            public void run() {
                while(done.get().booleanValue() == false) {
                    RandomRoutingTable.Snapshot snapshot = rt.snapshot();
                    Map<TrustGraphNodeId, TrustGraphNodeId> routes =
                        snapshot.getRoutes();
                    if (!BasicRandomRoutingTable.isValidSnapshot(snapshot)) {
                        snapshotResult.set(Boolean.FALSE);
                        return; // test failed
                    }
                    /* check that all expected neighbors are present in the 
                     * routing table as a key and as a value
                     */ 
                    for (TrustGraphNodeId n : alwaysIncluded) {
                        if (!(routes.containsKey(n) && routes.containsValue(n))) {
                            snapshotResult.set(Boolean.FALSE);
                            return; // test failed
                        }   
                    }
                }
            }
        });

        /* this thread repeatedly verifies that there is a valid 
         * non-null routing for all neighbors that are always included
         * in the table.
         */
        final AtomicReference<Boolean> alwaysRoutableResult = 
            new AtomicReference<Boolean>(Boolean.TRUE);
        Thread alwaysRoutableVerifier = new Thread(new Runnable() {
            @Override
            public void run() {
                while(done.get().booleanValue() == false) {
                    /* check that all expected neighbors are present in the 
                     * routing table as a key and as a value
                     */ 
                    for (TrustGraphNodeId n : alwaysIncluded) {
                        if (!(rt.contains(n) && rt.getNextHop(n) != null)) {
                            alwaysRoutableResult.set(Boolean.FALSE);
                            return; // test failed
                        }   
                    }
                }
            }
        });

        /* This thread randomly inserts and deletes items from the 
         * table one at a time, this is disruptive to existing routes.
         */
        Thread addDeleteOneByOne = new Thread(new Runnable() {
            @Override
            public void run() {

                for (int i = 0; i < 5000; i++) {
                    // shuffle, then add one by one
                    Collections.shuffle(oneByOne);
                    for (TrustGraphNodeId n : oneByOne) { rt.addNeighbor(n); }

                    // shuffle, then delete one by one
                    Collections.shuffle(oneByOne);
                    for (TrustGraphNodeId n : oneByOne) { rt.removeNeighbor(n); }
                }
            }
        });


        /* This thread repeatedly inserts and deletes a set of items 
         * by bulk operations.
         */
        Thread addDeleteBulk = new Thread(new Runnable() {
            @Override
            public void run() {

                for (int i = 0; i < 5000; i++) {
                    rt.addNeighbors(bulk);
                    rt.removeNeighbors(bulk);
                }
            }
        });

        // start all the threads
        snapshotVerifier.start();
        alwaysRoutableVerifier.start(); 
        addDeleteOneByOne.start();
        addDeleteBulk.start();

        // wait for the mutator threads to complete
        addDeleteOneByOne.join();
        addDeleteBulk.join();

        // signal the verifiers that the mutations are done
        done.set(Boolean.TRUE);

        // wait for the verifiers to complete
        snapshotVerifier.join(); 
        alwaysRoutableVerifier.join(); 

        // test the results of the verifiers
        assertTrue(snapshotResult.get().booleanValue());
        assertTrue(alwaysRoutableResult.get().booleanValue());

    }

}