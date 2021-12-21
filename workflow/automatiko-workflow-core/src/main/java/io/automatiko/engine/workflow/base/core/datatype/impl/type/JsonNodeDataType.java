package io.automatiko.engine.workflow.base.core.datatype.impl.type;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.automatiko.engine.api.workflow.datatype.DataType;

public class JsonNodeDataType implements DataType {

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {

    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

    }

    @Override
    public boolean verifyDataType(Object value) {
        if (value instanceof JsonNode) {
            return true;
        }
        return false;
    }

    @Override
    public String writeValue(Object value) {
        if (value instanceof JsonNode) {
            return ((JsonNode) value).toString();
        }
        return null;
    }

    @Override
    public Object readValue(String value) {
        try {
            return mapper.readTree(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getStringType() {
        return JsonNode.class.getCanonicalName();
    }

    @Override
    public Class<?> getClassType() {
        return JsonNode.class;
    }

}
