package io.automatiko.engine.services.signal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.runtime.process.EventListener;
import io.automatiko.engine.api.workflow.signal.SignalManagerHub;
import io.automatiko.engine.services.signal.EventListenerResolver;
import io.automatiko.engine.services.signal.LightSignalManager;

public class LightSignalManagerTest {

	@Test
	public void testAddNewListener() {
		LightSignalManager sm = new LightSignalManager(mock(EventListenerResolver.class), mock(SignalManagerHub.class));
		EventListener listen = mock(EventListener.class);
		String type = "completion";

		sm.addEventListener(type, listen);
		assertThat(sm.getListeners()).hasEntrySatisfying(type, s -> assertThat(s).hasSize(1));

		sm.addEventListener(type, listen);
		assertThat(sm.getListeners()).hasEntrySatisfying(type, s -> assertThat(s).hasSize(1));
	}
}
