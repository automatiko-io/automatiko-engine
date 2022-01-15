
package io.automatiko.engine.services.event;

import java.util.Map;

import io.automatiko.engine.services.event.impl.ProcessInstanceEventBody;

public class ProcessInstanceDataEvent extends AbstractProcessDataEvent<ProcessInstanceEventBody> {

    public ProcessInstanceDataEvent(String source, String addons, Map<String, String> metaData,
            ProcessInstanceEventBody body) {
        super("ProcessInstanceEvent", source, body);
    }
}
