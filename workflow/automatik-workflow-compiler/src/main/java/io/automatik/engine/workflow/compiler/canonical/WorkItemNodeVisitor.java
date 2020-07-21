
package io.automatik.engine.workflow.compiler.canonical;

import java.util.Map.Entry;

import static io.automatik.engine.workflow.process.executable.core.factory.WorkItemNodeFactory.METHOD_WORK_NAME;
import static io.automatik.engine.workflow.process.executable.core.factory.WorkItemNodeFactory.METHOD_WORK_PARAMETER;

import java.util.Objects;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;

import io.automatik.engine.workflow.base.core.Work;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.process.core.node.WorkItemNode;
import io.automatik.engine.workflow.process.executable.core.factory.WorkItemNodeFactory;

public class WorkItemNodeVisitor<T extends WorkItemNode> extends AbstractNodeVisitor<T> {

	private enum ParamType {
		BOOLEAN(Boolean.class.getSimpleName()), INTEGER(Integer.class.getSimpleName()),
		FLOAT(Float.class.getSimpleName());

		final String name;

		public String getName() {
			return name;
		}

		ParamType(String name) {
			this.name = name;
		}

		public static ParamType fromString(String name) {
			for (ParamType p : ParamType.values()) {
				if (Objects.equals(p.name, name)) {
					return p;
				}
			}
			return null;
		}
	}

	private final ClassLoader contextClassLoader;

	public WorkItemNodeVisitor(ClassLoader contextClassLoader) {
		this.contextClassLoader = contextClassLoader;
	}

	@Override
	protected String getNodeKey() {
		return "workItemNode";
	}

	@Override
	public void visitNode(String factoryField, T node, BlockStmt body, VariableScope variableScope,
			ProcessMetaData metadata) {
		Work work = node.getWork();
		String workName = node.getWork().getName();

		if (workName.equals("Service Task")) {
			ServiceTaskDescriptor d = new ServiceTaskDescriptor(node, contextClassLoader);
			String mangledName = d.mangledName();
			CompilationUnit generatedHandler = d.generateHandlerClassForService();
			metadata.getGeneratedHandlers().put(mangledName, generatedHandler);
			workName = mangledName;
		}

		body.addStatement(getAssignedFactoryMethod(factoryField, WorkItemNodeFactory.class, getNodeId(node),
				getNodeKey(), new LongLiteralExpr(node.getId()))).addStatement(getNameMethod(node, work.getName()))
				.addStatement(getFactoryMethod(getNodeId(node), METHOD_WORK_NAME, new StringLiteralExpr(workName)));

		addWorkItemParameters(work, body, getNodeId(node));
		addNodeMappings(node, body, getNodeId(node));

		body.addStatement(getDoneMethod(getNodeId(node)));

		visitMetaData(node.getMetaData(), body, getNodeId(node));

		metadata.getWorkItems().add(workName);
	}

	protected void addWorkItemParameters(Work work, BlockStmt body, String variableName) {
		for (Entry<String, Object> entry : work.getParameters().entrySet()) {
			if (entry.getValue() == null) {
				continue; // interfaceImplementationRef ?
			}
			String paramType = null;
			if (work.getParameterDefinition(entry.getKey()) != null) {
				paramType = work.getParameterDefinition(entry.getKey()).getType().getStringType();
			}
			body.addStatement(getFactoryMethod(variableName, METHOD_WORK_PARAMETER,
					new StringLiteralExpr(entry.getKey()), getParameterExpr(paramType, entry.getValue().toString())));
		}
	}

	private Expression getParameterExpr(String type, String value) {
		ParamType pType = ParamType.fromString(type);
		if (pType == null) {
			return new StringLiteralExpr(value);
		}
		switch (pType) {
		case BOOLEAN:
			return new BooleanLiteralExpr(Boolean.parseBoolean(value));
		case FLOAT:
			return new MethodCallExpr().setScope(new NameExpr(Float.class.getName())).setName("parseFloat")
					.addArgument(new StringLiteralExpr(value));
		case INTEGER:
			return new IntegerLiteralExpr(Integer.parseInt(value));
		default:
			return new StringLiteralExpr(value);
		}
	}

}
