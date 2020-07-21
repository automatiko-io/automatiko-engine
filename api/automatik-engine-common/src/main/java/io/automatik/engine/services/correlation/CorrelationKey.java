package io.automatik.engine.services.correlation;

import java.util.List;

public interface CorrelationKey {

	String getName();

	List<CorrelationProperty<?>> getProperties();

	String toExternalForm();
}
