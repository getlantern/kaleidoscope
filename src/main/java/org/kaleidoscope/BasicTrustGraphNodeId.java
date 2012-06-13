package org.kaleidoscope; 


/**
 * BasicTrustGraphNodeId is a simple implementation of TrustGraphNodeId
 * that immutably wraps a String representing a unique identifier for a
 * neighbor (eg a normalized jid string, login name, etc. or some other
 * computed hash value)
 */
public class BasicTrustGraphNodeId implements TrustGraphNodeId {
    private final String neighborId;

    public BasicTrustGraphNodeId(final String neighborId) {
        this.neighborId = neighborId;
    }

    public String getNeighborId() {return neighborId;}

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BasicTrustGraphNodeId) {
            BasicTrustGraphNodeId other = (BasicTrustGraphNodeId) obj;
            return neighborId.equals(other.getNeighborId());
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return neighborId.hashCode();
    }

    @Override
    public String toString() {
        return neighborId;
    }
}