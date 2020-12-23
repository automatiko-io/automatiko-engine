
package io.automatiko.engine.api.workflow;

import java.util.Map;

public class BaseEventDescription extends AbstractEventDescription<NamedDataType> {

	public BaseEventDescription(String event, String nodeId, String nodeName, String eventType, String nodeInstanceId,
			String processInstanceId, NamedDataType dataType, Map<String, String> properties) {
		super(event, nodeId, nodeName, eventType, nodeInstanceId, processInstanceId, dataType, properties);
	}

	public BaseEventDescription(String event, String nodeId, String nodeName, String eventType, String nodeInstanceId,
			String processInstanceId, NamedDataType dataType) {
		super(event, nodeId, nodeName, eventType, nodeInstanceId, processInstanceId, dataType);
	}

}
