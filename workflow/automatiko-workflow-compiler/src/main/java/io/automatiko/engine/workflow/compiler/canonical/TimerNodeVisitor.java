
package io.automatiko.engine.workflow.compiler.canonical;

import static io.automatiko.engine.workflow.process.executable.core.factory.TimerNodeFactory.METHOD_DATE;
import static io.automatiko.engine.workflow.process.executable.core.factory.TimerNodeFactory.METHOD_DELAY;
import static io.automatiko.engine.workflow.process.executable.core.factory.TimerNodeFactory.METHOD_PERIOD;
import static io.automatiko.engine.workflow.process.executable.core.factory.TimerNodeFactory.METHOD_TYPE;

import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;

import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.core.timer.Timer;
import io.automatiko.engine.workflow.process.core.node.TimerNode;
import io.automatiko.engine.workflow.process.executable.core.factory.TimerNodeFactory;

public class TimerNodeVisitor extends AbstractNodeVisitor<TimerNode> {

	@Override
	protected String getNodeKey() {
		return "timerNode";
	}

	@Override
	public void visitNode(WorkflowProcess process, String factoryField, TimerNode node, BlockStmt body,
			VariableScope variableScope, ProcessMetaData metadata) {
		body.addStatement(getAssignedFactoryMethod(factoryField, TimerNodeFactory.class, getNodeId(node), getNodeKey(),
				new LongLiteralExpr(node.getId()))).addStatement(getNameMethod(node, "Timer"));

		Timer timer = node.getTimer();
		body.addStatement(getFactoryMethod(getNodeId(node), METHOD_TYPE, new IntegerLiteralExpr(timer.getTimeType())));

		if (timer.getTimeType() == Timer.TIME_CYCLE) {
			body.addStatement(getFactoryMethod(getNodeId(node), METHOD_DELAY, new StringLiteralExpr(timer.getDelay())));
			if (timer.getPeriod() != null && !timer.getPeriod().isEmpty()) {
				body.addStatement(
						getFactoryMethod(getNodeId(node), METHOD_PERIOD, new StringLiteralExpr(timer.getPeriod())));
			}
		} else if (timer.getTimeType() == Timer.TIME_DURATION) {
			body.addStatement(getFactoryMethod(getNodeId(node), METHOD_DELAY, new StringLiteralExpr(timer.getDelay())));
		} else if (timer.getTimeType() == Timer.TIME_DATE) {
			body.addStatement(getFactoryMethod(getNodeId(node), METHOD_DATE, new StringLiteralExpr(timer.getDate())));
		}

		visitMetaData(node.getMetaData(), body, getNodeId(node));
		body.addStatement(getDoneMethod(getNodeId(node)));
	}
}
