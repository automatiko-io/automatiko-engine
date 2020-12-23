
package io.automatiko.engine.services.event.impl;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import io.automatiko.engine.api.Addons;
import io.automatiko.engine.api.event.DataEvent;
import io.automatiko.engine.api.event.EventBatch;
import io.automatiko.engine.api.event.EventManager;
import io.automatiko.engine.api.event.EventPublisher;

public class BaseEventManager implements EventManager {

	private String service;
	private Addons addons;
	private Set<EventPublisher> publishers = new LinkedHashSet<>();

	@Override
	public EventBatch newBatch() {
		return new ProcessInstanceEventBatch(service, addons);
	}

	@Override
	public void publish(EventBatch batch) {
		if (publishers.isEmpty()) {
			// don't even process the batch if there are no publishers
			return;
		}
		Collection<DataEvent<?>> events = batch.events();

		publishers.forEach(p -> p.publish(events));
	}

	@Override
	public void addPublisher(EventPublisher publisher) {
		this.publishers.add(publisher);
	}

	@Override
	public void setService(String service) {
		this.service = service;
	}

	@Override
	public void setAddons(Addons addons) {
		this.addons = addons;
	}

}
