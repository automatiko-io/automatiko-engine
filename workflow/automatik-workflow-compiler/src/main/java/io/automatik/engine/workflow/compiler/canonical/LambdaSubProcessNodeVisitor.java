
package io.automatik.engine.workflow.compiler.canonical;

import static com.github.javaparser.StaticJavaParser.parse;
import static io.automatik.engine.workflow.compiler.util.ClassUtils.constructClass;
import static io.automatik.engine.workflow.process.executable.core.factory.SubProcessNodeFactory.METHOD_INDEPENDENT;
import static io.automatik.engine.workflow.process.executable.core.factory.SubProcessNodeFactory.METHOD_PROCESS_ID;
import static io.automatik.engine.workflow.process.executable.core.factory.SubProcessNodeFactory.METHOD_PROCESS_NAME;
import static io.automatik.engine.workflow.process.executable.core.factory.SubProcessNodeFactory.METHOD_WAIT_FOR_COMPLETION;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatik.engine.api.definition.process.WorkflowProcess;
import io.automatik.engine.services.utils.StringUtils;
import io.automatik.engine.workflow.base.core.context.variable.Variable;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatik.engine.workflow.process.core.node.SubProcessNode;
import io.automatik.engine.workflow.process.executable.core.factory.SubProcessNodeFactory;
import io.automatik.engine.workflow.util.PatternConstants;

public class LambdaSubProcessNodeVisitor extends AbstractNodeVisitor<SubProcessNode> {

	@Override
	protected String getNodeKey() {
		return "subProcessNode";
	}

	@Override
	public void visitNode(String factoryField, SubProcessNode node, BlockStmt body, VariableScope variableScope,
			ProcessMetaData metadata) {
		InputStream resourceAsStream = this.getClass()
				.getResourceAsStream("/class-templates/SubProcessFactoryTemplate.java");
		Optional<Expression> retValue = parse(resourceAsStream).findFirst(Expression.class);
		String name = node.getName();
		String subProcessId = node.getProcessId();

		NodeValidator.of(getNodeKey(), name).notEmpty("subProcessId", subProcessId).validate();

		body.addStatement(getAssignedFactoryMethod(factoryField, SubProcessNodeFactory.class, getNodeId(node),
				getNodeKey(), new LongLiteralExpr(node.getId()))).addStatement(getNameMethod(node, "Call Activity"))
				.addStatement(getFactoryMethod(getNodeId(node), METHOD_PROCESS_ID, new StringLiteralExpr(subProcessId)))
				.addStatement(getFactoryMethod(getNodeId(node), METHOD_PROCESS_NAME,
						new StringLiteralExpr(getOrDefault(node.getProcessName(), ""))))
				.addStatement(getFactoryMethod(getNodeId(node), METHOD_WAIT_FOR_COMPLETION,
						new BooleanLiteralExpr(node.isWaitForCompletion())))
				.addStatement(getFactoryMethod(getNodeId(node), METHOD_INDEPENDENT,
						new BooleanLiteralExpr(node.isIndependent())));

		Map<String, String> inputTypes = (Map<String, String>) node.getMetaData("BPMN.InputTypes");

		String subProcessModelClassName = ProcessToExecModelGenerator.extractModelClassName(subProcessId, null);
		ModelMetaData subProcessModel = new ModelMetaData(subProcessId, null, metadata.getPackageName(),
				subProcessModelClassName, WorkflowProcess.PRIVATE_VISIBILITY,
				VariableDeclarations.ofRawInfo(inputTypes), false);

		retValue.ifPresent(retValueExpression -> {
			retValueExpression.findAll(ClassOrInterfaceType.class).stream()
					.filter(t -> t.getNameAsString().equals("$Type$"))
					.forEach(t -> t.setName(subProcessModelClassName));

			retValueExpression.findFirst(MethodDeclaration.class, m -> m.getNameAsString().equals("bind"))
					.ifPresent(m -> m.setBody(bind(variableScope, node, subProcessModel)));
			retValueExpression.findFirst(MethodDeclaration.class, m -> m.getNameAsString().equals("createInstance"))
					.ifPresent(m -> m.setBody(createInstance(node, metadata)));
			retValueExpression.findFirst(MethodDeclaration.class, m -> m.getNameAsString().equals("unbind"))
					.ifPresent(m -> m.setBody(unbind(variableScope, node)));
		});

		if (retValue.isPresent()) {
			body.addStatement(getFactoryMethod(getNodeId(node), getNodeKey(), retValue.get()));
		} else {
			body.addStatement(getFactoryMethod(getNodeId(node), getNodeKey()));
		}

		visitMetaData(node.getMetaData(), body, getNodeId(node));
		body.addStatement(getDoneMethod(getNodeId(node)));
	}

	private BlockStmt bind(VariableScope variableScope, SubProcessNode subProcessNode, ModelMetaData subProcessModel) {
		BlockStmt actionBody = new BlockStmt();
		actionBody.addStatement(subProcessModel.newInstance("model"));

		for (Map.Entry<String, String> e : subProcessNode.getInMappings().entrySet()) {
			// check if given mapping is an expression
			Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(e.getValue());
			if (matcher.find()) {

				String expression = matcher.group(1);
				String topLevelVariable = expression.split("\\.")[0];
				Variable v = variableScope.findVariable(topLevelVariable);

				actionBody.addStatement(makeAssignment(v));
				actionBody.addStatement(
						subProcessModel.callSetter("model", e.getKey(), dotNotationToGetExpression(expression)));
			} else {

				Variable v = variableScope.findVariable(e.getValue());
				if (v != null) {
					actionBody.addStatement(makeAssignment(v));
					actionBody.addStatement(subProcessModel.callSetter("model", e.getKey(), e.getValue()));
				}
			}
		}

		actionBody.addStatement(new ReturnStmt(new NameExpr("model")));
		return actionBody;
	}

	private BlockStmt createInstance(SubProcessNode subProcessNode, ProcessMetaData metadata) {

		String processId = ProcessToExecModelGenerator.extractProcessId(subProcessNode.getProcessId(), null);
		String processFielName = "process" + processId;

		MethodCallExpr processInstanceSupplier = new MethodCallExpr(new NameExpr(processFielName), "createInstance")
				.addArgument("model");

		metadata.addSubProcess(processId, subProcessNode.getProcessId());

		return new BlockStmt().addStatement(new ReturnStmt(processInstanceSupplier));
	}

	private BlockStmt unbind(VariableScope variableScope, SubProcessNode subProcessNode) {
		BlockStmt stmts = new BlockStmt();

		for (Map.Entry<String, String> e : subProcessNode.getOutMappings().entrySet()) {

			// check if given mapping is an expression
			Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(e.getValue());
			if (matcher.find()) {

				String expression = matcher.group(1);
				String topLevelVariable = expression.split("\\.")[0];
				Map<String, String> dataOutputs = (Map<String, String>) subProcessNode.getMetaData("BPMN.OutputTypes");
				Variable variable = new Variable();
				variable.setName(topLevelVariable);
				variable.setType(
						new ObjectDataType(constructClass(dataOutputs.get(e.getKey())), dataOutputs.get(e.getKey())));

				stmts.addStatement(makeAssignment(variableScope.findVariable(topLevelVariable)));
				stmts.addStatement(makeAssignmentFromModel(variable, e.getKey()));

				stmts.addStatement(dotNotationToSetExpression(expression, e.getKey()));

				stmts.addStatement(new MethodCallExpr().setScope(new NameExpr(KCONTEXT_VAR)).setName("setVariable")
						.addArgument(new StringLiteralExpr(topLevelVariable)).addArgument(topLevelVariable));
			} else {

				stmts.addStatement(makeAssignmentFromModel(variableScope.findVariable(e.getValue()), e.getKey()));
				stmts.addStatement(new MethodCallExpr().setScope(new NameExpr(KCONTEXT_VAR)).setName("setVariable")
						.addArgument(new StringLiteralExpr(e.getValue())).addArgument(e.getKey()));
			}

		}

		return stmts;
	}

	protected Expression dotNotationToSetExpression(String dotNotation, String value) {
		String[] elements = dotNotation.split("\\.");
		Expression scope = new NameExpr(elements[0]);
		if (elements.length == 1) {
			return new AssignExpr(scope, new NameExpr(value), AssignExpr.Operator.ASSIGN);
		}
		for (int i = 1; i < elements.length - 1; i++) {
			scope = new MethodCallExpr().setScope(scope).setName("get" + StringUtils.capitalize(elements[i]));
		}

		return new MethodCallExpr().setScope(scope)
				.setName("set" + StringUtils.capitalize(elements[elements.length - 1])).addArgument(value);
	}

	protected Expression dotNotationToGetExpression(String dotNotation) {
		String[] elements = dotNotation.split("\\.");
		Expression scope = new NameExpr(elements[0]);

		if (elements.length == 1) {
			return scope;
		}

		for (int i = 1; i < elements.length; i++) {
			scope = new MethodCallExpr().setScope(scope).setName("get" + StringUtils.capitalize(elements[i]));
		}

		return scope;
	}
}
