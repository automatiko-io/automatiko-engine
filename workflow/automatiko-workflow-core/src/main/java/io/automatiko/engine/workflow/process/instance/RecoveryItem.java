package io.automatiko.engine.workflow.process.instance;

import java.util.List;

public class RecoveryItem {

    private String transactionId;

    private String nodeDefinitionId;

    private String instanceId;

    private String timerId;

    private List<String> stateTimerIds;

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getNodeDefinitionId() {
        return nodeDefinitionId;
    }

    public void setNodeDefinitionId(String nodeDefinitionId) {
        this.nodeDefinitionId = nodeDefinitionId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getTimerId() {
        return timerId;
    }

    public void setTimerId(String timerId) {
        this.timerId = timerId;
    }

    public List<String> getStateTimerIds() {
        return stateTimerIds;
    }

    public void setStateTimerIds(List<String> stateTimerIds) {
        this.stateTimerIds = stateTimerIds;
    }

    @Override
    public String toString() {
        return "RecoveryItem [transactionId=" + transactionId + ", nodeDefinitionId=" + nodeDefinitionId + ", instanceId="
                + instanceId + ", timerId=" + timerId + ", stateTimerIds=" + stateTimerIds + "]";
    }

}
