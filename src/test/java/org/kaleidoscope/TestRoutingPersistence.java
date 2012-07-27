package org.kaleidoscope; 

import static org.junit.Assert.assertTrue;
import static org.kaleidoscope.Helpers.createNeighbors;
import static org.kaleidoscope.Helpers.snapshotsAreEquivalent;

import java.io.File;
import java.util.Collection;

import org.junit.Test;

/**
 * 
 *
 */
public class TestRoutingPersistence {


    public TestRoutingPersistence() {}
    
    protected void doBasicPersistenceTest(RoutingPersistenceMechanism r)
        throws Exception {
        RandomRoutingTable rt = new BasicRandomRoutingTable();

        /* create a group of neighbors and add them to the 
         * routing table.
         */
        Collection<TrustGraphNodeId> originalNeighbors = createNeighbors(500);
        rt.addNeighbors(originalNeighbors);
        
        RandomRoutingTable.Snapshot snapshot = rt.snapshot();
        
        // save the snapshot
        r.store(snapshot);
        
        // load a snapshot from the storage mechanism
        RandomRoutingTable.Snapshot snapshot2 = r.load();
        
        // it should be legitimate to pass to the constructor of the 
        // routing table that created it... (this will throw exceptions 
        // if invalid)
        new BasicRandomRoutingTable(snapshot2);
        
        // it should be identical to the original snapshot
        assertTrue(snapshotsAreEquivalent(snapshot, snapshot2));
    }
    
    @Test
    public void testJsonPersistence() throws Exception {
        File tf = File.createTempFile("kltjp","json");
        tf.deleteOnExit();
        RoutingPersistenceMechanism jp = 
            new JsonFileRoutingPersistence(tf);
        doBasicPersistenceTest(jp);
    }
}