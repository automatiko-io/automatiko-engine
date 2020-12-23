
package io.automatiko.engine.api.workflow;

public interface Signal<T> {

	String channel();

	T payload();

	String referenceId();
}
