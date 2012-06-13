package org.kaleidoscope;

import java.io.IOException;

/**
 *
 * The RoutingPersistenceMechanism interface represents a method for storing
 * routing table and peer information between runs of the software.  Route 
 * information as well as certain peer ordering information must be stable 
 * between runs for proper functioning of the Kaleidoscope advertisement 
 * algorithm.
 *
 */
public interface RoutingPersistenceMechanism {
    
    /**
     * Store routing table information via this 
     * mechanism.
     *
     * @param snapshot a RandomRoutingTable state
     */
    public void store(RandomRoutingTable.Snapshot snapshot) throws IOException;
    
    /**
     * Load routing table snapshot from this mechansim.
     * @return a RandomRoutingTable.Snapshot suitable for constructing 
     *         a table.
     */
    public RandomRoutingTable.Snapshot load() throws IOException;
    
}