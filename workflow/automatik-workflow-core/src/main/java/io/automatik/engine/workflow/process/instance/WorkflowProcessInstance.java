
package io.automatik.engine.workflow.process.instance;

import io.automatik.engine.api.runtime.process.EventListener;
import io.automatik.engine.workflow.base.instance.ProcessInstance;

public interface WorkflowProcessInstance
		extends ProcessInstance, io.automatik.engine.api.runtime.process.WorkflowProcessInstance {

	void addEventListener(String type, EventListener eventListener, boolean external);

	void removeEventListener(String type, EventListener eventListener, boolean external);

}
