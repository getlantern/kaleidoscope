package org.kaleidoscope; 


/**
 * An simple implementation of TrustGraphAdvertisement that holds
 * an arbitrary String as its payload.
 */
public class BasicTrustGraphAdvertisement implements TrustGraphAdvertisement {
    
    protected final TrustGraphNodeId from;
    protected final String payload;
    protected final int ttl;

    public BasicTrustGraphAdvertisement(final TrustGraphNodeId from,
                              final String payload,
                              final int ttl) {
        this.from = from;
        this.payload = payload;
        this.ttl = ttl;
    }

    @Override
    public int getInboundTTL() {return ttl;}
    
    @Override
    public TrustGraphNodeId getSender() {return from;}

    public String getPayload() { return payload; }

    public BasicTrustGraphAdvertisement copyWith(TrustGraphNodeId sender, int ttl) {
        return new BasicTrustGraphAdvertisement(sender, getPayload(), ttl);
    }

}