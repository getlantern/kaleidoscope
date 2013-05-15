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

    @Override
    public String getNeighborId() {return neighborId;}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((neighborId == null) ? 0 : neighborId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BasicTrustGraphNodeId other = (BasicTrustGraphNodeId) obj;
        if (neighborId == null) {
            if (other.neighborId != null)
                return false;
        } else if (!neighborId.equals(other.neighborId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return neighborId;
    }
}