
package io.automatik.engine.workflow.compiler.canonical;

import static io.automatik.engine.workflow.process.executable.core.factory.JoinFactory.METHOD_TYPE;

import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;

import io.automatik.engine.api.definition.process.WorkflowProcess;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.process.core.node.Join;
import io.automatik.engine.workflow.process.executable.core.factory.JoinFactory;

public class JoinNodeVisitor extends AbstractNodeVisitor<Join> {

	@Override
	protected String getNodeKey() {
		return "joinNode";
	}

	@Override
	public void visitNode(WorkflowProcess process, String factoryField, Join node, BlockStmt body,
			VariableScope variableScope, ProcessMetaData metadata) {
		body.addStatement(getAssignedFactoryMethod(factoryField, JoinFactory.class, getNodeId(node), getNodeKey(),
				new LongLiteralExpr(node.getId())));
		body.addStatement(getNameMethod(node, "Join"));
		body.addStatement(getFactoryMethod(getNodeId(node), METHOD_TYPE, new IntegerLiteralExpr(node.getType())));

		visitMetaData(node.getMetaData(), body, getNodeId(node));
		body.addStatement(getDoneMethod(getNodeId(node)));
	}
}
