package io.automatiko.engine.workflow;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.workflow.InstanceMetadata;

public abstract class JsonModel implements Model {

    private static final long serialVersionUID = -801605831545725344L;

    private static final List<String> RESTRICTED_VARIABLES = List.of("$CONST");

    protected static ObjectMapper MAPPER = new ObjectMapper();

    protected ObjectNode data;

    public JsonModel() {
        this.data = new ObjectNode(JsonNodeFactory.withExactBigDecimals(false));
    }

    public JsonModel(JsonNode json) {
        this.data = new ObjectNode(JsonNodeFactory.withExactBigDecimals(false));
        Iterator<Entry<String, JsonNode>> it = json.fields();

        while (it.hasNext()) {
            Entry<String, JsonNode> entry = (Entry<String, JsonNode>) it.next();
            data.set(entry.getKey(), entry.getValue());
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
        Iterator<Entry<String, JsonNode>> it = data.fields();

        while (it.hasNext()) {
            Entry<String, JsonNode> entry = (Entry<String, JsonNode>) it.next();
            if (RESTRICTED_VARIABLES.contains(entry.getKey())) {
                continue;
            }
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
            data.set(entry.getKey(), (JsonNode) entry.getValue());
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
            data.set(entry.getKey(), entry.getValue());
        }
    }

    public JsonNode getWorkflowdata() {
        return data;
    }

    public void setMetadata(InstanceMetadata metadata) {
        data.set("metadata", MAPPER.valueToTree(metadata));
    }

    public void addWorkflowdata(String name, JsonNode workflowData) {
        if ("workflowdata".equals(name)) {
            setWorkflowdata(workflowData);
        } else {

            data.set(name, workflowData);
        }
    }

    public JsonNode getWorkflowdata(String name) {
        return data.get(name);
    }
}
