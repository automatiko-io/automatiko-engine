
package io.automatik.engine.workflow.compiler.canonical;

import static io.automatik.engine.workflow.process.executable.core.ExecutableNodeContainerFactory.METHOD_CONNECTION;
import static io.automatik.engine.workflow.process.executable.core.ExecutableProcessFactory.METHOD_DYNAMIC;
import static io.automatik.engine.workflow.process.executable.core.ExecutableProcessFactory.METHOD_GLOBAL;
import static io.automatik.engine.workflow.process.executable.core.ExecutableProcessFactory.METHOD_IMPORTS;
import static io.automatik.engine.workflow.process.executable.core.ExecutableProcessFactory.METHOD_NAME;
import static io.automatik.engine.workflow.process.executable.core.ExecutableProcessFactory.METHOD_PACKAGE_NAME;
import static io.automatik.engine.workflow.process.executable.core.ExecutableProcessFactory.METHOD_VALIDATE;
import static io.automatik.engine.workflow.process.executable.core.ExecutableProcessFactory.METHOD_VARIABLE;
import static io.automatik.engine.workflow.process.executable.core.ExecutableProcessFactory.METHOD_VERSION;
import static io.automatik.engine.workflow.process.executable.core.ExecutableProcessFactory.METHOD_VISIBILITY;
import static io.automatik.engine.workflow.process.executable.core.Metadata.HIDDEN;
import static io.automatik.engine.workflow.process.executable.core.Metadata.LINK_NODE_HIDDEN;
import static io.automatik.engine.workflow.process.executable.core.Metadata.UNIQUE_ID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatik.engine.api.definition.process.Connection;
import io.automatik.engine.api.definition.process.WorkflowProcess;
import io.automatik.engine.workflow.base.core.ContextContainer;
import io.automatik.engine.workflow.base.core.Work;
import io.automatik.engine.workflow.base.core.context.variable.Variable;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.NodeContainer;
import io.automatik.engine.workflow.process.core.impl.ConnectionImpl;
import io.automatik.engine.workflow.process.core.node.ActionNode;
import io.automatik.engine.workflow.process.core.node.BoundaryEventNode;
import io.automatik.engine.workflow.process.core.node.CompositeContextNode;
import io.automatik.engine.workflow.process.core.node.DynamicNode;
import io.automatik.engine.workflow.process.core.node.EndNode;
import io.automatik.engine.workflow.process.core.node.EventNode;
import io.automatik.engine.workflow.process.core.node.EventSubProcessNode;
import io.automatik.engine.workflow.process.core.node.FaultNode;
import io.automatik.engine.workflow.process.core.node.ForEachNode;
import io.automatik.engine.workflow.process.core.node.HumanTaskNode;
import io.automatik.engine.workflow.process.core.node.Join;
import io.automatik.engine.workflow.process.core.node.MilestoneNode;
import io.automatik.engine.workflow.process.core.node.RuleSetNode;
import io.automatik.engine.workflow.process.core.node.Split;
import io.automatik.engine.workflow.process.core.node.StartNode;
import io.automatik.engine.workflow.process.core.node.StateNode;
import io.automatik.engine.workflow.process.core.node.SubProcessNode;
import io.automatik.engine.workflow.process.core.node.TimerNode;
import io.automatik.engine.workflow.process.core.node.WorkItemNode;
import io.automatik.engine.workflow.process.executable.core.ExecutableProcessFactory;

public class ProcessVisitor extends AbstractVisitor {

	public static final String DEFAULT_VERSION = "1.0";

	private Map<Class<?>, AbstractNodeVisitor<? extends io.automatik.engine.api.definition.process.Node>> nodesVisitors = new HashMap<>();

	public ProcessVisitor(ClassLoader contextClassLoader) {
		this.nodesVisitors.put(StartNode.class, new StartNodeVisitor());
		this.nodesVisitors.put(ActionNode.class, new ActionNodeVisitor());
		this.nodesVisitors.put(EndNode.class, new EndNodeVisitor());
		this.nodesVisitors.put(HumanTaskNode.class, new HumanTaskNodeVisitor());
		this.nodesVisitors.put(WorkItemNode.class, new WorkItemNodeVisitor<>(contextClassLoader));
		this.nodesVisitors.put(SubProcessNode.class, new LambdaSubProcessNodeVisitor());
		this.nodesVisitors.put(Split.class, new SplitNodeVisitor());
		this.nodesVisitors.put(Join.class, new JoinNodeVisitor());
		this.nodesVisitors.put(FaultNode.class, new FaultNodeVisitor());
		this.nodesVisitors.put(RuleSetNode.class, new RuleSetNodeVisitor(contextClassLoader));
		this.nodesVisitors.put(BoundaryEventNode.class, new BoundaryEventNodeVisitor());
		this.nodesVisitors.put(EventNode.class, new EventNodeVisitor());
		this.nodesVisitors.put(ForEachNode.class, new ForEachNodeVisitor(nodesVisitors));
		this.nodesVisitors.put(CompositeContextNode.class, new CompositeContextNodeVisitor<>(nodesVisitors));
		this.nodesVisitors.put(EventSubProcessNode.class, new EventSubProcessNodeVisitor(nodesVisitors));
		this.nodesVisitors.put(TimerNode.class, new TimerNodeVisitor());
		this.nodesVisitors.put(MilestoneNode.class, new MilestoneNodeVisitor());
		this.nodesVisitors.put(DynamicNode.class, new DynamicNodeVisitor(nodesVisitors));
		this.nodesVisitors.put(StateNode.class, new StateNodeVisitor(nodesVisitors));
	}

	public void visitProcess(WorkflowProcess process, MethodDeclaration processMethod, ProcessMetaData metadata) {
		BlockStmt body = new BlockStmt();

		ClassOrInterfaceType processFactoryType = new ClassOrInterfaceType(null,
				ExecutableProcessFactory.class.getSimpleName());

		// create local variable factory and assign new fluent process to it
		VariableDeclarationExpr factoryField = new VariableDeclarationExpr(processFactoryType, FACTORY_FIELD_NAME);
		MethodCallExpr assignFactoryMethod = new MethodCallExpr(new NameExpr(processFactoryType.getName().asString()),
				"createProcess");
		assignFactoryMethod.addArgument(new StringLiteralExpr(process.getId()));
		body.addStatement(new AssignExpr(factoryField, assignFactoryMethod, AssignExpr.Operator.ASSIGN));

		// item definitions
		Set<String> visitedVariables = new HashSet<>();
		VariableScope variableScope = (VariableScope) ((io.automatik.engine.workflow.base.core.Process) process)
				.getDefaultContext(VariableScope.VARIABLE_SCOPE);

		visitVariableScope(variableScope, body, visitedVariables);
		visitSubVariableScopes(process.getNodes(), body, visitedVariables);

		visitInterfaces(process.getNodes(), body);

		metadata.setDynamic(((io.automatik.engine.workflow.process.core.WorkflowProcess) process).isDynamic());
		// the process itself
		body.addStatement(getFactoryMethod(FACTORY_FIELD_NAME, METHOD_NAME, new StringLiteralExpr(process.getName())))
				.addStatement(getFactoryMethod(FACTORY_FIELD_NAME, METHOD_PACKAGE_NAME,
						new StringLiteralExpr(process.getPackageName())))
				.addStatement(getFactoryMethod(FACTORY_FIELD_NAME, METHOD_DYNAMIC,
						new BooleanLiteralExpr(metadata.isDynamic())))
				.addStatement(getFactoryMethod(FACTORY_FIELD_NAME, METHOD_VERSION,
						new StringLiteralExpr(getOrDefault(process.getVersion(), DEFAULT_VERSION))))
				.addStatement(getFactoryMethod(FACTORY_FIELD_NAME, METHOD_VISIBILITY, new StringLiteralExpr(
						getOrDefault(process.getVisibility(), WorkflowProcess.PUBLIC_VISIBILITY))));

		visitMetaData(process.getMetaData(), body, FACTORY_FIELD_NAME);

		visitHeader(process, body);

		List<Node> processNodes = new ArrayList<>();
		for (io.automatik.engine.api.definition.process.Node procNode : process.getNodes()) {
			processNodes.add((io.automatik.engine.workflow.process.core.Node) procNode);
		}
		visitNodes(processNodes, body, variableScope, metadata);
		visitConnections(process.getNodes(), body);

		body.addStatement(getFactoryMethod(FACTORY_FIELD_NAME, METHOD_VALIDATE));

		MethodCallExpr getProcessMethod = new MethodCallExpr(new NameExpr(FACTORY_FIELD_NAME), "getProcess");
		body.addStatement(new ReturnStmt(getProcessMethod));
		processMethod.setBody(body);
	}

	private void visitVariableScope(VariableScope variableScope, BlockStmt body, Set<String> visitedVariables) {
		if (variableScope != null && !variableScope.getVariables().isEmpty()) {
			for (Variable variable : variableScope.getVariables()) {

				if (!visitedVariables.add(variable.getName())) {
					continue;
				}
				String tags = (String) variable.getMetaData(Variable.VARIABLE_TAGS);
				ClassOrInterfaceType variableType = new ClassOrInterfaceType(null,
						ObjectDataType.class.getSimpleName());
				ObjectCreationExpr variableValue = new ObjectCreationExpr(null, variableType, new NodeList<>(
						new ClassExpr(new ClassOrInterfaceType(null, variable.getType().getStringType()))));
				body.addStatement(
						getFactoryMethod(FACTORY_FIELD_NAME, METHOD_VARIABLE, new StringLiteralExpr(variable.getName()),
								variableValue, new StringLiteralExpr(Variable.VARIABLE_TAGS),
								tags != null ? new StringLiteralExpr(tags) : new NullLiteralExpr()));
			}
		}
	}

	private void visitSubVariableScopes(io.automatik.engine.api.definition.process.Node[] nodes, BlockStmt body,
			Set<String> visitedVariables) {
		for (io.automatik.engine.api.definition.process.Node node : nodes) {
			if (node instanceof ContextContainer) {
				VariableScope variableScope = (VariableScope) ((ContextContainer) node)
						.getDefaultContext(VariableScope.VARIABLE_SCOPE);
				if (variableScope != null) {
					visitVariableScope(variableScope, body, visitedVariables);
				}
			}
			if (node instanceof NodeContainer) {
				visitSubVariableScopes(((NodeContainer) node).getNodes(), body, visitedVariables);
			}
		}
	}

	private void visitHeader(WorkflowProcess process, BlockStmt body) {
		Map<String, Object> metaData = getMetaData(process.getMetaData());
		Set<String> imports = ((io.automatik.engine.workflow.base.core.Process) process).getImports();
		Map<String, String> globals = ((io.automatik.engine.workflow.base.core.Process) process).getGlobals();
		if ((imports != null && !imports.isEmpty()) || (globals != null && globals.size() > 0) || !metaData.isEmpty()) {
			if (imports != null) {
				for (String s : imports) {
					body.addStatement(getFactoryMethod(FACTORY_FIELD_NAME, METHOD_IMPORTS, new StringLiteralExpr(s)));
				}
			}
			if (globals != null) {
				for (Map.Entry<String, String> global : globals.entrySet()) {
					body.addStatement(getFactoryMethod(FACTORY_FIELD_NAME, METHOD_GLOBAL,
							new StringLiteralExpr(global.getKey()), new StringLiteralExpr(global.getValue())));
				}
			}
		}
	}

	private Map<String, Object> getMetaData(Map<String, Object> input) {
		Map<String, Object> metaData = new HashMap<>();
		for (Map.Entry<String, Object> entry : input.entrySet()) {
			String name = entry.getKey();
			if (entry.getKey().startsWith("custom") && entry.getValue() instanceof String) {
				metaData.put(name, entry.getValue());
			}
		}
		return metaData;
	}

	private <U extends io.automatik.engine.api.definition.process.Node> void visitNodes(List<U> nodes, BlockStmt body,
			VariableScope variableScope, ProcessMetaData metadata) {
		for (U node : nodes) {
			AbstractNodeVisitor<U> visitor = (AbstractNodeVisitor<U>) nodesVisitors.get(node.getClass());
			if (visitor == null) {
				throw new IllegalStateException("No visitor found for node " + node.getClass().getName());
			}
			visitor.visitNode(node, body, variableScope, metadata);
		}
	}

	private void visitConnections(io.automatik.engine.api.definition.process.Node[] nodes, BlockStmt body) {

		List<Connection> connections = new ArrayList<>();
		for (io.automatik.engine.api.definition.process.Node node : nodes) {
			for (List<Connection> connectionList : node.getIncomingConnections().values()) {
				connections.addAll(connectionList);
			}
		}
		for (Connection connection : connections) {
			visitConnection(connection, body);
		}
	}

	// KOGITO-1882 Finish implementation or delete completely
	private void visitInterfaces(io.automatik.engine.api.definition.process.Node[] nodes, BlockStmt body) {
		for (io.automatik.engine.api.definition.process.Node node : nodes) {
			if (node instanceof WorkItemNode) {
				Work work = ((WorkItemNode) node).getWork();
				if (work != null) {
					// TODO - finish this method
				}
			}
		}
	}

	private void visitConnection(Connection connection, BlockStmt body) {
		// if the connection was generated by a link event, don't dump.
		if (isConnectionRepresentingLinkEvent(connection)) {
			return;
		}
		// if the connection is a hidden one (compensations), don't dump
		Object hidden = ((ConnectionImpl) connection).getMetaData(HIDDEN);
		if (hidden != null && ((Boolean) hidden)) {
			return;
		}

		body.addStatement(getFactoryMethod(FACTORY_FIELD_NAME, METHOD_CONNECTION,
				new LongLiteralExpr(connection.getFrom().getId()), new LongLiteralExpr(connection.getTo().getId()),
				new StringLiteralExpr(getOrDefault((String) connection.getMetaData().get(UNIQUE_ID), ""))));
	}

	private boolean isConnectionRepresentingLinkEvent(Connection connection) {
		return connection.getMetaData().get(LINK_NODE_HIDDEN) != null;
	}
}
