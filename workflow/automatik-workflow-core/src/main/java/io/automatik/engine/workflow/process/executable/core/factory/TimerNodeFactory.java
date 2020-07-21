
package io.automatik.engine.workflow.process.executable.core.factory;

import io.automatik.engine.workflow.base.core.timer.Timer;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.NodeContainer;
import io.automatik.engine.workflow.process.core.node.TimerNode;
import io.automatik.engine.workflow.process.executable.core.ExecutableNodeContainerFactory;

public class TimerNodeFactory extends ExtendedNodeFactory {

	public static final String METHOD_TYPE = "type";
	public static final String METHOD_DELAY = "delay";
	public static final String METHOD_PERIOD = "period";
	public static final String METHOD_DATE = "date";

	public TimerNodeFactory(ExecutableNodeContainerFactory nodeContainerFactory, NodeContainer nodeContainer, long id) {
		super(nodeContainerFactory, nodeContainer, id);
	}

	protected Node createNode() {
		return new TimerNode();
	}

	protected TimerNode getTimerNode() {
		return (TimerNode) getNode();
	}

	@Override
	public TimerNodeFactory name(String name) {
		super.name(name);
		return this;
	}

	public TimerNodeFactory type(int type) {
		Timer timer = getTimerNode().getTimer();
		if (timer == null) {
			timer = new Timer();
			getTimerNode().setTimer(timer);
		}
		timer.setTimeType(type);
		return this;
	}

	public TimerNodeFactory delay(String delay) {
		Timer timer = getTimerNode().getTimer();
		if (timer == null) {
			timer = new Timer();
			getTimerNode().setTimer(timer);
		}
		timer.setDelay(delay);
		return this;
	}

	public TimerNodeFactory period(String period) {
		Timer timer = getTimerNode().getTimer();
		if (timer == null) {
			timer = new Timer();
			getTimerNode().setTimer(timer);
		}
		timer.setPeriod(period);
		return this;
	}

	public TimerNodeFactory date(String date) {
		Timer timer = getTimerNode().getTimer();
		if (timer == null) {
			timer = new Timer();
			getTimerNode().setTimer(timer);
		}
		timer.setDate(date);
		return this;
	}
}
