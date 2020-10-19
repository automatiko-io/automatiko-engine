
package io.automatik.engine.workflow.process.instance;

import java.util.List;

import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.api.runtime.process.EventListener;
import io.automatik.engine.workflow.base.instance.ProcessInstance;

public interface WorkflowProcessInstance
        extends ProcessInstance, io.automatik.engine.api.runtime.process.WorkflowProcessInstance {

    void addEventListener(String type, EventListener eventListener, boolean external);

    void removeEventListener(String type, EventListener eventListener, boolean external);

    boolean hasNodeInstanceActive(String uniqueNodeId);

    List<String> getCompletedNodeIds();

    boolean multipleInstancesOfNodeAllowed(Node node);

}
