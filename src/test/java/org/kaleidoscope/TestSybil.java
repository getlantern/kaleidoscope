package org.kaleidoscope; 

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * This class tests some basic "sybil" resistence
 * properties of the kaleidoscope implementation.
 * 
 */
public class TestSybil {

    public TestSybil() {}
    
    /** 
     * Basic test that an adversary's ability to 
     * advertise is limited by the number of 
     * links to the "real" trust graph and cannot
     * be increased by creating "sybil"
     * nodes only connected to adversarial nodes.
     * 
     */
    @Test
    public void testSybilAdvertisingLimitBasic() {
        
    }

}