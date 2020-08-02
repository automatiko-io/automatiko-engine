
package io.automatik.engine.workflow.compiler.canonical;

import static io.automatik.engine.workflow.process.executable.core.Metadata.EVENT_TYPE_SIGNAL;
import static io.automatik.engine.workflow.process.executable.core.Metadata.MESSAGE_TYPE;
import static io.automatik.engine.workflow.process.executable.core.Metadata.TRIGGER_CORRELATION;
import static io.automatik.engine.workflow.process.executable.core.Metadata.TRIGGER_CORRELATION_EXPR;
import static io.automatik.engine.workflow.process.executable.core.Metadata.TRIGGER_MAPPING;
import static io.automatik.engine.workflow.process.executable.core.Metadata.TRIGGER_REF;
import static io.automatik.engine.workflow.process.executable.core.Metadata.TRIGGER_TYPE;
import static io.automatik.engine.workflow.process.executable.core.factory.StartNodeFactory.METHOD_INTERRUPTING;
import static io.automatik.engine.workflow.process.executable.core.factory.StartNodeFactory.METHOD_TIMER;
import static io.automatik.engine.workflow.process.executable.core.factory.StartNodeFactory.METHOD_TRIGGER;

import java.util.Map;
import java.util.Map.Entry;

import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;

import io.automatik.engine.workflow.base.core.context.variable.Variable;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.base.core.timer.Timer;
import io.automatik.engine.workflow.process.core.node.Assignment;
import io.automatik.engine.workflow.process.core.node.DataAssociation;
import io.automatik.engine.workflow.process.core.node.StartNode;
import io.automatik.engine.workflow.process.executable.core.factory.StartNodeFactory;

public class StartNodeVisitor extends AbstractNodeVisitor<StartNode> {

	@Override
	protected String getNodeKey() {
		return "startNode";
	}

	@Override
	public void visitNode(String factoryField, StartNode node, BlockStmt body, VariableScope variableScope,
			ProcessMetaData metadata) {
		body.addStatement(getAssignedFactoryMethod(factoryField, StartNodeFactory.class, getNodeId(node), getNodeKey(),
				new LongLiteralExpr(node.getId()))).addStatement(getNameMethod(node, "Start"))
				.addStatement(getFactoryMethod(getNodeId(node), METHOD_INTERRUPTING,
						new BooleanLiteralExpr(node.isInterrupting())));

		visitMetaData(node.getMetaData(), body, getNodeId(node));

		for (DataAssociation entry : node.getOutAssociations()) {

			if (entry.getAssignments() != null && !entry.getAssignments().isEmpty()) {
				Assignment assignment = entry.getAssignments().get(0);
				body.addStatement(getFactoryMethod(getNodeId(node), "outMapping",
						new StringLiteralExpr(entry.getSources().get(0)), new NullLiteralExpr(),
						new StringLiteralExpr(assignment.getDialect()), new StringLiteralExpr(assignment.getFrom()),
						new StringLiteralExpr(assignment.getTo())));
			} else {
				body.addStatement(getFactoryMethod(getNodeId(node), "outMapping",
						new StringLiteralExpr(entry.getSources().get(0)), new StringLiteralExpr(entry.getTarget()),
						new NullLiteralExpr(), new NullLiteralExpr(), new NullLiteralExpr()));
			}
		}

		body.addStatement(getDoneMethod(getNodeId(node)));
		if (node.getTimer() != null) {
			Timer timer = node.getTimer();
			body.addStatement(getFactoryMethod(getNodeId(node), METHOD_TIMER, getOrNullExpr(timer.getDelay()),
					getOrNullExpr(timer.getPeriod()), getOrNullExpr(timer.getDate()),
					new IntegerLiteralExpr(node.getTimer().getTimeType())));

		} else if (node.getTriggers() != null && !node.getTriggers().isEmpty()) {
			Map<String, Object> nodeMetaData = node.getMetaData();
			metadata.addTrigger(
					new TriggerMetaData((String) nodeMetaData.get(TRIGGER_REF), (String) nodeMetaData.get(TRIGGER_TYPE),
							(String) nodeMetaData.get(MESSAGE_TYPE), (String) nodeMetaData.get(TRIGGER_MAPPING),
							String.valueOf(node.getId()), (String) nodeMetaData.get(TRIGGER_CORRELATION),
							(String) nodeMetaData.get(TRIGGER_CORRELATION_EXPR)).validate());

			handleSignal(node, nodeMetaData, body, variableScope, metadata);
		} else {
			// since there is start node without trigger then make sure it is startable
			metadata.setStartable(true);
		}

	}

	protected void handleSignal(StartNode startNode, Map<String, Object> nodeMetaData, BlockStmt body,
			VariableScope variableScope, ProcessMetaData metadata) {
		if (EVENT_TYPE_SIGNAL.equalsIgnoreCase((String) startNode.getMetaData(TRIGGER_TYPE))) {
			Variable variable = null;
			Map<String, String> variableMapping = startNode.getOutMappings();
			if (variableMapping != null && !variableMapping.isEmpty()) {
				Entry<String, String> varInfo = variableMapping.entrySet().iterator().next();

				body.addStatement(getFactoryMethod(getNodeId(startNode), METHOD_TRIGGER,
						new StringLiteralExpr((String) nodeMetaData.get(MESSAGE_TYPE)), getOrNullExpr(varInfo.getKey()),
						getOrNullExpr(varInfo.getValue())));
				variable = variableScope.findVariable(varInfo.getValue());

				if (variable == null) {
					// check parent node container
					VariableScope vscope = (VariableScope) startNode.resolveContext(VariableScope.VARIABLE_SCOPE,
							varInfo.getKey());
					variable = vscope.findVariable(varInfo.getValue());
				}
			} else {
				body.addStatement(getFactoryMethod(getNodeId(startNode), METHOD_TRIGGER,
						new StringLiteralExpr((String) nodeMetaData.get(MESSAGE_TYPE)),
						new StringLiteralExpr(getOrDefault((String) nodeMetaData.get(TRIGGER_MAPPING), ""))));
			}
			metadata.addSignal((String) nodeMetaData.get(MESSAGE_TYPE),
					variable != null ? variable.getType().getStringType() : null);
		} else {
			String triggerMapping = (String) nodeMetaData.get(TRIGGER_MAPPING);
			body.addStatement(getFactoryMethod(getNodeId(startNode), METHOD_TRIGGER,
					new StringLiteralExpr((String) nodeMetaData.get(TRIGGER_REF)),
					new StringLiteralExpr(getOrDefault((String) nodeMetaData.get(TRIGGER_MAPPING), "")),
					new StringLiteralExpr(getOrDefault(startNode.getOutMapping(triggerMapping), ""))));
		}
	}
}
