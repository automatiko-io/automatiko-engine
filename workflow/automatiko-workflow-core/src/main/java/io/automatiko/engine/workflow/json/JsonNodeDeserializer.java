package io.automatiko.engine.workflow.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class JsonNodeDeserializer extends StdDeserializer<Object> {
    private static final long serialVersionUID = 1729289640418476544L;

    public JsonNodeDeserializer() {
        this(null);
    }

    public JsonNodeDeserializer(Class<ObjectNode> t) {
        super(t);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {

        try {

            JsonNode node = p.getCodec().readTree(p);
            return build(node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract Object build(JsonNode json);
}
