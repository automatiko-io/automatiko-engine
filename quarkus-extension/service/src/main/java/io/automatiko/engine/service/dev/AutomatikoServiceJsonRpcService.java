package io.automatiko.engine.service.dev;

import java.util.Collection;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class AutomatikoServiceJsonRpcService {

    public JsonArray getInfo() {
        JsonArray result = new JsonArray();

        Collection<WorkflowInfo> workflows = new WorkflowInfoSupplier().get();
        for (WorkflowInfo workflow : workflows) {
            JsonObject workflowAsJson = toJson(workflow);
            result.add(workflowAsJson);
        }
        return result;
    }

    private JsonObject toJson(WorkflowInfo workflow) {
        JsonObject json = new JsonObject();
        json.put("id", workflow.getId());
        json.put("name", workflow.getName());
        json.put("description", workflow.getDescription());
        json.put("publicProcess", workflow.isPublicProcess());
        return json;
    }
}
