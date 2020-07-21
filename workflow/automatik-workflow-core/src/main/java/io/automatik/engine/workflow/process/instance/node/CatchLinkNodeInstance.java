
package io.automatik.engine.workflow.process.instance.node;

import java.util.Date;

import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.workflow.process.instance.impl.NodeInstanceImpl;

public class CatchLinkNodeInstance extends NodeInstanceImpl {

	private static final long serialVersionUID = 20110505L;

	@Override
	public void internalTrigger(NodeInstance from, String type) {
		triggerTime = new Date();
		this.triggerCompleted();

	}

	public void triggerCompleted() {
		this.triggerCompleted(io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE, true);
	}

}
