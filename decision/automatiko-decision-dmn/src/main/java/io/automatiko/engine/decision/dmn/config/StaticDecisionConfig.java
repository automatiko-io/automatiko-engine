package io.automatiko.engine.decision.dmn.config;

import org.kie.dmn.api.core.event.DMNRuntimeEventListener;

import io.automatiko.engine.api.decision.DecisionConfig;
import io.automatiko.engine.api.decision.DecisionEventListenerConfig;

public class StaticDecisionConfig implements DecisionConfig {

	private final DecisionEventListenerConfig<DMNRuntimeEventListener> decisionEventListenerConfig;

	public StaticDecisionConfig(DecisionEventListenerConfig<DMNRuntimeEventListener> decisionEventListenerConfig) {
		this.decisionEventListenerConfig = decisionEventListenerConfig;
	}

	@Override
	public DecisionEventListenerConfig<DMNRuntimeEventListener> decisionEventListeners() {
		return decisionEventListenerConfig;
	}

}
