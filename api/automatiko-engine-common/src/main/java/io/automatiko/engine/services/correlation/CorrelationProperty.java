package io.automatiko.engine.services.correlation;

public interface CorrelationProperty<T> {

	String getName();

	String getType();

	T getValue();
}
