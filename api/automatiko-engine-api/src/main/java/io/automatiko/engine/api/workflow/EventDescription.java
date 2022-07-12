
package io.automatiko.engine.api.workflow;

import java.util.Map;

public interface EventDescription<T> {

    String getId();

    String getEvent();

    String getNodeId();

    String getNodeName();

    String getEventType();

    String getNodeInstanceId();

    String getProcessInstanceId();

    String getReferenceId();

    T getDataType();

    Map<String, String> getProperties();

}
