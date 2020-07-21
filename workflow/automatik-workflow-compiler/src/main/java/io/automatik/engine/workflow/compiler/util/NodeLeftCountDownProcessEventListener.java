
package io.automatik.engine.workflow.compiler.util;

import io.automatik.engine.api.event.process.ProcessNodeLeftEvent;

public class NodeLeftCountDownProcessEventListener extends NodeCountDownProcessEventListener {

	private boolean reactOnBeforeNodeLeft = false;

	public NodeLeftCountDownProcessEventListener() {

	}

	public NodeLeftCountDownProcessEventListener(String nodeName, int threads) {
		super(nodeName, threads);
	}

	public NodeLeftCountDownProcessEventListener(String nodeName, int threads, boolean reactOnBeforeNodeLeft) {
		super(nodeName, threads);
		this.reactOnBeforeNodeLeft = reactOnBeforeNodeLeft;
	}

	@Override
	public void afterNodeLeft(ProcessNodeLeftEvent event) {
		if (nodeName.equals(event.getNodeInstance().getNodeName())) {
			countDown();
		}
	}

	@Override
	public void beforeNodeLeft(ProcessNodeLeftEvent event) {
		if (reactOnBeforeNodeLeft && nodeName.equals(event.getNodeInstance().getNodeName())) {
			countDown();
		}
	}
}