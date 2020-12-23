
package io.automatiko.engine.workflow.base.instance.impl.humantask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.workflow.workitem.Policy;
import io.automatiko.engine.api.workflow.workitem.Transition;

/**
 * Human task dedicated transition what uses <code>Map</code> of objects to be
 * associated with work item - human task work item.
 *
 */
public class HumanTaskTransition implements Transition<Map<String, Object>> {

	private String phase;
	private Map<String, Object> data;
	private List<Policy<?>> policies = new ArrayList<>();

	public HumanTaskTransition(String phase) {
		this(phase, null);
	}

	public HumanTaskTransition(String phase, Map<String, Object> data) {
		this.phase = phase;
		this.data = data;
	}

	public HumanTaskTransition(String phase, Map<String, Object> data, IdentityProvider identity) {
		this.phase = phase;
		this.data = data;
		if (identity != null) {
			this.policies.add(SecurityPolicy.of(identity));
		}
	}

	public HumanTaskTransition(String phase, Map<String, Object> data, Policy<?>... policies) {
		this.phase = phase;
		this.data = data;
		for (Policy<?> policy : policies) {
			this.policies.add(policy);
		}
	}

	@Override
	public String phase() {
		return phase;
	}

	@Override
	public Map<String, Object> data() {
		return data;
	}

	@Override
	public List<Policy<?>> policies() {
		return policies;
	}

}
