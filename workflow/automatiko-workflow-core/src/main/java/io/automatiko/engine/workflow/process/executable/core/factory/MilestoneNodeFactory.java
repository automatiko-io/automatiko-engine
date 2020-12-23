
package io.automatiko.engine.workflow.process.executable.core.factory;

import java.util.function.Predicate;

import io.automatiko.engine.api.runtime.process.ProcessContext;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.NodeContainer;
import io.automatiko.engine.workflow.process.core.node.MilestoneNode;
import io.automatiko.engine.workflow.process.executable.core.ExecutableNodeContainerFactory;

public class MilestoneNodeFactory extends StateBasedNodeFactory {

	public static final String METHOD_CONDITION = "condition";

	public MilestoneNodeFactory(ExecutableNodeContainerFactory nodeContainerFactory, NodeContainer nodeContainer,
			long id) {
		super(nodeContainerFactory, nodeContainer, id);
	}

	protected Node createNode() {
		return new MilestoneNode();
	}

	protected MilestoneNode getMilestoneNode() {
		return (MilestoneNode) getNode();
	}

	@Override
	public MilestoneNodeFactory name(String name) {
		super.name(name);
		return this;
	}

	@Override
	public MilestoneNodeFactory onEntryAction(String dialect, String action) {
		super.onEntryAction(dialect, action);
		return this;
	}

	@Override
	public MilestoneNodeFactory onExitAction(String dialect, String action) {
		super.onExitAction(dialect, action);
		return this;
	}

	@Override
	public MilestoneNodeFactory timer(String delay, String period, String dialect, String action) {
		super.timer(delay, period, dialect, action);
		return this;
	}

	@Override
	public MilestoneNodeFactory metaData(String name, Object value) {
		super.metaData(name, value);
		return this;
	}

	public MilestoneNodeFactory condition(Predicate<ProcessContext> condition) {
		getMilestoneNode().setCondition(condition);
		return this;
	}
}
