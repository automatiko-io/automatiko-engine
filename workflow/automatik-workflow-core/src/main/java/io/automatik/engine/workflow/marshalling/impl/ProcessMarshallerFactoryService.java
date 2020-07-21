package io.automatik.engine.workflow.marshalling.impl;

import io.automatik.engine.api.Service;

public interface ProcessMarshallerFactoryService extends Service {

	ProcessMarshaller newProcessMarshaller();

}
