
package io.automatik.engine.workflow.process.executable.core.factory;

import io.automatik.engine.workflow.base.core.context.variable.Mappable;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.NodeContainer;
import io.automatik.engine.workflow.process.core.node.SubProcessFactory;
import io.automatik.engine.workflow.process.core.node.SubProcessNode;
import io.automatik.engine.workflow.process.executable.core.ExecutableNodeContainerFactory;

public class SubProcessNodeFactory extends StateBasedNodeFactory implements MappableNodeFactory {

	public static final String METHOD_PROCESS_ID = "processId";
	public static final String METHOD_PROCESS_NAME = "processName";
	public static final String METHOD_WAIT_FOR_COMPLETION = "waitForCompletion";
	public static final String METHOD_INDEPENDENT = "independent";

	public SubProcessNodeFactory(ExecutableNodeContainerFactory nodeContainerFactory, NodeContainer nodeContainer,
			long id) {
		super(nodeContainerFactory, nodeContainer, id);
	}

	protected Node createNode() {
		return new SubProcessNode();
	}

	protected SubProcessNode getSubProcessNode() {
		return (SubProcessNode) getNode();
	}

	@Override
	public SubProcessNodeFactory name(String name) {
		super.name(name);
		return this;
	}

	@Override
	public SubProcessNodeFactory onEntryAction(String dialect, String action) {
		super.onEntryAction(dialect, action);
		return this;
	}

	@Override
	public SubProcessNodeFactory onExitAction(String dialect, String action) {
		super.onExitAction(dialect, action);
		return this;
	}

	@Override
	public SubProcessNodeFactory timer(String delay, String period, String dialect, String action) {
		super.timer(delay, period, dialect, action);
		return this;
	}

	@Override
	public Mappable getMappableNode() {
		return getSubProcessNode();
	}

	@Override
	public SubProcessNodeFactory inMapping(String parameterName, String variableName) {
		MappableNodeFactory.super.inMapping(parameterName, variableName);
		return this;
	}

	@Override
	public SubProcessNodeFactory outMapping(String parameterName, String variableName) {
		MappableNodeFactory.super.outMapping(parameterName, variableName);
		return this;
	}

	public SubProcessNodeFactory processId(final String processId) {
		getSubProcessNode().setProcessId(processId);
		return this;
	}

	public SubProcessNodeFactory processName(final String processName) {
		getSubProcessNode().setProcessName(processName);
		return this;
	}

	public SubProcessNodeFactory waitForCompletion(boolean waitForCompletion) {
		getSubProcessNode().setWaitForCompletion(waitForCompletion);
		return this;
	}

	public SubProcessNodeFactory independent(boolean independent) {
		getSubProcessNode().setIndependent(independent);
		return this;
	}

	public <T> SubProcessNodeFactory subProcessNode(SubProcessFactory<T> factory) {
		getSubProcessNode().setSubProcessFactory(factory);
		return this;
	}
}
