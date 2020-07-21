
package io.automatik.engine.workflow.compiler.canonical;

import static io.automatik.engine.workflow.process.executable.core.factory.FaultNodeFactory.METHOD_FAULT_NAME;
import static io.automatik.engine.workflow.process.executable.core.factory.FaultNodeFactory.METHOD_FAULT_VARIABLE;

import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;

import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.process.core.node.FaultNode;
import io.automatik.engine.workflow.process.executable.core.factory.FaultNodeFactory;

public class FaultNodeVisitor extends AbstractNodeVisitor<FaultNode> {

	@Override
	protected String getNodeKey() {
		return "faultNode";
	}

	@Override
	public void visitNode(String factoryField, FaultNode node, BlockStmt body, VariableScope variableScope,
			ProcessMetaData metadata) {
		body.addStatement(getAssignedFactoryMethod(factoryField, FaultNodeFactory.class, getNodeId(node), getNodeKey(),
				new LongLiteralExpr(node.getId()))).addStatement(getNameMethod(node, "Error"));
		if (node.getFaultVariable() != null) {
			body.addStatement(getFactoryMethod(getNodeId(node), METHOD_FAULT_VARIABLE,
					new StringLiteralExpr(node.getFaultVariable())));
		}
		if (node.getFaultName() != null) {
			body.addStatement(
					getFactoryMethod(getNodeId(node), METHOD_FAULT_NAME, new StringLiteralExpr(node.getFaultName())));
		}

		visitMetaData(node.getMetaData(), body, getNodeId(node));
		body.addStatement(getDoneMethod(getNodeId(node)));

	}
}
