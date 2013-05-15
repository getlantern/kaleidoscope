package org.kaleidoscope; 

import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Helper functions for testing
 *
 */
public class Helpers {
    
    private static final AtomicInteger neighborSeq = new AtomicInteger(0);
    
    /**
     * create a collection of TrustGraphNeighbors of the size given.
     * 
     * @param number the number of neighbors to create
     */ 
    public static List<TrustGraphNodeId> createNeighbors(int number) {

        final int startId = neighborSeq.getAndAdd(number);

        List<TrustGraphNodeId> neighbors = new ArrayList<TrustGraphNodeId>(number);

        for (int i = 0; i < number; i++) {
            neighbors.add(new BasicTrustGraphNodeId("Neighbor #" + (i + startId)));
        }

        return neighbors;
    }
    
    public static boolean snapshotsAreEquivalent(RandomRoutingTable.Snapshot a, 
                                                 RandomRoutingTable.Snapshot b) {
        final Map<TrustGraphNodeId,TrustGraphNodeId> routesA = a.getRoutes(); 
        final Map<TrustGraphNodeId,TrustGraphNodeId> routesB = b.getRoutes(); 
        
        if (routesA.size() != routesB.size()) {
            return false;
        }
        for (Map.Entry<TrustGraphNodeId,TrustGraphNodeId> e : routesA.entrySet()) {
            final TrustGraphNodeId key = e.getKey();
            final TrustGraphNodeId value = e.getValue();
            final TrustGraphNodeId route = routesB.get(key);
            if (route == null) {
                System.err.println("route "+key+" not in: "+ routesB);
                //return false;
            }
            if (!route.equals(value)) {
                return false;
            }
        }
         
        final List<TrustGraphNodeId> orderA = a.getOrderedNeighbors(); 
        final List<TrustGraphNodeId> orderB = b.getOrderedNeighbors();
        if (orderA.size() != orderB.size()) {
            return false;
        }
        for (int i = 0; i < orderA.size(); i++) {
            if (!orderA.get(i).equals(orderB.get(i))) {
                return false;
            }
        }
         
        return true;
    }

}