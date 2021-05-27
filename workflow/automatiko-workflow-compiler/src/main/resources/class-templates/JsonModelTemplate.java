package org.jbpm.process.codegen;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.automatiko.engine.workflow.json.JsonNodeDeserializer;

@JsonSerialize(using = io.automatiko.engine.workflow.json.JsonNodeSerializer.class)
@JsonDeserialize(using = $TYPE$.ModelJsonNodeDeserializer.class)
public class XXXModel extends io.automatiko.engine.workflow.JsonModel {
    
    public XXXModel() {
        super();
    }
    
    public XXXModel(JsonNode jsonNode) {
        super(jsonNode);
    }
    
    public static class ModelJsonNodeDeserializer extends JsonNodeDeserializer {
        
        public ModelJsonNodeDeserializer() {
            super();
        }
        
        @Override
        protected Object build(JsonNode json) {
            return new $TYPE$(json);
        }
    }
}