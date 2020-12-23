
package io.automatiko.engine.workflow.process.executable.core.factory;

import java.util.function.Supplier;

import io.automatiko.engine.api.decision.DecisionModel;
import io.automatiko.engine.workflow.base.core.context.variable.Mappable;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.NodeContainer;
import io.automatiko.engine.workflow.process.core.node.RuleSetNode;
import io.automatiko.engine.workflow.process.executable.core.ExecutableNodeContainerFactory;

public class RuleSetNodeFactory extends StateBasedNodeFactory implements MappableNodeFactory {

	public static final String METHOD_DECISION = "decision";
	public static final String METHOD_PARAMETER = "parameter";

	public RuleSetNodeFactory(ExecutableNodeContainerFactory nodeContainerFactory, NodeContainer nodeContainer,
			long id) {
		super(nodeContainerFactory, nodeContainer, id);
	}

	protected Node createNode() {
		return new RuleSetNode();
	}

	protected RuleSetNode getRuleSetNode() {
		return (RuleSetNode) getNode();
	}

	@Override
	public RuleSetNodeFactory name(String name) {
		super.name(name);
		return this;
	}

	@Override
	public RuleSetNodeFactory timer(String delay, String period, String dialect, String action) {
		super.timer(delay, period, dialect, action);
		return this;
	}

	@Override
	public Mappable getMappableNode() {
		return getRuleSetNode();
	}

	@Override
	public RuleSetNodeFactory inMapping(String parameterName, String variableName) {
		MappableNodeFactory.super.inMapping(parameterName, variableName);
		return this;
	}

	@Override
	public RuleSetNodeFactory outMapping(String parameterName, String variableName) {
		MappableNodeFactory.super.outMapping(parameterName, variableName);
		return this;
	}

	public RuleSetNodeFactory decision(String namespace, String model, String decision,
			Supplier<DecisionModel> supplier) {
		return decision(namespace, model, decision, true, supplier);
	}

	public RuleSetNodeFactory decision(String namespace, String model, String decision, boolean decisionService,
			Supplier<DecisionModel> supplier) {
		getRuleSetNode().setRuleType(RuleSetNode.RuleType.decision(namespace, model, decision, decisionService));
		getRuleSetNode().setLanguage(RuleSetNode.DMN_LANG);
		getRuleSetNode().setDecisionModel(supplier);
		return this;
	}

	public RuleSetNodeFactory parameter(String name, Object value) {
		getRuleSetNode().setParameter(name, value);
		return this;
	}
}
