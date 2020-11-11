
package io.automatik.engine.addons.process.management;

import java.util.List;

public interface ProcessInstanceManagement<T> {

    T getInstanceInError(String processId, String processInstanceId, String user, List<String> groups);

    T getWorkItemsInProcessInstance(String processId, String processInstanceId, String user, List<String> groups);

    T retriggerInstanceInError(String processId, String processInstanceId, String user, List<String> groups);

    T skipInstanceInError(String processId, String processInstanceId, String user, List<String> groups);

    T triggerNodeInstanceId(String processId, String processInstanceId, String nodeId, String user, List<String> groups);

    T retriggerNodeInstanceId(String processId, String processInstanceId, String nodeInstanceId, String user,
            List<String> groups);

    T cancelNodeInstanceId(String processId, String processInstanceId, String nodeInstanceId, String user, List<String> groups);

    T cancelProcessInstanceId(String processId, String processInstanceId, String user, List<String> groups);
}
