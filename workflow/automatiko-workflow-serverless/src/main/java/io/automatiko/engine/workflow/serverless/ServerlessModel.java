package io.automatiko.engine.workflow.serverless;

import com.fasterxml.jackson.databind.JsonNode;

import io.automatiko.engine.workflow.JsonModel;

public class ServerlessModel extends JsonModel {

    public ServerlessModel() {
        super();
    }

    public ServerlessModel(JsonNode json) {
        super(json);
    }

    public static ServerlessModel from(JsonNode data) {
        return new ServerlessModel(data);
    }
}
