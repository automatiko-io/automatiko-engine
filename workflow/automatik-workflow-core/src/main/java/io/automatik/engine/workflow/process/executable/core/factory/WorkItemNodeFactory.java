
package io.automatik.engine.workflow.process.executable.core.factory;

import java.util.Set;

import io.automatik.engine.api.workflow.datatype.DataType;
import io.automatik.engine.workflow.base.core.ParameterDefinition;
import io.automatik.engine.workflow.base.core.Work;
import io.automatik.engine.workflow.base.core.context.variable.Mappable;
import io.automatik.engine.workflow.base.core.impl.ParameterDefinitionImpl;
import io.automatik.engine.workflow.base.core.impl.WorkImpl;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.NodeContainer;
import io.automatik.engine.workflow.process.core.node.WorkItemNode;
import io.automatik.engine.workflow.process.executable.core.ExecutableNodeContainerFactory;

public class WorkItemNodeFactory extends StateBasedNodeFactory implements MappableNodeFactory {

	public static final String METHOD_WORK_NAME = "workName";
	public static final String METHOD_WORK_PARAMETER = "workParameter";

	public WorkItemNodeFactory(ExecutableNodeContainerFactory nodeContainerFactory, NodeContainer nodeContainer,
			long id) {
		super(nodeContainerFactory, nodeContainer, id);
	}

	protected Node createNode() {
		return new WorkItemNode();
	}

	protected WorkItemNode getWorkItemNode() {
		return (WorkItemNode) getNode();
	}

	@Override
	public WorkItemNodeFactory name(String name) {
		super.name(name);
		return this;
	}

	@Override
	public WorkItemNodeFactory onEntryAction(String dialect, String action) {
		super.onEntryAction(dialect, action);
		return this;
	}

	@Override
	public WorkItemNodeFactory onExitAction(String dialect, String action) {
		super.onExitAction(dialect, action);
		return this;
	}

	@Override
	public WorkItemNodeFactory timer(String delay, String period, String dialect, String action) {
		super.timer(delay, period, dialect, action);
		return this;
	}

	@Override
	public Mappable getMappableNode() {
		return getWorkItemNode();
	}

	@Override
	public WorkItemNodeFactory inMapping(String parameterName, String variableName) {
		MappableNodeFactory.super.inMapping(parameterName, variableName);
		return this;
	}

	@Override
	public WorkItemNodeFactory outMapping(String parameterName, String variableName) {
		MappableNodeFactory.super.outMapping(parameterName, variableName);
		return this;
	}

	public WorkItemNodeFactory waitForCompletion(boolean waitForCompletion) {
		getWorkItemNode().setWaitForCompletion(waitForCompletion);
		return this;
	}

	public WorkItemNodeFactory workName(String name) {
		Work work = getWorkItemNode().getWork();
		if (work == null) {
			work = new WorkImpl();
			getWorkItemNode().setWork(work);
		}
		work.setName(name);
		return this;
	}

	public WorkItemNodeFactory workParameter(String name, Object value) {
		Work work = getWorkItemNode().getWork();
		if (work == null) {
			work = new WorkImpl();
			getWorkItemNode().setWork(work);
		}
		work.setParameter(name, value);
		return this;
	}

	public WorkItemNodeFactory workParameterDefinition(String name, DataType dataType) {
		Work work = getWorkItemNode().getWork();
		if (work == null) {
			work = new WorkImpl();
			getWorkItemNode().setWork(work);
		}
		Set<ParameterDefinition> parameterDefinitions = work.getParameterDefinitions();
		parameterDefinitions.add(new ParameterDefinitionImpl(name, dataType));
		work.setParameterDefinitions(parameterDefinitions);
		return this;
	}
}
