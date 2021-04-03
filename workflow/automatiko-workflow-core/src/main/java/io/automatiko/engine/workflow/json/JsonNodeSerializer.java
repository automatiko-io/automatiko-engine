package io.automatiko.engine.workflow.json;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.automatiko.engine.workflow.JsonModel;

public class JsonNodeSerializer extends StdSerializer<JsonModel> {

    private static final long serialVersionUID = 1729289640418476544L;

    public JsonNodeSerializer() {
        this(null);
    }

    public JsonNodeSerializer(Class<JsonModel> t) {
        super(t);
    }

    @Override
    public void serialize(
            JsonModel value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        Iterator<Entry<String, JsonNode>> it = value.fields();
        jgen.writeStartObject();
        while (it.hasNext()) {
            Entry<String, JsonNode> entry = (Entry<String, JsonNode>) it.next();
            jgen.writeObjectField(entry.getKey(), entry.getValue());
        }
        jgen.writeStringField("id", value.getId());
        jgen.writeEndObject();
    }
}
