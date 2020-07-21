
package io.automatik.engine.workflow.process.instance;

import java.util.Date;
import java.util.Map;

import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.workflow.base.instance.ContextInstance;

/**
 * Represents a node instance in a RuleFlow. This is the runtime counterpart of
 * a node, containing all runtime state. Node instance classes also contain the
 * logic on what to do when it is being triggered (start executing) or completed
 * (end of execution).
 * 
 */
public interface NodeInstance extends io.automatik.engine.api.runtime.process.NodeInstance {

	void trigger(io.automatik.engine.api.runtime.process.NodeInstance from, String type);

	void cancel();

	Node getNode();

	ContextInstance resolveContextInstance(String contextId, Object param);

	int getLevel();

	void setDynamicParameters(Map<String, Object> dynamicParameters);

	int getSlaCompliance();

	Date getSlaDueDate();

	String getSlaTimerId();

}
