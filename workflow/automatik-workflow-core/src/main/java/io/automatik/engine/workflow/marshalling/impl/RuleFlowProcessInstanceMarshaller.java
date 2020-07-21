
package io.automatik.engine.workflow.marshalling.impl;

import io.automatik.engine.workflow.process.executable.instance.ExecutableProcessInstance;
import io.automatik.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;

/**
 * Marshaller class for RuleFlowProcessInstances
 * 
 */

public class RuleFlowProcessInstanceMarshaller extends AbstractProcessInstanceMarshaller {

	public static RuleFlowProcessInstanceMarshaller INSTANCE = new RuleFlowProcessInstanceMarshaller();

	private RuleFlowProcessInstanceMarshaller() {
	}

	protected WorkflowProcessInstanceImpl createProcessInstance() {
		return new ExecutableProcessInstance();
	}

}
