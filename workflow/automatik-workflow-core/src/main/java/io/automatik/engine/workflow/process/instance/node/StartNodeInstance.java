
package io.automatik.engine.workflow.process.instance.node;

import java.util.Date;

import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.base.core.event.EventTransformer;
import io.automatik.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatik.engine.workflow.process.core.node.StartNode;
import io.automatik.engine.workflow.process.instance.impl.NodeInstanceImpl;

/**
 * Runtime counterpart of a start node.
 * 
 */
public class StartNodeInstance extends NodeInstanceImpl {

	private static final long serialVersionUID = 510l;

	public void internalTrigger(final NodeInstance from, String type) {
		if (type != null) {
			throw new IllegalArgumentException("A StartNode does not accept incoming connections!");
		}
		if (from != null) {
			throw new IllegalArgumentException("A StartNode can only be triggered by the process itself!");
		}
		triggerTime = new Date();
		triggerCompleted();
	}

	public void signalEvent(String type, Object event) {
		String variableName = (String) getStartNode().getMetaData("TriggerMapping");
		if (variableName != null) {
			VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
					VariableScope.VARIABLE_SCOPE, variableName);
			if (variableScopeInstance == null) {
				throw new IllegalArgumentException("Could not find variable for start node: " + variableName);
			}

			EventTransformer transformer = getStartNode().getEventTransformer();
			if (transformer != null) {
				event = transformer.transformEvent(event);
			}

			variableScopeInstance.setVariable(this, variableName, event);
		}
		triggerCompleted();
	}

	public StartNode getStartNode() {
		return (StartNode) getNode();
	}

	public void triggerCompleted() {
		((io.automatik.engine.workflow.process.instance.NodeInstanceContainer) getNodeInstanceContainer())
				.setCurrentLevel(getLevel());
		triggerCompleted(io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE, true);
	}
}
