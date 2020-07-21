package io.automatik.engine.services.correlation;

import java.util.Collections;
import java.util.List;

public class StringCorrelationKey implements CorrelationKey {

	private final String correlationKey;

	public StringCorrelationKey(String correlationKey) {
		this.correlationKey = correlationKey;
	}

	@Override
	public String getName() {
		return correlationKey;
	}

	@Override
	public List<CorrelationProperty<?>> getProperties() {
		return Collections.emptyList();
	}

	@Override
	public String toExternalForm() {
		return correlationKey;
	}

}
