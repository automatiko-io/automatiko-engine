package io.automatiko.engine.services.signal;

public interface Signalable {

	public void signalEvent(String type, Object event);
}
