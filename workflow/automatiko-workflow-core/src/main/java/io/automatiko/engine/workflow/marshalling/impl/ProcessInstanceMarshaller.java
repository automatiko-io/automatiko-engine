
package io.automatiko.engine.workflow.marshalling.impl;

import java.io.IOException;

import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.runtime.process.NodeInstanceContainer;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.api.runtime.process.WorkflowProcessInstance;

/**
 * A ProcessInstanceMarshaller must contain all the write/read logic for nodes
 * of a specific ProcessInstance. It colaborates with OutputMarshaller and
 * InputMarshaller, that delegates in a ProcessInstanceMarshaller to stream
 * in/out runtime information.
 * 
 * @see InputMarshaller
 * @see ProcessMarshallerRegistry
 * 
 */

public interface ProcessInstanceMarshaller {

    public Object writeProcessInstance(MarshallerWriteContext context, ProcessInstance processInstance)
            throws IOException;

    public Object writeNodeInstance(MarshallerWriteContext context, NodeInstance nodeInstance, Object processInstanceBuilder)
            throws IOException;

    public ProcessInstance readProcessInstance(MarshallerReaderContext context) throws IOException;

    public NodeInstance readNodeInstance(MarshallerReaderContext context, NodeInstanceContainer nodeInstanceContainer,
            WorkflowProcessInstance processInstance) throws IOException;
}
