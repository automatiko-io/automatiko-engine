
package io.automatiko.engine.workflow.base.core.datatype.impl.coverter;

import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;

import io.automatiko.engine.workflow.base.core.datatype.impl.type.JsonNodeDataType;

public class JsonNodeTypeConverter implements Function<String, JsonNode> {

    private JsonNodeDataType jsonNodeDataType = new JsonNodeDataType();

    @Override
    public JsonNode apply(String t) {
        return (JsonNode) jsonNodeDataType.readValue(t);
    }

}
