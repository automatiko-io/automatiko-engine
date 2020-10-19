
package io.automatik.engine.workflow.marshalling.impl;

import io.automatik.engine.workflow.process.executable.instance.ExecutableProcessInstance;
import io.automatik.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;

/**
 * Marshaller class for RuleFlowProcessInstances
 * 
 */

public class ProtobufRuleFlowProcessInstanceMarshaller extends AbstractProtobufProcessInstanceMarshaller {

    public static ProtobufRuleFlowProcessInstanceMarshaller INSTANCE = new ProtobufRuleFlowProcessInstanceMarshaller();

    protected ProtobufRuleFlowProcessInstanceMarshaller() {
    }

    protected WorkflowProcessInstanceImpl createProcessInstance() {
        return new ExecutableProcessInstance();
    }

}
