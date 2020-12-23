
package io.automatiko.engine.services.event;

import java.util.Map;

import io.automatiko.engine.api.event.AbstractDataEvent;
import io.automatiko.engine.services.event.impl.ProcessInstanceEventBody;
import io.automatiko.engine.services.event.impl.UserTaskInstanceEventBody;

public class UserTaskInstanceDataEvent extends AbstractDataEvent<UserTaskInstanceEventBody> {

	private final String automatikUserTaskinstanceId;
	private final String automatikUserTaskinstanceState;

	public UserTaskInstanceDataEvent(String source, String addons, Map<String, String> metaData,
			UserTaskInstanceEventBody body) {

		super("UserTaskInstanceEvent", source, body, metaData.get(ProcessInstanceEventBody.ID_META_DATA),
				metaData.get(ProcessInstanceEventBody.ROOT_ID_META_DATA),
				metaData.get(ProcessInstanceEventBody.PROCESS_ID_META_DATA),
				metaData.get(ProcessInstanceEventBody.ROOT_PROCESS_ID_META_DATA), addons);

		this.automatikUserTaskinstanceState = metaData.get(UserTaskInstanceEventBody.UT_STATE_META_DATA);
		this.automatikUserTaskinstanceId = metaData.get(UserTaskInstanceEventBody.UT_ID_META_DATA);
	}

	public String getAutomatikUserTaskinstanceId() {
		return automatikUserTaskinstanceId;
	}

	public String getAutomatikUserTaskinstanceState() {
		return automatikUserTaskinstanceState;
	}
}
