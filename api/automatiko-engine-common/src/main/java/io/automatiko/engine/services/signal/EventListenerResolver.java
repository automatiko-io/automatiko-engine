package io.automatiko.engine.services.signal;

import java.util.Optional;

import io.automatiko.engine.api.runtime.process.EventListener;

public interface EventListenerResolver {
	Optional<EventListener> find(String id);
}
