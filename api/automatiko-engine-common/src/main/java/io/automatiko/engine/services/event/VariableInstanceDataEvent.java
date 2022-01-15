
package io.automatiko.engine.services.event;

import java.util.Map;

import io.automatiko.engine.api.event.AbstractDataEvent;
import io.automatiko.engine.services.event.impl.VariableInstanceEventBody;

public class VariableInstanceDataEvent extends AbstractDataEvent<VariableInstanceEventBody> {

    public VariableInstanceDataEvent(String source, String addons, Map<String, String> metaData,
            VariableInstanceEventBody body) {

        super("VariableInstanceEvent", source, body);

    }

}
