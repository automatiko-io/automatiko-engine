
package io.automatik.engine.workflow.compiler.canonical;

import static io.automatik.engine.workflow.process.executable.core.factory.CompositeContextNodeFactory.METHOD_LINK_INCOMING_CONNECTIONS;
import static io.automatik.engine.workflow.process.executable.core.factory.CompositeContextNodeFactory.METHOD_LINK_OUTGOING_CONNECTIONS;
import static io.automatik.engine.workflow.process.executable.core.factory.CompositeContextNodeFactory.METHOD_VARIABLE;
import static io.automatik.engine.workflow.process.executable.core.factory.ForEachNodeFactory.METHOD_COLLECTION_EXPRESSION;
import static io.automatik.engine.workflow.process.executable.core.factory.ForEachNodeFactory.METHOD_OUTPUT_COLLECTION_EXPRESSION;
import static io.automatik.engine.workflow.process.executable.core.factory.ForEachNodeFactory.METHOD_OUTPUT_VARIABLE;

import java.util.Map;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatik.engine.workflow.process.core.node.ForEachNode;
import io.automatik.engine.workflow.process.executable.core.factory.ForEachNodeFactory;

public class ForEachNodeVisitor extends AbstractCompositeNodeVisitor<ForEachNode> {

	public ForEachNodeVisitor(Map<Class<?>, AbstractNodeVisitor<? extends Node>> nodesVisitors) {
		super(nodesVisitors);
	}

	@Override
	protected String getNodeKey() {
		return "forEachNode";
	}

	@Override
	public void visitNode(String factoryField, ForEachNode node, BlockStmt body, VariableScope variableScope,
			ProcessMetaData metadata) {
		body.addStatement(getAssignedFactoryMethod(factoryField, ForEachNodeFactory.class, getNodeId(node),
				getNodeKey(), new LongLiteralExpr(node.getId()))).addStatement(getNameMethod(node, "ForEach"));
		visitMetaData(node.getMetaData(), body, getNodeId(node));

		body.addStatement(
				getFactoryMethod(getNodeId(node), METHOD_COLLECTION_EXPRESSION,
						new StringLiteralExpr(stripExpression(node.getCollectionExpression()))))
				.addStatement(
						getFactoryMethod(getNodeId(node), METHOD_VARIABLE,
								new StringLiteralExpr(node.getVariableName()),
								new ObjectCreationExpr(null,
										new ClassOrInterfaceType(null, ObjectDataType.class.getSimpleName()), NodeList
												.nodeList(
														new ClassExpr(new ClassOrInterfaceType(null,
																node.getVariableType().getClassType()
																		.getCanonicalName())),
														new StringLiteralExpr(
																node.getVariableType().getStringType())))));

		if (node.getOutputCollectionExpression() != null) {
			body.addStatement(getFactoryMethod(getNodeId(node), METHOD_OUTPUT_COLLECTION_EXPRESSION,
					new StringLiteralExpr(stripExpression(node.getOutputCollectionExpression())))).addStatement(
							getFactoryMethod(getNodeId(node), METHOD_OUTPUT_VARIABLE,
									new StringLiteralExpr(node.getOutputVariableName()),
									new ObjectCreationExpr(null,
											new ClassOrInterfaceType(null, ObjectDataType.class.getSimpleName()),
											NodeList.nodeList(
													new ClassExpr(new ClassOrInterfaceType(null,
															node.getOutputVariableType().getClassType()
																	.getCanonicalName())),
													new StringLiteralExpr(
															node.getOutputVariableType().getStringType())))));
		}
		// visit nodes
		visitNodes(getNodeId(node), node.getNodes(), body,
				((VariableScope) node.getCompositeNode().getDefaultContext(VariableScope.VARIABLE_SCOPE)), metadata);
		body.addStatement(getFactoryMethod(getNodeId(node), METHOD_LINK_INCOMING_CONNECTIONS,
				new LongLiteralExpr(node
						.getLinkedIncomingNode(io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE)
						.getNodeId())))
				.addStatement(getFactoryMethod(getNodeId(node), METHOD_LINK_OUTGOING_CONNECTIONS,
						new LongLiteralExpr(node
								.getLinkedOutgoingNode(
										io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE)
								.getNodeId())))
				.addStatement(getDoneMethod(getNodeId(node)));

	}

}
