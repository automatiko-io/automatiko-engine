package io.automatiko.engine.addons.process.management.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.automatiko.engine.api.workflow.ExportedProcessInstance;

public class JsonExportedProcessInstance extends ExportedProcessInstance<JsonNode> {

    private static ObjectMapper mapper = new ObjectMapper();
    private List<JsonExportedProcessInstance> subInstances = new ArrayList<JsonExportedProcessInstance>();

    public JsonExportedProcessInstance() {
        super(null, null, null);
    }

    public JsonExportedProcessInstance(JsonNode header, JsonNode instance, JsonNode timers) {
        super(header, instance, timers);
    }

    public void setSubInstances(List<JsonExportedProcessInstance> subinstances) {
        this.subInstances.addAll(subinstances);
    }

    public List<JsonExportedProcessInstance> getSubInstances() {
        return subInstances;
    }

    public static JsonExportedProcessInstance of(ExportedProcessInstance<String> instance) {
        try {
            ObjectNode header = (ObjectNode) mapper.readTree(instance.getHeader());

            ObjectNode content = (ObjectNode) mapper.readTree(instance.getInstance());

            ArrayNode timers = (ArrayNode) mapper.readTree(instance.getTimers());

            return new JsonExportedProcessInstance(header, content, timers);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Map<String, String>> convertTimers() {

        return mapper.convertValue(getTimers(), List.class);
    }
}
