package org.kaleidoscope; 


/**
 * An simple implementation of TrustGraphAdvertisement that holds
 * an arbitrary String as its payload.
 */
public class BasicTrustGraphAdvertisement implements TrustGraphAdvertisement {
    
    private final TrustGraphNodeId from;
    private final String payload;
    private final int ttl;

    public BasicTrustGraphAdvertisement(final TrustGraphNodeId from,
        final String payload, final int ttl) {
        this.from = from;
        this.payload = payload;
        this.ttl = ttl;
    }

    @Override
    public int getInboundTTL() {return ttl;}
    
    @Override
    public TrustGraphNodeId getSender() {return from;}

    @Override
    public String getPayload() { return payload; }

    public BasicTrustGraphAdvertisement copyWith(final TrustGraphNodeId sender, 
        final int ttl) {
        return new BasicTrustGraphAdvertisement(sender, getPayload(), ttl);
    }

}