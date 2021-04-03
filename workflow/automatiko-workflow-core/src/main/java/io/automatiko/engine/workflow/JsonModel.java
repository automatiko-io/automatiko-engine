package io.automatiko.engine.workflow;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.automatiko.engine.api.Model;

public abstract class JsonModel extends ObjectNode implements Model {

    private static final long serialVersionUID = -801605831545725344L;

    protected static ObjectMapper MAPPER = new ObjectMapper();

    public JsonModel() {
        super(JsonNodeFactory.withExactBigDecimals(false));
    }

    public JsonModel(JsonNode json) {
        super(JsonNodeFactory.withExactBigDecimals(false));
        Iterator<Entry<String, JsonNode>> it = json.fields();

        while (it.hasNext()) {
            Entry<String, JsonNode> entry = (Entry<String, JsonNode>) it.next();
            set(entry.getKey(), entry.getValue());
        }
    }

    private String id;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> copy = new LinkedHashMap<>();
        Iterator<Entry<String, JsonNode>> it = fields();

        while (it.hasNext()) {
            Entry<String, JsonNode> entry = (Entry<String, JsonNode>) it.next();
            copy.put(entry.getKey(), entry.getValue());
        }

        return copy;
    }

    @Override
    public void fromMap(Map<String, Object> params) {
        if (params == null) {
            return;
        }

        for (Entry<String, Object> entry : params.entrySet()) {
            set(entry.getKey(), (JsonNode) entry.getValue());
        }

    }

    public void fromMap(String id, Map<String, Object> params) {
        this.id = id;
        fromMap(params);
    }

    public void setWorkflowdata(JsonNode workflowData) {
        Iterator<Entry<String, JsonNode>> it = workflowData.fields();

        while (it.hasNext()) {
            Entry<String, JsonNode> entry = (Entry<String, JsonNode>) it.next();
            set(entry.getKey(), entry.getValue());
        }
    }

    public JsonNode getWorkflowdata() {
        return this;
    }
}
