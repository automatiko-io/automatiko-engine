
package io.automatik.engine.workflow.base.core.event;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import io.automatik.engine.api.runtime.process.DataTransformer;
import io.automatik.engine.workflow.base.core.impl.DataTransformerRegistry;
import io.automatik.engine.workflow.process.core.node.Transformation;

public class EventTransformerImpl implements EventTransformer, Serializable {

	private static final long serialVersionUID = 5861307291725051774L;

	private Transformation transformation;
	private String name;

	public EventTransformerImpl(Transformation transformation) {
		if (transformation != null) {
			this.transformation = transformation;
			this.name = transformation.getSource();

			if (this.name == null) {
				this.name = "event";
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public Object transformEvent(Object event) {
		if (event == null || transformation == null) {
			return event;
		}
		DataTransformer transformer = DataTransformerRegistry.get().find(transformation.getLanguage());
		if (transformer != null) {

			return transformer.transform(transformation.getCompiledExpression(),
					Optional.ofNullable(event).filter(Map.class::isInstance).map(Map.class::cast)
							.orElseGet(() -> Collections.singletonMap(name, event)));
		}
		return event;
	}

}
