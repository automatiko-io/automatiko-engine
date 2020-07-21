
package io.automatik.engine.addons.monitoring.system.metrics.dmnhandlers;

public interface TypeHandler<T> {

	void record(String type, String endpointName, T sample);

	String getDmnType();
}
