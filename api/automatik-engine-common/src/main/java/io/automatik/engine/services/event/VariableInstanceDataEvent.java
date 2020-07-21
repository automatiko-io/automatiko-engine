
package io.automatik.engine.services.event;

import java.util.Map;

import io.automatik.engine.api.event.AbstractDataEvent;
import io.automatik.engine.services.event.impl.ProcessInstanceEventBody;
import io.automatik.engine.services.event.impl.VariableInstanceEventBody;

public class VariableInstanceDataEvent extends AbstractDataEvent<VariableInstanceEventBody> {

	private final String automatikVariableName;

	public VariableInstanceDataEvent(String source, String addons, Map<String, String> metaData,
			VariableInstanceEventBody body) {

		super("VariableInstanceEvent", source, body, metaData.get(ProcessInstanceEventBody.ID_META_DATA),
				metaData.get(ProcessInstanceEventBody.ROOT_ID_META_DATA),
				metaData.get(ProcessInstanceEventBody.PROCESS_ID_META_DATA),
				metaData.get(ProcessInstanceEventBody.ROOT_PROCESS_ID_META_DATA), addons);
		this.automatikVariableName = body.getVariableName();

	}

	public String getAutomatikVariableName() {
		return automatikVariableName;
	}
}
