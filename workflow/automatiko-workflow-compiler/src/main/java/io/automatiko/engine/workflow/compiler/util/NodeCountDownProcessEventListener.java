
package io.automatiko.engine.workflow.compiler.util;

import java.util.concurrent.CountDownLatch;

public class NodeCountDownProcessEventListener extends DefaultCountDownProcessEventListener {

	protected String nodeName;

	public NodeCountDownProcessEventListener() {

	}

	public NodeCountDownProcessEventListener(String nodeName, int threads) {
		super(threads);
		this.nodeName = nodeName;
	}

	public void reset(String nodeName, int threads) {
		this.nodeName = nodeName;
		this.latch = new CountDownLatch(threads);
	}
}