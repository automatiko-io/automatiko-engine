
package io.automatik.engine.api.workflow;

import java.util.Map;

public interface WorkItem {

	String getId();

	String getNodeInstanceId();

	String getName();

	int getState();

	String getPhase();

	String getPhaseStatus();

	Map<String, Object> getParameters();

	Map<String, Object> getResults();
}
