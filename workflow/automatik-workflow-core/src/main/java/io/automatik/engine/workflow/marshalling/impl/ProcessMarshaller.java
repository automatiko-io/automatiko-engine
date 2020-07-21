package io.automatik.engine.workflow.marshalling.impl;

import java.io.IOException;
import java.util.List;

import io.automatik.engine.api.runtime.process.ProcessInstance;
import io.automatik.engine.api.runtime.process.WorkItem;

public interface ProcessMarshaller {

	void writeProcessInstances(MarshallerWriteContext context) throws IOException;

	void writeWorkItems(MarshallerWriteContext context) throws IOException;

	List<ProcessInstance> readProcessInstances(MarshallerReaderContext context) throws IOException;

	void readWorkItems(MarshallerReaderContext context) throws IOException;

	void init(MarshallerReaderContext context);

	void writeWorkItem(MarshallerWriteContext context, WorkItem workItem);

	WorkItem readWorkItem(MarshallerReaderContext context);

}
