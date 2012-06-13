package org.kaleidoscope; 

/**
 * 
 * The TrustGraphAdvertisement interface represents messages
 * that are send "over" the trust graph via random repeatable 
 * routes according to the Kaleidoscope protocol (eg relay availabilty 
 * advertisements)
 * 
 * TrustGraphAdvertisements are pushed out to neighbors on the 
 * trust graph and forwarded accorded to the random routing tables 
 * established at each node.  Each node reached by the advertisements
 * gains whatever knowledge is contained in the advertisement. 
 *
 * The message ttl determines the intended remaining number of hops
 * the message should traverse before being dropped.
 *
 * The payload of the message is expected to be implementation
 * dependant. This interface only serves to represent the parts
 * directly used to abstractly route messages.
 *
 */
public interface TrustGraphAdvertisement {

    /**
     * 
     * @return the TrustGraphNodeId that this advertisement was
     *         received from.
     */
    public TrustGraphNodeId getSender();
    
    /**
     * Gets the Time-To-Live that the sender specified for
     * this message.  This represents the remaining number
     * of hops (including the receiving node) that the message
     * should travel before being dropped.
     * 
     * @return the remaining number of hops this message should
     *         travel before being dropped.
     */
    public int getInboundTTL();

}