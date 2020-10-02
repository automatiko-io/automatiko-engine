package io.automatik.engine.workflow.bpmn2;

import java.util.Date;

public class Subprocess {

    private final String processInstanceId;
    private final String processId;
    private final String processName;
    private final String processInstanceName;
    private final Date created;

    public Subprocess(String processInstanceId, String processInstanceName, String processId, String processName,
            Date created) {
        this.processInstanceId = processInstanceId;
        this.processInstanceName = processInstanceName;
        this.processId = processId;
        this.processName = processName;
        this.created = created;
    }

    public String processInstanceId() {
        return processInstanceId;
    }

    public String processId() {
        return processId;
    }

    public String processName() {
        return processName;
    }

    public String processInstanceName() {
        return processInstanceName;
    }

    public Date created() {
        return created;
    }

    public String toString() {
        return "Subprocess [processInstanceId=" + processInstanceId + ", processId=" + processId + ", processName="
                + processName + "]";
    }

}
