package org.kaleidoscope;

/**
 *
 * The TrustGraphNodeId interface represents a neighboring node in the 
 * underlying trust graph, eg a friend or buddy. A neighbor in the trust graph 
 * is a peer node that is trusted by a particular Kaleidoscope node and with 
 * whom messages are directly exchanged. Neighbor relationships should be 
 * symmetric.
 *
 * Implementations should insure that the hashCode/equals methods of a
 * TrustGraphNeigbor correspond to the identity of individual unique 
 * neighbors in the underlying trust graph.
 */
public interface TrustGraphNodeId {

    String getNeighborId();
}