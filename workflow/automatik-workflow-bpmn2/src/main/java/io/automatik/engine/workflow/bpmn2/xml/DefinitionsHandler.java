
package io.automatik.engine.workflow.bpmn2.xml;

import static io.automatik.engine.workflow.compiler.util.ClassUtils.constructClass;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.mvel2.MVEL;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.api.definition.process.Process;
import io.automatik.engine.api.runtime.process.ProcessContext;
import io.automatik.engine.api.workflow.datatype.DataType;
import io.automatik.engine.workflow.base.core.ContextContainer;
import io.automatik.engine.workflow.base.core.context.variable.Variable;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.base.core.datatype.impl.type.BooleanDataType;
import io.automatik.engine.workflow.base.core.datatype.impl.type.FloatDataType;
import io.automatik.engine.workflow.base.core.datatype.impl.type.IntegerDataType;
import io.automatik.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatik.engine.workflow.base.core.datatype.impl.type.StringDataType;
import io.automatik.engine.workflow.base.core.datatype.impl.type.UndefinedDataType;
import io.automatik.engine.workflow.base.instance.impl.Action;
import io.automatik.engine.workflow.bpmn2.core.Definitions;
import io.automatik.engine.workflow.bpmn2.core.Interface;
import io.automatik.engine.workflow.bpmn2.core.Interface.Operation;
import io.automatik.engine.workflow.bpmn2.core.ItemDefinition;
import io.automatik.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatik.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatik.engine.workflow.compiler.xml.Handler;
import io.automatik.engine.workflow.compiler.xml.ProcessBuildData;
import io.automatik.engine.workflow.process.core.NodeContainer;
import io.automatik.engine.workflow.process.core.ProcessAction;
import io.automatik.engine.workflow.process.core.impl.ConsequenceAction;
import io.automatik.engine.workflow.process.core.node.ActionNode;
import io.automatik.engine.workflow.process.core.node.ForEachNode;
import io.automatik.engine.workflow.process.core.node.WorkItemNode;
import io.automatik.engine.workflow.process.executable.core.ExecutableProcess;
import io.automatik.engine.workflow.process.instance.impl.NodeInstanceResolverFactory;
import io.automatik.engine.workflow.process.instance.node.ActionNodeInstance;

public class DefinitionsHandler extends BaseAbstractHandler implements Handler {

	@SuppressWarnings("unchecked")
	public DefinitionsHandler() {
		if ((this.validParents == null) && (this.validPeers == null)) {
			this.validParents = new HashSet();
			this.validParents.add(null);

			this.validPeers = new HashSet();
			this.validPeers.add(null);

			this.allowNesting = false;
		}
	}

	public Object start(final String uri, final String localName, final Attributes attrs,
			final ExtensibleXmlParser parser) throws SAXException {
		parser.startElementBuilder(localName, attrs);
		return new Definitions();
	}

	public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
		final Element element = parser.endElementBuilder();
		Definitions definitions = (Definitions) parser.getCurrent();
		String namespace = element.getAttribute("targetNamespace");
		List<Process> processes = ((ProcessBuildData) parser.getData()).getProcesses();
		Map<String, ItemDefinition> itemDefinitions = (Map<String, ItemDefinition>) ((ProcessBuildData) parser
				.getData()).getMetaData("ItemDefinitions");

		List<Interface> interfaces = (List<Interface>) ((ProcessBuildData) parser.getData()).getMetaData("Interfaces");

		for (Process process : processes) {
			ExecutableProcess ruleFlowProcess = (ExecutableProcess) process;
			ruleFlowProcess.setMetaData("TargetNamespace", namespace);
			postProcessItemDefinitions(ruleFlowProcess, itemDefinitions, parser.getClassLoader());
			postProcessInterfaces(ruleFlowProcess, interfaces);

			postProcessNodes(ruleFlowProcess, Collections.emptyList(), parser);
		}
		definitions.setTargetNamespace(namespace);
		return definitions;
	}

	public Class<?> generateNodeFor() {
		return Definitions.class;
	}

	private void postProcessInterfaces(NodeContainer nodeContainer, List<Interface> interfaces) {

		for (Node node : nodeContainer.getNodes()) {
			if (node instanceof NodeContainer) {
				postProcessInterfaces((NodeContainer) node, interfaces);
			}
			if (node instanceof WorkItemNode && "Service Task".equals(((WorkItemNode) node).getMetaData("Type"))) {
				WorkItemNode workItemNode = (WorkItemNode) node;
				if (interfaces == null) {
					throw new IllegalArgumentException("No interfaces found");
				}
				String operationRef = (String) workItemNode.getMetaData("OperationRef");
				String implementation = (String) workItemNode.getMetaData("Implementation");
				Operation operation = null;
				for (Interface i : interfaces) {
					operation = i.getOperation(operationRef);
					if (operation != null) {
						break;
					}
				}
				if (operation == null) {
					throw new IllegalArgumentException("Could not find operation " + operationRef);
				}
				// avoid overriding parameters set by data input associations
				if (workItemNode.getWork().getParameter("Interface") == null) {
					workItemNode.getWork().setParameter("Interface", operation.getInterface().getName());
				}
				if (workItemNode.getWork().getParameter("Operation") == null) {
					workItemNode.getWork().setParameter("Operation", operation.getName());
				}
				if (workItemNode.getWork().getParameter("ParameterType") == null && operation.getMessage() != null) {
					workItemNode.getWork().setParameter("ParameterType", operation.getMessage().getType());
				}
				// parameters to support web service invocation
				if (implementation != null) {
					workItemNode.getWork().setParameter("interfaceImplementationRef",
							operation.getInterface().getImplementationRef());
					workItemNode.getWork().setParameter("operationImplementationRef", operation.getImplementationRef());
					workItemNode.getWork().setParameter("implementation", implementation);
				}
			}
		}
	}

	private void postProcessItemDefinitions(NodeContainer nodeContainer, Map<String, ItemDefinition> itemDefinitions,
			ClassLoader cl) {
		if (nodeContainer instanceof ContextContainer) {
			setVariablesDataType((ContextContainer) nodeContainer, itemDefinitions, cl);
		}
		// process composite context node of for each to enhance its variables with
		// types
		if (nodeContainer instanceof ForEachNode) {
			setVariablesDataType(((ForEachNode) nodeContainer).getCompositeNode(), itemDefinitions, cl);
		}
		for (Node node : nodeContainer.getNodes()) {
			if (node instanceof NodeContainer) {
				postProcessItemDefinitions((NodeContainer) node, itemDefinitions, cl);
			}
			if (node instanceof ContextContainer) {
				setVariablesDataType((ContextContainer) node, itemDefinitions, cl);
			}
		}
	}

	private void setVariablesDataType(ContextContainer container, Map<String, ItemDefinition> itemDefinitions,
			ClassLoader cl) {
		VariableScope variableScope = (VariableScope) container.getDefaultContext(VariableScope.VARIABLE_SCOPE);
		if (variableScope != null) {
			for (Variable variable : variableScope.getVariables()) {
				setVariableDataType(variable, itemDefinitions, cl);
			}
		}
	}

	private void setVariableDataType(Variable variable, Map<String, ItemDefinition> itemDefinitions, ClassLoader cl) {
		// retrieve type from item definition

		String itemSubjectRef = (String) variable.getMetaData("ItemSubjectRef");
		if (UndefinedDataType.getInstance().equals(variable.getType()) && itemDefinitions != null
				&& itemSubjectRef != null) {
			DataType dataType = new ObjectDataType();
			ItemDefinition itemDefinition = itemDefinitions.get(itemSubjectRef);
			if (itemDefinition != null) {
				String structureRef = itemDefinition.getStructureRef();

				if ("java.lang.Boolean".equals(structureRef) || "Boolean".equals(structureRef)) {
					dataType = new BooleanDataType();

				} else if ("java.lang.Integer".equals(structureRef) || "Integer".equals(structureRef)) {
					dataType = new IntegerDataType();

				} else if ("java.lang.Float".equals(structureRef) || "Float".equals(structureRef)) {
					dataType = new FloatDataType();

				} else if ("java.lang.String".equals(structureRef) || "String".equals(structureRef)) {
					dataType = new StringDataType();

				} else if ("java.lang.Object".equals(structureRef) || "Object".equals(structureRef)) {
					// use FQCN of Object
					dataType = new ObjectDataType(java.lang.Object.class, structureRef);

				} else {
					dataType = new ObjectDataType(constructClass(structureRef, cl), structureRef);
				}

			}
			variable.setType(dataType);
		}
	}

	protected void postProcessNodes(NodeContainer nodeContainer, List<Variable> parentVariables,
			ExtensibleXmlParser parser) throws SAXException {

		for (Node node : nodeContainer.getNodes()) {

			List<Variable> variables = new LinkedList<>(parentVariables);
			VariableScope variableScope = (VariableScope) ((ContextContainer) nodeContainer)
					.getDefaultContext(VariableScope.VARIABLE_SCOPE);
			if (variableScope != null) {
				variables.addAll(variableScope.getVariables());
			}
			if (node instanceof NodeContainer) {
				postProcessNodes((NodeContainer) node, variables, parser);
			} else {
				if (node instanceof ActionNode) {
					ActionNode actionNode = (ActionNode) node;
					ProcessAction action = actionNode.getAction();
					if (action instanceof ConsequenceAction) {
						ConsequenceAction consequenceAction = (ConsequenceAction) action;
						switch (consequenceAction.getDialect()) {
						case "java":
							if (actionNode.getAction().getMetaData("Action") == null) {
								actionNode.getAction().setMetaData("Action", new MvelAction(actionNode));
							}
							break;
						case "mvel":
							if (actionNode.getAction().getMetaData("Action") == null) {
								actionNode.getAction().setMetaData("Action", new MvelAction(actionNode));
							}
							break;
						default:

						}
					}
				}
			}
		}
	}

	private static class MvelAction implements Action {

		private ActionNode actionNode;

		public MvelAction(final ActionNode actionNode) {
			this.actionNode = actionNode;
		}

		@Override
		public void execute(ProcessContext context) throws Exception {

			String expression = ((ConsequenceAction) actionNode.getAction()).getConsequence();
			NodeInstanceResolverFactory resolverFactory = new NodeInstanceResolverFactory(
					(io.automatik.engine.workflow.process.instance.NodeInstance) context.getNodeInstance());
			resolverFactory.addExtraParameters(Collections.singletonMap("kcontext", context));
			Object result = MVEL.eval(expression, resolverFactory);

			((ActionNodeInstance) context.getNodeInstance()).setOutputVariable(result);

		}
	}

}
