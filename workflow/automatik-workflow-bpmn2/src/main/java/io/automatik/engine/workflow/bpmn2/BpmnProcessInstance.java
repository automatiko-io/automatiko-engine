
package io.automatik.engine.workflow.bpmn2;

import java.util.Map;

import io.automatik.engine.api.runtime.process.ProcessRuntime;
import io.automatik.engine.workflow.AbstractProcess;
import io.automatik.engine.workflow.AbstractProcessInstance;

public class BpmnProcessInstance extends AbstractProcessInstance<BpmnVariables> {

	public BpmnProcessInstance(AbstractProcess<BpmnVariables> process, BpmnVariables variables, ProcessRuntime rt) {
		super(process, variables, rt);
	}

	public BpmnProcessInstance(AbstractProcess<BpmnVariables> process, BpmnVariables variables, String businessKey,
			ProcessRuntime rt) {
		super(process, variables, businessKey, rt);
	}

	@Override
	protected Map<String, Object> bind(BpmnVariables variables) {

		if (variables == null) {
			return null;
		}
		return variables.toMap();
	}

	@Override
	protected void unbind(BpmnVariables variables, Map<String, Object> vmap) {

		if (variables == null) {
			return;
		}
		variables.fromMap(vmap);
	}
}
