package io.automatiko.engine.workflow.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.automatiko.engine.api.workflow.ArchivedVariable;

public class JsonArchiveVariable extends ArchivedVariable {

    private ObjectMapper mapper = new ObjectMapper();

    public JsonArchiveVariable(String name, Object value) {
        super(name, value);
    }

    @Override
    public byte[] data() {

        try {
            return mapper.writeValueAsBytes(getValue());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to serialize variable " + name, e);
        }
    }

    @Override
    public String getName() {
        return super.getName() + ".json";
    }

}
