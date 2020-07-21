
package io.automatik.engine.workflow.base.instance.context.swimlane;

import java.util.HashMap;
import java.util.Map;

import io.automatik.engine.workflow.base.core.context.swimlane.SwimlaneContext;
import io.automatik.engine.workflow.base.instance.context.AbstractContextInstance;

public class SwimlaneContextInstance extends AbstractContextInstance {

	private static final long serialVersionUID = 510l;

	private Map<String, String> swimlaneActors = new HashMap<String, String>();

	public String getContextType() {
		return SwimlaneContext.SWIMLANE_SCOPE;
	}

	public SwimlaneContext getSwimlaneContext() {
		return (SwimlaneContext) getContext();
	}

	public String getActorId(String swimlane) {
		return swimlaneActors.get(swimlane);
	}

	public void setActorId(String swimlane, String actorId) {
		swimlaneActors.put(swimlane, actorId);
	}

	public Map<String, String> getSwimlaneActors() {
		return swimlaneActors;
	}

}
