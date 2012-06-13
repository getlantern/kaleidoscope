package org.kaleidoscope; 

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List; 
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig.Feature;

/** 
 * JsonFileRoutingPersistence is a basic RoutingPersistenceMechanism 
 * that uses a file and json serialization.  It expects and produces 
 * snapshots using BasicTrustGraphNodeId.
 */
class JsonFileRoutingPersistence implements RoutingPersistenceMechanism {
    
    private final File file;
    
    public JsonFileRoutingPersistence(final File file) {
        this.file = file;
    }
        
    /**
     * Store routing table information via this 
     * mechanism.
     *
     * @param snapshot a RandomRoutingTable state
     */
    @Override
    public void store(final RandomRoutingTable.Snapshot snapshot) throws IOException {
        try {
            saveString(serialize(snapshot));
        } catch (final JsonGenerationException e) {
            throw new IOException("Error serializing routing snapshot", e);
        } catch (final JsonMappingException e) {
            throw new IOException("Error serializing routing snapshot", e);
        }
    }
    
    /**
     * Load routing table snapshot from this mechansim.
     * @return a RandomRoutingTable.Snapshot suitable for constructing 
     *         a table.
     */
    @Override
    public RandomRoutingTable.Snapshot load() throws IOException {
        return deserialize(loadString());
    }
    
    protected String serialize(final RandomRoutingTable.Snapshot snapshot) 
        throws IOException, JsonGenerationException, JsonMappingException {
        final SnapshotBean bean = new SnapshotBean(snapshot);
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(Feature.INDENT_OUTPUT, true);
        return mapper.writeValueAsString(bean);
    }
    
    protected RandomRoutingTable.Snapshot deserialize(final String json)
        throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, SnapshotBean.class);
    }
    
    protected String loadString() throws IOException {
        return IOUtils.toString(new FileInputStream(file));
    }
    
    protected void saveString(final String str)
        throws IOException {
        IOUtils.write(str, new FileOutputStream(file, false));
    }

    /** 
     * A RandomRoutingTable.Snapshot that is a bean suitable for 
     * use with Jackson json serialization.
     */
    public static class SnapshotBean implements RandomRoutingTable.Snapshot {
    
        private final Map<TrustGraphNodeId,TrustGraphNodeId> routes;
        private final List<TrustGraphNodeId> neighbors;
    
        public SnapshotBean(Map<TrustGraphNodeId,TrustGraphNodeId> routes,
                            List<TrustGraphNodeId> neighbors) {
            this.routes = new HashMap<TrustGraphNodeId, TrustGraphNodeId>(routes);
            this.neighbors = new ArrayList<TrustGraphNodeId>(neighbors);            
        }
    
        public SnapshotBean(RandomRoutingTable.Snapshot snapshot) {
            this(snapshot.getRoutes(), snapshot.getOrderedNeighbors());
        }
    
        public SnapshotBean() {
            this.routes = new HashMap<TrustGraphNodeId, TrustGraphNodeId>();
            this.neighbors = new ArrayList<TrustGraphNodeId>();            
        }
    
        @JsonIgnore
        @Override
        public Map<TrustGraphNodeId,TrustGraphNodeId> getRoutes() {return routes;}
    
        @JsonIgnore
        @Override
        public List<TrustGraphNodeId> getOrderedNeighbors() {return neighbors;}
    
        public Map<String,String> getRoutesMap() {
            final Map<String,String> strRoutes = new HashMap<String,String>(); 
            for (Map.Entry<TrustGraphNodeId,TrustGraphNodeId> mapping : routes.entrySet()) {
                strRoutes.put(idToString(mapping.getKey()),
                              idToString(mapping.getValue()));
            }
            return strRoutes;
        }
    
        public void setRoutesMap(final Map<String,String> r) {
            routes.clear();
            for (Map.Entry<String,String> mapping : r.entrySet()) {
                routes.put(stringToId(mapping.getKey()),
                           stringToId(mapping.getValue()));
            }
        }
    
        public List<String> getOrderedNeighborsList() {
            final List<String> strNeighbors = new ArrayList<String>(neighbors.size());
            for (TrustGraphNodeId n : neighbors) {
                strNeighbors.add(idToString(n));
            }
            return strNeighbors;
        }
    
        public void setOrderedNeighborsList(final List<String> strNeighbors) {
            neighbors.clear(); 
            for (String n : strNeighbors) {
                neighbors.add(stringToId(n));
            }
        }

        protected String idToString(final TrustGraphNodeId nodeId) {
            return nodeId.toString();
        }

        protected TrustGraphNodeId stringToId(final String nodeId) {
            return new BasicTrustGraphNodeId(nodeId);
        }
    }
}