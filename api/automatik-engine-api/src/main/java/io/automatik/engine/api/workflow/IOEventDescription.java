
package io.automatik.engine.api.workflow;

import java.util.Map;

public class IOEventDescription extends AbstractEventDescription<GroupedNamedDataType> {

	public IOEventDescription(String event, String nodeId, String nodeName, String eventType, String nodeInstanceId,
			String processInstanceId, GroupedNamedDataType dataType, Map<String, String> properties) {
		super(event, nodeId, nodeName, eventType, nodeInstanceId, processInstanceId, dataType, properties);
	}

	public IOEventDescription(String event, String nodeId, String nodeName, String eventType, String nodeInstanceId,
			String processInstanceId, GroupedNamedDataType dataType) {
		super(event, nodeId, nodeName, eventType, nodeInstanceId, processInstanceId, dataType);
	}

}
