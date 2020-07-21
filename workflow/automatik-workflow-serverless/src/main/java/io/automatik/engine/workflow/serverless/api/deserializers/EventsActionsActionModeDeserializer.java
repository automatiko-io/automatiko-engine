
package io.automatik.engine.workflow.serverless.api.deserializers;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import io.automatik.engine.workflow.serverless.api.WorkflowPropertySource;
import io.automatik.engine.workflow.serverless.api.events.EventsActions;

public class EventsActionsActionModeDeserializer extends StdDeserializer<EventsActions.ActionMode> {

	private static final long serialVersionUID = 510l;
	private static Logger logger = LoggerFactory.getLogger(EventsActionsActionModeDeserializer.class);

	private WorkflowPropertySource context;

	public EventsActionsActionModeDeserializer() {
		this(EventsActions.ActionMode.class);
	}

	public EventsActionsActionModeDeserializer(WorkflowPropertySource context) {
		this(EventsActions.ActionMode.class);
		this.context = context;
	}

	public EventsActionsActionModeDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public EventsActions.ActionMode deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

		String value = jp.getText();
		if (context != null) {
			try {
				String result = context.getPropertySource().getProperty(value);

				if (result != null) {
					return EventsActions.ActionMode.fromValue(result);
				} else {
					return EventsActions.ActionMode.fromValue(jp.getText());
				}
			} catch (Exception e) {
				logger.info("Exception trying to evaluate property: {}", e.getMessage());
				return EventsActions.ActionMode.fromValue(jp.getText());
			}
		} else {
			return EventsActions.ActionMode.fromValue(jp.getText());
		}
	}
}