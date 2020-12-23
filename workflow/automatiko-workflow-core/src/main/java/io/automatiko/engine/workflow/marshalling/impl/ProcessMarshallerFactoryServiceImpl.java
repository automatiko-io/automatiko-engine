
package io.automatiko.engine.workflow.marshalling.impl;

public class ProcessMarshallerFactoryServiceImpl implements ProcessMarshallerFactoryService {

	public ProcessMarshaller newProcessMarshaller() {
		return new ProtobufProcessMarshaller();
	}

}
