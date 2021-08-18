package org.jbpm.process.codegen;

import java.util.Map;
import java.util.HashMap;

public class XXXModel implements io.automatiko.engine.api.Model {
    
    private String id;
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getId() {
        return this.id;
    }
    
    public Map<String, Object> toMap() {
        
    }
    

    public void fromMap(Map<String, Object> params) {
        fromMap(null, params);
    }

    public void fromMap(String id, Map<String, Object> params) {
        
    }
}