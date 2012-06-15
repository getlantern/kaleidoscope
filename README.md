===========================================
Kaleidoscope Limited Advertisement Protocol
===========================================

This library is a java implementation of the network neutral portions
of the Kaleidoscope limited advertisement protocol as outlined in 
section 4.1 of:

Unblocking the Internet: Social networks foil censors
Yair Sovran, Jinyang Li, Lakshminarayanan Submaranian
NYU Computer Science Technical Report TR2008-918, Sep 2009 
http://kscope.news.cs.nyu.edu/pub/TR-2008-918.pdf

Please refer to the above document for a more complete description of the 
technical details, motivation and threat model of the protocol.  Information 
is included in this package for completeness, but should not be considered
authoritative.

Status
======

This library is currently under heavy development and should be considered alpha quality.

Install
=======

This library is currently only available as a git repository, but you can install it thusly:
    
    git clone git://github.com/getlantern/kaleidoscope.git
    cd kaleidoscope
    mvn install
    
    
Library Quickstart
==================

The library is organized from the viewpoint of a particular node in the
network and the actions it must take to participate. A given node has 
a single instance of "TrustGraphNode" which represents "this node".

Other neighboring trusted nodes are referenced abstractly by 
"TrustGraphNodeIds" which may take on particular meanings on a per
network basis, for example usernames, jabber ids etc.

Trusted neighbors are added to the "RoutingTable" of the TrustGraphNode
which determines how messages flow through the node and where to send 
messages originating at the node.

If a node has something to advertise to other nodes, a TrustGraphAdvertisement 
is send via "advertiseSelf".  If a message is received from a peer, it should be
passed to "handleMessage" for forwarding or any other local processing necessary.

Library Utilization
~~~~~~~~~~~~~~~~~~~

This library implements the network neutral portions of the kaleidoscope
advertisement protocol.  To implement the protocol on a specific 
trust network, a few pieces must be implemented:

  * communication mechanism between neighbors
  * enumeration of trusted neighbors
  * periodic calls to advertise local information
  * specific form of messages delivered (syntax and semantics)
  * requesting save at exit, and load during startup
  * any adjustments to default kaleidoscope network parameters


Sending and Receiving Messages 
------------------------------

To deliver messages, a concrete subclass of TrustGraphNode that implements "sendAdvertisement" must be implemented.  This method's job is to deliver a 
message from the node to some neighbor, for example a chat message, a packet over a socket or some other specific message delivery mechanism.  A simple "BasicTrustGraphAdvertisement" class that holds a String payload is provided for ease, but a tailored subclass of TrustGraphAdvertisement for the network may be easier to use depending on the circumstances.

A node is also responsible for receiving messages from the network and transforming them into some subclass of TrustGraphAdvertisement.  At a minimum, these messages should be passed to to the "handleAdvertisement" method of the TrustGraphNode for forwarding and other processing.  It is expected that the messages will be locally meaningful in some way that is not specified by the library.

Adding Trusted Neighbors
------------------------

To function correctly, the adjacent nodes in the trust graph ("buddies", "friends", etc)
must be provided to the library. These "neighbors" are represented by some subclass of "TrustGraphNodeId".  Likely, the BasicTrustGraphNodeId, which holds a String, will suffice.

Each TrustGraphNode has a "RandomRoutingTable" which holds the list of neighbors and
determines routing information for messages send to and received from neighbors.  The
"BasicRandomRoutingTable" implements the default Kaleidoscope routing algorithm and
uses default parameters.

Use the addNeighbor(s) function of the RandomRoutingTable to specify the trusted neighbors.  There are related functions for removal and maintenance of neighbors
etc.

Saving and Loading
------------------

In order to function properly, routing information must be persistent across runs
of the software.  The routing information should be saved at program exit and loaded
at startup. 

To facilitate this, each RandomRoutingTable is capable of producing a Snapshot that
may be used to later construct an identical routing table.  A basic json file 
based persistence mechanism for routing tables using BasicTrustGraphNodeIds is
provided in "JsonFileRoutingPersistence".


Basic Kaleidoscope Network Parameters
-------------------------------------

The basic implementation uses a set of default kaleidoscope parameters.
These are embodied by the following methods on TrustGraphNode: 

  * getIdealReach aka "r" defaults to 100
  * getMaxRouteLength aka "w_max", defaults to 20
  * getMinRouteLength aka "w_min", defaults to 7
  
These may be overridden in a subclass of TrustGraphNode to suit a particular 
circumstance. Please see the source paper for further explanation of these parameters.


License
=======

This library is licensed under an MIT style license. 
Complete license details can be found in the included LICENSE document.

