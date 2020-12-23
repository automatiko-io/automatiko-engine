
package io.automatiko.engine.workflow.process.instance.node;

import java.util.Date;

import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.workflow.process.instance.impl.NodeInstanceImpl;

public class ThrowLinkNodeInstance extends NodeInstanceImpl {

	private static final long serialVersionUID = 20110505L;

	@Override
	public void internalTrigger(NodeInstance from, String type) {
		triggerTime = new Date();
		this.triggerCompleted();
	}

	public void triggerCompleted() {
		this.triggerCompleted(io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE, true);
	}

}
