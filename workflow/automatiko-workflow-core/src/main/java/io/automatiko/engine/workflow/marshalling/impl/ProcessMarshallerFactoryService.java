package io.automatiko.engine.workflow.marshalling.impl;

import io.automatiko.engine.api.Service;

public interface ProcessMarshallerFactoryService extends Service {

	ProcessMarshaller newProcessMarshaller();

}
