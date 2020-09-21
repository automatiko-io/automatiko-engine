
package io.automatik.engine.workflow.compiler.canonical;

import static io.automatik.engine.workflow.process.executable.core.factory.RuleSetNodeFactory.METHOD_DECISION;
import static io.automatik.engine.workflow.process.executable.core.factory.RuleSetNodeFactory.METHOD_PARAMETER;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.UnknownType;

import io.automatik.engine.api.definition.process.WorkflowProcess;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.process.core.node.RuleSetNode;
import io.automatik.engine.workflow.process.executable.core.factory.RuleSetNodeFactory;

public class RuleSetNodeVisitor extends AbstractNodeVisitor<RuleSetNode> {

	public static final Logger logger = LoggerFactory.getLogger(ProcessToExecModelGenerator.class);

	private final ClassLoader contextClassLoader;

	public RuleSetNodeVisitor(ClassLoader contextClassLoader) {
		this.contextClassLoader = contextClassLoader;
	}

	@Override
	protected String getNodeKey() {
		return "ruleSetNode";
	}

	@Override
	public void visitNode(WorkflowProcess process, String factoryField, RuleSetNode node, BlockStmt body,
			VariableScope variableScope, ProcessMetaData metadata) {
		String nodeName = node.getName();

		body.addStatement(getAssignedFactoryMethod(factoryField, RuleSetNodeFactory.class, getNodeId(node),
				getNodeKey(), new LongLiteralExpr(node.getId()))).addStatement(getNameMethod(node, "Rule"));

		RuleSetNode.RuleType ruleType = node.getRuleType();
		if (ruleType.getName().isEmpty()) {
			throw new IllegalArgumentException(MessageFormat.format(
					"Rule task \"{0}\" is invalid: you did not set a unit name, a rule flow group or a decision model.",
					nodeName));
		}

		addNodeMappings(node, body, getNodeId(node));
		addParams(node, body, getNodeId(node));

		NameExpr methodScope = new NameExpr(getNodeId(node));
		MethodCallExpr m;
		if (ruleType.isDecision()) {
			m = handleDecision((RuleSetNode.RuleType.Decision) ruleType);
		} else {
			throw new IllegalArgumentException(
					"Rule task " + nodeName + "is invalid: unsupported rule language " + node.getLanguage());
		}
		m.setScope(methodScope);
		body.addStatement(m);

		visitMetaData(node.getMetaData(), body, getNodeId(node));
		body.addStatement(getDoneMethod(getNodeId(node)));
	}

	private void addParams(RuleSetNode node, BlockStmt body, String nodeId) {
		node.getParameters().forEach((k, v) -> body.addStatement(getFactoryMethod(nodeId, METHOD_PARAMETER,
				new StringLiteralExpr(k), new StringLiteralExpr(v.toString()))));
	}

	private MethodCallExpr handleDecision(RuleSetNode.RuleType.Decision ruleType) {

		StringLiteralExpr namespace = new StringLiteralExpr(ruleType.getNamespace());
		StringLiteralExpr model = new StringLiteralExpr(ruleType.getModel());
		Expression decision = ruleType.getDecision() == null ? new NullLiteralExpr()
				: new StringLiteralExpr(ruleType.getDecision());

		MethodCallExpr decisionModels = new MethodCallExpr(new NameExpr("app"), "decisionModels");
		MethodCallExpr decisionModel = new MethodCallExpr(decisionModels, "getDecisionModel").addArgument(namespace)
				.addArgument(model);

		BlockStmt actionBody = new BlockStmt();
		LambdaExpr lambda = new LambdaExpr(new Parameter(new UnknownType(), "()"), actionBody);
		actionBody.addStatement(new ReturnStmt(decisionModel));

		return new MethodCallExpr(METHOD_DECISION).addArgument(namespace).addArgument(model).addArgument(decision)
				.addArgument(lambda);
	}

}
