
package io.automatiko.engine.services.event;

import java.util.Map;

import io.automatiko.engine.api.event.AbstractDataEvent;
import io.automatiko.engine.services.event.impl.UserTaskInstanceEventBody;

public class UserTaskInstanceDataEvent extends AbstractDataEvent<UserTaskInstanceEventBody> {

    public UserTaskInstanceDataEvent(String source, String addons, Map<String, String> metaData,
            UserTaskInstanceEventBody body) {

        super("UserTaskInstanceEvent", source, body);
    }
}
