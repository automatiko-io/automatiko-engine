package io.automatiko.engine.api.runtime.process;

public enum NodeInstanceState {

    Created("create"),
    Available("available"),
    Enabled("enable"),
    Disabled("disable"),
    Active("active"),
    Suspended("suspend"),
    Failed("fail"),
    Completed("complete"),
    Teminated("terminate"),
    Occur("occur"),
    Retrying("retrying");

    private String eventId;

    NodeInstanceState(String eventId) {
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }

}
