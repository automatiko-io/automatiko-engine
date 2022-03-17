
package io.automatiko.engine.workflow.process.instance;

import java.util.List;

import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.runtime.process.EventListener;
import io.automatiko.engine.workflow.base.instance.ProcessInstance;

public interface WorkflowProcessInstance
        extends ProcessInstance, io.automatiko.engine.api.runtime.process.WorkflowProcessInstance {

    void addEventListener(String type, EventListener eventListener, boolean external);

    void removeEventListener(String type, EventListener eventListener, boolean external);

    boolean hasNodeInstanceActive(String uniqueNodeId);

    List<String> getCompletedNodeIds();

    boolean multipleInstancesOfNodeAllowed(Node node);

    RecoveryItem getRecoveryItem(String nodeId);

    void setRecoveryItem(RecoveryItem item);

}
