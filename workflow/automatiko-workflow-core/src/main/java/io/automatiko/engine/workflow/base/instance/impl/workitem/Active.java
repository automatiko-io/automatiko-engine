package io.automatiko.engine.workflow.base.instance.impl.workitem;

import java.util.Arrays;
import java.util.List;

import io.automatiko.engine.api.workflow.workitem.LifeCyclePhase;

public class Active implements LifeCyclePhase {

	public static final String ID = "active";
	public static final String STATUS = "Ready";

	private List<String> allowedTransitions = Arrays.asList();

	@Override
	public String id() {
		return ID;
	}

	@Override
	public String status() {
		return STATUS;
	}

	@Override
	public boolean isTerminating() {
		return false;
	}

	@Override
	public boolean canTransition(LifeCyclePhase phase) {
		if (phase == null) {
			return true;
		}

		return allowedTransitions.contains(phase.id());
	}

}
