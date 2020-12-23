
package io.automatiko.engine.api;

import io.automatiko.engine.api.decision.DecisionConfig;
import io.automatiko.engine.api.workflow.ProcessConfig;

public class StaticConfig implements Config {

	private final Addons addons;
	private final ProcessConfig processConfig;
	private final DecisionConfig decisionConfig;

	public StaticConfig(Addons addons, ProcessConfig processConfig, DecisionConfig decisionConfig) {
		this.addons = addons;
		this.processConfig = processConfig;
		this.decisionConfig = decisionConfig;
	}

	public StaticConfig(ProcessConfig processConfig, DecisionConfig decisionConfig) {
		this.addons = Addons.EMTPY;
		this.processConfig = processConfig;
		this.decisionConfig = decisionConfig;
	}

	@Override
	public ProcessConfig process() {
		return this.processConfig;
	}

	@Override
	public DecisionConfig decision() {
		return decisionConfig;
	}

	@Override
	public Addons addons() {
		return addons;
	}

}
