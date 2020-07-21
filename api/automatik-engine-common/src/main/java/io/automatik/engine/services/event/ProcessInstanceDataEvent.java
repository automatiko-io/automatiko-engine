
package io.automatik.engine.services.event;

import java.util.Map;

import io.automatik.engine.services.event.impl.ProcessInstanceEventBody;

public class ProcessInstanceDataEvent extends AbstractProcessDataEvent<ProcessInstanceEventBody> {

	public ProcessInstanceDataEvent(String source, String addons, Map<String, String> metaData,
			ProcessInstanceEventBody body) {
		super("ProcessInstanceEvent", source, body, metaData.get(ProcessInstanceEventBody.ID_META_DATA),
				metaData.get(ProcessInstanceEventBody.PARENT_ID_META_DATA),
				metaData.get(ProcessInstanceEventBody.ROOT_ID_META_DATA),
				metaData.get(ProcessInstanceEventBody.PROCESS_ID_META_DATA),
				metaData.get(ProcessInstanceEventBody.ROOT_PROCESS_ID_META_DATA),
				metaData.get(ProcessInstanceEventBody.STATE_META_DATA), addons);
	}
}
