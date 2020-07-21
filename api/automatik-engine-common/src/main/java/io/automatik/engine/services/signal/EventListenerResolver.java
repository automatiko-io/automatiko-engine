package io.automatik.engine.services.signal;

import java.util.Optional;

import io.automatik.engine.api.runtime.process.EventListener;

public interface EventListenerResolver {
	Optional<EventListener> find(String id);
}
