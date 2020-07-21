
package io.automatik.engine.workflow.compiler.canonical;

import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;

import io.automatik.engine.workflow.base.core.Work;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.process.core.node.HumanTaskNode;
import io.automatik.engine.workflow.process.executable.core.factory.HumanTaskNodeFactory;

public class HumanTaskNodeVisitor extends WorkItemNodeVisitor<HumanTaskNode> {

	public HumanTaskNodeVisitor() {
		super(null);
	}

	@Override
	protected String getNodeKey() {
		return "humanTaskNode";
	}

	@Override
	public void visitNode(String factoryField, HumanTaskNode node, BlockStmt body, VariableScope variableScope,
			ProcessMetaData metadata) {
		Work work = node.getWork();

		body.addStatement(getAssignedFactoryMethod(factoryField, HumanTaskNodeFactory.class, getNodeId(node),
				getNodeKey(), new LongLiteralExpr(node.getId()))).addStatement(getNameMethod(node, "Task"));

		addWorkItemParameters(work, body, getNodeId(node));
		addNodeMappings(node, body, getNodeId(node));
		body.addStatement(getDoneMethod(getNodeId(node)));

		visitMetaData(node.getMetaData(), body, getNodeId(node));

		metadata.getWorkItems().add(work.getName());
	}
}
